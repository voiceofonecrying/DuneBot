package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class HomeworldTerritoryTest extends DuneTest {
    private HomeworldTerritory caladan;
    private HomeworldTerritory tupile;
    private HomeworldTerritory ixHomeworld;
    private HomeworldTerritory ecazHomeworld;

    @BeforeEach
    public void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.addFaction(atreides);
        game.addFaction(harkonnen);
        game.addFaction(emperor);
        game.addFaction(choam);
        game.addFaction(ix);
        game.addFaction(ecaz);
        caladan = (HomeworldTerritory) game.getTerritory(atreides.getHomeworld());
        tupile = (HomeworldTerritory) game.getTerritory(choam.getHomeworld());
        ixHomeworld = (HomeworldTerritory) game.getTerritory(ix.getHomeworld());
        ecazHomeworld = (HomeworldTerritory) game.getTerritory(ecaz.getHomeworld());
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
            assertNull(caladan.getOccupyingFaction());
            assertEquals("Caladan is no longer occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getLast());
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
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getLast());
        }

        @Test
        void test2ndNonNativeOccupiesIfHomeworldWasEmpty() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().getFirst());
            caladan.removeForces(game, "Harkonnen", 1);
            caladan.addForces("Emperor", 1);
            assertEquals("Emperor", caladan.getOccupierName());
            assertEquals("Caladan is now occupied by " + Emojis.EMPEROR, turnSummary.getMessages().getLast());
        }

        @Test
        void testNativeDefeatingNonNativeEndsOccupy() {
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            caladan.addForces("Atreides", 10);
            assertEquals("Harkonnen", caladan.getOccupierName());
            caladan.removeForces(game, "Harkonnen", 1);
            assertNull(caladan.getOccupierName());
            assertEquals("Caladan is no longer occupied by " + Emojis.HARKONNEN, turnSummary.getMessages().get(1));
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

        @Test
        void testNativeRevivalWithZeroRegularsDoesNotEndOccupy() {
            game.addGameOption(GameOption.HOMEWORLDS);
            ixHomeworld.removeForces(game, "Ix", 10);
            ixHomeworld.removeForces(game, "Ix*", 4);
            ixHomeworld.addForces("Harkonnen", 1);
            assertTrue(ix.isHomeworldOccupied());
            ixHomeworld.addForces("Ix", 0);
            ixHomeworld.addForces("Ix*", 1);
            assertEquals("Harkonnen", ixHomeworld.getOccupierName());
            assertEquals(2, turnSummary.getMessages().size());
        }

        @Test
        void testNativeToEmptyHomeworldEndsOccupation() {
            game.addGameOption(GameOption.HOMEWORLDS);
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 1);
            assertEquals("Harkonnen", caladan.getOccupierName());
            assertTrue(atreides.isHomeworldOccupied());
            caladan.removeForces(game, "Harkonnen", 1);
            caladan.addForces("Atreides", 1);
            assertFalse(atreides.isHomeworldOccupied());
            assertNull(caladan.getOccupyingFaction());
        }

        @Test
        void testOccupyingTupileIncreasesHandLimit() {
            game.createAlliance(emperor, harkonnen);
            tupile.removeForces(game, "CHOAM", 20);
            turnSummary.clear();
            tupile.addForces("Emperor*", 1);
            assertEquals(emperor, tupile.getOccupyingFaction());
            assertEquals(5, emperor.getHandLimit());
            assertEquals(Emojis.EMPEROR + " " + Emojis.TREACHERY + " limit has been increased to 5.", turnSummary.getMessages().get(1));
            assertEquals(9, harkonnen.getHandLimit());
            assertEquals(Emojis.HARKONNEN + " " + Emojis.TREACHERY + " limit has been increased to 9.", turnSummary.getMessages().getLast());
        }

        @Test
        void testClearingTupileOccupationDecreasesHandLimit() {
            game.createAlliance(emperor, harkonnen);
            tupile.removeForces(game, "CHOAM", 20);
            tupile.addForces("Emperor*", 1);
            assertEquals(emperor, tupile.getOccupyingFaction());
            tupile.addForces("CHOAM", 20);
            turnSummary.clear();
            tupile.removeForces(game, "Emperor*", 1);
            assertFalse(choam.isHomeworldOccupied());
            assertEquals(4, emperor.getHandLimit());
            assertEquals(Emojis.EMPEROR + " " + Emojis.TREACHERY + " limit has been reduced to 4.", turnSummary.getMessages().get(1));
            assertEquals(8, harkonnen.getHandLimit());
            assertEquals(Emojis.HARKONNEN + " " + Emojis.TREACHERY + " limit has been reduced to 8.", turnSummary.getMessages().getLast());
        }

        @Test
        void testClearingCaladanOccupationDoesNotDecreaseHandLimit() {
            game.createAlliance(emperor, harkonnen);
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Emperor*", 1);
            assertEquals(emperor, caladan.getOccupyingFaction());
            caladan.addForces("Atreides", 10);
            turnSummary.clear();
            caladan.removeForces(game, "Emperor*", 1);
            assertFalse(atreides.isHomeworldOccupied());
            assertEquals(4, emperor.getHandLimit());
            assertEquals(8, harkonnen.getHandLimit());
            assertTrue(turnSummary.getMessages().stream().noneMatch(m -> m.contains("limit has been reduced")));
        }

        @Test
        void testClearingEcazOccupationReleasesDukeVidalFromOccupier() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.createAlliance(emperor, harkonnen);
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            ecazHomeworld.addForces("Emperor*", 1);
            assertEquals(emperor, ecazHomeworld.getOccupyingFaction());
            assertTrue(emperor.getLeader("Duke Vidal").isPresent());
            ecazHomeworld.addForces("Ecaz", 14);
            ecazHomeworld.removeForces(game, "Emperor*", 1);
            assertTrue(emperor.getLeader("Duke Vidal").isEmpty());
            assertTrue(harkonnen.getLeader("Duke Vidal").isEmpty());
            assertTrue(ecaz.getLeader("Duke Vidal").isEmpty());
        }

        @Test
        void testClearingEcazOccupationReleasesDukeVidalFromOccupierAlly() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.createAlliance(emperor, harkonnen);
            ecazHomeworld.removeForces(game, "Ecaz", 14);
            ecazHomeworld.addForces("Emperor*", 1);
            assertEquals(emperor, ecazHomeworld.getOccupyingFaction());
            assertTrue(emperor.getLeader("Duke Vidal").isPresent());
            game.assignDukeVidalToAFaction("Harkonnen");
            assertTrue(harkonnen.getLeader("Duke Vidal").isPresent());
            assertTrue(emperor.getLeader("Duke Vidal").isEmpty());
            ecazHomeworld.addForces("Ecaz", 14);
            ecazHomeworld.removeForces(game, "Emperor*", 1);
            assertTrue(emperor.getLeader("Duke Vidal").isEmpty());
            assertTrue(harkonnen.getLeader("Duke Vidal").isEmpty());
            assertTrue(ecaz.getLeader("Duke Vidal").isEmpty());
        }
    }
}
