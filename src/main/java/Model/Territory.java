package Model;

import Model.CompositeKeys.TerritoryId;
import jakarta.persistence.*;

@Entity
@Table(name = "territory")
@IdClass(TerritoryId.class)
public class Territory {

    @Id
    @Column(name = "GAME_ID")
    private int gameId;
    @Id
    @Column(name = "TERRITORY_NAME")
    private String territoryName;
    @Id
    @Column(name = "SECTOR")
    private int sector;
    @Column(name = "IS_ROCK")
    private boolean isRock;
    @Column(name = "IS_STRONGHOLD")
    private boolean isStronghold;
    @Column(name = "FREMEN_CAN_SHIP")
    private boolean fremenCanShip;
    @Column(name = "FORCES")
    private int forces;
    @Column(name = "CONTROLLING_FACTION")
    private String controllingFaction;
    @Column(name = "ADVISORS")
    private int advisors;
    @Column(name = "SPICE")
    private int spice;


    public Territory(int gameId, String territoryName, int sector, boolean isRock, boolean isStronghold, boolean fremenCanShip, int forces, String controllingFaction, int advisors, int spice) {
        this.gameId = gameId;
        this.territoryName = territoryName;
        this.sector = sector;
        this.isRock = isRock;
        this.isStronghold = isStronghold;
        this.fremenCanShip = fremenCanShip;
        this.forces = forces;
        this.controllingFaction = controllingFaction;
        this.advisors = advisors;
        this.spice = spice;
    }

    public Territory() {}

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public boolean isRock() {
        return isRock;
    }

    public void setRock(boolean rock) {
        isRock = rock;
    }

    public boolean isStronghold() {
        return isStronghold;
    }

    public void setStronghold(boolean stronghold) {
        isStronghold = stronghold;
    }

    public boolean isFremenCanShip() {
        return fremenCanShip;
    }

    public void setFremenCanShip(boolean fremenCanShip) {
        this.fremenCanShip = fremenCanShip;
    }

    public int getForces() {
        return forces;
    }

    public void setForces(int forces) {
        this.forces = forces;
    }

    public String getControllingFaction() {
        return controllingFaction;
    }

    public void setControllingFaction(String controllingFaction) {
        this.controllingFaction = controllingFaction;
    }

    public int getAdvisors() {
        return advisors;
    }

    public void setAdvisors(int advisors) {
        this.advisors = advisors;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }
}
