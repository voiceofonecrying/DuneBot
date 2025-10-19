package testutil.discord.state;

/**
 * Stores state for a single option in a Discord string select menu.
 *
 * <p>Select options are choices within a dropdown menu. Each option has a
 * label (displayed text), value (internal identifier), optional description,
 * optional emoji, and can be marked as the default selection.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Label</b> - Text displayed in the dropdown</li>
 *   <li><b>Value</b> - Internal value returned when selected</li>
 *   <li><b>Description</b> - Optional additional description text</li>
 *   <li><b>Emoji</b> - Optional emoji shown next to the label</li>
 *   <li><b>Default</b> - Whether this option is selected by default</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockSelectOptionState option1 = new MockSelectOptionState("Atreides", "atreides");
 * option1.setDescription("The noble house");
 *
 * MockEmojiState emoji = guild.createEmoji("atreides", false);
 * option1.setEmoji(emoji);
 * option1.setDefaultOption(true);
 *
 * assertThat(option1.getLabel()).isEqualTo("Atreides");
 * assertThat(option1.getValue()).isEqualTo("atreides");
 * }</pre>
 *
 * @see MockStringSelectMenuState
 * @see MockEmojiState
 */
public class MockSelectOptionState {
    private final String label;
    private final String value;
    private String description;
    private MockEmojiState emoji;
    private boolean defaultOption;

    /**
     * Creates a select option with label and value.
     *
     * @param label The display text
     * @param value The internal value
     */
    public MockSelectOptionState(String label, String value) {
        this.label = label;
        this.value = value;
        this.defaultOption = false;
    }

    /**
     * Creates a select option with label, value, and description.
     *
     * @param label The display text
     * @param value The internal value
     * @param description Additional description text
     */
    public MockSelectOptionState(String label, String value, String description) {
        this.label = label;
        this.value = value;
        this.description = description;
        this.defaultOption = false;
    }

    /**
     * Gets the label (display text) of this option.
     *
     * @return The label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the value of this option.
     *
     * <p>This is the internal identifier returned when the option is selected.
     *
     * @return The value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the description of this option.
     *
     * @return The description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this option.
     *
     * @param description The description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the emoji for this option.
     *
     * @return The emoji, or {@code null} if not set
     */
    public MockEmojiState getEmoji() {
        return emoji;
    }

    /**
     * Sets the emoji for this option.
     *
     * @param emoji The emoji to display
     */
    public void setEmoji(MockEmojiState emoji) {
        this.emoji = emoji;
    }

    /**
     * Checks if this option is the default selection.
     *
     * @return {@code true} if default, {@code false} otherwise
     */
    public boolean isDefaultOption() {
        return defaultOption;
    }

    /**
     * Sets whether this option is the default selection.
     *
     * @param defaultOption {@code true} to make this the default, {@code false} otherwise
     */
    public void setDefaultOption(boolean defaultOption) {
        this.defaultOption = defaultOption;
    }

    /**
     * Checks if this option has an emoji.
     *
     * @return {@code true} if an emoji is set, {@code false} otherwise
     */
    public boolean hasEmoji() {
        return emoji != null;
    }

    /**
     * Checks if this option has a description.
     *
     * @return {@code true} if a description is set, {@code false} otherwise
     */
    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }
}
