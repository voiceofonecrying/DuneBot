package constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmojisTest {

    /**
     * Tests for the `standardiseEmojiName` method in the `Emojis` class.
     * The `standardiseEmojiName` method takes in an emoji name in shorthand format and returns the corresponding standardized emoji string.
     */

    @Test
    void testStandardiseEmojiNameAtreides() {
        // Test case: shorthand "atr"
        String emojiName = ":atr:";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals(":atreides:", result, "Expected shorthand 'atr' to standardize to ':atreides:'");
    }

    @Test
    void testStandardiseEmojiNameHarkonnen() {
        // Test case: shorthand "hark"
        String emojiName = ":hark:";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals(":harkonnen:", result, "Expected shorthand 'hark' to standardize to ':harkonnen:'");
    }

    @Test
    void testStandardiseEmojiNameEmperor() {
        // Test case: shorthand "emp"
        String emojiName = ":emp:";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals(":emperor:", result, "Expected shorthand 'emp' to standardize to ':emperor:'");
    }

    @Test
    void testStandardiseEmojiNameFremen() {
        // Test case: shorthand "frem"
        String emojiName = ":frem:";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals(":fremen:", result, "Expected shorthand 'frem' to standardize to ':fremen:'");
    }

    @Test
    void testStandardiseEmojiNameRichese() {
        // Test case: shorthand "rich"
        String emojiName = ":rich:";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals(":rich:", result, "Expected shorthand 'rich' to standardize to ':rich:'");
    }

    @Test
    void testStandardiseEmojiNameUnknownEmoji() {
        // Test case: unknown shorthand "unknown"
        String emojiName = "unknown";
        String result = Emojis.standardiseEmojiName(emojiName);
        assertEquals("unknown", result, "Expected unknown shorthand 'unknown' to return itself");
    }
}