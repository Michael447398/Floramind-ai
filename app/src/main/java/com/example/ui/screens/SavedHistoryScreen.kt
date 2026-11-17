package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PdfReportExporter
import com.example.model.BotanicalReport
import com.example.model.ConfidenceTier
import com.example.model.PlantScanRecord
import com.example.model.SafetyLevel
import com.example.model.SavedPlantRecord
import com.example.ui.components.PlantThumbnailView
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.viewmodel.CollectionUiState
import com.example.ui.viewmodel.SavedHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedHistoryScreen(
    viewModel: SavedHistoryViewModel,
    onOpenReport: (BotanicalReport, String?) -> Unit,
    onAddToCompare: (BotanicalReport) -> Unit,
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
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingPlant by remember { mutableStateOf<SavedPlantRecord?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Saved Plants") },
                modifier = Modifier.testTag("tab_saved_plants")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Scan History") },
                modifier = Modifier.testTag("tab_scan_history")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            Text(toastMsg.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filter by plant name, family, tag, or notes...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_history_filter_input")
                )
            }

            if (selectedTab == 0) {
                // Folder chips + Create Folder
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Collections & Folders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        TextButton(
                            onClick = { showCreateFolderDialog = true },
                            modifier = Modifier.testTag("create_folder_button")
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
                                label = { Text("All") }
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
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete folder ${folder.name}",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.deleteFolder(folder.id) }
                                    )
                                }
                            )
                        }
                    }
                }

                when (val sState = savedState) {
                    CollectionUiState.Loading -> item {
                        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is CollectionUiState.Error -> item {
                        Text(sState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    is CollectionUiState.Success -> {
                        val q = filterQuery.trim().lowercase()
                        val filtered = sState.items.filter { item ->
                            val matchesFolder = selectedFolderFilter == "ALL" ||
                                item.folderName.equals(selectedFolderFilter, ignoreCase = true)
                            val matchesQ = q.isBlank() ||
                                item.commonName.lowercase().contains(q) ||
                                item.scientificName.lowercase().contains(q) ||
                                item.notes.lowercase().contains(q) ||
                                item.tags.any { it.lowercase().contains(q) }
                            matchesFolder && matchesQ
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No saved plants match your filter. Identify a specimen and tap Save Plant to build your herbarium.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { saved ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenReport(saved.toBotanicalReport(), saved.thumbnailBase64) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            PlantThumbnailView(
                                                thumbnailBase64 = saved.thumbnailBase64,
                                                contentDescription = saved.commonName,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(saved.commonName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                    SafetyBadgeChip(SafetyLevel.fromString(saved.safetyLevel))
                                                }
                                                Text(
                                                    saved.scientificName,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Folder: ${saved.folderName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (saved.tags.isNotEmpty()) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                saved.tags.forEach { tag ->
                                                    AssistChip(onClick = {}, label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) })
                                                }
                                            }
                                        }
                                        if (saved.notes.isNotBlank()) {
                                            Text(saved.notes, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = { editingPlant = saved }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit notes & tags")
                                            }
                                            IconButton(onClick = { onAddToCompare(saved.toBotanicalReport()) }) {
                                                Icon(Icons.Filled.CompareArrows, contentDescription = "Add to compare")
                                            }
                                            IconButton(onClick = { viewModel.deleteSavedPlant(saved.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Remove saved plant", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Scan History Tab
                when (val hState = scansState) {
                    CollectionUiState.Loading -> item {
                        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is CollectionUiState.Error -> item {
                        Text(hState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    is CollectionUiState.Success -> {
                        val q = filterQuery.trim().lowercase()
                        val filteredScans = hState.items.filter { scan ->
                            q.isBlank() ||
                                scan.commonName.lowercase().contains(q) ||
                                scan.scientificName.lowercase().contains(q) ||
                                scan.regionContext.lowercase().contains(q)
                        }
                        if (filteredScans.isEmpty()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No scan history records yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(filteredScans, key = { it.id }) { scan ->
                                val dateStr = remember(scan.createdAt) {
                                    val millis = scan.createdAt?.toDate()?.time ?: 0L
                                    if (millis > 0L) SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(millis)) else "Just now"
                                }
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenReport(scan.toBotanicalReport(), scan.thumbnailBase64) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            PlantThumbnailView(
                                                thumbnailBase64 = scan.thumbnailBase64,
                                                contentDescription = scan.commonName,
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(scan.commonName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                    SafetyBadgeChip(SafetyLevel.fromString(scan.safetyLevel))
                                                }
                                                if (scan.scientificName.isNotBlank()) {
                                                    Text(
                                                        scan.scientificName,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    "$dateStr • ${ConfidenceTier.fromString(scan.confidenceTier).name} • ${scan.regionContext.ifBlank { "Global" }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (!scan.thumbnailBase64.isNullOrBlank()) {
                                                TextButton(onClick = { viewModel.deleteScanThumbnailOnly(scan.id) }) {
                                                    Icon(Icons.Filled.HideImage, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Remove Photo", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            IconButton(onClick = {
                                                PdfReportExporter.exportAndSharePdf(context, scan.toBotanicalReport())
                                                    .onFailure {
                                                        Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                                                    }
                                            }) {
                                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export scan PDF")
                                            }
                                            IconButton(onClick = { viewModel.deleteScanRecord(scan.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete scan record", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
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
}
