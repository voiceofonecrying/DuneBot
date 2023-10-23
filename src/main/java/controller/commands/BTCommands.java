package controller.commands;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.TraitorCard;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

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
            faction.addTraitorCard(traitorDeck.pollLast());

            discordGame.pushGame();
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " swapped a Face Dancer");
        }
    }
}
