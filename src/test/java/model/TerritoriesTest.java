package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TerritoriesTest {
    Game game;
    Territories territories;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        territories = game.getTerritories();
    }

    @Test
    void getTerritoryValidSectorName() {
        assertEquals(18, territories.getTerritory("Cielago North (West Sector)").getSector());
    }

    @Test
    void getTerritoryInvalidSectorName() {
        assertThrows(IllegalArgumentException.class, () -> territories.getTerritory("Cielago North"));
    }

    @Test
    void getDistinctAggregateTerritoryNames() {
        assertEquals(43, territories.getDistinctAggregateTerritoryNames().size());
    }

    @Test
    void getTerritorySectors() {
        List<Territory> territorySectorsList = territories.getTerritorySectors("Cielago North");
        assertEquals(3, territorySectorsList.size());
    }

    @Test
    void getTerritorySectorsWindPass() {
        List<Territory> territorySectorsList = territories.getTerritorySectors("Wind Pass");
        assertEquals(4, territorySectorsList.size());
    }

    @Test
    void getAggregateTerritoryListWindPassNorthNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Wind Pass North", 10);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
    }

    @Test
    void getAggregateTerritoryListWindPassNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Wind Pass", 10);
        assertEquals(1, territorySectorsList.size());
        assertEquals(4, territorySectorsList.getFirst().size());
    }

    @Test
    void getAggregateTerritoryListMeridianNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Meridian", 9);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Meridian (West Sector)")));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Meridian (East Sector)")));
    }

    @Test
    void getAggregateTerritoryListCielagoNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 10);
        assertEquals(1, territorySectorsList.size());
        assertEquals(3, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (Center Sector)")));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (East Sector)")));
    }

    @Test
    void getAggregateTerritoryListCielagoStormEast() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 2);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (Center Sector)")));
    }

    @Test
    void getAggregateTerritoryListCielagoStormCenter() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 1);
        assertEquals(2, territorySectorsList.size());
        assertEquals(1, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.get(1).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (East Sector)")));
    }

    @Test
    void getAggregateTerritoryListFWEStormNorth() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("False Wall East", 7);
        assertEquals(2, territorySectorsList.size());
        assertEquals(3, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Far South Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (South Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Middle Sector)")));
        assertTrue(territorySectorsList.get(1).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Far North Sector)")));
    }

    @Test
    void fighterNamesInAggTerritoryTwoFighters() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 1);
        game.getTerritory("Cielago North (East Sector)").addForces("Atreides", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(2, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAndAdvisor() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 1);
        game.getTerritory("Cielago North (East Sector)").addForces("Advisor", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAlone() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAloneWithSpecial() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 1);
        game.getTerritory("Cielago North (East Sector)").addForces("Fremen*", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryRicheseNoField() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 1);
        game.getTerritory("Cielago North (East Sector)").addForces("BG", 1);
        game.getTerritory("Cielago North (Center Sector)").setRicheseNoField(3);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(3, fighterNames.size());
    }

    @Test
    void noFighterNamesInPolarSink() {
        game.getTerritory("Polar Sink").addForces("Fremen", 1);
        game.getTerritory("Polar Sink").addForces("BG", 1);
        game.getTerritory("Polar Sink").setRicheseNoField(3);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Polar Sink", 10).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(0, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 3);
        game.getTerritory("Cielago North (East Sector)").addForces("BG", 1);
        List<Territory> territorySectorsBefore = territories.getAggregateTerritoryList("Cielago North", 1).getFirst();
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsBefore).size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm2() {
        game.getTerritory("Cielago North (West Sector)").addForces("Fremen", 3);
        game.getTerritory("Cielago North (East Sector)").addForces("BG", 1);
        List<Territory> territorySectorsAfter = territories.getAggregateTerritoryList("Cielago North", 1).get(1);
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsAfter).size());
    }
}
