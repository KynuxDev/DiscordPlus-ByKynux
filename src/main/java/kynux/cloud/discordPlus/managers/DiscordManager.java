package kynux.cloud.discordPlus.managers;

import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.listeners.DiscordCommandListener;
import kynux.cloud.discordPlus.listeners.DiscordInteractionListener;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import kynux.cloud.discordPlus.utils.TimeUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import okhttp3.OkHttpClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DiscordManager {

    public enum BotStatus {
        STARTING,
        READY,
        FAILED
    }

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private JDA jda;
    private Guild guild;
    private volatile BotStatus status = BotStatus.STARTING;

    
    private OkHttpClient httpClient;
    private ScheduledExecutorService activityScheduler;

    
    private int currentActivityIndex = 0;
    private final Map<String, String> placeholderCache = new HashMap<>();
    private long lastPlaceholderUpdate = 0;

    public DiscordManager(DiscordPlus plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        String token = configManager.getDiscordToken();
        if (token.isEmpty() || token.equals("BOT_TOKEN_BURAYA")) {
            plugin.getLogger().severe("Discord bot token is not set in config.yml!");
            this.status = BotStatus.FAILED;
            return;
        }

        try {
            
            this.httpClient = new OkHttpClient.Builder().build();

            this.jda = JDABuilder.createDefault(token)
                    .setHttpClient(httpClient)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordCommandListener(plugin), new DiscordInteractionListener(plugin))
                    .setActivity(Activity.watching(configManager.getBotActivity()))
                    .build();

            plugin.getLogger().info("Discord bot is starting... waiting for connection.");
            this.jda.awaitReady();

            this.guild = jda.getGuildById(configManager.getGuildId());
            if (guild == null) {
                plugin.getLogger().severe("Guild with ID " + configManager.getGuildId() + " not found! Please check your config.yml.");
                shutdown(); 
                this.status = BotStatus.FAILED;
                return;
            }

            guild.updateCommands().addCommands(
                    Commands.slash("profil", "Minecraft oyuncu profilinizi görüntüler.")
            ).queue();

            plugin.getLogger().info("Discord bot started successfully! Connected to Guild: " + guild.getName());
            this.status = BotStatus.READY;

            
            startActivityUpdates();

            Bukkit.getScheduler().runTaskLater(plugin, this::validateAllPersistentMessages, 20L);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start Discord bot! Check your token, guild-id, and internet connection.", e);
            shutdown(); 
            this.status = BotStatus.FAILED;
        }
    }

    public void shutdown() {
        if (activityScheduler != null && !activityScheduler.isShutdown()) {
            activityScheduler.shutdownNow();
            try {
                if (!activityScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                     plugin.getLogger().warning("Activity scheduler did not terminate in time.");
                }
            } catch (InterruptedException e) {
                activityScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (jda != null) {
            jda.shutdown(); 
            try {
                if (!jda.awaitShutdown(5, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("JDA did not shut down in 5 seconds, forcing shutdown.");
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }

        plugin.getLogger().info("Discord services have been shut down.");
    }
    
    private void startActivityUpdates() {
        if (!configManager.isBotActivityDynamic()) {
            
            updateBotActivity(configManager.getBotActivityStaticMessage());
            return;
        }
        
        List<String> messages = configManager.getBotActivityMessages();
        if (messages.isEmpty()) {
            plugin.getLogger().warning("Dynamic bot activity is enabled but no messages found in config!");
            return;
        }
        
        
        updateBotActivity(messages.get(0));
        
        
        if (activityScheduler != null && !activityScheduler.isShutdown()) {
            activityScheduler.shutdownNow();
        }
        
        activityScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "DiscordPlus-Activity-Scheduler"));
        activityScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!isReady()) return; 
                currentActivityIndex = (currentActivityIndex + 1) % messages.size();
                String message = messages.get(currentActivityIndex);
                updateBotActivity(message);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating bot activity: " + e.getMessage());
            }
        }, configManager.getBotActivityUpdateInterval(), configManager.getBotActivityUpdateInterval(), TimeUnit.SECONDS);
        
        plugin.getLogger().info("Dynamic bot activity started with " + messages.size() + " messages, updating every " + configManager.getBotActivityUpdateInterval() + " seconds.");
    }
    
    private void updateBotActivity(String message) {
        if (!isReady()) return;
        
        String processedMessage = message;
        
        
        if (configManager.isBotActivityPlaceholdersEnabled()) {
            processedMessage = replacePlaceholders(processedMessage);
        }
        
        updateNormalActivity(processedMessage);
    }
    
    private void updateNormalActivity(String message) {
        
        Activity activity;
        String activityType = configManager.getBotActivityType().toUpperCase();
        
        switch (activityType) {
            case "PLAYING":
                activity = Activity.playing(message);
                break;
            case "LISTENING":
                activity = Activity.listening(message);
                break;
            case "STREAMING":
                activity = Activity.streaming(message, "https://nexoro.com.tr");
                break;
            case "COMPETING":
                activity = Activity.competing(message);
                break;
            case "WATCHING":
            default:
                activity = Activity.watching(message);
                break;
        }
        
        jda.getPresence().setActivity(activity);
    }
    
    
    private String replacePlaceholders(String text) {
        if (text == null) return "";
        
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlaceholderUpdate > (configManager.getBotActivityPlaceholderCacheDuration() * 1000L)) {
            updatePlaceholderCache();
            lastPlaceholderUpdate = currentTime;
        }
        
        
        for (Map.Entry<String, String> entry : placeholderCache.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return text;
    }
    
    private void updatePlaceholderCache() {
        try {
            
            placeholderCache.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
            
            
            placeholderCache.put("max_players", String.valueOf(Bukkit.getMaxPlayers()));
            
            
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (!onlinePlayers.isEmpty()) {
                List<String> playerNames = onlinePlayers.stream()
                    .map(Player::getName)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                String randomPlayer = playerNames.get(new Random().nextInt(playerNames.size()));
                placeholderCache.put("player", randomPlayer);
            } else {
                placeholderCache.put("player", "Hiç kimse");
            }
            
            
            placeholderCache.put("server_name", configManager.getServerName());
            
            
            placeholderCache.put("tps", getFormattedTPS());
            
            
            long uptime = System.currentTimeMillis() - plugin.getStartTime();
            placeholderCache.put("uptime", TimeUtil.formatDuration(uptime));
            
            
            placeholderCache.put("total_playtime", "1000");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating placeholder cache: " + e.getMessage());
        }
    }

    public void sendVerificationMessage() {
        if (!isReady() || !configManager.isAccountLinkingEnabled()) return;
        String channelId = configManager.getLinkingChannelId();
        if (channelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
             plugin.getLogger().warning("Account linking channel not found: " + channelId);
            return;
        }

        MessageEmbed embed = EmbedBuilder.verificationEmbed();
        Button button = Button.primary("verify-button", configManager.getLinkingButtonLabel());

        String messageId = configManager.getVerificationMessageId();
        if (messageId != null && !messageId.isEmpty()) {
            channel.retrieveMessageById(messageId).queue(message -> {
                message.editMessageEmbeds(embed).setActionRow(button).queue();
            }, throwable -> sendNewVerificationMessage(channel, embed, button));
        } else {
            sendNewVerificationMessage(channel, embed, button);
        }
    }
    
    private void sendNewVerificationMessage(TextChannel channel, MessageEmbed embed, Button button) {
        
        channel.getHistory().retrievePast(50).queue(messages -> {
            messages.stream()
                .filter(m -> m.getAuthor().equals(jda.getSelfUser()))
                .forEach(message -> message.delete().queue());
            
            
            channel.sendMessageEmbeds(embed).setActionRow(button).queue(message -> {
                configManager.setVerificationMessageId(message.getId());
            });
        });
    }

    public void sendVoteLeaderboard() {
        if (!isReady() || !configManager.isVoteLeaderboardEnabled()) return;
        String channelId = configManager.getVoteLeaderboardChannelId();
        if (channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Vote leaderboard channel not found: " + channelId);
            return;
        }

        MessageEmbed embed = EmbedBuilder.votersListEmbed(null, Collections.emptyList());
        StringSelectMenu menu = StringSelectMenu.create("vote-leaderboard-menu")
                .setPlaceholder("Görüntülenecek dönemi seçin...")
                .addOption("Bu Ayın Liderleri", "current")
                .addOption("Geçen Ayın Liderleri", "previous")
                .build();

        String messageId = configManager.getVoteLeaderboardMessageId();
        if (messageId != null && !messageId.isEmpty()) {
            channel.retrieveMessageById(messageId).queue(message -> {
                message.editMessageEmbeds(embed).setActionRow(menu).queue();
            }, throwable -> sendNewVoteLeaderboard(channel, embed, menu));
        } else {
            sendNewVoteLeaderboard(channel, embed, menu);
        }
    }

    private void sendNewVoteLeaderboard(TextChannel channel, MessageEmbed embed, StringSelectMenu menu) {
        channel.sendMessageEmbeds(embed).setActionRow(menu).queue(message -> {
            configManager.setVoteLeaderboardMessageId(message.getId());
        });
    }

    public CompletableFuture<Message> sendMessage(String channelId, String message) {
        if (!isReady()) return failedFuture(new IllegalStateException("Discord bot not ready."));
        MessageChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            return channel.sendMessage(message).submit();
        }
        return failedFuture(new NullPointerException("Channel not found"));
    }

    public CompletableFuture<Message> sendEmbed(String channelId, MessageEmbed embed) {
        if (!isReady()) return failedFuture(new IllegalStateException("Discord bot not ready."));
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            return channel.sendMessageEmbeds(embed).submit();
        }
        return failedFuture(new NullPointerException("Channel not found"));
    }
    
    public CompletableFuture<Message> sendPrivateMessage(String userId, String message) {
        if (!isReady()) return failedFuture(new IllegalStateException("Discord bot not ready."));
        return jda.openPrivateChannelById(userId).flatMap(channel -> channel.sendMessage(message)).submit();
    }

    public void sendOrUpdateEmbed(String channelId, String messageId, MessageEmbed embed, Consumer<String> messageIdConsumer) {
        sendOrUpdateEmbed(channelId, messageId, embed, null, messageIdConsumer);
    }

    public void sendOrUpdateEmbed(String channelId, String messageId, MessageEmbed embed, 
                                  List<net.dv8tion.jda.api.interactions.components.LayoutComponent> components, 
                                  Consumer<String> messageIdConsumer) {
        if (!isReady()) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Channel not found for sending/updating embed: " + channelId);
            return;
        }

        MessageEditBuilder editBuilder = new MessageEditBuilder().setEmbeds(embed);
        MessageCreateBuilder createBuilder = new MessageCreateBuilder().setEmbeds(embed);

        if (components != null && !components.isEmpty()) {
            editBuilder.setComponents(components);
            createBuilder.setComponents(components);
        }

        if (configManager.isWebsiteButtonEnabled() && (components == null || components.isEmpty())) {
            String label = configManager.getWebsiteButtonLabel();
            String url = configManager.getWebsiteButtonUrl();
            String emoji = configManager.getWebsiteButtonEmoji();

            if (url != null && !url.isEmpty()) {
                Button button = Button.link(url, label);
                if (emoji != null && !emoji.isEmpty()) {
                    try {
                        button = button.withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid emoji found in config for website button: " + emoji);
                    }
                }
                editBuilder.setActionRow(button);
                createBuilder.setActionRow(button);
            }
        }

        validateAndUpdateMessage(channel, messageId, editBuilder, createBuilder, messageIdConsumer);
    }

    /**
     * Validates if a message exists and updates it, or sends a new one if not found
     */
    private void validateAndUpdateMessage(TextChannel channel, String messageId, 
                                        MessageEditBuilder editBuilder, MessageCreateBuilder createBuilder,
                                        Consumer<String> messageIdConsumer) {
        if (messageId != null && !messageId.isEmpty()) {
            channel.retrieveMessageById(messageId).queue(message -> {
                message.editMessage(editBuilder.build()).queue(
                    success -> plugin.getLogger().fine("Successfully updated message: " + messageId),
                    error -> {
                        plugin.getLogger().warning("Failed to update message " + messageId + ": " + error.getMessage());
                        sendNewMessage(channel, createBuilder, messageIdConsumer);
                    }
                );
            }, throwable -> {
                plugin.getLogger().info("Message " + messageId + " not found in channel " + channel.getName() + ", sending new message");
                sendNewMessage(channel, createBuilder, messageIdConsumer);
            });
        } else {
            sendNewMessage(channel, createBuilder, messageIdConsumer);
        }
    }

    /**
     * Sends a new message and updates the message ID in config
     */
    private void sendNewMessage(TextChannel channel, MessageCreateBuilder createBuilder, Consumer<String> messageIdConsumer) {
        channel.sendMessage(createBuilder.build()).queue(newMessage -> {
            messageIdConsumer.accept(newMessage.getId());
            plugin.getLogger().info("Sent new message with ID: " + newMessage.getId() + " in channel: " + channel.getName());
        }, error -> {
            plugin.getLogger().severe("Failed to send new message in channel " + channel.getName() + ": " + error.getMessage());
        });
    }

    /**
     * Validates all persistent messages and recreates them if they don't exist
     */
    public void validateAllPersistentMessages() {
        if (!isReady()) {
            plugin.getLogger().warning("Cannot validate messages - Discord bot not ready");
            return;
        }

        plugin.getLogger().info("Starting validation of all persistent messages...");

        if (configManager.isAccountLinkingEnabled()) {
            validateVerificationMessage();
        }

        if (configManager.isServerStatusEnabled() && configManager.isServerStatusPersistent()) {
            validateServerStatusMessage();
        }

        if (configManager.isVoteLeaderboardEnabled()) {
            validateVoteLeaderboardMessage();
        }

        if (configManager.isLeaderboardsEnabled()) {
            validateStatisticsLeaderboardMessage();
        }

        plugin.getLogger().info("Persistent message validation completed");
    }

    /**
     * Validates the verification message specifically
     */
    private void validateVerificationMessage() {
        String channelId = configManager.getLinkingChannelId();
        String messageId = configManager.getVerificationMessageId();
        
        if (channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Verification channel not found: " + channelId);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            plugin.getLogger().info("No verification message ID found, sending new verification message");
            sendVerificationMessage();
            return;
        }

        channel.retrieveMessageById(messageId).queue(
            message -> plugin.getLogger().fine("Verification message " + messageId + " validated successfully"),
            throwable -> {
                plugin.getLogger().info("Verification message " + messageId + " not found, recreating...");
                sendVerificationMessage();
            }
        );
    }

    /**
     * Validates the server status message specifically
     */
    private void validateServerStatusMessage() {
        String channelId = configManager.getServerStatusChannelId();
        String messageId = configManager.getServerStatusMessageId();
        
        if (channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Server status channel not found: " + channelId);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            plugin.getLogger().info("No server status message ID found, sending new status message");
            sendServerStatusUpdate();
            return;
        }

        channel.retrieveMessageById(messageId).queue(
            message -> plugin.getLogger().fine("Server status message " + messageId + " validated successfully"),
            throwable -> {
                plugin.getLogger().info("Server status message " + messageId + " not found, recreating...");
                sendServerStatusUpdate();
            }
        );
    }

    /**
     * Validates the vote leaderboard message specifically
     */
    private void validateVoteLeaderboardMessage() {
        String channelId = configManager.getVoteLeaderboardChannelId();
        String messageId = configManager.getVoteLeaderboardMessageId();
        
        if (channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Vote leaderboard channel not found: " + channelId);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            plugin.getLogger().info("No vote leaderboard message ID found, sending new leaderboard");
            sendVoteLeaderboard();
            return;
        }

        channel.retrieveMessageById(messageId).queue(
            message -> plugin.getLogger().fine("Vote leaderboard message " + messageId + " validated successfully"),
            throwable -> {
                plugin.getLogger().info("Vote leaderboard message " + messageId + " not found, recreating...");
                sendVoteLeaderboard();
            }
        );
    }

    /**
     * Validates the statistics leaderboard message specifically
     */
    private void validateStatisticsLeaderboardMessage() {
        String channelId = configManager.getStatsChannelId();
        String messageId = configManager.getLeaderboardMessageId();
        
        if (channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Statistics channel not found: " + channelId);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            plugin.getLogger().info("No statistics leaderboard message ID found, sending new leaderboard");
            sendLeaderboard();
            return;
        }

        channel.retrieveMessageById(messageId).queue(
            message -> plugin.getLogger().fine("Statistics leaderboard message " + messageId + " validated successfully"),
            throwable -> {
                plugin.getLogger().info("Statistics leaderboard message " + messageId + " not found, recreating...");
                sendLeaderboard();
            }
        );
    }

    public CompletableFuture<Boolean> assignRole(String userId, String roleId) {
        if (!isReady() || guild == null || roleId.isEmpty()) return CompletableFuture.completedFuture(false);
        Role role = guild.getRoleById(roleId);
        if (role == null) return CompletableFuture.completedFuture(false);
        return guild.addRoleToMember(User.fromId(userId), role).submit().handle((s, e) -> e == null);
    }

    public CompletableFuture<Boolean> removeRole(String userId, String roleId) {
        if (!isReady() || guild == null || roleId.isEmpty()) return CompletableFuture.completedFuture(false);
        Role role = guild.getRoleById(roleId);
        if (role == null) return CompletableFuture.completedFuture(false);
        return guild.removeRoleFromMember(User.fromId(userId), role).submit().handle((s, e) -> e == null);
    }

    public void sendLeaderboard() {
        if (!isReady() || !configManager.isLeaderboardsEnabled()) return;
        String channelId = configManager.getStatsChannelId();
        if (channelId.isEmpty()) {
            plugin.getLogger().warning("Statistics channel ID is not set. Leaderboard will not be sent.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Statistics channel not found: " + channelId);
            return;
        }

        MessageEmbed embed = EmbedBuilder.initialLeaderboardEmbed();
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("leaderboard-menu")
                .setPlaceholder("📈 Bir kategori seçin...");

        for (String category : configManager.getLeaderboardCategories()) {
            if (category.equalsIgnoreCase("votes") && !configManager.isVoteSystemEnabled()) {
                continue;
            }
            String categoryName = configManager.getLeaderboardCategoryName(category);
            String categoryEmoji = configManager.getLeaderboardCategoryIcon(category);
            menuBuilder.addOption(categoryName, category, net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(categoryEmoji));
        }

        if (menuBuilder.getOptions().isEmpty()) {
            plugin.getLogger().warning("No leaderboard categories found in config.yml. Leaderboard will not be sent.");
            return;
        }

        StringSelectMenu menu = menuBuilder.build();
        String messageId = configManager.getLeaderboardMessageId();

        if (messageId != null && !messageId.isEmpty()) {
            channel.retrieveMessageById(messageId).queue(message -> {
                message.editMessageEmbeds(embed).setActionRow(menu).queue();
            }, throwable -> sendNewLeaderboard(channel, embed, menu));
        } else {
            sendNewLeaderboard(channel, embed, menu);
        }
    }

    private void sendNewLeaderboard(TextChannel channel, MessageEmbed embed, StringSelectMenu menu) {
        
        channel.getHistory().retrievePast(50).queue(messages -> {
            messages.stream()
                .filter(m -> m.getAuthor().equals(jda.getSelfUser()) && !m.getEmbeds().isEmpty())
                .filter(m -> {
                    MessageEmbed messageEmbed = m.getEmbeds().get(0);
                    String title = messageEmbed.getTitle();
                    return title != null && title.contains("Liderlik Tabloları");
                })
                .forEach(message -> message.delete().queue());
            
            
            channel.sendMessageEmbeds(embed).setActionRow(menu).queue(newMessage -> {
                configManager.setLeaderboardMessageId(newMessage.getId());
            });
        });
    }

    public void sendServerStatusUpdate() {
        if (!isReady() || !configManager.isServerStatusEnabled()) return;
        String channelId = configManager.getServerStatusChannelId();
        if (channelId.isEmpty()) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String tps = getFormattedTPS();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
        String memory = usedMemory + "MB / " + maxMemory + "MB";
        String uptime = TimeUtil.formatDuration(System.currentTimeMillis() - plugin.getStartTime());

        MessageEmbed embed = EmbedBuilder.serverStatus(online, max, tps, memory, uptime);

        if (configManager.isServerStatusPersistent()) {
            String messageId = configManager.getServerStatusMessageId();
            sendOrUpdateEmbed(channelId, messageId, embed, configManager::setServerStatusMessageId);
        } else {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) return;

            MessageCreateBuilder createBuilder = new MessageCreateBuilder().addEmbeds(embed);

            if (configManager.isWebsiteButtonEnabled()) {
                String label = configManager.getWebsiteButtonLabel();
                String url = configManager.getWebsiteButtonUrl();
                String emoji = configManager.getWebsiteButtonEmoji();

                if (url != null && !url.isEmpty()) {
                    Button button = Button.link(url, label);
                    if (emoji != null && !emoji.isEmpty()) {
                        try {
                            button = button.withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Invalid emoji found in config for website button: " + emoji);
                        }
                    }
                    createBuilder.setActionRow(button);
                }
            }

            channel.sendMessage(createBuilder.build()).queue();
        }
    }
    

    private String getFormattedTPS() {
        try {
            Object server = Bukkit.getServer();
            Object mcServer = server.getClass().getMethod("getServer").invoke(server);
            Field tpsField = mcServer.getClass().getField("recentTps");
            double[] tps = (double[]) tpsField.get(mcServer);
            return String.format("%.2f", tps[0]);
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    public JDA getJDA() {
        return jda;
    }

    public Guild getGuild() {
        return guild;
    }

    public BotStatus getStatus() {
        return status;
    }

    public boolean isReady() {
        return status == BotStatus.READY && jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    private <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }
}
