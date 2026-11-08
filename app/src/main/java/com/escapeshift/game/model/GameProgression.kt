package com.escapeshift.game

import kotlin.math.max

/** Persistent unlocked level and implemented playable levels are different things. */
object GameProgression {
    const val MAX_LEVEL = 30 // Levels 1..30 playable; 31..40 remain experimental JSON prototypes.
    const val DISPLAY_LEVELS = 10 // Slots shown in the level selection menu.

    fun safeUnlocked(value: Int): Int = value.coerceAtLeast(1)

    /** A level is unlocked by progress, even if it is not implemented yet. */
    fun isUnlocked(level: Int, unlocked: Int): Boolean =
        level >= 1 && level <= safeUnlocked(unlocked)

    fun isPlayable(level: Int, unlocked: Int): Boolean =
        level in 1..MAX_LEVEL && isUnlocked(level, unlocked)

    /** Winning N always unlocks N+1; never reduce the saved progress. */
    fun afterWin(unlocked: Int, level: Int): Int =
        max(safeUnlocked(unlocked), level + 1)

    /** NEXT exists only if the next level's game content is implemented. */
    fun nextLevel(level: Int, unlocked: Int): Int? =
        (level + 1).takeIf { isPlayable(it, unlocked) }

    /** Start the highest playable level, not a placeholder. */
    fun latestPlayable(unlocked: Int): Int = safeUnlocked(unlocked).coerceAtMost(MAX_LEVEL)
}
