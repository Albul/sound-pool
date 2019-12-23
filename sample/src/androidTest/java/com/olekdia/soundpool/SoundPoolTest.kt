package com.olekdia.soundpool

import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.androidcommontest.ActivityFinisher
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

    fun createSoundPool(): SoundPoolCompat = SoundPoolCompat(context, 5, 100000)

    private val context: Context get() = activityTestRule.activity

    private fun getResource(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    @Before
    fun setUp() {
    }

    @Test
    fun loadSound_play_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_play_pause_resume_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))

        pool.pause(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))

        pool.resume(soundId)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isPlaying(soundId))
        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_play_pause_play_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))

        pool.pause(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isPaused(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertFalse(pool.isPaused(soundId))
        assertTrue(pool.isPlaying(soundId))

        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_play_stop_play_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertFalse(pool.isStopped(soundId))
        assertTrue(pool.isPlaying(soundId))

        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_play_stop_resume_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))

        pool.stop(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertTrue(pool.isStopped(soundId))

        pool.resume(soundId)
        Thread.sleep(1000)
        assertTrue(pool.isStopped(soundId))
        assertFalse(pool.isPlaying(soundId))

        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_play_unload_properState() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        assertTrue(pool.isPlaying(soundId))

        pool.unload(soundId)
        assertFalse(pool.isPlaying(soundId))
        assertFalse(pool.isLoaded(soundId))

        ActivityFinisher.finishOpenActivities()
    }

    @Test // todo
    fun loadSound_play_pause_stop_startPlayingFromTheBeginning() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val loadIdle = LoadIdlingResource(pool)
        val soundId: Int = pool.load(resourceId)
        Espresso.onIdle()
        assertTrue(pool.isLoaded(soundId))

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        Thread.sleep(20000)
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

        pool.play(soundId, 5F, 5F, -1, 1F)
        do {
            Thread.sleep(1000)
        } while (pool.playThreadPool.activeCount < 1)
        Thread.sleep(20000)
        assertTrue(pool.isPlaying(soundId))

        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_stopImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.stop(soundId)

        ActivityFinisher.finishOpenActivities()
    }

    @Test
    fun loadSound_pauseImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.pause(soundId)

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

        ActivityFinisher.finishOpenActivities()
    }
}