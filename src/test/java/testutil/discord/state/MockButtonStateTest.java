package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockButtonStateTest {

    private MockButtonState button;
    private MockEmojiState emoji;

    @BeforeEach
    void setUp() {
        button = new MockButtonState("storm-button-roll", "Roll Storm", "PRIMARY");
        emoji = new MockEmojiState(7000001L, "storm", false, 12345L);
    }

    @Test
    void constructor_withThreeParams_setsComponentIdLabelAndStyle() {
        assertThat(button.getComponentId()).isEqualTo("storm-button-roll");
        assertThat(button.getLabel()).isEqualTo("Roll Storm");
        assertThat(button.getStyle()).isEqualTo("PRIMARY");
    }

    @Test
    void constructor_withThreeParams_setsDisabledToFalse() {
        assertThat(button.isDisabled()).isFalse();
    }

    @Test
    void constructor_withFourParams_setsAllFields() {
        MockButtonState disabledButton = new MockButtonState("test-button", "Test", "SECONDARY", true);

        assertThat(disabledButton.getComponentId()).isEqualTo("test-button");
        assertThat(disabledButton.getLabel()).isEqualTo("Test");
        assertThat(disabledButton.getStyle()).isEqualTo("SECONDARY");
        assertThat(disabledButton.isDisabled()).isTrue();
    }

    @Test
    void constructor_withFourParams_canSetDisabledToFalse() {
        MockButtonState enabledButton = new MockButtonState("test-button", "Test", "PRIMARY", false);

        assertThat(enabledButton.isDisabled()).isFalse();
    }

    @Test
    void getComponentId_returnsCorrectId() {
        assertThat(button.getComponentId()).isEqualTo("storm-button-roll");
    }

    @Test
    void getLabel_returnsCorrectLabel() {
        assertThat(button.getLabel()).isEqualTo("Roll Storm");
    }

    @Test
    void getStyle_returnsCorrectStyle() {
        assertThat(button.getStyle()).isEqualTo("PRIMARY");
    }

    @Test
    void style_supportsPRIMARY() {
        MockButtonState primaryButton = new MockButtonState("id", "Label", "PRIMARY");
        assertThat(primaryButton.getStyle()).isEqualTo("PRIMARY");
    }

    @Test
    void style_supportsSECONDARY() {
        MockButtonState secondaryButton = new MockButtonState("id", "Label", "SECONDARY");
        assertThat(secondaryButton.getStyle()).isEqualTo("SECONDARY");
    }

    @Test
    void style_supportsSUCCESS() {
        MockButtonState successButton = new MockButtonState("id", "Label", "SUCCESS");
        assertThat(successButton.getStyle()).isEqualTo("SUCCESS");
    }

    @Test
    void style_supportsDANGER() {
        MockButtonState dangerButton = new MockButtonState("id", "Label", "DANGER");
        assertThat(dangerButton.getStyle()).isEqualTo("DANGER");
    }

    @Test
    void style_supportsLINK() {
        MockButtonState linkButton = new MockButtonState("id", "Label", "LINK");
        assertThat(linkButton.getStyle()).isEqualTo("LINK");
    }

    @Test
    void isDisabled_returnsFalseByDefault() {
        assertThat(button.isDisabled()).isFalse();
    }

    @Test
    void setDisabled_updatesDisabledState() {
        button.setDisabled(true);

        assertThat(button.isDisabled()).isTrue();
    }

    @Test
    void setDisabled_canBeToggledMultipleTimes() {
        button.setDisabled(true);
        assertThat(button.isDisabled()).isTrue();

        button.setDisabled(false);
        assertThat(button.isDisabled()).isFalse();

        button.setDisabled(true);
        assertThat(button.isDisabled()).isTrue();
    }

    @Test
    void getEmoji_returnsNullInitially() {
        assertThat(button.getEmoji()).isNull();
    }

    @Test
    void setEmoji_setsTheEmoji() {
        button.setEmoji(emoji);

        assertThat(button.getEmoji()).isEqualTo(emoji);
        assertThat(button.getEmoji().getName()).isEqualTo("storm");
    }

    @Test
    void setEmoji_canBeChanged() {
        button.setEmoji(emoji);

        MockEmojiState newEmoji = new MockEmojiState(7000002L, "atreides", false, 12345L);
        button.setEmoji(newEmoji);

        assertThat(button.getEmoji()).isEqualTo(newEmoji);
        assertThat(button.getEmoji().getName()).isEqualTo("atreides");
    }

    @Test
    void hasEmoji_returnsFalseInitially() {
        assertThat(button.hasEmoji()).isFalse();
    }

    @Test
    void hasEmoji_returnsTrueAfterSettingEmoji() {
        button.setEmoji(emoji);

        assertThat(button.hasEmoji()).isTrue();
    }

    @Test
    void hasEmoji_returnsFalseAfterSettingToNull() {
        button.setEmoji(emoji);
        button.setEmoji(null);

        assertThat(button.hasEmoji()).isFalse();
    }

    @Test
    void getUrl_returnsNullInitially() {
        assertThat(button.getUrl()).isNull();
    }

    @Test
    void setUrl_setsTheUrl() {
        button.setUrl("https://example.com");

        assertThat(button.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void setUrl_isUsedForLinkButtons() {
        MockButtonState linkButton = new MockButtonState("link-id", "Visit Site", "LINK");
        linkButton.setUrl("https://example.com");

        assertThat(linkButton.getStyle()).isEqualTo("LINK");
        assertThat(linkButton.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void button_canHaveBothEmojiAndLabel() {
        button.setEmoji(emoji);

        assertThat(button.getLabel()).isEqualTo("Roll Storm");
        assertThat(button.hasEmoji()).isTrue();
    }

    @Test
    void button_canBeDisabledWithEmoji() {
        button.setEmoji(emoji);
        button.setDisabled(true);

        assertThat(button.isDisabled()).isTrue();
        assertThat(button.hasEmoji()).isTrue();
    }
}
