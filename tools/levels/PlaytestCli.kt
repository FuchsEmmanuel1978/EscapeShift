package com.escapeshift.game

import java.io.File

/** Native JVM simulator using the exact GameWorld physics as Android. */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: "app/src/main/assets/levels")
    require(root.isDirectory) { "Levels folder not found: ${root.absolutePath}" }
    LevelCatalog.install { n -> File(root, LevelCatalog.filename(n)).readText(Charsets.UTF_8) }
    val selected = args.getOrNull(1)?.toIntOrNull()?.let { listOf(it) } ?: (1..20).toList()
    var failed = 0
    for (level in selected) {
        val data = LevelCatalog.create(level)
        val result = LevelPlaytester.test(level)
        println("N%02d %-5s %4d ticks (%5.2f s)  jumps=%-2d shifts=%-2d reversals=%-2d  obstacles=%-2d".format(
            level, if (result.passed) "PASS" else "FAIL", result.ticks,
            result.ticks.toFloat() / 60f, result.jumps, result.shifts,
            result.reversals, data.objects.size
        ))
        if (!result.passed) {
            failed++
            result.checks.filterNot { it.passed }.forEach { println("    ${it.name}") }
            result.events.takeLast(4).forEach { println("    $it") }
            println("    TRACE_LAST: " + result.csv.lineSequence().filter { it.isNotBlank() }.last())
        }
        val traceFile = File("tools/levels/reports/level_%02d.csv".format(level))
        traceFile.parentFile.mkdirs()
        traceFile.writeText(result.csv)
    }
    println("Summary: ${selected.size - failed}/${selected.size} winning runs")
    if (failed != 0) error("One or more levels are not validated")
}
