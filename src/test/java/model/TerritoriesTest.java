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
    void getTerritorySectorsForBattleCielagoNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getTerritorySectorsForBattle("Cielago North", 10);
        assertEquals(1, territorySectorsList.size());
        assertEquals(3, territorySectorsList.get(0).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (Center Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (East Sector)")));
    }

    @Test
    void getTerritorySectorsForBattleCielagoStormEast() {
        List<List<Territory>> territorySectorsList = territories.getTerritorySectorsForBattle("Cielago North", 2);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.get(0).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (Center Sector)")));
    }

    @Test
    void getTerritorySectorsForBattleCielagoStormCenter() {
        List<List<Territory>> territorySectorsList = territories.getTerritorySectorsForBattle("Cielago North", 1);
        assertEquals(2, territorySectorsList.size());
        assertEquals(1, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (West Sector)")));
        assertTrue(territorySectorsList.get(1).stream().anyMatch(t -> t.getTerritoryName().equals("Cielago North (East Sector)")));
    }

    @Test
    void getTerritorySectorsForBattleFWEStormNorth() {
        List<List<Territory>> territorySectorsList = territories.getTerritorySectorsForBattle("False Wall East", 7);
        assertEquals(2, territorySectorsList.size());
        assertEquals(3, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Far South Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (South Sector)")));
        assertTrue(territorySectorsList.get(0).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Middle Sector)")));
        assertTrue(territorySectorsList.get(1).stream().anyMatch(t -> t.getTerritoryName().equals("False Wall East (Far North Sector)")));
    }

    @Test
    void fighterNamesInAggTerritoryTwoFighers() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 1, "Fremen"));
        game.getTerritory("Cielago North (East Sector)").addForce(new Force("Atreides", 1, "Atreides"));
        List<Territory> territorySectors = territories.getTerritorySectorsForBattle("Cielago North", 10).get(0);
        Set<String> fighters = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(2, fighters.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAndAdvisor() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 1, "Fremen"));
        game.getTerritory("Cielago North (East Sector)").addForce(new Force("Advisor", 1, "BG"));
        List<Territory> territorySectors = territories.getTerritorySectorsForBattle("Cielago North", 10).get(0);
        Set<String> fighters = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighters.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAlone() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 1, "Fremen"));
        List<Territory> territorySectors = territories.getTerritorySectorsForBattle("Cielago North", 10).get(0);
        Set<String> fighters = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighters.size());
    }

    @Test
    void fighterNamesInAggTerritoryRicheseNoField() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 1, "Fremen"));
        game.getTerritory("Cielago North (East Sector)").addForce(new Force("BG", 1, "BG"));
        game.getTerritory("Cielago North (Center Sector)").setRicheseNoField(3);
        List<Territory> territorySectors = territories.getTerritorySectorsForBattle("Cielago North", 10).get(0);
        Set<String> fighters = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(3, fighters.size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 3, "Fremen"));
        game.getTerritory("Cielago North (East Sector)").addForce(new Force("BG", 1, "BG"));
        List<Territory> territorySectorsBefore = territories.getTerritorySectorsForBattle("Cielago North", 1).get(0);
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsBefore).size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm2() {
        game.getTerritory("Cielago North (West Sector)").addForce(new Force("Fremen", 3, "Fremen"));
        game.getTerritory("Cielago North (East Sector)").addForce(new Force("BG", 1, "BG"));
        List<Territory> territorySectorsAfter = territories.getTerritorySectorsForBattle("Cielago North", 1).get(1);
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsAfter).size());
    }
}
