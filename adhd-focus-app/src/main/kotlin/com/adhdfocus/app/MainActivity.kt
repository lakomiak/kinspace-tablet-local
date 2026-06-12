package com.adhdfocus.app

import android.view.accessibility.AccessibilityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.app.ActivityManager
import android.os.SystemClock
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
import com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.ui.common.AffirmationViewModel
import com.adhdfocus.app.ui.common.component.AffirmationDisplay
import com.adhdfocus.app.ui.achievements.AchievementsView
import com.adhdfocus.app.ui.family.FamilyManagementScreen
import com.adhdfocus.app.ui.focus.CreateTodoScreen
import com.adhdfocus.app.ui.focus.DailyFocusViewScreen
import com.adhdfocus.app.ui.reports.ReportsScreen
import com.adhdfocus.app.ui.settings.SettingsScreen
import com.adhdfocus.app.ui.setup.LocalSetupScreen
import com.adhdfocus.app.ui.setup.MemberSelectionScreen
import com.adhdfocus.app.ui.timer.TimerScreen
import com.adhdfocus.app.ui.welcome.WelcomeScreen
import com.adhdfocus.app.ui.theme.AdhdfocusAppThemeWithTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeManager: ThemeManager
    @Inject lateinit var setupManager: TabletSetupManager
    @Inject lateinit var categoryReminderScheduler: CategoryReminderScheduler
    private var accessibilitySetupGraceUntilMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        applyKioskModePolicy()
        lifecycleScope.launch {
            runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
        }
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsStateWithLifecycle()
            AdhdfocusAppThemeWithTheme(theme = currentTheme) {
                val affirmationViewModel: AffirmationViewModel = hiltViewModel()
                val affirmation by affirmationViewModel.affirmationEvent.collectAsStateWithLifecycle()
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = backStackEntry?.destination
                        var focusRefreshToken by remember { mutableStateOf(0) }
                        val showChrome = currentDestination?.route != "welcome" &&
                            currentDestination?.route != "local_setup" &&
                            currentDestination?.route != "member_selection"
                        val startDestination = "welcome"

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
                                startDestination = startDestination,
                                modifier = Modifier.padding(paddingValues)
                            ) {

                            composable("welcome") {
                                val isSetupComplete = remember {
                                    setupManager.isSetupComplete() && !setupManager.getHouseholdId().isNullOrBlank()
                                }
                                WelcomeScreen(
                                    isSetupComplete = isSetupComplete,
                                    onContinueClick = {
                                        val nextRoute = when {
                                            setupManager.getHouseholdId().isNullOrBlank() -> "local_setup"
                                            setupManager.isSetupComplete() -> "focus"
                                            else -> "member_selection"
                                        }
                                        navController.navigate(nextRoute) {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("local_setup") {
                                LocalSetupScreen(
                                    onSetupCompleted = {
                                        navController.navigate("focus") {
                                            popUpTo("local_setup") { inclusive = true }
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
                                    householdId = setupManager.getHouseholdId().orEmpty(),
                                    memberId = setupManager.getAssignedMemberId().orEmpty(),
                                    memberName = setupManager.getAssignedMemberName().orEmpty(),
                                    refreshToken = focusRefreshToken,
                                    onChangeMemberClick = {
                                        navController.navigate("member_selection") {
                                            popUpTo("focus") { inclusive = false }
                                        }
                                    },
                                    onTaskEditRequested = { task ->
                                        navController.navigate("edit_todo/${Uri.encode(task.id)}")
                                    },
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
                                    onSaveSuccess = {
                                        focusRefreshToken += 1
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("edit_todo/{taskId}") { backStackEntry ->
                                val taskId = Uri.decode(backStackEntry.arguments?.getString("taskId").orEmpty())
                                CreateTodoScreen(
                                    taskId = taskId,
                                    onBackClick = { navController.popBackStack() },
                                    onSaveSuccess = {
                                        focusRefreshToken += 1
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    userId = setupManager.getAssignedMemberId() ?: "",
                                    onViewReportsClick = {
                                        navController.navigate("reports")
                                    },
                                    onManageFamilyClick = {
                                        navController.navigate("family_management")
                                    },
                                    onChangeMemberClick = {
                                        navController.navigate("member_selection") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    },
                                    onRestartAppClick = {
                                        restartApplication()
                                    },
                                    onOpenAccessibilitySettingsClick = {
                                        openAccessibilitySettings()
                                    },
                                    onBackClick = {
                                        focusRefreshToken += 1
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("achievements") {
                                AchievementsView(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("reports") {
                                ReportsScreen(
                                    householdId = setupManager.getHouseholdId().orEmpty(),
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("family_management") {
                                FamilyManagementScreen(
                                    onBackClick = { navController.popBackStack() }
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
                    AffirmationDisplay(
                        affirmation = affirmation,
                        onDismiss = { affirmationViewModel.dismissAffirmation() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                    BatteryStatusPill(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .offset(y = (-4).dp)
                            .padding(end = 4.dp)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyKioskModePolicy()
    }

    private fun applyKioskModePolicy() {
        runCatching {
            if (!BuildConfig.ENABLE_KIOSK_MODE) {
                stopLockTaskIfActive()
                return@runCatching
            }

            ensureLockTaskPackagesIfDeviceOwner()

            val activityManager = getSystemService(ActivityManager::class.java)
            val lockTaskState = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
            if (shouldBypassKioskForAccessibility()) {
                if (lockTaskState != ActivityManager.LOCK_TASK_MODE_NONE) {
                    stopLockTaskIfActive()
                }
            } else {
                if (lockTaskState == ActivityManager.LOCK_TASK_MODE_NONE && canEnterDedicatedLockTask()) {
                    startLockTask()
                }
            }
        }
    }

    private fun ensureLockTaskPackagesIfDeviceOwner() {
        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java) ?: return
        val admin = ComponentName(this, KinspaceDeviceAdminReceiver::class.java)
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            runCatching {
                devicePolicyManager.setLockTaskPackages(admin, arrayOf(packageName))
            }
        }
    }

    private fun canEnterDedicatedLockTask(): Boolean {
        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        return devicePolicyManager?.isLockTaskPermitted(packageName) == true
    }

    private fun shouldBypassKioskForAccessibility(): Boolean {
        if (SystemClock.elapsedRealtime() < accessibilitySetupGraceUntilMs) {
            return true
        }

        val accessibilityManager = getSystemService(AccessibilityManager::class.java)
        if (accessibilityManager?.isEnabled == true) {
            return true
        }

        val accessibilityEnabled = runCatching {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        }.getOrDefault(false)

        return accessibilityEnabled
    }

    private fun stopLockTaskIfActive() {
        runCatching { stopLockTask() }
    }

    private fun openAccessibilitySettings() {
        accessibilitySetupGraceUntilMs = SystemClock.elapsedRealtime() + 5 * 60 * 1000L
        stopLockTaskIfActive()
        runCatching {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun restartApplication() {
        runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }
}

@Composable
private fun BatteryStatusPill(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryPercent by remember { mutableIntStateOf(readBatteryPercent(context)) }
    var isCharging by remember { mutableStateOf(readBatteryCharging(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                batteryPercent = readBatteryPercent(context ?: return)
                isCharging = readBatteryCharging(context)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BatteryGlyph(
            level = batteryPercent,
            charging = isCharging,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = "$batteryPercent%",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BatteryGlyph(
    level: Int,
    charging: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 8.dp)
                .border(width = 1.dp, color = color, shape = MaterialTheme.shapes.extraSmall)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(level.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        when {
                            charging -> Color(0xFF5FBF67)
                            level <= 15 -> Color(0xFFCC4B4B)
                            else -> color
                        }
                    )
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 1.dp)
                .size(width = 2.dp, height = 4.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
    }
}

private fun readBatteryPercent(context: Context): Int {
    val batteryManager = context.getSystemService(BatteryManager::class.java)
    return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?.takeIf { it in 0..100 }
        ?: 0
}

private fun readBatteryCharging(context: Context): Boolean {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}
