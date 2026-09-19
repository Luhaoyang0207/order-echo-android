package com.luhaoyang.orderecho.calls

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ComponentName
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallMonitoringTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun enableRegistersOnceAndDisableInvalidatesPendingCall() {
        val prefs = context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE)
        val old = prefs.getBoolean("enabled", false)
        val trace = context.getSharedPreferences("first_call_diagnostics", Context.MODE_PRIVATE)
        val events = trace.getString("events", null)
        try {
            instrumentation.runOnMainSync { CallMonitoring.disable(context) }
            awaitRunning(false)
            CallDiagnostics.clear(context)
            instrumentation.runOnMainSync { assertTrue(CallMonitoring.enable(context)) }
            awaitRunning(true)
            instrumentation.runOnMainSync { assertTrue(CallMonitoring.restoreIfEnabled(context)) }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(250)
            assertEquals(1, trace.getString("events", "")!!.lines().count { it.endsWith("|MONITOR_READY") })
            instrumentation.runOnMainSync {
                IncomingCalls.session.idle()
                val pending = IncomingCalls.session.ringing("92345671", System.currentTimeMillis())!!
                CallMonitoring.disable(context)
                assertFalse(IncomingCalls.session.isCurrent(pending))
                assertFalse(CallMonitoring.isEnabled(context))
            }
            awaitRunning(false)
            assertFalse(CallMonitoring.restoreIfEnabled(context))
        } finally {
            instrumentation.runOnMainSync { CallMonitoring.disable(context) }
            awaitRunning(false)
            prefs.edit().putBoolean("enabled", old).commit()
            trace.edit().putString("events", events).commit()
        }
    }

    @Test fun recoveryStartsOnlyForEnabledSettingAndRecognizedSystemEvent() {
        val prefs = context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE)
        val old = prefs.getBoolean("enabled", false)
        var starts = 0
        val recordingContext = object : ContextWrapper(context) {
            override fun startForegroundService(service: Intent): ComponentName {
                starts++
                return service.component!!
            }
        }
        try {
            prefs.edit().putBoolean("enabled", false).commit()
            CallMonitoringRecoveryReceiver().onReceive(recordingContext, Intent(Intent.ACTION_BOOT_COMPLETED))
            assertEquals(0, starts)
            prefs.edit().putBoolean("enabled", true).commit()
            CallMonitoringRecoveryReceiver().onReceive(recordingContext, Intent("unrelated"))
            assertEquals(0, starts)
            CallMonitoringRecoveryReceiver().onReceive(recordingContext, Intent(Intent.ACTION_BOOT_COMPLETED))
            CallMonitoringRecoveryReceiver().onReceive(recordingContext, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
            assertEquals(2, starts)
        } finally {
            prefs.edit().putBoolean("enabled", old).commit()
        }
    }

    private fun awaitRunning(expected: Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5000
        do {
            if (CallMonitoringService.isRunning == expected) return
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Monitor running state did not become $expected")
    }
}
