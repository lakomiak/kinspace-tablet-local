package com.adhdfocus.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SessionTimeoutWarningDialog displays a warning before session timeout.
 *
 * Features:
 * - Display warning message
 * - Show time remaining before logout
 * - Provide "Extend Session" button
 * - Provide "Logout" button
 * - Auto-dismiss on logout
 *
 * @param isVisible Whether the dialog is visible
 * @param timeRemaining Time remaining before logout (in seconds)
 * @param onExtendSession Callback when user extends session
 * @param onLogout Callback when user chooses to logout
 */
@Composable
fun SessionTimeoutWarningDialog(
    isVisible: Boolean,
    timeRemaining: Long,
    onExtendSession: () -> Unit,
    onLogout: () -> Unit
) {
    if (!isVisible) return

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeText = if (minutes > 0) {
        "$minutes minute${if (minutes > 1) "s" else ""} $seconds second${if (seconds != 1L) "s" else ""}"
    } else {
        "$seconds second${if (seconds != 1L) "s" else ""}"
    }

    AlertDialog(
        onDismissRequest = { /* Prevent dismissal by clicking outside */ },
        title = {
            Text(
                text = "Session Timeout Warning",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your session will expire in:",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                // Time remaining display
                Text(
                    text = timeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .fillMaxWidth()
                )

                Text(
                    text = "You will be automatically logged out due to inactivity. Tap 'Extend Session' to continue.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onExtendSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Extend Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Logout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        },
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    )
}

/**
 * SessionTimeoutWarningDialogWithButtons displays a warning with both buttons in a row.
 *
 * This variant is useful for tablet screens where horizontal layout is preferred.
 *
 * @param isVisible Whether the dialog is visible
 * @param timeRemaining Time remaining before logout (in seconds)
 * @param onExtendSession Callback when user extends session
 * @param onLogout Callback when user chooses to logout
 */
@Composable
fun SessionTimeoutWarningDialogWithButtons(
    isVisible: Boolean,
    timeRemaining: Long,
    onExtendSession: () -> Unit,
    onLogout: () -> Unit
) {
    if (!isVisible) return

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeText = if (minutes > 0) {
        "$minutes minute${if (minutes > 1) "s" else ""} $seconds second${if (seconds != 1L) "s" else ""}"
    } else {
        "$seconds second${if (seconds != 1L) "s" else ""}"
    }

    AlertDialog(
        onDismissRequest = { /* Prevent dismissal by clicking outside */ },
        title = {
            Text(
                text = "Session Timeout Warning",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your session will expire in:",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                // Time remaining display
                Text(
                    text = timeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .fillMaxWidth()
                )

                Text(
                    text = "You will be automatically logged out due to inactivity. Tap 'Extend Session' to continue.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onExtendSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = "Extend Session",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = null,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    )
}
