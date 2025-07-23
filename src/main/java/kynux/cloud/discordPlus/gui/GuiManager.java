package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class GuiManager implements InventoryHolder, Listener {

    protected final DiscordPlus plugin;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> itemActions;

    public GuiManager(DiscordPlus plugin, int size, String title) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.colorize(title));
        this.itemActions = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected abstract void initializeItems(Player player);

    public void open(Player player) {
        initializeItems(player);
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void setItem(int slot, Material material, String name, List<String> lore, Consumer<InventoryClickEvent> action) {
        ItemStack item = createItem(material, name, lore);
        inventory.setItem(slot, item);
        if (action != null) {
            itemActions.put(slot, action);
        }
    }
    
    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            itemActions.put(slot, action);
        }
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            if (lore != null) {
                meta.setLore(MessageUtil.colorizeList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void fillEmptySlots(Material material) {
        ItemStack item = createItem(material, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        Consumer<InventoryClickEvent> action = itemActions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }
}
