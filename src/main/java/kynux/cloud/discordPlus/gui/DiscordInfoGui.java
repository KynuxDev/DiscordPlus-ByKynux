package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class DiscordInfoGui extends GuiManager {

    public DiscordInfoGui(DiscordPlus plugin, Player player) {
        super(plugin, 27, "&9&lDiscord Bilgileri");
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        fillEmptySlots(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        
        List<String> serverLore = new ArrayList<>();
        serverLore.add("&7Sunucu Discord bilgileri.");
        setItem(11, Material.PLAYER_HEAD, "&b&lSunucu Bilgisi", serverLore, null);

        
        String inviteLink = plugin.getConfigManager().getDiscordInviteLink();
        List<String> inviteLore = new ArrayList<>();
        if (inviteLink != null && !inviteLink.isEmpty()) {
            inviteLore.add("&aSunucuya katılmak için tıkla!");
            setItem(13, Material.BOOK, "&a&lDiscord'a Katıl", inviteLore, this::handleInviteClick);
        } else {
            inviteLore.add("&cDavet linki ayarlanmamış.");
            setItem(13, Material.BARRIER, "&c&lDavet Linki Yok", inviteLore, null);
        }

        
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            List<String> accountLore = new ArrayList<>();
            if (playerData != null && playerData.isLinked()) {
                accountLore.add("&aHesabınız bağlı.");
                setItem(15, Material.EMERALD, "&a&lHesap Bağlı", accountLore, null);
            } else {
                accountLore.add("&cHesabınız bağlı değil.");
                setItem(15, Material.REDSTONE, "&c&lHesap Bağlı Değil", accountLore, null);
            }
        });

        
        setItem(26, Material.BARRIER, "&c&lAna Menüye Dön", null, e -> new MainGui(plugin, (Player) e.getWhoClicked()));
    }

    private void handleInviteClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        String discordLink = plugin.getConfigManager().getDiscordInviteLink();
        p.closeInventory();
        p.sendMessage(MessageUtil.colorize("&aDiscord sunucumuza katıl: &f" + discordLink));
    }
}