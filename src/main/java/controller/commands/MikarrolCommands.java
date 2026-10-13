package controller.commands;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.MikarrolFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class MikarrolCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("mikarrol", "Commands related to Mikarrol.").addSubcommands(
                        new SubcommandData("raid", "Raid a faction's treachery hand.").addOptions(faction),
                        new SubcommandData("slavers", "Activate Mikarrol's Slavers advantage.").addOptions(amount),
	                    new SubcommandData("ally-slavers", "Activate Mikarrol's alliance ability.").addOptions(amount),
                        new SubcommandData("block-raiders", "Block or unblock Mikarrol from using Raiders.").addOptions(mikarrolKaramad)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "raid" -> raid(discordGame, game);
	        case "slavers" -> slavers(discordGame, game);
	        case "ally-slavers" -> allySlavers(discordGame, game);
            case "block-raiders" -> blockRaiders(discordGame, game);
        }
    }

    private static void raid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = discordGame.required(faction).getAsString();
        game.getMikarrolFaction().raid(factionName);
        discordGame.pushGame();
    }

    private static void blockRaiders(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean raidersBlocked = discordGame.optional(mikarrolKaramad) == null || discordGame.optional(mikarrolKaramad).getAsBoolean();
        game.getMikarrolFaction().setRaidersBlocked(raidersBlocked);
        discordGame.pushGame();
    }

    private static void slavers(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int slaversAmount = discordGame.required(amount).getAsInt();
        MikarrolFaction faction = game.getMikarrolFaction();
        faction.setSlaversAmount(slaversAmount);
        faction.slavers();
	}

    private static void allySlavers(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int slaversAmount = discordGame.required(amount).getAsInt();
        MikarrolFaction faction = game.getMikarrolFaction();
        faction.setAllySlaversAmount(slaversAmount);
        faction.allySlavers();
    }
}

