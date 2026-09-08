package com.helpinghands.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpinghands.app.data.model.AppUser
import com.helpinghands.app.data.repository.DashboardSummary
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.RuleNoticeBanner
import com.helpinghands.app.ui.components.SectionHeader
import com.helpinghands.app.ui.components.StatCard
import com.helpinghands.app.ui.components.formatCurrency
import com.helpinghands.app.ui.theme.TealLight
import com.helpinghands.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    user: AppUser,
    repository: HelpingHandsRepository,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(dataVersion) {
        isLoading = true
        summary = repository.getDashboardSummary()
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "THE HELPING HANDS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "कांगहट्टी • मंदसौर (म.प्र.)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (user.role == "ADMIN") "ADMIN" else user.username,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "वर्तमान मासिक अंशदान",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(summary?.currentMonthContributionRate ?: 500.0) + " / माह",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "वर्तमान कैश बैलेंस",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(summary?.cashBalance ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Rules Banner
        item {
            RuleNoticeBanner()
        }

        // Primary Metrics Grid
        item {
            SectionHeader(
                title = "वित्तीय सारांश (Financial Overview)",
                subtitle = "समिति के मुख्य वित्तीय आंकड़े"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "कुल सदस्य",
                    value = "${summary?.totalMembers ?: 30}",
                    subtitle = "${summary?.activeMembers ?: 30} सक्रिय सदस्य",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "कुल अंशदान जमा",
                    value = formatCurrency(summary?.totalContributionsCollected ?: 0.0),
                    subtitle = "मासिक अंशदान + पेनल्टी",
                    icon = Icons.Default.Savings,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "कुल दिया गया ऋण",
                    value = formatCurrency(summary?.totalLoansDisbursed ?: 0.0),
                    subtitle = "स्वीकृत ऋण राशि",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "बकाया ऋण",
                    value = formatCurrency(summary?.totalOutstandingLoan ?: 0.0),
                    subtitle = "शेष मूलधन",
                    icon = Icons.Default.TrendingDown,
                    iconBgColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "अर्जित ऋण ब्याज",
                    value = formatCurrency(summary?.totalInterestEarned ?: 0.0),
                    subtitle = "0.90% मासिक दर से",
                    icon = Icons.Default.Payments,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "कुल समिति खर्च",
                    value = formatCurrency(summary?.totalExpenses ?: 0.0),
                    subtitle = "स्टेशनरी व अन्य खर्च",
                    icon = Icons.Default.ReceiptLong,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Action Tiles
        item {
            SectionHeader(
                title = "त्वरित सुविधाएं (Modules)",
                subtitle = "समिति के सभी मॉड्यूल"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickNavCard(
                    title = "मासिक अंशदान (Contributions)",
                    description = "जनवरी–दिसंबर 2026 अंशदान रिकॉर्ड व पेनल्टी",
                    icon = Icons.Default.EventNote,
                    onClick = { onNavigate("contributions") }
                )
                QuickNavCard(
                    title = "ऋण एवं ईएमआई (Loans & EMI)",
                    description = "नया ऋण स्वीकृति, Reducing Balance EMI कैलकुलेटर व भुगतान",
                    icon = Icons.Default.AccountBalance,
                    onClick = { onNavigate("loans") }
                )
                QuickNavCard(
                    title = "रोकड़ बही (Cash Book)",
                    description = "नकद आवक-जावक (Cash In/Out) व खर्च विवरण",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = { onNavigate("cash_book") }
                )
                QuickNavCard(
                    title = "सदस्य सूची (Members)",
                    description = "सभी 30 सदस्यों का विवरण, स्थिति एवं संपर्क",
                    icon = Icons.Default.Group,
                    onClick = { onNavigate("members") }
                )
                QuickNavCard(
                    title = "वार्षिक रिपोर्ट (Annual Summary)",
                    description = "2026 वार्षिक वित्तीय सारांश व ऑडिट लॉग",
                    icon = Icons.Default.Assessment,
                    onClick = { onNavigate("reports") }
                )
            }
        }
    }
}

@Composable
fun QuickNavCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
