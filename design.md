# PadiBot — Design System & UI Specification
## Android Mobile App · Versi 1.1

**Versi Dokumen:** 1.1  
**Tanggal:** August 2026  
**Platform:** Android 8.0+ (API Level 26+)  
**Bahasa UI:** Bahasa Indonesia  

---

## 📋 DAFTAR ISI

1. [Design Principles](#1-design-principles)
2. [Design Tokens](#2-design-tokens)
3. [Typography](#3-typography)
4. [Spacing & Layout](#4-spacing--layout)
5. [Component Library](#5-component-library)
6. [Navigation Structure](#6-navigation-structure)
7. [Screen Specifications](#7-screen-specifications)
8. [Interaction Patterns](#8-interaction-patterns)
9. [Accessibility Guidelines](#9-accessibility-guidelines)
10. [Error & Empty States](#10-error--empty-states)

---

## 1. Design Principles

### 1.1 Konteks Penggunaan
PadiBot digunakan **di lapangan sawah** dalam kondisi:
- ☀️ Sinar matahari langsung (outdoor glare)
- 🧤 Pengguna mungkin memakai sarung tangan
- 💧 Tangan bisa basah atau kotor
- 📶 Koneksi internet tidak tersedia (fully offline)
- 🧑‍🌾 Tingkat literasi teknologi bervariasi

### 1.2 Prinsip Utama

| Prinsip | Implementasi |
|---------|-------------|
| **Clarity First** | Teks besar, kontras tinggi, label yang jelas |
| **Touch Friendly** | Minimum touch target 48×48dp, spacing antar elemen |
| **Glance-able** | Info penting terlihat dalam sekali pandang |
| **Error Tolerant** | Konfirmasi untuk aksi destruktif/berbahaya |
| **Offline First** | Semua fungsi bekerja tanpa internet |
| **Farmer-Centered** | Bahasa Indonesia sederhana, tidak ada jargon teknis |

---

## 2. Design Tokens

### 2.1 Color Palette

#### Primary — Hijau Sawah
```
Green 50:   #F1F8E9   ← Background subtle
Green 100:  #DCEDC8   ← Surface hover
Green 200:  #C5E1A5   ← Surface active
Green 400:  #9CCC65   ← Secondary action
Green 600:  #7CB342   ← Primary action (normal)
Green 700:  #558B2F   ← Primary action (pressed)
Green 800:  #33691E   ← Dark variant
Green 900:  #1B5E20   ← Darkest / high-emphasis
```

#### Semantic Colors
```
Success:      #4CAF50   ← Misi selesai, GPS fix
Warning:      #FF9800   ← Battery rendah, GPS lemah
Error/Danger: #F44336   ← Error kritis, Emergency Stop
Info:         #2196F3   ← Informasi umum
```

#### Neutral — Abu
```
Gray 50:   #FAFAFA   ← Background app
Gray 100:  #F5F5F5   ← Card background
Gray 200:  #EEEEEE   ← Divider
Gray 400:  #BDBDBD   ← Placeholder, disabled
Gray 600:  #757575   ← Secondary text
Gray 800:  #424242   ← Primary text
Gray 900:  #212121   ← High emphasis text
```

#### Status Colors — Mission State
```
DRAFT:     #9E9E9E (Gray 500)    ← Belum diapprove
READY:     #2196F3 (Blue 500)    ← Siap dijalankan
RUNNING:   #4CAF50 (Green 500)   ← Sedang berjalan
PAUSED:    #FF9800 (Orange 500)  ← Di-pause
COMPLETED: #8BC34A (Light Green) ← Selesai
STOPPED:   #F44336 (Red 500)     ← Dihentikan
ERROR:     #9C27B0 (Purple 500)  ← Error / Gangguan
```

#### Emergency Colors
```
Emergency Background: #B71C1C   ← Merah sangat pekat
Emergency Text:       #FFFFFF   ← Putih, kontras maksimal
Emergency Border:     #FF1744   ← Aksen merah cerah
```

### 2.2 Color Token Map (TypeScript)
```typescript
// constants/theme.ts
export const Colors = {
  // Primary
  primary:         '#7CB342',
  primaryDark:     '#558B2F',
  primaryLight:    '#DCEDC8',

  // Semantic
  success:         '#4CAF50',
  warning:         '#FF9800',
  error:           '#F44336',
  info:            '#2196F3',

  // Background
  background:      '#FAFAFA',
  surface:         '#FFFFFF',
  surfaceVariant:  '#F5F5F5',

  // Text
  textPrimary:     '#212121',
  textSecondary:   '#757575',
  textDisabled:    '#BDBDBD',
  textOnPrimary:   '#FFFFFF',

  // Border
  border:          '#EEEEEE',
  borderStrong:    '#BDBDBD',

  // Mission Status
  statusDraft:     '#9E9E9E',
  statusReady:     '#2196F3',
  statusRunning:   '#4CAF50',
  statusPaused:    '#FF9800',
  statusCompleted: '#8BC34A',
  statusStopped:   '#F44336',
  statusError:     '#9C27B0',

  // Emergency
  emergency:       '#B71C1C',
  emergencyAccent: '#FF1744',
} as const;
```

---

## 3. Typography

### 3.1 Font Stack
- **Primary Font:** `Roboto` (default Android system font)
- **Monospace:** `Roboto Mono` (untuk koordinat, data numerik)
- **Fallback:** System default

### 3.2 Type Scale
```
Display Large:   40sp  Bold    ← Splash screen, onboarding
Headline Large:  28sp  Bold    ← Screen title utama
Headline Medium: 24sp  SemiBold← Section header
Headline Small:  20sp  SemiBold← Card title, dialog title
Title Large:     18sp  Medium  ← Item header, mission name
Title Medium:    16sp  Medium  ← Secondary header
Body Large:      16sp  Regular ← Body text utama (minimum outdoor)
Body Medium:     14sp  Regular ← Secondary body
Body Small:      12sp  Regular ← Caption, metadata
Label Large:     14sp  Medium  ← Button label
Label Medium:    12sp  Medium  ← Tab label, chip
Label Small:     11sp  Medium  ← Badge, timestamp (minimum)
```

> ⚠️ **Minimum font size untuk outdoor readability: 14sp**  
> Untuk kriteria keselamatan (Emergency Stop, warning): minimum 18sp Bold

### 3.3 Typography Tokens (TypeScript)
```typescript
export const Typography = {
  headlineLarge:  { fontSize: 28, fontWeight: '700', lineHeight: 36 },
  headlineMedium: { fontSize: 24, fontWeight: '600', lineHeight: 32 },
  headlineSmall:  { fontSize: 20, fontWeight: '600', lineHeight: 28 },
  titleLarge:     { fontSize: 18, fontWeight: '500', lineHeight: 24 },
  titleMedium:    { fontSize: 16, fontWeight: '500', lineHeight: 22 },
  bodyLarge:      { fontSize: 16, fontWeight: '400', lineHeight: 24 },
  bodyMedium:     { fontSize: 14, fontWeight: '400', lineHeight: 20 },
  labelLarge:     { fontSize: 14, fontWeight: '500', lineHeight: 20 },
  labelSmall:     { fontSize: 12, fontWeight: '500', lineHeight: 16 },
  emergency:      { fontSize: 20, fontWeight: '700', lineHeight: 28 },
  telemetry:      { fontSize: 18, fontWeight: '700', fontFamily: 'monospace' },
  coordinate:     { fontSize: 14, fontWeight: '400', fontFamily: 'monospace' },
} as const;
```

---

## 4. Spacing & Layout

### 4.1 Spacing Scale (8-point grid)
```
Spacing.xs:   4dp   ← Internal padding text
Spacing.sm:   8dp   ← Padding dalam card kecil
Spacing.md:   12dp  ← Padding standar
Spacing.lg:   16dp  ← Padding card, section
Spacing.xl:   24dp  ← Padding screen horizontal
Spacing.xxl:  32dp  ← Margin section besar
Spacing.xxxl: 48dp  ← Margin display utama
```

### 4.2 Touch Target Guidelines
```
Minimum touch target:     48×48dp     ← Semua interactable elements
Recommended touch target: 56×56dp     ← Tombol penting
Emergency Stop button:    MIN 80×80dp ← Ukuran khusus keselamatan
Action buttons (bottom):  Penuh lebar ← Mulai/Stop/Pause
```

### 4.3 Screen Layout
```
Horizontal Padding:   16dp kiri + 16dp kanan
Max Content Width:    min(screenWidth, 480dp)
Bottom Tab Height:    56dp
Action Button Height: 52dp (primary), 48dp (secondary)
Card Border Radius:   12dp
Card Elevation:       2dp (resting), 8dp (pressed)
```

---

## 5. Component Library

### 5.1 Buttons

#### Primary Button
```
Height: 52dp | Border radius: 12dp
Background: #7CB342 | Text: #FFFFFF 16sp Bold
State Pressed: #558B2F | State Disabled: opacity 0.38
```

#### Danger Button
```
Background: #F44336 | Text: #FFFFFF 16sp Bold
Use case:   Stop misi, hapus field
```

#### Emergency Stop Button
```
Height:          80dp minimum
Width:           Full width (tidak kurang dari 200dp)
Background:      #B71C1C
Border:          3dp #FF1744
Text:            "⛔ BERHENTI DARURAT" 20sp Bold #FFFFFF
Shadow:          0dp 4dp 12dp rgba(183,28,28,0.5)
Position:        SELALU VISIBLE, tidak pernah di-hide
Interaction:     1× tap = konfirmasi dialog
                 Hold 2s = langsung berhenti
Accessibility:   accessibilityLabel="Tombol berhenti darurat"
```

#### Secondary Button (Outlined)
```
Height: 48dp | Border: 1.5dp #7CB342
Background: transparent | Text: #7CB342 14sp Medium
State Pressed: Background #DCEDC8
```

### 5.2 Status Badge (Mission State)
```
Padding: 4dp × 12dp | Border radius: 20dp (pill)
Font: 12sp Medium

Status    │ Background │ Text Color
──────────┼────────────┼──────────
DRAFT     │ #E0E0E0    │ #616161
READY     │ #BBDEFB    │ #1565C0
RUNNING   │ #C8E6C9    │ #2E7D32
PAUSED    │ #FFE0B2    │ #E65100
COMPLETED │ #DCEDC8    │ #33691E
STOPPED   │ #FFCDD2    │ #C62828
ERROR     │ #E1BEE7    │ #6A1B9A
```

### 5.3 Telemetry Card (Grid 2×2)
```
┌─────────────────┬─────────────────┐
│ 🔋 Baterai      │ 📍 GPS          │
│ 82%             │ Fix (±1.2m)     │
├─────────────────┼─────────────────┤
│ ⚡ Kecepatan    │ 🛤️ Jalur        │
│ 1.5 m/s         │ 3 / 12          │
└─────────────────┴─────────────────┘
Value: 24sp Bold, textPrimary
Label: 12sp, textSecondary
```

### 5.4 Progress Bar
```
Height: 12dp | Border radius: 6dp
Background: #EEEEEE | Fill: #7CB342
Label: "72% selesai" 14sp Medium di atas
```

### 5.5 Input Field
```
Height: 56dp | Border radius: 8dp
Border: 1dp #BDBDBD (resting) → 2dp #7CB342 (focused)
Font: 16sp Regular
Error state: border merah + helper text merah 12sp
```

---

## 6. Navigation Structure

### 6.1 Bottom Tab Navigation
```
Tab 1: 🏠 Beranda      → Dashboard
Tab 2: 🌾 Sawah        → Field List
Tab 3: 📊 Riwayat      → Mission History
Tab 4: ⚙️ Pengaturan   → Settings

Height: 56dp | Icon: 24dp | Label: 12sp Medium
Active: Colors.primary | Inactive: Colors.textSecondary
```

### 6.2 Navigation Flow
```
Splash Screen (2s)
    ↓
[Tab] Beranda (Dashboard)
    ├── "Mulai Misi Baru" → Planting Settings
    │       ↓ Generate
    │   Route Preview
    │       ↓ Approve
    │   Mission Execution (fullscreen)
    │
    ├── "Kontrol Manual" → Manual Control (modal)
    │
    ├── [Tab] Sawah → Field List
    │       ├── (+) → Create Field
    │       └── Tap → Field Detail → "Misi Baru" → Planting Settings
    │
    ├── [Tab] Riwayat → Mission History → Mission Detail
    │
    └── [Tab] Pengaturan → Settings
```

---

## 7. Screen Specifications

### 7.1 Splash Screen
```
Background: #7CB342 (primary green)
Center:
├── Logo/Icon: 80×80dp, putih
├── App Name: "PadiBot" 36sp Bold, putih
└── Tagline: "Tanam Cerdas, Panen Lebih" 16sp, putih 80%
Duration: 2 detik → auto-navigate ke Dashboard
Animation: Logo scale 0.8→1.0 + fade-in (300ms ease-out)
```

### 7.2 Dashboard Screen
```
┌─────────────────────────────────────┐
│ Selamat Datang       PadiBot        │  ← Header
│                   ● Terhubung ▾     │
├─────────────────────────────────────┤
│  CARD: Status Mesin                 │
│  🤖 Simulator Aktif    🔋 82%       │
│  📍 GPS Fix ±1.2m                   │
├─────────────────────────────────────┤
│  CARD: Sawah Aktif                  │
│  🌾 Sawah Utama    1.240 m²         │
│  [Ganti Sawah]                      │
├─────────────────────────────────────┤
│  CARD: Misi Terakhir                │
│  Misi #3  [SELESAI]  27 Agt 08:30   │
│  Cakupan: 94%                       │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │   🌾  MULAI MISI BARU       │    │  ← Primary
│  └─────────────────────────────┘    │
│  ┌──────────────┐┌──────────────┐   │
│  │🕹️ Kontrol   ││📊 Riwayat   │   │  ← Secondary
│  │  Manual     ││  Misi       │   │
│  └──────────────┘└──────────────┘   │
└─────────────────────────────────────┘
```

### 7.3 Field List Screen
```
┌─────────────────────────────────────┐
│ ← Daftar Sawah                   + │
├─────────────────────────────────────┤
│ 🔍 Cari sawah...                    │
├─────────────────────────────────────┤
│ [🌾] Sawah Utama    1.240 m²    >   │
│       4 titik · 2 hari lalu         │
├─────────────────────────────────────┤
│ [🌾] Sawah Timur      890 m²    >   │
│       5 titik · 1 minggu lalu       │
└─────────────────────────────────────┘
Swipe kiri → [Hapus] merah
```

### 7.4 Create Field Screen
```
┌─────────────────────────────────────┐
│ ← Batal      Tambah Sawah    Simpan │
├─────────────────────────────────────┤
│ Nama Sawah *                        │
│ [Contoh: Sawah Utama            ]   │
├─────────────────────────────────────┤
│ Titik Batas Sawah (min. 3 titik) *  │
│ ┌─────────────────────────────────┐ │
│ │   [Preview Polygon Canvas]      │ │
│ │   200dp height                  │ │
│ └─────────────────────────────────┘ │
│ Titik 1: -6.9234°, 107.6100°  [✕]  │
│ Titik 2: -6.9240°, 107.6108°  [✕]  │
│ [+ Tambah Titik Manual]             │
├─────────────────────────────────────┤
│ 📐 Luas: 1.240 m²  Keliling: 145 m  │
└─────────────────────────────────────┘
```

### 7.5 Planting Settings Screen
```
┌─────────────────────────────────────┐
│ ← Kembali    Pengaturan Tanam       │
│ [1] Settings  [2] Preview  [3] Misi │  ← Step indicator
├─────────────────────────────────────┤
│ Sawah: Sawah Utama (1.240 m²)       │
├─────────────────────────────────────┤
│ Lebar Kerja Mesin (meter) *         │
│ [    1.50 m                     ]   │
│ ℹ️ Biasanya 1.2 – 2.0 meter         │
│                                     │
│ Lebar Headland (meter) *            │
│ [    3.00 m                     ]   │
│ ℹ️ Area untuk mesin berputar         │
│                                     │
│ Orientasi Jalur                     │
│ 0° ──────────●──────────── 360°     │
│              45°                    │
├─────────────────────────────────────┤
│ Estimasi: ~12 jalur · ~450m · ~94%  │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │        Generate Jalur →         │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### 7.6 Route Preview Screen
```
┌─────────────────────────────────────┐
│ ← Ubah Pengaturan   Preview Jalur   │
│ [1] Settings  [2] Preview  [3] Misi │
├─────────────────────────────────────┤
│                                     │
│   [Canvas: Field Boundary]          │
│   ┌─────────────────────────────┐   │
│   │ ●──────────────────────── ● │   │ ← Lanes (biru)
│   │ ●──────────────────────── ● │   │
│   │ ...                         │   │
│   │ ● (start)          (end) ● │   │
│   └─────────────────────────────┘   │
│                       [+] [-] [↺]   │
├─────────────────────────────────────┤
│ 12 Jalur │ 450 m Jarak │ 94% Cakupan│
├─────────────────────────────────────┤
│ [← Ubah Pengaturan]   [✓ Approve →] │
└─────────────────────────────────────┘
```

### 7.7 Mission Execution Screen
```
┌─────────────────────────────────────┐
│ ✕  ● BERJALAN  00:12:34  [⛔DARURAT]│  ← Header (compact)
├─────────────────────────────────────┤
│                                     │
│   [Live Map Canvas — flex: 1]       │
│   ████████████░░░░░░ Jalur 7/12    │
│         ● (machine position)        │
│                                     │
├─────────────────────────────────────┤
│ 🔋82% │ 📍Fix │ 1.5m/s │ 07/12     │  ← Telemetry strip
├─────────────────────────────────────┤
│ [████████████░░░░░░░░] 72% selesai  │
│ Perkiraan selesai: ±8 menit         │
├─────────────────────────────────────┤
│ ┌──────────────────┐┌─────────────┐ │
│ │  ⏸ PAUSE        ││   ■ STOP   │ │
│ └──────────────────┘└─────────────┘ │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │    ⛔ BERHENTI DARURAT       │    │  ← 80dp height
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 7.8 Manual Control Screen
```
┌─────────────────────────────────────┐
│ ✕             Kontrol Manual        │
├─────────────────────────────────────┤
│ ⚠️ Mode manual — perhatikan sekitar │
│ ● Simulator Aktif                   │
├─────────────────────────────────────┤
│              ┌──────┐               │
│              │  ▲   │ MAJU          │
│              │      │               │
│  ┌──────┐    └──────┘    ┌──────┐   │
│  │  ◄   │    ┌──────┐    │  ►   │   │
│  │KIRI  │    │  ●   │    │KANAN │   │
│  └──────┘    └──────┘    └──────┘   │
│              ┌──────┐               │
│              │  ▼   │ MUNDUR        │
│              └──────┘               │
│  [72×72dp per tombol]               │
├─────────────────────────────────────┤
│ Kecepatan: 50% (0.75 m/s)          │
│ Lambat ──────●────── Cepat          │
│ [Lambat]  [Sedang]  [Cepat]        │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │    ⛔ BERHENTI DARURAT       │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 7.9 Mission History Screen
```
┌─────────────────────────────────────┐
│              Riwayat Misi           │
├─────────────────────────────────────┤
│ [Semua] [Selesai] [Dihentikan] [...] │  ← Filter chips
├─────────────────────────────────────┤
│ [SELESAI] 27 Agt 2026, 08:30        │
│ Misi Sawah Utama #3                 │
│ Sawah Utama · Cakupan 94%  →        │
├─────────────────────────────────────┤
│ [DIHENTIKAN] 26 Agt 2026, 16:45     │
│ Misi Sawah Timur #1                 │
│ Sawah Timur · Cakupan 60%  →        │
└─────────────────────────────────────┘
```

### 7.10 Mission Detail Screen
```
┌─────────────────────────────────────┐
│ ←             Detail Misi           │
├─────────────────────────────────────┤
│ Misi Sawah Utama #3   [SELESAI]     │
│ 27 Agustus 2026, 08:30 – 08:43     │
│ Durasi: 12 menit 34 detik           │
├─────────────────────────────────────┤
│  94%          │  1.165 m²           │
│  Cakupan      │  Area Tertanam      │
├───────────────┼─────────────────────┤
│  450 m        │  12/12 jalur        │
│  Jarak Total  │  Selesai            │
├─────────────────────────────────────┤
│  [Preview Route Canvas — static]    │
├─────────────────────────────────────┤
│ Log Misi:                           │
│ 08:30 [INFO] Misi dimulai           │
│ 08:35 [INFO] Jalur 6/12 selesai     │
│ 08:43 [INFO] Misi selesai 94%       │
└─────────────────────────────────────┘
```

### 7.11 Settings Screen
```
┌─────────────────────────────────────┐
│              Pengaturan             │
├─────────────────────────────────────┤
│ KONEKSI ARDUINO                     │
│ Tipe Koneksi  [Simulator        ▼]  │
│               [WiFi             ▼]  │ ← pilih salah satu
│               [Bluetooth        ▼]  │
│               [GSM 4G           ▼]  │
│                                     │
│ ── Saat WiFi dipilih: ───────────── │
│ IP Address     [192.168.4.1      ]  │
│ Port           [80               ]  │
│ [● Test Koneksi WiFi]               │
│                                     │
│ ── Saat Bluetooth dipilih: ──────── │
│ Perangkat      [Scan Bluetooth ▾]   │
│   HC-05 (00:11:22:AA:BB:CC)        │
│   HM-10 BLE   (00:11:22:DD:EE:FF) │
│ [🔄 Scan Ulang Perangkat]           │
│                                     │
│ ── Saat GSM 4G dipilih: ─────────── │
│ MQTT Broker    [broker.hivemq.com]  │
│ Device ID      [padibot-001      ]  │
│ Username       [opsional         ]  │
│ Password       [opsional         ]  │
│ [● Test Koneksi MQTT]               │
│                                     │
│ Status:  ● WiFi Terhubung          │
├─────────────────────────────────────┤
│ DEFAULT MESIN (ARDUINO)             │
│ Lebar Kerja Default    [1.50 m]     │
│ Lebar Headland Default [3.00 m]     │
│ Kecepatan Max          [1.0 m/s]    │
├─────────────────────────────────────┤
│ APLIKASI                            │
│ Mode Debug              [Toggle]    │
│ Bahasa             [Indonesia   ▼]  │
├─────────────────────────────────────┤
│ DATA                                │
│ Sawah: 2 · Misi: 15 · DB: 2.4 MB   │
│ [Hapus Semua Data]  ← merah         │
├─────────────────────────────────────┤
│ TENTANG                             │
│ PadiBot v0.1.0 · Build 2026.08.27   │
│ Hardware: Arduino + WiFi/BT/GSM     │
│ GitHub: adimaryanto-stack/Padi-Bot  │
└─────────────────────────────────────┘
```

---

## 8. Interaction Patterns

### 8.1 Loading States
```
Screen loading:     Skeleton shimmer loader
Button loading:     Spinner kiri + "Memproses..." (disabled)
Route generation:   Progress bar + "Membuat jalur..."
Data save:          Inline ✓ (muncul 1.5s, auto-hilang)
```

### 8.2 Konfirmasi Dialog

**Hapus Sawah:**
> Title: "Hapus Sawah?"  
> Body: "Sawah 'Sawah Utama' dan semua riwayat misinya akan dihapus permanen."  
> Actions: [Batal] [Hapus]

**Stop Misi:**
> Title: "Hentikan Misi?"  
> Body: "Misi akan dihentikan. Mesin akan berhenti di posisi saat ini."  
> Actions: [Lanjutkan Misi] [Hentikan]

**Emergency Stop (1× tap):**
> Title: "⚠️ Berhenti Darurat?"  
> Body: "Mesin akan segera berhenti."  
> Actions: [Batalkan] [YA, BERHENTI] ← bold, merah

**Emergency Stop (hold 2s):**
> Langsung berhenti tanpa dialog konfirmasi

### 8.3 Toast / Snackbar
```
Position: Bottom (di atas bottom tab bar)
Duration: 3 detik (info/success) / 5 detik (error)
Border radius: 8dp

Types:
✓ Success:  BG #4CAF50  teks putih
✕ Error:    BG #F44336  teks putih
⚠ Warning:  BG #FF9800  teks putih
ℹ Info:     BG #424242  teks putih

Contoh: "✓ Sawah berhasil disimpan"
Contoh: "✕ Gagal menyimpan. [Coba lagi]"
```

### 8.4 Haptic Feedback
```
Emergency Stop press:  Heavy impact
Mission Start:         Success haptic (medium)
Error terjadi:         Error haptic
Button press biasa:    Light impact
Jalur selesai (milestone): Medium haptic
```

---

## 9. Accessibility Guidelines

### 9.1 Touch Targets
```
Minimum:       48×48dp  — semua interactable elements
Preferred:     56×56dp  — tombol penting
Emergency:     80dp height minimum
D-Pad buttons: 72×72dp each
```

### 9.2 Color Contrast (WCAG)
```
textPrimary (#212121) on white:     16.1:1  ✅ AAA
primary (#7CB342) on white:          3.3:1  ✅ AA large text
white on emergency (#B71C1C):        8.5:1  ✅ AAA (safety-critical)
```

### 9.3 Screen Reader (TalkBack Android)
```
Wajib untuk setiap elemen interaktif:
├── accessibilityLabel: Deskripsi (Bahasa Indonesia)
├── accessibilityHint: Aksi yang terjadi
└── accessibilityRole: 'button' | 'text' | 'image' | 'progressbar'

Contoh Emergency Stop:
  accessibilityLabel="Tombol berhenti darurat"
  accessibilityRole="button"
  accessibilityHint="Tekan untuk menghentikan mesin segera"
```

### 9.4 Outdoor Readability
```
✅ Font minimum 14sp (body text)
✅ Font minimum 18sp Bold (aksi penting)
✅ Icon minimum 24dp
✅ High contrast mode support (Android system)
✅ Informasi status TIDAK hanya bergantung pada warna
   (gunakan label teks + ikon pendamping)
```

---

## 10. Error & Empty States

### 10.1 Empty States

**Belum Ada Sawah:**
```
Ilustrasi: 🌾 / SVG sawah kosong
Judul:     "Belum Ada Sawah"
Deskripsi: "Tambahkan sawah pertama Anda untuk mulai membuat misi tanam"
Aksi:      [+ Tambah Sawah]
```

**Belum Ada Riwayat:**
```
Ilustrasi: 📋
Judul:     "Belum Ada Riwayat Misi"
Deskripsi: "Riwayat misi Anda akan muncul di sini setelah misi selesai"
Aksi:      [Mulai Misi Baru] (link)
```

### 10.2 Error Banners

**Koneksi Terputus:**
```
⚠️ "Koneksi ke mesin terputus — Misi dijeda otomatis"
[Coba Sambung Ulang]
Color: warning (#FFF3E0 bg, #FF9800 border)
```

**GPS Tidak Tersedia:**
```
📍 "Sinyal GPS tidak tersedia — Akurasi posisi berkurang"
Color: info
Auto-dismiss saat GPS kembali
```

**Baterai Kritis (< 15%):**
```
Alert Dialog:
Title:  "⚠️ Baterai Kritis"
Body:   "Baterai mesin tersisa 15%. Segera kembali ke area pengisian."
Aksi:   [Hentikan Misi] [Lanjutkan (Risiko)]
```

### 10.3 Error Messages Catalog (Indonesian)
```
Koneksi:
├── "Koneksi terputus"
├── "Gagal terhubung ke mesin"
└── "Coba sambung ulang"

Validasi Form:
├── "Wajib diisi"
├── "Minimal 3 titik batas sawah"
├── "Luas sawah terlalu kecil (min. 100 m²)"
├── "Nilai harus antara [min] – [max]"
└── "Format koordinat tidak valid"

Misi:
├── "Gagal memulai misi"
├── "Misi tidak dapat dilanjutkan"
└── "Jalur tidak dapat dibuat dengan parameter ini"

Database:
├── "Gagal menyimpan data"
└── "Gagal memuat data — Coba refresh"
```

---

## 📚 Referensi

- [Material Design 3](https://m3.material.io/) — Komponen dan design tokens
- [Expo Router Docs](https://expo.github.io/router) — Navigation & routing
- [React Native Accessibility](https://reactnative.dev/docs/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/TR/WCAG21/) — Contrast & accessibility
- [Android Design Guidelines](https://developer.android.com/design)

---

**Document Owner:** Adi Maryanto  
**Created:** August 27, 2026  
**Last Updated:** August 27, 2026  
**Status:** ✅ Ready untuk implementasi Phase 1
