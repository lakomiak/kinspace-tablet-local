package com.adhdfocus.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun LocalSetupScreen(
    onSetupCompleted: () -> Unit,
    viewModel: LocalSetupViewModel = hiltViewModel()
) {
    val householdName by viewModel.householdName.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var memberName by remember { mutableStateOf("") }
    var memberBirthDate by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ADHD_USER) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val contentWidth = if (maxWidth < 720.dp) maxWidth else 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .width(contentWidth)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set Up This Tablet",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "This local build keeps everything on the device. Add the family members who should use this tablet, then choose who it should open to first.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = householdName,
                onValueChange = viewModel::updateHouseholdName,
                label = { Text("Household name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
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
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = memberBirthDate,
                        onValueChange = { memberBirthDate = it },
                        label = { Text("Birthdate (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleButton(
                            label = "Member",
                            selected = selectedRole == UserRole.ADHD_USER,
                            onClick = { selectedRole = UserRole.ADHD_USER }
                        )
                        RoleButton(
                            label = "Caregiver",
                            selected = selectedRole == UserRole.CAREGIVER,
                            onClick = { selectedRole = UserRole.CAREGIVER }
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addMember(
                                displayName = memberName,
                                birthDate = memberBirthDate.toLocalDateOrNull(),
                                role = selectedRole
                            )
                            memberName = ""
                            memberBirthDate = ""
                            selectedRole = UserRole.ADHD_USER
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add member")
                    }
                }
            }

            Text(
                text = "Family Members",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (members.isEmpty()) {
                Text(
                    text = "No family members added yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                members.forEach { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
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
                                        member.birthDate?.let { append(" • $it") }
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(onClick = { viewModel.removeMember(member.id) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = { viewModel.completeSetup(onSetupCompleted) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Finish local setup")
                }
            }
        }
    }
}

@Composable
private fun RoleButton(
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
        Button(onClick = onClick, modifier = semanticsModifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = semanticsModifier) {
            Text(label)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}
