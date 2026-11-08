import com.escapeshift.game.ExperimentalPuzzleEngine
import com.escapeshift.game.LevelCatalog
import java.io.File

fun main() {
    var passed = 0
    for (n in 21..40) {
        val raw = File("app/src/main/assets/levels/${LevelCatalog.filename(n)}").readText(Charsets.UTF_8)
        val puzzle = ExperimentalPuzzleEngine.fromLevelJson(raw)
        val result = puzzle.replaySolution()
        require(result.solved && !result.failed) { "Level $n logic failed: $result" }
        val early = ExperimentalPuzzleEngine.fromLevelJson(raw)
        require(!early.apply(mapOf("action" to "exit", "target" to "doorA")).solved) { "Level $n exit bypass" }
        val physicalSpec = com.escapeshift.game.LevelJsonParser.parse(raw, n)
        require(physicalSpec.number == n && physicalSpec.objects.single { it.type == "door" }.x > 0)
        passed++
        println("level_${n.toString().padStart(2,'0')}: Kotlin JSON + logic PASS")
    }
    println("KOTLIN LOGIC PUZZLES: $passed/20 PASS (no physics pathfinding)")
}
