package com.escapeshift.game

import android.app.Activity
import com.android.billingclient.api.*

/**
 * Google Play non-consumable purchases. Test ONLY through a Play internal test track.
 * Production must verify purchase tokens with a backend/Google Play Developer API;
 * client-only validation is NOT a secure production entitlement check.
 */
class PlayBillingManager(
    private val activity: Activity,
    private val onChanged: () -> Unit,
    private val onMessage: (String) -> Unit
) : PurchasesUpdatedListener {
    private val repository = WorldEntitlements(activity)
    private val wallet = ConsumableWallet(activity)
    private val consumingTokens = mutableSetOf<String>()
    private val details = mutableMapOf<String, ProductDetails>()
    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()
    private var connecting = false
    private var closed = false

    fun connect() {
        if (closed || connecting) return
        if (client.isReady) { refresh(); return }
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else onMessage("Google Play indisponible (${result.responseCode})")
            }
            override fun onBillingServiceDisconnected() { connecting = false }
        })
    }

    fun formattedPrice(productId: String): String? {
        val offers = details[productId]?.oneTimePurchaseOfferDetailsList
        return offers?.firstOrNull()?.formattedPrice
    }

    fun buy(productId: String) {
        if (!BizConfig.Billing.ENABLE_PURCHASES) { onMessage("Achats desactives"); return }
        if (!client.isReady) { onMessage("Connexion Play en cours"); connect(); return }
        val item = details[productId]
        if (item == null) { onMessage("Produit indisponible dans Play Console"); refreshProducts(); return }
        val offer = item.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (offer == null) { onMessage("Aucune offre disponible"); return }
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(item)
        offer.offerToken?.let { paramsBuilder.setOfferToken(it) }
        val productParams = paramsBuilder.build()
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams)).build())
    }

    fun refresh() {
        if (!client.isReady) { connect(); return }
        refreshProducts()
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) reconcile(purchases)
            else onMessage("Restauration impossible (${result.responseCode})")
        }
    }

    private fun refreshProducts() {
        val products = (WorldEntitlements.allProducts() +
            if (BizConfig.Billing.ENABLE_CONSUMABLES) BizConfig.Billing.CONSUMABLE_PRODUCTS else emptyList()).map { id ->
            QueryProductDetailsParams.Product.newBuilder().setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP).build()
        }
        val request = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(request) { result, found ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details.clear()
                found.productDetailsList.forEach { details[it.productId] = it }
                onChanged()
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases?.any { it.purchaseState == Purchase.PurchaseState.PENDING } == true)
                    onMessage("Paiement en attente : aucun droit accorde")
                refresh() // Full inventory: never erase other purchases based on an incremental callback.
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Achat annule")
            else -> onMessage("Achat non finalise (${result.responseCode})")
        }
    }

    private fun reconcile(purchases: List<Purchase>) {
        val owned = mutableSetOf<String>()
        var pending = false
        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    val consumable = purchase.products.firstOrNull { it in BizConfig.Billing.CONSUMABLE_PRODUCTS }
                    if (consumable != null) {
                        processConsumable(purchase, consumable)
                    } else {
                        owned.addAll(purchase.products)
                        if (!purchase.isAcknowledged) acknowledge(purchase)
                    }
                }
                Purchase.PurchaseState.PENDING -> pending = true
            }
        }
        // Do not discard cached rights until Play has returned a successful query.
        repository.updateFromPlay(owned)
        onChanged()
        if (pending) onMessage("Paiement en attente : aucun droit accorde")
    }

    /** A consumable must be consumed, not acknowledged as a permanent entitlement. */
    private fun processConsumable(purchase: Purchase, sku: String) {
        if (!BizConfig.Billing.ENABLE_CONSUMABLES) return
        val token = purchase.purchaseToken
        if (!consumingTokens.add(token)) return
        // Dev/test-only local fulfillment. Production must verify this purchase with Google's API first.
        val status = wallet.creditOnce(sku, token)
        when (status) {
            ConsumableWallet.CreditResult.UNKNOWN_PRODUCT,
            ConsumableWallet.CreditResult.WRITE_FAILED -> {
                consumingTokens.remove(token)
                onMessage("Récompense non enregistrée : achat à réessayer")
                return
            }
            ConsumableWallet.CreditResult.CREDITED -> {
                onMessage(if (sku == BizConfig.Billing.HEART_PACK_3)
                    "3 cœurs ajoutés (excédent conservé)" else "Pièces ajoutées au portefeuille")
                onChanged()
            }
            ConsumableWallet.CreditResult.ALREADY_CREDITED -> Unit
        }
        val request = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        client.consumeAsync(request) { result, _ ->
            consumingTokens.remove(token)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onMessage("Achat reçu, validation Google à réessayer")
            } else onChanged()
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val request = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(request) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK)
                onMessage("Achat recu, confirmation Google en attente")
        }
    }

    fun close() { closed = true; if (client.isReady) client.endConnection() }
}
