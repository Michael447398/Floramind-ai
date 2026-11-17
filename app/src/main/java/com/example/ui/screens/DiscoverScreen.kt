package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.BotanicalConstants
import com.example.model.BotanicalReport
import com.example.model.CuratedPlant
import com.example.model.SafetyLevel
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.viewmodel.DossierLookupState
import com.example.ui.viewmodel.LibraryCompareViewModel

@Composable
fun DiscoverScreen(
    viewModel: LibraryCompareViewModel,
    curatedPlants: List<CuratedPlant>,
    userRegion: String,
    compareCount: Int,
    onOpenDossierReport: (BotanicalReport) -> Unit,
    onNavigateCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSafety by viewModel.selectedSafetyFilter.collectAsStateWithLifecycle()
    val gbifResults by viewModel.gbifResults.collectAsStateWithLifecycle()
    val isSearchingGbif by viewModel.isSearchingGbif.collectAsStateWithLifecycle()
    val gbifError by viewModel.gbifSearchError.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val dossierState by viewModel.dossierState.collectAsStateWithLifecycle()

    val filteredCurated = viewModel.filterCuratedPlants(curatedPlants)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Botanical Library & Search",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Search curated profiles or live GBIF Plantae taxonomy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateCompare,
                        modifier = Modifier.testTag("discover_open_compare_button")
                    ) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compare ($compareCount)")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Common name, scientific name, family, or region...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchLiveGbif() }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discover_search_input")
                    )
                    Button(
                        onClick = { viewModel.searchLiveGbif() },
                        enabled = !isSearchingGbif && searchQuery.trim().length >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("discover_gbif_search_button")
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSearchingGbif) "Querying Global GBIF Database..." else "Search Live Global GBIF Plantae")
                    }
                }
            }

            // Category Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BotanicalConstants.POPULAR_CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "SAFE", "CAUTION", "TOXIC")) { safety ->
                        FilterChip(
                            selected = selectedSafety == safety,
                            onClick = { viewModel.selectSafetyFilter(safety) },
                            label = {
                                Text(
                                    when (safety) {
                                        "ALL" -> "All Safety Levels"
                                        "SAFE" -> "Low Risk"
                                        "CAUTION" -> "Caution"
                                        else -> "Toxic"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Recently Viewed Strip
            if (recentlyViewed.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently Viewed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentlyViewed, key = { it.identity.scientificName.ifBlank { it.identity.commonName } }) { rep ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .width(190.dp)
                                    .clickable { onOpenDossierReport(rep) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(rep.identity.commonName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        rep.identity.scientificName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live GBIF Search Results
            if (gbifError != null) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(gbifError.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (gbifResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Live GBIF Global Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(gbifResults, key = { it.usageKey }) { entry ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openFullDossier(
                                    scientificName = entry.canonicalName,
                                    commonName = entry.vernacularName,
                                    regionContext = userRegion,
                                    onReady = onOpenDossierReport
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.vernacularName?.takeIf { it.isNotBlank() } ?: entry.canonicalName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${entry.scientificName} • ${entry.family.ifBlank { "Plantae" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Status: ${entry.taxonomicStatus} • Tap for full AI + Deep Research dossier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.Public, contentDescription = "Live GBIF record", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Curated Library Section
            item {
                Text(
                    text = "Curated Scientific Catalog (${filteredCurated.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(filteredCurated, key = { it.id.ifBlank { it.scientificName } }) { plant ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.openFullDossier(
                                scientificName = plant.scientificName,
                                commonName = plant.commonName,
                                regionContext = userRegion,
                                onReady = onOpenDossierReport
                            )
                        }
                        .testTag("curated_plant_card_${plant.scientificName}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(plant.commonName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SafetyBadgeChip(SafetyLevel.fromString(plant.safetyLevel))
                        }
                        Text(
                            text = "${plant.scientificName} • Family ${plant.family} • ${plant.region}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = plant.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        when (val dState = dossierState) {
            DossierLookupState.Loading -> {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Generating Scientific Dossier") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Synthesizing morphology & querying GBIF, Kew POWO, WFO, and PubMed...")
                        }
                    },
                    confirmButton = {}
                )
            }
            is DossierLookupState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearDossierState() },
                    title = { Text("Lookup Failed") },
                    text = { Text(dState.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearDossierState() }) { Text("Close") }
                    }
                )
            }
            else -> Unit
        }
    }
}
