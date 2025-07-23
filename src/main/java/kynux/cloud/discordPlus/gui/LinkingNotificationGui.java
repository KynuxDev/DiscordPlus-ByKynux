package kynux.cloud.discordPlus.gui;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;


public class LinkingNotificationGui {
    
    private final DiscordPlus plugin;
    private final Player player;
    private final User discordUser;
    private Inventory gui;
    private BukkitRunnable animationTask;
    private int animationFrame = 0;
    
    public LinkingNotificationGui(DiscordPlus plugin, Player player, User discordUser) {
        this.plugin = plugin;
        this.player = player;
        this.discordUser = discordUser;
        this.createGui();
    }
    
    private void createGui() {
        this.gui = Bukkit.createInventory(null, 27, 
            MessageUtil.colorize("&a&l✅ &6&lHESAP EŞLEME BAŞARILI &a&l✅"));
        
        fillBackground();
        setMainContent();
        playSuccessEffects();
        startAnimation();
        scheduleAutoClose();
    }
    
    private void fillBackground() {
        
        ItemStack greenGlass = createItem(Material.LIME_STAINED_GLASS_PANE, 
            "&a", Arrays.asList());
        
        
        ItemStack yellowGlass = createItem(Material.YELLOW_STAINED_GLASS_PANE, 
            "&e", Arrays.asList());
        
        
        for (int i = 0; i < 27; i++) {
            if (i == 4 || i == 13 || i == 22) {
                
                continue;
            } else if (i % 2 == 0) {
                gui.setItem(i, greenGlass);
            } else {
                gui.setItem(i, yellowGlass);
            }
        }
    }
    
    private void setMainContent() {
        
        ItemStack successIcon = createItem(Material.EMERALD, 
            "&a&l✅ BAŞARILI EŞLEME", 
            Arrays.asList(
                "&7",
                "&aHesabınız başarıyla Discord ile eşlendi!",
                "&7",
                "&e⚡ &7Artık tüm özel özelliklerden",
                "&7yararlanabilirsiniz!",
                "&7"
            ));
        gui.setItem(4, successIcon);
        
        
        ItemStack discordInfo = createDiscordUserHead();
        gui.setItem(13, discordInfo);
        
        
        ItemStack rewardIcon = createItem(Material.CHEST, 
            "&6&l🎁 ÖZELLİKLER AKTİF", 
            Arrays.asList(
                "&7",
                "&e• &aÖzel Discord rolleri",
                "&e• &aGelişmiş sohbet özellikleri", 
                "&e• &aÖzel komutlar ve yetkiler",
                "&e• &aVe daha fazlası...",
                "&7"
            ));
        gui.setItem(22, rewardIcon);
    }
    
    private ItemStack createDiscordUserHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&9&l💬 DISCORD HESABI"));
            
            List<String> lore = Arrays.asList(
                "&7",
                "&7Discord Kullanıcı: &b" + discordUser.getAsTag(),
                "&7Discord ID: &b" + discordUser.getId(),
                "&7",
                "&aEşleme başarıyla tamamlandı!",
                "&7"
            );
            meta.setLore(MessageUtil.colorizeList(lore));
            
            
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            if (!lore.isEmpty()) {
                meta.setLore(MessageUtil.colorizeList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void playSuccessEffects() {
        
        player.sendTitle(
            MessageUtil.colorize("&a&l✅ BAŞARILI!"),
            MessageUtil.colorize("&7Discord hesabınız eşlendi"),
            20, 60, 20
        );
        
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        });
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }, 10L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }, 20L);
        
        
        spawnParticleEffects();
    }
    
    private void spawnParticleEffects() {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) { 
                    cancel();
                    return;
                }
                
                
                if (ticks % 5 == 0) {
                    player.spawnParticle(Particle.VILLAGER_HAPPY, 
                        player.getLocation().add(0, 2, 0), 10, 1, 1, 1, 0.1);
                }
                
                
                if (ticks % 10 == 0) {
                    player.spawnParticle(Particle.CRIT_MAGIC, 
                        player.getLocation().add(0, 1.5, 0), 15, 0.5, 0.5, 0.5, 0.2);
                }
                
                
                if (ticks % 15 == 0) {
                    player.spawnParticle(Particle.ENCHANTMENT_TABLE, 
                        player.getLocation().add(0, 1, 0), 20, 1, 1, 1, 1);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startAnimation() {
        this.animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gui.getViewers().contains(player)) {
                    cancel();
                    return;
                }
                
                animateGUI();
                animationFrame++;
                
                if (animationFrame >= 40) { 
                    animationFrame = 0;
                }
            }
        };
        animationTask.runTaskTimer(plugin, 0L, 2L); 
    }
    
    private void animateGUI() {
        
        Material[] glassTypes = {
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE
        };
        
        Material currentGlass = glassTypes[animationFrame % glassTypes.length];
        
        
        int[] animatedSlots = {0, 2, 6, 8, 18, 20, 24, 26};
        
        for (int slot : animatedSlots) {
            ItemStack animatedItem = createItem(currentGlass, "&e", Arrays.asList());
            gui.setItem(slot, animatedItem);
        }
    }
    
    private void scheduleAutoClose() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && gui.getViewers().contains(player)) {
                player.closeInventory();
                
                
                player.sendMessage(MessageUtil.colorize("&7"));
                player.sendMessage(MessageUtil.colorize("&a&l✅ &7Hesap eşleme işlemi tamamlandı!"));
                player.sendMessage(MessageUtil.colorize("&7Discord: &b" + discordUser.getAsTag()));
                player.sendMessage(MessageUtil.colorize("&e⚡ &7Özel özellikleriniz aktif edildi!"));
                player.sendMessage(MessageUtil.colorize("&7"));
                
                
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
            
            cleanup();
        }, 200L); 
    }
    
    public void show() {
        if (player.isOnline()) {
            player.openInventory(gui);
        }
    }
    
    public void cleanup() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }
    
    public Inventory getGui() {
        return gui;
    }
}
