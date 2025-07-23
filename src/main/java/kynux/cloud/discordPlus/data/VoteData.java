package kynux.cloud.discordPlus.data;

import java.time.LocalDateTime;
import java.util.UUID;

public class VoteData {
    
    private final int id;
    private final UUID minecraftUUID;
    private final String voteSite;
    private final LocalDateTime voteDate;
    private final boolean rewarded;
    private final String rewardData;
    private final int streakCount;
    
    public VoteData(int id, UUID minecraftUUID, String voteSite, LocalDateTime voteDate, 
                   boolean rewarded, String rewardData, int streakCount) {
        this.id = id;
        this.minecraftUUID = minecraftUUID;
        this.voteSite = voteSite;
        this.voteDate = voteDate;
        this.rewarded = rewarded;
        this.rewardData = rewardData;
        this.streakCount = streakCount;
    }
    
    public VoteData(UUID minecraftUUID, String voteSite, boolean rewarded, String rewardData, int streakCount) {
        this.id = -1; 
        this.minecraftUUID = minecraftUUID;
        this.voteSite = voteSite;
        this.voteDate = LocalDateTime.now();
        this.rewarded = rewarded;
        this.rewardData = rewardData;
        this.streakCount = streakCount;
    }
    
    public VoteData(UUID minecraftUUID, String voteSite, LocalDateTime voteDate, int streakCount, String ipAddress) {
        this.id = -1; 
        this.minecraftUUID = minecraftUUID;
        this.voteSite = voteSite;
        this.voteDate = voteDate;
        this.rewarded = false;
        this.rewardData = ipAddress; 
        this.streakCount = streakCount;
    }
    
    public int getId() { return id; }
    public UUID getMinecraftUUID() { return minecraftUUID; }
    public String getVoteSite() { return voteSite; }
    public LocalDateTime getVoteDate() { return voteDate; }
    public boolean isRewarded() { return rewarded; }
    public String getRewardData() { return rewardData; }
    public int getStreakCount() { return streakCount; }
    
    @Override
    public String toString() {
        return "VoteData{" +
                "id=" + id +
                ", minecraftUUID=" + minecraftUUID +
                ", voteSite='" + voteSite + '\'' +
                ", voteDate=" + voteDate +
                ", rewarded=" + rewarded +
                ", rewardData='" + rewardData + '\'' +
                ", streakCount=" + streakCount +
                '}';
    }
}
