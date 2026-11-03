package com.escapeshift.game

import kotlin.math.abs

/**
 * Deterministic input-driver for World 2 (levels 11..20).
 * It uses the exact production physics, not a geometry approximation.
 * Its policy is intentionally published and replayable; it is NOT a universal solver.
 */
object World2Playtester {
    fun test(level: Int): PlaytestResult {
        require(level in 11..20)
        val w = GameWorld(level)
        val log = mutableListOf<String>()
        val trace = StringBuilder("tick,stage,x,y,crate,shift,key,switch,dead,won,plates\n")
        val pairs = w.spec.objects.filter { it.type == "crate" }.sortedBy { it.x }.map { crate ->
            crate to w.spec.objects.filter { it.type == "wall" && it.x > crate.x }
                .minBy { it.x - crate.x }
        }
        var moduleIndex = 0
        var modulePhase = 0
        var phaseTicks = 0
        var eventIndex = 0
        var eventTicks = 0
        var committed = false
        var jumps = 0
        var shifts = 0
        var reversals = 0
        var prevDirection = 0
        var prevJump = false
        var prevShift = false
        var seen = BooleanArray(w.spec.challenges.size)
        var timeout = false
        while (w.ticks < 7800 && !w.dead && !w.won) {
            var cmd = GameInput()
            var stage = "exit"
            var advance = false
            if (moduleIndex < pairs.size) {
                val (crate, wall) = pairs[moduleIndex]
                val box = w.crateBoxes[moduleIndex]
                stage = "crate${moduleIndex+1}-phase$modulePhase"
                when(modulePhase) {
                    0 -> {
                        cmd = GameInput(right = true)
                        if (box.left >= wall.x - crate.width - .11f && w.grounded) advance = true
                    }
                    1 -> {
                        // Jump onto the crate, remain directly above it until landing.
                        cmd = GameInput(right = phaseTicks < 28, jump = phaseTicks == 0)
                        if (phaseTicks > 16 && w.grounded && w.y < 388f) advance = true
                    }
                    2 -> {
                        // From top of crate, jump onto 120px wall and over its far edge.
                        cmd = GameInput(right = true, jump = phaseTicks == 0)
                        if (w.x > wall.x + wall.width + 80f && w.grounded) advance = true
                    }
                }
                if (advance) {
                    log.add("crate module ${moduleIndex+1} phase $modulePhase at tick ${w.ticks} x=${w.x.toInt()}")
                    modulePhase++
                    phaseTicks = 0
                    if (modulePhase == 3) { moduleIndex++; modulePhase=0 }
                    continue
                }
                phaseTicks++
            } else if (eventIndex < w.spec.challenges.size) {
                val e = w.spec.challenges[eventIndex]
                stage = "${e.type}-$eventIndex"
                val startMargin = when (e.type) {
                    ChallengeType.SAW -> 128f + e.amplitude * .25f
                    ChallengeType.MONSTER -> 103f
                    ChallengeType.GAP -> 75f
                    ChallengeType.SPIKE -> 79f
                    ChallengeType.WALL -> 82f
                    else -> 85f
                }
                val begin = e.x - startMargin
                val finish = e.x + when(e.type) {
                    ChallengeType.GAP -> e.width + 34f
                    ChallengeType.SPIKE -> e.width * (w.spec.objects.first {it.x == e.x && it.type == "spike"}.count) + 45f
                    ChallengeType.SAW -> 126f
                    ChallengeType.MONSTER -> 110f
                    ChallengeType.WALL -> 85f
                    else -> 87f
                }
                if (w.x < begin) cmd = GameInput(right = true)
                else {
                    when(e.type) {
                        ChallengeType.GAP, ChallengeType.SPIKE, ChallengeType.SAW,
                        ChallengeType.MONSTER, ChallengeType.WALL -> {
                            cmd = GameInput(right=true, jump=eventTicks==0)
                            if (w.x > finish && w.grounded) advance = true
                        }
                        ChallengeType.RED_LASER, ChallengeType.BLUE_LASER -> {
                            val desired = e.type == ChallengeType.RED_LASER
                            cmd = if (w.shiftOn != desired) GameInput(shift=true) else GameInput(right=true)
                            if (w.x > finish) advance=true
                        }
                        ChallengeType.SWITCH -> {
                            cmd=GameInput(right=true)
                            if (w.x > finish) advance=true
                        }
                        else -> {
                            cmd=GameInput(right=true)
                            if (w.x > finish) advance=true
                        }
                    }
                    eventTicks++
                    if (advance) {
                        seen[eventIndex] = true
                        log.add("route $eventIndex ${e.type} done at tick ${w.ticks}, x=${w.x.toInt()}")
                        eventIndex++;eventTicks=0;committed=false
                        continue
                    }
                }
            } else cmd = GameInput(right=true)
            val direction = (if(cmd.right)1 else 0) - (if(cmd.left)1 else 0)
            if (direction != 0 && prevDirection != 0 && prevDirection != direction) reversals++
            if (direction != 0) prevDirection = direction
            if (cmd.jump && !prevJump) jumps++
            if (cmd.shift && !prevShift) shifts++
            prevJump=cmd.jump;prevShift=cmd.shift
            w.tick(cmd)
            if (w.ticks % 6 == 0 || w.dead || w.won) {
                trace.append("${w.ticks},$stage,${w.x},${w.y},${w.crateX},${w.shiftOn},${w.keyCollected},${w.switchActivated},${w.dead},${w.won},${w.pressureReady}\n")
            }
        }
        if (w.ticks >= 7800) timeout=true
        val checks = listOf(
            PlaytestCheck("victory", w.won && !w.dead),
            PlaytestCheck("crate modules done", moduleIndex==pairs.size),
            PlaytestCheck("route completed", seen.all { it }),
            PlaytestCheck("all player switches", w.switches.isEmpty() || w.switchActivated),
            PlaytestCheck("all crate pressure pads", w.pressureReady),
            PlaytestCheck("no timeout", !timeout)
        )
        log.add("END tick=${w.ticks} x=${w.x.toInt()} y=${w.y.toInt()} dead=${w.dead} won=${w.won} module=$moduleIndex phase=$modulePhase event=$eventIndex, eventTicks=$eventTicks")
        return PlaytestResult(w.ticks,checks.all {it.passed},checks,log,trace.toString(),jumps,shifts,reversals)
    }
}
