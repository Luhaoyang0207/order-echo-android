package com.luhaoyang.orderecho.data

import com.luhaoyang.orderecho.model.RecordingFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingRepositoryTest {
    private lateinit var baseDirectory: File
    private lateinit var repository: RecordingRepository

    @Before
    fun setUp() {
        baseDirectory = Files.createTempDirectory("callrecord").toFile()
        repository = RecordingRepository(baseDirectory)
    }

    @After
    fun tearDown() {
        baseDirectory.deleteRecursively()
    }

    @Test
    fun parsesKnownHuaweiName() {
        File(baseDirectory, "4712345678_20260726_141530.amr").writeText("amr")

        val recording = repository.list().single()

        assertEquals("4712345678", recording.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 7, 26, 14, 15, 30), recording.recordedAt)
        assertEquals(3L, recording.sizeBytes)
    }

    @Test
    fun ignoresNonAmrFilesAndNestedAmrFiles() {
        File(baseDirectory, "notes.txt").writeText("not a recording")
        File(baseDirectory, "4712345678_20260726_141530.mp3").writeText("not a recording")
        File(baseDirectory, "nested").mkdir()
        File(baseDirectory, "nested/4712345678_20260726_141530.amr").writeText("amr")
        File(baseDirectory, "4712345678_20260726_141530.amr").writeText("amr")

        assertEquals(1, repository.list().size)
    }

    @Test
    fun fallsBackToLastModifiedWhenNameCannotBeParsed() {
        val malformed = File(baseDirectory, "unrecognised.amr")
        malformed.writeText("amr")
        malformed.setLastModified(1_785_000_000_000L)

        val recording = repository.list().single()

        assertEquals(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(1_785_000_000_000L), java.time.ZoneId.systemDefault()), recording.recordedAt)
    }

    @Test
    fun fallsBackToLastModifiedWhenFilenameContainsAnImpossibleDate() {
        val malformedDate = File(baseDirectory, "4712345678_20260230_141530.amr")
        malformedDate.writeText("amr")
        malformedDate.setLastModified(1_785_000_000_000L)

        val recording = repository.list().single()

        assertEquals(
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(1_785_000_000_000L),
                java.time.ZoneId.systemDefault()
            ),
            recording.recordedAt
        )
    }

    @Test
    fun doesNotDeleteARecordingOutsideTheConfiguredDirectory() {
        val outside = Files.createTempFile("outside", ".amr").toFile()
        outside.writeText("amr")
        val recording = RecordingFile(
            file = outside,
            phoneNumber = null,
            recordedAt = LocalDateTime.now(),
            sizeBytes = outside.length()
        )

        assertFalse(repository.delete(recording))
        assertTrue(outside.exists())
        outside.delete()
    }

    @Test
    fun doesNotDeleteDirectoriesEvenWhenTheyEndInAmr() {
        val directory = File(baseDirectory, "directory.amr")
        directory.mkdir()
        val recording = RecordingFile(directory, null, LocalDateTime.now(), 0)

        assertFalse(repository.delete(recording))
        assertTrue(directory.exists())
    }

    @Test
    fun scanSkipsOneUnreadableEntryAndReportsTheFailure() {
        val good = File(baseDirectory, "4712345678_20260726_141530.amr").apply { writeText("amr") }
        val unreadable = MetadataFailureFile(baseDirectory, "unreadable.amr")
        val repository = RecordingRepository(baseDirectory) { arrayOf(unreadable, good) }

        val result = repository.scan()

        assertEquals(listOf(good), result.recordings.map { it.file })
        assertEquals(1, result.failedCount)
    }

    @Test
    fun deleteReturnsFalseWhenFileValidationThrows() {
        val unreadable = CanonicalFailureFile(File(baseDirectory, "unreadable.amr").path)
        val recording = RecordingFile(unreadable, null, LocalDateTime.now(), 0)

        assertFalse(repository.delete(recording))
    }

    @Test
    fun derivesRecordingDurationSafelyDuringScan() {
        File(baseDirectory, "4712345678_20260726_141530.amr").writeText("amr")
        val repository = RecordingRepository(
            baseDirectory = baseDirectory,
            durationReader = { 125_000 }
        )

        assertEquals(125_000, repository.list().single().durationMillis)
    }

    @Test
    fun unavailableDurationDoesNotHideAnOtherwiseValidRecording() {
        File(baseDirectory, "4712345678_20260726_141530.amr").writeText("amr")
        val repository = RecordingRepository(
            baseDirectory = baseDirectory,
            durationReader = { throw IllegalStateException("duration unavailable") }
        )

        val result = repository.scan()

        assertEquals(1, result.recordings.size)
        assertEquals(null, result.recordings.single().durationMillis)
        assertEquals(0, result.failedCount)
    }

    private class MetadataFailureFile(parent: File, child: String) : File(parent, child) {
        override fun getCanonicalFile(): File = this
        override fun isFile(): Boolean = true
        override fun length(): Long = throw SecurityException("metadata unavailable")
    }

    private class CanonicalFailureFile(path: String) : File(path) {
        override fun getCanonicalFile(): File = throw IOException("canonical path unavailable")
    }
}
