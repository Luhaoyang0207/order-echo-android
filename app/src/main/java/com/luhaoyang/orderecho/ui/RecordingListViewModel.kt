package com.luhaoyang.orderecho.ui

import com.luhaoyang.orderecho.cleanup.RetentionCleaner
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackController
import com.luhaoyang.orderecho.playback.PlaybackState

class RecordingListViewModel(
    private val repository: RecordingRepository,
    private val grouper: RecordingGrouper,
    private val playbackController: PlaybackController,
    private val retentionCleaner: RetentionCleaner
) {
    private var allRecordings: List<RecordingFile> = emptyList()
    private var query: String = ""

    fun refresh(): RecordingListState {
        if (!repository.baseDirectory.isDirectory) return RecordingListState.MissingDirectory
        return runCatching {
            allRecordings = repository.list()
            displayedState()
        }.getOrElse { RecordingListState.Error }
    }

    fun setQuery(query: String): RecordingListState {
        this.query = query.trim()
        return displayedState()
    }

    fun play(recording: RecordingFile): RecordingListState {
        val playback = playbackController.state()
        if (playback is PlaybackState.Playing && playback.file == recording.file) {
            playbackController.pause()
        } else {
            playbackController.play(recording)
        }
        return displayedState()
    }

    fun delete(recording: RecordingFile): RecordingListState {
        repository.delete(recording)
        return refresh()
    }

    fun runCleanup(): RecordingListState {
        retentionCleaner.clean()
        return refresh()
    }

    fun playbackState(): PlaybackState = playbackController.state()

    fun refreshPlayback(): RecordingListState {
        playbackController.updateProgress()
        return displayedState()
    }

    fun release() = playbackController.release()

    private fun displayedState(): RecordingListState {
        if (allRecordings.isEmpty()) return RecordingListState.Empty
        val filtered = allRecordings.filter { it.phoneNumber.orEmpty().contains(query) }
        return if (filtered.isEmpty()) RecordingListState.NoMatches else RecordingListState.Content(grouper.group(filtered))
    }
}

sealed interface RecordingListState {
    data object MissingDirectory : RecordingListState
    data object Empty : RecordingListState
    data object NoMatches : RecordingListState
    data object Error : RecordingListState
    data class Content(val groups: List<com.luhaoyang.orderecho.data.MonthGroup>) : RecordingListState
}
