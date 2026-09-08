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
import com.helpinghands.app.data.model.CashTransaction
import com.helpinghands.app.data.model.Expense
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.StatusChip
import com.helpinghands.app.ui.components.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookScreen(
    user: AppUser,
    repository: HelpingHandsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()

    var transactions by remember { mutableStateOf<List<CashTransaction>>(emptyList()) }
    var filterType by remember { mutableStateOf("All") }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dataVersion) {
        transactions = repository.getCashTransactions()
    }

    val filteredList = transactions.filter { t ->
        when (filterType) {
            "Cash In" -> t.txnType == "Cash In"
            "Cash Out" -> t.txnType == "Cash Out"
            else -> true
        }
    }

    val totalCashIn = transactions.filter { it.txnType == "Cash In" }.sumOf { it.amount }
    val totalCashOut = transactions.filter { it.txnType == "Cash Out" }.sumOf { it.amount }
    val currentBalance = totalCashIn - totalCashOut

    Scaffold(
        floatingActionButton = {
            if (user.role == "ADMIN") {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = "Add Expense")
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

            // Cash Balance Overview Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "रोकड़ बही (Cash Book Ledger)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "कांगहट्टी समिति नकद आवक-जावक रजिस्टर",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "शेष: " + formatCurrency(currentBalance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("कुल आवक (Cash In)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                formatCurrency(totalCashIn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("कुल जावक (Cash Out)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                formatCurrency(totalCashOut),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "All",
                    onClick = { filterType = "All" },
                    label = { Text("सभी प्रविष्टियाँ (${transactions.size})") }
                )
                FilterChip(
                    selected = filterType == "Cash In",
                    onClick = { filterType = "Cash In" },
                    label = { Text("आवक (Cash In)") }
                )
                FilterChip(
                    selected = filterType == "Cash Out",
                    onClick = { filterType = "Cash Out" },
                    label = { Text("जावक/खर्च (Cash Out)") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transactions Ledger
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { txn ->
                    CashTransactionCard(txn = txn)
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { date, category, desc, amt ->
                coroutineScope.launch {
                    repository.addExpense(
                        Expense(
                            expenseDate = date,
                            category = category,
                            description = desc,
                            amount = amt
                        ),
                        performedBy = user.username
                    )
                    showAddExpenseDialog = false
                }
            }
        )
    }
}

@Composable
fun CashTransactionCard(txn: CashTransaction) {
    val isIn = txn.txnType == "Cash In"

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
                    .background(if (isIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = txn.category.ifEmpty { txn.txnType },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = (if (isIn) "+ " else "- ") + formatCurrency(txn.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = txn.reference.ifEmpty { "Cash Transaction" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = txn.txnDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "शेष: " + formatCurrency(txn.balance),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val categories = remember {
        listOf(
            "Stationery",
            "Meeting/Refreshment",
            "Office Expense",
            "Welfare/Event",
            "Bank Charges",
            "Printing",
            "Other"
        )
    }

    var date by remember { mutableStateOf(today) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया खर्च दर्ज करें (Add Expense)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("खर्च तिथि (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("खर्च श्रेणी (Category)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("राशि (Amount ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("खर्च का विवरण (Description)") },
                    placeholder = { Text("e.g. मीटिंग चाय नाश्ता या स्टेशनरी") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorText = "कृपया सही राशि भरें"
                    } else {
                        onSave(date, selectedCategory, description.ifEmpty { selectedCategory }, amt)
                    }
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
