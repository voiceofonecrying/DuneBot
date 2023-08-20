package model.factions;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MoritaniFaction extends Faction {


    private final List<String> terrorTokens;
    public MoritaniFaction(String player, String userName, Game game) throws IOException {
        super("Moritani", player, userName, game);

        setSpice(12);
        this.freeRevival = 2;
        this.reserves = new Force("Moritani", 14);
        this.emoji = Emojis.MORITANI;
        this.terrorTokens = new LinkedList<>();

        terrorTokens.add("Assassination");
        terrorTokens.add("Atomics");
        terrorTokens.add("Extortion");
        terrorTokens.add("Robbery");
        terrorTokens.add("Sabotage");
        terrorTokens.add("Sneak Attack");
    }
    public void triggerTerrorToken(Game game, DiscordGame discordGame, Faction triggeringFaction, Territory location, String terror) throws ChannelNotFoundException, IOException {

        discordGame.sendMessage("turn-summary", "The " + terror + " token has been triggered!");

        switch (terror) {
            case "Assassination" -> discordGame.sendMessage("mod-info", "Send a random " + triggeringFaction.getEmoji()
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
            }
            case "Robbery" -> discordGame.prepareMessage("moritani-chat", "Your terrorist in " + triggeringFaction + " has robbed the " + triggeringFaction.getEmoji() +
                        "! What would you like to do?").addActionRow(Button.primary("moritani-robbery-rob", "Steal spice"),
                        Button.primary("moritani-robbery-draw", "Draw card")).queue();
            case "Sabotage" -> {
                Collections.shuffle(triggeringFaction.getTreacheryHand());
                discordGame.sendMessage("turn-summary", triggeringFaction.getEmoji() + " discarded " + triggeringFaction.getTreacheryHand().get(0));
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
        if (!terror.equals("Extortion")) terrorTokens.remove(terror);
    }

    public List<String> getTerrorTokens() {
        return terrorTokens;
    }
}
