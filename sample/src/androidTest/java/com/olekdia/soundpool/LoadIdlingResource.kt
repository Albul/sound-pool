package com.olekdia.soundpool

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class LoadIdlingResource : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val isIdleNow = AtomicBoolean(true)

    private var soundPool: SoundPoolCompat? = null

    constructor(pool: SoundPoolCompat) {
        if (soundPool == null) {
            soundPool = pool
        }

        soundPool?.setOnLoadCompleteListener(
            object : SoundPoolCompat.OnLoadCompleteListener {
                override fun onLoadComplete(
                    soundPool: SoundPoolCompat,
                    sampleId: Int,
                    isSuccess: Boolean,
                    errorMsg: String?
                ) {
                    setIdleState(true)
                    IdlingRegistry.getInstance().unregister(this@LoadIdlingResource)
                }
            }
        )

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