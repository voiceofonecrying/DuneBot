package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockStringSelectMenuStateTest {

    private MockStringSelectMenuState menu;
    private MockStringSelectMenuState menuWithPlaceholder;

    @BeforeEach
    void setUp() {
        menu = new MockStringSelectMenuState("bidding-menu");
        menuWithPlaceholder = new MockStringSelectMenuState("faction-select", "Select your faction");
    }

    @Test
    void constructor_withOneParam_setsComponentIdAndNullPlaceholder() {
        assertThat(menu.getComponentId()).isEqualTo("bidding-menu");
        assertThat(menu.getPlaceholder()).isNull();
    }

    @Test
    void constructor_withTwoParams_setsComponentIdAndPlaceholder() {
        assertThat(menuWithPlaceholder.getComponentId()).isEqualTo("faction-select");
        assertThat(menuWithPlaceholder.getPlaceholder()).isEqualTo("Select your faction");
    }

    @Test
    void constructor_setsDefaultMinValuesToOne() {
        assertThat(menu.getMinValues()).isEqualTo(1);
    }

    @Test
    void constructor_setsDefaultMaxValuesToOne() {
        assertThat(menu.getMaxValues()).isEqualTo(1);
    }

    @Test
    void constructor_initializesEmptyOptionsList() {
        assertThat(menu.getOptions()).isEmpty();
    }

    @Test
    void getComponentId_returnsCorrectId() {
        assertThat(menu.getComponentId()).isEqualTo("bidding-menu");
    }

    @Test
    void getPlaceholder_returnsCorrectPlaceholder() {
        assertThat(menuWithPlaceholder.getPlaceholder()).isEqualTo("Select your faction");
    }

    @Test
    void setPlaceholder_setsPlaceholder() {
        menu.setPlaceholder("Choose an option");

        assertThat(menu.getPlaceholder()).isEqualTo("Choose an option");
    }

    @Test
    void setPlaceholder_canChangeExistingPlaceholder() {
        menuWithPlaceholder.setPlaceholder("New placeholder");

        assertThat(menuWithPlaceholder.getPlaceholder()).isEqualTo("New placeholder");
    }

    @Test
    void getOptions_returnsEmptyListInitially() {
        assertThat(menu.getOptions()).isEmpty();
    }

    @Test
    void addOption_addsOptionToMenu() {
        MockSelectOptionState option = new MockSelectOptionState("Bid 5", "5");
        menu.addOption(option);

        List<MockSelectOptionState> options = menu.getOptions();

        assertThat(options).hasSize(1);
        assertThat(options.get(0).getLabel()).isEqualTo("Bid 5");
    }

    @Test
    void addOption_addsMultipleOptions() {
        MockSelectOptionState option1 = new MockSelectOptionState("Bid 5", "5");
        MockSelectOptionState option2 = new MockSelectOptionState("Bid 10", "10");
        MockSelectOptionState option3 = new MockSelectOptionState("Auto", "auto");

        menu.addOption(option1);
        menu.addOption(option2);
        menu.addOption(option3);

        List<MockSelectOptionState> options = menu.getOptions();

        assertThat(options).hasSize(3);
        assertThat(options.get(0).getLabel()).isEqualTo("Bid 5");
        assertThat(options.get(1).getLabel()).isEqualTo("Bid 10");
        assertThat(options.get(2).getLabel()).isEqualTo("Auto");
    }

    @Test
    void clearOptions_removesAllOptions() {
        menu.addOption(new MockSelectOptionState("Option 1", "1"));
        menu.addOption(new MockSelectOptionState("Option 2", "2"));

        menu.clearOptions();

        assertThat(menu.getOptions()).isEmpty();
    }

    @Test
    void getOptions_returnsNewListEachTime() {
        menu.addOption(new MockSelectOptionState("Test", "test"));

        List<MockSelectOptionState> options1 = menu.getOptions();
        List<MockSelectOptionState> options2 = menu.getOptions();

        assertThat(options1).isNotSameAs(options2);
    }

    @Test
    void getOptions_modifyingReturnedListDoesNotAffectState() {
        MockSelectOptionState realOption = new MockSelectOptionState("Real", "real");
        menu.addOption(realOption);

        List<MockSelectOptionState> options = menu.getOptions();
        MockSelectOptionState fakeOption = new MockSelectOptionState("Fake", "fake");
        options.add(fakeOption);

        assertThat(menu.getOptions()).hasSize(1);
        assertThat(menu.getOptions().get(0)).isEqualTo(realOption);
    }

    @Test
    void getMinValues_returnsDefaultValue() {
        assertThat(menu.getMinValues()).isEqualTo(1);
    }

    @Test
    void setMinValues_updatesMinValues() {
        menu.setMinValues(2);

        assertThat(menu.getMinValues()).isEqualTo(2);
    }

    @Test
    void setMinValues_canBeSetToZero() {
        menu.setMinValues(0);

        assertThat(menu.getMinValues()).isZero();
    }

    @Test
    void getMaxValues_returnsDefaultValue() {
        assertThat(menu.getMaxValues()).isEqualTo(1);
    }

    @Test
    void setMaxValues_updatesMaxValues() {
        menu.setMaxValues(5);

        assertThat(menu.getMaxValues()).isEqualTo(5);
    }

    @Test
    void setMaxValues_canBeSetToMax() {
        menu.setMaxValues(25);

        assertThat(menu.getMaxValues()).isEqualTo(25);
    }

    @Test
    void hasOptions_returnsFalseInitially() {
        assertThat(menu.hasOptions()).isFalse();
    }

    @Test
    void hasOptions_returnsTrueAfterAddingOption() {
        menu.addOption(new MockSelectOptionState("Test", "test"));

        assertThat(menu.hasOptions()).isTrue();
    }

    @Test
    void hasOptions_returnsFalseAfterClearingOptions() {
        menu.addOption(new MockSelectOptionState("Test", "test"));
        menu.clearOptions();

        assertThat(menu.hasOptions()).isFalse();
    }

    @Test
    void isMultiSelect_returnsFalseWhenMaxValuesIsOne() {
        assertThat(menu.isMultiSelect()).isFalse();
    }

    @Test
    void isMultiSelect_returnsTrueWhenMaxValuesIsGreaterThanOne() {
        menu.setMaxValues(2);

        assertThat(menu.isMultiSelect()).isTrue();
    }

    @Test
    void isMultiSelect_returnsTrueForHighMaxValues() {
        menu.setMaxValues(10);

        assertThat(menu.isMultiSelect()).isTrue();
    }

    @Test
    void menu_canHaveMinAndMaxValuesDifferent() {
        menu.setMinValues(1);
        menu.setMaxValues(3);

        assertThat(menu.getMinValues()).isEqualTo(1);
        assertThat(menu.getMaxValues()).isEqualTo(3);
        assertThat(menu.isMultiSelect()).isTrue();
    }

    @Test
    void menu_canRequireMultipleSelections() {
        menu.setMinValues(2);
        menu.setMaxValues(4);

        assertThat(menu.getMinValues()).isEqualTo(2);
        assertThat(menu.getMaxValues()).isEqualTo(4);
    }

    @Test
    void menu_canHaveAllPropertiesSet() {
        menu.setPlaceholder("Select your choices");
        menu.setMinValues(1);
        menu.setMaxValues(3);
        menu.addOption(new MockSelectOptionState("Option 1", "1"));
        menu.addOption(new MockSelectOptionState("Option 2", "2"));
        menu.addOption(new MockSelectOptionState("Option 3", "3"));

        assertThat(menu.getPlaceholder()).isEqualTo("Select your choices");
        assertThat(menu.getMinValues()).isEqualTo(1);
        assertThat(menu.getMaxValues()).isEqualTo(3);
        assertThat(menu.hasOptions()).isTrue();
        assertThat(menu.getOptions()).hasSize(3);
        assertThat(menu.isMultiSelect()).isTrue();
    }

    @Test
    void menu_withSingleSelection() {
        menu.setPlaceholder("Pick one");
        menu.setMinValues(1);
        menu.setMaxValues(1);
        menu.addOption(new MockSelectOptionState("Only Option", "only"));

        assertThat(menu.isMultiSelect()).isFalse();
        assertThat(menu.getMinValues()).isEqualTo(1);
        assertThat(menu.getMaxValues()).isEqualTo(1);
    }
}
