package com.olekdia.soundpool

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class LoadIdlingResource : IdlingResource {

    @Volatile
    private var mCallback: IdlingResource.ResourceCallback? = null

    private val mIsIdleNow = AtomicBoolean(true)

    override fun getName(): String {
        return this.javaClass.name
    }

    override fun isIdleNow(): Boolean {
        return mIsIdleNow.get()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        mCallback = callback
    }

    fun setIdleState(isIdleNow: Boolean) {
        mIsIdleNow.set(isIdleNow)
        if (isIdleNow) {
            mCallback?.onTransitionToIdle()
        }
    }
}