package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.ConfigManager;
import kynux.cloud.discordPlus.managers.DiscordManager;
import kynux.cloud.discordPlus.managers.StatisticManager;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DiscordManager discordManager;
    private final StatisticManager statisticManager;

    public PlayerDeathListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.discordManager = plugin.getDiscordManager();
        this.statisticManager = plugin.getStatisticManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        statisticManager.incrementDeaths(player);

        
        Player killer = player.getKiller();
        if (killer != null && killer != player) {
            statisticManager.incrementKills(killer);
        }

        if (!configManager.isNotificationsEnabled() || !configManager.isDeathNotificationsEnabled()) {
            return;
        }

        String deathMessage = event.getDeathMessage() != null ? event.getDeathMessage().replaceAll("§[0-9a-fk-or]", "") : "Bilinmeyen bir nedenle öldü.";
        String killerName = killer != null ? killer.getName() : null;
        String locationString = null;
        if (configManager.shouldShowDeathCoordinates()) {
            Location loc = player.getLocation();
            locationString = String.format("%s: %d, %d, %d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        MessageEmbed embed = EmbedBuilder.deathNotification(player.getName(), player.getUniqueId().toString(), deathMessage, killerName, locationString);

        sendDeathNotification(embed);
    }

    private void sendDeathNotification(MessageEmbed embed) {
        String channelId = configManager.getDeathChannelId();
        if (channelId != null && !channelId.isEmpty() && discordManager.isReady()) {
            discordManager.sendEmbed(channelId, embed);
        }
    }
}
