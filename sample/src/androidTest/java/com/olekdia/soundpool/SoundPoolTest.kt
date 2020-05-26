package com.olekdia.soundpool

import android.content.Context
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.common.INVALID
import com.olekdia.sample.MainActivity
import com.olekdia.sample.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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
    fun static_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.sec_tick_bird_ouzel, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.bg_sea_retain, isStatic = false)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_mp3File_sampleIsLoadedAndStopped() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.dyathon_hope, isStatic = false)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertTrue(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_unloadSample_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.bg_sea_retain, isStatic = false)
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
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_unloadFewTimes_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.bg_sea_retain, isStatic = false)
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
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun stream_loadSample_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.design_patterns_pdf, isStatic = false)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
    }

    @Test
    fun static_loadSample_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load(R.raw.design_patterns_pdf, isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.unload(soundId)
        Thread.sleep(LOAD_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
    }

    @Test
    fun stream_loadSample_fromHttps_sampleIsLoadedAndPlaying() {
        val pool = createSoundPool()
        val soundId = pool.load("https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg", isStatic = false)
        Thread.sleep(LOAD_LONG_TIMEOUT)

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
        val soundId = pool.load("https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg", isStatic = true)
        Thread.sleep(LOAD_LONG_TIMEOUT)

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
    fun stream_loadSample_fromHttps_notAudioFile_sampleIsUnloaded() {
        val pool = createSoundPool()
        val soundId = pool.load("https://pranabreath.info/images/6/6b/Vajrasana_pos.png", isStatic = false)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))

        pool.play(soundId)
        Thread.sleep(PLAY_TIMEOUT)

        assertFalse(pool.isPlaying(soundId))
    }

    @Test
    fun stream_loadSample_fromEmptyPathString_sampleIsUnloaded() {
        val pool = createSoundPool()
        var soundId = INVALID

        assertThrows(
            IOException::class.java
        ) {
            soundId = pool.load("", isStatic = false)
        }

        assertEquals(INVALID, soundId)
    }

    @Test
    fun stream_loadSample_fromNullPathString_sampleIsUnloaded() {
        val pool = createSoundPool()
        var soundId = INVALID

        assertThrows(
            IOException::class.java
        ) {
            soundId = pool.load(null, isStatic = false)
        }

        assertEquals(INVALID, soundId)
    }

    // todo random resource

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

    @Test
    fun loadSound2xSounds_playLoop1_pause_playLoop2_stop_playLoop1_pause_playLoop2() {
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

//--------------------------------------------------------------------------------------------------
//  Non loop sounds
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSound_playOneTime_waitTillTheEnd_trackIsStopped() {
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(R.raw.sec_tick_cricket)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, 0)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(1500)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
    }

//--------------------------------------------------------------------------------------------------
//  Play once
//--------------------------------------------------------------------------------------------------

    @Test
    fun playOnce_sampleIsPlayingAndStoppedAfter() {
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
        assertTrue(pool.isStopped(soundId))
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
        assertTrue(pool.isStopped(soundId))
    }

    @Test
    fun playOnce_sampleIsPlaying_pauseThenPlayAnotherThenResume_samplesIsStoppedAfter() {
        val pool = createSoundPool()
        val otherSoundId = pool.load(R.raw.phase_tick_bumblebee)
        Thread.sleep(LOAD_LONG_TIMEOUT)

        val soundId = pool.playOnce(R.raw.sec_tick_frog, 1f, 1f, 1f)
        Thread.sleep(LOAD_LONG_TIMEOUT)
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
        assertTrue(resumeResult)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(600)

        assertFalse(pool.isLoaded(soundId))
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isStopped(soundId))

        assertTrue(pool.isPlaying(otherSoundId))
        assertTrue(pool.isPlaying())

        Thread.sleep(800)

        assertFalse(pool.isPlaying(otherSoundId))
        assertFalse(pool.isPlaying())
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
        this.play(sampleId, loop)
        Thread.sleep(PLAY_TIMEOUT)
    }

    private fun SoundPoolCompat.resumeAndWait(sampleId: Int) {
        this.resume(sampleId)
        Thread.sleep(PLAY_TIMEOUT)
    }
}