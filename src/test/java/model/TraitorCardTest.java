package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraitorCardTest extends DuneTest {
    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @Nested
    @DisplayName("#nameAndStrengthString")
    class NameAndStrengthString {
        @Test
        void testFeydRautha() {
            TraitorCard feyd = new TraitorCard("Feyd Rautha", "Harkonnen", 6);
            assertEquals("Feyd Rautha (6)", feyd.getNameAndStrengthString());
        }

        @Test
        void testZoal() {
            TraitorCard zoal = new TraitorCard("Zoal", "BT", -1);
            assertEquals("Zoal (X)", zoal.getNameAndStrengthString());
        }

        @Test
        void testCheapHero() {
            TraitorCard chTraitor = new TraitorCard("Cheap Hero", "Any", 0);
            assertEquals("Cheap Hero (0)", chTraitor.getNameAndStrengthString());
        }
    }

    @Nested
    @DisplayName("#emojiNameAndStrengthString")
    class EmojiNameAndStrengthString {
        @Test
        void testFeydRautha() {
            TraitorCard feyd = new TraitorCard("Feyd Rautha", "Harkonnen", 6);
            assertEquals(Emojis.HARKONNEN + " Feyd Rautha (6)", feyd.getEmojiNameAndStrengthString());
        }

        @Test
        void testZoal() {
            TraitorCard zoal = new TraitorCard("Zoal", "BT", -1);
            assertEquals(Emojis.BT + " Zoal (X)", zoal.getEmojiNameAndStrengthString());
        }

        @Test
        void testCheapHero() {
            TraitorCard chTraitor = new TraitorCard("Cheap Hero", "Any", 0);
            assertEquals("Cheap Hero (0)", chTraitor.getEmojiNameAndStrengthString());
        }
    }
}
