package model;

import constants.Emojis;
import model.factions.Faction;

import java.util.Objects;

public final class TraitorCard {
    private final String name;
    private final String factionName;
    private final int strength;

    public TraitorCard(String name, String factionName, int strength) {
        this.name = name;
        this.factionName = factionName;
        this.strength = strength;
    }

    public String getName() {
        return name;
    }

    public String getNameAndStrengthString() {
        String valueString = String.valueOf(strength);
        if (name.equals("Zoal"))
            valueString = "X";
        return name + " (" + valueString + ")";
    }

    public String getFactionEmoji() {
        if (name.equals("Cheap Hero"))
            return Emojis.WEIRDING;
        return Emojis.getFactionEmoji(factionName);
    }

    public String getEmojiAndNameString() {
        if (name.equals("Cheap Hero"))
            return name;
        return Emojis.getFactionEmoji(factionName) + " " + name;
    }

    public String getEmojiNameAndStrengthString() {
        if (name.equals("Cheap Hero"))
            return getNameAndStrengthString();
        return Emojis.getFactionEmoji(factionName) + " " + getNameAndStrengthString();
    }

    public boolean canBeCalledAgainst(Faction faction) {
        return factionName.equals(faction.getName()) || factionName.equals("Any");
    }

    public boolean isHarkonnenTraitor() {
        return factionName.equals("Harkonnen");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TraitorCard) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.factionName, that.factionName) &&
                this.strength == that.strength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, factionName, strength);
    }

    @Override
    public String toString() {
        return "TraitorCard[" +
                "name=" + name + ", " +
                "factionName=" + factionName + ", " +
                "strength=" + strength + ']';
    }
}
