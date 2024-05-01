package model.factions;

import constants.Emojis;
import controller.DiscordGame;
import controller.channels.TurnSummary;
import controller.commands.ShowCommands;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        game.getTerritories().put("Grumman", new Territory("Grumman", -1, false, false, false));
        game.getTerritory("Grumman").addForce(new Force("Moritani", 20));
        game.getHomeworlds().put(getName(), homeworld);
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

    public void triggerTerrorToken(Game game, DiscordGame discordGame, Faction triggeringFaction, Territory location, String terror) throws ChannelNotFoundException, IOException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        turnSummary.queueMessage("The " + terror + " token has been triggered!");

        switch (terror) {
            case "Assassination" -> discordGame.getModInfo().queueMessage("Send a random " + triggeringFaction.getEmoji()
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
                ShowCommands.refreshChangedInfo(discordGame);
                turnSummary.queueMessage("During Mentat Pause, any faction in storm order may pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game.");
            }
            case "Robbery" -> {
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("moritani-robbery-rob", "Steal spice"));
                buttons.add(Button.primary("moritani-robbery-draw", "Draw card"));
                discordGame.getMoritaniChat().queueMessage("Your terrorist in " + triggeringFaction + " has robbed the " + triggeringFaction.getEmoji() +
                        "! What would you like to do?", buttons);
            }
            case "Sabotage" -> {
                Collections.shuffle(triggeringFaction.getTreacheryHand());
                String cardName = triggeringFaction.getTreacheryHand().get(0).name();
                turnSummary.queueMessage(Emojis.MORITANI + " took " + cardName + " from " + triggeringFaction.getEmoji() + " and discarded it.");
                game.getTreacheryDiscard().add(triggeringFaction.removeTreacheryCard(cardName));
                List<Button> treacheryCards = new LinkedList<>();
                for (TreacheryCard card : getTreacheryHand()) {
                    treacheryCards.add(Button.primary("moritani-sabotage-give-card-" + triggeringFaction.getName() + "-" + card.name(), card.name()));
                }
                treacheryCards.add(Button.secondary("moritani-sabotage-no-card-" + triggeringFaction.getName(), "Don't send a card"));
                discordGame.getMoritaniChat().queueMessage("Give a treachery card from your hand to " + triggeringFaction.getEmoji() + "?", treacheryCards);
            }
            case "Sneak Attack" -> {
                Force reserves = game.getTerritory("Grumman").getForce("Moritani");
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("moritani-sneak-attack-1", "1").withDisabled(reserves.getStrength() < 1));
                buttons.add(Button.primary("moritani-sneak-attack-2", "2").withDisabled(reserves.getStrength() < 2));
                buttons.add(Button.primary("moritani-sneak-attack-3", "3").withDisabled(reserves.getStrength() < 3));
                buttons.add(Button.primary("moritani-sneak-attack-4", "4").withDisabled(reserves.getStrength() < 4));
                buttons.add(Button.primary("moritani-sneak-attack-5", "5").withDisabled(reserves.getStrength() < 5));
                discordGame.getMoritaniChat().queueMessage("How many forces do you want to send on your sneak attack?", buttons);
            }
        }

        for (Territory territory : game.getTerritories().values()) {
            territory.getTerrorTokens().removeIf(t -> t.equals(terror));
        }
        game.setUpdated(UpdateType.MAP);
    }

    public void sendTerrorTokenMessage(DiscordGame discordGame, String territory) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (String terror : terrorTokens) {
            buttons.add(Button.primary("moritani-terror-selected-" + terror + "-" + territory + "-", terror));
        }
        discordGame.getMoritaniChat().queueMessage("Which terror token would you like to place?", buttons);
    }

    public void placeTerrorToken(Game game, Territory territory, String terror) {
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

    public void sendTerrorTokenLocationMessage(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            Button stronghold = Button.primary("moritani-place-terror-" + territory.getTerritoryName(), "Place Terror Token in " + territory.getTerritoryName());
            if (!this.isHighThreshold() && (!territory.getTerrorTokens().isEmpty() || game.getStorm() == territory.getSector())) stronghold = stronghold.asDisabled();
            else if (this.isHighThreshold() && !territory.getTerrorTokens().isEmpty()) {
                for (String terror : territory.getTerrorTokens()) {
                    buttons.add(Button.secondary("moritani-remove-terror-" + territory.getTerritoryName() + "-" + terror, "Remove " + terror + " Token from " + territory.getTerritoryName() + " (gain 4 " + Emojis.SPICE + ")"));
                }
            }
            buttons.add(stronghold);
        }
        discordGame.getMoritaniChat().queueMessage("Use these buttons to place Terror Tokens from your supply.", buttons);
    }

    public void sendTerrorTokenTriggerMessage(Game game, DiscordGame discordGame, Territory targetTerritory, Faction targetFaction) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (String terror : targetTerritory.getTerrorTokens()) {
            buttons.add(Button.primary("moritani-trigger-terror-" + terror + "-" + targetFaction.getName(), "Trigger " + terror));
            buttons.add(Button.danger("moritani-offer-alliance-" + targetFaction.getName() + "-" + targetTerritory.getTerritoryName() + "-" + terror, "Offer alliance (will trigger " + terror + ")"));
        }
        buttons.add(Button.danger("moritani-don't-trigger-terror", "Don't Trigger"));
        discordGame.getTurnSummary().queueMessage(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + targetFaction.getEmoji());
        discordGame.getMoritaniChat().queueMessage("Will you trigger your terror token now?" + game.getFaction("Moritani").getPlayer(), buttons);
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
