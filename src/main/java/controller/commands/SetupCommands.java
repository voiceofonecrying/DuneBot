package controller.commands;

import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import templates.ChannelPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static controller.Initializers.getCSVFile;

public class SetupCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("setup", "Commands related to game setup.").addSubcommands(
                        new SubcommandData("faction", "Register a user to a faction in a game")
                                .addOptions(CommandOptions.allFactions, CommandOptions.user),
                        new SubcommandData("new-faction-resource", "Initialize a new resource for a faction")
                                .addOptions(CommandOptions.faction, CommandOptions.resourceName, CommandOptions.value),
                        new SubcommandData("expansion-choices", "Configure rules for a game before it starts.")
                                .addOptions(CommandOptions.techTokens, CommandOptions.sandTrout,
                                        CommandOptions.cheapHeroTraitor, CommandOptions.expansionTreacheryCards,
                                        CommandOptions.leaderSkills, CommandOptions.strongholdSkills
                                ),
                        new SubcommandData("ix-hand-selection", "Only use this command to select the Ix starting treachery card").addOptions(CommandOptions.ixCard),
                        new SubcommandData("traitor", "Select a starting traitor from hand.")
                                .addOptions(CommandOptions.faction, CommandOptions.traitor),
                        new SubcommandData("advance", "Advance the setup of the game.")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String name = event.getSubcommandName();

        switch (name) {
            case "faction" -> addFaction(event, discordGame, gameState);
            case "new-faction-resource" -> newFactionResource(event, discordGame, gameState);
            case "expansion-choices" -> expansionChoices(event, discordGame, gameState);
            case "ix-hand-selection" -> ixHandSelection(event, discordGame, gameState);
            case "traitor" -> selectTraitor(event, discordGame, gameState);
            case "advance" -> advance(event, discordGame, gameState);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.getTurn() != 0) {
            return;
        }

        int phase = gameState.getPhase();

        if (gameState.hasLeaderSkills()) {
            switch (phase) {
                case 0 -> factionPositionsAndBGPrediction(event, discordGame, gameState);
                case 1 -> treacheryCards(event, discordGame, gameState);
                case 2 -> leaderSkillCards(event, discordGame, gameState);
            }
        }

        gameState.advancePhase();
        discordGame.pushGameState();
    }

    public static void addFaction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        TextChannel modInfo = discordGame.getTextChannel("mod-info");

        if (gameState.getTurn() != 0) {
            modInfo.sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (gameState.getFactions().size() >= 6) {
            modInfo.sendMessage("This game is already full!").queue();
            return;
        }
        String factionName = event.getOption("faction").getAsString();
        if (gameState.hasFaction(factionName)) {
            modInfo.sendMessage("This faction has already been taken!").queue();
            return;
        }

        gameState.addFaction(new Faction(factionName, event.getOption("player").getAsUser().getAsMention(), event.getOption("player").getAsMember().getNickname(), gameState));

        Category game = discordGame.getGameCategory();
        discordGame.pushGameState();

        Member player = event.getOption("player").getAsMember();

        game.createTextChannel(factionName.toLowerCase() + "-info")
                .addPermissionOverride(
                        player,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .queue();

        game.createTextChannel(factionName.toLowerCase() + "-chat")
                .addPermissionOverride(
                        player,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .queue();
    }

    public static void newFactionResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.getFaction(event.getOption("factionname").getAsString())
                .addResource(new Resource(event.getOption("resource").getAsString(),
                        event.getOption("value").getAsString()));
        discordGame.pushGameState();
    }

    public static void expansionChoices(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {

        if (event.getOption("techtokens").getAsBoolean()) {
            gameState.setTechTokens(true);
        }

        if (event.getOption("sandtrout").getAsBoolean()) {
            gameState.getSpiceDeck().add(new SpiceCard("Sandtrout", -1, 0));
        }

        if (event.getOption("cheapherotraitor").getAsBoolean()) {
            gameState.getTraitorDeck().add(new TraitorCard("Cheap Hero", "Any", 0));
        }

        if (event.getOption("expansiontreacherycards").getAsBoolean()) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                gameState.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0), csvRecord.get(1)));
            }
        }

        if (event.getOption("leaderskills").getAsBoolean()) {
            gameState.setLeaderSkills(true);
        }

        if (event.getOption("strongholdskills").getAsBoolean()) {
            gameState.setStrongholdSkills(true);
        }

        discordGame.pushGameState();
    }

    public static void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        List<TreacheryCard> hand = gameState.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        TreacheryCard card = hand.stream().filter(treacheryCard -> treacheryCard.name().equals(event.getOption("ixcard").getAsString())).findFirst().orElseThrow();
        for (TreacheryCard treacheryCard : hand) {
            if (treacheryCard.equals(card)) continue;
            gameState.getTreacheryDeck().add(treacheryCard);
        }
        hand.removeIf(treacheryCard -> !treacheryCard.equals(card));
        ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("Ix"));
        discordGame.pushGameState();
    }

    public static void selectTraitor(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        TraitorCard traitor = faction.getTraitorHand().stream().filter(traitorCard -> traitorCard.name().toLowerCase()
                .contains(event.getOption("traitor").getAsString().toLowerCase())).findFirst().orElseThrow();
        for (TraitorCard card : faction.getTraitorHand()) {
            if (!card.equals(traitor)) gameState.getTraitorDeck().add(card);
        }
        faction.getTraitorHand().clear();
        faction.getTraitorHand().add(traitor);
        ShowCommands.writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public static void factionPositionsAndBGPrediction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getTreacheryDeck());
        Collections.shuffle(gameState.getSpiceDeck());
        Collections.shuffle(gameState.getFactions());

        discordGame.sendMessage("turn-summary", "__**Game Setup**__");

        ShowCommands.showBoard(discordGame, gameState);

        //If Bene Gesserit are present, time to make a prediction
        if (gameState.hasFaction("BG")) {
            discordGame.sendMessage("bg-chat", "Please make your secret prediction.");
        }
    }

    public static void treacheryCards(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Faction faction : gameState.getFactions()) {
            if (!faction.getName().equals("Ix")) gameState.drawCard("treachery deck", faction.getName());
            if (faction.getName().equals("Harkonnen")) gameState.drawCard("treachery deck", faction.getName());
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
    }

    public static void leaderSkillCards(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getLeaderSkillDeck());
        for (Faction faction : gameState.getFactions()) {

            // Drawing two Leader Skill Cards for user to choose from
            gameState.drawCard("leader skills deck", faction.getName());
            gameState.drawCard("leader skills deck", faction.getName());

            StringBuilder leaderSkillsMessage = new StringBuilder();
            leaderSkillsMessage.append("Please select your leader and their skill from the following two options:\n");
            leaderSkillsMessage.append("<:weirding:991763073281040518>" + faction.getLeaderSkillsHand().get(0).name() + "<:weirding:991763073281040518>\n");
            leaderSkillsMessage.append("<:weirding:991763073281040518>" + faction.getLeaderSkillsHand().get(1).name() + "<:weirding:991763073281040518>\n");


            discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", leaderSkillsMessage.toString());
        }
    }


}
