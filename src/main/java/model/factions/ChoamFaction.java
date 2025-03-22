package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.TreacheryCard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChoamFaction extends Faction {
    private int firstInflationRound;
    private ChoamInflationType firstInflationType;
    private boolean allySpiceForBattle;

    public ChoamFaction(String player, String userName) throws IOException {
        super("CHOAM", player, userName);

        this.spice = 2;
        this.freeRevival = 0;
        this.maxRevival = 20;
        this.emoji = Emojis.CHOAM;
        this.highThreshold = 11;
        this.lowThreshold = 10;
        this.homeworld = "Tupile";
        this.occupiedIncome = 2;
        this.handLimit = 5;
        clearInflation();
        allySpiceForBattle = false;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory tupile = game.getTerritories().addHomeworld(game, homeworld, name);
        tupile.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
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
        game.getTurnSummary().publish(Emojis.CHOAM + " set Inflation to " + firstInflationType + " for turn " + firstInflationRound);
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

    public void swapCardWithAlly(String choamCardName, String allyCardName) {
        Faction allyFaction = game.getFaction(getAlly());
        TreacheryCard choamCard = getTreacheryCard(choamCardName);
        TreacheryCard allyCard = allyFaction.getTreacheryCard(allyCardName);
        handLimit++;
        game.transferCard(allyFaction, this, allyCard);
        game.transferCard(this, allyFaction, choamCard);
        handLimit--;
        game.getTurnSummary().publish(Emojis.CHOAM + " swaps a " + Emojis.TREACHERY + " card with " + allyFaction.getEmoji() + ".");
    }

    @Override
    public int baseRevivalCost(int regular, int starred) {
        return regular + starred;
    }

    @Override
    public void resetAllySpiceSupportAfterShipping(Game game) {
        // Choam ally support remains active through Battle Phase
    }

    public boolean isAllySpiceForBattle() {
        return allySpiceForBattle;
    }

    public void setAllySpiceForBattle(boolean allySpiceForBattle) {
        this.allySpiceForBattle = allySpiceForBattle;
    }

    @Override
    public int getBattleSupport() {
        return allySpiceForBattle ? getSpiceForAlly() : 0;
    }

    @Override
    public String getSpiceSupportPhasesString() {
        return allySpiceForBattle ? " for bidding, shipping and battles!" : " for bidding and shipping only!";
    }

    @Override
    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        super.performMentatPauseActions(extortionTokenTriggered);
        if (firstInflationRound == 0) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("choam-inflation-double", "Yes, Double side up"));
            choices.add(new DuneChoice("choam-inflation-cancel", "Yes, Cancel side up"));
            choices.add(new DuneChoice("choam-inflation-not-yet", "No"));
            chat.publish("Would you like to set Inflation? " + player, choices);
        } else {
            int doubleRound = firstInflationRound;
            if (firstInflationType == ChoamInflationType.CANCEL) doubleRound++;

            if (doubleRound == game.getTurn() + 1)
                game.getTurnSummary().publish("No bribes may be made while the " + Emojis.CHOAM + " Inflation token is Double side up.");
            else if (doubleRound == game.getTurn())
                game.getTurnSummary().publish("Bribes may be made again. The Inflation Token is no longer Double side up.");
        }
    }
}
