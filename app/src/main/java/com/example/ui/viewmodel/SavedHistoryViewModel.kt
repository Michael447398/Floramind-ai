package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.unwrapFirestoreException
import com.example.data.repository.FloraRepository
import com.example.model.PlantFolder
import com.example.model.PlantScanRecord
import com.example.model.SavedPlantRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CollectionUiState<out T> {
    data object Loading : CollectionUiState<Nothing>
    data class Success<T>(val items: T) : CollectionUiState<T>
    data class Error(val message: String) : CollectionUiState<Nothing>
}

class SavedHistoryViewModel(
    private val repository: FloraRepository,
    private val currentUserId: String
) : ViewModel() {

    val scansState: StateFlow<CollectionUiState<List<PlantScanRecord>>> =
        repository.observeScans(currentUserId)
            .map<List<PlantScanRecord>, CollectionUiState<List<PlantScanRecord>>> { CollectionUiState.Success(it) }
            .catch { error ->
                val fsError = error.unwrapFirestoreException()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing scans", root)
                emit(CollectionUiState.Error(root.message ?: "Failed to load scan history"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CollectionUiState.Loading)

    val savedPlantsState: StateFlow<CollectionUiState<List<SavedPlantRecord>>> =
        repository.observeSavedPlants(currentUserId)
            .map<List<SavedPlantRecord>, CollectionUiState<List<SavedPlantRecord>>> { CollectionUiState.Success(it) }
            .catch { error ->
                val fsError = error.unwrapFirestoreException()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing saved plants", root)
                emit(CollectionUiState.Error(root.message ?: "Failed to load saved plants"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CollectionUiState.Loading)

    val folders: StateFlow<List<PlantFolder>> =
        repository.observeFolders(currentUserId)
            .catch { error ->
                val fsError = error.unwrapFirestoreException()
                if (fsError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(currentUserId, name)
                .onSuccess { _message.value = "Folder \"$name\" created." }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to create folder." }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(currentUserId, folderId)
                .onFailure { _message.value = it.localizedMessage ?: "Failed to delete folder." }
        }
    }

    fun updateSavedPlantMetadata(
        plantId: String,
        folderId: String,
        folderName: String,
        tags: List<String>,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updateSavedPlant(currentUserId, plantId, folderId, folderName, tags, notes)
                .onSuccess { _message.value = "Plant notes & tags updated." }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to update plant." }
        }
    }

    fun deleteSavedPlant(plantId: String) {
        viewModelScope.launch {
            repository.deleteSavedPlant(currentUserId, plantId)
                .onSuccess { _message.value = "Removed from saved plants." }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to remove plant." }
        }
    }

    fun deleteScanThumbnailOnly(scanId: String) {
        viewModelScope.launch {
            repository.deleteScanThumbnailOnly(currentUserId, scanId)
                .onSuccess { _message.value = "Scan photograph deleted; botanical report retained." }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to delete image." }
        }
    }

    fun deleteScanRecord(scanId: String) {
        viewModelScope.launch {
            repository.deleteScan(currentUserId, scanId)
                .onSuccess { _message.value = "Scan record deleted." }
                .onFailure { _message.value = it.localizedMessage ?: "Failed to delete scan." }
        }
    }

    fun saveScanToCollection(scan: PlantScanRecord) {
        viewModelScope.launch {
            repository.savePlantToCollection(
                report = scan.toBotanicalReport(),
                scanId = scan.id,
                thumbnailBase64 = scan.thumbnailBase64
            ).onSuccess {
                _message.value = "${scan.commonName} added to My Collection."
            }.onFailure {
                _message.value = it.localizedMessage ?: "Failed to save to My Collection."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private companion object {
        const val TAG = "SavedHistoryVM"
    }
}
