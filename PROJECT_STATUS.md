# Status Proyek

## Keputusan permanen

- Nama aplikasi: **Kalkulator Keuangan Pribadi**
- Package ID: `id.djawadwipa.kalkulatorkeuangan`
- Repository: public, `Kalkulator-Keuangan-Pribadi`
- Logo: **Cashflow Orbit**, navy `#0F172A` dan emerald `#10B981`
- Platform: Android native Kotlin + Jetpack Compose

## Sudah tersedia pada v0.7.0

- Dashboard arus kas dan progres target pemasukan bulan berjalan.
- Profil keuangan lokal dengan target pemasukan dan target tabungan.
- CRUD transaksi dengan tanggal, rekening, kategori, dan catatan.
- Pencarian dan filter transaksi.
- Master rekening dan kategori.
- Budget bulanan per kategori beserta realisasi, sisa, pemakaian, dan status.
- Target tabungan, dana darurat, dan target keuangan.
- CRUD target dan riwayat setoran dengan foreign key cascade.
- Progres target, sisa nominal, tenggat, status, dan estimasi tanggal tercapai.
- Laporan arus kas bulanan, tren 12 bulan, evaluasi, dan snapshot lokal.
- CRUD cicilan dan utang beserta riwayat pembayaran.
- Simulasi Debt Snowball dan Debt Avalanche.
- Anggaran pembayaran bulanan, estimasi bebas utang, urutan pelunasan, dan estimasi total bunga.
- CRUD aset investasi dan harga pasar manual.
- Transaksi beli, jual, dividen/hasil, dan biaya investasi.
- Unit, harga rata-rata, cost basis, nilai pasar, keuntungan/rugi, return, serta alokasi aktual dan target.
- Ringkasan pendapatan dan biaya investasi.
- Simulasi investasi dengan compounding bulanan, setoran rutin, return, durasi, dan inflasi.
- Cakupan dana darurat, kepatuhan budget, dan Financial Health Score pada laporan.
- Room Database/SQLite privat dengan Flow.
- Migrasi non-destruktif v1 → v2 → v3 → v4 → v5 → v6 → v7 dan schema Room terkunci.
- Relasi pembayaran utang dan transaksi investasi menggunakan foreign key cascade.
- Tema Material 3 navy–emerald dan adaptive launcher icon.
- Privacy policy di dalam aplikasi dan repository.
- Release non-debuggable, R8, shrink resources, dan private signing.
- CI, lint, unit test, kompilasi tes migrasi, dependency review, APK/AAB release, dan SHA-256 artifact.
- GitHub Actions dipatok ke commit SHA.
- Gradle Wrapper 9.3.1 diverifikasi dengan SHA-256 resmi.

## Modul berikutnya

1. Aset dan Liabilitas
2. Net Worth Tracker
3. Financial Health Score lanjutan
4. Proyeksi Financial Freedom
5. Ekspor dan impor data
6. Backup lokal terenkripsi
7. Pengujian instrumentasi pada emulator/perangkat
8. Signed production release
9. Privacy policy URL publik
10. Google Play Internal/Closed Testing

## Batas verifikasi saat ini

Android CI telah membangun APK debug dan release, menjalankan unit test dan lint, membuat dependency report, serta mengompilasi APK tes migrasi Room. Tes instrumentasi belum dijalankan pada emulator/perangkat oleh CI. Harga investasi dimasukkan manual dan simulasi return merupakan alat perencanaan, bukan jaminan hasil atau rekomendasi investasi. Signed APK dan AAB produksi tetap membutuhkan empat GitHub Actions Secrets yang dijelaskan di README.
