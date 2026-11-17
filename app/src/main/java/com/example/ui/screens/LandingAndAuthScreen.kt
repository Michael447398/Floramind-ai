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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.BotanicalConstants
import com.example.data.repository.AuthRepository
import com.example.data.repository.GoogleSignInResult
import com.example.model.Announcement
import com.example.model.AppConfig
import com.example.model.CuratedPlant
import com.example.model.SafetyLevel
import com.example.ui.components.PolicyTextDialog
import com.example.ui.components.SafetyBadgeChip
import com.example.ui.theme.BotanicalGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingAndAuthScreen(
    authRepository: AuthRepository,
    curatedPlants: List<CuratedPlant>,
    announcements: List<Announcement>,
    appConfig: AppConfig,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authTabIndex by remember { mutableIntStateOf(0) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedPreviewPlant by remember { mutableStateOf<CuratedPlant?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showAuthHelp by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_botanical),
                        contentDescription = "FloraMind AI botanical field guide banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEB0E2D1F),
                                        Color(0xBD143D2B),
                                        Color(0x44143D2B)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BotanicalGold.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, BotanicalGold.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "BOTANICAL RESEARCH PLATFORM • v${appConfig.currentAppVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6E9CC),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "FloraMind AI",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Discover the Science of Every Plant.",
                                style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                                color = Color(0xFFDCE8DF)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Multimodal species analysis paired with live GBIF, Kew POWO, World Flora Online, and PubMed verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEBE8DE)
                            )
                        }
                    }
                }
            }

            item {
                // Sign-In / Registration Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SecondaryTabRow(
                            selectedTabIndex = authTabIndex,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = authTabIndex == 0,
                                onClick = {
                                    authTabIndex = 0
                                    authError = null
                                },
                                text = { Text("Sign In") },
                                modifier = Modifier.testTag("auth_tab_sign_in")
                            )
                            Tab(
                                selected = authTabIndex == 1,
                                onClick = {
                                    authTabIndex = 1
                                    authError = null
                                },
                                text = { Text("Create Account") },
                                modifier = Modifier.testTag("auth_tab_create_account")
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (authTabIndex == 0) {
                                "Sign in to your Herbarium Workspace"
                            } else {
                                "Create your FloraMind AI Account"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (authTabIndex == 0) {
                                "Choose a Google account on your device to restore your scan history, saved plant folders, and climate preferences."
                            } else {
                                "Start in seconds with Google. Your unique Google account automatically creates one private Herbarium Workspace with zero duplicate accounts."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Receives only your name, email, and profile photo. Never accesses Gmail or your Google password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        if (authError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isSigningIn) return@Button
                                isSigningIn = true
                                authError = null
                                scope.launch {
                                    when (val res = authRepository.signInWithGoogleInteractive(context)) {
                                        is GoogleSignInResult.Success -> {
                                            isSigningIn = false
                                            onSignedIn()
                                        }
                                        GoogleSignInResult.Cancelled -> {
                                            isSigningIn = false
                                            authError = "Google Sign-In was cancelled. Tap Continue with Google whenever you are ready."
                                        }
                                        is GoogleSignInResult.Error -> {
                                            isSigningIn = false
                                            authError = res.message
                                        }
                                    }
                                }
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting with Google...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Google account"
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (authTabIndex == 0) {
                                        "Continue with Google"
                                    } else {
                                        "Sign up with Google"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { showAuthHelp = true },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("auth_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Account access & sign-in info")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showPrivacy = true },
                                modifier = Modifier.testTag("landing_privacy_button")
                            ) {
                                Text("Privacy Policy")
                            }
                            TextButton(
                                onClick = { showTerms = true },
                                modifier = Modifier.testTag("landing_terms_button")
                            ) {
                                Text("Terms of Service")
                            }
                            TextButton(
                                onClick = { showChangelog = true },
                                modifier = Modifier.testTag("landing_changelog_button")
                            ) {
                                Text("Release Notes")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Core Scientific Capabilities",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CapabilityItem(
                        icon = Icons.Filled.CameraAlt,
                        title = "Multi-Angle Visual Identification",
                        body = "Capture or upload up to 4 angles (leaf, flower, bark, habit) with crop & region context."
                    )
                    CapabilityItem(
                        icon = Icons.Filled.Science,
                        title = "Modular Deep Research Verification",
                        body = "Cross-checks GBIF, Kew POWO, World Flora Online & PubMed with transparent evidence ratings."
                    )
                    CapabilityItem(
                        icon = Icons.Filled.Shield,
                        title = "Safety-First Toxicity Alerts",
                        body = "Highlights human, pet, and livestock risks plus look-alike warnings before anything else."
                    )
                    CapabilityItem(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Ask FloraMind Botanical Assistant",
                        body = "Stream follow-up Q&A for Nigerian, African, European, Asian, and Americas climates."
                    )
                }
            }

            if (curatedPlants.isNotEmpty()) {
                item {
                    Text(
                        text = "Public Botanical Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explore curated species profiles before signing in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(curatedPlants.take(6), key = { it.id.ifBlank { it.scientificName } }) { plant ->
                            Card(
                                modifier = Modifier
                                    .width(230.dp)
                                    .clickable { selectedPreviewPlant = plant }
                                    .testTag("landing_preview_card_${plant.scientificName}"),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Eco,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        SafetyBadgeChip(SafetyLevel.fromString(plant.safetyLevel))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = plant.commonName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = plant.scientificName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = plant.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = BotanicalConstants.SAFETY_DISCLAIMER,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    selectedPreviewPlant?.let { plant ->
        AlertDialog(
            onDismissRequest = { selectedPreviewPlant = null },
            title = {
                Column {
                    Text(plant.commonName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${plant.scientificName} • ${plant.family}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SafetyBadgeChip(SafetyLevel.fromString(plant.safetyLevel))
                    Text("Region: ${plant.region}", style = MaterialTheme.typography.labelMedium)
                    Text(plant.summary, style = MaterialTheme.typography.bodyMedium)
                    if (plant.safetyNotice.isNotBlank()) {
                        Text(
                            "Safety: ${plant.safetyNotice}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPreviewPlant = null }) { Text("Close") }
            }
        )
    }

    if (showPrivacy) {
        PolicyTextDialog("Privacy Policy", BotanicalConstants.PRIVACY_POLICY_TEXT) {
            showPrivacy = false
        }
    }
    if (showTerms) {
        PolicyTextDialog("Terms of Service", BotanicalConstants.TERMS_OF_SERVICE_TEXT) {
            showTerms = false
        }
    }
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Announcements & Releases", style = MaterialTheme.typography.headlineSmall)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Current Version: ${appConfig.currentAppVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(appConfig.releaseNotes, style = MaterialTheme.typography.bodySmall)
                    announcements.take(4).forEach { ann ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(ann.title, style = MaterialTheme.typography.titleSmall)
                                Text(ann.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showChangelog = false }) { Text("Done") }
            }
        )
    }
    if (showAuthHelp) {
        AlertDialog(
            onDismissRequest = { showAuthHelp = false },
            title = { Text("Account Access & Privacy", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Google OAuth via Android Credential Manager lets you pick an existing Google account on your device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Your session stays signed in automatically across app restarts until you choose Sign Out.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Every scan, folder, and saved plant is isolated to your Firebase UID so other users cannot view your private herbarium.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• If you see 'No Google account found', add a Google account under Android Settings > Accounts and ensure Google Play Services is updated.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuthHelp = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun CapabilityItem(
    icon: ImageVector,
    title: String,
    body: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
