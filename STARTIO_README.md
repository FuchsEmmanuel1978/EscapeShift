# ESCAPE SHIFT — V17 Start.io INTERSTITIAL TEST

## Current status
- Test interstitials on Android debug builds (official demo ID `205489527`).
- Production advertising **OFF by design** until the Google Play audience/privacy/consent review is complete.
- This project **does not include your own Start.io App ID**. Add it to `StartioConfig.kt` when ready.
- Start.io SDK 5.2.4 (com.startapp:inapp-sdk) from Maven Central; needs network on first Gradle sync.

## Where and when
- Only after **victory**, 1.4 s after the victory screen appears (not on start, no mid-play interruption).
- No ad after final level 10, because there is no playable next level.
- Requires preloaded ready ad; otherwise it is skipped, with normal NEXT navigation.
- Capped at one every 120 seconds even if the user finishes levels faster.
- Music/audio pause when the app goes into background; SDK presentation controls its own overlay.
- No location or Bluetooth permissions requested by Escape Shift.

## The required steps BEFORE turning live ads on
1. In Google Play Console > App content > Target audience, check your declared age groups.
2. **Start.io is not Google Play Families Self-Certified**. Never show Start.io ads to children (including mixed audiences without compliant age screening). If children are included, use a certified SDK instead or leave ads off.
3. Update Google Play's declaration of ads to YES and redo Data Safety based on Start.io SDK disclosures.
4. Update the in-app and publicly hosted privacy policy to describe advertising and SDK data processing.
5. Implement and test the GDPR consent choice where required; do not claim consent automatically. Start.io has its own consent handling but it must be reviewed.
6. Add your actual Start.io APP_ID and explicitly switch the three release readiness flags + RELEASE_ADS_ENABLED, only after verification.
7. Generate and sign a **new** AAB with a versionCode greater than the last submitted to Google Play. Never edit or reuse the existing signed AAB.

## Tests
The supplied tests cover the ad gate, cooldown, no ad after final world-1 level and gameplay regression tests. 
Real Start.io ad display and Google SDK policy compliance require Android Studio and physical-device tests.

References:
- https://github.com/StartApp-SDK/android-sdk-demo
- https://support.google.com/googleplay/android-developer/answer/12955712
- https://support.google.com/googleplay/android-developer/answer/9857753
