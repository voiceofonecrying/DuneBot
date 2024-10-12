package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.AtreidesFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class AtreidesCommands {

    public static List<CommandData> getCommands() {

        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("atreides", "Commands related to the Atreides Faction.").addSubcommands(
                        new SubcommandData("kh-count-increase", "Track forces lost for the purpose of unlocking the Kwisatz Haderach.").addOptions(amount),
                        new SubcommandData("block-card-prescience", "Block Atreides prescience on the next card for bid.").addOptions(atreidesKaramad)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (name.equals("kh-count-increase")) {
            addForcesLost(discordGame, game);
        } else if (name.equals("block-card-prescience")) {
            blockCardPrescience(discordGame, game);
        }
    }

    private static void addForcesLost(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        AtreidesFaction atreides = (AtreidesFaction) game.getFaction("Atreides");
        atreides.addForceLost(discordGame.required(amount).getAsInt());
        discordGame.pushGame();
    }

    private static void blockCardPrescience(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean prescienceBlocked = discordGame.optional(atreidesKaramad) != null && discordGame.required(atreidesKaramad).getAsBoolean();
        AtreidesFaction atreides = (AtreidesFaction) game.getFaction("Atreides");
        atreides.setCardPrescienceBlocked(prescienceBlocked);
        discordGame.pushGame();
    }
}
