package model;

import constants.Emojis;
import model.factions.AtreidesFaction;
import model.factions.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TerritoryTest {
    private Game game;
    Territory sihayaRidge;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        sihayaRidge = game.getTerritory("Sihaya Ridge");
    }

    @Nested
    @DisplayName("#stormRemoveTroops")
    class StormRemoveTroops {
        String response;

        @BeforeEach
        void setUp() throws IOException {
            Faction atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            game.addFaction(atreides);
            sihayaRidge.setSpice(6);
            Force atreidesForce = new Force("Atreides", 4);
            sihayaRidge.addForce(atreidesForce);
            Force force = sihayaRidge.getForce("Atreides");
            response = sihayaRidge.stormRemoveTroops(force, 4, game);
        }

        @Test
        void testTroopCount() {
            assertFalse(sihayaRidge.getForces().stream().anyMatch(f -> f.getFactionName().equals("Atreides")));
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose 4 {1} to the storm in Sihaya Ridge\n", Emojis.ATREIDES, Emojis.ATREIDES_TROOP), response);
        }
    }

    @Nested
    @DisplayName("#stormRemoveSpice")
    class StormRemoveSpice {
        @Test
        void testSpiceIsRemoved() {
            sihayaRidge.setSpice(6);
            String response = sihayaRidge.stormRemoveSpice();

            assertEquals(0, sihayaRidge.getSpice());
            assertEquals(MessageFormat.format("6 {0} in Sihaya Ridge was blown away by the storm\n", Emojis.SPICE), response);
        }
    }
}
