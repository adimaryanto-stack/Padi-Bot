# 🌾 PadiBot — Smart Rice Planter

> Aplikasi Android untuk kontrol mesin tanam padi otomatis berbasis Arduino

[![Status](https://img.shields.io/badge/Status-Planning%20Phase-yellow)](https://github.com/adimaryanto-stack/Padi-Bot)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0+-green)](https://expo.dev)
[![Framework](https://img.shields.io/badge/Framework-React%20Native%20%2B%20Expo-blue)](https://expo.dev)
[![Hardware](https://img.shields.io/badge/Hardware-Arduino-teal)](https://www.arduino.cc)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## 📋 Deskripsi

**PadiBot** adalah sistem presisi pertanian yang memungkinkan petani Indonesia mengotomatiskan proses tanam padi menggunakan mesin tanam yang dikontrol lewat **smartphone Android**.

Aplikasi terhubung ke **Arduino** melalui 3 metode koneksi:
- 📶 **WiFi** — via ESP8266/ESP32 (jangkauan ~100m, latensi rendah)
- 🔵 **Bluetooth** — via HC-05/HC-06/HM-10 BLE (koneksi langsung, tanpa router)
- 📡 **GSM 4G** — via SIM800L/SIM7600 + MQTT (monitoring remote, unlimited range)

Data disimpan **lokal di SQLite** pada device Android — **100% bekerja offline** tanpa internet.

---

## ✨ Fitur Utama (MVP Phase 1)

| Fitur | Deskripsi |
|-------|-----------|
| 🗺️ **Pemetaan Sawah** | Input batas sawah manual (titik demi titik) |
| 🛣️ **Generate Jalur Otomatis** | Algoritma boustrophedon (pola meander/lawnmower) |
| 👁️ **Preview Rute** | Visualisasi jalur tanam sebelum dijalankan |
| 🎯 **Eksekusi Misi** | Start / Pause / Resume / Stop misi tanam |
| 📡 **Telemetri Real-time** | Monitor posisi, baterai, GPS, kecepatan |
| 🕹️ **Kontrol Manual** | D-Pad kontrol langsung ke mesin Arduino |
| ⛔ **Emergency Stop** | Tombol berhenti darurat — selalu tersedia |
| 📊 **Riwayat Misi** | Log historis semua misi + laporan cakupan |
| 💾 **Offline First** | SQLite lokal — tidak butuh internet |

---

## 🔧 Hardware yang Diperlukan

### Mikrokontroler
- **Arduino Uno / Mega / Nano** (logic utama)

### Modul Komunikasi (pilih salah satu)
| Modul | Tipe | Jangkauan |
|-------|------|-----------|
| ESP8266 / ESP32 | WiFi | ~50-100m |
| HC-05 / HC-06 | Bluetooth Classic | ~10-30m |
| HM-10 | Bluetooth BLE | ~10-30m |
| SIM800L | GSM 2G/GPRS | Unlimited |
| SIM7600 | 4G LTE | Unlimited |

### Komponen Lain
- **GPS Module:** u-blox NEO-6M / NEO-7M / NEO-8M
- **Motor Driver:** L298N atau IBT-2
- **Battery:** LiPo / SLA sesuai mesin

---

## 📱 Tech Stack

### Mobile App (Android)
- **Framework:** React Native + Expo SDK 51
- **Navigation:** Expo Router v3
- **Database:** SQLite via `expo-sqlite` + Drizzle ORM
- **State:** Zustand
- **Language:** TypeScript (strict)

### Arduino Firmware (C/C++)
- **WiFi:** HTTP Server + WebSocket via ESP8266/ESP32
- **Bluetooth:** Serial JSON via HC-05/HM-10
- **GSM:** MQTT via SIM7600 + PubSubClient
- **Libraries:** ArduinoJson, TinyGPS++, PubSubClient

---

## 🚀 Quick Start

### Prasyarat
- Node.js 20+
- Expo CLI (`npm install -g expo-cli`)
- Android Studio (untuk emulator) atau device Android fisik
- Arduino IDE / PlatformIO (untuk firmware)

### Setup Mobile App
```bash
# Clone repository
git clone https://github.com/adimaryanto-stack/Padi-Bot.git
cd Padi-Bot

# Install dependencies
npm install

# Jalankan di Android emulator
npx expo run:android

# Atau jalankan di device fisik
npx expo start --android
```

### Setup Arduino Firmware

#### WiFi (ESP8266/ESP32)
1. Buka `arduino/PadiBot_WiFi/PadiBot_WiFi.ino` di Arduino IDE
2. Edit `config.h` — isi WiFi SSID & password
3. Upload ke board
4. Di app Android → Pengaturan → Koneksi: WiFi → masukkan IP Arduino

#### Bluetooth (HC-05)
1. Buka `arduino/PadiBot_Bluetooth/PadiBot_Bluetooth.ino`
2. Edit `config.h` — atur baud rate (default 9600)
3. Upload ke Arduino, pasang modul HC-05 ke Serial1
4. Di app Android → Pengaturan → Koneksi: Bluetooth → scan & pilih HC-05

#### GSM 4G (SIM7600)
1. Buka `arduino/PadiBot_GSM/PadiBot_GSM.ino`
2. Edit `config.h` — isi APN, MQTT broker, device ID
3. Upload ke Arduino
4. Di app Android → Pengaturan → Koneksi: GSM 4G → isi MQTT config

---

## 📁 Struktur Project

```
Padi-Bot/
├── app/                    # React Native screens (Expo Router)
├── components/             # UI components
├── db/                     # SQLite schema & queries (Drizzle ORM)
├── services/               # Business logic (route planner, machine protocol)
│   ├── routePlanner/       # Boustrophedon coverage path planning
│   └── machineProtocol/    # WiFi / BT / GSM / Simulator connections
├── stores/                 # Zustand global state
├── arduino/                # Arduino firmware (C/C++)
│   ├── PadiBot_WiFi/       # Firmware WiFi (ESP8266/ESP32)
│   ├── PadiBot_Bluetooth/  # Firmware Bluetooth (HC-05/HM-10)
│   └── PadiBot_GSM/        # Firmware GSM 4G (SIM800L/SIM7600)
├── docs/                   # Dokumentasi lengkap
│   ├── design.md           # UI/UX Design System
│   └── ...
└── README.md
```

---

## 📖 Dokumentasi

| Dokumen | Deskripsi |
|---------|-----------|
| [PRD v1.1](PADI_BOT_PRD_v1.1.md) | Product Requirements Document |
| [Design System](design.md) | UI/UX design tokens, wireframes, komponen |
| [MVP Roadmap](PADI_BOT_MVP_ROADMAP.md) | Sprint plan 6 minggu |
| [System Diagrams](PADI_BOT_DIAGRAMS.md) | Architecture & flow diagrams (Mermaid) |
| [Analysis Summary](PADI_BOT_ANALYSIS_SUMMARY.md) | Gap analysis & improvements |

---

## 🗺️ Roadmap

| Phase | Minggu | Deskripsi |
|-------|--------|-----------|
| **Phase 1 MVP** | 1-4 | App Android + Simulator (offline, SQLite) |
| **Phase 1+** | 5-6 | Koneksi Arduino WiFi + Bluetooth |
| **Phase 2** | 7-10 | GPS walk-mapping, advanced routing |
| **Phase 2.5** | 11-12 | Obstacle avoidance, headland strategies |
| **Phase 3** | 13-16 | GSM 4G remote, cloud sync, analytics |
| **Phase 4** | Future | RTK-GNSS, LiDAR, full autonomy |

---

## 🤝 Kontribusi

1. Fork repository ini
2. Buat branch fitur: `git checkout -b feature/nama-fitur`
3. Commit perubahan: `git commit -m 'feat: tambah fitur X'`
4. Push ke branch: `git push origin feature/nama-fitur`
5. Buat Pull Request

---

## 👨‍💻 Author

**Adi Maryanto**  
GitHub: [@adimaryanto-stack](https://github.com/adimaryanto-stack)

---

## 📄 License

MIT License — lihat [LICENSE](LICENSE) untuk detail.

---

> **PadiBot** — Tanam Cerdas, Panen Lebih 🌾🤖
