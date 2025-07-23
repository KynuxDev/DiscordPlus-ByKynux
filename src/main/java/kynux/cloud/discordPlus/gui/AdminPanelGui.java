package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelGui extends GuiManager {

    public AdminPanelGui(DiscordPlus plugin, Player player) {
        super(plugin, 27, "&c&lYönetici Paneli");
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        fillEmptySlots(Material.RED_STAINED_GLASS_PANE);

        
        List<String> reloadLore = new ArrayList<>();
        reloadLore.add("&7Plugin'i yeniden yükler.");
        setItem(11, Material.REPEATER, "&a&lPlugin'i Yeniden Yükle", reloadLore, this::handleReloadClick);

        
        List<String> statusLore = new ArrayList<>();
        statusLore.add("&7Plugin'in durumunu kontrol et.");
        setItem(13, Material.COMPARATOR, "&e&lDurumu Kontrol Et", statusLore, this::handleStatusClick);
        
        
        setItem(26, Material.BARRIER, "&c&lAna Menüye Dön", null, e -> new MainGui(plugin, (Player) e.getWhoClicked()));
    }

    private void handleReloadClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        p.closeInventory();
        plugin.reloadPlugin();
        p.sendMessage(MessageUtil.colorize("&aDiscordPlus eklentisi başarıyla yeniden yüklendi!"));
    }

    private void handleStatusClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        p.closeInventory();
        p.performCommand("discordplus status");
    }
}
