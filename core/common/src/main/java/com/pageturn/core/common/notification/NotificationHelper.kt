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

object NotificationHelper {

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
    fun scheduleDailyReminder(context: Context) {
        createChannel(context)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            // Minimum interval for testing: swap to 15 minutes for quick test
            // PeriodicWorkRequestBuilder<DailyReminderWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(10, TimeUnit.SECONDS) // fire 10s after enable for easy testing
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY)
    }

    /**
     * Actually posts the notification. Called from DailyReminderWorker.
     */
    fun showDailyReminderNotification(context: Context) {
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
            "✨ Mỗi trang sách là một hành trình mới. Mở PageTurn nào!",
            "🌟 Bạn đang đọc gì hôm nay? PageTurn đang chờ bạn!",
            "📚 Thói quen đọc sách mỗi ngày giúp bạn phát triển không ngừng.",
            "🎯 Chỉ cần 20 phút đọc sách mỗi ngày — hãy bắt đầu ngay!"
        )
        val message = messages.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("PageTurn — Nhắc nhở đọc sách")
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
