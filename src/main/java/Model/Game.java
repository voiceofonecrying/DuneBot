package Model;

public class Game {

    private String name;
    private int turn;
    private boolean shieldWallBroken;

    public Game(String name, int turn, boolean shieldWallBroken) {
        this.name = name;
        this.turn = turn;
        this.shieldWallBroken = shieldWallBroken;
    }
    public Game() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public boolean isShieldWallBroken() {
        return shieldWallBroken;
    }

    public void setShieldWallBroken(boolean shieldWallBroken) {
        this.shieldWallBroken = shieldWallBroken;
    }
}
