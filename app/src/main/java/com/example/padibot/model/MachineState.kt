package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionType {
    SIMULATOR,
    WIFI,
    BLUETOOTH,
    GSM_MQTT;

    val label: String
        get() = when (this) {
            SIMULATOR -> "Simulator (Virtual)"
            WIFI -> "WiFi (ESP8266/ESP32)"
            BLUETOOTH -> "Bluetooth (HC-05/HM-10)"
            GSM_MQTT -> "GSM 4G (SIM7600/MQTT)"
        }
}

@Serializable
data class MachineSettings(
    val connectionType: ConnectionType = ConnectionType.SIMULATOR,
    val wifiIp: String = "192.168.4.1",
    val wifiPort: Int = 80,
    val bluetoothDeviceName: String = "HC-05-PadiBot",
    val bluetoothDeviceMac: String = "00:11:22:33:44:55",
    val mqttBroker: String = "broker.hivemq.com",
    val mqttPort: Int = 1883,
    val mqttDeviceId: String = "padibot-001",
    val defaultMachineWidthM: Double = 1.5,
    val defaultHeadlandWidthM: Double = 3.0,
    val maxSpeedMps: Double = 1.0,
    val debugMode: Boolean = true
)

@Serializable
data class MissionEvent(
    val id: Long = 0,
    val missionId: String,
    val eventType: String,
    val message: String,
    val severity: String = "INFO", // INFO, WARNING, CRITICAL
    val timestamp: Long = System.currentTimeMillis()
)
