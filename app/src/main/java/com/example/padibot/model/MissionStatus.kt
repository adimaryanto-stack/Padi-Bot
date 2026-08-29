package com.example.padibot.model

enum class MissionStatus(val label: String) {
    READY("Siap"),
    RUNNING("Berjalan"),
    PAUSED("Dijeda"),
    COMPLETED("Selesai"),
    STOPPED("Dihentikan"),
    ERROR("Gagal")
}
