package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import java.util.logging.Level;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticManager {

    private final DiscordPlus plugin;
    private final DatabaseManager databaseManager;
    private final DiscordManager discordManager;
    private final ConfigManager configManager;
    private final Map<UUID, Long> playtimeSessions = new ConcurrentHashMap<>();

    public StatisticManager(DiscordPlus plugin, DatabaseManager databaseManager, DiscordManager discordManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.discordManager = discordManager;
        this.configManager = configManager;
    }

    public void startTracking(Player player) {
        if (configManager.isPlaytimeTrackingEnabled()) {
            playtimeSessions.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public long stopTracking(Player player) {
        if (configManager.isPlaytimeTrackingEnabled() && playtimeSessions.containsKey(player.getUniqueId())) {
            long sessionStart = playtimeSessions.remove(player.getUniqueId());
            long sessionDurationMillis = System.currentTimeMillis() - sessionStart;
            databaseManager.addPlaytime(player.getUniqueId(), (int) (sessionDurationMillis / 1000)); 
            return sessionDurationMillis;
        }
        return 0;
    }

    public void incrementDeaths(Player player) {
        if (configManager.isStatisticsEnabled()) {
            databaseManager.incrementDeaths(player.getUniqueId());
        }
    }

    public void incrementKills(Player player) {
        if (configManager.isStatisticsEnabled()) {
            databaseManager.incrementKills(player.getUniqueId());
        }
    }

    public void schedulePlaytimeUpdates() {
        if (!configManager.isStatisticsEnabled() || !configManager.isPlaytimeTrackingEnabled()) return;

        long interval = configManager.getPlaytimeUpdateInterval() * 20L * 60L; 
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    stopTracking(player);
                    startTracking(player);
                }
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);
    }

    

    public MessageEmbed createLeaderboardEmbed(String category) {
        if (category == null || category.isEmpty()) {
            return EmbedBuilder.initialLeaderboardEmbed();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI not found! ajLeaderboards integration requires PlaceholderAPI.");
            return EmbedBuilder.errorEmbed("PlaceholderAPI bulunamadı. Liderlik tabloları devre dışı.");
        }

        StringBuilder sb = new StringBuilder();
        boolean hasData = false;
        for (int i = 1; i <= 10; i++) {
            String placeholderName = "%ajlb_lb_" + category + "_" + i + "_alltime_name%";
            String placeholderValue = "%ajlb_lb_" + category + "_" + i + "_alltime_value%";

            String playerName = PlaceholderAPI.setPlaceholders(null, placeholderName);
            String playerValue = PlaceholderAPI.setPlaceholders(null, placeholderValue);

            if (playerName != null && !playerName.isEmpty() && !playerName.equals(placeholderName) && !playerName.equalsIgnoreCase("board does not exist") && playerValue != null) {
                hasData = true;
                String medal = "";
                if (i == 1) medal = "🥇 ";
                else if (i == 2) medal = "🥈 ";
                else if (i == 3) medal = "🥉 ";
                sb.append(String.format("%s`%d.` **%s** - %s\n", medal, i, playerName, playerValue));
            } else {
                break;
            }
        }

        if (!hasData) {
            sb.append("Bu kategoride henüz sıralanacak veri bulunmuyor. 😢");
        }
        
        String categoryName = configManager.getLeaderboardCategoryName(category);
        String categoryIcon = configManager.getLeaderboardCategoryIcon(category);
        return EmbedBuilder.leaderboardEmbed(categoryName, sb.toString(), categoryIcon);
    }
    public PlayerData getPlayerData(UUID uuid) {
        try {
            return databaseManager.loadPlayerData(uuid).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player data for " + uuid, e);
            return null;
        }
    }

    public Long getPlaytimeSession(UUID uuid) {
        return playtimeSessions.get(uuid);
    }

    
    public int getPlayerRank(String playerName, String category) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return -1;
        }

        try {
            
            String placeholder = "%ajlb_lb_" + category + "_" + playerName + "_alltime_rank%";
            String rankStr = PlaceholderAPI.setPlaceholders(null, placeholder);
            
            if (rankStr != null && !rankStr.equals(placeholder) && !rankStr.equalsIgnoreCase("player not found")) {
                return Integer.parseInt(rankStr);
            }
        } catch (NumberFormatException e) {
            
        }
        
        return -1; 
    }

    
    public String getPlayerLeaderboardValue(String playerName, String category) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return null;
        }

        try {
            String placeholder = "%ajlb_lb_" + category + "_" + playerName + "_alltime_value%";
            String value = PlaceholderAPI.setPlaceholders(null, placeholder);
            
            if (value != null && !value.equals(placeholder) && !value.equalsIgnoreCase("player not found")) {
                return value;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting leaderboard value for " + playerName + " in " + category + ": " + e.getMessage());
        }
        
        return null;
    }

    
    public Map<String, PlayerRankInfo> getPlayerRankings(String playerName, List<String> categories) {
        Map<String, PlayerRankInfo> rankings = new HashMap<>();
        
        for (String category : categories) {
            int rank = getPlayerRank(playerName, category);
            String value = getPlayerLeaderboardValue(playerName, category);
            String categoryName = configManager.getLeaderboardCategoryName(category);
            String categoryIcon = configManager.getLeaderboardCategoryIcon(category);
            
            if (rank > 0) { 
                rankings.put(category, new PlayerRankInfo(category, categoryName, categoryIcon, rank, value));
            }
        }
        
        return rankings;
    }

    
    public String getRankTier(int rank) {
        if (rank <= 10) return "top-10";
        if (rank <= 50) return "top-50";
        if (rank <= 100) return "top-100";
        return "default";
    }

    
    public int getBestRank(Map<String, PlayerRankInfo> rankings) {
        return rankings.values().stream()
                .mapToInt(PlayerRankInfo::getRank)
                .min()
                .orElse(-1);
    }

    
    public static class PlayerRankInfo {
        private final String category;
        private final String categoryName;
        private final String categoryIcon;
        private final int rank;
        private final String value;

        public PlayerRankInfo(String category, String categoryName, String categoryIcon, int rank, String value) {
            this.category = category;
            this.categoryName = categoryName;
            this.categoryIcon = categoryIcon;
            this.rank = rank;
            this.value = value;
        }

        public String getCategory() { return category; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryIcon() { return categoryIcon; }
        public int getRank() { return rank; }
        public String getValue() { return value; }
        
        public String getFormattedRank() {
            return categoryIcon + " **" + categoryName + ":** #" + rank;
        }
        
        public String getFormattedRankWithValue() {
            return categoryIcon + " **" + categoryName + ":** #" + rank + " (" + value + ")";
        }
    }
}
