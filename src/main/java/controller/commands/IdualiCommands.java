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

public class IdualiCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("iduali", "Commands related to the Iduali Faction.").addSubcommands(
                new SubcommandData("place-territory", "Place an Iduali special southern territory").addOptions(idualiTerritory, adjacentTerritory, sector),
				new SubcommandData("craft-card", "Craft a card").addOptions(craftCard, firstCard, secondCard),
				new SubcommandData("ally-craft-card", "Craft a card for Iduali's ally").addOptions(craftCard, firstCard, secondCard),
                new SubcommandData("karama-caves", "Prevent Iduali from using Deep Desert Caves in their next Movement")
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
			case "place-territory"-> placeTerritory(discordGame, game);
            case "craft-card" -> craftCard(discordGame, game, false);
			case "ally-craft-card" -> craftCard(discordGame, game, true);
			case "karama-caves" -> karamaCaves(discordGame, game);
        }
    }

	public static void placeTerritory(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
		String idualiTerritoryName = discordGame.required(idualiTerritory).getAsString();
		String adjacentTerritoryName = discordGame.required(adjacentTerritory).getAsString();
		int sectorInt = Integer.parseInt(discordGame.required(sector).getAsString());
		game.getIdualiFaction().placeTerritory(idualiTerritoryName, sectorInt, adjacentTerritoryName);
        discordGame.pushGame();
    }

	public static void craftCard(DiscordGame discordGame, Game game, boolean forAlly) throws InvalidGameStateException, ChannelNotFoundException {
		String craftCardName = discordGame.required(craftCard).getAsString();
		String firstCardName = discordGame.required(firstCard).getAsString();
		String secondCardName = discordGame.required(secondCard).getAsString();
		game.getIdualiFaction().craft(craftCardName, firstCardName, secondCardName, forAlly);
        discordGame.pushGame();
    }

	public static void karamaCaves(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
		game.getIdualiFaction().setCavesBlocked(true);
        discordGame.pushGame();
    }
}
