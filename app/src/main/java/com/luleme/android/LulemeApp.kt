package com.luleme.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.luleme.android.logic.ReminderScheduler

class LulemeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionLockState.lock()
        ReminderScheduler.getInstance(this).ensureChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                SessionLockState.lock()
            }
        })
    }
}
