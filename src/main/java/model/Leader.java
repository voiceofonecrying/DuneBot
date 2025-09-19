package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.BTFaction;
import model.factions.Faction;

public class Leader {
    private final String name;
    private final int value;
    private final String originalFactionName;
    private String emojiFaction;
    private LeaderSkillCard skillCard;
    private boolean faceDown;
    private String battleTerritoryName;
    private boolean pulledBehindShield;
    private String homebrewImageMessage;

    public Leader(String name, int value, String originalFactionName, LeaderSkillCard skillCard, boolean faceDown) {
        this.name = name;
        this.value = value;
        this.originalFactionName = originalFactionName;
        this.skillCard = skillCard;
        this.faceDown = faceDown;
        this.battleTerritoryName = null;
        this.pulledBehindShield = false;
    }

    public Leader(String name, int value, String originalFactionName, String emojiFaction, LeaderSkillCard skillCard, boolean faceDown) {
        this.name = name;
        this.value = value;
        this.originalFactionName = originalFactionName;
        this.emojiFaction = emojiFaction;
        this.skillCard = skillCard;
        this.faceDown = faceDown;
        this.battleTerritoryName = null;
        this.pulledBehindShield = false;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getOriginalFactionName() {
        return originalFactionName;
    }

    public String getNameAndValueString() {
        String valueString = String.valueOf(value);
        if (name.equals("Zoal"))
            valueString = "X";
        return name + " (" + valueString + ")";
    }

    public String getEmoiNameAndValueString() {
        String emoji = originalFactionName;
        if (emojiFaction != null)
            emoji = emojiFaction;
        return Emojis.getFactionEmoji(emoji) + " " + getNameAndValueString();
    }

    public int getAssassinationValue() {
        return name.equals("Zoal") ? 3 : value;
    }

    public int getRevivalCost(Faction revivingFaction) {
        int cost = value;
        if (name.equals("Zoal"))
            cost = 3;
        else if (revivingFaction instanceof BTFaction || revivingFaction.getAlly().equals("BT"))
            cost = Math.ceilDiv(cost, 2);
        return cost;
    }

    public LeaderSkillCard getSkillCard() {
        return skillCard;
    }

    public void removeSkillCard() {
        skillCard = null;
    }

    public void setSkillCard(LeaderSkillCard skillCard) throws InvalidGameStateException {
        if (this.skillCard != null)
            throw new InvalidGameStateException(name + " has a skill that must be removed before adding a new one.");
        this.skillCard = skillCard;
    }

    public boolean isFaceDown() {
        return faceDown;
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
    }

    public String getBattleTerritoryName() {
        return battleTerritoryName;
    }

    public void setBattleTerritoryName(String battleTerritoryName) {
        this.battleTerritoryName = battleTerritoryName;
    }

    public boolean isPulledBehindShield() {
        return pulledBehindShield;
    }

    public void setPulledBehindShield(boolean pulledBehindShield) {
        this.pulledBehindShield = pulledBehindShield;
    }

    public String getHomebrewImageMessage() {
        return homebrewImageMessage;
    }

    public void setHomebrewImageMessage(String homebrewImageMessage) {
        this.homebrewImageMessage = homebrewImageMessage;
    }
}
