# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository target: public
- Repository name: `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.2.0

- Dashboard arus kas bulan berjalan.
- CRUD transaksi pemasukan/pengeluaran lokal dengan tanggal, rekening, kategori, dan catatan.
- Pencarian dan filter transaksi.
- Master rekening dan kategori.
- Room Database dengan Flow dan migrasi non-destruktif v1 ke v2.
- Saving rate, expense ratio, dan Financial Health Score.
- Database Room/SQLite privat perangkat.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Konfigurasi release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, kompilasi tes migrasi, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul workbook yang dipetakan untuk implementasi bertahap

1. Profil Keuangan
2. Pemasukan Bulanan lanjutan
3. Budget Bulanan
4. Analisis Pengeluaran lanjutan
5. Tabungan & Dana Darurat
6. Cicilan & Utang
7. Debt Snowball
8. Debt Avalanche
9. Target Keuangan
10. Cash Flow Bulanan
11. Investasi
12. Simulasi Investasi
13. Aset & Liabilitas
14. Net Worth Tracker
15. Financial Health Score lanjutan
16. Proyeksi Financial Freedom
17. Laporan Bulanan

## Batas verifikasi lingkungan saat ini

Android CI telah membangun APK debug dan release, menjalankan unit test, lint, dependency report, serta mengompilasi tes migrasi Room. Signed release tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
