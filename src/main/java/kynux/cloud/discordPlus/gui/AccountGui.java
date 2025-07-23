package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AccountGui extends GuiManager {

    public AccountGui(DiscordPlus plugin, Player player) {
        super(plugin, 27, "&b&lHesap Yönetimi");
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        fillEmptySlots(Material.CYAN_STAINED_GLASS_PANE);

        
        List<String> discordLore = new ArrayList<>();
        discordLore.add("&7Sunucumuzun Discord'una katıl.");
        setItem(11, Material.ENDER_PEARL, "&3&lDiscord Sunucusuna Katıl", discordLore, this::handleDiscordLinkClick);

        
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            if (playerData != null && playerData.isLinked()) {
                List<String> linkedLore = new ArrayList<>();
                linkedLore.add("&aHesabınız zaten bağlı.");
                setItem(15, Material.EMERALD_BLOCK, "&a&lHesap Eşlenmiş", linkedLore, e -> e.getWhoClicked().closeInventory());
            } else {
                List<String> linkLore = new ArrayList<>();
                linkLore.add("&eHesabınızı bağlamak için tıkla.");
                setItem(15, Material.NAME_TAG, "&e&lHesap Eşleştirme Kodu Al", linkLore, this::handleVerifyClick);
            }
        });

        
        setItem(26, Material.BARRIER, "&c&lAna Menüye Dön", null, e -> new MainGui(plugin, (Player) e.getWhoClicked()));
    }

    private void handleDiscordLinkClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        String discordLink = plugin.getConfigManager().getDiscordInviteLink();
        p.closeInventory();
        p.sendMessage(MessageUtil.colorize("&aDiscord sunucumuza katılmak için tıkla: &b&n" + discordLink));
    }

    private void handleVerifyClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        p.closeInventory();
        p.performCommand("verify");
    }
}
