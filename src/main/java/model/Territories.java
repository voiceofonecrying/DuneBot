package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Territories extends HashMap<String, Territory> {

    /**
     * Returns the territory with the given name.
     *
     * @param name The territory's name.
     * @return The territory with the given name.
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
        return values().stream().filter(t -> t.getTerritoryName().indexOf(aggregateTerritoryName) == 0).toList();
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
}
