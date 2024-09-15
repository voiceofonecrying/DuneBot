package model;

import constants.Emojis;
import model.factions.AtreidesFaction;
import model.factions.EmperorFaction;
import model.factions.HarkonnenFaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HomeworldTerritoryTest {
    private TestTopic turnSummary;
    private HomeworldTerritory caladan;

    @BeforeEach
    public void setUp() throws IOException {
        Game game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        AtreidesFaction atreides = new AtreidesFaction("p", "u");
        game.addFaction(atreides);
        HarkonnenFaction harkonnen = new HarkonnenFaction("p", "u");
        game.addFaction(harkonnen);
        EmperorFaction emperor = new EmperorFaction("p", "u");
        game.addFaction(emperor);
        caladan = (HomeworldTerritory) game.getTerritory(atreides.getHomeworld());
    }

    @Nested
    @DisplayName("#homeworldOccupy")
    class HomeworldOccupy {
        @Test
        void resetWhenEmptyEndsOccupy() {
            caladan.removeForce("Atreides");
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.removeForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
            caladan.resetOccupation();
            assertNull(caladan.getOccupierName());
            assertEquals("Caladan is no longer occupied.", turnSummary.getMessages().get(1));
        }

        @Test
        void testNativeDoesNotCauseOccupy() {
            caladan.addForces("Atreides", 1);
            assertNull(caladan.getOccupierName());
        }

        @Test
        void testNonNativeWithNativeDoesNotCauseOccupy() {
            caladan.addForces("Harkonnen", 1);
            assertNull(caladan.getOccupierName());
        }

        @Test
        void testNonNativeIntoEmptyCausesOccupy() {
            caladan.removeForce("Atreides");
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
        }

        @Test
        void test2ndNonNativeDoesNotEndOccupyUntilTheyDefeat1st() {
            caladan.removeForce("Atreides");
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.addForces("Emperor", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
            caladan.removeForce("Harkonnen");
            assertEquals("Emperor", caladan.getOccupierName());
        }

        @Test
        void testNativeDefeatingNonNativeEndsOccupy() {
            caladan.removeForce("Atreides");
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            caladan.removeForces("Harkonnen", 1);
            assertNull(caladan.getOccupierName());
            assertEquals("Caladan is no longer occupied.", turnSummary.getMessages().get(1));
        }

        @Test
        void testNonNativeDefeatingNativeCausesOccupy() {
            caladan.addForces("Harkonnen", 1);
            caladan.removeForce("Atreides");
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
        }

        @Test
        void testTwoNonNativeDefeatingNativeDoesNotOccupy() {
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Emperor", 1);
            assertNull(caladan.getOccupierName());
            caladan.removeForce("Atreides");
            assertNull(caladan.getOccupierName());
            caladan.removeForces("Harkonnen", 1);
            assertEquals("Emperor", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getFirst());
        }

        @Test
        void testNativeRevivalDoesNotEndOccupy() {
            caladan.removeForce("Atreides");
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
        }
    }
}
