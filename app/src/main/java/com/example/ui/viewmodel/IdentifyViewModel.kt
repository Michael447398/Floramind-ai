package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.BotanicalConstants
import com.example.core.ImageUtils
import com.example.core.ValidatedImage
import com.example.data.ai.PlantAiRepository
import com.example.data.billing.EntitlementApiService
import com.example.data.repository.FloraRepository
import com.example.data.research.BotanicalResearchCoordinator
import com.example.model.AppConfig
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.QuotaExhaustedException
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface IdentifyUiState {
    data object Idle : IdentifyUiState
    data class Analyzing(val stageLabel: String) : IdentifyUiState
    data class Success(
        val report: BotanicalReport,
        val primaryLocalImagePath: String?,
        val thumbnailBase64: String?,
        val savedScanId: String?
    ) : IdentifyUiState
    data class QuotaExhausted(val message: String) : IdentifyUiState
    data class Error(val message: String) : IdentifyUiState
}

class IdentifyViewModel(
    private val aiRepository: PlantAiRepository,
    private val researchCoordinator: BotanicalResearchCoordinator,
    private val floraRepository: FloraRepository,
    private val currentUserId: String,
    private val entitlementApi: EntitlementApiService = EntitlementApiService()
) : ViewModel() {

    private val _selectedImages = MutableStateFlow<List<ValidatedImage>>(emptyList())
    val selectedImages: StateFlow<List<ValidatedImage>> = _selectedImages.asStateFlow()

    private val _regionInput = MutableStateFlow("Global")
    val regionInput: StateFlow<String> = _regionInput.asStateFlow()

    private val _habitatInput = MutableStateFlow("")
    val habitatInput: StateFlow<String> = _habitatInput.asStateFlow()

    private val _uiState = MutableStateFlow<IdentifyUiState>(IdentifyUiState.Idle)
    val uiState: StateFlow<IdentifyUiState> = _uiState.asStateFlow()

    private val _activeReport = MutableStateFlow<BotanicalReport?>(null)
    val activeReport: StateFlow<BotanicalReport?> = _activeReport.asStateFlow()

    private val _activeThumbnailBase64 = MutableStateFlow<String?>(null)
    val activeThumbnailBase64: StateFlow<String?> = _activeThumbnailBase64.asStateFlow()

    private val _activeLocalPath = MutableStateFlow<String?>(null)
    val activeLocalPath: StateFlow<String?> = _activeLocalPath.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private var isRequestInFlight = false
    private var pendingScanRequestId: String? = null

    fun setRegionInput(region: String) {
        _regionInput.value = region
    }

    fun setHabitatInput(habitat: String) {
        _habitatInput.value = habitat
    }

    fun addImageFromUri(context: Context, uri: Uri) {
        if (_selectedImages.value.size >= BotanicalConstants.MAX_PHOTOS_PER_SCAN) {
            _actionMessage.value = "You can attach up to ${BotanicalConstants.MAX_PHOTOS_PER_SCAN} angles per identification."
            return
        }
        ImageUtils.validateAndSaveLocalCopy(context, uri)
            .onSuccess { img ->
                pendingScanRequestId = UUID.randomUUID().toString()
                _selectedImages.update { it + img }
                if (_uiState.value is IdentifyUiState.Error) {
                    _uiState.value = IdentifyUiState.Idle
                }
            }
            .onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Invalid image selected."
            }
    }

    fun removeImage(imageId: String) {
        val target = _selectedImages.value.firstOrNull { it.id == imageId }
        ImageUtils.deleteLocalFile(target?.localPath)
        _selectedImages.update { list -> list.filterNot { it.id == imageId } }
    }

    fun rotateImage(imageId: String) {
        val target = _selectedImages.value.firstOrNull { it.id == imageId } ?: return
        ImageUtils.rotateLocalImage90(target.localPath)
            .onSuccess { updated ->
                _selectedImages.update { list ->
                    list.map { if (it.id == imageId) updated.copy(id = imageId) else it }
                }
            }
            .onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to rotate photo."
            }
    }

    fun cropImage(
        imageId: String,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ) {
        val target = _selectedImages.value.firstOrNull { it.id == imageId } ?: return
        ImageUtils.cropLocalImageNormalized(
            target.localPath,
            leftFraction,
            topFraction,
            rightFraction,
            bottomFraction
        ).onSuccess { updated ->
            _selectedImages.update { list ->
                list.map { if (it.id == imageId) updated.copy(id = imageId) else it }
            }
        }.onFailure { err ->
            _actionMessage.value = err.localizedMessage ?: "Failed to crop photo."
        }
    }

    fun startNewScan() {
        _selectedImages.value = emptyList()
        _habitatInput.value = ""
        pendingScanRequestId = null
        _uiState.value = IdentifyUiState.Idle
    }

    fun openExistingReport(report: BotanicalReport, thumbnailBase64: String? = null, localPath: String? = null) {
        _activeReport.value = report
        _activeThumbnailBase64.value = thumbnailBase64
        _activeLocalPath.value = localPath
    }

    fun clearActiveReport() {
        _activeReport.value = null
        _activeThumbnailBase64.value = null
        _activeLocalPath.value = null
    }

    fun runIdentification(
        appConfig: AppConfig,
        userProfile: UserProfile?,
        entitlement: UserEntitlement? = null
    ) {
        if (isRequestInFlight) return
        val images = _selectedImages.value
        if (images.isEmpty()) {
            _actionMessage.value = "Please capture or upload at least one plant photograph."
            return
        }

        if (appConfig.maintenanceMode) {
            _uiState.value = IdentifyUiState.Error(
                appConfig.maintenanceMessage.ifBlank {
                    "FloraMind AI identification is temporarily paused for maintenance. Your saved plants remain accessible."
                }
            )
            return
        }

        // Enforce 10-identification freemium allowance (server-synced / Firestore rule-enforced)
        if (entitlement != null && !entitlement.canIdentify()) {
            viewModelScope.launch {
                floraRepository.logAiUsage(
                    action = "identify_error",
                    modelName = PlantAiRepository.MODEL_FLASH,
                    status = "quota_blocked",
                    latencyMs = 0L,
                    errorSummary = "Free plan identification allowance exhausted (10/10)"
                )
            }
            _uiState.value = IdentifyUiState.QuotaExhausted(
                "You have used all ${entitlement.freeIdentificationsLimit} free plant identifications. Your settings and saved collection remain accessible — upgrade to Premium for unlimited identifications and full reports."
            )
            return
        }

        isRequestInFlight = true
        val startMs = System.currentTimeMillis()
        val requestId = pendingScanRequestId ?: UUID.randomUUID().toString().also { pendingScanRequestId = it }

        viewModelScope.launch {
            try {
                // Re-verify fresh entitlement from Firestore/backend before invoking Gemini
                val latestEntitlement = if (currentUserId.isNotBlank()) {
                    floraRepository.ensureUserEntitlement(currentUserId, entitlementApi).getOrNull()
                } else {
                    entitlement
                }
                if (latestEntitlement != null && !latestEntitlement.canIdentify()) {
                    _uiState.value = IdentifyUiState.QuotaExhausted(
                        "You have used all ${latestEntitlement.freeIdentificationsLimit} free plant identifications. Upgrade to Premium to continue identifying plants."
                    )
                    return@launch
                }

                _uiState.value = IdentifyUiState.Analyzing("Inspecting leaf, flower, and stem morphology with Gemini...")
                val bitmaps = images.map { ImageUtils.prepareBitmapForGemini(it.localPath) }
                val region = _regionInput.value.ifBlank { userProfile?.region ?: "Global" }
                val habitat = _habitatInput.value.trim()

                val aiReport = aiRepository.identifyPlant(
                    bitmaps = bitmaps,
                    regionContext = region,
                    habitatContext = habitat
                )
                bitmaps.forEach { it.recycle() }

                val isSuccessfulPlantIdentification =
                    aiReport.isPlantRecognized && aiReport.confidenceTier != ConfidenceTier.NON_PLANT

                // Charge free identification allowance ONLY after a valid plant identification succeeds.
                // Failed requests, network/API errors, and NON_PLANT responses never decrement the user's quota.
                if (isSuccessfulPlantIdentification && currentUserId.isNotBlank()) {
                    val quotaResult = floraRepository.consumeIdentificationUseAtomic(
                        uid = currentUserId,
                        requestId = requestId,
                        apiService = entitlementApi
                    )
                    quotaResult.onFailure { err ->
                        if (err is QuotaExhaustedException || err.cause is QuotaExhaustedException) {
                            _uiState.value = IdentifyUiState.QuotaExhausted(
                                err.localizedMessage ?: "You have used all 10 free plant identifications."
                            )
                            return@launch
                        }
                        throw err
                    }
                    pendingScanRequestId = null
                }

                _uiState.value = IdentifyUiState.Analyzing("Verifying taxonomy & citations via GBIF, Kew POWO, WFO & PubMed...")
                val deepResearch = researchCoordinator.verifySpecies(
                    scientificName = aiReport.identity.scientificName,
                    commonName = aiReport.identity.commonName,
                    confidenceTier = aiReport.confidenceTier
                )

                val fullReport = aiReport.copy(deepResearch = deepResearch)
                val primaryPath = images.firstOrNull()?.localPath
                val retainThumb = userProfile?.retainThumbnails ?: true
                val thumbB64 = if (retainThumb && primaryPath != null) {
                    ImageUtils.createCompressedThumbnailBase64(primaryPath)
                } else {
                    null
                }

                val savedId = if (currentUserId.isNotBlank() && isSuccessfulPlantIdentification) {
                    floraRepository.incrementDailyScanCount(currentUserId, userProfile)
                    floraRepository.saveScanRecord(
                        report = fullReport,
                        regionContext = region,
                        habitatContext = habitat,
                        thumbnailBase64 = thumbB64
                    ).getOrNull()
                } else {
                    null
                }

                val elapsed = System.currentTimeMillis() - startMs
                floraRepository.logAiUsage(
                    action = "identify_success",
                    modelName = PlantAiRepository.MODEL_FLASH,
                    status = "success",
                    latencyMs = elapsed
                )

                _activeReport.value = fullReport
                _activeThumbnailBase64.value = thumbB64
                _activeLocalPath.value = primaryPath
                _uiState.value = IdentifyUiState.Success(
                    report = fullReport,
                    primaryLocalImagePath = primaryPath,
                    thumbnailBase64 = thumbB64,
                    savedScanId = savedId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (q: QuotaExhaustedException) {
                _uiState.value = IdentifyUiState.QuotaExhausted(
                    q.localizedMessage ?: "Free identification allowance exhausted (10 of 10 used)."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Identification pipeline error", e)
                val elapsed = System.currentTimeMillis() - startMs
                floraRepository.logAiUsage(
                    action = "identify_error",
                    modelName = PlantAiRepository.MODEL_FLASH,
                    status = "error",
                    latencyMs = elapsed,
                    errorSummary = e.localizedMessage ?: e.javaClass.simpleName
                )
                _uiState.value = IdentifyUiState.Error(
                    e.localizedMessage ?: "Unable to complete plant identification. Your identification allowance was not charged."
                )
            } finally {
                isRequestInFlight = false
            }
        }
    }

    fun saveActivePlantToCollection(
        folderId: String = "",
        folderName: String = "General",
        tags: List<String> = emptyList(),
        notes: String = ""
    ) {
        val report = _activeReport.value ?: return
        val scanId = (_uiState.value as? IdentifyUiState.Success)?.savedScanId.orEmpty()
        viewModelScope.launch {
            floraRepository.savePlantToCollection(
                report = report,
                scanId = scanId,
                folderId = folderId,
                folderName = folderName,
                tags = tags,
                notes = notes,
                thumbnailBase64 = _activeThumbnailBase64.value
            ).onSuccess {
                _actionMessage.value = "${report.identity.commonName} saved to your botanical collection."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save plant."
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "IdentifyVM"
    }
}
