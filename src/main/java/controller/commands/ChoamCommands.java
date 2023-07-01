package controller.commands;

import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.factions.ChoamFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class ChoamCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("choam", "Commands related to CHOAM.").addSubcommands(
                        new SubcommandData(
                                "set-inflation",
                                "Set CHOAM Inflation for next round."
                        ).addOptions(CommandOptions.choamInflationType)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();

        switch (name) {
            case "set-inflation" -> setInflation(event, discordGame, game);
        }
    }

    public static void setInflation(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasFaction("CHOAM")) {
            String inflationType = event.getOption(CommandOptions.choamInflationType.getName()).getAsString();
            ChoamInflationType choamInflationType = ChoamInflationType.valueOf(inflationType);

            ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");

            faction.setFirstInflation(game.getTurn(), choamInflationType);

            discordGame.pushGame();
            discordGame.sendMessage("turn-summary", "CHOAM set inflation to " + choamInflationType + " for next round!");
        }
    }
}
