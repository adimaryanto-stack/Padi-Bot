package com.example.padibot.model

enum class ConnectionType(val label: String) {
    SIMULATOR("Simulator Otonom (Virtual / Tanpa Alat)"),
    WIFI("WiFi ESP32 Access Point (TCP/IP)"),
    BLUETOOTH("Bluetooth SPP / BLE Wireless"),
    GSM_MQTT("Koneksi Cloud IoT (GSM 4G MQTT)")
}
