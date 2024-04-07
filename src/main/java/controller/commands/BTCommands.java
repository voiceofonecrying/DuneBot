package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.TraitorCard;
import model.factions.BTFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static controller.commands.CommandOptions.btFaceDancer;

public class BTCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("bt", "Commands related to the Bene Tleilaxu.").addSubcommands(
                        new SubcommandData(
                                "swap-face-dancer",
                                "Swaps a BT Face Dancer."
                        ).addOptions(btFaceDancer),
                        new SubcommandData(
                                "reveal-face-dancer",
                                "Reveals a BT Face Dancer."
                        ).addOptions(btFaceDancer)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (name.equals("swap-face-dancer")) {
            swapBTFaceDancer(discordGame, game);
        } else if (name.equals("reveal-face-dancer")) {
            revealBTFaceDancer(discordGame, game);
        } else {
            throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void swapBTFaceDancer(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasFaction("BT")) {
            String faceDancer = discordGame.required(btFaceDancer).getAsString();
            Faction faction = game.getFaction("BT");

            List<TraitorCard> btFaceDancerHand = faction.getTraitorHand();
            LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();

            TraitorCard traitorCard = btFaceDancerHand.stream()
                    .filter(t -> t.name().equalsIgnoreCase(faceDancer))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));

            faction.removeTraitorCard(traitorCard);

            traitorDeck.add(traitorCard);
            Collections.shuffle(traitorDeck);
            TraitorCard newFD = traitorDeck.pop();
            faction.addTraitorCard(newFD);

            discordGame.pushGame();
            String newFDEmoji = newFD.name().equals("Cheap Hero") ? "" : game.getFaction(newFD.factionName()).getEmoji() + " ";
            String oldFDEmoji = traitorCard.name().equals("Cheap Hero") ? "" : game.getFaction(traitorCard.factionName()).getEmoji() + " ";
            discordGame.getBTLedger().queueMessage(
                    MessageFormat.format(
                            "{0}{1} is your new Face Dancer. You have swapped out {2}{3}.",
                            newFDEmoji, newFD.name(), oldFDEmoji, traitorCard.name()
                    )
            );
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " swapped a Face Dancer");
        }
    }

    public static void revealBTFaceDancer(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        BTFaction btFaction = (BTFaction)game.getFaction("BT");
        String faceDancer = discordGame.required(btFaceDancer).getAsString();
        List<TraitorCard> btFaceDancerHand = btFaction.getTraitorHand();
        TraitorCard traitorCard = btFaceDancerHand.stream()
                .filter(t -> t.name().equalsIgnoreCase(faceDancer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));
        int fdsRemaining = btFaction.getTraitorHand().size();
        btFaction.revealFaceDancer(traitorCard, game);
        discordGame.getTurnSummary().queueMessage(btFaction.getEmoji() + " revealed " + faceDancer + " as a Face Dancer!");
        if (fdsRemaining == 1 && btFaction.getTraitorHand().size() == 3) {
            discordGame.getTurnSummary().queueMessage(Emojis.BT + " revealed all of their Face Dancers and have drawn a new set of 3.");
        }
        discordGame.pushGame();
    }
}
