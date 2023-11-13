package model;

import constants.Emojis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerritoryTest {
    private Game game;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
    }

    @Nested
    @DisplayName("#stormRemoveSpice")
    class StormRemoveSpice {
        @Test
        void testSpiceIsRemoved() {
            Territory sihayaRidge = game.getTerritory("Sihaya Ridge");
            sihayaRidge.setSpice(6);
            String response = sihayaRidge.stormRemoveSpice();

            assertEquals(0, sihayaRidge.getSpice());
            assertEquals(MessageFormat.format("6 {0} in Sihaya Ridge was blown away by the storm\n", Emojis.SPICE), response);
        }
    }
}
