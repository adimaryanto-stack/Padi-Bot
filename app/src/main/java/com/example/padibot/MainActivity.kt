package com.example.padibot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.padibot.theme.PadiBotTheme
import com.example.padibot.ui.components.PadiBotBottomBar
import com.example.padibot.ui.components.PadiBotTopBar
import com.example.padibot.ui.navigation.Screen
import com.example.padibot.ui.screens.*
import com.example.padibot.viewmodel.PadiBotViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: PadiBotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            PadiBotTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isConnected by viewModel.isMachineConnected.collectAsState()
                val settings by viewModel.machineSettings.collectAsState()
                val telemetry by viewModel.telemetry.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Listen for Emergency Stop notifications
                LaunchedEffect(Unit) {
                    viewModel.emergencyStopTriggered.collectLatest { reason ->
                        snackbarHostState.showSnackbar("⚠️ EMERGENCY STOP: $reason")
                    }
                }

                // Listen for Low Battery Alert Events
                LaunchedEffect(Unit) {
                    viewModel.batteryAlertEvents.collectLatest { alert ->
                        val result = snackbarHostState.showSnackbar(
                            message = "${alert.title}: ${alert.message}",
                            actionLabel = "Lihat",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            if (currentRoute != Screen.Dashboard.route) {
                                navController.navigate(Screen.Dashboard.route)
                            }
                        }
                    }
                }

                val showBottomBar = currentRoute in listOf(
                    Screen.Dashboard.route,
                    Screen.FieldList.route,
                    Screen.MissionHistory.route,
                    Screen.Settings.route
                )

                val screenTitle = when {
                    currentRoute == Screen.Dashboard.route -> "PadiBot"
                    currentRoute == Screen.FieldList.route -> "Peta & Sawah"
                    currentRoute == Screen.CreateField.route -> "Tambah Sawah Baru"
                    currentRoute == Screen.PlantingSettings.route -> "Parameter Tanam"
                    currentRoute == Screen.RoutePreview.route -> "Preview Jalur Tanam"
                    currentRoute == Screen.MissionExecution.route -> "Eksekusi Misi Tanam"
                    currentRoute == Screen.ManualControl.route -> "Kontrol Manual"
                    currentRoute == Screen.MissionHistory.route -> "Riwayat Misi"
                    currentRoute?.startsWith("mission_detail") == true -> "Detail & Audit Misi"
                    currentRoute == Screen.Settings.route -> "Pengaturan Perangkat"
                    else -> "PadiBot"
                }

                val canNavigateBack = currentRoute !in listOf(
                    Screen.Dashboard.route,
                    Screen.FieldList.route,
                    Screen.MissionHistory.route,
                    Screen.Settings.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        PadiBotTopBar(
                            title = screenTitle,
                            canNavigateBack = canNavigateBack,
                            onNavigateBack = { navController.navigateUp() },
                            connectionType = settings.connectionType,
                            isConnected = isConnected,
                            batteryPct = telemetry.batteryPct,
                            onBatteryClick = {
                                if (currentRoute != Screen.Dashboard.route) {
                                    navController.navigate(Screen.Dashboard.route)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            PadiBotBottomBar(
                                currentRoute = currentRoute,
                                onNavigateTo = { route ->
                                    if (route != currentRoute) {
                                        if (route == Screen.Dashboard.route) {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToPlantingSettings = {
                                    navController.navigate(Screen.PlantingSettings.route)
                                },
                                onNavigateToManualControl = {
                                    navController.navigate(Screen.ManualControl.route)
                                },
                                onNavigateToFields = {
                                    navController.navigate(Screen.FieldList.route)
                                },
                                onNavigateToHistory = {
                                    navController.navigate(Screen.MissionHistory.route)
                                },
                                onNavigateToMissionDetail = { missionId ->
                                    navController.navigate(Screen.MissionDetail.createRoute(missionId))
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        composable(Screen.FieldList.route) {
                            FieldListScreen(
                                viewModel = viewModel,
                                onNavigateToCreateField = {
                                    navController.navigate(Screen.CreateField.route)
                                },
                                onNavigateToPlantingSettings = {
                                    navController.navigate(Screen.PlantingSettings.route)
                                }
                            )
                        }

                        composable(Screen.CreateField.route) {
                            CreateFieldScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(Screen.PlantingSettings.route) {
                            PlantingSettingsScreen(
                                viewModel = viewModel,
                                onNavigateToPreview = {
                                    navController.navigate(Screen.RoutePreview.route)
                                },
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(Screen.RoutePreview.route) {
                            RoutePreviewScreen(
                                viewModel = viewModel,
                                onNavigateToExecution = {
                                    navController.navigate(Screen.MissionExecution.route) {
                                        popUpTo(Screen.Dashboard.route)
                                    }
                                },
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(Screen.MissionExecution.route) {
                            MissionExecutionScreen(
                                viewModel = viewModel,
                                onNavigateToManualControl = {
                                    navController.navigate(Screen.ManualControl.route)
                                },
                                onNavigateToHistory = {
                                    navController.navigate(Screen.MissionHistory.route) {
                                        popUpTo(Screen.Dashboard.route)
                                    }
                                },
                                onNavigateBackToHome = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.ManualControl.route) {
                            ManualControlScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(Screen.MissionHistory.route) {
                            MissionHistoryScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { missionId ->
                                    navController.navigate(Screen.MissionDetail.createRoute(missionId))
                                }
                            )
                        }

                        composable(
                            route = Screen.MissionDetail.route,
                            arguments = listOf(navArgument("missionId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val missionId = backStackEntry.arguments?.getString("missionId") ?: ""
                            MissionDetailScreen(
                                missionId = missionId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.navigateUp() },
                                onRerunMission = {
                                    navController.navigate(Screen.MissionExecution.route) {
                                        popUpTo(Screen.Dashboard.route)
                                    }
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
