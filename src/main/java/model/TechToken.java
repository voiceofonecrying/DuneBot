package model;

public class TechToken {
    private final String name;
    private int spice;

    public TechToken(String name) {
        this.name = name;
        this.spice = 0;
    }

    public void addSpice(int spice) {
        this.spice += spice;
    }

    public int collectSpice() {
        int spice = this.spice;
        this.spice = 0;
        return spice;
    }

    public String getName() {
        return name;
    }
}
