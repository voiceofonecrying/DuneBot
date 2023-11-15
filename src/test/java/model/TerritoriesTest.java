package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

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
    void getTerritory1() {
        assertEquals(18, territories.getTerritory("Cielago North (West Sector)").getSector());
    }

    @Test
    void getTerritory2() {
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
}
