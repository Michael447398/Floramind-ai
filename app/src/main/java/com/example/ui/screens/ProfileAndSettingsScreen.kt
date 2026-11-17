package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.example.core.BotanicalConstants
import com.example.data.repository.AuthRepository
import com.example.data.repository.FloraRepository
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.BillingFlowState
import com.example.model.PlanType
import com.example.model.PurchaseProvider
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.example.ui.components.FeedbackSubmissionDialog
import com.example.ui.components.PlanBadgeChip
import com.example.ui.components.PolicyTextDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BotanicalGold
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndSettingsScreen(
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    appConfig: AppConfig,
    announcements: List<Announcement>,
    isVerifiedOwnerAdmin: Boolean,
    authRepository: AuthRepository,
    floraRepository: FloraRepository,
    onSavePreferences: (displayName: String, region: String, darkMode: Boolean, retainThumbnails: Boolean, advancedMode: Boolean) -> Unit,
    onDarkModeToggleImmediate: (Boolean) -> Unit,
    onSubmitFeedback: (category: String, plantName: String, message: String) -> Unit,
    onOpenAdminConsole: () -> Unit,
    onSignedOut: () -> Unit,
    entitlement: UserEntitlement = UserEntitlement(),
    billingFlowState: BillingFlowState = BillingFlowState.Idle,
    onNavigateUpgrade: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember(userProfile?.displayName, currentUser?.displayName) {
        mutableStateOf(userProfile?.displayName ?: currentUser?.displayName ?: "Botanical Explorer")
    }
    var selectedRegion by remember(userProfile?.region) {
        mutableStateOf(userProfile?.region ?: "Global")
    }
    var darkMode by remember(userProfile?.darkMode) {
        mutableStateOf(userProfile?.darkMode ?: false)
    }
    var retainThumbnails by remember(userProfile?.retainThumbnails) {
        mutableStateOf(userProfile?.retainThumbnails ?: true)
    }
    var advancedMode by remember(userProfile?.advancedBotanyMode) {
        mutableStateOf(userProfile?.advancedBotanyMode ?: false)
    }
    var regionExpanded by remember { mutableStateOf(false) }

    var showAnnouncements by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isAccountBusy by remember { mutableStateOf(false) }
    var accountActionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            displayName = it.displayName
            selectedRegion = it.region
            darkMode = it.darkMode
            retainThumbnails = it.retainThumbnails
            advancedMode = it.advancedBotanyMode
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Profile, Privacy & Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // User Card
        item {
            val resolvedPhotoUrl = currentUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
                ?: userProfile?.photoUrl?.takeIf { it.isNotBlank() }
            val resolvedEmail = currentUser?.email?.takeIf { it.isNotBlank() }
                ?: userProfile?.email.orEmpty()
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = resolvedPhotoUrl,
                        displayName = displayName,
                        size = 64.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (resolvedEmail.isNotBlank()) {
                            Text(
                                text = resolvedEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentUser?.isEmailVerified == true) {
                                    "Google Verified Session"
                                } else {
                                    "Google Account Session"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Private plant history & favorites synced to your account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PlanBadgeChip(entitlement = entitlement)
                    }
                }
            }
        }

        // Subscription & Plan Management Section
        item {
            val effectivePlan = entitlement.effectivePlan()
            val expiryFormatted = entitlement.expiresAt?.toDate()?.let {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)
            }
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plan & Subscription Management",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PlanBadgeChip(entitlement = entitlement)
                    }

                    Text(
                        text = "Current Plan: ${effectivePlan.displayName} • Status: ${entitlement.parsedStatus().label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (effectivePlan == PlanType.FREE) {
                        Text(
                            text = "Remaining Free Identifications: ${entitlement.formattedRemainingLabel()} (no time limit).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (effectivePlan == PlanType.LIFETIME_PREMIUM) {
                        Text(
                            text = "Permanent Lifetime access enabled on this account — no recurring billing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (expiryFormatted != null) {
                        Text(
                            text = "${if (entitlement.willAutoRenew) "Renews" else "Expires"} on $expiryFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (entitlement.parsedProvider() != PurchaseProvider.NONE && entitlement.purchaseReference.isNotBlank()) {
                        Text(
                            text = "Provider: ${entitlement.parsedProvider().displayName} • Ref: ${entitlement.purchaseReference.take(28)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (billingFlowState !is BillingFlowState.Idle) {
                        val statusMsg = when (billingFlowState) {
                            is BillingFlowState.Processing -> billingFlowState.message
                            is BillingFlowState.Success -> billingFlowState.message
                            is BillingFlowState.Pending -> billingFlowState.message
                            is BillingFlowState.Cancelled -> billingFlowState.message
                            is BillingFlowState.Error -> billingFlowState.message
                            BillingFlowState.Idle -> ""
                        }
                        if (statusMsg.isNotBlank()) {
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (billingFlowState is BillingFlowState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateUpgrade,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("profile_upgrade_plan_button")
                        ) {
                            Text(if (effectivePlan == PlanType.FREE) "Upgrade Plan" else "View Plans")
                        }
                        OutlinedButton(
                            onClick = onRestorePurchases,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("profile_restore_purchases_button")
                        ) {
                            Text("Restore")
                        }
                    }

                    if (entitlement.parsedProvider() == PurchaseProvider.GOOGLE_PLAY &&
                        (effectivePlan == PlanType.PREMIUM_MONTHLY || effectivePlan == PlanType.PREMIUM_ANNUAL)
                    ) {
                        TextButton(
                            onClick = {
                                val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Subscription in Google Play")
                        }
                    }
                }
            }
        }

        // Owner Admin Console Entry (Only for verified owner admin)
        if (isVerifiedOwnerAdmin) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, BotanicalGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Owner Admin Dashboard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Manage botanical CMS, maintenance mode, quotas, releases & logs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier.testTag("open_admin_console_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Botanical & Display Preferences
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Workspace Preferences", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(expanded = regionExpanded, onExpandedChange = { regionExpanded = !regionExpanded }) {
                        OutlinedTextField(
                            value = selectedRegion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Default Climate / Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                            BotanicalConstants.GLOBAL_REGIONS.forEach { reg ->
                                DropdownMenuItem(
                                    text = { Text(reg) },
                                    onClick = {
                                        selectedRegion = reg
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    SettingToggleRow(
                        title = "Dark Botanical Mode",
                        subtitle = "Switch between Warm Ivory and Deep Pine Night themes",
                        checked = darkMode,
                        onCheckedChange = {
                            darkMode = it
                            onDarkModeToggleImmediate(it)
                        }
                    )
                    SettingToggleRow(
                        title = "Sync Compressed Thumbnails Across Devices",
                        subtitle = "Stores <=150 KB preview thumbnails with scan records; full-res photos stay device-only",
                        checked = retainThumbnails,
                        onCheckedChange = { retainThumbnails = it }
                    )
                    SettingToggleRow(
                        title = "Advanced Scientific Mode Default",
                        subtitle = "Ask FloraMind defaults to technical botanical terminology",
                        checked = advancedMode,
                        onCheckedChange = { advancedMode = it }
                    )

                    Button(
                        onClick = {
                            onSavePreferences(displayName, selectedRegion, darkMode, retainThumbnails, advancedMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_preferences_button")
                    ) {
                        Text("Save Preferences")
                    }
                }
            }
        }

        // Release Notes, Announcements, Policies & Support
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Releases, Policies & Support", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = { showAnnouncements = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Announcements & Changelog (v${appConfig.currentAppVersion})")
                    }
                    OutlinedButton(
                        onClick = { showFeedback = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Bug or Botanical Correction")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { showPrivacy = true }) { Text("Privacy Policy") }
                        TextButton(onClick = { showTerms = true }) { Text("Terms of Service") }
                    }
                }
            }
        }

        if (accountActionError != null) {
            item {
                Text(accountActionError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Sign Out & Account Deletion
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (isAccountBusy) return@OutlinedButton
                        isAccountBusy = true
                        accountActionError = null
                        scope.launch {
                            authRepository.signOut(context)
                            isAccountBusy = false
                            onSignedOut()
                        }
                    },
                    enabled = !isAccountBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    if (isAccountBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signing Out...")
                    } else {
                        Icon(Icons.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out or Switch Google Account")
                    }
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isAccountBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_account_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & All Data")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPrivacy) {
        PolicyTextDialog("Privacy Policy", BotanicalConstants.PRIVACY_POLICY_TEXT) { showPrivacy = false }
    }
    if (showTerms) {
        PolicyTextDialog("Terms of Service", BotanicalConstants.TERMS_OF_SERVICE_TEXT) { showTerms = false }
    }
    if (showFeedback) {
        FeedbackSubmissionDialog(
            defaultPlantName = "",
            onSubmit = onSubmitFeedback,
            onDismiss = { showFeedback = false }
        )
    }
    if (showAnnouncements) {
        AlertDialog(
            onDismissRequest = { showAnnouncements = false },
            title = { Text("Announcements & Changelog", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App Version: ${appConfig.currentAppVersion}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(appConfig.releaseNotes, style = MaterialTheme.typography.bodySmall)
                    announcements.forEach { ann ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(ann.title, style = MaterialTheme.typography.titleSmall)
                                if (ann.versionTag.isNotBlank()) {
                                    Text(ann.versionTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(ann.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnnouncements = false }) { Text("Close") }
            }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text("This permanently erases your Firestore profile, all saved plants, folders, scan history, thumbnails, and device scan photos. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        isAccountBusy = true
                        accountActionError = null
                        scope.launch {
                            authRepository.deleteAccountAndData(context, floraRepository)
                                .onSuccess {
                                    isAccountBusy = false
                                    onSignedOut()
                                }
                                .onFailure { err ->
                                    isAccountBusy = false
                                    accountActionError = err.localizedMessage ?: "Account deletion failed."
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
