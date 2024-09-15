package model;

import exceptions.InvalidGameStateException;
import model.factions.BTFaction;
import model.factions.Faction;

public class Leader {
    private final String name;
    private final int value;
    private final String originalFactionName;
    private LeaderSkillCard skillCard;
    private final boolean faceDown;
    private String battleTerritoryName;
    private boolean pulledBehindShield;

    public Leader(String name, int value, String originalFactionName, LeaderSkillCard skillCard, boolean faceDown) {
        this.name = name;
        this.value = value;
        this.originalFactionName = originalFactionName;
        this.skillCard = skillCard;
        this.faceDown = faceDown;
        this.battleTerritoryName = null;
        this.pulledBehindShield = false;
    }

    public Leader(String name, int value, LeaderSkillCard skillCard, boolean faceDown) {
        this.name = name;
        this.value = value;
        this.originalFactionName = "None";
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

    public int getStandardRevivalCost() {
        return name.equals("Zoal") ? 3 : value;
    }

    public int getRevivalCost(Faction revivingFaction) {
        int cost = getStandardRevivalCost();
        if (revivingFaction instanceof BTFaction || revivingFaction.getAlly().equals("BT"))
            cost = Math.ceilDiv(cost, 2);
        return cost;
    }

    public LeaderSkillCard getSkillCard() {
        return skillCard;
    }

    public void setSkillCard(LeaderSkillCard skillCard) throws InvalidGameStateException {
        if (this.skillCard != null)
            throw new InvalidGameStateException(name + " has a skill that must be removed before adding a new one.");
        this.skillCard = skillCard;
    }

    public boolean isFaceDown() {
        return faceDown;
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
}
