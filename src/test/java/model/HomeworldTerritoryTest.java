package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HomeworldTerritoryTest extends DuneTest {
    private HomeworldTerritory caladan;

    @BeforeEach
    public void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.addFaction(atreides);
        game.addFaction(harkonnen);
        game.addFaction(emperor);
        caladan = (HomeworldTerritory) game.getTerritory(atreides.getHomeworld());
    }

    @Nested
    @DisplayName("#homeworldOccupy")
    class HomeworldOccupy {
        @Test
        void resetWhenEmptyEndsOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.removeForces(game, "Harkonnen", 1);
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
        void testNativeExitingDoesNotCauseOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            assertNull(caladan.getOccupierName());
        }

        @Test
        void testNonNativeIntoEmptyCausesOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
        }

        @Test
        void test2ndNonNativeDoesNotOccupyUntilTheyDefeat1st() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.addForces("Emperor", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
            turnSummary.clear();
            caladan.removeForces(game, "Harkonnen", 1);
            assertEquals("Emperor", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getFirst());
        }

        @Test
        void test2ndNonNativeOccupiesIfHomeworldWasEmpty() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.removeForces(game, "Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
            turnSummary.clear();
            caladan.addForces("Emperor", 1);
            assertEquals("Emperor", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getFirst());
        }

        @Test
        void testNativeDefeatingNonNativeEndsOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            caladan.removeForces(game, "Harkonnen", 1);
            assertNull(caladan.getOccupierName());
            assertEquals("Caladan is no longer occupied.", turnSummary.getMessages().get(1));
        }

        @Test
        void testNonNativeDefeatingNativeCausesOccupy() {
            caladan.addForces("Harkonnen", 1);
            caladan.removeForces(game, "Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
        }

        @Test
        void testTwoNonNativeDefeatingNativeDoesNotOccupy() {
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Emperor", 1);
            assertNull(caladan.getOccupierName());
            caladan.removeForces(game, "Atreides", 10);
            assertNull(caladan.getOccupierName());
            caladan.removeForces(game, "Harkonnen", 1);
            assertEquals("Emperor", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getFirst());
        }

        @Test
        void testNativeRevivalDoesNotEndOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals(1, turnSummary.getMessages().size());
        }
    }
}
