package model;

import java.util.Objects;

public class Force {
    private final String name;
    private final String factionName;
    private final int strength;

    public Force(String name, int strength) {
        this.name = name;
        this.strength = strength;
        if (name.equals("Advisor"))
            this.factionName = "BG";
        else if (name.equals("NoField"))
            this.factionName = "Richese";
        else
            this.factionName = name.replace("*", "");
    }

    public String getName() {
        return name;
    }

    public String getFactionName() {
        return factionName;
    }

    public int getStrength() {
        return strength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass() == String.class && o.equals(this.getName())) return true;
        if (getClass() != o.getClass()) return false;
        Force force = (Force) o;
        return name.equals(force.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " forces: " + strength;
    }
}