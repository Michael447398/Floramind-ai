package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.BotanicalConstants
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.billing.EntitlementApiService
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.BotanicalReport
import com.example.model.CuratedPlant
import com.example.model.FeedbackReport
import com.example.model.PlanType
import com.example.model.PlantFolder
import com.example.model.PlantScanRecord
import com.example.model.PurchaseProvider
import com.example.model.QuotaExhaustedException
import com.example.model.SavedPlantRecord
import com.example.model.SubscriptionStatus
import com.example.model.UsageLog
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class FloraRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- User Profile & Quota ---

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<UserProfile> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (snap.exists()) {
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing != null) {
                val syncedUpdates = mutableMapOf<String, Any>()
                val cleanPhoto = photoUrl?.take(500)
                val cleanEmail = email.take(160)
                if (!cleanPhoto.isNullOrBlank() && cleanPhoto != existing.photoUrl) {
                    syncedUpdates["photoUrl"] = cleanPhoto
                }
                if (cleanEmail.isNotBlank() && cleanEmail != existing.email) {
                    syncedUpdates["email"] = cleanEmail
                }
                if (syncedUpdates.isNotEmpty()) {
                    syncedUpdates["updatedAt"] = FieldValue.serverTimestamp()
                    try {
                        docRef.update(syncedUpdates).await()
                        val refreshed = docRef.get().await()
                        return@runCatching refreshed.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: existing
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                    }
                }
                return@runCatching existing
            }
        }

        val today = todayUtcString()
        val cleanName = displayName.ifBlank { "Botanical Explorer" }.take(100)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to cleanName,
            "email" to email.take(160),
            "region" to "Global",
            "darkMode" to false,
            "retainThumbnails" to true,
            "advancedBotanyMode" to false,
            "dailyScanCount" to 0L,
            "dailyScanDate" to today,
            "totalScans" to 0L,
            "schemaVersion" to BotanicalConstants.SCHEMA_VERSION.toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank()) {
            payload["photoUrl"] = photoUrl.take(500)
        }

        try {
            docRef.set(payload).await()
            val refreshed = docRef.get().await()
            refreshed.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = cleanName, email = email)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserProfilePreferences(
        uid: String,
        displayName: String,
        region: String,
        darkMode: Boolean,
        retainThumbnails: Boolean,
        advancedBotanyMode: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "displayName" to displayName.trim().ifBlank { "Botanical Explorer" }.take(100),
            "region" to region.trim().ifBlank { "Global" }.take(100),
            "darkMode" to darkMode,
            "retainThumbnails" to retainThumbnails,
            "advancedBotanyMode" to advancedBotanyMode,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementDailyScanCount(uid: String, currentProfile: UserProfile?): Result<Unit> = runCatching {
        requireUserId()
        val today = todayUtcString()
        val docRef = db.collection("users").document(uid)
        val newDailyCount = if (currentProfile?.dailyScanDate == today) {
            (currentProfile.dailyScanCount + 1L)
        } else {
            1L
        }
        val newTotal = (currentProfile?.totalScans ?: 0L) + 1L
        try {
            docRef.update(
                mapOf(
                    "dailyScanDate" to today,
                    "dailyScanCount" to newDailyCount,
                    "totalScans" to newTotal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Freemium Entitlement & Identification Quota ---

    suspend fun ensureUserEntitlement(
        uid: String,
        apiService: EntitlementApiService? = null
    ): Result<UserEntitlement> = runCatching {
        requireUserId()
        if (apiService?.isBackendConfigured == true) {
            apiService.fetchEntitlement().onSuccess { return@runCatching it }
        }

        val docRef = db.collection("entitlements").document(uid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (snap.exists()) {
            val existing = snap.toObject(UserEntitlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing != null) return@runCatching existing
        }

        val payload = mapOf(
            "userId" to uid,
            "planType" to PlanType.FREE.name,
            "freeIdentificationsUsed" to 0L,
            "freeIdentificationsLimit" to BotanicalConstants.FREE_IDENTIFICATION_LIMIT.toLong(),
            "subscriptionStatus" to SubscriptionStatus.FREE.name,
            "purchaseProvider" to PurchaseProvider.NONE.name,
            "purchaseReference" to "",
            "productId" to "",
            "willAutoRenew" to false,
            "hasLifetimeEntitlement" to false,
            "lastScanRequestId" to "",
            "schemaVersion" to BotanicalConstants.SCHEMA_VERSION.toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            val refreshed = docRef.get().await()
            refreshed.toObject(UserEntitlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserEntitlement(id = uid, userId = uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeUserEntitlement(uid: String): Flow<UserEntitlement> = flow {
        val docRef = db.collection("entitlements").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(UserEntitlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserEntitlement(id = uid, userId = uid)
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                    }
                    emit(UserEntitlement(id = uid, userId = uid))
                }
        )
    }

    suspend fun consumeIdentificationUseAtomic(
        uid: String,
        requestId: String,
        apiService: EntitlementApiService? = null
    ): Result<UserEntitlement> = runCatching {
        requireUserId()
        if (apiService?.isBackendConfigured == true) {
            return@runCatching apiService.consumeIdentification(requestId).getOrThrow()
        }

        val docRef = db.collection("entitlements").document(uid)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    val seeded = mapOf(
                        "userId" to uid,
                        "planType" to PlanType.FREE.name,
                        "freeIdentificationsUsed" to 0L,
                        "freeIdentificationsLimit" to BotanicalConstants.FREE_IDENTIFICATION_LIMIT.toLong(),
                        "subscriptionStatus" to SubscriptionStatus.FREE.name,
                        "purchaseProvider" to PurchaseProvider.NONE.name,
                        "purchaseReference" to "",
                        "productId" to "",
                        "willAutoRenew" to false,
                        "hasLifetimeEntitlement" to false,
                        "lastScanRequestId" to "",
                        "schemaVersion" to BotanicalConstants.SCHEMA_VERSION.toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(docRef, seeded)
                    // First identification starts at 0 before increment; keep atomic via 2-step or immediate update:
                    // To honor firestore.rules create==0 + update==+1, we ensure doc exists prior to scan,
                    // and if missing here, return 0 used and let caller retry or run second tx.
                    UserEntitlement(id = uid, userId = uid, freeIdentificationsUsed = 0L)
                } else {
                    val current = snap.toObject(
                        UserEntitlement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserEntitlement(id = uid, userId = uid)

                    if (current.isPremiumActive()) {
                        current
                    } else if (current.lastScanRequestId == requestId) {
                        current
                    } else if (current.freeIdentificationsUsed >= current.freeIdentificationsLimit) {
                        throw QuotaExhaustedException()
                    } else {
                        val nextUsed = current.freeIdentificationsUsed + 1L
                        tx.update(
                            docRef,
                            mapOf(
                                "freeIdentificationsUsed" to nextUsed,
                                "lastScanRequestId" to requestId.take(128),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        current.copy(
                            freeIdentificationsUsed = nextUsed,
                            lastScanRequestId = requestId.take(128)
                        )
                    }
                }
            }.await().let { firstPass ->
                if (!firstPass.isPremiumActive() &&
                    firstPass.freeIdentificationsUsed == 0L &&
                    firstPass.lastScanRequestId.isBlank()
                ) {
                    db.runTransaction { tx ->
                        val snap = tx.get(docRef)
                        val current = snap.toObject(
                            UserEntitlement::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: UserEntitlement(id = uid, userId = uid)
                        if (current.isPremiumActive() || current.lastScanRequestId == requestId) {
                            current
                        } else if (current.freeIdentificationsUsed >= current.freeIdentificationsLimit) {
                            throw QuotaExhaustedException()
                        } else {
                            val nextUsed = current.freeIdentificationsUsed + 1L
                            tx.update(
                                docRef,
                                mapOf(
                                    "freeIdentificationsUsed" to nextUsed,
                                    "lastScanRequestId" to requestId.take(128),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            current.copy(
                                freeIdentificationsUsed = nextUsed,
                                lastScanRequestId = requestId.take(128)
                            )
                        }
                    }.await()
                } else {
                    firstPass
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (q: QuotaExhaustedException) {
            throw q
        } catch (e: Exception) {
            if (e.cause is QuotaExhaustedException) throw e.cause as QuotaExhaustedException
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Scans ---

    fun observeScans(uid: String): Flow<List<PlantScanRecord>> = flow {
        val colRef = db.collection("users").document(uid).collection("scans")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(PlantScanRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveScanRecord(
        report: BotanicalReport,
        regionContext: String,
        habitatContext: String,
        thumbnailBase64: String?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("scans").document()
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "commonName" to report.identity.commonName.ifBlank { "Unidentified Specimen" }.take(150),
            "scientificName" to report.identity.scientificName.take(180),
            "family" to report.identity.family.take(120),
            "confidenceTier" to report.confidenceTier.name,
            "confidenceNote" to report.confidenceRationale.take(600),
            "summary" to report.summary.ifBlank { "Morphological botanical report." }.take(2000),
            "safetyLevel" to report.safetyLevel.name,
            "safetyHeadline" to report.safetyHeadline.take(600),
            "regionContext" to regionContext.take(200),
            "habitatContext" to habitatContext.take(300),
            "evidenceQuality" to report.deepResearch.evidenceQuality.name,
            "reportData" to report.toFirestoreMap(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!thumbnailBase64.isNullOrBlank()) {
            payload["thumbnailBase64"] = thumbnailBase64
        }

        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteScanThumbnailOnly(uid: String, scanId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("scans").document(scanId)
        try {
            docRef.update(
                mapOf(
                    "thumbnailBase64" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteScan(uid: String, scanId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("scans").document(scanId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Saved Plants & Folders ---

    fun observeSavedPlants(uid: String): Flow<List<SavedPlantRecord>> = flow {
        val colRef = db.collection("users").document(uid).collection("saved_plants")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(SavedPlantRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun savePlantToCollection(
        report: BotanicalReport,
        scanId: String = "",
        folderId: String = "",
        folderName: String = "General",
        tags: List<String> = emptyList(),
        notes: String = "",
        thumbnailBase64: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_plants").document()
        val sciName = report.identity.scientificName.ifBlank { report.identity.commonName }.ifBlank { "Unknown" }
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "scanId" to scanId.take(128),
            "commonName" to report.identity.commonName.ifBlank { sciName }.take(150),
            "scientificName" to sciName.take(180),
            "family" to report.identity.family.take(120),
            "folderId" to folderId.take(128),
            "folderName" to folderName.ifBlank { "General" }.take(80),
            "tags" to tags.map { it.trim() }.filter { it.isNotBlank() }.take(12),
            "notes" to notes.take(4000),
            "safetyLevel" to report.safetyLevel.name,
            "reportData" to report.toFirestoreMap(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!thumbnailBase64.isNullOrBlank()) {
            payload["thumbnailBase64"] = thumbnailBase64
        }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSavedPlant(
        uid: String,
        plantId: String,
        folderId: String,
        folderName: String,
        tags: List<String>,
        notes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_plants").document(plantId)
        val updates = mapOf(
            "folderId" to folderId.take(128),
            "folderName" to folderName.ifBlank { "General" }.take(80),
            "tags" to tags.map { it.trim() }.filter { it.isNotBlank() }.take(12),
            "notes" to notes.take(4000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSavedPlant(uid: String, plantId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_plants").document(plantId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeFolders(uid: String): Flow<List<PlantFolder>> = flow {
        val colRef = db.collection("users").document(uid).collection("folders")
        emitAll(
            colRef.orderBy("name", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(PlantFolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createFolder(uid: String, name: String, colorHex: String = "#5D8368"): Result<String> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("folders").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(60),
            "colorHex" to colorHex.take(16),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFolder(uid: String, folderId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(uid).collection("folders").document(folderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAllUserData(uid: String): Result<Unit> = runCatching {
        requireUserId()
        val userDoc = db.collection("users").document(uid)
        val subcollections = listOf("scans", "saved_plants", "folders")
        for (sub in subcollections) {
            val docs = userDoc.collection(sub).get().await().documents
            for (d in docs) {
                d.reference.delete().await()
            }
        }
        userDoc.delete().await()
    }

    // --- Public & Curated Content, Config, Announcements ---

    fun observeAppConfig(): Flow<AppConfig> = flow {
        val docRef = db.collection("app_config").document("global")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppConfig(id = "global")
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                    emit(AppConfig(id = "global"))
                }
        )
    }

    fun observePublishedCuratedPlants(): Flow<List<CuratedPlant>> = flow {
        val query = db.collection("curated_plants").whereEqualTo("status", "published")
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(CuratedPlant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "curated_plants")
                    throw error
                }
        )
    }

    fun observePublishedAnnouncements(): Flow<List<Announcement>> = flow {
        val query = db.collection("announcements").whereEqualTo("published", true)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestore()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "announcements")
                    throw error
                }
        )
    }

    // --- Feedback & Telemetry ---

    suspend fun submitFeedbackReport(
        category: String,
        plantName: String,
        message: String,
        userEmail: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("feedback_reports").document()
        val payload = mapOf(
            "userId" to uid,
            "userEmail" to userEmail.take(160),
            "plantName" to plantName.trim().take(150),
            "category" to category,
            "message" to message.trim().take(2000),
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun logAiUsage(
        action: String,
        modelName: String,
        status: String,
        latencyMs: Long,
        errorSummary: String = ""
    ) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("usage_logs").document()
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "action" to action,
            "modelName" to modelName.take(64),
            "status" to status,
            "latencyMs" to latencyMs.coerceIn(0L, 300_000L),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (errorSummary.isNotBlank()) {
            payload["errorSummary"] = errorSummary.take(300)
        }
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    // --- Admin / Owner Dashboard Operations (Server-Verified by Security Rules) ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val col = db.collection("users")
        emitAll(
            col.snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fsErr = e.unwrapFirestore()
                    if (fsErr == null && e is CancellationException) throw e
                    handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
                    throw e
                }
        )
    }

    fun observeAllCuratedPlantsAdmin(): Flow<List<CuratedPlant>> = flow {
        val col = db.collection("curated_plants")
        emitAll(
            col.snapshots()
                .map { it.toObjects(CuratedPlant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fsErr = e.unwrapFirestore()
                    if (fsErr == null && e is CancellationException) throw e
                    handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
                    throw e
                }
        )
    }

    fun observeAllAnnouncementsAdmin(): Flow<List<Announcement>> = flow {
        val col = db.collection("announcements")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    val fsErr = e.unwrapFirestore()
                    if (fsErr == null && e is CancellationException) throw e
                    handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
                    throw e
                }
        )
    }

    fun observeAllFeedbackAdmin(): Flow<List<FeedbackReport>> = flow {
        val col = db.collection("feedback_reports")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    val fsErr = e.unwrapFirestore()
                    if (fsErr == null && e is CancellationException) throw e
                    handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
                    throw e
                }
        )
    }

    fun observeUsageLogsAdmin(): Flow<List<UsageLog>> = flow {
        val col = db.collection("usage_logs")
        emitAll(
            col.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
                .snapshots()
                .map { it.toObjects(UsageLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fsErr = e.unwrapFirestore()
                    if (fsErr == null && e is CancellationException) throw e
                    handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
                    throw e
                }
        )
    }

    suspend fun upsertCuratedPlantAdmin(plant: CuratedPlant, changeSummary: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val slug = plant.id.ifBlank {
            plant.scientificName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        }.ifBlank { "plant_${UUID.randomUUID().toString().take(8)}" }

        val docRef = db.collection("curated_plants").document(slug)
        val existingSnap = docRef.get().await()
        val nextRev = if (existingSnap.exists()) {
            (existingSnap.getLong("revisionNumber") ?: 1L) + 1L
        } else {
            1L
        }

        val payload = mutableMapOf<String, Any>(
            "commonName" to plant.commonName.trim().take(150),
            "scientificName" to plant.scientificName.trim().take(180),
            "family" to plant.family.trim().ifBlank { "Unknown" }.take(120),
            "category" to plant.category.trim().ifBlank { "General" }.take(80),
            "region" to plant.region.trim().ifBlank { "Global" }.take(120),
            "safetyLevel" to plant.safetyLevel,
            "summary" to plant.summary.trim().take(2000),
            "safetyNotice" to plant.safetyNotice.trim().take(1000),
            "habitat" to plant.habitat.trim().take(600),
            "uses" to plant.uses.trim().take(1200),
            "medicinalEvidence" to plant.medicinalEvidence.trim().take(1200),
            "careGuide" to plant.careGuide.trim().take(1200),
            "sources" to plant.sources.take(10),
            "featured" to plant.featured,
            "status" to plant.status,
            "revisionNumber" to nextRev,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!existingSnap.exists()) {
            payload["authorId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }

        // Write immutable revision history entry
        val revRef = docRef.collection("revisions").document("rev_$nextRev")
        revRef.set(
            mapOf(
                "editorId" to uid,
                "revisionNumber" to nextRev,
                "changeSummary" to changeSummary.ifBlank { "Updated curated plant entry" }.take(500),
                "snapshot" to mapOf(
                    "commonName" to plant.commonName,
                    "scientificName" to plant.scientificName,
                    "status" to plant.status,
                    "safetyLevel" to plant.safetyLevel
                ),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteCuratedPlantAdmin(slug: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("curated_plants").document(slug)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun saveAppConfigAdmin(config: AppConfig): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("app_config").document("global")
        val snap = docRef.get().await()
        val payload = mutableMapOf<String, Any>(
            "updatedBy" to uid,
            "maintenanceMode" to config.maintenanceMode,
            "maintenanceMessage" to config.maintenanceMessage.take(400),
            "dailyScanLimit" to config.dailyScanLimit.coerceIn(1L, 500L),
            "featuredPlantSlug" to config.featuredPlantSlug.take(128),
            "featuredPlantTitle" to config.featuredPlantTitle.take(160),
            "featuredPlantFact" to config.featuredPlantFact.take(600),
            "currentAppVersion" to config.currentAppVersion.ifBlank { BotanicalConstants.APP_VERSION }.take(32),
            "releaseNotes" to config.releaseNotes.take(2000),
            "schemaVersion" to config.schemaVersion.coerceAtLeast(1L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }
    }

    suspend fun upsertAnnouncementAdmin(ann: Announcement): Result<Unit> = runCatching {
        val uid = requireUserId()
        val id = ann.id.ifBlank { "ann_${UUID.randomUUID().toString().take(10)}" }
        val docRef = db.collection("announcements").document(id)
        val snap = docRef.get().await()
        val payload = mutableMapOf<String, Any>(
            "title" to ann.title.trim().take(160),
            "body" to ann.body.trim().take(2000),
            "type" to ann.type,
            "versionTag" to ann.versionTag.trim().take(32),
            "published" to ann.published,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["authorId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }
    }

    suspend fun deleteAnnouncementAdmin(id: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("announcements").document(id).delete().await()
    }

    suspend fun updateFeedbackStatusAdmin(
        reportId: String,
        status: String,
        adminNotes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("feedback_reports").document(reportId).update(
            mapOf(
                "status" to status,
                "adminNotes" to adminNotes.trim().take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun seedStarterCatalogAdmin(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val plants = DefaultBotanicalSeed.starterCuratedPlants(uid)
        for (plant in plants) {
            upsertCuratedPlantAdmin(plant, "Starter botanical seed entry").getOrThrow()
        }
        saveAppConfigAdmin(AppConfig(id = "global", updatedBy = uid)).getOrThrow()
    }

    private fun Throwable.unwrapFirestore(): FirebaseFirestoreException? =
        generateSequence(this) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        fun todayUtcString(): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.format(Date())
        }
    }
}
