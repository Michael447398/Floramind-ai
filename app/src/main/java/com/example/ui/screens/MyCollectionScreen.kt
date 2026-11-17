package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.PdfReportExporter
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.SafetyLevel
import com.example.model.SavedPlantRecord
import com.example.model.UserProfile
import com.example.ui.components.CollectionPlantCard
import com.example.ui.components.ScanHistoryCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BotanicalGold
import com.example.ui.viewmodel.CollectionUiState
import com.example.ui.viewmodel.SavedHistoryViewModel
import com.google.firebase.auth.FirebaseUser

private enum class CollectionSortMode(val label: String) {
    RECENT("Newest Saved"),
    NAME_ASC("Name (A–Z)"),
    CONFIDENCE("Highest Confidence")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    viewModel: SavedHistoryViewModel,
    onOpenReport: (BotanicalReport, String?) -> Unit,
    onAddToCompare: (BotanicalReport) -> Unit,
    onNavigateIdentify: () -> Unit,
    onOpenGridView: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scansState by viewModel.scansState.collectAsStateWithLifecycle()
    val savedState by viewModel.savedPlantsState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val toastMsg by viewModel.message.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf("ALL") }
    var selectedSafetyFilter by remember { mutableStateOf<SafetyLevel?>(null) }
    var sortMode by remember { mutableStateOf(CollectionSortMode.RECENT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingPlant by remember { mutableStateOf<SavedPlantRecord?>(null) }
    var deletingPlant by remember { mutableStateOf<SavedPlantRecord?>(null) }

    val savedItems = (savedState as? CollectionUiState.Success)?.items.orEmpty()
    val scanItems = (scansState as? CollectionUiState.Success)?.items.orEmpty()
    val photosSavedCount = remember(savedItems) { savedItems.count { !it.thumbnailBase64.isNullOrBlank() } }

    val ownerName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Botanical Explorer"
    val ownerPhoto = currentUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
        ?: userProfile?.photoUrl?.takeIf { it.isNotBlank() }

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My Collection (${savedItems.size})") },
                modifier = Modifier.testTag("tab_saved_plants")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Scan History (${scanItems.size})") },
                modifier = Modifier.testTag("tab_scan_history")
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTwoColumns = maxWidth >= 680.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // My Collection Herbarium Summary Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF102E20),
                                        Color(0xFF1B4A33),
                                        Color(0xFF285E43)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UserAvatar(
                                    photoUrl = ownerPhoto,
                                    displayName = ownerName,
                                    size = 50.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "My Collection",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$ownerName • Cloud Firestore Herbarium",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFDCE8DF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenGridView,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, BotanicalGold.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("open_my_plants_grid_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GridView,
                                        contentDescription = "Open My Plants grid view",
                                        tint = BotanicalGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grid", color = Color.White, maxLines = 1)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CollectionStatPill(
                                    value = savedItems.size.toString(),
                                    label = "Saved Plants",
                                    modifier = Modifier.weight(1f)
                                )
                                CollectionStatPill(
                                    value = photosSavedCount.toString(),
                                    label = "With Photos",
                                    modifier = Modifier.weight(1f)
                                )
                                CollectionStatPill(
                                    value = (folders.size + 1).toString(),
                                    label = "Folders",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (toastMsg != null) {
                    item {
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

                // Search & Sort Controls
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = {
                                Text("Search name, species, family, notes, or #tag...")
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (filterQuery.isNotBlank()) {
                                    IconButton(onClick = { filterQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saved_history_filter_input")
                        )

                        if (selectedTab == 0) {
                            Box {
                                OutlinedButton(
                                    onClick = { sortMenuExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("collection_sort_button")
                                ) {
                                    Icon(Icons.Filled.Sort, contentDescription = "Sort collection", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sort", maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    CollectionSortMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = mode.label,
                                                    fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                sortMode = mode
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Folder & Safety Filters
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Folders & Safety",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showCreateFolderDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("create_folder_button")
                                ) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Folder")
                                }
                            }

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
                                        label = { Text(folder.name) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete folder ${folder.name}",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.deleteFolder(folder.id) }
                                            )
                                        }
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
                        CollectionUiState.Loading -> item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is CollectionUiState.Error -> item {
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

                        is CollectionUiState.Success -> {
                            val q = filterQuery.trim().lowercase()
                            val filtered = sState.items
                                .filter { item ->
                                    val matchesFolder = selectedFolderFilter == "ALL" ||
                                        item.folderName.equals(selectedFolderFilter, ignoreCase = true)
                                    val matchesSafety = selectedSafetyFilter == null ||
                                        SafetyLevel.fromString(item.safetyLevel) == selectedSafetyFilter
                                    val matchesQ = q.isBlank() ||
                                        item.commonName.lowercase().contains(q) ||
                                        item.scientificName.lowercase().contains(q) ||
                                        item.family.lowercase().contains(q) ||
                                        item.notes.lowercase().contains(q) ||
                                        item.tags.any { it.lowercase().contains(q) }
                                    matchesFolder && matchesSafety && matchesQ
                                }
                                .let { list ->
                                    when (sortMode) {
                                        CollectionSortMode.RECENT -> list
                                        CollectionSortMode.NAME_ASC -> list.sortedBy { it.commonName.lowercase() }
                                        CollectionSortMode.CONFIDENCE -> list.sortedBy {
                                            when (it.toBotanicalReport().confidenceTier) {
                                                ConfidenceTier.HIGH -> 0
                                                ConfidenceTier.MODERATE -> 1
                                                ConfidenceTier.UNCERTAIN -> 2
                                                ConfidenceTier.NON_PLANT -> 3
                                            }
                                        }
                                    }
                                }

                            if (filtered.isEmpty()) {
                                item {
                                    EmptyCollectionCard(
                                        hasAnyPlants = sState.items.isNotEmpty(),
                                        onResetFilters = {
                                            filterQuery = ""
                                            selectedFolderFilter = "ALL"
                                            selectedSafetyFilter = null
                                        },
                                        onNavigateIdentify = onNavigateIdentify
                                    )
                                }
                            } else if (useTwoColumns) {
                                val rows = filtered.chunked(2)
                                items(rows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        pair.forEach { saved ->
                                            CollectionPlantCard(
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
                                                onRequestDelete = { deletingPlant = saved },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(filtered, key = { it.id }) { saved ->
                                    CollectionPlantCard(
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
                } else {
                    // Scan History Tab
                    when (val hState = scansState) {
                        CollectionUiState.Loading -> item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is CollectionUiState.Error -> item {
                            Text(
                                text = hState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is CollectionUiState.Success -> {
                            val q = filterQuery.trim().lowercase()
                            val filteredScans = hState.items.filter { scan ->
                                q.isBlank() ||
                                    scan.commonName.lowercase().contains(q) ||
                                    scan.scientificName.lowercase().contains(q) ||
                                    scan.family.lowercase().contains(q) ||
                                    scan.regionContext.lowercase().contains(q)
                            }
                            if (filteredScans.isEmpty()) {
                                item {
                                    EmptyCollectionCard(
                                        hasAnyPlants = hState.items.isNotEmpty(),
                                        emptyText = "No scan history records match your search yet.",
                                        onResetFilters = { filterQuery = "" },
                                        onNavigateIdentify = onNavigateIdentify
                                    )
                                }
                            } else {
                                items(filteredScans, key = { it.id }) { scan ->
                                    ScanHistoryCard(
                                        scan = scan,
                                        onOpenReport = { onOpenReport(scan.toBotanicalReport(), scan.thumbnailBase64) },
                                        onSaveToCollection = { viewModel.saveScanToCollection(scan) },
                                        onRemovePhoto = { viewModel.deleteScanThumbnailOnly(scan.id) },
                                        onExportPdf = {
                                            PdfReportExporter.exportAndSharePdf(context, scan.toBotanicalReport())
                                                .onFailure {
                                                    Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                                                }
                                        },
                                        onDelete = { viewModel.deleteScanRecord(scan.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Collection Folder", style = MaterialTheme.typography.headlineSmall) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name (e.g. Nigerian Herbs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolder(folderName)
                        showCreateFolderDialog = false
                    },
                    enabled = folderName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
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
                    "Remove this saved specimen and its personal notes from your Firestore collection? Any original entry in Scan History will remain unchanged."
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
private fun CollectionStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x2BFFFFFF),
        border = BorderStroke(1.dp, BotanicalGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE9E2D0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyCollectionCard(
    hasAnyPlants: Boolean,
    emptyText: String = "Your My Collection herbarium is ready. Identify a plant and tap Save Plant to store its photo, taxonomy, safety status, and notes to your Firestore profile.",
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
                contentDescription = "Empty herbarium collection",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (hasAnyPlants) {
                    "No saved plants match your current search or filters."
                } else {
                    emptyText
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
                        .testTag("empty_collection_identify_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identify a Plant")
                }
            }
        }
    }
}
