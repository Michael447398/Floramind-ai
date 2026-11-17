package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.CuratedPlant
import com.example.model.SafetyLevel
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    appConfig: AppConfig,
    isVerifiedOwnerAdmin: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    if (!isVerifiedOwnerAdmin) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Access Restricted", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Only the verified application owner account can access FloraMind AI Admin Tools.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Return") }
        }
        return
    }

    val users by viewModel.users.collectAsStateWithLifecycle()
    val curatedPlants by viewModel.allCuratedPlants.collectAsStateWithLifecycle()
    val announcements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    val feedbackReports by viewModel.feedbackReports.collectAsStateWithLifecycle()
    val usageLogs by viewModel.usageLogs.collectAsStateWithLifecycle()
    val adminToast by viewModel.adminToast.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingPlant by remember { mutableStateOf<CuratedPlant?>(null) }
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
            }
            Column {
                Text("FloraMind Owner Console", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Live CMS, Telemetry, Quotas & Releases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (adminToast != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(adminToast.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearToast() }) { Text("OK") }
                }
            }
        }

        val tabs = listOf("Overview", "Botanical CMS", "Announcements", "Feedback & Logs", "Config & Release")
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        val totalScans = users.sumOf { it.totalScans }
                        val aiSuccess = usageLogs.count { it.status == "success" }
                        val aiErrors = usageLogs.count { it.status != "success" }
                        val avgLatency = usageLogs.map { it.latencyMs }.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricCard("Users", users.size.toString(), Modifier.weight(1f))
                            AdminMetricCard("Total Scans", totalScans.toString(), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricCard("AI OK / Err", "$aiSuccess / $aiErrors", Modifier.weight(1f))
                            AdminMetricCard("Avg Latency", "${avgLatency} ms", Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Starter Botanical Seed & Sync", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Populate Firestore with curated global & Nigerian/West African species (Moringa, Neem, Aloe, Cassava, Hibiscus, Oleander) and default global configuration.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { viewModel.seedStarterCatalog() },
                                    modifier = Modifier.fillMaxWidth().testTag("admin_seed_catalog_button")
                                ) {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Seed Starter Botanical Catalog & Config")
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Continuous Update Architecture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "• Live Updates (Instant, No Reinstall): Edit Botanical CMS entries, Announcements, Daily Discovery, Maintenance Mode, and Daily Scan Quotas right here.\n• Source & APK Updates: Edit code in Google AI Studio, keep the same Firebase project & applicationId so all Firestore user data stays intact, increment versionCode/versionName, and export a new signed APK/AAB.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Button(
                            onClick = { editingPlant = CuratedPlant(status = "published", safetyLevel = SafetyLevel.SAFE.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Curated Plant Entry")
                        }
                    }
                    items(curatedPlants, key = { it.id.ifBlank { it.scientificName } }) { plant ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${plant.commonName} (${plant.status})", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${plant.scientificName} • Rev #${plant.revisionNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafetyBadgeChip(SafetyLevel.fromString(plant.safetyLevel))
                                }
                                Row {
                                    IconButton(onClick = { editingPlant = plant }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${plant.commonName}")
                                    }
                                    IconButton(onClick = { viewModel.deleteCuratedPlant(plant.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${plant.commonName}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Button(
                            onClick = { editingAnnouncement = Announcement(type = "changelog", published = true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Publish New Announcement")
                        }
                    }
                    items(announcements, key = { it.id }) { ann ->
                        Card(
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
                                    Text(ann.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${ann.type} • ${if (ann.published) "Published" else "Draft"} • ${ann.versionTag}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(ann.body, style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { editingAnnouncement = ann }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit announcement")
                                    }
                                    IconButton(onClick = { viewModel.deleteAnnouncement(ann.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete announcement", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item { Text("User Feedback & Misidentification Reports (${feedbackReports.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                    items(feedbackReports, key = { it.id }) { rep ->
                        var note by remember(rep.id) { mutableStateOf(rep.adminNotes.orEmpty()) }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${rep.category} • ${rep.status.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                if (rep.plantName.isNotBlank()) Text("Plant: ${rep.plantName}", style = MaterialTheme.typography.titleSmall)
                                Text(rep.message, style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = { Text("Admin Resolution Notes") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.updateFeedbackStatus(rep.id, "reviewed", note) }) {
                                        Text("Mark Reviewed")
                                    }
                                    Button(onClick = { viewModel.updateFeedbackStatus(rep.id, "resolved", note) }) {
                                        Text("Resolve")
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Recent AI Usage & Error Logs (${usageLogs.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                    items(usageLogs.take(25), key = { it.id }) { log ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${log.action} • ${log.status} • ${log.latencyMs}ms (${log.modelName})", style = MaterialTheme.typography.labelMedium)
                                if (log.errorSummary.isNotBlank()) {
                                    Text(log.errorSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        AdminConfigEditorCard(
                            appConfig = appConfig,
                            onSaveConfig = { viewModel.saveAppConfig(it) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    editingPlant?.let { plant ->
        EditCuratedPlantDialog(
            initial = plant,
            onSave = { updated, summary ->
                viewModel.upsertCuratedPlant(updated, summary)
                editingPlant = null
            },
            onDismiss = { editingPlant = null }
        )
    }

    editingAnnouncement?.let { ann ->
        EditAnnouncementDialog(
            initial = ann,
            onSave = { updated ->
                viewModel.upsertAnnouncement(updated)
                editingAnnouncement = null
            },
            onDismiss = { editingAnnouncement = null }
        )
    }
}

@Composable
private fun AdminMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminConfigEditorCard(
    appConfig: AppConfig,
    onSaveConfig: (AppConfig) -> Unit
) {
    var maintenance by remember(appConfig) { mutableStateOf(appConfig.maintenanceMode) }
    var maintenanceMsg by remember(appConfig) { mutableStateOf(appConfig.maintenanceMessage) }
    var dailyLimit by remember(appConfig) { mutableStateOf(appConfig.dailyScanLimit.toString()) }
    var featuredTitle by remember(appConfig) { mutableStateOf(appConfig.featuredPlantTitle) }
    var featuredFact by remember(appConfig) { mutableStateOf(appConfig.featuredPlantFact) }
    var version by remember(appConfig) { mutableStateOf(appConfig.currentAppVersion) }
    var releaseNotes by remember(appConfig) { mutableStateOf(appConfig.releaseNotes) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Live Platform Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Maintenance Mode (Pauses AI Scans)", style = MaterialTheme.typography.titleSmall)
                Switch(checked = maintenance, onCheckedChange = { maintenance = it })
            }
            OutlinedTextField(value = maintenanceMsg, onValueChange = { maintenanceMsg = it }, label = { Text("Maintenance Banner Message") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dailyLimit, onValueChange = { dailyLimit = it.filter { ch -> ch.isDigit() } }, label = { Text("Daily Scan Quota per User (1-500)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = featuredTitle, onValueChange = { featuredTitle = it }, label = { Text("Daily Botanical Discovery Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = featuredFact, onValueChange = { featuredFact = it }, label = { Text("Daily Discovery Scientific Fact") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Current App Version Tag") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = releaseNotes, onValueChange = { releaseNotes = it }, label = { Text("Release Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onSaveConfig(
                        appConfig.copy(
                            maintenanceMode = maintenance,
                            maintenanceMessage = maintenanceMsg,
                            dailyScanLimit = dailyLimit.toLongOrNull()?.coerceIn(1L, 500L) ?: 25L,
                            featuredPlantTitle = featuredTitle,
                            featuredPlantFact = featuredFact,
                            currentAppVersion = version,
                            releaseNotes = releaseNotes
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Publish Live Configuration")
            }
        }
    }
}

@Composable
private fun EditCuratedPlantDialog(
    initial: CuratedPlant,
    onSave: (CuratedPlant, String) -> Unit,
    onDismiss: () -> Unit
) {
    var common by remember { mutableStateOf(initial.commonName) }
    var scientific by remember { mutableStateOf(initial.scientificName) }
    var family by remember { mutableStateOf(initial.family) }
    var category by remember { mutableStateOf(initial.category.ifBlank { "Medicinal & Herbs" }) }
    var region by remember { mutableStateOf(initial.region.ifBlank { "Global" }) }
    var safety by remember { mutableStateOf(initial.safetyLevel) }
    var status by remember { mutableStateOf(initial.status) }
    var summary by remember { mutableStateOf(initial.summary) }
    var changeSummary by remember { mutableStateOf("CMS content update") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New Curated Plant" else "Edit ${initial.commonName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = common, onValueChange = { common = it }, label = { Text("Common Name") }, singleLine = true)
                OutlinedTextField(value = scientific, onValueChange = { scientific = it }, label = { Text("Scientific Name") }, singleLine = true)
                OutlinedTextField(value = family, onValueChange = { family = it }, label = { Text("Family") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SAFE", "CAUTION", "TOXIC").forEach { lvl ->
                        OutlinedButton(onClick = { safety = lvl }, modifier = Modifier.weight(1f)) {
                            Text(lvl, style = MaterialTheme.typography.labelSmall, fontWeight = if (safety == lvl) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("published", "draft", "archived").forEach { st ->
                        OutlinedButton(onClick = { status = st }, modifier = Modifier.weight(1f)) {
                            Text(st, style = MaterialTheme.typography.labelSmall, fontWeight = if (status == st) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") }, minLines = 2)
                OutlinedTextField(value = changeSummary, onValueChange = { changeSummary = it }, label = { Text("Revision Change Note") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            commonName = common,
                            scientificName = scientific,
                            family = family,
                            category = category,
                            region = region,
                            safetyLevel = safety,
                            status = status,
                            summary = summary
                        ),
                        changeSummary
                    )
                },
                enabled = common.isNotBlank() && scientific.isNotBlank() && summary.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditAnnouncementDialog(
    initial: Announcement,
    onSave: (Announcement) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }
    var versionTag by remember { mutableStateOf(initial.versionTag) }
    var published by remember { mutableStateOf(initial.published) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Announcement / Changelog") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = versionTag, onValueChange = { versionTag = it }, label = { Text("Version Tag (e.g. v1.0.1)") }, singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") }, minLines = 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published Live")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial.copy(title = title, body = body, versionTag = versionTag, published = published)) },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
