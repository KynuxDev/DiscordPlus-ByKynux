package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.PermissionSyncManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LuckPermsListener {

    private final DiscordPlus plugin;

    public LuckPermsListener(DiscordPlus plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        
        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = event.getUser();
        Player player = Bukkit.getPlayer(user.getUniqueId());

        if (player != null && player.isOnline()) {
             PermissionSyncManager syncManager = plugin.getPermissionSyncManager();
             if (syncManager != null && syncManager.isEnabled()) {
                 syncManager.queuePlayerSync(player);
             }
        }
    }
}

