package model;

import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class GameBattlePhaseTest {
    private Game game;
    private BGFaction bg;
    private EcazFaction ecaz;
    private FremenFaction fremen;
    private HarkonnenFaction harkonnen;
    private MoritaniFaction moritani;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
    }

    @Nested
    @DisplayName("#startBattlePhase")
    class StartBattlePhase {
        @BeforeEach
        void setUp() throws IOException {
            bg = new BGFaction("bgPlayer", "bgUser", game);
            game.addFaction(bg);
            fremen = new FremenFaction("fPlayer", "fUser", game);
            game.addFaction(fremen);
        }

        @Test
        void testBattleDetected() {
            game.setStorm(2);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("BG", 6));
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            TestTopic turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.startBattlePhase();

            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
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

//        @Test
//        void testNoBattleUnderStorm() {
//            game.setStorm(1);
//            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
//            eastCielagoNorth.addForce(new Force("BG", 6));
//            eastCielagoNorth.addForce(new Force("Fremen", 7));
//            TestTopic turnSummary = new TestTopic();
//            game.setTurnSummary(turnSummary);
//
//            game.startBattlePhase();
//
//            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
//        }
//
//        @Test
//        void testNoBattleUnderStormAdvisor() {
//            game.setStorm(1);
//            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
//            eastCielagoNorth.addForce(new Force("Advisor", 6));
//            eastCielagoNorth.addForce(new Force("Fremen", 7));
//            TestTopic turnSummary = new TestTopic();
//            game.setTurnSummary(turnSummary);
//
//            game.startBattlePhase();
//
//            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
//        }
//
//        @Test
//        void testBattleAcrossSectors() {
//            game.setStorm(10);
//            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
//            westCielagoNorth.addForce(new Force("BG", 6));
//            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
//            eastCielagoNorth.addForce(new Force("Fremen", 7));
//            TestTopic turnSummary = new TestTopic();
//            game.setTurnSummary(turnSummary);
//
//            game.startBattlePhase();
//
//            assertEquals(0, turnSummary.messages.get(1).indexOf("The following battles will take place this turn:"));
//        }
//
//        @Test
//        void testNoBattleAcrossSectorsDueToStorm() {
//            game.setStorm(1);
//            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
//            westCielagoNorth.addForce(new Force("BG", 6));
//            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
//            eastCielagoNorth.addForce(new Force("Fremen", 7));
//            TestTopic turnSummary = new TestTopic();
//            game.setTurnSummary(turnSummary);
//
//            game.startBattlePhase();
//
//            assertEquals("There are no battles this turn.", turnSummary.messages.get(1));
//        }
//
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
