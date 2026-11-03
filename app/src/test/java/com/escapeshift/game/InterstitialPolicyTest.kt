package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class InterstitialPolicyTest {
    @Test fun noAdAtFinalLevel() {
        val policy = InterstitialPolicy()
        assertFalse(policy.recordVictory(false, 0))
    }
    @Test fun firstVictoryIsEligible() {
        val policy = InterstitialPolicy()
        assertTrue(policy.recordVictory(true, 0))
    }
    @Test fun cooldownPreventsBackToBackAds() {
        val policy = InterstitialPolicy(minIntervalMs = 120_000)
        assertTrue(policy.recordVictory(true, 100))
        policy.adWasShown(100)
        assertFalse(policy.recordVictory(true, 119_999))
        assertTrue(policy.recordVictory(true, 120_100))
    }
    @Test fun noLiveAdsWithoutAllSafetyGates() {
        assertTrue(StartioConfig.canRun(true))
        assertFalse(StartioConfig.canRun(false))
    }
}
