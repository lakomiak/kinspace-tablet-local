package com.adhdfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.ui.achievements.AchievementsView
import com.adhdfocus.app.ui.auth.SignInScreen
import com.adhdfocus.app.ui.focus.CreateTodoScreen
import com.adhdfocus.app.ui.focus.DailyFocusViewScreen
import com.adhdfocus.app.ui.settings.SettingsScreen
import com.adhdfocus.app.ui.setup.MemberSelectionScreen
import com.adhdfocus.app.ui.timer.TimerScreen
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
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination
                    var focusRefreshToken by remember { mutableStateOf(0) }
                    val showChrome = currentDestination?.route != "signin" && currentDestination?.route != "member_selection"

                    Scaffold(
                        bottomBar = {
                            if (showChrome) {
                                NavigationBar {
                                    val tabs = listOf(
                                        Triple("focus", "Home", Icons.Default.Home),
                                        Triple("achievements", "Achievements", Icons.Default.Favorite),
                                        Triple("settings", "Settings", Icons.Default.Settings)
                                    )
                                    tabs.forEach { (route, label, icon) ->
                                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(imageVector = icon, contentDescription = label) },
                                            label = { androidx.compose.material3.Text(label) }
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (currentDestination?.route == "focus") {
                                FloatingActionButton(
                                    onClick = { navController.navigate("create_todo") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Create To Do"
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "signin",
                            modifier = Modifier.padding(paddingValues)
                        ) {

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
                                    memberName = setupManager.getAssignedMemberName(),
                                    refreshToken = focusRefreshToken,
                                    onTimerStartRequested = { task ->
                                        val durationSeconds = ((task.timerDurationMs ?: 0L) / 1000L).toInt()
                                        if (durationSeconds > 0) {
                                            navController.navigate("timer/${Uri.encode(task.id)}/$durationSeconds")
                                        }
                                    }
                                )
                            }

                            composable("create_todo") {
                                CreateTodoScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onCreateSuccess = {
                                        focusRefreshToken += 1
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    userId = setupManager.getAssignedMemberId() ?: "",
                                    onChangeMemberClick = {
                                        setupManager.resetSetup()
                                        navController.navigate("member_selection") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("achievements") {
                                AchievementsView(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("timer/{taskId}/{durationSeconds}") { backStackEntry ->
                                val taskId = Uri.decode(backStackEntry.arguments?.getString("taskId").orEmpty())
                                val durationSeconds = backStackEntry.arguments
                                    ?.getString("durationSeconds")
                                    ?.toIntOrNull()
                                    ?: 0
                                TimerScreen(
                                    taskId = taskId,
                                    initialDurationSeconds = durationSeconds,
                                    onTaskCompleted = { navController.popBackStack("focus", false) },
                                    onCancel = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
