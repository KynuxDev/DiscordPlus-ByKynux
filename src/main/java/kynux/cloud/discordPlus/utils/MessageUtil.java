package kynux.cloud.discordPlus.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MessageUtil {

    private static FileConfiguration messages;
    private static String lang;
    private static Logger logger;

    public static void initialize(FileConfiguration config, Logger aLogger, File dataFolder) {
        logger = aLogger;
        lang = config.getString("language", "tr");
        loadMessages(lang, dataFolder);
    }

    public static void loadMessages(String lang, File dataFolder) {
        File langFile = new File(dataFolder, "messages_" + lang + ".yml");
        if (!langFile.exists()) {
            logger.info("messages_" + lang + ".yml not found, creating from resources...");
            try {
                dataFolder.mkdirs();
                
                InputStream in = MessageUtil.class.getResourceAsStream("/messages_" + lang + ".yml");
                if (in == null) {
                    logger.severe("Could not find messages_" + lang + ".yml in the plugin resources!");
                    
                    if (!"en".equals(lang)) {
                        logger.info("Falling back to English (messages_en.yml).");
                        loadMessages("en", dataFolder);
                    }
                    return;
                }
                messages = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                messages.save(langFile);
            } catch (Exception e) {
                logger.severe("Could not create messages_" + lang + ".yml: " + e.getMessage());
                return;
            }
        }
        messages = YamlConfiguration.loadConfiguration(langFile);
        logger.info("Loaded messages from messages_" + lang + ".yml");
    }

    public static String getMessage(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return colorize(message);
    }

    public static String getMessage(Messages message) {
        return getMessage(message.getPath());
    }
    
    public static String getRawMessage(String path) {
        return messages.getString(path, "Message not found: " + path);
    }
    
    public static String getRawMessage(Messages message, String... replacements) {
        String rawMessage = getRawMessage(message.getPath());
        for (int i = 0; i < replacements.length; i += 2) {
            rawMessage = rawMessage.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return rawMessage;
    }

    public static String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return message;
    }

    public static String getMessage(Messages message, String... replacements) {
        return getMessage(message.getPath(), replacements);
    }

    public static List<String> getMessageList(String path) {
        return messages.getStringList(path).stream().map(MessageUtil::colorize).collect(Collectors.toList());
    }
    
    public static List<String> getMessageList(String path, String... replacements) {
        List<String> list = getMessageList(path);
        return list.stream().map(line -> {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
            return line;
        }).collect(Collectors.toList());
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(message));
        }
    }
    
    public static void sendMessage(Player player, String message) {
        if (player != null && player.isOnline() && message != null && !message.isEmpty()) {
            player.sendMessage(colorize(message));
        }
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static List<String> colorizeList(List<String> list) {
        return list.stream().map(MessageUtil::colorize).collect(Collectors.toList());
    }

    public static String getCurrentLanguage() {
        return lang;
    }
    
    public enum Messages {
        PLAYER_ONLY("error.player-only"),
        NO_PERMISSION("error.no-permission"),
        ERROR_COMMAND_FAILED("error.command-failed"),
        ERROR_DATABASE_ERROR("error.database-error"),
        ALREADY_LINKED("linking.already-linked"),
        COMMANDS_DISCORDPLUS_INVALID_ARGS("commands.discordplus.invalid-args"),
        COMMANDS_DISCORDPLUS_HELP("commands.discordplus.help.header"),
        COMMANDS_DISCORDPLUS_HELP_RELOAD("commands.discordplus.help.reload"),
        COMMANDS_DISCORDPLUS_HELP_STATUS("commands.discordplus.help.status"),
        COMMANDS_DISCORDPLUS_HELP_INFO("commands.discordplus.help.info"),
        COMMANDS_DISCORDPLUS_RELOADED("commands.discordplus.reloaded"),
        COMMANDS_DISCORDPLUS_STATUS_TITLE("commands.discordplus.status.title"),
        COMMANDS_DISCORDPLUS_STATUS_VERSION("commands.discordplus.status.version"),
        COMMANDS_DISCORDPLUS_STATUS_UPTIME("commands.discordplus.status.uptime"),
        COMMANDS_DISCORDPLUS_STATUS_DISCORD("commands.discordplus.status.discord"),
        COMMANDS_DISCORDPLUS_STATUS_DATABASE("commands.discordplus.status.database"),
        COMMANDS_DISCORDPLUS_STATUS_PLAYERS("commands.discordplus.status.players"),
        COMMANDS_DISCORDPLUS_CONNECTED("commands.discordplus.connected"),
        COMMANDS_DISCORDPLUS_DISCONNECTED("commands.discordplus.disconnected"),
        COMMANDS_DISCORDPLUS_INFO_TITLE("commands.discordplus.info.title"),
        COMMANDS_DISCORDPLUS_INFO_DEVELOPER("commands.discordplus.info.developer"),
        COMMANDS_DISCORDPLUS_INFO_WEBSITE("commands.discordplus.info.website"),
        COMMANDS_DISCORDPLUS_INFO_VERSION("commands.discordplus.info.version"),
        COMMANDS_DISCORDPLUS_INFO_DESCRIPTION("commands.discordplus.info.description"),
        COMMANDS_DISCORDPLUS_INFO_LANGUAGE("commands.discordplus.info.language"),
        COMMANDS_REWARDS_INVALID_ARGS("commands.rewards.invalid-args"),
        PERMISSION_SYSTEM_DISABLED("permission-sync.disabled"),
        PERMISSION_SYNC_STARTED("permission-sync.started"),
        PERMISSION_SYNC_COMPLETED("permission-sync.completed"),
        PERMISSION_ROLE_ADDED("permission-sync.role-added"),
        PERMISSION_ROLE_REMOVED("permission-sync.role-removed"),
        ACHIEVEMENT_PLAYER_ACHIEVED("notifications.achievement.player-achieved");

        private final String path;

        Messages(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}
