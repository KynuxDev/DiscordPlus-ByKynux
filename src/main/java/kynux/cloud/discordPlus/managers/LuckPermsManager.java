package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsManager {

    private final DiscordPlus plugin;
    private LuckPerms luckPerms;

    public LuckPermsManager(DiscordPlus plugin) {
        this.plugin = plugin;
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
            }
        }
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPerms == null) {
            
            return player.hasPermission(permission);
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            
            return user.getCachedData().getPermissionData(QueryOptions.nonContextual()).checkPermission(permission).asBoolean();
        }
        return false;
    }

    public String getPlayerPrimaryGroup(Player player) {
        if (luckPerms == null) {
            return "default";
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getPrimaryGroup();
        }
        return "default";
    }
    
    public boolean isLuckPermsAvailable() {
        return this.luckPerms != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}