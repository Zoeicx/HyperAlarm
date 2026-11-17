Kebijakan ini menjelaskan bagaimana HyperAlarm ("modul") menangani informasi Anda. Ringkasnya: modul ini tidak mengumpulkan dan tidak mengunggah apa pun. Log dan pengaturan Anda tetap berada di perangkat ini. Rinciannya sebagai berikut.

## 0. Versi dan tanggal berlaku

- 0.1. Versi kebijakan: 1.0
- 0.2. Tanggal berlaku: 15 November 2026
- 0.3. Kebijakan ini disertakan pada setiap build.

## 1. Informasi yang disimpan di perangkat Anda

- 1.1. Log skrip: waktu, kode keluar, keluaran standar, dan keluaran galat standar dari setiap eksekusi skrip. Disimpan dalam basis data modul dan direktori penyimpanan eksternalnya.
- 1.2. Log aplikasi: log operasional modul itu sendiri, digunakan untuk memecahkan masalah.
- 1.3. Catatan crash: ringkasan pengecualian singkat yang digunakan oleh mode aman, disimpan di direktori privat modul.
- 1.4. Pengaturan: bahasa, tingkat log, tombol filter daftar, dan sebagainya, disimpan dalam berkas preferensi modul.
- 1.5. Kata sandi layar kunci: hanya ditulis jika Anda memasukkannya secara aktif di Pengaturan, dengan mode 600, hanya dapat dibaca oleh root dan modul ini, dan digunakan untuk membuka kunci layar sebelum tugas senyap dijalankan.
- 1.6. Tidak ada hal di atas yang meninggalkan perangkat Anda kecuali Anda sendiri mengekspor, membagikannya, atau mengirimkannya dalam laporan masalah.

## 2. Informasi yang tidak kami kumpulkan

- 2.1. Tidak mengumpulkan pengenal perangkat (IMEI, Android ID, nomor seri).
- 2.2. Tidak mengumpulkan data lokasi.
- 2.3. Tidak mengumpulkan kontak, pesan, atau riwayat panggilan.
- 2.4. Tidak mengumpulkan salinan skrip Anda.
- 2.5. Modul ini tidak memuat SDK analitik, pelaporan crash, atau iklan.

## 3. Dua tempat yang menggunakan jaringan

- 3.1. Halaman Tentang memanggil endpoint publik GitHub untuk membaca avatar penulis dan daftar kontributor. Permintaan hanya dikirim ke api.github.com dan avatars.githubusercontent.com, membawa User-Agent standar yang diminta GitHub, dan tidak membawa pengenal perangkat atau pengguna. Karena itu, GitHub akan melihat alamat IP Anda.
- 3.2. Wizard penyiapan dan halaman pengaturan mengambil teks perjanjian pengguna dan kebijakan ini dari GitHub agar teks dapat diperbarui secara terpisah dari rilis. Permintaan tersebut juga hanya membawa header standar. Jika pengambilan gagal, salinan dalam paket pemasangan akan ditampilkan atau pemberitahuan kegagalan akan muncul.
- 3.3. Selain itu, modul ini tidak melakukan permintaan jaringan apa pun.

## 4. Berbagi dengan pihak ketiga

- 4.1. Kami tidak membagikan apa pun kepada siapa pun, karena kami tidak mengumpulkan apa pun.
- 4.2. Permintaan GitHub di atas merupakan interaksi langsung Anda dengan GitHub dan tunduk pada kebijakan privasi GitHub.
- 4.3. Saat Anda mengekspor dan membagikan log, penerimanya Anda pilih sendiri; hal itu bukan pengungkapan oleh kami.

## 5. Penyimpanan dan penghapusan

- 5.1. Log dipangkas secara otomatis pada batas tetap, dimulai dari yang paling lama.
- 5.2. Anda dapat mengosongkan log di aplikasi atau langsung menghapus data modul.
- 5.3. Mencopot pemasangan modul akan menghapus semua data di direktori privatnya.
- 5.4. Salinan log yang dicerminkan ke penyimpanan eksternal harus Anda hapus secara manual sebelum mencopot pemasangan.

## 6. Hak Anda

- 6.1. Anda dapat melihat, mengekspor, dan menghapus semua informasi yang disimpan modul kapan saja.
- 6.2. Anda dapat menolak izin apa pun; fitur terkait tidak akan berfungsi.
- 6.3. Anda dapat menghapus kata sandi layar kunci di Pengaturan; isi berkasnya akan dikosongkan.

## 7. Anak di bawah umur

- 7.1. Modul ini ditujukan untuk pengguna yang memiliki pengetahuan sistem yang memadai. Anak di bawah umur harus menggunakannya dengan bimbingan orang tua atau wali.
- 7.2. Karena melibatkan root dan eksekusi skrip, kami tidak menyarankan anak di bawah umur mengoperasikannya sendiri.

## 8. Perubahan kebijakan

- 8.1. Kebijakan ini dapat direvisi seiring dengan rilis; teks hasil revisi disertakan pada build baru dan dipublikasikan di repositori.
- 8.2. Jika suatu revisi memperkenalkan cara baru dalam menangani informasi, kami akan menyatakannya secara jelas dalam catatan rilis.

## 9. Hubungi kami

- 9.1. Untuk pertanyaan privasi, laporkan melalui halaman masalah di repositori proyek.
- 9.2. Kode sumber bersifat publik dan dapat diaudit, sehingga Anda dapat memverifikasi sendiri pernyataan di atas.
