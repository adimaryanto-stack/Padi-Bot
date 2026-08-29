package com.example.padibot.model

enum class RoutePattern(
    val title: String,
    val description: String
) {
    BOUSTROPHEDON(
        title = "Boustrophedon (Zig-Zag Paralel)",
        description = "Pola bolak-balik standar pertanian presisi untuk petak teratur"
    ),
    HEADLAND_INNER(
        title = "Headland & Jalur Tengah",
        description = "Mulai dari tengah petak, diakhiri dengan putaran headland terluar"
    ),
    SPIRAL_INWARD(
        title = "Spiral Konsentris Inward",
        description = "Menanam dari batas terluar melingkar masuk ke pusat sawah"
    ),
    SPIRAL_OUTWARD(
        title = "Spiral Konsentris Outward",
        description = "Menanam dari pusat tengah sawah melingkar menuju batas luar"
    )
}
