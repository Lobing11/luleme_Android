package com.luleme.android.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.luleme.android.R
import com.luleme.android.data.SettingsStore
import com.luleme.android.logic.DailyReminderConfig
import com.luleme.android.logic.EXTRA_KIND
import com.luleme.android.logic.EXTRA_TEXT
import com.luleme.android.logic.KIND_DAILY
import com.luleme.android.logic.KIND_GESHEN
import com.luleme.android.logic.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val title = when (kind) {
            KIND_GESHEN -> "葛神模式"
            else -> "鹿了么"
        }
        ReminderScheduler.getInstance(context).ensureChannel()
        val notification = NotificationCompat.Builder(
            context,
            ReminderScheduler.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(ReminderScheduler.activityPendingIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(kind.hashCode(), notification)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler.getInstance(context)
                val store = SettingsStore.getInstance(context)
                when (kind) {
                    KIND_DAILY -> {
                        if (store.isReminderEnabled()) {
                            scheduler.applyDaily(
                                DailyReminderConfig(
                                    hour = store.getReminderHour(),
                                    minute = store.getReminderMinute(),
                                    text = store.getReminderText()
                                )
                            )
                        }
                    }
                    KIND_GESHEN -> {
                        store.setGeshenReminderId(0)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
