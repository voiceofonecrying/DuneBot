package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Game extends GameFactionBase {
    private String gameRole;
    private String modRole;
    private Boolean mute;

    private final List<Faction> factions;

    private final List<Territory> territories;
    public Game() {
        super();

        factions = new ArrayList<>();
        territories = new ArrayList<>();
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public List<Territory> getTerritories() {
        return territories;
    }
    public void addTerritories(List<Territory> territories) {
        territories.addAll(territories);
    }

    public String getGameRole() {
        return gameRole;
    }

    public void setGameRole(String gameRole) {
        this.gameRole = gameRole;
    }

    public String getModRole() {
        return modRole;
    }

    public void setModRole(String modRole) {
        this.modRole = modRole;
    }

    public Boolean getMute() {
        return mute;
    }

    public void setMute(Boolean mute) {
        this.mute = mute;
    }

    private Optional<Faction> findFaction(String name) {
        return factions.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }
    public Faction getFaction(String name) {
        return findFaction(name).get();
    }

    public Boolean hasFaction(String name) {
        return findFaction(name).isPresent();
    }

    public void addFaction(Faction faction) {
        factions.add(faction);
    }
}
