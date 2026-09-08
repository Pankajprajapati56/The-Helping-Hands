package com.helpinghands.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.helpinghands.app.data.model.MonthlyContribution
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.StatusChip
import com.helpinghands.app.ui.components.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionsScreen(
    user: AppUser,
    repository: HelpingHandsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()

    val months = remember {
        listOf(
            "January 2026", "February 2026", "March 2026", "April 2026",
            "May 2026", "June 2026", "July 2026", "August 2026",
            "September 2026", "October 2026", "November 2026", "December 2026"
        )
    }

    var selectedMonth by remember { mutableStateOf("March 2026") }
    var contributions by remember { mutableStateOf<List<MonthlyContribution>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") }

    var recordingPaymentFor by remember { mutableStateOf<MonthlyContribution?>(null) }

    LaunchedEffect(selectedMonth, dataVersion) {
        contributions = repository.getMonthlyContributions(selectedMonth)
    }

    val selectedMonthIndex = months.indexOf(selectedMonth)
    val applicableRate = if (selectedMonthIndex >= 7) 600.0 else 500.0

    val filteredList = contributions.filter { item ->
        val matchesQuery = item.memberName.contains(searchQuery, ignoreCase = true) ||
                item.memberId.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterStatus) {
            "PAID" -> item.status.equals("PAID", ignoreCase = true)
            "PENDING" -> !item.status.equals("PAID", ignoreCase = true)
            else -> true
        }

        matchesQuery && matchesFilter
    }

    val totalExpected = contributions.sumOf { it.applicableAmount }
    val totalCollected = contributions.sumOf { it.paidAmount }
    val totalPenalties = contributions.sumOf { it.penalty }
    val paidCount = contributions.count { it.status == "PAID" }
    val pendingCount = contributions.count { it.status != "PAID" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Month Selector Pills (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEachIndexed { idx, m ->
                val isSelected = m == selectedMonth
                val isAugustOnward = idx >= 7
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedMonth = m },
                    label = {
                        Text(
                            text = "${m.substringBefore(" ")} (${if (isAugustOnward) "₹600" else "₹500"})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedMonth अंशदान विवरण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "दर: ₹${applicableRate.toInt()} / सदस्य",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("कुल जमा", style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatCurrency(totalCollected),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text("पेनल्टी", style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatCurrency(totalPenalties),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("स्थिति", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$paidCount जमा / $pendingCount शेष",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search and Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("सदस्य खोजें (Search)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("contribution_search"),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = filterStatus == "PAID",
                onClick = { filterStatus = if (filterStatus == "PAID") "All" else "PAID" },
                label = { Text("जमा") }
            )

            FilterChip(
                selected = filterStatus == "PENDING",
                onClick = { filterStatus = if (filterStatus == "PENDING") "All" else "PENDING" },
                label = { Text("बाकी") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Contributions List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                ContributionItemCard(
                    item = item,
                    isAdmin = user.role == "ADMIN",
                    onRecordPayment = { recordingPaymentFor = item }
                )
            }
        }
    }

    // Payment Dialog
    if (recordingPaymentFor != null) {
        RecordContributionDialog(
            item = recordingPaymentFor!!,
            onDismiss = { recordingPaymentFor = null },
            onSave = { paidAmount, date, penalty, remarks ->
                coroutineScope.launch {
                    repository.recordContributionPayment(
                        memberId = recordingPaymentFor!!.memberId,
                        monthYear = recordingPaymentFor!!.monthYear,
                        paidAmount = paidAmount,
                        paymentDate = date,
                        penalty = penalty,
                        remarks = remarks,
                        performedBy = user.username
                    )
                    recordingPaymentFor = null
                }
            }
        )
    }
}

@Composable
fun ContributionItemCard(
    item: MonthlyContribution,
    isAdmin: Boolean,
    onRecordPayment: () -> Unit
) {
    val isPaid = item.status.equals("PAID", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                    contentDescription = null,
                    tint = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.memberName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.memberId} • देय राशि: ${formatCurrency(item.applicableAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPaid) {
                    Text(
                        text = "जमा: ${formatCurrency(item.paidAmount)}" +
                                if (item.penalty > 0) " + पेनल्टी: ${formatCurrency(item.penalty)}" else "" +
                                if (item.paymentDate.isNotEmpty()) " (${item.paymentDate})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = item.status)
                if (isAdmin) {
                    TextButton(
                        onClick = onRecordPayment,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (isPaid) "अपडेट" else "जमा करें")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordContributionDialog(
    item: MonthlyContribution,
    onDismiss: () -> Unit,
    onSave: (Double, String, Double, String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var paidAmountText by remember {
        mutableStateOf(if (item.paidAmount > 0) item.paidAmount.toInt().toString() else item.applicableAmount.toInt().toString())
    }
    var paymentDate by remember {
        mutableStateOf(if (item.paymentDate.isNotEmpty()) item.paymentDate else today)
    }
    var penaltyText by remember {
        mutableStateOf(item.penalty.toInt().toString())
    }
    var remarks by remember { mutableStateOf(item.remarks) }

    // Auto-calculate penalty if date entered has day > 5
    fun checkAutoPenalty(dateStr: String) {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val day = parts[2].toIntOrNull() ?: 1
                if (day > 5 && penaltyText == "0") {
                    penaltyText = "50"
                    if (remarks.isEmpty()) remarks = "Late payment after 5th"
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("अंशदान जमा करें (Record Contribution)")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${item.memberName} (${item.memberId}) • ${item.monthYear}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("जमा राशि (Amount Paid)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = {
                        paymentDate = it
                        checkAutoPenalty(it)
                    },
                    label = { Text("जमा तारीख (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-03-05") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = penaltyText,
                    onValueChange = { penaltyText = it },
                    label = { Text("विलंब शुल्क/पेनल्टी (Late Fee)") },
                    supportingText = { Text("नियम: 5 तारीख के बाद ₹50 पेनल्टी") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("टिप्पणी (Remarks)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = paidAmountText.toDoubleOrNull() ?: item.applicableAmount
                    val pen = penaltyText.toDoubleOrNull() ?: 0.0
                    onSave(amt, paymentDate, pen, remarks)
                }
            ) {
                Text("जमा दर्ज करें (Submit)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
