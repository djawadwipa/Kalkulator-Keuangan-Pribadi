# Kalkulator Keuangan Pribadi

![Logo Cashflow Orbit](docs/branding/cashflow-orbit.webp)

Aplikasi Android native murni untuk pencatatan, perencanaan, perhitungan, dan analisis keuangan pribadi.

- **Package ID permanen:** `id.djawadwipa.kalkulatorkeuangan`
- **Repository:** public
- **UI:** Kotlin + Jetpack Compose + Material 3
- **Penyimpanan:** Room/SQLite privat perangkat
- **Minimum Android:** API 23
- **Compile/target SDK:** API 36
- **Internet permission:** tidak ada
- **WebView/PWA/browser wrapper:** tidak ada
- **Logo:** Cashflow Orbit, navy `#0F172A` dan emerald `#10B981`

## Status implementasi

Versi `0.9.0` menyediakan fondasi transaksi, perencanaan, tabungan, laporan, utang, investasi, Net Worth, Financial Freedom, dan portabilitas data:

- Dashboard arus kas bulan berjalan dan progres target pemasukan.
- Profil keuangan lokal, CRUD transaksi, rekening, kategori, pencarian, dan filter.
- Budget bulanan per kategori beserta realisasi, sisa, dan status.
- Target tabungan, dana darurat, target keuangan, dan riwayat setoran.
- Laporan arus kas, tren 12 bulan, evaluasi, dan snapshot lokal.
- Pengelolaan cicilan/utang serta simulasi Debt Snowball dan Debt Avalanche.
- Portofolio investasi, transaksi beli/jual/dividen/biaya, alokasi, return, dan simulasi pertumbuhan.
- Aset dan liabilitas manual, Net Worth terpadu, snapshot historis, dan Financial Health Score lanjutan.
- Proyeksi Financial Freedom dengan target modal, pendapatan pasif, milestone, dan estimasi tanggal tercapai.
- Asumsi safe withdrawal rate, return, inflasi, setoran bulanan, dan pilihan penyertaan saldo tabungan.
- Ekspor dan impor database melalui Storage Access Framework tanpa permission penyimpanan umum.
- Backup lokal terenkripsi AES-256-GCM dengan PBKDF2, salt dan nonce acak, serta autentikasi file.
- Validasi header SQLite, ukuran file, dan versi database sebelum impor.
- Tema navy–emerald dan adaptive launcher icon **Cashflow Orbit**.
- Kebijakan privasi di dalam aplikasi dan repository.
- Room Database 2.8.4 dengan migrasi non-destruktif v1 → v2 → v3 → v4 → v5 → v6 → v7 → v8.
- Unit test, lint, Dependency Review, debug/release CI, dan tes instrumentasi nyata pada emulator Android 15/API 35.
- Smoke test emulator mencakup peluncuran aplikasi, migrasi Room, ekspor database, backup terenkripsi, pemulihan data, dan `PRAGMA quick_check`.

Tahap berikutnya berfokus pada private release keystore, signed production release, privacy policy URL publik, pengujian perangkat fisik, dan Google Play Internal/Closed Testing.

Lihat [status proyek](PROJECT_STATUS.md) untuk batas implementasi saat ini.

## Build lokal

Gunakan JDK 17 dan Android SDK 36:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew assembleRelease
```

Untuk menjalankan seluruh tes instrumentasi pada emulator Android 15/API 35 di Linux dengan Android SDK terpasang:

```bash
bash scripts/run-connected-tests.sh
```

Script tersebut memasang system image resmi Android, membuat AVD, menunggu emulator selesai boot, lalu menjalankan `connectedDebugAndroidTest`.

Pada pemanggilan pertama, launcher `gradlew` memasang Gradle Wrapper 9.3.1 dari repository resmi melalui HTTPS. File JAR dan distribusi Gradle diverifikasi dengan SHA-256 resmi.

APK debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

## Ekspor, impor, dan backup

Menu **Lainnya → Ekspor, impor, dan backup** menyediakan:

- `.kkpdb` untuk portabilitas database tanpa enkripsi. File ini dapat memuat data keuangan sensitif.
- `.kkpbak` untuk backup terenkripsi AES-256-GCM dengan password minimal delapan karakter.
- Pemulihan database dengan pemeriksaan format, ukuran, dan versi sebelum data aktif diganti.
- Mulai ulang aplikasi setelah impor agar seluruh layar membuka database baru.

Password backup tidak disimpan dan tidak dapat dipulihkan. Simpan password serta file backup di lokasi yang berbeda dan tepercaya.

## Private release signing

Buat release keystore **di perangkat yang aman** dan jangan commit ke repository:

```bash
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -alias kalkulator-keuangan \
  -keyalg RSA -keysize 4096 -validity 10000
```

Tambahkan GitHub Actions Secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Contoh nilai Base64 satu baris:

```bash
base64 -w 0 release-keystore.jks
```

Jangan pernah menaruh hasil Base64, password, alias privat, atau file keystore di source code, issue, log, maupun artifact publik.

## Release

Workflow release berjalan saat tag `v*` dibuat atau melalui `workflow_dispatch`. Pipeline menghasilkan APK, AAB, SHA-256, laporan lint/unit test, dan dependency report. Pipeline memverifikasi package ID, status non-debuggable, permission, signature, serta checksum Gradle Wrapper dan distribusi.

## Supply-chain dan dependency audit

- Semua GitHub Actions dipatok ke full commit SHA.
- Dependabot memeriksa Gradle dan GitHub Actions setiap minggu.
- Dependency Review menolak kerentanan baru tingkat `moderate` atau lebih tinggi.
- Lisensi GPL-3.0 dan AGPL-3.0 ditolak pada dependency review.
- CI menjalankan lint, unit test, debug build, release/R8 build, dependency report, dan `connectedDebugAndroidTest` pada emulator Android resmi.
- Runner emulator menggunakan `sdkmanager`, `avdmanager`, dan `adb`, tanpa action emulator pihak ketiga.
- Repository tidak menyimpan keystore, password, token, atau API key.

## Batas perhitungan

Simulasi utang, investasi, nilai aset, Net Worth, dan Financial Freedom adalah alat perencanaan berdasarkan asumsi pengguna. Hasilnya bukan jaminan return, penilaian resmi aset, rekomendasi investasi, atau pengganti informasi resmi penyedia produk keuangan.

## Distribusi

Gunakan Google Play Internal/Closed Testing atau situs unduhan HTTPS. Jangan mendistribusikan keystore, password, backup, atau APK dari kanal HTTP.

## Privacy

Baca [Kebijakan Privasi](docs/PRIVACY_POLICY.md).

## Lisensi source

Repository public tidak otomatis memberikan izin penggunaan ulang. Belum ada lisensi open-source yang dipilih; hak cipta source tetap pada pemilik proyek sampai file `LICENSE` ditambahkan secara eksplisit.
