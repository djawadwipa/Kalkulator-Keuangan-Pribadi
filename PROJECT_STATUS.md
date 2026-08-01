# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository: public, `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.9.0

- Dashboard, profil, CRUD transaksi, rekening, kategori, pencarian, dan filter.
- Budget bulanan, analisis pengeluaran, laporan arus kas, evaluasi, dan snapshot.
- Target tabungan, dana darurat, target keuangan, dan riwayat setoran.
- Cicilan/utang, riwayat pembayaran, Debt Snowball, dan Debt Avalanche.
- Portofolio investasi, transaksi, harga pasar manual, alokasi, return, dan simulasi.
- Aset/liabilitas manual, Net Worth terpadu, snapshot historis, dan Financial Health Score lanjutan.
- Proyeksi Financial Freedom: target modal, pendapatan pasif, progres, milestone, dan estimasi tanggal tercapai.
- Asumsi safe withdrawal rate, return, inflasi, setoran bulanan, dan penyertaan saldo tabungan.
- Ekspor/impor database melalui Storage Access Framework tanpa permission penyimpanan umum.
- Backup lokal terenkripsi AES-256-GCM dengan PBKDF2, salt dan nonce acak, serta autentikasi file.
- Pemeriksaan header SQLite, ukuran, dan versi database sebelum impor.
- Room Database/SQLite privat dengan migrasi non-destruktif v1 → v2 → v3 → v4 → v5 → v6 → v7 → v8.
- Tema Material 3 navy–emerald, adaptive launcher icon, privacy policy, R8, dan private signing pipeline.
- CI, lint, unit test, kompilasi APK tes migrasi, Dependency Review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA dan Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Pekerjaan berikutnya

1. Menjalankan tes instrumentasi pada emulator/perangkat, bukan hanya mengompilasi APK tes.
2. Menyiapkan private release keystore dan empat GitHub Actions Secrets.
3. Menjalankan signed APK/AAB production release.
4. Menerbitkan privacy policy pada URL HTTPS publik.
5. Menyiapkan screenshot, deskripsi, Data Safety form, dan materi Play Store.
6. Menjalankan Google Play Internal Testing dan Closed Testing.
7. Menentukan lisensi source.
8. Melakukan smoke test ekspor/impor dan backup pada perangkat fisik.

## Batas verifikasi saat ini

Android CI membangun APK debug dan release, menjalankan unit test dan lint, membuat dependency report, serta mengompilasi APK tes migrasi Room. Tes instrumentasi belum dijalankan pada emulator/perangkat. Ekspor/impor dan backup telah dikompilasi serta diuji pada lapisan codec, tetapi pemulihan database tetap perlu smoke test perangkat. Password backup tidak dapat dipulihkan. Financial Freedom adalah proyeksi berdasarkan asumsi dan bukan jaminan hasil. Signed APK dan AAB produksi tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
