package com.helpinghands.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.helpinghands.app.data.model.Loan
import com.helpinghands.app.data.model.LoanScheduleRow
import com.helpinghands.app.data.model.Member
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.DetailRow
import com.helpinghands.app.ui.components.SectionHeader
import com.helpinghands.app.ui.components.StatusChip
import com.helpinghands.app.ui.components.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    user: AppUser,
    repository: HelpingHandsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()

    var loans by remember { mutableStateOf<List<Loan>>(emptyList()) }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var filterStatus by remember { mutableStateOf("All") }

    var showNewLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanForSchedule by remember { mutableStateOf<Loan?>(null) }
    var selectedLoanForPayment by remember { mutableStateOf<Loan?>(null) }

    LaunchedEffect(dataVersion) {
        loans = repository.getLoans()
        members = repository.getMembers()
    }

    val filteredLoans = loans.filter { l ->
        when (filterStatus) {
            "ACTIVE" -> l.status.equals("ACTIVE", ignoreCase = true)
            "CLOSED" -> l.status.equals("CLOSED", ignoreCase = true)
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            if (user.role == "ADMIN") {
                FloatingActionButton(
                    onClick = { showNewLoanDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_loan_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Loan")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Method information header
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ऋण गणना पद्धति: Reducing Balance (घटती शेष पद्धति)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "मासिक ब्याज दर: 0.90% | हर माह का ब्याज = चालू बकाया मूलधन × 0.90%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == "All",
                    onClick = { filterStatus = "All" },
                    label = { Text("सभी ऋण (${loans.size})") }
                )
                FilterChip(
                    selected = filterStatus == "ACTIVE",
                    onClick = { filterStatus = "ACTIVE" },
                    label = { Text("चालू (${loans.count { it.status == "ACTIVE" }})") }
                )
                FilterChip(
                    selected = filterStatus == "CLOSED",
                    onClick = { filterStatus = "CLOSED" },
                    label = { Text("पूर्ण/बंद (${loans.count { it.status == "CLOSED" }})") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Loans List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLoans, key = { it.id }) { loan ->
                    LoanItemCard(
                        loan = loan,
                        isAdmin = user.role == "ADMIN",
                        onViewSchedule = { selectedLoanForSchedule = loan },
                        onRecordRepayment = { selectedLoanForPayment = loan }
                    )
                }
            }
        }
    }

    // New Loan Dialog
    if (showNewLoanDialog) {
        NewLoanDialog(
            members = members,
            repository = repository,
            onDismiss = { showNewLoanDialog = false },
            onSave = { mId, principal, term, rate, date, purpose ->
                coroutineScope.launch {
                    repository.createLoan(
                        memberId = mId,
                        principal = principal,
                        termMonths = term,
                        monthlyRate = rate,
                        startDate = date,
                        purpose = purpose,
                        performedBy = user.username
                    )
                    showNewLoanDialog = false
                }
            }
        )
    }

    // Loan Schedule Dialog
    if (selectedLoanForSchedule != null) {
        LoanScheduleDialog(
            loan = selectedLoanForSchedule!!,
            repository = repository,
            onDismiss = { selectedLoanForSchedule = null }
        )
    }

    // Loan Payment Dialog
    if (selectedLoanForPayment != null) {
        RecordLoanPaymentDialog(
            loan = selectedLoanForPayment!!,
            onDismiss = { selectedLoanForPayment = null },
            onSave = { date, total, interest, principal, notes ->
                coroutineScope.launch {
                    repository.recordLoanPayment(
                        loanId = selectedLoanForPayment!!.id,
                        paymentDate = date,
                        totalAmount = total,
                        interestComponent = interest,
                        principalComponent = principal,
                        notes = notes,
                        performedBy = user.username
                    )
                    selectedLoanForPayment = null
                }
            }
        )
    }
}

@Composable
fun LoanItemCard(
    loan: Loan,
    isAdmin: Boolean,
    onViewSchedule: () -> Unit,
    onRecordRepayment: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${loan.memberName} (${loan.memberId})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Loan ID: ${loan.loanId} • प्रारंभ: ${loan.startDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(status = loan.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("स्वीकृत मूलधन", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatCurrency(loan.principal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("मासिक ईएमआई", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatCurrency(loan.emi),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("बकाया मूलधन", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatCurrency(loan.outstandingPrincipal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "अवधि: ${loan.termMonths} माह | जमा मूलधन: ${formatCurrency(loan.totalPaidPrincipal)} | जमा ब्याज: ${formatCurrency(loan.totalPaidInterest)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (loan.purpose.isNotEmpty()) {
                Text(
                    text = "उद्देश्य: ${loan.purpose}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewSchedule,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EMI तालिका")
                }

                if (isAdmin && loan.status == "ACTIVE") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRecordRepayment,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("किस्त जमा करें")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanDialog(
    members: List<Member>,
    repository: HelpingHandsRepository,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, Double, String, String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.memberId ?: "") }
    var principalText by remember { mutableStateOf("25000") }
    var termText by remember { mutableStateOf("12") }
    var monthlyRateText by remember { mutableStateOf("0.009") } // 0.90%
    var startDate by remember { mutableStateOf(today) }
    var purpose by remember { mutableStateOf("कृषि उपकरण / व्यवसाय विस्तार") }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val term = termText.toIntOrNull() ?: 12
    val rate = monthlyRateText.toDoubleOrNull() ?: 0.009

    val estimatedEmi = remember(principal, term, rate) {
        repository.calculateEMI(principal, term, rate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया ऋण स्वीकृति (New Loan)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Member Selector
                Text("ऋणी सदस्य चुनें (Select Member):", style = MaterialTheme.typography.labelMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val currentMember = members.find { it.memberId == selectedMemberId }
                    OutlinedTextField(
                        value = if (currentMember != null) "${currentMember.name} (${currentMember.memberId})" else selectedMemberId,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.name} (${m.memberId})") },
                                onClick = {
                                    selectedMemberId = m.memberId
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("ऋण राशि (Principal ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = termText,
                        onValueChange = { termText = it },
                        label = { Text("अवधि (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlyRateText,
                        onValueChange = { monthlyRateText = it },
                        label = { Text("मासिक ब्याज दर") },
                        supportingText = { Text("0.009 = 0.90%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("स्वीकृति तिथि (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("ऋण का उद्देश्य (Purpose)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // EMI Live Preview Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "अनुमानित मासिक EMI: ${formatCurrency(estimatedEmi)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Reducing Balance पद्धति से कुल $term महीने की किश्तें",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMemberId.isNotEmpty() && principal > 0) {
                        onSave(selectedMemberId, principal, term, rate, startDate, purpose)
                    }
                }
            ) {
                Text("ऋण स्वीकृत करें (Submit)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun LoanScheduleDialog(
    loan: Loan,
    repository: HelpingHandsRepository,
    onDismiss: () -> Unit
) {
    val schedule = remember(loan) {
        repository.generateLoanSchedule(
            principal = loan.principal,
            termMonths = loan.termMonths,
            monthlyRate = loan.interestRateMonthly,
            startDate = loan.startDate
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ऋण किश्त तालिका (EMI Schedule)")
                Text(
                    text = "${loan.memberName} (${loan.loanId}) • मूलधन: ${formatCurrency(loan.principal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("माह", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("शुरुआती शेष", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("किश्त (EMI)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("ब्याज (0.9%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text("अंतिम शेष", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(schedule) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${row.monthIndex}. ${row.monthLabel.take(3)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("₹${row.openingBalance.toInt()}", style = MaterialTheme.typography.bodySmall)
                        Text("₹${row.emi.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("₹${row.interestComponent.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("₹${row.closingBalance.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें (Close)")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordLoanPaymentDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, String) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val currentInterest = remember(loan) {
        Math.round(loan.outstandingPrincipal * loan.interestRateMonthly * 100.0) / 100.0
    }
    val suggestedPrincipal = remember(loan, currentInterest) {
        (loan.emi - currentInterest).coerceAtMost(loan.outstandingPrincipal).coerceAtLeast(0.0)
    }

    var paymentDate by remember { mutableStateOf(today) }
    var totalAmountText by remember { mutableStateOf(loan.emi.toInt().toString()) }
    var interestText by remember { mutableStateOf(currentInterest.toInt().toString()) }
    var principalText by remember { mutableStateOf(suggestedPrincipal.toInt().toString()) }
    var notes by remember { mutableStateOf("Monthly EMI Repayment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ऋण किश्त जमा करें (Loan Repayment)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${loan.memberName} (${loan.loanId}) • शेष मूलधन: ${formatCurrency(loan.outstandingPrincipal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("भुगतान तिथि (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("कुल जमा राशि (Total EMI ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("मूलधन भाग (Principal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = { Text("ब्याज भाग (Interest)") },
                        supportingText = { Text("0.90% of ${formatCurrency(loan.outstandingPrincipal)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("विवरण / टिप्पणी (Notes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = totalAmountText.toDoubleOrNull() ?: loan.emi
                    val intr = interestText.toDoubleOrNull() ?: currentInterest
                    val prin = principalText.toDoubleOrNull() ?: (tot - intr)
                    onSave(paymentDate, tot, intr, prin, notes)
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
