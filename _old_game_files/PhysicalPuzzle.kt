package com.escapeshift.game

import kotlin.math.abs
import kotlin.math.sin

/**
 * Physical, offline puzzle runtime for levels 21, 22 and 23.
 * All dimensions are read from puzzle.devices in the level JSON.
 * No Android dependency: the exact same state machine runs in unit tests.
 */
data class PuzzleDevice(
    val id: String, val kind: String, val x: Float, val y: Float,
    val width: Float, val height: Float,
    val targetX: Float = x, val travel: Float = 0f,
    val windowTicks: Int = 120, val baseSpeed: Float = 130f,
    val speedFactor: Float = 1f,
    val syncedTravel: Float = 16f, val unsyncedTravel: Float = 90f,
    val syncPeriodTicks: Int = 190, val unsyncPeriodTicks: Int = 60
) {
    val box get() = HitBox(x, y, x + width, y + height)
}

data class PhysicalPuzzlePlan(val number: Int, val devices: List<PuzzleDevice>) {
    fun first(kind: String) = devices.firstOrNull { it.kind == kind }
    fun all(kind: String) = devices.filter { it.kind == kind }

    companion object {
        fun fromJson(number: Int, raw: Any?): PhysicalPuzzlePlan? {
            if (number !in 21..23) return null
            val root = raw as? Map<*, *> ?: error("level_$number needs a puzzle object")
            val devices = (root["devices"] as? List<*>)?.map { rawDevice ->
                val d = rawDevice as? Map<*, *> ?: error("Invalid puzzle device")
                fun n(k: String, default: Float = 0f) = (d[k] as? Number)?.toFloat() ?: default
                fun i(k: String, default: Int) = (d[k] as? Number)?.toInt() ?: default
                val x = n("x"); val y = n("y"); val width = n("width"); val height = n("height")
                require(width > 0f && height > 0f && x >= 0f) { "Invalid device size ${d["id"]}" }
                PuzzleDevice(
                    d["id"] as? String ?: error("Missing device id"),
                    d["kind"] as? String ?: error("Missing device kind"),
                    x, y, width, height,
                    n("targetX", x), n("travel", 0f),
                    i("windowTicks", 120), n("baseSpeed", 130f), n("speedFactor", 1f),
                    n("syncedTravel", 16f), n("unsyncedTravel", 90f),
                    i("syncPeriodTicks", 190), i("unsyncPeriodTicks", 60)
                )
            } ?: error("level_$number is missing puzzle.devices")
            require(devices.map { it.id }.distinct().size == devices.size)
            for (d in devices) require(d.syncPeriodTicks > 0 && d.unsyncPeriodTicks > 0 &&
                d.syncedTravel >= 0f && d.unsyncedTravel >= 0f && d.windowTicks > 0)
            fun needed(kind: String, count: Int = 1) = require(devices.count { it.kind == kind } == count) {
                "Level $number needs $count x $kind"
            }
            when (number) {
                21 -> { needed("sliding_platform"); needed("ground_gap"); needed("crate"); needed("pressure_button") }
                22 -> { needed("pushable_wheel"); needed("timed_button"); needed("laser") }
                23 -> { needed("crate", 2); needed("speed_button", 4); needed("moving_platform", 3) }
            }
            return PhysicalPuzzlePlan(number, devices)
        }
    }
}

class PhysicalPuzzle(val plan: PhysicalPuzzlePlan) {
    private val sliding = plan.first("sliding_platform")
    private val wheel = plan.first("pushable_wheel")
    private val pads = plan.all("speed_button")
    private val platforms = plan.all("moving_platform")
    private val laser = plan.first("laser")
    private val pressure = plan.first("pressure_button")
    private val timerButton = plan.first("timed_button")
    private val groundHole = plan.first("ground_gap")

    private var sliderX = sliding?.x ?: 0f
    private var wheelX = wheel?.x ?: 0f
    private var wasWheelOnButton = false
    private var laserTicks = 0
    private var crossingRecorded = false
    private var visitedFarSide = false
    private var returned = false
    private var sync = false
    private val landed = linkedSetOf<String>()
    private var ticks = 0
    private val padPressed = mutableMapOf<String, Boolean>()

    fun reset() {
        sliderX = sliding?.x ?: 0f; wheelX = wheel?.x ?: 0f
        wasWheelOnButton = false; laserTicks = 0; crossingRecorded = false
        visitedFarSide = false; returned = false; sync = false
        landed.clear(); ticks = 0; padPressed.clear()
    }

    val sliderBox: HitBox? get() = sliding?.let { HitBox(sliderX, it.y, sliderX + it.width, it.y + it.height) }
    val wheelBox: HitBox? get() = wheel?.let { HitBox(wheelX, it.y, wheelX + it.width, it.y + it.height) }
    val buttonBoxes: List<HitBox> get() = (pads + listOfNotNull(pressure, timerButton)).map { it.box }
    val buttonStates: List<Boolean> get() = (pads + listOfNotNull(pressure, timerButton)).map { padPressed[it.id] == true }
    val gap: HitBox? get() = groundHole?.box
    val laserBox: HitBox? get() = laser?.box
    val laserActive: Boolean get() = laserTicks <= 0
    val laserRemainingTicks: Int get() = laserTicks
    val isSynced: Boolean get() = sync
    val landedCount: Int get() = landed.size
    val isBridgePlaced: Boolean get() = sliding != null && abs(sliderX - sliding.targetX) < 2f
    val isReturning: Boolean get() = returned
    val isLaserCrossed: Boolean get() = crossingRecorded
    val isSolved: Boolean get() = when (plan.number) {
        21 -> isBridgePlaced && returned && padPressed[pressure?.id] == true
        22 -> crossingRecorded
        23 -> sync && landed.size == 3
        else -> false
    }

    /** Returns amount a movable object actually advanced. */
    fun pushSlider(amount: Float): Float {
        val device = sliding ?: return 0f
        val before = sliderX
        sliderX = (sliderX + amount).coerceIn(device.x, device.targetX)
        return sliderX - before
    }
    fun pushWheel(amount: Float): Float {
        val device = wheel ?: return 0f
        val before = wheelX
        wheelX = (wheelX + amount).coerceIn(device.x, device.targetX)
        return wheelX - before
    }

    private fun crateOnPad(pad: PuzzleDevice, crates: List<HitBox>): Boolean = crates.any {
        val centerX = (it.left + it.right) / 2f
        centerX >= pad.x && centerX <= pad.x + pad.width &&
            it.bottom >= pad.y && it.top <= pad.y + pad.height
    }

    fun update(player: HitBox, crates: List<HitBox>) {
        ticks++
        if (laserTicks > 0) laserTicks--
        pressure?.let { padPressed[it.id] = crateOnPad(it, crates) }
        val wheelCenter = wheelBox?.let { (it.left + it.right) / 2f }
        val onButton = wheelCenter != null && timerButton != null &&
            wheelCenter in timerButton.x..(timerButton.x + timerButton.width)
        if (onButton && !wasWheelOnButton) laserTicks = timerButton!!.windowTicks
        wasWheelOnButton = onButton
        timerButton?.let { padPressed[it.id] = onButton }
        for (pad in pads) padPressed[pad.id] = crateOnPad(pad, crates)
        sync = pads.size == 4 && padPressed[pads[0].id] == true && padPressed[pads[3].id] == true &&
            padPressed[pads[1].id] != true && padPressed[pads[2].id] != true
        if (!sync) landed.clear()
        groundHole?.let { gap ->
            if (isBridgePlaced && player.left > gap.x + gap.width + 10f) visitedFarSide = true
            if (visitedFarSide && player.right < gap.x - 15f) returned = true
        }
        laser?.let { gate ->
            if (!laserActive && player.left > gate.x + gate.width + 2f) crossingRecorded = true
        }
    }

    fun movingPlatforms(): List<HitBox> = platforms.mapIndexed { index, d ->
        // Buttons 1 and 4 synchronize the phases and limit horizontal travel.
        // Wrong buttons result in large, fast, out-of-sync travel.
        val travel = if (sync) d.syncedTravel else d.unsyncedTravel
        val activeButtonFactor = pads.firstOrNull { padPressed[it.id] == true }?.speedFactor ?: 1f
        val period = if (sync) d.syncPeriodTicks else d.unsyncPeriodTicks
        val f = (2f * Math.PI.toFloat() / period) *
            (if (sync) 1f else (index + 1) * activeButtonFactor * (d.baseSpeed / 130f))
        val offset = travel * sin(ticks * f + if (sync) index * 0.18f else index * 2.1f).toFloat()
        HitBox(d.x + offset, d.y, d.x + offset + d.width, d.y + d.height)
    }

    fun onLandMovingPlatform(index: Int) {
        if (!sync || index !in platforms.indices) return
        // Landing sequence: 1 -> 2 -> 3 (no fake 'jump' button victory).
        if (index == landed.size) landed.add(platforms[index].id)
    }
}
