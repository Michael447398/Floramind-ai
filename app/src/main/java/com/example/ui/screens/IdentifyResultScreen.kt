package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BotanicalConstants
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.PlantFolder
import com.example.ui.components.BotanicalSectionCard
import com.example.ui.components.ConfidenceAndEvidenceRow
import com.example.ui.components.FeedbackSubmissionDialog
import com.example.ui.components.PlantThumbnailView
import com.example.ui.components.PremiumFeatureLockCard
import com.example.ui.components.SafetyAlertBanner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentifyResultScreen(
    report: BotanicalReport,
    thumbnailBase64: String?,
    localImagePath: String?,
    folders: List<PlantFolder>,
    onBack: () -> Unit,
    onOpenFullReport: () -> Unit,
    onSavePlant: (folderId: String, folderName: String, tags: List<String>, notes: String) -> Unit,
    onAddToCompare: () -> Unit,
    onAskFloraMind: () -> Unit,
    onNewScan: () -> Unit,
    onSubmitFeedback: (category: String, plantName: String, message: String) -> Unit,
    isPremium: Boolean = false,
    onNavigateUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showSaveDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Identification Result",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            PlantThumbnailView(
                thumbnailBase64 = thumbnailBase64,
                localPath = localImagePath,
                contentDescription = "${report.identity.commonName} scanned specimen",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }

        if (!report.isPlantRecognized || report.confidenceTier == ConfidenceTier.NON_PLANT) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "No Botanical Specimen Detected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = report.summary.ifBlank {
                                "The uploaded image does not clearly depict a plant, leaf, flower, crop, moss, or cactus. Try a sharp, well-lit close-up of the plant."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onNewScan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Another Photo")
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = report.identity.commonName.ifBlank { "Unidentified Specimen" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (report.identity.scientificName.isNotBlank()) {
                        Text(
                            text = report.identity.scientificName,
                            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (report.identity.family.isNotBlank()) {
                        Text(
                            text = "Family: ${report.identity.family}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ConfidenceAndEvidenceRow(
                        confidenceTier = report.confidenceTier,
                        evidenceQuality = report.deepResearch.evidenceQuality
                    )
                    if (report.confidenceRationale.isNotBlank()) {
                        Text(
                            text = report.confidenceRationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SafetyAlertBanner(
                    safetyLevel = report.safetyLevel,
                    headline = report.safetyHeadline,
                    isUncertain = report.confidenceTier == ConfidenceTier.UNCERTAIN
                )
            }

            item {
                BotanicalSectionCard(title = "Botanical Summary") {
                    Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (report.cultivation.wateringAndLight.isNotBlank() || report.cultivation.soilAndFertilizer.isNotBlank()) {
                item {
                    BotanicalSectionCard(
                        title = "Basic Care Guide",
                        subtitle = "Included with Free & Premium accounts"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (report.cultivation.wateringAndLight.isNotBlank()) {
                                Text(
                                    text = "Light & Watering: ${report.cultivation.wateringAndLight}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (report.cultivation.soilAndFertilizer.isNotBlank()) {
                                Text(
                                    text = "Soil & Nutrition: ${report.cultivation.soilAndFertilizer}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (!isPremium) {
                item {
                    PremiumFeatureLockCard(
                        featureTitle = "Detailed Botanical & Pathology Analysis",
                        featureDescription = "Upgrade to unlock full 12-section reports, Advanced Plant Disease & Pest Diagnostics, medicinal clinical evidence, propagation schedules, and complete Deep Research citations.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }

            if (report.visibleCharacteristics.isNotEmpty()) {
                item {
                    BotanicalSectionCard(
                        title = "Observed Directly in Photo",
                        subtitle = "Visual features seen in your uploaded image(s)"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.visibleCharacteristics.forEach { trait ->
                                AssistChip(onClick = {}, label = { Text(trait) })
                            }
                        }
                    }
                }
            }

            if (report.confidenceTier != ConfidenceTier.HIGH || report.missingInformation.isNotEmpty() || report.alternativeCandidates.isNotEmpty()) {
                item {
                    BotanicalSectionCard(
                        title = "Diagnostic Gaps & Alternative Species",
                        subtitle = "Helpful angles to confirm look-alike taxa"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (report.missingInformation.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Additional photos helpful: ${report.missingInformation.joinToString("; ")}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            report.alternativeCandidates.forEach { cand ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "${cand.commonName} (${cand.scientificName})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(cand.rationale, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenFullReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("result_open_full_report_button")
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPremium) "Open Full Botanical & Health Dossier" else "Open Dossier (Free Summary + Care)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("result_save_plant_button")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Plant")
                        }
                        OutlinedButton(
                            onClick = onAddToCompare,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("result_compare_button")
                        ) {
                            Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compare")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAskFloraMind,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("result_ask_floramind_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask FloraMind")
                        }
                        OutlinedButton(
                            onClick = onNewScan,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Scan")
                        }
                    }

                    TextButton(
                        onClick = { showFeedbackDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report Misidentification or Safety Issue")
                    }
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = BotanicalConstants.SAFETY_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSaveDialog) {
        SavePlantMetadataDialog(
            plantName = report.identity.commonName,
            folders = folders,
            onSave = { fId, fName, tags, notes ->
                onSavePlant(fId, fName, tags, notes)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackSubmissionDialog(
            defaultPlantName = "${report.identity.commonName} (${report.identity.scientificName})",
            onSubmit = onSubmitFeedback,
            onDismiss = { showFeedbackDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePlantMetadataDialog(
    plantName: String,
    folders: List<PlantFolder>,
    initialFolderId: String = "",
    initialFolderName: String = "General",
    initialTags: List<String> = emptyList(),
    initialNotes: String = "",
    onSave: (folderId: String, folderName: String, tags: List<String>, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember(folders) {
        listOf("" to "General") + folders.map { it.id to it.name }
    }
    var selectedFolder by remember {
        mutableStateOf(options.firstOrNull { it.first == initialFolderId || it.second == initialFolderName } ?: options.first())
    }
    var expanded by remember { mutableStateOf(false) }
    var tagsText by remember { mutableStateOf(initialTags.joinToString(", ")) }
    var notes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save $plantName", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedFolder.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Collection Folder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.second) },
                                onClick = {
                                    selectedFolder = opt
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma separated, e.g. medicinal, farm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Field Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(selectedFolder.first, selectedFolder.second, tags, notes)
                },
                modifier = Modifier.testTag("confirm_save_plant_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
