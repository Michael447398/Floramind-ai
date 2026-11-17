package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.BotanicalConstants
import com.example.core.ImageUtils
import com.example.core.ValidatedImage
import com.example.model.AppConfig
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.example.ui.components.CameraView
import com.example.ui.components.FreeUsageMeterCard
import com.example.ui.viewmodel.IdentifyUiState
import com.example.ui.viewmodel.IdentifyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentifyScreen(
    viewModel: IdentifyViewModel,
    appConfig: AppConfig,
    userProfile: UserProfile?,
    initialCameraMode: Boolean,
    onResultReady: () -> Unit,
    entitlement: UserEntitlement = UserEntitlement(),
    onNavigateUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val regionInput by viewModel.regionInput.collectAsStateWithLifecycle()
    val habitatInput by viewModel.habitatInput.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    var showCameraPreview by remember(initialCameraMode) { mutableStateOf(initialCameraMode) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var cropTargetImage by remember { mutableStateOf<ValidatedImage?>(null) }
    var regionExpanded by remember { mutableStateOf(false) }
    var captureErrorBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile?.region) {
        val profRegion = userProfile?.region
        if (!profRegion.isNullOrBlank() && regionInput == "Global") {
            viewModel.setRegionInput(profRegion)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is IdentifyUiState.Success) {
            onResultReady()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraPermissionDenied = !granted
        if (granted) {
            showCameraPreview = true
        }
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(BotanicalConstants.MAX_PHOTOS_PER_SCAN)
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addImageFromUri(context, uri) }
    }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.addImageFromUri(context, uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Identify a Botanical Specimen",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Capture or upload 1–4 angles (leaf, flower, bark, fruit, or full plant) for higher taxonomic accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FreeUsageMeterCard(
                entitlement = entitlement,
                onUpgradeClick = onNavigateUpgrade
            )
        }

        if (actionMessage != null) {
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
                            text = actionMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearActionMessage() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Capture / Upload Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            showCameraPreview = !showCameraPreview
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("identify_camera_toggle_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showCameraPreview && hasCameraPermission) "Hide Camera" else "Live Camera")
                }

                OutlinedButton(
                    onClick = {
                        val remaining = (BotanicalConstants.MAX_PHOTOS_PER_SCAN - selectedImages.size).coerceAtLeast(1)
                        if (remaining > 1) {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            singlePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    enabled = selectedImages.size < BotanicalConstants.MAX_PHOTOS_PER_SCAN,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("identify_gallery_picker_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo Picker")
                }
            }
        }

        if (cameraPermissionDenied) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Camera permission notice",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Camera permission was denied. You can still upload clear plant photos anytime using the zero-permission Photo Picker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Live CameraX Preview via CameraView component
        if (showCameraPreview && hasCameraPermission) {
            item {
                CameraView(
                    onPhotoCaptured = { uri ->
                        captureErrorBanner = null
                        viewModel.addImageFromUri(context, uri)
                    },
                    isCaptureEnabled = selectedImages.size < BotanicalConstants.MAX_PHOTOS_PER_SCAN,
                    onCaptureError = { msg -> captureErrorBanner = msg }
                )
            }
        }

        if (captureErrorBanner != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = captureErrorBanner.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Selected Photos Tray + Crop/Rotate
        if (selectedImages.isNotEmpty()) {
            item {
                Text(
                    text = "Attached Photos (${selectedImages.size}/${BotanicalConstants.MAX_PHOTOS_PER_SCAN})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(selectedImages, key = { it.id }) { img ->
                        val bmp = remember(img.localPath, img.width, img.height) {
                            ImageUtils.loadBitmapForDisplay(img.localPath, 500)
                        }
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.width(176.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Selected botanical photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeImage(img.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(onClick = { viewModel.rotateImage(img.id) }) {
                                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 degrees", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rotate", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(onClick = { cropTargetImage = img }) {
                                        Icon(Icons.Filled.Crop, contentDescription = "Crop photo", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Crop", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Region & Habitat Context
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Observation Context (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ExposedDropdownMenuBox(
                        expanded = regionExpanded,
                        onExpandedChange = { regionExpanded = !regionExpanded }
                    ) {
                        OutlinedTextField(
                            value = regionInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Country / Geographic Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false }
                        ) {
                            BotanicalConstants.GLOBAL_REGIONS.forEach { region ->
                                DropdownMenuItem(
                                    text = { Text(region) },
                                    onClick = {
                                        viewModel.setRegionInput(region)
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = habitatInput,
                        onValueChange = { viewModel.setHabitatInput(it) },
                        label = { Text("Habitat / Plant part notes (e.g. farm edge, houseplant, milky sap)") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("identify_habitat_input")
                    )
                }
            }
        }

        // Progress / Error / Analyze Action
        item {
            when (val state = uiState) {
                is IdentifyUiState.Analyzing -> {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Analyzing Specimen...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = state.stageLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                is IdentifyUiState.QuotaExhausted -> {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Free Identification Limit Reached (10 of 10 Used)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = onNavigateUpgrade,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quota_exhausted_upgrade_button")
                            ) {
                                Text("Upgrade Plan (Monthly, Annual, or Lifetime)")
                            }
                        }
                    }
                }

                is IdentifyUiState.Error -> {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Identification Incomplete",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.runIdentification(appConfig, userProfile, entitlement) }
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Analysis")
                            }
                        }
                    }
                }

                else -> Unit
            }

            if (uiState !is IdentifyUiState.Analyzing) {
                val canIdentify = entitlement.canIdentify()
                Spacer(modifier = Modifier.height(6.dp))
                if (!canIdentify) {
                    Button(
                        onClick = onNavigateUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("identify_upgrade_required_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upgrade Required (0 of 10 Uses Left)", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = { viewModel.runIdentification(appConfig, userProfile, entitlement) },
                        enabled = selectedImages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("analyze_plant_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Identify & Run Deep Research", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    cropTargetImage?.let { target ->
        CropPhotoDialog(
            image = target,
            onApplyCrop = { l, t, r, b ->
                viewModel.cropImage(target.id, l, t, r, b)
                cropTargetImage = null
            },
            onDismiss = { cropTargetImage = null }
        )
    }
}

@Composable
private fun CropPhotoDialog(
    image: ValidatedImage,
    onApplyCrop: (Float, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var left by remember { mutableFloatStateOf(0.08f) }
    var top by remember { mutableFloatStateOf(0.08f) }
    var right by remember { mutableFloatStateOf(0.92f) }
    var bottom by remember { mutableFloatStateOf(0.92f) }
    val previewBmp: Bitmap? = remember(image.localPath) {
        ImageUtils.loadBitmapForDisplay(image.localPath, 600)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crop Botanical Specimen", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (previewBmp != null) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Photo to crop",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { left = 0.15f; top = 0.15f; right = 0.85f; bottom = 0.85f },
                        modifier = Modifier.weight(1f)
                    ) { Text("Center 70%", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { left = 0.25f; top = 0.25f; right = 0.75f; bottom = 0.75f },
                        modifier = Modifier.weight(1f)
                    ) { Text("Tight 50%", style = MaterialTheme.typography.labelSmall) }
                }
                Text("Left ${(left * 100).toInt()}% • Right ${(right * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(value = left, onValueChange = { left = it.coerceAtMost(right - 0.15f) }, valueRange = 0f..0.75f)
                Slider(value = right, onValueChange = { right = it.coerceAtLeast(left + 0.15f) }, valueRange = 0.25f..1f)
                Text("Top ${(top * 100).toInt()}% • Bottom ${(bottom * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(value = top, onValueChange = { top = it.coerceAtMost(bottom - 0.15f) }, valueRange = 0f..0.75f)
                Slider(value = bottom, onValueChange = { bottom = it.coerceAtLeast(top + 0.15f) }, valueRange = 0.25f..1f)
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyCrop(left, top, right, bottom) },
                modifier = Modifier.testTag("apply_crop_button")
            ) {
                Text("Apply Crop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
