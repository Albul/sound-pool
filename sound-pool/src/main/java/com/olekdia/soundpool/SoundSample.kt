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
import java.io.Closeable
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
    private val codecLock = Object()
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
    private var codecState: Int = CodecState.UNINITIALIZED
    @CodecState
    @Volatile
    private var playState: Int = PlayState.UNINITIALIZED
    @Volatile
    private var _isClosed: Boolean = true

    var isClosed: Boolean
        get() = _isClosed
        private set(value) {
            _isClosed = value
        }

    val isLoaded: Boolean
        get() = audioTrack != null

    val isPlaying: Boolean
        get() = playState == PlayState.PLAYING

    val isPaused: Boolean
        get() = playState == PlayState.PAUSED

    val isStopped: Boolean
        get() = playState == PlayState.STOPPED

    @WorkerThread
    fun load(descriptor: SoundSampleDescriptor): Boolean {
        _isClosed = false
        synchronized(codecLock) {
            isStatic = isStatic && descriptor.fileSize < MAX_STATIC_FILE_SIZE
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
                .also { codecState = CodecState.UNINITIALIZED }
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
                playState = PlayState.STOPPED
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
            var waitTimeout: Long = 1000 // microseconds to wait before get buffer (1 ms)
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

                                audioTrack?.let { track ->
                                    if (playState == PlayState.PLAYING) {
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
                    } else {
                        waitTimeout += 25
                        if (waitTimeout > 10_000) {
                            throw IllegalStateException()
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
                        && playState != PlayState.PAUSED
                    ) {
                        if (isStatic) {
                            // For static track, here track is not stopped and paused,
                            // that means that track is playing but hit EOF,
                            // so we should mark it as FullyLoaded and releaseCoded, we no longer need it
                            if (playState != PlayState.STOPPED) {
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
            playState = PlayState.UNINITIALIZED
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
        }  catch (e: IllegalArgumentException) {
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
                    CodecState.ERROR -> run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            it.reset()
                                .also { codecState = CodecState.UNINITIALIZED }
                        } else { // Recreate codec
                            codec?.release()
                                .also { codecState = CodecState.RELEASED }
                            createCodec()
                                .also { codecState = CodecState.UNINITIALIZED }
                        }
                        startCodec() // Recursive
                    }

                    CodecState.UNINITIALIZED -> {
                        // If we got IllegalStateException,
                        // that means that codec has moved to ERROR state silently
                        try {
                            it.configure(mediaFormat, null, null, 0)
                                .also { codecState = CodecState.CONFIGURED }
                        } catch (e: IllegalStateException) {
                            codecState = ERROR
                            e.printStackTrace()
                        }
                        startCodec() // Recursive
                    }

                    CodecState.CONFIGURED -> {
                        try {
                            it.start()
                                .also { codecState = CodecState.EXECUTING }
                            true
                        } catch (e: IllegalStateException) {
                            codecState = ERROR
                            e.printStackTrace()
                            false
                        }
                    }

                    CodecState.EXECUTING -> true

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
            if (codecState == CodecState.EXECUTING) {
                codecState = try {
                    it.stop()
                    CodecState.UNINITIALIZED
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
            .also { codecState = CodecState.RELEASED }
        codec = null

        extractor?.release()
        extractor = null

        mediaFormat = null
    }

    /**
     * Play is made asynchronously, so isPlaying will return true after some delay
     * @param leftVolume (range = 0.0 to 1.0)
     * @param rightVolume (range = 0.0 to 1.0)
     * @param repeat replay number of times (0 = play once, 1 play twice, -1 = play forever)
     * @return true if successfully started playing, false otherwise
     */
    @UiThread
    fun play(
        leftVolume: Float,
        rightVolume: Float,
        repeat: Int,
        rate: Float,
        threadPool: ThreadPoolExecutor
    ): Boolean =
        audioTrack?.let { track ->
            if (playState == PlayState.PLAYING) {
                false
            } else {
                setVolume(leftVolume, rightVolume)
                setRate(rate)
                setLoop(repeat)

                playState = PlayState.PLAYING
                track.play()

                threadPool.execute(playRun)
                true
            }
        } ?: false

    /**
     * Play is made asynchronously, so isPlaying will return true after some delay
     * @param leftVolume (range = 0.0 to 1.0)
     * @param rightVolume (range = 0.0 to 1.0)
     * @return true if successfully started playing, false otherwise
     */
    @WorkerThread
    fun play(
        leftVolume: Float,
        rightVolume: Float,
        rate: Float
    ): Boolean =
        audioTrack?.let { track ->
            if (playState == PlayState.PLAYING) {
                false
            } else {
                setVolume(leftVolume, rightVolume)
                setRate(rate)

                playState = PlayState.PLAYING
                track.play()

                playRun.run()
                true
            }
        } ?: false

    @UiThread
    fun stop(): Boolean =
        audioTrack?.let { track ->
            if (playState == PlayState.PLAYING
                || playState == PlayState.PAUSED
            ) {
                when (playState) {
                    PlayState.PLAYING -> {
                        playState = PlayState.PAUSED
                        track.pause()

                        stopCodecAndSeek0()
                    }

                    // If track was already paused we need seekTo(0),
                    // as it would not be called in loadNextSamples
                    PlayState.PAUSED ->
                        stopCodecAndSeek0()
                }

                synchronized(codecLock) {
                    if (playState == PlayState.PLAYING
                        || playState == PlayState.PAUSED
                    ) {
                        pausedPlaybackInBytes = 0
                        track.flush()

                        playState = PlayState.STOPPED
                        track.stop()
                    }
                }
                true
            } else {
                false
            }
        } ?: false

    /**
     * Pause the audio
     * @return true if successfully paused, false otherwise
     */
    @UiThread
    fun pause(): Boolean =
        audioTrack?.let { track ->
            if (playState == PlayState.PLAYING) {
                playState = PlayState.PAUSED
                track.pause()

                synchronized(codecLock) {
                    if (playState == PlayState.PAUSED) {
                        track.playbackHeadPosition.let { playbackInFrames ->
                            pausedPlaybackInBytes = if (playbackInFrames > 0) {
                                playbackInFrames * frameSizeInBytes
                            } else {
                                0
                            }
                        }

                        // Skip written bytes, so next resume will be in sync with pausedPlaybackInBytes
                        if (audioBuffer != null) {
                            track.flush()
                        }
                    }
                }
                true
            } else {
                false
            }
        } ?: false

    /**
     * Resume previously paused audio.
     * Resume is made asynchronously, so isPlaying will return true after some delay
     * @return true if successfully resumed, false otherwise
     */
    @UiThread
    fun resume(threadPool: ThreadPoolExecutor): Boolean =
        audioTrack?.let { track ->
            if (playState == PlayState.PAUSED) {
                playState = PlayState.PLAYING
                track.play()

                threadPool.execute(playRun)
                true
            } else {
                false
            }
        } ?: false

    /**
     * @param leftVolume left volume value (range = 0.0 to 1.0)
     * @param rightVolume right volume value (range = 0.0 to 1.0)
     * @return error code or success, see [SUCCESS], [ERROR_INVALID_OPERATION]
     */
    @AnyThread
    @Suppress("DEPRECATION")
    fun setVolume(
        leftVolume: Float,
        rightVolume: Float
    ): Int =
        if (currLeftVolume != leftVolume
            || currRightVolume != rightVolume
        ) {
            audioTrack
                ?.let {
                    currLeftVolume = leftVolume
                    currRightVolume = rightVolume
                    it.setStereoVolume(leftVolume, rightVolume)
                }
                ?: ERROR_INVALID_OPERATION
        } else {
            if (audioTrack == null) {
                ERROR_INVALID_OPERATION
            } else {
                SUCCESS
            }
        }

    /**
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return error code or success, see [SUCCESS], [ERROR_INVALID_OPERATION],
     * [ERROR_BAD_VALUE]
     */
    @AnyThread
    fun setRate(rate: Float): Int =
        if (currRate != rate) {
            audioTrack
                ?.let {
                    currRate = rate
                    it.setPlaybackRate((rate * it.sampleRate).toInt())
                } ?: ERROR_INVALID_OPERATION
        } else {
            if (audioTrack == null) {
                ERROR_INVALID_OPERATION
            } else {
                SUCCESS
            }
        }

    /**
     * @param repeat repeat number of times (0 = play once, 1 play twice, -1 = play forever)
     * @return error code or success, see [SUCCESS], [ERROR_INVALID_OPERATION],
     * [ERROR_BAD_VALUE]
     */
    @UiThread
    fun setLoop(repeat: Int): Int =
        when {
            repeat < -1 -> ERROR_BAD_VALUE
            audioTrack == null -> ERROR_INVALID_OPERATION

            else -> {
                toPlayCount = if (repeat == -1) Int.MAX_VALUE else 1 + repeat
                SUCCESS
            }
        }

//--------------------------------------------------------------------------------------------------
//  Runnable classes
//--------------------------------------------------------------------------------------------------

    inner class PlayRunnable : Runnable {
        override fun run() {
            synchronized(codecLock) {
                audioTrack?.let { track ->
                    while (toPlayCount > 0) {
                        if (_isClosed || playState != PlayState.PLAYING) return@let

                        audioBuffer?.let { buffer ->
                            if (isFullyLoaded || pausedPlaybackInBytes == 0) {
                                val offsetInBytes = pausedPlaybackInBytes
                                pausedPlaybackInBytes = 0
                                track.write(buffer, offsetInBytes, bufferSize)
                            }
                        }

                        if (_isClosed || playState != PlayState.PLAYING) return@let

                        if (!isFullyLoaded) {
                            if (!isStatic) audioBuffer = null
                            loadNextSamples(false)
                        }

                        if (_isClosed || playState != PlayState.PLAYING) return@let
                        toPlayCount--
                    }

                    playState = PlayState.STOPPED
                    track.stop()
                    pausedPlaybackInBytes = 0
                }
                codecLock.notifyAll()
            }
        }
    }

    companion object {
        const val MAX_STATIC_FILE_SIZE = 200 * 1024 // 200 Kb
        const val SMALL_FILE_SIZE = 20 * 1024 // 20 Kb

        // https://developer.android.com/reference/android/media/MediaCodec.html
        @IntDef(
            CodecState.ERROR,
            CodecState.UNINITIALIZED,
            CodecState.CONFIGURED,
            CodecState.EXECUTING,
            CodecState.RELEASED
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

        @IntDef(
            PlayState.UNINITIALIZED,
            PlayState.STOPPED,
            PlayState.PAUSED,
            PlayState.PLAYING
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class PlayState {
            companion object {
                const val UNINITIALIZED = -1
                const val STOPPED = 0
                const val PAUSED = 1
                const val PLAYING = 2
            }
        }
    }
}