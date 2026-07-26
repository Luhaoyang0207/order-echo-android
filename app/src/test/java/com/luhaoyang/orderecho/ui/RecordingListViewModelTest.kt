package com.luhaoyang.orderecho.ui

import com.luhaoyang.orderecho.cleanup.CleanupResult
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackCommands
import com.luhaoyang.orderecho.playback.PlaybackState
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingListViewModelTest {
    private val baseDirectory = Files.createTempDirectory("recording-list").toFile()
    private val recordingFile = File(baseDirectory, "4712345678_20260726_141530.amr").apply {
        writeText("amr")
    }
    private val recording = RecordingFile(
        file = recordingFile,
        phoneNumber = "4712345678",
        recordedAt = LocalDateTime.of(2026, 7, 26, 14, 15, 30),
        sizeBytes = recordingFile.length(),
        durationMillis = 5_000
    )

    @After
    fun tearDown() {
        baseDirectory.deleteRecursively()
    }

    @Test
    fun clearingANoMatchSearchRestoresContentWithoutRefreshingTheActivity() {
        val viewModel = viewModel()
        assertTrue(viewModel.refresh() is RecordingListState.Content)
        assertEquals(RecordingListState.NoMatches, viewModel.setQuery("999"))

        val recovered = viewModel.clearQuery()

        assertTrue(recovered is RecordingListState.Content)
    }

    @Test
    fun tappingThePausedRecordingResumesInsteadOfRestartingIt() {
        val playback = FakePlayback(PlaybackState.Paused(recording.file, 2_000, 5_000))
        val viewModel = viewModel(playback = playback)
        viewModel.refresh()

        viewModel.play(recording)

        assertEquals(1, playback.resumeCount)
        assertEquals(0, playback.playCount)
    }

    @Test
    fun explicitStopStopsTheActiveRecording() {
        val playback = FakePlayback(PlaybackState.Playing(recording.file, 2_000, 5_000))
        val viewModel = viewModel(playback = playback)
        viewModel.refresh()

        val state = viewModel.stopPlayback()

        assertEquals(1, playback.stopCount)
        assertTrue(state is RecordingListState.Content)
    }

    @Test
    fun cleanupFailureCountIsReturnedToTheUi() {
        val viewModel = viewModel(cleanup = { CleanupResult(deletedCount = 2, failedCount = 1) })

        val run = viewModel.runCleanup()

        assertEquals(CleanupResult(deletedCount = 2, failedCount = 1), run.cleanupResult)
    }

    private fun viewModel(
        playback: FakePlayback = FakePlayback(),
        cleanup: () -> CleanupResult = { CleanupResult(0, 0) }
    ) = RecordingListViewModel(
        repository = RecordingRepository(baseDirectory),
        grouper = RecordingGrouper(),
        playbackController = playback,
        cleanup = cleanup
    )

    private class FakePlayback(
        private var currentState: PlaybackState = PlaybackState.Idle
    ) : PlaybackCommands {
        var playCount = 0
        var resumeCount = 0
        var stopCount = 0

        override fun play(recording: RecordingFile) {
            playCount++
        }

        override fun pause() = Unit

        override fun resume() {
            resumeCount++
        }

        override fun stop() {
            stopCount++
            currentState = PlaybackState.Idle
        }

        override fun state(): PlaybackState = currentState
        override fun updateProgress() = Unit
        override fun release() = Unit
    }
}
