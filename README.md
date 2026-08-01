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

Versi `0.7.0` menyediakan fondasi transaksi, perencanaan, tabungan, laporan, utang, serta investasi yang dapat dibuild:

- Dashboard arus kas bulan berjalan dan progres target pemasukan.
- Profil keuangan lokal: nama/panggilan, target pemasukan, dan target tabungan.
- CRUD pemasukan/pengeluaran dengan tanggal, rekening, kategori, serta catatan.
- Master rekening dan kategori dengan relasi foreign key.
- Pencarian dan filter transaksi.
- Budget bulanan per kategori pengeluaran beserta realisasi dan status.
- Target tabungan, dana darurat, dan target keuangan dengan CRUD lengkap.
- Riwayat setoran per target, progres, sisa nominal, tenggat, status, dan estimasi tanggal tercapai.
- Laporan arus kas bulanan, perbandingan bulan sebelumnya, dan tren 12 bulan.
- Rincian kategori pengeluaran, aktivitas per rekening, evaluasi pribadi, dan snapshot laporan lokal.
- CRUD cicilan dan utang dengan riwayat pembayaran.
- Simulasi **Debt Snowball** dan **Debt Avalanche** beserta estimasi bebas utang dan bunga.
- CRUD aset investasi: deposito, obligasi/SBN, reksa dana, saham, emas, kripto, dan aset lain.
- Transaksi beli, jual, dividen/hasil, dan biaya investasi.
- Perhitungan unit, harga rata-rata, cost basis, nilai pasar, keuntungan/rugi, return, serta alokasi portofolio.
- Harga pasar dan target alokasi diperbarui manual tanpa akses internet.
- Simulasi investasi dengan modal awal, setoran bulanan, compounding, return tahunan, durasi, dan inflasi.
- Cakupan dana darurat, budget adherence, expense ratio, dan Financial Health Score.
- Tema navy–emerald dan adaptive launcher icon **Cashflow Orbit**.
- Kebijakan privasi di dalam aplikasi dan repository.
- Room Database 2.8.4 dengan migrasi non-destruktif v1 → v2 → v3 → v4 → v5 → v6 → v7.
- Unit test, kompilasi tes migrasi Android, lint, dependency review, debug/release CI, dan signed release pipeline.

Modul berikut telah dipetakan untuk iterasi selanjutnya: aset-liabilitas, net worth, Financial Health Score lanjutan, financial freedom, serta ekspor/impor dan backup data.

Lihat [status proyek](PROJECT_STATUS.md) untuk batas implementasi saat ini.

## Build lokal

Gunakan JDK 17 dan Android SDK 36. Jalankan debug/test terlebih dahulu agar schema Room baru dihasilkan sebelum build release:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew assembleRelease
```

Pada pemanggilan pertama, launcher `gradlew` memasang Gradle Wrapper 9.3.1 dari repository resmi melalui HTTPS. File JAR dan distribusi Gradle diverifikasi dengan SHA-256 resmi.

APK debug tersedia di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

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

Contoh membuat nilai Base64 satu baris:

```bash
base64 -w 0 release-keystore.jks
```

Jangan pernah menaruh hasil Base64, password, alias privat, atau file keystore di source code, issue, log, maupun artifact publik.

## Release

Workflow release berjalan saat tag `v*` dibuat atau melalui `workflow_dispatch`. Pipeline menghasilkan:

- `app-release.apk`
- `app-release.aab`
- `SHA256SUMS.txt`
- laporan lint dan unit test
- dependency report release

Pipeline memverifikasi package ID, status non-debuggable, permission, signature APK/AAB, serta checksum Gradle Wrapper dan distribusi.

## Supply-chain dan dependency audit

- Semua GitHub Actions dipatok ke full commit SHA.
- Dependabot memeriksa Gradle dan GitHub Actions setiap minggu.
- Dependency Review menolak kerentanan baru tingkat `moderate` atau lebih tinggi.
- Lisensi GPL-3.0 dan AGPL-3.0 ditolak pada dependency review.
- CI membuat dependency report dan menjalankan lint, unit test, debug build, instrumentation-test APK, serta release/R8 build.
- Repository tidak menyimpan keystore, password, token, atau API key.

## Batas simulasi

Simulasi pelunasan utang dan investasi adalah alat perencanaan berdasarkan asumsi yang dimasukkan pengguna. Hasilnya bukan jaminan return, rekomendasi investasi, atau pengganti tagihan dan informasi resmi penyedia produk keuangan.

## Distribusi

Gunakan Google Play Internal/Closed Testing atau situs unduhan HTTPS. Jangan mendistribusikan keystore, password, atau APK dari kanal HTTP.

## Privacy

Baca [Kebijakan Privasi](docs/PRIVACY_POLICY.md).

## Lisensi source

Repository public tidak otomatis memberikan izin penggunaan ulang. Belum ada lisensi open-source yang dipilih; hak cipta source tetap pada pemilik proyek sampai file `LICENSE` ditambahkan secara eksplisit.
