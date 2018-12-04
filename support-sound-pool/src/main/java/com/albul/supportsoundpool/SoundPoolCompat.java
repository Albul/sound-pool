package com.albul.supportsoundpool;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class SoundPoolCompat {

    private static final int SOUND_LOADED = 1;

    private final SparseArray<SoundSample> mSamplePool;
    private EventHandler mEventHandler;
    private final Handler mLoadHandlerThread;
    private final LoadSoundRun mLoadSoundRun;
    private final Object mLoadQueueLock;
    private final Queue<SoundSampleMetadata> mToLoadQueue;
    private OnLoadCompleteListener mOnLoadCompleteListener;

    /**
     * In bytes, inclusive
     */
    private final int mBufferSize;
    private final int mMaxSamples;

    private int mNextId;

    public SoundPoolCompat(final int maxSamples, final int bufferSize) {
        mMaxSamples = maxSamples;
        mBufferSize = bufferSize;

        mSamplePool = new SparseArray<>(maxSamples);
        final HandlerThread thread = new HandlerThread("LoadWorker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLoadHandlerThread = new Handler(thread.getLooper());

        mLoadSoundRun = new LoadSoundRun();
        mLoadQueueLock = new Object();
        mToLoadQueue = new LinkedList<>();
    }

    /**
     * Release the SoundPool resources.
     *
     * Release all memory and native resources used by the SoundPool
     * object. The SoundPool can no longer be used and the reference
     * should be set to null.
     */
    public final void release() {
        for (int i = 0, size = mSamplePool.size(); i < size; i++) {
            mSamplePool.valueAt(i).dispose();
        }
        mSamplePool.clear();
        mLoadHandlerThread.removeCallbacksAndMessages(null);
        mLoadHandlerThread.getLooper().quit();
        synchronized (mLoadQueueLock) {
            while (mToLoadQueue.size() > 0) mToLoadQueue.poll().close();
        }
        mNextId = 0;
    }

    /**
     * Load the sound from the specified path.
     *
     * @param path the path to the audio file

     * @return a sample ID. This value can be used to play or unload the sound.
     */
    public final int load(final String path) {
        return load(path, mBufferSize);
    }

    public final int load(final String path, final int bufferSize) {
        int id = -1;
        if (mSamplePool.size() == mMaxSamples) return id;

        try {
            final File file = new File(path);
            final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            if (pfd != null) id = _load(pfd, null, 0, file.length(), bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Load the sound from the specified APK resource.
     *
     * @param resId the resource ID

     * @return a sample ID. This value can be used to play or unload the sound.
     */
    public final int load(final Context context, final int resId) {
        return load(context, resId, mBufferSize);
    }

    public final int load(final Context context, final int resId, final int bufferSize) {
        int id = -1;
        if (mSamplePool.size() == mMaxSamples) return id;

        final AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
        if (afd != null) id = _load(null, afd, afd.getStartOffset(), afd.getLength(), bufferSize);
        return id;
    }

    private int _load(final ParcelFileDescriptor parcelDescr, final AssetFileDescriptor assetDescr,
                      final long fileOffset, final long fileSize, final int bufferSize) {
        final int id = mNextId;
        mNextId++;

        final SoundSample sample = new SoundSample(bufferSize);
        mSamplePool.append(id, sample);

        synchronized (mLoadQueueLock) {
            mToLoadQueue.add(new SoundSampleMetadata(id, parcelDescr, assetDescr, fileOffset, fileSize));
        }

        mLoadHandlerThread.post(mLoadSoundRun);

        return id;
    }

    /**
     * Unload a sound from a sound ID.
     *
     * Unloads the sound specified by the sampleId. This is the value
     * returned by the load() function. Returns true if the sound is
     * successfully unloaded, false if the sound was already unloaded.
     *
     * @param sampleId an id returned by the load() function
     * @return true if just unloaded, false if previously unloaded
     */
    public final boolean unload(final int sampleId) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample == null) {
            return false;
        } else {
            sample.dispose();
            mSamplePool.remove(sampleId);
            return true;
        }
    }


    public final void play(final int sampleId, final float leftVolume, final float rightVolume,
                           final int loop, final float rate) {
        final SoundSample sample = mSamplePool.get(sampleId);
        if (sample != null) sample.play(leftVolume, rightVolume, loop, rate);
    }

    public void stop(final int sampleId) {
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
        if (sample != null) sample.resume();
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
            mSamplePool.valueAt(i).resume();
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
    public final void setVolume(final int sampleId, final float leftVolume, final float rightVolume) {
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
         * @param status the status of the load operation (0 = success)
         */
        public void onLoadComplete(SoundPoolCompat soundPool, int sampleId, int status);
    }


    private final class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SOUND_LOADED:
                    if (mOnLoadCompleteListener != null) {
                        mOnLoadCompleteListener.onLoadComplete(SoundPoolCompat.this, msg.arg1, msg.arg2);
                    }
                    break;
            }
        }
    }


    private class LoadSoundRun implements Runnable {
        @Override
        public void run() {
            if (mToLoadQueue.isEmpty()) return;

            SoundSampleMetadata metadata;
            synchronized (mLoadQueueLock) {
                metadata = mToLoadQueue.poll();
            }

            final SoundSample sample = mSamplePool.get(metadata.mSampleId);
            if (sample != null) {
                sample.load(metadata.getFileDescriptor(), metadata.mFileOffset, metadata.mFileSize);
            }

            metadata.close();

            if (mEventHandler != null) {
                mEventHandler.sendMessage(mEventHandler.obtainMessage(SOUND_LOADED, metadata.mSampleId, sample == null ? 1 : 0));
            }

            synchronized (mLoadQueueLock) {
                if (mToLoadQueue.size() > 0 && !Thread.currentThread().isInterrupted()) mLoadHandlerThread.post(mLoadSoundRun);
            }
        }
    }
}