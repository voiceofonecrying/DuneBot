package model;

public class StormCard extends Card{
    private final int stormMovement;

    public StormCard(int stormMovement) {
        super(Integer.toString(stormMovement));
        this.stormMovement = stormMovement;
    }

    public int getStormMovement() {
        return stormMovement;
    }
}
