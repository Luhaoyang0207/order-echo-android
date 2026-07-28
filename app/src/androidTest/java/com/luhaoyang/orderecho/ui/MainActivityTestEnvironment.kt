package com.luhaoyang.orderecho.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.rules.ExternalResource

class MainActivityTestEnvironment(
    private val fixtureName: String
) : ExternalResource() {
    lateinit var testRoot: File
        private set
    lateinit var recordingDirectory: File
        private set

    private lateinit var preferences: SharedPreferences
    private lateinit var preferenceSnapshot: Map<String, *>
    private var previousRecordingDirectory: File? = null

    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheDirectory = context.cacheDir.canonicalFile
        testRoot = File(cacheDirectory, "$fixtureName-${System.nanoTime()}").apply {
            check(mkdirs())
        }.canonicalFile
        recordingDirectory = File(testRoot, "Callrecord").apply {
            check(mkdirs())
        }.canonicalFile

        check(testRoot.parentFile == cacheDirectory)
        check(recordingDirectory.parentFile == testRoot)
        check(recordingDirectory.path != PRODUCTION_RECORDING_DIRECTORY)

        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferenceSnapshot = HashMap(preferences.all)
        previousRecordingDirectory = MainActivity.recordingDirectoryForTesting
        MainActivity.recordingDirectoryForTesting = recordingDirectory
        check(MainActivity.recordingDirectoryForTesting?.canonicalFile == recordingDirectory)
    }

    override fun after() {
        try {
            restorePreferences()
        } finally {
            try {
                MainActivity.recordingDirectoryForTesting = previousRecordingDirectory
            } finally {
                testRoot.deleteRecursively()
            }
        }
    }

    private fun restorePreferences() {
        val editor = preferences.edit().clear()
        preferenceSnapshot.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        check(editor.commit())
    }

    private companion object {
        const val PREFERENCES_NAME = "order_echo_settings"
        const val PRODUCTION_RECORDING_DIRECTORY = "/storage/emulated/0/Sounds/Callrecord"
    }
}
