<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | **Türkçe** | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm, **tek kapsamı Xiaomi Saat'i olan** bir Xposed modülüdür. Saat uygulamasının alarmlarını shell betikleri için tetikleyiciye çevirir.

## Hedef uygulama

Yalnızca aşağıdaki sürümlerde doğrulandı. Diğerlerinde kendin dene, uyarlama için APK gönder ya da bu sürümlere güncelle/geri dön:

- Xiaomi Saat `com.android.deskclock`
- Sürüm `18.28.0`, sürüm kodu `1306002800`

## Kurulum

1. root (KernelSU / Magisk) ve LSPosed (Xposed API 101 veya üstü) hazır olsun.
2. Modülü kur ve LSPosed içinde HyperAlarm'ı etkinleştir.
3. Kapsam olarak yalnızca `com.android.deskclock` işaretle. Başka paket ekleme.
4. Xiaomi Saat'i zorla durdur ve sürecin yeniden başlamasına izin ver; yoksa modül devreye girmez.

## Kullanım

- Yeni görev: üstteki işlem çubuğunun sağındaki `+`.
- Alarmın etiketi betiğin adıdır ve `.sh` ile bitmelidir:
  - `alert.sh`: saat gelince sessizce çalışır, çalmaz ve alarm ekranı açılmaz
  - `@alert.sh`: betiği çalıştırır, alarm ekranı ve zil sesi kalır
- Listede `@` gizlenir; «Zil sesi» anahtarı iki mod arasında geçiş yapar.
- «Şimdi çalıştır» saat gelmesini beklemeden betiği dener.
- Vardiyalı görevler, başlangıç tarihi ve döngüden hesaplanan vardiyaya göre tetiklenir.

## Betik kuralları

Betikler `/data/adb/alarm_shell/scripts/` içinde durur; etiketteki ad dosya adıyla birebir aynı olmalıdır.

```text
/data/adb/alarm_shell/scripts/      # betik dizini
/data/adb/alarm_shell/password      # kilit ekranı parolası
/data/adb/alarm_shell/unlock_phone  # varsayılan kilit açma betiği
```

- Satır sonları LF olmalı. Windows'un CRLF dosyası doğrudan başarısız olur.
- Betikler root olarak sistemin ana isim alanında çalışır, bu yüzden diğer uygulamaların veri dizinlerini görür.
- Her çalıştırma log bırakır: uygulama içindeki «Betik logları» sayfasında görünür, ayrıca
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<betik adı>/` altına kopyalanır.
- Sessiz görevden önce modül ekranı yalnızca cihaz hâlâ kilitliyse açar ve kilidini açar (örneğin yeniden başlatmadan sonraki ilk açılıştan önce). Yalnızca kapanmış ekran kapalı kalır.

## Ayarlar

- **Saat yalnızca alarmları göstersin**: Xiaomi Saat listesinde gerçek alarmlar kalır, betik görevleri görünmez.
- **Çalan görevleri gizle**: zil sesi açık görevleri de gizler.
- Log düzeyi / Uygulama logları / Betik logları: sorun ararken önce düzeyi yükselt.
- **Kilit ekranı parolası**: yeniden başlatmadan sonra cihaz hâlâ kilitliyken sessiz görevler için ekranı açar; boş bırakmak değiştirmez.
- Yeniden başlatma rehberi, betik dizinini kopyala, yapılandırmayı yedekle ve geri yükle.

## Bilinen sınırlar

- Yalnızca Android 16 / HyperOS 3 / Xiaomi Saat 18.28.0 üzerinde doğrulandı; diğer birleşimler senin sorumluluğunda.
- Vardiya modu saat uygulamasının iç işleyişine bağlıdır; saat güncellenince yeniden doğrulanmalıdır.
- root yoksa modül çalışmaz; ayarlar sayfası bunu söyler.
- Betik logları harici depoya kopyalanır; root ve adb shell okuyabilir. Sırları betik çıktısına yazma.

## Sık sorulanlar

- **Görev ekledim ama saat tepki vermiyor**: modülün LSPosed'de açık olduğunu, kapsamın yalnızca Xiaomi Saat olduğunu ve saat sürecini yeniden başlattığını kontrol et.
- **Görev saat listesinde yok**: «Saat yalnızca alarmları göstersin» çalışıyor, kapatınca geri gelir.
- **Saat gelince hiçbir şey çalışmadı**: uygulama içindeki betik loglarına bak; eksik betik veya yanlış yazılmış etiket orada açıkça belirtilir.

## Lisans

- Bu depoya ait kod: **AGPL-3.0** (`../LICENSE`)
- Üçüncü taraf bileşenler ve lisansları: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Teşekkürler

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): projenin başlangıç noktası; MIUIX arayüzünün ve ayarlar altyapısının çoğu oradan
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): Xiaomi tarzı arayüz bileşenleri
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): root ve Xposed API'leri
- Issue açan ve log gönderen herkes
