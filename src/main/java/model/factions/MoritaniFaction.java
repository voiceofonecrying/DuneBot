package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.*;
import model.topics.DuneTopic;

import java.io.IOException;
import java.util.*;

public class MoritaniFaction extends Faction {


    private final List<String> terrorTokens;
    private final List<String> assassinationTargets;
    private boolean newAssassinationTargetNeeded;

    public MoritaniFaction(String player, String userName, Game game) throws IOException {
        super("Moritani", player, userName, game);

        setSpice(12);
        this.freeRevival = 2;
        this.emoji = Emojis.MORITANI;
        this.highThreshold = 8;
        this.lowThreshold = 7;
        this.occupiedIncome = 2;
        this.homeworld = "Grumman";
        Territory grumman = game.getTerritories().addHomeworld(homeworld);
        grumman.addForce(new Force(name, 20));
        game.getHomeworlds().put(name, homeworld);
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

    public void triggerTerrorToken(Faction triggeringFaction, Territory location, String terror) {
        DuneTopic turnSummary = game.getTurnSummary();
        turnSummary.publish("The " + terror + " token has been triggered!");

        switch (terror) {
            case "Assassination" -> game.getModInfo().publish("Send a random " + triggeringFaction.getEmoji()
                    + " leader to the tanks. " + Emojis.MORITANI + " collects " + Emojis.SPICE + " for it.");
            case "Atomics" -> {
                this.handLimit = 3;
                location.setAftermathToken(true);
                for (Force force : location.getForces()) {
                    if (force.getName().contains("*")) game
                            .removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), 0, force.getStrength(), true);
                    else
                        game.removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), force.getStrength(), 0, true);
                }
            }
            case "Extortion" -> {
                game.setExtortionTokenRevealed(true);
                addFrontOfShieldSpice(5);
                turnSummary.publish("During Mentat Pause, any faction in storm order may pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game.");
            }
            case "Robbery" -> {
                List<DuneChoice> choices = new LinkedList<>();
                choices.add(new DuneChoice("moritani-robbery-rob", "Steal spice"));
                choices.add(new DuneChoice("moritani-robbery-draw", "Draw card"));
                chat.publish("Your terrorist in " + triggeringFaction + " has robbed the " + triggeringFaction.getEmoji() +
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
                Force reserves = game.getTerritory("Grumman").getForce("Moritani");
                List<DuneChoice> choices = new LinkedList<>();
                for (int i = 1; i < 6; i++) {
                    String digit = String.valueOf(i);
                    DuneChoice choice = new DuneChoice("moritani-sneak-attack-" + digit, digit);
                    choice.setDisabled(reserves.getStrength() < i);
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

    public void sendTerrorTokenMessage(String territory) {
        List<DuneChoice> choices = new LinkedList<>();
        for (String terror : terrorTokens)
            choices.add(new DuneChoice("moritani-terror-selected-" + terror + "-" + territory + "-", terror));
        chat.publish("Which terror token would you like to place?", choices);
    }

    public void placeTerrorToken(Territory territory, String terror) {
        terrorTokens.removeIf(a -> a.equals(terror));
        territory.addTerrorToken(terror);
        game.setUpdated(UpdateType.MAP);
    }

    public void getDukeVidal() {
        if (getLeader("Duke Vidal").isPresent()) return;
        addLeader(new Leader("Duke Vidal", 6, null, false));
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }

    public void addTerrorToken(String name) {
        getTerrorTokens().add(name);
        getLedger().publish(name + " Terror token was added to your hand.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void sendTerrorTokenLocationMessage() {
        List<DuneChoice> choices = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            if (territory.getTerritoryName().equals("Orgiz Processing Station")) continue;
            DuneChoice stronghold = new DuneChoice("moritani-place-terror-" + territory.getTerritoryName(), "Place Terror Token in " + territory.getTerritoryName());
            if (!this.isHighThreshold() && (!territory.getTerrorTokens().isEmpty() || game.getStorm() == territory.getSector()))
                stronghold.setDisabled(true);
            else if (this.isHighThreshold() && !territory.getTerrorTokens().isEmpty()) {
                for (String terror : territory.getTerrorTokens()) {
                    choices.add(new DuneChoice("secondary", "moritani-remove-terror-" + territory.getTerritoryName() + "-" + terror, "Remove " + terror + " Token from " + territory.getTerritoryName() + " (gain 4 " + Emojis.SPICE + ")"));
                }
            }
            choices.add(stronghold);
        }
        chat.publish("Use these buttons to place a Terror Token from your supply. " + getPlayer(), choices);
    }

    public void sendTerrorTokenTriggerMessage(Territory targetTerritory, Faction targetFaction) {
        List<DuneChoice> choices = new LinkedList<>();
        for (String terror : targetTerritory.getTerrorTokens()) {
            choices.add(new DuneChoice("moritani-trigger-terror-" + terror + "-" + targetFaction.getName(), "Trigger " + terror));
            choices.add(new DuneChoice("danger", "moritani-offer-alliance-" + targetFaction.getName() + "-" + targetTerritory.getTerritoryName() + "-" + terror, "Offer alliance (will trigger " + terror + ")"));
        }
        choices.add(new DuneChoice("danger", "moritani-don't-trigger-terror", "Don't Trigger"));
        game.getTurnSummary().publish(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + targetFaction.getEmoji());
        chat.publish("Will you trigger your terror token now?" + game.getFaction("Moritani").getPlayer(), choices);
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

    public String getTerrorTokenMessage() {
        StringBuilder supply = new StringBuilder();
        supply.append("\nTerror Tokens:\n");

        for (String token : terrorTokens) {
            supply.append(token).append("\n");
        }
        return supply.toString();
    }
}
