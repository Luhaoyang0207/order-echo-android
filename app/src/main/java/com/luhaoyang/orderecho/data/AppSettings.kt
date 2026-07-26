package com.luhaoyang.orderecho.data

import android.content.Context

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

    fun lastCleanupAt(): Long = preferences.getLong(LAST_CLEANUP_AT_KEY, NEVER_CLEANED)

    fun setLastCleanupAt(epochMillis: Long) {
        preferences.edit().putLong(LAST_CLEANUP_AT_KEY, epochMillis).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "order_echo_settings"
        const val RETENTION_DAYS_KEY = "retention_days"
        const val LAST_CLEANUP_AT_KEY = "last_cleanup_at"
        const val DEFAULT_RETENTION_DAYS = 30
        const val NEVER_CLEANED = 0L
    }
}
