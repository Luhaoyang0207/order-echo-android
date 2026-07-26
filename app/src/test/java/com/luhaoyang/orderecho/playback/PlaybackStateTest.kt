package com.luhaoyang.orderecho.playback

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun startingAnotherFileReplacesActiveFile() {
        val first = File("first.amr")
        val second = File("second.amr")

        val result = reducePlaybackState(
            PlaybackState.Playing(first, 50, 1_000),
            PlaybackEvent.Start(second, 2_000)
        )

        assertEquals(second, (result as PlaybackState.Playing).file)
    }

    @Test
    fun pausingPreservesTheActiveFileAndDuration() {
        val file = File("recording.amr")

        val result = reducePlaybackState(
            PlaybackState.Playing(file, 50, 1_000),
            PlaybackEvent.Pause(300)
        )

        assertEquals(PlaybackState.Paused(file, 300, 1_000), result)
    }

    @Test
    fun stoppingReturnsToIdle() {
        val result = reducePlaybackState(
            PlaybackState.Paused(File("recording.amr"), 300, 1_000),
            PlaybackEvent.Stop
        )

        assertEquals(PlaybackState.Idle, result)
    }
}
