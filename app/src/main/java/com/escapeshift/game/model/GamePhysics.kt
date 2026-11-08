package com.escapeshift.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import com.escapeshift.game.game_elements.adventure.SpikeElements
import com.escapeshift.game.game_elements.adventure.SpikeCollision
import com.escapeshift.game.game_elements.bate.MovingSpikes
import com.escapeshift.game.game_elements.bate.PushBlock
import com.escapeshift.game.game_elements.puzzle.LaserElements

data class HitBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(other: HitBox): Boolean = left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class GameInput(val left: Boolean = false, val right: Boolean = false, val jump: Boolean = false, val shift: Boolean = false, val interact: Boolean = false)

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
    val objects: List<LevelObject>,
    val traps: List<TrapDefinition> = emptyList(),
    val physicalPuzzle: PhysicalPuzzlePlan? = null,
    val advancedPuzzle: AdvancedPuzzlePlan? = null
)

data class MonsterSpawn(
    val anchorX: Float, val yTop: Float, val amplitude: Float,
    val width: Float, val height: Float, val phase: Int,
    val period: Int = 130
)

/** Same V18.1 physics; the geometry is exclusively loaded from level JSONs. */
class GameWorld(val levelNumber: Int = 1, private val previewSpec: LevelSpec? = null) {
    companion object {
        const val STEP = 1f / 60f
        // Input grace periods are expressed in fixed physics ticks (60 Hz).
        const val JUMP_BUFFER_TICKS = 6 // 100 ms
        const val COYOTE_TICKS = 4 // 67 ms
        const val SPEED = 250f
        const val JUMP_SPEED = -420f
        const val SPRING_SPEED = -655f
        const val GRAVITY = 980f
        const val SIZE = 39f
        const val CRATE_WIDTH = 46f
        const val CRATE_HEIGHT = 44f
        const val GROUND_Y = 444f
    }
    val spec = previewSpec ?: LevelCatalog.create(levelNumber)
    val traps = TrapEngine(if (levelNumber in 1..10) spec.traps else emptyList())
    val worldRight = spec.worldRight
    val physicalPuzzle: PhysicalPuzzle? = spec.physicalPuzzle?.let { PhysicalPuzzle(it) }
    val advancedPuzzle: AdvancedPuzzle? = spec.advancedPuzzle?.let { AdvancedPuzzle(it) }
    private var previousPuzzlePlatforms: List<HitBox> = physicalPuzzle?.movingPlatforms().orEmpty()
    private var previousAdvancedPlatforms: List<HitBox> = advancedPuzzle?.platformBoxes().orEmpty()
    private fun objects(type: String) = spec.objects.filter { it.type == type }
    private fun challenge(o: LevelObject, type: ChallengeType) = Challenge(
        type, o.x, o.phase, o.y, o.width, o.height, o.amplitude,
        o.period, o.activeTicks, o.offsetX, o.speed
    )
    private val crateConfigs = objects("crate") + spec.physicalPuzzle?.all("crate").orEmpty().map { d ->
        LevelObject(d.id, "crate", d.x, d.y, d.width, d.height)
    } + spec.advancedPuzzle?.all("crate").orEmpty().map { d ->
        LevelObject(d.id, "crate", d.x, d.y, d.width, d.height)
    }
    private val crateConfig = crateConfigs.firstOrNull()
    private var cratePositions = crateConfigs.map { it.x }.toMutableList()
    // A crate's JSON y is its SPAWN position, not a permanently fixed height.
    private var crateYs = crateConfigs.map { it.y }.toMutableList()
    private var crateVys = MutableList(crateConfigs.size) { 0f }
    private var carriedCrateIndex: Int? = null
    private val lockedCrateIndices = mutableSetOf<Int>()
    private var interactPrevious = false
    val carryingCrate: Boolean get() = carriedCrateIndex != null
    val hasCrate = crateConfigs.isNotEmpty()
    private val crateY = crateConfig?.y ?: -5000f
    private val crateWidth = crateConfig?.width ?: CRATE_WIDTH
    private val crateHeight = crateConfig?.height ?: CRATE_HEIGHT
    /** All crate hitboxes, including those used on pressure plates. */
    val crateBoxes: List<HitBox> get() = crateConfigs.indices.map { i ->
        val config = crateConfigs[i]; val px = cratePositions[i]
        HitBox(px, if (carriedCrateIndex == i) y - config.height - 7f else crateYs[i], px + config.width, if (carriedCrateIndex == i) y - 7f else crateYs[i] + config.height)
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
    val bridges: List<HitBox> = objects("bridge").map { it.box }
    val bridge: HitBox? get() = bridges.firstOrNull() // Compatibility for existing callers
    private val initialExit = objects("door").single().box
    val exit: HitBox get() = traps.exit(initialExit)

    val blocks: List<HitBox> = buildList {
        // Ground is a single baseline with explicit empty intervals (gaps).
        // Widen a gap in JSON to make a jump longer; no hidden code must be changed.
        var cursor = 0f
        val floorCuts = (spec.gaps + traps.floorDefinitions().map { GroundGap(it.id, it.x, it.width) }).sortedBy { it.x }
        for (gap in floorCuts) {
            if (gap.x > cursor) add(HitBox(cursor, spec.groundY, gap.x, spec.groundY + spec.groundDepth))
            cursor = gap.x + gap.width
        }
        if (cursor < spec.worldRight) add(HitBox(cursor, spec.groundY, spec.worldRight, spec.groundY + spec.groundDepth))
        addAll(objects("wall").map { it.box })
        addAll(objects("platform").map { it.box })
    }
    /** The player can jump up THROUGH platforms, then stand on top. */
    val oneWayPlatforms: List<HitBox> = objects("platform").map { it.box }
    private val staticSolids: List<HitBox> = blocks - oneWayPlatforms
    private val fixedSpikeBoxes: List<HitBox> = objects("spike").flatMap { SpikeElements.boxes(it) }
    val activePlatforms get() = oneWayPlatforms + traps.platforms() + physicalPuzzle?.movingPlatforms().orEmpty() + listOfNotNull(physicalPuzzle?.sliderBox) + advancedPuzzle?.platformBoxes().orEmpty()
    val dynamicTrapViews get() = traps.views()
    val dynamicGroundGaps get() = spec.gaps + traps.collapsedGaps()
    private val movingSpikes = MovingSpikes(objects("moving_spikes"))
    /** Includes the existing static triangles and any bait triangles, at live positions. */
    val spikes: List<HitBox> get() = fixedSpikeBoxes + movingSpikes.boxes()
    val lasers = objects("laser_red").map { it.box }
    val invertedLasers = objects("laser_blue").map { it.box }
    val timedLasers = objects("laser_timed").map { challenge(it, ChallengeType.TIMED_LASER) }
    val springs = objects("spring").map { it.box }
    val keys = objects("key").map { it.box }
    val switches = objects("switch").map { it.box }
    val conveyors = objects("conveyor").map { it.box }
    private val conveyorSpeeds = objects("conveyor").map {
        it.box to (if (levelNumber in 1..10) GameVisualConfig.WORLD1_CONVEYOR_SPEED else it.speed)
    }
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
    private var jumpBufferTicks = 0
    private var coyoteTicks = 0
    private var shiftPrevious = false
    private var springCooldown = 0
    private var beltLeap = false
    private var monsterAlive = MutableList(monsters.size) { true }
    private var coinTaken = BooleanArray(coins.size)
    val coinsCollected: Int get() = coinTaken.count { it }
    fun isCoinVisible(index: Int): Boolean = !coinTaken[index]

    val playerBox get() = HitBox(x, y, x + SIZE, y + SIZE)
    val crateBox get() = crateBoxes.firstOrNull() ?: HitBox(-5000f, -5000f, -4954f, -4956f)
    val exitUnlocked get() = (!spec.requiresKey || keyCollected) &&
        (switches.isEmpty() || switchActivated) && pressureReady && (physicalPuzzle?.isSolved ?: true) &&
        (advancedPuzzle?.isSolved ?: true)
    fun solids(): List<HitBox> = (staticSolids + traps.solids() + advancedPuzzle?.wallBoxes().orEmpty()).let { if (shiftOn) it + bridges else it }
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
        crateYs = crateConfigs.map { it.y }.toMutableList()
        crateVys = MutableList(crateConfigs.size) { 0f }
        carriedCrateIndex = null; lockedCrateIndices.clear(); interactPrevious = false
        shiftOn = false; dead = false; won = false; keyCollected = false; switchActivated = false
        switchTouched = BooleanArray(switches.size)
        elapsed = 0f; ticks = 0; jumpPrevious = false; jumpBufferTicks = 0; coyoteTicks = 0; shiftPrevious = false; springCooldown = 0; beltLeap = false
        monsterAlive = MutableList(monsters.size) { true }
        coinTaken = BooleanArray(coins.size)
        traps.reset()
        movingSpikes.reset()
        physicalPuzzle?.reset()
        advancedPuzzle?.reset()
        previousPuzzlePlatforms = physicalPuzzle?.movingPlatforms().orEmpty()
        previousAdvancedPlatforms = advancedPuzzle?.platformBoxes().orEmpty()
    }

    fun tick(input: GameInput) {
        if (dead || won) return
        ticks++
        elapsed += STEP
        movingSpikes.update(playerBox, STEP, worldRight)
        if (input.shift && !shiftPrevious) shiftOn = !shiftOn
        shiftPrevious = input.shift
        val rawDirection = (if (input.right) 1 else 0) - (if (input.left) 1 else 0)
        val direction = if (traps.reverseControls(playerBox) || advancedPuzzle?.controlsReversed == true) -rawDirection else rawDirection
        val conveyorBoost = if (grounded) conveyorSpeeds.firstOrNull { (box, _) -> playerBox.intersects(box.copy(top = box.top - 7f)) }?.second ?: 0f else 0f
        val justInteracted = input.interact && !interactPrevious
        interactPrevious = input.interact
        if (justInteracted && advancedPuzzle != null) {
            val idx = carriedCrateIndex
            if (idx != null) {
                // Drop at the right edge of the hero, using actual world coordinates.
                cratePositions[idx] = (x + SIZE + 5f).coerceIn(0f, spec.worldRight - crateConfigs[idx].width)
                crateYs[idx] = y - crateConfigs[idx].height - 7f
                crateVys[idx] = 0f
                carriedCrateIndex = null
                val c = crateBoxes[idx]
                val cx = (c.left + c.right) / 2f
                val onLatch = advancedPuzzle.plan.devices.any { d ->
                    d.kind in setOf("heavy_pressure_plate", "weighted_button", "weight_button", "pressure_button", "speed_button", "code_button", "inverse_button") &&
                        cx >= d.x && cx <= d.x + d.width && c.bottom >= d.y - 5f
                }
                if (onLatch) lockedCrateIndices.add(idx)
                carriedCrateIndex = null
            } else if (advancedPuzzle.interact(playerBox)) {
                // Handled by a nearby wheel or puzzle board.
            } else {
                val near = crateConfigs.indices.filter { i ->
                    val c = crateBoxes[i]
                    kotlin.math.abs((c.left + c.right) / 2f - (x + SIZE / 2f)) < 70f &&
                        kotlin.math.abs(c.bottom - playerBox.bottom) < 60f
                }.minByOrNull { i -> kotlin.math.abs(crateBoxes[i].left - x) }
                if (near != null) { carriedCrateIndex = near; lockedCrateIndices.remove(near) }
            }
        }
        // Buffer brief presses and allow a short edge-of-platform grace period.
        // Still require a NEW press: holding jump cannot cause automatic repeated jumps.
        if (input.jump && !jumpPrevious) jumpBufferTicks = JUMP_BUFFER_TICKS
        val canJump = grounded || coyoteTicks > 0
        val justJumped = jumpBufferTicks > 0 && canJump
        if (justJumped) {
            // The long belts cannot be crossed by walking: approach and jump with momentum.
            beltLeap = levelNumber in 1..10 && conveyorSpeeds.any { (box, _) ->
                direction > 0 && playerBox.right >= box.left - 65f &&
                    playerBox.left < box.left && playerBox.bottom >= box.top - 16f
            }
            vy = JUMP_SPEED
            grounded = false
            coyoteTicks = 0
            jumpBufferTicks = 0
        }
        vx = SPEED * GameVisualConfig.HERO_SPEED_MULTIPLIER * direction * (if (beltLeap && !grounded) GameVisualConfig.WORLD1_BELT_JUMP_MULTIPLIER else 1f) + conveyorBoost
        jumpPrevious = input.jump
        if (jumpBufferTicks > 0) jumpBufferTicks--
        val oldBox = playerBox
        val platformBefore = traps.platforms()
        val pushBlocksBefore = traps.pushBlocks()
        traps.update(oldBox, oldBox, justJumped, vx)
        // Carry the player along the platform they were standing on.
        val platformAfter = traps.platforms()
        if (grounded) for (i in platformBefore.indices) {
            val previous = platformBefore[i]
            if (oldBox.right > previous.left && oldBox.left < previous.right &&
                kotlin.math.abs(oldBox.bottom - previous.top) < 3f) {
                x += platformAfter[i].left - previous.left
                y += platformAfter[i].top - previous.top
                break
            }
        }
        val latestPuzzlePlatforms = physicalPuzzle?.movingPlatforms().orEmpty()
        if (grounded && latestPuzzlePlatforms.size == previousPuzzlePlatforms.size) {
            for (i in latestPuzzlePlatforms.indices) {
                val previous = previousPuzzlePlatforms[i]; val latest = latestPuzzlePlatforms[i]
                if (oldBox.right > previous.left && oldBox.left < previous.right &&
                    kotlin.math.abs(oldBox.bottom - previous.top) < 3f) {
                    x += latest.left - previous.left
                    y += latest.top - previous.top
                    break
                }
            }
        }
        previousPuzzlePlatforms = latestPuzzlePlatforms
        val latestAdvanced = advancedPuzzle?.platformBoxes().orEmpty()
        if (grounded && previousAdvancedPlatforms.size == latestAdvanced.size) {
            for (i in latestAdvanced.indices) {
                val oldPlatform = previousAdvancedPlatforms[i]
                if (oldBox.right > oldPlatform.left && oldBox.left < oldPlatform.right &&
                    kotlin.math.abs(oldBox.bottom - oldPlatform.top) < 3f) {
                    x += latestAdvanced[i].left - oldPlatform.left
                    y += latestAdvanced[i].top - oldPlatform.top
                    break
                }
            }
        }
        previousAdvancedPlatforms = latestAdvanced
        val dx = vx * STEP
        x += dx
        // Animated blocks shove the hero sideways even if no direction key is held.
        // They are not one-hit-kill hazards: falling through a gap is the danger.
        x = PushBlock.resolveHorizontal(oldBox, playerBox, pushBlocksBefore,
            traps.pushBlocks(), worldRight)
        for (i in crateConfigs.indices) {
            if (carriedCrateIndex != null) continue // carrying: other crates stay in place rather than getting dragged across the room
            val crate = crateBoxes[i]
            if (!playerBox.intersects(crate)) continue
            if (i in lockedCrateIndices) {
                if (dx > 0f) x = crate.left - SIZE
                if (dx < 0f) x = crate.right
                continue
            }
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
        carriedCrateIndex?.let { i -> cratePositions[i] = x + 4f }
        // Re-evaluate gravity/support after horizontal pushes: a box leaving
        // a platform starts falling on this very physics tick.
        updateCratePhysics()
        // Push advanced-world movable bridges and springs by real contact.
        advancedPuzzle?.let { puzzle ->
            for (device in puzzle.devices.filter { it.kind == "sliding_platform" || it.kind == "movable_spring" }) {
                val body = puzzle.box(device)
                if (playerBox.intersects(body) && dx > 0f && oldBox.right <= body.left + 12f) {
                    val needed = playerBox.right - body.left
                    val moved = puzzle.pushMovable(device.id, needed)
                    x -= needed - moved
                }
            }
        }
        // Push the sliding bridge / wheel through actual player contact.
        physicalPuzzle?.let { puzzle ->
            for ((body, isWheel) in listOfNotNull(
                puzzle.sliderBox?.let { Pair(it, false) },
                puzzle.wheelBox?.let { Pair(it, true) }
            )) {
                if (playerBox.intersects(body) && oldBox.bottom > body.top + 3f) {
                    if (dx > 0f && oldBox.right <= body.left + 10f) {
                        val needed = playerBox.right - body.left
                        val moved = if (isWheel) puzzle.pushWheel(needed) else puzzle.pushSlider(needed)
                        x -= needed - moved
                    } else if (dx < 0f && oldBox.left >= body.right - 10f) {
                        // The wheel and sliding bridge are deliberately one-direction push puzzles.
                        x = body.right
                    }
                }
            }
        }
        for (solid in solids()) {
            if (playerBox.intersects(solid)) {
                if (dx > 0f) x = min(x, solid.left - SIZE)
                if (dx < 0f) x = max(x, solid.right)
            }
        }
        x = max(0f, x)
        vy = (vy + (if (advancedPuzzle?.gravityActive == true) -GRAVITY else GRAVITY) * STEP).coerceIn(-800f, 800f)
        val previousBottom = y + SIZE
        val previousTop = y
        val wasGrounded = grounded
        y += vy * STEP
        grounded = false
        for (solid in solids() + activePlatforms + traps.pushBlocks().map { it.box } + crateBoxes.filterIndexed { i, _ -> i != carriedCrateIndex }) {
            if (!playerBox.intersects(solid)) continue
            if (vy >= 0f && previousBottom <= solid.top + 2f) {
                y = solid.top - SIZE
                vy = 0f
                grounded = true
                physicalPuzzle?.let { puzzle ->
                    val platformIndex = puzzle.movingPlatforms().indexOf(solid)
                    if (platformIndex >= 0) puzzle.onLandMovingPlatform(platformIndex)
                }
                advancedPuzzle?.let { puzzle ->
                    val platformIndex = puzzle.platformBoxes().indexOf(solid)
                    if (platformIndex >= 0) puzzle.onPlatformLanding(platformIndex)
                }
            } else if (solid !in activePlatforms && vy < 0f && previousTop >= solid.bottom - 2f) {
                y = solid.bottom
                vy = 0f
            }
        }
        // A missed ledge should not immediately cancel a jump pressed a few frames late.
        coyoteTicks = when {
            grounded -> COYOTE_TICKS
            justJumped -> 0
            wasGrounded -> COYOTE_TICKS - 1
            else -> (coyoteTicks - 1).coerceAtLeast(0)
        }
        if (grounded) beltLeap = false
        if (springCooldown > 0) springCooldown--
        if (grounded && springCooldown == 0 && (springs + advancedPuzzle?.springBoxes().orEmpty()).any { playerBox.intersects(it.copy(top = it.top - 3f)) }) {
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
        if (y > 650f || spikes.any { SpikeCollision.touches(player, it) } ||
            LaserElements.isDangerous(lasers, invertedLasers, player, shiftOn) ||
            timedLasers.any { timedLaserActive(it) && player.intersects(timedLaserBox(it)) } ||
            saws.any { player.intersects(sawBox(it)) } ||
            traps.spikes().any { SpikeCollision.touches(player, it) } ||
            traps.views().any { it.type == "surprise_wall" && it.state == "active" && player.intersects(it.box) } ||
            advancedPuzzle?.spikeBoxes()?.any { SpikeCollision.touches(player, it) } == true
        ) dead = true
        physicalPuzzle?.update(player, crateBoxes)
        advancedPuzzle?.tick(player, crateBoxes.mapIndexed { i, box -> if (i == carriedCrateIndex) HitBox(-9000f,-9000f,-8000f,-8000f) else box }, !grounded, vx)
        if (physicalPuzzle?.laserActive == true && physicalPuzzle.laserBox?.intersects(player) == true) dead = true
        if (!dead && player.intersects(exit) && exitUnlocked) won = true
    }

    /** Apply gravity to loose crates, with one-way platform and crate stacking support. */
    private fun updateCratePhysics() {
        // Compute lower crates first, so a falling box can land on an updated box.
        val indices = crateConfigs.indices.sortedByDescending { crateYs[it] + crateConfigs[it].height }
        val supports = solids() + activePlatforms
        for (i in indices) {
            if (i == carriedCrateIndex) continue
            val c = crateConfigs[i]
            val previousBottom = crateYs[i] + c.height
            val velocity = (crateVys[i] + GRAVITY * STEP).coerceAtMost(800f)
            val nextBottom = previousBottom + velocity * STEP
            var landingTop = Float.POSITIVE_INFINITY
            val x0 = cratePositions[i]
            val x1 = x0 + c.width
            for (support in supports) {
                if (x1 <= support.left || x0 >= support.right) continue
                if (previousBottom <= support.top + 2f && nextBottom >= support.top) {
                    landingTop = min(landingTop, support.top)
                }
            }
            for (j in crateConfigs.indices) {
                if (j == i || j == carriedCrateIndex) continue
                val other = crateConfigs[j]
                val otherX = cratePositions[j]
                if (x1 <= otherX || x0 >= otherX + other.width) continue
                val otherTop = crateYs[j]
                if (previousBottom <= otherTop + 2f && nextBottom >= otherTop) {
                    landingTop = min(landingTop, otherTop)
                }
            }
            if (landingTop.isFinite()) {
                crateYs[i] = landingTop - c.height
                crateVys[i] = 0f
            } else {
                crateYs[i] += velocity * STEP
                crateVys[i] = velocity
            }
        }
    }

    /**
     * Move a stack as one rigid horizontal group while its crates remain in contact.
     * Each supported crate inherits the horizontal movement of the crate underneath.
     * All members share the same permitted displacement so the stack cannot pull apart
     * or tunnel through a wall / another crate.
     */
    private fun pushCrate(index: Int, amount: Float): Float {
        if (amount == 0f || index in lockedCrateIndices || index == carriedCrateIndex) return 0f

        // A crate belongs to this push if it is resting directly on a moving crate.
        val moving = mutableSetOf(index)
        var added: Boolean
        do {
            added = false
            for (upper in crateConfigs.indices) {
                if (upper in moving || upper == carriedCrateIndex) continue
                val upperBox = crateBoxes[upper]
                val supported = moving.any { lower ->
                    val lowerBox = crateBoxes[lower]
                    val horizontalOverlap = min(upperBox.right, lowerBox.right) -
                        max(upperBox.left, lowerBox.left)
                    horizontalOverlap > 1f &&
                        kotlin.math.abs(upperBox.bottom - lowerBox.top) <= 2.5f
                }
                if (supported) {
                    moving.add(upper)
                    added = true
                }
            }
        } while (added)

        if (moving.any { it in lockedCrateIndices }) return 0f
        var permitted = amount
        for (i in moving) {
            val current = crateBoxes[i]
            // World bounds apply to every crate in the moving stack.
            if (amount > 0f) permitted = min(permitted, worldRight - current.right)
            else permitted = max(permitted, -current.left)

            val obstacles = blocks + crateBoxes.filterIndexed { j, _ -> j !in moving && j != carriedCrateIndex }
            for (obstacle in obstacles) {
                // Only horizontally block overlapping vertical ranges.
                if (current.bottom <= obstacle.top + 0.01f ||
                    current.top >= obstacle.bottom - 0.01f) continue
                if (amount > 0f && current.right <= obstacle.left + 0.01f &&
                    current.right + permitted > obstacle.left) {
                    permitted = min(permitted, obstacle.left - current.right)
                } else if (amount < 0f && current.left >= obstacle.right - 0.01f &&
                    current.left + permitted < obstacle.right) {
                    permitted = max(permitted, obstacle.right - current.left)
                }
            }
        }
        permitted = if (amount > 0f) permitted.coerceIn(0f, amount)
                    else permitted.coerceIn(amount, 0f)
        for (i in moving) cratePositions[i] += permitted
        return permitted
    }

}
