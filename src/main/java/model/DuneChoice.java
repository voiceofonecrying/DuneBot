package model;

public class DuneChoice {
    private final String type;
    private final String id;
    private final String label;
    private String emoji;
    private boolean disabled;

    /**
     * DuneChoice constructor with all parameters
     *
     * @param type primary, secondary, success, or danger
     * @param id Must be present, must be unique, must not clash with other choice/button ids
     * @param label The text to show the user. May be null, but then an emoji string is required
     * @param emoji The emoji to place on the button. Get the value from the constants.Emojis class. May be null, but then a label is required
     * @param disabled If the button should be grayed out and not pressable
     */
    public DuneChoice(String type, String id, String label, String emoji, boolean disabled) {
        this.type = type;
        this.id = id;
        this.emoji = emoji;
        this.label = label;
        this.disabled = disabled;
    }

    public DuneChoice(String type, String id, String label) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.disabled = false;
    }

    public DuneChoice(String id, String label) {
        this.type = "primary";
        this.id = id;
        this.label = label;
        this.disabled = false;
    }

    public DuneChoice(String id, String label, boolean disabled) {
        this.type = "primary";
        this.id = id;
        this.label = label;
        this.disabled = disabled;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
