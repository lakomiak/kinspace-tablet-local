package com.adhdfocus.app.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for managing PIN protection for a user profile.
 *
 * Displays:
 * - Current PIN status
 * - "Set PIN" button if no PIN
 * - "Change PIN" and "Remove PIN" buttons if PIN is set
 * - Loading and error states
 * - Success messages
 *
 * @param userId User ID to manage PIN for
 * @param viewModel PinManagementViewModel
 */
@Composable
fun PinManagementScreen(
    userId: String,
    viewModel: PinManagementViewModel = hiltViewModel()
) {
    val pinStatus by viewModel.currentPinStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initialize(userId)
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage!!)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                "PIN Protection",
                style = MaterialTheme.typography.headlineSmall
            )

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Current Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            when (pinStatus) {
                                PinStatus.UNPROTECTED -> "No PIN protection"
                                PinStatus.PROTECTED -> "PIN protected"
                                PinStatus.CHANGING -> "Updating..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (pinStatus == PinStatus.CHANGING) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Error Message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Action Buttons
            when (pinStatus) {
                PinStatus.UNPROTECTED -> {
                    Button(
                        onClick = { showSetPinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Set PIN")
                    }
                }

                PinStatus.PROTECTED -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Change PIN")
                        }

                        OutlinedButton(
                            onClick = { showRemovePinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Remove PIN")
                        }
                    }
                }

                PinStatus.CHANGING -> {
                    // Buttons disabled during operation
                }
            }

            // Information
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "PIN Requirements",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        "• 4-8 digits, numeric only\n" +
                            "• Required to switch to this profile\n" +
                            "• Stored securely with SHA-256 hashing",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Dialogs
    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onConfirm = { pin ->
                viewModel.setupPin(pin)
                showSetPinDialog = false
            },
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { currentPin, newPin ->
                viewModel.changePin(currentPin, newPin)
                showChangePinDialog = false
            },
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

    if (showRemovePinDialog) {
        RemovePinDialog(
            onDismiss = { showRemovePinDialog = false },
            onConfirm = { pin ->
                viewModel.removePin(pin)
                showRemovePinDialog = false
            },
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }
}
