package com.luhaoyang.orderecho.cleanup

import android.content.Context
import android.os.Environment
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingRepository
import java.io.File

class CleanupWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val recordingsDirectory = File(Environment.getExternalStorageDirectory(), "Sounds/Callrecord")
        RetentionCleaner(RecordingRepository(recordingsDirectory), AppSettings(applicationContext)).clean()
        return Result.success()
    }
}
