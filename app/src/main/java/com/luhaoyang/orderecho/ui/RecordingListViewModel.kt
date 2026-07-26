package com.luhaoyang.orderecho.ui

import com.luhaoyang.orderecho.cleanup.CleanupResult
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackCommands
import com.luhaoyang.orderecho.playback.PlaybackState

class RecordingListViewModel(
    private val repository: RecordingRepository,
    private val grouper: RecordingGrouper,
    private val playbackController: PlaybackCommands,
    private val cleanup: () -> CleanupResult
) {
    private var allRecordings: List<RecordingFile> = emptyList()
    private var query: String = ""
    private var scanFailedCount: Int = 0

    fun refresh(): RecordingListState {
        if (!repository.baseDirectory.isDirectory) return RecordingListState.MissingDirectory
        return runCatching {
            val scan = repository.scan()
            allRecordings = scan.recordings
            scanFailedCount = scan.failedCount
            displayedState()
        }.getOrElse { RecordingListState.Error }
    }

    fun setQuery(query: String): RecordingListState {
        this.query = query.trim()
        return displayedState()
    }

    fun clearQuery(): RecordingListState {
        query = ""
        return displayedState()
    }

    fun play(recording: RecordingFile): RecordingListState {
        val playback = playbackController.state()
        when {
            playback is PlaybackState.Playing && playback.file == recording.file ->
                playbackController.pause()
            playback is PlaybackState.Paused && playback.file == recording.file ->
                playbackController.resume()
            else -> playbackController.play(recording)
        }
        return displayedState()
    }

    fun stopPlayback(): RecordingListState {
        playbackController.stop()
        return displayedState()
    }

    fun delete(recording: RecordingFile): RecordingListState {
        return runCatching {
            if (repository.delete(recording)) refresh() else RecordingListState.Error
        }.getOrElse { RecordingListState.Error }
    }

    fun runCleanup(): CleanupRunResult {
        val cleanupResult = runCatching(cleanup).getOrNull()
            ?: return CleanupRunResult(RecordingListState.Error, null)
        return CleanupRunResult(refresh(), cleanupResult)
    }

    fun playbackState(): PlaybackState = playbackController.state()

    fun refreshPlayback(): RecordingListState {
        playbackController.updateProgress()
        return displayedState()
    }

    fun release() = playbackController.release()

    fun recordingStatistics(): RecordingStatistics = runCatching {
        val recordings = repository.list()
        RecordingStatistics(
            count = recordings.size,
            occupiedBytes = recordings.sumOf { it.sizeBytes },
            oldestRecordedAt = recordings.minOfOrNull { it.recordedAt }
        )
    }.getOrDefault(RecordingStatistics(0, 0L, null))

    private fun displayedState(): RecordingListState {
        if (allRecordings.isEmpty()) {
            return if (scanFailedCount > 0) RecordingListState.Error else RecordingListState.Empty
        }
        val filtered = allRecordings.filter { it.phoneNumber.orEmpty().contains(query) }
        return if (filtered.isEmpty()) {
            RecordingListState.NoMatches
        } else {
            RecordingListState.Content(grouper.group(filtered), scanFailedCount)
        }
    }
}

data class CleanupRunResult(
    val listState: RecordingListState,
    val cleanupResult: CleanupResult?
)

data class RecordingStatistics(
    val count: Int,
    val occupiedBytes: Long,
    val oldestRecordedAt: java.time.LocalDateTime?
)

sealed interface RecordingListState {
    data object MissingDirectory : RecordingListState
    data object Empty : RecordingListState
    data object NoMatches : RecordingListState
    data object Error : RecordingListState
    data class Content(
        val groups: List<com.luhaoyang.orderecho.data.MonthGroup>,
        val scanFailedCount: Int = 0
    ) : RecordingListState
}
