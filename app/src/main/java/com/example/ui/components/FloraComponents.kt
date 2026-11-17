package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.ImageUtils
import com.example.model.ConfidenceTier
import com.example.model.EvidenceQuality
import com.example.model.SafetyLevel
import com.example.ui.theme.BotanicalGold
import com.example.ui.theme.SafetyCautionAmber
import com.example.ui.theme.SafetyCautionBg
import com.example.ui.theme.SafetySafeBg
import com.example.ui.theme.SafetySafeGreen
import com.example.ui.theme.SafetyToxicBg
import com.example.ui.theme.SafetyToxicCrimson
import com.example.ui.theme.SafetyUnknownBg
import com.example.ui.theme.SafetyUnknownSlate

@Composable
fun FloraAnimatedSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "botanical_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = "FloraMind AI Emblem",
                    tint = BotanicalGold,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "FloraMind AI",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover the Science of Every Plant.",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SafetyAlertBanner(
    safetyLevel: SafetyLevel,
    headline: String,
    isUncertain: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label, icon) = when {
        safetyLevel == SafetyLevel.TOXIC -> Quadruple(
            SafetyToxicBg,
            SafetyToxicCrimson,
            "TOXICITY WARNING",
            Icons.Filled.Warning
        )
        safetyLevel == SafetyLevel.CAUTION || isUncertain -> Quadruple(
            SafetyCautionBg,
            SafetyCautionAmber,
            if (isUncertain) "UNCERTAIN ID — DO NOT CONSUME" else "SAFETY CAUTION",
            Icons.Filled.Warning
        )
        safetyLevel == SafetyLevel.SAFE -> Quadruple(
            SafetySafeBg,
            SafetySafeGreen,
            "LOW KNOWN TOXICITY",
            Icons.Filled.CheckCircle
        )
        else -> Quadruple(
            SafetyUnknownBg,
            SafetyUnknownSlate,
            "SAFETY UNVERIFIED",
            Icons.Filled.HelpOutline
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headline.ifBlank {
                        "Never eat, taste, or apply any plant medicinally based on automated identification alone."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1E2420)
                )
            }
        }
    }
}

@Composable
fun SafetyBadgeChip(safetyLevel: SafetyLevel, modifier: Modifier = Modifier) {
    val (bg, fg, text) = when (safetyLevel) {
        SafetyLevel.SAFE -> Triple(SafetySafeBg, SafetySafeGreen, "Low Risk")
        SafetyLevel.CAUTION -> Triple(SafetyCautionBg, SafetyCautionAmber, "Caution")
        SafetyLevel.TOXIC -> Triple(SafetyToxicBg, SafetyToxicCrimson, "Toxic")
        SafetyLevel.UNKNOWN -> Triple(SafetyUnknownBg, SafetyUnknownSlate, "Unverified")
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConfidenceAndEvidenceRow(
    confidenceTier: ConfidenceTier,
    evidenceQuality: EvidenceQuality,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Identification confidence",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "ID: ${confidenceTier.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = "Evidence quality",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Evidence: ${evidenceQuality.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun PlantThumbnailView(
    thumbnailBase64: String?,
    localPath: String? = null,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap: Bitmap? = remember(thumbnailBase64, localPath) {
        when {
            !localPath.isNullOrBlank() -> ImageUtils.loadBitmapForDisplay(localPath, 600)
                ?: ImageUtils.decodeBase64Thumbnail(thumbnailBase64)
            else -> ImageUtils.decodeBase64Thumbnail(thumbnailBase64)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BotanicalSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun PolicyTextDialog(
    title: String,
    bodyText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(bodyText.trim(), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_policy_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSubmissionDialog(
    defaultPlantName: String,
    onSubmit: (category: String, plantName: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "misidentification" to "Misidentification",
        "safety_concern" to "Safety / Toxicity Concern",
        "bug" to "App Bug",
        "feature_request" to "Feature Request"
    )
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var expanded by remember { mutableStateOf(false) }
    var plantName by remember { mutableStateOf(defaultPlantName) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Issue or Feedback", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.second) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = plantName,
                    onValueChange = { plantName = it },
                    label = { Text("Plant Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Describe the issue or correction") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedCategory.first, plantName, message)
                    onDismiss()
                },
                enabled = message.trim().length >= 5,
                modifier = Modifier.testTag("submit_feedback_button")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { "FM" }
    }

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.5.dp, BotanicalGold.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size >= 52.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "$displayName profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun PlanBadgeChip(
    entitlement: com.example.model.UserEntitlement,
    modifier: Modifier = Modifier
) {
    val effective = entitlement.effectivePlan()
    val (bg, fg, label) = when (effective) {
        com.example.model.PlanType.LIFETIME_PREMIUM -> Triple(
            BotanicalGold.copy(alpha = 0.22f),
            Color(0xFF7A5600),
            "Lifetime Premium"
        )
        com.example.model.PlanType.PREMIUM_ANNUAL -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Premium Annual"
        )
        com.example.model.PlanType.PREMIUM_MONTHLY -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Premium Monthly"
        )
        com.example.model.PlanType.FREE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Free Plan"
        )
    }
    Surface(
        modifier = modifier.testTag("plan_badge_chip"),
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(
            width = 1.dp,
            color = if (effective != com.example.model.PlanType.FREE) BotanicalGold.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (effective != com.example.model.PlanType.FREE) Icons.Filled.Verified else Icons.Filled.Eco,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FreeUsageMeterCard(
    entitlement: com.example.model.UserEntitlement,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremium = entitlement.isPremiumActive()
    val used = entitlement.freeIdentificationsUsed.toInt().coerceAtLeast(0)
    val limit = entitlement.freeIdentificationsLimit.toInt().coerceAtLeast(1)
    val remaining = entitlement.remainingFreeUses()
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("free_usage_meter_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (!isPremium && remaining == 0) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (!isPremium && remaining == 0) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlanBadgeChip(entitlement = entitlement)
                Text(
                    text = if (isPremium) "Unlimited Identifications" else entitlement.formattedRemainingLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!isPremium && remaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            if (!isPremium) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (remaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = if (remaining == 0) {
                        "Your 10 free plant identifications are used ($used/$limit). Your saved collection, history, and account settings remain accessible. Upgrade to continue identifying plants."
                    } else {
                        "Includes $limit successful plant identifications with basic botanical summary & care. Failed scans are never charged."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("usage_meter_upgrade_button")
                ) {
                    Text(
                        if (remaining == 0) "Upgrade to Continue Identifying" else "View Premium & Lifetime Plans"
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureLockCard(
    featureTitle: String,
    featureDescription: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("premium_lock_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, BotanicalGold.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BotanicalGold.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "PREMIUM FEATURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A5600),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = featureTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = featureDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_premium_feature_button")
            ) {
                Text("Unlock with Monthly, Annual, or Lifetime")
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
