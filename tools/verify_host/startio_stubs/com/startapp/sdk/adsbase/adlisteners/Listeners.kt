package com.startapp.sdk.adsbase.adlisteners
import com.startapp.sdk.adsbase.Ad
interface AdEventListener {
    fun onReceiveAd(ad: Ad)
    fun onFailedToReceiveAd(ad: Ad?)
}
interface AdDisplayListener {
    fun adDisplayed(ad: Ad)
    fun adHidden(ad: Ad)
    fun adClicked(ad: Ad)
    fun adNotDisplayed(ad: Ad)
}
