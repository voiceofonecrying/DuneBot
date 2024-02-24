package model;

public class Leader {
    private final String name;
    private final int value;
    private final LeaderSkillCard skillCard;
    private final boolean faceDown;

    public Leader(String name, int value, LeaderSkillCard skillCard, boolean faceDown) {
        this.name = name;
        this.value = value;
        this.skillCard = skillCard;
        this.faceDown = faceDown;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public LeaderSkillCard getSkillCard() {
        return skillCard;
    }

    public boolean isFaceDown() {
        return faceDown;
    }
}
