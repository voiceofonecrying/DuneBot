package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.AtreidesFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class AtreidesCommands {

    public static List<CommandData> getCommands() {

        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("atreides", "Commands related to the Atreides Faction.").addSubcommands(
                        new SubcommandData(
                                "kh-count-increase",
                                "Track forces lost for the purpose of unlocking the Kwisatz Haderach."
                        ).addOptions(amount))
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "kh-count-increase" -> addForcesLost(discordGame, game);
        }
    }

    private static void addForcesLost(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        AtreidesFaction atreides = (AtreidesFaction) game.getFaction("Atreides");
        if (atreides.getForcesLost() >= 7)
            return;
        atreides.addForceLost(discordGame.required(amount).getAsInt());
        if (atreides.getForcesLost() >= 7) {
            atreides.addLeader(new Leader("Kwisatz Haderach", 2, null, false));
            discordGame.getTurnSummary().queueMessage("The sleeper has awakened! " + Emojis.ATREIDES + " Paul Muad'Dib! Muad'Dib! Muad'Dib!");
        }
        discordGame.pushGame();
    }

    public static void sendAtreidesCardPrescience(DiscordGame discordGame, Game game, TreacheryCard card) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (game.hasFaction("Atreides")) {
            discordGame.getAtreidesChat().queueMessage(
                    MessageFormat.format(
                            "You predict {0} {1} {0} is up for bid (R{2}:C{3}).",
                            Emojis.TREACHERY, card.name().strip(), game.getTurn(), bidding.getBidCardNumber()
                    )
            );
            if (game.getFaction("Atreides").isHomeworldOccupied()) {
                discordGame.getFactionChat(game.getFaction("Atreides").getOccupier().getName()).queueMessage(
                        MessageFormat.format(
                                "Your " + Emojis.ATREIDES + " subjects in Caladan predict {0} {1} {0} is up for bid (R{2}:C{3}).",
                                Emojis.TREACHERY, card.name().strip(), game.getTurn(), bidding.getBidCardNumber()
                        )
                );
            }
        }
    }
}
