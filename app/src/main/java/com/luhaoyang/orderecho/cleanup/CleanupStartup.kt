package com.luhaoyang.orderecho.cleanup

import android.content.Context

class CleanupStartup(
    private val schedule: (Context) -> Unit = CleanupScheduler::schedule
) {
    fun initialize(context: Context) {
        schedule(context)
    }
}
