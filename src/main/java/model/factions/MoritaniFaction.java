package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import model.*;
import model.topics.DuneTopic;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class MoritaniFaction extends Faction {


    private final List<String> terrorTokens;
    private final List<String> assassinationTargets;
    private boolean newAssassinationTargetNeeded;

    public MoritaniFaction(String player, String userName) throws IOException {
        super("Moritani", player, userName);

        setSpice(12);
        this.freeRevival = 2;
        this.emoji = Emojis.MORITANI;
        this.highThreshold = 8;
        this.lowThreshold = 7;
        this.occupiedIncome = 2;
        this.homeworld = "Grumman";
        this.terrorTokens = new LinkedList<>();
        this.assassinationTargets = new LinkedList<>();
        this.newAssassinationTargetNeeded = false;

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
    }

    public void assassinateLeader(Faction triggeringFaction, Leader leader) {
        if (game.getLeaderTanks().contains(leader))
            throw new IllegalArgumentException("Moritani cannot assassintate a leader in the tanks.");
        int spiceGained = leader.getAssassinationValue();
        game.getTurnSummary().publish(Emojis.MORITANI + " collect " + spiceGained + " " + Emojis.SPICE + " by assassinating " + leader. getName() + "!");
        game.killLeader(triggeringFaction, leader.getName());
        addSpice(spiceGained, "assassination of " + leader.getName());
    }

    public void triggerTerrorToken(Faction triggeringFaction, Territory location, String terror) {
        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish("The " + terror + " token has been triggered!");

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
                this.handLimit = 3;
                location.setAftermathToken(true);
                for (Force force : location.getForces()) {
                    if (force.getName().contains("*"))
                        game.removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), 0, force.getStrength(), true);
                    else
                        game.removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), force.getStrength(), 0, true);
                }
            }
            case "Extortion" -> {
                game.triggerExtortionToken();
                addFrontOfShieldSpice(5);
                turnSummary.publish("During Mentat Pause, any faction in storm order may pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game.");
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
                turnSummary.publish(Emojis.MORITANI + " took " + cardName + " from " + triggeringFaction.getEmoji() + " and discarded it.");
                game.getTreacheryDiscard().add(triggeringFaction.removeTreacheryCard(cardName));
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

        for (Territory territory : game.getTerritories().values()) {
            territory.getTerrorTokens().removeIf(t -> t.equals(terror));
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void robberyRob(String factionName) {
        Faction faction = game.getFaction(factionName);
        int spiceToSteal = Math.ceilDiv(faction.getSpice(), 2);
        if (spiceToSteal == 0) {
            ledger.publish(faction.getEmoji() + " had no " + Emojis.SPICE + " to steal");
        } else {
            faction.subtractSpice(spiceToSteal, "stolen by " + Emojis.MORITANI + " Robbery");
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
        game.getTurnSummary().publish(Emojis.MORITANI + " has drawn a " + Emojis.TREACHERY + " card with Robbery.");
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
        chat.publish("Which terror token would you like to place?", choices);
    }

    public void placeTerrorToken(Territory territory, String terror) {
        terrorTokens.removeIf(a -> a.equals(terror));
        territory.addTerrorToken(terror);
        game.getTurnSummary().publish("A " + Emojis.MORITANI + " Terror Token has been placed in " + territory.getTerritoryName());
        ledger.publish(terror + " Terror Token was placed in " + territory.getTerritoryName() + ".");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        game.setUpdated(UpdateType.MAP);
    }

    public void moveTerrorToken(Territory toTerritory, String terror, Territory fromTerritory) {
        fromTerritory.removeTerrorToken(terror);
        toTerritory.addTerrorToken(terror);
        ledger.publish(terror + " Terror Token was moved to " + toTerritory.getTerritoryName() + " from " + fromTerritory.getTerritoryName() + ".");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        game.setUpdated(UpdateType.MAP);
    }

    public void getDukeVidal() {
        if (getLeader("Duke Vidal").isPresent()) return;
        addLeader(new Leader("Duke Vidal", 6, "None", null, false));
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }

    public void addTerrorToken(String name) {
        getTerrorTokens().add(name);
        getLedger().publish(name + " Terror token was added to your hand.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void sendTerrorTokenHighThresholdMessage() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS) || !isHighThreshold())
            return;
        List<DuneChoice> choices = new LinkedList<>();
        List<DuneChoice> removeChoices = new LinkedList<>();
        boolean canPlace = false;
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            if (territory.getTerritoryName().equals("Orgiz Processing Station")) continue;
            DuneChoice stronghold = new DuneChoice("moritani-place-terror-" + territory.getTerritoryName(), "Place in " + territory.getTerritoryName());
            stronghold.setDisabled(!territory.hasTerrorToken());
            if (territory.hasTerrorToken())
                canPlace = true;
            choices.add(stronghold);
            for (String terror : territory.getTerrorTokens()) {
                removeChoices.add(new DuneChoice("secondary", "moritani-remove-terror-" + territory.getTerritoryName() + "-" + terror, "Remove " + terror + " from " + territory.getTerritoryName()));
            }
        }
        if (canPlace || !removeChoices.isEmpty()) {
            choices.addAll(removeChoices);
            choices.add(new DuneChoice("danger", "moritani-don't-place-terror", "Don't place or remove"));
            chat.publish("You are at High Threshold and can place a Terror Token in a stronghold that has one or remove one to gain 4 " + Emojis.SPICE + " during Spice Collection phase. " + getPlayer(), choices);
        }
    }

    public void sendTerrorTokenLocationMessage() {
        List<DuneChoice> choices = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            if (territory.getTerritoryName().equals("Orgiz Processing Station")) continue;
            DuneChoice stronghold = new DuneChoice("moritani-place-terror-" + territory.getTerritoryName(), "Place in " + territory.getTerritoryName());
            stronghold.setDisabled(territory.hasTerrorToken());
            choices.add(stronghold);
        }
        List<Territory> territoriesWithTerrorTokens = game.getTerritories().values().stream().filter(t -> !t.getTerrorTokens().isEmpty()).toList();
        if (!terrorTokens.isEmpty()) {
            DuneChoice moveOption = new DuneChoice("secondary", "moritani-move-option", "Move a Terror Token");
            moveOption.setDisabled(territoriesWithTerrorTokens.isEmpty());
            choices.add(moveOption);
            choices.add(new DuneChoice("danger", "moritani-don't-place-terror", "Don't place or move"));
            chat.publish("Use these buttons to place a Terror Token from your supply. " + getPlayer(), choices);
        } else if (!territoriesWithTerrorTokens.isEmpty()) {
            presentTerrorTokenMoveChoices();
        } else {
            game.getTurnSummary().publish(Emojis.MORITANI + " does not have Terror Tokens to place or move.");
        }
    }

    public void presentTerrorTokenMoveChoices() {
        List<DuneChoice> choices = new LinkedList<>();
        game.getTerritories().values().forEach(territory -> territory.getTerrorTokens().stream().map(terrorToken -> new DuneChoice("moritani-move-terror-" + terrorToken + "-" + territory.getTerritoryName(), terrorToken + " from " + territory.getTerritoryName())).forEach(choices::add));
        choices.add(new DuneChoice("secondary", "moritani-no-move", "No move"));
        chat.publish("Which Terror Token would you like to move to a new stronghold? " + player, choices);
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
        chat.publish("Where would you like to move " + token + " to? " + player, choices);
    }

    public void checkForTerrorTrigger(Territory targetTerritory, Faction targetFaction, int numForces) {
        if (!targetTerritory.getTerrorTokens().isEmpty() && !(targetFaction instanceof MoritaniFaction)
                && !getAlly().equals(targetFaction.getName())) {
            if (!isHighThreshold && numForces < 3) {
                game.getTurnSummary().publish(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time");
            } else {
                List<DuneChoice> choices = new ArrayList<>();
                for (String terror : targetTerritory.getTerrorTokens()) {
                    choices.add(new DuneChoice("moritani-trigger-terror-" + terror + "-" + targetFaction.getName(), "Trigger " + terror));
                    choices.add(new DuneChoice("danger", "moritani-offer-alliance-" + targetFaction.getName() + "-" + targetTerritory.getTerritoryName() + "-" + terror, "Offer alliance (will trigger " + terror + ")"));
                }
                choices.add(new DuneChoice("danger", "moritani-don't-trigger-terror", "Don't Trigger"));
                game.getTurnSummary().publish(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + targetFaction.getEmoji());
                chat.publish("Will you trigger your Terror Token in " + targetTerritory.getTerritoryName() + "? " + game.getFaction("Moritani").getPlayer(), choices);
            }
        }
    }

    public List<String> getAssassinationTargets() {
        return assassinationTargets;
    }

    public boolean isNewAssassinationTargetNeeded() {
        return newAssassinationTargetNeeded;
    }

    public void setNewAssassinationTargetNeeded(boolean newAssassinationTargetNeeded) {
        this.newAssassinationTargetNeeded = newAssassinationTargetNeeded;
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
            game.getTurnSummary().publish(Emojis.MORITANI + " has drawn a new traitor.");
            setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        }
        newAssassinationTargetNeeded = false;
    }
}
