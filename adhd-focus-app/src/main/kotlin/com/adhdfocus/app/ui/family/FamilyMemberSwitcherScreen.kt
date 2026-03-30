package com.adhdfocus.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.adhdfocus.app.ui.common.util.UiConstants

/**
 * FamilyMemberSwitcherScreen displays the current family member and provides access to member selection.
 *
 * Shows:
 * - Current member indicator with avatar and name
 * - Button to open member selection modal
 * - Loading state while loading members
 * - Error messages if any
 */
@Composable
fun FamilyMemberSwitcherScreen(
    householdId: String,
    viewModel: FamilyMemberSwitcherViewModel = hiltViewModel()
) {
    val householdMembers by viewModel.householdMembers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isModalOpen by viewModel.isModalOpen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(householdId) {
        viewModel.loadHouseholdMembers(householdId)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiConstants.SPACING_MEDIUM)
        ) {
            // Current member display
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else if (currentUser != null) {
                CurrentMemberIndicator(
                    user = currentUser!!,
                    onTap = { viewModel.openMemberSelector() }
                )
            } else {
                Text(
                    text = "No member selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Error message display
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = UiConstants.SPACING_SMALL)
                )
            }
        }

        // Member selection modal
        if (isModalOpen) {
            FamilyMemberSelectionModal(
                members = householdMembers,
                currentUser = currentUser,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onMemberSelected = { userId, pin ->
                    viewModel.switchToMember(userId, pin)
                },
                onDismiss = { viewModel.closeMemberSelector() },
                onErrorDismiss = { viewModel.clearError() }
            )
        }
    }
}

/**
 * CurrentMemberIndicator displays the currently selected family member.
 *
 * Shows:
 * - Member avatar
 * - Member name
 * - Tap to open member selection
 */
@Composable
private fun CurrentMemberIndicator(
    user: com.adhdfocus.app.data.model.User,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(UiConstants.SPACING_MEDIUM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(UiConstants.SPACING_MEDIUM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar for ${user.displayName}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }

            // Name and role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.role.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Menu button to open member selection
        IconButton(onClick = onTap) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Switch family member",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
