package model;

public class DuneChoice {
    private final String type;
    private final String id;
    private final String label;
    private boolean disabled;

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

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
