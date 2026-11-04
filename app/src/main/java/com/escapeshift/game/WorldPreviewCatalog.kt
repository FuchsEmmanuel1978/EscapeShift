package com.escapeshift.game

import java.io.File

/** Nine first-level previews. Original world-2 JSON levels 11..20 are left intact. */
object WorldPreviewCatalog {
    private val previewLevels = (2..10).map { (it - 1) * 10 + 1 }.toSet()
    private var readJson: (Int) -> String = { number ->
        val name = filename(number)
        val file = listOf(
            File("app/src/main/assets/previews/$name"),
            File("src/main/assets/previews/$name"),
            File("../app/src/main/assets/previews/$name")
        ).firstOrNull { it.isFile } ?: error("Preview file not found: $name")
        file.readText(Charsets.UTF_8)
    }
    private val cache = mutableMapOf<Int, LevelSpec>()

    fun filename(number: Int): String = "level_${number.toString().padStart(2, '0')}.json"

    @Synchronized fun install(loader: (Int) -> String) {
        readJson = loader
        cache.clear()
    }

    @Synchronized fun create(number: Int): LevelSpec {
        require(number in previewLevels) { "No preview exists for level $number" }
        return cache.getOrPut(number) { LevelJsonParser.parse(readJson(number), number) }
    }
}
