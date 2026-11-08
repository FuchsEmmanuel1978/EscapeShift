package com.escapeshift.game

import java.io.File

/** Showcase first-level previews for world 2 and worlds 4..10. World 3 levels 21..23 are now real. */
object WorldPreviewCatalog {
    private val previewLevels = (listOf(2) + (4..10)).map { (it - 1) * 10 + 1 }.toSet()
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
