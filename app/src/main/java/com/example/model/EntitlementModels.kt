package com.example.model

import com.example.core.BotanicalConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlanType(val displayName: String) {
    FREE("Free"),
    PREMIUM_MONTHLY("Premium Monthly"),
    PREMIUM_ANNUAL("Premium Annual"),
    LIFETIME_PREMIUM("Lifetime Premium");

    companion object {
        fun fromString(value: String?): PlanType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}

enum class SubscriptionStatus(val label: String) {
    FREE("Free Plan"),
    ACTIVE("Active"),
    CANCELLED("Cancelled (Active until period ends)"),
    EXPIRED("Expired"),
    PAST_DUE("Past Due / Payment Issue"),
    REFUNDED("Refunded"),
    REVOKED("Revoked");

    companion object {
        fun fromString(value: String?): SubscriptionStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}

enum class PurchaseProvider(val displayName: String) {
    NONE("None"),
    GOOGLE_PLAY("Google Play"),
    PAYSTACK("Paystack");

    companion object {
        fun fromString(value: String?): PurchaseProvider =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
}

@IgnoreExtraProperties
data class UserEntitlement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planType: String = PlanType.FREE.name,
    val freeIdentificationsUsed: Long = 0L,
    val freeIdentificationsLimit: Long = BotanicalConstants.FREE_IDENTIFICATION_LIMIT.toLong(),
    val subscriptionStatus: String = SubscriptionStatus.FREE.name,
    val expiresAt: Timestamp? = null,
    val purchaseProvider: String = PurchaseProvider.NONE.name,
    val purchaseReference: String = "",
    val productId: String = "",
    val willAutoRenew: Boolean = false,
    val hasLifetimeEntitlement: Boolean = false,
    val lastScanRequestId: String = "",
    val schemaVersion: Long = BotanicalConstants.SCHEMA_VERSION.toLong(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedPlanType(): PlanType = PlanType.fromString(planType)
    fun parsedStatus(): SubscriptionStatus = SubscriptionStatus.fromString(subscriptionStatus)
    fun parsedProvider(): PurchaseProvider = PurchaseProvider.fromString(purchaseProvider)

    fun effectivePlan(nowMillis: Long = System.currentTimeMillis()): PlanType {
        val status = parsedStatus()
        if (hasLifetimeEntitlement && status != SubscriptionStatus.REFUNDED && status != SubscriptionStatus.REVOKED) {
            return PlanType.LIFETIME_PREMIUM
        }
        return when (val plan = parsedPlanType()) {
            PlanType.FREE -> PlanType.FREE
            PlanType.LIFETIME_PREMIUM -> {
                if (status == SubscriptionStatus.REFUNDED || status == SubscriptionStatus.REVOKED) {
                    PlanType.FREE
                } else {
                    PlanType.LIFETIME_PREMIUM
                }
            }
            PlanType.PREMIUM_MONTHLY, PlanType.PREMIUM_ANNUAL -> {
                val validStatus = status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELLED
                val expiryMillis = expiresAt?.toDate()?.time
                val notExpired = expiryMillis == null || expiryMillis > nowMillis
                if (validStatus && notExpired) plan else PlanType.FREE
            }
        }
    }

    fun isPremiumActive(nowMillis: Long = System.currentTimeMillis()): Boolean =
        effectivePlan(nowMillis) != PlanType.FREE

    fun remainingFreeUses(): Int =
        (freeIdentificationsLimit - freeIdentificationsUsed)
            .toInt()
            .coerceIn(0, BotanicalConstants.FREE_IDENTIFICATION_LIMIT)

    fun canIdentify(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isPremiumActive(nowMillis) || remainingFreeUses() > 0

    fun formattedRemainingLabel(): String =
        "${remainingFreeUses()} of $freeIdentificationsLimit uses remaining"
}

class QuotaExhaustedException(
    message: String = "You have used all 10 free plant identifications. Upgrade to Premium to continue identifying plants."
) : IllegalStateException(message)

sealed interface PlayProductLoadState {
    data object Loading : PlayProductLoadState
    data class Ready(
        val monthlyFormattedPrice: String?,
        val annualFormattedPrice: String?,
        val lifetimeFormattedPrice: String?,
        val storeAvailable: Boolean,
        val statusNote: String? = null
    ) : PlayProductLoadState
}

sealed interface BillingFlowState {
    data object Idle : BillingFlowState
    data class Processing(val message: String) : BillingFlowState
    data class Success(val message: String) : BillingFlowState
    data class Pending(val message: String) : BillingFlowState
    data class Cancelled(val message: String) : BillingFlowState
    data class Error(val message: String) : BillingFlowState
}
