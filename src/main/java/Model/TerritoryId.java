package Model;

import java.io.Serializable;
import java.util.Objects;

public class TerritoryId implements Serializable {
    private int gameId;
    private String territoryName;
    private int sector;

    public TerritoryId(int gameId, String territoryName, int sector) {
        this.gameId = gameId;
        this.territoryName = territoryName;
        this.sector = sector;
    }

    public TerritoryId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerritoryId that = (TerritoryId) o;
        return gameId == that.gameId && sector == that.sector && Objects.equals(territoryName, that.territoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, territoryName, sector);
    }
}
