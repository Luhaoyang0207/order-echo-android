package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.luhaoyang.orderecho.R
import org.junit.Test

class MainActivityNavigationTest {
    @Test
    fun settingsTabReplacesRecordingListInsteadOfOverlayingIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName android.permission.READ_EXTERNAL_STORAGE"
        ).close()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE"
        ).close()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.settings_tab)).perform(click())

            onView(withId(R.id.retention_choices)).check(matches(isDisplayed()))
            onView(withId(R.id.recording_list)).check(doesNotExist())
        }
    }
}
