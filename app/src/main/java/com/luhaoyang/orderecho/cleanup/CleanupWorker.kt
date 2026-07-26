package com.luhaoyang.orderecho.cleanup

import android.content.Context
import android.os.Environment
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingRepository
import java.io.File

class CleanupWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val recordingsDirectory = File(Environment.getExternalStorageDirectory(), "Sounds/Callrecord")
        val cleanup = runCatching {
            RetentionCleaner(RecordingRepository(recordingsDirectory), AppSettings(applicationContext)).clean()
        }.getOrElse {
            return Result.failure(workDataOf(FAILED_COUNT_KEY to 1))
        }
        val output = workDataOf(
            DELETED_COUNT_KEY to cleanup.deletedCount,
            FAILED_COUNT_KEY to cleanup.failedCount
        )
        return if (cleanup.failedCount == 0) Result.success(output) else Result.failure(output)
    }

    private companion object {
        const val DELETED_COUNT_KEY = "deleted_count"
        const val FAILED_COUNT_KEY = "failed_count"
    }
}
