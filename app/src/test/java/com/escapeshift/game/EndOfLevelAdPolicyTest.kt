package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class EndOfLevelAdPolicyTest {
    @Test fun victoryAndDefeatAreEligible() {
        val policy = EndOfLevelAdPolicy(minIntervalMs = 0)
        assertTrue(policy.recordResult(victory = true, nowMs = 100))
        policy.adWasShown(100)
        assertTrue(policy.recordResult(victory = false, nowMs = 101))
    }

    @Test fun rapidDefeatsAreCappedInProduction() {
        val policy = EndOfLevelAdPolicy(minIntervalMs = 120_000)
        assertTrue(policy.recordResult(false, 100))
        policy.adWasShown(100)
        assertFalse(policy.recordResult(false, 119_999))
        assertTrue(policy.recordResult(false, 120_100))
    }

    @Test fun debugAllowsEachResultIfAnAdIsReady() {
        val policy = EndOfLevelAdPolicy(minIntervalMs = 0)
        for (i in 0..4) {
            assertTrue(policy.recordResult(i % 2 == 0, i.toLong()))
            policy.adWasShown(i.toLong())
        }
    }

    @Test fun resultsCanBeGroupedForFutureExperiment() {
        val policy = EndOfLevelAdPolicy(everyNResults = 2, minIntervalMs = 0)
        assertFalse(policy.recordResult(true, 10))
        assertTrue(policy.recordResult(false, 20))
        policy.adWasShown(20)
        assertFalse(policy.recordResult(false, 30))
        assertTrue(policy.recordResult(true, 40))
    }
}
