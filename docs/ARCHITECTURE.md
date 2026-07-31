# Arsitektur Android Native

## Prinsip

Aplikasi adalah Android native murni, single-activity, offline-first, dan tanpa permission internet. Tidak ada komponen web atau browser wrapper.

## Lapisan

- `ui`: Jetpack Compose, Material 3, state collection lifecycle-aware, dan ViewModel.
- `domain`: kalkulasi keuangan murni dan dapat diuji tanpa Android runtime.
- `data`: repository coroutine/Flow.
- `data/local`: SQLite privat melalui `SQLiteOpenHelper`.
- `model`: model transaksi, ringkasan dashboard, dan input health score.

## Aliran data

1. Pengguna memasukkan transaksi melalui Compose UI.
2. ViewModel memvalidasi state UI dan memanggil repository.
3. Repository menjalankan operasi database pada `Dispatchers.IO`.
4. Daftar transaksi dan ringkasan dipublikasikan melalui `StateFlow`.
5. UI mengamati state secara lifecycle-aware.

## Keputusan keamanan

- Tidak ada permission Android pada manifest.
- `allowBackup=false` dan data extraction dinonaktifkan.
- `usesCleartextTraffic=false`.
- Data tidak disinkronkan ke server.
- Nilai uang disimpan sebagai `Long` dalam satuan rupiah untuk menghindari ketidakpresisian floating point.
- Rasio hanya dihitung setelah validasi pembagian nol.
- Release memakai R8, resource shrinking, private keystore, dan `debuggable=false`.

## Evolusi database

Database dimulai pada versi 1. Setiap perubahan skema wajib memiliki migrasi eksplisit dan unit/instrumentation test sebelum `DATABASE_VERSION` dinaikkan. Destructive migration tidak boleh menjadi default untuk data keuangan pengguna.
