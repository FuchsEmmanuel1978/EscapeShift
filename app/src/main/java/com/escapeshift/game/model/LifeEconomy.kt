package com.escapeshift.game

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences

/** Local Step-1 economy. Play Billing entitlements and AdMob rewards are wired in later steps. */
class LifeEconomy(context: Context) {
    // Ne jamais activer les vies infinies dans une version release, meme si DevConfig.ENABLED est reste a true.
    private val infiniteDebugLives = DevConfig.ENABLED && DevConfig.INFINITE_LIVES &&
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val prefs: SharedPreferences = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    private val keyLives = "economy_lives_v1"
    private val keyRegen = "economy_regen_at_v1"
    private val keySpent = "economy_coins_spent_v1"
    private val keyHeartIds = "economy_heart_ids_v1"
    private val entitlements = WorldEntitlements(context)
    private val rechargeMs = BizConfig.Lives.REGEN_MINUTES * 60_000L
    private var livesRaw = prefs.getInt(keyLives, BizConfig.Lives.INITIAL).coerceIn(0, BizConfig.Lives.MAX)
    private var anchor = prefs.getLong(keyRegen, System.currentTimeMillis())
    private var spent = prefs.getInt(keySpent, 0).coerceAtLeast(0)
    private val collectedHeartIds = prefs.getString(keyHeartIds, "")
        .orEmpty().split('|').filter { it.isNotBlank() }.toMutableSet()

    val lives: Int get() { if (infiniteDebugLives) return BizConfig.Lives.MAX; if (entitlements.isPremium) return BizConfig.Lives.MAX; refresh(); return livesRaw }
    fun availableCoins(totalCollectedUnique: Int): Int =
        (totalCollectedUnique.toLong() + prefs.getInt(ConsumableWallet.KEY_PAID_COINS, 0).coerceAtLeast(0) - spent)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    val paidHeartReserve: Int get() = prefs.getInt(ConsumableWallet.KEY_HEART_RESERVE, 0).coerceAtLeast(0)
    fun hasCollectedHeart(level: Int, id: String): Boolean = "${level}:${id}" in collectedHeartIds

    fun refresh(now: Long = System.currentTimeMillis()) {
        if (livesRaw >= BizConfig.Lives.MAX) { anchor = now; return }
        // Clock rollback does not earn natural lives, but prepaid heart credits can still be used.
        val gained = if (now < anchor) 0 else ((now - anchor) / rechargeMs).toInt().coerceAtLeast(0)
        if (gained > 0) {
            livesRaw = (livesRaw + gained).coerceAtMost(BizConfig.Lives.MAX)
            anchor = if (livesRaw == BizConfig.Lives.MAX) now else anchor + gained.toLong() * rechargeMs
            save()
        }
        usePaidHeartReserve(now)
    }

    /** Automatically apply prepaid heart credits only when room is available. No purchased heart is lost. */
    private fun usePaidHeartReserve(now: Long) {
        if (entitlements.isPremium || livesRaw >= BizConfig.Lives.MAX) return
        val reserve = paidHeartReserve
        val add = minOf(BizConfig.Lives.MAX - livesRaw, reserve)
        if (add <= 0) return
        val newLives = livesRaw + add
        val newAnchor = if (newLives == BizConfig.Lives.MAX) now else anchor
        val saved = prefs.edit().putInt(ConsumableWallet.KEY_HEART_RESERVE, reserve - add)
            .putInt(keyLives, newLives).putLong(keyRegen, newAnchor).commit()
        if (saved) { livesRaw = newLives; anchor = newAnchor }
    }

    fun millisToNextLife(now: Long = System.currentTimeMillis()): Long {
        if (infiniteDebugLives) return 0L
        refresh(now)
        if (livesRaw >= BizConfig.Lives.MAX) return 0
        return (rechargeMs - (now - anchor).coerceAtLeast(0)).coerceIn(0, rechargeMs)
    }

    fun loseLife(): Boolean {
        if (infiniteDebugLives) return true
        refresh()
        if (entitlements.isPremium) return true
        if (livesRaw <= 0) return false
        if (livesRaw == BizConfig.Lives.MAX) anchor = System.currentTimeMillis()
        livesRaw--
        save()
        return true
    }

    fun addLives(amount: Int): Boolean {
        if (infiniteDebugLives) return false
        require(amount > 0)
        refresh()
        if (livesRaw >= BizConfig.Lives.MAX) return false
        livesRaw = (livesRaw + amount).coerceAtMost(BizConfig.Lives.MAX)
        if (livesRaw == BizConfig.Lives.MAX) anchor = System.currentTimeMillis()
        save()
        return true
    }

    fun buyLife(totalCollectedUnique: Int): Boolean {
        if (infiniteDebugLives) return false
        refresh()
        if (entitlements.isPremium || livesRaw >= BizConfig.Lives.MAX || availableCoins(totalCollectedUnique) < BizConfig.Lives.COINS_PER_LIFE) return false
        spent += BizConfig.Lives.COINS_PER_LIFE
        livesRaw++
        if (livesRaw == BizConfig.Lives.MAX) anchor = System.currentTimeMillis()
        save()
        return true
    }

    /** Debit the existing coin wallet and grant an in-game item in one durable transaction.
     * No Play receipt, credited consumable or other purchase ledger is touched.
     */
    @Synchronized
    fun spendCoinsWith(totalCollectedUnique: Int, amount: Int,
                       write: (SharedPreferences.Editor) -> Unit): Boolean {
        if (amount < 0 || availableCoins(totalCollectedUnique) < amount) return false
        val nextSpent = spent.toLong() + amount.toLong()
        if (nextSpent > Int.MAX_VALUE) return false
        val editor = prefs.edit().putInt(keySpent, nextSpent.toInt())
        write(editor)
        if (!editor.commit()) return false
        spent = nextSpent.toInt()
        return true
    }

    /** A collected heart is claimed once permanently, also after death/restart. */
    fun claimHeart(level: Int, id: String): Boolean {
        if (infiniteDebugLives) return false
        refresh()
        val key = "${level}:${id}"
        if (entitlements.isPremium || livesRaw >= BizConfig.Lives.MAX || key in collectedHeartIds) return false
        collectedHeartIds.add(key)
        livesRaw = (livesRaw + BizConfig.Lives.HEART_PICKUP_LIVES).coerceAtMost(BizConfig.Lives.MAX)
        if (livesRaw == BizConfig.Lives.MAX) anchor = System.currentTimeMillis()
        prefs.edit().putString(keyHeartIds, collectedHeartIds.sorted().joinToString("|")).apply()
        save()
        return true
    }

    private fun save() {
        prefs.edit().putInt(keyLives, livesRaw).putLong(keyRegen, anchor)
            .putInt(keySpent, spent).apply()
    }
}
