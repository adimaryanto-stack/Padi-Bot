package com.example.padibot.model

data class MachineSettings(
    val connectionType: ConnectionType = ConnectionType.SIMULATOR,
    val wifiIp: String = "192.168.4.1",
    val wifiPort: Int = 80,
    val bluetoothDeviceName: String = "PadiBot_ESP32",
    val bluetoothDeviceMac: String = "00:11:22:33:44:55",
    val mqttBroker: String = "broker.hivemq.com",
    val mqttDeviceId: String = "padibot_01",
    val firebaseDbUrl: String = "https://padibot-22de3-default-rtdb.asia-southeast1.firebasedatabase.app",
    val firebaseAuthToken: String = "EI3vfJn9M89e1DpEDI4vDCqMBDAYEGN2kNxOJNMt",
    val firebaseAutoSync: Boolean = true
)
