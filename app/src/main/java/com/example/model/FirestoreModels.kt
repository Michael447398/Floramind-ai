package com.example.model

import com.example.core.BotanicalConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val region: String = "Global",
    val darkMode: Boolean = false,
    val retainThumbnails: Boolean = true,
    val advancedBotanyMode: Boolean = false,
    val dailyScanCount: Long = 0L,
    val dailyScanDate: String = "",
    val totalScans: Long = 0L,
    val schemaVersion: Long = BotanicalConstants.SCHEMA_VERSION.toLong(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlantScanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val commonName: String = "",
    val scientificName: String = "",
    val family: String = "",
    val confidenceTier: String = ConfidenceTier.MODERATE.name,
    val confidenceNote: String = "",
    val summary: String = "",
    val safetyLevel: String = SafetyLevel.UNKNOWN.name,
    val safetyHeadline: String = "",
    val regionContext: String = "",
    val habitatContext: String = "",
    val thumbnailBase64: String? = null,
    val evidenceQuality: String = EvidenceQuality.LIMITED.name,
    val reportData: Map<String, Any> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toBotanicalReport(): BotanicalReport = BotanicalReport.fromFirestoreMap(reportData)
}

@IgnoreExtraProperties
data class SavedPlantRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scanId: String = "",
    val commonName: String = "",
    val scientificName: String = "",
    val family: String = "",
    val folderId: String = "",
    val folderName: String = "General",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val safetyLevel: String = SafetyLevel.UNKNOWN.name,
    val thumbnailBase64: String? = null,
    val reportData: Map<String, Any> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toBotanicalReport(): BotanicalReport = BotanicalReport.fromFirestoreMap(reportData)
}

@IgnoreExtraProperties
data class PlantFolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#5D8368",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CuratedPlant(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val commonName: String = "",
    val scientificName: String = "",
    val family: String = "",
    val category: String = "",
    val region: String = "",
    val safetyLevel: String = SafetyLevel.UNKNOWN.name,
    val summary: String = "",
    val safetyNotice: String = "",
    val habitat: String = "",
    val uses: String = "",
    val medicinalEvidence: String = "",
    val careGuide: String = "",
    val sources: List<String> = emptyList(),
    val featured: Boolean = false,
    val status: String = "published",
    val revisionNumber: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CuratedPlantRevision(
    @DocumentId val id: String = "",
    val editorId: String = "",
    val revisionNumber: Long = 1L,
    val changeSummary: String = "",
    val snapshot: Map<String, Any> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val updatedBy: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val dailyScanLimit: Long = BotanicalConstants.DEFAULT_DAILY_SCAN_LIMIT.toLong(),
    val featuredPlantSlug: String = "moringa_oleifera",
    val featuredPlantTitle: String = "Moringa oleifera (Drumstick Tree)",
    val featuredPlantFact: String =
        "Drought-resilient multipurpose tree cultivated widely across Nigeria, West Africa, and South Asia for nutrient-dense leaves and water-clarifying seed proteins.",
    val currentAppVersion: String = BotanicalConstants.APP_VERSION,
    val releaseNotes: String =
        "v1.0.0: Initial release featuring Gemini multimodal identification, live GBIF/Kew POWO/WFO/PubMed verification, plant comparison, collections, Ask FloraMind, and owner CMS.",
    val schemaVersion: Long = BotanicalConstants.SCHEMA_VERSION.toLong(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "announcement",
    val versionTag: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedbackReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val plantName: String = "",
    val category: String = "misidentification",
    val message: String = "",
    val status: String = "open",
    val adminNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsageLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "identify_success",
    val modelName: String = "gemini-3.8-flash",
    val status: String = "success",
    val errorSummary: String = "",
    val latencyMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
