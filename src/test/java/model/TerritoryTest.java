package model;

import constants.Emojis;
import model.factions.AtreidesFaction;
import model.factions.Faction;
import model.factions.FremenFaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TerritoryTest {
    private Game game;
    Territory sihayaRidge;
    Territory cielagoNorth_westSector;
    Territory cielagoNorth_middleSector;
    Territory windPassNorth_northSector;
    Territory windPass_northSector;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        sihayaRidge = game.getTerritory("Sihaya Ridge");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        cielagoNorth_middleSector = game.getTerritory("Cielago North (Center Sector)");
        windPassNorth_northSector = game.getTerritory("Wind Pass North (North Sector)");
        windPass_northSector = game.getTerritory("Wind Pass (North Sector)");
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
            Force fremenForce = new Force("Fremen", numFremen);
            Force fedaykinForce = new Force("Fremen*", numFedaykin);
            sihayaRidge.addForce(fremenForce);
            sihayaRidge.addForce(fedaykinForce);
            assertEquals(numFremen, sihayaRidge.getForce("Fremen").getStrength());
            assertEquals(numFedaykin, sihayaRidge.getForce("Fremen*").getStrength());
            response = sihayaRidge.stormTroopsFremen(List.of(fremenForce, fedaykinForce), game);
        }

        @Test
        void testTroopCount() {
            assertEquals(1, sihayaRidge.getForce("Fremen").getStrength());
        }

        @Test
        void testFedaykinCount() {
            assertEquals(2, sihayaRidge.getForce("Fremen*").getStrength());
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose 3 {1} to the storm in Sihaya Ridge\n", Emojis.FREMEN, Emojis.FREMEN_TROOP), response);
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
            Force atreidesForce = new Force("Atreides", numForces);
            sihayaRidge.addForce(atreidesForce);
            Force force = sihayaRidge.getForce("Atreides");
            assertEquals(numForces, sihayaRidge.getForce("Atreides").getStrength());
            response = sihayaRidge.stormRemoveTroops(force, numForces, game);
        }

        @Test
        void testTroopCount() {
            assertFalse(sihayaRidge.getForces().stream().anyMatch(f -> f.getFactionName().equals("Atreides")));
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose {1} {2} to the storm in Sihaya Ridge\n", Emojis.ATREIDES, numForces, Emojis.ATREIDES_TROOP), response);
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
            Territory caladan = new Territory("Caladan", -1, false, false, true, false, false);
            assertEquals(1, caladan.costToShipInto());
        }

        @Test
        void testCistern() {
            Territory cistern = new Territory("Cistern", 1, true, false, false, true, false);
            assertEquals(1, cistern.costToShipInto());
        }

        @Test
        void testEcologicalTestingStation() {
            Territory ecologicalTestingStation = new Territory("Ecological Testing Station", 1, true, false, false, true, false);
            assertEquals(1, ecologicalTestingStation.costToShipInto());
        }

        @Test
        void testJacurutuSietch() {
            Territory jacurutuSietch = new Territory("Jacurutu Sietch", 1, true, true, false, true, false);
            assertEquals(1, jacurutuSietch.costToShipInto());
        }

        @Test
        void testOrgizProcessingStation() {
            Territory orgizProcessingStation = new Territory("Orgiz Processing Station", 1, true, false, false, true, false);
            assertEquals(1, orgizProcessingStation.costToShipInto());
        }

        @Test
        void testShrine() {
            Territory shrine = new Territory("Shrine", 1, true, false, false, true, false);
            assertEquals(1, shrine.costToShipInto());
        }
    }
}
