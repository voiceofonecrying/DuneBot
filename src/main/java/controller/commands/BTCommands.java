package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BTCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("bt", "Commands related to the Bene Tleilaxu.").addSubcommands(
                        new SubcommandData(
                                "swap-face-dancer",
                                "Swaps a BT Face Dancer."
                        ).addOptions(CommandOptions.btFaceDancer)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String name = event.getSubcommandName();

        switch (name) {
            case "swap-face-dancer" -> swapBTFaceDancer(event, discordGame, gameState);
        }
    }

    public static void swapBTFaceDancer(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.hasFaction("BT")) {
            String faceDancer = event.getOption(CommandOptions.btFaceDancer.getName()).getAsString();
            Faction faction = gameState.getFaction("BT");

            List<TraitorCard> btFaceDancerHand = faction.getTraitorHand();
            LinkedList<TraitorCard> traitorDeck = gameState.getTraitorDeck();

            TraitorCard traitorCard = btFaceDancerHand.stream()
                    .filter(t -> t.name().equalsIgnoreCase(faceDancer))
                    .findFirst()
                    .get();

            btFaceDancerHand.remove(traitorCard);

            traitorDeck.add(traitorCard);
            Collections.shuffle(traitorDeck);
            btFaceDancerHand.add(traitorDeck.pollLast());

            discordGame.pushGameState();
            discordGame.sendMessage("turn-summary", faction.getEmoji() + " swapped a Face Dancer");
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
    }
}
