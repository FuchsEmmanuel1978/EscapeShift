import com.escapeshift.game.LevelPlaytester
import java.io.File

/** Simulate and save the real game physics for every level. */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "tools/results")
    out.mkdirs()
    for (level in 1..10) {
        val result = LevelPlaytester.test(level)
        File(out, "level${level}_trace.csv").writeText(result.csv)
        println("Niveau $level : ${if (result.passed) "PASS" else "FAIL"} | ${result.ticks} frames / ${"%.1f".format(result.ticks/60f)} s | sauts=${result.jumps}, SHIFT=${result.shifts}, demi-tours=${result.reversals}")
        result.checks.forEach { println("  ${if(it.passed) "OK" else "FAIL"} ${it.name}") }
        require(result.passed) { "Le niveau $level est bloque : ${result.events.takeLast(5)}" }
    }
}
