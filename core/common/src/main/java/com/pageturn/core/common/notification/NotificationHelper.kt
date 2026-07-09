package com.pageturn.core.common.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object NotificationHelper {

    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reminderJob: Job? = null

    const val CHANNEL_ID = "pageturn_daily_reminder"
    const val CHANNEL_NAME = "Nhắc nhở đọc sách"
    const val CHANNEL_DESC = "Thông báo nhắc bạn đọc sách mỗi ngày"
    const val WORK_NAME_DAILY = "pageturn_daily_reminder_work"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESC
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Schedule daily reminder — fires every 24h from now.
     * Safe to call multiple times (ExistingPeriodicWorkPolicy.UPDATE re-queues if needed).
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        createChannel(context)

        val calendar = java.util.Calendar.getInstance()
        val nowMillis = calendar.timeInMillis

        val targetCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        if (targetCalendar.timeInMillis <= nowMillis) {
            targetCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = targetCalendar.timeInMillis - nowMillis

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleIntervalReminder(context: Context, intervalVal: Int, intervalUnit: String, isRescheduling: Boolean = false) {
        if (!isRescheduling) {
            cancelDailyReminder(context)
        }
        createChannel(context)

        val durationMs = when (intervalUnit) {
            "seconds" -> intervalVal.toLong() * 1000
            "minutes" -> intervalVal.toLong() * 60 * 1000
            "hours" -> intervalVal.toLong() * 60 * 60 * 1000
            else -> intervalVal.toLong() * 60 * 1000
        }

        val isShortInterval = intervalUnit == "seconds" || (intervalUnit == "minutes" && intervalVal < 15)

        if (isShortInterval) {
            reminderJob?.cancel()
            reminderJob = helperScope.launch {
                while (isActive) {
                    delay(durationMs)
                    showDailyReminderNotification(context)
                }
            }
        } else {
            val intervalMins = when (intervalUnit) {
                "hours" -> intervalVal.toLong() * 60
                else -> intervalVal.toLong()
            }
            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(intervalMins, TimeUnit.MINUTES)
                .addTag("reminder_tag")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_DAILY,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        reminderJob?.cancel()
        reminderJob = null
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY)
        WorkManager.getInstance(context).cancelAllWorkByTag("reminder_tag")
    }

    /**
     * Actually posts the notification. Called from DailyReminderWorker.
     */
    fun showDailyReminderNotification(context: Context) {
        createChannel(context)
        // Launch app on tap
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = listOf(
            "📖 Hôm nay bạn đã đọc sách chưa? Tiếp tục nhé!",
            "✨ Mỗi trang sách là một hành trình mới. Mở Libra nào!",
            "🌟 Bạn đang đọc gì hôm nay? Libra đang chờ bạn!",
            "📚 Thói quen đọc sách mỗi ngày giúp bạn phát triển không ngừng.",
            "🎯 Chỉ cần 20 phút đọc sách mỗi ngày — hãy bắt đầu ngay!"
        )
        val message = messages.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Libra — Nhắc nhở đọc sách")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Check permission on Android 13+
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted — user can grant via Settings
        }
    }
}
