package model;

public class DuneChoice {
    private final String type;
    private final String id;
    private final String label;

    DuneChoice(String type, String id, String label) {
        this.type = type;
        this.id = id;
        this.label = label;
    }

    DuneChoice(String id, String label) {
        this.type = "primary";
        this.id = id;
        this.label = label;
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
}
