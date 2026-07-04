package com.pageturn.core.common.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import kotlinx.coroutines.flow.first

/**
 * WorkManager Worker that fires the daily reading reminder notification.
 * Uses @HiltWorker so Hilt can inject dependencies if needed in the future.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesDataSource: UserPreferencesDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            NotificationHelper.showDailyReminderNotification(context)

            // Reschedule short-interval reminders (sub-15 minutes)
            val settings = preferencesDataSource.userSettings.first()
            if (settings.dailyNotify && settings.reminderMode == "interval") {
                val isShortInterval = settings.reminderIntervalUnit == "seconds" || 
                        (settings.reminderIntervalUnit == "minutes" && settings.reminderIntervalVal < 15)
                if (isShortInterval) {
                    NotificationHelper.scheduleIntervalReminder(context, settings.reminderIntervalVal, settings.reminderIntervalUnit, isRescheduling = true)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
