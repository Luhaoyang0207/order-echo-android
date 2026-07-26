package com.luhaoyang.orderecho.cleanup

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertSame
import org.junit.Test

class CleanupStartupTest {
    @Test
    fun appStartupRequestsDailyCleanupScheduling() {
        val context = ContextWrapper(null)
        var scheduledContext: Context? = null

        CleanupStartup { scheduledContext = it }.initialize(context)

        assertSame(context, scheduledContext)
    }
}
