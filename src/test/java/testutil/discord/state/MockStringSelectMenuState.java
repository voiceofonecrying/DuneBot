package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord string select menu (dropdown).
 *
 * <p>String select menus are dropdown components that allow users to select
 * one or more options from a list. Common uses include faction selection,
 * card choices, and configuration menus.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Component ID</b> - Unique identifier for this menu</li>
 *   <li><b>Placeholder</b> - Text shown when no option is selected</li>
 *   <li><b>Options</b> - List of available choices</li>
 *   <li><b>Min Values</b> - Minimum number of selections required</li>
 *   <li><b>Max Values</b> - Maximum number of selections allowed</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockStringSelectMenuState menu = new MockStringSelectMenuState("bidding-menu", "Select your bid");
 * menu.setMinValues(1);
 * menu.setMaxValues(2);
 *
 * // Add options
 * menu.addOption(new MockSelectOptionState("Bid 5", "5"));
 * menu.addOption(new MockSelectOptionState("Bid 10", "10"));
 * menu.addOption(new MockSelectOptionState("Auto-increment", "auto-increment"));
 *
 * assertThat(menu.getOptions()).hasSize(3);
 * assertThat(menu.getPlaceholder()).isEqualTo("Select your bid");
 * }</pre>
 *
 * @see MockSelectOptionState
 * @see MockMessageState
 */
public class MockStringSelectMenuState {
    private final String componentId;
    private String placeholder;
    private final List<MockSelectOptionState> options = new ArrayList<>();
    private int minValues = 1;
    private int maxValues = 1;

    /**
     * Creates a string select menu.
     *
     * @param componentId Unique identifier for this menu
     * @param placeholder Text shown when no option is selected
     */
    public MockStringSelectMenuState(String componentId, String placeholder) {
        this.componentId = componentId;
        this.placeholder = placeholder;
    }

    /**
     * Creates a string select menu without a placeholder.
     *
     * @param componentId Unique identifier for this menu
     */
    public MockStringSelectMenuState(String componentId) {
        this.componentId = componentId;
        this.placeholder = null;
    }

    /**
     * Gets the component ID (custom ID) of this menu.
     *
     * <p>This is the identifier used in StringSelectInteractionEvent.
     *
     * @return The component ID
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Gets the placeholder text.
     *
     * @return The placeholder, or {@code null} if not set
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the placeholder text.
     *
     * @param placeholder Text to show when no option is selected
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * Gets all options in this menu.
     *
     * @return A new list containing all options (modifications won't affect the menu)
     */
    public List<MockSelectOptionState> getOptions() {
        return new ArrayList<>(options);
    }

    /**
     * Adds an option to this menu.
     *
     * @param option The option to add
     */
    public void addOption(MockSelectOptionState option) {
        options.add(option);
    }

    /**
     * Clears all options from this menu.
     */
    public void clearOptions() {
        options.clear();
    }

    /**
     * Gets the minimum number of selections required.
     *
     * @return Minimum selections (default: 1)
     */
    public int getMinValues() {
        return minValues;
    }

    /**
     * Sets the minimum number of selections required.
     *
     * @param minValues Minimum selections (1-25)
     */
    public void setMinValues(int minValues) {
        this.minValues = minValues;
    }

    /**
     * Gets the maximum number of selections allowed.
     *
     * @return Maximum selections (default: 1)
     */
    public int getMaxValues() {
        return maxValues;
    }

    /**
     * Sets the maximum number of selections allowed.
     *
     * @param maxValues Maximum selections (1-25)
     */
    public void setMaxValues(int maxValues) {
        this.maxValues = maxValues;
    }

    /**
     * Checks if this menu has any options.
     *
     * @return {@code true} if there are options, {@code false} otherwise
     */
    public boolean hasOptions() {
        return !options.isEmpty();
    }

    /**
     * Checks if this menu allows multiple selections.
     *
     * @return {@code true} if max values > 1, {@code false} otherwise
     */
    public boolean isMultiSelect() {
        return maxValues > 1;
    }
}
