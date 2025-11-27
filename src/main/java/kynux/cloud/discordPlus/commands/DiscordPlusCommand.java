package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.gui.AdminPanelGui;
import kynux.cloud.discordPlus.gui.MainGui;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class DiscordPlusCommand implements CommandExecutor {

    private final DiscordPlus plugin;

    public DiscordPlusCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                
                new MainGui(plugin, (Player) sender);
            } else {
                
                showHelpMenu(sender);
            }
            return true;
        }

        
        if (!sender.hasPermission("discordplus.admin")) {
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.NO_PERMISSION));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "status":
                showStatus(sender);
                break;
            case "info":
                showInfo(sender);
                break;
            case "complete-config":
            case "config":
                handleConfigComplete(sender);
                break;
            case "validate-messages":
            case "validate":
                handleValidateMessages(sender);
                break;
            default:
                sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INVALID_ARGS));
                break;
        }
        return true;
    }

    private void showHelpMenu(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_HELP));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_HELP_RELOAD));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_HELP_STATUS));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_HELP_INFO));
        sender.sendMessage("§6/discordplus config §7- Config otomatik tamamlama");
        sender.sendMessage("§6/discordplus validate §7- Discord mesajlarını doğrula");
        sender.sendMessage("");
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_RELOADED));
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.ERROR_COMMAND_FAILED));
            plugin.getLogger().severe("Reload failed: " + e.getMessage());
        }
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_TITLE));
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_VERSION, "version", version));
        long uptime = System.currentTimeMillis() - plugin.getStartTime();
        String uptimeStr = kynux.cloud.discordPlus.utils.TimeUtil.formatDuration(uptime);
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_UPTIME, "uptime", uptimeStr));
        String discordStatus = plugin.isDiscordReady() ?
                MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_CONNECTED) :
                MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_DISCONNECTED);
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_DISCORD, "status", discordStatus));
        String databaseStatus = plugin.getDatabaseManager().isConnected() ?
                MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_CONNECTED) :
                MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_DISCONNECTED);
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_DATABASE, "status", databaseStatus));
        try {
            int linkedPlayers = plugin.getDatabaseManager().getTotalLinkedPlayers().join();
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_PLAYERS, "count", String.valueOf(linkedPlayers)));
        } catch (Exception e) {
             sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_STATUS_PLAYERS, "count", "0"));
        }
    }

    private void showInfo(CommandSender sender) {
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_TITLE));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_DEVELOPER, "developer", "Kynux Cloud"));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_WEBSITE, "website", "https://kynux.cloud"));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_VERSION, "version", plugin.getDescription().getVersion()));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_DESCRIPTION, "description", "Advanced Discord and Minecraft integration"));
        sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.COMMANDS_DISCORDPLUS_INFO_LANGUAGE, "language", MessageUtil.getCurrentLanguage().toUpperCase()));
    }

    private void handleConfigComplete(CommandSender sender) {
        try {
            sender.sendMessage("§e[DiscordPlus] Config otomatik tamamlama başlatılıyor...");
            plugin.getConfigManager().autoCompleteConfig();
            sender.sendMessage("§a[DiscordPlus] Config otomatik tamamlama işlemi tamamlandı!");
        } catch (Exception e) {
            sender.sendMessage("§c[DiscordPlus] Config otomatik tamamlama sırasında hata oluştu: " + e.getMessage());
            plugin.getLogger().severe("Config auto-complete failed: " + e.getMessage());
        }
    }

    private void handleValidateMessages(CommandSender sender) {
        try {
            if (!plugin.isDiscordReady()) {
                sender.sendMessage("§c[DiscordPlus] Discord bağlantısı kurulmadı! Lütfen bot'un hazır olmasını bekleyin.");
                return;
            }

            sender.sendMessage("§e[DiscordPlus] Discord mesajları doğrulanıyor...");
            sender.sendMessage("§7Config'deki message ID'ler kontrol ediliyor ve eksik mesajlar yeniden gönderiliyor...");
            
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getDiscordManager().validateAllPersistentMessages();
                    
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a[DiscordPlus] Discord mesaj doğrulaması tamamlandı!");
                        sender.sendMessage("§7Detaylar için server console'unu kontrol edin.");
                    });
                } catch (Exception e) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§c[DiscordPlus] Mesaj doğrulaması sırasında hata oluştu: " + e.getMessage());
                    });
                    plugin.getLogger().severe("Message validation failed: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            sender.sendMessage("§c[DiscordPlus] Mesaj doğrulaması başlatılamadı: " + e.getMessage());
            plugin.getLogger().severe("Failed to start message validation: " + e.getMessage());
        }
    }
}
