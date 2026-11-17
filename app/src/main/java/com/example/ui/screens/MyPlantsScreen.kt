package com.example.ui.screens

import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.R
import com.example.core.PdfReportExporter
import com.example.model.BotanicalReport
import com.example.model.SafetyLevel
import com.example.model.SavedPlantRecord
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.theme.BotanicalGold
import com.example.ui.viewmodel.CollectionUiState
import com.example.ui.viewmodel.SavedHistoryViewModel

@Composable
fun MyPlantsScreen(
    viewModel: SavedHistoryViewModel,
    onOpenReport: (BotanicalReport, String?) -> Unit,
    onAddToCompare: (BotanicalReport) -> Unit,
    onNavigateIdentify: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val savedState by viewModel.savedPlantsState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val toastMsg by viewModel.message.collectAsStateWithLifecycle()

    var filterQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf("ALL") }
    var selectedSafetyFilter by remember { mutableStateOf<SafetyLevel?>(null) }

    var editingPlant by remember { mutableStateOf<SavedPlantRecord?>(null) }
    var deletingPlant by remember { mutableStateOf<SavedPlantRecord?>(null) }

    val savedItems = (savedState as? CollectionUiState.Success)?.items.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("my_plants_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "My Plants",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${savedItems.size} saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Firestore herbarium grid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("my_plants_list_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ViewList,
                        contentDescription = "Switch to detailed collection list"
                    )
                }

                FilledTonalButton(
                    onClick = onNavigateIdentify,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("my_plants_add_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Identify", maxLines = 1)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("my_plants_grid"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (toastMsg != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = toastMsg.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearMessage() }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }

            // Search & Filter Controls
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        placeholder = { Text("Search saved plants, scientific name, tags...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filterQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { filterQuery = "" },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_plants_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFolderFilter == "ALL",
                                onClick = { selectedFolderFilter = "ALL" },
                                label = { Text("All (${savedItems.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFolderFilter == "General",
                                onClick = { selectedFolderFilter = "General" },
                                label = { Text("General") }
                            )
                        }
                        items(folders, key = { it.id }) { folder ->
                            FilterChip(
                                selected = selectedFolderFilter == folder.name,
                                onClick = { selectedFolderFilter = folder.name },
                                label = { Text(folder.name) }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSafetyFilter == null,
                                onClick = { selectedSafetyFilter = null },
                                label = { Text("All Safety") }
                            )
                        }
                        listOf(
                            SafetyLevel.SAFE to "Low Risk",
                            SafetyLevel.CAUTION to "Caution",
                            SafetyLevel.TOXIC to "Toxic"
                        ).forEach { (level, label) ->
                            item {
                                FilterChip(
                                    selected = selectedSafetyFilter == level,
                                    onClick = {
                                        selectedSafetyFilter = if (selectedSafetyFilter == level) null else level
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            when (val sState = savedState) {
                CollectionUiState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is CollectionUiState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is CollectionUiState.Success -> {
                    val q = filterQuery.trim().lowercase()
                    val filtered = sState.items.filter { item ->
                        val matchesFolder = selectedFolderFilter == "ALL" ||
                            item.folderName.equals(selectedFolderFilter, ignoreCase = true)
                        val matchesSafety = selectedSafetyFilter == null ||
                            SafetyLevel.fromString(item.safetyLevel) == selectedSafetyFilter
                        val matchesQuery = q.isBlank() ||
                            item.commonName.lowercase().contains(q) ||
                            item.scientificName.lowercase().contains(q) ||
                            item.family.lowercase().contains(q) ||
                            item.notes.lowercase().contains(q) ||
                            item.tags.any { it.lowercase().contains(q) }
                        matchesFolder && matchesSafety && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyMyPlantsGridCard(
                                hasAnyPlants = sState.items.isNotEmpty(),
                                onResetFilters = {
                                    filterQuery = ""
                                    selectedFolderFilter = "ALL"
                                    selectedSafetyFilter = null
                                },
                                onNavigateIdentify = onNavigateIdentify
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { saved ->
                            MyPlantGridCard(
                                saved = saved,
                                onOpenReport = {
                                    onOpenReport(saved.toBotanicalReport(), saved.thumbnailBase64)
                                },
                                onEditMetadata = { editingPlant = saved },
                                onAddToCompare = { onAddToCompare(saved.toBotanicalReport()) },
                                onExportPdf = {
                                    PdfReportExporter.exportAndSharePdf(context, saved.toBotanicalReport())
                                        .onFailure {
                                            Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                onRequestDelete = { deletingPlant = saved }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    editingPlant?.let { plant ->
        SavePlantMetadataDialog(
            plantName = plant.commonName,
            folders = folders,
            initialFolderId = plant.folderId,
            initialFolderName = plant.folderName,
            initialTags = plant.tags,
            initialNotes = plant.notes,
            onSave = { fId, fName, tags, notes ->
                viewModel.updateSavedPlantMetadata(plant.id, fId, fName, tags, notes)
                editingPlant = null
            },
            onDismiss = { editingPlant = null }
        )
    }

    deletingPlant?.let { plant ->
        AlertDialog(
            onDismissRequest = { deletingPlant = null },
            title = { Text("Remove ${plant.commonName}?") },
            text = {
                Text(
                    "Remove this saved plant from your Firestore collection? Any original entry in Scan History will remain unchanged."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedPlant(plant.id)
                        deletingPlant = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlant = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MyPlantGridCard(
    saved: SavedPlantRecord,
    onOpenReport: () -> Unit,
    onEditMetadata: () -> Unit,
    onAddToCompare: () -> Unit,
    onExportPdf: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = remember(saved.id, saved.updatedAt, saved.reportData) {
        saved.toBotanicalReport()
    }
    val familyName = remember(saved.family, report.identity.family) {
        saved.family.ifBlank { report.identity.family }.ifBlank { "Botanical specimen" }
    }
    val coilImageModel: Any? = remember(saved.id, saved.thumbnailBase64, saved.reportData) {
        val remoteUrl = (saved.reportData["imageUrl"] as? String)?.takeIf { it.isNotBlank() }
        if (remoteUrl != null) {
            remoteUrl
        } else {
            val raw = saved.thumbnailBase64?.trim()
            if (raw.isNullOrBlank()) {
                null
            } else {
                val cleaned = raw.substringAfter("base64,", raw)
                runCatching { Base64.decode(cleaned, Base64.DEFAULT) }.getOrNull()
            }
        }
    }

    var menuExpanded by remember(saved.id) { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenReport)
            .testTag("my_plant_grid_card_${saved.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
            ) {
                if (coilImageModel != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coilImageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${saved.commonName.ifBlank { "Saved plant" }} specimen photograph",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }

                            is AsyncImagePainter.State.Error -> {
                                PlantGridPhotoFallback(
                                    contentDescription = "${saved.commonName} placeholder"
                                )
                            }

                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                } else {
                    PlantGridPhotoFallback(
                        contentDescription = "${saved.commonName} placeholder"
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x52000000),
                                    Color.Transparent,
                                    Color(0x990D2117)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC132B1F)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = BotanicalGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = saved.folderName.ifBlank { "General" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    SafetyBadgeChip(SafetyLevel.fromString(saved.safetyLevel))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC18402C),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${report.confidenceTier.name} ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF6E9CC),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = saved.commonName.ifBlank { "Identified Plant" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (saved.scientificName.isNotBlank()) {
                            Text(
                                text = saved.scientificName,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("my_plant_menu_${saved.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More actions for ${saved.commonName}"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Full Report") },
                                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenReport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Folder & Notes") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditMetadata()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compare Species") },
                                leadingIcon = { Icon(Icons.Filled.CompareArrows, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToCompare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF") },
                                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onExportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Plant", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRequestDelete()
                                }
                            )
                        }
                    }
                }

                Text(
                    text = familyName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )

                val firstTag = saved.tags.firstOrNull()
                if (!firstTag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    ) {
                        Text(
                            text = "#$firstTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantGridPhotoFallback(
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Eco,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun EmptyMyPlantsGridCard(
    hasAnyPlants: Boolean,
    onResetFilters: () -> Unit,
    onNavigateIdentify: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_herbarium),
                contentDescription = "Empty herbarium grid",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (hasAnyPlants) {
                    "No saved plants match your search or selected filters."
                } else {
                    "No saved plants in your Firestore collection yet. Identify a botanical specimen and tap Save Plant to build your visual grid."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasAnyPlants) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Reset Filters")
                }
            } else {
                Button(
                    onClick = onNavigateIdentify,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_my_plants_identify_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identify a Plant")
                }
            }
        }
    }
}
