# Kalkulator Keuangan Pribadi

![Logo Cashflow Orbit](docs/branding/cashflow-orbit.webp)

Aplikasi Android native murni untuk pencatatan, perhitungan, dan analisis keuangan pribadi.

- **Package ID permanen:** `id.djawadwipa.kalkulatorkeuangan`
- **Repository target:** public
- **UI:** Kotlin + Jetpack Compose + Material 3
- **Penyimpanan:** SQLite privat perangkat
- **Minimum Android:** API 23
- **Compile/target SDK:** API 37
- **Internet permission:** tidak ada
- **WebView/PWA/browser wrapper:** tidak ada
- **Logo:** Cashflow Orbit, navy `#0F172A` dan emerald `#10B981`

## Status implementasi

Versi `0.1.0` menyediakan fondasi yang dapat dibuild:

- Dashboard arus kas bulan berjalan.
- Pencatatan pemasukan dan pengeluaran lokal.
- Filter transaksi.
- Saving rate dan expense ratio.
- Financial Health Score yang aman terhadap nilai kosong dan pembagian nol.
- Tema navy–emerald dan adaptive launcher icon **Cashflow Orbit**.
- Kebijakan privasi di dalam aplikasi dan repository.
- Unit test, Android lint, dependency review, debug CI, serta signed release pipeline.

Modul workbook Ultimate berikut telah dipetakan untuk iterasi selanjutnya: budget, tabungan dan dana darurat, cicilan dan utang, snowball, avalanche, target keuangan, cash flow, investasi, simulasi investasi, aset-liabilitas, net worth, financial freedom, dan laporan bulanan.

Lihat [status proyek](PROJECT_STATUS.md) untuk batas implementasi saat ini.

## Build lokal

Gunakan JDK 17 dan Android SDK 37.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Pada pemanggilan pertama, launcher `gradlew` memasang Gradle Wrapper 9.3.1 dari distribusi resmi melalui HTTPS. File JAR diverifikasi terhadap SHA-256 resmi sebelum dijalankan. Distribusi Gradle juga diverifikasi melalui `distributionSha256Sum` di `gradle-wrapper.properties`.

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
- laporan lint
- laporan unit test
- dependency report release

Pipeline juga memverifikasi:

- package ID benar,
- release tidak debuggable,
- manifest tidak meminta permission,
- signature APK dan AAB valid,
- Gradle Wrapper dan distribusi cocok dengan SHA-256 resmi.

## Supply-chain dan dependency audit

- Semua GitHub Actions dipatok ke full commit SHA.
- Dependabot memeriksa Gradle dan GitHub Actions setiap minggu.
- Dependency Review menolak kerentanan baru tingkat `moderate` atau lebih tinggi.
- Lisensi GPL-3.0 dan AGPL-3.0 ditolak pada dependency review.
- CI membuat dependency report dan menjalankan Android lint serta unit test.
- Repository tidak menyimpan keystore, password, token, atau API key.

## Distribusi

Gunakan Google Play Internal/Closed Testing atau situs unduhan HTTPS. Jangan mendistribusikan keystore, password, atau APK dari kanal HTTP.

## Privacy

Baca [Kebijakan Privasi](docs/PRIVACY_POLICY.md).

## Lisensi source

Repository dapat dibuat public tanpa otomatis memberikan izin penggunaan ulang. Belum ada lisensi open-source yang dipilih; hak cipta source tetap pada pemilik proyek sampai sebuah file `LICENSE` ditambahkan secara eksplisit.
