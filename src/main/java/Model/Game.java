package Model;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

@Entity
@Table(name = "game")
public class Game {
    @Id
    @Column(name = "GAME_ID")
    @GeneratedValue(generator = "incrementor")
    @GenericGenerator(name = "incrementor", strategy = "increment")
    private int gameId;
    @Column(name = "GAME_NAME")
    private String name;
    @Column(name = "BG_PREDICTION")
    private String prediction;
    @Column(name = "TURN")
    private int turn;
    @Column(name = "SHIELD_WALL_BROKEN")
    private boolean shieldWallBroken;

    public Game(int gameId, String name, String prediction, int turn, boolean shieldWallBroken) {
        this.gameId = gameId;
        this.name = name;
        this.prediction = prediction;
        this.turn = turn;
        this.shieldWallBroken = shieldWallBroken;
    }
    public Game() {}

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
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
