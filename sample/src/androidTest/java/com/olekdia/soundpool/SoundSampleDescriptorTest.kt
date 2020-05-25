package com.olekdia.soundpool

import android.content.Context
import androidx.test.rule.ActivityTestRule
import com.olekdia.sample.MainActivity
import com.olekdia.sample.R
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class SoundSampleDescriptorTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    private val context: Context get() = activityTestRule.activity

    @Test
    fun createRawRes_descriptorIsValid() {
        val metadata = SoundSampleMetadata(12, R.raw.sec_tick_bird_ouzel, null)
        val descr = SoundSampleDescriptor(context, metadata)

        assertNotEquals(0, descr.fileSize)
    }
}