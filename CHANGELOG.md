# 📋 DiscordPlus Changelog

## [v1.1.0] - 2025-01-07

### ✨ Yeni Özellikler
- **🔗 Admin Unlink Komutu**: Adminler artık `/unlink <minecraft_username>` komutu ile oyuncuların Discord bağlantısını kesebilir
- **🛡️ Gelişmiş Yetki Sistemi**: Unlink komutu sadece `discordplus.admin` yetkisine sahip adminler tarafından kullanılabilir
- **🌐 Çoklu Dil Desteği**: Unlink komutu için Türkçe ve İngilizce mesaj desteği eklendi
- **📊 Gelişmiş Loglama**: Tüm unlink işlemleri detaylı olarak loglanır ve Discord log kanalına bildirim gönderilir

### 🐛 Hata Düzeltmeleri
- **🌐 Localization Hataları Düzeltildi**: "Message not found" hatalarının tümü çözüldü
- **🔧 Mesaj Anahtarları Yeniden Yapılandırıldı**: MessageUtil ile uyumlu nested yapıya geçirildi
- **📝 Status Komut Mesajları**: `/discordplus status` komutu artık doğru mesajları gösteriyor
- **🔗 Help Menü Düzeltmesi**: Komut yardım menüleri artık düzgün çalışıyor

### 🛠️ Localization Değişiklikleri
- **messages_tr.yml**: Mesaj anahtarları nested yapıya dönüştürüldü (`error.no-permission`, `commands.discordplus.help.header`, vb.)
- **messages_en.yml**: İngilizce mesaj dosyası da aynı yapıya güncellendi
- **MessageUtil Uyumluluğu**: Kod tarafından beklenen mesaj yolları ile dosya yapısı eşleştirildi

### 🔧 Teknik Geliştirmeler
- **DatabaseManager**: `getPlayerByUsername()` metodu eklendi - Minecraft kullanıcı adı ile oyuncu araması
- **LinkingManager**: `unlinkAccountByUsername()` metodu eklendi - Kullanıcı adı ile hesap bağlantısı kesme
- **UnlinkCommand**: Yeni komut sınıfı oluşturuldu - Tam async işlem desteği ile
- **Discord Entegrasyonu**: Unlink işlemi sonrası Discord rollerinin otomatik kaldırılması

### 📝 Komut Kullanımı
```
/unlink <minecraft_username>
```
**Aliases**: `baglantikesme`, `unlinkaccount`  
**Yetki**: `discordplus.admin`  
**Açıklama**: Belirtilen oyuncunun Discord hesap bağlantısını keser

### 🚀 Özellik Detayları
- ✅ Minecraft kullanıcı adı ile çalışır
- ✅ Discord rollerini otomatik kaldırır  
- ✅ Kapsamlı hata kontrolü
- ✅ Async işlem desteği
- ✅ Discord log kanalına bildirim
- ✅ Admin activity loglama
- ✅ Gelişmiş mesaj sistemi

### 📂 Değişen Dosyalar
```
src/main/resources/plugin.yml                                   # Komut tanımlaması
src/main/java/kynux/cloud/discordPlus/commands/UnlinkCommand.java    # Yeni komut sınıfı
src/main/java/kynux/cloud/discordPlus/managers/DatabaseManager.java  # Username lookup metodu
src/main/java/kynux/cloud/discordPlus/managers/LinkingManager.java   # Unlink metodu
src/main/java/kynux/cloud/discordPlus/DiscordPlus.java              # Komut kaydı
src/main/resources/messages_tr.yml                             # Türkçe mesajlar + Localization düzeltmeleri
src/main/resources/messages_en.yml                             # İngilizce mesajlar + Localization düzeltmeleri
```

### 📂 Düzeltilen Mesaj Anahtarları
```
error.no-permission                    # Yetki hatası mesajı
error.command-failed                   # Komut başarısız mesajı
commands.discordplus.help.header       # Yardım menü başlığı
commands.discordplus.status.title      # Status başlığı
commands.discordplus.status.uptime     # Çalışma süresi
commands.discordplus.status.discord    # Discord bağlantı durumu
commands.discordplus.status.database   # Veritabanı durumu
commands.discordplus.status.players    # Bağlı oyuncu sayısı
```

### 🔍 Kullanım Örnekleri
```bash
# Oyuncu bağlantısını kes
/unlink Steve

# Hata durumları
/unlink                    # Kullanım: /unlink <minecraft_username>
/unlink UnknownPlayer      # Oyuncu 'UnknownPlayer' bulunamadı
/unlink AlreadyUnlinked    # Oyuncu zaten Discord ile bağlı değil
```

### 🛡️ Güvenlik
- Admin yetkisi kontrolü
- Input validation 
- Exception handling
- Rate limiting (mevcut sistem kullanılır)

### 🔄 Geriye Uyumluluk
- ✅ Mevcut tüm özellikler korundu
- ✅ Veritabanı şeması değişmedi
- ✅ Konfigürasyon uyumlu
- ✅ API değişikliği yok

### 🙏 Teşekkürler
Hataları ve önerileri bildirdiği için **@heroghost_**'a çok teşekkür ederiz! 🙏

---
*Bu güncelleme ile admin kontrolü artırılmış, hesap yönetimi daha esnek hale getirilmiş ve localization hataları düzeltilmiştir.*