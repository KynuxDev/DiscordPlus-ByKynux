package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.managers.StatisticManager;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import kynux.cloud.discordPlus.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;

public class PlayerQuitListener implements Listener {
    
    private final DiscordPlus plugin;
    private final StatisticManager statisticManager;
    
    public PlayerQuitListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.statisticManager = plugin.getStatisticManager();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerUuid = player.getUniqueId().toString();
        int onlineCount = Bukkit.getOnlinePlayers().size() - 1;

        long sessionDurationMillis = statisticManager.stopTracking(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                    .thenAccept(playerData -> handlePlayerQuit(playerName, playerUuid, onlineCount, playerData, sessionDurationMillis))
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to load player data for " + playerName + " on quit: " + throwable.getMessage());
                        return null;
                    });
        });
    }

    private void handlePlayerQuit(String playerName, String playerUuid, int onlineCount, PlayerData playerData, long sessionDurationMillis) {
        playerData.setLastSeen(TimeUtil.now());

        if (plugin.getConfigManager().isJoinQuitEnabled() && plugin.getConfigManager().shouldShowQuit()) {
            String channelId = plugin.getConfigManager().getJoinQuitChannelId();
            if (channelId != null && !channelId.isEmpty()) {
                String sessionDuration = TimeUtil.formatDuration(sessionDurationMillis);
                plugin.getDiscordManager().sendEmbed(channelId, EmbedBuilder.playerQuit(playerName, playerUuid, onlineCount, sessionDuration));
            }
        }

        plugin.getDatabaseManager().savePlayerData(playerData);
    }
    
    
    
}
