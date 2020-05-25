package com.olekdia.soundpool

import android.content.Context
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.olekdia.sample.MainActivity
import com.olekdia.sample.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@LargeTest
class SoundSampleTest {
    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    private val context: Context get() = activityTestRule.activity


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
    fun loadStaticSample_stateCorrect() {
        val sample = SoundSample(12, 123, false)
        val metadata = SoundSampleMetadata(12, R.raw.bg_sea_retain, null)
        val descr = SoundSampleDescriptor(context, metadata)

        sample.load(
            descr.fileDescriptor,
            descr.fileOffset,
            descr.fileSize
        )
        assertFalse(sample.isClosed)
        assertTrue(sample.isLoaded())
        assertFalse(sample.isPlaying())
        assertFalse(sample.isPaused())
        assertTrue(sample.isStopped())
        assertEquals(12, sample.id)
    }
}