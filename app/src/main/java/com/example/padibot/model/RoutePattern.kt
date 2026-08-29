package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
enum class RoutePattern(
    val title: String,
    val description: String,
    val emoji: String
) {
    BOUSTROPHEDON(
        title = "1. Persegi Panjang / Zig-Zag",
        description = "Pola bolak-balik paralel standar paling efisien untuk petakan kotak",
        emoji = "⚡"
    ),
    HEADLAND_INNER(
        title = "2. Dengan Headland (Area Putar Keliling)",
        description = "Tanam area tengah terlebih dahulu, sisakan putaran di keliling pematang",
        emoji = "🔄"
    ),
    SPIRAL_INWARD(
        title = "3. Oval / Spiral (Pinggir ke Tengah)",
        description = "Memutar mengelilingi batas sawah dari luar menuju titik tengah",
        emoji = "🌀"
    ),
    SPIRAL_OUTWARD(
        title = "4. Oval / Spiral (Tengah ke Pinggir)",
        description = "Mulai dari pusat tengah sawah memutar membesar ke arah luar",
        emoji = "💫"
    );

    fun formatLabel(): String = "$emoji $title"
}
