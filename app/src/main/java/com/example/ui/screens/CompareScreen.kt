package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BotanicalReport
import com.example.model.CuratedPlant
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.viewmodel.LibraryCompareViewModel

@Composable
fun CompareScreen(
    viewModel: LibraryCompareViewModel,
    curatedPlants: List<CuratedPlant>,
    userRegion: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val compareList by viewModel.compareList.collectAsStateWithLifecycle()
    val horizontalScroll = rememberScrollState()

    val comparisonRows: List<Pair<String, (BotanicalReport) -> String>> = listOf(
        "Scientific Name" to { it.identity.scientificName.ifBlank { "—" } },
        "Family" to { it.identity.family.ifBlank { "—" } },
        "Confidence / Evidence" to { "${it.confidenceTier.name} / ${it.deepResearch.evidenceQuality.name}" },
        "Safety & Toxicity" to { "${it.safetyLevel.name}: ${it.safety.humanToxicity.ifBlank { it.safetyHeadline }}".take(220) },
        "Appearance & Leaves" to { it.physical.leafMorphology.ifBlank { it.physical.generalAppearance }.take(220) },
        "Habitat & Distribution" to { it.habitat.nativeRange.ifBlank { it.habitat.countriesAndRegions }.take(220) },
        "Care & Watering" to { it.cultivation.wateringAndLight.ifBlank { it.cultivation.plantingAndPropagation }.take(220) },
        "Food & Practical Uses" to { it.uses.foodAndNutrition.ifBlank { it.uses.traditionalUses }.take(220) },
        "Scientific Benefits" to { it.benefits.establishedScientific.ifBlank { it.benefits.preliminaryResearch }.take(220) },
        "Medicinal Evidence" to { it.medicinal.humanClinicalEvidence.ifBlank { it.medicinal.labAndAnimalEvidence }.take(220) },
        "Ecology & Conservation" to { it.ecology.conservationStatus.ifBlank { it.ecology.pollinatorsAndWildlife }.take(220) }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Compare Plants (${compareList.size}/4)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Side-by-side comparison of taxonomy, safety, habitat, care & pharmacology",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Add from Curated Library
        item {
            Text(
                text = "Quick-Add Curated Species to Compare",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(curatedPlants.take(8), key = { it.id.ifBlank { it.scientificName } }) { plant ->
                    val alreadyAdded = compareList.any {
                        it.identity.scientificName.equals(plant.scientificName, ignoreCase = true)
                    }
                    AssistChip(
                        onClick = {
                            if (!alreadyAdded) {
                                viewModel.openFullDossier(
                                    scientificName = plant.scientificName,
                                    commonName = plant.commonName,
                                    regionContext = userRegion
                                ) { report ->
                                    viewModel.addToCompare(report)
                                }
                            }
                        },
                        enabled = !alreadyAdded && compareList.size < 4,
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(plant.commonName) }
                    )
                }
            }
        }

        if (compareList.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Plants Selected Yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap Compare from any identification result, saved plant, or quick-add chips above to compare 2–4 species side by side.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Attribute",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(140.dp)
                            )
                            compareList.forEach { plant ->
                                Column(
                                    modifier = Modifier
                                        .width(210.dp)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = plant.identity.commonName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeFromCompare(plant.identity.scientificName) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("remove_compare_${plant.identity.scientificName}")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove from comparison", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = plant.identity.scientificName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafetyBadgeChip(plant.safetyLevel)
                                }
                            }
                        }

                        // Data Rows
                        comparisonRows.forEachIndexed { idx, (label, extractor) ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.width(140.dp)
                                )
                                compareList.forEach { plant ->
                                    Text(
                                        text = extractor(plant).ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .width(210.dp)
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
