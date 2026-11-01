package com.escapeshift.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class HitBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(other: HitBox): Boolean = left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class GameInput(val left: Boolean = false, val right: Boolean = false, val jump: Boolean = false, val shift: Boolean = false)

enum class StartType { OPEN, CRATE_WALL, SHIFT_BRIDGE, SPRING_WALL, UPPER_PLATFORM }
enum class ChallengeType { SPIKE, RED_LASER, BLUE_LASER, TIMED_LASER, KEY, SPRING, SAW, CONVEYOR, SWITCH, MONSTER }
data class Challenge(val type: ChallengeType, val x: Float, val phase: Int = 0)
data class LevelDesign(val title: String, val intro: StartType, val sequence: String, val backtrackCount: Int = 0, val hint: String)

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
    val hint: String
)

data class MonsterSpawn(
    val anchorX: Float,
    val yTop: Float,
    val amplitude: Float,
    val width: Float,
    val height: Float,
    val phase: Int
)

/** Ten individually designed routes, instead of extending one single linear level. */
object LevelCatalog {
    const val MAX_LEVEL = 10
    private val designs = listOf(
        LevelDesign("Premier saut", StartType.OPEN, "S", hint = "SAUT : evite les pics rouges."),
        LevelDesign("La caisse", StartType.CRATE_WALL, "SR", hint = "Pousse la caisse et saute dessus pour franchir le mur."),
        LevelDesign("Le pont fantome", StartType.SHIFT_BRIDGE, "RBS", hint = "SHIFT cree un pont. Le laser bleu est dangereux en SHIFT ON."),
        LevelDesign("Le bon moment", StartType.OPEN, "STSR", hint = "Les lasers orange pulsent : attends la bonne ouverture."),
        LevelDesign("Carte sur la hauteur", StartType.UPPER_PLATFORM, "SKRS", hint = "Monte sur la plateforme, puis trouve la carte jaune."),
        LevelDesign("Retour obligatoire", StartType.SHIFT_BRIDGE, "RSTSK", 1, "Passe la sortie fermee, prends la carte, puis REVIENS."),
        LevelDesign("Saut propulse", StartType.SPRING_WALL, "SMSBRS", hint = "Le ressort vert te propulse au-dessus du grand mur."),
        LevelDesign("Tapis sous pression", StartType.OPEN, "STCESBRS", hint = "Tapis rapide ! Saute sur le monstre pour le neutraliser."),
        LevelDesign("Interrupteur secret", StartType.CRATE_WALL, "RBSTSCERW", 2, "Saute sur le monstre, active la plaque et reviens vers la sortie."),
        LevelDesign("Evasion totale", StartType.UPPER_PLATFORM, "SMTRBSPCESW", 3, "Combo scies, tapis, monstre et retour jusqu'a la sortie !")
    )
    val titles get() = designs.map { it.title }
    fun create(level: Int): LevelSpec {
        require(level in 1..MAX_LEVEL)
        val d = designs[level - 1]
        var cursor = when (d.intro) {
            StartType.OPEN -> 385f
            StartType.CRATE_WALL -> 825f
            StartType.SHIFT_BRIDGE -> 675f
            StartType.SPRING_WALL -> 710f
            StartType.UPPER_PLATFORM -> 695f
        }
        val events = mutableListOf<Challenge>()
        for ((i, letter) in d.sequence.withIndex()) {
            val type = when(letter) {
                'S' -> ChallengeType.SPIKE
                'R' -> ChallengeType.RED_LASER
                'B' -> ChallengeType.BLUE_LASER
                'T' -> ChallengeType.TIMED_LASER
                'K' -> ChallengeType.KEY
                'P' -> ChallengeType.SPRING
                'M' -> ChallengeType.SAW
                'C' -> ChallengeType.CONVEYOR
                'W' -> ChallengeType.SWITCH
                'E' -> ChallengeType.MONSTER
                else -> error("Obstacle inconnu $letter")
            }
            events.add(Challenge(type, cursor, (level * 17 + i * 23) % 120))
            cursor += when (type) {
                ChallengeType.SPRING -> 600f
                ChallengeType.CONVEYOR -> 310f
                ChallengeType.MONSTER -> 270f
                else -> 255f
            }
        }
        val exitX = if (d.backtrackCount == 0) cursor + 25f
        else events[events.size - d.backtrackCount].x - 130f
        return LevelSpec(level, d.title, d.intro, events, exitX,
            max(cursor + 140f, exitX + 250f),
            events.any { it.type == ChallengeType.KEY },
            events.any { it.type == ChallengeType.SWITCH },
            d.backtrackCount, d.hint)
    }
}

/** Physics shared with the on-device game and the desktop virtual player. */
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
    val hasCrate = spec.intro == StartType.CRATE_WALL
    val bridge: HitBox? = when (spec.intro) {
        StartType.CRATE_WALL -> HitBox(450f, GROUND_Y, 590f, 464f)
        StartType.SHIFT_BRIDGE -> HitBox(300f, GROUND_Y, 440f, 464f)
        else -> null
    }
    val exit = HitBox(spec.exitX, 354f, spec.exitX + 65f, GROUND_Y)

    private fun extraPlatformsForLevel(level: Int): List<HitBox> = when (level) {
        4 -> listOf(
            HitBox(1010f, 366f, 1132f, 386f),
            HitBox(1450f, 332f, 1584f, 352f)
        )
        5 -> listOf(
            HitBox(835f, 356f, 980f, 376f),
            HitBox(1188f, 318f, 1326f, 338f)
        )
        6 -> listOf(
            HitBox(915f, 352f, 1038f, 372f),
            HitBox(1408f, 318f, 1542f, 338f)
        )
        8 -> listOf(
            HitBox(965f, 356f, 1098f, 376f),
            HitBox(1405f, 320f, 1548f, 340f),
            HitBox(1810f, 286f, 1935f, 306f)
        )
        9 -> listOf(
            HitBox(955f, 356f, 1088f, 376f),
            HitBox(1540f, 320f, 1680f, 340f),
            HitBox(2065f, 286f, 2200f, 306f)
        )
        10 -> listOf(
            HitBox(860f, 358f, 980f, 378f),
            HitBox(1315f, 323f, 1452f, 343f),
            HitBox(1780f, 286f, 1915f, 306f),
            HitBox(2305f, 340f, 2440f, 360f)
        )
        else -> emptyList()
    }

    val blocks: List<HitBox> = buildList {
        when (spec.intro) {
            StartType.CRATE_WALL -> {
                add(HitBox(0f, GROUND_Y, 450f, 590f))
                add(HitBox(590f, GROUND_Y, spec.worldRight, 590f))
                add(HitBox(390f, 328f, 430f, GROUND_Y))
            }
            StartType.SHIFT_BRIDGE -> {
                add(HitBox(0f, GROUND_Y, 300f, 590f))
                add(HitBox(440f, GROUND_Y, spec.worldRight, 590f))
            }
            else -> add(HitBox(0f, GROUND_Y, spec.worldRight, 590f))
        }
        if (spec.intro == StartType.SPRING_WALL) add(HitBox(460f, 298f, 512f, GROUND_Y))
        if (spec.intro == StartType.UPPER_PLATFORM) add(HitBox(320f, 363f, 415f, 382f))
        spec.challenges.filter { it.type == ChallengeType.SPRING }.forEach {
            add(HitBox(it.x + 185f, 298f, it.x + 234f, GROUND_Y))
        }
        addAll(extraPlatformsForLevel(levelNumber))
    }

    // Progressive hazard groups: single, tall, or two consecutive triangles.
    // Keep collision shapes synchronized with artwork in GameDesign.
    val oneWayPlatforms: List<HitBox> = blocks.filter { it.bottom < GROUND_Y && it.top < GROUND_Y - 20f }
    val spikes = spec.challenges.filter { it.type == ChallengeType.SPIKE }.flatMapIndexed { index, event ->
        val variant = (levelNumber + index) % 3
        when {
            levelNumber <= 2 -> listOf(HitBox(event.x, 421f, event.x + 34f, GROUND_Y))
            variant == 0 -> listOf(HitBox(event.x, 398f, event.x + 58f, GROUND_Y))
            variant == 1 -> listOf(
                HitBox(event.x, 421f, event.x + 34f, GROUND_Y),
                HitBox(event.x + 37f, 421f, event.x + 71f, GROUND_Y)
            )
            else -> listOf(HitBox(event.x, 409f, event.x + 52f, GROUND_Y))
        }
    }
    val lasers = spec.challenges.filter { it.type == ChallengeType.RED_LASER }.map { HitBox(it.x, 300f, it.x + 15f, GROUND_Y) }
    val invertedLasers = spec.challenges.filter { it.type == ChallengeType.BLUE_LASER }.map { HitBox(it.x, 300f, it.x + 15f, GROUND_Y) }
    val timedLasers = spec.challenges.filter { it.type == ChallengeType.TIMED_LASER }
    val springs = buildList {
        if (spec.intro == StartType.SPRING_WALL) add(HitBox(240f, 437f, 285f, GROUND_Y))
        spec.challenges.filter { it.type == ChallengeType.SPRING }.forEach { add(HitBox(it.x, 437f, it.x + 45f, GROUND_Y)) }
    }
    val keys = spec.challenges.filter { it.type == ChallengeType.KEY }.map { HitBox(it.x, 388f, it.x + 27f, 433f) }
    val switches = spec.challenges.filter { it.type == ChallengeType.SWITCH }.map { HitBox(it.x, 431f, it.x + 48f, GROUND_Y) }
    val conveyors = spec.challenges.filter { it.type == ChallengeType.CONVEYOR }.map { HitBox(it.x, 438f, it.x + 135f, GROUND_Y) }
    val coins = oneWayPlatforms.filter { it.right - it.left >= 46f }.map {
        val center = (it.left + it.right) * 0.5f
        HitBox(center - 12f, it.top - 31f, center + 12f, it.top - 7f)
    }

    val saws = spec.challenges.filter { it.type == ChallengeType.SAW }
    val monsters: List<MonsterSpawn> = spec.challenges.filter { it.type == ChallengeType.MONSTER }.map { e ->
        MonsterSpawn(e.x - 4f, GROUND_Y - 48f, 14f, 64f, 48f, e.phase)
    }

    var x = 78f; private set
    var y = 405f; private set
    var vx = 0f; private set
    var vy = 0f; private set
    var grounded = false; private set
    var crateX = if (hasCrate) 201f else -5000f; private set
    var shiftOn = false; private set
    var dead = false; private set
    var won = false; private set
    var keyCollected = false; private set
    var switchActivated = false; private set
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
    val crateBox get() = HitBox(crateX, GROUND_Y - CRATE_HEIGHT, crateX + CRATE_WIDTH, GROUND_Y)
    val exitUnlocked get() = (!spec.requiresKey || keyCollected) && (!spec.requiresSwitch || switchActivated)
    fun solids(): List<HitBox> = (blocks - oneWayPlatforms).let { if (shiftOn && bridge != null) it + bridge else it }
    fun sawBox(saw: Challenge, t: Int = ticks): HitBox {
        val offset = 55f * sin((t + saw.phase) * (2 * Math.PI / 155.0)).toFloat()
        return HitBox(saw.x + 35f + offset, 405f, saw.x + 68f + offset, GROUND_Y)
    }
    fun monsterBox(index: Int, t: Int = ticks): HitBox {
        val m = monsters[index]
        val offset = m.amplitude * sin((t + m.phase) * (2 * Math.PI / 130.0)).toFloat()
        return HitBox(m.anchorX + offset, m.yTop, m.anchorX + m.width + offset, m.yTop + m.height)
    }
    fun isMonsterAlive(index: Int): Boolean = monsterAlive.getOrNull(index) == true
    fun timedLaserActive(laser: Challenge, t: Int = ticks): Boolean = (t + laser.phase) % 120 < 64
    fun timedLaserBox(laser: Challenge) = HitBox(laser.x, 285f, laser.x + 16f, GROUND_Y)

    fun reset() {
        x = 78f; y = 405f; vx = 0f; vy = 0f; grounded = false
        crateX = if (hasCrate) 201f else -5000f
        shiftOn = false; dead = false; won = false; keyCollected = false; switchActivated = false
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
        val conveyorBoost = if (grounded && conveyors.any { playerBox.intersects(it.copy(top = it.top - 7f)) }) -145f else 0f
        vx = SPEED * direction + conveyorBoost
        if (input.jump && !jumpPrevious && grounded) { vy = JUMP_SPEED; grounded = false }
        jumpPrevious = input.jump
        val dx = vx * STEP
        x += dx
        if (hasCrate && playerBox.intersects(crateBox)) {
            if (dx > 0f) {
                val needed = playerBox.right - crateBox.left
                val moved = pushCrate(needed)
                x -= needed - moved
            } else if (dx < 0f) {
                val needed = playerBox.left - crateBox.right
                val moved = pushCrate(needed)
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
        for (solid in solids() + oneWayPlatforms + (if (hasCrate) listOf(crateBox) else emptyList())) {
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
        if (switches.any { playerBox.intersects(it.copy(top = it.top - 6f)) }) switchActivated = true

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

    private fun pushCrate(amount: Float): Float {
        if (amount == 0f) return 0f
        val old = crateX
        var target = (crateX + amount).coerceAtLeast(0f)
        for (solid in blocks) {
            if (solid.bottom <= GROUND_Y - CRATE_HEIGHT || solid.top >= GROUND_Y) continue
            val targetBox = HitBox(target, GROUND_Y - CRATE_HEIGHT, target + CRATE_WIDTH, GROUND_Y)
            if (targetBox.intersects(solid)) {
                if (amount > 0f) target = min(target, solid.left - CRATE_WIDTH)
                else target = max(target, solid.right)
            }
        }
        crateX = target
        return crateX - old
    }
}
