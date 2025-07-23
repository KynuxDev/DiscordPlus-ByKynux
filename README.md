# DiscordPlus

<div align="center">

![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)

**🔗 Gelişmiş Minecraft-Discord Entegrasyon Plugin'i**

*Discord Bot ile hesap eşleme, rol senkronizasyonu, chat köprüsü ve kapsamlı istatistik sistemi*

[![Version](https://img.shields.io/badge/version-1.0-blue.svg)](https://github.com/KynuxDev/DiscordPlus-ByKynux)
[![Minecraft](https://img.shields.io/badge/minecraft-1.16.5+-green.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

</div>

---

## 📋 İçindekiler

- [📋 İçindekiler](#-i̇çindekiler)
- [✨ Özellikler](#-özellikler)
- [⚙️ Gereksinimler](#️-gereksinimler)
- [📦 Kurulum](#-kurulum)
- [🔧 Yapılandırma](#-yapılandırma)
- [🎮 Komutlar](#-komutlar)
- [🔑 İzinler](#-i̇zinler)
- [🔌 API Entegrasyonları](#-api-entegrasyonları)
- [📊 Özellik Detayları](#-özellik-detayları)
- [🌐 Çok Dil Desteği](#-çok-dil-desteği)
- [❓ Sıkça Sorulan Sorular](#-sıkça-sorulan-sorular)
- [🐛 Hata Bildirimi](#-hata-bildirimi)
- [📝 Lisans](#-lisans)

---

## ✨ Özellikler

### 🔗 **Hesap Eşleme Sistemi**
- ✅ Discord hesaplarını Minecraft hesapları ile güvenli eşleme
- ✅ Benzersiz doğrulama kodu sistemi
- ✅ Otomatik rol atama ve kaldırma
- ✅ Whitelist entegrasyonu desteği

### 🎭 **Rol & İzin Senkronizasyonu**
- ✅ LuckPerms ile tam entegrasyon
- ✅ Otomatik Discord rol senkronizasyonu
- ✅ Öncelik tabanlı rol yönetimi
- ✅ Real-time izin güncellemeleri

### 💬 **Chat Köprüsü**
- ✅ İki yönlü Minecraft ↔ Discord chat
- ✅ Webhook desteği (zengin görünüm)
- ✅ LuckPerms prefix entegrasyonu
- ✅ Bot mesaj filtreleme

### 📊 **İstatistik & Leaderboard Sistemi**
- ✅ Oyuncu istatistikleri tracking
- ✅ Dinamik leaderboard'lar
- ✅ PlaceholderAPI entegrasyonu
- ✅ Discord embed ile otomatik güncelleme

### 🗳️ **Vote Sistemi**
- ✅ Vote sitesi entegrasyonu
- ✅ Otomatik ödül dağıtımı
- ✅ Vote streak bonusları
- ✅ Leaderboard desteği

### 📢 **Bildirim Sistemi**
- ✅ Oyuncu giriş/çıkış bildirimleri
- ✅ Ölüm ve achievement bildirimleri
- ✅ Server status güncellemeleri
- ✅ Boost ödülleri

### 🎯 **Gelişmiş GUI Sistemi**
- ✅ İnteraktif menüler
- ✅ Hesap yönetimi paneli
- ✅ İstatistik görüntüleme
- ✅ Admin kontrol paneli

---

## ⚙️ Gereksinimler

| Bileşen | Minimum Sürüm | Önerilen |
|---------|----------------|----------|
| **Minecraft Server** | 1.16.5+ | 1.20.1+ |
| **Java** | 8+ | 17+ |
| **Discord Bot** | JDA 5.0+ | ✅ |
| **Veritabanı** | H2 (dahili) | ✅ |

### 🔌 **Opsiyonel Entegrasyonlar**
- **LuckPerms** - Rol senkronizasyonu için
- **Vault** - Ekonomi entegrasyonu için
- **PlaceholderAPI** - Gelişmiş placeholder desteği için

---

## 📦 Kurulum

### 1️⃣ **Plugin Kurulumu**
```bash
# Plugin'i sunucunuzun plugins klasörüne kopyalayın
cp discordplus-1.0.jar /path/to/server/plugins/

# Sunucuyu başlatın (konfigürasyon dosyaları oluşturulacak)
java -jar server.jar
```

### 2️⃣ **Discord Bot Oluşturma**
1. [Discord Developer Portal](https://discord.com/developers/applications)'a gidin
2. **New Application** → Bot oluşturun
3. **Bot Token**'ı kopyalayın
4. **Privileged Gateway Intents**'i etkinleştirin:
   - `GUILD_MEMBERS`
   - `MESSAGE_CONTENT`

### 3️⃣ **Bot İzinleri**
Bot'unuza şu izinleri verin:
- `Manage Roles`
- `Send Messages`
- `Embed Links`
- `Read Message History`
- `Use Slash Commands`

---

## 🔧 Yapılandırma

### **Ana Konfigürasyon** (`config.yml`)
...
---

## 🎮 Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/verify` | Discord hesabını eşle | `discordplus.command.verify` |
| `/discord` | Discord bilgilerini göster | `discordplus.command.discord` |
| `/dpgui` | Ana menüyü aç | `discordplus.use` |
| `/sync` | Rolleri manuel senkronize et | `discordplus.command.sync` |
| `/istatistik` | İstatistik menüsünü aç | `discordplus.command.stats` |
| `/discordplus reload` | Plugin'i yeniden yükle | `discordplus.admin` |
| `/discordplus status` | Plugin durumunu göster | `discordplus.admin` |

### **Komut Takma Adları**
- `/verify` → `/dogrula`, `/link`
- `/discord` → `/dc`
- `/discordplus` → `/dp`, `/discordp`
- `/sync` → `/syncrole`, `/rolesync`
- `/istatistik` → `/stats`, `/istatistikler`

---

## 🔑 İzinler

### **Ana İzinler**
```yaml
discordplus.*:          # Tüm izinler
discordplus.admin:      # Admin komutları
discordplus.use:        # Temel kullanım
discordplus.bypass:     # Zorunlu eşleme atlama
```

### **Komut İzinleri**
```yaml
discordplus.command.verify:    # /verify komutu
discordplus.command.discord:   # /discord komutu  
discordplus.command.sync:      # /sync komutu
discordplus.command.stats:     # /istatistik komutu
```

---

## 🔌 API Entegrasyonları

### **LuckPerms Entegrasyonu**
```java
// Otomatik rol senkronizasyonu
// İzin değişiklikleri real-time Discord'a yansır
luckPermsManager.syncPlayerPermissions(player);
```

### **Vault Entegrasyonu**
```java
// Ekonomi sisteminden bakiye çekme
Economy economy = getEconomy();
double balance = economy.getBalance(player);
```

### **PlaceholderAPI Desteği**
```yaml
# Kullanılabilir placeholderlar:
%discordplus_user_tag%          # Discord tag
%discordplus_user_id%           # Discord ID
%discordplus_linked_status%     # Eşleme durumu
%discordplus_server_members%    # Sunucu üye sayısı
```

---

## 📊 Özellik Detayları

### 🔗 **Hesap Eşleme Süreci**
1. Oyuncu `/verify` komutunu kullanır
2. Discord'da eşleme kanalına mesaj gönderilir
3. "🔗 Hesap Eşle" butonuna tıklanır
4. Benzersiz kod üretilir ve DM gönderilir
5. Oyuncu kodu Minecraft'ta onaylar
6. Hesaplar başarıyla eşlenir

### 🎭 **Rol Senkronizasyonu**
- **Otomatik**: İzin değişiklikleri 5 dakikada kontrol edilir
- **Manuel**: `/sync` komutu ile anında senkronizasyon
- **Öncelik**: Yüksek priority rolleri diğerlerini geçersiz kılar
- **Bildirim**: DM ile rol değişiklikleri bildirilir

### 📊 **İstatistik Sistemi**
```yaml
# Desteklenen istatistikler:
- playtime          # Oyun süresi
- deaths            # Ölüm sayısı  
- kills             # Öldürme sayısı
- votes             # Oy sayısı
- vault_eco_balance # Ekonomi bakiyesi
- loginstreak       # Giriş serisi
```

---

## 🌐 Çok Dil Desteği

Plugin şu dilleri destekler:

### 🇹🇷 **Türkçe** (`messages_tr.yml`)
```yaml
language: "tr"
```

### 🇺🇸 **İngilizce** (`messages_en.yml`)  
```yaml
language: "en"
```

### **Özel Mesajlar**
Tüm mesajlar dil dosyalarında özelleştirilebilir:
```yaml
account-linking:
  verification-success: "{prefix}&aHesap başarıyla eşlendi!"
  already-linked: "{prefix}&cBu hesap zaten eşlenmiş!"
```

---

## ❓ Sıkça Sorulan Sorular

<details>
<summary><b>Q: Bot çevrimdışı görünüyor, ne yapmalıyım?</b></summary>

**A:** Şunları kontrol edin:
1. Bot token'ın doğru olduğunu
2. Bot'un sunucunuzda olduğunu
3. Gerekli izinlere sahip olduğunu
4. Konsol loglarını inceleyin
</details>

<details>
<summary><b>Q: Rol senkronizasyonu çalışmıyor?</b></summary>

**A:** Şunları kontrol edin:
1. LuckPerms'in yüklü olduğunu
2. Rol ID'lerinin doğru olduğunu
3. Bot'un "Manage Roles" iznine sahip olduğunu
4. Bot'un rolü, verilecek rolden daha yüksekte olduğunu
</details>

<details>
<summary><b>Q: Chat köprüsü mesajları göndermiyor?</b></summary>

**A:** Şunları kontrol edin:
1. Chat kanal ID'sinin doğru olduğunu
2. Bot'un kanala mesaj gönderme iznine sahip olduğunu
3. `chat-bridge.enabled: true` olduğunu
</details>

<details>
<summary><b>Q: İstatistikler güncelleniyor mu?</b></summary>

**A:** İstatistikler şu şekilde güncellenir:
- **Playtime**: Her dakika otomatik
- **Leaderboard**: 60 dakikada bir
- **Manual**: `/istatistik` komutu ile anında
</details>

---

## 🔄 Güncelleme Notları

### **v1.0** - İlk Sürüm
- ✅ Temel Discord entegrasyonu
- ✅ Hesap eşleme sistemi
- ✅ Rol senkronizasyonu
- ✅ Chat köprüsü
- ✅ İstatistik sistemi
- ✅ Vote sistemi
- ✅ Çok dil desteği

---

## 🛠️ Geliştirici API

Plugin, diğer geliştiriciler için API sağlar:

```java
// Plugin instance alma
DiscordPlus plugin = DiscordPlus.getInstance();

// Hesap eşleme kontrolü
boolean isLinked = plugin.getLinkingManager().isLinked(player.getUniqueId());

// Discord kullanıcı bilgisi alma
String discordTag = plugin.getLinkingManager().getDiscordTag(player.getUniqueId());

// Manuel rol senkronizasyonu
plugin.getPermissionSyncManager().syncPlayer(player);
```

---

## 🐛 Hata Bildirimi

Hata bulduysanız lütfen şunları dahil edin:

1. **Server versiyonu** (Spigot/Paper/etc.)
2. **Plugin versiyonu** 
3. **Hata logu** (console log)
4. **Yapılandırma dosyası** (token'ları gizleyin)
5. **Yeniden üretme adımları**https://github.com/KynuxDev/DiscordPlus-ByKynux

**🔗 Issues:** [GitHub Issues](https://github.com/KynuxDev/DiscordPlus-ByKynux/issues)

---

## 💡 Katkıda Bulunma

Projeye katkıda bulunmak isterseniz:

1. **Fork** edin
2. **Feature branch** oluşturun (`git checkout -b feature/amazing-feature`)
3. **Commit** edin (`git commit -m 'Add amazing feature'`)
4. **Push** edin (`git push origin feature/amazing-feature`)
5. **Pull Request** açın

---

## 🤝 Destek

- **Discord**: [Nexoro](https://discord.gg/wCK5dVSY2n)
- **Website**: [kynux.cloud](https://kynux.cloud)
- **E-mail**: support@kynux.cloud

---

## 📝 Lisans

Bu proje [MIT License](LICENSE) altında lisanslanmıştır.

---

<div align="center">

**⭐ Eğer bu plugin'i beğendiyseniz, lütfen star verin!**

Made with ❤️ by [Kynux](https://github.com/kynuxdev)

![Footer](https://img.shields.io/badge/Made%20with-❤️-red.svg)
![Java](https://img.shields.io/badge/Built%20with-Java-orange.svg)
![Discord](https://img.shields.io/badge/Powered%20by-Discord-blue.svg)

</div>