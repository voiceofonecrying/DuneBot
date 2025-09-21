package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.factions.HomebrewFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class HomebrewCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("homebrew", "Commands related to Homebrew Factions.").addSubcommands(
                new SubcommandData("set-proxy-faction", "Set the faction to be used for emojis and default images.").addOptions(homebrewFaction, proxyFaction)
//                new SubcommandData("set-proxy-homeworld", "Set a different homeworld image if desired.").addOptions(atreidesKaramad)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        if (name.equals("set-proxy-faction")) setProxyFaction(discordGame, game);
    }

    private static void setProxyFaction(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String homebrewFactionName = discordGame.required(homebrewFaction).getAsString();
        String proxyFactionName = discordGame.required(proxyFaction).getAsString();
        ((HomebrewFaction) game.getFaction(homebrewFactionName)).setFactionProxy(proxyFactionName);
        discordGame.pushGame();
    }
}
