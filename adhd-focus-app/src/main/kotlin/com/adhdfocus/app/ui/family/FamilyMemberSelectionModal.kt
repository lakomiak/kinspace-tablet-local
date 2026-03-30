package com.adhdfocus.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.ui.common.util.UiConstants

/**
 * FamilyMemberSelectionModal displays all household members and allows switching between them.
 *
 * Features:
 * - List of all household members with avatars and names
 * - Current member highlighted
 * - One-tap switching between members
 * - PIN entry dialog for protected profiles
 * - Loading state during switch
 * - Error message display
 */
@Composable
fun FamilyMemberSelectionModal(
    members: List<User>,
    currentUser: User?,
    isLoading: Boolean,
    errorMessage: String?,
    onMemberSelected: (userId: String, pin: String?) -> Unit,
    onDismiss: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    var selectedMemberForPin by remember { mutableStateOf<User?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UiConstants.SPACING_LARGE)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Family Member",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close member selection"
                        )
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = UiConstants.SPACING_MEDIUM)
                    )
                }

                // Member list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(UiConstants.SPACING_LARGE),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (members.isEmpty()) {
                    Text(
                        text = "No household members found",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(UiConstants.SPACING_MEDIUM)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = UiConstants.SPACING_MEDIUM),
                        verticalArrangement = Arrangement.spacedBy(UiConstants.SPACING_SMALL)
                    ) {
                        items(members) { member ->
                            FamilyMemberCard(
                                member = member,
                                isCurrentMember = member.id == currentUser?.id,
                                isPinProtected = member.isPinProtected,
                                onTap = {
                                    if (member.isPinProtected) {
                                        selectedMemberForPin = member
                                        showPinDialog = true
                                    } else {
                                        onMemberSelected(member.id, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // PIN entry dialog
    if (showPinDialog && selectedMemberForPin != null) {
        PinEntryDialog(
            memberName = selectedMemberForPin!!.displayName,
            onPinSubmit = { pin ->
                onMemberSelected(selectedMemberForPin!!.id, pin)
                showPinDialog = false
                selectedMemberForPin = null
            },
            onDismiss = {
                showPinDialog = false
                selectedMemberForPin = null
            }
        )
    }
}

/**
 * FamilyMemberCard displays an individual family member.
 *
 * Shows:
 * - Avatar (placeholder or actual image)
 * - Display name
 * - User role
 * - PIN protection indicator (lock icon if protected)
 * - Current member indicator (checkmark or highlight)
 */
@Composable
fun FamilyMemberCard(
    member: User,
    isCurrentMember: Boolean,
    isPinProtected: Boolean,
    onTap: () -> Unit
) {
    val backgroundColor = if (isCurrentMember) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = backgroundColor,
        onClick = onTap
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiConstants.SPACING_MEDIUM),
            horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACING_MEDIUM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (member.avatarUrl != null) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = "Avatar for ${member.displayName}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }

            // Member info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiConstants.SPACING_EXTRA_SMALL)
            ) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.role.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PIN protection indicator
            if (isPinProtected) {
                Text(
                    text = "🔒",
                    modifier = Modifier.padding(end = UiConstants.SPACING_SMALL)
                )
            }

            // Current member indicator
            if (isCurrentMember) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
