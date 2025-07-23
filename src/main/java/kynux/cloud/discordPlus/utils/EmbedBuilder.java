package kynux.cloud.discordPlus.utils;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.VoteManager;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

public class EmbedBuilder {

    private final net.dv8tion.jda.api.EmbedBuilder jdaBuilder;
    private static FileConfiguration config;

    
    public enum EmbedType {
        SUCCESS("✅", "#43B581"),
        ERROR("❌", "#F04747"),
        WARNING("⚠️", "#FAA61A"),
        INFO("ℹ️", "#7289DA"),
        BRAND("✨", "#5865F2");

        private final String emoji;
        private final String color;

        EmbedType(String emoji, String color) {
            this.emoji = emoji;
            this.color = color;
        }

        public String getEmoji() {
            return emoji;
        }

        public Color getColor() {
            return Color.decode(color);
        }
    }

    
    public EmbedBuilder() {
        this.jdaBuilder = new net.dv8tion.jda.api.EmbedBuilder();
        this.jdaBuilder.setTimestamp(Instant.now());
    }

    public static void initialize(FileConfiguration configuration) {
        config = configuration;
    }

    
    public EmbedBuilder setTitle(String title) { jdaBuilder.setTitle(title); return this; }
    public EmbedBuilder setDescription(String description) { jdaBuilder.setDescription(description); return this; }
    public EmbedBuilder setColor(Color color) { jdaBuilder.setColor(color); return this; }
    public EmbedBuilder addField(String name, String value, boolean inline) { jdaBuilder.addField(name, value, inline); return this; }
    public EmbedBuilder setFooter(String text, String iconUrl) {
        jdaBuilder.setFooter(text, (iconUrl != null && !iconUrl.trim().isEmpty()) ? iconUrl : null);
        return this;
    }
    public EmbedBuilder setThumbnail(String url) {
        if (url != null && !url.trim().isEmpty()) jdaBuilder.setThumbnail(url);
        return this;
    }
    public EmbedBuilder setAuthor(String name, String url, String iconUrl) {
        jdaBuilder.setAuthor(name, url, (iconUrl != null && !iconUrl.trim().isEmpty()) ? iconUrl : null);
        return this;
    }

    public MessageEmbed build() {
        
        if (jdaBuilder.build().getFooter() == null) {
            String footerText = getFooterText();
            String footerIcon = getFooterIcon(); 

            
            if (footerIcon == null || footerIcon.trim().isEmpty() || !(footerIcon.startsWith("http://") || footerIcon.startsWith("https://"))) {
                
                footerIcon = "https://media.discordapp.net/attachments/1338874773207384064/1396541287741657088/4ae9487627d3329972689e3b7f3832e3_1.png?ex=687e75ed&is=687d246d&hm=6fc8aae2b10367627f3d6d8b03f01746a6ad72a3bdee8c005fbd7d695d229ba3&=&format=webp&quality=lossless&width=648&height=648";
            }
            
            jdaBuilder.setFooter(footerText, footerIcon);
        }
        return jdaBuilder.build();
    }

    
    private static String getFooterText() { return config != null ? config.getString("embeds.footer.text", "DiscordPlus") : "DiscordPlus"; }
    private static String getFooterIcon() { return config != null ? config.getString("embeds.footer.icon") : null; }

    private static MessageEmbed createEmbed(EmbedType type, String title, String description) {
        return new EmbedBuilder()
                .setTitle(type.getEmoji() + " " + title)
                .setDescription(description)
                .setColor(type.getColor())
                .build();
    }

    
    public static MessageEmbed success(String title, String description) { return createEmbed(EmbedType.SUCCESS, title, description); }
    public static MessageEmbed error(String title, String description) { return createEmbed(EmbedType.ERROR, title, description); }
    public static MessageEmbed info(String title, String description) { return createEmbed(EmbedType.INFO, title, description); }
    public static MessageEmbed warning(String title, String description) { return createEmbed(EmbedType.WARNING, title, description); }

    public static MessageEmbed dailyReward(String playerName, int streak, String rewardInfo) {
        return new EmbedBuilder()
                .setTitle("🎁 Günlük Ödül Alındı!")
                .setDescription("Tebrikler **" + playerName + "**! Günlük ödülünü başarıyla aldın.")
                .addField("🔥 Seri", "`" + streak + " gün`", true)
                .addField("🏆 Ödül", "`" + rewardInfo + "`", true)
                .setColor(EmbedType.SUCCESS.getColor())
                .build();
    }

    public static MessageEmbed weeklyReward(String playerName, String rewardInfo) {
        return new EmbedBuilder()
                .setTitle("💎 Haftalık Ödül Alındı!")
                .setDescription("Harika! **" + playerName + "**, haftalık ödülünü kaptın!")
                .addField("🏆 Büyük Ödül", "`" + rewardInfo + "`", false)
                .setColor(EmbedType.BRAND.getColor())
                .build();
    }

    public static MessageEmbed voteNotification(String playerName, String site, int streak, String rewardInfo) {
        return new EmbedBuilder()
                .setAuthor(playerName + " oy kullandı!", null, null)
                .setTitle("🗳️ Yeni Oy!")
                .setDescription("`" + playerName + "` adlı oyuncu **" + site + "** sitesinde oy kullanarak sunucumuzu destekledi!")
                .addField("🔥 Oy Serisi", "`" + streak + " gün`", true)
                .addField("🎁 Ödül", "`" + rewardInfo + "`", true)
                .setColor(EmbedType.SUCCESS.getColor())
                .build();
    }

    public static MessageEmbed deathNotification(String playerName, String playerUuid, String deathMessage, String killer, String location) {
        String playerHeadUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";
        String errorColor = config.getString("embeds.colors.error", "#F04747");

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(errorColor))
                .setAuthor(playerName + " hayatını kaybetti.", null, playerHeadUrl)
                .setDescription("*" + deathMessage + "*");

        if (killer != null && !killer.isEmpty()) {
            builder.addField("⚔️ Katil", "`" + killer + "`", true);
        }
        if (location != null && !location.isEmpty()) {
            builder.addField("📍 Konum", "`" + location + "`", true);
        }

        return builder.build();
    }

    public static MessageEmbed playerJoin(String playerName, String uuid, int onlineCount) {
        return new EmbedBuilder()
                .setAuthor(playerName + " sunucuya katıldı!", null, "https://mc-heads.net/avatar/" + uuid + "/64")
                .setColor(EmbedType.SUCCESS.getColor())
                .setDescription("Sunucudaki oyuncu sayısı: **" + onlineCount + "**")
                .build();
    }

    public static MessageEmbed playerQuit(String playerName, String uuid, int onlineCount, String sessionDuration) {
        return new EmbedBuilder()
                .setAuthor(playerName + " sunucudan ayrıldı.", null, "https://mc-heads.net/avatar/" + uuid + "/64")
                .setColor(EmbedType.WARNING.getColor())
                .setDescription("Oturum Süresi: **" + sessionDuration + "**\n" + "Sunucudaki oyuncu sayısı: **" + onlineCount + "**")
                .build();
    }

    public static MessageEmbed serverStatus(int onlineCount, int maxPlayers, String tps, String memory, String uptime) {
        String serverName = config.getString("embeds.server-name", "Sunucumuz");
        String serverIcon = config.getString("embeds.server-icon", null);
        String serverIp = config.getString("embeds.server-ip", "IP_ADRESI_YOK");
        String serverUrl = config.getString("embeds.server-url", "");

        double load = maxPlayers > 0 ? (double) onlineCount / maxPlayers : 0;
        Color statusColor;
        String statusText;
        String statusEmoji;

        if (onlineCount == 0) {
            statusColor = Color.decode("#747F8D"); statusText = "Çevrimdışı"; statusEmoji = "⚫";
        } else if (load < 0.5) {
            statusColor = Color.decode("#2ECC71"); statusText = "Online"; statusEmoji = "🟢";
        } else if (load < 0.8) {
            statusColor = Color.decode("#F1C40F"); statusText = "Yoğun"; statusEmoji = "🟡";
        } else {
            statusColor = Color.decode("#E74C3C"); statusText = "Dolu"; statusEmoji = "🔴";
        }

        String finalUrl = (serverUrl != null && (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))) ? serverUrl : null;
        return new EmbedBuilder()
                .setAuthor(serverName + " | Sunucu Durumu", finalUrl, serverIcon)
                .setColor(statusColor)
                .setTitle(statusEmoji + " Sunucu " + statusText)
                .setDescription("Sunucuya bağlanmak için IP adresi: `" + serverIp + "`")
                .addField("👥 Oyuncular", onlineCount + " / " + maxPlayers, true)
                .addField("⚡ TPS", tps, true)
                .addField("💾 RAM", memory, true)
                .addField("⏰ Aktif Süre", uptime, false)
                .build();
    }

    public static MessageEmbed playerProfile(net.dv8tion.jda.api.entities.User discordUser, org.bukkit.OfflinePlayer player, kynux.cloud.discordPlus.data.PlayerData playerData, double balance) {
        String playerName = player.getName() != null ? player.getName() : "Bilinmiyor";
        String playerHeadUrl = "https://mc-heads.net/avatar/" + player.getUniqueId() + "/64";

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(playerName + " Profili", null, discordUser.getEffectiveAvatarUrl())
                .setColor(EmbedType.BRAND.getColor())
                .setThumbnail(playerHeadUrl);

        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
        String totalPlaytime = TimeUtil.formatDuration(playerData.getTotalPlaytime());
        String lastSeen = playerData.getLastSeen() != null ? TimeUtil.formatTimeAgo(playerData.getLastSeen()) : "Bilinmiyor";

        builder.setDescription(
                "**Discord:** " + discordUser.getAsTag() + "\n" +
                "**Son Görülme:** " + lastSeen
        );

        builder.addField("💰 Bakiye", df.format(balance) + " " + DiscordPlus.getInstance().getEconomy().currencyNamePlural(), true);
        builder.addField("⏰ Toplam Oyun Süresi", totalPlaytime, true);
        java.text.DecimalFormat kdFormat = new java.text.DecimalFormat("#0.0");
        builder.addField("📊 İstatistikler",
                "**Giriş Serisi:** " + playerData.getLoginStreak() + " gün\n" +
                "**Oy Sayısı:** " + playerData.getVoteCount() + "\n" +
                "**K/D Oranı:** " + kdFormat.format(playerData.getKDRatio()),
                false);

        return builder.build();
    }

    
    public static MessageEmbed enhancedPlayerProfile(
            net.dv8tion.jda.api.entities.User discordUser, 
            org.bukkit.OfflinePlayer player, 
            kynux.cloud.discordPlus.data.PlayerData playerData, 
            double balance,
            java.util.Map<String, kynux.cloud.discordPlus.managers.StatisticManager.PlayerRankInfo> rankings) {
        
        kynux.cloud.discordPlus.managers.ConfigManager configManager = DiscordPlus.getInstance().getConfigManager();
        kynux.cloud.discordPlus.managers.StatisticManager statisticManager = DiscordPlus.getInstance().getStatisticManager();
        
        String playerName = player.getName() != null ? player.getName() : "Bilinmiyor";
        
        
        String title = configManager.getProfileEmbedTitle().replace("{player_name}", playerName);
        
        
        String skinUrl = getSkinUrl(player.getUniqueId().toString(), configManager.getMinecraftSkinType());
        
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setColor(Color.decode(configManager.getProfileEmbedColor()));

        
        if (configManager.shouldShowDiscordAvatar()) {
            builder.setAuthor(playerName, null, discordUser.getEffectiveAvatarUrl());
        }
        
        if (configManager.shouldShowMinecraftSkin() && skinUrl != null) {
            builder.setThumbnail(skinUrl);
        }

        
        if (configManager.isProfileFieldEnabled("discord-info")) {
            StringBuilder discordInfo = new StringBuilder();
            
            if (configManager.shouldShowDiscordTag()) {
                discordInfo.append("**Discord:** ").append(discordUser.getAsTag()).append("\n");
            }
            
            if (configManager.shouldShowLastSeen() && playerData.getLastSeen() != null) {
                String lastSeen = TimeUtil.formatTimeAgo(playerData.getLastSeen());
                discordInfo.append("**Son Görülme:** ").append(lastSeen);
            }
            
            if (discordInfo.length() > 0) {
                builder.addField(configManager.getProfileFieldName("discord-info"), discordInfo.toString(), true);
            }
        }

        
        if (configManager.isProfileFieldEnabled("balance")) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
            String balanceText = df.format(balance);
            
            if (configManager.shouldShowCurrency() && DiscordPlus.getInstance().getEconomy() != null) {
                balanceText += " " + DiscordPlus.getInstance().getEconomy().currencyNamePlural();
            }
            
            builder.addField(configManager.getProfileFieldName("balance"), balanceText, true);
        }

        
        if (configManager.isProfileFieldEnabled("playtime")) {
            StringBuilder playtimeInfo = new StringBuilder();
            String totalPlaytime = TimeUtil.formatDuration(playerData.getTotalPlaytime());
            playtimeInfo.append("**Toplam:** ").append(totalPlaytime);
            
            if (configManager.shouldShowCurrentSession()) {
                Long currentSession = statisticManager.getPlaytimeSession(player.getUniqueId());
                if (currentSession != null) {
                    long sessionDuration = System.currentTimeMillis() - currentSession;
                    String currentSessionTime = TimeUtil.formatDuration((int) (sessionDuration / 1000));
                    playtimeInfo.append("\n**Bu Oturum:** ").append(currentSessionTime);
                }
            }
            
            builder.addField(configManager.getProfileFieldName("playtime"), playtimeInfo.toString(), true);
        }

        
        if (configManager.isProfileFieldEnabled("leaderboard-ranks") && !rankings.isEmpty()) {
            StringBuilder rankInfo = new StringBuilder();
            int maxCategories = configManager.getProfileMaxCategories();
            int showOnlyTop = configManager.getProfileShowOnlyTop();
            int count = 0;
            
            for (kynux.cloud.discordPlus.managers.StatisticManager.PlayerRankInfo rankData : rankings.values()) {
                if (count >= maxCategories) break;
                
                
                if (showOnlyTop > 0 && rankData.getRank() > showOnlyTop) continue;
                
                rankInfo.append(rankData.getFormattedRank()).append("\n");
                count++;
            }
            
            if (rankInfo.length() > 0) {
                
                rankInfo.setLength(rankInfo.length() - 1);
                builder.addField(configManager.getProfileFieldName("leaderboard-ranks"), rankInfo.toString(), false);
            }
        }

        
        if (configManager.isProfileFieldEnabled("statistics")) {
            StringBuilder statsInfo = new StringBuilder();
            
            if (configManager.shouldShowLoginStreak()) {
                statsInfo.append("**Giriş Serisi:** ").append(playerData.getLoginStreak()).append(" gün\n");
            }
            
            if (configManager.shouldShowVoteCount()) {
                statsInfo.append("**Oy Sayısı:** ").append(playerData.getVoteCount()).append("\n");
            }
            
            if (configManager.shouldShowKDRatio()) {
                java.text.DecimalFormat kdFormat = new java.text.DecimalFormat("#0.0");
                statsInfo.append("**K/D Oranı:** ").append(kdFormat.format(playerData.getKDRatio()));
            }
            
            if (statsInfo.length() > 0) {
                
                String finalStats = statsInfo.toString().replaceAll("\n$", "");
                builder.addField(configManager.getProfileFieldName("statistics"), finalStats, false);
            }
        }

        
        if (configManager.isProfileFieldEnabled("achievements")) {
            StringBuilder achievementInfo = new StringBuilder();
            
            if (configManager.shouldShowTotalAchievements()) {
                achievementInfo.append("**Toplam:** Yakında...\n");
            }
            
            if (configManager.shouldShowRecentAchievements()) {
                achievementInfo.append("**Son Başarım:** Yakında...");
            }
            
            if (achievementInfo.length() > 0) {
                builder.addField(configManager.getProfileFieldName("achievements"), achievementInfo.toString(), true);
            }
        }

        
        if (configManager.shouldShowRankFooter() && !rankings.isEmpty()) {
            int bestRank = statisticManager.getBestRank(rankings);
            if (bestRank > 0) {
                String rankTier = statisticManager.getRankTier(bestRank);
                String footerMessage = configManager.getRankFooterMessage(rankTier);
                builder.setFooter(footerMessage, null);
            }
        }

        return builder.build();
    }

    
    private static String getSkinUrl(String uuid, String skinType) {
        String baseUrl = "https://mc-heads.net/";
        
        switch (skinType.toLowerCase()) {
            case "avatar":
                return baseUrl + "avatar/" + uuid + "/64";
            case "head":
                return baseUrl + "head/" + uuid + "/64";
            case "body":
                return baseUrl + "body/" + uuid + "/right";
            case "head-3d":
                return baseUrl + "head/" + uuid + "/right";
            case "body-3d":
                return baseUrl + "body/" + uuid + "/right";
            default:
                return baseUrl + "body/" + uuid + "/right";
        }
    }

    public static MessageEmbed verificationEmbed() {
        String serverName = config.getString("embeds.server-name", "Sunucumuz");
        String serverIcon = config.getString("embeds.server-icon", "");

        return new EmbedBuilder()
                .setColor(EmbedType.BRAND.getColor())
                .setAuthor(serverName + " Hesap Doğrulama", null, serverIcon)
                .setTitle("🔗 Minecraft & Discord Hesap Eşleştirme")
                .setDescription("Hesabınızı eşleyerek sunucumuzdaki **özel özelliklere** ve **ödüllere** erişim kazanın!")
                .addField("1️⃣ Adım: Kodu Alın", "Minecraft sunucusuna girin ve `/verify` komutunu kullanarak doğrulama kodunuzu alın.", false)
                .addField("2️⃣ Adım: Kodu Girin", "Aşağıdaki **'Hesabı Eşle'** butonuna tıklayın ve açılan pencereye size verilen 6 haneli kodu girin.", false)
                .addField("✨ Avantajlar", "• Rol ve yetki senkronizasyonu\n• Özel ödüller ve çekilişler\n• Oyun içi istatistiklerinizi Discord'da görüntüleme", false)
                .setFooter(serverName + " | Güvenli ve Hızlı Eşleştirme", serverIcon)
                .build();
    }

    public static MessageEmbed votersListEmbed(String period, List<VoteManager.Voter> voters) {
        String title = period.equals("current") ? "Bu Ayın En Çok Oy Verenleri" : "Geçen Ayın En Çok Oy Verenleri";
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("🏆 " + title)
                .setColor(EmbedType.BRAND.getColor());

        if (voters == null || voters.isEmpty()) {
            builder.setDescription("Bu dönem için hiç oy veren bulunamadı. 😢");
        } else {
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            for (VoteManager.Voter voter : voters) {
                String medal = "";
                if (rank == 1) medal = "🥇 ";
                else if (rank == 2) medal = "🥈 ";
                else if (rank == 3) medal = "🥉 ";
                sb.append(String.format("%s`%d.` **%s** - %d oy\n", medal, rank, voter.nickname, voter.votes));
                rank++;
            }
            builder.setDescription(sb.toString());
        }

        return builder.build();
    }

    public static MessageEmbed initialLeaderboardEmbed() {
        String serverName = config.getString("embeds.server-name", "Sunucumuz");
        String serverIcon = config.getString("embeds.server-icon");
        return new EmbedBuilder()
                .setTitle("🏆 Sunucu Liderlik Tabloları")
                .setColor(EmbedType.BRAND.getColor())
                .setDescription("Aşağıdaki menüden görüntülemek istediğiniz istatistik kategorisini seçin.")
                .setThumbnail(serverIcon)
                .setFooter(serverName + " | İstatistikler", serverIcon)
                .build();
    }

    public static MessageEmbed leaderboardEmbed(String categoryTitle, String description, String icon) {
        String serverName = config.getString("embeds.server-name", "Sunucumuz");
        String serverIcon = config.getString("embeds.server-icon");
        return new EmbedBuilder()
                .setTitle(icon + " " + categoryTitle + " Sıralaması")
                .setColor(EmbedType.BRAND.getColor())
                .setThumbnail(serverIcon)
                .setDescription(description)
                .setFooter(serverName + " | " + categoryTitle, serverIcon)
                .build();
    }

    public static MessageEmbed errorEmbed(String description) {
        return createEmbed(EmbedType.ERROR, "Hata", description);
    }
}
