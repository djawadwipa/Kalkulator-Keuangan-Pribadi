# Release Checklist

## Sebelum rilis

- [ ] Version code dan version name dinaikkan.
- [ ] Unit test dan lint lulus.
- [ ] Dependency Review tidak menemukan kerentanan baru tingkat moderate atau lebih tinggi.
- [ ] Dependabot PR yang relevan telah ditinjau.
- [ ] Keystore release tersimpan offline dengan backup aman.
- [ ] Empat GitHub Actions Secrets tersedia dan tidak pernah dicetak ke log.
- [ ] Privacy policy publik tersedia melalui HTTPS.
- [ ] Store listing menyatakan data tersimpan lokal dan tidak dikirim keluar.

## Verifikasi artifact

- [ ] APK dan AAB bertanda tangan dengan key release yang benar.
- [ ] Package ID adalah `id.djawadwipa.kalkulatorkeuangan`.
- [ ] APK release tidak debuggable.
- [ ] Manifest tidak meminta permission.
- [ ] SHA-256 APK dan AAB cocok dengan `SHA256SUMS.txt`.
- [ ] Instalasi dan smoke test dilakukan pada minimal dua versi Android.

## Distribusi

- [ ] Utamakan Google Play Internal Testing, lalu Closed Testing.
- [ ] Alternatif unduhan hanya melalui situs HTTPS.
- [ ] Jangan pernah mengunggah keystore atau password sebagai artifact.
