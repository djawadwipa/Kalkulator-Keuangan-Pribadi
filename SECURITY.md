# Security Policy

## Supported version

Hanya versi release terbaru yang didukung.

## Reporting a vulnerability

Jangan mempublikasikan detail kerentanan atau data sensitif di GitHub Issues. Gunakan kanal kontak privat pemilik repository.

## Release controls

- Private keystore tidak pernah disimpan di repository.
- Secret hanya berada di GitHub Actions Secrets.
- Release build harus `debuggable=false`.
- Manifest release tidak boleh meminta permission yang tidak diperlukan.
- Cleartext traffic dinonaktifkan.
- APK dan AAB diverifikasi dan disertai SHA-256.
