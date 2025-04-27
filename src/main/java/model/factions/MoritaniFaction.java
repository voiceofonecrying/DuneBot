package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import model.topics.DuneTopic;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class MoritaniFaction extends Faction {
    private final List<String> terrorTokens;
    private final List<String> assassinationTargets;
    private boolean newAssassinationTargetNeeded;
    protected boolean htRemovalTBD;
    protected boolean removedTerrorTokenWithHT;

    public MoritaniFaction(String player, String userName) throws IOException {
        super("Moritani", player, userName);

        this.spice = 12;
        this.freeRevival = 2;
        this.emoji = Emojis.MORITANI;
        this.highThreshold = 8;
        this.lowThreshold = 7;
        this.occupiedIncome = 2;
        this.homeworld = "Grumman";
        this.terrorTokens = new LinkedList<>();
        this.assassinationTargets = new LinkedList<>();
        this.newAssassinationTargetNeeded = false;
        this.htRemovalTBD = false;
        this.removedTerrorTokenWithHT = false;

        terrorTokens.add("Assassination");
        terrorTokens.add("Atomics");
        terrorTokens.add("Extortion");
        terrorTokens.add("Robbery");
        terrorTokens.add("Sabotage");
        terrorTokens.add("Sneak Attack");
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory grumman = game.getTerritories().addHomeworld(game, homeworld, name);
        grumman.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
        game.createDukeVidal();
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
        chat.publish("Where would you like to place your starting 6 " + Emojis.MORITANI_TROOP + "? " + player, choices);
    }

    @Override
    public void presentStartingForcesExecutionChoices() {
        String buttonSuffix = "-starting-forces";
        shipment.setForce(6);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("execute-shipment" + buttonSuffix, "Confirm placement"));
        choices.add(new DuneChoice("secondary", "reset-shipment" + buttonSuffix, "Start over"));
        chat.reply("Placing **6 " + Emojis.MORITANI_TROOP + "** in " + shipment.getTerritoryName(), choices);
    }

    @Override
    public boolean placeChosenStartingForces() throws InvalidGameStateException {
        chat.reply("Initial force placement complete.");
        executeShipment(game, false, true);
        return true;
    }

    public void assassinateLeader(Faction triggeringFaction, Leader leader) {
        if (game.getLeaderTanks().contains(leader))
            throw new IllegalArgumentException("Moritani cannot assassintate a leader in the tanks.");
        int spiceGained = leader.getAssassinationValue();
        game.getTurnSummary().publish(emoji + " collect " + spiceGained + " " + Emojis.SPICE + " by assassinating " + leader. getName() + "!");
        game.killLeader(triggeringFaction, leader.getName());
        addSpice(spiceGained, "assassination of " + leader.getName());
    }

    public void triggerTerrorToken(Faction triggeringFaction, Territory location, String terror) throws InvalidGameStateException {
        if (ally != null && ally.equals(triggeringFaction.getName()))
            throw new InvalidGameStateException("You cannot trigger against your ally.");
        DuneTopic turnSummary = game.getTurnSummary();
        game.getTerritories().getTerritoryWithTerrorToken(terror).removeTerrorToken(game, terror, false);
        turnSummary.publish(emoji + " have triggered their " + terror + " Terror Token in " + location.getTerritoryName() + " against " + triggeringFaction.getEmoji() + "!");
        chat.reply("You have triggered your " + terror + " Terror Token in " + location.getTerritoryName() + " against " + triggeringFaction.getEmoji() + ".");

        switch (terror) {
            case "Assassination" -> {
                List<Leader> leaders = new ArrayList<>();
                triggeringFaction.getLeaders().stream()
                        .filter(l -> !l.getName().equals("Kwisatz Haderach"))
                        .forEach(leaders::add);
                Collections.shuffle(leaders);
                int numLeaders = leaders.size();
                if (numLeaders == 0)
                    turnSummary.publish(triggeringFaction.getEmoji() + " has no leaders to assassinate.");
                else
                    assassinateLeader(triggeringFaction, leaders.getFirst());
            }
            case "Atomics" -> {
                location.setAftermathToken(true);
                for (Force force : location.getForces().stream().toList()) {
                    if (force.getName().contains("*"))
                        game.removeForcesAndReportToTurnSummary(location.getTerritoryName(), game.getFaction(force.getFactionName()), 0, force.getStrength(), true, false);
                    else if (force.getName().equals("Advisor"))
                        game.removeAdvisorsAndReportToTurnSummary(location.getTerritoryName(), game.getFaction(force.getFactionName()), force.getStrength(), true);
                    else
                        game.removeForcesAndReportToTurnSummary(location.getTerritoryName(), game.getFaction(force.getFactionName()), force.getStrength(), 0, true, false);
                }
                reduceHandLimitDueToAtomics(this);
                if (ally != null)
                    reduceHandLimitDueToAtomics(game.getFaction(ally));
            }
            case "Extortion" -> {
                game.triggerExtortionToken();
                addFrontOfShieldSpice(5);
                turnSummary.publish("During Mentat Pause, any faction in storm order may pay " + emoji + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game.");
            }
            case "Robbery" -> {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("moritani-robbery-rob-" + triggeringFaction.getName(), "Steal half of their spice"));
                choices.add(new DuneChoice("moritani-robbery-draw", "Draw a card from the deck"));
                chat.publish("Your terrorist in " + location.getTerritoryName() + " can rob the " + triggeringFaction.getEmoji() +
                        "! What would you like to do?", choices);
            }
            case "Sabotage" -> {
                Collections.shuffle(triggeringFaction.getTreacheryHand());
                String cardName = triggeringFaction.getTreacheryHand().getFirst().name();
                triggeringFaction.discard(cardName);
                turnSummary.publish(emoji + " took " + cardName + " from " + triggeringFaction.getEmoji() + " with Sabotage and discarded it.");
                List<DuneChoice> treacheryCards = new LinkedList<>();
                for (TreacheryCard card : getTreacheryHand()) {
                    treacheryCards.add(new DuneChoice("moritani-sabotage-give-card-" + triggeringFaction.getName() + "-" + card.name(), card.name()));
                }
                treacheryCards.add(new DuneChoice("secondary", "moritani-sabotage-no-card-" + triggeringFaction.getName(), "Don't send a card"));
                chat.publish("Give a treachery card from your hand to " + triggeringFaction.getEmoji() + "?", treacheryCards);
            }
            case "Sneak Attack" -> {
                List<DuneChoice> choices = new LinkedList<>();
                for (int i = 1; i < 6; i++) {
                    String digit = String.valueOf(i);
                    DuneChoice choice = new DuneChoice("moritani-sneak-attack-" + digit + "-" + location.getTerritoryName(), digit);
                    choice.setDisabled(getReservesStrength() < i);
                    choices.add(choice);
                }
                chat.publish("How many forces do you want to send on your sneak attack?", choices);
            }
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void reduceHandLimitDueToAtomics(Faction faction) {
        List<TreacheryCard> hand = faction.getTreacheryHand();
        faction.setHandLimit(faction.getHandLimit() - 1);
        int handLimit = faction.getHandLimit();
        game.getTurnSummary().publish(faction.getEmoji() + " " + Emojis.TREACHERY + " limit has been reduced to " + handLimit + ".");
        if (hand.size() > handLimit)
            faction.discard(hand.get((int) (Math.random() * hand.size())).name());
    }

    public void reduceHandLmitIfNecessary(Faction faction) {
        if (handLimit == 3)
            reduceHandLimitDueToAtomics(faction);
    }

    public void restoreHandLimitIfNecessary(Faction faction) {
        if (handLimit == 3) {
            faction.setHandLimit(faction.getHandLimit() + 1);
            game.getTurnSummary().publish(faction.getEmoji() + " " + Emojis.TREACHERY + " limit has been restored to " + faction.getHandLimit() + ".");
        }
    }

    public void robberyRob(String factionName) {
        Faction faction = game.getFaction(factionName);
        int spiceToSteal = Math.ceilDiv(faction.getSpice(), 2);
        if (spiceToSteal == 0) {
            ledger.publish(faction.getEmoji() + " had no " + Emojis.SPICE + " to steal");
        } else {
            faction.subtractSpice(spiceToSteal, "stolen by " + emoji + " Robbery");
            addSpice(spiceToSteal, "stolen from " + faction.getEmoji() + " with Robbery");
        }
        game.getTurnSummary().publish(emoji + " stole " + spiceToSteal + " " + Emojis.SPICE + " from " + faction.getEmoji() + " with Robbery");
    }

    public void robberyDraw() {
        LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
        if (treacheryDeck.isEmpty()) {
            game.getTurnSummary().publish("The " + Emojis.TREACHERY + " deck was empty and has been replenished from the discard pile.");
            List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
            treacheryDeck.addAll(treacheryDiscard);
            Collections.shuffle(treacheryDeck);
            treacheryDiscard.clear();
        }
        TreacheryCard card = game.getTreacheryDeck().pollLast();
        String cardName = Objects.requireNonNull(card).name();
        if (treacheryHand.size() < handLimit) {
            addTreacheryCard(card);
            ledger.publish(cardName + " drawn from deck.");
        } else {
            handLimit++;
            game.setRobberyDiscardOutstanding(true);
            addTreacheryCard(card);
            ledger.publish(cardName + " drawn from deck.");
            List<DuneChoice> choices = new ArrayList<>();
            int i = 1;
            for (TreacheryCard c : treacheryHand) {
                DuneChoice duneChoice = new DuneChoice("moritani-robbery-discard-" + c.name() + "-" + i, c.name());
                choices.add(duneChoice);
                i++;
            }
            chat.publish("You have drawn " + cardName + ", but your hand was full. What would you like to discard?", choices);
        }
        game.getTurnSummary().publish(emoji + " has drawn a " + Emojis.TREACHERY + " card with Robbery.");
    }

    public void robberyDiscard(String cardName) {
        discard(cardName);
        handLimit--;
        game.setRobberyDiscardOutstanding(false);
    }

    public void sendTerrorTokenMessage(String territory) {
        List<DuneChoice> choices = new LinkedList<>();
        for (String terror : terrorTokens)
            choices.add(new DuneChoice("moritani-terror-selected-" + terror + "-" + territory, terror));
        chat.reply("Which terror token would you like to place?", choices);
    }

    public void placeTerrorToken(Territory territory, String terrorTokenName) throws InvalidGameStateException {
        if (!terrorTokens.remove(terrorTokenName))
            throw new IllegalArgumentException("Moritani does not have the " + terrorTokenName + " Terror Token.");
        boolean highTreshold = territory.hasTerrorToken();
        territory.addTerrorToken(game, terrorTokenName);
        String message = "A " + emoji + " Terror Token was placed in " + territory.getTerritoryName();
        message += highTreshold ? " with Grumman High Treshold ability." : ".";
        game.getTurnSummary().publish(message);
        chat.reply(terrorTokenName + " Terror Token was placed in " + territory.getTerritoryName() + ".");
        ledger.publish(terrorTokenName + " Terror Token was placed in " + territory.getTerritoryName() + ".");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void moveTerrorToken(Territory toTerritory, String terrorTokenName) throws InvalidGameStateException {
        Territory fromTerritory = game.getTerritories().getTerritoryWithTerrorToken(terrorTokenName);
        game.getTerritories().removeTerrorTokenFromMap(game, terrorTokenName, false);
        toTerritory.addTerrorToken(game, terrorTokenName);
        game.getTurnSummary().publish("The " + emoji + " Terror Token in " + fromTerritory.getTerritoryName() + " was moved to " + toTerritory.getTerritoryName() + ".");
        chat.reply(terrorTokenName + " Terror Token was moved to " + toTerritory.getTerritoryName() + " from " + fromTerritory.getTerritoryName() + ".");
        ledger.publish(terrorTokenName + " Terror Token was moved to " + toTerritory.getTerritoryName() + " from " + fromTerritory.getTerritoryName() + ".");
    }

    public void removeTerrorTokenWithHighThreshold(String territoryName, String terrorTokenName) {
        htRemovalTBD = false;
        if (territoryName.isEmpty()) {
            chat.reply("You will not remove a Terror Token.");
            return;
        }
        Territory territory = game.getTerritory(territoryName);
        territory.removeTerrorToken(game, terrorTokenName, true);
        removedTerrorTokenWithHT = true;
        addSpice(4, terrorTokenName + " Terror Token returned to supply");
        game.getTurnSummary().publish(emoji + " has removed a Terror Token from " + territory.getTerritoryName() + " for 4 " + Emojis.SPICE);
        chat.reply("You removed " + terrorTokenName + " from " + territory.getTerritoryName() + ".");
    }

    public void getDukeVidal() {
        if (getLeader("Duke Vidal").isEmpty())
            addLeader(game.getDukeVidal());
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }

    public void addTerrorToken(String name) {
        terrorTokens.add(name);
        ledger.publish(name + " Terror Token was added to your hand.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void startSpiceCollectionPhase() {
        removedTerrorTokenWithHT = false;
        if (!game.hasGameOption(GameOption.HOMEWORLDS) || !isHighThreshold())
            return;
        List<DuneChoice> removeChoices = new LinkedList<>();
        game.getTerritories().values()
                .forEach(territory -> territory.getTerrorTokens().stream()
                        .map(tt -> new DuneChoice("moritani-remove-terror-" + territory.getTerritoryName() + "-" + tt, "Remove " + tt + " from " + territory.getTerritoryName()))
                        .forEach(removeChoices::add));
        if (!removeChoices.isEmpty()) {
            htRemovalTBD = true;
            removeChoices.add(new DuneChoice("secondary", "moritani-remove-terror--None", "Don't remove a Terror Token"));
            chat.publish("You are at High Threshold and can remove a Terror Token to gain 4 " + Emojis.SPICE + ". " + getPlayer(), removeChoices);
        }
    }

    public boolean isHtRemovalTBD() {
        return htRemovalTBD;
    }

    public boolean isRemovedTerrorTokenWithHT() {
        return removedTerrorTokenWithHT;
    }

    public void sendTerrorTokenLocationMessage() {
        List<DuneChoice> choices = new ArrayList<>();
        List<String> tokensInStrongholdsForHT = new ArrayList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            if (territory.getTerritoryName().equals("Orgiz Processing Station")) continue;
            DuneChoice stronghold = new DuneChoice("moritani-place-terror-" + territory.getTerritoryName(), territory.getTerritoryName());
            if (game.hasGameOption(GameOption.HOMEWORLDS) && isHighThreshold && !removedTerrorTokenWithHT)
                tokensInStrongholdsForHT.addAll(territory.getTerrorTokens().stream().map(tt -> "- " + tt + " is in " + territory.getTerritoryName()).toList());
            else
                stronghold.setDisabled(territory.hasTerrorToken());
            choices.add(stronghold);
        }
        List<Territory> territoriesWithTerrorTokens = game.getTerritories().values().stream().filter(t -> !t.getTerrorTokens().isEmpty()).toList();
        if (!terrorTokens.isEmpty()) {
            DuneChoice moveOption = new DuneChoice("secondary", "moritani-move-option", "Move a Terror Token");
            moveOption.setDisabled(territoriesWithTerrorTokens.isEmpty());
            choices.add(moveOption);
            choices.add(new DuneChoice("danger", "moritani-don't-place-terror", "Don't place or move"));
            String message = "Where would you like to place a Terror Token? " + getPlayer();
            if (!tokensInStrongholdsForHT.isEmpty()) {
                message += "\nYou are at High Threshold and may place in a stronghold that has one.\n";
                message += String.join("\n", tokensInStrongholdsForHT);
            }
            chat.publish(message, choices);
        } else if (!territoriesWithTerrorTokens.isEmpty()) {
            presentTerrorTokenMoveChoices();
        } else {
            game.getTurnSummary().publish(emoji + " does not have Terror Tokens to place or move.");
        }
    }

    public void presentTerrorTokenMoveChoices() {
        List<DuneChoice> choices = new LinkedList<>();
        game.getTerritories().values().forEach(territory -> territory.getTerrorTokens().stream().map(terrorToken -> new DuneChoice("moritani-move-terror-" + terrorToken + "-" + territory.getTerritoryName(), terrorToken + " from " + territory.getTerritoryName())).forEach(choices::add));
        choices.add(new DuneChoice("secondary", "moritani-no-move", "No move"));
        chat.reply("Which Terror Token would you like to move to a new stronghold? " + player, choices);
    }

    public void presentTerrorTokenMoveDestinations(String token, Territory fromTerritory) {
        List<DuneChoice> choices = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            if (territory.getTerritoryName().equals("Orgiz Processing Station")) continue;
            DuneChoice stronghold = new DuneChoice("moritani-move-to-" + territory.getTerritoryName() + "-" + token + "-" + fromTerritory.getTerritoryName(), "Move to " + territory.getTerritoryName());
            stronghold.setDisabled(territory.hasTerrorToken());
            choices.add(stronghold);
        }
        choices.add(new DuneChoice("secondary", "moritani-no-move", "No move"));
        chat.reply("Where would you like to move " + token + " to? " + player, choices);
    }

    public void checkForTerrorTrigger(Territory targetTerritory, Faction targetFaction, int numForces) {
        if (!targetTerritory.getTerrorTokens().isEmpty() && !(targetFaction instanceof MoritaniFaction)
                && !getAlly().equals(targetFaction.getName())) {
            if (!isHighThreshold && numForces < 3) {
                game.getTurnSummary().publish(emoji + " are at low threshold and may not trigger their Terror Token at this time");
            } else {
                for (String terror : targetTerritory.getTerrorTokens()) {
                    List<DuneChoice> choices = new ArrayList<>();
                    choices.add(new DuneChoice("moritani-trigger-terror-" + terror + "-" + targetFaction.getName(), "Trigger"));
                    choices.add(new DuneChoice("moritani-offer-alliance-" + targetFaction.getName() + "-" + targetTerritory.getTerritoryName() + "-" + terror, "Offer alliance"));
                    choices.add(new DuneChoice("secondary", "moritani-don't-trigger-terror", "Don't Trigger"));
                    chat.publish("Will you trigger your " + terror + " Terror Token in " + targetTerritory.getTerritoryName() + " against " + targetFaction.getEmoji() + "? " + player, choices);
                }
                String message = emoji + " has an opportunity to trigger their Terror Token";
                message += targetTerritory.getTerrorTokens().size() > 1 ? "s" : "";
                message += " in " + targetTerritory.getTerritoryName() + " against " + targetFaction.getEmoji();
                game.getTurnSummary().publish(message);
            }
        }
    }

    public List<String> getAssassinationTargets() {
        return assassinationTargets;
    }

    public boolean isNewAssassinationTargetNeeded() {
        return newAssassinationTargetNeeded;
    }

    public void assassinateTraitor() throws InvalidGameStateException {
        if (traitorHand.isEmpty())
            throw new InvalidGameStateException("Moritani has already assassinated this turn.");
        TraitorCard traitor = traitorHand.getFirst();
        String assassinated = traitor.getName();
        if (game.getLeaderTanks().stream().anyMatch(l -> l.getName().equals(assassinated)))
            throw new InvalidGameStateException(assassinated + " is in the tanks and cannot be assassinated.");
        else if (assassinationTargets.stream().anyMatch(t -> t.contains(traitor.getFactionEmoji())))
            throw new InvalidGameStateException("Moritani cannot assassinate the same faction twice.");
        assassinationTargets.add(traitor.getEmojiAndNameString());
        traitorHand.clear();
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
        for (Faction faction : game.getFactions()) {
            Optional<Leader> optLeader = faction.getLeaders().stream().filter(leader1 -> leader1.getName().equals(assassinated)).findFirst();
            if (optLeader.isPresent()) {
                assassinateLeader(faction, optLeader.get());
                break;
            }
        }
        newAssassinationTargetNeeded = true;
    }

    public String getTerrorTokenMessage(boolean placedTokensOnly) {
        String response = "\n__" + (placedTokensOnly ? "Placed " : "") + "Terror Tokens:__\n" +
                (placedTokensOnly ? "" : "_Available_: " + String.join(", ", terrorTokens) + "\n");
        String placedTokens = String.join("\n", game.getTerritories().values().stream()
                .filter(Territory::isStronghold)
                .filter(t -> !t.getTerrorTokens().isEmpty())
                .map(t -> "_In " + t.getTerritoryName() + ":_ " + String.join(", ", t.getTerrorTokens())).toList());
        if (placedTokensOnly && placedTokens.isEmpty())
            return "";
        else
            return response + placedTokens;
    }

    public void presentTerrorAllianceChoices(String factionName, String territory, String terror) throws InvalidGameStateException {
        if (ally != null && ally.equals(factionName))
            throw new InvalidGameStateException("You cannot trigger against your ally.");
        Faction faction = game.getFaction(factionName);
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("moritani-accept-offer-" + territory + "-" + terror, "Yes"));
        choices.add(new DuneChoice("danger", "moritani-deny-offer-" + territory + "-" + terror, "No"));
        faction.getChat().publish("An emissary of " + Emojis.MORITANI + " has offered an alliance with you!  Or else.  Do you accept? " + faction.getPlayer(), choices);
        game.getTurnSummary().publish(Emojis.MORITANI + " are offering an alliance to " + faction.getEmoji() + " in exchange for safety from their Terror Token!");
        chat.reply("You have offered an alliance to " + faction.getEmoji() + " in exchange for safety from your Terror Token!");
    }

    @Override
    public void denyTerrorAlliance(String territoryName, String terror) throws InvalidGameStateException {
        throw new InvalidGameStateException("Moritani cannot deny alliance with themselves.");
    }

    @Override
    public void acceptTerrorAlliance(Faction moritani, String territoryName, String terror) throws InvalidGameStateException {
        throw new InvalidGameStateException("Moritani cannot accept alliance with themselves.");
    }

    @Override
    protected void presentExtortionChoices() {
        // Moritani does not get buttons for removing Extortion from the game
    }

    @Override
    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        super.performMentatPauseActions(extortionTokenTriggered);
        sendTerrorTokenLocationMessage();
        if (newAssassinationTargetNeeded) {
            traitorHand.add(game.getTraitorDeck().pollFirst());
            game.getTurnSummary().publish(emoji + " has drawn a new traitor.");
            setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        }
        newAssassinationTargetNeeded = false;
    }
}
