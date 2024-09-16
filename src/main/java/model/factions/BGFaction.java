package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.HomeworldTerritory;
import model.Territory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BGFaction extends Faction {
    private String predictionFactionName;
    private int predictionRound;

    public BGFaction(String player, String userName) throws IOException {
        super("BG", player, userName);

        setSpice(5);
        this.freeRevival = 1;
        this.emoji = Emojis.BG;
        this.highThreshold = 11;
        this.lowThreshold = 10;
        this.occupiedIncome = 1;
        this.homeworld = "Wallach IX";
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory wallachIX = game.getTerritories().addHomeworld(game, homeworld, name);
        wallachIX.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
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
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
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
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public int homeworldDialAdvantage(Game game, Territory territory) {
        String territoryName = territory.getTerritoryName();
        if (game.hasGameOption(GameOption.HOMEWORLDS) && homeworld.equals(territoryName))
            return isHighThreshold() ? 3 : 2;
        return 0;
    }


    @Override
    public void setDecliningCharity(boolean decliningCharity) throws InvalidGameStateException {
        throw new InvalidGameStateException("BG never decline charity.");
    }

    @Override
    protected boolean doesNotHaveKarama() {
        if (treacheryHand.stream().anyMatch(c -> c.type().equals("Worthless Card")))
            return false;
        return super.doesNotHaveKarama();
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param amount        The amount of the force.
     * @param isSpecial     Whether the force is special or not.
     * @param toTanks       Whether the force is going to the tanks or not.
     */
    @Override
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        Territory territory = getGame().getTerritory(territoryName);

        if (isSpecial) {
            throw new IllegalArgumentException("Faction does not have special forces");
        }

        if (territory.hasForce("BG")) {
            removeForces(territoryName, "BG", amount, toTanks, false);
        } else {
            removeForces(territoryName, "Advisor", amount, toTanks, false);
        }
    }

    /**
     * Flip BG forces in a territory between normal and advisor.
     *
     * @param territory Territory in which to flip forces
     */
    public void flipForces(Territory territory) {
        String from, to;

        if (territory.hasForce("BG")) {
            from = "BG";
            to = "Advisor";
        } else if (territory.hasForce("Advisor")) {
            from = "Advisor";
            to = "BG";
        } else {
            throw new IllegalStateException("Territory does not have BG forces");
        }

        int count = territory.getForceStrength(from);
        territory.removeForce(from);
        territory.addForces(to, count);
    }

    public void presentAdvisorButtons(Game game, Faction targetFaction, Territory targetTerritory) {
        if (!(targetFaction instanceof BGFaction || targetFaction instanceof FremenFaction)
                && !(game.hasGameOption(GameOption.HOMEWORLDS)
                        && !isHighThreshold()
                        && !(targetTerritory instanceof HomeworldTerritory)
        )) {
            List<DuneChoice> choices = new LinkedList<>();
            String territoryName = targetTerritory.getTerritoryName();
            choices.add(new DuneChoice("bg-advise-" + territoryName, "Advise"));
            choices.add(new DuneChoice("secondary", "bg-advise-Polar Sink", "Advise to Polar Sink"));
            if (game.hasGameOption(GameOption.HOMEWORLDS))
                choices.add(new DuneChoice("secondary", "bg-ht", "Advise 2 to Polar Sink"));
            choices.add(new DuneChoice("danger", "bg-dont-advise-" + territoryName, "No"));
            chat.publish(Emojis.BG + " Would you like to advise the shipment to " + territoryName + "? " + game.getFaction("BG").getPlayer(), choices);
        }
    }

    public void advise(Game game, Territory territory, int amount) throws InvalidGameStateException {
        boolean isPolarSink = territory.getTerritoryName().equals("Polar Sink");
        if (!isPolarSink && territory.hasForce("BG"))
            throw new InvalidGameStateException("BG cannot send an advisor to a territory with BG fighters.");
        territory.placeForceFromReserves(game, this, amount, false);
        if (!isPolarSink) {
            // placeForcesFromReserves flipped advisors to fighters, so flip them back
            int fighters = territory.getForceStrength("BG");
            territory.getForces().removeIf(force -> force.getName().equals("BG"));
            territory.addForces("Advisor", fighters);
        }
        game.getTurnSummary().publish(Emojis.BG + " sent " + amount + " " + Emojis.BG_ADVISOR + " to " + territory.getTerritoryName());
        if (game.hasFaction("Moritani"))
            ((MoritaniFaction) game.getFaction("Moritani")).checkForTerrorTrigger(territory, this, amount);
    }

    public void bgFlipMessageAndButtons(Game game, String territoryName) {
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("bg-flip-" + territoryName, "Flip"));
        choices.add(new DuneChoice("secondary", "bg-dont-flip-" + territoryName, "Don't Flip"));
        game.getTurnSummary().publish(Emojis.BG + " to decide whether they want to flip to " + Emojis.BG_ADVISOR + " in " + territoryName);
        chat.publish("Will you flip to " + Emojis.BG_ADVISOR + " in " + territoryName + "? " + game.getFaction("BG").getPlayer(), choices);
    }

}
