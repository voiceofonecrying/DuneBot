package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IxFaction extends Faction {
    public IxFaction(String player, String userName) throws IOException {
        super("Ix", player, userName);

        this.spice = 10;
        this.freeRevival = 1;
        this.emoji = Emojis.IX;
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
    public String forcesStringWithZeroes(int numForces, int numSpecialForces) {
        return numForces + " " + Emojis.getForceEmoji(name) + " " + numSpecialForces + " " + Emojis.getForceEmoji(name + "*");
    }

    public void presentHMSPlacementChoices() {
        shipment.clear();
        String buttonSuffix = "-hms-placement";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
        chat.reply("Where would you like to place the HMS? " + player, choices);
    }

    public void presentHMSPlacementExecutionChoices() {
        String buttonSuffix = "-hms-placement";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("execute-shipment" + buttonSuffix, "Confirm placement"));
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        chat.reply("Placing the HMS in " + shipment.getTerritoryName(), choices);
    }

    public void placeHMS() {
        placeHMS(shipment.getTerritoryName());
    }

    public void placeHMS(String territoryName) {
        Territories territories = game.getTerritories();
        Territory territoryWithHMS = territories.values().stream().filter(t -> t.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().orElse(null);
        if (territoryWithHMS != null) {
            territoryWithHMS.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
            game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territoryWithHMS);
        }
        Territory targetTerritory = territories.get(territoryName);
        targetTerritory.addForces("Hidden Mobile Stronghold", 1);
        game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
        game.setIxHMSActionRequired(false);
        if (territoryWithHMS == null) {
            chat.reply("You placed the HMS in " + territoryName);
            game.getTurnSummary().publish(Emojis.IX + " placed the HMS in " + territoryName);
        } else if (territoryWithHMS == targetTerritory) {
            chat.reply("You left the HMS in " + territoryName);
            game.getTurnSummary().publish(Emojis.IX + " left the HMS in " + territoryName);
        } else {
            chat.reply("You moved the HMS to " + territoryName);
            game.getTurnSummary().publish(Emojis.IX + " moved the HMS to " + territoryName);
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void presentStartingCardsListAndChoices() throws InvalidGameStateException {
        setHandLimit(13); // Only needs 7 with Harkonnen in a 6p game, but allowing here for a 12p game with Hark.
        for (Faction ignored : game.getFactions())
            game.drawTreacheryCard(name, false, false);
        if (game.hasFaction("Harkonnen") && !game.hasGameOption(GameOption.IX_ONLY_1_CARD_PER_FACTION))
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

    public void presentRejectedCardLocationChoices(String cardName, int rejectionTurn) throws InvalidGameStateException {
        if (rejectionTurn != game.getTurn())
            throw new InvalidGameStateException("Button is from turn " + rejectionTurn);
        else if (!game.getBidding().isIxRejectOutstanding())
            throw new InvalidGameStateException("You have already sent a card back.");
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("ix-reject-" + game.getTurn() + "-" + cardName + "-top", "Top"));
        choices.add(new DuneChoice("ix-reject-" + game.getTurn() + "-" + cardName + "-bottom", "Bottom"));
        choices.add(new DuneChoice("secondary", "ix-reject-reset", "Choose a different card"));
        chat.reply("Where do you want to send " + cardName + "?", choices);
    }

    public void presentRejectConfirmationChoices(String cardName, String location, int rejectionTurn) throws InvalidGameStateException {
        if (rejectionTurn != game.getTurn())
            throw new InvalidGameStateException("Button is from turn " + rejectionTurn);
        else if (!game.getBidding().isIxRejectOutstanding())
            throw new InvalidGameStateException("You have already sent a card back.");
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "ix-confirm-reject-" + cardName + "-" + location, "Confirm " + cardName + " to " + location));
        choices.add(new DuneChoice("ix-confirm-reject-technology-" + cardName + "-" + location, "Confirm and use Technology on first card"));
        choices.add(new DuneChoice("secondary", "ix-reject-reset", "Start over"));
        chat.reply("Confirm your selection of " + cardName.trim() + " to " + location + ".", choices);
    }

    public void sendCardBack(String cardName, String location, boolean requestTechnology) throws InvalidGameStateException {
        if (!game.getBidding().isIxRejectOutstanding())
            throw new InvalidGameStateException("You have already sent a card back.");
        game.getBidding().putBackIxCard(game, cardName, location, requestTechnology);
        chat.reply("You sent " + cardName.trim() + " to the " + location.toLowerCase() + " of the deck.");
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
