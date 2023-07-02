package model;

import java.util.Objects;

//Name = the faction name, or the special troop type (Sardaukar, Advisor, etc)
//value = the strength of the force
public class Force {
   private final String name;
   private String factionName;
   private int strength;

    public Force(String name, int strength) {
        this.name = name;
        this.strength = strength;
        this.factionName = name.replace("*", "");
    }

    public String getName() {
        return name;
    }

    public String getFactionName() {
        if (factionName == null) {
            factionName = name.replace("*", "");
        }
        return factionName;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public void addStrength(int amount) {
        if (amount < 0) throw new IllegalArgumentException("You cannot add a negative strength value to a force.");
        this.strength += amount;
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