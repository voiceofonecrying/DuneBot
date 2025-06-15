package model;

import constants.Emojis;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TerritoriesTest extends DuneTest {
    Territories territories;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        territories = game.getTerritories();
    }

    @Test
    void getTerritoryValidSectorName() {
        assertEquals(18, cielagoNorth_westSector.getSector());
    }

    @Test
    void getTerritoryInvalidSectorName() {
        assertThrows(IllegalArgumentException.class, () -> territories.getTerritory("Cielago North"));
    }

    @Nested
    @DisplayName("#handleStormMovement")
    class HandleStormMovement {
        @BeforeEach
        void setUp() {
            game.setStorm(16);
            game.setStormMovement(4);
        }

        @Test
        void testFremenLoseHalfForces() {
            game.addFaction(fremen);
            fremen.placeForceFromReserves(game, cielagoNorth_eastSector, 2, false);
            fremen.placeForceFromReserves(game, cielagoNorth_eastSector, 3, true);
            territories.moveStorm(game);
            assertEquals(Emojis.FREMEN + " lose 2 " + Emojis.FREMEN_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.FREMEN + " lose 1 " + Emojis.FREMEN_FEDAYKIN + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testOtherFactionLosesAllForces() {
            game.addFaction(emperor);
            emperor.placeForceFromReserves(game, cielagoNorth_eastSector, 2, false);
            emperor.placeForceFromReserves(game, cielagoNorth_eastSector, 3, true);
            territories.moveStorm(game);
            assertEquals(Emojis.EMPEROR + " lose 2 " + Emojis.EMPEROR_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.EMPEROR + " lose 3 " + Emojis.EMPEROR_SARDAUKAR + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testRicheseNoFieldRevealedAndForcesLost() {
            game.addFaction(richese);
            cielagoNorth_eastSector.setRicheseNoField(5);
            territories.moveStorm(game);
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Cielago North (East Sector) reveals 5 " + Emojis.RICHESE_TROOP, turnSummary.getMessages().getFirst());
            assertEquals(Emojis.RICHESE + " lose 5 " + Emojis.RICHESE_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testSpiceBlownAway() {
            cielagoNorth_eastSector.setSpice(6);
            territories.moveStorm(game);
            assertEquals("6 " + Emojis.SPICE + " in Cielago North (East Sector) was blown away by the storm.", turnSummary.getMessages().getLast());
        }

        @Test
        void testAmbassadorsReturnedToSupply() {
            game.addFaction(ecaz);
            cielagoNorth_eastSector.setEcazAmbassador("BG");
            territories.moveStorm(game);
            assertEquals(Emojis.ECAZ + " BG Ambassador was removed from Cielago North (East Sector) and returned to supply.", turnSummary.getMessages().getLast());
        }

        @Test
        void testTerrorTokensRemainOnMap() throws InvalidGameStateException {
            cielagoNorth_eastSector.addTerrorToken(game, "Sabotage");
            territories.moveStorm(game);
            assertTrue(cielagoNorth_eastSector.hasTerrorToken("Sabotage"));
        }

        @Test
        void testDiscoveryTokensRemainOnMap() {
            meridian_westSector.setDiscoveryToken("Cistern");
            meridian_westSector.setDiscovered(false);
            territories.moveStorm(game);
            assertEquals("Cistern", meridian_westSector.getDiscoveryToken());
        }
    }

    @Test
    void getDistinctAggregateTerritoryNames() {
        assertEquals(42, territories.getDistinctAggregateTerritoryNames().size());
    }

    @Test
    void getDistinctAggregateTerritoryNamesWithIx() {
        game.addFaction(ix);
        assertEquals(44, territories.getDistinctAggregateTerritoryNames().size());
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
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Wind Pass North", 10, false);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
    }

    @Test
    void getAggregateTerritoryListWindPassNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Wind Pass", 10, false);
        assertEquals(1, territorySectorsList.size());
        assertEquals(4, territorySectorsList.getFirst().size());
    }

    @Test
    void getAggregateTerritoryListMeridianNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Meridian", 9, false);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().contains(meridian_westSector));
        assertTrue(territorySectorsList.getFirst().contains(meridian_eastSector));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Meridian (West Sector)")));
        assertTrue(territorySectorsList.getFirst().stream().anyMatch(t -> t.getTerritoryName().equals("Meridian (East Sector)")));
    }

    @Test
    void getAggregateTerritoryListCielagoNoStorm() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 10, false);
        assertEquals(1, territorySectorsList.size());
        assertEquals(3, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_westSector));
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_middleSector));
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_eastSector));
    }

    @Test
    void getAggregateTerritoryListCielagoStormEast() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 2, false);
        assertEquals(1, territorySectorsList.size());
        assertEquals(2, territorySectorsList.getFirst().size());
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_westSector));
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_middleSector));
        assertFalse(territorySectorsList.getFirst().contains(cielagoNorth_eastSector));
    }

    @Test
    void getAggregateTerritoryListCielagoStormCenter() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("Cielago North", 1, false);
        assertEquals(2, territorySectorsList.size());
        assertEquals(1, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.getFirst().contains(cielagoNorth_westSector));
        assertFalse(territorySectorsList.getFirst().contains(cielagoNorth_middleSector));
        assertTrue(territorySectorsList.getLast().contains(cielagoNorth_eastSector));
        assertFalse(territorySectorsList.getLast().contains(cielagoNorth_middleSector));
    }

    @Test
    void getAggregateTerritoryListFWEStormNorth() {
        List<List<Territory>> territorySectorsList = territories.getAggregateTerritoryList("False Wall East", 7, false);
        assertEquals(2, territorySectorsList.size());
        assertEquals(3, territorySectorsList.get(0).size());
        assertEquals(1, territorySectorsList.get(1).size());
        assertTrue(territorySectorsList.getFirst().contains(falseWallEast_farSouthSector));
        assertTrue(territorySectorsList.getFirst().contains(falseWallEast_southSector));
        assertTrue(territorySectorsList.getFirst().contains(falseWallEast_middleSector));
        assertTrue(territorySectorsList.getLast().contains(falseWallEast_farNorthSector));
    }

    @Test
    void fighterNamesInAggTerritoryTwoFighters() {
        cielagoNorth_westSector.addForces("Fremen", 1);
        cielagoNorth_eastSector.addForces("Atreides", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(2, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAndAdvisor() {
        cielagoNorth_westSector.addForces("Fremen", 1);
        cielagoNorth_eastSector.addForces("Advisor", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAlone() {
        cielagoNorth_westSector.addForces("Fremen", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFighterAloneWithSpecial() {
        cielagoNorth_westSector.addForces("Fremen", 1);
        cielagoNorth_eastSector.addForces("Fremen*", 1);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(1, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryRicheseNoField() {
        cielagoNorth_westSector.addForces("Fremen", 1);
        cielagoNorth_eastSector.addForces("BG", 1);
        cielagoNorth_middleSector.setRicheseNoField(3);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Cielago North", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(3, fighterNames.size());
    }

    @Test
    void noFighterNamesInPolarSink() {
        polarSink.addForces("Fremen", 1);
        polarSink.addForces("BG", 1);
        polarSink.setRicheseNoField(3);
        List<Territory> territorySectors = territories.getAggregateTerritoryList("Polar Sink", 10, false).getFirst();
        Set<String> fighterNames = territories.getFighterNamesInAggTerritory(territorySectors);
        assertEquals(0, fighterNames.size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm() {
        cielagoNorth_westSector.addForces("Fremen", 3);
        cielagoNorth_eastSector.addForces("BG", 1);
        List<Territory> territorySectorsBefore = territories.getAggregateTerritoryList("Cielago North", 1, false).getFirst();
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsBefore).size());
    }

    @Test
    void fighterNamesInAggTerritoryFightersSeparatedByStorm2() {
        cielagoNorth_westSector.addForces("Fremen", 3);
        cielagoNorth_eastSector.addForces("BG", 1);
        List<Territory> territorySectorsAfter = territories.getAggregateTerritoryList("Cielago North", 1, false).get(1);
        assertEquals(1, territories.getFighterNamesInAggTerritory(territorySectorsAfter).size());
    }

    @Test
    void fighterNamesInAggTerritoryUnderTheStorm() {
        cielagoNorth_middleSector.addForces("Fremen", 3);
        cielagoNorth_middleSector.addForces("BG", 1);
        List<List<Territory>> territorySectorsUnder = territories.getAggregateTerritoryList("Cielago North", 1, true);
        assertEquals(3, territorySectorsUnder.size());
        assertEquals(2, territories.getFighterNamesInAggTerritory(territorySectorsUnder.getLast()).size());
    }

    @Nested
    @DisplayName("#flipAdvisorsIfAlone")
    class FlipAdvisorsIfAlone {
        @BeforeEach
        void setUp() {
            game.addFaction(bg);
            game.addFaction(harkonnen);
        }

        @Test
        void testAdvisorsAlone() {
            sietchTabr.addForces("Advisor", 1);
            territories.flipAdvisorsIfAlone(game);
            assertTrue(sietchTabr.hasForce("BG"));
            assertFalse(sietchTabr.hasForce("Advisor"));
            assertEquals(Emojis.BG_ADVISOR + " are alone in Sietch Tabr and have flipped to " + Emojis.BG_FIGHTER, turnSummary.getMessages().getFirst());
        }

        @Test
        void testAdvisorsNotAlone() {
            carthag.addForces("Advisor", 1);
            territories.flipAdvisorsIfAlone(game);
            assertFalse(carthag.hasForce("BG"));
            assertTrue(carthag.hasForce("Advisor"));
            assertTrue(turnSummary.getMessages().isEmpty());
        }

        @Test
        void testAdvisorsNotAloneIfUnderStormTogether() {
            game.setStorm(10);
            carthag.addForces("Advisor", 1);
            territories.flipAdvisorsIfAlone(game);
            assertFalse(carthag.hasForce("BG"));
            assertTrue(carthag.hasForce("Advisor"));
            assertTrue(turnSummary.getMessages().isEmpty());
        }

        @Test
        void testAdvisorsNotAloneMultipleSectors() {
            Territory shieldWallNorth = game.getTerritory("Shield Wall (North Sector)");
            Territory shieldWallSouth = game.getTerritory("Shield Wall (South Sector)");
            shieldWallNorth.addForces("Advisor", 1);
            shieldWallSouth.addForces("Harkonnen", 1);
            territories.flipAdvisorsIfAlone(game);
            assertFalse(shieldWallNorth.hasForce("BG"));
            assertTrue(shieldWallNorth.hasForce("Advisor"));
            assertTrue(turnSummary.getMessages().isEmpty());
        }

        @Test
        void testAloneSeparatedByStorm() {
            game.setStorm(1);
            cielagoNorth_westSector.addForces("Advisor", 1);
            cielagoNorth_eastSector.addForces("Harkonnen", 1);
            territories.flipAdvisorsIfAlone(game);
            assertTrue(cielagoNorth_westSector.hasForce("BG"));
            assertFalse(cielagoNorth_westSector.hasForce("Advisor"));
            assertEquals(Emojis.BG_ADVISOR + " are alone in Cielago North (West Sector) and have flipped to " + Emojis.BG_FIGHTER, turnSummary.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#validStrongholdForStrongholdShippingButtonsAndHMS")
    class ValidStrongholdForStrongholdShippingButtonsAndHMS {
        @Test
        void testNoHMS() {
            assertEquals(5, territories.values().stream().filter(t -> t.isValidStrongholdForShipmentFremenRideAndBTHT(bt, true)).count());
        }

        @Test
        void testHMSIxInGame() {
            game.addFaction(ix);
            assertEquals(6, territories.values().stream().filter(t -> t.isValidStrongholdForShipmentFremenRideAndBTHT(bt, true)).count());
        }
    }

    @Nested
    @DisplayName("#getTerritoryWithTerrorToken")
    class GetTerritoryWithTerrorToken {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(moritani);
            moritani.placeTerrorToken(sietchTabr, "Robbery");
        }

        @Test
        void testTerrorTokenOnMap() {
            assertEquals(sietchTabr, territories.getTerritoryWithTerrorToken("Robbery"));
        }

        @Test
        void testTerrorTokenNotFound() {
            assertThrows(IllegalArgumentException.class, () -> territories.getTerritoryWithTerrorToken("Sabotage"));
        }
    }

    @Nested
    @DisplayName("#removeTerrorTokenFromMap")
    class RemoveTerrorTokenFromMap {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(moritani);
            moritani.placeTerrorToken(sietchTabr, "Robbery");
            assertTrue(sietchTabr.hasTerrorToken());
            moritani.getUpdateTypes().clear();
            game.getUpdateTypes().clear();
        }

        @Test
        void testTerrorTokenRemoved() {
            territories.removeTerrorTokenFromMap(game, "Robbery", false);
            assertFalse(sietchTabr.hasTerrorToken());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
            assertFalse(moritani.getTerrorTokens().contains("Robbery"));
            assertFalse(moritani.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
        }

        @Test
        void testTerrorTokenRemovedAndReturnedToSupply() {
            territories.removeTerrorTokenFromMap(game, "Robbery", true);
            assertFalse(sietchTabr.hasTerrorToken());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
            assertTrue(moritani.getTerrorTokens().contains("Robbery"));
            assertTrue(moritani.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
        }

        @Test
        void testTerrorTokenNotFound() {
            assertThrows(IllegalArgumentException.class, () -> territories.removeTerrorTokenFromMap(game, "Sabotage", false));
        }
    }
}
