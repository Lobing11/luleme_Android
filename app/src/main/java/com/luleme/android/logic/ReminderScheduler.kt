package com.luleme.android.logic

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.luleme.android.MainActivity
import com.luleme.android.data.SettingsStore
import com.luleme.android.receiver.AlarmReceiver
import java.util.Calendar

const val DEFAULT_REMINDER_HOUR: Int = 21
const val DEFAULT_REMINDER_MINUTE: Int = 0
const val DEFAULT_REMINDER_TEXT: String = "是时候鹿了！"
const val GESHEN_TEXT: String = "想变得和葛神一样强大吗？快来鹿吧！"

private const val GESHEN_DELAY_HOURS: Int = 8
private const val QUIET_END_HOUR: Int = 8
private const val QUIET_START_HOUR: Int = 23
private const val CHANNEL_ID: String = "luleme_reminders"
private const val TAG: String = "Reminder"

const val EXTRA_KIND: String = "kind"
const val EXTRA_TEXT: String = "text"
const val KIND_DAILY: String = "daily"
const val KIND_GESHEN: String = "geshen"
const val REQ_DAILY: Int = 1001
const val REQ_GESHEN: Int = 1002

data class DailyReminderConfig(
    val hour: Int,
    val minute: Int,
    val text: String
)

class ReminderScheduler private constructor(private val appContext: Context) {

    @Volatile
    var lastErrorCode: Int = 0

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    suspend fun applyDaily(config: DailyReminderConfig): Boolean {
        cancelDaily()
        return try {
            ensureChannel()
            val fireAt = nextDailyTrigger(config.hour, config.minute)
            scheduleExact(REQ_DAILY, fireAt, KIND_DAILY, config.text)
            SettingsStore.getInstance(appContext).setReminderIds(listOf(REQ_DAILY))
            lastErrorCode = 0
            true
        } catch (e: Exception) {
            lastErrorCode = e.hashCode()
            Log.e(TAG, "publish daily failed", e)
            false
        }
    }

    suspend fun cancelDaily() {
        cancelAlarm(REQ_DAILY)
        SettingsStore.getInstance(appContext).setReminderIds(emptyList())
    }

    suspend fun onRecordCreated(recordTs: Long) {
        val store = SettingsStore.getInstance(appContext)
        if (!store.isGeshenEnabled()) return
        cancelGeshen()
        val fireAt = geshenFireTime(recordTs)
        if (fireAt <= System.currentTimeMillis()) return
        try {
            ensureChannel()
            scheduleExact(REQ_GESHEN, fireAt, KIND_GESHEN, GESHEN_TEXT)
            store.setGeshenReminderId(REQ_GESHEN)
        } catch (_: Exception) {
            // 提醒是增强功能，失败不阻塞记录
        }
    }

    suspend fun cancelGeshen() {
        val store = SettingsStore.getInstance(appContext)
        val id = store.getGeshenReminderId()
        if (id > 0) {
            cancelAlarm(id)
            store.setGeshenReminderId(0)
        }
    }

    suspend fun syncGeshenFromLastRecord(lastRecordTs: Long) {
        val store = SettingsStore.getInstance(appContext)
        if (!store.isGeshenEnabled()) {
            cancelGeshen()
            return
        }
        if (lastRecordTs > 0) {
            onRecordCreated(lastRecordTs)
        }
    }

    suspend fun rescheduleFromSettings() {
        val store = SettingsStore.getInstance(appContext)
        val snap = store.snapshot()
        if (snap.reminderEnabled) {
            applyDaily(
                DailyReminderConfig(
                    hour = snap.reminderHour,
                    minute = snap.reminderMinute,
                    text = snap.reminderText
                )
            )
        } else {
            cancelDaily()
        }
        // Geshen is re-applied by caller with last record timestamp when needed.
    }

    private fun scheduleExact(requestCode: Int, triggerAt: Long, kind: String, text: String) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TEXT, text)
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                throw SecurityException("exact alarm not allowed")
            }
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun cancelAlarm(requestCode: Int) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    companion object {
        @Volatile
        private var instance: ReminderScheduler? = null

        fun getInstance(context: Context): ReminderScheduler {
            return instance ?: synchronized(this) {
                instance ?: ReminderScheduler(context.applicationContext).also { instance = it }
            }
        }

        fun geshenFireTime(recordTs: Long): Long {
            val fire = Calendar.getInstance().apply {
                timeInMillis = recordTs + GESHEN_DELAY_HOURS * 3600L * 1000L
            }
            val hour = fire.get(Calendar.HOUR_OF_DAY)
            if (hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR) {
                val next = Calendar.getInstance().apply { timeInMillis = fire.timeInMillis }
                if (hour >= QUIET_START_HOUR) {
                    next.add(Calendar.DAY_OF_MONTH, 1)
                }
                next.set(Calendar.HOUR_OF_DAY, QUIET_END_HOUR)
                next.set(Calendar.MINUTE, 0)
                next.set(Calendar.SECOND, 0)
                next.set(Calendar.MILLISECOND, 0)
                return next.timeInMillis
            }
            return fire.timeInMillis
        }

        fun nextDailyTrigger(hour: Int, minute: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return cal.timeInMillis
        }

        fun activityPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        const val NOTIFICATION_CHANNEL_ID: String = CHANNEL_ID
    }
}
