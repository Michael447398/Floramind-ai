package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.BotanicalConstants
import com.example.data.repository.FloraRepository
import com.example.model.AppConfig
import com.example.model.BotanicalReport
import com.example.model.CuratedPlant
import com.example.model.PlantScanRecord
import com.example.model.SafetyLevel
import com.example.model.SavedPlantRecord
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.example.ui.components.FreeUsageMeterCard
import com.example.ui.components.PlanBadgeChip
import com.example.ui.components.PlantThumbnailView
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BotanicalGold
import java.util.Calendar

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    fallbackDisplayName: String,
    photoUrl: String? = userProfile?.photoUrl,
    appConfig: AppConfig,
    recentScans: List<PlantScanRecord>,
    savedPlants: List<SavedPlantRecord>,
    curatedPlants: List<CuratedPlant>,
    onNavigateIdentifyCamera: () -> Unit,
    onNavigateIdentifyGallery: () -> Unit,
    onNavigateDiscoverSearch: (query: String, category: String) -> Unit,
    onNavigateAskFloraMind: () -> Unit,
    onNavigateCompare: () -> Unit,
    onNavigateCollection: () -> Unit = {},
    onNavigateMyPlants: () -> Unit = {},
    onNavigateProfile: () -> Unit,
    entitlement: UserEntitlement = UserEntitlement(),
    onNavigateUpgrade: () -> Unit = {},
    onOpenReport: (BotanicalReport, String?) -> Unit,
    onOpenCuratedPlant: (CuratedPlant) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val displayName = (userProfile?.displayName ?: fallbackDisplayName).substringBefore(" ").ifBlank { "Botanist" }
    val today = FloraRepository.todayUtcString()
    val todayUsed = if (userProfile?.dailyScanDate == today) userProfile.dailyScanCount else 0L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting + Profile Quick Access
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$greeting, $displayName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanBadgeChip(entitlement = entitlement)
                        Text(
                            text = userProfile?.region ?: "Global",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onNavigateProfile,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_profile_button")
                ) {
                    UserAvatar(
                        photoUrl = photoUrl ?: userProfile?.photoUrl,
                        displayName = userProfile?.displayName ?: fallbackDisplayName,
                        size = 44.dp
                    )
                }
            }
        }

        // Maintenance Banner if enabled by Owner
        if (appConfig.maintenanceMode) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Maintenance notice",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appConfig.maintenanceMessage.ifBlank {
                                "Scheduled botanical AI maintenance is underway. Saved plants remain available."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Botanical Knowledge Search Bar
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search species, family, medicinal use, or region...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        TextButton(onClick = { onNavigateDiscoverSearch(searchText, "All") }) {
                            Text("Go")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onNavigateDiscoverSearch(searchText, "All")
                }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input")
            )
        }

        // Identify a Plant Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_botanical),
                    contentDescription = "Identify a plant botanical header",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xD8113324), Color(0xF0143D2B))
                            )
                        )
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Identify Any Botanical Specimen",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Photograph leaves, flowers, bark, or fruits for AI morphology + live GBIF, Kew POWO, WFO & PubMed verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFDCE8DF)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateIdentifyCamera,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BotanicalGold,
                                contentColor = Color(0xFF1A1405)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_identify_camera_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Scan", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onNavigateIdentifyGallery,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_identify_upload_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Statistics Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    label = "Total Scans",
                    value = (userProfile?.totalScans ?: recentScans.size.toLong()).toString(),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    label = "My Collection",
                    value = savedPlants.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onNavigateCollection() }
                        .testTag("dashboard_my_collection_stat")
                )
                StatMiniCard(
                    label = if (entitlement.isPremiumActive()) "Plan Access" else "Free Uses Left",
                    value = if (entitlement.isPremiumActive()) "Unlimited" else "${entitlement.remainingFreeUses()}/10",
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onNavigateUpgrade() }
                        .testTag("dashboard_quota_stat")
                )
            }
        }

        // Freemium Plan & Usage Card
        item {
            FreeUsageMeterCard(
                entitlement = entitlement,
                onUpgradeClick = onNavigateUpgrade
            )
        }

        // Quick Tools Row (Ask FloraMind & Compare)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateAskFloraMind() }
                        .testTag("dashboard_ask_floramind_card"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ask FloraMind", style = MaterialTheme.typography.titleSmall)
                            Text("AI botanist Q&A", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateCompare() }
                        .testTag("dashboard_compare_card"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Compare Plants", style = MaterialTheme.typography.titleSmall)
                            Text("Side-by-side table", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Daily Botanical Discovery
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BotanicalGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Eco, contentDescription = null, tint = BotanicalGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DAILY BOTANICAL DISCOVERY",
                            style = MaterialTheme.typography.labelMedium,
                            color = BotanicalGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = appConfig.featuredPlantTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appConfig.featuredPlantFact,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onNavigateDiscoverSearch(appConfig.featuredPlantTitle.substringBefore(" ("), "All") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Explore in Botanical Library ->")
                    }
                }
            }
        }

        // Popular Categories
        item {
            Text(
                text = "Popular Categories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BotanicalConstants.POPULAR_CATEGORIES.drop(1)) { cat ->
                    FilterChip(
                        selected = false,
                        onClick = { onNavigateDiscoverSearch("", cat) },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Recent Discoveries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Discoveries",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (recentScans.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_herbarium),
                            contentDescription = "Empty herbarium illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No plant scans yet. Capture or upload a specimen to start your scientific journal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentScans.take(8), key = { it.id }) { scan ->
                        BotanicalMiniCard(
                            commonName = scan.commonName,
                            scientificName = scan.scientificName,
                            summary = scan.summary,
                            safetyLevel = SafetyLevel.fromString(scan.safetyLevel),
                            thumbnailBase64 = scan.thumbnailBase64,
                            onClick = { onOpenReport(scan.toBotanicalReport(), scan.thumbnailBase64) }
                        )
                    }
                }
            }
        }

        // Saved Plants Strip
        if (savedPlants.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "My Collection",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = onNavigateMyPlants,
                            modifier = Modifier.testTag("dashboard_my_plants_grid_button")
                        ) {
                            Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grid")
                        }
                        TextButton(
                            onClick = onNavigateCollection,
                            modifier = Modifier.testTag("dashboard_view_collection_button")
                        ) {
                            Text("View All (${savedPlants.size}) →")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(savedPlants.take(8), key = { it.id }) { plant ->
                        BotanicalMiniCard(
                            commonName = plant.commonName,
                            scientificName = plant.scientificName,
                            summary = plant.notes.ifBlank { plant.folderName },
                            safetyLevel = SafetyLevel.fromString(plant.safetyLevel),
                            thumbnailBase64 = plant.thumbnailBase64,
                            onClick = { onOpenReport(plant.toBotanicalReport(), plant.thumbnailBase64) }
                        )
                    }
                }
            }
        }

        // Curated Knowledge Highlights
        if (curatedPlants.isNotEmpty()) {
            item {
                Text(
                    text = "Curated Botanical Highlights",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    curatedPlants.take(4).forEach { curated ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCuratedPlant(curated) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(curated.commonName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${curated.scientificName} • ${curated.region}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        curated.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                SafetyBadgeChip(SafetyLevel.fromString(curated.safetyLevel))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BotanicalMiniCard(
    commonName: String,
    scientificName: String,
    summary: String,
    safetyLevel: SafetyLevel,
    thumbnailBase64: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            PlantThumbnailView(
                thumbnailBase64 = thumbnailBase64,
                contentDescription = "$commonName specimen photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = commonName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SafetyBadgeChip(safetyLevel)
                }
                if (scientificName.isNotBlank()) {
                    Text(
                        text = scientificName,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
