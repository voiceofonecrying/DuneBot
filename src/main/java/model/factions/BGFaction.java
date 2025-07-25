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
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class BGFaction extends Faction {
    private String predictionFactionName;
    private int predictionRound;
    private boolean denyingAllyVoice;
    private final Set<String> intrudedTerritories;

    public BGFaction(String player, String userName) throws IOException {
        super("BG", player, userName);

        this.spice = 5;
        this.freeRevival = 1;
        this.emoji = Emojis.BG;
        this.highThreshold = 11;
        this.lowThreshold = 10;
        this.occupiedIncome = 1;
        this.homeworld = "Wallach IX";
        this.intrudedTerritories = new HashSet<>();
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

    public void presentPredictedFactionChoices() {
        List<DuneChoice> choices = game.getFactions().stream().filter(f -> !(f instanceof BGFaction)).map(f -> new DuneChoice("primary", "bg-prediction-faction-" + f.getName(), null, f.getEmoji(), false)).toList();
        List<String> factionsAndPlayers = game.getFactions().stream().filter(f -> !(f instanceof BGFaction)).map(f -> f.getEmoji() + " - " + f.getPlayer()).toList();
        String message = "Which faction do you predict to win? " + player + "\n" + String.join("\n", factionsAndPlayers);
        chat.publish(message, choices);
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
    public void setPredictionRound(int predictionRound) throws InvalidGameStateException {
        if (predictionFactionName == null)
            throw new InvalidGameStateException("Predicted faction must be selected first.");
        if (predictionRound <= 0 || predictionRound > 10)
            throw new IllegalArgumentException("Prediction round must be between 1 and 10");
        this.predictionRound = predictionRound;
        chat.publish("You predict " + Emojis.getFactionEmoji(predictionFactionName) + " to win on turn " + predictionRound + ".");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void presentPredictedTurnChoices(String factionName) {
        setPredictionFactionName(factionName);
        List<DuneChoice> choices = IntStream.rangeClosed(1, 10).mapToObj(i -> new DuneChoice("bg-prediction-turn-" + i, String.valueOf(i))).toList();
        chat.reply("Which turn do you predict " + Emojis.getFactionEmoji(predictionFactionName) + " to win? " + player, choices);
    }

    @Override
    public void presentStartingForcesChoices() {
        shipment.clear();
        String buttonSuffix = "-starting-forces";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("stronghold" + buttonSuffix, "Stronghold"));
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
        chat.publish("Where would you like to place your starting " + Emojis.BG_ADVISOR + " or " + Emojis.BG_FIGHTER + "? " + player, choices);
        game.getGameActions().publish(emoji + " will place their starting " + Emojis.BG_ADVISOR + " or " + Emojis.BG_FIGHTER);
    }

    @Override
    public void presentStartingForcesExecutionChoices() {
        String buttonSuffix = "-starting-forces";
        shipment.setForce(1);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("execute-shipment" + buttonSuffix, "Confirm placement"));
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        Territory territory = game.getTerritory(shipment.getTerritoryName());
        String forceEmoji = (territory.getForces().isEmpty() || shipment.getTerritoryName().equals("Polar Sink")) ? Emojis.BG_FIGHTER : Emojis.BG_ADVISOR;
        chat.reply("Placing **1 " + forceEmoji + "** in " + shipment.getTerritoryName(), choices);
    }

    @Override
    public boolean placeChosenStartingForces() {
        chat.reply("Initial force placement complete.");
        String territoryName = shipment.getTerritoryName();
        Territory territory = game.getTerritory(territoryName);
        if (territory.getForces().isEmpty())
            placeForcesFromReserves(territory, 1, false);
        else
            placeAdvisorsFromReserves(game, territory, 1);
        return true;
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
     * Places forces or advisors from reserves into this territory.
     *
     * @param territory The territory to place the force in.
     * @param amount    The number of regular forces or advisors to place.
     * @param starredAmount   The number of starred forces to place. Always ignored for BGFaction
     *
     * @return A string with faction emoji and the number of forces or advisors placed.
     */
    @Override
    protected String placeAndReportWhatWasPlaced(Territory territory, int amount, int starredAmount) {
        if (territory.hasForce("Advisor")) {
            placeAdvisorsFromReserves(game, territory, amount);
            return emoji + ": " + amount + " " + Emojis.BG_ADVISOR;
        } else
            return super.placeAndReportWhatWasPlaced(territory, amount, starredAmount);
    }

    /**
     * Places BG advisors from reserves into this territory.
     * Reports removal from reserves to ledger.
     * Switches homeworld to low threshold if applicable.
     *
     * @param game      The Game instance.
     * @param territory The territory to place the force in.
     * @param amount    The number of forces to place.
     */
    public void placeAdvisorsFromReserves(Game game, Territory territory, int amount) {
        String forceName = "Advisor";
        removeReserves(amount);
        ledger.publish(MessageFormat.format("{0} {1} removed from reserves.", amount, Emojis.getForceEmoji(forceName)));
        territory.addForces(forceName, amount);
        checkForLowThreshold();
        game.setUpdated(UpdateType.MAP);
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
            intrudedTerritories.remove(territory.getTerritoryName());
            from = "BG";
            to = "Advisor";
        } else if (territory.hasForce("Advisor")) {
            from = "Advisor";
            to = "BG";
        } else {
            throw new IllegalStateException("Territory does not have BG forces");
        }

        int count = territory.getForceStrength(from);
        territory.removeForces(game, from, count);
        territory.addForces(to, count);
        game.setUpdated(UpdateType.MAP);
    }

    public void dontFlipFighters(Game game, String territoryName) {
        intrudedTerritories.remove(territoryName);
        game.getTurnSummary().publish(emoji + " don't flip in " + territoryName);
        chat.reply("You will not flip.");
    }

    public void presentAdvisorChoices(Game game, Faction targetFaction, Territory targetTerritory) {
        if (targetFaction instanceof BGFaction || targetFaction instanceof FremenFaction)
            return;
        if (targetTerritory instanceof HomeworldTerritory || targetTerritory.getTerritoryName().equals("Polar Sink"))
            return;
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !isHighThreshold)
            return;
        List<DuneChoice> choices = new LinkedList<>();
        String territoryName = targetTerritory.getTerritoryName();
        DuneChoice adviseChoice = new DuneChoice("bg-advise-" + territoryName, "Advise");
        adviseChoice.setDisabled(!game.hasGameOption(GameOption.BG_COEXIST_WITH_ALLY) && targetFaction.getName().equals(ally) && !targetFaction.getName().equals("Ecaz") && targetTerritory.hasActiveFaction(targetFaction));
        choices.add(adviseChoice);
        choices.add(new DuneChoice("secondary", "bg-advise-Polar Sink", "Advise to Polar Sink"));
        if (game.hasGameOption(GameOption.HOMEWORLDS))
            choices.add(new DuneChoice("secondary", "bg-ht", "Advise 2 to Polar Sink"));
        choices.add(new DuneChoice("danger", "bg-dont-advise-" + territoryName, "No"));
        chat.publish("Would you like to advise the shipment to " + territoryName + "? " + player, choices);
    }

    /**
     * Places advisors from reserves into this territory.
     * Temporarily flips advisors to fighters so Faction::placeForceFromReserves creates a consistent force.
     * Leave the force as fighters if the territory is Polar Sink
     *
     * @param game      The Game instance.
     * @param territory The territory to place the advisor in.
     * @param amount    The number of advisors to place.
     */
    public void advise(Game game, Territory territory, int amount) throws InvalidGameStateException {
        boolean isPolarSink = territory.getTerritoryName().equals("Polar Sink");
        if (!isPolarSink) {
            if (!game.hasGameOption(GameOption.BG_COEXIST_WITH_ALLY) && ally != null && !ally.equals("Ecaz") && territory.getActiveFactions(game).contains(game.getFaction(ally)))
                throw new InvalidGameStateException("BG cannot co-exist with their ally.");
            if (territory.hasForce("BG"))
                throw new InvalidGameStateException("BG cannot send an advisor to a territory with BG fighters.");
        }

        if (territory.hasForce("Advisor")) {
            int advisors = territory.getForceStrength("Advisor");
            territory.addForces("BG", advisors);
            territory.removeForces(game, "Advisor", advisors);
        }
        placeForcesFromReserves(territory, amount, false);
        if (!isPolarSink) {
            int fighters = territory.getForceStrength("BG");
            territory.removeForces(game, "BG", fighters);
            territory.addForces("Advisor", fighters);
        }

        game.getTurnSummary().publish(Emojis.BG + " sent " + amount + " " + Emojis.BG_ADVISOR + " to " + territory.getTerritoryName());
        game.checkForTerrorTrigger(territory, this, amount);
        chat.reply("You sent " + amount + " " + Emojis.BG_ADVISOR + " to " + territory.getTerritoryName() + ".");
        game.setUpdated(UpdateType.MAP);
    }

    public void presentFlipMessage(Game game, String territoryName) {
        if (territoryName.equals("Polar Sink") || game.getTerritory(territoryName) instanceof HomeworldTerritory)
            return;
        intrudedTerritories.add(territoryName);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("bg-flip-" + territoryName, "Flip"));
        choices.add(new DuneChoice("secondary", "bg-dont-flip-" + territoryName, "Don't Flip"));
        game.getTurnSummary().publish(Emojis.BG + " to decide if they want to flip to " + Emojis.BG_ADVISOR + " in " + territoryName + ".");
        chat.publish("Will you flip to " + Emojis.BG_ADVISOR + " in " + territoryName + "? " + player, choices);
    }

    public boolean hasIntrudedTerritoriesDecisions() {
        return !intrudedTerritories.isEmpty();
    }

    public String getIntrudedTerritoriesString() {
        return String.join(", ", intrudedTerritories);
    }

    public void clearIntrudedTerritories() {
        intrudedTerritories.clear();
    }

    public boolean isDenyingAllyVoice() {
        return denyingAllyVoice;
    }

    public void setDenyingAllyVoice(boolean denyingAllyVoice) {
        this.denyingAllyVoice = denyingAllyVoice;
        ledger.publish("You are " + (denyingAllyVoice ? "denying " : "granting ") + "The Voice to " + Emojis.getFactionEmoji(ally));
    }
}
