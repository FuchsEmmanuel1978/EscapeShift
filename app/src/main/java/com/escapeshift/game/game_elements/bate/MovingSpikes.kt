package com.escapeshift.game.game_elements.bate

import com.escapeshift.game.HitBox
import com.escapeshift.game.LevelObject
import com.escapeshift.game.game_elements.adventure.SpikeElements
import kotlin.math.min

/**
 * Bait: triangles already visible on the ground flee right when the hero approaches
 * from the LEFT. No new spikes are spawned, and their hitboxes move with the sprite.
 * A group moves once per life, then remains at its final position.
 * Designed for fixed-step (60 Hz) simulation without Android dependencies.
 */
class MovingSpikes(elements: List<LevelObject>) {
    private data class State(val definition: LevelObject, var travelled: Float = 0f, var fired: Boolean = false)
    private val states = elements.map { State(it) }

    fun reset() { states.forEach { it.travelled = 0f; it.fired = false } }

    fun update(player: HitBox, seconds: Float, worldRight: Float) {
        for (s in states) {
            val d = s.definition
            val width = d.width * d.count + d.spacing * (d.count - 1)
            // Player approaches the beginning of the ORIGINAL group from the left.
            if (!s.fired && player.right <= d.x && d.x - player.right <= d.triggerDistance &&
                player.bottom >= d.y - 100f && player.top <= d.y + d.height + 50f) s.fired = true
            if (s.fired && s.travelled < d.travelDistance) {
                val maxTravel = (worldRight - d.x - width).coerceAtLeast(0f)
                val target = min(d.travelDistance, maxTravel)
                s.travelled = min(target, s.travelled + d.speed * seconds)
            }
        }
    }

    fun boxes(): List<HitBox> = states.flatMap { SpikeElements.boxes(it.definition, it.travelled) }
    fun isTriggered(id: String): Boolean = states.firstOrNull { it.definition.id == id }?.fired ?: false
}
