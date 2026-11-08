package com.escapeshift.game

/**
 * EXPERIMENTAL: puzzle-state evaluator for level_21.json ... level_40.json.
 * It does not move GameWorld characters or render graphics yet.
 * It is intentionally Android-independent so that Python and Kotlin can test
 * the very same puzzle JSON scripts before wiring them to player collisions.
 */
class ExperimentalPuzzleEngine private constructor(private val puzzle: Map<String, Any?>) {
    data class Outcome(val solved: Boolean, val failed: Boolean, val message: String)

    private val variables = mutableMapOf<String, Any?>()
    private val sequenceSteps = mutableMapOf<String, Int>()
    private val collected = mutableSetOf<String>()
    private val feedback = mutableMapOf<String, Int>()
    private val expires = mutableMapOf<String, Int>()
    var solved: Boolean = false; private set
    var failed: Boolean = false; private set
    var ticks: Int = 0; private set
    var lastMessage: String = ""; private set

    init {
        puzzle.list("sequences").forEach { entry ->
            val seq = entry.asObject()
            sequenceSteps[seq.string("id")] = 0
        }
    }

    private fun value(name: String): Any? = when {
        name.startsWith("sequence:") -> sequenceSteps[name.substringAfter(':')] ?: 0
        else -> variables[name] ?: false
    }

    private fun equal(a: Any?, b: Any?): Boolean = when {
        a is Number && b is Number -> a.toDouble() == b.toDouble()
        else -> a == b
    }

    private fun matchesConditions(conditions: List<Any?>): Boolean =
        conditions.all { c ->
            val clause = c.asObject()
            equal(value(clause.string("var")), clause["eq"])
        }

    private fun expire(): Unit {
        for ((key, deadline) in expires.toMap()) {
            if (ticks >= deadline) {
                variables[key] = false
                expires.remove(key)
            }
        }
    }

    fun apply(action: Map<String, Any?>): Outcome {
        check(!solved) { "Puzzle already solved" }
        val verb = action.string("action")
        val target = action["target"] as? String ?: ""
        val input = action["value"]
        ticks++
        expire()
        lastMessage = "$verb $target"
        when (verb) {
            "push", "move", "place" -> variables["at:$target"] = input
            "rotate" -> variables["angle:$target"] = input
            "stand" -> variables["hero:$target"] = true
            "leave" -> variables["hero:$target"] = false
            "enter" -> variables["zone:$target"] = input
            "cross" -> variables["crossed:$target"] = true
            "press" -> variables["pressed:$target"] = true
            "release" -> variables["pressed:$target"] = false
            "collect" -> {
                if (!collected.add(target)) failed = true
                for (v in puzzle.list("sequences")) {
                    val seq = v.asObject()
                    val members = seq.list("items").map { it as String }
                    val decoys = seq.list("decoys").map { it as String }
                    if (target !in members && target !in decoys) continue
                    val id = seq.string("id")
                    val index = sequenceSteps.getValue(id)
                    if (index < members.size && members[index] == target) {
                        sequenceSteps[id] = index + 1
                    } else if (seq["failOnWrong"] != false) {
                        failed = true
                        lastMessage = "Wrong token: $target for sequence $id"
                    }
                    break
                }
            }
            "submit" -> {
                val combination = (puzzle["combination"] as? Map<*, *>)?.asObject()
                    ?: error("No combination lock")
                require(target == combination.string("id")) { "Wrong lock ID $target" }
                val submitted = input.asObject()
                val expected = combination.obj("target")
                val pads = combination.list("pads").map { it as String }
                require(submitted.keys == pads.toSet()) { "Exactly one assignment per button required" }
                require(submitted.values.toSet().size == submitted.size) { "Crates cannot be assigned twice" }
                val correct = pads.count { expected[it] == submitted[it] }
                feedback[target] = correct
                variables["solved:$target"] = correct == pads.size
                lastMessage = "$correct/${pads.size} matches; positions hidden"
            }
            "wait" -> { ticks += (input as? Number)?.toInt() ?: 0; expire() }
            "return", "run", "jump", "exit" -> Unit
            else -> error("Unrecognized puzzle action: $verb")
        }
        for (r in puzzle.list("rules")) {
            val rule = r.asObject()
            val event = rule.obj("on")
            if (event.string("action") != verb || event.string("target") != target) continue
            if (event.containsKey("value") && !equal(event["value"], input)) continue
            if (!matchesConditions(rule.list("if"))) continue
            variables.putAll(rule.obj("set"))
            for ((key, period) in (rule["expires"] as? Map<*, *>) ?: emptyMap<Any?,Any?>()) {
                expires[key as String] = ticks + (period as Number).toInt()
            }
            lastMessage = rule["message"] as? String ?: lastMessage
        }
        if (verb == "exit") {
            solved = !failed && matchesConditions(puzzle.list("goal"))
            lastMessage = if (solved) "EXIT UNLOCKED" else "EXIT LOCKED"
        }
        return Outcome(solved, failed, lastMessage)
    }

    fun feedbackFor(lock: String): Int? = feedback[lock]
    fun valueFor(key: String): Any? = value(key)

    fun replaySolution(): Outcome {
        var result = Outcome(false, false, "Not run")
        for (step in puzzle.list("solution")) result = apply(step.asObject())
        return result
    }

    companion object {
        fun fromLevelJson(raw: String): ExperimentalPuzzleEngine {
            val data = MiniJson(raw).parse().asObject()
            val id = (data["level"] as? Number)?.toInt() ?: error("level number missing")
            require(id in 21..40) { "Puzzle lab only supports levels 21 to 40" }
            val puzzle = data.obj("puzzle")
            require(puzzle["prototype"] == true) { "Not a prototype puzzle" }
            require(puzzle.list("goal").isNotEmpty()) { "No victory conditions" }
            return ExperimentalPuzzleEngine(puzzle)
        }
    }
}

private fun Any?.asObject(): Map<String, Any?> {
    require(this is Map<*, *>) { "Expected JSON object but got $this" }
    @Suppress("UNCHECKED_CAST")
    return this as Map<String, Any?>
}
private fun Map<String, Any?>.obj(key: String) = this[key].asObject()
private fun Map<String, Any?>.string(key: String): String =
    this[key] as? String ?: error("Missing string: $key")
private fun Map<String, Any?>.list(key: String): List<Any?> =
    (this[key] as? List<*>)?.toList() ?: emptyList()
