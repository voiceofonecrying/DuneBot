package Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "game")
public class Game {
    @Id
    @Column(name = "GAME_ID")
    int gameId;
    @Column(name = "GAME_NAME")
    String name;
    @Column(name = "BG_PREDICTION")
    String prediction;
    @Column(name = "TURN")
    int turn;
    @Column(name = "SHIELD_WALL_BROKEN")
    boolean shieldWallBroken;

    public Game (List<Player> players) {
    }
    protected Game () {}


}
