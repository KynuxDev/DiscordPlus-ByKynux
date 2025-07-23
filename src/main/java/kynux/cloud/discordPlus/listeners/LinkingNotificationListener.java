package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.gui.LinkingNotificationGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LinkingNotificationListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String inventoryTitle = event.getView().getTitle();
        if (inventoryTitle != null && inventoryTitle.contains("HESAP EŞLEME BAŞARILI")) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                
                player.closeInventory();
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        String inventoryTitle = event.getView().getTitle();
        if (inventoryTitle != null && inventoryTitle.contains("HESAP EŞLEME BAŞARILI")) {
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (player.getOpenInventory() != null) {
            String inventoryTitle = player.getOpenInventory().getTitle();
            if (inventoryTitle != null && inventoryTitle.contains("HESAP EŞLEME BAŞARILI")) {
            }
        }
    }
}
