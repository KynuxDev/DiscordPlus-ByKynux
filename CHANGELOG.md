# Değişiklik Günlüğü (Changelog)

Tüm değişiklikler ve güncellemeler bu dosyada listelenmektedir.

## [1.1.0] - 2025-11-26

### 🚀 Yeni Özellikler
- **Yönetici Güvenlik Sistemi (2FA):**
  - Yöneticiler için oyun içi hareket kısıtlama ve Discord üzerinden giriş onayı eklendi.
  - Onaylanmayan girişlerde otomatik kick sistemi.
  - Güvenlik logları ve DM bildirimleri.

- **Görsel Profil Sistemi:**
  - `/profil` komutu artık metin yerine resim (PNG) formatında kart gönderiyor.
  - Özel "Cyber/Tech" temalı arka plan, neon efektler ve 3D skin render desteği.
  - Java AWT tabanlı yüksek performanslı resim işleme motoru.

- **Gelişmiş Role Sync (v2):**
  - Periyodik tarama (loop) sistemi kaldırıldı.
  - **Event-Based (Olay Tabanlı)** sisteme geçildi. Artık sadece gerektiğinde çalışıyor.
  - LuckPerms API entegrasyonu ile anlık rütbe değişimlerini algılama özelliği.
  - Discord API limitlerine takılmamak için akıllı kuyruk (Queue) yapısı.
  - Config yapısı sadeleştirildi (`discordplus -> vip` yerine `groups -> vip`).

### ⚡ İyileştirmeler
- **Performans:**
  - Gereksiz veritabanı sorguları azaltıldı.
  - Discord botu başlatma süresi optimize edildi.
  - Resim oluşturma işlemleri asenkron (Async) yapıya taşındı, sunucu TPS'i etkilenmiyor.
- **Yapılandırma (Config):**
  - `role-mappings` bölümü daha anlaşılır ve standart bir yapıya kavuşturuldu.
- **Test:**
  - `SecurityManagerTest`, `ImageRendererTest` ve `PermissionSyncTest` birim testleri ile sistemler doğrulandı.
  - Sunucusuz ortamda test yapabilmek için Mockito altyapısı kuruldu.

### 🐛 Düzeltmeler
- Role Sync sisteminin bazen geç tepki vermesine neden olan zamanlayıcı hatası giderildi.
- Discord botunun bağlantı kopması durumunda yeniden bağlanamama sorunu çözüldü.
- Config dosyasındaki bazı hatalı varsayılan değerler düzeltildi.
