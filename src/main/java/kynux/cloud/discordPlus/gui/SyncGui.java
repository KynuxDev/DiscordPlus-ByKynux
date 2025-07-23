package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PermissionSyncData;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.managers.PermissionSyncManager;
import kynux.cloud.discordPlus.utils.MessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SyncGui extends GuiManager {

    private final PermissionSyncManager permissionSyncManager;

    public SyncGui(DiscordPlus plugin, Player player) {
        super(plugin, 45, "§8• §bRol Senkronizasyonu §8•");
        this.permissionSyncManager = plugin.getPermissionSyncManager();
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        setDecorativeItems();
        setAccountInfo(player);
        setSyncControl(player);
        setRoleInfo(player);
    }

    private void setDecorativeItems() {
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        ItemStack lightBluePane = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);

        for (int i = 0; i < getInventory().getSize(); i++) {
            if (i < 10 || i > 34 || i % 9 == 0 || i % 9 == 8) {
                setItem(i, grayPane, null);
            }
        }
        setItem(4, lightBluePane, null);
        setItem(13, lightBluePane, null);
        setItem(22, lightBluePane, null);
        setItem(31, lightBluePane, null);
        setItem(40, lightBluePane, null);
    }

    private void setAccountInfo(Player player) {
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            boolean isLinked = playerData != null && playerData.isLinked();
            
            List<String> playerHeadLore = new ArrayList<>();
            playerHeadLore.add("§7Discord Bağlantı Durumu");
            playerHeadLore.add("");
            if(isLinked) {
                playerHeadLore.add("§a✔ Bağlantı Aktif");
                playerHeadLore.add("§7Discord: §f" + getDiscordName(playerData.getDiscordId()));
            } else {
                playerHeadLore.add("§c✖ Bağlantı Yok");
                playerHeadLore.add("§7Hesabınızı bağlamak için §e/verify");
            }
            playerHeadLore.add("");
            ItemStack playerHead = createPlayerHead(player, "§b§l" + player.getName(), playerHeadLore);
            setItem(11, playerHead, null);

            List<String> discordStatusLore = new ArrayList<>();
            if(isLinked) {
                discordStatusLore.add("§7Hesabınız başarıyla bağlandı.");
                discordStatusLore.add("§7Rolleriniz senkronize edilebilir.");
            } else {
                discordStatusLore.add("§7Hesabınız bağlı değil.");
                discordStatusLore.add("§7Lütfen önce hesabınızı bağlayın.");
            }
            ItemStack discordStatus = createItem(isLinked ? Material.DIAMOND : Material.GUNPOWDER,
                    isLinked ? "§a§lDiscord Bağlantısı" : "§c§lDiscord Bağlantısı", discordStatusLore);
            setItem(20, discordStatus, null);
        });
    }

    private void setSyncControl(Player player) {
        boolean isSyncEnabled = permissionSyncManager.isEnabled();
        
        List<String> syncButtonLore = new ArrayList<>();
        syncButtonLore.add("§7Minecraft izinlerinizi kontrol ederek");
        syncButtonLore.add("§7Discord rollerinizi günceller.");
        syncButtonLore.add("");
        syncButtonLore.add(isSyncEnabled ? "§e► Tıklayarak senkronize et" : "§cBu özellik şu an devre dışı.");

        ItemStack syncButton = createItem(Material.EMERALD, "§a§lRolleri Senkronize Et", syncButtonLore);
        setItem(24, syncButton, isSyncEnabled ? this::handleSyncClick : null);
    }

    private void setRoleInfo(Player player) {
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            if (playerData == null || !playerData.isLinked()) {
                List<String> noRolesLore = new ArrayList<>();
                noRolesLore.add("§7Hesabınız bağlı olmadığı için");
                noRolesLore.add("§7rolleriniz görüntülenemiyor.");
                ItemStack noRoles = createItem(Material.BARRIER, "§c§lRol Bilgisi Yok", noRolesLore);
                setItem(15, noRoles, null);
                setItem(33, noRoles, null);
                return;
            }

            Member member = getDiscordMember(playerData.getDiscordId());
            if (member == null) {
                List<String> notOnServerLore = new ArrayList<>();
                notOnServerLore.add("§7Discord sunucusunda olmadığınız için");
                notOnServerLore.add("§7rolleriniz görüntülenemiyor.");
                ItemStack notOnServer = createItem(Material.BARRIER, "§c§lDiscord'da Bulunamadı", notOnServerLore);
                setItem(15, notOnServer, null);
                setItem(33, notOnServer, null);
                return;
            }

            Map<String, PermissionSyncData> roleMappings = permissionSyncManager.getRoleMappings();
            
            
            List<String> currentRolesLore = member.getRoles().stream()
                    .filter(role -> roleMappings.values().stream().anyMatch(data -> data.getRoleId().equals(role.getId())))
                    .map(role -> "§a✔ §f" + role.getName())
                    .collect(Collectors.toList());
            if (currentRolesLore.isEmpty()) currentRolesLore.add("§7Senkronize edilmiş rolün��z yok.");
            
            ItemStack currentRolesItem = createItem(Material.CHEST, "§6§lMevcut Discord Rolleri", currentRolesLore);
            setItem(15, currentRolesItem, null);

            
            Set<String> playerPermissions = getPlayerSyncPermissions(player);
            List<String> requiredRolesLore = playerPermissions.stream()
                    .map(permission -> {
                        PermissionSyncData data = roleMappings.get(permission);
                        return data != null ? "§e✧ §f" + data.getRoleName() : null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            if (requiredRolesLore.isEmpty()) requiredRolesLore.add("§7Rol alabileceğiniz izniniz yok.");

            ItemStack requiredRolesItem = createItem(Material.WRITABLE_BOOK, "§d§lAlabileceğin Roller", requiredRolesLore);
            setItem(33, requiredRolesItem, null);
        });
    }

    private void handleSyncClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        p.closeInventory();
        p.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.PERMISSION_SYNC_STARTED));
        
        permissionSyncManager.syncPlayerManual(p);
    }

    
    private ItemStack createPlayerHead(Player owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(MessageUtil.colorize(name));
            meta.setLore(MessageUtil.colorizeList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getDiscordName(String discordId) {
        try {
            Member member = getDiscordMember(discordId);
            return member != null ? member.getEffectiveName() : "Bilinmiyor";
        } catch (Exception e) {
            return "Bilinmiyor";
        }
    }

    private Member getDiscordMember(String discordId) {
        if (!plugin.isDiscordReady()) return null;
        Guild guild = plugin.getDiscordManager().getGuild();
        if (guild == null) return null;
        try {
            return guild.retrieveMemberById(discordId).complete();
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> getPlayerSyncPermissions(Player player) {
        if (!plugin.getLuckPermsManager().isLuckPermsAvailable()) {
            return Collections.emptySet();
        }
        Map<String, PermissionSyncData> roleMappings = permissionSyncManager.getRoleMappings();
        return player.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .filter(roleMappings::containsKey)
                .collect(Collectors.toSet());
    }
}
