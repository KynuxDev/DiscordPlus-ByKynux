package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.gui.DiscordInfoGui;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class DiscordCommand implements CommandExecutor {

    private final DiscordPlus plugin;

    public DiscordCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.PLAYER_ONLY));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("discordplus.command.discord")) {
            player.sendMessage(MessageUtil.getMessage(MessageUtil.Messages.NO_PERMISSION));
            return true;
        }

        new DiscordInfoGui(plugin, player);
        return true;
    }
}
