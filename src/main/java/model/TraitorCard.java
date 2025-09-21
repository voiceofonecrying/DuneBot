package model;

import constants.Emojis;
import model.factions.Faction;

import java.util.Objects;

public final class TraitorCard {
    private final String name;
    private final String factionName;
    private String emojiFaction;
    private final int strength;

    public TraitorCard(String name, String factionName, int strength) {
        this.name = name;
        this.factionName = factionName;
        this.strength = strength;
    }

    public TraitorCard(String name, String factionName, String emojiFaction, int strength) {
        this.name = name;
        this.factionName = factionName;
        this.emojiFaction = emojiFaction;
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

    public String getFactionName() {
        return factionName;
    }

    public void setEmojiFaction(String emojiFaction) {
        this.emojiFaction = emojiFaction;
    }

    public String getFactionEmoji() {
        if (name.equals("Cheap Hero"))
            return Emojis.WEIRDING;
        String emoji = factionName;
        if (emojiFaction != null)
            emoji = emojiFaction;
        return Emojis.getFactionEmoji(emoji);
    }

    public String getEmojiAndNameString() {
        if (name.equals("Cheap Hero"))
            return name;
        return getFactionEmoji() + " " + name;
    }

    public String getEmojiNameAndStrengthString() {
        if (name.equals("Cheap Hero"))
            return getNameAndStrengthString();
        return getFactionEmoji() + " " + getNameAndStrengthString();
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
