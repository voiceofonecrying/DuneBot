package Model.CompositeKeys;

import java.io.Serializable;
import java.util.Objects;

public class TechTokenId implements Serializable {
    private int gameId;
    private String techName;

    public TechTokenId(int gameId, String techName) {
        this.gameId = gameId;
        this.techName = techName;
    }
    public TechTokenId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechTokenId that = (TechTokenId) o;
        return gameId == that.gameId && Objects.equals(techName, that.techName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, techName);
    }
}
