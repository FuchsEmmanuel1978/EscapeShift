package com.escapeshift.game

import kotlin.math.abs
import kotlin.math.sin

/**
 * Worlds 3–4 experimental scenes. Inputs are real contact/push/interaction events;
 * the scripted "solution" property in the JSON is never read at runtime.
 * This engine has no Android dependencies and can be tested with GameWorld.tick().
 */
data class AdvancedDevice(
    val id: String, val kind: String, val x: Float, val y: Float,
    val width: Float, val height: Float, val weight: Int = 1,
    val requiredWeight: Int = 1, val text: String = "",
    val symbol: String = "", val angles: List<Int> = emptyList(),
    val targetX: Float = x
) {
    val box get() = HitBox(x, y, x + width, y + height)
}

data class AdvancedSequence(val id: String, val items: List<String>, val decoys: List<String>, val failOnWrong: Boolean)
data class AdvancedCombination(val id: String, val pads: List<String>, val target: Map<String, String>)

data class AdvancedPuzzlePlan(
    val level: Int, val devices: List<AdvancedDevice>,
    val sequences: List<AdvancedSequence>, val combination: AdvancedCombination?
) {
    fun all(kind: String) = devices.filter { it.kind == kind }
    fun find(id: String) = devices.firstOrNull { it.id == id }
    companion object {
        fun fromJson(level: Int, value: Any?): AdvancedPuzzlePlan? {
            if (level !in 24..30) return null
            val root = value as? Map<*, *> ?: error("Level $level needs 'puzzle'")
            val entries = root["devices"] as? List<*> ?: error("Missing puzzle.devices on $level")
            val devices = entries.map { raw ->
                val d = raw as? Map<*, *> ?: error("Invalid device on $level")
                fun number(k: String, fallback: Float = 0f) = (d[k] as? Number)?.toFloat() ?: fallback
                fun integer(k: String, fallback: Int = 1) = (d[k] as? Number)?.toInt() ?: fallback
                val x = number("x"); val y = number("y")
                AdvancedDevice(
                    d["id"] as? String ?: error("Missing device id"),
                    d["kind"] as? String ?: error("Missing device kind"), x, y,
                    number("width"), number("height"), integer("weight"),
                    integer("requiredWeight"), d["text"] as? String ?: "",
                    (d["symbol"] ?: d["letter"]) as? String ?: "",
                    (d["angles"] as? List<*>)?.map { (it as Number).toInt() } ?: emptyList(),
                    number("targetX", x)
                ).also { require(it.width > 0f && it.height > 0f && it.weight >= 1) }
            }
            require(devices.map { it.id }.distinct().size == devices.size) { "Duplicate device on $level" }
            val sequences = (root["sequences"] as? List<*>)?.map { raw ->
                val s = raw as Map<*, *>
                AdvancedSequence(s["id"] as String, (s["items"] as List<*>).map { it as String },
                    (s["decoys"] as? List<*>)?.map { it as String } ?: emptyList(), s["failOnWrong"] != false)
            } ?: emptyList()
            val combo = (root["combination"] as? Map<*, *>)?.let { c ->
                AdvancedCombination(c["id"] as String,
                    (c["pads"] as List<*>).map { it as String },
                    (c["target"] as Map<*, *>).entries.associate { it.key as String to it.value as String })
            }
            return AdvancedPuzzlePlan(level, devices, sequences, combo)
        }
    }
}

/**
 * World state is exclusively derived from objects in the scene:
 *  - crates on plates, including their identities/weights
 *  - pickup order (touch actual token boxes)
 *  - wheel rotation (ACTION while near a wheel)
 *  - pushable bridge/spring positions and real gap crossing
 *  - geometry-based zone entry, wall/spike avoidance and platform landing.
 */
class AdvancedPuzzle(val plan: AdvancedPuzzlePlan) {
    private val offsets = mutableMapOf<String, Float>()
    private val angles = mutableMapOf<String, Int>()
    private val tokenCollected = mutableSetOf<String>()
    private val sequenceProgress = mutableMapOf<String, Int>()
    private var previousInversePressed = false
    private var inverseReleased = false
    private var heavyLatched = false
    private var platformVisited = false
    private var crossedGap = false
    private val avoided = mutableSetOf<String>()
    private var wordActivated = false
    private var fakeDoorRevealed = false
    private var gravityEntered = false
    private var reverseEntered = false
    private var wrongToken = false
    private var ticks = 0
    private var lastCorrect = 0
    var feedback: String = ""; private set
    var isSolved = false; private set
    var hasFailed = false; private set
    var gravityActive = false; private set
    var controlsReversed = false; private set
    private var heroWasBeforeGap = false

    val crateDevices get() = plan.all("crate")
    val devices get() = plan.devices
    val sequenceStatus: String get() = plan.sequences.joinToString(" ") { seq ->
        "${seq.id}:${sequenceProgress[seq.id] ?: 0}/${seq.items.size}"
    }
    fun reset() {
        offsets.clear(); angles.clear(); tokenCollected.clear(); sequenceProgress.clear()
        previousInversePressed = false; inverseReleased = false; heavyLatched = false
        platformVisited = false; crossedGap = false; avoided.clear()
        wordActivated = false; fakeDoorRevealed = false; gravityEntered = false; reverseEntered = false
        wrongToken = false; ticks = 0; lastCorrect = 0; feedback = ""; isSolved = false; hasFailed = false
        gravityActive = false; controlsReversed = false; heroWasBeforeGap = false
        lastCrates = emptyList(); lastPlayerX = -1000f
    }
    fun box(device: AdvancedDevice): HitBox {
        val offset = offsets[device.id] ?: 0f
        return HitBox(device.x + offset, device.y, device.x + device.width + offset, device.y + device.height)
    }
    fun box(id: String): HitBox? = plan.find(id)?.let { box(it) }
    fun wheelAngle(id: String): Int = angles[id] ?: 0
    fun isCollected(id: String): Boolean = id in tokenCollected
    fun isPressed(id: String, player: HitBox, crates: List<HitBox>): Boolean {
        val pad = plan.find(id) ?: return false
        val count = crateDevices.indices.count { i -> crates.getOrNull(i)?.let { crate -> crateTouches(pad, crate) } == true }
        return count > 0 || (pad.kind == "heavy_pressure_plate" && playerTouches(pad, player))
    }
    private fun crateTouches(d: AdvancedDevice, c: HitBox): Boolean =
        (c.left + c.right) * .5f in d.x..(d.x + d.width) && c.bottom >= d.y - 6f && c.top < d.y + d.height
    private fun playerTouches(d: AdvancedDevice, p: HitBox): Boolean =
        (p.left + p.right) * .5f in d.x..(d.x + d.width) && p.bottom >= d.y - 10f && p.top <= d.y + d.height
    private fun onPad(id: String, crates: List<HitBox>): String? {
        val d = plan.find(id) ?: return null
        return crateDevices.indices.firstOrNull { crates.getOrNull(it)?.let { crate -> crateTouches(d, crate) } == true }?.let { crateDevices[it].id }
    }
    private fun padWeight(id: String, player: HitBox, crates: List<HitBox>): Int {
        val pad = plan.find(id) ?: return 0
        val weight = crateDevices.indices.sumOf { i -> if (crates.getOrNull(i)?.let { crate -> crateTouches(pad, crate) } == true) crateDevices[i].weight else 0 }
        return weight + if (playerTouches(pad, player)) 1 else 0
    }
    private fun at(id: String, target: String, crates: List<HitBox>): Boolean = onPad(target, crates) == id
    fun movableBodies(): List<HitBox> = plan.devices.filter { it.kind in setOf("sliding_platform", "movable_spring") }.map { box(it) }
    fun pushMovable(deviceId: String, dx: Float): Float {
        val d = plan.find(deviceId) ?: return 0f
        if (d.kind !in setOf("sliding_platform", "movable_spring")) return 0f
        val before = offsets[deviceId] ?: 0f
        val target = (d.targetX - d.x).coerceAtLeast(0f)
        val next = (before + dx).coerceIn(0f, target)
        offsets[deviceId] = next
        return next - before
    }
    fun platformBoxes(): List<HitBox> = plan.devices.filter { it.kind in setOf("moving_platform", "adjustable_platform", "lowering_platform", "sliding_platform") }.map { d ->
        val base = box(d)
        when (d.kind) {
            "moving_platform" -> {
                val progress = if (plan.level == 33) at("crate2", "pad1", lastCrates) && at("crate3", "pad3", lastCrates) else true
                val amplitude = if (progress) 24f else 105f
                val phase = sin(ticks * (if (progress) .025 else .09)) * amplitude
                HitBox(base.left + phase.toFloat(), base.top, base.right + phase.toFloat(), base.bottom)
            }
            "adjustable_platform" -> {
                val ok = wheelAngle("wheelA") == 135
                val y = if (ok) base.top + 55f else base.top
                base.copy(top = y, bottom = y + d.height)
            }
            "lowering_platform" -> {
                val y = if (heavyLatched) 405f else base.top
                base.copy(top = y, bottom = y + d.height)
            }
            else -> base
        }
    }
    private var lastCrates: List<HitBox> = emptyList()
    private var lastPlayerX: Float = -1000f
    fun wallBoxes(): List<HitBox> = buildList {
        plan.all("lift_wall").forEach { d -> if (!wallsAligned()) add(box(d)) }
        plan.all("surprise_wall").forEach { d ->
            // The trap springs up only when the player approaches.
            if (abs(lastPlayerX - d.x) < 200f) add(box(d))
        }
    }
    fun spikeBoxes(): List<HitBox> = plan.all("moving_spike").map { d ->
        val b = box(d); b.copy(top = b.top + (if (ticks % 150 < 65) 0f else d.height * .75f))
    }
    fun tokenBoxes() = plan.devices.filter { it.kind.endsWith("_token") && it.id !in tokenCollected }
    fun wheelDevices() = plan.all("rotating_wheel")
    fun springBoxes() = plan.devices.filter { it.kind == "spring" || it.kind == "movable_spring" }.map { box(it) }
    fun gravityZones() = plan.all("gravity_zone")
    fun reverseZones() = plan.all("reverse_controls_zone")
    fun groundGaps() = plan.all("ground_gap").map { it.box }
    fun onPlatformLanding(index: Int) { if (index >= 0) platformVisited = true }
    private fun wheelsGood() = when (plan.level) { 26 -> wheelAngle("wheelA") == 135; 32 -> wheelAngle("wheelA") == 180; 38 -> wheelAngle("wheelA") == 270; 40 -> wheelAngle("wheelA") == 90; else -> true }
    private fun wallsAligned() = when (plan.level) {
        28 -> at("crateA", "plateB", lastCrates) && at("crateB", "plateA", lastCrates)
        38 -> at("crateA", "padB", lastCrates) && at("crateB", "padA", lastCrates)
        else -> true
    }
    private fun springAligned(): Boolean = plan.all("movable_spring").all { d ->
        abs(box(d).left - d.targetX) <= 12f
    }
    private fun bridgeAligned(): Boolean = plan.all("sliding_platform").all { d ->
        abs(box(d).left - d.targetX) <= 9f
    }
    private fun sequencesDone() = plan.sequences.all { (sequenceProgress[it.id] ?: 0) == it.items.size }
    private fun codeMatches(): Boolean {
        val combo = plan.combination ?: return true
        return combo.pads.all { p -> onPad(p, lastCrates) == combo.target[p] }
    }
    fun interact(player: HitBox): Boolean {
        // Interact only when the hero physically stands next to the wheel / inscription.
        val heroX = (player.left + player.right) / 2f
        val wheel = wheelDevices().minByOrNull { abs(heroX - (box(it).left + box(it).right) / 2f) }
        if (wheel != null && abs(heroX - (box(wheel).left + box(wheel).right) / 2f) < 100f) {
            val choices = wheel.angles.ifEmpty { listOf(0,45,90,135,180,225,270,315) }
            val old = angles[wheel.id] ?: choices.first()
            angles[wheel.id] = choices[(choices.indexOf(old).coerceAtLeast(0) + 1) % choices.size]
            feedback = "${wheel.id}: ${angles[wheel.id]}°"
            return true
        }
        val board = plan.devices.filter { it.kind == "word_board" }.firstOrNull {
            abs(heroX - (it.x + it.width / 2f)) < 115f
        }
        if (board != null) {
            wordActivated = when (plan.level) { 36 -> board.text == "RADAR"; 40 -> board.text == "HOT"; else -> true }
            feedback = if (wordActivated) "OK" else "?"
            return true
        }
        return false
    }
    fun tick(player: HitBox, crates: List<HitBox>, airborne: Boolean, vx: Float) {
        ticks++
        lastPlayerX = player.left
        lastCrates = crates
        gravityActive = gravityZones().any { player.intersects(it.box) }
        controlsReversed = reverseZones().any { player.intersects(it.box) }
        if (gravityActive) gravityEntered = true
        if (controlsReversed) reverseEntered = true
        for (d in tokenBoxes()) {
            if (!player.intersects(d.box)) continue
            tokenCollected += d.id
            for (seq in plan.sequences) {
                if (d.id !in seq.items && d.id !in seq.decoys) continue
                val progress = sequenceProgress[seq.id] ?: 0
                if (progress < seq.items.size && d.id == seq.items[progress]) {
                    sequenceProgress[seq.id] = progress + 1
                    feedback = "${seq.id}: ${progress + 1}/${seq.items.size}"
                } else if (seq.failOnWrong) {
                    wrongToken = true
                    feedback = "X: ${d.id}"
                }
            }
        }
        val inv = plan.all("inverse_button").firstOrNull()
        val invPressed = inv != null && (crateDevices.indices.any { crateTouches(inv, crates[it]) } || playerTouches(inv, player))
        if (previousInversePressed && !invPressed) inverseReleased = true
        previousInversePressed = invPressed
        val heavy = plan.all("heavy_pressure_plate")
        if (heavy.any { padWeight(it.id, player, crates) >= it.requiredWeight }) heavyLatched = true
        val gap = groundGaps().firstOrNull()
        if (gap != null) {
            if (player.right < gap.left - 4f) heroWasBeforeGap = true
            if (heroWasBeforeGap && player.left > gap.right + 4f) crossedGap = true
        }
        for (wall in plan.devices.filter { it.kind == "surprise_wall" || it.kind == "moving_spike" }) {
            if (airborne && player.left > wall.x + wall.width + 6f) avoided += wall.id
        }
        if (plan.level == 39 && plan.all("false_exit").any { player.intersects(it.box) }) fakeDoorRevealed = true
        val combo = plan.combination
        if (combo != null) {
            val correct = combo.pads.count { onPad(it, crates) == combo.target[it] }
            if (correct != lastCorrect) { lastCorrect = correct; feedback = "$correct/${combo.pads.size}" }
        }
        val complete = when (plan.level) {
            24 -> heavyLatched && at("crateB", "springA", crates)
            25,36 -> codeMatches() && (plan.level != 36 || wordActivated)
            26 -> wheelsGood() && crossedGap
            27,29 -> sequencesDone()
            28 -> wallsAligned() && springAligned()
            30 -> sequencesDone() && gravityEntered && reverseEntered
            31 -> bridgeAligned() && padWeight("plateA", player, crates) >= 5
            32 -> sequencesDone() && wheelsGood()
            33 -> at("crate2", "pad1", crates) && at("crate3", "pad3", crates) && gravityEntered && platformVisited
            34 -> inverseReleased && springAligned()
            35 -> sequencesDone() && "wallA" in avoided && "spikeA" in avoided
            37 -> sequencesDone() && gravityEntered && platformVisited
            38 -> wheelsGood() && wallsAligned() && springAligned()
            39 -> sequencesDone() && at("crateA", "padA", crates) && fakeDoorRevealed
            40 -> sequencesDone() && padWeight("padA", player, crates) >= 5 && wheelsGood() && wordActivated && bridgeAligned()
            else -> false
        }
        if (wrongToken) { hasFailed = true; isSolved = false }
        else if (complete) isSolved = true // Latched after genuine, physical solution.
    }
}
