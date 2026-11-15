package com.escapeshift.game

/** Plain Kotlin shop contract. No dependency on Canvas or BillingClient. */
enum class ShopTab { LIVES, COINS, WORLDS, PREMIUM }

data class ShopWorldOffer(
    val world: Int,
    val developed: Boolean,
    val accessible: Boolean,
    val formattedPrice: String?
)

data class ShopCoinPack(val productId: String, val amount: Int, val formattedPrice: String?)

data class ShopState(
    val coins: Int,
    val lives: Int,
    val maxLives: Int,
    val premium: Boolean,
    val rewardedAdPending: Boolean,
    val worlds: List<ShopWorldOffer>,
    val premiumPrice: String?,
    val coinsForSale: List<ShopCoinPack> = emptyList(),
    val paidHeartsPrice: String? = null,
    val paidHeartReserve: Int = 0
) {
    fun world(number: Int): ShopWorldOffer? = worlds.firstOrNull { it.world == number }
    fun canExchangeLife(cost: Int = BizConfig.Lives.COINS_PER_LIFE): Boolean =
        !premium && lives < maxLives && coins >= cost
    fun canWatchAd(): Boolean = !premium && lives < maxLives && !rewardedAdPending
    fun canBuyWorld(number: Int): Boolean =
        !premium && world(number)?.let { it.developed && !it.accessible && it.formattedPrice != null } == true
    fun canBuyPremium(): Boolean = !premium && premiumPrice != null
    fun coinPack(id: String): ShopCoinPack? = coinsForSale.firstOrNull { it.productId == id }
    fun canBuyCoins(id: String): Boolean = !premium && coinPack(id)?.formattedPrice != null
    fun canBuyPaidHearts(): Boolean = !premium && lives < maxLives && paidHeartsPrice != null
}

sealed class ShopCommand {
    object Close : ShopCommand()
    object BuyLifeWithCoins : ShopCommand()
    object WatchRewardedLives : ShopCommand()
    data class BuyWorld(val number: Int) : ShopCommand()
    object BuyPremium : ShopCommand()
    data class BuyCoins(val productId: String) : ShopCommand()
    object BuyPaidHearts : ShopCommand()
    object RestorePurchases : ShopCommand()
}
