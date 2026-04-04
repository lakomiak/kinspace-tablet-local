package com.adhdfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.ui.achievements.AchievementsView
import com.adhdfocus.app.ui.auth.SignInScreen
import com.adhdfocus.app.ui.focus.DailyFocusViewScreen
import com.adhdfocus.app.ui.settings.SettingsScreen
import com.adhdfocus.app.ui.setup.MemberSelectionScreen
import com.adhdfocus.app.ui.theme.AdhdfocusAppThemeWithTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeManager: ThemeManager
    @Inject lateinit var setupManager: TabletSetupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsStateWithLifecycle()
            AdhdfocusAppThemeWithTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "signin") {

                        composable("signin") {
                            SignInScreen(
                                onSignInSuccess = {
                                    // After sign-in: check if tablet has been assigned to a member
                                    if (setupManager.isSetupComplete()) {
                                        navController.navigate("focus") {
                                            popUpTo("signin") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("member_selection") {
                                            popUpTo("signin") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("member_selection") {
                            MemberSelectionScreen(
                                onMemberSelected = {
                                    navController.navigate("focus") {
                                        popUpTo("member_selection") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("focus") {
                            DailyFocusViewScreen(
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToAchievements = { navController.navigate("achievements") },
                                onCreateTask = { /* TODO */ },
                                onTaskClick = { /* TODO */ }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                userId = setupManager.getAssignedMemberId() ?: "",
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("achievements") {
                            AchievementsView(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
