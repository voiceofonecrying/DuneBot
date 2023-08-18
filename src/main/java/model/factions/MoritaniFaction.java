package model.factions;

import model.Game;

import java.io.IOException;

public class MoritaniFaction extends Faction {
    public MoritaniFaction(String player, String userName, Game game) throws IOException {
        super("Moritani", player, userName, game);
    }
}
