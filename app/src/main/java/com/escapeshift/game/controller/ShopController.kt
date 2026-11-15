package com.escapeshift.game

/** Hit areas match ShopView Canvas coordinates in its fixed 1000x560 design space. */
class ShopController {
    var tab: ShopTab = ShopTab.LIVES
        private set

    fun open(initial: ShopTab = ShopTab.LIVES) { tab = initial }

    /** Tap processing is pure Kotlin; nothing is purchased by merely switching tabs. */
    fun tap(x: Float, y: Float, state: ShopState): ShopCommand? {
        if (x in 30f..160f && y in 22f..91f) return ShopCommand.Close
        if (y in 105f..159f) {
            tab = when (x) {
                in 45f..270f -> ShopTab.LIVES
                in 276f..500f -> ShopTab.COINS
                in 506f..733f -> ShopTab.WORLDS
                in 739f..965f -> ShopTab.PREMIUM
                else -> tab
            }
            return null
        }
        if (x in 765f..963f && y in 520f..555f) return ShopCommand.RestorePurchases
        return when (tab) {
            ShopTab.LIVES -> when {
                x in 77f..319f && y in 400f..475f && state.canExchangeLife() -> ShopCommand.BuyLifeWithCoins
                x in 379f..621f && y in 400f..475f && state.canWatchAd() -> ShopCommand.WatchRewardedLives
                x in 680f..920f && y in 400f..475f && state.canBuyPaidHearts() -> ShopCommand.BuyPaidHearts
                else -> null
            }
            ShopTab.COINS -> when {
                x in 95f..312f && y in 410f..479f && state.canBuyCoins(BizConfig.Billing.COIN_PACK_50) -> ShopCommand.BuyCoins(BizConfig.Billing.COIN_PACK_50)
                x in 391f..609f && y in 410f..479f && state.canBuyCoins(BizConfig.Billing.COIN_PACK_120) -> ShopCommand.BuyCoins(BizConfig.Billing.COIN_PACK_120)
                x in 687f..905f && y in 410f..479f && state.canBuyCoins(BizConfig.Billing.COIN_PACK_350) -> ShopCommand.BuyCoins(BizConfig.Billing.COIN_PACK_350)
                else -> null
            }
            ShopTab.WORLDS -> when {
                x in 106f..449f && y in 381f..471f && state.canBuyWorld(2) -> ShopCommand.BuyWorld(2)
                x in 555f..898f && y in 381f..471f && state.canBuyWorld(3) -> ShopCommand.BuyWorld(3)
                else -> null
            }
            ShopTab.PREMIUM -> when {
                x in 632f..905f && y in 367f..461f && state.canBuyPremium() -> ShopCommand.BuyPremium
                else -> null
            }
        }
    }
}
