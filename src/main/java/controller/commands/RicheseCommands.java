package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class RicheseCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("richese", "Commands related to the Richese Faction.").addSubcommands(
                        new SubcommandData(
                                "no-fields-to-front-of-shield",
                                "Move the Richese No-Fields token to the Front of Shield."
                        ).addOptions(CommandOptions.richeseNoFields)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String name = event.getSubcommandName();

        switch (name) {
            case "no-fields-to-front-of-shield" -> moveNoFieldsToFrontOfShield(event, discordGame, gameState);
        }
    }

    public static void moveNoFieldsToFrontOfShield(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        int noFieldValue = event.getOption(CommandOptions.richeseNoFields.getName()).getAsInt();

        if (gameState.hasFaction("Richese")) {
            Faction faction = gameState.getFaction("Richese");

            if (!faction.hasResource("frontOfShieldNoField")) {
                faction.addResource(new IntegerResource("frontOfShieldNoField", noFieldValue, 0, 5));
            } else {
                ((Resource<Integer>)faction.getResource("frontOfShieldNoField")).setValue(noFieldValue);
            }

            ShowCommands.refreshFrontOfShieldInfo(event, discordGame, gameState);
            discordGame.pushGameState();
        }
    }
}
