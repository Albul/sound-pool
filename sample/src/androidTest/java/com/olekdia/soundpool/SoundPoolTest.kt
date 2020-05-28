package com.olekdia.soundpool

import android.content.Context
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.androidcommon.NO_RESOURCE
import com.olekdia.common.INVALID
import com.olekdia.sample.MainActivity
import com.olekdia.sample.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

const val STREAM_SMALL_BUFFER: Int = 1000
const val LOAD_LONG_TIMEOUT: Long = 400L
const val LOAD_TIMEOUT: Long = 50L
const val PLAY_TIMEOUT: Long = 50L

@LargeTest
class SoundPoolTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    private val context: Context get() = activityTestRule.activity

//--------------------------------------------------------------------------------------------------
//  Load methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSample_play_releaseSoundPool_nothingLoadedNorPlaying() {
        val pool = createSoundPool()

        val soundId1 = pool.load(R.raw.sec_tick_bird_ouzel, isStatic = true)
        val soundId2 = pool.load(
            R.raw.bg_sea_retain,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId1))
        assertTrue(pool.isLoaded(soundId2))
        pool.play(soundId1, repeat = -1)
        pool.play(soundId2, repeat = 1)

        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId1))
        assertTrue(pool.isPlaying(soundId2))
        assertTrue(pool.isPlaying())

        Thread.sleep(500)
        pool.release()

        assertFalse(pool.isLoaded(soundId1))
        assertFalse(pool.isLoaded(soundId2))
        assertFalse(pool.isPlaying(soundId1))
        assertFalse(pool.isPlaying(soundId2))
        assertFalse(pool.isPlaying())

        Thread.sleep(1000)
    }

    @Test
    fun static_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(R.raw.sec_tick_bird_ouzel, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertTrue(success)
        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            R.raw.bg_sea_retain,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertTrue(success)
        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_mp3File_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            R.raw.dyathon_hope,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertTrue(success)
        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_unloadSample_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(
            R.raw.bg_sea_retain,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_unloadFewTimes_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(
            R.raw.bg_sea_retain,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.unload(soundId)
        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)
        pool.unload(soundId)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            R.raw.design_patterns_pdf,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertFalse(success)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
    }

    @Test
    fun static_loadSample_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(R.raw.design_patterns_pdf, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertFalse(success)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
    }

    @Test
    fun stream_loadSample_fromHttps_sampleIsLoadedAndPlaying() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            "https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg",
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(1000L)

        assertTrue(onLoadCalled)
        assertTrue(success)
        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(1000)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        pool.stop(soundId)
    }

    @Test
    fun static_loadSample_fromHttps_sampleIsLoadedAndPlaying() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            "https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg",
            isStatic = true
        )
        Thread.sleep(1000L)

        assertTrue(onLoadCalled)
        assertTrue(success)
        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(1000L)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        pool.stop(soundId)
    }

    @Test
    fun stream_loadSample_fromHttps_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load(
            "https://pranabreath.info/images/6/6b/Vajrasana_pos.png",
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(1000L)

        assertTrue(onLoadCalled)
        assertFalse(success)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)

        assertFalse(pool.isPlaying(soundId))
    }

    @Test
    fun stream_loadSample_fromEmptyPathString_sampleIsNotLoaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })

        val soundId = pool.load("", isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(onLoadCalled)
        assertEquals(INVALID, soundId)
    }

    @Test
    fun stream_loadSample_fromNullPathString_sampleIsNotLoaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })
        val soundId = pool.load(null, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(onLoadCalled)
        assertEquals(INVALID, soundId)
    }

    @Test
    fun stream_loadSample_from0Resource_sampleIsNotloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })
        val soundId = pool.load(NO_RESOURCE, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(onLoadCalled)
        assertEquals(INVALID, soundId)
    }

    @Test
    fun stream_loadSample_fromNegativeResource_sampleIsUnloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })
        val soundId = pool.load(-12345, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertFalse(success)
        assertNotEquals(INVALID, soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_fromNotExistingResource_sampleIsUnloaded() {
        val pool = createSoundPool()
        var onLoadCalled = false
        var success = false
        pool.setOnLoadCompleteListener(object : SoundPoolCompat.OnLoadCompleteListener {
            override fun onLoadComplete(
                soundPool: SoundPoolCompat,
                sampleId: Int,
                isSuccess: Boolean,
                errorMsg: String?
            ) {
                onLoadCalled = true
                success = isSuccess
            }
        })
        val soundId = pool.load(1234567890, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(onLoadCalled)
        assertFalse(success)
        assertNotEquals(INVALID, soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun load5Sounds_loadOneMore_maxSamplesExceeded() {
        val pool = createSoundPool()

        val soundId1: Int = pool.loadAndWait(R.raw.dyathon_hope)
        assertTrue(pool.isLoaded(soundId1))
        assertNotEquals(INVALID, soundId1)

        val soundId2: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(soundId2))
        assertNotEquals(INVALID, soundId2)

        val soundId3: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId3))
        assertNotEquals(INVALID, soundId3)

        val soundId4: Int = pool.loadAndWait(R.raw.sec_tick_bird_ouzel)
        assertTrue(pool.isLoaded(soundId4))
        assertNotEquals(INVALID, soundId4)

        val soundId5: Int = pool.loadAndWait(R.raw.sec_tick_bird_goldfinch)
        assertTrue(pool.isLoaded(soundId5))
        assertNotEquals(INVALID, soundId5)

        val soundId6: Int = pool.load(R.raw.sec_tick_frog)
        assertFalse(pool.isLoaded(soundId6))
        assertEquals(INVALID, soundId6)
    }

    @Test
    fun loadSameSoundsThreeTimes_loadedWithDifferentSamples() {
        val pool = createSoundPool()

        val soundId1: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(soundId1))
        assertNotEquals(INVALID, soundId1)

        val soundId2: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(soundId2))
        assertNotEquals(INVALID, soundId2)

        val soundId3: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(soundId3))
        assertNotEquals(INVALID, soundId3)

        assertNotEquals(soundId1, soundId2)
        assertNotEquals(soundId2, soundId3)

        pool.play(soundId1)
        pool.play(soundId2)
        pool.play(soundId3)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId1))
        assertTrue(pool.isPlaying(soundId2))
        assertTrue(pool.isPlaying(soundId3))

        pool.stop(soundId1)
        pool.stop(soundId2)
        pool.stop(soundId3)
    }

//--------------------------------------------------------------------------------------------------
//  Immediately methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSound_stopImmediately() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        val stopResult = pool.stop(soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
        assertFalse(stopResult)
    }

    @Test
    fun loadSound_pauseImmediately() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        val result = pool.pause(soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(result)
    }

    @Test
    fun loadSound_pauseStopImmediately() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        val pauseResult = pool.pause(soundId)
        val stopResult = pool.stop(soundId)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
        assertFalse(stopResult)
        assertFalse(pauseResult)
    }

    @Test
    fun loadSound_stopPauseImmediately() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        val stopResult = pool.stop(soundId)
        val pauseResult = pool.pause(soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(stopResult)
        assertFalse(pauseResult)
    }

    @Test
    fun loadSound_playImmediately() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        pool.play(soundId)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isStopped(soundId))
        assertFalse(pool.isPlaying(soundId))
    }

//--------------------------------------------------------------------------------------------------
//  Play, pause, resume, stop
//--------------------------------------------------------------------------------------------------

    @Test
    fun static_playThenPauseThenResume_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        val pauseResult = pool.pause(soundId)

        assertTrue(pauseResult)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val resumeResult = pool.resume(soundId)
        assertTrue(resumeResult)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_playThenPauseThenResume_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        val pauseResult = pool.pause(soundId)

        assertTrue(pauseResult)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val resumeResult = pool.resume(soundId)
        assertTrue(resumeResult)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_playThenPauseThenStopThenResume_sampleIsNotResumed() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        val pauseResult = pool.pause(soundId)

        assertTrue(pauseResult)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val stopResult = pool.stop(soundId)
        assertTrue(stopResult)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        val resumeResult = pool.resume(soundId)
        assertFalse(resumeResult)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_playThenPauseThenStopThenResume_sampleIsNotResumed() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(
            R.raw.phase_tick_bumblebee,
            isStatic = false,
            bufferSize = STREAM_SMALL_BUFFER
        )
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
        val pauseResult = pool.pause(soundId)

        assertTrue(pauseResult)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val stopResult = pool.stop(soundId)
        assertTrue(stopResult)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        val resumeResult = pool.resume(soundId)
        assertFalse(resumeResult)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_playThenThenStopThenPlay_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val stopResult = pool.stop(soundId)
        assertTrue(stopResult)

        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_playThenThenStopThenPlay_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val stopResult = pool.stop(soundId)
        assertTrue(stopResult)

        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

//--------------------------------------------------------------------------------------------------
//  Loop sounds
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSound_playLoop_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_pause_resume_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        val result = pool.pause(soundId)
        assertTrue(result)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))

        pool.resumeAndWait(soundId)
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_pause_playLoop_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.pause(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))

        pool.playAndWait(soundId, -1)
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_stop_playLoop_sampleIsPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.bg_sea_retain)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.playAndWait(soundId, -1)
        assertFalse(pool.isStopped(soundId))
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_stop_resume_sampleIsStopped() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.resume(soundId)
        Thread.sleep(1000)
        assertTrue(pool.isStopped(soundId))
        assertFalse(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_unload_sampleIsUnloadedAndNotPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.unload(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isLoaded(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_pause_stop_playLoop_sampleIsStartedFromTheBeginning() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.pause(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))
        //assertTrue(pool.samplePool.get(soundId)?.pausedPlaybackInBytes ?: 0 > 0)

        pool.stop(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
        //assertFalse(pool.samplePool.get(soundId)?.pausedPlaybackInBytes ?: 0 > 0)

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
    }

    @Test
    fun loadSound_playLoop_wait3xOfLength_trackIsStillPlaying() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(8000)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

//--------------------------------------------------------------------------------------------------
//  Non loop sounds
//--------------------------------------------------------------------------------------------------

    @Test
    fun stream_loadSound_playOneTime_waitTillTheEnd_trackIsStopped() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, 0)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(300)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_waitTillTheEnd_trackIsStopped() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, 0)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(300)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_waitTillTheEnd_trackIsStopped_playAgain() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, 0)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(300)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_setLoopToPlayTwice_sampleIsPlayedTwice() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setLoop(soundId, 1)
        assertTrue(result)

        Thread.sleep(300)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(300)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_setLoopToPlayForever_sampleIsPlayingForever() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setLoop(soundId, -1)
        assertTrue(result)

        Thread.sleep(300)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(8900)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_setRate2x_sampleIsPlayedTwiceFaster() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setRate(soundId, 2f)
        assertTrue(result)

        Thread.sleep(150)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_loadSound_playOneTime_setRate2x_sampleIsPlayedTwiceFaster() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setRate(soundId, 2f)
        assertTrue(result)

        Thread.sleep(150)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun static_loadSound_playOneTime_setRate2xSlower_sampleIsPlayedTwiceSlower() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setRate(soundId, 0.5f)
        assertTrue(result)

        Thread.sleep(260)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_loadSound_playOneTime_setRate2xSlower_sampleIsPlayedTwiceSlower() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result = pool.setRate(soundId, 0.5f)
        assertTrue(result)

        Thread.sleep(1100)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
    }

    @Test
    fun stream_loadSound_playOneTime_setVolume_sampleIsPlayed() {
        val pool = createSoundPool()

        val soundId: Int = pool.load(R.raw.sec_tick_cricket, isStatic = false, bufferSize = STREAM_SMALL_BUFFER)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 0)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(pool.isPlaying(soundId))
        val result1 = pool.setVolume(soundId, 0.5f)
        assertTrue(result1)

        val result2 = pool.setVolume(soundId, 0.8f, 0.8f)
        assertTrue(result2)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(300)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

//--------------------------------------------------------------------------------------------------
//  Play once
//--------------------------------------------------------------------------------------------------

    @Test
    fun playOnceFromRes_sampleIsPlayingAndStoppedAfter() {
        val pool = createSoundPool()

        val soundId = pool.playOnce(R.raw.phase_tick_bumblebee, 1f, 1f, 1f)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))
        assertTrue(pool.isPlaying(soundId))
        assertTrue(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(1300)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun playOnceFromHttpPath_sampleIsPlayingAndStoppedAfter() {
        val pool = createSoundPool()

        val soundId = pool.playOnce("https://olekdia.com/a/prana_breath/soundfiles/sh_white_stork_0.ogg")
        Thread.sleep(1000L)
        assertTrue(pool.isLoaded(soundId))
        assertTrue(pool.isPlaying(soundId))
        assertTrue(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(3000)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun playOnce_sampleIsPlaying_pauseAndWait_sampleIsUnloadedAndCannotBeResumed() {
        val pool = createSoundPool()

        val soundId = pool.playOnce(R.raw.phase_tick_bumblebee, 1f, 1f, 1f)
        Thread.sleep(LOAD_LONG_TIMEOUT)
        assertTrue(pool.isLoaded(soundId))
        assertTrue(pool.isPlaying(soundId))
        assertTrue(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(50)
        val pauseResult = pool.pause(soundId)
        assertTrue(pauseResult)
        Thread.sleep(50)

        val resumeResult = pool.resume(soundId)
        assertFalse(resumeResult)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))
    }

    @Test
    fun playOnce_sampleIsPlaying_pauseThenPlayAnotherThenResume_sampleIsUnloadedAndCannotBeResumed() {
        val pool = createSoundPool()
        val otherSoundId = pool.load(R.raw.phase_tick_bumblebee)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        val soundId = pool.playOnce(R.raw.sec_tick_frog, 1f, 1f, 1f)
        Thread.sleep(200)
        assertTrue(pool.isLoaded(soundId))
        assertTrue(pool.isPlaying(soundId))
        assertTrue(pool.isPlaying())
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        val pauseResult = pool.pause(soundId)
        assertTrue(pauseResult)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPlaying())

        pool.play(otherSoundId)
        assertTrue(pool.isLoaded(otherSoundId))
        assertTrue(pool.isPlaying(otherSoundId))
        assertTrue(pool.isPlaying())
        assertFalse(pool.isPaused(otherSoundId))
        assertFalse(pool.isStopped(otherSoundId))

        val resumeResult = pool.resume(soundId)
        assertFalse(resumeResult)
        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertFalse(pool.isStopped(soundId))

        Thread.sleep(600)

        assertTrue(pool.isPlaying(otherSoundId))
        assertTrue(pool.isPlaying())

        Thread.sleep(800)

        assertFalse(pool.isPlaying(otherSoundId))
        assertFalse(pool.isPlaying())
    }

//--------------------------------------------------------------------------------------------------
//  Complex case
//--------------------------------------------------------------------------------------------------

    @Test
    fun load2xSounds_playLoop1_pause_playLoop2_stop_playLoop1_pause_playLoop2() {
        val pool = createSoundPool()

        val sound1Id: Int = pool.loadAndWait(R.raw.bg_sunrise_inhale)
        assertTrue(pool.isLoaded(sound1Id))

        val sound2Id: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(sound2Id))

        pool.playAndWait(sound1Id, -1)
        assertTrue(pool.isPlaying(sound1Id))

        Thread.sleep(4000)
        pool.pause(sound1Id)
        assertTrue(pool.isPaused(sound1Id))
        assertFalse(pool.isPlaying(sound1Id))
        Thread.sleep(10)

        pool.playAndWait(sound2Id, -1)
        assertTrue(pool.isPlaying(sound2Id))
        Thread.sleep(4000)
        pool.stop(sound2Id)
        assertTrue(pool.isStopped(sound2Id))
        assertFalse(pool.isPlaying(sound2Id))
        Thread.sleep(10)

        pool.playAndWait(sound1Id, -1)
        assertTrue(pool.isPlaying(sound1Id))
        Thread.sleep(4000)
        pool.pause(sound1Id)
        assertTrue(pool.isPaused(sound1Id))
        assertFalse(pool.isPlaying(sound1Id))
        Thread.sleep(10)

        pool.playAndWait(sound2Id, -1)
        assertTrue(pool.isPlaying(sound2Id))

        pool.stop(sound1Id)
        pool.stop(sound2Id)
    }

    @Test
    fun play2Loops_play3ShortSounds() {
        val pool = createSoundPool()

        val loopId1: Int = pool.loadAndWait(R.raw.dyathon_hope)
        assertTrue(pool.isLoaded(loopId1))

        val loopId2: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(loopId2))

        val shortId1: Int = pool.load(R.raw.sec_tick_cricket)
        val shortId2: Int = pool.load(R.raw.sec_tick_bird_ouzel)
        val shortId3: Int = pool.load(R.raw.phase_tick_bumblebee)
        Thread.sleep(800L)

        assertTrue(pool.isLoaded(shortId1))
        assertTrue(pool.isLoaded(shortId2))
        assertTrue(pool.isLoaded(shortId3))

        pool.playAndWait(loopId1, -1)
        pool.playAndWait(loopId2, -1)
        pool.play(shortId1)
        pool.play(shortId2)
        pool.play(shortId3)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))
        assertTrue(pool.isPlaying(shortId1))
        assertTrue(pool.isPlaying(shortId2))
        assertTrue(pool.isPlaying(shortId3))

        Thread.sleep(2000)

        assertFalse(pool.isPlaying(shortId1))
        assertFalse(pool.isPlaying(shortId2))
        assertFalse(pool.isPlaying(shortId3))
        assertTrue(pool.isStopped(shortId1))
        assertTrue(pool.isStopped(shortId2))
        assertTrue(pool.isStopped(shortId3))

        pool.stop(loopId1)
        pool.stop(loopId2)
    }

    @Test
    fun play2Loops_playOnce2Sounds() {
        val pool = createSoundPool()

        val loopId1: Int = pool.loadAndWait(R.raw.dyathon_hope)
        assertTrue(pool.isLoaded(loopId1))

        val loopId2: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(loopId2))

        pool.playAndWait(loopId1, -1)
        pool.playAndWait(loopId2, -1)

        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))

        val shortId1 = pool.playOnce(R.raw.sec_tick_cricket)
        assertNotEquals(INVALID, shortId1)
        Thread.sleep(100)
        assertTrue(pool.isPlaying(shortId1))

        val shortId2 = pool.playOnce(R.raw.sec_tick_bird_ouzel)
        assertNotEquals(INVALID, shortId2)
        Thread.sleep(100)
        assertTrue(pool.isPlaying(shortId2))

        Thread.sleep(1000)
        assertFalse(pool.isPlaying(shortId1))
        assertFalse(pool.isPlaying(shortId2))
        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))

        assertFalse(pool.isLoaded(shortId1))
        assertFalse(pool.isLoaded(shortId2))

        pool.stop(loopId1)
        pool.stop(loopId2)
    }

    @Test
    fun play3Sounds_autoPause_nothingPlaying_autoResume_samplesResumedSavingStates() {
        val pool = createSoundPool()

        val loopId1: Int = pool.load(R.raw.dyathon_hope, isStatic = false)
        val loopId2: Int = pool.load(R.raw.bg_wind_retain, isStatic = true)
        val shortId3: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = false)
        val shortId4: Int = pool.load(R.raw.voice_male_inhale, isStatic = true)
        val shortId5: Int = pool.load(R.raw.sec_tick_grasshopper, isStatic = true)
        Thread.sleep(1500)

        assertTrue(pool.isLoaded(loopId1))
        assertTrue(pool.isLoaded(loopId2))
        assertTrue(pool.isLoaded(shortId3))
        assertTrue(pool.isLoaded(shortId4))
        assertTrue(pool.isLoaded(shortId5))


        pool.play(loopId1, repeat = -1)
        pool.play(loopId2, repeat = -1)
        pool.play(shortId3, repeat = 1, rate = 0.5F)
        pool.play(shortId4, repeat = 0)

        Thread.sleep(200)

        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))
        assertTrue(pool.isPlaying(shortId3))
        assertTrue(pool.isPlaying(shortId4))
        assertFalse(pool.isPlaying(shortId5))
        assertTrue(pool.isPlaying())

        pool.autoPause()

        assertFalse(pool.isPlaying())
        assertFalse(pool.isPlaying(loopId1))
        assertFalse(pool.isPlaying(loopId2))
        assertFalse(pool.isPlaying(shortId3))
        assertFalse(pool.isPlaying(shortId4))
        assertFalse(pool.isPlaying(shortId5))

        assertTrue(pool.isPaused(loopId1))
        assertTrue(pool.isPaused(loopId2))
        assertTrue(pool.isPaused(shortId3))
        assertTrue(pool.isPaused(shortId4)) // todo
        assertFalse(pool.isPaused(shortId5))

        pool.autoResume()

        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))
        assertTrue(pool.isPlaying(shortId3))
        assertTrue(pool.isPlaying(shortId4))
        assertFalse(pool.isPlaying(shortId5))
        assertTrue(pool.isPlaying())

        Thread.sleep(1500)
        assertTrue(pool.isPlaying(loopId1))
        assertTrue(pool.isPlaying(loopId2))
        assertTrue(pool.isPlaying(shortId3))
        assertFalse(pool.isPlaying(shortId4))

        pool.stop(loopId1)
        pool.stop(loopId2)
        pool.stop(shortId3)
        pool.stop(shortId4)
    }

    @Test
    fun stream_stressTest_load20SoundsOneByOne_play_samplesIsLoadedAndPlayed() {
        val pool = SoundPoolCompat(context, 25, 80000)

        val soundId1: Int = pool.loadAndWait(R.raw.bg_wind_retain)
        assertTrue(pool.isLoaded(soundId1))
        assertNotEquals(INVALID, soundId1)

        val soundId2: Int = pool.loadAndWait(R.raw.bg_sunrise_inhale)
        assertTrue(pool.isLoaded(soundId2))
        assertNotEquals(INVALID, soundId2)

        val soundId3: Int = pool.loadAndWait(R.raw.bg_sea_retain)
        assertTrue(pool.isLoaded(soundId3))
        assertNotEquals(INVALID, soundId3)

        val soundId4: Int = pool.loadAndWait(R.raw.sec_tick_bird_goldfinch)
        assertTrue(pool.isLoaded(soundId4))
        assertNotEquals(INVALID, soundId4)

        val soundId5: Int = pool.loadAndWait(R.raw.sec_tick_bird_ouzel)
        assertTrue(pool.isLoaded(soundId5))
        assertNotEquals(INVALID, soundId5)

        val soundId6: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId6))
        assertNotEquals(INVALID, soundId6)

        val soundId7: Int = pool.loadAndWait(R.raw.sec_tick_frog)
        assertTrue(pool.isLoaded(soundId7))
        assertNotEquals(INVALID, soundId7)

        val soundId8: Int = pool.loadAndWait(R.raw.sec_tick_grasshopper)
        assertTrue(pool.isLoaded(soundId8))
        assertNotEquals(INVALID, soundId8)

        val soundId9: Int = pool.loadAndWait(R.raw.voice_female_retain)
        assertTrue(pool.isLoaded(soundId9))
        assertNotEquals(INVALID, soundId9)

        val soundId10: Int = pool.loadAndWait(R.raw.voice_male_inhale)
        assertTrue(pool.isLoaded(soundId10))
        assertNotEquals(INVALID, soundId10)

        val soundId11: Int = pool.loadAndWait(R.raw.sec_tick_frog)
        assertTrue(pool.isLoaded(soundId11))
        assertNotEquals(INVALID, soundId11)

        val soundId12: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId12))
        assertNotEquals(INVALID, soundId12)

        val soundId13: Int = pool.loadAndWait(R.raw.sec_tick_bird_ouzel)
        assertTrue(pool.isLoaded(soundId13))
        assertNotEquals(INVALID, soundId13)

        val soundId14: Int = pool.loadAndWait(R.raw.sec_tick_bird_goldfinch)
        assertTrue(pool.isLoaded(soundId14))
        assertNotEquals(INVALID, soundId14)

        val soundId15: Int = pool.loadAndWait(R.raw.phase_tick_bird_oriole)
        assertTrue(pool.isLoaded(soundId15))
        assertNotEquals(INVALID, soundId15)

        val soundId16: Int = pool.loadAndWait(R.raw.phase_tick_bird_oriole_golden)
        assertTrue(pool.isLoaded(soundId16))
        assertNotEquals(INVALID, soundId16)

        val soundId17: Int = pool.loadAndWait(R.raw.phase_tick_bird_owl)
        assertTrue(pool.isLoaded(soundId17))
        assertNotEquals(INVALID, soundId17)

        val soundId18: Int = pool.loadAndWait(R.raw.phase_tick_bumblebee)
        assertTrue(pool.isLoaded(soundId18))
        assertNotEquals(INVALID, soundId18)

        val soundId19: Int = pool.loadAndWait(R.raw.sec_tick_grasshopper)
        assertTrue(pool.isLoaded(soundId19))
        assertNotEquals(INVALID, soundId19)

        val soundId20: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId20))
        assertNotEquals(INVALID, soundId20)

        pool.play(soundId1, repeat = -1)
        pool.play(soundId2, repeat = -1)
        pool.play(soundId3, repeat = -1)
        pool.play(soundId4, repeat = -1)
        pool.play(soundId5, repeat = -1)
        pool.play(soundId6, repeat = -1)
        pool.play(soundId7, repeat = -1)
        pool.play(soundId8, repeat = -1)
        pool.play(soundId9, repeat = -1)
        pool.play(soundId10, repeat = -1)
        pool.play(soundId11, repeat = -1)
        pool.play(soundId12, repeat = -1)
        pool.play(soundId13, repeat = -1)
        pool.play(soundId14, repeat = -1)
        pool.play(soundId15, repeat = -1)
        pool.play(soundId16, repeat = -1)
        pool.play(soundId17, repeat = -1)
        pool.play(soundId18, repeat = -1)
        pool.play(soundId19, repeat = -1)
        pool.play(soundId20, repeat = -1)
        Thread.sleep(500)

        assertTrue(pool.isPlaying(soundId1))
        assertTrue(pool.isPlaying(soundId2))
        assertTrue(pool.isPlaying(soundId3))
        assertTrue(pool.isPlaying(soundId4))
        assertTrue(pool.isPlaying(soundId5))
        assertTrue(pool.isPlaying(soundId6))
        assertTrue(pool.isPlaying(soundId7))
        assertTrue(pool.isPlaying(soundId8))
        assertTrue(pool.isPlaying(soundId9))
        assertTrue(pool.isPlaying(soundId10))
        assertTrue(pool.isPlaying(soundId11))
        assertTrue(pool.isPlaying(soundId12))
        assertTrue(pool.isPlaying(soundId13))
        assertTrue(pool.isPlaying(soundId14))
        assertTrue(pool.isPlaying(soundId15))
        assertTrue(pool.isPlaying(soundId16))
        assertTrue(pool.isPlaying(soundId17))
        assertTrue(pool.isPlaying(soundId18))
        assertTrue(pool.isPlaying(soundId19))
        assertTrue(pool.isPlaying(soundId20))

        Thread.sleep(2000)

        pool.stop(soundId1)
        pool.stop(soundId2)
        pool.stop(soundId3)
        pool.stop(soundId4)
        pool.stop(soundId5)
        pool.stop(soundId6)
        pool.stop(soundId7)
        pool.stop(soundId8)
        pool.stop(soundId9)
        pool.stop(soundId10)
        pool.stop(soundId11)
        pool.stop(soundId12)
        pool.stop(soundId13)
        pool.stop(soundId14)
        pool.stop(soundId15)
        pool.stop(soundId16)
        pool.stop(soundId17)
        pool.stop(soundId18)
        pool.stop(soundId19)
        pool.stop(soundId20)
    }

    @Test
    fun static_stressTest_load20SoundsSimultaneously_play_samplesIsLoadedAndPlayed() {
        val pool = SoundPoolCompat(context, 25, 80000)

        val soundId1: Int = pool.load(R.raw.bg_wind_retain, isStatic = true)
        assertNotEquals(INVALID, soundId1)

        val soundId2: Int = pool.load(R.raw.bg_sunrise_inhale, isStatic = true)
        assertNotEquals(INVALID, soundId2)

        val soundId3: Int = pool.load(R.raw.bg_sea_retain, isStatic = true)
        assertNotEquals(INVALID, soundId3)

        val soundId4: Int = pool.load(R.raw.sec_tick_bird_goldfinch, isStatic = true)
        assertNotEquals(INVALID, soundId4)

        val soundId5: Int = pool.load(R.raw.sec_tick_bird_ouzel, isStatic = true)
        assertNotEquals(INVALID, soundId5)

        val soundId6: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        assertNotEquals(INVALID, soundId6)

        val soundId7: Int = pool.load(R.raw.sec_tick_frog, isStatic = true)
        assertNotEquals(INVALID, soundId7)

        val soundId8: Int = pool.load(R.raw.sec_tick_grasshopper, isStatic = true)
        assertNotEquals(INVALID, soundId8)

        val soundId9: Int = pool.load(R.raw.voice_female_retain, isStatic = true)
        assertNotEquals(INVALID, soundId9)

        val soundId10: Int = pool.load(R.raw.voice_male_inhale, isStatic = true)
        assertNotEquals(INVALID, soundId10)

        val soundId11: Int = pool.load(R.raw.sec_tick_frog, isStatic = true)
        assertNotEquals(INVALID, soundId11)

        val soundId12: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        assertNotEquals(INVALID, soundId12)

        val soundId13: Int = pool.load(R.raw.sec_tick_bird_ouzel, isStatic = true)
        assertNotEquals(INVALID, soundId13)

        val soundId14: Int = pool.load(R.raw.sec_tick_bird_goldfinch, isStatic = true)
        assertNotEquals(INVALID, soundId14)

        val soundId15: Int = pool.load(R.raw.phase_tick_bird_oriole, isStatic = true)
        assertNotEquals(INVALID, soundId15)

        val soundId16: Int = pool.load(R.raw.phase_tick_bird_oriole_golden, isStatic = true)
        assertNotEquals(INVALID, soundId16)

        val soundId17: Int = pool.load(R.raw.phase_tick_bird_owl, isStatic = true)
        assertNotEquals(INVALID, soundId17)

        val soundId18: Int = pool.load(R.raw.phase_tick_bumblebee, isStatic = true)
        assertNotEquals(INVALID, soundId18)

        val soundId19: Int = pool.load(R.raw.sec_tick_grasshopper, isStatic = true)
        assertNotEquals(INVALID, soundId19)

        val soundId20: Int = pool.load(R.raw.sec_tick_cricket, isStatic = true)
        assertNotEquals(INVALID, soundId20)

        Thread.sleep(2500L)
        assertTrue(pool.isLoaded(soundId1))
        assertTrue(pool.isLoaded(soundId2))
        assertTrue(pool.isLoaded(soundId3))
        assertTrue(pool.isLoaded(soundId4))
        assertTrue(pool.isLoaded(soundId5))
        assertTrue(pool.isLoaded(soundId6))
        assertTrue(pool.isLoaded(soundId7))
        assertTrue(pool.isLoaded(soundId8))
        assertTrue(pool.isLoaded(soundId9))
        assertTrue(pool.isLoaded(soundId10))
        assertTrue(pool.isLoaded(soundId11))
        assertTrue(pool.isLoaded(soundId12))
        assertTrue(pool.isLoaded(soundId13))
        assertTrue(pool.isLoaded(soundId14))
        assertTrue(pool.isLoaded(soundId15))
        assertTrue(pool.isLoaded(soundId16))
        assertTrue(pool.isLoaded(soundId17))
        assertTrue(pool.isLoaded(soundId18))
        assertTrue(pool.isLoaded(soundId19))
        assertTrue(pool.isLoaded(soundId20))

        pool.play(soundId1, repeat = -1)
        pool.play(soundId2, repeat = -1)
        pool.play(soundId3, repeat = -1)
        pool.play(soundId4, repeat = -1)
        pool.play(soundId5, repeat = -1)
        pool.play(soundId6, repeat = -1)
        pool.play(soundId7, repeat = -1)
        pool.play(soundId8, repeat = -1)
        pool.play(soundId9, repeat = -1)
        pool.play(soundId10, repeat = -1)
        pool.play(soundId11, repeat = -1)
        pool.play(soundId12, repeat = -1)
        pool.play(soundId13, repeat = -1)
        pool.play(soundId14, repeat = -1)
        pool.play(soundId15, repeat = -1)
        pool.play(soundId16, repeat = -1)
        pool.play(soundId17, repeat = -1)
        pool.play(soundId18, repeat = -1)
        pool.play(soundId19, repeat = -1)
        pool.play(soundId20, repeat = -1)
        Thread.sleep(500)

        assertTrue(pool.isPlaying(soundId1))
        assertTrue(pool.isPlaying(soundId2))
        assertTrue(pool.isPlaying(soundId3))
        assertTrue(pool.isPlaying(soundId4))
        assertTrue(pool.isPlaying(soundId5))
        assertTrue(pool.isPlaying(soundId6))
        assertTrue(pool.isPlaying(soundId7))
        assertTrue(pool.isPlaying(soundId8))
        assertTrue(pool.isPlaying(soundId9))
        assertTrue(pool.isPlaying(soundId10))
        assertTrue(pool.isPlaying(soundId11))
        assertTrue(pool.isPlaying(soundId12))
        assertTrue(pool.isPlaying(soundId13))
        assertTrue(pool.isPlaying(soundId14))
        assertTrue(pool.isPlaying(soundId15))
        assertTrue(pool.isPlaying(soundId16))
        assertTrue(pool.isPlaying(soundId17))
        assertTrue(pool.isPlaying(soundId18))
        assertTrue(pool.isPlaying(soundId19))
        assertTrue(pool.isPlaying(soundId20))

        Thread.sleep(2000)

        pool.stop(soundId1)
        pool.stop(soundId2)
        pool.stop(soundId3)
        pool.stop(soundId4)
        pool.stop(soundId5)
        pool.stop(soundId6)
        pool.stop(soundId7)
        pool.stop(soundId8)
        pool.stop(soundId9)
        pool.stop(soundId10)
        pool.stop(soundId11)
        pool.stop(soundId12)
        pool.stop(soundId13)
        pool.stop(soundId14)
        pool.stop(soundId15)
        pool.stop(soundId16)
        pool.stop(soundId17)
        pool.stop(soundId18)
        pool.stop(soundId19)
        pool.stop(soundId20)
    }

//--------------------------------------------------------------------------------------------------
//  Utils
//--------------------------------------------------------------------------------------------------

    fun createSoundPool(): SoundPoolCompat = SoundPoolCompat(context, 5, 80000)

    private fun SoundPoolCompat.loadAndWait(resourceId: Int): Int {
        val soundId = this.load(resourceId)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        return soundId
    }

    private fun SoundPoolCompat.playAndWait(
        sampleId: Int,
        loop: Int = 0
    ) {
        this.play(sampleId, repeat = loop)
        Thread.sleep(PLAY_TIMEOUT)
    }

    private fun SoundPoolCompat.resumeAndWait(sampleId: Int) {
        this.resume(sampleId)
        Thread.sleep(PLAY_TIMEOUT)
    }
}