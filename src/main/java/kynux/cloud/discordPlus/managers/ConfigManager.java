package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {

    private final DiscordPlus plugin;
    private FileConfiguration config;
    private ConfigAutoCompleter autoCompleter;

    public ConfigManager(DiscordPlus plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.autoCompleter = new ConfigAutoCompleter(plugin, this);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        autoCompleteConfig();
    }

    
    public void autoCompleteConfig() {
        if (autoCompleter != null) {
            autoCompleter.completeConfig();
            
            this.config = plugin.getConfig();
        }
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public String getTimezone() { return config.getString("timezone", "Europe/Istanbul"); }
    public String getLanguage() { return config.getString("language", "tr"); }

    
    public String getDiscordToken() { return config.getString("discord.token", ""); }
    public String getGuildId() { return config.getString("discord.guild-id", ""); }
    public String getBotActivity() { return config.getString("discord.activity.static-message", "Minecraft Sunucusunu İzliyor"); }
    public String getDiscordInviteLink() { return config.getString("discord.invite-link", ""); }
    
    
    public String getBotActivityType() { return config.getString("discord.activity.type", "WATCHING"); }
    public boolean isBotActivityDynamic() { return config.getBoolean("discord.activity.dynamic", false); }
    public int getBotActivityUpdateInterval() { return config.getInt("discord.activity.update-interval", 30); }
    public List<String> getBotActivityMessages() { return config.getStringList("discord.activity.messages"); }
    public String getBotActivityStaticMessage() { return config.getString("discord.activity.static-message", "Minecraft Sunucusunu İzliyor"); }
    public boolean isBotActivityPlaceholdersEnabled() { return config.getBoolean("discord.activity.placeholders.enabled", true); }
    public int getBotActivityPlaceholderCacheDuration() { return config.getInt("discord.activity.placeholders.cache-duration", 10); }
    

    
    public String getDatabaseFile() { return config.getString("database.file", "discordplus.db"); }
    public int getDatabaseTimeout() { return config.getInt("database.timeout", 30); }

    
    public boolean isModuleEnabled(String moduleName) {
        return config.getBoolean("modules." + moduleName + ".enabled", false);
    }

    
    public boolean isAccountLinkingEnabled() { return isModuleEnabled("account-linking"); }
    public int getVerificationTimeout() { return config.getInt("modules.account-linking.verification-timeout", 10); }
    public boolean isWhitelistIntegrationEnabled() { return config.getBoolean("modules.account-linking.whitelist-integration", false); }
    public boolean isAccountLinkingRequired() { return config.getBoolean("modules.account-linking.required", false); }
    public String getLinkingChannelId() { return config.getString("modules.account-linking.channel-id", ""); }
    public String getLinkingLogChannelId() { return config.getString("modules.account-linking.log-channel-id", ""); }
    public String getLinkingEmbedTitle() { return config.getString("modules.account-linking.embed.title", "🔗 Hesap Eşleme Sistemi"); }
    public String getLinkingEmbedDescription() { return config.getString("modules.account-linking.embed.description", "Minecraft hesabınızı Discord ile eşlemek için aşağıdaki adımları takip edin:"); }
    public String getLinkingButtonLabel() { return config.getString("modules.account-linking.button.label", "🔗 Hesap Eşle"); }
    public String getVerificationMessageId() { return config.getString("modules.account-linking.message-id", ""); }
    public void setVerificationMessageId(String messageId) {
        config.set("modules.account-linking.message-id", messageId);
        saveConfig();
    }
    public boolean isRoleManagementEnabled() { return config.getBoolean("modules.account-linking.role-management.enabled", true); }
    public String getLinkedRoleId() { return config.getString("modules.account-linking.role-management.linked-role-id", ""); }
    public String getUnlinkedRoleId() { return config.getString("modules.account-linking.role-management.unlinked-role-id", ""); }

    
    public boolean isPermissionSyncEnabled() { return isModuleEnabled("permission-sync"); }
    public int getPermissionCheckInterval() { return config.getInt("modules.permission-sync.check-interval", 300); }
    public ConfigurationSection getRoleMappings() { return config.getConfigurationSection("modules.permission-sync.role-mappings"); }
    public boolean isDmNotificationsEnabled() { return config.getBoolean("modules.permission-sync.dm-notifications.enabled", true); }
    public boolean isDmOnRoleAddedEnabled() { return config.getBoolean("modules.permission-sync.dm-notifications.on-role-added", true); }
    public boolean isDmOnRoleRemovedEnabled() { return config.getBoolean("modules.permission-sync.dm-notifications.on-role-removed", true); }
    public boolean isAutoCleanupEnabled() { return config.getBoolean("modules.permission-sync.auto-cleanup", true); }
    public boolean isSingleRoleModeEnabled() { return config.getBoolean("modules.permission-sync.single-role-mode", false); }

    
    public boolean isChatBridgeEnabled() { return isModuleEnabled("chat-bridge"); }
    public String getChatChannelId() { return config.getString("modules.chat-bridge.channel-id", ""); }
    public boolean isMinecraftToDiscordEnabled() { return config.getBoolean("modules.chat-bridge.minecraft-to-discord", true); }
    public boolean isDiscordToMinecraftEnabled() { return config.getBoolean("modules.chat-bridge.discord-to-minecraft", true); }
    public boolean shouldUseWebhook() { return config.getBoolean("modules.chat-bridge.use-webhook", true); }
    public String getWebhookUrl() { return config.getString("modules.chat-bridge.webhook-url", ""); }
    public boolean shouldShowBotMessages() { return config.getBoolean("modules.chat-bridge.show-bot-messages", false); }
    public boolean isLuckPermsPrefixEnabled() { return config.getBoolean("modules.chat-bridge.luckperms-prefix.enabled", true); }
    public String getLuckPermsPrefixFormat() { return config.getString("modules.chat-bridge.luckperms-prefix.format", "[%group%] "); }

    
    public boolean isNotificationsEnabled() { return isModuleEnabled("notifications"); }
    public boolean isJoinQuitEnabled() { return isNotificationsEnabled() && config.getBoolean("modules.notifications.join-quit.enabled", true); }
    public String getJoinQuitChannelId() { return config.getString("modules.notifications.join-quit.channel-id", ""); }
    public boolean shouldShowJoin() { return config.getBoolean("modules.notifications.join-quit.show-join", true); }
    public boolean shouldShowQuit() { return config.getBoolean("modules.notifications.join-quit.show-quit", true); }
    public boolean isDeathNotificationsEnabled() { return isNotificationsEnabled() && config.getBoolean("modules.notifications.death-notifications.enabled", true); }
    public String getDeathChannelId() { return config.getString("modules.notifications.death-notifications.channel-id", ""); }
    public boolean shouldShowKiller() { return config.getBoolean("modules.notifications.death-notifications.show-killer", true); }
    public boolean shouldShowDeathCoordinates() { return config.getBoolean("modules.notifications.death-notifications.show-coordinates", true); }
    public boolean isAchievementsEnabled() { return isNotificationsEnabled() && config.getBoolean("modules.notifications.achievements.enabled", true); }
    public String getAchievementChannelId() { return config.getString("modules.notifications.achievements.channel-id", ""); }
    public boolean shouldShowAdvancement() { return config.getBoolean("modules.notifications.achievements.show-advancement", true); }
    public boolean isServerStatusEnabled() { return isNotificationsEnabled() && config.getBoolean("modules.notifications.server-status.enabled", true); }
    public String getServerStatusChannelId() { return config.getString("modules.notifications.server-status.channel-id", ""); }
    public int getServerStatusUpdateInterval() { return config.getInt("modules.notifications.server-status.update-interval", 5); }
    public boolean isServerStatusPersistent() { return config.getBoolean("modules.notifications.server-status.persistent-message", true); }
    public String getServerStatusMessageId() { return config.getString("modules.notifications.server-status.message-id", ""); }
    public void setServerStatusMessageId(String messageId) {
        config.set("modules.notifications.server-status.message-id", messageId);
        saveConfig();
    }

    
    public boolean isRewardsEnabled() { return isModuleEnabled("rewards"); }
    public boolean isDailyRewardsEnabled() { return isRewardsEnabled() && config.getBoolean("modules.rewards.daily-rewards.enabled", true); }
    public boolean isStreakBonusEnabled() { return isDailyRewardsEnabled() && config.getBoolean("modules.rewards.daily-rewards.streak-bonus", true); }
    public int getMaxStreak() { return config.getInt("modules.rewards.daily-rewards.max-streak", 30); }
    public List<String> getDailyRewards(int day) { return config.getStringList("modules.rewards.daily-rewards.rewards." + day); }
    public boolean isWeeklyRewardsEnabled() { return isRewardsEnabled() && config.getBoolean("modules.rewards.weekly-rewards.enabled", true); }
    public List<String> getWeeklyRewards() { return config.getStringList("modules.rewards.weekly-rewards.rewards"); }
    public boolean isCustomRewardsEnabled() { return isRewardsEnabled() && config.getBoolean("modules.rewards.custom-rewards.enabled", true); }
    public ConfigurationSection getCustomRewards() { return config.getConfigurationSection("modules.rewards.custom-rewards.rewards"); }
    public boolean isBoostRewardsEnabled() { return isRewardsEnabled() && config.getBoolean("modules.rewards.boost-rewards.enabled", true); }
    public String getBoostChannelId() { return config.getString("modules.rewards.boost-rewards.channel-id", ""); }
    public List<String> getBoostRewards() { return config.getStringList("modules.rewards.boost-rewards.rewards"); }
    public List<String> getServerWideBoostRewards() { return config.getStringList("modules.rewards.boost-rewards.server-wide-rewards"); }

    
    public boolean isVoteSystemEnabled() { return isModuleEnabled("vote-system"); }
    public String getVoteChannelId() { return config.getString("modules.vote-system.channel-id", ""); }
    public String getVoteApiKey() { return config.getString("modules.vote-system.api-key", ""); }
    public boolean isVoteSiteEnabled(String siteName) { return isVoteSystemEnabled() && config.getBoolean("modules.vote-system.sites." + siteName + ".enabled", false); }
    public String getVoteSiteUrl(String siteName) { return config.getString("modules.vote-system.sites." + siteName + ".url", ""); }
    public List<String> getVoteSiteRewards(String siteName) { return config.getStringList("modules.vote-system.sites." + siteName + ".rewards"); }
    public boolean isVoteStreakEnabled() { return isVoteSystemEnabled() && config.getBoolean("modules.vote-system.streak-rewards.enabled", true); }
    public List<String> getVoteStreakRewards(int streak) { return config.getStringList("modules.vote-system.streak-rewards.rewards." + streak); }
    public boolean isVoteLeaderboardEnabled() { return isVoteSystemEnabled() && config.getBoolean("modules.vote-system.leaderboard.enabled", true); }
    public String getVoteLeaderboardChannelId() { return config.getString("modules.vote-system.leaderboard.channel-id", ""); }
    public String getVoteLeaderboardMessageId() { return config.getString("modules.vote-system.leaderboard.message-id", ""); }
    public void setVoteLeaderboardMessageId(String messageId) {
        config.set("modules.vote-system.leaderboard.message-id", messageId);
        saveConfig();
    }

    
    public boolean isStatisticsEnabled() { return isModuleEnabled("statistics"); }
    public String getStatsChannelId() { return config.getString("modules.statistics.channel-id", ""); }
    public boolean isPlaytimeTrackingEnabled() { return isStatisticsEnabled() && config.getBoolean("modules.statistics.playtime.enabled", true); }
    public int getPlaytimeUpdateInterval() { return config.getInt("modules.statistics.playtime.update-interval", 1); }
    public boolean isLeaderboardsEnabled() { return isStatisticsEnabled() && config.getBoolean("modules.statistics.leaderboards.enabled", true); }
    public int getLeaderboardUpdateInterval() { return config.getInt("modules.statistics.leaderboards.update-interval", 60); }
    public List<String> getLeaderboardCategories() {
        ConfigurationSection categorySettings = config.getConfigurationSection("modules.statistics.leaderboards.category-settings");
        if (categorySettings == null) {
            return Collections.emptyList();
        }
        return categorySettings.getKeys(false).stream()
                .filter(key -> categorySettings.getBoolean(key + ".enabled", false))
                .collect(Collectors.toList());
    }
    public boolean isPlayerProfilesEnabled() { return isStatisticsEnabled() && config.getBoolean("modules.statistics.player-profiles.enabled", true); }
    public String getLeaderboardMessageId() { return config.getString("modules.statistics.leaderboards.message-id", ""); }
    public void setLeaderboardMessageId(String messageId) {
        config.set("modules.statistics.leaderboards.message-id", messageId);
        saveConfig();
    }

    
    public String getProfileEmbedTitle() { return config.getString("modules.statistics.player-profiles.embed.title", "{player_name} Profili"); }
    public String getProfileEmbedColor() { return config.getString("modules.statistics.player-profiles.embed.color", "#5865F2"); }
    public boolean shouldShowDiscordAvatar() { return config.getBoolean("modules.statistics.player-profiles.embed.show-discord-avatar", true); }
    public boolean shouldShowMinecraftSkin() { return config.getBoolean("modules.statistics.player-profiles.embed.show-minecraft-skin", true); }
    public String getMinecraftSkinType() { return config.getString("modules.statistics.player-profiles.embed.skin-type", "body"); }
    
    
    public boolean isProfileFieldEnabled(String fieldName) { 
        return config.getBoolean("modules.statistics.player-profiles.embed.fields." + fieldName + ".enabled", false); 
    }
    public String getProfileFieldName(String fieldName) { 
        return config.getString("modules.statistics.player-profiles.embed.fields." + fieldName + ".name", fieldName); 
    }
    
    
    public boolean shouldShowDiscordTag() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.discord-info.show-tag", true); }
    public boolean shouldShowLastSeen() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.discord-info.show-last-seen", true); }
    
    
    public boolean shouldShowCurrency() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.balance.show-currency", true); }
    
    
    public boolean shouldShowCurrentSession() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.playtime.show-current-session", true); }
    
    
    public List<String> getProfileRankCategories() { 
        return config.getStringList("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.categories"); 
    }
    public int getProfileMaxCategories() { return config.getInt("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.max-categories", 4); }
    public int getProfileShowOnlyTop() { return config.getInt("modules.statistics.player-profiles.embed.fields.leaderboard-ranks.show-only-top", 100); }
    
    
    public boolean shouldShowLoginStreak() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.statistics.show-login-streak", true); }
    public boolean shouldShowVoteCount() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.statistics.show-vote-count", true); }
    public boolean shouldShowKDRatio() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.statistics.show-kd-ratio", true); }
    public boolean shouldShowDeathCount() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.statistics.show-death-count", true); }
    
    
    public boolean shouldShowTotalAchievements() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.achievements.show-total-count", true); }
    public boolean shouldShowRecentAchievements() { return config.getBoolean("modules.statistics.player-profiles.embed.fields.achievements.show-recent", true); }
    
    
    public ConfigurationSection getProfileCustomFields() { 
        return config.getConfigurationSection("modules.statistics.player-profiles.embed.fields.custom-fields"); 
    }
    
    
    public boolean shouldShowRankFooter() { return config.getBoolean("modules.statistics.player-profiles.embed.footer.show-rank-footer", true); }
    public String getRankFooterMessage(String rankType) { 
        return config.getString("modules.statistics.player-profiles.embed.footer.rank-messages." + rankType, 
                              config.getString("modules.statistics.player-profiles.embed.footer.rank-messages.default", "İstatistiklerin güncelleniyor...")); 
    }

    public String getLeaderboardCategoryName(String category) {
        return config.getString("modules.statistics.leaderboards.category-settings." + category + ".name", capitalize(category));
    }

    public String getLeaderboardCategoryIcon(String category) {
        return config.getString("modules.statistics.leaderboards.category-settings." + category + ".emoji", "🏆");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    
    public boolean isStatisticsGuiEnabled() { return isStatisticsEnabled() && hasPath("modules.statistics.gui"); }
    public String getStatisticsGuiTitle() { return config.getString("modules.statistics.gui.title", "&9&lİstatistikler"); }
    public int getStatisticsGuiRows() { return config.getInt("modules.statistics.gui.rows", 4); }
    public ConfigurationSection getStatisticsGuiItems() { return config.getConfigurationSection("modules.statistics.gui.items"); }
    
    // GUI Item metodları
    public boolean isGuiItemEnabled(String itemName) { 
        return config.getBoolean("modules.statistics.gui.items." + itemName + ".enabled", false); 
    }
    public String getGuiItemName(String itemName) { 
        return config.getString("modules.statistics.gui.items." + itemName + ".name", itemName); 
    }
    public String getGuiItemMaterial(String itemName) { 
        return config.getString("modules.statistics.gui.items." + itemName + ".material", "STONE"); 
    }
    public List<String> getGuiItemLore(String itemName) { 
        return config.getStringList("modules.statistics.gui.items." + itemName + ".lore"); 
    }
    public int getGuiItemSlot(String itemName) { 
        return config.getInt("modules.statistics.gui.items." + itemName + ".slot", 0); 
    }

    
    public String getEmbedColor(String type) { return config.getString("embeds.colors." + type, "#7289DA"); }
    public String getEmbedFooterText() { return config.getString("embeds.footer.text", "DiscordPlus • Made by LoraProject"); }
    public String getEmbedFooterIcon() { return config.getString("embeds.footer.icon", ""); }
    public String getEmbedThumbnail() { return config.getString("embeds.thumbnail", ""); }
    public String getServerName() { return config.getString("embeds.server-name", "Minecraft Sunucusu"); }
    public String getServerUrl() { return config.getString("embeds.server-url", ""); }
    public boolean isWebsiteButtonEnabled() { return config.getBoolean("modules.notifications.server-status.website-button.enabled", false); }
    public String getWebsiteButtonLabel() { return config.getString("modules.notifications.server-status.website-button.label", "Website"); }
    public String getWebsiteButtonUrl() { return config.getString("modules.notifications.server-status.website-button.url", ""); }
    public String getWebsiteButtonEmoji() { return config.getString("modules.notifications.server-status.website-button.emoji", "🌐"); }

    
    public String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return null;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    public boolean hasPath(String path) {
        return config.contains(path);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }
}
