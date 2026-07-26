package com.luhaoyang.orderecho.cleanup

import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.AppSettingsTest
import com.luhaoyang.orderecho.data.RecordingRepository
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RetentionCleanerTest {
    private val baseDirectory = Files.createTempDirectory("cleanup").toFile()

    @After
    fun tearDown() {
        baseDirectory.deleteRecursively()
    }

    @Test
    fun deletionFailureIsReportedAndDoesNotStopLaterFiles() {
        val failing = expiredRecording("111_20260701_120000.amr")
        val deleted = expiredRecording("222_20260701_120000.amr")
        val repository = RecordingRepository(
            baseDirectory = baseDirectory,
            deleteFile = { file ->
                if (file.name == failing.name) throw SecurityException("delete denied")
                file.delete()
            }
        )
        val settings = AppSettings(AppSettingsTest.TestContext()).also { it.setRetentionDays(7) }

        val result = RetentionCleaner(repository, settings) { LocalDate.of(2026, 7, 26) }.clean()

        assertEquals(CleanupResult(deletedCount = 1, failedCount = 1), result)
        assertFalse(deleted.exists())
        assertEquals(1, settings.lastCleanupResult().failedCount)
    }

    private fun expiredRecording(name: String): File =
        File(baseDirectory, name).apply { writeText("amr") }
}
