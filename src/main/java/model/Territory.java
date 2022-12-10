package model;

import java.util.ArrayList;
import java.util.List;

public class Territory {
    private final String territoryName;
    private final int sector;
    private final boolean isRock;
    private final boolean isStronghold;
    private int spice;
    private final List<Force> forces;

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold) {
        this.territoryName = territoryName;
        this.sector = sector;
        this.isRock = isRock;
        this.isStronghold = isStronghold;
        this.spice = 0;
        this.forces = new ArrayList<>();
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public int getSector() {
        return sector;
    }

    public boolean isRock() {
        return isRock;
    }

    public boolean isStronghold() {
        return isStronghold;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }

    public List<Force> getForces() {
        return forces;
    }

    public void addSpice(Integer spice) {
        this.spice += spice;
    }
}
