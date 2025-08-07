package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.data.VoteData;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private Connection connection;

    public DatabaseManager(DiscordPlus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            String dbFile = configManager.getDatabaseFile();
            String databasePath = new File(dataFolder, dbFile).getAbsolutePath();
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:" + databasePath);
            createTables();
            updateDatabaseSchema();
            plugin.getLogger().info("H2 database initialized at: " + databasePath);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (" +
                    "minecraft_uuid VARCHAR(36) PRIMARY KEY, discord_id VARCHAR(255), verification_code VARCHAR(10), " +
                    "verified BOOLEAN DEFAULT FALSE, link_date TIMESTAMP, last_seen TIMESTAMP, total_playtime BIGINT DEFAULT 0, " +
                    "session_start TIMESTAMP, daily_playtime BIGINT DEFAULT 0, last_daily_reset TIMESTAMP, death_count INT DEFAULT 0, " +
                    "kill_count INT DEFAULT 0, vote_count INT DEFAULT 0, login_streak INT DEFAULT 0, last_login TIMESTAMP, " +
                    "verification_code_timestamp TIMESTAMP)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS vote_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36), vote_site VARCHAR(255), " +
                    "vote_date TIMESTAMP, rewarded BOOLEAN, reward_data TEXT, streak_count INT)");
        }
    }

    private void updateDatabaseSchema() throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        
        
        try (ResultSet columns = meta.getColumns(null, null, "PLAYER_DATA", "VERIFICATION_CODE_TIMESTAMP")) {
            if (!columns.next()) {
                plugin.getLogger().info("Updating database schema: Adding 'verification_code_timestamp' column to 'player_data' table.");
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE player_data ADD verification_code_timestamp TIMESTAMP");
                    plugin.getLogger().info("Database schema updated successfully.");
                }
            }
        }
        
        
        try (ResultSet columns = meta.getColumns(null, null, "PLAYER_DATA", "KILL_COUNT")) {
            if (!columns.next()) {
                plugin.getLogger().info("Updating database schema: Adding 'kill_count' column to 'player_data' table.");
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE player_data ADD kill_count INT DEFAULT 0");
                    plugin.getLogger().info("Kill count column added successfully.");
                }
            }
        }
        
        
        try (ResultSet columns = meta.getColumns(null, null, "PLAYER_DATA", "LAST_LOGIN")) {
            if (columns.next()) {
                String typeName = columns.getString("TYPE_NAME");
                if ("DATE".equals(typeName)) {
                    plugin.getLogger().info("Updating database schema: Converting 'last_login' column from DATE to TIMESTAMP.");
                    try (Statement stmt = connection.createStatement()) {
                        
                        stmt.executeUpdate("ALTER TABLE player_data ADD last_login_temp TIMESTAMP");
                        
                        stmt.executeUpdate("UPDATE player_data SET last_login_temp = last_login WHERE last_login IS NOT NULL");
                        
                        stmt.executeUpdate("ALTER TABLE player_data DROP COLUMN last_login");
                        
                        stmt.executeUpdate("ALTER TABLE player_data RENAME COLUMN last_login_temp TO last_login");
                        plugin.getLogger().info("last_login column converted from DATE to TIMESTAMP successfully.");
                    }
                }
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection.", e);
        }
    }
public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_data WHERE minecraft_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return createPlayerDataFromResultSet(rs);
                    return new PlayerData(uuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
                return new PlayerData(uuid);
            }
        });
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                
                String checkSql = "SELECT COUNT(*) FROM player_data WHERE minecraft_uuid = ?";
                boolean exists = false;
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, data.getMinecraftUUID().toString());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            exists = rs.getInt(1) > 0;
                        }
                    }
                }

                if (exists) {
                    
                    String updateSql = "UPDATE player_data SET discord_id = ?, verification_code = ?, verified = ?, " +
                            "link_date = ?, last_seen = ?, total_playtime = ?, session_start = ?, daily_playtime = ?, " +
                            "last_daily_reset = ?, death_count = ?, kill_count = ?, vote_count = ?, login_streak = ?, " +
                            "last_login = ?, verification_code_timestamp = ? WHERE minecraft_uuid = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, data.getDiscordId());
                        updateStmt.setString(2, data.getVerificationCode());
                        updateStmt.setBoolean(3, data.isVerified());
                        updateStmt.setTimestamp(4, toTimestamp(data.getLinkDate()));
                        updateStmt.setTimestamp(5, toTimestamp(data.getLastSeen()));
                        updateStmt.setLong(6, data.getTotalPlaytime());
                        updateStmt.setTimestamp(7, toTimestamp(data.getSessionStart()));
                        updateStmt.setLong(8, data.getDailyPlaytime());
                        updateStmt.setTimestamp(9, toTimestamp(data.getLastDailyReset()));
                        updateStmt.setInt(10, data.getDeathCount());
                        updateStmt.setInt(11, data.getKillCount());
                        updateStmt.setInt(12, data.getVoteCount());
                        updateStmt.setInt(13, data.getLoginStreak());
                        updateStmt.setTimestamp(14, toTimestamp(data.getLastLogin()));
                        updateStmt.setTimestamp(15, toTimestamp(data.getVerificationCodeTimestamp()));
                        updateStmt.setString(16, data.getMinecraftUUID().toString());
                        updateStmt.executeUpdate();
                    }
                } else {
                    
                    String insertSql = "INSERT INTO player_data (minecraft_uuid, discord_id, verification_code, verified, " +
                            "link_date, last_seen, total_playtime, session_start, daily_playtime, last_daily_reset, " +
                            "death_count, kill_count, vote_count, login_streak, last_login, verification_code_timestamp) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, data.getMinecraftUUID().toString());
                        insertStmt.setString(2, data.getDiscordId());
                        insertStmt.setString(3, data.getVerificationCode());
                        insertStmt.setBoolean(4, data.isVerified());
                        insertStmt.setTimestamp(5, toTimestamp(data.getLinkDate()));
                        insertStmt.setTimestamp(6, toTimestamp(data.getLastSeen()));
                        insertStmt.setLong(7, data.getTotalPlaytime());
                        insertStmt.setTimestamp(8, toTimestamp(data.getSessionStart()));
                        insertStmt.setLong(9, data.getDailyPlaytime());
                        insertStmt.setTimestamp(10, toTimestamp(data.getLastDailyReset()));
                        insertStmt.setInt(11, data.getDeathCount());
                        insertStmt.setInt(12, data.getKillCount());
                        insertStmt.setInt(13, data.getVoteCount());
                        insertStmt.setInt(14, data.getLoginStreak());
                        insertStmt.setTimestamp(15, toTimestamp(data.getLastLogin()));
                        insertStmt.setTimestamp(16, toTimestamp(data.getVerificationCodeTimestamp()));
                        insertStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getMinecraftUUID(), e);
            }
        });
    }

    public CompletableFuture<PlayerData> getPlayerByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_data WHERE discord_id = ? AND verified = TRUE")) {
                stmt.setString(1, discordId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return createPlayerDataFromResultSet(rs);
                    return null;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player by discord ID " + discordId, e);
                return null;
            }
        });
    }

    public CompletableFuture<PlayerData> getPlayerByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                    return null;
                }
                
                UUID playerUUID = offlinePlayer.getUniqueId();
                PlayerData playerData = loadPlayerData(playerUUID).join();
                
                if (playerData != null && playerData.isLinked()) {
                    return playerData;
                }
                return null;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player by username: " + username, e);
                return null;
            }
        });
    }

    public CompletableFuture<PlayerData> getPlayerByVerificationCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_data WHERE verification_code = ?")) {
                stmt.setString(1, code);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return createPlayerDataFromResultSet(rs);
                    return null;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player by verification code.", e);
                return null;
            }
        });
    }
public UUID getPlayerUUIDByDiscordId(String discordId) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT minecraft_uuid FROM player_data WHERE discord_id = ? AND verified = TRUE")) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("minecraft_uuid"));
                }
                return null;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player UUID by discord ID " + discordId, e);
            return null;
        }
    }

    public void addPlaytime(UUID uuid, int seconds) {
        runAsync("UPDATE player_data SET total_playtime = total_playtime + ? WHERE minecraft_uuid = ?", seconds, uuid.toString());
    }

    public void incrementDeaths(UUID uuid) {
        runAsync("UPDATE player_data SET death_count = death_count + 1 WHERE minecraft_uuid = ?", uuid.toString());
    }

    public void incrementKills(UUID uuid) {
        runAsync("UPDATE player_data SET kill_count = kill_count + 1 WHERE minecraft_uuid = ?", uuid.toString());
    }
    
    public void incrementPlaytime(UUID uuid, int minutes) {
        addPlaytime(uuid, minutes * 60);
    }
    
    public void incrementDailyPlaytime(UUID uuid, int minutes) {
        
        runAsync("UPDATE player_data SET daily_playtime = daily_playtime + ? WHERE minecraft_uuid = ?", minutes * 60, uuid.toString());
    }

    public CompletableFuture<Map<String, Object>> getDatabaseStats() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> stats = new HashMap<>();
            try (Statement stmt = connection.createStatement()) {
                stats.put("total_players", count(stmt, "SELECT COUNT(*) FROM player_data"));
                stats.put("linked_players", count(stmt, "SELECT COUNT(*) FROM player_data WHERE verified = TRUE"));
                stats.put("total_votes", count(stmt, "SELECT COUNT(*) FROM vote_history"));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get database stats.", e);
            }
            return stats;
        });
    }

    public CompletableFuture<Integer> getTotalLinkedPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try (Statement stmt = connection.createStatement()) {
                return count(stmt, "SELECT COUNT(*) FROM player_data WHERE discord_id IS NOT NULL AND verified = TRUE");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get total linked players.", e);
                return 0;
            }
        });
    }

    public List<Map.Entry<String, Integer>> getTopPlaytime(int limit) { return getTop("total_playtime", limit); }
    public List<Map.Entry<String, Integer>> getTopDeaths(int limit) { return getTop("death_count", limit); }
    public List<Map.Entry<String, Integer>> getTopKills(int limit) { return getTop("kill_count", limit); }
    public List<Map.Entry<String, Integer>> getTopVotes(int limit) { return getTop("vote_count", limit); }
    public List<Map.Entry<String, Integer>> getTopLoginStreak(int limit) { return getTop("login_streak", limit); }


    public CompletableFuture<Void> saveVoteData(VoteData data) {
        return runAsync("INSERT INTO vote_history (player_uuid, vote_site, vote_date, rewarded, reward_data, streak_count) VALUES (?, ?, ?, ?, ?, ?)",
                data.getMinecraftUUID().toString(), data.getVoteSite(), toTimestamp(data.getVoteDate()), data.isRewarded(), data.getRewardData(), data.getStreakCount());
    }

    public CompletableFuture<Map<String, Object>> getPlayerVoteStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> stats = new HashMap<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) as total, MAX(vote_date) as last_vote FROM vote_history WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("total_votes", rs.getInt("total"));
                        stats.put("last_vote", toLocalDateTime(rs.getTimestamp("last_vote")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get vote stats for " + uuid, e);
            }
            return stats;
        });
    }


    

    private PlayerData createPlayerDataFromResultSet(ResultSet rs) throws SQLException {
        return new PlayerData(
            UUID.fromString(rs.getString("minecraft_uuid")),
            rs.getString("discord_id"),
            rs.getString("verification_code"),
            rs.getBoolean("verified"),
            toLocalDateTime(rs.getTimestamp("verification_code_timestamp")),
            toLocalDateTime(rs.getTimestamp("link_date")),
            rs.getLong("total_playtime"),
            toLocalDateTime(rs.getTimestamp("session_start")),
            rs.getLong("daily_playtime"),
            toLocalDateTime(rs.getTimestamp("last_daily_reset")),
            rs.getInt("death_count"),
            rs.getInt("kill_count"),
            rs.getInt("vote_count"),
            rs.getInt("login_streak"),
            toLocalDateTime(rs.getTimestamp("last_login")),
            toLocalDateTime(rs.getTimestamp("last_seen"))
        );
    }

    private List<Map.Entry<String, Integer>> getTop(String column, int limit) {
        List<Map.Entry<String, Integer>> topList = new ArrayList<>();
        String sql = "SELECT minecraft_uuid, " + column + " FROM player_data WHERE " + column + " > 0 ORDER BY " + column + " DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("minecraft_uuid"));
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) name = "Bilinmeyen";
                    topList.add(new AbstractMap.SimpleEntry<>(name, rs.getInt(column)));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top " + column, e);
        }
        return topList;
    }

    private int count(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private CompletableFuture<Void> runAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to execute async DB update.", e);
            }
        });
    }

    private Timestamp toTimestamp(LocalDateTime ldt) { 
        if (ldt == null) return null;
        try {
            return Timestamp.valueOf(ldt);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to convert LocalDateTime to Timestamp: " + ldt, e);
            return null;
        }
    }
    
    private LocalDateTime toLocalDateTime(Timestamp ts) { 
        if (ts == null) return null;
        try {
            return ts.toLocalDateTime();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to convert Timestamp to LocalDateTime: " + ts, e);
            return null;
        }
    }
}
