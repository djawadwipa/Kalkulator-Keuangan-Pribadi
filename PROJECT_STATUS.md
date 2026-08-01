# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository: public, `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.6.0

- Dashboard arus kas dan progres target pemasukan bulan berjalan.
- Profil keuangan lokal dengan target pemasukan dan target tabungan.
- CRUD transaksi dengan tanggal, rekening, kategori, dan catatan.
- Pencarian dan filter transaksi.
- Master rekening dan kategori.
- Budget bulanan per kategori beserta realisasi, sisa, pemakaian, dan status.
- Target tabungan, dana darurat, dan target keuangan.
- CRUD target dan riwayat setoran dengan foreign key cascade.
- Progres target, sisa nominal, tenggat, status, dan estimasi tanggal tercapai.
- Laporan arus kas bulanan dan perbandingan dengan bulan sebelumnya.
- Tren arus kas 12 bulan, rincian kategori, dan arus kas per rekening.
- Evaluasi pribadi dan snapshot laporan bulanan lokal.
- CRUD cicilan dan utang dengan kreditur, jenis, bunga tahunan, minimum pembayaran, jatuh tempo, serta target pelunasan.
- Riwayat pembayaran utang dengan saldo dan progres otomatis.
- Simulasi Debt Snowball dan Debt Avalanche.
- Anggaran pembayaran bulanan, estimasi bebas utang, urutan pelunasan, dan estimasi total bunga.
- Cakupan dana darurat, kepatuhan budget, dan Financial Health Score pada laporan.
- Room Database/SQLite privat dengan Flow.
- Migrasi non-destruktif v1 → v2 → v3 → v4 → v5 → v6 dan schema Room terkunci.
- Relasi pembayaran utang menggunakan foreign key cascade.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, kompilasi tes migrasi, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul berikutnya

1. Investasi dan portofolio
2. Simulasi investasi
3. Aset dan Liabilitas
4. Net Worth Tracker
5. Financial Health Score lanjutan
6. Proyeksi Financial Freedom
7. Ekspor dan impor data
8. Backup lokal terenkripsi
9. Pengujian instrumentasi pada emulator/perangkat
10. Signed production release

## Batas verifikasi saat ini

Android CI telah membangun APK debug dan release, menjalankan unit test dan lint, membuat dependency report, serta mengompilasi APK tes migrasi Room. Tes instrumentasi belum dijalankan pada emulator/perangkat oleh CI. Perhitungan Snowball dan Avalanche adalah simulasi perencanaan berbasis bunga bulanan dan anggaran tetap, bukan pengganti rincian tagihan resmi kreditur. Signed APK dan AAB produksi tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
