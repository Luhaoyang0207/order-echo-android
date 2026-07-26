package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {
    @Test
    fun deniedStoragePermissionShowsRecoveryAction() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand("pm revoke $packageName android.permission.READ_EXTERNAL_STORAGE").close()
        instrumentation.uiAutomation.executeShellCommand("pm revoke $packageName android.permission.WRITE_EXTERNAL_STORAGE").close()

        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(instrumentation)
            val denyButton = device.wait(
                Until.findObject(By.res(PACKAGE_INSTALLER, DENY_BUTTON)),
                PERMISSION_DIALOG_TIMEOUT_MILLIS
            ) ?: device.wait(
                Until.findObject(By.res(PERMISSION_CONTROLLER, DENY_BUTTON)),
                FALLBACK_TIMEOUT_MILLIS
            ) ?: device.wait(
                Until.findObject(By.text(Pattern.compile("(?i)deny|拒绝"))),
                FALLBACK_TIMEOUT_MILLIS
            )
            assertNotNull("Expected the Android storage-permission deny action", denyButton)
            denyButton!!.click()
            onView(withText("需要存储权限")).check(matches(isDisplayed()))
            onView(withText("打开系统设置")).check(matches(isDisplayed()))
        }
    }

    private companion object {
        const val PACKAGE_INSTALLER = "com.android.packageinstaller"
        const val PERMISSION_CONTROLLER = "com.android.permissioncontroller"
        const val DENY_BUTTON = "permission_deny_button"
        const val PERMISSION_DIALOG_TIMEOUT_MILLIS = 5_000L
        const val FALLBACK_TIMEOUT_MILLIS = 1_000L
    }
}
