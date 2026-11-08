package com.escapeshift.game

/**
 * START.IO CONFIGURATION
 *
 * For development the official Start.io demo App ID is used with test ads only.
 * To use live ads, review the application's Google Play target audience, update
 * privacy disclosures and implement an appropriate consent flow first.
 *
 * IMPORTANT: Start.io is NOT on Google Play's Families Self-Certified Ads SDK list.
 * Don't enable it for an audience that includes children unless a compliant
 * age-screening / certified SDK solution is implemented and checked.
 */
object StartioConfig {
    /** Your real App ID from portal.start.io, not the Google Play package name. */
    const val APP_ID = ""

    /** Debug uses test advertisements only. */
    const val DEBUG_TEST_ADS = true
    const val STARTIO_DEMO_APP_ID = "205489527"

    /** SAFETY GATES: All must be true for release ads to initialize. */
    const val RELEASE_ADS_ENABLED = false
    const val AUDIENCE_APPROVED_FOR_STARTIO = false
    const val PRIVACY_DECLARATIONS_UPDATED = false
    const val CONSENT_FLOW_CONFIGURED = false

    /** Show only after victory, and only between *existing* levels. */
    const val EVERY_N_VICTORIES = 1
    const val MIN_INTERVAL_MS = 120_000L
    const val VICTORY_SCREEN_DELAY_MS = 1400L

    fun canRun(isDebugBuild: Boolean): Boolean =
        if (isDebugBuild) DEBUG_TEST_ADS
        else RELEASE_ADS_ENABLED && AUDIENCE_APPROVED_FOR_STARTIO &&
            PRIVACY_DECLARATIONS_UPDATED && CONSENT_FLOW_CONFIGURED && APP_ID.isNotBlank()

    fun appId(isDebugBuild: Boolean) =
        if (isDebugBuild) STARTIO_DEMO_APP_ID else APP_ID
}

/** Pure logic, deterministic and separately testable without Android SDK. */
class InterstitialPolicy(
    private val everyNWins: Int = StartioConfig.EVERY_N_VICTORIES,
    private val minIntervalMs: Long = StartioConfig.MIN_INTERVAL_MS
) {
    private var winsSinceAd = 0
    private var lastShownMs: Long? = null

    /** Returns true only for a playable next-level transition. */
    fun recordVictory(hasNextPlayableLevel: Boolean, nowMs: Long): Boolean {
        if (!hasNextPlayableLevel) return false
        winsSinceAd++
        if (winsSinceAd < everyNWins.coerceAtLeast(1)) return false
        val last = lastShownMs
        return last == null || nowMs - last >= minIntervalMs
    }

    fun adWasShown(nowMs: Long) {
        lastShownMs = nowMs
        winsSinceAd = 0
    }
}
