package com.olekdia.soundpool

import android.content.Context
import android.os.*
import com.olekdia.androidcollection.IntKeySparseArray
import com.olekdia.androidcommon.INVALID
import com.olekdia.androidcommon.NO_RESOURCE
import java.io.IOException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SoundPoolCompat(
    private val context: Context,
    private val maxSamples: Int,
    /**
     * In bytes, inclusive
     */
    var bufferSize: Int
) {
    private val samplePool: IntKeySparseArray<SoundSample> = IntKeySparseArray(maxSamples)
    private var eventHandler: EventHandler? = null
    private val loadHandlerThread: Handler
    private val playThreadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        4,
        8,
        2,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        SoundThreadFactory()
    )
    private val loadSoundRun: LoadSoundRun = LoadSoundRun()
    private val toLoadQueue: Queue<SoundSampleMetadata> = LinkedBlockingQueue()
    private val toUnloadQueue: Queue<SoundSample> = LinkedBlockingQueue()
    private var onLoadCompleteListener: OnLoadCompleteListener? = null

    private var nextId: Int = 0

    init {
        val thread = HandlerThread("LoadWorker", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        loadHandlerThread = Handler(thread.looper)
    }

    /**
     * Release the SoundPool resources.
     *
     * Release all memory and native resources used by the SoundPool
     * object. The SoundPool can no longer be used and the reference
     * should be set to null.
     */
    fun release() {
        loadHandlerThread.removeCallbacksAndMessages(null)
        loadHandlerThread.looper.quit()
        toLoadQueue.clear()

        for (i in 0 until samplePool.size()) {
            samplePool.valueAt(i)?.let {
                it.stop()
                it.close()
            }
        }
        samplePool.clear()
        toUnloadQueue.clear()
        playThreadPool.shutdown()
        nextId = 0
    }

    private fun generateNextId(): Int = nextId++

    @Throws(IOException::class)
    @JvmOverloads
    fun load(
        path: String,
        isStatic: Boolean = false
    ): Int = load(path, bufferSize, isStatic)

    /**
     * Load the sound from the specified path.
     *
     * @param path the path to the audio file
     *
     * @return a sample ID. This value can be used to play or unload the sound.
     */
    @Throws(IOException::class)
    fun load(
        path: String?,
        bufferSize: Int,
        isStatic: Boolean
    ): Int {
        if (path.isNullOrEmpty()) throw IOException()
        return if (samplePool.size() == maxSamples) INVALID else _load(0, path, bufferSize, isStatic)
    }

    /**
     * Load the sound from the specified APK resource.
     *
     * @param rawResId the resource ID
     *
     * @return a sample ID. This value can be used to play or unload the sound.
     */
    fun load(rawResId: Int): Int = if (rawResId == NO_RESOURCE) INVALID else load(rawResId, false)

    fun load(
        rawResId: Int,
        isStatic: Boolean = false
    ): Int = if (rawResId == NO_RESOURCE) INVALID else load(rawResId, bufferSize, isStatic)

    fun load(
        rawResId: Int,
        bufferSize: Int,
        isStatic: Boolean
    ): Int =
        if (rawResId == NO_RESOURCE
            || samplePool.size() == maxSamples
        ) {
            INVALID
        } else {
            _load(
                rawResId,
                null,
                bufferSize,
                isStatic
            )
        }

    private fun _load(
        rawResId: Int,
        path: String?,
        bufferSize: Int,
        isStatic: Boolean
    ): Int = generateNextId()
        .also { sampleId ->
            samplePool.append(sampleId, SoundSample(sampleId, bufferSize, isStatic))

            toLoadQueue.add(SoundSampleMetadata(sampleId, rawResId, path))

            loadHandlerThread.post(loadSoundRun)
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
    fun unload(sampleId: Int): Boolean = samplePool.get(sampleId)
        ?.let { sample ->
            samplePool.remove(sampleId)
            sample.stop()
            toUnloadQueue.add(sample)
            loadHandlerThread.post(loadSoundRun)
            true
        }
        ?: false


    fun isPlaying(sampleId: Int): Boolean = samplePool.get(sampleId)?.isPlaying() ?: false

    fun isPaused(sampleId: Int): Boolean = samplePool.get(sampleId)?.isPaused() ?: false

    fun isStopped(sampleId: Int): Boolean = samplePool.get(sampleId)?.isStopped() ?: true

    fun isPlaying(): Boolean {
        var isPlaying = false
        for (i in 0 until samplePool.size()) {
            isPlaying = isPlaying or isPlaying(samplePool.keyAt(i))
        }
        return isPlaying
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
    fun play(
        sampleId: Int,
        leftVolume: Float,
        rightVolume: Float,
        loop: Int,
        rate: Float
    ): Int = samplePool.get(sampleId)
        ?.let { sample ->
            sample.play(leftVolume, rightVolume, loop, rate, playThreadPool)
            sampleId
        }
        ?: INVALID

    /**
     * Similar to #play(), but for track that weren't preloaded.
     * All audio data will be loaded before playing.
     * When playback is done, all data will be released
     * @return return sampleId that can be used to stop this sample,
     * but it is prohibited to use this id to play, pause, unload
     */
    fun playOnce(
        resId: Int,
        leftVolume: Float,
        rightVolume: Float,
        rate: Float
    ): Int = if (resId == NO_RESOURCE) {
        INVALID
    } else {
        generateNextId().also { sampleId ->
            val sample = SoundSample(sampleId, 5000, false)
            samplePool.append(sampleId, sample)

            playOnce(
                sample,
                SoundSampleMetadata(sampleId, resId, null),
                leftVolume,
                rightVolume,
                rate
            )
        }
    }



    /**
     * Similar to #play(), but for track that weren't preloaded.
     * All audio data will be loaded before playing.
     * When playback is done, all data will be released
     * @return return sampleId that can be used to stop this sample,
     * but it is prohibited to use this id to play, pause, unload
     */
    fun playOnce(
        path: String?,
        leftVolume: Float,
        rightVolume: Float,
        rate: Float
    ): Int = if (path.isNullOrEmpty()) {
        INVALID
    } else {
        generateNextId().also { sampleId ->
            val sample = SoundSample(sampleId, 5000, false)
            samplePool.append(sampleId, sample)

            playOnce(
                sample,
                SoundSampleMetadata(sampleId, NO_RESOURCE, path),
                leftVolume,
                rightVolume,
                rate
            )
        }
    }

    private fun playOnce(
        sample: SoundSample,
        metadata: SoundSampleMetadata,
        leftVolume: Float,
        rightVolume: Float,
        rate: Float
    ) {
        playThreadPool.execute {
            try {
                SoundSampleDescriptor(context, metadata)
                    .use { d ->
                        if (sample.load(d.fileDescriptor, d.fileOffset, d.fileSize)) {
                            if (!sample.isClosed) {
                                sample.play(leftVolume, rightVolume, rate)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                unload(sample.id)
            }
        }
    }

    fun stop(sampleId: Int) {
        samplePool.get(sampleId)?.stop()
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
    fun pause(sampleId: Int) {
        samplePool.get(sampleId)?.pause()
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
    fun resume(sampleId: Int) {
        samplePool.get(sampleId)?.resume(playThreadPool)
    }

    /**
     * Pause all active streams.
     *
     * Pause all streams that are currently playing. This function
     * iterates through all the active streams and pauses any that
     * are playing.
     */
    fun autoPause() {
        for (i in 0 until samplePool.size()) {
            samplePool.valueAt(i)?.pause()
        }
    }

    /**
     * Resume all previously active streams.
     *
     * Automatically resumes all streams that were paused.
     */
    fun autoResume() {
        for (i in 0 until samplePool.size()) {
            samplePool.valueAt(i)?.resume(playThreadPool)
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
    fun setVolume(
        sampleId: Int,
        leftVolume: Float,
        rightVolume: Float
    ) {
        samplePool.get(sampleId)?.setVolume(leftVolume, rightVolume)
    }

    fun setVolume(sampleId: Int, volume: Float) {
        setVolume(sampleId, volume, volume)
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
    fun setRate(sampleId: Int, rate: Float) {
        samplePool.get(sampleId)?.setRate(rate)
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
    fun setLoop(sampleId: Int, loop: Int) {
        samplePool.get(sampleId)?.setLoop(loop)
    }

    /**
     * Sets the callback hook for the OnLoadCompleteListener.
     */
    fun setOnLoadCompleteListener(listener: OnLoadCompleteListener?) {
        eventHandler = if (listener != null) {
            EventHandler(Looper.getMainLooper())
        } else {
            null
        }

        onLoadCompleteListener = listener
    }

//--------------------------------------------------------------------------------------------------
//  Helper classes
//--------------------------------------------------------------------------------------------------

    interface OnLoadCompleteListener {
        /**
         * Called when a sound has completed loading.
         *
         * @param soundPool SoundPool object from the load() method
         * @param sampleId the sample ID of the sound loaded.
         * @param isSuccess
         */
        fun onLoadComplete(
            soundPool: SoundPoolCompat,
            sampleId: Int,
            isSuccess: Boolean,
            errorMsg: String?
        )
    }


    private inner class EventHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (msg.what == LOADING_COMPLETE) {
                onLoadCompleteListener?.onLoadComplete(
                    this@SoundPoolCompat,
                    msg.arg1,
                    msg.arg2 == 1,
                    msg.obj as? String
                )
            }
        }
    }


    private inner class LoadSoundRun : Runnable {
        override fun run() {
            if (toLoadQueue.isEmpty() && toUnloadQueue.isEmpty()) return

            val currentThread = Thread.currentThread()
            if (currentThread.priority != Thread.NORM_PRIORITY) {
                currentThread.priority = Thread.NORM_PRIORITY
            }

            if (!tryUnload()) tryLoad()

            if (!currentThread.isInterrupted) {
                if (!toUnloadQueue.isEmpty() || !toLoadQueue.isEmpty()) {
                    loadHandlerThread.post(loadSoundRun)
                } else {
                    currentThread.priority = Thread.MIN_PRIORITY
                }
            }
        }

        private fun tryUnload(): Boolean = toUnloadQueue.poll()
            ?.let { sample ->
                sample.close()
                true
            }
            ?: false

        private fun tryLoad(): Boolean = toLoadQueue.poll()
            ?.let { metadata ->
                val sample = samplePool.get(metadata.sampleId)
                var isSuccess = false
                var errorMsg: String? = null

                if (sample != null) {
                    try {
                        SoundSampleDescriptor(context, metadata)
                            .use { descr ->
                                isSuccess = sample.load(
                                    descr.fileDescriptor,
                                    descr.fileOffset,
                                    descr.fileSize
                                )
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorMsg = e.message
                        unload(sample.id)
                    }
                }

                if (eventHandler != null
                    && sample != null
                    && !sample.isClosed
                ) {
                    eventHandler?.run {
                        sendMessage(
                            obtainMessage(
                                LOADING_COMPLETE,
                                metadata.sampleId,
                                if (isSuccess) 1 else 0,
                                errorMsg
                            )
                        )
                    }
                }
                true
            }
            ?: false
    }


    /**
     * The default thread factory.
     */
    private class SoundThreadFactory internal constructor() : ThreadFactory {
        private val group: ThreadGroup? = System.getSecurityManager().let {
            if (it == null) {
                Thread.currentThread().threadGroup
            } else {
                it.threadGroup
            }
        }
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String = "pool-${POOL_NUMBER.getAndIncrement()}-thread-"

        override fun newThread(r: Runnable): Thread {
            val t = Thread(
                group,
                r,
                namePrefix + threadNumber.getAndIncrement(),
                0
            )
            if (t.isDaemon) t.isDaemon = false
            if (t.priority != Thread.MAX_PRIORITY) t.priority = Thread.MAX_PRIORITY
            return t
        }

        companion object {
            private val POOL_NUMBER = AtomicInteger(1)
        }
    }

    companion object {
        private const val LOADING_COMPLETE = 1
    }
}