package com.example.padibot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                val snackbarHostState = remember { SnackbarHostState() }

                // Listen for Emergency Stop notifications
                LaunchedEffect(Unit) {
                    viewModel.emergencyStopTriggered.collectLatest { reason ->
                        snackbarHostState.showSnackbar("⚠️ EMERGENCY STOP: $reason")
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
                            isConnected = isConnected
                        )
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            PadiBotBottomBar(
                                currentRoute = currentRoute,
                                onNavigateTo = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
