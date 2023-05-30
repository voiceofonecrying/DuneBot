package controller.commands;

import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase.").addOptions(CommandOptions.amount),
                new SubcommandData("set-auto-bid-policy", "Set policy to either increment (bids current bid + 1) or " +
                        "exact (bids whatever your current set bid is).").addOptions(CommandOptions.incrementOrExact),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting. If enabled, bot" +
                        " will pass for you if your bid is lower than current bid on your turn.").addOptions(CommandOptions.autoPass)
        ));

        return commandData;
    }


    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {


    }
}
