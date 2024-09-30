package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RevivalTest extends DuneTest {
    Revival revival;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.startRevival();
        revival = game.getRevival();
        game.addFaction(bt);
        game.addFaction(choam);
        game.addFaction(harkonnen);
    }

    @Test
    void testBTCollectsForFreeRevivals() throws InvalidGameStateException {
        game.removeForces("Tleilax", bt, 4, false, true);
        game.removeForces("Tupile", choam, 6, false, true);
        game.removeForces("Giedi Prime", harkonnen, 2, false, true);
        revival.startRevivingForces(game);
        assertEquals("**Turn 0 Revival Phase**", turnSummary.getMessages().getFirst());
        assertEquals(Emojis.BT + " revives 2 " + Emojis.BT_TROOP + " for free.", turnSummary.getMessages().get(1));
        assertEquals(Emojis.HARKONNEN + " revives 2 " + Emojis.HARKONNEN_TROOP + " for free.", turnSummary.getMessages().get(2));
        assertEquals(Emojis.BT + " gain 2 " + Emojis.SPICE + " from free revivals.\n", turnSummary.getMessages().get(3));
    }
}
