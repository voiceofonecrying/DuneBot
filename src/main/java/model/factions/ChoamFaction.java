package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;

import java.io.IOException;

public class ChoamFaction extends Faction {
    private int firstInflationRound;
    private ChoamInflationType firstInflationType;

    public ChoamFaction(String player, String userName, Game game) throws IOException {
        super("CHOAM", player, userName, game);

        setSpice(2);
        this.freeRevival = 0;
        this.maxRevival = 20;
        this.emoji = Emojis.CHOAM;
        this.highThreshold = 11;
        this.lowThreshold = 10;
        this.homeworld = "Tupile";
        Territory tupile = game.getTerritories().addHomeworld(game, homeworld, name);
        tupile.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
        this.occupiedIncome = 2;
        this.handLimit = 5;
        clearInflation();
    }

    @Override
    public int getMaxRevival() {
        return maxRevival;
    }

    @Override
    public void setMaxRevival(int maxRevival) {
        // CHOAM revival limit always remains 20
        this.maxRevival = 20;
    }

    @Override
    public void setDecliningCharity(boolean decliningCharity) throws InvalidGameStateException {
        throw new InvalidGameStateException("CHOAM never declines charity.");
    }

    /**
     * @return the first round where inflation is active.
     */
    public int getFirstInflationRound() {
        return firstInflationRound;
    }

    /**
     * @return the type of inflation that is active in the first round.
     */
    public ChoamInflationType getFirstInflationType() {
        return firstInflationType;
    }

    /**
     * Sets the inflation type for the next applicable round.
     *
     * @param firstInflationType  the type of inflation that is active in the first applicable round.
     */
    public void setFirstInflation(ChoamInflationType firstInflationType) throws InvalidGameStateException {
        if (firstInflationRound != 0)
            throw new InvalidGameStateException("Inflation has already been activated.");

        firstInflationRound = game.getTurn();
        if (game.getPhase() > 3) firstInflationRound++;
        if (firstInflationRound <= 1) firstInflationRound = 2;
        this.firstInflationType = firstInflationType;
        game.getTurnSummary().publish(Emojis.CHOAM + " set inflation to " + firstInflationType + " for turn " + firstInflationRound);
    }

    /**
     * Clears inflation to allow it to be set for a different turn.
     */
    public void clearInflation() {
        firstInflationRound = 0;
        firstInflationType = null;
    }

    /**
     * @return the opposite inflation type of the given inflation type.
     */
    private ChoamInflationType oppositeChoamInflationType(ChoamInflationType choamInflationType) {
        if (choamInflationType == ChoamInflationType.DOUBLE) {
            return ChoamInflationType.CANCEL;
        } else if (choamInflationType == ChoamInflationType.CANCEL) {
            return ChoamInflationType.DOUBLE;
        } else {
            return null;
        }
    }

    /**
     * @return the inflation type for the given round.
     */
    public ChoamInflationType getInflationType(int round) {
        if (round == getFirstInflationRound()) {
            return getFirstInflationType();
        } else if (round == getFirstInflationRound() + 1) {
            return oppositeChoamInflationType(getFirstInflationType());
        } else {
            return null;
        }
    }

    /**
     * @return the multiplier for the given round.
     */
    public int getChoamMultiplier(int round) {
        if (getInflationType(round) == ChoamInflationType.DOUBLE) {
            return 2;
        } else if (getInflationType(round) == ChoamInflationType.CANCEL) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int baseRevivalCost(int regular, int starred) {
        return regular + starred;
    }
}
