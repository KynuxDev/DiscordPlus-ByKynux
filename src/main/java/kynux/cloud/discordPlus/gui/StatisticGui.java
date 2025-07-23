package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.ConfigManager;
import kynux.cloud.discordPlus.utils.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.stream.Collectors;

public class StatisticGui extends GuiManager {

    private final ConfigManager configManager;

    public StatisticGui(DiscordPlus plugin, Player player) {
        super(plugin, plugin.getConfigManager().getStatisticsGuiRows() * 9, plugin.getConfigManager().getStatisticsGuiTitle());
        this.configManager = plugin.getConfigManager();
        open(player);
    }

    @Override
    protected void initializeItems(Player player) {
        if (!configManager.isStatisticsGuiEnabled()) {
            player.closeInventory();
            player.sendMessage(MessageUtil.colorize("&cİstatistik menüsü şu anda devre dışı."));
            return;
        }

        ConfigurationSection itemsSection = configManager.getStatisticsGuiItems();
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
            if (itemConfig == null || !itemConfig.getBoolean("enabled", false)) {
                continue;
            }

            String name = itemConfig.getString("name", "&cİsimsiz Öğe");
            Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
            List<String> lore = itemConfig.getStringList("lore");
            int slot = itemConfig.getInt("slot", -1);

            if (material == null) {
                plugin.getLogger().warning("[StatisticGui] Geçersiz materyal: " + itemConfig.getString("material"));
                material = Material.BARRIER;
            }

            if (slot != -1) {
                
                List<String> processedLore = lore.stream()
                        .map(line -> PlaceholderAPI.setPlaceholders(player, line))
                        .collect(Collectors.toList());

                setItem(slot, material, name, processedLore, this::handleClick);
            }
        }
        
        
        setItem(configManager.getStatisticsGuiRows() * 9 - 5, Material.ARROW, "&c&lGeri", null, e -> new MainGui(plugin, player));
    }

    private void handleClick(InventoryClickEvent event) {
        
        
        event.setCancelled(true);
    }
}