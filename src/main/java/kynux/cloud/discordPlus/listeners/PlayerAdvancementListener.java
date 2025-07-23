package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.ConfigManager;
import kynux.cloud.discordPlus.managers.DiscordManager;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;


public class PlayerAdvancementListener implements Listener {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DiscordManager discordManager;

    public PlayerAdvancementListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.discordManager = plugin.getDiscordManager();
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!configManager.isNotificationsEnabled() || !configManager.isAchievementsEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        String key = advancement.getKey().getKey(); 
        
        
        if (key.contains("recipes/")) {
            return; 
        }

        String advancementName = formatAdvancementName(key);
        
        if (configManager.shouldShowAdvancement()) { 
            sendAchievementNotification(player, advancementName);
        }
    }
    

    private String formatAdvancementName(String key) {
        String name = key.substring(key.lastIndexOf('/') + 1).replace('_', ' ');
        return capitalizeString(name);
    }

    private void sendAchievementNotification(Player player, String advancementName) {
        String message = MessageUtil.getRawMessage(MessageUtil.Messages.ACHIEVEMENT_PLAYER_ACHIEVED,
            "player", player.getName(), "achievement", advancementName);
        
        
        String discordMessage = ":trophy: " + message;
        
        String channelId = configManager.getAchievementChannelId();
        if (channelId != null && !channelId.isEmpty() && discordManager != null && discordManager.isReady()) {
            discordManager.sendMessage(channelId, discordMessage)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to send achievement notification: " + throwable.getMessage());
                    return null;
                });
        } else {
            plugin.getLogger().warning("Achievement notification channel ID not set or Discord manager not ready for advancement: " + advancementName);
        }
    }


    private String capitalizeString(String string) {
        char[] chars = string.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { 
                found = false;
            }
        }
        return String.valueOf(chars);
    }
}
