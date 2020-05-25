package com.olekdia.soundpool

import android.media.*
import android.media.AudioTrack.*
import android.os.Build
import androidx.annotation.AnyThread
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.olekdia.androidcommon.extensions.buildAudioTrack
import com.olekdia.androidcommon.extensions.getInputBufferCompat
import com.olekdia.androidcommon.extensions.getOutputBufferCompat
import com.olekdia.common.extensions.ifNotNull
import com.olekdia.soundpool.SoundSample.Companion.CodecState.Companion.CONFIGURED
import com.olekdia.soundpool.SoundSample.Companion.CodecState.Companion.ERROR
import com.olekdia.soundpool.SoundSample.Companion.CodecState.Companion.EXECUTING
import com.olekdia.soundpool.SoundSample.Companion.CodecState.Companion.RELEASED
import com.olekdia.soundpool.SoundSample.Companion.CodecState.Companion.UNINITIALIZED
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
    private var mime: String? = null

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

    @CodecState
    @Volatile
    private var codecState: Int = UNINITIALIZED
    @Volatile
    private var _isClosed: Boolean = true

    var isClosed: Boolean
        get() = _isClosed
        private set(value) {
            _isClosed = value
        }

    @WorkerThread
    fun load(descriptor: SoundSampleDescriptor): Boolean {
        _isClosed = false
        synchronized(codecLock) {
            isStatic = isStatic && descriptor.fileSize < MAX_STATIC_SIZE
            audioBuffer = if (isStatic) {
                ByteArray(descriptor.fileSize.toInt() * 12)
            } else {
                ByteArray(bufferMaxSize * 14)
            }

            mediaFormat = null
            var mime: String? = null
            var channels: Int = 1
            var sampleRate: Int = 44100

            extractor = MediaExtractor()
                .also {
                    try {
                        descriptor.setExtractorDataSource(it)
                        mediaFormat = it.getTrackFormat(0)
                            ?.apply {
                                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
                                mime = getString(MediaFormat.KEY_MIME)
                                sampleRate = getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                channels = getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            this.mime = mime?.let {
                if (mediaFormat != null
                    && it.startsWith("audio/")
                ) {
                    it
                } else {
                    null
                }
            }

            createCodec()
            if (codec == null
                || mediaFormat == null
            ) {
                return false.also { close() }
            }

            extractor?.selectTrack(0)
            loadNextSamples(true)

            if (_isClosed) {
                return false.also { close() }
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
                    && descriptor.fileSize < SMALL_FILE_SIZE
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
        return audioTrack.let {
            if (it == null || it.state == STATE_UNINITIALIZED) {
                close()
                false
            } else {
                true
            }
        }
    }

    // Inside codecLock
    @WorkerThread
    private fun loadNextSamples(isFromStart: Boolean) {
        ifNotNull(
            codec, extractor
        ) { codec, extractor ->
            if (!startCodec()) return

            val bufInfo = MediaCodec.BufferInfo()
            val waitTimeout: Long = 1000 // microseconds to wait before get buffer
            var sawInputEOS = false
            var sawOutputEOS = false

            try {
                while (!sawOutputEOS) {
                    if (_isClosed) return

                    if (!sawInputEOS) {
                        val inputBufIndex: Int = codec.dequeueInputBuffer(waitTimeout)
                        if (inputBufIndex >= 0) {
                            val inputBuffer: ByteBuffer =
                                codec.getInputBufferCompat(inputBufIndex) ?: continue
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
                        val outputBuffer: ByteBuffer =
                            codec.getOutputBufferCompat(outputBufIndex) ?: continue

                        if (isFromStart) {
                            checkBufferSize(bufInfo.size)
                            audioBuffer?.let { outputBuffer.get(it, bufferSize, bufInfo.size) }
                            bufferSize += bufInfo.size
                            if (bufferSize > bufferMaxSize) {
                                sawOutputEOS = true
                                isFullyLoaded = false
                            }
                        } else {
                            if (!_isClosed) {
                                val chunk = ByteArray(bufInfo.size)
                                outputBuffer.get(chunk)

                                audioTrack.let { track ->
                                    if (track?.playState == PLAYSTATE_PLAYING) {
                                        track.write(chunk, 0, chunk.size)
                                        if (isStatic) {
                                            checkBufferSize(chunk.size)
                                            audioBuffer?.let {
                                                System.arraycopy(
                                                    chunk,
                                                    0,
                                                    it,
                                                    bufferSize,
                                                    chunk.size
                                                )
                                            }
                                            bufferSize += chunk.size
                                        }
                                    } else {
                                        sawOutputEOS = true
                                    }
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
            } catch (e: IllegalStateException) {
                codecState = ERROR
            }

            if (isFullyLoaded) {
                // If fully loaded, we no longer need the codec
                releaseCodec()
            } else {
                audioTrack?.let { track ->
                    if (!isFromStart
                        && track.playState != PLAYSTATE_PAUSED
                    ) {
                        if (isStatic) {
                            // For static track, here track is not stopped and paused,
                            // that means that track is playing but hit EOF,
                            // so we should mark it as FullyLoaded and releaseCoded, we no longer need it
                            if (track.playState != PLAYSTATE_STOPPED) {
                                isFullyLoaded = true
                                releaseCodec()
                            }
                        } else {
                            // For non static track, here track could be stopped or hit EOF,
                            // so we need seekTo(0), so next play will be started from the beginning
                            stopCodecAndSeek0()
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
        _isClosed = true
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

    // Inside codecLock
    private fun createCodec() {
        codec = try {
            mime?.let { MediaCodec.createDecoderByType(it) }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Inside codecLock
    // Returns isSuccess
    private fun startCodec(): Boolean =
        codec
            ?.let {
                when (codecState) {
                    ERROR -> run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            it.reset()
                                .also { codecState = UNINITIALIZED }
                        } else { // Recreate codec
                            codec?.release()
                                .also { codecState = RELEASED }
                            createCodec()
                                .also { codecState = UNINITIALIZED }
                        }
                        startCodec() // Recursive
                    }

                    UNINITIALIZED -> {
                        // If we got IllegalStateException,
                        // that means that codec has moved to ERROR state silently
                        try {
                            it.configure(mediaFormat, null, null, 0)
                                .also { codecState = CONFIGURED }
                        } catch (e: IllegalStateException) {
                            codecState = ERROR
                            e.printStackTrace()
                        }
                        startCodec() // Recursive
                    }

                    CONFIGURED -> {
                        it.start()
                            .also { codecState = EXECUTING }
                        true
                    }

                    EXECUTING -> true

                    else -> false
                }
            }
            ?: false

    // Inside codecLock
    private fun stopCodecAndSeek0() {
        synchronized(codecLock) {
            stopCodec()
            extractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }
    }

    // Inside codecLock
    private fun stopCodec() {
        codec?.let {
            if (codecState == EXECUTING) {
                codecState = try {
                    it.stop()
                    UNINITIALIZED
                } catch (e: IllegalStateException) {
                    ERROR
                }
            }
        }
    }

    // Inside codecLock
    private fun releaseCodec() {
        stopCodec()
        codec?.release()
            .also { codecState = RELEASED }
        codec = null

        extractor?.release()
        extractor = null

        mediaFormat = null
    }

    fun isLoaded(): Boolean = audioTrack != null

    fun isPlaying(): Boolean = audioTrack?.playState == PLAYSTATE_PLAYING

    fun isPaused(): Boolean = audioTrack?.playState == PLAYSTATE_PAUSED

    fun isStopped(): Boolean = audioTrack?.playState == PLAYSTATE_STOPPED

    /**
     * @param leftVolume (range = 0.0 to 1.0)
     * @param rightVolume (range = 0.0 to 1.0)
     */
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

    /**
     * @param leftVolume (range = 0.0 to 1.0)
     * @param rightVolume (range = 0.0 to 1.0)
     */
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
                when (state) {
                    PLAYSTATE_PLAYING ->
                        track.pauseSafely()

                    // If track was already paused we need seekTo(0),
                    // as it would not be called in loadNextSamples
                    PLAYSTATE_PAUSED ->
                        stopCodecAndSeek0()
                }
                pausedPlaybackInBytes = 0
                track.flush()
                track.stopSafely()
            }
        }
    }

    @UiThread
    fun pause() {
        if ((audioTrack ?: return).playState != PLAYSTATE_PLAYING) return

        audioTrack?.let { track ->
            track.pauseSafely()

            track.playbackHeadPosition.let { playbackInFrames ->
                pausedPlaybackInBytes = if (playbackInFrames > 0) {
                    playbackInFrames * frameSizeInBytes
                } else {
                    0
                }
            }
            // Skip written bytes, so next resume will be in sync with pausedPlaybackInBytes
            if (audioBuffer != null) track.flush()
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
                        if (_isClosed) return

                        if (track.playState != PLAYSTATE_PLAYING) track.play()

                        audioBuffer?.let { buffer ->
                            if (isFullyLoaded || pausedPlaybackInBytes == 0) {
                                val offsetInBytes = pausedPlaybackInBytes
                                pausedPlaybackInBytes = 0
                                track.write(buffer, offsetInBytes, bufferSize)
                            }
                        }

                        if (_isClosed || track.playState != PLAYSTATE_PLAYING) return

                        if (!isFullyLoaded) {
                            if (!isStatic) audioBuffer = null
                            loadNextSamples(false)
                        }

                        if (_isClosed || track.playState != PLAYSTATE_PLAYING) return
                        toPlayCount--
                    }

                    track.stopSafely()
                    pausedPlaybackInBytes = 0
                }
            }
        }
    }

    companion object {
        const val MAX_STATIC_SIZE = 140 * 1024 // 140 Kb
        const val SMALL_FILE_SIZE = 20 * 1024 // 20 Kb

        fun AudioTrack.pauseSafely() {
            try {
                this.pause()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        fun AudioTrack.stopSafely() {
            try {
                this.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        // https://developer.android.com/reference/android/media/MediaCodec.html
        @IntDef(
            ERROR,
            UNINITIALIZED,
            CONFIGURED,
            EXECUTING,
            RELEASED
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class CodecState {
            companion object {
                const val ERROR = -1
                const val UNINITIALIZED = 0
                const val CONFIGURED = 1
                const val EXECUTING = 2
                const val RELEASED = 3
            }
        }
    }
}