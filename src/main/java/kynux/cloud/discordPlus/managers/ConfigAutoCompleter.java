package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class ConfigAutoCompleter {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final Map<String, Object> defaultValues = new LinkedHashMap<>();
    private final List<String> addedKeys = new ArrayList<>();

    public ConfigAutoCompleter(DiscordPlus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        initializeDefaultValues();
    }

    
    public void completeConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.getLogger().info("Config dosyası bulunamadı, varsayılan config oluşturuluyor...");
                plugin.saveDefaultConfig();
                return;
            }

            
            createBackup(configFile);

            
            boolean hasChanges = checkAndAddMissingValues(configFile);

            if (hasChanges) {
                plugin.getLogger().info("Config başarıyla tamamlandı! " + addedKeys.size() + " yeni ayar eklendi.");
                for (String key : addedKeys) {
                    plugin.getLogger().info("+ " + key + ": " + getValueAsString(defaultValues.get(key)));
                }
                
                
                plugin.reloadConfig();
            } else {
                plugin.getLogger().info("Config kontrolü tamamlandı. Tüm ayarlar mevcut.");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Config otomatik tamamlanırken hata oluştu!", e);
        }
    }

    
    private void createBackup(File configFile) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(plugin.getDataFolder(), "config.yml.backup." + timestamp);
        
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Config yedeklendi: " + backupFile.getName());
    }

    
    private boolean checkAndAddMissingValues(File configFile) throws IOException {
        
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        
        
        List<String> missingKeys = new ArrayList<>();
        for (String key : defaultValues.keySet()) {
            if (!currentConfig.contains(key)) {
                missingKeys.add(key);
            }
        }

        if (missingKeys.isEmpty()) {
            return false;
        }

        plugin.getLogger().info("Config tamamlanıyor... " + missingKeys.size() + " eksik ayar bulundu:");

        
        for (String key : missingKeys) {
            Object value = defaultValues.get(key);
            currentConfig.set(key, value);
            addedKeys.add(key);
        }

        
        currentConfig.save(configFile);
        return true;
    }

    
    private void initializeDefaultValues() {
        
        defaultValues.put("timezone", "Europe/Istanbul");
        defaultValues.put("language", "tr");

        
        defaultValues.put("discord.token", "BOT_TOKEN_BURAYA");
        defaultValues.put("discord.guild-id", "GUILD_ID_BURAYA");
        defaultValues.put("discord.invite-link", "https://discord.gg/YOUR_INVITE");

        
        defaultValues.put("discord.activity.type", "WATCHING");
        defaultValues.put("discord.activity.dynamic", true);
        defaultValues.put("discord.activity.update-interval", 30);
        defaultValues.put("discord.activity.static-message", "Minecraft Sunucusunu İzliyor");
        defaultValues.put("discord.activity.placeholders.enabled", true);
        defaultValues.put("discord.activity.placeholders.cache-duration", 10);
        
        
        List<String> activityMessages = Arrays.asList(
            "%online% oyuncu online!",
            "%player% izliyorum",
            "Sunucuyu koruyorum",
            "%online%/%max_players% oyuncu",
            "Minecraft Sunucusunu İzliyor",
            "%server_name% sunucusunda",
            "Toplam %total_playtime% saat oynanmış"
        );
        defaultValues.put("discord.activity.messages", activityMessages);

        
        defaultValues.put("database.file", "discordplus.db");
        defaultValues.put("database.timeout", 30);

        
        initializeModuleDefaults();

        
        initializeEmbedDefaults();
    }

    
    private void initializeModuleDefaults() {
        
        defaultValues.put("modules.account-linking.enabled", true);
        defaultValues.put("modules.account-linking.channel-id", "ACCOUNT_LINKING_CHANNEL_ID");
        defaultValues.put("modules.account-linking.log-channel-id", "LOG_CHANNEL_ID_BURAYA");
        defaultValues.put("modules.account-linking.verification-timeout", 10);
        defaultValues.put("modules.account-linking.whitelist-integration", false);
        defaultValues.put("modules.account-linking.required", false);
        defaultValues.put("modules.account-linking.embed.title", "🔗 Hesap Eşleme Sistemi");
        defaultValues.put("modules.account-linking.embed.description", "Minecraft hesabınızı Discord ile eşlemek için aşağıdaki adımları takip edin:");
        defaultValues.put("modules.account-linking.button.label", "🔗 Hesap Eşle");
        defaultValues.put("modules.account-linking.role-management.enabled", true);
        defaultValues.put("modules.account-linking.role-management.linked-role-id", "");
        defaultValues.put("modules.account-linking.role-management.unlinked-role-id", "");

        
        defaultValues.put("modules.permission-sync.enabled", true);
        defaultValues.put("modules.permission-sync.check-interval", 300);
        defaultValues.put("modules.permission-sync.dm-notifications.enabled", true);
        defaultValues.put("modules.permission-sync.dm-notifications.on-role-added", true);
        defaultValues.put("modules.permission-sync.dm-notifications.on-role-removed", true);
        defaultValues.put("modules.permission-sync.auto-cleanup", true);
        defaultValues.put("modules.permission-sync.single-role-mode", false);

        
        initializeRoleMappings();

        
        defaultValues.put("modules.chat-bridge.enabled", true);
        defaultValues.put("modules.chat-bridge.channel-id", "CHAT_CHANNEL_ID");
        defaultValues.put("modules.chat-bridge.minecraft-to-discord", true);
        defaultValues.put("modules.chat-bridge.discord-to-minecraft", true);
        defaultValues.put("modules.chat-bridge.show-bot-messages", false);
        defaultValues.put("modules.chat-bridge.use-webhook", true);
        defaultValues.put("modules.chat-bridge.webhook-url", "");
        defaultValues.put("modules.chat-bridge.luckperms-prefix.enabled", true);
        defaultValues.put("modules.chat-bridge.luckperms-prefix.format", "[%group%] ");

        
        initializeNotificationDefaults();

        
        initializeVoteSystemDefaults();

        
        initializeRewardsDefaults();

        
        initializeStatisticsDefaults();
    }

    private void initializeRoleMappings() {
        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.role-id", "VIP_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.name", "VIP");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.priority", 1);
        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.plus.role-id", "VIP_PLUS_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.plus.name", "VIP+");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.vip.plus.priority", 2);

        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.mod.role-id", "MODERATOR_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.mod.name", "Moderatör");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.mod.priority", 10);
        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.admin.role-id", "ADMIN_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.admin.name", "Admin");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.admin.priority", 20);
        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.owner.role-id", "OWNER_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.owner.name", "Owner");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.owner.priority", 99);

        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.donator.role-id", "DONATOR_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.donator.name", "Destekçi");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.donator.priority", 5);
        
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.builder.role-id", "BUILDER_ROLE_ID");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.builder.name", "Builder");
        defaultValues.put("modules.permission-sync.role-mappings.discordplus.builder.priority", 3);
    }

    private void initializeNotificationDefaults() {
        defaultValues.put("modules.notifications.enabled", true);
        
        
        defaultValues.put("modules.notifications.join-quit.enabled", true);
        defaultValues.put("modules.notifications.join-quit.channel-id", "NOTIFICATIONS_CHANNEL_ID");
        defaultValues.put("modules.notifications.join-quit.show-join", true);
        defaultValues.put("modules.notifications.join-quit.show-quit", true);
        
        
        defaultValues.put("modules.notifications.death-notifications.enabled", true);
        defaultValues.put("modules.notifications.death-notifications.channel-id", "DEATH_CHANNEL_ID");
        defaultValues.put("modules.notifications.death-notifications.show-killer", true);
        defaultValues.put("modules.notifications.death-notifications.show-coordinates", true);
        
        
        defaultValues.put("modules.notifications.achievements.enabled", true);
        defaultValues.put("modules.notifications.achievements.channel-id", "ACHIEVEMENT_CHANNEL_ID");
        defaultValues.put("modules.notifications.achievements.show-advancement", true);
        
        
        defaultValues.put("modules.notifications.server-status.enabled", true);
        defaultValues.put("modules.notifications.server-status.channel-id", "STATUS_CHANNEL_ID");
        defaultValues.put("modules.notifications.server-status.update-interval", 5);
        defaultValues.put("modules.notifications.server-status.persistent-message", true);
        defaultValues.put("modules.notifications.server-status.message-id", "");
        defaultValues.put("modules.notifications.server-status.website-button.enabled", true);
        defaultValues.put("modules.notifications.server-status.website-button.label", "Website");
        defaultValues.put("modules.notifications.server-status.website-button.url", "https://kynux.cloud");
        defaultValues.put("modules.notifications.server-status.website-button.emoji", "🌐");
    }

    private void initializeVoteSystemDefaults() {
        defaultValues.put("modules.vote-system.enabled", true);
        defaultValues.put("modules.vote-system.channel-id", "VOTE_CHANNEL_ID");
        defaultValues.put("modules.vote-system.api-key", "YOUR_API_KEY");
        
        
        defaultValues.put("modules.vote-system.sites.minecraftmp.enabled", true);
        defaultValues.put("modules.vote-system.sites.minecraftmp.url", "https://minecraft-mp.com/server/YOUR_SERVER_ID");
        
        List<String> voteRewards = Arrays.asList("give {player} diamond 2", "eco give {player} 150");
        defaultValues.put("modules.vote-system.sites.minecraftmp.rewards", voteRewards);
        
        
        defaultValues.put("modules.vote-system.leaderboard.enabled", true);
        defaultValues.put("modules.vote-system.leaderboard.channel-id", "VOTE_LEADERBOARD_CHANNEL_ID");
        defaultValues.put("modules.vote-system.leaderboard.message-id", "");
        
        
        defaultValues.put("modules.vote-system.streak-rewards.enabled", true);
        defaultValues.put("modules.vote-system.streak-rewards.rewards.5", Arrays.asList("give {player} diamond 5"));
        defaultValues.put("modules.vote-system.streak-rewards.rewards.10", Arrays.asList("give {player} netherite_ingot 1"));
    }

    private void initializeRewardsDefaults() {
        defaultValues.put("modules.rewards.enabled", true);
        
        
        defaultValues.put("modules.rewards.daily-rewards.enabled", true);
        defaultValues.put("modules.rewards.daily-rewards.streak-bonus", true);
        defaultValues.put("modules.rewards.daily-rewards.max-streak", 30);
        
        
        defaultValues.put("modules.rewards.weekly-rewards.enabled", true);
        defaultValues.put("modules.rewards.weekly-rewards.rewards", Arrays.asList("give {player} diamond 10"));
        
        
        defaultValues.put("modules.rewards.custom-rewards.enabled", true);
        
        
        defaultValues.put("modules.rewards.boost-rewards.enabled", true);
        defaultValues.put("modules.rewards.boost-rewards.channel-id", "");
        defaultValues.put("modules.rewards.boost-rewards.rewards", Arrays.asList("give {player} diamond 5"));
        defaultValues.put("modules.rewards.boost-rewards.server-wide-rewards", Arrays.asList("broadcast &6Sunucu boost aldı! Teşekkürler!"));
    }

    private void initializeStatisticsDefaults() {
        defaultValues.put("modules.statistics.enabled", true);
        defaultValues.put("modules.statistics.channel-id", "STATS_CHANNEL_ID");
        
        
        defaultValues.put("modules.statistics.playtime.enabled", true);
        defaultValues.put("modules.statistics.playtime.update-interval", 1);
        
        
        defaultValues.put("modules.statistics.leaderboards.enabled", true);
        defaultValues.put("modules.statistics.leaderboards.update-interval", 60);
        defaultValues.put("modules.statistics.leaderboards.message-id", "");
        
        
        defaultValues.put("modules.statistics.leaderboards.category-settings.vault_eco_balance.name", "Bakiye");
        defaultValues.put("modules.statistics.leaderboards.category-settings.vault_eco_balance.emoji", "🏆");
        defaultValues.put("modules.statistics.leaderboards.category-settings.vault_eco_balance.enabled", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.command-enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.title", "{player_name} Profili");
        defaultValues.put("modules.statistics.player-profiles.embed.color", "#5865F2");
        defaultValues.put("modules.statistics.player-profiles.embed.show-discord-avatar", true);
        defaultValues.put("modules.statistics.player-profiles.embed.show-minecraft-skin", true);
        defaultValues.put("modules.statistics.player-profiles.embed.skin-type", "body");
        
        
        initializeProfileFieldDefaults();
        
        
        defaultValues.put("modules.statistics.command.enabled", true);
        defaultValues.put("modules.statistics.command.aliases", Arrays.asList("stats", "istatistikler"));
        
        
        initializeStatisticsGuiDefaults();
    }

    private void initializeProfileFieldDefaults() {
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.discord-info.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.discord-info.name", "📱 Discord Bilgileri");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.discord-info.show-tag", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.discord-info.show-last-seen", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.balance.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.balance.name", "💰 Bakiye");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.balance.show-currency", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.playtime.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.playtime.name", "⏰ Oyun Süresi");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.playtime.show-current-session", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.name", "🏆 Sıralamalar");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.categories", 
            Arrays.asList("playtime", "deaths", "votes", "vault_eco_balance"));
        defaultValues.put("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.max-categories", 4);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.show-only-top", 100);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.statistics.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.statistics.name", "📊 İstatistikler");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.statistics.show-login-streak", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.statistics.show-vote-count", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.statistics.show-death-count", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.fields.achievements.enabled", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.achievements.name", "🎯 Başarımlar");
        defaultValues.put("modules.statistics.player-profiles.embed.fields.achievements.show-total-count", true);
        defaultValues.put("modules.statistics.player-profiles.embed.fields.achievements.show-recent", true);
        
        
        defaultValues.put("modules.statistics.player-profiles.embed.footer.show-rank-footer", true);
        defaultValues.put("modules.statistics.player-profiles.embed.footer.rank-messages.top-10", "🏆 Top 10'da yer alıyor!");
        defaultValues.put("modules.statistics.player-profiles.embed.footer.rank-messages.top-50", "⭐ Top 50'de yer alıyor!");
        defaultValues.put("modules.statistics.player-profiles.embed.footer.rank-messages.top-100", "🎯 Top 100'de yer alıyor!");
        defaultValues.put("modules.statistics.player-profiles.embed.footer.rank-messages.default", "İstatistiklerin güncelleniyor...");
    }

    private void initializeStatisticsGuiDefaults() {
        defaultValues.put("modules.statistics.gui.title", "&9&l✦ &6&lİstatistikler &9&l✦");
        defaultValues.put("modules.statistics.gui.rows", 4);
        
        
        defaultValues.put("modules.statistics.gui.items.playtime.name", "&b&lOynama Süresi");
        defaultValues.put("modules.statistics.gui.items.playtime.material", "CLOCK");
        defaultValues.put("modules.statistics.gui.items.playtime.lore", Arrays.asList(
            "&7Sunucuda geçirdiğin toplam süre.",
            "",
            "&eSıralama: &f%ajlb_lb_playtime_1_alltime_name% - %ajlb_lb_playtime_1_alltime_value%"
        ));
        defaultValues.put("modules.statistics.gui.items.playtime.slot", 11);
        defaultValues.put("modules.statistics.gui.items.playtime.enabled", true);
        
        defaultValues.put("modules.statistics.gui.items.deaths.name", "&c&lÖlümler");
        defaultValues.put("modules.statistics.gui.items.deaths.material", "SKELETON_SKULL");
        defaultValues.put("modules.statistics.gui.items.deaths.lore", Arrays.asList(
            "&7Toplam ölüm sayın.",
            "",
            "&eEn Çok Ölen: &f%ajlb_lb_deaths_1_alltime_name% - %ajlb_lb_deaths_1_alltime_value%"
        ));
        defaultValues.put("modules.statistics.gui.items.deaths.slot", 13);
        defaultValues.put("modules.statistics.gui.items.deaths.enabled", true);
        
        defaultValues.put("modules.statistics.gui.items.login-streak.name", "&6&lGiriş Serisi");
        defaultValues.put("modules.statistics.gui.items.login-streak.material", "GOLDEN_APPLE");
        defaultValues.put("modules.statistics.gui.items.login-streak.lore", Arrays.asList(
            "&7Ardışık giriş yapma serin.",
            "",
            "&eEn İyi Seri: &f%ajlb_lb_loginstreak_1_alltime_name% - %ajlb_lb_loginstreak_1_alltime_value%"
        ));
        defaultValues.put("modules.statistics.gui.items.login-streak.slot", 15);
        defaultValues.put("modules.statistics.gui.items.login-streak.enabled", true);
        
        defaultValues.put("modules.statistics.gui.items.votes.name", "&d&lOylar");
        defaultValues.put("modules.statistics.gui.items.votes.material", "CHEST");
        defaultValues.put("modules.statistics.gui.items.votes.lore", Arrays.asList(
            "&7Sunucuya verdiğin toplam oy.",
            "",
            "&eEn Çok Oy Veren: &f%ajlb_lb_votes_1_alltime_name% - %ajlb_lb_votes_1_alltime_value%"
        ));
        defaultValues.put("modules.statistics.gui.items.votes.slot", 22);
        defaultValues.put("modules.statistics.gui.items.votes.enabled", true);
        
        defaultValues.put("modules.statistics.gui.items.balance.name", "&a&lBakiye");
        defaultValues.put("modules.statistics.gui.items.balance.material", "EMERALD");
        defaultValues.put("modules.statistics.gui.items.balance.lore", Arrays.asList(
            "&7Mevcut bakiye miktarınız.",
            "",
            "&eBakiye: &f%vault_eco_balance%"
        ));
        defaultValues.put("modules.statistics.gui.items.balance.slot", 20);
        defaultValues.put("modules.statistics.gui.items.balance.enabled", true);
    }

    
    private void initializeEmbedDefaults() {
        defaultValues.put("embeds.server-name", "Minecraft Sunucusu");
        defaultValues.put("embeds.server-icon", "");
        defaultValues.put("embeds.server-ip", "play.sunucum.com");
        defaultValues.put("embeds.server-url", "store.sunucum.com");
        
        
        defaultValues.put("embeds.colors.success", "#00FF00");
        defaultValues.put("embeds.colors.error", "#FF0000");
        defaultValues.put("embeds.colors.info", "#00BFFF");
        defaultValues.put("embeds.colors.warning", "#FFA500");
        defaultValues.put("embeds.colors.default", "#7289DA");
        
        
        defaultValues.put("embeds.footer.text", "DiscordPlus • Made by LoraProject");
        defaultValues.put("embeds.footer.icon", "");
        
        
        defaultValues.put("embeds.thumbnail", "");
    }

    
    private String getValueAsString(Object value) {
        if (value instanceof List) {
            return "[" + String.join(", ", ((List<?>) value).stream().map(String::valueOf).toArray(String[]::new)) + "]";
        }
        return String.valueOf(value);
    }
}
