package com.olekdia.soundpool

import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.androidcommontest.ActivityFinisher
import com.olekdia.androidcommontest.delayShortToResume
import com.olekdia.sample.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class SoundPoolTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    fun createSoundPool(): SoundPoolCompat = SoundPoolCompat(context, 5, 80000)

    private val context: Context get() = activityTestRule.activity

    private fun getResource(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    private fun SoundPoolCompat.loadAndWait(resourceId: Int): Int {
        LoadIdlingResource(this)
        return this.load(resourceId)
            .also { Espresso.onIdle() }
    }

    private fun SoundPoolCompat.playAndWait(
        sampleId: Int,
        loop: Int = 0
    ): Int {
        PlayIdlingResource(this)
        return this.play(sampleId, loop)
            .also { Espresso.onIdle() }
    }

    private fun SoundPoolCompat.resumeAndWait(sampleId: Int) {
        PlayIdlingResource(this)
        this.resume(sampleId)
            .also { Espresso.onIdle() }
    }

    @Before
    fun setUp() {
    }

    @Test
    fun loadSound_stopImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        pool.stop(soundId)
        assertFalse(pool.isLoaded(soundId))

        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_pauseImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        pool.pause(soundId)
        assertFalse(pool.isLoaded(soundId))

        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_pauseStopImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        pool.pause(soundId)
        pool.stop(soundId)

        assertFalse(pool.isLoaded(soundId))

        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_stopPauseImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        pool.stop(soundId)
        pool.pause(soundId)
        assertFalse(pool.isLoaded(soundId))

        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

//--------------------------------------------------------------------------------------------------
//  Loop sounds
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSound_playLoop_sampleIsPlaying() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_pause_resume_sampleIsPlaying() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.pause(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))

        pool.resumeAndWait(soundId)
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_pause_playLoop_sampleIsPlaying() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
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
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_stop_playLoop_sampleIsPlaying() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
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
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_stop_resume_sampleIsStopped() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
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
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_unload_sampleIsUnloadedAndNotPlaying() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        pool.unload(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isLoaded(soundId))

        pool.stop(soundId)
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test // todo
    fun loadSound_playLoop_pause_stop_playLoop_sampleIsStartedFromTheBeginning() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
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
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_playLoop_wait3xOfLength_trackIsStillPlaying() {
        val resourceId = getResource("sec_tick_cricket")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, -1)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(8000)
        assertTrue(pool.isPlaying(soundId))
        assertFalse(pool.isStopped(soundId))

        pool.stop(soundId)
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound2xSounds_playLoop1_pause_playLoop2_stop_playLoop1_pause_playLoop2() {
        val resource1Id = getResource("bg_sunrise_inhale")
        val resource2Id = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val sound1Id: Int = pool.loadAndWait(resource1Id)
        assertTrue(pool.isLoaded(sound1Id))

        val sound2Id: Int = pool.loadAndWait(resource2Id)
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
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }

//--------------------------------------------------------------------------------------------------
//  Non loop sounds
//--------------------------------------------------------------------------------------------------

    @Test
    fun loadSound_playOneTime_waitTillTheEnd_trackIsStopped() {
        val resourceId = getResource("sec_tick_cricket")
        val pool = createSoundPool()

        val soundId: Int = pool.loadAndWait(resourceId)
        assertTrue(pool.isLoaded(soundId))

        pool.playAndWait(soundId, 0)
        assertTrue(pool.isPlaying(soundId))

        Thread.sleep(1500)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.stop(soundId)
        delayShortToResume()
        ActivityFinisher.finishOpenActivities()
    }
}