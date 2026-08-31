package com.example.padibot.model

import java.util.UUID

enum class MarkerType(
    val title: String,
    val code: String,
    val emoji: String,
    val colorHex: Long,
    val description: String
) {
    IRRIGATION_INLET(
        title = "Inlet Irigasi",
        code = "IN",
        emoji = "💧",
        colorHex = 0xFF0284C7,
        description = "Saluran / Pintu Masuk Air Sawah"
    ),
    IRRIGATION_OUTLET(
        title = "Outlet Pembuangan",
        code = "OUT",
        emoji = "🌊",
        colorHex = 0xFF0891B2,
        description = "Saluran Pembuangan / Keluar Air"
    ),
    PLANTING_START(
        title = "Titik Awal Tanam",
        code = "START",
        emoji = "🌱",
        colorHex = 0xFF16A34A,
        description = "Titik Mulai Jalur Mesin Tanam"
    ),
    PLANTING_FINISH(
        title = "Titik Selesai",
        code = "STOP",
        emoji = "🏁",
        colorHex = 0xFFEA580C,
        description = "Titik Akhir / Parkir Mesin"
    ),
    WATER_PUMP(
        title = "Pompa / Sumur",
        code = "PUMP",
        emoji = "🚰",
        colorHex = 0xFF4F46E5,
        description = "Sumber Air Irigasi Pompa"
    ),
    OBSTACLE(
        title = "Rintangan Sawah",
        code = "OBST",
        emoji = "⚠️",
        colorHex = 0xFFDC2626,
        description = "Tiang / Batu / Area Berbahaya"
    )
}

data class FieldMarker(
    val id: String = UUID.randomUUID().toString(),
    val type: MarkerType,
    val point: GeoPoint,
    val note: String = ""
)
