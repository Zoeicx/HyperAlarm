<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | **Bahasa Indonesia**

</div>

---

HyperAlarm adalah modul Xposed dengan **cakupan tunggal Jam Xiaomi**. Modul ini mengubah alarm jam menjadi pemicu skrip shell.

## Aplikasi target

Hanya versi di bawah ini yang diuji. Untuk versi lain, uji sendiri, kirim APK agar bisa diadaptasi, atau perbarui/turunkan ke versi berikut:

- Jam Xiaomi `com.android.deskclock`
- Versi `18.28.0`, kode versi `1306002800`

## Pemasangan

1. Siapkan root (KernelSU / Magisk) dan LSPosed (Xposed API 101 atau lebih baru).
2. Pasang modul ini, lalu aktifkan HyperAlarm di LSPosed.
3. Tandai cakupan hanya `com.android.deskclock`. Jangan menambah paket lain.
4. Hentikan paksa Jam Xiaomi lalu biarkan prosesnya hidup lagi, kalau tidak modul tidak jalan.

## Pemakaian

- Tugas baru: tombol `+` di kanan bilah aksi atas.
- Label alarm adalah nama skrip dan harus berakhiran `.sh`:
  - `alert.sh`: berjalan senyap saat waktunya, tanpa dering dan tanpa layar alarm
  - `@alert.sh`: menjalankan skrip sekaligus mempertahankan layar alarm dan nada dering
- Tanda `@` disembunyikan di daftar; sakelar «Nada dering» memindahkan tugas antara dua mode.
- «Jalankan sekarang» untuk mencoba skrip tanpa menunggu waktunya.
- Tugas shift menyala per shift, dihitung dari tanggal mulai dan siklusnya.

## Aturan skrip

Skrip disimpan di `/data/adb/alarm_shell/scripts/`, dan nama di label harus sama dengan nama berkas.

```text
/data/adb/alarm_shell/scripts/      # direktori skrip
/data/adb/alarm_shell/password      # sandi layar kunci
/data/adb/alarm_shell/unlock_phone  # skrip buka kunci bawaan
```

- Akhir baris harus LF. Berkas dengan CRLF dari Windows langsung gagal.
- Skrip berjalan sebagai root di ruang nama utama sistem, jadi bisa melihat direktori data aplikasi lain.
- Setiap eksekusi meninggalkan log: terlihat di «Log skrip» dalam aplikasi, sekaligus dicerminkan ke
  `/sdcard/Android/data/com.hyperalarm/files/script_logs/<nama skrip>/`.
- Sebelum tugas senyap, modul membuka layar dengan sandi layar kunci kalau sudah diatur.

## Pengaturan

- **Jam hanya menampilkan alarm**: daftar Jam Xiaomi hanya menyisakan alarm sungguhan, tugas skrip disembunyikan.
- **Sembunyikan tugas berdering**: ikut menyembunyikan tugas yang nada deringnya aktif.
- Tingkat log / Log aplikasi / Log skrip: naikkan tingkatnya dulu saat menelusuri masalah.
- **Sandi layar kunci**: dipakai membuka layar sebelum tugas senyap; kosong berarti tidak diubah.
- Panduan mulai ulang, salin direktori skrip, cadangkan dan pulihkan konfigurasi modul.

## Batasan yang diketahui

- Hanya diuji di Android 16 / HyperOS 3 / Jam 18.28.0; kombinasi lain tanggung jawab Anda.
- Mode shift bergantung pada isi internal Jam, jadi perlu diuji ulang setelah jam diperbarui.
- Tanpa root modul tidak bekerja; halaman pengaturan akan memberi tahu.
- Log skrip dicerminkan ke penyimpanan eksternal dan bisa dibaca root maupun adb shell. Jangan menulis rahasia ke keluaran skrip.

## Pertanyaan umum

- **Tugas sudah ditambah tapi jam tidak bereaksi**: pastikan modul aktif di LSPosed, cakupannya hanya Jam Xiaomi, dan proses jam sudah dimulai ulang.
- **Tugas tidak muncul di daftar jam**: itu kerja «Jam hanya menampilkan alarm», matikan saja.
- **Waktunya lewat tanpa eksekusi**: lihat log skrip di aplikasi; skrip hilang atau label salah ketik akan dilaporkan di sana.

## Lisensi

- Kode milik repositori ini: **AGPL-3.0** (lihat `../LICENSE`)
- Komponen pihak ketiga dan lisensinya: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Terima kasih

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): titik awal proyek ini; sebagian besar antarmuka MIUIX dan kerangka pengaturan berasal dari sana
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): komponen antarmuka bergaya Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): API root dan Xposed
- Semua yang membuka issue dan mengirim log
