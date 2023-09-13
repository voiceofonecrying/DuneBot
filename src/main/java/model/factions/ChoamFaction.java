package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import model.Force;
import model.Game;

import java.io.IOException;

public class ChoamFaction extends Faction {
    private int firstInflationRound;
    private ChoamInflationType firstInflationType;

    public ChoamFaction(String player, String userName, Game game) throws IOException {
        super("CHOAM", player, userName, game);

        setSpice(2);
        this.freeRevival = 0;
        this.reserves = new Force("CHOAM", 20);
        this.emoji = Emojis.CHOAM;
        this.highThreshold = 11;
        this.lowThreshold = 10;
        this.occupiedIncome = 2;
        this.handLimit = 5;
        this.firstInflationRound = 0;
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
     * Sets the first round where inflation is active.
     *
     * @param firstInflationRound the first round where inflation is active.
     * @param firstInflationType  the type of inflation that is active in the first round.
     */
    public void setFirstInflation(int firstInflationRound, ChoamInflationType firstInflationType) {
        if (firstInflationRound < 2) {
            throw new IllegalArgumentException("First inflation round must be at least 2");
        }

        if (firstInflationRound > 10) {
            throw new IllegalArgumentException("First inflation round must be at most 10");
        }

        this.firstInflationRound = firstInflationRound;
        this.firstInflationType = firstInflationType;
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
}
