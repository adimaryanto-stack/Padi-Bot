package com.example.padibot.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "PadiBot")
    data object FieldList : Screen("field_list", "Daftar Sawah")
    data object CreateField : Screen("create_field", "Tambah Sawah")
    data object PlantingSettings : Screen("planting_settings", "Pengaturan Tanam")
    data object RoutePreview : Screen("route_preview", "Preview Jalur")
    data object MissionExecution : Screen("mission_execution", "Eksekusi Misi")
    data object ManualControl : Screen("manual_control", "Kontrol Manual")
    data object MissionHistory : Screen("mission_history", "Riwayat Misi")
    data object MissionDetail : Screen("mission_detail/{missionId}", "Detail Misi") {
        fun createRoute(missionId: String) = "mission_detail/$missionId"
    }
    data object Settings : Screen("settings", "Pengaturan")
}
