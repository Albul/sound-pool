package com.olekdia.soundpool

import android.os.Handler
import android.os.Looper
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class PlayIdlingResource(private val pool: SoundPoolCompat) : IdlingResource, Runnable {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val isIdleNow = AtomicBoolean(true)
    private val handler = Handler(Looper.getMainLooper())
    private var initActivePlaying: Int = pool.playThreadPool.activeCount

    init {
        setIdleState(false)
        IdlingRegistry.getInstance().register(this)

        run()
    }

    override fun run() {
        if (pool.playThreadPool.activeCount - initActivePlaying == 1) {
            setIdleState(true)
            IdlingRegistry.getInstance().unregister(this)
        } else {
            handler.postDelayed(this, 100)
        }
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