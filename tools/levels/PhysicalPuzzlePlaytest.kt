package com.escapeshift.game

import java.io.File

/** Uses actual GameWorld.tick() collisions; no teleporting or shortcut puzzle actions. */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "app/src/main/assets/levels")
    LevelCatalog.install { n -> File(dir, LevelCatalog.filename(n)).readText(Charsets.UTF_8) }
    fun report(n: Int, game: GameWorld, mustBeTrue: Boolean = game.won) {
        check(mustBeTrue && !game.dead && game.won) { "N$n not physically winnable with recorded input path, x=${game.x}, y=${game.y}" }
        println("LEVEL $n PASS: ${game.ticks} physics ticks, x=${game.x.toInt()} (door unlocked by actual interactions)")
    }

    run {
        val g = GameWorld(21); val scene = g.physicalPuzzle!!
        check(!g.exitUnlocked)
        var phase = 0
        var previousJump = false
        for (tick in 0..1200) {
            if (phase == 0 && scene.isBridgePlaced) phase = 1
            if (phase == 1 && g.x > 1240f) phase = 2
            if (phase == 2 && scene.isReturning) phase = 3
            if (phase == 3 && g.x <= 85f) phase = 4
            if (phase == 4 && scene.buttonStates.first()) phase = 5
            val left = phase == 2 || phase == 3
            val right = !left
            val desiredJump = g.grounded && (
                (phase == 1 && g.x in 785f..835f) ||
                (phase == 2 && g.x in 1210f..1240f) ||
                (phase == 3 && g.x in 300f..365f) ||
                (phase >= 4 && g.x in 785f..835f) ||
                (phase == 5 && g.x in 425f..525f)
            )
            val jump = desiredJump && !previousJump
            previousJump = desiredJump
            g.tick(GameInput(left=left, right=right, jump=jump))
            if (g.dead || g.won) break
        }
        check(scene.isBridgePlaced && scene.isReturning && scene.buttonStates.first())
        report(21, g)
    }

    run {
        val g = GameWorld(22); val scene = g.physicalPuzzle!!
        check(!g.exitUnlocked)
        for (tick in 0..420) {
            g.tick(GameInput(right=true, jump=tick in 125..145))
            if (g.dead || g.won) break
        }
        check(scene.isLaserCrossed)
        report(22, g)
        g.reset()
        check(!g.exitUnlocked && scene.laserActive && !scene.isLaserCrossed) { "N22: reset failed" }
    }

    run {
        val g = GameWorld(23); val scene = g.physicalPuzzle!!
        check(!g.exitUnlocked)
        var phase = 0; var jumpReleased = true
        for (tick in 0..1100) {
            if (phase == 0 && scene.buttonStates[0]) phase = 1
            if (phase == 1 && g.x > 800f) phase = 2
            if (phase == 2 && scene.buttonStates[3]) phase = 3
            if (phase == 3 && g.x > 1310f && g.grounded) phase = 4
            val desiredJump = g.grounded && when (phase) {
                1 -> g.x in 540f..640f
                3 -> g.x in 1080f..1190f
                4 -> g.x in 1305f..1385f || scene.landedCount in 1..2
                else -> false
            }
            val jump = desiredJump && jumpReleased
            if (jump) jumpReleased = false
            if (!desiredJump) jumpReleased = true
            val advance = when {
                scene.landedCount == 1 && g.x > 1690f -> false
                scene.landedCount == 2 && g.x > 1890f -> false
                else -> true
            }
            g.tick(GameInput(right=advance, jump=jump))
            if (g.dead || g.won) break
        }
        check(scene.isSynced && scene.landedCount == 3) { "N23 did not activate 1+4 / land on all moving platforms" }
        report(23, g)
    }
    println("PASS: 3/3 new levels physically completed with game collisions")
}
