package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luhaoyang.orderecho.R
import java.io.File
import java.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {
    private lateinit var testDirectory: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDirectory = File(context.cacheDir, "recording-list-${System.nanoTime()}").apply { mkdirs() }
        File(testDirectory, "4712345678_${LocalDate.now().minusDays(1).toString().replace("-", "")}_141530.amr")
            .writeText("amr")
        MainActivity.recordingDirectoryForTesting = testDirectory
    }

    @After
    fun tearDown() {
        MainActivity.recordingDirectoryForTesting = null
        testDirectory.deleteRecursively()
    }

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

    @Test
    fun olderDateHeaderTogglesItsRecordingRows() {
        grantStoragePermissions()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("昨天 · 1 条")).perform(click())
            onView(withText("4712345678")).check(matches(isDisplayed()))
            onView(withText("昨天 · 1 条")).perform(click())
            onView(withText("4712345678")).check(doesNotExist())
        }
    }

    private fun grantStoragePermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName android.permission.READ_EXTERNAL_STORAGE"
        ).close()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE"
        ).close()
    }
}
