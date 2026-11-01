package com.escapeshift.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.math.abs
import kotlin.math.max

/** Short effects & four positional continuous sources. Rename/rewrite audio in res/raw, not code. */
class GameAudio(context: Context) {
    companion object {
        const val HEARING_DISTANCE = 480f
        const val MAX_EFFECT_VOLUME = .55f
        const val MAX_AMBIENT_VOLUME = .32f
        fun proximity(distance: Float): Float {
            val x = (1f - abs(distance) / HEARING_DISTANCE).coerceIn(0f, 1f)
            return x * x // gradual approach, not an abrupt volume jump
        }
    }
    private val pool = SoundPool.Builder().setMaxStreams(12)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val samples = mutableMapOf<String, Int>()
    private val ready = mutableSetOf<Int>()
    private val streams = mutableMapOf<String, Int>()
    private var enabled = true
    private var released = false
    private val loops = setOf("laser", "saw", "monster", "conveyor")

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        val ids = linkedMapOf(
            "jump" to R.raw.sfx_jump, "land" to R.raw.sfx_land,
            "shift" to R.raw.sfx_shift, "laser" to R.raw.sfx_laser,
            "saw" to R.raw.sfx_saw, "monster" to R.raw.sfx_monster,
            "conveyor" to R.raw.sfx_conveyor, "crate" to R.raw.sfx_crate,
            "spring" to R.raw.sfx_spring, "coin" to R.raw.sfx_coin,
            "key" to R.raw.sfx_key, "door" to R.raw.sfx_door,
            "victory" to R.raw.sfx_victory, "death" to R.raw.sfx_death)
        for ((name, file) in ids) samples[name] = pool.load(context, file, 1)
    }

    fun oneShot(name: String, volume: Float = .55f) {
        if (released || !enabled) return
        val id = samples[name] ?: return
        if (id !in ready) return
        val v = volume.coerceIn(0f, MAX_EFFECT_VOLUME)
        pool.play(id, v, v, 1, 0, 1f)
    }

    /** One nearest source per type; start muted, update smoothly as hero moves. */
    fun updateAmbient(type: String, distance: Float?) {
        if (released || type !in loops) return
        val id = samples[type] ?: return
        val volume = if (!enabled || distance == null) 0f else MAX_AMBIENT_VOLUME * proximity(distance)
        val stream = streams[type]
        if (volume < .002f || id !in ready) {
            if (stream != null) { pool.stop(stream); streams.remove(type) }
            return
        }
        if (stream == null) {
            val newStream = pool.play(id, volume, volume, 1, -1, 1f)
            if (newStream != 0) streams[type] = newStream
        } else pool.setVolume(stream, volume, volume)
    }

    fun pause() { enabled = false; streams.values.forEach { pool.stop(it) }; streams.clear() }
    fun resume() { enabled = true }
    fun release() { if (!released) { pause(); released = true; pool.release() } }
}
