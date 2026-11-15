package com.escapeshift.game

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Consent refreshed once per app launch. Ads load only when UMP canRequestAds(). */
class ConsentManager(private val activity: Activity) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    fun request(onResult: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                onResult(info.canRequestAds())
            }
        }, {
            // Prior consent may remain valid even if the refresh failed.
            onResult(info.canRequestAds())
        })
    }
    fun privacyOptionsRequired(): Boolean =
        info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun openPrivacyOptions(onDone: () -> Unit = {}) {
        if (!privacyOptionsRequired()) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }
}
