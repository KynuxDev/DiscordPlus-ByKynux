package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.managers.StatisticManager;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import kynux.cloud.discordPlus.utils.MessageUtil;
import kynux.cloud.discordPlus.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDateTime;

public class PlayerJoinListener implements Listener {

    private final DiscordPlus plugin;
    private final StatisticManager statisticManager;

    public PlayerJoinListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.statisticManager = plugin.getStatisticManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        statisticManager.startTracking(player);

        if (plugin.getPermissionSyncManager().isEnabled()) {
            plugin.getPermissionSyncManager().queuePlayerSync(player);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                    .thenAccept(playerData -> handlePlayerJoin(player, playerData))
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + throwable.getMessage());
                        return null;
                    });
        });
    }

    private void handlePlayerJoin(Player player, PlayerData playerData) {
        LocalDateTime now = TimeUtil.now();
        playerData.setLastSeen(now);

        if (plugin.getConfigManager().isJoinQuitEnabled() && plugin.getConfigManager().shouldShowJoin()) {
            String channelId = plugin.getConfigManager().getJoinQuitChannelId();
            if (channelId != null && !channelId.isEmpty()) {
                int onlineCount = Bukkit.getOnlinePlayers().size();
                plugin.getDiscordManager().sendEmbed(channelId, EmbedBuilder.playerJoin(player.getName(), player.getUniqueId().toString(), onlineCount));
            }
        }
        
        plugin.getDatabaseManager().savePlayerData(playerData);
    }
    
    
}
