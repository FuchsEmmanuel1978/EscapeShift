package com.escapeshift.game

import java.io.File

/** JVM-only regression check for all nine copies of level 01. */
fun main() {
    LevelCatalog.useProjectFiles()
    val baseline = LevelCatalog.create(1)
    var passed = 0
    for (w in 2..10) {
        val n = (w - 1) * 10 + 1
        val p = WorldPreviewCatalog.create(n)
        check(p.number == n)
        check(p.worldRight == baseline.worldRight)
        check(p.startX == baseline.startX && p.startY == baseline.startY)
        check(p.objects == baseline.objects)
        check(p.gaps == baseline.gaps)
        check(p.challenges == baseline.challenges)
        check(WorldCatalog.isPlayable(w,1))
        check(!WorldCatalog.isPlayable(w,2) || w==2)
        val sim = GameWorld(n,p)
        var i=0
        while (!sim.won && !sim.dead && i<1800) {
            // Level 1 is a simple run and single jump over a spike.
            val jump = (sim.x >= 285f && sim.x <= 310f && sim.grounded)
            sim.tick(GameInput(right=true,jump=jump))
            i++
        }
        check(sim.won && !sim.dead) { "Preview $n failed after $i ticks x=${sim.x}" }
        println("PASS WORLD $w PREVIEW LEVEL $n = level01 geometry / $i ticks")
        passed++
    }
    check(LevelCatalog.create(11).objects.size > WorldPreviewCatalog.create(11).objects.size)
    println("SUMMARY: $passed/9 preview files and 1 original-level preservation check")
}
