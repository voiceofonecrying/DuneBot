package model;

import constants.Emojis;
import model.factions.BGFaction;

import java.util.*;
import java.util.stream.Collectors;

public class Territories extends HashMap<String, Territory> {
    public Territory addHomeworld(Game game, String homeworldName, String factionName) {
        Territory homeworld = new HomeworldTerritory(game, homeworldName, factionName);
        put(homeworldName, homeworld);
        return homeworld;
    }

    public Territory addDiscoveryToken(String name, boolean isStronghold) {
        Territory discoveryToken = new Territory(name, -1, false, isStronghold, true, false);
        discoveryToken.setJustDiscovered(true);
        put(name, discoveryToken);
        return discoveryToken;
    }

    public void addHMS() {
        String name = "Hidden Mobile Stronghold";
        Territory hms = new Territory(name, -1, false, true, false, false);
        put(name, hms);
    }

    /**
     * Returns the territory sector with the given name.
     *
     * @param name The territory sector's name.
     * @return The territory sector with the given name.
     */
    public Territory getTerritory(String name) {
        Territory territory = get(name);
        if (territory == null) {
            throw new IllegalArgumentException("No territory with name " + name);
        }
        return territory;
    }

    public void moveStorm(Game game) {
        List<Territory> territoriesInStorm = new ArrayList<>();
        for (int i = 0; i < game.getStormMovement(); i++) {
            game.advanceStorm(1);
            territoriesInStorm.addAll(values().stream().filter(t -> t.getSector() == game.getStorm() && !t.isRock()).toList());
        }
        territoriesInStorm.forEach(t -> t.stormTroops(game));
        territoriesInStorm.forEach(t -> t.stormRemoveSpice(game));
        territoriesInStorm.forEach(t -> t.stormRemoveAmbassador(game));
    }

    public Set<String> getDistinctAggregateTerritoryNames() {
        return values().stream().map(Territory::getAggregateTerritoryName).collect(Collectors.toSet());
    }

    public List<Territory> getTerritorySectorsInStormOrder(String aggregateTerritoryName) {
        List<Territory> territorySectors = new ArrayList<>(this.values().stream().filter(t -> t.getTerritoryName().replaceAll("\\s*\\([^)]*\\)\\s*", "").equalsIgnoreCase(aggregateTerritoryName)).toList());
        territorySectors.sort(Comparator.comparingInt(Territory::getSector));
        if (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression") || aggregateTerritoryName.equals("Meridian"))
            territorySectors.addFirst(territorySectors.removeLast());
        return territorySectors;
    }

    public List<List<Territory>> getAggregateTerritoryList(String aggregateTerritoryName, int storm, boolean includeSectorsUnderStorm) {
        List<Territory> territorySectors = getTerritorySectorsInStormOrder(aggregateTerritoryName);
        List<Territory> sectorsBeforeStorm;
        List<Territory> sectorsAfterStorm;
        List<Territory> sectorsUnderStorm = new ArrayList<>();
        if (storm != 1 && (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression") || aggregateTerritoryName.equals("Meridian"))) {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() != storm).toList();
            sectorsAfterStorm = new ArrayList<>();
        } else if (storm == 1 && (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression"))) {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() == 18).toList();
            sectorsAfterStorm = territorySectors.stream().filter(t -> t.getSector() == 2).toList();
        } else {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() < storm).toList();
            sectorsAfterStorm = territorySectors.stream().filter(t -> t.getSector() > storm).toList();
        }
        if (includeSectorsUnderStorm)
            sectorsUnderStorm = territorySectors.stream().filter(t -> t.getSector() == storm).toList();
        List<List<Territory>> returnList = new ArrayList<>();
        if (!sectorsBeforeStorm.isEmpty()) returnList.add(sectorsBeforeStorm);
        if (!sectorsAfterStorm.isEmpty()) returnList.add(sectorsAfterStorm);
        if (!sectorsUnderStorm.isEmpty()) returnList.add(sectorsUnderStorm);
        return returnList;
    }

    public Set<String> getFighterNamesInAggTerritory(List<Territory> territorySectors) {
        List<Force> forces = new ArrayList<>();
        boolean addRichese = false;
        for (Territory territory : territorySectors) {
            if (territorySectors.getFirst().getTerritoryName().equals("Polar Sink")) continue;
            forces.addAll(territory.getForces().stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                    .toList()
            );
            if (territory.hasRicheseNoField()) addRichese = true;
        }
        Set<String> factionNames = forces.stream()
                .map(Force::getFactionName)
                .collect(Collectors.toSet());
        if (addRichese) factionNames.add("Richese");
        return factionNames;
    }

    public void flipAdvisorsIfAlone(Game game) {
        values().stream().filter(t -> t.hasForce("Advisor")).forEach(t -> flipAdvisorsIfAlone(game, t));
    }

    public void flipAdvisorsIfAlone(Game game, Territory territory) {
        if (territory.hasForce("Advisor")) {
            List<List<Territory>> sectorsLists = getAggregateTerritoryList(territory.getAggregateTerritoryName(), game.getStorm(), true);
            List<Territory> connectedSectors = sectorsLists.stream().filter(sectors -> sectors.contains(territory)).findFirst().orElse(new ArrayList<>());
            if (getFighterNamesInAggTerritory(connectedSectors).isEmpty()) {
                BGFaction bg = game.getBGFactionOrNull();
                bg.flipForces(territory);
                game.getTurnSummary().publish(Emojis.BG_ADVISOR + " are alone in " + territory.getTerritoryName() + " and have flipped to " + Emojis.BG_FIGHTER);
            }
        }
    }

    public Territory getTerritoryWithTerrorToken(String terrorTokenName) {
        return values().stream().filter(t -> t.hasTerrorToken(terrorTokenName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Terror Token not found on map"));
    }

    public void removeTerrorTokenFromMap(Game game, String terrorTokenName, boolean returnToMoritani) {
        getTerritoryWithTerrorToken(terrorTokenName).removeTerrorToken(game, terrorTokenName, returnToMoritani);
    }

    public boolean isNotStronghold(String wholeTerritoryName){
        try {
            if (getTerritory(wholeTerritoryName).isStronghold())
                return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
        return true;
    }

    public List<String> getSpiceBlowTerritoryNames() {
        return List.of(
                "Habbanya Ridge Flat", "Cielago South", "Broken Land", "South Mesa", "Sihaya Ridge",
                "Hagga Basin", "Red Chasm", "The Minor Erg", "Cielago North", "Funeral Plain",
                "The Great Flat", "Habbanya Erg", "Old Gap", "Rock Outcroppings", "Wind Pass North"
        );
    }

    public List<String> getRockTerritoryNames() {
        return List.of("False Wall South", "Pasty Mesa", "False Wall East", "Shield Wall", "Rim Wall West", "Plastic Basin", "False Wall West");
    }

    public List<String> getNonSpiceNonRockTerritoryNames() {
        return List.of(
                "Polar Sink", "Cielago Depression", "Meridian", "Cielago East", "Harg Pass",
                "Gara Kulon", "Hole In The Rock", "Basin", "Imperial Basin", "Arsunt",
                "Tsimpo", "Bight Of The Cliff", "Wind Pass", "The Greater Flat", "Cielago West"
        );
    }
}
