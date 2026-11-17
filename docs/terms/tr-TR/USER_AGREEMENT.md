HyperAlarm, Xposed çerçevesi üzerinde çalışan ve Mi Clock'a yönelik ticari olmayan bir sistem iyileştirme modülüdür. Mi Clock alarmlarını root betikleri için tetikleyicilere dönüştürür: alarm çaldığında, zil sesi yerine veya zil sesine ek olarak belirlediğiniz bir betik çalıştırılabilir. Devam etmeden önce lütfen bu HyperAlarm Kullanıcı Sözleşmesi'ni okuyup anlayın.

## 0. Sürüm ve yürürlük tarihi

- 0.1. Sözleşme sürümü: 1.0
- 0.2. Yürürlük tarihi: 15 Kasım 2026
- 0.3. Bu sözleşme her derlemeyle birlikte sunulur; güncel sürüm proje deposunda bulunur.

## 1. Genel hükümler

- 1.1. HyperAlarm, "HyperAlarm Projesi" (bundan sonra "biz") tarafından geliştirilir ve sürdürülür.
- 1.2. Modül, root izinleri verilmiş ve Xposed / LSPosed çerçevesi kurulmuş bir cihaz gerektirir. Bu koşullardan biri eksikse işlevler kullanılamaz.
- 1.3. Modülün tek kapsamı Mi Clock'tur (com.android.deskclock). Başka uygulamalara kanca enjekte etmeyiz.
- 1.4. Modülü kullanarak, alarmın yalnızca çalmakla kalmayıp belirlenen saatte sağladığınız betiği çalıştırdığını anladığınızı kabul etmiş olursunuz.
- 1.5. Kullanımı istediğiniz zaman durdurabilirsiniz: modülü kaldırabilir veya Xposed yöneticisinde devre dışı bırakabilirsiniz.

## 2. Lisans

- 2.1. Modülü kurmanız ve kullanmanız için size kişisel, devredilemez, alt lisans verilemeyen ve münhasır olmayan bir lisans veriyoruz.
- 2.2. Kaynak kodu GNU Affero General Public License v3.0 (AGPL-3.0) kapsamında yayımlanmıştır. Bu lisansın izin verdiği ölçüde kopyalayabilir, değiştirebilir ve yeniden dağıtabilirsiniz.
- 2.3. Ticari ön kurulum, paket halinde satış veya cihazla birlikte dağıtım için önceden yazılı iznimiz gerekir.
- 2.4. HyperCeiler'dan devralınan bölümler özgün lisans koşullarına tabi olmaya devam eder; ayrıntılar için depodaki THIRD_PARTY_NOTICES.md dosyasına bakın.
- 2.5. HyperAlarm adı ve simgesi kaynak kodu lisansına dahil değildir.

## 3. Yetenekler ve sorumluluğunuz

- 3.1. Betikler root olarak sistemin ana bağlama ad alanında çalışır; bu nedenle diğer uygulamaların veri dizinlerini görebilirler.
- 3.2. Betikleri kendiniz sağlar, kurar ve bakımını yaparsınız. Betiklerin davranışlarından ve yol açtıkları her türlü sonuçtan sorumlu değiliz.
- 3.3. Sessiz görev alarm ekranını bastırır; betik eksikse, reddedilirse veya yeniden zamanlanamazsa modül normal alarm sesini korur.
- 3.4. Bu modül güvenilir bir hatırlatma aracı değildir. Kaçırılan bir alarmın yaralanmaya veya önemli maddi kayba yol açabileceği tıbbi görevler, yangın güvenliği veya nöbet gibi durumlarda kullanmayın.
- 3.5. Sistem davranışını değiştirmek, cihazın açılmaması, veri kaybı ve garantinin geçersizleşmesi dahil ancak bunlarla sınırlı olmamak üzere riskler taşır.

## 4. Bilgilerin işlenmesi

- 4.1. Betik günlükleri, uygulama günlükleri, çökme kayıtları, ayarlar ve kilit ekranı parolası yalnızca cihazınızda kalır. Bunları yüklemez, toplamaz veya satmayız.
- 4.2. Modülde tam olarak iki yerde ağ erişimi vardır: Hakkında sayfası yazarın avatarını ve katkıda bulunanlar listesini herkese açık GitHub uç noktalarından alır; sözleşme metni GitHub'dan alınır. Her iki durumda da GitHub yalnızca IP adresinizi görür ve hiçbir cihaz tanımlayıcısı aktarılmaz.
- 4.3. Ayrıntılar, bu sözleşmeyle birlikte yayımlanan HyperAlarm Gizlilik Politikası'nda yer alır.

## 5. İstediğimiz izinler

- 5.1. Root: betikleri çalıştırmak, betik dizinini okumak ve yazmak, günlükleri eşlemek ve genel ayarları değiştirmek için. Temel işlevler için zorunludur.
- 5.2. Yüklü uygulama listesi: uygulamaları kapsam seçicide göstermek için.
- 5.3. Bildirimler: çalışan ve tamamlanan betikleri ve başarısızlık nedenlerini göstermek için.
- 5.4. Ağ: yalnızca 4.2'de açıklanan durumlarda.
- 5.5. İzinlerden herhangi birini reddedebilirsiniz; ilgili işlev çalışmayı durdurur, diğerleri etkilenmez.

## 6. Kullanım kuralları

- 6.1. Modülden ve kopyalarından telif hakkı veya lisans bildirimlerini kaldıramazsınız.
- 6.2. Modülü ağ güvenliğine zarar vermek, başkalarının gizliliğini ihlal etmek veya yasaları çiğnemek için kullanamazsınız.
- 6.3. Cihaz sahibinin rızası olmadan modülü başka birinin cihazına kuramaz veya orada kullanamazsınız.
- 6.4. Modülü ödeme atlatmak, yazılım kırmak veya erişim denetimlerini aşmak için kullanamazsınız.

## 7. Garanti reddi ve sorumluluğun sınırlandırılması

- 7.1. Modül "olduğu gibi", satılabilirlik, belirli bir amaca uygunluk ve hak ihlali içermeme dahil ancak bunlarla sınırlı olmamak üzere açık veya zımni hiçbir garanti olmaksızın sunulur.
- 7.2. İşlevselliği yalnızca Mi Clock 18.28.0 üzerinde doğruladık. Saat veya sistem güncellemesinden sonra bazı işlevler çalışmayabilir veya davranışı değişebilir.
- 7.3. Sizin işlemleriniz, betik içeriğiniz, root ortamınız, Xposed çerçevesi veya sistem güncellemesinden kaynaklanan sorunlar size aittir.
- 7.4. Yasaların izin verdiği azami ölçüde, modülün kullanımından veya kullanılamamasından doğan dolaylı, arızi, özel veya cezai tazminatlardan sorumlu değiliz.
- 7.5. Herhangi bir hüküm geçersiz sayılırsa, kalan hükümler yürürlükte kalır.

## 8. Sözleşme değişiklikleri ve fesih

- 8.1. Bu sözleşmeyi sürümlerle birlikte revize edebiliriz; revize metin yeni derlemeyle sunulur ve depoda yayımlanır.
- 8.2. Revizyon yürürlüğe girdikten sonra kullanıma devam etmeniz, revizyonu kabul ettiğiniz anlamına gelir. Kabul etmiyorsanız lütfen kullanımı durdurun ve modülü kaldırın.
- 8.3. Modülü kaldırarak bu sözleşmeyi istediğiniz zaman sona erdirebilirsiniz.

## 9. Uygulanacak hukuk ve uyuşmazlıklar

- 9.1. Bu sözleşme Çin Halk Cumhuriyeti yasalarına tabidir.
- 9.2. Bu sözleşmeden doğan uyuşmazlıklar öncelikle iyi niyetli müzakerelerle çözülür; anlaşma sağlanamazsa uyuşmazlık, ana iş yeri konumumuzdaki yetkili mahkemeye sunulur.

## 10. Bize ulaşın

- 10.1. Sözleşmeyle ilgili konular için proje deposunda issue açın.
- 10.2. Sözleşmenin bir hükmünün modülün gerçek davranışıyla uyuşmadığını düşünüyorsanız, onu bağlayıcı saymak yerine önce düzeltmemiz için bize bildirin.
