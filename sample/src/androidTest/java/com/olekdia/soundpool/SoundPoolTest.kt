package com.olekdia.soundpool

import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.sample.MainActivity
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
    fun loadSound_stopImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.stop(soundId)
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
    }


    @Test
    fun loadSound_pauseImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.pause(soundId)
    }

    @Test
    fun loadSound_pauseStopImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.pause(soundId)
        pool.stop(soundId)
    }

    @Test
    fun loadSound_stopPauseImmediately() {
        val resourceId = getResource("bg_sea_retain")
        val pool = createSoundPool()

        val soundId: Int = pool.load(resourceId)
        Thread.sleep(10)
        pool.stop(soundId)
        pool.pause(soundId)
        assertTrue(pool.isStopped(soundId))
    }
}

