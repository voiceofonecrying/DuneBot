package testutil.discord.state;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord message embed.
 *
 * <p>Embeds are rich, formatted message containers that can include titles,
 * descriptions, fields, images, and more. They are commonly used for game
 * state displays, help messages, and structured information.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Title</b> - Large heading at the top</li>
 *   <li><b>Description</b> - Main body text</li>
 *   <li><b>Color</b> - Left border color</li>
 *   <li><b>Fields</b> - List of name-value field pairs</li>
 *   <li><b>Footer</b> - Small text at the bottom</li>
 *   <li><b>Image URL</b> - Large image displayed in the embed</li>
 *   <li><b>Thumbnail URL</b> - Small image displayed in the top-right</li>
 *   <li><b>Author</b> - Author name and icon</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockEmbedState embed = new MockEmbedState();
 * embed.setTitle("Storm Phase");
 * embed.setDescription("The storm has moved!");
 * embed.setColor(Color.BLUE);
 * embed.addField("New Position", "Sector 5", false);
 * embed.addField("Spice", "3", true);
 * embed.setFooter("Turn 3 of 10");
 *
 * message.addEmbed(embed);
 * }</pre>
 *
 * @see MockEmbedFieldState
 * @see MockMessageState
 */
public class MockEmbedState {
    private String title;
    private String description;
    private Color color;
    private final List<MockEmbedFieldState> fields = new ArrayList<>();
    private String footer;
    private String imageUrl;
    private String thumbnailUrl;
    private String authorName;
    private String authorIconUrl;

    /**
     * Creates an empty embed.
     */
    public MockEmbedState() {
    }

    /**
     * Gets the title of this embed.
     *
     * @return The title, or {@code null} if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of this embed.
     *
     * @param title The title text
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description (main body text) of this embed.
     *
     * @return The description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this embed.
     *
     * @param description The description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the color of this embed's left border.
     *
     * @return The color, or {@code null} if not set
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color of this embed's left border.
     *
     * @param color The color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Gets all fields in this embed.
     *
     * @return A new list containing all fields (modifications won't affect the embed)
     */
    public List<MockEmbedFieldState> getFields() {
        return new ArrayList<>(fields);
    }

    /**
     * Adds a field to this embed.
     *
     * @param field The field to add
     */
    public void addField(MockEmbedFieldState field) {
        fields.add(field);
    }

    /**
     * Adds a field to this embed.
     *
     * <p>Convenience method that creates a field and adds it.
     *
     * @param name The field name
     * @param value The field value
     * @param inline Whether the field should display inline
     */
    public void addField(String name, String value, boolean inline) {
        fields.add(new MockEmbedFieldState(name, value, inline));
    }

    /**
     * Clears all fields from this embed.
     */
    public void clearFields() {
        fields.clear();
    }

    /**
     * Gets the footer text.
     *
     * @return The footer, or {@code null} if not set
     */
    public String getFooter() {
        return footer;
    }

    /**
     * Sets the footer text.
     *
     * @param footer The footer text
     */
    public void setFooter(String footer) {
        this.footer = footer;
    }

    /**
     * Gets the large image URL.
     *
     * @return The image URL, or {@code null} if not set
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the large image URL.
     *
     * @param imageUrl The image URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the thumbnail image URL.
     *
     * @return The thumbnail URL, or {@code null} if not set
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Sets the thumbnail image URL.
     *
     * @param thumbnailUrl The thumbnail URL
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Gets the author name.
     *
     * @return The author name, or {@code null} if not set
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Sets the author name.
     *
     * @param authorName The author name
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * Gets the author icon URL.
     *
     * @return The author icon URL, or {@code null} if not set
     */
    public String getAuthorIconUrl() {
        return authorIconUrl;
    }

    /**
     * Sets the author icon URL.
     *
     * @param authorIconUrl The author icon URL
     */
    public void setAuthorIconUrl(String authorIconUrl) {
        this.authorIconUrl = authorIconUrl;
    }

    /**
     * Checks if this embed has any fields.
     *
     * @return {@code true} if there are fields, {@code false} otherwise
     */
    public boolean hasFields() {
        return !fields.isEmpty();
    }
}
