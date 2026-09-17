package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
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
