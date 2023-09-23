package model.factions;

import constants.Emojis;
import controller.channels.TurnSummary;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
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
            case "Assassination" -> discordGame.queueMessage("mod-info", "Send a random " + triggeringFaction.getEmoji()
                    + " leader to the tanks. " + Emojis.MORITANI + " collects " + Emojis.SPICE + " for it.");
            case "Atomics" -> {
                this.handLimit = 3;
                location.setAftermathToken(true);
                for (Force force : location.getForces()) {
                    if (force.getName().contains("*")) CommandManager
                            .removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), 0, force.getStrength(), true);
                    else CommandManager.removeForces(location.getTerritoryName(), game.getFaction(force.getFactionName()), force.getStrength(), 0, true);
                }
            }
            case "Extortion" -> {
                addFrontOfShieldSpice(5);
                ShowCommands.refreshChangedInfo(discordGame);
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("moritani-pay-extortion", "Pay to remove"));
                buttons.add(Button.secondary("moritani-pass-extortion", "Don't pay to remove"));
                turnSummary.queueMessage("The Extortion token will be returned unless someone " +
                        "pays 3 " + Emojis.SPICE + " to remove it from the game.", buttons);
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
                turnSummary.queueMessage(triggeringFaction.getEmoji() + " discarded " + triggeringFaction.getTreacheryHand().get(0));
                game.getTreacheryDiscard().add(triggeringFaction.removeTreacheryCard(triggeringFaction.getTreacheryHand().get(0).name()));
                List<Button> treacheryCards = new LinkedList<>();
                for (TreacheryCard card : getTreacheryHand()) {
                    treacheryCards.add(Button.primary("moritani-sabotage-give-card-" + card.name(), card.name()));
                }
                treacheryCards.add(Button.secondary("moritani-sabotage-no-card", "Don't send a card"));
                discordGame.getMoritaniChat().queueMessage("Give a treachery card from your hand to " + triggeringFaction.getEmoji() + "?", treacheryCards);
            }
            case "Sneak Attack" -> {
                Force reserves = game.getTerritory("Grumman").getForce("Moritani");
                List<Button> buttons = new LinkedList<>();
                buttons.add(Button.primary("moritani-sneak-attack-1", "1").withDisabled(reserves.getStrength()>=1));
                buttons.add(Button.primary("moritani-sneak-attack-2", "2").withDisabled(reserves.getStrength()>=2));
                buttons.add(Button.primary("moritani-sneak-attack-3", "3").withDisabled(reserves.getStrength()>=3));
                buttons.add(Button.primary("moritani-sneak-attack-4", "4").withDisabled(reserves.getStrength()>=4));
                buttons.add(Button.primary("moritani-sneak-attack-5", "5").withDisabled(reserves.getStrength()>=5));
                discordGame.getMoritaniChat().queueMessage("How many forces do you want to send on your sneak attack?", buttons);
            }
        }

        for (Territory territory : game.getTerritories().values()) {
            if (territory.getTerrorToken() == null) continue;
            if (territory.getTerrorToken().equals(terror)) territory.setTerrorToken(null);
        }
    }

    public void sendTerrorTokenMessage(DiscordGame discordGame, String territory) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (String terror : terrorTokens) {
            buttons.add(Button.primary("moritani-terror-selected-" + terror + "-" + territory + "-", terror));
        }
        discordGame.getMoritaniChat().queueMessage("Which terror token would you like to place?", buttons);
    }

    public void placeTerrorToken(Territory territory, String terror) {
        terrorTokens.removeIf(a -> a.equals(terror));
        territory.setTerrorToken(terror);
    }
    public void getDukeVidal() {
        if (getLeader("Duke Vidal").isPresent()) return;
        addLeader(new Leader("Duke Vidal", 6, null, false));
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }

    public void sendTerrorTokenLocationMessage(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) continue;
            Button stronghold = Button.primary("moritani-place-terror-" + territory.getTerritoryName(), "Place Terror Token in " + territory.getTerritoryName());
            if (territory.getTerrorToken() != null || game.getStorm() == territory.getSector()) stronghold = stronghold.asDisabled();
            buttons.add(stronghold);
        }
        discordGame.getMoritaniChat().queueMessage("Use these buttons to place Terror Tokens from your supply.", buttons);
    }

    public List<String> getAssassinationTargets() {
        return assassinationTargets;
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
