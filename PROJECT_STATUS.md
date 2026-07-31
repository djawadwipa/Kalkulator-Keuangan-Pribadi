# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository target: public
- Repository name: `Kalkulator-Keuangan-Pribadi-Android`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.1.0

- Dashboard arus kas bulan berjalan.
- Input dan penyimpanan transaksi pemasukan/pengeluaran lokal.
- Filter transaksi.
- Saving rate, expense ratio, dan Financial Health Score.
- SQLite privat perangkat.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Konfigurasi release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul workbook yang dipetakan untuk implementasi bertahap

1. Profil Keuangan
2. Master Data
3. Pemasukan Bulanan
4. Budget Bulanan
5. Pengeluaran Harian
6. Analisis Pengeluaran
7. Tabungan & Dana Darurat
8. Cicilan & Utang
9. Debt Snowball
10. Debt Avalanche
11. Target Keuangan
12. Cash Flow Bulanan
13. Investasi
14. Simulasi Investasi
15. Aset & Liabilitas
16. Net Worth Tracker
17. Financial Health Score lanjutan
18. Proyeksi Financial Freedom
19. Laporan Bulanan

## Batas verifikasi lingkungan saat ini

Source telah melalui pemeriksaan statis lokal. Build APK/AAB penuh dijalankan oleh GitHub Actions setelah repository tersedia dan Android dependencies dapat diunduh. Signed release membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
