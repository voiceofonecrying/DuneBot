package Model.CompositeKeys;

import java.io.Serializable;
import java.util.Objects;

public class FactionId implements Serializable {
    private int gameId;
    private String discordId;

    public FactionId(int gameId, String discordId) {
        this.gameId = gameId;
        this.discordId = discordId;
    }

    public FactionId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactionId factionId = (FactionId) o;
        return gameId == factionId.gameId && Objects.equals(discordId, factionId.discordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, discordId);
    }

}
