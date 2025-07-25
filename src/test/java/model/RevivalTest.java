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

public class RevivalTest extends DuneTest {
    Revival revival;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.startRevival();
        revival = game.getRevival();
    }

    @Test
    void testBTCollectsForFreeRevivals() throws InvalidGameStateException {
        game.addFaction(bt);
        game.addFaction(choam);
        game.addFaction(harkonnen);
        game.removeForces("Tleilax", bt, 4, 0, true);
        game.removeForces("Tupile", choam, 6, 0, true);
        game.removeForces("Giedi Prime", harkonnen, 2, 0, true);
        revival.startRevivingForces(game);
        assertEquals("**Turn 0 Revival Phase**", turnSummary.getMessages().getFirst());
        assertEquals(Emojis.BT + " revives 2 " + Emojis.BT_TROOP + " for free.", turnSummary.getMessages().get(1));
        assertEquals(Emojis.HARKONNEN + " revives 2 " + Emojis.HARKONNEN_TROOP + " for free.", turnSummary.getMessages().get(2));
        assertEquals(Emojis.BT + " gain 2 " + Emojis.SPICE + " from free revivals.\n", turnSummary.getMessages().get(3));
    }

    @Nested
    @DisplayName("#askAboutRevivalLimits")
    class AskAboutRevivalLimits {
        @BeforeEach
        void setUp() {
            game.setTurn(2);
            game.addFaction(emperor);
        }

        @Test
        void testBTNotInGame() {
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            assertFalse(revival.askAboutRevivalLimits(game));
        }

        @Test
        void testBTInGameNoLimitsNeedToBeRaised() {
            game.addFaction(bt);
            game.removeForces("Kaitain", emperor, 2, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(revival.askAboutRevivalLimits(game));
            assertTrue(btChat.getMessages().getLast().contains(Emojis.EMPEROR + " has only 2 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " revivable forces."));
        }

        @Test
        void testBTInGameLimitCanBeRaised() {
            game.addFaction(bt);
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            assertTrue(revival.askAboutRevivalLimits(game));
        }
    }

    @Nested
    @DisplayName("#startRevivingForces")
    class StartRevivingForces {
        @BeforeEach
        void setUp() {
            game.setTurn(2);
            game.addFaction(emperor);
        }

        @Test
        void testBTNotInGame() {
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            revival.askAboutRevivalLimits(game);
            assertDoesNotThrow(() -> revival.startRevivingForces(game));
        }

        @Test
        void testBTInGameNoLimitsNeedToBeRaised() {
            game.addFaction(bt);
            game.removeForces("Kaitain", emperor, 2, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            revival.askAboutRevivalLimits(game);
            assertDoesNotThrow(() -> revival.startRevivingForces(game));
        }

        @Test
        void testBTInGameLimitCanBeRaised() {
            game.addFaction(bt);
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            revival.askAboutRevivalLimits(game);
            assertThrows(InvalidGameStateException.class, () -> revival.startRevivingForces(game));
        }

        @Test
        void testBTFreeRevivalSpice() throws InvalidGameStateException {
            game.addFaction(bt);
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            revival.askAboutRevivalLimits(game);
            bt.setRevivalLimit("Emperor", 3);
            turnSummary.clear();
            revival.startRevivingForces(game);
            assertEquals(Emojis.BT + " gain 1 " + Emojis.SPICE + " from free revivals.\n", turnSummary.getMessages().get(2));
        }

        @Test
        void testBTLowThresholdFreeRevivalSpice() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(bt);
            bt.placeForcesFromReserves(sietchTabr, 20, false);
            assertFalse(bt.isHighThreshold());
            game.removeForces("Kaitain", emperor, 6, 0, true);
            game.removeForces("Salusa Secundus", emperor, 0, 2, true);
            revival.askAboutRevivalLimits(game);
            bt.setRevivalLimit("Emperor", 3);
            turnSummary.clear();
            revival.startRevivingForces(game);
            assertNotEquals(Emojis.BT + " gain 1 " + Emojis.SPICE + " from free revivals.\n", turnSummary.getMessages().get(2));
        }
    }
}
