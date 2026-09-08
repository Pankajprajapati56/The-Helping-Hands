package com.helpinghands.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.helpinghands.app.data.model.AppUser
import com.helpinghands.app.data.model.CommitteeSettings
import com.helpinghands.app.data.model.ContributionRate
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.DetailRow
import com.helpinghands.app.ui.components.SectionHeader
import com.helpinghands.app.ui.components.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: AppUser,
    repository: HelpingHandsRepository,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()

    var settings by remember { mutableStateOf<CommitteeSettings?>(null) }
    var rates by remember { mutableStateOf<List<ContributionRate>>(emptyList()) }
    var showAddRateDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dataVersion) {
        settings = repository.getSettings()
        rates = repository.getContributionRates()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Committee Profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = settings?.committeeName ?: "THE HELPING HANDS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings?.address ?: "Kanghatti Dist. Mandsaur (M.P.)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailRow("कुल लक्षित सदस्य:", "${settings?.totalMembersTarget ?: 30} सदस्य")
                    DetailRow("मासिक देय तिथि:", "हर माह की ${settings?.dueDay ?: 5} तारीख")
                    DetailRow("विलंब शुल्क (Late Penalty):", formatCurrency(settings?.latePenalty ?: 50.0))
                    DetailRow("मासिक ऋण ब्याज दर:", "${((settings?.monthlyInterestRate ?: 0.009) * 100)}% (Reducing Balance)")
                }
            }
        }

        // Contribution Rates Table
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "अंशदान दर इतिहास (Contribution Rates)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "नियम: पुरानी दर में संपादन नहीं होता, नई प्रभावी तिथि जोड़ें",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (user.role == "ADMIN") {
                    TextButton(onClick = { showAddRateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नई दर")
                    }
                }
            }
        }

        items(rates) { rate ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "प्रभावी तिथि: ${rate.effectiveFrom}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (rate.notes.isNotEmpty()) {
                            Text(
                                text = rate.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "₹${rate.amount.toInt()} / माह",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Current Session & Account
        item {
            SectionHeader(title = "उपयोगकर्ता एवं सत्र (Session)")
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "भूमिका (Role): ${user.role} • ID: ${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("लॉगआउट")
                    }
                }
            }
        }

        // Reset Data option for Admin
        if (user.role == "ADMIN") {
            item {
                SectionHeader(title = "डेटा रीसेट (Maintenance)")
            }

            item {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("डिफ़ॉल्ट डेटा पर रीसेट करें (Reset to Demo Data)")
                }
            }
        }
    }

    // Add Rate Dialog
    if (showAddRateDialog) {
        AddRateDialog(
            onDismiss = { showAddRateDialog = false },
            onSave = { date, amount, notes ->
                coroutineScope.launch {
                    repository.addContributionRate(
                        ContributionRate(effectiveFrom = date, amount = amount, notes = notes),
                        performedBy = user.username
                    )
                    showAddRateDialog = false
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("डेटा रीसेट की पुष्टि करें") },
            text = {
                Text("क्या आप वाकई सभी तालिकाओं को मूल डिफ़ॉल्ट डेटा (30 सदस्य, 2026 रिकॉर्ड) पर रीसेट करना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.resetToDefaults(user.username)
                            showResetConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हाँ, रीसेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRateDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var effectiveDate by remember { mutableStateOf("2026-08-01") }
    var amountText by remember { mutableStateOf("600") }
    var notes by remember { mutableStateOf("Revised monthly contribution") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नई अंशदान दर जोड़ें (Add Effective Rate)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "नियम: पुरानी दर में परिवर्तन न करें, केवल नई प्रभावी तिथि जोड़ें।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = effectiveDate,
                    onValueChange = { effectiveDate = it },
                    label = { Text("प्रभावी तिथि (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("नई मासिक दर (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("टिप्पणी (Notes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 600.0
                    onSave(effectiveDate, amt, notes)
                }
            ) {
                Text("सुरक्षित करें (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
