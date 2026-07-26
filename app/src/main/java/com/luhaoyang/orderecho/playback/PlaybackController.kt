package com.luhaoyang.orderecho.playback

import android.media.MediaPlayer
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.model.RecordingFile
import java.io.File

interface PlaybackCommands {
    fun play(recording: RecordingFile)
    fun pause()
    fun resume()
    fun stop()
    fun state(): PlaybackState
    fun updateProgress()
    fun release()
}

class PlaybackController(
    private val repository: RecordingRepository,
    private val playerFactory: () -> MediaPlayer = { MediaPlayer() }
) : PlaybackCommands {
    private var player: MediaPlayer? = null
    private var playbackState: PlaybackState = PlaybackState.Idle

    override fun play(recording: RecordingFile) {
        if (runCatching { validatedCanonicalFile(recording) }.getOrNull() == null) {
            releasePlayer()
            publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
            return
        }

        releasePlayer()

        val newPlayer = playerFactory()
        player = newPlayer
        try {
            val playbackFile = validatedCanonicalFile(recording)
                ?: throw IllegalArgumentException("Recording is no longer valid")
            newPlayer.setDataSource(playbackFile.path)
            newPlayer.setOnCompletionListener {
                if (player === newPlayer) {
                    releasePlayer()
                    publish(PlaybackEvent.Complete)
                }
            }
            newPlayer.setOnErrorListener { _, _, _ ->
                if (player === newPlayer) {
                    releasePlayer()
                    publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
                }
                true
            }
            newPlayer.prepare()
            newPlayer.start()
            val duration = runCatching { newPlayer.duration }
                .getOrDefault(recording.durationMillis ?: 0)
                .takeIf { it > 0 }
                ?: recording.durationMillis
                ?: 0
            publish(PlaybackEvent.Start(playbackFile, duration))
        } catch (_: Exception) {
            if (player === newPlayer) {
                releasePlayer()
                publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
            }
        }
    }

    override fun pause() {
        val activePlayer = player ?: return
        if (playbackState is PlaybackState.Playing) {
            try {
                activePlayer.pause()
                publish(PlaybackEvent.Pause(activePlayer.currentPosition))
            } catch (_: IllegalStateException) {
                releasePlayer()
                publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
            }
        }
    }

    override fun resume() {
        val activePlayer = player ?: return
        if (playbackState is PlaybackState.Paused) {
            try {
                activePlayer.start()
                publish(PlaybackEvent.Resume)
            } catch (_: IllegalStateException) {
                releasePlayer()
                publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
            }
        }
    }

    override fun updateProgress() {
        val activePlayer = player ?: return
        if (playbackState !is PlaybackState.Playing) return
        try {
            publish(PlaybackEvent.Progress(activePlayer.currentPosition))
        } catch (_: IllegalStateException) {
            releasePlayer()
            publish(PlaybackEvent.Fail(PLAYBACK_ERROR))
        }
    }

    override fun stop() {
        releasePlayer()
        publish(PlaybackEvent.Stop)
    }

    override fun state(): PlaybackState = playbackState

    override fun release() {
        stop()
    }

    private fun validatedCanonicalFile(recording: RecordingFile): File? {
        val requestedFile = recording.file.canonicalFile
        return repository.list()
            .firstOrNull { it.file.canonicalFile == requestedFile }
            ?.file
            ?.canonicalFile
    }

    private fun releasePlayer() {
        val activePlayer = player ?: return
        player = null
        try {
            activePlayer.stop()
        } catch (_: IllegalStateException) {
            // The player can already be stopped after an error or completion.
        }
        activePlayer.release()
    }

    private fun publish(event: PlaybackEvent) {
        playbackState = reducePlaybackState(playbackState, event)
    }

    private companion object {
        const val PLAYBACK_ERROR = "录音播放失败"
    }
}
