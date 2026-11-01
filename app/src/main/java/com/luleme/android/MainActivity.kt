package com.luleme.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luleme.android.common.RecordEditParam
import com.luleme.android.common.Routes
import com.luleme.android.data.SettingsStore
import com.luleme.android.presentation.screens.HomeScreen
import com.luleme.android.presentation.screens.LockScreen
import com.luleme.android.presentation.screens.RecordEditScreen
import com.luleme.android.presentation.screens.SettingsScreen
import com.luleme.android.presentation.screens.StatsScreen
import com.luleme.android.presentation.theme.LulemeTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LulemeTheme {
                LulemeAppRoot(
                    applyPrivacyMode = { enabled ->
                        if (enabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LulemeAppRoot(applyPrivacyMode: (Boolean) -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val settings by SettingsStore.getInstance(context).settingsFlow.collectAsState(initial = null)
    val unlocked by SessionLockState.unlocked.collectAsState()
    var lockChecked by remember { mutableStateOf(false) }

    LaunchedEffect(settings?.privacyModeEnabled) {
        applyPrivacyMode(settings?.privacyModeEnabled ?: true)
    }

    LaunchedEffect(settings?.lockEnabled, settings?.hasPin, unlocked) {
        val snap = settings ?: return@LaunchedEffect
        lockChecked = true
        val needLock = snap.lockEnabled && snap.hasPin && !unlocked
        val current = navController.currentBackStackEntry?.destination?.route
        if (needLock && current != Routes.LOCK) {
            navController.navigate(Routes.LOCK) {
                launchSingleTop = true
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route in setOf(Routes.HOME, Routes.STATS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        label = { Text("日历") }
                    )
                    NavigationBarItem(
                        selected = route == Routes.STATS,
                        onClick = {
                            navController.navigate(Routes.STATS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        label = { Text("统计") }
                    )
                    NavigationBarItem(
                        selected = route == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddRecord = { param ->
                        val id = param?.id ?: -1L
                        val date = param?.date ?: -1L
                        navController.navigate("${Routes.RECORD_EDIT}?id=$id&date=$date")
                    }
                )
            }
            composable(Routes.STATS) {
                StatsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onPrivacyModeChanged = applyPrivacyMode)
            }
            composable(
                route = "${Routes.RECORD_EDIT}?id={id}&date={date}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("date") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                val date = entry.arguments?.getLong("date") ?: -1L
                val param = when {
                    id > 0 -> RecordEditParam(id = id)
                    date > 0 -> RecordEditParam(date = date)
                    else -> null
                }
                RecordEditScreen(
                    param = param,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.LOCK) {
                LockScreen(
                    onUnlocked = {
                        scope.launch {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.LOCK) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
