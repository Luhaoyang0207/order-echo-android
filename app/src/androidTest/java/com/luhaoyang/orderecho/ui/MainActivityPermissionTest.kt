package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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
            UiDevice.getInstance(instrumentation).findObject(By.text(Pattern.compile("(?i)deny|拒绝"))).click()
            onView(withText("需要存储权限")).check(matches(isDisplayed()))
            onView(withText("打开系统设置")).check(matches(isDisplayed()))
        }
    }
}
