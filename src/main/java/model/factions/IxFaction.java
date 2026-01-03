package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.MoveType;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class IxFaction extends Faction {
    private int hmsMoves;
    protected Set<String> hmsTerritories;

    public IxFaction(String player, String userName) throws IOException {
        super("Ix", player, userName);

        this.spice = 10;
        this.freeRevival = 1;
        this.emoji = Emojis.IX;
        this.forceEmoji = Emojis.IX_SUBOID;
        this.specialForceEmoji = Emojis.IX_CYBORG;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        this.homeworld = "Ix";
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        game.getTerritories().addHMS();
        game.getTerritories().get("Hidden Mobile Stronghold").addForces("Ix", 3);
        game.getTerritories().get("Hidden Mobile Stronghold").addForces("Ix*", 3);
        Territory ix = game.getTerritories().addHomeworld(game, homeworld, name);
        ix.addForces(name, 10);
        ix.addForces(name + "*", 4);
        game.getHomeworlds().put(name, homeworld);
    }

    @Override
    public boolean hasSpecialForces() {
        return true;
    }

    @Override
    public String forcesStringWithZeroes(int numForces, int numSpecialForces) {
        return numForces + " " + forceEmoji + " " + numSpecialForces + " " + specialForceEmoji;
    }

    public Territory getTerritoryWithHMS() {
        return game.getTerritories().values().stream().filter(t -> t.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().orElseThrow();
    }

    public void startHMSMovement() {
        hmsMoves = 3;
        hmsTerritories = new HashSet<>();
        hmsTerritories.add(getTerritoryWithHMS().getTerritoryName());
        game.getTurnSummary().publish(Emojis.IX + " to decide if they want to move the HMS.");
    }

    public int getHMSMoves() {
        return hmsMoves;
    }

    public void moveHMSOneTerritory(String territoryName) throws InvalidGameStateException {
        if (hmsMoves == 0)
            throw new InvalidGameStateException("The HMS has moved as far as it can.");
        hmsMoves--;
        hmsTerritories.add(territoryName);
        for (Territory territory : game.getTerritories().values()) {
            territory.getForces().removeIf(f -> f.getName().equals("Hidden Mobile Stronghold"));
            game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territory);
        }
        Territory targetTerritory = game.getTerritory(territoryName);
        targetTerritory.addForces("Hidden Mobile Stronghold", 1);
        game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
        game.getTurnSummary().publish(Emojis.IX + " moved the HMS to " + targetTerritory.getTerritoryName() + ".");
        game.setUpdated(UpdateType.MAP);
    }

    public void endHMSMovement() {
        if (hmsMoves == 3)
            hmsTerritories.clear();
        for (String territoryName : hmsTerritories) {
            Territory territory = game.getTerritory(territoryName);
            int spiceInTerritory = territory.getSpice();
            if (spiceInTerritory > 0) {
                Territory hms = game.getTerritory("Hidden Mobile Stronghold");
                int numForcesInHMS = hms.getForces().stream()
                        .filter(f -> !f.getName().equals("Advisor"))
                        .map(Force::getStrength)
                        .reduce(0, Integer::sum);
                int spiceCollected = Math.min(spiceInTerritory, 2 * numForcesInHMS);
                game.getTurnSummary().publish(Emojis.IX + " collects " + spiceCollected + " " + Emojis.SPICE + " from " + territoryName + ".");
                territory.setSpice(spiceInTerritory - spiceCollected);
                addSpice(spiceCollected, "HMS collection in " + territoryName + ".");
            }
        }
        game.getTurnSummary().publish(Emojis.IX + " HMS movement complete.");
        hmsMoves = 0;
        hmsTerritories = null;
        game.setIxHMSActionRequired(false);
    }

    public void presentHMSPlacementChoices() {
        movement.setMoveType(MoveType.HMS_PLACEMENT);
        movement.clear();
        String buttonPrefix = movement.getChoicePrefix();
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice(buttonPrefix + "spice-blow", "Spice Blow Territories"));
        choices.add(new DuneChoice(buttonPrefix + "rock", "Rock Territories"));
        choices.add(new DuneChoice(buttonPrefix + "other", "Somewhere else"));
        chat.reply("Where would you like to place the HMS? " + player, choices);
    }

    public void presentHMSPlacementExecutionChoices() {
        String buttonPrefix = movement.getChoicePrefix();
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice(buttonPrefix + "execute", "Confirm placement"));
        choices.add(new DuneChoice("secondary", buttonPrefix + "start-over", "Start over"));
        chat.reply("Placing the HMS in " + movement.getMovingTo(), choices);
    }

    public void placeHMS() {
        placeHMS(movement.getMovingTo());
    }

    public void placeHMS(String territoryName) {
        Territories territories = game.getTerritories();
        Territory territoryWithHMS = null;
        try {
            territoryWithHMS = getTerritoryWithHMS();
            territoryWithHMS.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
            game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territoryWithHMS);
        } catch (NoSuchElementException ignored) {
            // This is the initial placement of the HMS
        }
        Territory targetTerritory = territories.get(territoryName);
        targetTerritory.addForces("Hidden Mobile Stronghold", 1);
        game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
        game.setIxHMSActionRequired(false);
        if (territoryWithHMS == null) {
            chat.reply("You placed the HMS in " + territoryName + ".");
            game.getTurnSummary().publish(Emojis.IX + " placed the HMS in " + territoryName + ".");
        } else if (territoryWithHMS == targetTerritory) {
            chat.reply("You left the HMS in " + territoryName + ".");
            game.getTurnSummary().publish(Emojis.IX + " left the HMS in " + territoryName + ".");
        } else {
            chat.reply("You moved the HMS to " + territoryName + ".");
            game.getTurnSummary().publish(Emojis.IX + " moved the HMS to " + territoryName + ".");
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void presentStartingCardsListAndChoices() throws InvalidGameStateException {
        setHandLimit(13); // Only needs 7 with Harkonnen in a 6p game, but allowing here for a 12p game with Hark.
        game.getFactions().forEach(_ -> game.drawTreacheryCard(name, false, false));
        if (game.hasHarkonnenFaction() && !game.hasGameOption(GameOption.IX_ONLY_1_CARD_PER_FACTION))
            game.drawTreacheryCard(name, false, false);
        game.getModInfo().publish(Emojis.IX + " has received " + Emojis.TREACHERY + " cards.\nIx player can use buttons or mod can use /setup ix-hand-selection to select theirs.");

        String message = "Select one of the following " + Emojis.TREACHERY + " cards as your starting card. " + player;
        List<String> cardNameAndType = treacheryHand.stream().map(card -> "**" + card.name() + "** _" + card.type() + "_").toList();
        message += "\n\t" + String.join("\n\t", cardNameAndType);
        chat.publish(message);
        presentStartingCardChoices();
    }

    public void presentStartingCardChoices() throws InvalidGameStateException {
        if (game.isSetupFinished())
            throw new InvalidGameStateException("Setup phase is completed.");
        else if (treacheryHand.size() <= 4)
            throw new InvalidGameStateException("You have already selected your card.");
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0; // Integer value used to distinguish button IDs when card names are the same, e.g. Shield and Shield
        for (TreacheryCard card : treacheryHand)
            choices.add(new DuneChoice("ix-starting-card-" + i++ + "-" + card.name(), card.name()));
        chat.reply("", choices);
    }

    public void presentConfirmStartingCardChoices(String cardName) throws InvalidGameStateException {
        if (game.isSetupFinished())
            throw new InvalidGameStateException("Setup phase is completed.");
        else if (treacheryHand.size() <= 4)
            throw new InvalidGameStateException("You have already selected your card.");
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("ix-confirm-start-" + cardName, "Confirm " + cardName));
        choices.add(new DuneChoice("secondary", "ix-confirm-start-reset", "Choose a different card"));
        chat.reply("Confirm your selection of " + cardName.trim() + ".", choices);
    }

    public void startingCard(String ixCardName) throws InvalidGameStateException {
        if (game.isSetupFinished())
            throw new InvalidGameStateException("Setup phase is completed.");
        else if (treacheryHand.size() <= 4)
            throw new InvalidGameStateException("You have already selected your card.");
        TreacheryCard card = treacheryHand.stream().filter(treacheryCard -> treacheryCard.name().equals(ixCardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        LinkedList<TreacheryCard> ixRejects = new LinkedList<>();
        boolean cardToKeepFound = false;
        for (TreacheryCard treacheryCard : treacheryHand) {
            if (!cardToKeepFound && treacheryCard.equals(card)) {
                cardToKeepFound = true;
                continue;
            }
            ixRejects.add(treacheryCard);
        }
        Collections.shuffle(ixRejects);
        ixRejects.forEach(treacheryCard -> game.getTreacheryDeck().add(treacheryCard));
        this.treacheryHand.clear();
        setHandLimit(4);
        addTreacheryCard(card);
        chat.reply("You kept " + ixCardName);
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param amount        The amount of the force.
     * @param isSpecial     Whether the force is special or not.
     * @param toTanks       Whether the force is going to the tanks or not.
     */
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        String forceName = getName() + (isSpecial ? "*" : "");
        removeForces(territoryName, forceName, amount, toTanks, isSpecial);
    }

    /**
     * Get the total spice that would be collected from a territory.  This function does not actually add or subtract
     * spice.  It only calculates the total
     *
     * @param territory The territory to calculate the spice from
     * @return The total spice that would be collected from the territory
     */
    @Override
    public int getSpiceCollectedFromTerritory(Territory territory) {
        int multiplier = hasMiningEquipment() ? 3 : 2;
        int spiceFromSuboids = territory.getForceStrength("Ix") * multiplier;
        int spiceFromCyborgs = territory.getForceStrength("Ix*") * 3;

        int totalSpice = spiceFromSuboids + spiceFromCyborgs;
        return Math.min(totalSpice, territory.getSpice());
    }

    @Override
    public int countFreeStarredRevival() {
        return Math.min(getFreeRevival(), game.getTleilaxuTanks().getForceStrength(name + "*"));
    }

    @Override
    public int baseRevivalCost(int regular, int starred) {
        return regular * 2 + starred * 3;
    }

    /**
     * Give player choices for number of pad revivals
     *
     * @param numRevived  The number revived for free.
     * @return True if the faction can buy more revivals, false if not.
     */
    @Override
    public boolean presentPaidRevivalChoices(int numRevived) throws InvalidGameStateException {
        paidRevivalMessage = null;
        paidRevivalTBD = false;
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        if (tanks.getForceStrength(name + "*") == 0 || spice < revivalCost(0, 1))
            game.getRevival().setCyborgRevivalComplete(true);
        boolean cyborgRevivalComplete = game.getRevival().isCyborgRevivalComplete();
        String idPrefix;
        String idSuffix;
        String labelSuffix;
        String chatMessage;
        if (cyborgRevivalComplete) {
            idPrefix = "revival-forces-";
            idSuffix = "";
            labelSuffix = " Suboid";
            chatMessage = "Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? " + player;
        } else {
            idPrefix = "revival-cyborgs-";
            idSuffix = "-" + numRevived;
            labelSuffix = " Cyborg";
            chatMessage = "Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? " + player;
            if (tanks.getForceStrength(name) > 0)
                chatMessage += "\n" + Emojis.IX_SUBOID + " revivals if possible would be the next step.";
            else
                chatMessage += "\nThere are no " + Emojis.IX_SUBOID + " in the tanks.";
        }
        if (getMaxRevival() > numRevived) {
            int revivableForces = cyborgRevivalComplete ? tanks.getForceStrength(name) : tanks.getForceStrength(name + "*");
            if (revivableForces > 0) {
                List<DuneChoice> choices = new ArrayList<>();
                int maxButton = Math.min(revivableForces, getMaxRevival() - numRevived);
                for (int i = 0; i <= maxButton; i++) {
                    DuneChoice choice = new DuneChoice(idPrefix + i + idSuffix, i + labelSuffix);
                    choice.setDisabled(cyborgRevivalComplete && spice < revivalCost(i, 0) || !cyborgRevivalComplete && spice < revivalCost(0, 1));
                    choices.add(choice);
                }
                chat.publish(chatMessage, choices);
                paidRevivalTBD = true;
            } else {
                paidRevivalMessage = getNoRevivableForcesMessage();
            }
        } else {
            paidRevivalMessage = getRevivedMaximumMessage();
        }
        return paidRevivalTBD;
    }
}
