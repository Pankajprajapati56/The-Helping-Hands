package com.helpinghands.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.helpinghands.app.data.model.Member
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.components.DetailRow
import com.helpinghands.app.ui.components.SectionHeader
import com.helpinghands.app.ui.components.StatusChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    user: AppUser,
    repository: HelpingHandsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val dataVersion by repository.dataVersion.collectAsState()
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var selectedMemberForDetails by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(dataVersion) {
        members = repository.getMembers()
    }

    val filteredMembers = members.filter { m ->
        val matchesQuery = m.name.contains(searchQuery, ignoreCase = true) ||
                m.memberId.contains(searchQuery, ignoreCase = true) ||
                m.mobile.contains(searchQuery, ignoreCase = true) ||
                m.fatherHusbandName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterStatus) {
            "Active" -> m.status.equals("Active", ignoreCase = true)
            "Inactive" -> !m.status.equals("Active", ignoreCase = true)
            else -> true
        }

        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            if (user.role == "ADMIN") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_member_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
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

            // Search and count header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("सदस्य खोजें (Search by Name, ID, Mobile)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterStatus == "All",
                    onClick = { filterStatus = "All" },
                    label = { Text("सभी (${members.size})") }
                )
                FilterChip(
                    selected = filterStatus == "Active",
                    onClick = { filterStatus = "Active" },
                    label = { Text("सक्रिय (${members.count { it.status == "Active" }})") }
                )
                FilterChip(
                    selected = filterStatus == "Inactive",
                    onClick = { filterStatus = "Inactive" },
                    label = { Text("निष्क्रिय (${members.count { it.status != "Active" }})") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of Members
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMembers, key = { it.id }) { member ->
                    MemberItemCard(
                        member = member,
                        isAdmin = user.role == "ADMIN",
                        onEdit = { editingMember = member },
                        onClick = { selectedMemberForDetails = member }
                    )
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        AddOrEditMemberDialog(
            member = null,
            repository = repository,
            onDismiss = { showAddDialog = false },
            onSave = { newMember ->
                coroutineScope.launch {
                    repository.addMember(newMember, user.username)
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Member Dialog
    if (editingMember != null) {
        AddOrEditMemberDialog(
            member = editingMember,
            repository = repository,
            onDismiss = { editingMember = null },
            onSave = { updatedMember ->
                coroutineScope.launch {
                    repository.updateMember(updatedMember, user.username)
                    editingMember = null
                }
            }
        )
    }

    // Member Details Sheet / Dialog
    if (selectedMemberForDetails != null) {
        MemberDetailsDialog(
            member = selectedMemberForDetails!!,
            onDismiss = { selectedMemberForDetails = null }
        )
    }
}

@Composable
fun MemberItemCard(
    member: Member,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${member.memberId} • पिता/पति: ${member.fatherHusbandName.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "मो.: ${member.mobile.ifEmpty { "N/A" }} • व्यवसाय: ${member.occupation.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = member.status)
                if (isAdmin) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Member",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditMemberDialog(
    member: Member?,
    repository: HelpingHandsRepository,
    onDismiss: () -> Unit,
    onSave: (Member) -> Unit
) {
    val isEdit = member != null
    var memberId by remember { mutableStateOf(member?.memberId ?: "") }
    var name by remember { mutableStateOf(member?.name ?: "") }
    var fatherName by remember { mutableStateOf(member?.fatherHusbandName ?: "") }
    var mobile by remember { mutableStateOf(member?.mobile ?: "") }
    var nominee by remember { mutableStateOf(member?.nominee ?: "") }
    var occupation by remember { mutableStateOf(member?.occupation ?: "") }
    var status by remember { mutableStateOf(member?.status ?: "Active") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isEdit && memberId.isEmpty()) {
            memberId = repository.getNextMemberId()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "सदस्य विवरण संपादित करें (Edit)" else "नया सदस्य जोड़ें (Add Member)")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = memberId,
                    onValueChange = { memberId = it },
                    label = { Text("Member ID") },
                    singleLine = true,
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("सदस्य का नाम (Member Name) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("पिता/पति का नाम (Father/Husband)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाइल नंबर (Mobile)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nominee,
                    onValueChange = { nominee = it },
                    label = { Text("नामिनी (Nominee)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("व्यवसाय (Occupation)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("स्थिति (Status): $status")
                    Switch(
                        checked = status == "Active",
                        onCheckedChange = { status = if (it) "Active" else "Inactive" }
                    )
                }

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
                    if (name.trim().isEmpty()) {
                        errorText = "कृपया सदस्य का नाम भरें"
                    } else {
                        onSave(
                            Member(
                                id = member?.id ?: 0,
                                memberId = memberId.trim().ifEmpty { "THH000" },
                                name = name.trim(),
                                fatherHusbandName = fatherName.trim(),
                                mobile = mobile.trim(),
                                joiningDate = member?.joiningDate ?: "2026-01-01",
                                nominee = nominee.trim(),
                                occupation = occupation.trim(),
                                status = status
                            )
                        )
                    }
                }
            ) {
                Text("सुरक्षित करें (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें (Cancel)")
            }
        }
    )
}

@Composable
fun MemberDetailsDialog(
    member: Member,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(member.name)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("सदस्य ID:", member.memberId)
                DetailRow("पिता/पति का नाम:", member.fatherHusbandName.ifEmpty { "-" })
                DetailRow("मोबाइल:", member.mobile.ifEmpty { "-" })
                DetailRow("सदस्यता तिथि:", member.joiningDate)
                DetailRow("नामिनी:", member.nominee.ifEmpty { "-" })
                DetailRow("व्यवसाय:", member.occupation.ifEmpty { "-" })
                DetailRow("वर्तमान स्थिति:", member.status)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें (Close)")
            }
        }
    )
}
