package model;

import constants.Emojis;
import model.factions.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class GameBattlePhaseTest {
    private Game game;
    private EcazFaction ecaz;
    private FremenFaction fremen;
    private MoritaniFaction moritani;
    private Leader dukeVidal;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        dukeVidal = new Leader("Duke Vidal", 6, null, false);
    }

    @Nested
    @DisplayName("#startBattlePhase")
    class StartBattlePhase {
        @BeforeEach
        void setUp() throws IOException {
            BGFaction bg = new BGFaction("bgPlayer", "bgUser", game);
            game.addFaction(bg);
            fremen = new FremenFaction("fPlayer", "fUser", game);
            game.addFaction(fremen);
        }

        @Test
        void testBattleDetected() {
            game.setStorm(14);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("BG", 6));
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf(Emojis.BG));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf(Emojis.FREMEN));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf("Cielago North"));
        }

        @Test
        void testBattleInWindPassNorth() {
            game.setStorm(14);
            Territory windPassNorthNorth = game.getTerritory("Wind Pass North (North Sector)");
            windPassNorthNorth.addForce(new Force("BG", 6));
            windPassNorthNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf("Wind Pass North"));
            assertEquals(1, StringUtils.countMatches(turnSummary.messages.get(1),"Wind Pass"));
        }

        @Test
        void testBattleWithNoField() throws IOException {
            game.setStorm(14);
            RicheseFaction richeseFaction = new RicheseFaction("rPlayer", "rName", game);
            game.addFaction(richeseFaction);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("BG", 6));
            eastCielagoNorth.setRicheseNoField(5);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf(Emojis.BG));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf(Emojis.RICHESE));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf("Cielago North"));
        }

        @Test
        void testNoBattleInPolarSink() {
            game.setStorm(1);
            Territory polarSink = game.getTerritory("Polar Sink");
            polarSink.addForce(new Force("BG", 6));
            polarSink.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleUnderStorm() {
            game.setStorm(2);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("BG", 6));
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleWithAdvisor() {
            game.setStorm(14);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Advisor", 6));
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testBattleAcrossSectors() {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
            assertNotEquals(-1, turnSummary.messages.get(1).indexOf("Cielago North"));
        }

        @Test
        void testNoBattleAcrossSectorsDueToStorm() {
            game.setStorm(1);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testMoritaniInTwoStronholdBattlesGetsVidal() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForce(new Force("Moritani", 6));
            arrakeen.addForce(new Force("BG", 1));
            tueksSietch.addForce(new Force("Moritani", 6));
            tueksSietch.addForce(new Force("BG", 1));
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.get(0));
        }

        @Test
        void testMoritaniTakesVidalFromEcaz() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            ecaz = new EcazFaction("ePlayer", "eUser", game);
            game.addFaction(ecaz);
            ecaz.addLeader(dukeVidal);
            assertTrue(ecaz.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForce(new Force("Moritani", 6));
            arrakeen.addForce(new Force("BG", 1));
            tueksSietch.addForce(new Force("Moritani", 6));
            tueksSietch.addForce(new Force("BG", 1));
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic ecazChat = new TestTopic();
            ecaz.setChat(ecazChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(ecaz.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!", ecazChat.messages.get(0));
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.get(0));
        }

        @Test
        void testMoritaniTakesVidalFromHarkonnen() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            HarkonnenFaction harkonnen = new HarkonnenFaction("ePlayer", "eUser", game);
            game.addFaction(harkonnen);
            harkonnen.addLeader(dukeVidal);
            assertTrue(harkonnen.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForce(new Force("Moritani", 6));
            arrakeen.addForce(new Force("BG", 1));
            tueksSietch.addForce(new Force("Moritani", 6));
            tueksSietch.addForce(new Force("BG", 1));
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic harkonnenChat = new TestTopic();
            harkonnen.setChat(harkonnenChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(harkonnen.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has escaped to fight for the " + Emojis.MORITANI + "!", harkonnenChat.messages.get(0));
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.get(0));
        }

        @Test
        void testMoritaniWithAdvisorsDoesNotGetVidal() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForce(new Force("Moritani", 6));
            arrakeen.addForce(new Force("Advisor", 1));
            tueksSietch.addForce(new Force("Moritani", 6));
            tueksSietch.addForce(new Force("Advisor", 1));
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertTrue(moritaniChat.messages.isEmpty());
        }

        @Test
        void testMoritaniWithEcazDoesNotGetVidal() throws IOException {
            game.setStorm(14);
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            ecaz = new EcazFaction("ecazPlayer", "ecazUser", game);
            game.addFaction(ecaz);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForce(new Force("Moritani", 6));
            arrakeen.addForce(new Force("Ecaz", 1));
            tueksSietch.addForce(new Force("Moritani", 6));
            tueksSietch.addForce(new Force("Ecaz", 1));
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertTrue(moritaniChat.messages.isEmpty());
        }

        @Test
        void testEcazWithAllyNotABattle() throws IOException {
            ecaz = new EcazFaction("ecazPlayer", "ecazUser", game);
            game.addFaction(ecaz);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForce(new Force("Ecaz", 6));
            carthag.addForce(new Force("Fremen", 7));
            carthag.addForce(new Force("Advisor", 1));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testAllyWithEcazNotABattle() throws IOException {
            ecaz = new EcazFaction("ecazPlayer", "ecazUser", game);
            game.addFaction(ecaz);
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForce(new Force("Ecaz", 6));
            carthag.addForce(new Force("Moritani", 7));
            carthag.addForce(new Force("Advisor", 1));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testEcazAllyAndThirdHaveABattle() throws IOException {
            ecaz = new EcazFaction("ecazPlayer", "ecazUser", game);
            game.addFaction(ecaz);
            moritani = new MoritaniFaction("mPlayer", "mUser", game);
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            game.addFaction(fremen);
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForce(new Force("Ecaz", 6));
            carthag.addForce(new Force("Moritani", 7));
            carthag.addForce(new Force("Fremen", 1));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
        }
    }
}
