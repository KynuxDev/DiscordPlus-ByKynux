package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class UnlinkCommand implements CommandExecutor {

    private final DiscordPlus plugin;

    public UnlinkCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(MessageUtil.getMessage("unlink.usage"));
            return true;
        }

        if (!sender.hasPermission("discordplus.admin")) {
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.NO_PERMISSION));
            return true;
        }

        if (!plugin.getConfigManager().isAccountLinkingEnabled()) {
            sender.sendMessage(MessageUtil.getMessage("unlink.system-disabled"));
            return true;
        }

        String targetUsername = args[0];
        plugin.getLogger().info("[UnlinkCommand] " + sender.getName() + " is attempting to unlink: " + targetUsername);

        plugin.getDatabaseManager().getPlayerByUsername(targetUsername)
                .thenAccept(playerData -> {
                    if (playerData == null) {
                        sender.sendMessage(MessageUtil.getMessage("unlink.player-not-found")
                                .replace("{player}", targetUsername));
                        return;
                    }

                    if (!playerData.isLinked()) {
                        sender.sendMessage(MessageUtil.getMessage("unlink.not-linked")
                                .replace("{player}", targetUsername));
                        return;
                    }

                    handleUnlinkProcess(sender, targetUsername, playerData);
                })
                .exceptionally(throwable -> {
                    sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.ERROR_DATABASE_ERROR));
                    plugin.getLogger().log(Level.SEVERE, "[UnlinkCommand] Database error while processing unlink for " + targetUsername, throwable);
                    return null;
                });

        return true;
    }

    private void handleUnlinkProcess(CommandSender sender, String targetUsername, PlayerData playerData) {
        String discordId = playerData.getDiscordId();
        String discordTag = "Bilinmeyen#0000";
        
        try {
            if (plugin.isDiscordReady() && discordId != null) {
                net.dv8tion.jda.api.entities.User discordUser = plugin.getDiscordManager().getJDA().getUserById(discordId);
                if (discordUser != null) {
                    discordTag = discordUser.getAsTag();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not fetch Discord user info for ID: " + discordId, e);
        }

        // Create final variables for lambda expressions
        final String finalDiscordTag = discordTag;
        final String finalDiscordId = discordId;

        plugin.getLinkingManager().unlinkAccountByUsername(targetUsername)
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(MessageUtil.getMessage("unlink.success")
                                .replace("{player}", targetUsername)
                                .replace("{discord_tag}", finalDiscordTag)
                                .replace("{discord_id}", finalDiscordId != null ? finalDiscordId : "Bilinmeyen"));
                        
                        plugin.getLogger().info("[UnlinkCommand] Successfully unlinked " + targetUsername + " (Discord: " + finalDiscordTag + ")");
                        
                        sendUnlinkNotificationToChannel(targetUsername, finalDiscordTag, finalDiscordId, sender.getName());
                    } else {
                        sender.sendMessage(MessageUtil.getMessage("unlink.failed")
                                .replace("{player}", targetUsername));
                        plugin.getLogger().warning("[UnlinkCommand] Failed to unlink " + targetUsername);
                    }
                })
                .exceptionally(throwable -> {
                    sender.sendMessage(MessageUtil.getMessage("unlink.error"));
                    plugin.getLogger().log(Level.SEVERE, "[UnlinkCommand] Error during unlink process for " + targetUsername, throwable);
                    return null;
                });
    }

    private void sendUnlinkNotificationToChannel(String minecraftUsername, String discordTag, String discordId, String adminName) {
        String channelId = plugin.getConfigManager().getLinkingLogChannelId();
        if (channelId == null || channelId.isEmpty() || channelId.equals("LOG_CHANNEL_ID_BURAYA")) {
            return;
        }

        try {
            String embedDescription = String.format(
                "**Minecraft:** `%s`\n**Discord:** `%s` (%s)\n**Admin:** `%s`",
                minecraftUsername,
                discordTag,
                discordId != null ? discordId : "Bilinmeyen",
                adminName
            );

            net.dv8tion.jda.api.entities.MessageEmbed embed = kynux.cloud.discordPlus.utils.EmbedBuilder.warning(
                "Hesap Bağlantısı Kesildi",
                embedDescription
            );

            plugin.getDiscordManager().sendEmbed(channelId, embed)
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Discord log kanalına unlink bildirimi gönderilemedi: " + throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            plugin.getLogger().warning("Discord unlink bildirimi gönderilirken hata: " + e.getMessage());
        }
    }
}