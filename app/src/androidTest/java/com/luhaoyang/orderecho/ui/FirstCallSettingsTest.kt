package com.luhaoyang.orderecho.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withText
import android.os.SystemClock
import androidx.test.uiautomator.UiDevice
import com.luhaoyang.orderecho.calls.CallDiagnostics
import com.luhaoyang.orderecho.calls.CallDiagnosticEvent
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luhaoyang.orderecho.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallSettingsTest {
    @get:Rule val environment = MainActivityTestEnvironment("first-call-settings")

    @Test fun diagnosticOverlayIsVisibleAndRemovedOnStopAndReportCanBeCleared() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("first_call_diagnostics", Context.MODE_PRIVATE)
        val original = prefs.getString("events", null)
        val device = UiDevice.getInstance(instrumentation)
        assertTrue("Enable overlay permission before running this test", Settings.canDrawOverlays(context))
        try {
            CallDiagnostics.clear(context)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.settings_tab)).perform(click())
                onView(withId(R.id.test_call_overlay)).perform(scrollTo(), click())
                assertTrue(waitForOverlay(device, true))
                scenario.moveToState(Lifecycle.State.CREATED)
                assertTrue(waitForOverlay(device, false))
                scenario.moveToState(Lifecycle.State.RESUMED)
                onView(withId(R.id.show_call_diagnostics)).perform(scrollTo(), click())
                onView(withText(containsString(CallDiagnosticEvent.TEST_ADDED.label))).check(matches(isDisplayed()))
                instrumentation.runOnMainSync {
                    CallDiagnostics.record(context, CallDiagnosticEvent.HISTORY_UNKNOWN)
                }
                onView(withText(containsString(CallDiagnosticEvent.HISTORY_UNKNOWN.label))).check(matches(isDisplayed()))
                onView(withText(R.string.first_call_diagnostics_clear)).perform(click())
                assertFalse(CallDiagnostics.report(context).contains(CallDiagnosticEvent.TEST_ADDED.label))
                assertTrue(CallDiagnostics.report(context).contains(CallDiagnosticEvent.CLEARED.label))
            }
        } finally {
            prefs.edit().putString("events", original).commit()
        }
    }

    // Overlay deliberately hides from accessibility; inspect real system windows instead.
    private fun waitForOverlay(device: UiDevice, present: Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + 3000
        do {
            val windows = device.executeShellCommand("dumpsys window windows")
            if (windows.contains("ty=2038") == present) return true
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    @Test fun unrelatedPermissionResultDoesNotReplaceSettingsWithStorageRecovery() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        MainActivity.STORAGE_PERMISSIONS.forEach {
            instrumentation.uiAutomation.executeShellCommand("pm grant $packageName $it").close()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.settings_tab)).perform(click())
            onView(withId(R.id.first_call_permission_status)).check(matches(isDisplayed()))
            // Before the feature, every callback other than code 41 replaced the screen.
            scenario.onActivity { it.onRequestPermissionsResult(999, emptyArray(), intArrayOf()) }
            onView(withId(R.id.first_call_permission_status)).check(matches(isDisplayed()))
        }
    }
}
