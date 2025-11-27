package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.utils.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SecurityManager {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final LinkingManager linkingManager;
    private final DiscordManager discordManager;
    private final Logger logger;

    private final Map<UUID, Long> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    private boolean enabled;
    private int timeoutSeconds;

    public SecurityManager(DiscordPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.linkingManager = plugin.getLinkingManager();
        this.discordManager = plugin.getDiscordManager();
        this.logger = plugin.getLogger();

        initialize();
    }

    private void initialize() {
        this.enabled = configManager.getConfig().getBoolean("security.2fa.enabled", false);
        this.timeoutSeconds = configManager.getConfig().getInt("security.2fa.timeout", 60);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.containsKey(player.getUniqueId());
    }

    public void freezePlayer(Player player) {
        if (!enabled) return;
        
        if (!player.hasPermission("discordplus.2fa.required")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        frozenPlayers.put(uuid, System.currentTimeMillis());

        player.sendTitle(
                MessageUtil.colorize("&c&lGÜVENLİK KİLİDİ"),
                MessageUtil.colorize("&7Lütfen Discord üzerinden girişinizi onaylayın!"),
                10, 20000, 20
        );
        
        String discordId = linkingManager.getDiscordId(uuid.toString());
        if (discordId == null) {
            player.kickPlayer(MessageUtil.colorize("&cHesabınız Discord ile eşleşmemiş!\n&7Yetkili girişleri için Discord eşleşmesi zorunludur."));
            return;
        }

        discordManager.getJDA().retrieveUserById(discordId).queue(user -> {
            sendVerificationDM(user, player);
        }, failure -> {
            player.kickPlayer(MessageUtil.colorize("&cDiscord DM gönderilemedi!\n&7Lütfen DM kutunuzun açık olduğundan emin olun."));
        });

        Runnable timeoutRunnable = () -> {
            if (frozenPlayers.containsKey(uuid)) {
                player.kickPlayer(MessageUtil.colorize("&cGiriş onay süresi doldu!"));
                frozenPlayers.remove(uuid);
                timeoutTasks.remove(uuid);
            }
        };
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, timeoutRunnable, timeoutSeconds * 20L);
        timeoutTasks.put(uuid, task);
    }

    private void sendVerificationDM(User user, Player player) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Güvenlik Uyarısı: Giriş Tespit Edildi")
                .setDescription("Sunucuya **" + player.getName() + "** hesabıyla giriş yapıldı.\n\n" +
                        "**IP Adresi:** " + player.getAddress().getHostString() + "\n" +
                        "**Tarih:** <t:" + (System.currentTimeMillis() / 1000) + ":F>\n\n" +
                        "Bu girişi sen mi yaptın?")
                .setColor(Color.ORANGE)
                .setFooter(plugin.getConfigManager().getEmbedFooterText());

        user.openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.success("2fa_allow:" + player.getUniqueId(), "✅ Evet, Benim"),
                            Button.danger("2fa_deny:" + player.getUniqueId(), "🟥 Hayır, Kickle!")
                    )
                    .queue();
        });
    }

    public void handleVerification(String buttonId, User user) {
        String[] parts = buttonId.split(":");
        if (parts.length != 2) return;

        String action = parts[0];
        String uuidStr = parts[1];
        UUID uuid;
        
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        
        if (action.equals("2fa_allow")) {
            if (frozenPlayers.containsKey(uuid)) {
                unfreezePlayer(uuid);
                user.openPrivateChannel().queue(ch -> ch.sendMessage("✅ Giriş onaylandı! İyi oyunlar.").queue());
                if (player != null) {
                    player.sendMessage(MessageUtil.colorize("&aGiriş başarıyla doğrulandı! İyi oyunlar."));
                    player.sendTitle("", "", 0, 0, 0); 
                }
            } else {
                 user.openPrivateChannel().queue(ch -> ch.sendMessage("⚠️ Bu doğrulama isteğinin süresi dolmuş veya zaten onaylanmış.").queue());
            }
        } else if (action.equals("2fa_deny")) {
            if (player != null) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    player.kickPlayer(MessageUtil.colorize("&c&lGiriş reddedildi!\n&7Hesap sahibi girişi onaylamadı."))
                );
            }
            frozenPlayers.remove(uuid);
            cancelTimeoutTask(uuid);
            
            user.openPrivateChannel().queue(ch -> ch.sendMessage("🛑 Oyuncu sunucudan atıldı! Şifrenizi değiştirmeyi unutmayın.").queue());
            
            logger.warning("YETKİSİZ GİRİŞ DENEMESİ: " + (player != null ? player.getName() : uuidStr) + " reddedildi!");
        }
    }

    public void unfreezePlayer(UUID uuid) {
        frozenPlayers.remove(uuid);
        cancelTimeoutTask(uuid);
    }
    
    private void cancelTimeoutTask(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void shutdown() {
        timeoutTasks.values().forEach(BukkitTask::cancel);
        timeoutTasks.clear();
        frozenPlayers.clear();
    }
}
