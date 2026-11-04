package com.escapeshift.game

/** World navigation is independent of the physics engine and Android drawing. */
data class WorldInfo(val number: Int, val title: String, val subtitle: String, val developed: Boolean)

object WorldCatalog {
    const val WORLD_COUNT = 10
    const val LEVELS_PER_WORLD = 10
    val worlds = listOf(
        WorldInfo(1, "world_01_title", "world_01_subtitle", true),
        WorldInfo(2, "world_02_title", "world_02_subtitle", true),
        WorldInfo(3, "world_03_title", "world_03_subtitle", false),
        WorldInfo(4, "world_04_title", "world_04_subtitle", false),
        WorldInfo(5, "world_05_title", "world_05_subtitle", false),
        WorldInfo(6, "world_06_title", "world_06_subtitle", false),
        WorldInfo(7, "world_07_title", "world_07_subtitle", false),
        WorldInfo(8, "world_08_title", "world_08_subtitle", false),
        WorldInfo(9, "world_09_title", "world_09_subtitle", false),
        WorldInfo(10, "world_10_title", "world_10_subtitle", false)
    )
    fun world(index: Int): WorldInfo = worlds[index.coerceIn(1, WORLD_COUNT) - 1]
    fun clamp(index: Int): Int = index.coerceIn(1, WORLD_COUNT)
    /** Worlds 1 and 2 retain their real V20 levels. Other worlds offer only level 1 previews. */
    fun isPlayable(world: Int, level: Int): Boolean =
        world in 1..WORLD_COUNT && level in 1..LEVELS_PER_WORLD && (world <= 2 || level == 1)

    /** The first slot in each later world is a JSON preview of level 01. */
    fun isPreviewLevel(number: Int): Boolean =
        number in 11..91 && (number - 1) % LEVELS_PER_WORLD == 0

    fun isAvailableLevel(number: Int): Boolean =
        number in 1..20 || isPreviewLevel(number)
    fun totalSlots(): Int = WORLD_COUNT * LEVELS_PER_WORLD
    fun swipe(current: Int, dx: Float, dy: Float, minDistance: Float = 65f): Int {
        if (kotlin.math.abs(dx) < minDistance || kotlin.math.abs(dx) < kotlin.math.abs(dy) * 1.2f) return current
        return clamp(if (dx < 0f) current + 1 else current - 1)
    }
}
