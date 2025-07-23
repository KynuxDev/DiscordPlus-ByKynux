package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PermissionSyncData;
import kynux.cloud.discordPlus.utils.MessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class PermissionSyncManager {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final DiscordManager discordManager;
    private final LinkingManager linkingManager;
    private final LuckPermsManager luckPermsManager;
    private final Logger logger;
    
    private final Map<String, PermissionSyncData> roleMappings = new ConcurrentHashMap<>();
    
    private final Map<String, Set<String>> playerRoleCache = new ConcurrentHashMap<>();
    
    private BukkitRunnable syncTask;
    private boolean enabled;
    
    public PermissionSyncManager(DiscordPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.discordManager = plugin.getDiscordManager();
        this.linkingManager = plugin.getLinkingManager();
        this.luckPermsManager = plugin.getLuckPermsManager();
        this.logger = plugin.getLogger();
        
        initialize();
    }

    

    private void initialize() {
        this.enabled = configManager.isPermissionSyncEnabled();
        
        if (!enabled) {
            logger.info("Permission-Role Sync sistemi devre dışı.");
            return;
        }
        
        loadRoleMappings();
        startSyncTask();
        
        logger.info("Permission-Role Sync sistemi başlatıldı!");
    }

    private void loadRoleMappings() {
        roleMappings.clear();
        
        ConfigurationSection mappings = configManager.getRoleMappings();
        if (mappings == null) return;
        
        for (String permission : mappings.getKeys(false)) {
            ConfigurationSection section = mappings.getConfigurationSection(permission);
            if (section == null) continue;
            
            String roleId = section.getString("role-id");
            String roleName = section.getString("name");
            int priority = section.getInt("priority", 0);
            
            if (roleId != null && roleName != null) {
                roleMappings.put(permission, new PermissionSyncData(permission, roleId, roleName, priority));
                logger.info("Rol eşleştirmesi yüklendi: " + permission + " -> " + roleName + " (" + roleId + ")");
            }
        }
        
        logger.info("Toplam " + roleMappings.size() + " rol eşleştirmesi yüklendi.");
    }

    private void startSyncTask() {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
        
        int interval = configManager.getPermissionCheckInterval();
        long intervalTicks = interval * 20L;
        
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                syncAllPlayers();
            }
        };
        
        syncTask.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
        logger.info("Permission sync task başlatıldı. Kontrol aralığı: " + interval + " saniye");
    }

    public void syncAllPlayers() {
        if (!enabled || discordManager.getJDA() == null) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncPlayer(player);
        }
    }
    

    public CompletableFuture<Void> syncPlayer(Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!enabled || discordManager.getJDA() == null) {
            future.complete(null);
            return future;
        }

        String playerId = player.getUniqueId().toString();
        String discordId = linkingManager.getDiscordId(playerId);

        if (discordId == null) {
            future.complete(null);
            return future;
        }

        Guild guild = discordManager.getJDA().getGuildById(configManager.getGuildId());
        if (guild == null) {
            future.complete(null);
            return future;
        }

        logger.info("[Sync Debug] Oyuncu için senkronizasyon başlatılıyor: " + player.getName() + " (Discord ID: " + discordId + ")");
        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null) {
                logger.warning("[Sync Debug] Discord üyesi bulunamadı: " + discordId);
                future.complete(null);
                return;
            }

            Set<String> playerPermissions = getPlayerPermissions(player);
            Set<String> requiredRoles = new HashSet<>();
            boolean singleRoleMode = configManager.isSingleRoleModeEnabled();

            if (singleRoleMode) {
                PermissionSyncData highestPriorityData = null;
                for (String permission : playerPermissions) {
                    PermissionSyncData data = roleMappings.get(permission);
                    if (data != null && (highestPriorityData == null || data.getPriority() > highestPriorityData.getPriority())) {
                        highestPriorityData = data;
                    }
                }
                if (highestPriorityData != null) {
                    requiredRoles.add(highestPriorityData.getRoleId());
                }
            } else {
                for (String permission : playerPermissions) {
                    PermissionSyncData data = roleMappings.get(permission);
                    if (data != null) {
                        requiredRoles.add(data.getRoleId());
                    }
                }
            }

            Set<String> memberRoleIds = new HashSet<>();
            for (Role role : member.getRoles()) {
                memberRoleIds.add(role.getId());
            }

            Set<String> rolesToAdd = new HashSet<>(requiredRoles);
            rolesToAdd.removeAll(memberRoleIds);

            Set<String> rolesToRemove = new HashSet<>();
            if (configManager.isAutoCleanupEnabled()) {
                for (String roleId : memberRoleIds) {
                    if (roleMappings.values().stream().anyMatch(data -> data.getRoleId().equals(roleId)) &&
                            !requiredRoles.contains(roleId)) {
                        rolesToRemove.add(roleId);
                    }
                }
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String roleId : rolesToAdd) {
                futures.add(addRoleToPlayer(guild, member, roleId, player));
            }

            for (String roleId : rolesToRemove) {
                futures.add(removeRoleFromPlayer(guild, member, roleId, player));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                playerRoleCache.put(playerId, requiredRoles);
                future.complete(null);
            });

        }, failure -> future.complete(null));

        return future;
    }

    private CompletableFuture<Void> addRoleToPlayer(Guild guild, Member member, String roleId, Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            future.complete(null);
            return future;
        }

        guild.addRoleToMember(member, role).queue(
                success -> {
                    String roleName = getRoleNameById(roleId);
                    logger.info("Rol verildi: " + member.getUser().getAsTag() + " -> " + roleName);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String message = MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_ROLE_ADDED, "role", roleName);
                        player.sendMessage(message);
                    });

                    if (configManager.isDmNotificationsEnabled() && configManager.isDmOnRoleAddedEnabled()) {
                        sendDMNotification(member.getUser(), "rol-verildi", roleName);
                    }
                    future.complete(null);
                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, (ex) -> {
                            logger.warning("Botun " + role.getName() + " rolünü verme yetkisi yok!");
                            future.complete(null);
                        })
                        .handle(ErrorResponse.UNKNOWN_ROLE, (ex) -> {
                            logger.warning("Rol bulunamadı: " + roleId);
                            future.complete(null);
                        })
        );
        return future;
    }

    private CompletableFuture<Void> removeRoleFromPlayer(Guild guild, Member member, String roleId, Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            future.complete(null);
            return future;
        }

        guild.removeRoleFromMember(member, role).queue(
                success -> {
                    String roleName = getRoleNameById(roleId);
                    logger.info("Rol kaldırıldı: " + member.getUser().getAsTag() + " -> " + roleName);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String message = MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_ROLE_REMOVED, "role", roleName);
                        player.sendMessage(message);
                    });

                    if (configManager.isDmNotificationsEnabled() && configManager.isDmOnRoleRemovedEnabled()) {
                        sendDMNotification(member.getUser(), "rol-kaldirildi", roleName);
                    }
                    future.complete(null);
                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, (ex) -> {
                            logger.warning("Botun " + role.getName() + " rolünü kaldırma yetkisi yok!");
                            future.complete(null);
                        })
                        .handle(ErrorResponse.UNKNOWN_ROLE, (ex) -> {
                            logger.warning("Rol bulunamadı: " + roleId);
                            future.complete(null);
                        })
        );
        return future;
    }


    private void sendDMNotification(User user, String type, String roleName) {
        try {
            user.openPrivateChannel().queue(channel -> {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                        .setTitle("🎭 Rol Değişikliği")
                        .setColor(type.equals("rol-verildi") ? 0x00FF00 : 0xFF4444)
                        .setDescription(type.equals("rol-verildi") ? 
                            "✅ **" + roleName + "** rolü size verildi!" :
                            "❌ **" + roleName + "** rolü sizden alındı!")
                        .addField("Sunucu", configManager.getBotActivity(), true)
                        .addField("Zaman", "<t:" + (System.currentTimeMillis() / 1000) + ":F>", true)
                        .setThumbnail(configManager.getEmbedThumbnail())
                        .setFooter(configManager.getEmbedFooterText());
                
                channel.sendMessageEmbeds(embed.build()).queue(
                    success -> logger.info("DM bildirimi gönderildi: " + user.getAsTag()),
                    error -> logger.warning("DM gönderilemedi: " + user.getAsTag() + " - " + error.getMessage())
                );
            });
        } catch (Exception e) {
            logger.warning("DM gönderme hatası: " + e.getMessage());
        }
    }

    private Set<String> getPlayerPermissions(Player player) {
        Set<String> permissions = new HashSet<>();

        
        if (luckPermsManager == null) {
            logger.warning("LuckPermsManager is not initialized. Falling back to Bukkit permissions.");
            for (String permission : roleMappings.keySet()) {
                if (player.hasPermission(permission)) {
                    permissions.add(permission);
                }
            }
            return permissions;
        }

        logger.info("[Sync Debug] " + player.getName() + " için LuckPerms yetkileri kontrol ediliyor...");
        
        
        for (String permission : roleMappings.keySet()) {
            
            if (luckPermsManager.hasPermission(player, permission)) {
                logger.info("[Sync Debug] ++ Eşleşen yetki bulundu: " + permission);
                permissions.add(permission);
            } else {
                logger.info("[Sync Debug] -- Yetki yok: " + permission);
            }
        }
        
        if (permissions.isEmpty()) {
            logger.info("[Sync Debug] " + player.getName() + " için eşleşen hiçbir yetki bulunamadı.");
            logger.info("[Sync Debug] Kontrol edilen permission'lar: " + String.join(", ", roleMappings.keySet()));
        } else {
            logger.info("[Sync Debug] " + player.getName() + " için bulunan yetkiler: " + String.join(", ", permissions));
        }
        
        return permissions;
    }

    private String getRoleNameById(String roleId) {
        return roleMappings.values().stream()
                .filter(data -> data.getRoleId().equals(roleId))
                .map(PermissionSyncData::getRoleName)
                .findFirst()
                .orElse("Unknown Role");
    }

    public void syncPlayerManual(Player player) {
        if (!enabled) {
            player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_SYSTEM_DISABLED));
            return;
        }

        String message = MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_SYNC_STARTED);
        player.sendMessage(message);

        syncPlayer(player).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String completedMessage = MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_SYNC_COMPLETED);
                player.sendMessage(completedMessage);
            });
        });
    }


    public Map<String, PermissionSyncData> getRoleMappings() {
        return new HashMap<>(roleMappings);
    }

    public void reload() {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
        
        playerRoleCache.clear();
        initialize();
    }

    public void shutdown() {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
        
        playerRoleCache.clear();
        roleMappings.clear();
        
        logger.info("Permission-Role Sync sistemi kapatıldı.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static PermissionSyncManager getPermissionSyncManager() {
        return DiscordPlus.getPlugin(DiscordPlus.class).getPermissionSyncManager();
    }
}
