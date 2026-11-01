package com.escapeshift.game

import android.content.Context
import android.media.MediaPlayer

/** A single long ambient track; low-volume and seamless repeat via MediaPlayer.
 * Activity owns this object and calls resume/pause/release with its lifecycle.
 * All sound operations happen on the UI thread and never inside GameView.onDraw().
 */
class GameMusic(context: Context) {
    companion object {
        const val DEFAULT_VOLUME = 0.36f
    }

    private var player: MediaPlayer? = try {
        MediaPlayer.create(context.applicationContext, R.raw.ambient_loop)?.apply {
            isLooping = true
            setVolume(DEFAULT_VOLUME, DEFAULT_VOLUME)
        }
    } catch (_: Exception) {
        null // Allow game to run even when audio cannot be loaded on a device.
    }

    fun resume() {
        try {
            player?.let { if (!it.isPlaying) it.start() }
        } catch (_: IllegalStateException) {
            // The gameplay must remain usable even if a device audio service fails.
        }
    }

    fun pause() {
        try {
            player?.let { if (it.isPlaying) it.pause() }
        } catch (_: IllegalStateException) {
        }
    }

    fun release() {
        val old = player
        player = null
        try { old?.release() } catch (_: Exception) { }
    }
}
