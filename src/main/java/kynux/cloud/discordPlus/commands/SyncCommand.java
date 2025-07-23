package kynux.cloud.discordPlus.commands;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.gui.SyncGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class SyncCommand implements CommandExecutor {
    
    private final DiscordPlus plugin;
    
    public SyncCommand(DiscordPlus plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cBu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }
        
        Player player = (Player) sender;
        
        new SyncGui(plugin, player).open(player);
        return true;
    }
}
