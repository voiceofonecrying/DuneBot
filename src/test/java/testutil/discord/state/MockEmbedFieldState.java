package testutil.discord.state;

/**
 * Stores state for a single field within a Discord message embed.
 *
 * <p>Embed fields are name-value pairs displayed in a structured format
 * within an embed. They can be displayed inline (side-by-side) or in
 * separate rows.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Name</b> - The field heading/title</li>
 *   <li><b>Value</b> - The field content/body</li>
 *   <li><b>Inline</b> - Whether this field should display inline with others</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockEmbedFieldState field = new MockEmbedFieldState("Spice", "5", true);
 *
 * assertThat(field.getName()).isEqualTo("Spice");
 * assertThat(field.getValue()).isEqualTo("5");
 * assertThat(field.isInline()).isTrue();
 * }</pre>
 *
 * @see MockEmbedState
 */
public class MockEmbedFieldState {
    private final String name;
    private final String value;
    private final boolean inline;

    /**
     * Creates an embed field.
     *
     * @param name The field name/heading
     * @param value The field value/content
     * @param inline Whether to display this field inline
     */
    public MockEmbedFieldState(String name, String value, boolean inline) {
        this.name = name;
        this.value = value;
        this.inline = inline;
    }

    /**
     * Gets the name (heading) of this field.
     *
     * @return The field name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value (content) of this field.
     *
     * @return The field value
     */
    public String getValue() {
        return value;
    }

    /**
     * Checks if this field is displayed inline.
     *
     * <p>Inline fields can appear side-by-side (up to 3 per row),
     * while non-inline fields take up a full row.
     *
     * @return {@code true} if inline, {@code false} otherwise
     */
    public boolean isInline() {
        return inline;
    }
}
