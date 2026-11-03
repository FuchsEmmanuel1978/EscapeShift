package com.escapeshift.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class HitBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(other: HitBox): Boolean = left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class GameInput(val left: Boolean = false, val right: Boolean = false, val jump: Boolean = false, val shift: Boolean = false)

enum class StartType { OPEN, CRATE_WALL, SHIFT_BRIDGE, SPRING_WALL, UPPER_PLATFORM }
enum class ChallengeType { SPIKE, RED_LASER, BLUE_LASER, TIMED_LASER, KEY, SPRING, SAW, CONVEYOR, SWITCH, MONSTER, GAP, WALL }

/** Route checkpoints are references to JSON objects, never a second set of coordinates. */
data class Challenge(
    val type: ChallengeType, val x: Float, val phase: Int = 0,
    val y: Float = 0f, val width: Float = 0f, val height: Float = 0f,
    val amplitude: Float = 0f, val period: Int = 120,
    val activeTicks: Int = 64, val offsetX: Float = 0f, val speed: Float = -145f
)

data class LevelSpec(
    val number: Int,
    val title: String,
    val intro: StartType,
    val challenges: List<Challenge>,
    val exitX: Float,
    val worldRight: Float,
    val requiresKey: Boolean,
    val requiresSwitch: Boolean,
    val backtrackCount: Int,
    val hint: String,
    val startX: Float,
    val startY: Float,
    val groundY: Float,
    val groundDepth: Float,
    val gaps: List<GroundGap>,
    val objects: List<LevelObject>
)

data class MonsterSpawn(
    val anchorX: Float, val yTop: Float, val amplitude: Float,
    val width: Float, val height: Float, val phase: Int,
    val period: Int = 130
)

/** Same V18.1 physics; the geometry is exclusively loaded from level JSONs. */
class GameWorld(val levelNumber: Int = 1) {
    companion object {
        const val STEP = 1f / 60f
        const val SPEED = 250f
        const val JUMP_SPEED = -420f
        const val SPRING_SPEED = -655f
        const val GRAVITY = 980f
        const val SIZE = 39f
        const val CRATE_WIDTH = 46f
        const val CRATE_HEIGHT = 44f
        const val GROUND_Y = 444f
    }
    val spec = LevelCatalog.create(levelNumber)
    val worldRight = spec.worldRight
    private fun objects(type: String) = spec.objects.filter { it.type == type }
    private fun challenge(o: LevelObject, type: ChallengeType) = Challenge(
        type, o.x, o.phase, o.y, o.width, o.height, o.amplitude,
        o.period, o.activeTicks, o.offsetX, o.speed
    )
    private val crateConfigs = objects("crate")
    private val crateConfig = crateConfigs.firstOrNull()
    private var cratePositions = crateConfigs.map { it.x }.toMutableList()
    val hasCrate = crateConfigs.isNotEmpty()
    private val crateY = crateConfig?.y ?: -5000f
    private val crateWidth = crateConfig?.width ?: CRATE_WIDTH
    private val crateHeight = crateConfig?.height ?: CRATE_HEIGHT
    /** All crate hitboxes, including those used on pressure plates. */
    val crateBoxes: List<HitBox> get() = crateConfigs.indices.map { i ->
        val config = crateConfigs[i]; val px = cratePositions[i]
        HitBox(px, config.y, px + config.width, config.y + config.height)
    }
    val crateSwitches = objects("crate_switch").map { it.box }
    fun isPlateActive(index: Int): Boolean = crateBoxes.any { crate ->
        val pad = crateSwitches[index]
        crate.right > pad.left + 12f && crate.left < pad.right - 12f &&
            crate.bottom >= pad.top && crate.top < pad.bottom
    }
    val pressureReady: Boolean get() = crateSwitches.indices.all { isPlateActive(it) }
    fun isSwitchTouched(index: Int): Boolean = switchTouched.getOrNull(index) == true
    val snowflakes = objects("snowflake")
    fun snowflakeBox(index: Int, atTick: Int = ticks): HitBox {
        val s = snowflakes[index]
        val floor = spec.groundY + s.height
        val range = (floor - s.y).coerceAtLeast(40f)
        val fall = ((atTick + s.phase) * s.speed * STEP) % range
        val yy = s.y + fall
        val xx = s.x + s.amplitude * sin((atTick + s.phase) * (2 * Math.PI / s.period.toDouble())).toFloat()
        return HitBox(xx, yy, xx + s.width, yy + s.height)
    }
    val bridge: HitBox? = objects("bridge").firstOrNull()?.box
    val exit = objects("door").single().box

    val blocks: List<HitBox> = buildList {
        // Ground is a single baseline with explicit empty intervals (gaps).
        // Widen a gap in JSON to make a jump longer; no hidden code must be changed.
        var cursor = 0f
        for (gap in spec.gaps) {
            if (gap.x > cursor) add(HitBox(cursor, spec.groundY, gap.x, spec.groundY + spec.groundDepth))
            cursor = gap.x + gap.width
        }
        if (cursor < spec.worldRight) add(HitBox(cursor, spec.groundY, spec.worldRight, spec.groundY + spec.groundDepth))
        addAll(objects("wall").map { it.box })
        addAll(objects("platform").map { it.box })
    }
    /** The player can jump up THROUGH platforms, then stand on top. */
    val oneWayPlatforms: List<HitBox> = objects("platform").map { it.box }
    val spikes: List<HitBox> = objects("spike").flatMap { o ->
        (0 until o.count).map { index ->
            val left = o.x + index * (o.width + o.spacing)
            HitBox(left, o.y, left + o.width, o.y + o.height)
        }
    }
    val lasers = objects("laser_red").map { it.box }
    val invertedLasers = objects("laser_blue").map { it.box }
    val timedLasers = objects("laser_timed").map { challenge(it, ChallengeType.TIMED_LASER) }
    val springs = objects("spring").map { it.box }
    val keys = objects("key").map { it.box }
    val switches = objects("switch").map { it.box }
    val conveyors = objects("conveyor").map { it.box }
    private val conveyorSpeeds = objects("conveyor").map { it.box to it.speed }
    val coins = objects("coin").map { it.box }
    val saws = objects("saw").map { challenge(it, ChallengeType.SAW) }
    val monsters: List<MonsterSpawn> = objects("monster").map { o ->
        MonsterSpawn(o.x + o.offsetX, o.y, o.amplitude, o.width, o.height, o.phase, o.period)
    }

    var x = spec.startX; private set
    var y = spec.startY; private set
    var vx = 0f; private set
    var vy = 0f; private set
    var grounded = false; private set
    val crateX: Float get() = cratePositions.firstOrNull() ?: -5000f
    var shiftOn = false; private set
    var dead = false; private set
    var won = false; private set
    var keyCollected = false; private set
    var switchActivated = false; private set
    private var switchTouched = BooleanArray(0)
    var elapsed = 0f; private set
    var ticks = 0; private set
    private var jumpPrevious = false
    private var shiftPrevious = false
    private var springCooldown = 0
    private var monsterAlive = MutableList(monsters.size) { true }
    private var coinTaken = BooleanArray(coins.size)
    val coinsCollected: Int get() = coinTaken.count { it }
    fun isCoinVisible(index: Int): Boolean = !coinTaken[index]

    val playerBox get() = HitBox(x, y, x + SIZE, y + SIZE)
    val crateBox get() = crateBoxes.firstOrNull() ?: HitBox(-5000f, -5000f, -4954f, -4956f)
    val exitUnlocked get() = (!spec.requiresKey || keyCollected) &&
        (switches.isEmpty() || switchActivated) && pressureReady
    fun solids(): List<HitBox> = (blocks - oneWayPlatforms).let { if (shiftOn && bridge != null) it + bridge else it }
    fun sawBox(saw: Challenge, t: Int = ticks): HitBox {
        val offset = saw.amplitude * sin((t + saw.phase) * (2 * Math.PI / saw.period.toDouble())).toFloat()
        val left = saw.x + saw.offsetX + offset
        return HitBox(left, saw.y, left + saw.width, saw.y + saw.height)
    }
    fun monsterBox(index: Int, t: Int = ticks): HitBox {
        val m = monsters[index]
        val offset = m.amplitude * sin((t + m.phase) * (2 * Math.PI / m.period.toDouble())).toFloat()
        return HitBox(m.anchorX + offset, m.yTop, m.anchorX + m.width + offset, m.yTop + m.height)
    }
    fun isMonsterAlive(index: Int): Boolean = monsterAlive.getOrNull(index) == true
    fun timedLaserActive(laser: Challenge, t: Int = ticks): Boolean = (t + laser.phase) % laser.period < laser.activeTicks
    fun timedLaserBox(laser: Challenge) = HitBox(laser.x, laser.y, laser.x + laser.width, laser.y + laser.height)

    fun reset() {
        x = spec.startX; y = spec.startY; vx = 0f; vy = 0f; grounded = false
        cratePositions = crateConfigs.map { it.x }.toMutableList()
        shiftOn = false; dead = false; won = false; keyCollected = false; switchActivated = false
        switchTouched = BooleanArray(switches.size)
        elapsed = 0f; ticks = 0; jumpPrevious = false; shiftPrevious = false; springCooldown = 0
        monsterAlive = MutableList(monsters.size) { true }
        coinTaken = BooleanArray(coins.size)
    }

    fun tick(input: GameInput) {
        if (dead || won) return
        ticks++
        elapsed += STEP
        if (input.shift && !shiftPrevious) shiftOn = !shiftOn
        shiftPrevious = input.shift
        val direction = (if (input.right) 1 else 0) - (if (input.left) 1 else 0)
        val conveyorBoost = if (grounded) conveyorSpeeds.firstOrNull { (box, _) -> playerBox.intersects(box.copy(top = box.top - 7f)) }?.second ?: 0f else 0f
        vx = SPEED * direction + conveyorBoost
        if (input.jump && !jumpPrevious && grounded) { vy = JUMP_SPEED; grounded = false }
        jumpPrevious = input.jump
        val dx = vx * STEP
        x += dx
        for (i in crateConfigs.indices) {
            val crate = crateBoxes[i]
            if (!playerBox.intersects(crate)) continue
            if (dx > 0f) {
                val needed = playerBox.right - crate.left
                val moved = pushCrate(i, needed)
                x -= needed - moved
            } else if (dx < 0f) {
                val needed = playerBox.left - crate.right
                val moved = pushCrate(i, needed)
                x -= needed - moved
            }
        }
        for (solid in solids()) {
            if (playerBox.intersects(solid)) {
                if (dx > 0f) x = min(x, solid.left - SIZE)
                if (dx < 0f) x = max(x, solid.right)
            }
        }
        x = max(0f, x)
        vy = min(800f, vy + GRAVITY * STEP)
        val previousBottom = y + SIZE
        val previousTop = y
        y += vy * STEP
        grounded = false
        for (solid in solids() + oneWayPlatforms + crateBoxes) {
            if (!playerBox.intersects(solid)) continue
            if (vy >= 0f && previousBottom <= solid.top + 2f) {
                y = solid.top - SIZE
                vy = 0f
                grounded = true
            } else if (solid !in oneWayPlatforms && vy < 0f && previousTop >= solid.bottom - 2f) {
                y = solid.bottom
                vy = 0f
            }
        }
        if (springCooldown > 0) springCooldown--
        if (grounded && springCooldown == 0 && springs.any { playerBox.intersects(it.copy(top = it.top - 3f)) }) {
            vy = SPRING_SPEED
            grounded = false
            springCooldown = 55
        }
        if (keys.any { playerBox.intersects(it) }) keyCollected = true
        for (i in coins.indices) if (!coinTaken[i] && playerBox.intersects(coins[i])) coinTaken[i] = true
        if (switchTouched.size != switches.size) switchTouched = BooleanArray(switches.size)
        for (i in switches.indices) if (playerBox.intersects(switches[i].copy(top = switches[i].top - 6f))) switchTouched[i] = true
        switchActivated = switches.isNotEmpty() && switchTouched.all { it }

        var player = playerBox
        for (i in monsters.indices) {
            if (!monsterAlive[i]) continue
            val box = monsterBox(i)
            if (player.intersects(box)) {
                val stomping = vy > 0f && previousBottom <= box.top + 10f && player.bottom >= box.top
                if (stomping) {
                    y = box.top - SIZE
                    vy = JUMP_SPEED * 0.72f
                    grounded = false
                    monsterAlive[i] = false
                    player = playerBox
                } else {
                    dead = true
                }
            }
        }
        if (y > 650f || spikes.any { player.intersects(it) } ||
            (!shiftOn && lasers.any { player.intersects(it) }) ||
            (shiftOn && invertedLasers.any { player.intersects(it) }) ||
            timedLasers.any { timedLaserActive(it) && player.intersects(timedLaserBox(it)) } ||
            saws.any { player.intersects(sawBox(it)) }) dead = true
        if (!dead && player.intersects(exit) && exitUnlocked) won = true
    }

    private fun pushCrate(index: Int, amount: Float): Float {
        if (amount == 0f) return 0f
        val config = crateConfigs[index]
        val old = cratePositions[index]
        var target = (old + amount).coerceAtLeast(0f)
        for (solid in blocks) {
            if (solid.bottom <= config.y || solid.top >= config.y + config.height) continue
            val targetBox = HitBox(target, config.y, target + config.width, config.y + config.height)
            if (targetBox.intersects(solid)) {
                if (amount > 0f) target = min(target, solid.left - config.width)
                else target = max(target, solid.right)
            }
        }
        for (other in crateBoxes.filterIndexed { i, _ -> i != index }) {
            val targetBox = HitBox(target, config.y, target + config.width, config.y + config.height)
            if (targetBox.intersects(other)) {
                if (amount > 0f) target = min(target, other.left - config.width)
                else target = max(target, other.right)
            }
        }
        cratePositions[index] = target
        return target - old
    }
}
