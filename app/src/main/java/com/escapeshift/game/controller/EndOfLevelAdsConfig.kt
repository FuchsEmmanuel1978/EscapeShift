package com.escapeshift.game

/**
 * END-OF-LEVEL INTERSTITIAL SETTINGS (V18).
 * Debug mode has no time cap so it is easy to verify both win and defeat placements
 * with TEST ADS. Production uses a two-minute cooldown: repeated rapid Game Overs
 * must not become an ad trap.
 */
object EndOfLevelAdsConfig {
    const val SHOW_AFTER_VICTORY = true
    const val SHOW_AFTER_DEFEAT = true
    const val EVERY_N_RESULTS = 1
    const val DEBUG_MIN_INTERVAL_MS = 0L
    const val RELEASE_MIN_INTERVAL_MS = 120_000L
    const val RESULT_SCREEN_DELAY_MS = 1400L
}

/** Pure, deterministic ad-placement policy; never triggers an ad by itself. */
class EndOfLevelAdPolicy(
    private val everyNResults: Int = EndOfLevelAdsConfig.EVERY_N_RESULTS,
    private val minIntervalMs: Long = EndOfLevelAdsConfig.RELEASE_MIN_INTERVAL_MS
) {
    private var resultsSinceAd = 0
    private var lastShownMs: Long? = null

    fun recordResult(victory: Boolean, nowMs: Long): Boolean {
        if (victory && !EndOfLevelAdsConfig.SHOW_AFTER_VICTORY) return false
        if (!victory && !EndOfLevelAdsConfig.SHOW_AFTER_DEFEAT) return false
        resultsSinceAd++
        if (resultsSinceAd < everyNResults.coerceAtLeast(1)) return false
        val last = lastShownMs
        return last == null || nowMs - last >= minIntervalMs
    }

    fun adWasShown(nowMs: Long) {
        lastShownMs = nowMs
        resultsSinceAd = 0
    }
}
