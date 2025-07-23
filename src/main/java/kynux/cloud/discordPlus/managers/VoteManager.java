package kynux.cloud.discordPlus.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import kynux.cloud.discordPlus.DiscordPlus;
import kynux.cloud.discordPlus.data.PlayerData;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import kynux.cloud.discordPlus.utils.MessageUtil;
import kynux.cloud.discordPlus.utils.TimeUtil;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class VoteManager {

    private final DiscordPlus plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final DiscordManager discordManager;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private static final String API_BASE_URL = "https://minecraft-mp.com/api/";

    
    private final ConcurrentHashMap<UUID, Integer> voteStreaks;
    private final ConcurrentHashMap<UUID, LocalDateTime> lastVoteDate;
    private int currentVotePartyCount = 0;
    private int votePartyGoal = 100;

    public VoteManager(DiscordPlus plugin, ConfigManager configManager,
                       DatabaseManager databaseManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.discordManager = discordManager;

        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.apiKey = configManager.getVoteApiKey();

        this.voteStreaks = new ConcurrentHashMap<>();
        this.lastVoteDate = new ConcurrentHashMap<>();

        if (isVoteApiEnabled()) {
            plugin.getLogger().info("VoteManager initialized with Minecraft-MP.com API.");
            initializePeriodicTasks();
        } else {
            plugin.getLogger().warning("VoteManager is disabled or API key is not set.");
        }
    }

    private boolean isVoteApiEnabled() {
        return configManager.isVoteSystemEnabled() && apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY");
    }

    private void initializePeriodicTasks() {
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::validateVoteStreaks, 20L * 60 * 60, 20L * 60 * 60);
    }

    public CompletableFuture<Integer> checkVoteClaim(String username) {
        if (!isVoteApiEnabled()) {
            return CompletableFuture.completedFuture(-1);
        }

        CompletableFuture<Integer> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(API_BASE_URL + "?object=votes&element=claim&key=" + apiKey + "&username=" + username)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                plugin.getLogger().log(Level.SEVERE, "API request to check vote claim for " + username + " failed.", e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        future.complete(0);
                        return;
                    }
                    String responseBody = body.string();
                    future.complete(Integer.parseInt(responseBody));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to parse vote claim response for " + username, e);
                    future.complete(0);
                }
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> setVoteAsClaimed(String username) {
        if (!isVoteApiEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(API_BASE_URL + "?action=post&object=votes&element=claim&key=" + apiKey + "&username=" + username)
                .post(RequestBody.create(new byte[0]))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                plugin.getLogger().log(Level.SEVERE, "API request to set vote as claimed for " + username + " failed.", e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        future.complete(false);
                        return;
                    }
                    String responseBody = body.string();
                    future.complete("1".equals(responseBody));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to parse set vote as claimed response for " + username, e);
                    future.complete(false);
                }
            }
        });
        return future;
    }

    public void processVoteReward(Player player) {
        if (!isVoteApiEnabled()) {
            player.sendMessage(MessageUtil.colorize("&cVote sistemi şu anda aktif değil."));
            return;
        }

        String username = player.getName();
        UUID playerUUID = player.getUniqueId();

        checkVoteClaim(username).thenAcceptAsync(status -> {
            switch (status) {
                case 1:
                    setVoteAsClaimed(username).thenAccept(claimed -> {
                        if (claimed) {
                            PlayerData playerData = databaseManager.loadPlayerData(playerUUID).join();
                            playerData.incrementVoteCount();
                            databaseManager.savePlayerData(playerData);
                            int newStreak = updateVoteStreak(playerUUID);
                            sendVoteNotifications(playerData, "minecraft-mp.com", newStreak);
                            sendThankYouMessage(username, "minecraft-mp.com", newStreak);
                            updateVoteParty();
                            plugin.getLogger().info(String.format("Vote reward claimed by %s. New streak: %d", username, newStreak));
                        } else {
                            player.sendMessage(MessageUtil.colorize("&cÖdül talep edilirken bir hata oluştu. Lütfen tekrar deneyin."));
                        }
                    });
                    break;
                case 2:
                    player.sendMessage(MessageUtil.colorize("&eSon 24 saat içindeki oy ödülünüzü zaten talep ettiniz."));
                    break;
                default:
                    player.sendMessage(MessageUtil.colorize("&cSon 24 saat içinde oy vermemişsiniz veya API'ye ulaşılamıyor."));
                    break;
            }
        }).exceptionally(ex -> {
            player.sendMessage(MessageUtil.colorize("&cVote durumu kontrol edilirken bir hata oluştu."));
            return null;
        });
    }

    public CompletableFuture<List<Voter>> getVotersList(String period, int limit) {
        if (!isVoteApiEnabled()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        CompletableFuture<List<Voter>> future = new CompletableFuture<>();
        String url = API_BASE_URL + "?object=servers&element=voters&key=" + apiKey +
                     "&month=" + period + "&format=json&limit=" + limit;
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                plugin.getLogger().log(Level.SEVERE, "API request to get voters list failed.", e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        future.complete(Collections.emptyList());
                        return;
                    }
                    String responseBody = body.string();
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    Type voterListType = new TypeToken<List<Voter>>() {}.getType();
                    List<Voter> voters = gson.fromJson(jsonObject.get("voters"), voterListType);
                    future.complete(voters);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to parse voters list response.", e);
                    future.complete(Collections.emptyList());
                }
            }
        });
        return future;
    }

    private int updateVoteStreak(UUID playerUUID) {
        LocalDateTime now = TimeUtil.now();
        LocalDateTime lastVote = lastVoteDate.get(playerUUID);
        if (lastVote == null) {
            voteStreaks.put(playerUUID, 1);
            lastVoteDate.put(playerUUID, now);
            return 1;
        }
        long daysSinceLastVote = TimeUtil.daysBetween(lastVote, now);
        if (daysSinceLastVote <= 1) {
            int newStreak = voteStreaks.getOrDefault(playerUUID, 0) + 1;
            voteStreaks.put(playerUUID, newStreak);
            lastVoteDate.put(playerUUID, now);
            return newStreak;
        } else {
            voteStreaks.put(playerUUID, 1);
            lastVoteDate.put(playerUUID, now);
            return 1;
        }
    }

    private void validateVoteStreaks() {
        LocalDateTime now = TimeUtil.now();
        lastVoteDate.entrySet().removeIf(entry -> {
            if (TimeUtil.daysBetween(entry.getValue(), now) > 2) {
                voteStreaks.remove(entry.getKey());
                plugin.getLogger().fine("Vote streak reset for player: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    private void updateVoteParty() {
        currentVotePartyCount++;
        if (currentVotePartyCount >= votePartyGoal) {
            triggerVoteParty();
        } else {
            sendVotePartyProgress();
        }
    }

    private void triggerVoteParty() {
        currentVotePartyCount = 0;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage(MessageUtil.colorize("&d&l🎉 VOTE PARTİSİ! 🎉"));
            Bukkit.broadcastMessage(MessageUtil.colorize("&b" + votePartyGoal + " vote hedefine ulaşıldı!"));
            Bukkit.broadcastMessage(MessageUtil.colorize("&aTüm oyunculara özel ödüller veriliyor!"));
        });
        sendVotePartyNotification();
        plugin.getLogger().info("Vote party triggered! Goal: " + votePartyGoal + " votes reached");
    }

    private void sendVotePartyProgress() {
        int remaining = votePartyGoal - currentVotePartyCount;
        if (currentVotePartyCount % 10 == 0) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage(MessageUtil.colorize(
                    "&b&l📊 Vote Party: &e" + currentVotePartyCount + "&7/&e" + votePartyGoal +
                    " &8(Kalan: &c" + remaining + "&8)"));
            });
        }
    }

    private void sendVoteNotifications(PlayerData playerData, String voteSite, int streak) {
        if (discordManager.isReady()) {
            String channelId = configManager.getVoteChannelId();
            if (!channelId.isEmpty()) {
                String playerName = Bukkit.getOfflinePlayer(playerData.getMinecraftUUID()).getName();
                String rewardInfo = String.join(", ", configManager.getVoteSiteRewards(voteSite));
                discordManager.sendEmbed(channelId,
                    EmbedBuilder.voteNotification(playerName, voteSite, streak, rewardInfo));
            }
        }
    }
    
    private void sendVotePartyNotification() {
        if (discordManager.isReady()) {
            String channelId = configManager.getVoteChannelId();
            if (!channelId.isEmpty()) {
                String message = String.format("🎉 **VOTE PARTİSİ!** 🎉\n\n" +
                    "**%d** vote hedefine ulaşıldı!\n" +
                    "Tüm oyunculara özel ödüller verildi! ✨", votePartyGoal);
                discordManager.sendMessage(channelId, message);
            }
        }
    }

    private void sendThankYouMessage(String playerName, String voteSite, int streak) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(MessageUtil.colorize("&a&l✅ Oy verdiğiniz için teşekkürler!"));
                player.sendMessage(MessageUtil.colorize("&7Site: &b" + voteSite));
                player.sendMessage(MessageUtil.colorize("&7Vote Seriniz: &e" + streak + " &7gün"));
                player.sendMessage(MessageUtil.colorize("&aÖdülleriniz verildi!"));
            });
        }
    }

    public static class Voter {
        public String nickname;
        public String votes;
    }
}
