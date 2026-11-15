package com.escapeshift.game

import android.content.Context

/** Local persistent deaths and skipped levels; no rewards are granted by this class. */
class SkipProgress(context: Context) {
    private val entitlements = WorldEntitlements(context)
    private val prefs = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    private fun deathKey(level: Int) = "economy_deaths_v1_$level"
    private fun skipKey(level: Int) = "economy_skipped_v1_$level"
    fun deaths(level: Int): Int = prefs.getInt(deathKey(level), 0).coerceAtLeast(0)
    fun recordDeath(level: Int) {
        prefs.edit().putInt(deathKey(level), (deaths(level) + 1).coerceAtMost(999999)).apply()
    }
    fun canSkip(level: Int, unlocked: Int): Boolean =
        level in 1 until GameProgression.MAX_LEVEL &&
            entitlements.canPlay(level, unlocked) &&
            !isSkipped(level) && deaths(level) >= BizConfig.Progression.DEATHS_TO_OFFER_SKIP
    fun isSkipped(level: Int): Boolean = prefs.getBoolean(skipKey(level), false)
    fun markSkipped(level: Int) { prefs.edit().putBoolean(skipKey(level), true).apply() }
}
