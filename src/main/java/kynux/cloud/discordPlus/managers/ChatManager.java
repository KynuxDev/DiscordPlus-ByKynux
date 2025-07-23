package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ChatManager implements Listener {
    
    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final LuckPermsManager luckPermsManager;
    
    public ChatManager(DiscordPlus plugin, ConfigManager configManager, 
                      DiscordManager discordManager, DatabaseManager databaseManager, LuckPermsManager luckPermsManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
        this.luckPermsManager = luckPermsManager;
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("ChatManager initialized with advanced chat bridge features");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!configManager.isChatBridgeEnabled() || !configManager.isMinecraftToDiscordEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        if (shouldFilterMessage(message)) {
            return;
        }
        
        sendMinecraftMessageToDiscord(player, message);
    }
    
    public CompletableFuture<Boolean> sendMinecraftMessageToDiscord(Player player, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channelId = configManager.getChatChannelId();
                if (channelId.isEmpty()) {
                    plugin.getLogger().warning("Chat channel ID is not configured!");
                    return false;
                }
                
                if (configManager.shouldUseWebhook()) {
                    String webhookUrl = configManager.getWebhookUrl();
                    if (webhookUrl != null && !webhookUrl.isEmpty()) {
                        return sendWebhookMessage(player, message);
                    } else {
                        plugin.getLogger().warning("Webhook is enabled but URL is empty, falling back to normal message");
                    }
                }
                
                String formattedMessage = formatMinecraftMessageSync(player, message);
                
                discordManager.sendMessage(channelId, formattedMessage)
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Error sending message: " + throwable.getMessage());
                        return null;
                    });
                    
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending Minecraft message to Discord", e);
                return false;
            }
        });
    }
    
    private String formatMinecraftMessageSync(Player player, String message) {
        try {
            return databaseManager.loadPlayerData(player.getUniqueId())
                    .thenApply(playerData -> {
                        StringBuilder formatted = new StringBuilder();
                        
                        if (configManager.isLuckPermsPrefixEnabled() && luckPermsManager.isLuckPermsAvailable()) {
                            String primaryGroup = luckPermsManager.getPlayerPrimaryGroup(player);
                            if (primaryGroup != null) {
                                formatted.append(configManager.getLuckPermsPrefixFormat().replace("%group%", primaryGroup));
                            }
                        } else {
                            String playerRank = getPlayerRank(player);
                            if (playerRank != null && !playerRank.isEmpty()) {
                                formatted.append("**[").append(playerRank).append("]** ");
                            } else {
                                formatted.append("**[Minecraft]** ");
                            }
                        }
                        
                        formatted.append("**").append(player.getName()).append("**");
                        
                        
                        formatted.append(": ").append(processMessageContent(message));
                        
                        return formatted.toString();
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Error formatting message for " + player.getName() + ": " + throwable.getMessage());
                        return String.format("**[Minecraft]** **%s**: %s", 
                            player.getName(), processMessageContent(message));
                    })
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Timeout or error formatting message, using fallback: " + e.getMessage());
            return String.format("**[Minecraft]** **%s**: %s", 
                player.getName(), processMessageContent(message));
        }
    }
    
    private String processMessageContent(String message) {
        message = convertMinecraftToDiscord(message);
        message = processMentions(message);
        message = processEmotes(message);
        return message;
    }
    
    private String convertMinecraftToDiscord(String message) {
        return message
                .replaceAll("&l([^&]*)", "**$1**")      
                .replaceAll("&o([^&]*)", "*$1*")        
                .replaceAll("&n([^&]*)", "__$1__")      
                .replaceAll("&m([^&]*)", "~~$1~~")      
                .replaceAll("&[0-9a-fk-or]", "");       
    }
    
    private String processMentions(String message) {
        return message;
    }
    
    private String processEmotes(String message) {
        return message
                .replace(":)", "😊")
                .replace(":(", "😢")
                .replace(":D", "😄")
                .replace(":P", "😛")
                .replace("<3", "❤️")
                .replace(":heart:", "❤️");
    }
    
    private String getPlayerRank(Player player) {
        if (luckPermsManager != null && luckPermsManager.isLuckPermsAvailable() && configManager.isLuckPermsPrefixEnabled()) {
            String primaryGroup = luckPermsManager.getPlayerPrimaryGroup(player);
            if (primaryGroup != null) {
                return primaryGroup.substring(0, 1).toUpperCase() + primaryGroup.substring(1);
            }
        }
        
        if (player.hasPermission("discordplus.admin")) {
            return "Admin";
        } else if (player.hasPermission("discordplus.moderator")) {
            return "Mod";
        } else if (player.hasPermission("discordplus.vip")) {
            return "VIP";
        }
        return null;
    }
    
    private boolean sendWebhookMessage(Player player, String message) {
        String webhookUrl = configManager.getWebhookUrl();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL not configured!");
            return false;
        }
        
        try {
            String playerName = player.getName();
            String playerUuid = player.getUniqueId().toString();
            String avatarUrl = "https://mc-heads.net/avatar/" + playerUuid;
            
            String playerRank = getPlayerRank(player);
            String displayName = (playerRank != null ? "[" + playerRank + "] " : "") + playerName;
            
            String processedMessage = processMessageContent(message);
            
            String jsonPayload = String.format(
                "{\"username\":\"%s\",\"avatar_url\":\"%s\",\"content\":\"%s\"}",
                escapeJson(displayName),
                avatarUrl,
                escapeJson(processedMessage)
            );
            
            return sendWebhookRequest(webhookUrl, jsonPayload);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending webhook message", e);
            return false;
        }
    }
    
    private boolean sendWebhookRequest(String webhookUrl, String jsonPayload) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "DiscordPlus-Webhook/2.0");
            connection.setDoOutput(true);
            
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            } else {
                plugin.getLogger().warning("Webhook request failed with response code: " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending webhook request", e);
            return false;
        }
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    public void sendDiscordMessageToMinecraft(String authorName, String authorId, String message) {
        if (!configManager.isChatBridgeEnabled() || !configManager.isDiscordToMinecraftEnabled()) {
            return;
        }
        
        String formattedMessage = formatDiscordMessage(authorName, authorId, message);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage(formattedMessage);
        });
    }
    
    private String formatDiscordMessage(String authorName, String authorId, String message) {
        return databaseManager.getPlayerByDiscordId(authorId)
                .thenApply(playerData -> {
                    StringBuilder formatted = new StringBuilder();
                    
                    formatted.append("§8[§9Discord§8] ");
                    
                    if (playerData != null && playerData.isLinked()) {
                        String minecraftName = Bukkit.getOfflinePlayer(playerData.getMinecraftUUID()).getName();
                        formatted.append("§b").append(minecraftName);
                    } else {
                        formatted.append("§b").append(authorName);
                    }
                    
                    formatted.append("§8: §f").append(processDiscordMessage(message));
                    
                    return formatted.toString();
                })
                .exceptionally(throwable -> {
                    return String.format("§8[§9Discord§8] §b%s§8: §f%s", 
                        authorName, processDiscordMessage(message));
                })
                .join();
    }
    
    private String processDiscordMessage(String message) {
        message = convertDiscordToMinecraft(message);
        message = processDiscordMentions(message);
        message = filterContent(message);
        return message;
    }
    
    private String convertDiscordToMinecraft(String message) {
        return message
                .replaceAll("\\*\\*([^*]*)\\*\\*", "&l$1&r")     
                .replaceAll("\\*([^*]*)\\*", "&o$1&r")          
                .replaceAll("__([^_]*)__", "&n$1&r")            
                .replaceAll("~~([^~]*)~~", "&m$1&r")            
                .replaceAll("`([^`]*)`", "&7$1&r");             
    }
    
    private String processDiscordMentions(String message) {
        return message.replaceAll("<@!?(\\d+)>", "@$1");
    }
    
    private boolean shouldFilterMessage(String message) {
        if (message.trim().isEmpty()) {
            return true;
        }
        
        if (message.startsWith("/")) {
            return true;
        }
        
        if (!configManager.shouldShowBotMessages() && isAutomatedMessage(message)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isAutomatedMessage(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("[server]") || 
               lowerMessage.contains("[system]") ||
               lowerMessage.contains("[bot]");
    }
    
    private String filterContent(String message) {
        if (message.length() > 256) {
            message = message.substring(0, 253) + "...";
        }
        
        return message;
    }

    public static class ChatStats {
        private int minecraftMessages = 0;
        private int discordMessages = 0;
        private int totalUsers = 0;
        
        public int getMinecraftMessages() { return minecraftMessages; }
        public int getDiscordMessages() { return discordMessages; }
        public int getTotalUsers() { return totalUsers; }
        public int getTotalMessages() { return minecraftMessages + discordMessages; }
    }
}
