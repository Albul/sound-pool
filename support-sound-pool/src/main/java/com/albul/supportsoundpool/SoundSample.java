package com.albul.supportsoundpool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.media.AudioTrack.PLAYSTATE_PAUSED;
import static android.media.AudioTrack.PLAYSTATE_PLAYING;
import static android.media.AudioTrack.PLAYSTATE_STOPPED;
import static java.lang.Thread.MAX_PRIORITY;

public class SoundSample {

    private final SoundPoolCompat mSoundPool;
    private AudioTrack mAudioTrack;

    private MediaExtractor mExtractor;
    private MediaCodec mCodec;
    private MediaFormat mMediaFormat;
    private byte[] mLoadedAudio;
    private Thread mAudioThread;
    private final Object mThreadLock = new Object();

    /**
     * Static if all sound data loaded to start buffer, false - otherwise.
     */
    private boolean mIsStatic = true;
    private int mLoopCount;

    public SoundSample(final SoundPoolCompat soundPool) {
        mSoundPool = soundPool;
    }

    public boolean load(final FileDescriptor fd, final long fileOffset, final long fileSize) {
        synchronized (mThreadLock) {
            mExtractor = new MediaExtractor();
            try {
                mExtractor.setDataSource(fd, fileOffset, fileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mMediaFormat = null;
            String mime = null;
            int channels = 0, sampleRate = 44100;
            try {
                mMediaFormat = mExtractor.getTrackFormat(0);
                mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
                sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                channels = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mMediaFormat == null || !mime.startsWith("audio/")) return false;

            try {
                mCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mCodec == null) return false;

            final int channelConfiguration = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
            final int minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration,
                                         AudioFormat.ENCODING_PCM_16BIT, minSize * 2 /*todo*/, AudioTrack.MODE_STREAM);
            mExtractor.selectTrack(0);

            loadNextSamples(true);
        }

        Log.d("fileSize", fileSize + "");
        Log.d("mLoadedAudio.length", mLoadedAudio.length + "");
        Log.d("mIsStatic", mIsStatic +"");
        return true;
    }

    private void loadNextSamples(final boolean isFromStart) {
        synchronized (mThreadLock) {
            if (isFromStart) mLoadedAudio = new byte[0];
            mCodec.configure(mMediaFormat, null, null, 0);
            mCodec.start();

            final ByteBuffer[] codecInputBuffers = mCodec.getInputBuffers();
            ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();
            final MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
            final long kTimeOutUs = 1000; // todo
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            ByteBuffer outputBuffer, inputBuffer;
            int inputBufIndex, outputBufIndex;
            long sampleTime = 0;
            int sampleSize;

            while (!sawOutputEOS) {
                if (isPaused()) continue;

                if (!sawInputEOS) {
                    inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
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

                outputBufIndex = mCodec.dequeueOutputBuffer(bufInfo, kTimeOutUs);

                if (outputBufIndex >= 0) {
                    outputBuffer = codecOutputBuffers[outputBufIndex];

                    if (isFromStart) {
                        final int prevSize = mLoadedAudio.length;
                        mLoadedAudio = Arrays.copyOf(mLoadedAudio, mLoadedAudio.length + bufInfo.size);
                        outputBuffer.get(mLoadedAudio, prevSize, bufInfo.size);
                        if (mLoadedAudio.length > mSoundPool.mBufferSize) {
                            sawOutputEOS = true;
                            mIsStatic = false;
                        }
                    } else {
                        if (isPlaying()) {
                            final byte[] chunk = new byte[bufInfo.size];
                            outputBuffer.get(chunk);
                            mAudioTrack.write(chunk, 0, chunk.length);
                        } else {
                            sawOutputEOS = isStopped();
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

            mCodec.stop();
            if (mIsStatic) {
                releaseCodec();
            } else {
                if (!isFromStart) mExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
        }
    }

    private void releaseCodec() {
        synchronized (mThreadLock) {
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
    }

    public final void dispose() {
        stop();
        synchronized (mThreadLock) {
            releaseCodec();
            mLoadedAudio = null;
            if (mAudioTrack != null) {
                mAudioTrack.flush();
                mAudioTrack.release();
                mAudioTrack = null;
            }
            mAudioThread = null;
        }
    }


    public final void play(final float leftVolume, final float rightVolume, final int loop, final float rate) {
        if (mAudioTrack == null) return;

        if (isPlaying()) mAudioTrack.pause();
        mAudioTrack.flush();
        mAudioTrack.stop();

        setVolume(leftVolume, rightVolume);
        setRate(rate);
        setLoop(loop);

        mAudioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mThreadLock) {
                    mAudioTrack.play();

                    if (mLoadedAudio != null) mAudioTrack.write(mLoadedAudio, 0, mLoadedAudio.length);

                    if (!mIsStatic && !isStopped()) {
                        mLoadedAudio = null;
                        loadNextSamples(false);
                    }

                    if (!isStopped()) {
                        mLoopCount--;
                        if (mLoopCount != 0) run();
                    }

                    if (Thread.currentThread() == mAudioThread) mAudioThread = null;
                    mThreadLock.notify();
                }
            }
        });

        mAudioThread.setPriority(MAX_PRIORITY);
        mAudioThread.start();
    }

    public final void stop() {
        if (mAudioTrack != null && !isStopped()) {

            if (isPlaying()) mAudioTrack.pause();
            mAudioTrack.flush();
            mAudioTrack.stop();
            mAudioThread = null;
        }
    }

    public final void pause() {
        if (mAudioTrack != null && isPlaying()) mAudioTrack.pause();
    }

    public final void resume() {
        if (mAudioTrack != null && isPaused()) mAudioTrack.play();
    }

    public final void setVolume(final float leftVolume, final float rightVolume) {
        if (mAudioTrack != null) mAudioTrack.setStereoVolume(leftVolume, rightVolume);
    }

    public final void setRate(final float rate) {
        if (mAudioTrack != null) mAudioTrack.setPlaybackRate((int) (rate * mAudioTrack.getSampleRate()));
    }

    public final void setLoop(final int loop) {
        mLoopCount = loop + 1;
    }

    public final boolean isPlaying() {
        return mAudioTrack.getPlayState() == PLAYSTATE_PLAYING;
    }

    public final boolean isPaused() {
        return mAudioTrack.getPlayState() == PLAYSTATE_PAUSED;
    }

    public final boolean isStopped() {
        return mAudioTrack.getPlayState() == PLAYSTATE_STOPPED;
    }
}