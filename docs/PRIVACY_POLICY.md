# Kebijakan Privasi — Kalkulator Keuangan Pribadi

Terakhir diperbarui: 1 Agustus 2026

## Ringkasan

Kalkulator Keuangan Pribadi dirancang sebagai aplikasi Android native yang bekerja secara lokal. Aplikasi tidak meminta permission internet, tidak menggunakan iklan, analytics, pelacak, WebView, atau layanan pihak ketiga untuk mengumpulkan data pengguna.

## Data yang disimpan

Data keuangan yang dimasukkan pengguna—termasuk transaksi, kategori, rekening, budget, target, utang, investasi, aset, liabilitas, Net Worth, dan asumsi Financial Freedom—disimpan dalam penyimpanan privat aplikasi di perangkat Android pengguna.

## Pengiriman dan pembagian data

Aplikasi tidak mengirim, menjual, menyewakan, atau membagikan data keuangan kepada pengembang maupun pihak ketiga.

## Ekspor, impor, dan backup

Aplikasi menyediakan ekspor dan impor yang sepenuhnya dipicu pengguna melalui pemilih dokumen Android. Aplikasi tidak memilih tujuan penyimpanan secara otomatis.

- File `.kkpdb` berisi database tanpa enkripsi dan dapat memuat data keuangan sensitif.
- File `.kkpbak` dienkripsi dengan AES-256-GCM menggunakan kunci yang diturunkan dari password pengguna melalui PBKDF2.
- Password backup tidak disimpan, tidak dikirim, dan tidak dapat dipulihkan oleh pengembang.
- Pengguna bertanggung jawab menjaga file ekspor, file backup, dan password di lokasi yang aman.

Backup cloud dan transfer data otomatis Android tetap dinonaktifkan. Fitur backup aplikasi hanya berjalan ketika pengguna memilih dokumen dan menjalankan perintah secara eksplisit.

## Penghapusan data

Pengguna dapat menghapus transaksi lokal melalui menu **Lainnya**. Menghapus aplikasi juga menghapus data privat aplikasi dari perangkat. File ekspor atau backup yang telah disimpan di luar penyimpanan privat aplikasi harus dihapus sendiri oleh pengguna melalui aplikasi pengelola dokumen.

## Keamanan

Build release tidak debuggable, diperkecil dengan R8, ditandatangani menggunakan private release keystore, dan hanya didistribusikan melalui kanal HTTPS atau Google Play testing. Backup terenkripsi menggunakan authenticated encryption sehingga perubahan file atau password yang salah ditolak.

## Batasan

Perhitungan, simulasi, Financial Health Score, Net Worth, dan proyeksi Financial Freedom adalah alat bantu edukasi serta pengelolaan pribadi, bukan nasihat keuangan profesional atau jaminan hasil.

## Kontak

Pertanyaan privasi dapat disampaikan melalui kanal kontak resmi AV Sinergi yang dicantumkan pada halaman distribusi aplikasi.
