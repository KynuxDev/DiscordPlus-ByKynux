package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.utils.MessageUtil;
import kynux.cloud.discordPlus.utils.TimeUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LinkingManager {
    
    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final DiscordManager discordManager;
    
    private final ConcurrentHashMap<String, LocalDateTime> rateLimitMap;
    
    private final ConcurrentHashMap<String, Integer> attemptCountMap;
    
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int RATE_LIMIT_MINUTES = 2;
    
    public LinkingManager(DiscordPlus plugin, ConfigManager configManager, 
                         DatabaseManager databaseManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.discordManager = discordManager;
        this.rateLimitMap = new ConcurrentHashMap<>();
        this.attemptCountMap = new ConcurrentHashMap<>();
        
        plugin.getLogger().info("LinkingManager initialized with enhanced security features");
    }

    public CompletableFuture<LinkingError> linkAccount(User discordUser, String verificationCode) {
        return CompletableFuture.supplyAsync(() -> {
            if (isRateLimited(discordUser.getId())) {
                return LinkingError.RATE_LIMITED;
            }

            if (isMaxAttemptsReached(discordUser.getId())) {
                return LinkingError.MAX_ATTEMPTS;
            }

            if (!isValidVerificationCode(verificationCode)) {
                incrementAttemptCount(discordUser.getId());
                return LinkingError.INVALID_CODE;
            }

            if (isDiscordUserLinked(discordUser.getId())) {
                return LinkingError.ALREADY_LINKED;
            }

            PlayerData playerData = findPlayerByVerificationCode(verificationCode);
            if (playerData == null) {
                incrementAttemptCount(discordUser.getId());
                return LinkingError.CODE_NOT_FOUND;
            }

            if (isCodeExpired(playerData)) {
                incrementAttemptCount(discordUser.getId());
                return LinkingError.CODE_EXPIRED;
            }

            if (playerData.isLinked()) {
                incrementAttemptCount(discordUser.getId());
                return LinkingError.MINECRAFT_ALREADY_LINKED;
            }

            boolean linkingSuccess = performLinking(playerData, discordUser);
            if (!linkingSuccess) {
                return LinkingError.LINKING_FAILED;
            }

            executePostLinkingActions(playerData, discordUser);
            clearUserLimitData(discordUser.getId());
            return LinkingError.SUCCESS;
        });
    }
    
    public CompletableFuture<LinkingResult> processLinkingAttempt(User discordUser, 
                                                                 String verificationCode, 
                                                                 String minecraftUsername) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isRateLimited(discordUser.getId())) {
                    return LinkingResult.rateLimited(RATE_LIMIT_MINUTES);
                }
                
                if (isMaxAttemptsReached(discordUser.getId())) {
                    return LinkingResult.maxAttemptsReached(MAX_VERIFICATION_ATTEMPTS);
                }
                
                if (!isValidVerificationCode(verificationCode)) {
                    incrementAttemptCount(discordUser.getId());
                    return LinkingResult.invalidCode();
                }
                
                if (isDiscordUserLinked(discordUser.getId())) {
                    return LinkingResult.alreadyLinked();
                }
                
                PlayerData playerData = findPlayerByVerificationCode(verificationCode);
                if (playerData == null) {
                    incrementAttemptCount(discordUser.getId());
                    return LinkingResult.codeNotFound();
                }
                
                if (isCodeExpired(playerData)) {
                    incrementAttemptCount(discordUser.getId());
                    return LinkingResult.codeExpired();
                }
                
                if (minecraftUsername != null && !minecraftUsername.trim().isEmpty()) {
                    if (!validateMinecraftUsername(playerData, minecraftUsername)) {
                        incrementAttemptCount(discordUser.getId());
                        return LinkingResult.usernameMismatch();
                    }
                }
                
                if (playerData.isLinked()) {
                    incrementAttemptCount(discordUser.getId());
                    return LinkingResult.minecraftAlreadyLinked();
                }
                
                boolean linkingSuccess = performLinking(playerData, discordUser);
                if (!linkingSuccess) {
                    return LinkingResult.linkingFailed();
                }
                
                executePostLinkingActions(playerData, discordUser);
                
                clearUserLimitData(discordUser.getId());
                
                return LinkingResult.success(playerData.getMinecraftUUID());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, 
                    "Error processing linking attempt for Discord user: " + discordUser.getId(), e);
                return LinkingResult.systemError();
            }
        });
    }
    
    public CompletableFuture<String> generateVerificationCode(UUID minecraftUUID) {
        return databaseManager.loadPlayerData(minecraftUUID)
            .thenApply(playerData -> {
                String code = String.format("%06d", (int) (Math.random() * 1000000));
                
                playerData.setVerificationCode(code);
                playerData.setVerificationCodeTimestamp(TimeUtil.now());
                databaseManager.savePlayerData(playerData);
                
                plugin.getLogger().info("Generated verification code for player: " + minecraftUUID);
                return code;
            });
    }
    
    public CompletableFuture<Boolean> unlinkAccount(String discordUserId) {
        return databaseManager.getPlayerByDiscordId(discordUserId)
            .thenCompose(playerData -> {
                if (playerData == null) {
                    return CompletableFuture.completedFuture(false);
                }
                
                playerData.setDiscordId(null);
                playerData.setVerified(false);
                playerData.setLinkDate(null);
                playerData.setVerificationCode(null);
                
                return databaseManager.savePlayerData(playerData)
                    .thenApply(v -> {
                        removeLinkedRoles(discordUserId);
                        
                        plugin.getLogger().info("Successfully unlinked account: " + discordUserId);
                        return true;
                    });
            });
    }
    
    private boolean isRateLimited(String discordUserId) {
        LocalDateTime lastAttempt = rateLimitMap.get(discordUserId);
        if (lastAttempt == null) {
            rateLimitMap.put(discordUserId, TimeUtil.now());
            return false;
        }
        
        long minutesSinceLastAttempt = TimeUtil.minutesBetween(lastAttempt, TimeUtil.now());
        if (minutesSinceLastAttempt < RATE_LIMIT_MINUTES) {
            return true;
        }
        
        rateLimitMap.put(discordUserId, TimeUtil.now());
        return false;
    }
    
    private boolean isMaxAttemptsReached(String discordUserId) {
        Integer attempts = attemptCountMap.get(discordUserId);
        return attempts != null && attempts >= MAX_VERIFICATION_ATTEMPTS;
    }
    
    private void incrementAttemptCount(String discordUserId) {
        attemptCountMap.merge(discordUserId, 1, Integer::sum);
    }
    
    private void clearUserLimitData(String discordUserId) {
        rateLimitMap.remove(discordUserId);
        attemptCountMap.remove(discordUserId);
    }
    
    private boolean isValidVerificationCode(String code) {
        return code != null && code.matches("\\d{6}");
    }
    
    private boolean isDiscordUserLinked(String discordUserId) {
        try {
            PlayerData existingLink = databaseManager.getPlayerByDiscordId(discordUserId).join();
            return existingLink != null && existingLink.isLinked();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking Discord user link status", e);
            return false;
        }
    }
    
    private PlayerData findPlayerByVerificationCode(String code) {
        try {
            return databaseManager.getPlayerByVerificationCode(code).join();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding player by verification code", e);
            return null;
        }
    }
    
    private boolean isCodeExpired(PlayerData playerData) {
        LocalDateTime codeTime = playerData.getVerificationCodeTimestamp();
        if (codeTime == null) {
            return true;
        }
        
        long minutesSinceCode = TimeUtil.minutesBetween(codeTime, TimeUtil.now());
        return minutesSinceCode > configManager.getVerificationTimeout();
    }
    
    private boolean validateMinecraftUsername(PlayerData playerData, String inputUsername) {
        Player onlinePlayer = Bukkit.getPlayer(playerData.getMinecraftUUID());
        if (onlinePlayer != null) {
            return onlinePlayer.getName().equalsIgnoreCase(inputUsername.trim());
        }
        
        return true;
    }
    
    private boolean performLinking(PlayerData playerData, User discordUser) {
        try {
            playerData.setDiscordId(discordUser.getId());
            playerData.setVerified(true);
            playerData.setLinkDate(TimeUtil.now());
            playerData.setVerificationCode(null); 
            
            databaseManager.savePlayerData(playerData).join();
            
            plugin.getLogger().info(String.format("Successfully linked accounts - Minecraft: %s, Discord: %s", 
                playerData.getMinecraftUUID(), discordUser.getId()));
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to perform account linking", e);
            return false;
        }
    }
    
    private void executePostLinkingActions(PlayerData playerData, User discordUser) {
        assignLinkedRole(discordUser.getId());
        
        notifyMinecraftPlayer(playerData.getMinecraftUUID(), discordUser);
        
        notifyDiscordChannel(playerData, discordUser);

        sendLinkingLog(playerData, discordUser);
        
        logLinkingEvent(playerData, discordUser);
        
        executeWelcomeLinkingReward(playerData);
    }
    
    private void assignLinkedRole(String discordUserId) {
        if (!configManager.isRoleManagementEnabled()) {
            return;
        }
        
        String linkedRoleId = configManager.getLinkedRoleId();
        if (linkedRoleId.isEmpty()) {
            return;
        }
        
        discordManager.assignRole(discordUserId, linkedRoleId)
            .thenRun(() -> plugin.getLogger().info("Assigned linked role to: " + discordUserId))
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Failed to assign linked role: " + throwable.getMessage());
                return null;
            });
    }
    
    private void removeLinkedRoles(String discordUserId) {
        if (!configManager.isRoleManagementEnabled()) {
            return;
        }
        
        String linkedRoleId = configManager.getLinkedRoleId();
        if (!linkedRoleId.isEmpty()) {
            discordManager.removeRole(discordUserId, linkedRoleId);
        }
    }
    
    private void notifyMinecraftPlayer(UUID minecraftUUID, User discordUser) {
        Player player = Bukkit.getPlayer(minecraftUUID);
        if (player != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                kynux.cloud.discordPlus.gui.LinkingNotificationGui notificationGui =
                    new kynux.cloud.discordPlus.gui.LinkingNotificationGui(plugin, player, discordUser);
                notificationGui.show();
            });
        }
    }
    
    private void notifyDiscordChannel(PlayerData playerData, User discordUser) {
        String channelId = configManager.getLinkingChannelId();
        if (channelId == null || channelId.isEmpty()) {
            plugin.getLogger().warning("Hesap eşleme kanal ID'si ayarlanmamış!");
            return;
        }
        
        try {
            String playerName = "Bilinmeyen Oyuncu";
            org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerData.getMinecraftUUID());
            if (onlinePlayer != null) {
                playerName = onlinePlayer.getName();
            }
            
            String embedDescription = "Yeni bir hesap başarıyla eşlendi!\n\n" +
                                      "🎮 **Minecraft:** " + playerName + "\n" +
                                      "💬 **Discord:** " + discordUser.getAsTag() + "\n" +
                                      "🆔 **Discord ID:** " + discordUser.getId() + "\n" +
                                      "📅 **Eşleme Zamanı:** " + TimeUtil.formatDateTime(TimeUtil.now());
            
            net.dv8tion.jda.api.entities.MessageEmbed embed = kynux.cloud.discordPlus.utils.EmbedBuilder.success(
                "Hesap Eşleme Başarılı!", 
                embedDescription
            );
            
            discordManager.sendEmbed(channelId, embed)
                .thenRun(() -> plugin.getLogger().info("Hesap eşleme bildirimi Discord kanalına gönderildi: " + channelId))
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Discord kanalına hesap eşleme bildirimi gönderilemedi: " + throwable.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            plugin.getLogger().warning("Discord bildirimi gönderilirken hata: " + e.getMessage());
        }
    }

    private void sendLinkingLog(PlayerData playerData, User discordUser) {
        String logChannelId = configManager.getLinkingLogChannelId();
        if (logChannelId == null || logChannelId.isEmpty() || logChannelId.equals("LOG_CHANNEL_ID_BURAYA")) {
            return;
        }

        try {
            String playerName = "Bilinmeyen Oyuncu";
            Player onlinePlayer = Bukkit.getPlayer(playerData.getMinecraftUUID());
            if (onlinePlayer != null) {
                playerName = onlinePlayer.getName();
            }

            String embedDescription = String.format(
                "**Minecraft:** `%s` (%s)\n**Discord:** `%s` (%s)",
                playerName,
                playerData.getMinecraftUUID(),
                discordUser.getAsTag(),
                discordUser.getId()
            );

            net.dv8tion.jda.api.entities.MessageEmbed embed = kynux.cloud.discordPlus.utils.EmbedBuilder.info(
                "Yeni Hesap Eşleştirildi",
                embedDescription
            );

            discordManager.sendEmbed(logChannelId, embed)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Discord log kanalına hesap eşleme bildirimi gönderilemedi: " + throwable.getMessage());
                    return null;
                });

        } catch (Exception e) {
            plugin.getLogger().warning("Discord log bildirimi gönderilirken hata: " + e.getMessage());
        }
    }
    
    private void logLinkingEvent(PlayerData playerData, User discordUser) {
        plugin.getLogger().info(String.format(
            "ACCOUNT_LINKED - Minecraft: %s, Discord: %s (%s), Timestamp: %s",
            playerData.getMinecraftUUID(),
            discordUser.getAsTag(),
            discordUser.getId(),
            TimeUtil.now()
        ));
    }
    
    private void executeWelcomeLinkingReward(PlayerData playerData) {
        if (!configManager.isCustomRewardsEnabled()) {
            return;
        }
        
        plugin.getLogger().info("Welcome linking reward triggered for: " + playerData.getMinecraftUUID());
    }
    
    public boolean isPlayerLinked(UUID minecraftUUID) {
        try {
            PlayerData playerData = databaseManager.loadPlayerData(minecraftUUID).join();
            return playerData.isLinked();
        } catch (Exception e) {
            return false;
        }
    }
    
    public CompletableFuture<PlayerData> getLinkedPlayer(String discordUserId) {
        return databaseManager.getPlayerByDiscordId(discordUserId);
    }

    public String getDiscordId(String minecraftUUID) {
        try {
            PlayerData playerData = databaseManager.loadPlayerData(UUID.fromString(minecraftUUID)).join();
            return playerData != null && playerData.isLinked() ? playerData.getDiscordId() : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting Discord ID for player: " + minecraftUUID, e);
            return null;
        }
    }
    
    public void cleanupExpiredData() {
        LocalDateTime cutoff = TimeUtil.now().minusHours(1);
        
        rateLimitMap.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        if (TimeUtil.now().getHour() == 0) {
            attemptCountMap.clear();
        }
    }
    
    public static class LinkingResult {
        private final boolean success;
        private final String message;
        private final UUID linkedUUID;
        private final LinkingError error;
        
        private LinkingResult(boolean success, String message, UUID linkedUUID, LinkingError error) {
            this.success = success;
            this.message = message;
            this.linkedUUID = linkedUUID;
            this.error = error;
        }
        
        public static LinkingResult success(UUID uuid) {
            return new LinkingResult(true, "Hesap başarıyla eşlendi!", uuid, null);
        }
        
        public static LinkingResult rateLimited(int minutes) {
            return new LinkingResult(false, 
                "Çok hızlı deneme yapıyorsunuz! " + minutes + " dakika bekleyin.", 
                null, LinkingError.RATE_LIMITED);
        }
        
        public static LinkingResult maxAttemptsReached(int maxAttempts) {
            return new LinkingResult(false, 
                "Maksimum deneme sayısına (" + maxAttempts + ") ulaştınız! Daha sonra tekrar deneyin.", 
                null, LinkingError.MAX_ATTEMPTS);
        }
        
        public static LinkingResult invalidCode() {
            return new LinkingResult(false, "Geçersiz doğrulama kodu! 6 haneli sayı giriniz.", null, LinkingError.INVALID_CODE);
        }
        
        public static LinkingResult alreadyLinked() {
            return new LinkingResult(false, "Bu Discord hesabı zaten eşlenmiş!", null, LinkingError.ALREADY_LINKED);
        }
        
        public static LinkingResult codeNotFound() {
            return new LinkingResult(false, "Doğrulama kodu bulunamadı! Minecraft'ta /verify komutunu kullanın.", null, LinkingError.CODE_NOT_FOUND);
        }
        
        public static LinkingResult codeExpired() {
            return new LinkingResult(false, "Doğrulama kodu süresi dolmuş! Yeni kod almak için /verify kullanın.", null, LinkingError.CODE_EXPIRED);
        }
        
        public static LinkingResult usernameMismatch() {
            return new LinkingResult(false, "Kullanıcı adı eşleşmiyor! Doğru Minecraft kullanıcı adını girin.", null, LinkingError.USERNAME_MISMATCH);
        }
        
        public static LinkingResult minecraftAlreadyLinked() {
            return new LinkingResult(false, "Bu Minecraft hesabı zaten başka bir Discord hesabıyla eşlenmiş!", null, LinkingError.MINECRAFT_ALREADY_LINKED);
        }
        
        public static LinkingResult linkingFailed() {
            return new LinkingResult(false, "Eşleme işlemi başarısız! Lütfen daha sonra tekrar deneyin.", null, LinkingError.LINKING_FAILED);
        }
        
        public static LinkingResult systemError() {
            return new LinkingResult(false, "Sistem hatası! Lütfen yöneticiye bildirin.", null, LinkingError.SYSTEM_ERROR);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public UUID getLinkedUUID() { return linkedUUID; }
        public LinkingError getError() { return error; }
    }
    
    public enum LinkingError {
        SUCCESS,
        RATE_LIMITED,
        MAX_ATTEMPTS,
        INVALID_CODE,
        ALREADY_LINKED,
        CODE_NOT_FOUND,
        CODE_EXPIRED,
        USERNAME_MISMATCH,
        MINECRAFT_ALREADY_LINKED,
        LINKING_FAILED,
        DATABASE_ERROR,
        SYSTEM_ERROR
    }
}
