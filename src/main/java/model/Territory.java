package model;

import helpers.Exclude;
import model.factions.Faction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Territory {
    private final String territoryName;
    private final int sector;
    private final boolean isStronghold;
    private final boolean isNearShieldWall;
    private final List<Force> forces;
    private final List<String> adjacencyList;
    private boolean isRock;
    private int spice;
    private Integer richeseNoField;
    private String ecazAmbassador;
    private List<String> terrorTokens;
    @Exclude
    private String terrorToken;
    private boolean aftermathToken;
    private String discoveryToken;
    private boolean discovered;

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold, boolean isNearShieldWall) {
        this.territoryName = territoryName;
        this.sector = sector;
        this.isRock = isRock;
        this.isStronghold = isStronghold;
        this.isNearShieldWall = isNearShieldWall;
        this.spice = 0;
        this.forces = new ArrayList<>();
        this.richeseNoField = null;
        this.ecazAmbassador = null;
        this.adjacencyList = new LinkedList<>();
        this.aftermathToken = false;
        this.terrorTokens = new LinkedList<>();
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

    public void setRock(boolean rock) {
        isRock = rock;
    }

    public boolean isStronghold() {
        return isStronghold;
    }

    public boolean isNearShieldWall() {
        // Temporary patch until all games have started with the isNearShieldWall boolean in Territories.csv
        if (territoryName.startsWith("False Wall East")) return true;
        else if (territoryName.startsWith("Hole In The Rock")) return true;
        else if (territoryName.startsWith("Gara Kulon")) return true;
        else if (territoryName.startsWith("Imperial Basin")) return true;
        else if (territoryName.startsWith("Pasty Mesa")) return true;
        else if (territoryName.startsWith("Shield Wall")) return true;
        else if (territoryName.startsWith("Sihaya Ridge")) return true;
        else if (territoryName.startsWith("The Minor Erg")) return true;

        return isNearShieldWall;
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

    public Force getForce(String name) {
        return forces.stream().filter(force -> force.getName().equals(name)).findFirst().orElse(new Force(name, 0));
    }

    public boolean hasForce(String name) {
        return forces.stream().anyMatch(force -> force.getName().equals(name));
    }

    /**
     * Returns a list of forces in the territory that belong to the given faction.
     *
     * @param faction The faction to filter by.
     * @return A list of forces in the territory that belong to the given faction.
     */
    public List<Force> getForces(Faction faction) {
        return forces.stream().filter(force -> force.getFactionName().equals(faction.getName())).toList();
    }

    public void removeForce(String name) {
        forces.remove(getForce(name));
    }

    public void addForce(Force force) {
        forces.add(force);
    }

    public List<String> getActiveFactionNames() {
        Set<String> factions = forces.stream()
                .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                .map(Force::getFactionName)
                .collect(Collectors.toSet());

        if (hasRicheseNoField()) factions.add("Richese");

        return new ArrayList<>(factions);
    }

    // Returns list of factions that have active forces in the territories (not Advisors)
    public List<Faction> getActiveFactions(Game game) {
        return getActiveFactionNames().stream()
                .map(game::getFaction)
                .toList();
    }

    public boolean hasActiveFaction(Faction faction) {
        return getActiveFactionNames().contains(faction.getName());
    }

    public int countActiveFactions() {
        return getActiveFactionNames().size();
    }

    public void setForceStrength(String name, int strength) {
        if (strength <= 0) forces.remove(getForce(name));
        else if (getForce(name).getStrength() == 0) forces.add(new Force(name, strength));
        else getForce(name).setStrength(strength);
    }

    public void addSpice(Integer spice) {
        this.spice += spice;
    }

    public Integer getRicheseNoField() {
        return richeseNoField;
    }

    public void setRicheseNoField(Integer richeseNoField) {
        this.richeseNoField = richeseNoField;
    }

    public boolean hasRicheseNoField() {
        return getRicheseNoField() != null;
    }

    public List<String> getAdjacencyList() {
        return adjacencyList;
    }

    public String getEcazAmbassador() {
        return ecazAmbassador;
    }

    public void setEcazAmbassador(String ecazAmbassador) {
        this.ecazAmbassador = ecazAmbassador;
    }

    public List<String> getTerrorTokens() {
        if (terrorTokens == null) {
            LinkedList<String> tokens = new LinkedList<>();
            tokens.add(terrorToken);
            this.terrorTokens = tokens;
        }
        return terrorTokens;
    }

    public void addTerrorToken(String terrorToken) {
        if (terrorTokens == null) {
            LinkedList<String> tokens = new LinkedList<>();
            tokens.add(this.terrorToken);
            this.terrorTokens = tokens;
        }
        this.terrorTokens.add(terrorToken);
    }

    public boolean hasTerrorToken() {
        return terrorTokens != null;
    }

    public boolean isAftermathToken() {
        return aftermathToken;
    }

    public void setAftermathToken(boolean aftermathToken) {
        this.aftermathToken = aftermathToken;
    }

    public String getDiscoveryToken() {
        return discoveryToken;
    }

    public void setDiscoveryToken(String discoveryToken) {
        this.discoveryToken = discoveryToken;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }
}
