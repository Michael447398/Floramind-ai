package com.example.data.billing

import com.example.BuildConfig
import com.example.model.PlanType
import com.example.model.PurchaseProvider
import com.example.model.QuotaExhaustedException
import com.example.model.SubscriptionStatus
import com.example.model.UserEntitlement
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.Date
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class EntitlementDto(
    val userId: String = "",
    val planType: String = PlanType.FREE.name,
    val freeIdentificationsUsed: Long = 0L,
    val freeIdentificationsLimit: Long = 10L,
    val subscriptionStatus: String = SubscriptionStatus.FREE.name,
    val expiresAtMs: Long? = null,
    val purchaseProvider: String = PurchaseProvider.NONE.name,
    val purchaseReference: String = "",
    val productId: String = "",
    val willAutoRenew: Boolean = false,
    val hasLifetimeEntitlement: Boolean = false,
    val lastScanRequestId: String = ""
) {
    fun toDomain(): UserEntitlement = UserEntitlement(
        id = userId,
        userId = userId,
        planType = planType,
        freeIdentificationsUsed = freeIdentificationsUsed,
        freeIdentificationsLimit = freeIdentificationsLimit,
        subscriptionStatus = subscriptionStatus,
        expiresAt = expiresAtMs?.let { Timestamp(Date(it)) },
        purchaseProvider = purchaseProvider,
        purchaseReference = purchaseReference,
        productId = productId,
        willAutoRenew = willAutoRenew,
        hasLifetimeEntitlement = hasLifetimeEntitlement,
        lastScanRequestId = lastScanRequestId
    )
}

@JsonClass(generateAdapter = true)
data class ConsumeIdentificationRequest(
    val requestId: String
)

@JsonClass(generateAdapter = true)
data class VerifyPlayPurchaseRequest(
    val purchaseToken: String,
    val productId: String,
    val orderId: String? = null,
    val packageName: String
)

@JsonClass(generateAdapter = true)
data class RestorePlayPurchasesRequest(
    val packageName: String,
    val purchases: List<VerifyPlayPurchaseRequest>
)

@JsonClass(generateAdapter = true)
data class EntitlementResponse(
    val ok: Boolean = false,
    val entitlement: EntitlementDto? = null,
    val error: String? = null,
    val code: String? = null
)

interface EntitlementBackendRetrofitApi {
    @GET("api/entitlement")
    suspend fun getEntitlement(
        @Header("Authorization") bearerToken: String
    ): Response<EntitlementResponse>

    @POST("api/identifications/consume")
    suspend fun consumeIdentification(
        @Header("Authorization") bearerToken: String,
        @Body body: ConsumeIdentificationRequest
    ): Response<EntitlementResponse>

    @POST("api/payments/play/verify")
    suspend fun verifyPlayPurchase(
        @Header("Authorization") bearerToken: String,
        @Body body: VerifyPlayPurchaseRequest
    ): Response<EntitlementResponse>

    @POST("api/payments/play/restore")
    suspend fun restorePlayPurchases(
        @Header("Authorization") bearerToken: String,
        @Body body: RestorePlayPurchasesRequest
    ): Response<EntitlementResponse>
}

class EntitlementApiService(
    private val baseUrl: String = BuildConfig.ENTITLEMENT_BACKEND_URL.trim()
) {
    val isBackendConfigured: Boolean =
        baseUrl.isNotBlank() && (baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))

    private val api: EntitlementBackendRetrofitApi? by lazy {
        if (!isBackendConfigured) return@lazy null
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(normalizedBase)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EntitlementBackendRetrofitApi::class.java)
    }

    private suspend fun authHeader(): String {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("Please sign in with Google before verifying your account plan.")
        val token = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Unable to retrieve a verified Firebase ID token.")
        return "Bearer $token"
    }

    suspend fun fetchEntitlement(): Result<UserEntitlement> = withContext(Dispatchers.IO) {
        runCatching {
            val service = api
                ?: throw IllegalStateException("Entitlement verification backend URL is not configured yet.")
            val response = service.getEntitlement(authHeader())
            parseEntitlementResponse(response)
        }
    }

    suspend fun consumeIdentification(requestId: String): Result<UserEntitlement> = withContext(Dispatchers.IO) {
        runCatching {
            val service = api
                ?: throw IllegalStateException("Entitlement verification backend URL is not configured yet.")
            val response = service.consumeIdentification(authHeader(), ConsumeIdentificationRequest(requestId))
            if (response.code() == 402 || response.code() == 403) {
                val errBody = response.errorBody()?.string().orEmpty()
                if (errBody.contains("QUOTA_EXHAUSTED", ignoreCase = true) || response.code() == 402) {
                    throw QuotaExhaustedException()
                }
            }
            parseEntitlementResponse(response)
        }
    }

    suspend fun verifyPlayPurchase(
        purchaseToken: String,
        productId: String,
        orderId: String?,
        packageName: String
    ): Result<UserEntitlement> = withContext(Dispatchers.IO) {
        runCatching {
            val service = api
                ?: throw IllegalStateException(
                    "Backend entitlement server is required to verify Google Play purchases (ENTITLEMENT_BACKEND_URL is not configured)."
                )
            val response = service.verifyPlayPurchase(
                authHeader(),
                VerifyPlayPurchaseRequest(
                    purchaseToken = purchaseToken,
                    productId = productId,
                    orderId = orderId,
                    packageName = packageName
                )
            )
            parseEntitlementResponse(response)
        }
    }

    suspend fun restorePlayPurchases(
        packageName: String,
        purchases: List<VerifyPlayPurchaseRequest>
    ): Result<UserEntitlement> = withContext(Dispatchers.IO) {
        runCatching {
            val service = api
                ?: throw IllegalStateException(
                    "Backend entitlement server is required to restore Google Play purchases (ENTITLEMENT_BACKEND_URL is not configured)."
                )
            val response = service.restorePlayPurchases(
                authHeader(),
                RestorePlayPurchasesRequest(packageName = packageName, purchases = purchases)
            )
            parseEntitlementResponse(response)
        }
    }

    private fun parseEntitlementResponse(response: Response<EntitlementResponse>): UserEntitlement {
        if (!response.isSuccessful) {
            val rawError = response.errorBody()?.string().orEmpty()
            val clean = rawError.take(220).ifBlank { "Server returned HTTP ${response.code()}" }
            throw IllegalStateException(clean)
        }
        val body = response.body()
            ?: throw IllegalStateException("Entitlement server returned an empty response.")
        if (!body.ok || body.entitlement == null) {
            if (body.code.equals("QUOTA_EXHAUSTED", ignoreCase = true)) {
                throw QuotaExhaustedException(body.error ?: "Free identification quota exhausted.")
            }
            throw IllegalStateException(body.error ?: "Entitlement verification failed.")
        }
        return body.entitlement.toDomain()
    }
}
