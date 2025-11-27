package kynux.cloud.discordPlus;

import kynux.cloud.discordPlus.commands.*;
import kynux.cloud.discordPlus.listeners.PlayerAdvancementListener;
import kynux.cloud.discordPlus.listeners.PlayerDeathListener;
import kynux.cloud.discordPlus.listeners.PlayerJoinListener;
import kynux.cloud.discordPlus.listeners.PlayerQuitListener;
import kynux.cloud.discordPlus.managers.*;
import kynux.cloud.discordPlus.utils.EmbedBuilder;
import kynux.cloud.discordPlus.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

public final class DiscordPlus extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DiscordManager discordManager;
    private LinkingManager linkingManager;
    private ChatManager chatManager;
    private VoteManager voteManager;
    private PermissionSyncManager permissionSyncManager;
    private StatisticManager statisticManager;
    private kynux.cloud.discordPlus.managers.SecurityManager securityManager;
    private LuckPermsManager luckPermsManager;
    private static Economy econ = null;
    private static DiscordPlus instance;

    private long startTime;

    private static final String API_URL = "https://nexoro.com.tr/app/api/v1/check.php?product=9&ip=";

    @Override
    public void onEnable() {
        instance = this;
        startTime = System.currentTimeMillis();
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.autoCompleteConfig();

        if (!validateConfig()) {
            getLogger().severe("Invalid configuration. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeManagers();
        registerCommands();
        registerEvents();
        scheduleDiscordTasks();

        if (!setupEconomy()) {
            getLogger().info("Vault not found. Economy features will be disabled.");
        } else {
            getLogger().info("Successfully hooked into Vault.");
        }

        discordManager.initialize();

        getLogger().info("DiscordPlus has been successfully enabled!");
        getLogger().info("Took " + (System.currentTimeMillis() - startTime) + "ms to enable.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscordPlus is shutting down...");
        try {
            if (permissionSyncManager != null) permissionSyncManager.shutdown();
            if (discordManager != null) discordManager.shutdown();
            if (databaseManager != null) databaseManager.close();
            getLogger().info("DiscordPlus has been successfully disabled!");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin shutdown!", e);
        }
    }

    private void initializeManagers() {
        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initialize();

        discordManager = new DiscordManager(this, configManager, databaseManager);

        MessageUtil.initialize(getConfig(), getLogger(), getDataFolder());
        EmbedBuilder.initialize(getConfig());

        linkingManager = new LinkingManager(this, configManager, databaseManager, discordManager);
        luckPermsManager = new LuckPermsManager(this);
        chatManager = new ChatManager(this, configManager, discordManager, databaseManager, luckPermsManager);
        voteManager = new VoteManager(this, configManager, databaseManager, discordManager);
        permissionSyncManager = new PermissionSyncManager(this); 
        statisticManager = new StatisticManager(this, databaseManager, discordManager, configManager);
        securityManager = new kynux.cloud.discordPlus.managers.SecurityManager(this);

        getLogger().info("Managers initialized.");
    }

    private boolean validateConfig() {
        String token = configManager.getDiscordToken();
        if (token.isEmpty() || token.equals("BOT_TOKEN_BURAYA")) {
            getLogger().warning("Discord bot token is not set! Please edit discord.token in config.yml.");
        }
        return true;
    }

    private void registerEvents() {
        if (configManager.isNotificationsEnabled()) {
            if (configManager.isJoinQuitEnabled()) {
                getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
                getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
            }
            if (configManager.isDeathNotificationsEnabled()) {
                getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            }
            if (configManager.isAchievementsEnabled()) {
                getServer().getPluginManager().registerEvents(new PlayerAdvancementListener(this), this);
            }
        }
        if (configManager.isAccountLinkingEnabled()) {
            getServer().getPluginManager().registerEvents(new kynux.cloud.discordPlus.listeners.LinkingNotificationListener(), this);
        }
        
        getServer().getPluginManager().registerEvents(new kynux.cloud.discordPlus.listeners.SecurityListener(this), this);

        if (luckPermsManager.isLuckPermsAvailable()) {
            new kynux.cloud.discordPlus.listeners.LuckPermsListener(this, luckPermsManager.getLuckPerms());
            getLogger().info("LuckPerms listener registered.");
        }
        
        getLogger().info("Event listeners registered conditionally.");
    }

    private void registerCommands() {
        getCommand("discord").setExecutor(new DiscordCommand(this));
        getCommand("discordplus").setExecutor(new DiscordPlusCommand(this));
        getCommand("dpgui").setExecutor(new DiscordPlusGuiCommand(this));
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("sync").setExecutor(new SyncCommand(this));
        getCommand("istatistik").setExecutor(new StatisticCommand(this));
        getCommand("unlink").setExecutor(new UnlinkCommand(this));
        getLogger().info("Commands registered.");
    }

    private void scheduleDiscordTasks() {
        if (configManager.isServerStatusEnabled()) {
            long interval = configManager.getServerStatusUpdateInterval() * 60 * 20L;
            if (interval > 0) {
                getServer().getScheduler().runTaskTimerAsynchronously(this,
                    () -> discordManager.sendServerStatusUpdate(), 0L, interval);
            }
        }
        if (configManager.isStatisticsEnabled()) {
            statisticManager.schedulePlaytimeUpdates();
        }
    }
    
    private void notifyAdminsOfFailure() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("discordplus.admin")) {
                player.sendMessage(MessageUtil.colorize("&c&l[DiscordPlus] &eDiscord botu başlatılamadı! &7Lütfen konsol günlüklerini kontrol edin."));
            }
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public LinkingManager getLinkingManager() {
        return linkingManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public PermissionSyncManager getPermissionSyncManager() {
        return permissionSyncManager;
    }

    public StatisticManager getStatisticManager() {
        return statisticManager;
    }

    public kynux.cloud.discordPlus.managers.SecurityManager getSecurityManager() {
        return securityManager;
    }

    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    public long getStartTime() {
        return startTime;
    }

    public Economy getEconomy() {
        return econ;
    }

    public static DiscordPlus getInstance() {
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void reloadPlugin() {
        getLogger().info("Reloading plugin...");
        reloadConfig();
        configManager.reloadConfig();
        
        
        MessageUtil.initialize(getConfig(), getLogger(), getDataFolder());
        EmbedBuilder.initialize(getConfig());
        
        getLogger().info("Plugin reloaded successfully.");
    }

    public boolean isDiscordReady() {
        return discordManager != null && discordManager.isReady();
    }

}
