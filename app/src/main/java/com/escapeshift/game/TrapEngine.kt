package com.escapeshift.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Version 24: data-driven industrial traps. No Android imports: these rules are
 * shared by the game and native JVM tests. All positions use game-world units.
 * Unknown future trap kinds are rejected by the JSON parser, not ignored.
 */
data class TrapDefinition(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val trigger: String = "player_near",
    val radius: Float = 100f,
    val delayTicks: Int = 12,
    val durationTicks: Int = 0,
    val travelX: Float = 0f,
    val travelY: Float = 0f,
    val transitionTicks: Int = 18,
    val periodTicks: Int = 180,
    val thresholdSpeed: Float = 250f
) {
    val box: HitBox get() = HitBox(x, y, x + width, y + height)
}

data class TrapView(val id: String, val type: String, val box: HitBox, val state: String)

class TrapEngine(private val definitions: List<TrapDefinition>) {
    private data class State(var firedAt: Int = -1)
    private val states = MutableList(definitions.size) { State() }
    var tickCount: Int = 0; private set

    fun reset() {
        tickCount = 0
        states.forEach { it.firedAt = -1 }
    }

    private fun progress(def: TrapDefinition, s: State): Float {
        if (s.firedAt < 0) return 0f
        return ((tickCount - s.firedAt - def.delayTicks).toFloat() / def.transitionTicks.coerceAtLeast(1))
            .coerceIn(0f, 1f)
    }

    private fun activated(def: TrapDefinition, s: State): Boolean =
        s.firedAt >= 0 && tickCount - s.firedAt >= def.delayTicks

    fun update(player: HitBox, previous: HitBox, didJump: Boolean, horizontalSpeed: Float) {
        tickCount++
        for (i in definitions.indices) {
            val d = definitions[i]; val s = states[i]
            if (s.firedAt >= 0 && d.durationTicks > 0 && tickCount - s.firedAt >= d.delayTicks + d.durationTicks) {
                s.firedAt = -1
            }
            if (s.firedAt != -1) continue
            val center = (player.left + player.right) * .5f
            val distanceX = abs(center - (d.x + d.width * .5f))
            val inZone = player.right > d.x && player.left < d.x + d.width &&
                player.bottom > d.y - 80f && player.top < d.y + d.height + 80f
            val eligible = when (d.trigger) {
                "player_near" -> distanceX <= d.radius && abs(player.bottom - d.y) < max(180f, d.radius)
                "player_on" -> player.right > d.x && player.left < d.x + d.width &&
                    previous.bottom <= d.y + 4f && player.bottom >= d.y - 4f
                "player_jump" -> didJump && distanceX <= d.radius
                "approach_fast" -> distanceX <= d.radius && abs(horizontalSpeed) >= d.thresholdSpeed
                "player_enter" -> inZone
                else -> false
            }
            if (eligible) s.firedAt = tickCount
        }
    }

    /** World-space position for a trap, used consistently for rendering and collision. */
    fun viewAt(index: Int): TrapView {
        val d = definitions[index]
        val s = states[index]
        val active = activated(d, s)
        val p = progress(d, s)
        val box = when (d.type) {
            "moving_platform" -> {
                val angle = 2.0 * Math.PI * tickCount.toDouble() / d.periodTicks
                val wave = (sin(angle) + 1.0) * .5
                val dx = d.travelX * wave.toFloat()
                val dy = d.travelY * wave.toFloat()
                HitBox(d.x + dx, d.y + dy, d.x + dx + d.width, d.y + dy + d.height)
            }
            "surprise_wall" -> HitBox(d.x, d.y + d.height * (1f - p), d.x + d.width, d.y + d.height * (2f - p))
            "rising_spikes" -> HitBox(d.x, d.y + d.height * (1f - p), d.x + d.width, d.y + d.height * (2f - p))
            else -> HitBox(d.x + p * d.travelX, d.y + p * d.travelY,
                d.x + d.width + p * d.travelX, d.y + d.height + p * d.travelY)
        }
        return TrapView(d.id, d.type, box, when {
            d.type == "moving_platform" -> "moving"
            s.firedAt < 0 -> "hidden"
            !active -> "warning"
            else -> "active"
        })
    }

    fun views(): List<TrapView> = definitions.indices.map { viewAt(it) }
    fun floorDefinitions(): List<TrapDefinition> = definitions.filter { it.type == "collapse_floor" }
    fun collapsedGaps(): List<GroundGap> = definitions.indices.filter {
        definitions[it].type == "collapse_floor" && activated(definitions[it], states[it])
    }.map { GroundGap(definitions[it].id, definitions[it].x, definitions[it].width) }

    fun solids(): List<HitBox> = definitions.indices.mapNotNull { i ->
        val def = definitions[i]; val view = viewAt(i)
        when (def.type) {
            "collapse_floor" -> view.box.takeUnless { activated(def, states[i]) }
            "surprise_wall" -> view.box.takeIf { activated(def, states[i]) }
            else -> null
        }
    }

    fun platforms(): List<HitBox> = definitions.indices.mapNotNull { i ->
        val d = definitions[i]
        viewAt(i).box.takeIf { d.type == "moving_platform" }
    }

    fun spikes(): List<HitBox> = definitions.indices.mapNotNull { i ->
        val d = definitions[i]
        viewAt(i).box.takeIf { d.type == "rising_spikes" && activated(d, states[i]) }
    }

    fun exit(base: HitBox): HitBox {
        val i = definitions.indexOfFirst { it.type == "moving_exit" }
        return if (i >= 0) viewAt(i).box else base
    }

    fun reverseControls(player: HitBox): Boolean = definitions.indices.any { i ->
        val d = definitions[i]
        d.type == "reverse_zone" && player.intersects(d.box)
    }

    fun hasType(type: String) = definitions.any { it.type == type }
}
