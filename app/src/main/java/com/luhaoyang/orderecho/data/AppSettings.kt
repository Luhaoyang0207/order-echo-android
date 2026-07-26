package com.luhaoyang.orderecho.data

import android.content.Context

data class LastCleanupResult(
    val completedAtMillis: Long,
    val deletedCount: Int,
    val failedCount: Int
)

class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun retentionDays(): Int = preferences
        .getInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS)
        .takeIf { it in RetentionPolicy.allowedDays }
        ?: DEFAULT_RETENTION_DAYS

    fun setRetentionDays(days: Int) {
        require(days in RetentionPolicy.allowedDays)
        preferences.edit().putInt(RETENTION_DAYS_KEY, days).apply()
    }

    fun lastCleanupAt(): Long = lastCleanupResult().completedAtMillis

    fun setLastCleanupAt(epochMillis: Long) {
        preferences.edit().putLong(LAST_CLEANUP_AT_KEY, epochMillis).apply()
    }

    fun lastCleanupResult(): LastCleanupResult = LastCleanupResult(
        completedAtMillis = preferences.getLong(LAST_CLEANUP_AT_KEY, NEVER_CLEANED),
        deletedCount = preferences.getInt(LAST_CLEANUP_DELETED_KEY, 0),
        failedCount = preferences.getInt(LAST_CLEANUP_FAILED_KEY, 0)
    )

    fun setLastCleanupResult(completedAtMillis: Long, deletedCount: Int, failedCount: Int) {
        require(deletedCount >= 0)
        require(failedCount >= 0)
        preferences.edit()
            .putLong(LAST_CLEANUP_AT_KEY, completedAtMillis)
            .putInt(LAST_CLEANUP_DELETED_KEY, deletedCount)
            .putInt(LAST_CLEANUP_FAILED_KEY, failedCount)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "order_echo_settings"
        const val RETENTION_DAYS_KEY = "retention_days"
        const val LAST_CLEANUP_AT_KEY = "last_cleanup_at"
        const val LAST_CLEANUP_DELETED_KEY = "last_cleanup_deleted"
        const val LAST_CLEANUP_FAILED_KEY = "last_cleanup_failed"
        const val DEFAULT_RETENTION_DAYS = 30
        const val NEVER_CLEANED = 0L
    }
}
