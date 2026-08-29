package com.example.padibot.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FieldList : Screen("field_list")
    object CreateField : Screen("create_field")
    object PlantingSettings : Screen("planting_settings")
    object RoutePreview : Screen("route_preview")
    object MissionExecution : Screen("mission_execution")
    object ManualControl : Screen("manual_control")
    object MissionHistory : Screen("mission_history")
    object MissionDetail : Screen("mission_detail/{missionId}") {
        fun createRoute(missionId: String): String = "mission_detail/$missionId"
    }
    object Settings : Screen("settings")
}
