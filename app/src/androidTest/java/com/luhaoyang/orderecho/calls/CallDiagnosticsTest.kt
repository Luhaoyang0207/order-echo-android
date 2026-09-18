package com.luhaoyang.orderecho.calls

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallDiagnosticsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun missingPermissionStillLeavesRingingEvidenceWithoutCallerNumber() {
        val denied = object : ContextWrapper(context) {
            override fun checkPermission(permission: String, pid: Int, uid: Int) = PackageManager.PERMISSION_DENIED
        }
        val prefs = context.getSharedPreferences("first_call_diagnostics", Context.MODE_PRIVATE)
        val original = prefs.getString("events", null)
        try {
            CallDiagnostics.clear(context)
            instrumentation.runOnMainSync {
                IncomingCallReceiver().onReceive(denied,
                    Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                        .putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_RINGING)
                        .putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, "+4791234567"))
            }
            val report = CallDiagnostics.report(context)
            assertTrue(report.contains(CallDiagnosticEvent.RINGING.label))
            assertTrue(report.contains(CallDiagnosticEvent.PERMISSIONS_MISSING.label))
            assertFalse(report.contains("91234567"))
            assertFalse(prefs.all.toString().contains("91234567"))
        } finally {
            prefs.edit().putString("events", original).commit()
        }
    }

    @Test fun boundedEvidenceSurvivesNewContextAndClearRemovesIt() {
        val prefs = context.getSharedPreferences("first_call_diagnostics", Context.MODE_PRIVATE)
        val original = prefs.getString("events", null)
        try {
            CallDiagnostics.clear(context)
            repeat(50) { CallDiagnostics.record(context, CallDiagnosticEvent.RINGING) }
            CallDiagnostics.record(context, CallDiagnosticEvent.HISTORY_FIRST)
            val fresh = context.createPackageContext(context.packageName, 0)
            val report = CallDiagnostics.report(fresh)
            assertEquals(32, prefs.getString("events", "")!!.lines().size)
            assertTrue(report, report.contains(CallDiagnosticEvent.HISTORY_FIRST.label))
            CallDiagnostics.clear(context)
            assertFalse(CallDiagnostics.report(fresh).contains(CallDiagnosticEvent.HISTORY_FIRST.label))
        } finally {
            prefs.edit().putString("events", original).commit()
        }
    }
}
