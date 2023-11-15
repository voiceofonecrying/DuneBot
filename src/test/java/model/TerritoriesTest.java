package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerritoriesTest {
    Game game;
    Territories territories;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        territories = game.getTerritories();
    }

    @Test
    void getDistinctAggregateTerritoryNames() {
        assertEquals(43, territories.getDistinctAggregateTerritoryNames().size());
    }
}
