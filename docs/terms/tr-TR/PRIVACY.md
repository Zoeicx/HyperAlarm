Bu politika, HyperAlarm'ın ("modül") bilgilerinizi nasıl işlediğini açıklar. Kısaca: hiçbir şey toplamaz ve hiçbir şey yüklemez. Günlükleriniz ve ayarlarınız bu cihazda kalır. Ayrıntılar aşağıdadır.

## 0. Sürüm ve yürürlük tarihi

- 0.1. Politika sürümü: 1.0
- 0.2. Yürürlük tarihi: 15 Kasım 2026
- 0.3. Bu politika her derlemeyle birlikte sunulur.

## 1. Cihazınızda saklananlar

- 1.1. Betik günlükleri: her betik çalışmasının başlangıç zamanı, çıkış kodu, standart çıktısı ve standart hatası. Modülün veritabanında ve harici depolama dizininde saklanır.
- 1.2. Uygulama günlükleri: sorun giderme için kullanılan, modülün kendi çalışma günlüğü.
- 1.3. Çökme kayıtları: güvenli mod tarafından kullanılan kısa istisna özetleri, modülün özel dizininde saklanır.
- 1.4. Ayarlar: dil, günlük düzeyi, liste filtreleri vb., modülün tercihler dosyasında saklanır.
- 1.5. Kilit ekranı parolası: yalnızca Ayarlar'da siz girerseniz dosyaya yazılır; izinler 600'dür, yalnızca root ve bu modül okuyabilir; sessiz görev çalışmadan önce ekranın kilidini açmak için kullanılır.
- 1.6. Yukarıdakilerin hiçbiri, siz kendiniz dışa aktarmadıkça, paylaşmadıkça veya bir issue'da yayımlamadıkça cihazınızdan ayrılmaz.

## 2. Toplamadığımız bilgiler

- 2.1. Cihaz tanımlayıcıları (IMEI, Android ID, seri numarası vb.) toplanmaz.
- 2.2. Konum bilgileri.
- 2.3. Kişiler, mesajlar veya arama kayıtları.
- 2.4. Betiklerinizin içeriği.
- 2.5. Modülde analiz, çökme raporlama veya reklam SDK'sı bulunmaz.

## 3. İki ağ isteği

- 3.1. Hakkında sayfası, yazarın avatarını ve katkıda bulunanlar listesini okumak için herkese açık GitHub uç noktalarını çağırır. İstekler yalnızca api.github.com ve avatars.githubusercontent.com adreslerine gider, GitHub'ın istediği standart User-Agent başlığını taşır ve hiçbir cihaz veya kullanıcı tanımlayıcısı içermez. Bu nedenle GitHub, IP adresinizi görür.
- 3.2. Kurulum sihirbazı ve ayarlar sayfası, metinlerin sürümden bağımsız güncellenebilmesi için kullanıcı sözleşmesinin ve bu politikanın metnini GitHub'dan alır. Bu istekler de yalnızca standart başlıklar taşır. Alma işlemi başarısız olursa, kurulum paketindeki kopya gösterilir veya başarısızlık bildirimi verilir.
- 3.3. Bunların dışında modül hiçbir ağ isteği yapmaz.

## 4. Üçüncü taraflarla paylaşım

- 4.1. Hiçbir şey toplamadığımız için hiçbir şeyi kimseyle paylaşmayız.
- 4.2. Yukarıdaki GitHub istekleri, sizin GitHub ile doğrudan etkileşiminizdir ve GitHub'ın kendi gizlilik politikasına tabidir.
- 4.3. Günlükleri kendiniz dışa aktarıp paylaştığınızda alıcıyı siz seçersiniz; bu, tarafımızdan bir açıklama değildir.

## 5. Saklama ve silme

- 5.1. Günlükler sabit bir üst sınıra göre otomatik olarak kırpılır; sınır aşıldığında en eski kayıtlar silinir.
- 5.2. Günlükleri uygulama içinde temizleyebilir veya modül verilerini doğrudan silebilirsiniz.
- 5.3. Modülün kaldırılması, özel dizinlerindeki tüm verileri de siler.
- 5.4. Modülü kaldırmadan önce harici depolamaya eşlenen günlük kopyalarını kendiniz silmeniz gerekir.

## 6. Haklarınız

- 6.1. Modülün sakladığı tüm bilgileri istediğiniz zaman görüntüleyebilir, dışa aktarabilir ve silebilirsiniz.
- 6.2. Herhangi bir izni reddedebilirsiniz; ilgili işlev çalışmayı durdurur.
- 6.3. Kilit ekranı parolasını Ayarlar'da temizleyebilirsiniz; dosyanın içeriği boşaltılır.

## 7. Küçükler

- 7.1. Modül, belirli düzeyde sistem bilgisine sahip kullanıcılara yöneliktir. Küçükler onu bir velinin rehberliğinde kullanmalıdır.
- 7.2. Root ve betik çalıştırma içerdiğinden, küçüklerin modülü tek başına kullanmasını önermiyoruz.

## 8. Politikadaki değişiklikler

- 8.1. Bu politika sürümlerle birlikte revize edilebilir; revize metin yeni derlemeyle sunulur ve depoda yayımlanır.
- 8.2. Bir revizyon yeni bilgi işleme yöntemleri getiriyorsa, bunu sürüm notlarında açıkça belirtiriz.

## 9. Bize ulaşın

- 9.1. Gizlilikle ilgili konular için proje deposunda issue açın.
- 9.2. Modülün kaynak kodu açık ve denetlenebilir olduğundan yukarıdaki ifadeleri kendiniz doğrulayabilirsiniz.
