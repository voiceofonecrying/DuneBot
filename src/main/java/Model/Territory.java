package Model;

public class Territory {

    private int gameId;
    private String territoryName;
    private int sector;
    private boolean isRock;
    private boolean isStronghold;
    private boolean fremenCanShip;
    private int forces;
    private String controllingFaction;
    private int advisors;
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
