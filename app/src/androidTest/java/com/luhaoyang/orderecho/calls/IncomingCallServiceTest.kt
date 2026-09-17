package com.luhaoyang.orderecho.calls

import android.os.Build
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
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
            }
        } finally {
            instrumentation.runOnMainSync {
                IncomingCalls.session.idle()
                IncomingCalls.service?.finishCall()
            }
        }
    }
}
