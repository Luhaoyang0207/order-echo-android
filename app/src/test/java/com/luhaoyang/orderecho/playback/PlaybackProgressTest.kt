package com.luhaoyang.orderecho.playback

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {
    @Test
    fun progressUpdateKeepsActiveRecordingAndDuration() {
        val file = File("recording.amr")

        val result = reducePlaybackState(
            PlaybackState.Playing(file, 50, 1_000),
            PlaybackEvent.Progress(300)
        )

        assertEquals(PlaybackState.Playing(file, 300, 1_000), result)
    }

    @Test
    fun progressStillAdvancesWhenTotalDurationIsUnavailable() {
        val file = File("recording.amr")

        val result = reducePlaybackState(
            PlaybackState.Playing(file, 50, 0),
            PlaybackEvent.Progress(300)
        )

        assertEquals(PlaybackState.Playing(file, 300, 0), result)
    }
}
