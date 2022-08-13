package Model;

import java.io.Serializable;
import java.util.Objects;

public class LeaderId implements Serializable {
    private int gameId;
    private String leaderName;

    public LeaderId(int gameId, String leaderName) {
        this.gameId = gameId;
        this.leaderName = leaderName;
    }

    public LeaderId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderId leaderId = (LeaderId) o;
        return gameId == leaderId.gameId && Objects.equals(leaderName, leaderId.leaderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, leaderName);
    }
}
