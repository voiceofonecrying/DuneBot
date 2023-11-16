package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Territories extends HashMap<String, Territory> {

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

    public Set<String> getDistinctAggregateTerritoryNames() {
        return values().stream().map(Territory::getAggregateTerritoryName).collect(Collectors.toSet());
    }

    public List<Territory> getTerritorySectors(String aggregateTerritoryName){
        if (aggregateTerritoryName.equals("Wind Pass")) return values().stream().filter(t -> t.getTerritoryName().indexOf("Wind Pass (") == 0).toList();
        else return values().stream().filter(t -> t.getTerritoryName().indexOf(aggregateTerritoryName) == 0).toList();
    }

    public List<List<Territory>> getTerritorySectorsForBattle(String aggregateTerritoryName, int storm) {
        List<Territory> territorySectors = getTerritorySectors(aggregateTerritoryName);
        List<Territory> sectorsBeforeStorm;
        List<Territory> sectorsAfterStorm;
        if (storm != 1 && (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression"))) {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() != storm).toList();
            sectorsAfterStorm = new ArrayList<>();
        } else if (storm == 1 && (aggregateTerritoryName.equals("Cielago North") || aggregateTerritoryName.equals("Cielago Depression"))) {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() == 18).toList();
            sectorsAfterStorm = territorySectors.stream().filter(t -> t.getSector() == 2).toList();
        } else {
            sectorsBeforeStorm = territorySectors.stream().filter(t -> t.getSector() < storm).toList();
            sectorsAfterStorm = territorySectors.stream().filter(t -> t.getSector() > storm).toList();
        }
        List<List<Territory>> returnList = new ArrayList<>();
        if (!sectorsBeforeStorm.isEmpty()) returnList.add(sectorsBeforeStorm);
        if (!sectorsAfterStorm.isEmpty()) returnList.add(sectorsAfterStorm);
        return returnList;
    }

    public Set<String> getFighterNamesInAggTerritory(List<Territory> territorySectors) {
        List<Force> forces = new ArrayList<>();
        boolean addRichese = false;
        for (Territory territory : territorySectors) {
            if (territorySectors.get(0).getTerritoryName().equals("Polar Sink")) continue;
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
}
