package com.luhaoyang.orderecho.cleanup

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.ui.MainActivity
import com.luhaoyang.orderecho.ui.MainActivityTestEnvironment
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanupBoundaryTest {
    @get:Rule
    val activityEnvironment = MainActivityTestEnvironment("cleanup-boundary")

    @Test
    fun settingsShowExactlyFiveRetentionChoices() {
        grantStoragePermissions()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("设置")).perform(androidx.test.espresso.action.ViewActions.click())

            listOf("7 天", "30 天", "60 天", "90 天", "180 天").forEach {
                onView(withText(it)).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun cleanupDoesNotDeleteOutsideCallrecord() {
        val callrecordDirectory = activityEnvironment.recordingDirectory
        val outsideFile = File(activityEnvironment.testRoot, "outside_20260701_120000.amr").apply { writeText("amr") }
        val settings = AppSettings(InstrumentationRegistry.getInstrumentation().targetContext).also { it.setRetentionDays(7) }

        RetentionCleaner(RecordingRepository(callrecordDirectory), settings) { LocalDate.of(2026, 7, 26) }.clean()

        assertTrue(outsideFile.exists())
    }

    private fun grantStoragePermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand("pm grant $packageName android.permission.READ_EXTERNAL_STORAGE").close()
        instrumentation.uiAutomation.executeShellCommand("pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE").close()
    }
}
