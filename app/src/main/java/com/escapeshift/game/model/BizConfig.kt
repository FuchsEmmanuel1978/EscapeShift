package com.escapeshift.game

/** ESCAPE SHIFT — commercial settings. Change only here. */
object BizConfig {
    object Lives {
        const val MAX = 5
        const val INITIAL = 5
        const val REGEN_MINUTES = 10L
        const val COINS_PER_LIFE = 5
        const val COIN_PICKUP_VALUE = 1
        const val HEART_PICKUP_LIVES = 1
        const val REWARDED_LIVES = 3 // AdMob: step 2
    }
    object Progression {
        const val LEVELS_PER_WORLD = 10
        const val PLANNED_WORLDS = 10
        const val DEATHS_TO_OFFER_SKIP = 10
        const val ADS_FOR_SKIP = 1
    }
    object Ads {
        const val LIVES_PLACEMENT = "reward_lives"
        const val SKIP_PLACEMENT = "reward_skip"
        const val FORCED_INTERSTITIALS = false
        // RELEASE SAFETY GATE: production remains disabled until audience/privacy review.
        const val RELEASE_ADS_ENABLED = false
        // Add AdMob rewarded unit IDs only when preparing a reviewed production build.
        const val LIVE_LIVES_UNIT_ID = ""
        const val LIVE_SKIP_UNIT_ID = ""
        const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val ADS_SDK_VERSION = "24.0.0" // Version used in the tested Kotlin 2.0 build
    }
    object Billing {
        const val WORLD_PRICE_EUR_REFERENCE = 1.99
        const val PREMIUM_PRICE_EUR_REFERENCE = 9.99
        const val WORLD_SKU_PREFIX = "escape_world_" // escape_world_02..escape_world_10
        const val PREMIUM_SKU = "escape_premium_all"
        const val BILLING_LIBRARY_VERSION = "8.2.1"
        const val ENABLE_PURCHASES = true // Play test track and test license required
        /** FOR LICENSE TESTERS ONLY until server receipt verification and cloud wallet are added. */
        const val ENABLE_CONSUMABLES = true
        const val COIN_PACK_50 = "escapecoins50"
        const val COIN_PACK_120 = "escapecoins120"
        const val COIN_PACK_350 = "escapecoins350"
        const val HEART_PACK_3 = "escapehearts3"
        val CONSUMABLES: Map<String, Int> = mapOf(
            COIN_PACK_50 to 50,
            COIN_PACK_120 to 120,
            COIN_PACK_350 to 350,
            HEART_PACK_3 to 3
        )
        val CONSUMABLE_PRODUCTS: List<String> = CONSUMABLES.keys.toList()
    }
}
