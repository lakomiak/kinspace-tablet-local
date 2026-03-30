package com.adhdfocus.app.ui.common.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhdfocus.app.domain.sync.SyncStatus
import com.adhdfocus.app.ui.theme.CompletedGreen
import com.adhdfocus.app.ui.theme.ErrorLight
import com.adhdfocus.app.ui.theme.InProgressOrange
import com.adhdfocus.app.ui.theme.SurfaceLight

/**
 * SyncStatusIndicator displays the current cloud synchronization status.
 *
 * Shows:
 * - IDLE: No indicator or subtle indicator
 * - SYNCING: Animated spinner with "Syncing..." text
 * - SYNCED: Checkmark icon with "Synced" text
 * - ERROR: Error icon with "Sync Error" text
 * - OFFLINE: Offline icon with "Offline" text
 *
 * Validates: Requirements 10 - Cloud Synchronization with calendar-cloud
 */
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        SyncStatus.IDLE -> {
            // No indicator for IDLE state
        }

        SyncStatus.SYNCING -> {
            SyncingIndicator(modifier)
        }

        SyncStatus.SYNCED -> {
            SyncedIndicator(modifier)
        }

        SyncStatus.ERROR -> {
            ErrorIndicator(modifier)
        }

        SyncStatus.OFFLINE -> {
            OfflineIndicator(modifier)
        }
    }
}

@Composable
private fun SyncingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(InProgressOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = InProgressOrange,
            strokeWidth = 2.dp
        )
        Text(
            text = "Syncing...",
            fontSize = 12.sp,
            color = InProgressOrange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SyncedIndicator(modifier: Modifier = Modifier) {
    val animatedColor by animateColorAsState(
        targetValue = CompletedGreen,
        label = "syncedColorAnimation"
    )

    Row(
        modifier = modifier
            .background(CompletedGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Synced",
            modifier = Modifier.size(16.dp),
            tint = animatedColor
        )
        Text(
            text = "Synced",
            fontSize = 12.sp,
            color = animatedColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ErrorIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(ErrorLight.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Sync Error",
            modifier = Modifier.size(16.dp),
            tint = ErrorLight
        )
        Text(
            text = "Sync Error",
            fontSize = 12.sp,
            color = ErrorLight,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun OfflineIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(SurfaceLight, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Offline",
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Text(
            text = "Offline",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
