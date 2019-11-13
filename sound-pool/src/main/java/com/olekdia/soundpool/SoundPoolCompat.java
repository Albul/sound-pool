package com.olekdia.soundpool;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.olekdia.androidcollection.IntKeySparseArray;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

import static com.olekdia.androidcommon.ConstantsKt.INVALID;
import static com.olekdia.androidcommon.ConstantsKt.NO_RESOURCE;

public class SoundPoolCompat {

    private static final int LOADING_COMPLETE = 1;

    private final IntKeySparseArray<SoundSample> mSamplePool;
    private EventHandler mEventHandler;
    private final Handler mLoadHandlerThread;
    private final ThreadPoolExecutor mPlayThreadPool;
    private final LoadSoundRun mLoadSoundRun;
    private final Queue<SoundSampleMetadata> mToLoadQueue;
    private final Queue<SoundSample> mToUnloadQueue;
    private final Context mContext;
    private OnLoadCompleteListener mOnLoadCompleteListener;

    /**
     * In bytes, inclusive
     */
    private final int mBufferSize;
    private final int mMaxSamples;

    private int mNextId;

    public SoundPoolCompat(
        final Context context,
        final int maxSamples,
        final int bufferSize
    ) {
        mContext = context;
        mMaxSamples = maxSamples;
        mBufferSize = bufferSize;

        mSamplePool = new IntKeySparseArray<>(maxSamples);
        final HandlerThread thread = new HandlerThread("LoadWorker", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLoadHandlerThread = new Handler(thread.getLooper());
        mPlayThreadPool = new ThreadPoolExecutor(
            4,
            8,
            2,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new SoundThreadFactory()
        );
        mLoadSoundRun = new LoadSoundRun();
        mToLoadQueue = new LinkedBlockingQueue<>();
        mToUnloadQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Release the SoundPool resources.
     *
     * Release all memory and native resources used by the SoundPool
     * object. The SoundPool can no longer be used and the reference
     * should be set to null.
     */
    public final void release() {
        mLoadHandlerThread.removeCallbacksAndMessages(null);
        mLoadHandlerThread.getLooper().quit();
        mToLoadQueue.clear();

        for (int i = 0, size = mSamplePool.size(); i < size; i++) {
            try {
                final SoundSample sample = mSamplePool.valueAt(i);
                if (sample != null) {
                    sample.stop();
                    sample.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mSamplePool.clear();
        mToUnloadQueue.clear();
        mPlayThreadPool.shutdown();
        mNextId = 0;
    }

    private int generateNextId() {
/*        final int id = mNextId;
        mNextId++;
        return id;*/
        return mNextId++;
    }

    /**
     * Load the sound from the specified path.
     *
     * @param path the path to the audio file

     * @return a sample ID. This value can be used to play or unload the sound.
     */
    public final int load(final String path) throws IOException {
        return load(path, false);
    }

    public final int load(
        final String path,
        final boolean isStatic
    ) throws IOException {
        return load(path, mBufferSize, isStatic);
    }

    public final int load(
        final String path,
        final int bufferSize,
        final boolean isStatic
    ) throws IOException {
        if (path == null || path.length() == 0) throw new IOException();
        if (mSamplePool.size() == mMaxSamples) return -1;

        return _load(0, path, bufferSize, isStatic);
    }

    /**
     * Load the sound from the specified APK resource.
     *
     * @param rawResId the resource ID

     * @return a sample ID. This value can be used to play or unload the sound.
     */
    public final int load(final int rawResId) {
        if (rawResId == NO_RESOURCE) return INVALID;
        return load(rawResId, false);
    }
    public final int load(
        final int rawResId,
        final boolean isStatic
    ) {
        if (rawResId == NO_RESOURCE) return INVALID;
        return load(rawResId, mBufferSize, isStatic);
    }
    public final int load(
        final int rawResId,
        final int bufferSize,
        final boolean isStatic
    ) {
        if (rawResId == NO_RESOURCE) return INVALID;
        if (mSamplePool.size() == mMaxSamples) return INVALID;

        return _load(rawResId, null, bufferSize, isStatic);
    }

    private int _load(
        final int rawResId,
        final String path,
        final int bufferSize,
        final boolean isStatic
    ) {
        final int sampleId = generateNextId();

        final SoundSample sample = new SoundSample(bufferSize, isStatic);
        mSamplePool.append(sampleId, sample);

        mToLoadQueue.add(new SoundSampleMetadata(sampleId, rawResId, path));

        mLoadHandlerThread.post(mLoadSoundRun);

        return sampleId;
    }

    /**
     * Unload a sound from a sample ID.
     *
     * Unloads the sound specified by the sampleId. This is the value
     * returned by the load() function. Returns true if the sound is
     * successfully unloaded, false if the sound was already unloaded.
     *
     * @param sampleId an id returned by the load() function
     * @return true if just unloaded, false if previously unloaded
     */
    public final boolean unload(final int sampleId) {
        if (sampleId < 0) return false;

        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample == null) {
            return false;
        } else {
            mSamplePool.remove(sampleId);
            mToUnloadQueue.add(sample);
            mLoadHandlerThread.post(mLoadSoundRun);
            return true;
        }
    }


    /**
     * Play a sound from a sample ID.
     *
     * Play the sound specified by the sampleId. This is the value
     * returned by the load() function. Returns the same sampleId.
     * This sampleId can be used to further control playback.
     * A loop value of -1 means loop forever,
     * a value of 0 means don't loop, other values indicate the
     * number of repeats, e.g. a value of 1 plays the audio twice.
     * The playback rate allows the application to vary the playback
     * rate (pitch) of the sound. A value of 1.0 means play back at
     * the original frequency. A value of 2.0 means play back twice
     * as fast, and a value of 0.5 means playback at half speed.
     *
     * @param sampleId a sampleId returned by the load() function
     * @param leftVolume left volume value (range = 0.0 to 1.0)
     * @param rightVolume right volume value (range = 0.0 to 1.0)
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return the same sampleId or -1 if there is no such sample
     */
    public final int play(
        final int sampleId,
        final float leftVolume,
        final float rightVolume,
        final int loop,
        final float rate
    ) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample == null) {
            return -1;
        } else {
            sample.play(leftVolume, rightVolume, loop, rate, mPlayThreadPool);
            return sampleId;
        }
    }

    /**
     * Similar to #play(), but for track that weren't preloaded.
     * All audio data will be loaded before playing.
     * When playback is done, all data will be released
     * @return return sampleId that can be used to stop this sample,
     * but it is prohibited to use this id to play, pause, unload
     */
    public final int playOnce(
        final int resId,
        final float leftVolume,
        final float rightVolume,
        final float rate
    ) {
        if (resId == NO_RESOURCE) return INVALID;
        final int sampleId = generateNextId();
        final SoundSample sample = new SoundSample(5000, false);
        mSamplePool.append(sampleId, sample);

        mPlayThreadPool.execute(() -> {
            try (final SoundSampleDescriptor descr = new SoundSampleDescriptor(mContext, new SoundSampleMetadata(sampleId, resId , null))) {
                if (sample.load(descr.getFileDescriptor(), descr.mFileOffset, descr.mFileSize)) {
                    if (!sample.isClosed()) sample.play(leftVolume, rightVolume, rate);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mCloseHandler.post(() -> {
                    sample.setClosed();
                    mSamplePool.remove(sampleId);
                });
            }
        });

        return sampleId;
    }

    /**
     * Similar to #play(), but for track that weren't preloaded.
     * All audio data will be loaded before playing.
     * When playback is done, all data will be released
     * @return return sampleId that can be used to stop this sample,
     * but it is prohibited to use this id to play, pause, unload
     */
    public final int playOnce(
        final String path,
        final float leftVolume,
        final float rightVolume,
        final float rate
    ) {
        final int sampleId = generateNextId();
        final SoundSample sample = new SoundSample(5000, false);
        mSamplePool.append(sampleId, sample);

        mPlayThreadPool.execute(() -> {
            try (final SoundSampleDescriptor descr = new SoundSampleDescriptor(mContext, new SoundSampleMetadata(sampleId, 0 , path))) {
                if (sample.load(descr.getFileDescriptor(), descr.mFileOffset, descr.mFileSize)) {
                    if (!sample.isClosed()) sample.play(leftVolume, rightVolume, rate);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mCloseHandler.post(() -> {
                    sample.setClosed();
                    mSamplePool.remove(sampleId);
                });
            }
        });
        return sampleId;
    }

    public void stop(final int sampleId) {
        if (sampleId < 0) return;

        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.stop();
    }

    /**
     * Pause a playback stream.
     *
     * Pause the stream specified by the sampleId. If the stream is
     * playing, it will be paused. If the stream is not playing
     * (e.g. is stopped or was previously paused), calling this
     * function will have no effect.
     *
     * @param sampleId an id returned by the load() function
     */
    public void pause(final int sampleId) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.pause();
    }

    /**
     * Resume a playback stream.
     *
     * Resume the stream specified by the sampleId. If the stream
     * is paused, this will resume playback. If the stream was not
     * previously paused, calling this function will have no effect.
     *
     * @param sampleId an id returned by the load() function
     */
    public void resume(final int sampleId) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.resume(mPlayThreadPool);
    }

    /**
     * Pause all active streams.
     *
     * Pause all streams that are currently playing. This function
     * iterates through all the active streams and pauses any that
     * are playing.
     */
    public final void autoPause() {
        for (int i = mSamplePool.size() - 1; i >= 0; i--) {
            mSamplePool.valueAt(i).pause();
        }
    }

    /**
     * Resume all previously active streams.
     *
     * Automatically resumes all streams that were paused.
     */
    public final void autoResume() {
        for (int i = mSamplePool.size() - 1; i >= 0; i--) {
            mSamplePool.valueAt(i).resume(mPlayThreadPool);
        }
    }

    /**
     * Set stream volume.
     *
     * Sets the volume on the stream specified by the sampleId.
     * This is the value returned by the play() function. The
     * value must be in the range of 0.0 to 1.0. If the stream does
     * not exist, it will have no effect.
     *
     * @param sampleId a sampleId returned by the load() function
     * @param leftVolume left volume value (range = 0.0 to 1.0)
     * @param rightVolume right volume value (range = 0.0 to 1.0)
     */
    public final void setVolume(
        final int sampleId,
        final float leftVolume,
        final float rightVolume
    ) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.setVolume(leftVolume, rightVolume);
    }

    public final void setVolume(final int sampleId, final float volume) {
        setVolume(sampleId, volume, volume);
    }

    /**
     * Change playback rate.
     *
     * The playback rate allows the application to vary the playback
     * rate (pitch) of the sound. A value of 1.0 means playback at
     * the original frequency. A value of 2.0 means playback twice
     * as fast, and a value of 0.5 means playback at half speed.
     * If the stream does not exist, it will have no effect.
     *
     * @param sampleId a sampleId returned by the load() function
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     */
    public final void setRate(final int sampleId, final float rate) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.setRate(rate);
    }

    /**
     * Set loop mode.
     *
     * Change the loop mode. A loop value of -1 means loop forever,
     * a value of 0 means don't loop, other values indicate the
     * number of repeats, e.g. a value of 1 plays the audio twice.
     * If the stream does not exist, it will have no effect.
     *
     * @param sampleId a sampleId returned by the load() function
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     */
    public final void setLoop(final int sampleId, final int loop) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.setLoop(loop);
    }

    /**
     * Sets the callback hook for the OnLoadCompleteListener.
     */
    public void setOnLoadCompleteListener(final OnLoadCompleteListener listener) {
        if (listener != null) {
            mEventHandler = new EventHandler(Looper.getMainLooper());
        } else {
            mEventHandler = null;
        }

        mOnLoadCompleteListener = listener;
    }

//--------------------------------------------------------------------------------------------------
//  Helper classes
//--------------------------------------------------------------------------------------------------

    public interface OnLoadCompleteListener {
        /**
         * Called when a sound has completed loading.
         *
         * @param soundPool SoundPool object from the load() method
         * @param sampleId the sample ID of the sound loaded.
         * @param isSuccess
         */
        public void onLoadComplete(
            SoundPoolCompat soundPool,
            int sampleId,
            boolean isSuccess,
            @Nullable String errorMsg
        );
    }


    private final class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == LOADING_COMPLETE && mOnLoadCompleteListener != null) {
                mOnLoadCompleteListener.onLoadComplete(SoundPoolCompat.this, msg.arg1, msg.arg2 == 1, (String) msg.obj);
            }
        }
    }


    private class LoadSoundRun implements Runnable {
        @Override
        public void run() {
            if (mToLoadQueue.isEmpty() && mToUnloadQueue.isEmpty()) return;

            final Thread currentThread = Thread.currentThread();
            if (currentThread.getPriority() != Thread.NORM_PRIORITY) {
                currentThread.setPriority(Thread.NORM_PRIORITY);
            }

            if (!tryUnload()) {
                tryLoad();
            }

            if (!currentThread.isInterrupted()) {
                if (!mToUnloadQueue.isEmpty()
                    || !mToLoadQueue.isEmpty()
                ) {
                    mLoadHandlerThread.post(mLoadSoundRun);
                } else {
                    currentThread.setPriority(Thread.MIN_PRIORITY);
                }
            }
        }

        private boolean tryUnload() {
            final SoundSample sample = mToUnloadQueue.poll();
            if (sample == null) {
                return false;
            } else {
                try {
                    sample.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        private boolean tryLoad() {
            final SoundSampleMetadata metadata = mToLoadQueue.poll();
            if (metadata == null) {
                return false;
            } else {
                final SoundSample sample = mSamplePool.get(metadata.mSampleId);
                boolean isSuccess = false;
                String errorMsg = null;

                if (sample != null) {
                    try (
                        final SoundSampleDescriptor descr = new SoundSampleDescriptor(mContext, metadata)
                    ) {
                        isSuccess = sample.load(
                            descr.getFileDescriptor(),
                            descr.mFileOffset,
                            descr.mFileSize
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMsg = e.getMessage();
                    }
                }

                if (mEventHandler != null
                    && sample != null
                    && !sample.isClosed()
                ) {
                    mEventHandler.sendMessage(
                        mEventHandler.obtainMessage(
                            LOADING_COMPLETE,
                            metadata.mSampleId, isSuccess ? 1 : 0, errorMsg
                        )
                    );
                }
                return true;
            }
        }
    }


    /**
     * The default thread factory.
     */
    private static class SoundThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup mGroup;
        private final AtomicInteger mThreadNumber = new AtomicInteger(1);
        private final String mNamePrefix;

        SoundThreadFactory() {
            final SecurityManager s = System.getSecurityManager();
            mGroup = (s != null)
                ? s.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
            mNamePrefix = "pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
        }

        public final Thread newThread(@NotNull final Runnable r) {
            final Thread t = new Thread(
                mGroup,
                r,
                mNamePrefix + mThreadNumber.getAndIncrement(),
                0
            );
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.MAX_PRIORITY) t.setPriority(Thread.MAX_PRIORITY);
            return t;
        }
    }
}