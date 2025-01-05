package controller.commands;

import controller.buttons.BTButtons;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.BTFaction;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class BTCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("bt", "Commands related to the Bene Tleilaxu.").addSubcommands(
                new SubcommandData("swap-face-dancer", "Swaps a BT Face Dancer.").addOptions(btFaceDancer),
                new SubcommandData("reveal-face-dancer", "Reveals a BT Face Dancer.").addOptions(btFaceDancer),
                new SubcommandData("set-revival-limit", "Set the revival limit for a faction.").addOptions(faction, amount),
                new SubcommandData("nexus-card-cunning", "Replace revealed Face Dancers.")
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "swap-face-dancer" -> swapBTFaceDancer(discordGame, game);
            case "reveal-face-dancer" -> revealBTFaceDancer(discordGame, game);
            case "set-revival-limit" -> setRevivalLimit(event, discordGame, game);
            case "nexus-card-cunning" -> nexusCardCunning(discordGame, game);
            default -> throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void swapBTFaceDancer(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String faceDancer = discordGame.required(btFaceDancer).getAsString();
        ((BTFaction) game.getFaction("BT")).swapFaceDancer(faceDancer);
        discordGame.pushGame();
    }

    public static void revealBTFaceDancer(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String faceDancer = discordGame.required(btFaceDancer).getAsString();
        ((BTFaction) game.getFaction("BT")).revealFaceDancer(faceDancer, game);
        discordGame.pushGame();
    }

    public static void setRevivalLimit(MessageChannel channel, DiscordGame discordGame, Game game, String factionName, int revivalLimit) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        BTFaction bt = (BTFaction) game.getFaction("BT");
        bt.setRevivalLimit(factionName, revivalLimit);
        if (bt.hasSetAllRevivalLimits()) {
            RunCommands.advance(discordGame, game);
            BTButtons.deleteRevivalButtonsInChannel(channel);
        } else
            discordGame.pushGame();
    }

    public static void setRevivalLimit(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException, IOException {
        String factionName = discordGame.required(faction).getAsString();
        if (factionName.equals("BT"))
            throw new IllegalArgumentException("BT revival limit is always 20.");
        int revivalLimit = discordGame.required(amount).getAsInt();
        setRevivalLimit(event.getMessageChannel(), discordGame, game, factionName, revivalLimit);
    }

    public static void nexusCardCunning(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        BTFaction bt = (BTFaction) game.getFaction("BT");
        bt.nexusCardCunning();
        discordGame.pushGame();
    }
}
