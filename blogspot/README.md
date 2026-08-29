# Panduan Instalasi & Penggunaan Template Blogspot PadiBot

File template resmi Blogger/Blogspot untuk **PadiBot** telah siap digunakan di:
`d:\DaVinci\Web Development\padi-bot\blogspot\padibot_template.xml`

---

## 🚀 Fitur Unggulan Template Blogspot PadiBot

1. **Struktur XML Blogger V3 Valid**:
   - Kompatibel penuh dengan parser XML Blogger modern (`xmlns:b`, `xmlns:data`, `xmlns:expr`).
   - Widget Blog (`Blog1`) yang responsif dan aman XML (tidak error tag/entity).
2. **Conditional Rendering Beranda vs Artikel**:
   - **Halaman Utama (Homepage)**: Menampilkan landing page interaktif lengkap (Hero 3D, Showcase Layar Aplikasi 3D, Live Mission Simulator Boustrophedon di Canvas HTML5, 3D Robot Schematics Inspector, Pola Jalur Tanam, dan Daftar Artikel Terkini).
   - **Halaman Artikel Tunggal (Single Post / Page)**: Tampilan pembaca artikel yang bersih, tipografi modern, komentar berulir (*threaded comments*), dan navigasi kembali yang mulus.
3. **Teknologi Terpadu**:
   - **Tailwind CSS Engine & Custom Styles** via `<b:skin>`.
   - **Lucide Icons** via CDN.
   - **HTML5 Canvas Simulator** dengan 3 Pola Jalur (Boustrophedon, Adaptif Kontur, Spiral).
   - **Interaktif 3D Tilt Card & Screen Showcase**.
   - **Scrollspy Navbar Dinamis**.

---

## 🛠️ Cara Memasang Template di Blogger / Blogspot

### Langkah 1: Buka Dashboard Blogger
1. Masuk ke [Blogger.com](https://www.blogger.com).
2. Pilih blog Anda (misalnya `padibot.blogspot.com`).

### Langkah 2: Backup Tema Lama (Opsional tapi Direkomendasikan)
1. Buka menu **Tema (Theme)** di sidebar kiri.
2. Klik tombol menu titik tiga (⋮) atau tanda panah ke bawah di sebelah tombol **SESUAIKAN (CUSTOMIZE)**.
3. Klik **Cadangkan (Backup)** dan unduh file XML tema lama Anda.

### Langkah 3: Pasang Tema PadiBot Baru
Terdapat dua cara mudah:

#### Opsi A: Melalui Menu Pulihkan (Restore) — *Paling Cepat*
1. Pada menu **Tema (Theme)**, klik panah di samping tombol **SESUAIKAN**.
2. Pilih **Pulihkan (Restore)**.
3. Klik **Upload** lalu pilih file:
   `D:\DaVinci\Web Development\padi-bot\blogspot\padibot_template.xml`
4. Tunggu beberapa detik hingga proses restore selesai.

#### Opsi B: Melalui Edit HTML — *Alternatif*
1. Pada menu **Tema (Theme)**, klik panah di samping tombol **SESUAIKAN**.
2. Pilih **Edit HTML**.
3. Hapus seluruh baris kode lama yang ada di dalam editor.
4. Buka file `padibot_template.xml` di text editor (VSCode / Notepad), salin seluruh kodenya (`Ctrl + A` lalu `Ctrl + C`).
5. Tempelkan (`Ctrl + V`) ke dalam editor HTML Blogger.
6. Klik ikon **Simpan (Save)** di sudut kanan atas.

---

## ⚙️ Kustomisasi Tambahan

- **Gambar & Aset**:
  Secara bawaan, aset gambar diarahkan ke link raw GitHub repositori PadiBot (`https://raw.githubusercontent.com/adimaryanto-stack/Padi-Bot/main/website/...`). Jika Anda ingin menggantinya dengan gambar yang di-upload ke Blogger sendiri, cukup ganti atribut `src="..."` pada template.
- **Tautan Download APK / GitHub**:
  Cari tombol `.download-trigger-btn` atau link `github.com/adimaryanto-stack/Padi-Bot` di dalam file XML dan sesuaikan dengan URL unduhan rilis APK Anda.
