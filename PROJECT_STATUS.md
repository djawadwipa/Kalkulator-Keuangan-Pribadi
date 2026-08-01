# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository: public, `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.4.0

- Dashboard arus kas dan progres target pemasukan bulan berjalan.
- Profil keuangan lokal dengan target pemasukan dan target tabungan.
- CRUD transaksi dengan tanggal, rekening, kategori, dan catatan.
- Pencarian dan filter transaksi.
- Master rekening dan kategori.
- Budget bulanan per kategori beserta realisasi, sisa, pemakaian, dan status.
- Navigasi bulan untuk budget dan analisis.
- Analisis pengeluaran: total, rata-rata harian, kategori terbesar, dan komposisi kategori.
- Target tabungan, dana darurat, dan target keuangan.
- CRUD target dan riwayat setoran dengan foreign key cascade.
- Progres target, sisa nominal, tenggat, status, dan estimasi tanggal tercapai.
- Cakupan dana darurat dalam bulan berdasarkan pengeluaran bulan berjalan.
- Emergency-fund coverage terintegrasi ke Financial Health Score.
- Room Database/SQLite privat dengan Flow.
- Migrasi non-destruktif v1 → v2 → v3 → v4 dan schema Room terkunci.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, kompilasi tes migrasi, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul berikutnya

1. Cash Flow dan Laporan Bulanan lanjutan
2. Cicilan dan Utang
3. Debt Snowball
4. Debt Avalanche
5. Investasi
6. Simulasi Investasi
7. Aset dan Liabilitas
8. Net Worth Tracker
9. Financial Health Score lanjutan
10. Proyeksi Financial Freedom

## Batas verifikasi saat ini

Android CI telah membangun APK debug dan release, menjalankan unit test dan lint, membuat dependency report, serta mengompilasi APK tes migrasi Room. Tes instrumentasi belum dijalankan pada emulator/perangkat oleh CI. Signed APK dan AAB produksi tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
