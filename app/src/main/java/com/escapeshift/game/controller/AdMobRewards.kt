package com.escapeshift.game

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Only two voluntary Rewarded Ad placements. No interstitials and no automatic pop-ups. */
class AdMobRewards(private val activity: Activity) {
    private val isDebug = activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val enabled = isDebug || BizConfig.Ads.RELEASE_ADS_ENABLED
    private var initialized = false
    private var released = false
    private var showing = false
    private val cached = mutableMapOf<AdRewardKind, RewardedAd>()
    private val loading = mutableSetOf<AdRewardKind>()
    private var activeResult: ((Boolean) -> Unit)? = null

    /** Call only after UMP says it is legal to request ads. */
    fun initialize() {
        if (!enabled || released || initialized) return
        initialized = true
        // Call on UI thread; Google handles SDK initialization internally.
        MobileAds.initialize(activity) {
            activity.runOnUiThread {
                if (!released) {
                    load(AdRewardKind.LIVES)
                    load(AdRewardKind.SKIP_LEVEL)
                }
            }
        }
    }
    private fun unitId(kind: AdRewardKind): String =
        if (isDebug) BizConfig.Ads.TEST_REWARDED_UNIT_ID else when (kind) {
            AdRewardKind.LIVES -> BizConfig.Ads.LIVE_LIVES_UNIT_ID
            AdRewardKind.SKIP_LEVEL -> BizConfig.Ads.LIVE_SKIP_UNIT_ID
        }

    fun load(kind: AdRewardKind) {
        if (!initialized || released || cached.containsKey(kind) || kind in loading) return
        val unitId = unitId(kind)
        if (unitId.isBlank()) return
        loading.add(kind)
        RewardedAd.load(activity, unitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading.remove(kind)
                    if (!released) cached[kind] = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading.remove(kind)
                    Log.d("EscapeShiftAds", "Rewarded load failed: ${error.code}")
                }
            })
    }
    fun show(kind: AdRewardKind, onFinished: (Boolean) -> Unit) {
        if (released || !initialized || showing) { onFinished(false); return }
        val ad = cached.remove(kind)
        if (ad == null) { load(kind); onFinished(false); return }
        showing = true
        var earned = false
        var resolved = false
        fun finish() {
            if (resolved) return
            resolved = true
            showing = false
            onFinished(earned)
            load(kind)
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { finish() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { finish() }
        }
        try {
            ad.show(activity) { earned = true }
        } catch (_: Exception) { finish() }
    }
    fun release() {
        released = true
        cached.clear()
        loading.clear()
    }
}
