package Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

@Entity
public class Game {
    @Id
    int id;
    String name;
    String prediction;
    int turn;
    boolean shieldWallBroken;

    public Game (List<Player> players) {
    }
    protected Game () {}


}
