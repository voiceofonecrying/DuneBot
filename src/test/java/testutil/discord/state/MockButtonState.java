package testutil.discord.state;

/**
 * Stores state for a Discord button component.
 *
 * <p>Buttons are interactive components that can be attached to messages.
 * When clicked, they trigger a ButtonInteractionEvent. Common uses include
 * game actions, confirmations, and menu navigation.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Component ID</b> - Unique identifier for this button (e.g., "storm-button-roll")</li>
 *   <li><b>Label</b> - Text displayed on the button</li>
 *   <li><b>Style</b> - Button color/style (PRIMARY, SECONDARY, SUCCESS, DANGER, LINK)</li>
 *   <li><b>Disabled</b> - Whether the button is disabled</li>
 *   <li><b>Emoji</b> - Optional emoji shown on the button</li>
 *   <li><b>URL</b> - For LINK-style buttons, the URL to open</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create a primary button with emoji
 * MockEmojiState stormEmoji = guild.createEmoji("storm", false);
 * MockButtonState button = new MockButtonState("storm-button-roll", "Roll Storm", "PRIMARY");
 * button.setEmoji(stormEmoji);
 *
 * // Add to message
 * message.addButton(button);
 *
 * assertThat(button.getComponentId()).isEqualTo("storm-button-roll");
 * assertThat(button.getLabel()).isEqualTo("Roll Storm");
 * assertThat(button.isDisabled()).isFalse();
 * }</pre>
 *
 * @see MockMessageState
 * @see MockEmojiState
 */
public class MockButtonState {
    private final String componentId;
    private final String label;
    private final String style; // PRIMARY, SECONDARY, SUCCESS, DANGER, LINK
    private boolean disabled;
    private MockEmojiState emoji;
    private String url; // For LINK-style buttons

    /**
     * Creates a button with the specified component ID, label, and style.
     *
     * @param componentId Unique identifier for this button
     * @param label Text displayed on the button
     * @param style Button style (PRIMARY, SECONDARY, SUCCESS, DANGER, LINK)
     */
    public MockButtonState(String componentId, String label, String style) {
        this.componentId = componentId;
        this.label = label;
        this.style = style;
        this.disabled = false;
    }

    /**
     * Creates a disabled/enabled button.
     *
     * @param componentId Unique identifier for this button
     * @param label Text displayed on the button
     * @param style Button style
     * @param disabled Whether the button is disabled
     */
    public MockButtonState(String componentId, String label, String style, boolean disabled) {
        this.componentId = componentId;
        this.label = label;
        this.style = style;
        this.disabled = disabled;
    }

    /**
     * Gets the component ID (custom ID) of this button.
     *
     * <p>This is the identifier used in ButtonInteractionEvent to determine
     * which button was clicked.
     *
     * @return The component ID
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Gets the label text displayed on this button.
     *
     * @return The button label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the style of this button.
     *
     * @return The button style (PRIMARY, SECONDARY, SUCCESS, DANGER, LINK)
     */
    public String getStyle() {
        return style;
    }

    /**
     * Checks if this button is disabled.
     *
     * @return {@code true} if disabled, {@code false} if enabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets whether this button is disabled.
     *
     * @param disabled {@code true} to disable, {@code false} to enable
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Gets the emoji shown on this button, if any.
     *
     * @return The emoji, or {@code null} if no emoji is set
     */
    public MockEmojiState getEmoji() {
        return emoji;
    }

    /**
     * Sets the emoji to show on this button.
     *
     * @param emoji The emoji to display
     */
    public void setEmoji(MockEmojiState emoji) {
        this.emoji = emoji;
    }

    /**
     * Gets the URL for LINK-style buttons.
     *
     * @return The URL, or {@code null} if this is not a LINK button
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL for LINK-style buttons.
     *
     * @param url The URL to open when clicked
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Checks if this button has an emoji.
     *
     * @return {@code true} if an emoji is set, {@code false} otherwise
     */
    public boolean hasEmoji() {
        return emoji != null;
    }
}
