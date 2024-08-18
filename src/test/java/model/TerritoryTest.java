package model;

import constants.Emojis;
import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerritoryTest {
    private Game game;
    Territory arrakeen;
    Territory sihayaRidge;
    Territory cielagoNorth_westSector;
    Territory cielagoNorth_middleSector;
    Territory windPassNorth_northSector;
    Territory windPass_northSector;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        arrakeen = game.getTerritory("Arrakeen");
        sihayaRidge = game.getTerritory("Sihaya Ridge");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        cielagoNorth_middleSector = game.getTerritory("Cielago North (Center Sector)");
        windPassNorth_northSector = game.getTerritory("Wind Pass North (North Sector)");
        windPass_northSector = game.getTerritory("Wind Pass (North Sector)");
    }

    @Nested
    @DisplayName("#getFactionCount")
    class GetFactionCount {
        Faction fremen;
        Faction atreides;
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            fremen = new FremenFaction("fakePlayer1", "userName1", game);
            game.addFaction(fremen);
            atreides = new AtreidesFaction("fakePlayer2", "userName2", game);
            game.addFaction(atreides);
        }

        @Test
        void fremenForceOnly() {
            sihayaRidge.addForces("Fremen", numFremen);
            assertEquals(1, sihayaRidge.countFactions());
        }

        @Test
        void fedaykinOnly() {
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(1, sihayaRidge.countFactions());
        }

        @Test
        void fremenAndFedaykin() {
            sihayaRidge.addForces("Fremen", numFremen);
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(1, sihayaRidge.countFactions());
        }

        @Test
        void fremenAndAtreides() {
            arrakeen.addForces("Fremen", numFremen);
            arrakeen.addForces("Fremen*", numFedaykin);
            assertEquals(2, arrakeen.countFactions());
        }
    }

    @Nested
    @DisplayName("#getTotalForceCount")
    class GetTotalForceCount {
        Faction fremen;
        Faction atreides;
        Faction bg;
        Faction richese;
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            fremen = new FremenFaction("fakePlayer1", "userName1", game);
            game.addFaction(fremen);
            atreides = new AtreidesFaction("fakePlayer2", "userName2", game);
            game.addFaction(atreides);
            bg = new BGFaction("fakePlayer3", "userName3", game);
            game.addFaction(bg);
            richese = new RicheseFaction("fakePlayer4", "userName4", game);
            game.addFaction(richese);
        }

        @Test
        void fremenForceOnly() {
            sihayaRidge.addForces("Fremen", numFremen);
            assertEquals(numFremen, sihayaRidge.getTotalForceCount(fremen));
        }

        @Test
        void fedaykinOnly() {
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(numFedaykin, sihayaRidge.getTotalForceCount(fremen));
        }

        @Test
        void fremenAndFedaykin() {
            sihayaRidge.addForces("Fremen", numFremen);
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(numFremen + numFedaykin, sihayaRidge.getTotalForceCount(fremen));
        }

        @Test
        void factionWithoutStars() {
            assertEquals(10, arrakeen.getTotalForceCount(atreides));
        }

        @Test
        void noFieldIsCounted() {
            sihayaRidge.setRicheseNoField(3);
            assertEquals(1, sihayaRidge.getTotalForceCount(richese));
            sihayaRidge.addForces("Richese", 2);
            assertEquals(3, sihayaRidge.getTotalForceCount(richese));
        }

        @Test
        void bgAdvisorsAreCounted() {
            sihayaRidge.addForces("Advisor", 1);
            assertEquals(1, sihayaRidge.getTotalForceCount(bg));
        }
    }

    @Nested
    @DisplayName("#stormTroopsFremen")
    class StormTroopsFremen {
        String response;
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            Faction fremen = new FremenFaction("fakePlayer1", "userName1", game);
            game.addFaction(fremen);
            sihayaRidge.setSpice(6);
            sihayaRidge.addForces("Fremen", numFremen);
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(numFremen, sihayaRidge.getForceStrength("Fremen"));
            assertEquals(numFedaykin, sihayaRidge.getForceStrength("Fremen*"));
            response = sihayaRidge.stormTroopsFremen(game);
        }

        @Test
        void testTroopCount() {
            assertEquals(1, sihayaRidge.getForceStrength("Fremen"));
        }

        @Test
        void testFedaykinCount() {
            assertEquals(2, sihayaRidge.getForceStrength("Fremen*"));
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose 3 {1} to the storm in Sihaya Ridge.\n", Emojis.FREMEN, Emojis.FREMEN_TROOP), response);
        }
    }

    @Nested
    @DisplayName("#stormRemoveTroops")
    class StormRemoveTroops {
        String response;
        final int numForces = 4;

        @BeforeEach
        void setUp() throws IOException {
            Faction atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            game.addFaction(atreides);
            sihayaRidge.setSpice(6);
            sihayaRidge.addForces("Atreides", numForces);
            assertEquals(numForces, sihayaRidge.getForceStrength("Atreides"));
            response = sihayaRidge.stormRemoveTroops("Atreides", "Atreides", numForces, game);
            assertEquals("", sihayaRidge.stormTroopsFremen(game));
        }

        @Test
        void testTroopCount() {
            assertEquals(0, sihayaRidge.getForceStrength("Atreides"));
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose {1} {2} to the storm in Sihaya Ridge.\n", Emojis.ATREIDES, numForces, Emojis.ATREIDES_TROOP), response);
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
            assertEquals(MessageFormat.format("6 {0} in Sihaya Ridge was blown away by the storm.\n", Emojis.SPICE), response);
        }
    }

    @Nested
    @DisplayName("#getAggregateTerritoryName")
    class GetAggregateTerritoryName {
        @Test
        void testSihayaRidge() {
            assertEquals("Sihaya Ridge", sihayaRidge.getAggregateTerritoryName());
        }

        @Test
        void testCielagoNorth_westSector() {
            assertEquals("Cielago North", cielagoNorth_westSector.getAggregateTerritoryName());
        }

        @Test
        void testCielagoSectorsMatch() {
            assertEquals("Wind Pass North", windPassNorth_northSector.getAggregateTerritoryName());
        }

        @Test
        void testWindPassNorth() {
            assertEquals("Wind Pass", windPass_northSector.getAggregateTerritoryName());
        }
    }

    @Nested
    @DisplayName("#costToShipInto")
    class CostToShip {
        @Test
        void testStronghold() {
            Territory carthag = game.getTerritory("Carthag");
            assertEquals(1, carthag.costToShipInto());
        }

        @Test
        void testNonStronghold() {
            assertEquals(2, sihayaRidge.costToShipInto());
        }

        @Test
        void testHomeworlds() {
            Territory caladan = new HomeworldTerritory(game, "Caladan", "Atreides");
            assertEquals(1, caladan.costToShipInto());
        }

        @Test
        void testCistern() {
            Territory cistern = new Territory("Cistern", 1, true, false, true, false);
            assertEquals(1, cistern.costToShipInto());
        }

        @Test
        void testEcologicalTestingStation() {
            Territory ecologicalTestingStation = new Territory("Ecological Testing Station", 1, true, false, true, false);
            assertEquals(1, ecologicalTestingStation.costToShipInto());
        }

        @Test
        void testJacurutuSietch() {
            Territory jacurutuSietch = new Territory("Jacurutu Sietch", 1, true, true, true, false);
            assertEquals(1, jacurutuSietch.costToShipInto());
        }

        @Test
        void testOrgizProcessingStation() {
            Territory orgizProcessingStation = new Territory("Orgiz Processing Station", 1, true, false, true, false);
            assertEquals(1, orgizProcessingStation.costToShipInto());
        }

        @Test
        void testShrine() {
            Territory shrine = new Territory("Shrine", 1, true, false, true, false);
            assertEquals(1, shrine.costToShipInto());
        }
    }
}
