package com.adhdfocus.app.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.R

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onSignUpClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val signInIntent by viewModel.signInIntent.collectAsStateWithLifecycle()

    // Launch Cognito hosted UI and handle the redirect result
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.data != null) {
            viewModel.handleAuthResult(result.data)
        } else {
            viewModel.clearSignInIntent()
        }
    }

    // When the intent is ready, launch it
    LaunchedEffect(signInIntent) {
        signInIntent?.let { launcher.launch(it) }
    }

    // Navigate on success
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) onSignInSuccess()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val outerPadding = when {
            maxWidth < 360.dp -> 20.dp
            maxWidth < 600.dp -> 32.dp
            else -> 48.dp
        }
        val logoSize = when {
            maxWidth < 360.dp -> 140.dp
            maxWidth < 600.dp -> 180.dp
            else -> 220.dp
        }
        val contentWidth = when {
            maxWidth < 360.dp -> maxWidth
            maxWidth < 600.dp -> 320.dp
            else -> 420.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .widthIn(max = contentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.kinspace_logo),
                contentDescription = "Kinspace logo",
                modifier = Modifier.size(logoSize)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Kinspace",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Family To Do's management",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = { viewModel.startSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 240.dp)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Sign in with Kinspace",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}
