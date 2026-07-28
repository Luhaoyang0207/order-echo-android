package com.luhaoyang.orderecho.ui

import com.luhaoyang.orderecho.cleanup.CleanupResult
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackCommands
import com.luhaoyang.orderecho.playback.PlaybackState
import java.io.File
import java.time.LocalDate
import java.nio.file.Files
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingListViewModelTest {
    private val baseDirectory = Files.createTempDirectory("recording-list").toFile()
    private val recording = createRecording(LocalDate.now(), "123 12 123")
    private val yesterdayRecording = createRecording(LocalDate.now().minusDays(1), "+47 (123)-45 678")
    private val olderRecording = createRecording(LocalDate.now().minusDays(7), "9187654320")

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
    fun refreshExpandsOnlyTodayByDefault() {
        val state = viewModel().refresh() as RecordingListState.Content

        val dates = state.groups.flatMap { it.dates }
        assertTrue(dates.single { it.date == LocalDate.now() }.expanded)
        assertFalse(dates.single { it.date == LocalDate.now().minusDays(1) }.expanded)
    }

    @Test
    fun togglingAnOlderDateShowsOnlyThatDatesRows() {
        val viewModel = viewModel()
        viewModel.refresh()

        val state = viewModel.toggleDate(LocalDate.now().minusDays(1)) as RecordingListState.Content

        assertTrue(state.groups.flatMap { it.dates }
            .single { it.date == LocalDate.now().minusDays(1) }.expanded)
    }

    @Test
    fun searchExpandsEveryDateThatContainsAMatch() {
        val state = viewModel().apply { refresh() }.setQuery("1") as RecordingListState.Content

        assertTrue(state.groups.flatMap { it.dates }.all { it.expanded })
    }

    @Test
    fun digitsOnlyQueryMatchesANumberDisplayedWithSpaces() {
        val state = viewModel().apply { refresh() }.setQuery("12312123") as RecordingListState.Content

        assertEquals(listOf("123 12 123"), state.groups.flatMap { it.dates }
            .flatMap { it.recordings }
            .map { it.phoneNumber })
    }

    @Test
    fun searchIgnoresCommonSeparatorsInTheQueryAndNumber() {
        val state = viewModel().apply { refresh() }.setQuery("47-123 45") as RecordingListState.Content

        assertEquals(listOf("+47 (123)-45 678"), state.groups.flatMap { it.dates }
            .flatMap { it.recordings }
            .map { it.phoneNumber })
    }

    @Test
    fun separatorOnlyQueryRestoresAllRecordingsWithTodayFirstExpansion() {
        val state = viewModel().apply { refresh() }.setQuery(" -() ") as RecordingListState.Content

        val dates = state.groups.flatMap { it.dates }
        assertEquals(3, dates.flatMap { it.recordings }.size)
        assertTrue(dates.single { it.date == LocalDate.now() }.expanded)
        assertFalse(dates.single { it.date == LocalDate.now().minusDays(1) }.expanded)
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

    private fun createRecording(date: LocalDate, phoneNumber: String): RecordingFile {
        val file = File(baseDirectory, "${phoneNumber}_${date.toString().replace("-", "")}_141530.amr")
            .apply { writeText("amr") }
        return RecordingFile(
            file = file,
            phoneNumber = phoneNumber,
            recordedAt = LocalDateTime.of(date.year, date.month, date.dayOfMonth, 14, 15, 30),
            sizeBytes = file.length(),
            durationMillis = 5_000
        )
    }

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
