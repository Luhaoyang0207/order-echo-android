package com.luhaoyang.orderecho.playback

import java.io.File

sealed interface PlaybackState {
    data object Idle : PlaybackState

    data class Playing(
        val file: File,
        val positionMillis: Int,
        val durationMillis: Int
    ) : PlaybackState

    data class Paused(
        val file: File,
        val positionMillis: Int,
        val durationMillis: Int
    ) : PlaybackState

    data class Error(val message: String) : PlaybackState
}

sealed interface PlaybackEvent {
    data class Start(val file: File, val durationMillis: Int) : PlaybackEvent
    data class Pause(val positionMillis: Int) : PlaybackEvent
    data object Stop : PlaybackEvent
    data object Complete : PlaybackEvent
    data class Fail(val message: String) : PlaybackEvent
}

fun reducePlaybackState(current: PlaybackState, event: PlaybackEvent): PlaybackState = when (event) {
    is PlaybackEvent.Start -> PlaybackState.Playing(event.file, 0, event.durationMillis)
    is PlaybackEvent.Pause -> when (current) {
        is PlaybackState.Playing -> PlaybackState.Paused(
            current.file,
            event.positionMillis,
            current.durationMillis
        )
        else -> current
    }
    PlaybackEvent.Stop, PlaybackEvent.Complete -> PlaybackState.Idle
    is PlaybackEvent.Fail -> PlaybackState.Error(event.message)
}
