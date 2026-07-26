package com.luhaoyang.orderecho.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun newSettingsUseThirtyDaysByDefault() {
        val settings = AppSettings(TestContext())

        assertEquals(30, settings.retentionDays())
    }

    @Test
    fun retentionChoicePersistsAcrossSettingsInstances() {
        val context = TestContext()
        AppSettings(context).setRetentionDays(90)

        assertEquals(90, AppSettings(context).retentionDays())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedRetentionValue() {
        AppSettings(TestContext()).setRetentionDays(14)
    }

    @Test
    fun latestCleanupResultPersistsAcrossSettingsInstances() {
        val context = TestContext()
        AppSettings(context).setLastCleanupResult(
            completedAtMillis = 1_234L,
            deletedCount = 2,
            failedCount = 1
        )

        assertEquals(
            LastCleanupResult(1_234L, deletedCount = 2, failedCount = 1),
            AppSettings(context).lastCleanupResult()
        )
    }

    class TestContext : ContextWrapper(null) {
        private val preferences = InMemoryPreferences()

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
    }

    private class InMemoryPreferences : SharedPreferences {
        private val values = ConcurrentHashMap<String, Any>()

        override fun getAll(): Map<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
            val storedValues = values[key] as? Set<*> ?: return defValues
            return storedValues.filterIsInstance<String>().toMutableSet()
        }
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private class Editor(private val values: ConcurrentHashMap<String, Any>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = put(key, value)
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = put(key, values?.toSet())
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = put(key, value)
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = put(key, value)
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = put(key, value)
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = put(key, value)
            override fun remove(key: String): SharedPreferences.Editor = put(key, null)
            override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearAll) values.clear()
                pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            }

            private fun put(key: String, value: Any?): SharedPreferences.Editor { pending[key] = value; return this }
        }
    }
}
