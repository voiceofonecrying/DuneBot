package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.faction;
import static controller.commands.CommandOptions.factionLeader;

public class HarkCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("hark", "Commands related to the Harkonnen.").addSubcommands(
                        new SubcommandData(
                                "capture-leader",
                                "Capture a faction's leader after winning a battle."
                        ).addOptions(faction, factionLeader),
                        new SubcommandData(
                                "kill-leader",
                                "Kill a faction's leader after winning a battle."
                        ).addOptions(faction, factionLeader)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (!game.hasFaction("Harkonnen")) return;

        switch (name) {
            case "capture-leader" -> captureLeader(discordGame, game);
            case "kill-leader" -> killLeader(discordGame, game);
        }
    }

    public static void captureLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();

        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        Faction harkonnenFaction = game.getFaction("Harkonnen");

        harkonnenFaction.addLeader(leader);
        faction.removeLeader(leader);

        if (leader.skillCard() != null) {
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

        discordGame.pushGame();
    }

    public static void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();

        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        if (leader.skillCard() != null) {
            game.getLeaderSkillDeck().add(leader.skillCard());
        }

        faction.removeLeader(leader);

        Faction harkonnenFaction = game.getFaction("Harkonnen");

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

        game.getLeaderTanks().add(killedLeader);

        CommandManager.spiceMessage(discordGame, 2, "Harkonnen", "from the killed leader", true);

        ShowCommands.showBoard(discordGame, game);

        discordGame.pushGame();
    }
}
