package com.escapeshift.game

import java.io.File

/** Scripted keyboard actions only: moves through GameWorld.tick, no setX, teleports, or forced completion. */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: "app/src/main/assets/levels")
    LevelCatalog.install { n -> File(root, LevelCatalog.filename(n)).readText() }
    fun step(g: GameWorld, left: Boolean=false, right: Boolean=false, jump: Boolean=false, interact: Boolean=false) {
        g.tick(GameInput(left=left, right=right, jump=jump, interact=interact))
        check(!g.dead) { "Level ${g.levelNumber}: death at x=${g.x}, t=${g.ticks}" }
    }
    fun moveTo(g: GameWorld, target: Float, maxTicks: Int=1100, jumpWhen: ((GameWorld)->Boolean)?=null) {
        var jumpPrev = false
        repeat(maxTicks) {
            val dx = target - g.x
            if (kotlin.math.abs(dx) < 4.9f || g.won) return
            val wantsJump = jumpWhen?.invoke(g) == true
            val jump = wantsJump && !jumpPrev
            jumpPrev = wantsJump
            step(g, left=dx < 0f, right=dx > 0f, jump=jump)
        }
        error("Level ${g.levelNumber}: movement blocked x=${g.x}, target=$target")
    }
    fun action(g: GameWorld) { step(g, interact=true); step(g) }
    fun verify(g: GameWorld) {
        check(g.won && !g.dead) { "Level ${g.levelNumber}: no physical win, x=${g.x}, puzzle=${g.advancedPuzzle?.feedback}" }
        println("Level ${g.levelNumber}: REAL-PHYSICS PASS, ticks=${g.ticks}, x=${g.x.toInt()}")
    }
    run {
        val g = GameWorld(24)
        moveTo(g, 155f)
        moveTo(g, 375f, jumpWhen = { it.grounded && it.x in 155f..180f })
        repeat(45) { step(g) }
        action(g); check(g.carryingCrate) { "24: pickup B failed, x=${g.x}, crates=${g.crateBoxes}" }
        moveTo(g, 526f); action(g)
        moveTo(g, 175f); action(g); check(g.carryingCrate)
        moveTo(g, 618f, jumpWhen = { it.grounded && it.x in 475f..510f })
        repeat(45) { step(g) }
        action(g)
        moveTo(g, 540f); action(g); check(g.carryingCrate)
        moveTo(g, 1616f); action(g)
        check(g.exitUnlocked) { "24: unsolved; crates=${g.crateBoxes}" }
        moveTo(g, 2480f)
        verify(g)
    }
    run {
        val g=GameWorld(26)
        moveTo(g, 350f)
        repeat(3) { action(g) }
        check(g.advancedPuzzle!!.wheelAngle("wheelA") == 135)
        moveTo(g, 2480f, jumpWhen = { it.grounded && it.x in 1515f..1545f })
        verify(g)
    }
    run {
        val g=GameWorld(29)
        moveTo(g, 2480f, jumpWhen = {
            it.grounded && (it.x in 500f..535f || it.x in 1060f..1095f || it.x in 1310f..1350f)
        })
        verify(g)
    }
    run {
        val g=GameWorld(32)
        moveTo(g, 1710f)
        repeat(4) { action(g) }
        check(g.advancedPuzzle!!.wheelAngle("wheelA") == 180)
        moveTo(g,2480f)
        verify(g)
    }
    run {
        val g=GameWorld(34)
        moveTo(g, 1405f)
        // Stepping on and then off the inverse plate should latch its release state.
        check(!g.exitUnlocked)
        moveTo(g, 1570f)
        check(g.advancedPuzzle!!.box("springA")!!.left >= 1560f)
        moveTo(g, 2480f, jumpWhen = { it.grounded && it.x in 1540f..1580f })
        verify(g)
    }
    run {
        val g=GameWorld(35)
        moveTo(g, 2480f, jumpWhen = {
            it.grounded && (it.x in 740f..765f || it.x in 1375f..1425f || it.x in 1780f..1830f)
        })
        verify(g)
    }
    run {
        val g=GameWorld(39)
        moveTo(g, 1388f)
        action(g); check(g.carryingCrate)
        moveTo(g, 1743f); action(g)
        check(g.advancedPuzzle!!.feedback.isNotBlank())
        moveTo(g, 1650f)
        moveTo(g, 2480f, jumpWhen = { it.grounded && it.x in 1690f..1720f })
        verify(g)
    }
}
