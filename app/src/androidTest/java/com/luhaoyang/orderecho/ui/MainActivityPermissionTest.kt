package com.luhaoyang.orderecho.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {
    @Test
    fun deniedStoragePermissionShowsRecoveryAction() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("需要存储权限")).check(matches(isDisplayed()))
            onView(withText("打开系统设置")).check(matches(isDisplayed()))
        }
    }
}
