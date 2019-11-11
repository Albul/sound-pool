package com.olekdia.soundpool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import static android.media.AudioTrack.PLAYSTATE_PAUSED;
import static android.media.AudioTrack.PLAYSTATE_PLAYING;
import static android.media.AudioTrack.PLAYSTATE_STOPPED;

public class SoundSample {

    public final static int MAX_STATIC_SIZE = 140 * 1024; // 140 Kb
    public final static int SMALL_FILE_SIZE = 20 * 1024; // 20 Kb

    private final int mBufferMaxSize;
    private final Handler mCloseHandler;
    private final Runnable mCloseRun = new CloseRunnable();
    private final Runnable mPlayRun = new PlayRunnable();
    private final Object mLockCodec = new Object();
    private AudioTrack mAudioTrack; // Null if initial data not loaded yet

    private byte[] mAudioBuffer;
    private int mBufferSize;

    private MediaExtractor mExtractor;
    private MediaCodec mCodec;
    private MediaFormat mMediaFormat;

    // true - if all sound data loaded to mAudioBuffer buffer, false - otherwise.
    private boolean mIsFullyLoaded = true;
    private boolean mIsStatic;
    private volatile int mToPlayCount = 1;
    private int mFrameSizeInBytes;
    private volatile int mPausedPlaybackInBytes;
    private float mCurrLeftVolume = -1F;
    private float mCurrRightVolume = -1F;
    private float mCurrRate = -1F;

    // Bug fix flag. On early androids api 16 - 18 the bug occurs when call codec.stop(),
    // and then reconfigure it and start it again before reaching eof
    private volatile boolean mIsCodecStarted;
    private volatile boolean mIsLockedByNonUiThread;
    private volatile boolean mIsClosedSet;

    public SoundSample(
        final Handler closeHandler,
        final int bufferMaxSize,
        final boolean isStatic
    ) {
        mCloseHandler = closeHandler;
        mBufferMaxSize = isStatic ? bufferMaxSize * 2 : bufferMaxSize;
        mIsStatic = isStatic;
    }

    @WorkerThread
    public boolean load(
        final FileDescriptor fd,
        final long fileOffset,
        final long fileSize
    ) {
        synchronized (mLockCodec) {
            mIsLockedByNonUiThread = true;
            mIsStatic = mIsStatic && fileSize < MAX_STATIC_SIZE;
            mAudioBuffer = mIsStatic ? new byte[(int) fileSize * 12] : new byte[mBufferMaxSize + 1024 * 1024];

            mExtractor = new MediaExtractor();
            try {
                mExtractor.setDataSource(fd, fileOffset, fileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mMediaFormat = null;
            String mime = null;
            int channels = 1, sampleRate = 44100;
            try {
                mMediaFormat = mExtractor.getTrackFormat(0);
                mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
                sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                channels = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mMediaFormat == null || mime == null || !mime.startsWith("audio/")) {
                return scheduleClose();
            }

            try {
                mCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mCodec == null) return scheduleClose();

            mExtractor.selectTrack(0);
            loadNextSamples(true);

            if (mIsClosedSet) return scheduleClose();

            final int channelConfiguration = channels == 1
                ? AudioFormat.CHANNEL_OUT_MONO
                : AudioFormat.CHANNEL_OUT_STEREO;
            final int minSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT
            );
            final int writeBuffSizeInBytes = mIsFullyLoaded && fileSize < SMALL_FILE_SIZE
                ? minSize : minSize * 2;
            mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT,
                writeBuffSizeInBytes,
                AudioTrack.MODE_STREAM
            );
            mFrameSizeInBytes = channels * 2; //  frameSizeInBytes = mChannelCount * AudioFormat.getBytesPerSample(mAudioFormat);

            if (BuildConfig.DEBUG) Log.d("SoundSample loaded:", " fileSize(Kb): " + fileSize / 1024
                + ", loadedSize(Kb): " + (mAudioBuffer == null ? "0" : mBufferSize / 1024)
                + ", writeBuffSize(Bytes): " + writeBuffSizeInBytes
                + ", mIsFullyLoaded: " + mIsFullyLoaded
                + ", isStatic: " + mIsStatic);
        }
        mIsLockedByNonUiThread = false;
        return true;
    }

    @WorkerThread
    private void loadNextSamples(final boolean isFromStart) {
        if (!mIsCodecStarted) {
            mCodec.configure(mMediaFormat, null, null, 0);
            mCodec.start();
            mIsCodecStarted = true;
        }

        final ByteBuffer[] codecInputBuffers = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();
        final MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
        final long waitTimeout = 1000; // 0
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        ByteBuffer outputBuffer, inputBuffer;
        int inputBufIndex, outputBufIndex;
        long sampleTime = 0;
        int sampleSize;

        while (!sawOutputEOS) {
            if (mIsClosedSet) {
                stopCodec();
                return;
            }

            if (!sawInputEOS) {
                inputBufIndex = mCodec.dequeueInputBuffer(waitTimeout);
                if (inputBufIndex >= 0) {
                    inputBuffer = codecInputBuffers[inputBufIndex];
                    sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                    if (sampleSize <= 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        sampleTime = mExtractor.getSampleTime();
                    }

                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, sampleTime,
                                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) mExtractor.advance();
                }
            }

            outputBufIndex = mCodec.dequeueOutputBuffer(bufInfo, waitTimeout);
            if (outputBufIndex >= 0) {
                outputBuffer = codecOutputBuffers[outputBufIndex];

                if (isFromStart) {
                    checkBufferSize(bufInfo.size);
                    outputBuffer.get(mAudioBuffer, mBufferSize, bufInfo.size);
                    mBufferSize += bufInfo.size;
                    if (mBufferSize > mBufferMaxSize) {
                        sawOutputEOS = true;
                        mIsFullyLoaded = false;
                    }
                } else {
                    if (mAudioTrack.getPlayState() == PLAYSTATE_PLAYING && !mIsClosedSet) {
                        final byte[] chunk = new byte[bufInfo.size];
                        outputBuffer.get(chunk);
                        mAudioTrack.write(chunk, 0, chunk.length);
                        if (mIsStatic) {
                            checkBufferSize(chunk.length);
                            System.arraycopy(chunk, 0, mAudioBuffer, mBufferSize, chunk.length);
                            mBufferSize += chunk.length;
                        }
                    } else {
                        sawOutputEOS = true;
                    }
                }

                outputBuffer.clear();
                mCodec.releaseOutputBuffer(outputBufIndex, false);

                if ((bufInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = mCodec.getOutputBuffers();
            }
        }

        if (mIsFullyLoaded) {
            stopCodec();
            releaseCodec();
        } else {
            if (!isFromStart
                && mAudioTrack != null
                && mAudioTrack.getPlayState() != PLAYSTATE_PAUSED) {

                if (mIsStatic) {
                    if (mAudioTrack.getPlayState() != PLAYSTATE_STOPPED) {
                        mIsFullyLoaded = true;
                        stopCodec();
                        releaseCodec();
                    }
                } else {
                    stopCodec();
                    mExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
        }
    }

    private void checkBufferSize(final int extraSize) {
        if (mBufferSize + extraSize > mAudioBuffer.length) {
            mAudioBuffer = Arrays.copyOf(mAudioBuffer, mAudioBuffer.length + mAudioBuffer.length / 8);
        }
    }

    public final boolean isClosedSet() {
        return mIsClosedSet;
    }

    @UiThread
    public final void setClosed() {
        stop();
        mIsClosedSet = true;

        mCloseHandler.post(mCloseRun);
    }

    @WorkerThread
    private boolean scheduleClose() {
        mIsLockedByNonUiThread = false;
        mCloseHandler.post(mCloseRun);
        return false;
    }

    private void stopCodec() {
        mCodec.stop();
        mIsCodecStarted = false;
    }

    private void releaseCodec() {
        if (mCodec != null) {
            mCodec.release();
            mCodec = null;
        }
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
        mMediaFormat = null;
    }

    @UiThread
    public final void play(
        final float leftVolume,
        final float rightVolume,
        final int loop,
        final float rate,
        final ThreadPoolExecutor threadPool
    ) {
        if (mAudioTrack == null || mAudioTrack.getPlayState() == PLAYSTATE_PLAYING) return;

        setVolume(leftVolume, rightVolume);
        setRate(rate);
        setLoop(loop);

        mIsLockedByNonUiThread = true;
        threadPool.execute(mPlayRun);
    }

    @WorkerThread
    public final void play(
        final float leftVolume,
        final float rightVolume,
        final float rate
    ) {
        if (mAudioTrack == null || mAudioTrack.getPlayState() == PLAYSTATE_PLAYING) return;

        mIsLockedByNonUiThread = true;
        setVolume(leftVolume, rightVolume);
        setRate(rate);
        mPlayRun.run();
    }

    @UiThread
    public final void stop() {
        if (mAudioTrack == null) return;
        final int state = mAudioTrack.getPlayState();

        if (state != PLAYSTATE_STOPPED) {
            if (state == PLAYSTATE_PLAYING) mAudioTrack.pause();
            mPausedPlaybackInBytes = 0;
            mAudioTrack.flush();
            mAudioTrack.stop();
        }
    }

    @UiThread
    public final void pause() {
        if (mAudioTrack == null || mAudioTrack.getPlayState() != PLAYSTATE_PLAYING) return;

        mAudioTrack.pause();
        final int playbackInFrames = mAudioTrack.getPlaybackHeadPosition();
        mPausedPlaybackInBytes = playbackInFrames > 0 ? playbackInFrames * mFrameSizeInBytes : 0;
        // Skip written bytes, so next resume will be in sync with mPausedPlaybackInBytes
        if (mAudioBuffer != null) mAudioTrack.flush();
    }

    @UiThread
    public final void resume(final ThreadPoolExecutor threadPool) {
        if (mAudioTrack == null || mAudioTrack.getPlayState() != PLAYSTATE_PAUSED) return;

        mIsLockedByNonUiThread = true;
        threadPool.execute(mPlayRun);
    }

    @AnyThread
    public final void setVolume(
        final float leftVolume,
        final float rightVolume
    ) {
        if (mAudioTrack != null
            && (mCurrLeftVolume != leftVolume || mCurrRightVolume != rightVolume)
        ) {
            mAudioTrack.setStereoVolume(leftVolume, rightVolume);
            mCurrLeftVolume = leftVolume;
            mCurrRightVolume = rightVolume;
        }
    }

    @AnyThread
    public final void setRate(final float rate) {
        if (mAudioTrack != null
            && mCurrRate != rate
        ) {
            mAudioTrack.setPlaybackRate((int) (rate * mAudioTrack.getSampleRate()));
            mCurrRate = rate;
        }
    }

    @UiThread
    public final void setLoop(final int loop) {
        mToPlayCount = loop == -1 ? Integer.MAX_VALUE : 1 + loop;
    }

//--------------------------------------------------------------------------------------------------
//  Runnable classes
//--------------------------------------------------------------------------------------------------

    public final class PlayRunnable implements Runnable {
        @Override
        public void run() {
            try {
                synchronized (mLockCodec) {
                    while (mToPlayCount > 0) {
                        if (mIsClosedSet) return;

                        mAudioTrack.play();

                        if (mAudioBuffer != null && (mIsFullyLoaded || mPausedPlaybackInBytes == 0)) {
                            final int offsetInBytes = mPausedPlaybackInBytes;
                            mPausedPlaybackInBytes = 0;
                            mAudioTrack.write(mAudioBuffer, offsetInBytes, mBufferSize);
                        }

                        if (mIsClosedSet || mAudioTrack.getPlayState() != PLAYSTATE_PLAYING) return;

                        if (!mIsFullyLoaded) {
                            if (!mIsStatic) mAudioBuffer = null;
                            loadNextSamples(false);
                        }

                        if (mIsClosedSet || mAudioTrack.getPlayState() != PLAYSTATE_PLAYING) return;
                        mToPlayCount--;
                    }

                    mAudioTrack.stop();
                    mPausedPlaybackInBytes = 0;
                }
            } finally {
                mIsLockedByNonUiThread = false;
            }
        }
    }

    public final class CloseRunnable implements Runnable {

        private int mDelay = 0;

        @Override
        @UiThread
        public void run() {
            if (mIsLockedByNonUiThread) {
                mCloseHandler.postDelayed(this, mDelay += 10);
            } else {
                synchronized (mLockCodec) {
                    releaseCodec();
                    mAudioBuffer = null;
                    if (mAudioTrack != null) {
                        if (mAudioTrack.getPlayState() != PLAYSTATE_STOPPED) stop();
                        mAudioTrack.release();
                        mAudioTrack = null;
                    }
                }
            }
        }
    }
}