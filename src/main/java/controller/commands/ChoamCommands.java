package controller.commands;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.ChoamFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class ChoamCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("choam", "Commands related to CHOAM.").addSubcommands(
                        new SubcommandData("set-inflation", "Set CHOAM Inflation for next round.").addOptions(choamInflationType),
                        new SubcommandData("clear-inflation", "Clear CHOAM inflation to allow setting on a different turn."),
                        new SubcommandData("swap-card-with-ally", "Swap a Treachery Card with CHOAM ally.").addOptions(card, allyCard)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "set-inflation" -> setInflation(discordGame, game);
            case "clear-inflation" -> clearInflation(discordGame, game);
            case "swap-card-with-ally" -> swapCardWithAlly(discordGame, game);
            default -> throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void setInflation(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String inflationType = discordGame.required(choamInflationType).getAsString();

        ChoamInflationType choamInflationType = ChoamInflationType.valueOf(inflationType);
        ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");
        faction.setFirstInflation(choamInflationType);
        discordGame.pushGame();
    }

    public static void clearInflation(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");
        faction.clearInflation();
        game.getTurnSummary().publish(Emojis.CHOAM + " inflation has been deactivated and can be set for a different turn.");
        discordGame.pushGame();
    }

    public static void swapCardWithAlly(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        ChoamFaction choam = (ChoamFaction) game.getFaction("CHOAM");
        choam.swapCardWithAlly(discordGame.required(card).getAsString(), discordGame.required(allyCard).getAsString());
        discordGame.pushGame();
    }
}
