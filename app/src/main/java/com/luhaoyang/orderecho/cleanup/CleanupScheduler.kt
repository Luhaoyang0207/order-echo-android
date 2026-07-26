package com.luhaoyang.orderecho.cleanup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CleanupScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS).build()
        )
    }

    private const val UNIQUE_WORK_NAME = "recording-retention-cleanup"
}
