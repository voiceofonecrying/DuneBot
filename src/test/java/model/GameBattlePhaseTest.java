package model;

import constants.Emojis;
import model.factions.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

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
        dukeVidal = new Leader("Duke Vidal", 6, "None", null, false);
    }

    @Nested
    @DisplayName("#startBattlePhase")
    class StartBattlePhase {
        @BeforeEach
        void setUp() throws IOException {
            BGFaction bg = new BGFaction("bgPlayer", "bgUser");
            game.addFaction(bg);
            fremen = new FremenFaction("fPlayer", "fUser");
            game.addFaction(fremen);
        }

        @Test
        void testBattleDetected() {
            game.setStorm(14);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("BG", 6);
            eastCielagoNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.FREMEN + " vs " + Emojis.BG + " in Cielago North", turnSummary.messages.get(1));
        }

        @Test
        void testBattleInWindPassNorth() {
            game.setStorm(14);
            Territory windPassNorthNorth = game.getTerritory("Wind Pass North (North Sector)");
            windPassNorthNorth.addForces("BG", 6);
            windPassNorthNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.FREMEN + " vs " + Emojis.BG + " in Wind Pass North", turnSummary.messages.get(1));
            assertEquals(1, StringUtils.countMatches(turnSummary.messages.get(1),"Wind Pass"));
        }

        @Test
        void testBattleWithNoField() throws IOException {
            game.setStorm(14);
            RicheseFaction richeseFaction = new RicheseFaction("rPlayer", "rName");
            game.addFaction(richeseFaction);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("BG", 6);
            eastCielagoNorth.setRicheseNoField(5);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.RICHESE + " vs " + Emojis.BG + " in Cielago North", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleInPolarSink() {
            game.setStorm(1);
            Territory polarSink = game.getTerritory("Polar Sink");
            polarSink.addForces("BG", 6);
            polarSink.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleUnderStorm() {
            game.setStorm(2);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("BG", 6);
            eastCielagoNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleWithAdvisor() {
            game.setStorm(14);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("Advisor", 6);
            eastCielagoNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testBattleAcrossSectors() {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForces("BG", 6);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.BG + " vs " + Emojis.FREMEN + " in Cielago North", turnSummary.messages.get(1));
        }

        @Test
        void testNoBattleAcrossSectorsDueToStorm() {
            game.setStorm(1);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForces("BG", 6);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("Fremen", 7);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testMoritaniInTwoStronholdBattlesGetsVidal() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniTakesVidalFromEcaz() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            ecaz = new EcazFaction("ePlayer", "eUser");
            game.addFaction(ecaz);
            ecaz.addLeader(dukeVidal);
            assertTrue(ecaz.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic ecazChat = new TestTopic();
            ecaz.setChat(ecazChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(ecaz.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!", ecazChat.messages.getFirst());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniTakesVidalFromHarkonnen() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            HarkonnenFaction harkonnen = new HarkonnenFaction("ePlayer", "eUser");
            game.addFaction(harkonnen);
            harkonnen.addLeader(dukeVidal);
            assertTrue(harkonnen.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            TestTopic harkonnenChat = new TestTopic();
            harkonnen.setChat(harkonnenChat);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertFalse(harkonnen.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has escaped to fight for the " + Emojis.MORITANI + "!", harkonnenChat.messages.getFirst());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniWithAdvisorsDoesNotGetVidal() throws IOException {
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("Advisor", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("Advisor", 1);
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
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            ecaz = new EcazFaction("ecazPlayer", "ecazUser");
            game.addFaction(ecaz);
            Territory arrakeen = game.getTerritory("Arrakeen");
            Territory tueksSietch = game.getTerritory("Tuek's Sietch");
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("Ecaz", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("Ecaz", 1);
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
            ecaz = new EcazFaction("ecazPlayer", "ecazUser");
            game.addFaction(ecaz);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Fremen", 7);
            carthag.addForces("Advisor", 1);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testAllyWithEcazNotABattle() throws IOException {
            ecaz = new EcazFaction("ecazPlayer", "ecazUser");
            game.addFaction(ecaz);
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Moritani", 7);
            carthag.addForces("Advisor", 1);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
        }

        @Test
        void testEcazAllyAndThirdHaveABattle() throws IOException {
            ecaz = new EcazFaction("ecazPlayer", "ecazUser");
            game.addFaction(ecaz);
            moritani = new MoritaniFaction("mPlayer", "mUser");
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            game.addFaction(fremen);
            Territory carthag = game.getTerritory("Carthag");
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Moritani", 7);
            carthag.addForces("Fremen", 1);
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(MessageFormat.format(
                    "The following battles will take place this turn:\n{0} vs {1}{2} in Carthag",
                    Emojis.FREMEN, Emojis.ECAZ, Emojis.MORITANI),
                    turnSummary.messages.get(1)
            );
        }

        @Test
        void testEcazAllyAndThirdHaveABattle2() throws IOException {
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            ecaz = new EcazFaction("aPlayer", "aUser");
            EmperorFaction emperor = new EmperorFaction("ePlayer", "eUser");
            HarkonnenFaction harkonnen = new HarkonnenFaction("hPlayer", "hUser");
            RicheseFaction richese = new RicheseFaction("rPlayer", "rUser");
            game.addFaction(ecaz);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            game.addFaction(richese);

            game.setStorm(10);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForces("BG", 6);
            westCielagoNorth.addForces("Emperor*", 1);
            westCielagoNorth.addForces("Ecaz", 1);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForces("Fremen", 7);
            eastCielagoNorth.addForces("Fremen*", 2);

            game.startBattlePhase();

            assertEquals(MessageFormat.format(
                    "The following battles will take place this turn:\n{0} vs {1}{2} vs {3} in Cielago North",
                    Emojis.BG, Emojis.FREMEN, Emojis.ECAZ, Emojis.EMPEROR),
                    turnSummary.messages.get(1)
            );
        }
    }
}
