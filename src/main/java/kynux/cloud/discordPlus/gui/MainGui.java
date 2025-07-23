package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainGui extends GuiManager {

    public MainGui(DiscordPlus plugin, Player player) {
        super(plugin, 36, "&8▰▱ &3&lD&b&liscord&3&lP&b&llus &8▱▰");
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        setModernBorders();
        
        
        List<String> headerLore = new ArrayList<>();
        headerLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        headerLore.add("&7Modern Discord entegrasyonu ile");
        headerLore.add("&7sunucunuzun tüm özelliklerine erişin.");
        headerLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        headerLore.add("");
        headerLore.add("&8▰ &7Sürüm: &b" + plugin.getDescription().getVersion());
        headerLore.add("&8▰ &7Geliştirici: &bKynux Cloud");
        setItem(4, createEnchantedItem(Material.NETHER_STAR, "&3&l✦ &b&lDiscordPlus &3&l✦", headerLore), null);

        
        if (player.hasPermission("discordplus.admin")) {
            List<String> adminLore = new ArrayList<>();
            adminLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            adminLore.add("&7Plugin'i yönetmek ve yapılandırmak");
            adminLore.add("&7için yönetici panelini açın.");
            adminLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            adminLore.add("");
            adminLore.add("&8▰ &cSadece yöneticiler");
            adminLore.add("&8▰ &7Sistem ayarları");
            setItem(10, createEnchantedItem(Material.COMMAND_BLOCK, "&c&l⚙ &c&lYönetici Paneli", adminLore), this::handleAdminPanelClick);
        }

        
        List<String> accountLore = new ArrayList<>();
        accountLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        accountLore.add("&7Discord hesabınızı Minecraft");
        accountLore.add("&7hesabınızla bağlayın ve yönetin.");
        accountLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        accountLore.add("");
        accountLore.add("&8▰ &7Hesap bağlama");
        accountLore.add("&8▰ &7Profil yönetimi");
        setItem(12, createEnchantedItem(Material.COMPASS, "&b&l👤 &b&lHesap Yönetimi", accountLore), this::handleAccountManagementClick);

        
        List<String> rewardsLore = new ArrayList<>();
        rewardsLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        rewardsLore.add("&7Günlük ve haftalık ödüllerinizi");
        rewardsLore.add("&7toplayın ve bonusları keşfedin.");
        rewardsLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        rewardsLore.add("");
        rewardsLore.add("&8▰ &6Günlük ödüller");
        rewardsLore.add("&8▰ &6Haftalık bonuslar");
        setItem(14, createEnchantedItem(Material.DIAMOND, "&6&l💎 &6&lÖdüller", rewardsLore), this::handleRewardsClick);

        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        statsLore.add("&7Sunucu ve kişisel istatistiklerinizi");
        statsLore.add("&7detaylı grafiklerle görüntüleyin.");
        statsLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        statsLore.add("");
        statsLore.add("&8▰ &dOyuncu istatistikleri");
        statsLore.add("&8▰ &dSunucu verileri");
        setItem(16, createEnchantedItem(Material.BOOK, "&d&l📊 &d&lİstatistikler", statsLore), this::handleStatsClick);

        
        List<String> voteLore = new ArrayList<>();
        voteLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        voteLore.add("&7Sunucuya oy vererek özel ödüller");
        voteLore.add("&7kazanın ve topluluğu destekleyin.");
        voteLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        voteLore.add("");
        voteLore.add("&8▰ &aOy verme linkleri");
        voteLore.add("&8▰ &aÖzel ödüller");
        setItem(19, createEnchantedItem(Material.BEACON, "&a&l🗳 &a&lVote Sistemi", voteLore), this::handleVoteClick);

        
        List<String> helpLore = new ArrayList<>();
        helpLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        helpLore.add("&7Komut listesi, rehberler ve");
        helpLore.add("&7sık sorulan sorular için tıklayın.");
        helpLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        helpLore.add("");
        helpLore.add("&8▰ &eKomut rehberi");
        helpLore.add("&8▰ &eDestek talebi");
        setItem(21, createEnchantedItem(Material.ENCHANTED_BOOK, "&e&l❓ &e&lYardım & Destek", helpLore), this::handleHelpClick);

        
        List<String> syncLore = new ArrayList<>();
        syncLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        syncLore.add("&7Rol ve izin senkronizasyonu");
        syncLore.add("&7ayarlarını yönetin.");
        syncLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        syncLore.add("");
        syncLore.add("&8▰ &9Rol senkronizasyonu");
        syncLore.add("&8▰ &9İzin yönetimi");
        setItem(23, createEnchantedItem(Material.ENDER_EYE, "&9&l🔄 &9&lSenkronizasyon", syncLore), this::handleSyncClick);

        
        List<String> closeLore = new ArrayList<>();
        closeLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        closeLore.add("&7Menüyü kapatmak için tıklayın.");
        closeLore.add("&f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        setItem(31, Material.BARRIER, "&c&l✖ &c&lKapat", closeLore, e -> {
            e.getWhoClicked().closeInventory();
            ((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        });
    }

    private void setModernBorders() {
        
        ItemStack darkPane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        
        
        for (int i = 0; i < 9; i++) {
            setItem(i, i == 4 ? null : darkPane, null);
        }
        for (int i = 27; i < 36; i++) {
            setItem(i, i == 31 ? null : darkPane, null);
        }
        
        
        setItem(9, darkPane, null);
        setItem(17, darkPane, null);
        setItem(18, darkPane, null);
        setItem(26, darkPane, null);
        
        
        setItem(1, grayPane, null);
        setItem(7, grayPane, null);
        setItem(28, grayPane, null);
        setItem(34, grayPane, null);
    }

    private ItemStack createEnchantedItem(Material material, String name, List<String> lore) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    
    private void handleAdminPanelClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.0f);
        new AdminPanelGui(plugin, player);
    }
    
    private void handleAccountManagementClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        new AccountGui(plugin, player);
    }
    
    private void handleRewardsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        player.closeInventory();
        player.sendMessage(MessageUtil.colorize("&6&l💎 &eÖdül sistemi yakında aktif olacak!"));
    }
    
    private void handleStatsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        new StatisticGui(plugin, player);
    }
    
    private void handleVoteClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        player.closeInventory();
        player.sendMessage(MessageUtil.colorize("&a&l✦ &eVote sistemi yakında aktif olacak!"));
    }
    
    private void handleHelpClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        player.closeInventory();
        player.sendMessage(MessageUtil.colorize("&e&l📚 &7Yardım menüsü: &b/dplus help"));
    }
    
    private void handleSyncClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        new SyncGui(plugin, player);
    }
}
