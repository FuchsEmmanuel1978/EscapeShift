package com.escapeshift.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Data-driven traps shared by all worlds. Fixed step: 60 ticks / second.
 * Only the JSON declares which traps exist; no implicit spawns in Kotlin.
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
    val thresholdSpeed: Float = 250f,
    val triggerX: Float? = null,
    /** Used by rising_spikes: up, down, left or right. */
    val orientation: String = "up",
    /** A false/invisible floor hole can have showWarning=false. */
    val showWarning: Boolean = true,
    val projectileSpeed: Float = 330f,
    val projectileRange: Float = 460f,
    val arcHeight: Float = 80f,
    val fallAcceleration: Float = 1050f
) {
    val box get() = HitBox(x, y, x + width, y + height)
}

data class TrapView(val id: String, val type: String, val box: HitBox, val state: String)

/** Visual-only collapse animation; the physics already removed the floor. */
data class FallingGroundPiece(val id: String, val x: Float, val width: Float, val dropY: Float)


class TrapEngine(private val definitions: List<TrapDefinition>) {
    private data class State(
        var firedAt: Int = -1,
        var projectileX: Float = 0f,
        var projectileY: Float = 0f,
        var aimX: Float = 1f,
        var aimY: Float = 0f,
        var distance: Float = 0f
    )
    private val states = MutableList(definitions.size) { State() }
    var tickCount: Int = 0; private set

    private val floorDefs = definitions.filter { it.type == "collapse_floor" }
    private var cachedViews: List<TrapView> = emptyList()
    private var cachedSolids: List<HitBox> = emptyList()
    private var cachedPlatforms: List<HitBox> = emptyList()
    private var cachedSpikes: List<HitBox> = emptyList()
    private var cachedCrushers: List<HitBox> = emptyList()
    private var cachedPushBlocks: List<TrapView> = emptyList()
    private var cachedCollapsedGaps: List<GroundGap> = emptyList()

    private val projectileTypes = setOf("flying_spike", "homing_spike", "arc_spike")
    private val platformTypes = setOf("moving_platform", "falling_platform", "flee_platform", "split_platform")
    private val crusherTypes = setOf("crushing_ceiling", "crushing_wall")

    init { refreshCache() }

    fun reset() {
        tickCount = 0
        states.forEach { s ->
            s.firedAt = -1; s.projectileX = 0f; s.projectileY = 0f
            s.aimX = 1f; s.aimY = 0f; s.distance = 0f
        }
        refreshCache()
    }

    private fun activated(d: TrapDefinition, s: State): Boolean =
        s.firedAt >= 0 && tickCount - s.firedAt >= d.delayTicks

    private fun progress(d: TrapDefinition, s: State): Float {
        if (s.firedAt < 0) return 0f
        return ((tickCount - s.firedAt - d.delayTicks).toFloat() / d.transitionTicks.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    private fun fire(s: State, d: TrapDefinition, player: HitBox) {
        s.firedAt = tickCount
        if (d.type !in projectileTypes) return
        s.projectileX = d.x + d.width / 2f
        s.projectileY = d.y + d.height / 2f
        s.distance = 0f
        val centerX = (player.left + player.right) * .5f
        val centerY = (player.top + player.bottom) * .5f
        val dx = centerX - s.projectileX
        val dy = centerY - s.projectileY
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(.001f)
        s.aimX = dx / length
        s.aimY = dy / length
    }

    fun update(player: HitBox, previous: HitBox, didJump: Boolean, horizontalSpeed: Float) {
        tickCount++
        for (i in definitions.indices) {
            val d = definitions[i]
            val s = states[i]
            // Projectiles, disappearing floors and one-off tricks do not re-arm during one life.
            val repeatable = d.type in setOf("rising_spikes", "surprise_wall", "reverse_zone")
            if (repeatable && s.firedAt >= 0 && d.durationTicks > 0 &&
                tickCount - s.firedAt >= d.delayTicks + d.durationTicks) s.firedAt = -1
            if (s.firedAt < 0) {
                val centerX = (player.left + player.right) * .5f
                val distanceX = abs(centerX - (d.x + d.width * .5f))
                val inZone = player.right > d.x && player.left < d.x + d.width &&
                    player.bottom > d.y - 80f && player.top < d.y + d.height + 80f
                val eligible = when (d.trigger) {
                    "player_near" -> distanceX <= d.radius && abs(player.bottom - d.y) < max(180f, d.radius)
                    "player_on" -> player.right > d.x && player.left < d.x + d.width &&
                        previous.bottom <= d.y + 4f && player.bottom >= d.y - 4f
                    "player_jump" -> didJump && distanceX <= d.radius
                    "approach_fast" -> distanceX <= d.radius && abs(horizontalSpeed) >= d.thresholdSpeed
                    "player_enter" -> inZone
                    "player_passed" -> player.left >= (d.triggerX ?: d.x)
                    else -> false
                }
                if (eligible) fire(s, d, player)
            }
            if (d.type in projectileTypes && activated(d, s) && s.distance < d.projectileRange) {
                if (d.type == "homing_spike") {
                    val dx = (player.left + player.right) * .5f - s.projectileX
                    val dy = (player.top + player.bottom) * .5f - s.projectileY
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(.001f)
                    // Controlled steering, not an instantaneous 180-degree turn.
                    val blendX = s.aimX * .90f + dx / len * .10f
                    val blendY = s.aimY * .90f + dy / len * .10f
                    val blendLen = sqrt(blendX * blendX + blendY * blendY).coerceAtLeast(.001f)
                    s.aimX = blendX / blendLen; s.aimY = blendY / blendLen
                }
                val step = minOf(d.projectileSpeed / 60f, d.projectileRange - s.distance)
                s.projectileX += s.aimX * step
                s.projectileY += s.aimY * step
                s.distance += step
            }
        }
        refreshCache()
    }

    fun viewAt(index: Int): TrapView {
        val d = definitions[index]; val s = states[index]
        val active = activated(d, s); val p = progress(d, s)
        val box: HitBox = when (d.type) {
            "moving_platform" -> {
                val wave = ((sin(2.0 * Math.PI * tickCount / d.periodTicks.toDouble()) + 1.0) * .5).toFloat()
                HitBox(d.x + d.travelX * wave, d.y + d.travelY * wave,
                    d.x + d.width + d.travelX * wave, d.y + d.height + d.travelY * wave)
            }
            "flee_platform" -> HitBox(d.x + p * d.travelX, d.y + p * d.travelY,
                d.x + d.width + p * d.travelX, d.y + d.height + p * d.travelY)
            "falling_platform" -> {
                val seconds = (tickCount - s.firedAt - d.delayTicks).coerceAtLeast(0) / 60f
                val dy = (0.5f * d.fallAcceleration * seconds * seconds).coerceAtMost(1100f)
                HitBox(d.x, d.y + dy, d.x + d.width, d.y + d.height + dy)
            }
            "split_platform" -> {
                val half = d.width / 2f; val travel = abs(d.travelX) * p
                HitBox(d.x - travel, d.y, d.x + half - travel, d.y + d.height)
            }
            "rising_spikes" -> {
                val slide = 1f - p
                val dx = when (d.orientation) { "left" -> d.width * slide; "right" -> -d.width * slide; else -> 0f }
                val dy = when (d.orientation) { "down" -> -d.height * slide; "up" -> d.height * slide; else -> 0f }
                HitBox(d.x + dx, d.y + dy, d.x + d.width + dx, d.y + d.height + dy)
            }
            "surprise_wall" -> HitBox(d.x, d.y + d.height * (1f - p), d.x + d.width, d.y + d.height * (2f - p))
            in projectileTypes -> {
                val arcOffset = if (d.type == "arc_spike" && active) {
                    sin(Math.PI * (s.distance / d.projectileRange).coerceIn(0f, 1f)).toFloat() * d.arcHeight
                } else 0f
                val px = if (s.firedAt >= 0) s.projectileX else d.x + d.width / 2f
                val py = (if (s.firedAt >= 0) s.projectileY else d.y + d.height / 2f) - arcOffset
                HitBox(px - d.width / 2f, py - d.height / 2f, px + d.width / 2f, py + d.height / 2f)
            }
            else -> HitBox(d.x + p * d.travelX, d.y + p * d.travelY,
                d.x + d.width + p * d.travelX, d.y + d.height + p * d.travelY)
        }
        val state = when {
            d.type in projectileTypes && active && s.distance >= d.projectileRange -> "spent"
            d.type == "moving_platform" -> "moving"
            !active && s.firedAt < 0 && d.type in platformTypes -> "idle"
            !active && s.firedAt < 0 -> "hidden"
            !active -> if (d.showWarning) "warning" else "hidden"
            else -> "active"
        }
        return TrapView(d.id, d.type, box, state)
    }

    private fun refreshCache() {
        val base = definitions.indices.map { viewAt(it) }
        cachedViews = buildList {
            for (i in definitions.indices) {
                val d = definitions[i]; val v = base[i]
                add(v)
                if (d.type == "split_platform") {
                    val p = progress(d, states[i]); val half = d.width / 2f; val travel = abs(d.travelX) * p
                    add(TrapView(d.id + "_right", d.type,
                        HitBox(d.x + half + travel, d.y, d.x + d.width + travel, d.y + d.height), v.state))
                }
            }
        }
        cachedSolids = buildList {
            for (i in definitions.indices) {
                val d = definitions[i]
                when (d.type) {
                    "collapse_floor" -> if (!activated(d, states[i])) add(base[i].box)
                    "surprise_wall" -> if (activated(d, states[i])) add(base[i].box)
                }
            }
        }
        cachedPlatforms = cachedViews.filter { it.type in platformTypes }.map { it.box }
        cachedSpikes = cachedViews.filter { v ->
            v.state == "active" && (v.type == "rising_spikes" || v.type in projectileTypes)
        }.map { it.box }
        cachedCrushers = cachedViews.filter { it.type in crusherTypes && it.state == "active" }.map { it.box }
        cachedPushBlocks = cachedViews.filter { it.type == "push_block" && it.state == "active" }
        cachedCollapsedGaps = definitions.indices.filter {
            definitions[it].type == "collapse_floor" && activated(definitions[it], states[it])
        }.map { GroundGap(definitions[it].id, definitions[it].x, definitions[it].width) }
    }

    fun views(): List<TrapView> = cachedViews
    fun floorDefinitions(): List<TrapDefinition> = floorDefs
    fun collapsedGaps(): List<GroundGap> = cachedCollapsedGaps
    /** Expose activated floor fragments for the industrial art layer only.
     *  Uses the existing fallAcceleration/60Hz tick model; no collision change.
     */
    fun fallingGroundPieces(): List<FallingGroundPiece> = buildList {
        for (i in definitions.indices) {
            val d = definitions[i]
            val s = states[i]
            if (d.type != "collapse_floor" || !activated(d, s)) continue
            val fallingTicks = (tickCount - s.firedAt - d.delayTicks).coerceAtLeast(0)
            val seconds = fallingTicks / 60f
            val drop = (0.5f * d.fallAcceleration * seconds * seconds).coerceAtMost(1500f)
            add(FallingGroundPiece(d.id, d.x, d.width, drop))
        }
    }
    fun solids(): List<HitBox> = cachedSolids
    fun platforms(): List<HitBox> = cachedPlatforms
    fun spikes(): List<HitBox> = cachedSpikes
    fun crushers(): List<HitBox> = cachedCrushers
    fun pushBlocks(): List<TrapView> = cachedPushBlocks

    fun exit(base: HitBox): HitBox {
        val index = definitions.indexOfFirst { it.type == "moving_exit" }
        return if (index >= 0) viewAt(index).box else base
    }

    fun reverseControls(player: HitBox): Boolean = definitions.indices.any { i ->
        val d = definitions[i]
        d.type == "reverse_zone" && player.intersects(d.box)
    }

    fun hasType(type: String) = definitions.any { it.type == type }
}
