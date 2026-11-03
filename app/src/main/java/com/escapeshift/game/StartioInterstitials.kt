package com.escapeshift.game

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

/**
 * Interstitials only at game result screens: BRAVO or GAME OVER.
 * Never show at app start, mid-game, after tapping NEXT/REJOUER, or when unavailable.
 */
class StartioInterstitials(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    // Tests can show each eligible result; production is frequency-capped.
    private val policy = EndOfLevelAdPolicy(
        minIntervalMs = if (activity.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
            EndOfLevelAdsConfig.DEBUG_MIN_INTERVAL_MS
        else EndOfLevelAdsConfig.RELEASE_MIN_INTERVAL_MS
    )
    private val isDebugBuild = activity.applicationInfo.flags and
        android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val enabled = StartioConfig.canRun(isDebugBuild)
    private var readyAd: StartAppAd? = null
    private var loading = false
    private var initialized = false
    private var pending: Runnable? = null
    private var paused = false
    private var destroyed = false

    fun initialize() {
        if (!enabled) return
        if (isDebugBuild) StartAppSDK.setTestAdsEnabled(true)
        // Disable automatic splash/return placements: only this class may show ads.
        StartAppAd.disableSplash()
        StartAppSDK.initParams(activity, StartioConfig.appId(isDebugBuild))
            .setReturnAdsEnabled(false)
            .setCallback {
                initialized = true
                loadNext()
            }
            .init()
    }

    private fun loadNext() {
        if (!initialized || destroyed || loading || readyAd != null) return
        loading = true
        val next = StartAppAd(activity)
        next.loadAd(object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                loading = false
                if (!destroyed) readyAd = next
            }
            override fun onFailedToReceiveAd(ad: Ad?) {
                loading = false
                // No interruption or retry loop: next chance is another screen transition.
            }
        })
    }

    fun onLevelFinished(levelNumber: Int, victory: Boolean, stillOnResult: () -> Boolean) {
        cancelScheduled()
        if (!enabled || destroyed) return
        // A level result can trigger one preloaded ad (never mid-game).
        // If there is no ad available we skip, with no late surprise pop-up.
        val now = android.os.SystemClock.elapsedRealtime()
        if (!policy.recordResult(victory = victory, nowMs = now)) return
        val job = Runnable {
            pending = null
            if (!paused && !destroyed && stillOnResult()) {
                showIfReady()
            }
        }
        pending = job
        handler.postDelayed(job, EndOfLevelAdsConfig.RESULT_SCREEN_DELAY_MS)
    }

    private fun showIfReady() {
        val ad = readyAd ?: run { loadNext(); return }
        if (!ad.isReady) { readyAd = null; loadNext(); return }
        readyAd = null
        // Any ad not available or failing to display must never prevent NEXT.
        ad.showAd(object : AdDisplayListener {
            override fun adDisplayed(ad: Ad) {
                policy.adWasShown(android.os.SystemClock.elapsedRealtime())
            }
            override fun adHidden(ad: Ad) { loadNext() }
            override fun adClicked(ad: Ad) { }
            override fun adNotDisplayed(ad: Ad) { loadNext() }
        })
    }

    fun cancelScheduled() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }

    fun onPause() { paused = true; cancelScheduled() }
    fun onResume() { paused = false }
    fun release() {
        destroyed = true
        cancelScheduled()
        readyAd = null
    }
}
