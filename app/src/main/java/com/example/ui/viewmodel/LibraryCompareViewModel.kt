package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PlantAiRepository
import com.example.data.research.BotanicalResearchCoordinator
import com.example.data.research.GbifSearchEntry
import com.example.model.BotanicalReport
import com.example.model.CuratedPlant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DossierLookupState {
    data object Idle : DossierLookupState
    data object Loading : DossierLookupState
    data class Ready(val report: BotanicalReport) : DossierLookupState
    data class Error(val message: String) : DossierLookupState
}

class LibraryCompareViewModel(
    private val aiRepository: PlantAiRepository,
    private val researchCoordinator: BotanicalResearchCoordinator
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSafetyFilter = MutableStateFlow("ALL")
    val selectedSafetyFilter: StateFlow<String> = _selectedSafetyFilter.asStateFlow()

    private val _gbifResults = MutableStateFlow<List<GbifSearchEntry>>(emptyList())
    val gbifResults: StateFlow<List<GbifSearchEntry>> = _gbifResults.asStateFlow()

    private val _isSearchingGbif = MutableStateFlow(false)
    val isSearchingGbif: StateFlow<Boolean> = _isSearchingGbif.asStateFlow()

    private val _gbifSearchError = MutableStateFlow<String?>(null)
    val gbifSearchError: StateFlow<String?> = _gbifSearchError.asStateFlow()

    private val _recentlyViewed = MutableStateFlow<List<BotanicalReport>>(emptyList())
    val recentlyViewed: StateFlow<List<BotanicalReport>> = _recentlyViewed.asStateFlow()

    private val _compareList = MutableStateFlow<List<BotanicalReport>>(emptyList())
    val compareList: StateFlow<List<BotanicalReport>> = _compareList.asStateFlow()

    private val _dossierState = MutableStateFlow<DossierLookupState>(DossierLookupState.Idle)
    val dossierState: StateFlow<DossierLookupState> = _dossierState.asStateFlow()

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun selectSafetyFilter(safety: String) {
        _selectedSafetyFilter.value = safety
    }

    fun searchLiveGbif(queryOverride: String? = null) {
        val q = (queryOverride ?: _searchQuery.value).trim()
        if (q.length < 2) {
            _gbifResults.value = emptyList()
            _gbifSearchError.value = "Enter at least 2 characters to search GBIF Plantae."
            return
        }
        viewModelScope.launch {
            _isSearchingGbif.value = true
            _gbifSearchError.value = null
            researchCoordinator.searchLivePlantae(q)
                .onSuccess { entries ->
                    _gbifResults.value = entries
                    if (entries.isEmpty()) {
                        _gbifSearchError.value = "No matching Plantae taxa found on GBIF for \"$q\"."
                    }
                }
                .onFailure { err ->
                    _gbifSearchError.value = err.localizedMessage ?: "Failed to query GBIF."
                }
            _isSearchingGbif.value = false
        }
    }

    fun openFullDossier(
        scientificName: String,
        commonName: String?,
        regionContext: String,
        onReady: (BotanicalReport) -> Unit
    ) {
        viewModelScope.launch {
            _dossierState.value = DossierLookupState.Loading
            try {
                val baseReport = aiRepository.generateSpeciesDossier(
                    scientificName = scientificName,
                    commonName = commonName,
                    regionContext = regionContext
                )
                val research = researchCoordinator.verifySpecies(
                    scientificName = baseReport.identity.scientificName.ifBlank { scientificName },
                    commonName = baseReport.identity.commonName,
                    confidenceTier = baseReport.confidenceTier
                )
                val full = baseReport.copy(deepResearch = research)
                recordRecentlyViewed(full)
                _dossierState.value = DossierLookupState.Ready(full)
                onReady(full)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Dossier lookup failed", e)
                _dossierState.value = DossierLookupState.Error(
                    e.localizedMessage ?: "Unable to load full botanical dossier."
                )
            }
        }
    }

    fun recordRecentlyViewed(report: BotanicalReport) {
        _recentlyViewed.update { current ->
            (listOf(report) + current.filterNot {
                it.identity.scientificName.equals(report.identity.scientificName, ignoreCase = true)
            }).take(8)
        }
    }

    fun addToCompare(report: BotanicalReport) {
        _compareList.update { current ->
            if (current.any { it.identity.scientificName.equals(report.identity.scientificName, ignoreCase = true) }) {
                current
            } else {
                (current + report).takeLast(4)
            }
        }
    }

    fun removeFromCompare(scientificName: String) {
        _compareList.update { list ->
            list.filterNot { it.identity.scientificName.equals(scientificName, ignoreCase = true) }
        }
    }

    fun filterCuratedPlants(all: List<CuratedPlant>): List<CuratedPlant> {
        val q = _searchQuery.value.trim().lowercase()
        val cat = _selectedCategory.value
        val safety = _selectedSafetyFilter.value
        return all.filter { plant ->
            val matchesCat = cat == "All" || plant.category.equals(cat, ignoreCase = true)
            val matchesSafety = safety == "ALL" || plant.safetyLevel.equals(safety, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                plant.commonName.lowercase().contains(q) ||
                plant.scientificName.lowercase().contains(q) ||
                plant.family.lowercase().contains(q) ||
                plant.region.lowercase().contains(q) ||
                plant.uses.lowercase().contains(q) ||
                plant.summary.lowercase().contains(q)
            matchesCat && matchesSafety && matchesQuery
        }
    }

    fun clearDossierState() {
        _dossierState.value = DossierLookupState.Idle
    }

    private companion object {
        const val TAG = "LibraryCompareVM"
    }
}
