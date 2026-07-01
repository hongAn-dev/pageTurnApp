package com.pageturn.core.common.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker that fires the daily reading reminder notification.
 * Uses @HiltWorker so Hilt can inject dependencies if needed in the future.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            NotificationHelper.showDailyReminderNotification(context)
            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times if something fails
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
