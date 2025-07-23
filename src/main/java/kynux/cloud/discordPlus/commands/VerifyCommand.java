package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.managers.DiscordManager;
import kynux.cloud.discordPlus.utils.MessageUtil;
import kynux.cloud.discordPlus.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.logging.Level;

public class VerifyCommand implements CommandExecutor {

    private final DiscordPlus plugin;
    private final Random random = new Random();

    public VerifyCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.PLAYER_ONLY));
            return true;
        }

        Player player = (Player) sender;
        plugin.getLogger().info("[VerifyCommand] " + player.getName() + " is running /verify.");

        if (!player.hasPermission("discordplus.command.verify")) {
            player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.NO_PERMISSION));
            return true;
        }

        if (!plugin.getConfigManager().isAccountLinkingEnabled()) {
            player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.ERROR_COMMAND_FAILED));
            return true;
        }

        DiscordManager.BotStatus botStatus = plugin.getDiscordManager().getStatus();
        plugin.getLogger().info("[VerifyCommand] Current Bot Status: " + botStatus);

        switch (botStatus) {
            case STARTING:
                player.sendMessage(MessageUtil.colorize("&eDiscord botu başlatılıyor, lütfen birkaç saniye sonra tekrar deneyin."));
                return true;
            case FAILED:
                player.sendMessage(MessageUtil.colorize("&cDiscord botu başlatılamadı. Lütfen konsol günlüklerini kontrol edin veya bir yetkiliye bildirin."));
                return true;
            case READY:
                plugin.getLogger().info("[VerifyCommand] Bot is READY. Proceeding with database operations for " + player.getName());
                break;
        }

        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                .thenAccept(playerData -> {
                    plugin.getLogger().info("[VerifyCommand] Player data loaded for " + player.getName() + ". Handling link command.");
                    handleLinkCommand(player, playerData);
                })
                .exceptionally(throwable -> {
                    player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.ERROR_DATABASE_ERROR));
                    plugin.getLogger().log(Level.SEVERE, "[VerifyCommand] Database error for " + player.getName(), throwable);
                    return null;
                });

        return true;
    }

    private void handleLinkCommand(Player player, PlayerData playerData) {
        if (playerData.isLinked()) {
            plugin.getLogger().info("[VerifyCommand] Player " + player.getName() + " is already linked.");
            player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.ALREADY_LINKED));
            return;
        }

        String linkCode = generateLinkCode();
        plugin.getLogger().info("[VerifyCommand] Generated new link code " + linkCode + " for " + player.getName());

        playerData.setVerificationCode(linkCode);
        playerData.setVerified(false);
        playerData.setVerificationCodeTimestamp(TimeUtil.now()); 

        plugin.getDatabaseManager().savePlayerData(playerData)
                .thenRun(() -> {
                    plugin.getLogger().info("[VerifyCommand] Successfully saved link code for " + player.getName() + ". Sending success message.");
                    sendSuccessMessage(player, linkCode);
                })
                .exceptionally(throwable -> {
                    player.sendMessage(MessageUtil.colorize("&cEşleme kodu oluşturulurken hata oluştu!"));
                    plugin.getLogger().log(Level.SEVERE, "[VerifyCommand] Failed to save link code for " + player.getName(), throwable);
                    return null;
                });
    }

    private String generateLinkCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private void sendSuccessMessage(Player player, String linkCode) {
        player.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtil.colorize("&b&l🔗 Discord Hesap Eşleme"));
        player.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtil.colorize("&7"));
        player.sendMessage(MessageUtil.colorize("&a&l✅ Eşleme kodu oluşturuldu!"));
        player.sendMessage(MessageUtil.colorize("&7"));
        player.sendMessage(MessageUtil.colorize("&f📋 &7Kodunuz: &e&l" + linkCode));
        player.sendMessage(MessageUtil.colorize("&7"));
        player.sendMessage(MessageUtil.colorize("&f🔔 &7Sonraki adımlar:"));
        player.sendMessage(MessageUtil.colorize("&e1. &7Discord sunucusuna gidin"));
        player.sendMessage(MessageUtil.colorize("&e2. &7Eşleme kanalında &b'🔗 Hesap Eşle' &7butonuna basın"));
        player.sendMessage(MessageUtil.colorize("&e3. &7Açılan kutucuğa yukarıdaki kodu girin: &e" + linkCode));
        player.sendMessage(MessageUtil.colorize("&7"));
        player.sendMessage(MessageUtil.colorize("&c⚠️ &7Bu kod 10 dakika geçerlidir!"));
        player.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
}
