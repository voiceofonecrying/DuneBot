package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.*;

public class GameBattlePhaseTest extends DuneTest {
    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @Nested
    @DisplayName("#startBattlePhase")
    class StartBattlePhase {
        @BeforeEach
        void setUp() {
            game.addFaction(bg);
            game.addFaction(fremen);
        }

        @Test
        void testBattleDetected() {
            game.setStorm(14);
            cielagoNorth_eastSector.addForces("BG", 6);
            cielagoNorth_eastSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.FREMEN + " vs " + Emojis.BG + " in Cielago North", turnSummary.messages.getLast());
        }

        @Test
        void testBattleInWindPassNorth() {
            game.setStorm(14);
            windPassNorth_northSector.addForces("BG", 6);
            windPassNorth_northSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.FREMEN + " vs " + Emojis.BG + " in Wind Pass North", turnSummary.messages.getLast());
            assertEquals(1, StringUtils.countMatches(turnSummary.messages.getLast(),"Wind Pass"));
        }

        @Test
        void testBattleInWindPass() {
            game.setStorm(13);
            windPass_northSector.addForces("BG", 6);
            windPass_northSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.FREMEN + " vs " + Emojis.BG + " in Wind Pass", turnSummary.messages.getLast());
            assertEquals(1, StringUtils.countMatches(turnSummary.messages.getLast(),"Wind Pass"));
        }

        @Test
        void testBattleWithNoField() {
            game.setStorm(14);
            game.addFaction(richese);
            cielagoNorth_eastSector.addForces("BG", 6);
            cielagoNorth_eastSector.setRicheseNoField(5);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.RICHESE + " vs " + Emojis.BG + " in Cielago North", turnSummary.messages.getLast());
        }

        @Test
        void testNoBattleInPolarSink() {
            game.setStorm(1);
            polarSink.addForces("BG", 6);
            polarSink.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testNoBattleUnderStorm() {
            game.setStorm(2);
            cielagoNorth_eastSector.addForces("BG", 6);
            cielagoNorth_eastSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testNoBattleWithAdvisor() {
            game.setStorm(14);
            cielagoNorth_eastSector.addForces("Advisor", 6);
            cielagoNorth_eastSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testBattleAcrossSectors() {
            game.setStorm(10);
            cielagoNorth_westSector.addForces("BG", 6);
            cielagoNorth_eastSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("The following battles will take place this turn:\n" + Emojis.BG + " vs " + Emojis.FREMEN + " in Cielago North", turnSummary.messages.getLast());
        }

        @Test
        void testNoBattleAcrossSectorsDueToStorm() {
            game.setStorm(1);
            cielagoNorth_westSector.addForces("BG", 6);
            cielagoNorth_eastSector.addForces("Fremen", 7);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testMoritaniInTwoStronholdBattlesGetsVidal() {
            game.addFaction(moritani);
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);

            game.startBattlePhase();
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
            assertEquals("Duke Vidal now works for " + Emojis.MORITANI, turnSummary.getMessages().getFirst());
        }

        @Test
        void testMoritaniDoesNotGetVidalBecauseEcazHomeworldIsOccupied() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(moritani);
            game.addFaction(ecaz);
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);
            Territory ecazHomeworld = game.getTerritory(ecaz.getHomeworld());
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            bg.placeForcesFromReserves(ecazHomeworld, 1, false);

            turnSummary.clear();
            game.startBattlePhase();
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals(Emojis.MORITANI + " may not take Duke Vidal because Ecaz Homeworld is occupied.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testMoritaniTakesVidalFromEcaz() {
            game.addFaction(moritani);
            game.addFaction(ecaz);
            ecaz.addLeader(dukeVidal);
            assertTrue(ecaz.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);

            game.startBattlePhase();

            assertFalse(ecaz.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!", ecazChat.messages.getFirst());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniTakesVidalFromHarkonnen() {
            game.addFaction(moritani);
            game.addFaction(harkonnen);
            harkonnen.addLeader(dukeVidal);
            assertTrue(harkonnen.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);

            game.startBattlePhase();

            assertFalse(harkonnen.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!", harkonnenChat.messages.getFirst());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniTakesVidalFromBT() {
            game.addFaction(moritani);
            game.addFaction(bt);
            bt.addLeader(dukeVidal);
            assertTrue(bt.getLeader("Duke Vidal").isPresent());
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("BG", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("BG", 1);

            game.startBattlePhase();

            assertFalse(bt.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!", btChat.messages.getFirst());
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has come to fight for you!", moritaniChat.messages.getFirst());
        }

        @Test
        void testMoritaniWithAdvisorsDoesNotGetVidal() {
            game.addFaction(moritani);
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("Advisor", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("Advisor", 1);

            game.startBattlePhase();

            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertTrue(moritaniChat.messages.isEmpty());
        }

        @Test
        void testMoritaniWithEcazDoesNotGetVidal() {
            game.setStorm(14);
            game.addFaction(moritani);
            game.addFaction(ecaz);
            arrakeen.addForces("Moritani", 6);
            arrakeen.addForces("Ecaz", 1);
            tueksSietch.addForces("Moritani", 6);
            tueksSietch.addForces("Ecaz", 1);

            game.startBattlePhase();

            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertTrue(moritaniChat.messages.isEmpty());
        }

        @Test
        void testEcazWithAllyNotABattle() {
            game.addFaction(ecaz);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Fremen", 7);
            carthag.addForces("Advisor", 1);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testAllyWithEcazNotABattle() {
            game.addFaction(ecaz);
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Moritani", 7);
            carthag.addForces("Advisor", 1);

            game.startBattlePhase();

            assertEquals("There are no battles this turn.", turnSummary.messages.getLast());
        }

        @Test
        void testEcazAllyAndThirdHaveABattle() {
            game.addFaction(ecaz);
            game.addFaction(moritani);
            ecaz.setAlly("Moritani");
            moritani.setAlly("Ecaz");
            game.addFaction(fremen);
            carthag.addForces("Ecaz", 6);
            carthag.addForces("Moritani", 7);
            carthag.addForces("Fremen", 1);

            game.startBattlePhase();

            assertEquals(MessageFormat.format(
                    "The following battles will take place this turn:\n{0} vs {1}{2} in Carthag",
                    Emojis.FREMEN, Emojis.ECAZ, Emojis.MORITANI),
                    turnSummary.messages.getLast()
            );
        }

        @Test
        void testEcazAllyAndThirdHaveABattle2() {
            game.addFaction(ecaz);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            game.addFaction(richese);

            game.setStorm(10);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            cielagoNorth_westSector.addForces("BG", 6);
            cielagoNorth_westSector.addForces("Emperor*", 1);
            cielagoNorth_westSector.addForces("Ecaz", 1);
            cielagoNorth_eastSector.addForces("Fremen", 7);
            cielagoNorth_eastSector.addForces("Fremen*", 2);

            game.startBattlePhase();

            assertEquals(MessageFormat.format(
                    "The following battles will take place this turn:\n{0} vs {1}{2} vs {3} in Cielago North",
                    Emojis.BG, Emojis.FREMEN, Emojis.ECAZ, Emojis.EMPEROR),
                    turnSummary.messages.getLast()
            );
        }
    }
}
