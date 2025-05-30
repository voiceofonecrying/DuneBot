package controller.commands;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class HarkCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("hark", "Commands related to the Harkonnen.").addSubcommands(
                        new SubcommandData("capture-leader", "Capture a faction's leader after winning a battle.").addOptions(faction, factionLeader),
                        new SubcommandData("kill-leader", "Kill a faction's leader after winning a battle.").addOptions(faction, factionLeader),
                        new SubcommandData("return-leader", "Return a captured leader to their faction.").addOptions(nonHarkLeader),
                        new SubcommandData("nexus-card-secret-ally", "Play Harkonnen Nexus Card as Secret Ally"),
                        new SubcommandData("block-bonus-card", "Block Harkonnen from receiving bonus card.").addOptions(harkonnenKaramad)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (!game.hasFaction("Harkonnen")) return;

        switch (name) {
            case "capture-leader" -> captureLeader(discordGame, game);
            case "kill-leader" -> killLeader(discordGame, game);
            case "return-leader" -> returnLeader(discordGame, game);
            case "nexus-card-secret-ally" -> nexusCardSecretAlly(discordGame, game);
            case "block-bonus-card" -> blockBonusCard(discordGame, game);
        }
    }

    private static void returnLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String returningLeader = discordGame.required(nonHarkLeader).getAsString();
        game.getHarkonnenFaction().returnCapturedLeader(returningLeader);
        discordGame.pushGame();
    }

    public static void captureLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();
        game.getHarkonnenFaction().keepCapturedLeader(factionName, leaderName);
        discordGame.pushGame();
    }

    public static void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();
        game.getHarkonnenFaction().killCapturedLeader(factionName, leaderName);
        discordGame.pushGame();
    }

    private static void nexusCardSecretAlly(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.harkonnenSecretAlly();
        discordGame.pushGame();
    }

    private static void blockBonusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean prescienceBlocked = discordGame.optional(harkonnenKaramad) != null && discordGame.required(harkonnenKaramad).getAsBoolean();
        game.getHarkonnenFaction().setBonusCardBlocked(prescienceBlocked);
        discordGame.pushGame();
    }
}
