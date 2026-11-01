package com.luleme.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luleme.android.data.RecordRepository
import com.luleme.android.data.SettingsStore
import com.luleme.android.logic.DailyReminderConfig
import com.luleme.android.logic.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = SettingsStore.getInstance(context)
                val scheduler = ReminderScheduler.getInstance(context)
                val snap = store.snapshot()
                if (snap.reminderEnabled) {
                    scheduler.applyDaily(
                        DailyReminderConfig(
                            hour = snap.reminderHour,
                            minute = snap.reminderMinute,
                            text = snap.reminderText
                        )
                    )
                }
                if (snap.geshenEnabled) {
                    val last = RecordRepository.getInstance(context).getAll().firstOrNull()
                    if (last != null) {
                        scheduler.syncGeshenFromLastRecord(last.timestamp)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
