package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.gui.StatisticGui;
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatisticCommand implements CommandExecutor {

    private final DiscordPlus plugin;

    public StatisticCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize("&cBu komut sadece oyuncular tarafından kullanılabilir!"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("discordplus.command.stats")) {
            player.sendMessage(MessageUtil.colorize("&cBu komutu kullanma izniniz yok."));
            return true;
        }

        StatisticGui statisticGui = new StatisticGui(plugin, player);
        statisticGui.open(player);

        return true;
    }
}