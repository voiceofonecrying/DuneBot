package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockSelectOptionStateTest {

    private MockSelectOptionState option;
    private MockEmojiState emoji;

    @BeforeEach
    void setUp() {
        option = new MockSelectOptionState("Atreides", "atreides");
        emoji = new MockEmojiState(7000001L, "atreides", false, 12345L);
    }

    @Test
    void constructor_withTwoParams_setsLabelAndValue() {
        assertThat(option.getLabel()).isEqualTo("Atreides");
        assertThat(option.getValue()).isEqualTo("atreides");
    }

    @Test
    void constructor_withTwoParams_setsDefaultOptionToFalse() {
        assertThat(option.isDefaultOption()).isFalse();
    }

    @Test
    void constructor_withTwoParams_setsDescriptionToNull() {
        assertThat(option.getDescription()).isNull();
    }

    @Test
    void constructor_withThreeParams_setsLabelValueAndDescription() {
        MockSelectOptionState optionWithDesc = new MockSelectOptionState(
            "Atreides",
            "atreides",
            "The noble house"
        );

        assertThat(optionWithDesc.getLabel()).isEqualTo("Atreides");
        assertThat(optionWithDesc.getValue()).isEqualTo("atreides");
        assertThat(optionWithDesc.getDescription()).isEqualTo("The noble house");
    }

    @Test
    void constructor_withThreeParams_setsDefaultOptionToFalse() {
        MockSelectOptionState optionWithDesc = new MockSelectOptionState(
            "Atreides",
            "atreides",
            "The noble house"
        );

        assertThat(optionWithDesc.isDefaultOption()).isFalse();
    }

    @Test
    void getLabel_returnsCorrectLabel() {
        assertThat(option.getLabel()).isEqualTo("Atreides");
    }

    @Test
    void getValue_returnsCorrectValue() {
        assertThat(option.getValue()).isEqualTo("atreides");
    }

    @Test
    void getDescription_returnsNullInitially() {
        assertThat(option.getDescription()).isNull();
    }

    @Test
    void setDescription_setsDescription() {
        option.setDescription("The noble house");

        assertThat(option.getDescription()).isEqualTo("The noble house");
    }

    @Test
    void setDescription_canBeChanged() {
        option.setDescription("First description");
        option.setDescription("Second description");

        assertThat(option.getDescription()).isEqualTo("Second description");
    }

    @Test
    void getEmoji_returnsNullInitially() {
        assertThat(option.getEmoji()).isNull();
    }

    @Test
    void setEmoji_setsTheEmoji() {
        option.setEmoji(emoji);

        assertThat(option.getEmoji()).isEqualTo(emoji);
        assertThat(option.getEmoji().getName()).isEqualTo("atreides");
    }

    @Test
    void setEmoji_canBeChanged() {
        option.setEmoji(emoji);

        MockEmojiState newEmoji = new MockEmojiState(7000002L, "harkonnen", false, 12345L);
        option.setEmoji(newEmoji);

        assertThat(option.getEmoji()).isEqualTo(newEmoji);
        assertThat(option.getEmoji().getName()).isEqualTo("harkonnen");
    }

    @Test
    void isDefaultOption_returnsFalseByDefault() {
        assertThat(option.isDefaultOption()).isFalse();
    }

    @Test
    void setDefaultOption_updatesDefaultOption() {
        option.setDefaultOption(true);

        assertThat(option.isDefaultOption()).isTrue();
    }

    @Test
    void setDefaultOption_canBeToggledMultipleTimes() {
        option.setDefaultOption(true);
        assertThat(option.isDefaultOption()).isTrue();

        option.setDefaultOption(false);
        assertThat(option.isDefaultOption()).isFalse();

        option.setDefaultOption(true);
        assertThat(option.isDefaultOption()).isTrue();
    }

    @Test
    void hasEmoji_returnsFalseInitially() {
        assertThat(option.hasEmoji()).isFalse();
    }

    @Test
    void hasEmoji_returnsTrueAfterSettingEmoji() {
        option.setEmoji(emoji);

        assertThat(option.hasEmoji()).isTrue();
    }

    @Test
    void hasEmoji_returnsFalseAfterSettingToNull() {
        option.setEmoji(emoji);
        option.setEmoji(null);

        assertThat(option.hasEmoji()).isFalse();
    }

    @Test
    void hasDescription_returnsFalseForNullDescription() {
        assertThat(option.hasDescription()).isFalse();
    }

    @Test
    void hasDescription_returnsTrueAfterSettingDescription() {
        option.setDescription("A description");

        assertThat(option.hasDescription()).isTrue();
    }

    @Test
    void hasDescription_returnsFalseForEmptyDescription() {
        option.setDescription("");

        assertThat(option.hasDescription()).isFalse();
    }

    @Test
    void hasDescription_returnsTrueForNonEmptyDescription() {
        option.setDescription("Non-empty");

        assertThat(option.hasDescription()).isTrue();
    }

    @Test
    void option_canHaveAllPropertiesSet() {
        option.setDescription("The noble house");
        option.setEmoji(emoji);
        option.setDefaultOption(true);

        assertThat(option.getLabel()).isEqualTo("Atreides");
        assertThat(option.getValue()).isEqualTo("atreides");
        assertThat(option.getDescription()).isEqualTo("The noble house");
        assertThat(option.hasEmoji()).isTrue();
        assertThat(option.isDefaultOption()).isTrue();
    }

    @Test
    void option_labelAndValueCanBeDifferent() {
        MockSelectOptionState customOption = new MockSelectOptionState("Display Name", "internal_value");

        assertThat(customOption.getLabel()).isEqualTo("Display Name");
        assertThat(customOption.getValue()).isEqualTo("internal_value");
    }

    @Test
    void option_canHaveEmojiWithoutDescription() {
        option.setEmoji(emoji);

        assertThat(option.hasEmoji()).isTrue();
        assertThat(option.hasDescription()).isFalse();
    }

    @Test
    void option_canHaveDescriptionWithoutEmoji() {
        option.setDescription("A description");

        assertThat(option.hasDescription()).isTrue();
        assertThat(option.hasEmoji()).isFalse();
    }

    @Test
    void multipleOptions_canHaveSameLabel() {
        MockSelectOptionState option1 = new MockSelectOptionState("Test", "value1");
        MockSelectOptionState option2 = new MockSelectOptionState("Test", "value2");

        assertThat(option1.getLabel()).isEqualTo(option2.getLabel());
        assertThat(option1.getValue()).isNotEqualTo(option2.getValue());
    }
}
