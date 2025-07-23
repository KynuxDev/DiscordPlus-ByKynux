package kynux.cloud.discordPlus.data;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String discordId;
    private boolean isVerified;
    private String verificationCode;
    private LocalDateTime verificationCodeTimestamp;
    private LocalDateTime linkDate;
    private LocalDateTime lastSeen;
    private long totalPlaytime;
    private LocalDateTime sessionStart;
    private long dailyPlaytime;
    private LocalDateTime lastDailyReset;
    private int deathCount;
    private int killCount;
    private int voteCount;
    private int loginStreak;
    private LocalDateTime lastLogin;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.sessionStart = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
        this.lastDailyReset = LocalDateTime.now();
    }

    public PlayerData(UUID uuid, String discordId, String verificationCode, boolean isVerified, LocalDateTime verificationCodeTimestamp, LocalDateTime linkDate, long totalPlaytime, LocalDateTime sessionStart, long dailyPlaytime, LocalDateTime lastDailyReset, int deathCount, int killCount, int voteCount, int loginStreak, LocalDateTime lastLogin, LocalDateTime lastSeen) {
        this.uuid = uuid;
        this.discordId = discordId;
        this.verificationCode = verificationCode;
        this.isVerified = isVerified;
        this.verificationCodeTimestamp = verificationCodeTimestamp;
        this.linkDate = linkDate;
        this.totalPlaytime = totalPlaytime;
        this.sessionStart = sessionStart;
        this.dailyPlaytime = dailyPlaytime;
        this.lastDailyReset = lastDailyReset;
        this.deathCount = deathCount;
        this.killCount = killCount;
        this.voteCount = voteCount;
        this.loginStreak = loginStreak;
        this.lastLogin = lastLogin;
        this.lastSeen = lastSeen;
    }

    public UUID getMinecraftUUID() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public boolean isLinked() {
        return discordId != null;
    }
    
    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getVerificationCodeTimestamp() {
        return verificationCodeTimestamp;
    }

    public void setVerificationCodeTimestamp(LocalDateTime verificationCodeTimestamp) {
        this.verificationCodeTimestamp = verificationCodeTimestamp;
    }

    public LocalDateTime getLinkDate() {
        return linkDate;
    }

    public void setLinkDate(LocalDateTime linkDate) {
        this.linkDate = linkDate;
    }
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getTotalPlaytime() {
        return totalPlaytime;
    }

    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }

    public LocalDateTime getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(LocalDateTime sessionStart) {
        this.sessionStart = sessionStart;
    }

    public long getDailyPlaytime() {
        return dailyPlaytime;
    }

    public void setDailyPlaytime(long dailyPlaytime) {
        this.dailyPlaytime = dailyPlaytime;
    }

    public LocalDateTime getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(LocalDateTime lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void incrementDeathCount() {
        this.deathCount++;
    }

    public int getKillCount() {
        return killCount;
    }

    public void incrementKillCount() {
        this.killCount++;
    }

    public double getKDRatio() {
        return killCount / Math.max(deathCount, 1.0);
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void incrementVoteCount() {
        this.voteCount++;
    }

    public int getLoginStreak() {
        return loginStreak;
    }

    public void setLoginStreak(int loginStreak) {
        this.loginStreak = loginStreak;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
