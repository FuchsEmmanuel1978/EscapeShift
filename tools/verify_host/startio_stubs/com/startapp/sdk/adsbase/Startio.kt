package com.startapp.sdk.adsbase
import android.app.Activity
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
open class Ad
class StartAppAd(activity: Activity) {
    val isReady: Boolean = false
    fun loadAd(listener: AdEventListener) {}
    fun showAd(listener: AdDisplayListener) {}
    companion object { fun disableSplash() {} }
}
object StartAppSDK {
    fun setTestAdsEnabled(value: Boolean) {}
    fun initParams(activity: Activity, id: String): Params = Params()
    class Params {
        fun setReturnAdsEnabled(v: Boolean): Params = this
        fun setCallback(fn: () -> Unit): Params = this
        fun init() {}
    }
}
