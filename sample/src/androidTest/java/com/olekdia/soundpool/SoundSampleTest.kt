package com.olekdia.soundpool

import android.content.Context
import android.media.AudioTrack
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.androidcommon.NO_RESOURCE
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

//--------------------------------------------------------------------------------------------------
//  Load methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun createSample_initialStateCorrect() {
        val sample = SoundSample(12, 123, true)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
        assertEquals(12, sample.id)
    }

    @Test
    fun static_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val sample = SoundSample(12, 1234567, true)
        val metadata = SoundSampleMetadata(12, R.raw.sec_tick_bird_ouzel, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
        assertEquals(12, sample.id)
    }

    @Test
    fun stream_loadSample_oggFile_sampleIsLoadedAndStopped() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
        assertEquals(12, sample.id)
    }

    @Test
    fun stream_loadSample_mp3File_sampleIsLoadedAndStopped() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.dyathon_hope, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
        assertEquals(12, sample.id)
    }

    @Test
    fun stream_loadSample_closeSample_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_loadSample_closeFewTimes_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        sample.close()
        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_loadSample_notAudioFile_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, false)
        val metadata = SoundSampleMetadata(12, R.raw.design_patterns_pdf, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun static_loadSample_notAudioFile_sampleIsClosed() {
        val sample = SoundSample(12, 1234567, true)
        val metadata = SoundSampleMetadata(12, R.raw.design_patterns_pdf, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        sample.close()
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_loadSample_fromHttps_sampleIsLoadedAndPlaying() {
        val sample = SoundSample(12, 10000, false)
        val metadata = SoundSampleMetadata(12, NO_RESOURCE, "https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg")
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(1000)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
        sample.stop()
    }

    @Test
    fun static_loadSample_fromHttps_sampleIsLoadedAndPlaying() {
        val sample = SoundSample(12, 100000000, true)
        val metadata = SoundSampleMetadata(12, NO_RESOURCE, "https://olekdia.com/a/prana_breath/soundfiles/lp_white_stork_1.ogg")
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(1000)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
        sample.stop()
    }

    @Test
    fun stream_loadSample_fromHttps_notAudioFile_sampleIsClosed() {
        val sample = SoundSample(12, 10000, false)
        val metadata = SoundSampleMetadata(12, NO_RESOURCE, "https://pranabreath.info/images/6/6b/Vajrasana_pos.png")
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
    }

    @Test
    fun stream_loadSample_fromEmptyPathString_sampleIsClosed() {
        val sample = SoundSample(12, 10000, false)
        val metadata = SoundSampleMetadata(12, NO_RESOURCE, "")
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
    }

    @Test
    fun stream_loadSample_fromNullPathString_sampleIsClosed() {
        val sample = SoundSample(12, 10000, false)
        val metadata = SoundSampleMetadata(12, NO_RESOURCE, "")
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertFalse(success)
        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
    }

//--------------------------------------------------------------------------------------------------
//  Play methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun stream_loadThenPlayThenClose_loadThenPlay_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.close()
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
        assertTrue(sample.isClosed)

        // Load #2
        val metadata = SoundSampleMetadata(12, R.raw.bg_wind_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        val success = sample.load(descr)
        assertTrue(success)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isClosed)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
    }

    @Test
    fun playPauseStopResumeWithoutLoading_sampleIsUninitialized() {
        val sample = SoundSample(12, 1234567, false)

        assertFalse(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.pause()
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.resume(playThreadPool)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_playInThread_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        val t = object : Thread() {
            override fun run() {
                sample.play(1f, 1f, 1f)
            }
        }
        t.start()
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
    }

    @Test
    fun static_playInThread_sampleIsPlaying_andStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        val t = object : Thread() {
            override fun run() {
                sample.play(1f, 1f, 1f)
            }
        }
        t.start()
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
        assertFalse(t.isAlive)
    }

    @Test
    fun stream_playStopImmediately_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.stop()
        Thread.sleep(PLAY_TIMEOUT)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playStopImmediately_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.stop()
        Thread.sleep(PLAY_TIMEOUT)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playThenPauseImmediately_sampleIsPaused() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()

        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_playThenPauseThenStopThenPauseImmediately_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()
        sample.stop()
        sample.pause()

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playThenPlay_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        val result1 = sample.play(1f, 1f, 0, 1f, playThreadPool)
        assertTrue(sample.isPlaying)
        assertTrue(result1)

        val result2 = sample.play(.5f, .5f, -1, 1f, playThreadPool)
        assertTrue(sample.isPlaying)
        assertFalse(result2)

        Thread.sleep(300)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playThenPauseThenStopThenPauseImmediately_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()
        sample.stop()
        sample.pause()

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playShortSoundOnce_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying)

        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playShortSoundTwice_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playShortSoundTwice_sampleIsStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playLoop_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, -1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_playLoop_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, -1, 1f, playThreadPool)
        Thread.sleep(50)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_playWaitUntilStop_playAgain_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        val playResult1 = sample.play(1f, 1f, 0, 1f, playThreadPool)
        assertTrue(playResult1)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isStopped)

        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        val playResult2 = sample.play(1f, 1f, 0, 1f, playThreadPool)
        assertTrue(playResult2)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

    @Test
    fun stream_playWaitUntilStop_playAgain_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        val playResult1 = sample.play(1f, 1f, 0, 1f, playThreadPool)
        assertTrue(playResult1)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isStopped)

        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        val playResult2 = sample.play(1f, 1f, 0, 1f, playThreadPool)
        assertTrue(playResult2)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)
    }

//--------------------------------------------------------------------------------------------------
//  Resume methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun stream_playThenResume_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_playThenResume_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun stream_loadThenResume_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.bg_wind_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_loadThenResume_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playThenPauseThenResume_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val pauseResult = sample.pause()
        assertTrue(pauseResult)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)

        val resumeResult = sample.resume(playThreadPool)
        assertTrue(resumeResult)
        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_playThenPauseThenResume_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_wind_retain, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val pauseResult = sample.pause()
        assertTrue(pauseResult)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)

        val resumeResult = sample.resume(playThreadPool)
        assertTrue(resumeResult)
        assertFalse(sample.isPaused)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_playThenPauseThenResumeThenResume_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val pauseResult = sample.pause()
        assertTrue(pauseResult)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)

        val resumeResult1 = sample.resume(playThreadPool)
        assertTrue(resumeResult1)
        assertFalse(sample.isPaused)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val resumeResult2 = sample.resume(playThreadPool)
        assertFalse(resumeResult2)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun stream_playThenPauseThenStopThenResume_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val pauseResult = sample.pause()
        assertTrue(pauseResult)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)

        val stopResult = sample.stop()
        assertTrue(stopResult)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        assertFalse(sample.isPaused)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playThenPauseThenStopThenResume_sampleIsStopped() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)

        val pauseResult = sample.pause()
        assertTrue(pauseResult)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)

        val stopResult = sample.stop()
        assertTrue(stopResult)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertFalse(resumeResult)
        assertFalse(sample.isPaused)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPaused)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_playThenPauseThenResumeImmediately_sampleIsPlayingAndStoppedAfter() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()

        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertTrue(resumeResult)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(200)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playThenPauseThenResumeImmediately_sampleIsPlaying() {
        val sample = createSampleAndLoad(R.raw.bg_sunrise_inhale, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        sample.pause()

        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertFalse(sample.isPlaying)
        assertTrue(sample.isPaused)
        assertFalse(sample.isStopped)

        val resumeResult = sample.resume(playThreadPool)
        assertTrue(resumeResult)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        Thread.sleep(200)
        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
    }

//--------------------------------------------------------------------------------------------------
//  Set methods
//--------------------------------------------------------------------------------------------------

    @Test
    fun setLoopToNotLoaded_invalidReturn() {
        val sample = SoundSample(12, 8000, true)

        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
        val result = sample.setLoop(1)
        assertEquals(AudioTrack.ERROR_INVALID_OPERATION, result)

        assertFalse(sample.isPlaying)
    }

    @Test
    fun setLoopBadValue_invalidReturn() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result1 = sample.setLoop(-2)
        assertEquals(AudioTrack.ERROR_BAD_VALUE, result1)
        val result2 = sample.setLoop(-1232)
        assertEquals(AudioTrack.ERROR_BAD_VALUE, result2)
        Thread.sleep(300)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_playShortOnce_setLoopForever_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setLoop(-1)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
    }

    @Test
    fun static_playShortOnce_setLoopForever_sampleIsPlayingContinuously() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setLoop(-1)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        Thread.sleep(300)
        assertTrue(sample.isPlaying)
        Thread.sleep(300)

        assertTrue(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertFalse(sample.isStopped)

        sample.stop()
    }

    @Test
    fun static_playShortOnce_setLoopTwice_sampleIsPlayedTwice() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setLoop(1)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(200)

        assertTrue(sample.isPlaying)
        Thread.sleep(150)
        assertTrue(sample.isPlaying)
        Thread.sleep(200)

        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun static_play_setRate_sampleIsPlayingTwiceFast() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setRate(2f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_play_setRate_sampleIsPlayingTwiceFast() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setRate(2f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertFalse(sample.isPlaying)
        assertFalse(sample.isPaused)
        assertTrue(sample.isStopped)
    }

    @Test
    fun stream_play_setRateBadValue_invalidReturn() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result1 = sample.setRate(-20f)
        assertEquals(AudioTrack.ERROR_BAD_VALUE, result1)
        val result2 = sample.setRate(2220f)
        assertEquals(AudioTrack.ERROR_BAD_VALUE, result2)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun setRateToNotLoaded_invalidReturned() {
        val sample = SoundSample(12, 8000, true)

        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
        val result = sample.setRate(2f)
        assertEquals(AudioTrack.ERROR_INVALID_OPERATION, result)

        assertFalse(sample.isPlaying)
    }

    @Test
    fun stream_play_setVolume_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.bg_sea_retain, isStatic = false)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_play_setVolume_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result)
        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun static_play_setVolumeFewTimes_sampleIsPlaying_successReturned() {
        val sample = createSampleAndLoad(R.raw.sec_tick_cricket, isStatic = true)

        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded)

        sample.play(1f, 1f, 0, 1f, playThreadPool)
        Thread.sleep(PLAY_TIMEOUT)
        assertTrue(sample.isPlaying)
        val result1 = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result1)
        val result2 = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.SUCCESS, result2)
        val result3 = sample.setVolume(.8f, .8f)
        assertEquals(AudioTrack.SUCCESS, result3)

        Thread.sleep(PLAY_TIMEOUT)

        assertTrue(sample.isPlaying)
        Thread.sleep(100)
        assertTrue(sample.isPlaying)

        sample.stop()
    }

    @Test
    fun setVolumeToNotLoaded_invalidReturned() {
        val sample = SoundSample(12, 8000, true)

        assertTrue(sample.isClosed)
        assertFalse(sample.isLoaded)
        val result = sample.setVolume(.5f, .5f)
        assertEquals(AudioTrack.ERROR_INVALID_OPERATION, result)

        assertFalse(sample.isPlaying)
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