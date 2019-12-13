package com.olekdia.soundpool

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

class PlayIdlingResource : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val isIdleNow = AtomicBoolean(true)

    private var workerExecutor: ThreadPoolExecutor? = null

    constructor(executor: ThreadPoolExecutor) {
        if (workerExecutor == null) {
            workerExecutor = executor
        }

        //todo add callback
//        setIdleState(true)
//        IdlingRegistry.getInstance().unregister(this@PlayIdlingResource)

        setIdleState(false)
        IdlingRegistry.getInstance().register(this)
    }

    override fun getName(): String {
        return this.javaClass.name
    }

    override fun isIdleNow(): Boolean {
        return isIdleNow.get()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    fun setIdleState(isIdle: Boolean) {
        isIdleNow.set(isIdle)
        if (isIdle) {
            callback?.onTransitionToIdle()
        }
    }
}