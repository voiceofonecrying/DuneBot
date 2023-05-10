package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class HarkCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("hark", "Commands related to the Harkonnen.").addSubcommands(
                        new SubcommandData(
                                "capture-leader",
                                "Capture a faction's leader after winning a battle."
                        ).addOptions(CommandOptions.faction, CommandOptions.factionLeader),
                        new SubcommandData(
                                "kill-leader",
                                "Kill a faction's leader after winning a battle."
                        ).addOptions(CommandOptions.faction, CommandOptions.factionLeader)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();

        if (!gameState.hasFaction("Harkonnen")) return;

        switch (name) {
            case "capture-leader" -> captureLeader(event, discordGame, gameState);
            case "kill-leader" -> killLeader(event, discordGame, gameState);
        }
    }

    public static void captureLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        String factionName = event.getOption(CommandOptions.faction.getName()).getAsString();
        String leaderName = event.getOption(CommandOptions.factionLeader.getName()).getAsString();

        Faction faction = gameState.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        Faction harkonnenFaction = gameState.getFaction("Harkonnen");
        List<Leader> harkonnenLeaders = harkonnenFaction.getLeaders();

        harkonnenLeaders.add(leader);
        faction.getLeaders().remove(leader);

        ShowCommands.writeFactionInfo(discordGame, faction);
        ShowCommands.writeFactionInfo(discordGame, harkonnenFaction);

        if (leader.skillCard() != null) {
            ShowCommands.refreshFrontOfShieldInfo(event, discordGame, gameState);
            discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} have captured a {1} skilled leader: {2} the {3}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(),
                    leader.name(), leader.skillCard().name()
            ));
        } else {
            discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} have captured a {1} leader",
                    harkonnenFaction.getEmoji(), faction.getEmoji()
            ));
        }

        discordGame.pushGameState();
    }

    public static void killLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        String factionName = event.getOption(CommandOptions.faction.getName()).getAsString();
        String leaderName = event.getOption(CommandOptions.factionLeader.getName()).getAsString();

        Faction faction = gameState.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        if (leader.skillCard() != null) {
            gameState.getLeaderSkillDeck().add(leader.skillCard());
        }



        faction.getLeaders().remove(leader);

        Faction harkonnenFaction = gameState.getFaction("Harkonnen");

        harkonnenFaction.addSpice(2);

        if (leader.skillCard()!= null) {
            discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} has killed the {1} skilled leader, {2}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(), leader.name()
            ));
        } else {
            discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} has killed the {1} leader",
                    harkonnenFaction.getEmoji(), faction.getEmoji()
            ));

        }

        Leader killedLeader = new Leader(leader.name(), leader.value(), null, true);

        gameState.getLeaderTanks().add(killedLeader);

        CommandManager.spiceMessage(discordGame, 2, "Harkonnen", "from the killed leader", true);

        ShowCommands.writeFactionInfo(discordGame, faction);
        ShowCommands.writeFactionInfo(discordGame, harkonnenFaction);

        ShowCommands.refreshFrontOfShieldInfo(event, discordGame, gameState);

        ShowCommands.showBoard(discordGame, gameState);

        discordGame.pushGameState();
    }
}
