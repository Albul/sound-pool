package com.albul.supportsoundpool;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class SoundPoolCompat {

    private final SparseArray<SoundChannelCompat> mChannelPool;

    private final Handler mHandlerThread;
    private final LoadSoundRun mLoadSoundRun;
    private final Object mLoadQueueLock;
    private final Queue<SoundChannelMetadata> mToLoadQueue;
    /**
     * In bytes, inclusive
     */
    final int mBufferSize;

    public SoundPoolCompat(final int maxStreams, final int bufferSize) {
        mBufferSize = bufferSize;

        mChannelPool = new SparseArray<>(maxStreams);
        final HandlerThread thread = new HandlerThread("LoadWorker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mHandlerThread = new Handler(thread.getLooper());

        mLoadSoundRun = new LoadSoundRun();
        mLoadQueueLock = new Object();
        mToLoadQueue = new LinkedList<>();
    }

    /**
     * Load the sound from the specified path.
     *
     * @param path the path to the audio file

     * @return a sound ID. This value can be used to play or unload the sound.
     */
    public final int load(final String path) {
        int id = 0;
        try {
            final File file = new File(path);
            final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            if (pfd != null) id = _load(pfd, null, 0, file.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Load the sound from the specified APK resource.
     *
     * @param resId the resource ID

     * @return a sound ID. This value can be used to play or unload the sound.
     */
    public final int load(final Context context, final int resId) {
        int id = 0;

        final AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
        if (afd != null) id = _load(null, afd, afd.getStartOffset(), afd.getLength());
        return id;
    }

    private int _load(final ParcelFileDescriptor parcelDescr, final AssetFileDescriptor assetDescr,
                      final long offset, final long size) {
        final int id = mChannelPool.size();

        final SoundChannelCompat channel = new SoundChannelCompat(this);
        mChannelPool.append(id, channel);

        synchronized (mLoadQueueLock) {
            mToLoadQueue.add(new SoundChannelMetadata(id, parcelDescr, assetDescr, offset, size));
        }

        mHandlerThread.post(mLoadSoundRun);

        return id;
    }

    /**
     * Unload a sound from a sound ID.
     *
     * Unloads the sound specified by the soundID. This is the value
     * returned by the load() function. Returns true if the sound is
     * successfully unloaded, false if the sound was already unloaded.
     *
     * @param soundID a soundID returned by the load() function
     * @return true if just unloaded, false if previously unloaded
     */
    public final boolean unload(final int soundID) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel == null) {
            return false;
        } else {
            channel.stop();
            channel.dispose();
            mChannelPool.remove(soundID);
            return true;
        }
    }


    public final void play(final int soundID, final float leftVolume, final float rightVolume,
                           final int loop, final float rate) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.play(leftVolume, rightVolume, loop, rate);
    }

    public void stop(final int soundID) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.stop();
    }

    /**
     * Pause a playback stream.
     *
     * Pause the stream specified by the soundID. If the stream is
     * playing, it will be paused. If the stream is not playing
     * (e.g. is stopped or was previously paused), calling this
     * function will have no effect.
     *
     * @param soundID a streamID returned by the load() function
     */
    public void pause(final int soundID) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.pause();
    }

    /**
     * Resume a playback stream.
     *
     * Resume the stream specified by the soundID. If the stream
     * is paused, this will resume playback. If the stream was not
     * previously paused, calling this function will have no effect.
     *
     * @param soundID a streamID returned by the load() function
     */
    public void resume(final int soundID) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.resume();
    }


    /**
     * Set stream volume.
     *
     * Sets the volume on the stream specified by the soundID.
     * This is the value returned by the play() function. The
     * value must be in the range of 0.0 to 1.0. If the stream does
     * not exist, it will have no effect.
     *
     * @param soundID a soundID returned by the load() function
     * @param leftVolume left volume value (range = 0.0 to 1.0)
     * @param rightVolume right volume value (range = 0.0 to 1.0)
     */
    public final void setVolume(final int soundID, final float leftVolume, final float rightVolume) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.setVolume(leftVolume, rightVolume);
    }

    public final void setVolume(final int streamID, final float volume) {
        setVolume(streamID, volume, volume);
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
     * @param soundID a soundID returned by the load() function
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     */
    public final void setRate(final int soundID, final float rate) {
        final SoundChannelCompat channel = mChannelPool.get(soundID);
        if (channel != null) channel.setRate(rate);
    }

    private class LoadSoundRun implements Runnable {
        @Override
        public void run() {
            SoundChannelMetadata channelMetadata;
            boolean hasNextToLoad;
            synchronized (mLoadQueueLock) {
                channelMetadata = mToLoadQueue.poll();
                hasNextToLoad = mToLoadQueue.size() > 0;
            }

            final SoundChannelCompat channel = mChannelPool.get(channelMetadata.mChannelId);
            if (channel != null) {
                channel.load(channelMetadata.getFileDescriptor(), channelMetadata.mFileOffset, channelMetadata.mFileSize);
            }

            channelMetadata.close();

            if (hasNextToLoad) mHandlerThread.post(mLoadSoundRun);
        }
    }
}