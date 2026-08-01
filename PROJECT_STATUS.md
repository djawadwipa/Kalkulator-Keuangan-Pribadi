# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository: public, `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.5.0

- Dashboard arus kas dan progres target pemasukan bulan berjalan.
- Profil keuangan lokal dengan target pemasukan dan target tabungan.
- CRUD transaksi dengan tanggal, rekening, kategori, dan catatan.
- Pencarian dan filter transaksi.
- Master rekening dan kategori.
- Budget bulanan per kategori beserta realisasi, sisa, pemakaian, dan status.
- Target tabungan, dana darurat, dan target keuangan.
- CRUD target dan riwayat setoran dengan foreign key cascade.
- Progres target, sisa nominal, tenggat, status, dan estimasi tanggal tercapai.
- Laporan arus kas bulanan dengan pemasukan, pengeluaran, arus kas bersih, saving rate, dan jumlah transaksi.
- Perbandingan pemasukan, pengeluaran, serta arus kas dengan bulan sebelumnya.
- Tren arus kas 12 bulan.
- Rincian pengeluaran per kategori dan arus kas per rekening.
- Evaluasi pribadi bulanan: nilai, sorotan, dan rencana perbaikan.
- Snapshot laporan bulanan yang disimpan lokal dan dapat dihapus.
- Cakupan dana darurat, kepatuhan budget, dan Financial Health Score pada laporan.
- Room Database/SQLite privat dengan Flow.
- Migrasi non-destruktif v1 → v2 → v3 → v4 → v5 dan schema Room terkunci.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, kompilasi tes migrasi, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul berikutnya

1. Cicilan dan Utang
2. Debt Snowball
3. Debt Avalanche
4. Investasi
5. Simulasi Investasi
6. Aset dan Liabilitas
7. Net Worth Tracker
8. Financial Health Score lanjutan
9. Proyeksi Financial Freedom
10. Ekspor dan impor laporan

## Batas verifikasi saat ini

Android CI telah membangun APK debug dan release, menjalankan unit test dan lint, membuat dependency report, serta mengompilasi APK tes migrasi Room. Tes instrumentasi belum dijalankan pada emulator/perangkat oleh CI. Signed APK dan AAB produksi tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
