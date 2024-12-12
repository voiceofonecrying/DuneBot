package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TraitorCardTest extends DuneTest {
    TraitorCard feyd;
    TraitorCard zoal;
    TraitorCard chTraitor;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        feyd = new TraitorCard("Feyd Rautha", "Harkonnen", 6);
        zoal = new TraitorCard("Zoal", "BT", -1);
        chTraitor = new TraitorCard("Cheap Hero", "Any", 0);
    }

    @Test
    void testNameAndStrengthString() {
        assertEquals("Feyd Rautha (6)", feyd.getNameAndStrengthString());
        assertEquals("Zoal (X)", zoal.getNameAndStrengthString());
        assertEquals("Cheap Hero (0)", chTraitor.getNameAndStrengthString());
    }

    @Test
    void testEmojiAndNameString() {
        assertEquals(Emojis.HARKONNEN + " Feyd Rautha", feyd.getEmojiAndNameString());
        assertEquals(Emojis.BT + " Zoal", zoal.getEmojiAndNameString());
        assertEquals("Cheap Hero", chTraitor.getEmojiAndNameString());
    }

    @Test
    void testEmojiNameAndStrengthString() {
        assertEquals(Emojis.HARKONNEN + " Feyd Rautha (6)", feyd.getEmojiNameAndStrengthString());
        assertEquals(Emojis.BT + " Zoal (X)", zoal.getEmojiNameAndStrengthString());
        assertEquals("Cheap Hero (0)", chTraitor.getEmojiNameAndStrengthString());
    }

    @Nested
    @DisplayName("#canBeCalledAgainst")
    class CanBeCalledAgainst {
        @Test
        void testFeydRautha() {
            assertTrue(feyd.canBeCalledAgainst(harkonnen));
            assertFalse(feyd.canBeCalledAgainst(atreides));
        }

        @Test
        void testZoal() {
            assertTrue(zoal.canBeCalledAgainst(bt));
            assertFalse(zoal.canBeCalledAgainst(ix));
        }

        @Test
        void testCheapHero() {
            assertTrue(chTraitor.canBeCalledAgainst(atreides));
            assertTrue(chTraitor.canBeCalledAgainst(bg));
            assertTrue(chTraitor.canBeCalledAgainst(emperor));
            assertTrue(chTraitor.canBeCalledAgainst(fremen));
            assertTrue(chTraitor.canBeCalledAgainst(guild));
            assertTrue(chTraitor.canBeCalledAgainst(harkonnen));
            assertTrue(chTraitor.canBeCalledAgainst(bt));
            assertTrue(chTraitor.canBeCalledAgainst(ix));
            assertTrue(chTraitor.canBeCalledAgainst(choam));
            assertTrue(chTraitor.canBeCalledAgainst(richese));
            assertTrue(chTraitor.canBeCalledAgainst(ecaz));
            assertTrue(chTraitor.canBeCalledAgainst(moritani));
        }
    }

    @Test
    void testIsHarkonnenTraitor() {
        assertTrue(feyd.isHarkonnenTraitor());
        assertFalse(zoal.isHarkonnenTraitor());
        assertFalse(chTraitor.isHarkonnenTraitor());
    }
}
