package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.gui.MainGui; 
import kynux.cloud.discordPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordPlusGuiCommand implements CommandExecutor {

    private final DiscordPlus plugin;

    public DiscordPlusGuiCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize("&cBu komut sadece oyuncular tarafından kullanılabilir!"));
            return true;
        }

        Player player = (Player) sender;
        MainGui mainGui = new MainGui(plugin, player); 
        mainGui.open(player); 

        return true;
    }
}
