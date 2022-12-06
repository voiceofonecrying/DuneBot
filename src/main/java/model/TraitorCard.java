package model;

public class TraitorCard extends Card{
    private final String factionName;
    private final int strength;
    public TraitorCard(String name, String factionName, int strength) {
        super(name);
        this.factionName = factionName;
        this.strength = strength;
    }
}
