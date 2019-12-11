package com.olekdia.soundpool

import android.media.*
import android.media.AudioTrack.*
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.olekdia.androidcommon.extensions.buildAudioTrack
import com.olekdia.androidcommon.extensions.ifNotNull
import java.io.Closeable
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ThreadPoolExecutor

class SoundSample(
    val id: Int,
    bufferMaxSizeProposed: Int,
    private var isStatic: Boolean
) : Closeable {
    private val bufferMaxSize: Int = if (isStatic) bufferMaxSizeProposed * 2 else bufferMaxSizeProposed
    private val playRun = PlayRunnable()
    private val codecLock = Any()
    private var audioTrack: AudioTrack? = null // Null if initial data not loaded yet

    private var audioBuffer: ByteArray? = null
    private var bufferSize: Int = 0

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var mediaFormat: MediaFormat? = null

    // true - if all sound data loaded to audioBuffer buffer, false - otherwise.
    private var isFullyLoaded = true
    @Volatile
    private var toPlayCount = 1
    private var frameSizeInBytes: Int = 0
    @Volatile
    private var pausedPlaybackInBytes: Int = 0
    private var currLeftVolume = -1F
    private var currRightVolume = -1F
    private var currRate = -1F

    // Bug fix flag. On early androids api 16 - 18 the bug occurs when call codec.stop(),
    // and then reconfigure it and start it again before reaching eof
    @Volatile
    private var isCodecStarted: Boolean = false
    @Volatile
    var isClosed: Boolean = false
        private set

    @WorkerThread
    fun load(
        fd: FileDescriptor,
        fileOffset: Long,
        fileSize: Long
    ): Boolean {
        synchronized(codecLock) {
            isStatic = isStatic && fileSize < MAX_STATIC_SIZE
            audioBuffer = if (isStatic) {
                ByteArray(fileSize.toInt() * 12)
            } else {
                ByteArray(bufferMaxSize + 1024 * 1024)
            }

            mediaFormat = null
            var rawMime: String? = null
            var channels: Int = 1
            var sampleRate: Int = 44100

            extractor = MediaExtractor()
                .also {
                    try {
                        it.setDataSource(fd, fileOffset, fileSize)
                        mediaFormat = it.getTrackFormat(0)
                            ?.apply {
                                rawMime = getString(MediaFormat.KEY_MIME)
                                sampleRate = getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                channels = getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            val mime: String? = rawMime
            if (mediaFormat == null
                || mime == null
                || !mime.startsWith("audio/")
            ) {
                return false.also { close() }
            }

            try {
                codec = MediaCodec.createDecoderByType(mime)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (codec == null) {
                return false.also { close() }
            }

            extractor?.selectTrack(0)
            loadNextSamples(true)

            if (isClosed) {
                return false
            }

            val channelConfiguration: Int = if (channels == 1) {
                AudioFormat.CHANNEL_OUT_MONO
            } else {
                AudioFormat.CHANNEL_OUT_STEREO
            }
            val minSize: Int = getMinBufferSize(
                sampleRate,
                channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val writeBuffSizeInBytes: Int =
                if (isFullyLoaded
                    && fileSize < SMALL_FILE_SIZE
                ) {
                    minSize
                } else {
                    minSize * 2
                }

            audioTrack = buildAudioTrack(
                writeBuffSizeInBytes,
                channelConfiguration,
                sampleRate
            )
            frameSizeInBytes = channels * 2 //  frameSizeInBytes = mChannelCount * AudioFormat.getBytesPerSample(mAudioFormat);

            /*if (BuildConfig.DEBUG) Log.d(
                "SoundSample loaded:",
                " fileSize(Kb): " + fileSize / 1024
                    + ", loadedSize(Kb): " + (audioBuffer == null ? "0" : bufferSize / 1024)
                    + ", writeBuffSize(b): " + writeBuffSizeInBytes
                    + ", isFullyLoaded: " + isFullyLoaded
                    + ", isStatic: " + isStatic
            );*/
        }
        return true
    }

    @WorkerThread
    private fun loadNextSamples(isFromStart: Boolean) {
        ifNotNull(
            codec, extractor
        ) { codec, extractor ->
            startCodec()
            if (!isCodecStarted) {
                releaseCodec()
                return
            }

            val bufInfo = MediaCodec.BufferInfo()
            val waitTimeout: Long = 1000 // 0
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (isClosed) {
                    stopCodec()
                    return
                }
                if (!sawInputEOS) {
                    val inputBufIndex: Int = codec.dequeueInputBuffer(waitTimeout)
                    if (inputBufIndex >= 0) {
                        val inputBuffer: ByteBuffer = codec.inputBuffers[inputBufIndex]
                        var sampleSize: Int = extractor.readSampleData(inputBuffer, 0)
                        var sampleTime: Long = 0L

                        if (sampleSize <= 0) {
                            sawInputEOS = true
                            sampleSize = 0
                        } else {
                            sampleTime = extractor.sampleTime
                        }

                        codec.queueInputBuffer(
                            inputBufIndex,
                            0,
                            sampleSize,
                            sampleTime,
                            if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        )

                        if (!sawInputEOS) extractor.advance()
                    }
                }

                val outputBufIndex: Int = codec.dequeueOutputBuffer(bufInfo, waitTimeout)
                if (outputBufIndex >= 0) {
                    val outputBuffer: ByteBuffer = codec.outputBuffers[outputBufIndex]

                    if (isFromStart) {
                        checkBufferSize(bufInfo.size)
                        audioBuffer?.let{ outputBuffer.get(it, bufferSize, bufInfo.size) }
                        bufferSize += bufInfo.size
                        if (bufferSize > bufferMaxSize) {
                            sawOutputEOS = true
                            isFullyLoaded = false
                        }
                    } else {
                        if (isPlaying() && !isClosed) {
                            val chunk = ByteArray(bufInfo.size)
                            outputBuffer.get(chunk)
                            audioTrack?.write(chunk, 0, chunk.size)
                            if (isStatic) {
                                checkBufferSize(chunk.size)
                                audioBuffer?.let { System.arraycopy(chunk, 0, it, bufferSize, chunk.size) }
                                bufferSize += chunk.size
                            }
                        } else {
                            sawOutputEOS = true
                        }
                    }

                    outputBuffer.clear()
                    codec.releaseOutputBuffer(outputBufIndex, false)

                    if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            if (isFullyLoaded) {
                stopCodec()
                releaseCodec()
            } else {
                audioTrack?.let { track ->
                    if (!isFromStart
                        && track.playState != PLAYSTATE_PAUSED
                    ) {
                        if (isStatic) {
                            if (track.playState != PLAYSTATE_STOPPED) {
                                isFullyLoaded = true
                                stopCodec()
                                releaseCodec()
                            }
                        } else {
                            stopCodec()
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                        }
                    }
                }

            }
        }
    }

    private fun checkBufferSize(extraSize: Int) {
        audioBuffer?.let {
            if (bufferSize + extraSize > it.size) {
                audioBuffer = Arrays.copyOf(it, it.size + it.size / 8)
            }
        }
    }

    override fun close() {
        isClosed = true
        synchronized(codecLock) {
            audioTrack?.let {
                stop()
                it.release()
            }
            audioTrack = null
            releaseCodec()
            audioBuffer = null
        }
    }

    private fun startCodec() {
        codec?.let {
            if (!isCodecStarted) {
                it.configure(mediaFormat, null, null, 0)
                it.start()
                isCodecStarted = true
            }
        }
    }

    private fun stopCodec() {
        codec?.let {
            if (isCodecStarted) {
                it.stop()
                isCodecStarted = false
            }
        }
    }

    private fun releaseCodec() {
        if (isCodecStarted) stopCodec()
        codec?.release()
        codec = null

        extractor?.release()
        extractor = null

        mediaFormat = null
    }

    fun isPlaying(): Boolean = audioTrack?.playState == PLAYSTATE_PLAYING

    fun isPaused(): Boolean = audioTrack?.playState == PLAYSTATE_PAUSED

    fun isStopped(): Boolean = audioTrack?.playState == PLAYSTATE_STOPPED

    @UiThread
    fun play(
        leftVolume: Float,
        rightVolume: Float,
        loop: Int,
        rate: Float,
        threadPool: ThreadPoolExecutor
    ) {
        if (isPlaying()) return

        setVolume(leftVolume, rightVolume)
        setRate(rate)
        setLoop(loop)

        threadPool.execute(playRun)
    }

    @WorkerThread
    fun play(
        leftVolume: Float,
        rightVolume: Float,
        rate: Float
    ) {
        if (isPlaying()) return

        setVolume(leftVolume, rightVolume)
        setRate(rate)
        playRun.run()
    }

    @UiThread
    fun stop() {
        audioTrack?.let { track ->
            val state = track.playState

            if (state != PLAYSTATE_STOPPED) {
                if (state == PLAYSTATE_PLAYING) track.pause()
                pausedPlaybackInBytes = 0
                track.flush()
                track.stop()
            }
        }
    }

    @UiThread
    fun pause() {
        if ((audioTrack ?: return).playState != PLAYSTATE_PLAYING) return

        audioTrack?.let {
            it.pause()
            val playbackInFrames = it.playbackHeadPosition
            pausedPlaybackInBytes = if (playbackInFrames > 0) playbackInFrames * frameSizeInBytes else 0
            // Skip written bytes, so next resume will be in sync with pausedPlaybackInBytes
            if (audioBuffer != null) it.flush()
        }
    }

    @UiThread
    fun resume(threadPool: ThreadPoolExecutor) {
        if ((audioTrack ?: return).playState != PLAYSTATE_PAUSED) return

        threadPool.execute(playRun)
    }

    @AnyThread
    fun setVolume(
        leftVolume: Float,
        rightVolume: Float
    ) {
        if (currLeftVolume != leftVolume || currRightVolume != rightVolume) {
            audioTrack?.setStereoVolume(leftVolume, rightVolume)
            currLeftVolume = leftVolume
            currRightVolume = rightVolume
        }
    }

    @AnyThread
    fun setRate(rate: Float) {
        if (currRate != rate) {
            audioTrack?.run {
                playbackRate = (rate * sampleRate).toInt()
            }
            currRate = rate
        }
    }

    @UiThread
    fun setLoop(loop: Int) {
        toPlayCount = if (loop == -1) Int.MAX_VALUE else 1 + loop
    }

//--------------------------------------------------------------------------------------------------
//  Runnable classes
//--------------------------------------------------------------------------------------------------

    inner class PlayRunnable : Runnable {
        override fun run() {
            synchronized(codecLock) {
                audioTrack?.let { track ->
                    while (toPlayCount > 0) {
                        if (isClosed) return

                        if (track.playState != PLAYSTATE_PLAYING) track.play()

                        audioBuffer?.let { buffer ->
                            if (isFullyLoaded || pausedPlaybackInBytes == 0) {
                                val offsetInBytes = pausedPlaybackInBytes
                                pausedPlaybackInBytes = 0
                                track.write(buffer, offsetInBytes, bufferSize)
                            }
                        }

                        if (isClosed || track.playState != PLAYSTATE_PLAYING) return

                        if (!isFullyLoaded) {
                            if (!isStatic) audioBuffer = null
                            loadNextSamples(false)
                        }

                        if (isClosed || track.playState != PLAYSTATE_PLAYING) return
                        toPlayCount--
                    }

                    track.stop()
                    pausedPlaybackInBytes = 0
                }
            }
        }
    }

    companion object {
        const val MAX_STATIC_SIZE = 140 * 1024 // 140 Kb
        const val SMALL_FILE_SIZE = 20 * 1024 // 20 Kb
    }
}