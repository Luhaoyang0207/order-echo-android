package com.luhaoyang.orderecho

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun appUsesApi26AsMinimum() {
        assertEquals(26, BuildConfig.MIN_SDK_FOR_TEST)
    }
}
