package com.adhdfocus.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.data.model.UserRole
import java.time.LocalDate

@Composable
fun FamilyManagementScreen(
    onBackClick: () -> Unit,
    viewModel: FamilyManagementViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val activeMemberId by viewModel.activeMemberId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ADHD_USER) }
    var editingMemberId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Family Members",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBackClick) {
                Text("Back")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Family Member",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birthdate (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RolePickerButton("Member", selectedRole == UserRole.ADHD_USER) {
                        selectedRole = UserRole.ADHD_USER
                    }
                    RolePickerButton("Caregiver", selectedRole == UserRole.CAREGIVER) {
                        selectedRole = UserRole.CAREGIVER
                    }
                }
                Button(
                    onClick = {
                        viewModel.addMember(name, birthDate.toLocalDateOrNull(), selectedRole)
                        name = ""
                        birthDate = ""
                        selectedRole = UserRole.ADHD_USER
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add member")
                }
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        members.forEach { member ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = member.displayName,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append(if (member.role == UserRole.CAREGIVER) "Caregiver" else "Member")
                                member.birthDate?.let { append(" | $it") }
                                if (activeMemberId == member.id) append(" | Active")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                editingMemberId = member.id
                                name = member.displayName
                                birthDate = member.birthDate?.toString().orEmpty()
                                selectedRole = member.role
                            }
                        ) {
                            Text("Edit")
                        }
                        if (activeMemberId != member.id) {
                            OutlinedButton(onClick = { viewModel.setActiveMember(member.id) }) {
                                Text("Set Active")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.removeMember(member.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }

    if (editingMemberId != null) {
        AlertDialog(
            onDismissRequest = {
                editingMemberId = null
                name = ""
                birthDate = ""
                selectedRole = UserRole.ADHD_USER
            },
            title = { Text("Edit Family Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("Birthdate (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RolePickerButton("Member", selectedRole == UserRole.ADHD_USER) {
                            selectedRole = UserRole.ADHD_USER
                        }
                        RolePickerButton("Caregiver", selectedRole == UserRole.CAREGIVER) {
                            selectedRole = UserRole.CAREGIVER
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingMemberId?.let {
                            viewModel.updateMember(it, name, birthDate.toLocalDateOrNull(), selectedRole)
                        }
                        editingMemberId = null
                        name = ""
                        birthDate = ""
                        selectedRole = UserRole.ADHD_USER
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingMemberId = null
                        name = ""
                        birthDate = ""
                        selectedRole = UserRole.ADHD_USER
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RolePickerButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val semanticsModifier = Modifier.semantics {
        role = Role.RadioButton
        this.selected = selected
        stateDescription = if (selected) "Selected" else "Not selected"
        contentDescription = "$label role"
    }
    if (selected) {
        Button(onClick = onClick, modifier = semanticsModifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = semanticsModifier) { Text(label) }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}
