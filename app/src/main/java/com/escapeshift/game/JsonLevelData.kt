package com.escapeshift.game

import java.io.File
import java.util.Locale

/** The only source of level geometry: app/src/main/assets/levels/level_XX.json. */
data class LevelObject(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val count: Int = 1,
    val spacing: Float = 0f,
    val phase: Int = 0,
    val amplitude: Float = 0f,
    val period: Int = 120,
    val activeTicks: Int = 64,
    val offsetX: Float = 0f,
    val speed: Float = -145f
) {
    val box get() = HitBox(x, y, x + width, y + height)
}

data class GroundGap(val id: String, val x: Float, val width: Float)

/** No network dependency. Android calls install() with its AssetManager before creating GameView. */
object LevelCatalog {
    const val MAX_LEVEL = 40
    private val cache = mutableMapOf<Int, LevelSpec>()
    private val fallbackSource: (Int) -> String = { n ->
        val file = listOf(
            File("app/src/main/assets/levels"),
            File("src/main/assets/levels"),
            File("../app/src/main/assets/levels")
        ).map { File(it, filename(n)) }.firstOrNull { it.isFile }
            ?: error("Missing ${filename(n)}. Install the Android asset loader or run from the project root.")
        file.readText(Charsets.UTF_8)
    }
    private var source: (Int) -> String = fallbackSource

    fun filename(number: Int): String = "level_${number.toString().padStart(2, '0')}.json"

    @Synchronized fun install(readJson: (Int) -> String) {
        source = readJson
        cache.clear()
    }

    @Synchronized fun useProjectFiles() = install(fallbackSource)

    @Synchronized fun create(number: Int): LevelSpec {
        require(number in 1..MAX_LEVEL) { "Level $number has not been implemented" }
        return cache.getOrPut(number) { LevelJsonParser.parse(source(number), number) }
    }

    val titles get() = (1..MAX_LEVEL).map { create(it).title }
}

/** Strict JSON schema validation: fail with an actionable error instead of silently changing collisions. */
object LevelJsonParser {
    private val supported = setOf(
        "wall", "platform", "crate", "bridge", "spike", "laser_red", "laser_blue",
        "laser_timed", "spring", "key", "coin", "switch", "crate_switch", "snowflake", "conveyor", "saw", "monster", "door"
    )
    private val routeTypes = setOf(
        "spike", "laser_red", "laser_blue", "laser_timed", "spring", "key", "switch", "conveyor", "saw", "monster", "wall"
    )
    private val challengeTypes = mapOf(
        "spike" to ChallengeType.SPIKE,
        "laser_red" to ChallengeType.RED_LASER,
        "laser_blue" to ChallengeType.BLUE_LASER,
        "laser_timed" to ChallengeType.TIMED_LASER,
        "spring" to ChallengeType.SPRING,
        "key" to ChallengeType.KEY,
        "switch" to ChallengeType.SWITCH,
        "conveyor" to ChallengeType.CONVEYOR,
        "saw" to ChallengeType.SAW,
        "monster" to ChallengeType.MONSTER,
        "wall" to ChallengeType.WALL
    )

    fun parse(raw: String, expectedLevel: Int): LevelSpec {
        val root = MiniJson(raw).parse() as? Map<*, *> ?: error("Root must be a JSON object")
        fun get(key: String) = root[key] ?: error("level_${expectedLevel}: missing '$key'")
        fun number(value: Any?, path: String): Float {
            val v = value as? Number ?: error("$path must be numeric")
            return v.toFloat().also { require(it.isFinite()) { "$path must be finite" } }
        }
        fun int(value: Any?, path: String) = number(value, path).toInt().also {
            require(it.toFloat() == number(value, path)) { "$path must be an integer" }
        }
        fun str(value: Any?, path: String) = value as? String ?: error("$path must be a string")
        fun map(value: Any?, path: String) = value as? Map<*, *> ?: error("$path must be an object")
        fun list(value: Any?, path: String) = value as? List<*> ?: error("$path must be an array")
        fun optNum(obj: Map<*, *>, key: String, default: Float) = obj[key]?.let { number(it, key) } ?: default
        fun optInt(obj: Map<*, *>, key: String, default: Int) = obj[key]?.let { int(it, key) } ?: default
        require(int(get("schemaVersion"), "schemaVersion") == 1) { "Unsupported schemaVersion" }
        val declaredWorld = int(get("world"), "world")
        require(declaredWorld == (expectedLevel - 1) / 10 + 1) { "Incorrect world for level $expectedLevel" }
        val level = int(get("level"), "level")
        require(level == expectedLevel) { "Wrong level inside ${LevelCatalog.filename(expectedLevel)}: $level" }
        val worldWidth = number(get("worldWidth"), "worldWidth")
        require(worldWidth > 500f) { "worldWidth must be > 500" }
        val intro = StartType.valueOf(str(get("intro"), "intro"))
        val spawn = map(get("playerStart"), "playerStart")
        val spawnX = number(spawn["x"], "playerStart.x")
        val spawnY = number(spawn["y"], "playerStart.y")
        require(spawnX >= 0 && spawnX + GameWorld.SIZE <= worldWidth && spawnY in -100f..700f)
        val ground = map(get("ground"), "ground")
        val groundY = number(ground["y"], "ground.y")
        val groundDepth = number(ground["depth"], "ground.depth")
        require(groundDepth > 0f)
        val gaps = list(ground["gaps"], "ground.gaps").mapIndexed { index, value ->
            val g = map(value, "ground.gaps[$index]")
            GroundGap(str(g["id"], "gap.id"), number(g["x"], "gap.x"), number(g["width"], "gap.width"))
        }.sortedBy { it.x }
        var previousEnd = 0f
        gaps.forEach {
            require(it.x >= previousEnd && it.width > 0f && it.x + it.width <= worldWidth) {
                "Invalid/overlapping ground gap at x=${it.x} width=${it.width}"
            }
            previousEnd = it.x + it.width
        }
        val seenIds = mutableSetOf<String>()
        for (gap in gaps) {
            require(gap.id.matches(Regex("[a-zA-Z0-9_]+")) && seenIds.add(gap.id)) { "Duplicate or invalid gap ID: ${gap.id}" }
        }
        val objects = list(get("objects"), "objects").mapIndexed { index, value ->
            val o = map(value, "objects[$index]")
            val id = str(o["id"], "objects[$index].id")
            val type = str(o["type"], "objects[$index].type")
            require(id.matches(Regex("[a-zA-Z0-9_]+"))) { "Invalid object id $id" }
            require(seenIds.add(id)) { "Duplicate object id: $id" }
            require(type in supported) { "Unknown object type '$type' on $id" }
            val obj = LevelObject(
                id, type,
                number(o["x"], "$id.x"), number(o["y"], "$id.y"),
                number(o["width"], "$id.width"), number(o["height"], "$id.height"),
                optInt(o, "count", 1), optNum(o, "spacing", 0f), optInt(o, "phase", 0),
                optNum(o, "amplitude", 0f), optInt(o, "period", if (type == "saw") 155 else if (type == "monster") 130 else 120),
                optInt(o, "activeTicks", 64), optNum(o, "offsetX", 0f), optNum(o, "speed", -145f)
            )
            require(obj.x >= 0f && obj.width > 0f && obj.height > 0f) { "Bad dimensions on $id" }
            require(obj.count in 1..3 && obj.spacing >= 0f) { "count must be 1..3 and spacing >=0 on $id" }
            require(obj.period > 0 && obj.activeTicks in 1..obj.period) { "Bad period/activeTicks on $id" }
            val right = obj.x + obj.width * obj.count + obj.spacing * (obj.count - 1)
            require(right <= worldWidth + 0.01f) { "$id extends beyond worldWidth" }
            obj
        }
        require(objects.count { it.type == "door" } == 1) { "Exactly one door is required" }
        require(objects.count { it.type == "bridge" } <= 1)
        require(objects.count { it.type == "crate" } <= if (declaredWorld == 1) 1 else 3)
        require(objects.count { it.type == "crate_switch" } <= 2)
        val traps = (root["traps"]?.let { list(it, "traps") } ?: emptyList<Any?>()).mapIndexed { index, value ->
            val t = map(value, "traps[$index]")
            val id = str(t["id"], "traps[$index].id")
            val type = str(t["type"], "$id.type")
            require(type in setOf("collapse_floor", "rising_spikes", "surprise_wall", "moving_platform", "moving_exit", "reverse_zone")) {
                "Unknown trap type $type on $id"
            }
            require(id.matches(Regex("[a-zA-Z0-9_]+")) && seenIds.add(id)) { "Invalid or duplicated trap ID $id" }
            val trigger = (t["trigger"] as? String) ?: "player_near"
            require(trigger in setOf("player_near", "player_on", "player_jump", "approach_fast", "player_enter")) {
                "Invalid trigger $trigger on $id"
            }
            TrapDefinition(
                id, type, number(t["x"], "$id.x"), number(t["y"], "$id.y"),
                number(t["width"], "$id.width"), number(t["height"], "$id.height"), trigger,
                optNum(t, "radius", 100f), optInt(t, "delayTicks", 12),
                optInt(t, "durationTicks", 0), optNum(t, "travelX", 0f),
                optNum(t, "travelY", 0f), optInt(t, "transitionTicks", 18),
                optInt(t, "periodTicks", 180), optNum(t, "thresholdSpeed", 250f)
            ).also { d ->
                require(d.width > 0f && d.height > 0f && d.x >= 0f && d.x + d.width <= worldWidth)
                require(d.radius >= 0f && d.delayTicks >= 0 && d.durationTicks >= 0 && d.transitionTicks > 0 && d.periodTicks > 0)
                if (d.type == "collapse_floor") {
                    require(d.y == groundY) { "$id: collapse floor must start at groundY" }
                    require(gaps.none { g -> d.x < g.x + g.width && g.x < d.x + d.width }) {
                        "$id intersects a permanent ground gap"
                    }
                }
            }
        }
        require(traps.count { it.type == "moving_exit" } <= 1)
        val mapById = objects.associateBy { it.id }
        val routeNames = list(get("route"), "route").map { str(it, "route element") }
        require(routeNames.distinct().size == routeNames.size) { "Duplicate route entry" }
        val route = routeNames.map { id ->
            val gap = gaps.firstOrNull { it.id == id }
            if (gap != null) return@map Challenge(ChallengeType.GAP, gap.x, width = gap.width, y = groundY)
            val obj = mapById[id] ?: error("Route references missing object or gap id: $id")
            require(obj.type in routeTypes) { "Route cannot reference ${obj.type} ($id)" }
            Challenge(
                challengeTypes.getValue(obj.type), obj.x, obj.phase,
                obj.y, obj.width, obj.height, obj.amplitude, obj.period,
                obj.activeTicks, obj.offsetX, obj.speed
            )
        }
        val backtrackCount = int(get("backtrackCount"), "backtrackCount")
        require(backtrackCount in 0..route.size) { "backtrackCount exceeds route length" }
        val physicalPlan = PhysicalPuzzlePlan.fromJson(expectedLevel, root["puzzle"])
        val advancedPlan = AdvancedPuzzlePlan.fromJson(expectedLevel, root["puzzle"])
        if (physicalPlan != null) {
            physicalPlan.devices.forEach { d ->
                require(d.x + d.width <= worldWidth + 0.01f) { "${d.id} exceeds worldWidth" }
            }
            physicalPlan.first("ground_gap")?.let { d ->
                require(gaps.any { g -> kotlin.math.abs(g.x - d.x) < 0.1f && kotlin.math.abs(g.width - d.width) < 0.1f }) {
                    "Puzzle gap ${d.id} must match ground.gaps in level $expectedLevel"
                }
            }
        }
        return LevelSpec(
            number = level,
            title = str(get("titleKey"), "titleKey"),
            intro = intro,
            challenges = route,
            exitX = objects.single { it.type == "door" }.x,
            worldRight = worldWidth,
            requiresKey = objects.any { it.type == "key" },
            requiresSwitch = objects.any { it.type in setOf("switch", "crate_switch") },
            backtrackCount = backtrackCount,
            hint = str(get("hintKey"), "hintKey"),
            startX = spawnX, startY = spawnY,
            groundY = groundY, groundDepth = groundDepth,
            gaps = gaps,
            objects = objects,
            traps = traps,
            physicalPuzzle = physicalPlan, advancedPuzzle = advancedPlan
        )
    }
}

/** Small self-contained JSON reader for Android and the desktop simulator; no extra Gradle library. */
internal class MiniJson(private val input: String) {
    private var i = 0
    fun parse(): Any? {
        val result = value()
        skip()
        require(i == input.length) { "Trailing JSON data at $i" }
        return result
    }
    private fun skip() { while (i < input.length && input[i].isWhitespace()) i++ }
    private fun fail(message: String): Nothing = error("Invalid level JSON at char $i: $message")
    private fun eat(ch: Char) { skip(); if (i >= input.length || input[i] != ch) fail("Expected '$ch'"); i++ }
    private fun value(): Any? {
        skip(); if (i >= input.length) fail("Unexpected end")
        return when (input[i]) {
            '{' -> obj()
            '[' -> array()
            '"' -> string()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> if (input[i] == '-' || input[i].isDigit()) number() else fail("Unexpected ${input[i]}")
        }
    }
    private fun literal(text: String, value: Any?): Any? {
        if (!input.startsWith(text, i)) fail("Expected $text")
        i += text.length
        return value
    }
    private fun obj(): Map<String, Any?> {
        eat('{'); skip()
        val out = linkedMapOf<String, Any?>()
        if (i < input.length && input[i] == '}') { i++; return out }
        while (true) {
            skip(); if (i >= input.length || input[i] != '"') fail("Object key expected")
            val key = string(); eat(':')
            if (out.containsKey(key)) fail("Duplicate key $key")
            out[key] = value(); skip()
            if (i < input.length && input[i] == '}') { i++; return out }
            eat(',')
        }
    }
    private fun array(): List<Any?> {
        eat('['); skip()
        val out = arrayListOf<Any?>()
        if (i < input.length && input[i] == ']') { i++; return out }
        while (true) {
            out.add(value()); skip()
            if (i < input.length && input[i] == ']') { i++; return out }
            eat(',')
        }
    }
    private fun string(): String {
        eat('"'); val out = StringBuilder()
        while (i < input.length) {
            val ch = input[i++]
            if (ch == '"') return out.toString()
            if (ch != '\\') { require(ch >= ' ') { "Invalid control character in JSON string" }; out.append(ch); continue }
            if (i >= input.length) fail("Unfinished string escape")
            when (val escape = input[i++]) {
                '"', '\\', '/' -> out.append(escape)
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'u' -> {
                    if (i + 4 > input.length) fail("Short unicode escape")
                    val hex = input.substring(i, i + 4)
                    out.append(hex.toIntOrNull(16)?.toChar() ?: fail("Invalid unicode escape"))
                    i += 4
                }
                else -> fail("Unknown string escape $escape")
            }
        }
        fail("Unfinished JSON string")
    }
    private fun number(): Double {
        val start = i
        if (input[i] == '-') i++
        while (i < input.length && input[i].isDigit()) i++
        if (i < input.length && input[i] == '.') {
            i++
            while (i < input.length && input[i].isDigit()) i++
        }
        if (i < input.length && (input[i] == 'e' || input[i] == 'E')) {
            i++; if (i < input.length && (input[i] == '+' || input[i] == '-')) i++
            while (i < input.length && input[i].isDigit()) i++
        }
        return input.substring(start, i).toDoubleOrNull() ?: fail("Invalid number")
    }
}
