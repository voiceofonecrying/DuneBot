package model.factions;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

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
        this.reserves = new Force("Moritani", 20);
        this.emoji = Emojis.MORITANI;
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

        discordGame.queueMessage("turn-summary", "The " + terror + " token has been triggered!");

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
                discordGame.queueMessage("turn-summary", new MessageCreateBuilder().addContent("The Extortion token will be returned unless someone " +
                        "pays 3 " + Emojis.SPICE + " to remove it from the game.")
                        .addActionRow(Button.primary("moritani-pay-extortion", "Pay to remove"),
                                Button.secondary("moritani-pass-extortion", "Don't pay to remove")));
            }
            case "Robbery" -> discordGame.prepareMessage("moritani-chat", "Your terrorist in " + triggeringFaction + " has robbed the " + triggeringFaction.getEmoji() +
                        "! What would you like to do?").addActionRow(Button.primary("moritani-robbery-rob", "Steal spice"),
                        Button.primary("moritani-robbery-draw", "Draw card")).queue();
            case "Sabotage" -> {
                Collections.shuffle(triggeringFaction.getTreacheryHand());
                discordGame.queueMessage("turn-summary", triggeringFaction.getEmoji() + " discarded " + triggeringFaction.getTreacheryHand().get(0));
                game.getTreacheryDiscard().add(triggeringFaction.removeTreacheryCard(triggeringFaction.getTreacheryHand().get(0).name()));
                MessageCreateAction message = discordGame.prepareMessage("moritani-chat", "Give a treachery card from your hand to " + triggeringFaction.getEmoji() + "?");

                List<Button> treacheryCards = new LinkedList<>();
                for (TreacheryCard card : getTreacheryHand()) {
                    treacheryCards.add(Button.primary("moritani-sabotage-give-card-" + card.name(), card.name()));
                }
                treacheryCards.add(Button.secondary("moritani-sabotage-no-card", "Don't send a card"));
                message.addActionRow(treacheryCards).queue();
            }
            case "Sneak Attack" -> {
                Button one = Button.primary("moritani-sneak-attack-1", "1");
                Button two = Button.primary("moritani-sneak-attack-2", "2");
                Button three = Button.primary("moritani-sneak-attack-3", "3");
                Button four = Button.primary("moritani-sneak-attack-4", "4");
                Button five = Button.primary("moritani-sneak-attack-5", "5");

                discordGame.prepareMessage("moritani-chat", "How many forces do you want to send on your sneak attack?")
                        .addActionRow(one.withDisabled(reserves.getStrength()>=1),
                                two.withDisabled(reserves.getStrength()>=2),
                                three.withDisabled(reserves.getStrength()>=3),
                                four.withDisabled(reserves.getStrength()>=4),
                                five.withDisabled(reserves.getStrength()>=5)).queue();
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
        if (buttons.size() > 5) {
            discordGame.prepareMessage("moritani-chat", "Which terror token would you like to place?")
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.get(5)).queue();
        }
        else {
            discordGame.prepareMessage("moritani-chat", "Which terror token would you like to place?")
                    .addActionRow(buttons).queue();
        }
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
        if (buttons.size() > 5) {
            discordGame.queueMessage("moritani-chat", new MessageCreateBuilder().addContent("Use these buttons to place Terror Tokens from your supply.")
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.subList(5, buttons.size())));
        } else {
            discordGame.queueMessage("moritani-chat", new MessageCreateBuilder().addContent("Use these buttons to place Terror Tokens from your supply.")
                    .addActionRow(buttons));
        }
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
