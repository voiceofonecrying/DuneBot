package model;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class Territories extends HashMap<String, Territory> {

    public Set<String> getDistinctAggregateTerritoryNames() {
        return values().stream().map(Territory::getAggregateTerritoryName).collect(Collectors.toSet());
    }
}
