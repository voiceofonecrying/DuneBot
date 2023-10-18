package controller.commands;

import constants.Emojis;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Leader;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static controller.commands.CommandOptions.*;

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
                        ).addOptions(faction, factionLeader),
                        new SubcommandData(
                                "return-leader",
                                "Return a captured leader to their faction.").addOptions(nonHarkLeader)
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
            case "return-leader" -> returnLeader(discordGame, game);
        }
    }

    private static void returnLeader(DiscordGame discordGame, Game game) throws IOException, ChannelNotFoundException {
        String returningLeader = discordGame.required(nonHarkLeader).getAsString();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));

        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);

        for (CSVRecord csvRecord : csvParser) {
            if (csvRecord.get(1).equals(returningLeader)) {
                Faction faction = game.getFaction(csvRecord.get(0));
                faction.addLeader(game.getFaction("Harkonnen").getLeader(returningLeader).orElseThrow());
                discordGame.getFactionLedger(faction).queueMessage(returningLeader + " has been returned to you.");
                break;
            }
        }
        game.getFaction("Harkonnen").removeLeader(returningLeader);
        discordGame.getHarkonnenLedger().queueMessage(returningLeader + " has returned to their original owner.");
        discordGame.getTurnSummary().queueMessage(returningLeader + " has returned to their original owner.");
        discordGame.pushGame();
    }

    public static void captureLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();

        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        Faction harkonnenFaction = game.getFaction("Harkonnen");

        harkonnenFaction.addLeader(leader);
        faction.removeLeader(leader);

        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (leader.skillCard() != null) {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} have captured a {1} skilled leader: {2} the {3}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(),
                    leader.name(), leader.skillCard().name()
            ));
        } else {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} have captured a {1} leader",
                    harkonnenFaction.getEmoji(), faction.getEmoji()
            ));
        }

        discordGame.getHarkonnenLedger().queueMessage("You have captured " + leader.name());
        discordGame.getFactionLedger(faction).queueMessage(
                leader.name() + " has been captured by " + harkonnenFaction.getEmoji()
        );

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

        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (leader.skillCard() != null) {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} has killed the {1} skilled leader, {2}, for 2 {3}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(), leader.name(), Emojis.SPICE
            ));
        } else {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} has killed the {1} leader for 2 {2}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(), Emojis.SPICE
            ));

        }

        Leader killedLeader = new Leader(leader.name(), leader.value(), null, true);

        game.getLeaderTanks().add(killedLeader);

        discordGame.getFactionChat(factionName).queueMessage(killedLeader.name() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        discordGame.getFactionLedger(factionName).queueMessage(killedLeader.name() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        discordGame.getHarkonnenLedger().queueMessage("You have killed " + killedLeader.name());
        CommandManager.spiceMessage(discordGame, 2, harkonnenFaction.getSpice(),
                "Harkonnen", "from the killed leader", true);

        if (game.hasGameOption(GameOption.NOT_READY_MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);

        discordGame.pushGame();
    }
}
