package com.pageturn.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background Worker that pushes local data to the backend API.
 * Scheduled as periodic (every 6h) when autoSync is enabled.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cloudSyncManager: CloudSyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = cloudSyncManager.pushToCloud()) {
            is SyncResult.Success -> Result.success(
                workDataOf(
                    "books" to result.books,
                    "highlights" to result.highlights,
                    "syncTime" to System.currentTimeMillis()
                )
            )
            is SyncResult.Error -> {
                if (runAttemptCount < 2) Result.retry() else Result.failure(
                    workDataOf("error" to result.message)
                )
            }
        }
    }

    companion object {
        const val WORK_NAME = "pageturn_cloud_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS) // quick first run for testing
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun runOnce(context: Context): androidx.work.Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            return WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_manual",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
