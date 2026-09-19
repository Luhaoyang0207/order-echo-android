package com.luhaoyang.orderecho.calls

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallReceptionProbeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun explicitProbeRegistersOnceAndUnregistersAfterItsDeadline() {
        val prefs = context.getSharedPreferences("first_call_diagnostics", Context.MODE_PRIVATE)
        val original = prefs.getString("events", null)
        val intent = Intent(context, CallReceptionProbeService::class.java)
        try {
            context.stopService(intent)
            awaitStopped()
            check(prefs.edit().remove("events").commit())
            context.startForegroundService(intent)
            awaitEvent(CallDiagnosticEvent.PROBE_STARTED, 5000)
            awaitEvent(CallDiagnosticEvent.PROBE_RECEIVER_READY, 5000)
            awaitEvent(CallDiagnosticEvent.PROBE_LISTENER_READY, 5000)
            awaitEvent(CallDiagnosticEvent.PROBE_LISTENER_IDLE, 5000)
            context.startForegroundService(intent)
            awaitEvent(CallDiagnosticEvent.PROBE_ALREADY_RUNNING, 5000)
            val lines = prefs.getString("events", "")!!.lines()
            assertEquals(1, lines.count { it.endsWith("|PROBE_STARTED") })
            // Real 60-second lifetime, with no test-only shortened production timeout.
            awaitEvent(CallDiagnosticEvent.PROBE_TIMEOUT, 65000)
            awaitEvent(CallDiagnosticEvent.PROBE_STOPPED, 5000)
            val countAfterStop = prefs.getString("events", "")!!.lines().size
            instrumentation.waitForIdleSync()
            assertEquals(countAfterStop, prefs.getString("events", "")!!.lines().size)
        } finally {
            context.stopService(intent)
            awaitStopped()
            prefs.edit().putString("events", original).commit()
        }
    }

    @Suppress("DEPRECATION")
    private fun awaitStopped() {
        val deadline = SystemClock.elapsedRealtime() + 5000
        val manager = context.getSystemService(ActivityManager::class.java)
        do {
            if (manager.getRunningServices(Int.MAX_VALUE).none {
                    it.service.className == CallReceptionProbeService::class.java.name
                }) return
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Probe service did not stop")
    }

    private fun awaitEvent(event: CallDiagnosticEvent, timeout: Long) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        do {
            if (CallDiagnostics.report(context).contains(event.label)) return
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Missing ${event.name}: ${CallDiagnostics.report(context)}")
    }
}
