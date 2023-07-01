package model;

import model.factions.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Territory {
    private final String territoryName;
    private final int sector;
    private boolean isRock;
    private final boolean isStronghold;
    private int spice;
    private final List<Force> forces;
    private Integer richeseNoField;

    public Territory(String territoryName, int sector, boolean isRock, boolean isStronghold) {
        this.territoryName = territoryName;
        this.sector = sector;
        this.isRock = isRock;
        this.isStronghold = isStronghold;
        this.spice = 0;
        this.forces = new ArrayList<>();
        this.richeseNoField = null;
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

    public Force getForce(String name) {
        return forces.stream().filter(force -> force.getName().equals(name)).findFirst().orElse(new Force(name, 0));
    }

    // Returns list of factions that have active forces in the territories (not Advisors)
    public List<Faction> getActiveFactions(Game game) {
        return forces.stream()
                .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                .map(Force::getFactionName)
                .distinct()
                .map(game::getFaction)
                .collect(Collectors.toList());
    }

    public int countActiveFactions(Game game) {
        return getActiveFactions(game).size();
    }

    public void setForceStrength(String name, int strength) {
        if (strength <= 0) forces.remove(getForce(name));
        else if (getForce(name).getStrength() == 0) forces.add(new Force(name, strength));
        else getForce(name).setStrength(strength);
    }

    public void setRock(boolean rock) {
        isRock = rock;
    }

    public void addSpice(Integer spice) {
        this.spice += spice;
    }

    public void setRicheseNoField(Integer richeseNoField) {
        this.richeseNoField = richeseNoField;
    }

    public Integer getRicheseNoField() {
        return richeseNoField;
    }

    public boolean hasRicheseNoField() {
        return getRicheseNoField() != null;
    }
}
