package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.FremenFaction;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class FremenCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("fremen", "Commands related to the Fremen Faction.").addSubcommands(
                new SubcommandData("karama-movement", "Prevent Fremen from moving 2 spaces in their next Movement")
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "karama-movement" -> karamaMovementSpeed(discordGame, game);
        }
    }

	public static void karamaMovementSpeed(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
		String chatMessage = "Your faster movement from knowledge of the desert has been negated by Karama. ";
		String publicMessage = " fast movement has been negated by Karama.";
		game.getFremenFaction().karamaMovement(chatMessage, publicMessage);
        discordGame.pushGame();
    }
}
