package com.escapeshift.game

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * TEST-TRACK ONLY fulfillment ledger for Play Billing consumables.
 * Important: SharedPreferences is not a secure receipt verifier, a cloud backup or an anti-fraud backend.
 * Production requires Play Developer API verification & a server-owned wallet before enabling real sales.
 * Purchases are credited BEFORE consumeAsync to make process crashes recoverable; token hashes prevent duplicates.
 */
class ConsumableWallet(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)

    enum class CreditResult { CREDITED, ALREADY_CREDITED, UNKNOWN_PRODUCT, WRITE_FAILED }

    @Synchronized
    fun creditOnce(productId: String, purchaseToken: String): CreditResult {
        val count = BizConfig.Billing.CONSUMABLES[productId] ?: return CreditResult.UNKNOWN_PRODUCT
        if (purchaseToken.isBlank()) return CreditResult.WRITE_FAILED
        val tokenId = sha256(purchaseToken)
        val seen = prefs.getStringSet(KEY_TOKENS, emptySet()).orEmpty()
        if (tokenId in seen) return CreditResult.ALREADY_CREDITED
        val edit = prefs.edit().putStringSet(KEY_TOKENS, seen.toMutableSet().apply { add(tokenId) })
        val current = if (productId == BizConfig.Billing.HEART_PACK_3)
            prefs.getInt(KEY_HEART_RESERVE, 0).coerceAtLeast(0)
        else prefs.getInt(KEY_PAID_COINS, 0).coerceAtLeast(0)
        val next = (current.toLong() + count.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (next == current) return CreditResult.WRITE_FAILED
        if (productId == BizConfig.Billing.HEART_PACK_3) edit.putInt(KEY_HEART_RESERVE, next)
        else edit.putInt(KEY_PAID_COINS, next)
        // commit(), not apply(): ledger and balance are stored in the same durable edit.
        return if (edit.commit()) CreditResult.CREDITED else CreditResult.WRITE_FAILED
    }

    companion object {
        const val KEY_PAID_COINS = "economy_paid_coins_v1"
        const val KEY_HEART_RESERVE = "economy_paid_heart_reserve_v1"
        const val KEY_TOKENS = "billing_consumed_token_hashes_v1"
        private fun sha256(raw: String): String = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
