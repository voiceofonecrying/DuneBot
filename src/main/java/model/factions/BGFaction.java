package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class BGFaction extends Faction {
    private String predictionFactionName;
    private int predictionRound;

    public BGFaction(String player, String userName, Game game) {
        super("BG", player, userName, game);

        setSpice(5);
        this.freeRevival = 1;
        this.reserves = new Force("BG", 20);
        this.emoji = Emojis.BG;
    }

    /**
     * @return The name of the faction that the Bene Gesserit player has predicted will win the game
     */
    public String getPredictionFactionName() {
        return predictionFactionName;
    }

    /**
     * @param predictionFactionName The name of the faction that the Bene Gesserit player has predicted will win the
     *                              game
     */
    public void setPredictionFactionName(String predictionFactionName) {
        this.predictionFactionName = predictionFactionName;
    }

    /**
     * @return The round in which the Bene Gesserit player has predicted that the faction will win the game
     */
    public int getPredictionRound() {
        return predictionRound;
    }

    /**
     * @param predictionRound The round in which the Bene Gesserit player has predicted that the faction will
     *                        win the game
     */
    public void setPredictionRound(int predictionRound) {
        if (predictionRound <= 0 || predictionRound > 10) {
            throw new IllegalArgumentException("Prediction round must be between 1 and 10");
        }
        this.predictionRound = predictionRound;
    }
}
