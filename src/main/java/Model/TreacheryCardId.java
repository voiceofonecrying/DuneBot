package Model;

import java.io.Serializable;
import java.util.Objects;

public class TreacheryCardId implements Serializable {
    private int gameId;
    private String cardName;

    public TreacheryCardId(int gameId, String cardName) {
        this.gameId = gameId;
        this.cardName = cardName;
    }

    public TreacheryCardId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreacheryCardId that = (TreacheryCardId) o;
        return gameId == that.gameId && Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, cardName);
    }
}
