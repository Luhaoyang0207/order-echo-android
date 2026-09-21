package com.luhaoyang.orderecho.calls

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.luhaoyang.orderecho.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in on an isolated emulator with an emulated GSM call, never on the restaurant phone. */
@RunWith(AndroidJUnit4::class)
class IncomingCallServiceTest {
    @Test fun staleQueuedStartCannotStopTheNextRingingCall() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(InstrumentationRegistry.getArguments().getString("testEmulatedCall") == "true")
        assumeTrue(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        assumeTrue(FirstCallPermissions.ready(context))
        @Suppress("DEPRECATION")
        val ringing = context.getSystemService(TelephonyManager::class.java).callState == TelephonyManager.CALL_STATE_RINGING
        assumeTrue(ringing)
        val monitor = context.getSharedPreferences("call_monitoring", android.content.Context.MODE_PRIVATE)
        val wasEnabled = monitor.getBoolean("enabled", false)
        monitor.edit().putBoolean("enabled", true).commit()
        try {
            instrumentation.runOnMainSync {
                IncomingCalls.service?.finishCall()
                IncomingCalls.session.idle()
                val old = IncomingCalls.session.ringing("92345110", System.currentTimeMillis())!!
                context.startForegroundService(IncomingCallService.intent(context, old))
                IncomingCalls.session.idle()
                val next = IncomingCalls.session.ringing("92345111", System.currentTimeMillis())!!
                context.startForegroundService(IncomingCallService.intent(context, next))
            }
            // Allow both queued starts and the empty test-emulator history query to finish.
            Thread.sleep(2_000)
            instrumentation.runOnMainSync {
                assertNotNull("The new first-call overlay must still own a live service", IncomingCalls.service)
                assertTrue("The locked-screen hint should post before cleanup", FirstCallLockedHint.show(context))
            }
            assertTrue("The locked-screen hint should be visible before cleanup", waitForLockedHint(context, true))
            instrumentation.runOnMainSync { IncomingCalls.service?.finishCall() }
            assertTrue("Finishing the call must remove the locked-screen hint", waitForLockedHint(context, false))
        } finally {
            monitor.edit().putBoolean("enabled", wasEnabled).commit()
            instrumentation.runOnMainSync {
                IncomingCalls.session.idle()
                IncomingCalls.service?.finishCall()
            }
        }
    }

    private fun waitForLockedHint(context: Context, expected: Boolean): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        repeat(20) {
            val showing = manager.activeNotifications.any {
                it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) == context.getString(R.string.first_call_label)
            }
            if (showing == expected) return true
            Thread.sleep(100)
        }
        return false
    }
}
