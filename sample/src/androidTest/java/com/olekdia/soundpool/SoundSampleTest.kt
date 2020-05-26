package com.olekdia.soundpool

import android.content.Context
import android.media.AudioTrack
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.sample.MainActivity
import com.olekdia.sample.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.isAccessible

/**
 * R.raw.sec_tick_cricket - 260 ms
 */
@LargeTest
class SoundSampleTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    private val context: Context get() = activityTestRule.activity

    private val playThreadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        4,
        8,
        2,
        TimeUnit.SECONDS,
        LinkedBlockingQueue()
    )

    @Test
    fun createSample_initialStateCorrect() {
        val sample = SoundSample(12, 123, true)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
        assertEquals(12, sample.id)
    }

    @Test
    fun loadStaticSample_sampleIsLoadedAndStopped() {
        val sample = SoundSample(12, 1234567, true)
        val metadata = SoundSampleMetadata(12, R.raw.sec_tick_bird_ouzel, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
        assertEquals(12, sample.id)
    }

    @Test
    fun loadNonStaticSample_sampleIsLoadedAndStopped() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
        assertEquals(12, sample.id)
    }

    @Test
    fun loadSample_closeSample_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun loadSample_closeFewTimes_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())

        sample.close()
        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun loadSample_nonAudioFile_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.design_patterns_pdf, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun loadSample_playAndWait_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        val t = object : Thread() {
            override fun run() {
                sample.play(1f, 1f, 1f)
            }
        }
        t.start()
        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun playPauseStopWithoutLoading_sampleIsPaused() {
        val sample = SoundSample(12, 1234567, false)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(50)

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())

        sample.pause()
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())

        sample.stop()
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }


    @Test
    fun loadSample_playPauseImmediately_sampleIsPaused() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        val t = object : Thread() {
            override fun run() {
                sample.play(1f, 1f, 1f)
            }
        }
        t.start()
        sample.pause()
        Thread.sleep(50)

        assertFalse(sample.isPlaying())
        assertTrue(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun playStopImmediately_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.stop()
        Thread.sleep(50)

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playPauseStopPauseImmediately_sampleIsPaused() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()
        sample.stop()
        sample.pause()
        Thread.sleep(50)

        assertFalse(sample.isPlaying())
        assertTrue(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun playPauseResumeWithDelay_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())

        sample.pause()
        assertFalse(sample.isPlaying())
        assertTrue(sample.isPaused())

        sample.resume(playThreadPool)
        Thread.sleep(50)
        assertFalse(sample.isPaused())
        assertTrue(sample.isPlaying())
    }

    @Test
    fun playShortStreamSoundOnce_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())

        Thread.sleep(300)

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playShortStreamSoundTwice_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playShortStaticSoundTwice_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playLoopStream_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, -1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)
        assertTrue(sample.isPlaying())
    }

    @Test
    fun playLoopStatic_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, -1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)
        assertTrue(sample.isPlaying())
    }

//--------------------------------------------------------------------------------------------------
//  Set methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun playShortStreamOnce_setLoopForever_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        sample.setLoop(-1)
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun playShortStaticOnce_setLoopForever_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        sample.setLoop(-1)
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        Thread.sleep(300)
        assertTrue(sample.isPlaying())
        Thread.sleep(300)

        assertTrue(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertFalse(sample.isStopped())
    }

    @Test
    fun playStatic_setRate_sampleIsPlayingTwiceFast() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        sample.setRate(2f)
        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        Thread.sleep(100)
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playStream_setRate_sampleIsPlayingTwiceFast() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        sample.setRate(2f)
        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        Thread.sleep(100)
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
    }

    @Test
    fun playStream_setVolume_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        Thread.sleep(100)
        assertTrue(sample.isPlaying())
    }

    @Test
    fun playStatic_setVolume_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        Thread.sleep(100)
        assertTrue(sample.isPlaying())
    }

    @Test
    fun playStatic_setVolumeFewTimes_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying())
        val result1 = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result1)
        val result2 = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result2)
        val result3 = sample.setVolume(.8f, .8f)
        assertEquals(AudioTrack.SUCCESS, result3)

        Thread.sleep(50)

        assertTrue(sample.isPlaying())
        Thread.sleep(100)
        assertTrue(sample.isPlaying())
    }

    @Test
    fun setVolumeToNotLoaded_invalidReturned() {
        val sample = SoundSample(12, 8000, true)

        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded())
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.ERROR_INVALID_OPERATION, result)

        assertFalse(sample.isPlaying())
    }

//--------------------------------------------------------------------------------------------------
//  Utils
//--------------------------------------------------------------------------------------------------

    private fun createSampleAndLoad(rawResId: Int, isStatic: Boolean = false): SoundSample {
        val sample = SoundSample(12, if (isStatic) 623000 else 8000, isStatic)
        val metadata = SoundSampleMetadata(12, rawResId, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)

        return sample
    }

    private fun getAudioTrack(sample: SoundSample): AudioTrack =
        SoundSample::class.members.find {
            it.isAccessible = true
            it.name == "audioTrack"
        }!!.call(sample) as AudioTrack
}