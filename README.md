# 🕌 Jadwal Sholat Indonesia

Aplikasi Android modern berbasis **Kotlin** dan **Jetpack Compose** untuk menampilkan jadwal waktu sholat secara akurat di seluruh wilayah Indonesia. Perhitungan waktu sholat menggunakan metode astronomis dari library [Adhan](https://github.com/batoulapps/adhan-java) yang disesuaikan dengan standar Kementerian Agama (Kemenag) Republik Indonesia.

---

## 🚀 Fitur Utama

- **Perhitungan Akurat (Standar Kemenag RI):**
  - Subuh: 20.0°
  - Isya: 18.0°
  - Mazhab: Syafi'i
  - Penyesuaian menit tambahan (+2 menit) untuk ikhtiyat.
- **Koreksi Elevasi Otomatis:** Mengambil data ketinggian mdpl (meter di atas permukaan laut) melalui Open-Meteo API untuk memperhitungkan sudut dip matahari pada waktu Maghrib dan Syuruq.
- **Deteksi Lokasi GPS & Pencarian Kota:** Mendukung GPS otomatis via `FusedLocationProviderClient` dan pencarian nama kota via Geocoder.
- **Notifikasi & Alarm Sholat:**
  - Pengingat 10 menit sebelum waktu sholat tiba.
  - Notifikasi adzan saat masuk waktu sholat.
  - Otomatis mendaftarkan ulang alarm setelah perangkat di-reboot (`BootReceiver`).
- **Countdown Real-Time:** Menampilkan waktu tersisa hingga waktu sholat berikutnya.
- **Kalender Hijriyah & Masehi:** Menampilkan penanggalan Masehi dan Hijriyah secara real-time.
- **Home Screen Widget:** Widget ringkas untuk memantau jadwal 5 waktu sholat langsung dari layar utama perangkat.

---

## 🛠️ Teknologi & Stack

- **Bahasa:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Lokasi & GPS:** Google Play Services Location (`com.google.android.gms:play-services-location`)
- **Kalkulasi Sholat:** Adhan Java Library (`com.batoulapps.adhan:adhan:1.2.1`)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36

---

## 📂 Struktur Project

```text
app/src/main/java/id/my/ionlinestudio/jadwalsholatindonesia/
├── MainActivity.kt          # Activity utama & Komponen UI Compose
├── Receivers.kt             # AlarmReceiver, BootReceiver, & AlarmScheduler
├── JadwalSholatWidget.kt    # App Widget Provider
└── ui/theme/                # Tema & Warna Material 3
```

---

## 📖 Cara Menjalankan

1. Clone repository ini:
   ```bash
   git clone https://github.com/wisya/jadwal-sholat-kotlin.git
   ```
2. Buka project menggunakan **Android Studio (Ladybug / terbaru)**.
3. Sync Gradle project.
4. Jalankan pada Emulator atau Perangkat Android (Min SDK 26).

---

## 📜 Lisensi

Project ini dibuat untuk keperluan pembelajaran dan pengembangan terbuka. Silakan berkontribusi atau menyesuaikan sesuai kebutuhan!
