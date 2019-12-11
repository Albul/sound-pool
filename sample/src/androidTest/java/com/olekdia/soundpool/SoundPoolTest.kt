package com.olekdia.soundpool

import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.olekdia.sample.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
//import org.robolectric.Shadows.shadowOf
//import org.robolectric.shadows.ShadowLooper

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

//        val threadShadow: ShadowLooper = shadowOf(pool.loadHandlerThread.looper)
        val soundId: Int = pool.load(resourceId)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.onIdle()
//        threadShadow.runOneTask()
        assertTrue(pool.isLoaded(soundId))


//        ShadowLooper.pauseMainLooper()
        pool.play(soundId)
//        threadShadow.runOneTask()
//        ShadowLooper.runMainLooperOneTask()
        Espresso.onIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertTrue(pool.isPlaying(soundId))
    }


/*    @Test
    fun loadSound_pauseImmediately() {
        val resourceId = getResource("bg_sea_retain")

        val soundId: Int = soundPool.load(resourceId)
        Thread.sleep(10)
        soundPool.pause(soundId)
    }

    @Test
    fun loadSound_pauseStopImmediately() {
        val resourceId = getResource("bg_sea_retain")

        val soundId: Int = soundPool.load(resourceId)
        Thread.sleep(10)
        soundPool.pause(soundId)
        soundPool.stop(soundId)
    }

    @Test
    fun loadSound_stopPauseImmediately() {
        val resourceId = getResource("bg_sea_retain")

        val soundId: Int = soundPool.load(resourceId)
        Thread.sleep(10)
        soundPool.stop(soundId)
        soundPool.pause(soundId)
        assertTrue(soundPool.isStopped(soundId))
    }

    */
}