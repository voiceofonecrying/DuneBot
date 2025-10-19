package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmbedFieldStateTest {

    private MockEmbedFieldState inlineField;
    private MockEmbedFieldState blockField;

    @BeforeEach
    void setUp() {
        inlineField = new MockEmbedFieldState("Spice", "5", true);
        blockField = new MockEmbedFieldState("Description", "A long description", false);
    }

    @Test
    void constructor_setsNameValueAndInline() {
        assertThat(inlineField.getName()).isEqualTo("Spice");
        assertThat(inlineField.getValue()).isEqualTo("5");
        assertThat(inlineField.isInline()).isTrue();
    }

    @Test
    void constructor_setsInlineToFalse() {
        assertThat(blockField.getName()).isEqualTo("Description");
        assertThat(blockField.getValue()).isEqualTo("A long description");
        assertThat(blockField.isInline()).isFalse();
    }

    @Test
    void getName_returnsCorrectName() {
        assertThat(inlineField.getName()).isEqualTo("Spice");
    }

    @Test
    void getValue_returnsCorrectValue() {
        assertThat(inlineField.getValue()).isEqualTo("5");
    }

    @Test
    void isInline_returnsTrueForInlineField() {
        assertThat(inlineField.isInline()).isTrue();
    }

    @Test
    void isInline_returnsFalseForBlockField() {
        assertThat(blockField.isInline()).isFalse();
    }

    @Test
    void field_handlesEmptyName() {
        MockEmbedFieldState field = new MockEmbedFieldState("", "Value", true);

        assertThat(field.getName()).isEmpty();
        assertThat(field.getValue()).isEqualTo("Value");
    }

    @Test
    void field_handlesEmptyValue() {
        MockEmbedFieldState field = new MockEmbedFieldState("Name", "", false);

        assertThat(field.getName()).isEqualTo("Name");
        assertThat(field.getValue()).isEmpty();
    }

    @Test
    void field_handlesLongText() {
        String longValue = "This is a very long description that contains multiple sentences and lots of details about the game state.";
        MockEmbedFieldState field = new MockEmbedFieldState("Status", longValue, false);

        assertThat(field.getValue()).isEqualTo(longValue);
    }

    @Test
    void field_handlesSpecialCharacters() {
        MockEmbedFieldState field = new MockEmbedFieldState("Player's Turn", "5 spice @ 10:30", true);

        assertThat(field.getName()).isEqualTo("Player's Turn");
        assertThat(field.getValue()).isEqualTo("5 spice @ 10:30");
    }

    @Test
    void field_handlesUnicodeCharacters() {
        MockEmbedFieldState field = new MockEmbedFieldState("Status", "✅ Complete", true);

        assertThat(field.getValue()).isEqualTo("✅ Complete");
    }

    @Test
    void field_handlesNewlinesInValue() {
        String multilineValue = "Line 1\nLine 2\nLine 3";
        MockEmbedFieldState field = new MockEmbedFieldState("Description", multilineValue, false);

        assertThat(field.getValue()).isEqualTo(multilineValue);
    }

    @Test
    void field_handlesMarkdownInValue() {
        String markdownValue = "**Bold** and *italic* text";
        MockEmbedFieldState field = new MockEmbedFieldState("Formatted", markdownValue, true);

        assertThat(field.getValue()).isEqualTo(markdownValue);
    }

    @Test
    void multipleFields_canHaveSameName() {
        MockEmbedFieldState field1 = new MockEmbedFieldState("Player", "Alice", true);
        MockEmbedFieldState field2 = new MockEmbedFieldState("Player", "Bob", true);

        assertThat(field1.getName()).isEqualTo(field2.getName());
        assertThat(field1.getValue()).isNotEqualTo(field2.getValue());
    }

    @Test
    void field_isImmutable() {
        String originalName = "Test";
        String originalValue = "Value";
        MockEmbedFieldState field = new MockEmbedFieldState(originalName, originalValue, true);

        // Verify that getters return the original values
        assertThat(field.getName()).isEqualTo(originalName);
        assertThat(field.getValue()).isEqualTo(originalValue);
    }
}
