package com.adhdfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.ui.theme.AdhdfocusAppTheme
import com.adhdfocus.app.ui.theme.AdhdfocusAppThemeWithTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsStateWithLifecycle()
            AdhdfocusAppThemeWithTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdhdfocusApp()
                }
            }
        }
    }
}

@Composable
fun AdhdfocusApp() {
    Text("ADHD Focus App")
}

@Preview(showBackground = true)
@Composable
fun AdhdfocusAppPreview() {
    AdhdfocusAppTheme {
        AdhdfocusApp()
    }
}
