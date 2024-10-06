package controller.commands;

import constants.Emojis;
import controller.channels.TurnSummary;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.Leader;
import model.TraitorCard;
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
                        new SubcommandData("nexus-card-lose-traitor", "Lose the played traitor to the deck. New traitor will be drawn in Mentat Pause.").addOptions(traitor)
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
            case "nexus-card-lose-traitor" -> nexusCardLoseTraitor(discordGame, game);
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
        Faction harkonnen = game.getFaction("Harkonnen");
        harkonnen.removeLeader(returningLeader);
        harkonnen.getLedger().publish(returningLeader + " has returned to their original owner.");
        game.getTurnSummary().publish(returningLeader + " has returned to their original owner.");
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
        if (leader.getSkillCard() != null) {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} have captured a {1} skilled leader: {2} the {3}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(),
                    leader.getName(), leader.getSkillCard().name()
            ));
        } else {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} have captured a {1} leader",
                    harkonnenFaction.getEmoji(), faction.getEmoji()
            ));
        }

        harkonnenFaction.getLedger().publish("You have captured " + leader.getName());
        discordGame.getFactionLedger(faction).queueMessage(
                leader.getName() + " has been captured by " + harkonnenFaction.getEmoji()
        );

        discordGame.pushGame();
    }

    public static void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();

        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        if (leader.getSkillCard() != null) {
            game.getLeaderSkillDeck().add(leader.getSkillCard());
        }

        faction.removeLeader(leader);

        Faction harkonnenFaction = game.getFaction("Harkonnen");

        harkonnenFaction.addSpice(2, "killing " + leader.getName());

        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (leader.getSkillCard() != null) {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} has killed the {1} skilled leader, {2}, for 2 {3}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(), leader.getName(), Emojis.SPICE
            ));
        } else {
            turnSummary.queueMessage(MessageFormat.format(
                    "{0} has killed the {1} leader for 2 {2}",
                    harkonnenFaction.getEmoji(), faction.getEmoji(), Emojis.SPICE
            ));

        }

        Leader killedLeader = new Leader(leader.getName(), leader.getValue(), leader.getOriginalFactionName(), null, true);

        game.getLeaderTanks().add(killedLeader);

        discordGame.getFactionChat(faction).queueMessage(killedLeader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        discordGame.getFactionLedger(faction).queueMessage(killedLeader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        harkonnenFaction.getLedger().publish("You have killed " + killedLeader.getName());

        game.setUpdated(UpdateType.MAP);

        discordGame.pushGame();
    }

    private static void nexusCardLoseTraitor(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String traitorName = discordGame.required(traitor).getAsString();
        Faction faction = game.getFaction("Harkonnen");
        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();
        TraitorCard traitorCard = faction.getTraitorHand().stream()
                .filter(t -> t.name().equalsIgnoreCase(traitorName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Traitor: " + traitorName));
        faction.removeTraitorCard(traitorCard);
        traitorDeck.add(traitorCard);
        Collections.shuffle(traitorDeck);
        faction.getLedger().publish(traitorName + " has been shuffled back into the Traitor Deck.");
        game.getTurnSummary().publish(faction.getEmoji() + " loses " + traitorName + " and will draw a new Traitor in Mentat Pause.");
        discordGame.pushGame();
    }
}
