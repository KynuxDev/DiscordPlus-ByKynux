package kynux.cloud.discordPlus.listeners;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.managers.*;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.UUID;

public class DiscordCommandListener extends ListenerAdapter {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final StatisticManager statisticManager;
    private final LinkingManager linkingManager;
    private final VoteManager voteManager;
    private final ChatManager chatManager;

    public DiscordCommandListener(DiscordPlus plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.statisticManager = plugin.getStatisticManager();
        this.linkingManager = plugin.getLinkingManager();
        this.voteManager = plugin.getVoteManager();
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String chatChannelId = configManager.getChatChannelId();
        if (event.getChannel().getId().equals(chatChannelId)) {
            if (configManager.isChatBridgeEnabled() && configManager.isDiscordToMinecraftEnabled()) {
                String authorName = event.getAuthor().getName();
                String authorId = event.getAuthor().getId();
                String message = event.getMessage().getContentDisplay();
                chatManager.sendDiscordMessageToMinecraft(authorName, authorId, message);
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if ("profil".equals(event.getName())) {
            if (configManager.isPlayerProfilesEnabled()) {
                handleProfileCommand(event);
            } else {
                event.replyEmbeds(EmbedBuilder.error("Devre Dışı", "Bu komut şu anda devre dışı.")).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.equals("vote-leaderboard-menu")) {
            if (configManager.isVoteLeaderboardEnabled()) {
                handleVoteLeaderboardMenu(event);
            } else {
                event.replyEmbeds(EmbedBuilder.error("Devre Dışı", "Bu özellik şu anda devre dışı.")).setEphemeral(true).queue();
            }
        } else if (componentId.equals("leaderboard-menu")) {
            if (configManager.isLeaderboardsEnabled()) {
                handleLeaderboardMenu(event);
            } else {
                event.replyEmbeds(EmbedBuilder.error("Devre Dışı", "Bu özellik şu anda devre dışı.")).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify-button")) {
            if (configManager.isAccountLinkingEnabled()) {
                TextInput codeInput = TextInput.create("verification-code", "Doğrulama Kodu", TextInputStyle.SHORT)
                        .setPlaceholder("Minecraft sunucusundan aldığınız kodu girin")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("verification-modal", "Hesap Doğrulama")
                        .addActionRow(codeInput)
                        .build();

                event.replyModal(modal).queue();
            } else {
                 event.replyEmbeds(EmbedBuilder.error("Devre Dışı", "Hesap eşleme özelliği şu anda devre dışı.")).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("verification-modal")) {
            if (configManager.isAccountLinkingEnabled()) {
                String code = event.getValue("verification-code").getAsString();
                User user = event.getUser();

                event.deferReply(true).queue();

                linkingManager.linkAccount(user, code).thenAccept(result -> {
                    switch (result) {
                        case SUCCESS:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.success("Başarılı!", "Hesabınız başarıyla eşleştirildi.")).setEphemeral(true).queue();
                            break;
                        case ALREADY_LINKED:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Zaten Eşleştirilmiş", "Bu Discord hesabı zaten bir Minecraft hesabıyla eşleştirilmiş.")).setEphemeral(true).queue();
                            break;
                        case MINECRAFT_ALREADY_LINKED:
                             event.getHook().sendMessageEmbeds(EmbedBuilder.error("Zaten Eşleştirilmiş", "Bu Minecraft hesabı zaten başka bir Discord hesabıyla eşleştirilmiş.")).setEphemeral(true).queue();
                            break;
                        case CODE_EXPIRED:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Kod Süresi Doldu", "Girdiğiniz doğrulama kodunun süresi dolmuş. Lütfen Minecraft sunucusuna girip `/verify` komutu ile yeni bir kod alın.")).setEphemeral(true).queue();
                            break;
                        case CODE_NOT_FOUND:
                             event.getHook().sendMessageEmbeds(EmbedBuilder.error("Kod Bulunamadı", "Girdiğiniz doğrulama kodu bulunamadı. Lütfen Minecraft sunucusuna girip `/verify` komutu ile yeni bir kod alın.")).setEphemeral(true).queue();
                            break;
                        case INVALID_CODE:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Geçersiz Kod", "Girdiğiniz doğrulama kodu yanlış. Lütfen kontrol edip tekrar deneyin.")).setEphemeral(true).queue();
                            break;
                        case RATE_LIMITED:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Çok Fazla Deneme", "Çok fazla hatalı deneme yaptınız. Lütfen birkaç dakika sonra tekrar deneyin.")).setEphemeral(true).queue();
                            break;
                        case MAX_ATTEMPTS:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Maksimum Deneme Aşıldı", "Maksimum doğrulama denemesi sayısına ulaştınız. Lütfen daha sonra tekrar deneyin.")).setEphemeral(true).queue();
                            break;
                        case LINKING_FAILED:
                        case DATABASE_ERROR:
                        case SYSTEM_ERROR:
                        default:
                            event.getHook().sendMessageEmbeds(EmbedBuilder.error("Bilinmeyen Hata", "Hesap eşleştirilirken bir hata oluştu. Lütfen daha sonra tekrar deneyin veya bir yetkiliye bildirin.")).setEphemeral(true).queue();
                            break;
                    }
                });
            }
        }
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        event.deferReply().queue();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID playerUUID = databaseManager.getPlayerUUIDByDiscordId(user.getId());
                if (playerUUID == null) {
                    event.getHook().sendMessageEmbeds(EmbedBuilder.error("Hesap Bulunamadı", "Bu Discord hesabı ile bağlantılı bir Minecraft hesabı bulunamadı.")).queue();
                    return;
                }

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                PlayerData playerData = statisticManager.getPlayerData(playerUUID);
                double balance = plugin.getEconomy() != null ? plugin.getEconomy().getBalance(offlinePlayer) : 0.0;

                
                String playerName = offlinePlayer.getName();
                if (playerName != null) {
                    
                    java.util.List<String> categories = configManager.getProfileRankCategories();
                    
                    
                    java.util.Map<String, kynux.cloud.discordPlus.managers.StatisticManager.PlayerRankInfo> rankings = 
                        statisticManager.getPlayerRankings(playerName, categories);

                    
                    if (configManager.isProfileFieldEnabled("leaderboard-ranks") || 
                        !configManager.getProfileEmbedTitle().equals("{player_name} Profili") ||
                        !configManager.getProfileEmbedColor().equals("#5865F2")) {
                        
                        event.getHook().sendMessageEmbeds(
                            EmbedBuilder.enhancedPlayerProfile(user, offlinePlayer, playerData, balance, rankings)
                        ).queue();
                    } else {
                        
                        event.getHook().sendMessageEmbeds(
                            EmbedBuilder.playerProfile(user, offlinePlayer, playerData, balance)
                        ).queue();
                    }
                } else {
                    
                    event.getHook().sendMessageEmbeds(
                        EmbedBuilder.playerProfile(user, offlinePlayer, playerData, balance)
                    ).queue();
                }

            } catch (Exception e) {
                event.getHook().sendMessageEmbeds(EmbedBuilder.error("Bilinmeyen Hata", "Profil bilgileri alınırken bir hata oluştu.")).queue();
                plugin.getLogger().severe("Error in profile command: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleVoteLeaderboardMenu(StringSelectInteractionEvent event) {
        String period = event.getValues().get(0);
        event.deferEdit().queue();

        voteManager.getVotersList(period, 20).thenAccept(voters -> {
            event.getHook().editOriginalEmbeds(EmbedBuilder.votersListEmbed(period, voters)).queue();
        }).exceptionally(ex -> {
            event.getHook().editOriginalEmbeds(EmbedBuilder.error("API Hatası", "Oy listesi alınamadı.")).queue();
            return null;
        });
    }

    private void handleLeaderboardMenu(StringSelectInteractionEvent event) {
        String category = event.getValues().get(0);
        event.deferReply(true).queue(); 

        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            net.dv8tion.jda.api.entities.MessageEmbed embed = statisticManager.createLeaderboardEmbed(category);
            event.getHook().sendMessageEmbeds(embed).queue();
        });
    }
}
