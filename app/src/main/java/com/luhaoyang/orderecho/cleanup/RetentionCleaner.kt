package com.luhaoyang.orderecho.cleanup

import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.data.RetentionPolicy
import java.time.LocalDate

data class CleanupResult(val deletedCount: Int, val failedCount: Int)

class RetentionCleaner(
    private val repository: RecordingRepository,
    private val settings: AppSettings,
    private val clock: () -> LocalDate = { LocalDate.now() }
) {
    fun clean(): CleanupResult {
        var deletedCount = 0
        var failedCount = 0
        val today = clock()
        val retentionDays = settings.retentionDays()

        repository.list()
            .filter { RetentionPolicy.isExpired(it.recordedAt.toLocalDate(), today, retentionDays) }
            .forEach { recording ->
                if (runCatching { repository.delete(recording) }.getOrDefault(false)) {
                    deletedCount++
                } else {
                    failedCount++
                }
            }

        settings.setLastCleanupAt(System.currentTimeMillis())
        return CleanupResult(deletedCount, failedCount)
    }
}
