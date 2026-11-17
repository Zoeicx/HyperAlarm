HyperAlarm adalah modul peningkatan sistem nonkomersial yang berjalan pada framework Xposed dan ditujukan untuk Jam Mi. Modul ini mengubah alarm Jam Mi menjadi pemicu skrip root: saat alarm berbunyi, skrip yang Anda tentukan dapat dijalankan sebagai pengganti atau tambahan bagi dering. Harap baca dan pahami Perjanjian Pengguna HyperAlarm ini sebelum melanjutkan.

## 0. Versi dan tanggal berlaku

- 0.1. Versi perjanjian: 1.0
- 0.2. Tanggal berlaku: 15 November 2026
- 0.3. Perjanjian ini disertakan pada setiap build; versi terbaru tersedia di repositori proyek.

## 1. Ketentuan umum

- 1.1. HyperAlarm dikembangkan dan dipelihara oleh Proyek HyperAlarm (selanjutnya disebut "kami").
- 1.2. Modul ini memerlukan perangkat yang telah di-root dan framework Xposed / LSPosed yang terpasang. Tanpa keduanya, modul tidak berfungsi.
- 1.3. Satu-satunya cakupan modul ini adalah Jam Mi (com.android.deskclock). Kami tidak melakukan hook pada aplikasi lain.
- 1.4. Dengan menggunakan modul ini, Anda memahami bahwa modul ini membuat alarm menjalankan skrip yang Anda sediakan pada waktu yang ditetapkan, bukan sekadar berdering.
- 1.5. Anda dapat berhenti menggunakannya kapan saja dengan mencopot pemasangan modul atau menonaktifkannya di pengelola Xposed.

## 2. Ruang lingkup lisensi

- 2.1. Kami memberikan Anda lisensi yang bersifat pribadi, tidak dapat dialihkan, tidak dapat disublisensikan, dan tidak eksklusif untuk memasang dan menggunakan modul ini.
- 2.2. Kode sumber modul ini dirilis di bawah GNU Affero General Public License v3.0 (AGPL-3.0). Anda dapat menyalin, mengubah, dan mendistribusikan ulang kode tersebut sesuai ketentuan lisensi itu.
- 2.3. Pemasangan awal untuk tujuan komersial, penjualan dalam paket, atau distribusi bersama perangkat memerlukan izin tertulis dari kami terlebih dahulu.
- 2.4. Bagian kode yang diwarisi dari HyperCeiler tetap tunduk pada ketentuan lisensi aslinya; lihat THIRD_PARTY_NOTICES.md di repositori.
- 2.5. Nama dan ikon HyperAlarm tidak termasuk dalam lisensi kode sumber.

## 3. Kemampuan dan tanggung jawab Anda

- 3.1. Skrip dijalankan sebagai root di namespace mount utama sistem, sehingga dapat melihat direktori data aplikasi lain.
- 3.2. Anda menyediakan, memasang, dan memelihara skrip. Kami tidak bertanggung jawab atas perilaku skrip atau akibat apa pun dari menjalankannya.
- 3.3. Tugas senyap menyembunyikan tampilan alarm; jika skrip tidak ada, ditolak, atau tidak dapat dijadwalkan ulang, modul tetap membunyikan alarm seperti biasa.
- 3.4. Modul ini bukan sarana pengingat yang andal. Jangan gunakan untuk keperluan yang jika alarmnya terlewat dapat menyebabkan cedera atau kerugian harta yang besar, termasuk keperluan medis, pemadaman kebakaran, atau tugas jaga keamanan.
- 3.5. Mengubah perilaku sistem menimbulkan risiko, termasuk namun tidak terbatas pada perangkat yang tidak dapat menyala, kehilangan data, atau garansi yang batal.

## 4. Penanganan informasi

- 4.1. Log skrip, log aplikasi, catatan crash, pengaturan, dan kata sandi layar kunci semuanya hanya disimpan di perangkat Anda. Kami tidak mengunggah, mengumpulkan, atau menjualnya.
- 4.2. Modul ini mengakses jaringan di tepat dua tempat: halaman Tentang membaca avatar penulis dan daftar kontributor dari endpoint publik GitHub, dan teks perjanjian diambil dari GitHub. Keduanya hanya mengungkapkan alamat IP Anda kepada GitHub dan tidak membawa pengenal perangkat apa pun.
- 4.3. Rinciannya terdapat dalam Kebijakan Privasi HyperAlarm yang dirilis bersama perjanjian ini.

## 5. Izin yang kami minta

- 5.1. Root: untuk menjalankan skrip, membaca dan menulis direktori skrip, menyinkronkan log, dan mengubah pengaturan global. Ini diperlukan untuk fungsi inti.
- 5.2. Daftar aplikasi terpasang: untuk menampilkan aplikasi di pemilih cakupan.
- 5.3. Notifikasi: untuk menampilkan skrip yang sedang berjalan dan yang telah selesai, serta alasan kegagalan eksekusi.
- 5.4. Jaringan: hanya untuk dua kasus yang dijelaskan pada 4.2.
- 5.5. Anda dapat menolak salah satunya; fitur terkait tidak akan berfungsi dan fitur lainnya tidak terpengaruh.

## 6. Penggunaan yang dapat diterima

- 6.1. Anda tidak boleh menghapus informasi hak cipta dan lisensi dari modul ini atau dari salinannya.
- 6.2. Anda tidak boleh menggunakan modul ini untuk merugikan keamanan jaringan, melanggar privasi orang lain, atau melakukan tindakan lain yang melanggar hukum.
- 6.3. Anda tidak boleh memasang atau menggunakan modul ini pada perangkat orang lain tanpa persetujuan pemilik perangkat.
- 6.4. Anda tidak boleh menggunakan modul ini untuk menghindari pembayaran, membobol perangkat lunak, atau mengakali kontrol akses.

## 7. Penafian dan pembatasan tanggung jawab

- 7.1. Modul ini disediakan "apa adanya", tanpa jaminan apa pun, baik tersurat maupun tersirat, termasuk jaminan kelayakan jual, kesesuaian untuk tujuan tertentu, dan tidak melanggar hak pihak lain.
- 7.2. Kami hanya memverifikasi fungsinya pada Jam Mi 18.28.0. Pembaruan jam atau sistem dapat membuat sebagian fitur tidak berfungsi atau mengubah perilakunya.
- 7.3. Masalah yang timbul akibat tindakan Anda, isi skrip, lingkungan root, framework Xposed, atau pembaruan sistem menjadi tanggung jawab Anda.
- 7.4. Sejauh diizinkan oleh hukum, kami tidak bertanggung jawab atas kerugian tidak langsung, insidental, khusus, atau punitif yang timbul dari penggunaan atau ketidakmampuan menggunakan modul ini.
- 7.5. Jika suatu ketentuan dinyatakan tidak sah, ketentuan lainnya tetap berlaku.

## 8. Perubahan dan pengakhiran

- 8.1. Kami dapat merevisi perjanjian ini seiring dengan rilis; teks hasil revisi disertakan pada build baru dan dipublikasikan di repositori.
- 8.2. Terus menggunakan modul setelah revisi berlaku berarti Anda menerimanya. Jika tidak, harap berhenti menggunakan modul dan mencopot pemasangannya.
- 8.3. Anda dapat mengakhiri perjanjian ini kapan saja dengan mencopot pemasangan modul.

## 9. Hukum yang berlaku dan penyelesaian sengketa

- 9.1. Perjanjian ini tunduk pada hukum Republik Rakyat Tiongkok.
- 9.2. Sengketa yang timbul dari perjanjian ini diselesaikan terlebih dahulu melalui musyawarah dengan itikad baik; jika tidak tercapai, diserahkan kepada pengadilan yang berwenang di tempat kedudukan usaha utama kami.

## 10. Hubungi kami

- 10.1. Untuk hal apa pun mengenai perjanjian ini, laporkan melalui halaman masalah di repositori proyek.
- 10.2. Jika Anda menilai suatu ketentuan tidak sesuai dengan perilaku modul yang sebenarnya, beri tahu kami agar dapat diperbaiki, bukan langsung menganggapnya mengikat.
