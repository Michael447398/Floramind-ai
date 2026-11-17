package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BotanicalConstants
import com.example.core.PdfReportExporter
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.PlantFolder
import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import com.example.ui.components.ConfidenceAndEvidenceRow
import com.example.ui.components.PlantThumbnailView
import com.example.ui.components.PremiumFeatureLockCard
import com.example.ui.components.SafetyAlertBanner
import kotlinx.coroutines.launch

@Composable
fun FullReportScreen(
    report: BotanicalReport,
    thumbnailBase64: String?,
    localImagePath: String?,
    folders: List<PlantFolder>,
    onBack: () -> Unit,
    onSavePlant: (folderId: String, folderName: String, tags: List<String>, notes: String) -> Unit,
    onAddToCompare: () -> Unit,
    onAskFloraMind: () -> Unit,
    isPremium: Boolean = false,
    onNavigateUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showSaveDialog by remember { mutableStateOf(false) }

    val tocItems = remember {
        listOf(
            "A. Identity",
            "B. Physical",
            "C. Habitat",
            "D. Uses",
            "E. Benefits",
            "F. Safety",
            "G. Medicinal",
            "H. Cultivation",
            "I. Ecology",
            "J. Disease & Pests",
            "K. Facts & AI",
            "L. Deep Research"
        )
    }

    val expandedState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            tocItems.forEach { put(it, true) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header + Export Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Botanical Dossier",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(
                        onClick = {
                            if (!isPremium) {
                                Toast.makeText(
                                    context,
                                    "PDF dossier export is a Premium feature. Upgrade to export full reports.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateUpgrade()
                            } else {
                                PdfReportExporter.exportAndSharePdf(context, report)
                                    .onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "PDF export failed", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF Report", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { PdfReportExporter.shareSummaryText(context, report) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("share_report_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Privacy-Safe Summary", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Specimen Hero Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!thumbnailBase64.isNullOrBlank() || !localImagePath.isNullOrBlank()) {
                    PlantThumbnailView(
                        thumbnailBase64 = thumbnailBase64,
                        localPath = localImagePath,
                        contentDescription = report.identity.commonName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(MaterialTheme.shapes.large)
                    )
                }
                Text(
                    text = report.identity.commonName.ifBlank { "Botanical Specimen" },
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
                ConfidenceAndEvidenceRow(
                    confidenceTier = report.confidenceTier,
                    evidenceQuality = report.deepResearch.evidenceQuality
                )
                SafetyAlertBanner(
                    safetyLevel = report.safetyLevel,
                    headline = report.safetyHeadline,
                    isUncertain = report.confidenceTier == ConfidenceTier.UNCERTAIN
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onAddToCompare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compare", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onAskFloraMind,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ask AI", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Table of Contents Jump Strip
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tocItems) { index, label ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            expandedState[label] = true
                            scope.launch {
                                listState.animateScrollToItem(index + 3)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        // A. Plant Identity & Classification
        item {
            CollapsibleSectionCard(
                title = "A. Plant Identity & Classification",
                expanded = expandedState["A. Identity"] != false,
                onToggle = { expandedState["A. Identity"] = !(expandedState["A. Identity"] ?: true) }
            ) {
                FieldRow("Common Name", report.identity.commonName)
                FieldRow("Scientific Name", report.identity.scientificName)
                FieldRow("Botanical Synonyms", report.identity.synonyms.joinToString(", "))
                FieldRow("Regional & Local Names", report.identity.regionalNames.joinToString(", "))
                FieldRow(
                    "Full Taxonomy",
                    listOf(
                        "Kingdom: ${report.identity.kingdom}",
                        "Phylum: ${report.identity.phylum}",
                        "Class: ${report.identity.plantClass}",
                        "Order: ${report.identity.order}",
                        "Family: ${report.identity.family}",
                        "Genus: ${report.identity.genus}",
                        "Species: ${report.identity.species}"
                    ).filter { !it.endsWith(": ") }.joinToString(" • ")
                )
                FieldRow("Taxonomic Notes", report.identity.taxonomicNotes)
            }
        }

        // B. Physical Characteristics
        item {
            CollapsibleSectionCard(
                title = "B. Physical Characteristics",
                subtitle = "Separates observed photo traits from general species botany",
                expanded = expandedState["B. Physical"] != false,
                onToggle = { expandedState["B. Physical"] = !(expandedState["B. Physical"] ?: true) }
            ) {
                FieldRow("Observed Directly in Photo", report.physical.observedInPhoto)
                FieldRow("General Appearance", report.physical.generalAppearance)
                FieldRow("Leaf Morphology", report.physical.leafMorphology)
                FieldRow("Flowers & Flowering Season", report.physical.flowersAndSeason)
                FieldRow("Stem & Bark", report.physical.stemAndBark)
                FieldRow("Roots", report.physical.roots)
                FieldRow("Fruits & Seeds", report.physical.fruitsAndSeeds)
                FieldRow("Height & Spread", report.physical.heightAndSpread)
                FieldRow("Growth Rate & Lifespan", report.physical.growthRateAndLifespan)
                FieldRow("Distinguishing Traits", report.physical.distinguishingTraits)
            }
        }

        // C. Habitat, Geography & Distribution
        item {
            CollapsibleSectionCard(
                title = "C. Habitat, Geography & Distribution",
                expanded = expandedState["C. Habitat"] != false,
                onToggle = { expandedState["C. Habitat"] = !(expandedState["C. Habitat"] ?: true) }
            ) {
                FieldRow("Native Range", report.habitat.nativeRange)
                FieldRow("Regions & Countries", report.habitat.countriesAndRegions)
                FieldRow("Introduced / Cultivated Range", report.habitat.introducedRange)
                FieldRow("Climate & Temperature", report.habitat.climateAndTemperature)
                FieldRow("Soil & pH Requirements", report.habitat.soilRequirements)
                FieldRow("Water & Humidity", report.habitat.waterAndHumidity)
                FieldRow("Sunlight & Altitude", report.habitat.sunlightAndAltitude)
                FieldRow("Seasonal & Ecological Role", report.habitat.seasonalAndEcologicalRole)
            }
        }

        // D. Practical, Cultural & Economic Uses
        item {
            CollapsibleSectionCard(
                title = "D. Practical, Cultural & Economic Uses",
                expanded = expandedState["D. Uses"] != false,
                onToggle = { expandedState["D. Uses"] = !(expandedState["D. Uses"] ?: true) }
            ) {
                if (isPremium) {
                    FieldRow("Food & Nutrition", report.uses.foodAndNutrition)
                    FieldRow("Agriculture & Horticulture", report.uses.agricultureAndHorticulture)
                    FieldRow("Traditional Uses (Cultural Context)", report.uses.traditionalUses)
                    FieldRow("Medicinal Research Context", report.uses.medicinalResearch)
                    FieldRow("Timber, Fibers & Materials", report.uses.timberCraftsAndMaterials)
                    FieldRow("Cosmetics & Landscaping", report.uses.cosmeticsAndLandscaping)
                    FieldRow("Restoration & Wildlife", report.uses.restorationAndWildlife)
                    FieldRow("Cultural & Scientific Importance", report.uses.culturalAndScientific)
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Detailed Uses & Ethnobotany",
                        featureDescription = "Unlock culinary, agricultural, cosmetic, material, and cultural uses with Premium.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // E. Scientific Benefits & Evidence Distinction
        item {
            CollapsibleSectionCard(
                title = "E. Potential Benefits & Evidence Tiers",
                subtitle = "Distinguishes established science, preliminary studies, and folk claims",
                expanded = expandedState["E. Benefits"] != false,
                onToggle = { expandedState["E. Benefits"] = !(expandedState["E. Benefits"] ?: true) }
            ) {
                if (isPremium) {
                    FieldRow("Established Scientific Findings", report.benefits.establishedScientific)
                    FieldRow("Preliminary / Preclinical Research", report.benefits.preliminaryResearch)
                    FieldRow("Traditional / Folkloric Claims (Unproven)", report.benefits.traditionalClaims)
                    FieldRow("Nutritional Profile", report.benefits.nutritionalProfile)
                    FieldRow("Agricultural & Ecological Value", report.benefits.agriculturalAndEcological)
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Scientific Benefit Evidence Tiers",
                        featureDescription = "See peer-reviewed nutritional, clinical, and agricultural evidence breakdowns.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // F. Safety, Toxicity & Risks (Always visible for user safety)
        item {
            CollapsibleSectionCard(
                title = "F. Safety, Toxicity & Look-Alike Risks",
                expanded = expandedState["F. Safety"] != false,
                onToggle = { expandedState["F. Safety"] = !(expandedState["F. Safety"] ?: true) }
            ) {
                FieldRow("Human Toxicity", report.safety.humanToxicity)
                FieldRow("Pets & Livestock Toxicity", report.safety.petsAndLivestockToxicity)
                FieldRow("Toxic Parts & Active Compounds", report.safety.toxicPartsAndCompounds)
                FieldRow("Skin Contact & Allergy Risks", report.safety.skinAndAllergyRisks)
                FieldRow("Ingestion & Preparation Hazards", report.safety.ingestionAndPreparationRisks)
                FieldRow("Invasiveness & Pests", report.safety.invasivenessAndPests)
                FieldRow("Misidentification / Toxic Look-Alikes", report.safety.misidentificationRisks)
                FieldRow("Medication Interactions", report.safety.medicationInteractions)
                FieldRow("Contraindications (Pregnancy / Children)", report.safety.contraindications)
            }
        }

        // G. Medicinal Properties & Research Evidence
        item {
            CollapsibleSectionCard(
                title = "G. Medicinal Properties & Clinical Evidence",
                expanded = expandedState["G. Medicinal"] != false,
                onToggle = { expandedState["G. Medicinal"] = !(expandedState["G. Medicinal"] ?: true) }
            ) {
                if (isPremium) {
                    FieldRow("Historical & Traditional Context", report.medicinal.historicalUses)
                    FieldRow("Plant Parts Studied", report.medicinal.partsStudied)
                    FieldRow("Phytochemicals / Active Compounds", report.medicinal.activeCompounds)
                    FieldRow("Proposed Biological Mechanisms", report.medicinal.biologicalMechanisms)
                    FieldRow("Laboratory & Animal Evidence", report.medicinal.labAndAnimalEvidence)
                    FieldRow("Human Clinical Evidence", report.medicinal.humanClinicalEvidence)
                    FieldRow("Established Medical Therapeutics", report.medicinal.establishedTherapeutics)
                    FieldRow("Research Gaps & Cautions", report.medicinal.contraindicationsAndGaps)
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Medicinal Pharmacology & Clinical Evidence",
                        featureDescription = "Unlock active phytochemicals, biological mechanisms, human clinical trial evidence, and contraindications.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // H. Care, Cultivation & Propagation
        item {
            CollapsibleSectionCard(
                title = "H. Care, Cultivation & Propagation",
                expanded = expandedState["H. Cultivation"] != false,
                onToggle = { expandedState["H. Cultivation"] = !(expandedState["H. Cultivation"] ?: true) }
            ) {
                // Basic Care always included on Free
                FieldRow("Watering & Light (Basic Care)", report.cultivation.wateringAndLight)
                FieldRow("Soil & Fertilization (Basic Care)", report.cultivation.soilAndFertilizer)
                if (isPremium) {
                    FieldRow("Regional Adaptation Note", report.cultivation.adaptedLocationNote)
                    FieldRow("Planting & Propagation", report.cultivation.plantingAndPropagation)
                    FieldRow("Pruning & Flowering Care", report.cultivation.pruningAndFlowering)
                    FieldRow("Pests & Troubleshooting", report.cultivation.pestsAndTroubleshooting)
                    FieldRow("Harvesting & Sustainability", report.cultivation.harvestingAndSustainability)
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Advanced Propagation, Pruning & Troubleshooting",
                        featureDescription = "Free includes core Light, Water & Soil care. Upgrade for regional adaptation, propagation, pruning, and harvesting guides.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // I. Ecology, Conservation & Environmental Value
        item {
            CollapsibleSectionCard(
                title = "I. Ecology & Conservation",
                expanded = expandedState["I. Ecology"] != false,
                onToggle = { expandedState["I. Ecology"] = !(expandedState["I. Ecology"] ?: true) }
            ) {
                if (isPremium) {
                    FieldRow("Pollinators & Wildlife", report.ecology.pollinatorsAndWildlife)
                    FieldRow("Soil & Water Retention", report.ecology.ecosystemAndSoilWater)
                    FieldRow("Carbon & Microclimate Impact", report.ecology.carbonAndClimate)
                    FieldRow("Conservation Status", report.ecology.conservationStatus)
                    FieldRow("Threats & Invasive Potential", report.ecology.threatsAndInvasivePotential)
                    FieldRow("Responsible Harvesting", report.ecology.responsibleHarvesting)
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Ecology & Conservation Status",
                        featureDescription = "Unlock pollinator networks, soil ecology, climate impact, and conservation status.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // J. Advanced Plant Disease & Pest Analysis
        item {
            CollapsibleSectionCard(
                title = "J. Advanced Disease, Pest & Stress Analysis",
                subtitle = "Visual pathology screening & integrated treatment guide",
                expanded = expandedState["J. Disease & Pests"] != false,
                onToggle = { expandedState["J. Disease & Pests"] = !(expandedState["J. Disease & Pests"] ?: true) }
            ) {
                if (isPremium) {
                    FieldRow("Overall Vigor & Assessment", report.plantHealth.overallHealthAssessment)
                    FieldRow("Visible Disease Symptoms", report.plantHealth.visibleDiseaseSymptoms)
                    FieldRow("Pest & Insect Indicators", report.plantHealth.pestIndicators)
                    FieldRow("Nutrient & Environmental Stress", report.plantHealth.nutrientAndEnvironmentalStress)
                    FieldRow("Diagnostic Confidence & Photo Notes", report.plantHealth.diagnosticConfidenceNotes)
                    FieldRow("Targeted Treatment Plan", report.plantHealth.targetedTreatmentPlan)
                    FieldRow("Prevention & Quarantine Monitoring", report.plantHealth.preventionAndMonitoring)
                } else {
                    if (report.plantHealth.overallHealthAssessment.isNotBlank()) {
                        FieldRow("Health Screening Preview", report.plantHealth.overallHealthAssessment.take(140) + "...")
                    }
                    PremiumFeatureLockCard(
                        featureTitle = "Advanced Disease & Pest Diagnostics",
                        featureDescription = "Unlock fungal/bacterial symptom inspection, pest identification, nutrient stress screening, and targeted organic treatment plans.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        // K. Interesting Facts & AI Provenance
        item {
            CollapsibleSectionCard(
                title = "K. Scientific Highlights & AI Transparency",
                expanded = expandedState["K. Facts & AI"] != false,
                onToggle = { expandedState["K. Facts & AI"] = !(expandedState["K. Facts & AI"] ?: true) }
            ) {
                report.interestingFacts.forEachIndexed { idx, fact ->
                    FieldRow("Highlight ${idx + 1}", fact)
                }
                FieldRow("AI Synthesis Disclosure", report.aiDisclaimerNote)
            }
        }

        // L. Deep Research Verification & Live Botanical Sources
        item {
            CollapsibleSectionCard(
                title = "L. Deep Research & Primary Databases",
                subtitle = "Retrieved ${report.deepResearch.retrievedAtIso.ifBlank { "at scan time" }}",
                expanded = expandedState["L. Deep Research"] != false,
                onToggle = { expandedState["L. Deep Research"] = !(expandedState["L. Deep Research"] ?: true) }
            ) {
                if (isPremium) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Evidence Quality: ${report.deepResearch.evidenceQuality.name}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (report.deepResearch.evidenceRationale.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(report.deepResearch.evidenceRationale, style = MaterialTheme.typography.bodySmall)
                                }
                                if (report.deepResearch.discrepanciesNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Discrepancies: ${report.deepResearch.discrepanciesNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        report.deepResearch.sources.forEach { src ->
                            SourceCitationCard(
                                source = src,
                                onOpenUrl = { url ->
                                    runCatching { uriHandler.openUri(url) }
                                        .onFailure {
                                            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            )
                        }
                    }
                } else {
                    PremiumFeatureLockCard(
                        featureTitle = "Full Deep Research Verification & Citations",
                        featureDescription = "Unlock verified taxonomic synonyms and direct source links across GBIF, Kew POWO, World Flora Online, and PubMed.",
                        onUpgradeClick = onNavigateUpgrade
                    )
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = BotanicalConstants.SAFETY_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp)
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
}

@Composable
private fun CollapsibleSectionCard(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SourceCitationCard(
    source: ResearchSourceRecord,
    onOpenUrl: (String) -> Unit
) {
    val tierBadge = when (source.authorityTier) {
        SourceAuthorityTier.PRIMARY_DATABASE.name -> "Primary Taxonomic Authority"
        SourceAuthorityTier.PEER_REVIEWED_LITERATURE.name -> "Peer-Reviewed Literature"
        else -> "Supplementary Encyclopedia Only"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.sourceName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = source.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (source.retrieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = tierBadge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            if (source.acceptedScientificName.isNotBlank()) {
                Text(
                    text = "Accepted Name: ${source.acceptedScientificName} (${source.taxonomicStatus})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            if (source.summary.isNotBlank()) {
                Text(text = source.summary, style = MaterialTheme.typography.bodySmall)
            }
            if (source.url.isNotBlank()) {
                TextButton(
                    onClick = { onOpenUrl(source.url) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Check Source", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.OpenInNew, contentDescription = "Open external source", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
