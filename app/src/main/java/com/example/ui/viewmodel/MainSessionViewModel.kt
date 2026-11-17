package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.BotanicalConstants
import com.example.core.unwrapFirestoreException
import com.example.data.billing.EntitlementApiService
import com.example.data.billing.PlayBillingManager
import com.example.data.repository.DefaultBotanicalSeed
import com.example.data.repository.FloraRepository
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.BillingFlowState
import com.example.model.CuratedPlant
import com.example.model.PlanType
import com.example.model.PlayProductLoadState
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainSessionViewModel(
    private val repository: FloraRepository,
    val currentUser: FirebaseUser?,
    appContext: Context? = null,
    private val entitlementApi: EntitlementApiService = EntitlementApiService()
) : ViewModel() {

    private val uid: String? = currentUser?.uid

    val isVerifiedOwnerAdmin: Boolean =
        currentUser != null &&
            currentUser.isEmailVerified &&
            currentUser.email.equals(BotanicalConstants.OWNER_ADMIN_EMAIL, ignoreCase = true)

    private val billingManager: PlayBillingManager? =
        if (uid != null && appContext != null) {
            PlayBillingManager(
                context = appContext,
                entitlementApi = entitlementApi,
                onEntitlementUpdated = { refreshEntitlementFromBackend() }
            )
        } else {
            null
        }

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            Log.w(TAG, "Error observing app_config", e)
            emit(AppConfig(id = "global"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig(id = "global"))

    val curatedPlants: StateFlow<List<CuratedPlant>> = repository.observePublishedCuratedPlants()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            Log.w(TAG, "Error observing curated_plants", e)
            emit(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            DefaultBotanicalSeed.starterCuratedPlants("system")
        )

    val announcements: StateFlow<List<Announcement>> = repository.observePublishedAnnouncements()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            Log.w(TAG, "Error observing announcements", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userProfile: StateFlow<UserProfile?> = if (uid != null) {
        repository.observeUserProfile(uid)
            .catch { e ->
                if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
                Log.w(TAG, "Error observing user profile", e)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    } else {
        MutableStateFlow(null)
    }

    val entitlement: StateFlow<UserEntitlement> = if (uid != null) {
        repository.observeUserEntitlement(uid)
            .catch { e ->
                if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
                Log.w(TAG, "Error observing entitlement", e)
                emit(UserEntitlement(id = uid, userId = uid))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                UserEntitlement(id = uid, userId = uid)
            )
    } else {
        MutableStateFlow(UserEntitlement())
    }

    val productLoadState: StateFlow<PlayProductLoadState> =
        billingManager?.productLoadState
            ?: MutableStateFlow(
                PlayProductLoadState.Ready(
                    monthlyFormattedPrice = null,
                    annualFormattedPrice = null,
                    lifetimeFormattedPrice = null,
                    storeAvailable = false,
                    statusNote = "Sign in to view Google Play Store pricing."
                )
            )

    val billingFlowState: StateFlow<BillingFlowState> =
        billingManager?.billingFlowState ?: MutableStateFlow(BillingFlowState.Idle)

    private val _themeOverrideDark = MutableStateFlow<Boolean?>(null)
    val themeOverrideDark: StateFlow<Boolean?> = _themeOverrideDark.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        if (uid != null && currentUser != null) {
            viewModelScope.launch {
                repository.ensureUserProfile(
                    uid = uid,
                    displayName = currentUser.displayName ?: "Botanical Explorer",
                    email = currentUser.email ?: "",
                    photoUrl = currentUser.photoUrl?.toString()
                )
                repository.ensureUserEntitlement(uid, entitlementApi)
            }
            billingManager?.startConnectionAndLoadProducts()
        }
    }

    fun purchasePlan(activity: Activity, planType: PlanType) {
        val manager = billingManager ?: run {
            _statusBannerMessage.value = "Sign in with Google to upgrade your plan."
            return
        }
        manager.launchPurchase(activity, planType)
    }

    fun restorePurchases() {
        val manager = billingManager
        if (manager != null) {
            manager.restorePurchases()
        } else {
            refreshEntitlementFromBackend()
        }
    }

    fun refreshEntitlementFromBackend() {
        val activeUid = uid ?: return
        viewModelScope.launch {
            repository.ensureUserEntitlement(activeUid, entitlementApi)
        }
    }

    fun clearBillingState() {
        billingManager?.clearBillingState()
    }

    fun setLocalDarkOverride(dark: Boolean) {
        _themeOverrideDark.value = dark
    }

    fun updateProfilePreferences(
        displayName: String,
        region: String,
        darkMode: Boolean,
        retainThumbnails: Boolean,
        advancedBotanyMode: Boolean
    ) {
        val activeUid = uid ?: return
        _themeOverrideDark.value = darkMode
        viewModelScope.launch {
            repository.updateUserProfilePreferences(
                uid = activeUid,
                displayName = displayName,
                region = region,
                darkMode = darkMode,
                retainThumbnails = retainThumbnails,
                advancedBotanyMode = advancedBotanyMode
            ).onSuccess {
                _statusBannerMessage.value = "Preferences saved."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to update preferences."
            }
        }
    }

    fun submitFeedback(category: String, plantName: String, message: String) {
        val user = currentUser ?: return
        if (message.trim().length < 5) {
            _statusBannerMessage.value = "Please enter at least 5 characters for your feedback."
            return
        }
        viewModelScope.launch {
            repository.submitFeedbackReport(
                category = category,
                plantName = plantName,
                message = message,
                userEmail = user.email ?: ""
            ).onSuccess {
                _statusBannerMessage.value = "Thank you — your report has been submitted."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to submit feedback."
            }
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        billingManager?.release()
    }

    private companion object {
        const val TAG = "MainSessionVM"
    }
}
