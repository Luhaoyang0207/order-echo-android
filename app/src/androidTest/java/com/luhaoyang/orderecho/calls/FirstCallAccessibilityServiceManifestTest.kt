package com.luhaoyang.orderecho.calls

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallAccessibilityServiceManifestTest {
    @Test fun serviceIsSystemBoundAndPublishesAccessibilityMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = ComponentName(context.packageName, "$COMPONENT_PACKAGE.FirstCallAccessibilityService")
        val info = context.packageManager.getServiceInfo(component, PackageManager.GET_META_DATA)

        assertFalse(info.exported)
        assertEquals(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, info.permission)
        assertTrue(info.metaData?.getInt("android.accessibilityservice", 0) ?: 0 > 0)
    }

    private companion object {
        const val COMPONENT_PACKAGE = "com.luhaoyang.orderecho.calls"
    }
}