package com.escapeshift.game

import android.content.Context

/** Cached Play entitlements. The Billing bridge refreshes these from Google Play. */
class WorldEntitlements(context: Context) {
    private val prefs = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    val isPremium: Boolean get() = prefs.getBoolean(KEY_PREMIUM, false)
    fun hasWorld(world: Int): Boolean = world == 1 || isPremium || prefs.getBoolean("billing_world_$world", false)
    fun isWorldPurchased(world: Int): Boolean = prefs.getBoolean("billing_world_$world", false)
    fun progress(world: Int): Int = prefs.getInt("owned_world_progress_$world", 1).coerceIn(1, 10)
    /** May only be invoked after a purchase has been observed by Billing or test restoration. */
    fun updateFromPlay(productIds: Set<String>) {
        val edit = prefs.edit().putBoolean(KEY_PREMIUM, BizConfig.Billing.PREMIUM_SKU in productIds)
        for (world in 2..BizConfig.Progression.PLANNED_WORLDS)
            edit.putBoolean("billing_world_$world", productId(world) in productIds)
        edit.apply()
    }
    /** Progression for a purchased world does not unlock previous unplayed worlds. */
    fun completeLevel(level: Int) {
        val world = (level - 1) / 10 + 1
        if (hasWorld(world)) {
            val nextPosition = (level - 1) % 10 + 2
            prefs.edit().putInt("owned_world_progress_$world", maxOf(progress(world), nextPosition.coerceAtMost(10))).apply()
        }
        // A complete purchased world also opens the next world through normal gameplay.
        if (level % 10 == 0 && world < BizConfig.Progression.PLANNED_WORLDS)
            prefs.edit().putBoolean("earned_world_${world + 1}", true).apply()
    }
    fun earnedWorld(world: Int): Boolean = prefs.getBoolean("earned_world_$world", false)
    fun canPlay(level: Int, globalUnlocked: Int): Boolean {
        if (level !in 1..GameProgression.MAX_LEVEL) return false
        val world = (level - 1) / 10 + 1
        val position = (level - 1) % 10 + 1
        if (isPremium) return true
        if (GameProgression.isPlayable(level, globalUnlocked)) return true
        if (earnedWorld(world) && position <= progress(world)) return true
        return hasWorld(world) && position <= progress(world)
    }
    companion object {
        private const val KEY_PREMIUM = "billing_premium_v1"
        fun productId(world: Int) = BizConfig.Billing.WORLD_SKU_PREFIX + world.toString().padStart(2, '0')
        fun allProducts(): List<String> = (2..BizConfig.Progression.PLANNED_WORLDS).map(::productId) + BizConfig.Billing.PREMIUM_SKU
    }
}
