package model;

//Name = the faction name, or the special troop type (Sardaukar, Advisor, etc)
//value = the strength of the force
public class Force {
   private String name;
   private int strength;

    public Force(String name, int strength) {
        this.name = name;
        this.strength = strength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void subtractStrength(int amount) {
        this.strength -= Math.abs(amount);
    }
}