package model;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.*;

public class TerritoryTest extends DuneTest {
    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @Nested
    @DisplayName("#getFactionCount")
    class GetFactionCount {
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
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
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(atreides);
            game.addFaction(bg);
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
    @DisplayName("#stormTroops")
    class StormTroops {
        @Test
        void testStormTroopsWithAdvisorPresent() {
            game.addFaction(atreides);
            game.addFaction(bg);
            arrakeen.addForces("Advisor", 1);
            assertDoesNotThrow(() -> arrakeen.stormTroops(game));
        }

        @Test
        void testStormTroopsInEmptyTerritory() {
            sietchTabr.stormTroops(game);
            assertTrue(turnSummary.getMessages().isEmpty());
        }

        @Test
        void testStormTroopsWithNoField() {
            game.addFaction(richese);
            sietchTabr.setRicheseNoField(5);
            sietchTabr.stormTroops(game);
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 5 " + Emojis.RICHESE_TROOP, turnSummary.getMessages().getFirst());
            assertEquals(Emojis.RICHESE + " lose 5 " + Emojis.RICHESE_TROOP + " to the storm in Sietch Tabr.", turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#stormTroopsFremen")
    class StormTroopsFremen {
        final int numFremen = 4;
        final int numFedaykin = 2;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            sihayaRidge.setSpice(6);
            sihayaRidge.addForces("Fremen", numFremen);
            sihayaRidge.addForces("Fremen*", numFedaykin);
            assertEquals(numFremen, sihayaRidge.getForceStrength("Fremen"));
            assertEquals(numFedaykin, sihayaRidge.getForceStrength("Fremen*"));
            sihayaRidge.stormTroopsFremen(game);
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
            assertEquals(MessageFormat.format("{0} lose 3 {1} to the storm in Sihaya Ridge.", Emojis.FREMEN, Emojis.FREMEN_TROOP), turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#stormRemoveTroops")
    class StormRemoveTroops {
        final int numForces = 4;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            sihayaRidge.setSpice(6);
            sihayaRidge.addForces("Atreides", numForces);
            assertEquals(numForces, sihayaRidge.getForceStrength("Atreides"));
            sihayaRidge.stormRemoveTroops("Atreides", "Atreides", numForces, game);
            assertEquals(1, turnSummary.getMessages().size());
            sihayaRidge.stormTroopsFremen(game);
            assertEquals(1, turnSummary.getMessages().size());
        }

        @Test
        void testTroopCount() {
            assertEquals(0, sihayaRidge.getForceStrength("Atreides"));
        }

        @Test
        void testTroopLossMessage()  {
            assertEquals(MessageFormat.format("{0} lose {1} {2} to the storm in Sihaya Ridge.", Emojis.ATREIDES, numForces, Emojis.ATREIDES_TROOP), turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#stormRemoveSpice")
    class StormRemoveSpice {
        @Test
        void testSpiceIsRemoved() {
            sihayaRidge.setSpice(6);
            sihayaRidge.stormRemoveSpice(game);

            assertEquals(0, sihayaRidge.getSpice());
            assertEquals(MessageFormat.format("6 {0} in Sihaya Ridge was blown away by the storm.", Emojis.SPICE), turnSummary.getMessages().getFirst());
        }

        @Test
        void testNoSpiceInTerritory() {
            sihayaRidge.stormRemoveSpice(game);
            assertTrue(turnSummary.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#stormRemoveAmbassador")
    class StormRemoveAmbassador {
        @Test
        void testAmbassadorIsReturnedToSupply() {
            game.addFaction(ecaz);
            sietchTabr.setEcazAmbassador("BG");
            sietchTabr.stormRemoveAmbassador(game);

            assertFalse(sietchTabr.hasEcazAmbassador());
            assertTrue(ecaz.getAmbassadorSupply().contains("BG"));
            assertEquals(Emojis.ECAZ + " BG Ambassador was removed from Sietch Tabr and returned to supply.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testNoAmbassadorInTerritory() {
            sietchTabr.stormRemoveAmbassador(game);
            assertTrue(turnSummary.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#shaiHuludAppears")
    class ShaiHuludAppears {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(atreides);
        }

        @Test
        void testFremenForcesNotKilled() {
            sihayaRidge.addForces("Fremen", 1);
            sihayaRidge.addForces("Fremen*", 1);
            assertFalse(sihayaRidge.shaiHuludAppears(game, "Shai-Hulud", true).contains("devoured"));
            assertEquals(1, sihayaRidge.getForceStrength("Fremen"));
            assertEquals(1, sihayaRidge.getForceStrength("Fremen*"));
        }

        @Test
        void testNonFremenForcesKilled() {
            sihayaRidge.addForces("Atreides", 1);
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Shai-Hulud", true).contains("devoured"));
            assertEquals(0, sihayaRidge.getForceStrength("Atreides"));
        }

        @Test
        void testAdvisorsKilled() {
            game.addFaction(bg);
            assertEquals(0, game.getTleilaxuTanks().getForceStrength("BG"));
            sihayaRidge.addForces("Advisor", 1);
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Shai-Hulud", true).contains("devoured"));
            assertEquals(0, sihayaRidge.getForceStrength("Advisor"));
            assertEquals(1, game.getTleilaxuTanks().getForceStrength("BG"));
        }

        @Test
        void testFremanAllyForcesNotKilled() {
            game.createAlliance(fremen, atreides);
            sihayaRidge.addForces("Atreides", 1);
            assertFalse(sihayaRidge.shaiHuludAppears(game, "Shai-Hulud", true).contains("devoured"));
            assertEquals(1, sihayaRidge.getForceStrength("Atreides"));
        }

        @Test
        void testFremenCanRideShaiHulud() {
            sihayaRidge.addForces("Fremen", 1);
            sihayaRidge.addForces("Fremen*", 2);
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Shai-Hulud", true).contains("After the Nexus, 1 " + Emojis.FREMEN_TROOP + " 2 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud!"));
//            assertEquals("Where would you like to ride to from Sihaya Ridge? p", fremenChat.getMessages().getFirst());
        }

        @Test
        void testFremenReservesCanRideGreatMaker() {
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Great Maker", true).contains("After the Nexus, 17 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!"));
//            assertEquals("Where would you like to ride to from Southern Hemisphere? p", fremenChat.getMessages().getFirst());
        }

        @Test
        void testFremenHasNoReservesForGreatMaker() {
            game.removeForces("Southern Hemisphere", fremen, 17, 3, true);
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Great Maker", true).contains(Emojis.FREMEN + " have no forces in reserves to ride Great Maker.\n"));
        }

        @Test
        void testGreatMakerFirst() {
            assertTrue(sihayaRidge.shaiHuludAppears(game, "Great Maker", true).contains("Great Maker has been spotted in Sihaya Ridge!\n"));
        }

        @Test
        void testGreatMakerNotFirst() {
            assertFalse(sihayaRidge.shaiHuludAppears(game, "Great Maker", false).contains("Great Maker has been spotted"));
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

    @Nested
    @DisplayName("#factionMayNotEnter")
    class FactionMayNotEnter {
        Territory hms;

        @BeforeEach
        void setUp() {
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(bg);
            game.addFaction(ix);
            hms = game.getTerritory("Hidden Mobile Stronghold");
            game.addFaction(ecaz);
            game.addFaction(richese);

            game.createAlliance(bg, ecaz);
        }

        @Test
//        void testFactionMayNotShipIntoTerritoryWithAlly() {
        void testFactionMayShipIntoTerritoryWithAlly() {
            assertFalse(arrakeen.factionMayNotEnter(game, richese, true, false));
            game.createAlliance(atreides, richese);
//            assertTrue(arrakeen.factionMayNotEnter(game, richese, true, false));
            assertFalse(arrakeen.factionMayNotEnter(game, richese, true, false));
        }

        @Test
        void testFactionMayNotMoveIntoTerritoryWithAlly() {
            assertFalse(arrakeen.factionMayNotEnter(game, richese, false, false));
            game.createAlliance(atreides, richese);
            assertTrue(arrakeen.factionMayNotEnter(game, richese, false, false));
        }

        @Test
        void testFactionMayNotShipIntoStrongholdWithTwoFactionsPresent() {
            arrakeen.addForces("BG", 1);
            assertTrue(arrakeen.factionMayNotEnter(game, richese, true, false));
        }

        @Test
        void testFactionMayNotMoveIntoStrongholdWithTwoFactionsPresent() {
            arrakeen.addForces("BG", 1);
            assertTrue(arrakeen.factionMayNotEnter(game, richese, false, false));
        }

        @Test
        void testFactionMayNotShipIntoStrongholdWithTwoFactionsPresentWithNoField() {
            arrakeen.setRicheseNoField(0);
            assertTrue(arrakeen.factionMayNotEnter(game, bg, true, false));
        }

        @Test
        void testFactionMayNotMoveIntoStrongholdWithTwoFactionsPresentWithNoField() {
            arrakeen.setRicheseNoField(0);
            assertTrue(arrakeen.factionMayNotEnter(game, bg, false, false));
        }

        @Test
        void testEcazMayShipIntoTerritoryWithEcazAlly() {
            carthag.addForces("BG", 1);
            assertFalse(carthag.factionMayNotEnter(game, ecaz, true, false));
        }

        @Test
        void testEcazMayMoveIntoTerritoryWithEcazAlly() {
            carthag.addForces("BG", 1);
            assertFalse(carthag.factionMayNotEnter(game, ecaz, false, false));
        }

        @Test
        void testEcazAllyMayShipIntoTerritoryWithEcaz() {
            carthag.addForces("Ecaz", 1);
            assertFalse(carthag.factionMayNotEnter(game, bg, true, false));
        }

        @Test
        void testEcazAllyMayMoveIntoTerritoryWithEcaz() {
            carthag.addForces("Ecaz", 1);
            assertFalse(carthag.factionMayNotEnter(game, bg, false, false));
        }


        @Test
        void testThirdFactionMayShipIntoTerritoryWithEcazAlly() {
            sietchTabr.addForces("Ecaz", 1);
            sietchTabr.addForces("BG", 1);
            assertFalse(sietchTabr.factionMayNotEnter(game, atreides, true, false));
        }
        @Test
        void testThirdFactionMayMoveIntoTerritoryWithEcazAlly() {
            sietchTabr.addForces("Ecaz", 1);
            sietchTabr.addForces("BG", 1);
            assertFalse(sietchTabr.factionMayNotEnter(game, atreides, false, false));
        }

        @Test
        void testIxMayShipIntoHMS() {
            assertFalse(hms.factionMayNotEnter(game, ix, true, false));
        }

        @Test
        void testNonIxMayNotShipIntoHMS() {
            assertTrue(hms.factionMayNotEnter(game, atreides, true, false));
        }

        @Test
        void testNonIxMayMoveIntoHMS() {
            assertFalse(hms.factionMayNotEnter(game, atreides, false, false));
        }

        @Test
        void testFactionMayNotShipIntoAftermath() {
            arrakeen.setAftermathToken(true);
            assertTrue(arrakeen.factionMayNotEnter(game, atreides, true, false));
        }

        @Test
        void testFactionMayMoveIntoAftermath() {
            arrakeen.setAftermathToken(true);
            assertFalse(arrakeen.factionMayNotEnter(game, atreides, false, false));
        }
    }

    @Nested
    @DisplayName("#factionMustMoveOut")
    class FactionMustMoveOut {
        @BeforeEach
        void setUp() {
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(bg);
            game.addFaction(ix);
            game.addFaction(ecaz);
            game.addFaction(richese);

            game.createAlliance(bg, ecaz);
        }

        @Test
        void testFactionMoveOutOfTerritoryWithAlly() {
            arrakeen.addForces("Richese", 1);
            assertFalse(arrakeen.factionMustMoveOut(game, richese));
            game.createAlliance(atreides, richese);
            assertTrue(arrakeen.factionMustMoveOut(game, richese));
        }

        @Test
        void testFactionMoveOutOfTerritoryWithAllyNoField() {
            arrakeen.setRicheseNoField(0);
            assertFalse(arrakeen.factionMustMoveOut(game, richese));
            game.createAlliance(atreides, richese);
            assertTrue(arrakeen.factionMustMoveOut(game, richese));
        }

        @Test
        void testFactionDoesNotHaveToMoveOutOfPolarSink() {
            game.createAlliance(atreides, richese);
            polarSink.addForces("Atreides", 1);
            polarSink.addForces("Richese", 1);
            assertFalse(polarSink.factionMustMoveOut(game, atreides));
            assertFalse(polarSink.factionMustMoveOut(game, richese));
        }

        @Test
        void testEcazDoesNotHaveToMoveOutOfTerritoryWithEcazAlly() {
            carthag.addForces("BG", 1);
            carthag.addForces("Ecaz", 1);
            assertFalse(carthag.factionMustMoveOut(game, ecaz));
        }

        @Test
        void testEcazAllyDoesNotHaveToMoveOutOfTerritoryWithEcaz() {
            carthag.addForces("BG", 1);
            carthag.addForces("Ecaz", 1);
            assertFalse(carthag.factionMustMoveOut(game, bg));
        }
    }

    @Nested
    @DisplayName("#onlyEcazAndAllyPresent")
    class OnlyEcazAndAllyPresent {
        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(moritani);
            game.addFaction(bg);
            game.addFaction(richese);
            sietchTabr.addForces("Ecaz", 2);
        }

        @Test
        void testOnlyOneFaction() {
            game.createAlliance(ecaz, moritani);
            assertFalse(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testEcazHasNoAlly() {
            sietchTabr.addForces("Moritani", 1);
            assertFalse(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testEcazAllyPlusAThird() {
            game.createAlliance(ecaz, moritani);
            sietchTabr.addForces("Moritani", 1);
            sietchTabr.addForces("BG", 1);
            assertFalse(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testEcazAllyPlusANoField() {
            game.createAlliance(ecaz, moritani);
            sietchTabr.addForces("Moritani", 1);
            sietchTabr.setRicheseNoField(3);
            assertFalse(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testOnlyEcazAndAlly() {
            game.createAlliance(ecaz, moritani);
            sietchTabr.addForces("Moritani", 1);
            assertTrue(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testOnlyEcazAndAlliedNoField() {
            game.createAlliance(ecaz, richese);
            sietchTabr.setRicheseNoField(3);
            assertTrue(sietchTabr.onlyEcazAndAllyPresent(game));
        }

        @Test
        void testOnlyEcazAllyAndAdvisor() {
            game.createAlliance(ecaz, moritani);
            sietchTabr.addForces("Moritani", 1);
            sietchTabr.addForces("Advisor", 1);
            assertTrue(sietchTabr.onlyEcazAndAllyPresent(game));
        }
    }

    @Nested
    @DisplayName("#addTerrorToken")
    class AddTerrorToken {
        @BeforeEach
        void setUp() {
            game.addFaction(moritani);
        }

        @Test
        void testAddTerrorToken() throws InvalidGameStateException {
            sietchTabr.addTerrorToken(game, "Robbery");
            assertTrue(sietchTabr.hasTerrorToken("Robbery"));
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testSecondTerrorTokenNotAllowed() throws InvalidGameStateException {
            sietchTabr.addTerrorToken(game, "Robbery");
            assertThrows(InvalidGameStateException.class, () -> sietchTabr.addTerrorToken(game, "Sabotage"));
        }

        @Test
        void testSecondTerrorTokenAllowedWithMoritaniHighThreshold() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            sietchTabr.addTerrorToken(game, "Robbery");
            assertDoesNotThrow(() -> sietchTabr.addTerrorToken(game, "Sabotage"));
            assertTrue(sietchTabr.hasTerrorToken("Robbery"));
            assertTrue(sietchTabr.hasTerrorToken("Sabotage"));
        }

        @Test
        void testSecondTerrorTokenNotAllowedWithMoritaniLowThreshold() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.getMoritaniFaction().placeForcesFromReserves(sietchTabr, 13, false);
            sietchTabr.addTerrorToken(game, "Robbery");
            assertThrows(InvalidGameStateException.class, () -> sietchTabr.addTerrorToken(game, "Sabotage"));
        }
    }

    @Nested
    @DisplayName("#removeTerrorToken")
    class RemoveTerrorToken {
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
            sietchTabr.removeTerrorToken(game, "Robbery", false);
            assertFalse(sietchTabr.hasTerrorToken());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
            assertFalse(moritani.getTerrorTokens().contains("Robbery"));
            assertFalse(moritani.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
        }

        @Test
        void testTerrorTokenRemovedAndReturnedToSupply() {
            sietchTabr.removeTerrorToken(game, "Robbery", true);
            assertFalse(sietchTabr.hasTerrorToken());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
            assertTrue(moritani.getTerrorTokens().contains("Robbery"));
            assertTrue(moritani.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
        }

        @Test
        void testTerrorTokenNotFound() {
            assertThrows(IllegalArgumentException.class, () -> sietchTabr.removeTerrorToken(game, "Sabotage", false));
        }
    }

    @Nested
    @DisplayName("#removeForces")
    class RemoveForces {
        @BeforeEach
        void setUp() throws IOException {
//            game.addFaction(emperor);
//            game.addFaction(guild);
//            game.addFaction(richese);
//            game.addFaction(fremen);
//            game.addFaction(atreides);
            game.addFaction(bg);
        }

        @Test
        void testBGFlipIfAlone() {
            game.setStorm(1);
            sietchTabr.addForces("Advisor", 1);
            sietchTabr.addForces("Atreides", 1);
            sietchTabr.removeForces(game, "Atreides", 1);
            assertTrue(sietchTabr.hasForce("BG"));
            assertFalse(sietchTabr.hasForce("Advisor"));
            assertEquals(Emojis.BG_ADVISOR + " are alone in Sietch Tabr and have flipped to " + Emojis.BG_FIGHTER, turnSummary.getMessages().getFirst());
        }

        @Test
        void testBGDontFlipWithOpponentInOtherSector() {
            game.setStorm(10);
            cielagoNorth_eastSector.addForces("Advisor", 1);
            cielagoNorth_eastSector.addForces("Guild", 1);
            cielagoNorth_middleSector.addForces("Atreides", 1);
            cielagoNorth_eastSector.removeForces(game, "Guild", 1);
            assertFalse(cielagoNorth_eastSector.hasForce("BG"));
            assertTrue(cielagoNorth_eastSector.hasForce("Advisor"));
        }
    }
}
