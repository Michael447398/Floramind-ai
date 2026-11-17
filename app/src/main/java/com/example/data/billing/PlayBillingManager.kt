package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.core.BotanicalConstants
import com.example.model.BillingFlowState
import com.example.model.PlanType
import com.example.model.PlayProductLoadState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayBillingManager(
    context: Context,
    private val entitlementApi: EntitlementApiService,
    private val onEntitlementUpdated: () -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _productLoadState = MutableStateFlow<PlayProductLoadState>(PlayProductLoadState.Loading)
    val productLoadState: StateFlow<PlayProductLoadState> = _productLoadState.asStateFlow()

    private val _billingFlowState = MutableStateFlow<BillingFlowState>(BillingFlowState.Idle)
    val billingFlowState: StateFlow<BillingFlowState> = _billingFlowState.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdate(billingResult, purchases)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun startConnectionAndLoadProducts() {
        if (billingClient.isReady) {
            scope.launch { queryCatalogProducts() }
            return
        }
        _productLoadState.value = PlayProductLoadState.Loading
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryCatalogProducts()
                    }
                } else {
                    Log.w(TAG, "Billing setup unavailable: ${result.responseCode} ${result.debugMessage}")
                    _productLoadState.value = PlayProductLoadState.Ready(
                        monthlyFormattedPrice = null,
                        annualFormattedPrice = null,
                        lifetimeFormattedPrice = null,
                        storeAvailable = false,
                        statusNote = "Google Play Billing is unavailable on this device (${result.debugMessage.ifBlank { "Play Store required" }})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) {
                        cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    suspend fun queryCatalogProducts() {
        val subsList = queryProductsForType(
            BillingClient.ProductType.SUBS,
            listOf(BotanicalConstants.PRODUCT_ID_MONTHLY, BotanicalConstants.PRODUCT_ID_ANNUAL)
        )
        val inAppList = queryProductsForType(
            BillingClient.ProductType.INAPP,
            listOf(BotanicalConstants.PRODUCT_ID_LIFETIME)
        )

        (subsList + inAppList).forEach { details ->
            productDetailsMap[details.productId] = details
        }

        val monthlyPrice = productDetailsMap[BotanicalConstants.PRODUCT_ID_MONTHLY]
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice

        val annualPrice = productDetailsMap[BotanicalConstants.PRODUCT_ID_ANNUAL]
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice

        val lifetimePrice = productDetailsMap[BotanicalConstants.PRODUCT_ID_LIFETIME]
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice

        val anyFound = monthlyPrice != null || annualPrice != null || lifetimePrice != null
        val backendNote = if (!entitlementApi.isBackendConfigured) {
            "Backend verification URL (ENTITLEMENT_BACKEND_URL) is not yet configured; purchases require server verification."
        } else null

        val note = when {
            !anyFound -> "Store products (${BotanicalConstants.PRODUCT_ID_MONTHLY}, ${BotanicalConstants.PRODUCT_ID_ANNUAL}, ${BotanicalConstants.PRODUCT_ID_LIFETIME}) are not published yet in Google Play Console."
            backendNote != null -> backendNote
            else -> null
        }

        _productLoadState.value = PlayProductLoadState.Ready(
            monthlyFormattedPrice = monthlyPrice,
            annualFormattedPrice = annualPrice,
            lifetimeFormattedPrice = lifetimePrice,
            storeAvailable = anyFound,
            statusNote = note
        )
    }

    private suspend fun queryProductsForType(
        productType: String,
        productIds: List<String>
    ): List<ProductDetails> = suspendCancellableCoroutine { cont ->
        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (!cont.isActive) return@queryProductDetailsAsync
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                cont.resume(queryResult.productDetailsList)
            } else {
                Log.w(TAG, "Product query failed ($productType): ${billingResult.debugMessage}")
                cont.resume(emptyList())
            }
        }
    }

    fun launchPurchase(activity: Activity, planType: PlanType) {
        val currentUid = Firebase.auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _billingFlowState.value = BillingFlowState.Error("Please sign in with Google before purchasing a plan.")
            return
        }

        val productId = when (planType) {
            PlanType.PREMIUM_MONTHLY -> BotanicalConstants.PRODUCT_ID_MONTHLY
            PlanType.PREMIUM_ANNUAL -> BotanicalConstants.PRODUCT_ID_ANNUAL
            PlanType.LIFETIME_PREMIUM -> BotanicalConstants.PRODUCT_ID_LIFETIME
            PlanType.FREE -> return
        }

        scope.launch {
            if (!ensureConnected()) {
                _billingFlowState.value = BillingFlowState.Error(
                    "Google Play Billing service is not available on this device."
                )
                return@launch
            }
            if (!productDetailsMap.containsKey(productId)) {
                queryCatalogProducts()
            }
            val details = productDetailsMap[productId]
            if (details == null) {
                _billingFlowState.value = BillingFlowState.Error(
                    "Product '$productId' is not configured or published in Google Play Console yet."
                )
                return@launch
            }

            val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)

            if (planType != PlanType.LIFETIME_PREMIUM) {
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken.isNullOrBlank()) {
                    _billingFlowState.value = BillingFlowState.Error(
                        "No active subscription offer found in Google Play for $productId."
                    )
                    return@launch
                }
                productParamsBuilder.setOfferToken(offerToken)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
                .setObfuscatedAccountId(currentUid)
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _billingFlowState.value = BillingFlowState.Error(
                    "Unable to launch Google Play checkout: ${result.debugMessage.ifBlank { "Error code ${result.responseCode}" }}"
                )
            }
        }
    }

    private fun handlePurchasesUpdate(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) return
                scope.launch {
                    verifyAndSyncPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingFlowState.value = BillingFlowState.Cancelled(
                    "Checkout was cancelled. Your account plan has not changed."
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                restorePurchases()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                _billingFlowState.value = BillingFlowState.Error(
                    "Google Play billing is temporarily unavailable (${billingResult.debugMessage})."
                )
            }
            else -> {
                _billingFlowState.value = BillingFlowState.Error(
                    billingResult.debugMessage.ifBlank { "Purchase could not be completed (code ${billingResult.responseCode})." }
                )
            }
        }
    }

    private suspend fun verifyAndSyncPurchases(purchases: List<Purchase>) {
        var hasPending = false
        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> {
                    hasPending = true
                }
                Purchase.PurchaseState.PURCHASED -> {
                    _billingFlowState.value = BillingFlowState.Processing(
                        "Verifying your Google Play purchase securely on the server..."
                    )
                    val productId = purchase.products.firstOrNull() ?: continue
                    val verifyResult = entitlementApi.verifyPlayPurchase(
                        purchaseToken = purchase.purchaseToken,
                        productId = productId,
                        orderId = purchase.orderId,
                        packageName = appContext.packageName
                    )
                    verifyResult.onSuccess { updated ->
                        onEntitlementUpdated()
                        _billingFlowState.value = BillingFlowState.Success(
                            "${updated.parsedPlanType().displayName} is now active on your account."
                        )
                    }.onFailure { err ->
                        _billingFlowState.value = BillingFlowState.Error(
                            err.localizedMessage ?: "Purchase completed in Play, awaiting server verification."
                        )
                    }
                    return
                }
                else -> Unit
            }
        }
        if (hasPending) {
            _billingFlowState.value = BillingFlowState.Pending(
                "Payment is pending confirmation from Google Play. Premium features unlock automatically once confirmed."
            )
        }
    }

    fun restorePurchases() {
        val currentUid = Firebase.auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _billingFlowState.value = BillingFlowState.Error("Sign in with Google first to restore purchases.")
            return
        }

        scope.launch {
            _billingFlowState.value = BillingFlowState.Processing("Checking Google Play purchase history...")
            if (!ensureConnected()) {
                _billingFlowState.value = BillingFlowState.Error("Google Play Billing is unavailable.")
                return@launch
            }
            val subs = queryOwnedPurchases(BillingClient.ProductType.SUBS)
            val inApp = queryOwnedPurchases(BillingClient.ProductType.INAPP)
            val allPurchased = (subs + inApp).filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (allPurchased.isEmpty()) {
                // Also ask backend in case user purchased on web via Paystack on the same UID
                if (entitlementApi.isBackendConfigured) {
                    entitlementApi.fetchEntitlement()
                        .onSuccess { remote ->
                            onEntitlementUpdated()
                            if (remote.isPremiumActive()) {
                                _billingFlowState.value = BillingFlowState.Success(
                                    "Synced ${remote.parsedPlanType().displayName} from your FloraMind account."
                                )
                            } else {
                                _billingFlowState.value = BillingFlowState.Cancelled(
                                    "No active Google Play or web purchases were found for this account."
                                )
                            }
                        }
                        .onFailure {
                            _billingFlowState.value = BillingFlowState.Cancelled(
                                "No active Google Play purchases were found on this device."
                            )
                        }
                } else {
                    _billingFlowState.value = BillingFlowState.Cancelled(
                        "No active Google Play purchases were found on this device."
                    )
                }
                return@launch
            }

            val payload = allPurchased.mapNotNull { p ->
                val prodId = p.products.firstOrNull() ?: return@mapNotNull null
                VerifyPlayPurchaseRequest(
                    purchaseToken = p.purchaseToken,
                    productId = prodId,
                    orderId = p.orderId,
                    packageName = appContext.packageName
                )
            }
            entitlementApi.restorePlayPurchases(appContext.packageName, payload)
                .onSuccess { updated ->
                    onEntitlementUpdated()
                    _billingFlowState.value = if (updated.isPremiumActive()) {
                        BillingFlowState.Success("Restored ${updated.parsedPlanType().displayName} to your account.")
                    } else {
                        BillingFlowState.Cancelled("No active premium entitlement found among prior purchases.")
                    }
                }
                .onFailure { err ->
                    _billingFlowState.value = BillingFlowState.Error(
                        err.localizedMessage ?: "Unable to restore purchases right now."
                    )
                }
        }
    }

    private suspend fun queryOwnedPurchases(productType: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (!cont.isActive) return@queryPurchasesAsync
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases)
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    fun clearBillingState() {
        _billingFlowState.value = BillingFlowState.Idle
    }

    fun release() {
        billingClient.endConnection()
        scope.cancel()
    }

    private companion object {
        const val TAG = "PlayBillingMgr"
    }
}
