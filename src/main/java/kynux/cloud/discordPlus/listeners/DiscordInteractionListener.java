package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.managers.SecurityManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordInteractionListener extends ListenerAdapter {

    private final DiscordPlus plugin;

    public DiscordInteractionListener(DiscordPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        if (buttonId == null) return;

        if (buttonId.startsWith("2fa_")) {
            SecurityManager securityManager = plugin.getSecurityManager();
            if (securityManager != null && securityManager.isEnabled()) {
                event.deferEdit().queue();
                User user = event.getUser();
                securityManager.handleVerification(buttonId, user);
            }
        }
    }
}

