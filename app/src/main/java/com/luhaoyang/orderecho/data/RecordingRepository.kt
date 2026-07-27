package com.luhaoyang.orderecho.data

import com.luhaoyang.orderecho.model.RecordingFile
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class RecordingScanResult(
    val recordings: List<RecordingFile>,
    val failedCount: Int
)

class RecordingRepository(
    val baseDirectory: File,
    private val deleteFile: (File) -> Boolean = { it.delete() },
    private val durationReader: (File) -> Int? = { null },
    private val childrenProvider: (File) -> Array<File>? = { it.listFiles() }
) {
    fun scan(): RecordingScanResult {
        val recordings = mutableListOf<RecordingFile>()
        var failedCount = 0

        childrenProvider(baseDirectory)?.forEach { file ->
            try {
                if (isSafeRecordingFile(file)) recordings += toRecordingFile(file)
            } catch (_: Exception) {
                failedCount++
            }
        }

        return RecordingScanResult(recordings, failedCount)
    }

    fun list(): List<RecordingFile> = scan().recordings

    /**
     * Reads media metadata only for a single recording that has just been
     * revalidated. Call this from background work when a duration is needed.
     */
    fun durationFor(recording: RecordingFile): Int? {
        return runCatching {
            val validatedFile = validatedRecordingFile(recording.file) ?: return null
            durationReader(validatedFile)?.takeIf { it > 0 }
        }.getOrNull()
    }

    fun delete(recording: RecordingFile): Boolean {
        return runCatching {
            val file = recording.file
            validatedRecordingFile(file) != null && deleteFile(file)
        }.getOrDefault(false)
    }

    private fun isSafeRecordingFile(file: File): Boolean = validatedRecordingFile(file) != null

    private fun validatedRecordingFile(file: File): File? {
        val canonicalBaseDirectory = baseDirectory.canonicalFile
        val canonicalFile = file.canonicalFile
        val directChildPrefix = canonicalBaseDirectory.path + File.separator

        return canonicalFile.takeIf {
            canonicalFile.path.startsWith(directChildPrefix) &&
            canonicalFile.parentFile == canonicalBaseDirectory &&
            canonicalFile.isFile &&
            canonicalFile.name.endsWith(AMR_EXTENSION, ignoreCase = true)
        }
    }

    private fun toRecordingFile(file: File): RecordingFile {
        val match = HUAWEI_FILE_NAME.matchEntire(file.name)
        val parsedDateTime = match?.let {
            runCatching {
                LocalDateTime.parse(
                    "${it.groupValues[2]}_${it.groupValues[3]}",
                    HUAWEI_DATE_TIME_FORMAT
                )
            }.getOrNull()
        }

        return RecordingFile(
            file = file,
            phoneNumber = match?.groupValues?.get(1)?.takeIf(String::isNotBlank),
            recordedAt = parsedDateTime ?: lastModifiedDateTime(file),
            sizeBytes = file.length(),
            durationMillis = null
        )
    }

    private fun lastModifiedDateTime(file: File): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
    }

    private companion object {
        const val AMR_EXTENSION = ".amr"
        val HUAWEI_FILE_NAME = Regex("^(.+?)_(\\d{8})_(\\d{6})\\.amr$", RegexOption.IGNORE_CASE)
        val HUAWEI_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("uuuuMMdd_HHmmss")
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
