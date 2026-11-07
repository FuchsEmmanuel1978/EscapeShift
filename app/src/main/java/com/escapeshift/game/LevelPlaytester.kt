package com.escapeshift.game

data class PlaytestCheck(val name: String, val passed: Boolean)
data class PlaytestResult(
    val ticks: Int,
    val passed: Boolean,
    val checks: List<PlaytestCheck>,
    val events: List<String>,
    val csv: String,
    val jumps: Int,
    val shifts: Int,
    val reversals: Int
)

/**
 * Deterministic input script, executed against the EXACT GameWorld used by Android.
 * Proves that a winning sequence exists. It is not yet an autonomous level solver.
 */
object LevelPlaytester {
    fun test(level: Int): PlaytestResult {
        if (level in 11..20) return World2Playtester.test(level)
        val w = GameWorld(level)
        val log = mutableListOf<String>()
        val trace = StringBuilder("tick,stage,x,y,crate,shift,key,switch,dead,won\n")
        var phase = 0
        var phaseTicks = 0
        var eventIndex = 0
        var eventTicks = 0
        var returning = false
        var reverseIndex = w.spec.challenges.lastIndex
        var committed = false
        var jumps = 0; var shifts = 0; var reversals = 0
        var lastDirection = 0
        var lastJump = false; var lastShift = false
        var introDone = w.spec.intro == StartType.OPEN
        val introChecks = mutableSetOf<String>()
        val visited = BooleanArray(w.spec.challenges.size)
        var riskTimeout = false
        // Mandatory vertical staircase loaded exclusively from the level JSON.
        // These platforms form the final ascent towards an elevated exit.
        val exitStairs = w.spec.objects.filter { it.id.startsWith("exit_stair_") && it.type == "platform" }.sortedBy { it.x }
        var stairIndex = 0
        var stairMode = 0   // 0: approach / launch; 1: cross to platform and wait to land
        var stairAirTicks = 0
        while (w.ticks < 9000 && !w.dead && !w.won) {
            var cmd = GameInput()
            var advance = false
            if (!introDone) {
                when (w.spec.intro) {
                    StartType.OPEN -> introDone = true
                    StartType.CRATE_WALL -> {
                        when (phase) {
                            0 -> {
                                cmd = GameInput(right=true)
                                if (w.crateX >= w.spec.objects.first { it.id == "starter_wall" }.x - (w.crateBox.right - w.crateBox.left) - .1f && w.grounded) { introChecks += "crate"; advance = true }
                            }
                            1 -> {
                                cmd = GameInput(right=phaseTicks < 12, jump=phaseTicks == 0)
                                if (phaseTicks > 20 && w.grounded && w.y < w.spec.objects.first { it.id == "starter_wall" }.y + 42f) {
                                    introChecks += "climbed"; advance = true
                                }
                            }
                            2 -> {
                                cmd = GameInput(right=true, jump=phaseTicks==0, shift=phaseTicks==0)
                                if (w.x > (w.bridge?.right ?: 590f) + 23f && w.grounded) { introChecks += "bridge"; advance = true }
                            }
                            3 -> {
                                cmd = GameInput(right=true)
                                if (w.x > (w.bridge?.right ?: 590f) + 34f && w.grounded) advance = true
                            }
                        }
                        if (advance) {
                            log += "starter crate phase $phase at tick ${w.ticks}, x=${w.x}"
                            phase++
                            phaseTicks = 0
                            if (phase == 4) introDone = true
                            continue
                        }
                    }
                    StartType.SHIFT_BRIDGE -> {
                        when (phase) {
                            0 -> { cmd=GameInput(right=true); if (w.x >= (w.bridge?.left ?: 300f) - 90f) advance=true }
                            1 -> { cmd=GameInput(shift=true); if (w.shiftOn) advance=true }
                            2 -> { cmd=GameInput(right=true); if (w.x > (w.bridge?.right ?: 440f) + 45f && w.grounded) advance=true }
                        }
                        if (advance) {
                            phase++; phaseTicks=0
                            if (phase==3) { introDone=true; introChecks += "bridge" }
                            continue
                        }
                    }
                    StartType.SPRING_WALL -> {
                        cmd=GameInput(right=true)
                        if (w.x > (w.spec.objects.first { it.id == "starter_spring_wall" }.x + w.spec.objects.first { it.id == "starter_spring_wall" }.width) + 88f && w.grounded) {introDone=true; introChecks+="spring wall";continue}
                    }
                    StartType.UPPER_PLATFORM -> {
                        when (phase) {
                            0 -> {cmd=GameInput(right=true); if (w.x >= w.spec.objects.first { it.id == "starter_upper_platform" }.x - 100f) advance=true}
                            1 -> {cmd=GameInput(right=true,jump=phaseTicks==0);if(w.x>w.spec.objects.first { it.id == "starter_upper_platform" }.x + w.spec.objects.first { it.id == "starter_upper_platform" }.width + 75f && w.grounded)advance=true}
                        }
                        if(advance){phase++;phaseTicks=0;if(phase==2){introDone=true;introChecks+="upper platform"};continue}
                    }
                }
                phaseTicks++
            } else if (!returning) {
                if(eventIndex >= w.spec.challenges.size) {
                    if(w.spec.backtrackCount>0){returning=true;reversals++;log+="return starts tick ${w.ticks}";continue}
                    if (stairIndex < exitStairs.size) {
                        val target = exitStairs[stairIndex]
                        val onTarget = w.grounded && kotlin.math.abs(w.y + GameWorld.SIZE - target.y) < 3f &&
                            w.playerBox.right > target.x + 10f && w.x < target.x + target.width - 10f
                        if (onTarget) {
                            log += "stair ${stairIndex + 1} at tick ${w.ticks} x=${w.x.toInt()} y=${w.y.toInt()}"
                            stairIndex++
                            stairMode = 0
                            stairAirTicks = 0
                            continue
                        }
                        if (stairMode == 0) {
                            if (!w.grounded) cmd = GameInput()
                            else if (w.x < target.x - 80f) cmd = GameInput(right=true)
                            else {
                                cmd = GameInput(right=true, jump=true)
                                stairMode = 1
                                stairAirTicks = 0
                            }
                        } else {
                            cmd = GameInput(right = stairAirTicks < 19)
                            stairAirTicks++
                            if (stairAirTicks > 95 && w.grounded) {
                                // Allows retry if the first launch landed short.
                                stairMode = 0
                                stairAirTicks = 0
                            }
                        }
                    } else cmd = GameInput(right=true)
                } else {
                    val e = w.spec.challenges[eventIndex]
                    val begin = e.x - when(e.type){ChallengeType.SAW->100f;ChallengeType.MONSTER->100f;ChallengeType.TIMED_LASER->85f;ChallengeType.GAP->110f;ChallengeType.CONVEYOR->37f;else->90f}
                    val finish = e.x + when(e.type){
                        ChallengeType.SPRING->291f
                        ChallengeType.GAP->e.width+65f
                        ChallengeType.SAW->122f
                        ChallengeType.MONSTER->123f
                        ChallengeType.SPIKE->118f
                        ChallengeType.CONVEYOR->e.width+5f
                        else->95f
                    }
                    if(w.x < begin) cmd = GameInput(right=true)
                    else {
                        when(e.type){
                            ChallengeType.SPIKE, ChallengeType.SAW, ChallengeType.MONSTER, ChallengeType.GAP, ChallengeType.WALL -> {
                                cmd=GameInput(right=true,jump=eventTicks==0)
                                if(w.x>finish && w.grounded)advance=true
                            }
                            ChallengeType.RED_LASER, ChallengeType.BLUE_LASER -> {
                                val required=e.type==ChallengeType.RED_LASER
                                cmd=if(w.shiftOn!=required)GameInput(shift=true)else GameInput(right=true)
                                if(w.x>finish)advance=true
                            }
                            ChallengeType.TIMED_LASER -> {
                                if(!committed) committed=(6..34).all{!w.timedLaserActive(e,w.ticks+it)}
                                cmd=if(committed)GameInput(right=true)else GameInput()
                                if(w.x>finish)advance=true
                            }
                            ChallengeType.KEY -> {
                                cmd=GameInput(right=true)
                                if(w.keyCollected && w.x>finish)advance=true
                            }
                            ChallengeType.SWITCH -> {
                                cmd=GameInput(right=true)
                                if(w.switchActivated && w.x>finish)advance=true
                            }
                            ChallengeType.SPRING -> {
                                cmd=GameInput(right=true)
                                if(w.x>finish && w.grounded)advance=true
                            }
                            ChallengeType.CONVEYOR -> {
                                cmd=GameInput(right=true,jump=eventTicks==0)
                                if(w.x>finish && w.grounded)advance=true
                            }
                        }
                        eventTicks++
                    }
                    if(advance){
                        visited[eventIndex]=true
                        log+="forward $eventIndex ${e.type} done at tick ${w.ticks} x=${w.x}"
                        eventIndex++;eventTicks=0;committed=false
                        continue
                    }
                }
            } else {
                // Only traverse features to the RIGHT of the locked exit again.
                if(reverseIndex<0 || w.spec.challenges[reverseIndex].x < w.exit.right+65f){
                    cmd=GameInput(left=true)
                } else {
                    val e=w.spec.challenges[reverseIndex]
                    val begin=e.x+when(e.type){ChallengeType.SAW->145f;ChallengeType.MONSTER->145f;ChallengeType.GAP->e.width+135f;else->90f}
                    val finish=e.x-100f
                    if(w.x>begin){cmd=GameInput(left=true)}
                    else {
                        when(e.type){
                            ChallengeType.SPIKE, ChallengeType.SAW, ChallengeType.MONSTER, ChallengeType.GAP, ChallengeType.WALL -> {
                                cmd=GameInput(left=true,jump=eventTicks==0)
                                if(w.x<finish && w.grounded)advance=true
                            }
                            ChallengeType.RED_LASER, ChallengeType.BLUE_LASER -> {
                                val required=e.type==ChallengeType.RED_LASER
                                cmd=if(w.shiftOn!=required)GameInput(shift=true)else GameInput(left=true)
                                if(w.x<finish)advance=true
                            }
                            ChallengeType.TIMED_LASER -> {
                                if(!committed)committed=(6..34).all{!w.timedLaserActive(e,w.ticks+it)}
                                cmd=if(committed)GameInput(left=true)else GameInput()
                                if(w.x<finish)advance=true
                            }
                            else -> {
                                cmd=GameInput(left=true)
                                if(w.x<finish)advance=true
                            }
                        }
                        eventTicks++
                    }
                    if(advance){
                        log+="reverse $reverseIndex ${e.type} done at tick ${w.ticks}"
                        reverseIndex--;eventTicks=0;committed=false
                        continue
                    }
                }
            }
            val direction = (if(cmd.right)1 else 0)-(if(cmd.left)1 else 0)
            if(direction!=0 && lastDirection!=0 && direction !=lastDirection)reversals++
            if(direction!=0)lastDirection=direction
            if(cmd.jump && !lastJump)jumps++
            if(cmd.shift && !lastShift)shifts++
            lastJump=cmd.jump;lastShift=cmd.shift
            // At 1.2x run speed, short direction releases allow fine obstacle timing.
            // These are real user inputs sent to the same physical game engine.
            val stepPeriod = if (level in setOf(2, 8, 10)) 5 else 6
            val stepOffset = if (stepPeriod == 5) 0 else 5
            if (GameVisualConfig.HERO_SPEED_MULTIPLIER > 1f && w.ticks % stepPeriod == stepOffset && !cmd.jump) {
                cmd = cmd.copy(left = false, right = false)
            }
            w.tick(cmd)
            if(w.ticks%6==0 || w.dead || w.won) {
                val stage=if(!introDone)"intro$phase" else if(returning)"return$reverseIndex" else "event$eventIndex"
                trace.append("${w.ticks},$stage,${w.x},${w.y},${w.crateX},${w.shiftOn},${w.keyCollected},${w.switchActivated},${w.dead},${w.won}\n")
            }
        }
        if(w.ticks>=9000)riskTimeout=true
        val checks=listOf(
            PlaytestCheck("exit reached alive",w.won&&!w.dead),
            PlaytestCheck("all ${visited.size} forward challenges visited",visited.all{it}),
            PlaytestCheck("crate pushed & climbed if required", !w.hasCrate || ("crate" in introChecks && "climbed" in introChecks)),
            PlaytestCheck("starter finished",introDone),
            PlaytestCheck("key obtained if needed",!w.spec.requiresKey||w.keyCollected),
            PlaytestCheck("switch triggered if needed",!w.spec.requiresSwitch||w.switchActivated),
            PlaytestCheck("backtrack executed if requested",w.spec.backtrackCount==0 || returning),
            PlaytestCheck("all elevated exit platforms climbed", stairIndex == exitStairs.size),
            PlaytestCheck("no time-out",!riskTimeout)
        )
        return PlaytestResult(w.ticks,checks.all{it.passed},checks,log,trace.toString(),jumps,shifts,reversals)
    }
}
