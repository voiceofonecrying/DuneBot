package Model.CompositeKeys;

import java.io.Serializable;
import java.util.Objects;

public class SpiceCardId implements Serializable {
    private int gameId;
    private String cardName;

    public SpiceCardId(int gameId, String cardName) {
        this.gameId = gameId;
        this.cardName = cardName;
    }

    public SpiceCardId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpiceCardId that = (SpiceCardId) o;
        return gameId == that.gameId && Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, cardName);
    }
}
