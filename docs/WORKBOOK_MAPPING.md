# Pemetaan Workbook Ultimate

Workbook **Kalkulator Keuangan Pribadi Ultimate** digunakan sebagai referensi kebutuhan dan rumus, bukan dibundel sebagai database pengguna.

| Modul workbook | Target fitur Android | Status v0.1.0 |
|---|---|---|
| Dashboard Utama | Ringkasan arus kas dan skor | Fondasi aktif |
| Profil Keuangan | Profil dan tujuan finansial | Terpetakan |
| Master Data | Kategori, rekening, metode | Terpetakan |
| Pemasukan Bulanan | Transaksi pemasukan | Fondasi aktif |
| Budget Bulanan | Batas per kategori | Terpetakan |
| Pengeluaran Harian | Transaksi pengeluaran | Fondasi aktif |
| Analisis Pengeluaran | Rasio dan tren kategori | Fondasi rasio aktif |
| Tabungan & Dana Darurat | Target dan progres | Terpetakan |
| Cicilan & Utang | Jadwal dan rasio utang | Terpetakan |
| Debt Snowball | Prioritas saldo terkecil | Terpetakan |
| Debt Avalanche | Prioritas bunga tertinggi | Terpetakan |
| Target Keuangan | Target nominal dan tenggat | Terpetakan |
| Cash Flow Bulanan | Rekap per bulan | Fondasi bulan aktif |
| Investasi | Portofolio dan return | Terpetakan |
| Simulasi Investasi | Proyeksi compounding | Terpetakan |
| Aset & Liabilitas | Posisi aset dan kewajiban | Terpetakan |
| Net Worth Tracker | Nilai bersih dan tren | Terpetakan |
| Financial Health Score | Skor 0–100 | Fondasi aktif |
| Proyeksi Financial Freedom | Target dan estimasi waktu | Terpetakan |
| Laporan Bulanan | Evaluasi dan insight | Terpetakan |

## Aturan migrasi rumus

- Formula Excel ditulis ulang sebagai fungsi Kotlin teruji.
- Nilai kosong, pembagian nol, nilai negatif, bunga, inflasi, dan periode harus divalidasi.
- Error formula seperti `#VALUE!` tidak boleh diteruskan ke UI.
- Data contoh tidak otomatis menjadi data pengguna.
