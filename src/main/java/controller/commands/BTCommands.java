package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.*;

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

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (name.equals("swap-face-dancer")) {
            swapBTFaceDancer(discordGame, game);
        } else {
            throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void swapBTFaceDancer(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (game.hasFaction("BT")) {
            String faceDancer = discordGame.required(btFaceDancer).getAsString();
            Faction faction = game.getFaction("BT");

            List<TraitorCard> btFaceDancerHand = faction.getTraitorHand();
            LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();

            TraitorCard traitorCard = btFaceDancerHand.stream()
                    .filter(t -> t.name().equalsIgnoreCase(faceDancer))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));

            btFaceDancerHand.remove(traitorCard);

            traitorDeck.add(traitorCard);
            Collections.shuffle(traitorDeck);
            btFaceDancerHand.add(traitorDeck.pollLast());

            discordGame.pushGame();
            discordGame.sendMessage("turn-summary", faction.getEmoji() + " swapped a Face Dancer");
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
    }
}
