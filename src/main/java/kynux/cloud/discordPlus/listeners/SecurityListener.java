package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.SecurityManager;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class SecurityListener implements Listener {

    private final DiscordPlus plugin;
    private final SecurityManager securityManager;

    public SecurityListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.securityManager = plugin.getSecurityManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (securityManager.isEnabled()) {
            securityManager.freezePlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!securityManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (securityManager.isFrozen(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getZ() != event.getTo().getZ() || 
                event.getFrom().getY() != event.getTo().getY()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtil.colorize("&cGiriş yapmadan komut kullanamazsınız!"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtil.colorize("&cGiriş yapmadan sohbet edemezsiniz!"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            if (checkFrozen((Player) event.getWhoClicked())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (checkFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (checkFrozen((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (checkFrozen((Player) event.getDamager())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean checkFrozen(Player player) {
        return securityManager.isEnabled() && securityManager.isFrozen(player);
    }
}

