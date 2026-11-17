package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.unwrapFirestoreException
import com.example.data.repository.FloraRepository
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.CuratedPlant
import com.example.model.FeedbackReport
import com.example.model.UsageLog
import com.example.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: FloraRepository
) : ViewModel() {

    val users: StateFlow<List<UserProfile>> = repository.observeAllUsersAdmin()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            Log.w(TAG, "Admin observe users error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allCuratedPlants: StateFlow<List<CuratedPlant>> = repository.observeAllCuratedPlantsAdmin()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = repository.observeAllAnnouncementsAdmin()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val feedbackReports: StateFlow<List<FeedbackReport>> = repository.observeAllFeedbackAdmin()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val usageLogs: StateFlow<List<UsageLog>> = repository.observeUsageLogsAdmin()
        .catch { e ->
            if (e is CancellationException && e.unwrapFirestoreException() == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _adminToast = MutableStateFlow<String?>(null)
    val adminToast: StateFlow<String?> = _adminToast.asStateFlow()

    fun saveAppConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.saveAppConfigAdmin(config)
                .onSuccess { _adminToast.value = "App configuration published live." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save config." }
        }
    }

    fun upsertCuratedPlant(plant: CuratedPlant, changeSummary: String) {
        viewModelScope.launch {
            repository.upsertCuratedPlantAdmin(plant, changeSummary)
                .onSuccess { _adminToast.value = "Botanical entry saved (${plant.status})." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save plant entry." }
        }
    }

    fun deleteCuratedPlant(slug: String) {
        viewModelScope.launch {
            repository.deleteCuratedPlantAdmin(slug)
                .onSuccess { _adminToast.value = "Curated entry deleted." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to delete entry." }
        }
    }

    fun upsertAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            repository.upsertAnnouncementAdmin(announcement)
                .onSuccess { _adminToast.value = "Announcement saved." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save announcement." }
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncementAdmin(id)
                .onSuccess { _adminToast.value = "Announcement deleted." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to delete announcement." }
        }
    }

    fun updateFeedbackStatus(reportId: String, status: String, adminNotes: String) {
        viewModelScope.launch {
            repository.updateFeedbackStatusAdmin(reportId, status, adminNotes)
                .onSuccess { _adminToast.value = "Feedback updated to $status." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to update feedback." }
        }
    }

    fun seedStarterCatalog() {
        viewModelScope.launch {
            repository.seedStarterCatalogAdmin()
                .onSuccess { _adminToast.value = "Starter botanical catalog & global config seeded to Firestore." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Seed failed." }
        }
    }

    fun clearToast() {
        _adminToast.value = null
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
