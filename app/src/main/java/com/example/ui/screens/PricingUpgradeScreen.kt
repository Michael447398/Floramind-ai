package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BotanicalConstants
import com.example.model.BillingFlowState
import com.example.model.PlanType
import com.example.model.PlayProductLoadState
import com.example.model.PurchaseProvider
import com.example.model.SubscriptionStatus
import com.example.model.UserEntitlement
import com.example.ui.components.PlanBadgeChip
import com.example.ui.theme.BotanicalGold
import java.text.DateFormat
import java.util.Date

@Composable
fun PricingUpgradeScreen(
    entitlement: UserEntitlement,
    productLoadState: PlayProductLoadState,
    billingFlowState: BillingFlowState,
    onPurchasePlan: (Activity, PlanType) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismissBillingBanner: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val activity = context as? Activity
    val effectivePlan = entitlement.effectivePlan()
    val readyState = productLoadState as? PlayProductLoadState.Ready
    val isBusy = billingFlowState is BillingFlowState.Processing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "FloraMind Plans & Upgrade",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Linked to your Firebase account across Android & Web",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Billing Status Banner (Processing / Success / Pending / Cancelled / Error)
        if (billingFlowState !is BillingFlowState.Idle) {
            item {
                val (bg, fg, title, msg) = when (billingFlowState) {
                    is BillingFlowState.Processing -> listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        "Verifying Purchase",
                        billingFlowState.message
                    )
                    is BillingFlowState.Success -> listOf(
                        Color(0xFFE4F4EA),
                        Color(0xFF155E34),
                        "Upgrade Confirmed",
                        billingFlowState.message
                    )
                    is BillingFlowState.Pending -> listOf(
                        Color(0xFFFFF4DE),
                        Color(0xFF8A5600),
                        "Payment Pending",
                        billingFlowState.message
                    )
                    is BillingFlowState.Cancelled -> listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "Checkout Notice",
                        billingFlowState.message
                    )
                    is BillingFlowState.Error -> listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer,
                        "Billing Notice",
                        billingFlowState.message
                    )
                    BillingFlowState.Idle -> listOf(Color.Transparent, Color.Transparent, "", "")
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = bg as Color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("billing_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (billingFlowState is BillingFlowState.Processing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title as String,
                                style = MaterialTheme.typography.titleSmall,
                                color = fg as Color,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg as String,
                                style = MaterialTheme.typography.bodySmall,
                                color = fg
                            )
                        }
                        if (billingFlowState !is BillingFlowState.Processing) {
                            TextButton(onClick = onDismissBillingBanner) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }

        // Current Account Plan Card
        item {
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
                            text = "Current Account Plan",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PlanBadgeChip(entitlement = entitlement)
                    }
                    Text(
                        text = when (effectivePlan) {
                            PlanType.FREE -> "Free allowance: ${entitlement.formattedRemainingLabel()} (no expiration timer)."
                            PlanType.LIFETIME_PREMIUM -> "Permanent Lifetime Premium entitlement active on this account."
                            PlanType.PREMIUM_MONTHLY, PlanType.PREMIUM_ANNUAL -> {
                                val expiry = entitlement.expiresAt?.toDate()?.let {
                                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)
                                }
                                val statusText = entitlement.parsedStatus().label
                                if (expiry != null) "$statusText • Period ends $expiry" else statusText
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entitlement.parsedProvider() != PurchaseProvider.NONE) {
                        Text(
                            text = "Verified via ${entitlement.parsedProvider().displayName} • Ref: ${entitlement.purchaseReference.take(28)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Store configuration status notice if Play products / backend are not published yet
        if (readyState?.statusNote != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = readyState.statusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Plan Cards
        item {
            PlanOptionCard(
                title = "Premium Monthly",
                badgeText = "Recurring Monthly",
                priceLabel = readyState?.monthlyFormattedPrice ?: "Store price configured in Google Play Console",
                billingTerms = "Billed monthly via Google Play. Renews automatically unless cancelled at least 24 hours before renewal.",
                isCurrentPlan = effectivePlan == PlanType.PREMIUM_MONTHLY &&
                    entitlement.parsedStatus() != SubscriptionStatus.CANCELLED,
                isIncludedInLifetime = effectivePlan == PlanType.LIFETIME_PREMIUM,
                enabled = !isBusy && activity != null && effectivePlan != PlanType.LIFETIME_PREMIUM,
                onSelect = { activity?.let { onPurchasePlan(it, PlanType.PREMIUM_MONTHLY) } },
                testTagName = "upgrade_monthly_button"
            )
        }

        item {
            PlanOptionCard(
                title = "Premium Annual",
                badgeText = "Recurring Annual",
                priceLabel = readyState?.annualFormattedPrice ?: "Store price configured in Google Play Console",
                billingTerms = "Billed annually via Google Play. Renews automatically unless cancelled at least 24 hours before renewal.",
                isCurrentPlan = effectivePlan == PlanType.PREMIUM_ANNUAL &&
                    entitlement.parsedStatus() != SubscriptionStatus.CANCELLED,
                isIncludedInLifetime = effectivePlan == PlanType.LIFETIME_PREMIUM,
                highlighted = true,
                enabled = !isBusy && activity != null && effectivePlan != PlanType.LIFETIME_PREMIUM,
                onSelect = { activity?.let { onPurchasePlan(it, PlanType.PREMIUM_ANNUAL) } },
                testTagName = "upgrade_annual_button"
            )
        }

        item {
            PlanOptionCard(
                title = "Lifetime Premium",
                badgeText = "One-Time Payment",
                priceLabel = readyState?.lifetimeFormattedPrice ?: "Store price configured in Google Play Console",
                billingTerms = "Single one-time payment — no recurring charges. Permanent premium entitlement for your FloraMind AI account while the service remains operational.",
                isCurrentPlan = effectivePlan == PlanType.LIFETIME_PREMIUM,
                isIncludedInLifetime = false,
                highlighted = true,
                enabled = !isBusy && activity != null && effectivePlan != PlanType.LIFETIME_PREMIUM,
                onSelect = { activity?.let { onPurchasePlan(it, PlanType.LIFETIME_PREMIUM) } },
                testTagName = "upgrade_lifetime_button"
            )
        }

        // Plan Feature Comparison
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What Premium Unlocks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FeatureCheckRow("Unlimited plant identifications (vs. 10 total free scans)")
                    FeatureCheckRow("Advanced Plant Disease, Pest & Stress Analysis")
                    FeatureCheckRow("Full 12-section botanical reports including medicinal & pharmacology evidence")
                    FeatureCheckRow("Detailed soil, propagation, pruning & regional cultivation guides")
                    FeatureCheckRow("Complete Deep Research database citations (GBIF, Kew POWO, WFO, PubMed)")
                    FeatureCheckRow("PDF dossier export & expanded botanical workflow")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Free Plan Always Keeps: 10 successful identifications with basic ID & care info, full access to Settings, Privacy controls, and your existing Saved Collection & History.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Restore Purchases & Play Management
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRestorePurchases,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_purchases_button")
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Purchases & Sync Account Plan")
                }

                if (entitlement.parsedProvider() == PurchaseProvider.GOOGLE_PLAY &&
                    effectivePlan != PlanType.FREE &&
                    effectivePlan != PlanType.LIFETIME_PREMIUM
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manage_play_subscription_button")
                    ) {
                        Text("Manage or Cancel in Google Play")
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
                    text = BotanicalConstants.BILLING_TERMS_SUMMARY,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    badgeText: String,
    priceLabel: String,
    billingTerms: String,
    isCurrentPlan: Boolean,
    isIncludedInLifetime: Boolean,
    highlighted: Boolean = false,
    enabled: Boolean,
    onSelect: () -> Unit,
    testTagName: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlan) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (highlighted || isCurrentPlan) 1.5.dp else 1.dp,
            color = if (isCurrentPlan) MaterialTheme.colorScheme.primary
            else if (highlighted) BotanicalGold.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant
        )
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (title.contains("Lifetime")) Icons.Filled.AllInclusive else Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = if (title.contains("Lifetime")) BotanicalGold else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BotanicalGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A5600),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = priceLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = billingTerms,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onSelect,
                enabled = enabled && !isCurrentPlan && !isIncludedInLifetime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTagName)
            ) {
                when {
                    isCurrentPlan -> {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active on Your Account")
                    }
                    isIncludedInLifetime -> Text("Included in Your Lifetime Plan")
                    else -> Text("Continue with Google Play")
                }
            }
        }
    }
}

@Composable
private fun FeatureCheckRow(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
