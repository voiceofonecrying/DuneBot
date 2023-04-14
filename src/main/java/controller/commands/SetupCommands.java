package controller.commands;

import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import templates.ChannelPermissions;
import utils.CardImages;

import java.text.MessageFormat;
import java.util.*;

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
                        new SubcommandData("advance", "Advance the setup of the game."),
                        new SubcommandData("faction-leader-skill", "Add leader skill to faction")
                                .addOptions(CommandOptions.faction, CommandOptions.factionLeader, CommandOptions.factionLeaderSkill)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();

        switch (name) {
            case "faction" -> addFaction(event, discordGame, gameState);
            case "new-faction-resource" -> newFactionResource(event, discordGame, gameState);
            case "expansion-choices" -> expansionChoices(event, discordGame, gameState);
            case "ix-hand-selection" -> ixHandSelection(event, discordGame, gameState);
            case "traitor" -> selectTraitor(event, discordGame, gameState);
            case "advance" -> advance(event, discordGame, gameState);
            case "faction-leader-skill" -> factionLeaderSkill(event, discordGame, gameState);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        if (gameState.getTurn() != 0) {
            return;
        }

        int phase = gameState.getPhase();

        if (gameState.hasLeaderSkills()) {
            switch (phase) {
                case 0 -> factionPositionsAndBGPredictionStep(event, discordGame, gameState);
                case 1 -> treacheryCardsStep(event, discordGame, gameState);
                case 2 -> leaderSkillCardsStep(event, discordGame, gameState);
                case 3 -> showLeaderSkillCardsStep(event, discordGame, gameState);
                case 4 -> traitorSelectionStep(event, discordGame, gameState);
                case 5 -> shuffleTraitorsDrawFaceDancers(event, discordGame, gameState);
                case 6 -> stormSelectionStep(event, discordGame, gameState);
                case 7 -> startGameStep(event, discordGame, gameState);
            }
        } else {
            switch (phase) {
                case 0 -> factionPositionsAndBGPredictionStep(event, discordGame, gameState);
                case 1 -> traitorSelectionStep(event, discordGame, gameState);
                case 2 -> shuffleTraitorsDrawFaceDancers(event, discordGame, gameState);
                case 3 -> treacheryCardsStep(event, discordGame, gameState);
                case 4 -> stormSelectionStep(event, discordGame, gameState);
                case 5 -> startGameStep(event, discordGame, gameState);
            }
        }

        if (gameState.getTurn() == 0) gameState.advancePhase();
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

        discordGame.sendMessage(faction.getName().toLowerCase() + "-chat",
                MessageFormat.format(
                        "{0} is in debt to you.  I'm sure they'll find a way to pay you back...",
                        traitor.name()
                ));

        discordGame.pushGameState();
    }

    public static void factionPositionsAndBGPredictionStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
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

    public static void treacheryCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Faction faction : gameState.getFactions()) {
            if (!faction.getName().equals("Ix")) gameState.drawCard("treachery deck", faction.getName());
            if (faction.getName().equals("Harkonnen")) gameState.drawCard("treachery deck", faction.getName());
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
    }

    public static void leaderSkillCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
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

    public static void showLeaderSkillCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String channelName = "turn-summary";
        List<Triple<String, String, String>> leaderSkillInfos = new ArrayList<>();

        for (Faction faction : gameState.getFactions()) {
            Optional<Leader> leader = faction.getLeaders().stream().filter(l -> l.skillCard() != null).findFirst();

            if (leader.isEmpty()) {
                throw new InvalidGameStateException(MessageFormat.format("Faction {0} do not have a skilled leader", faction.getName()));
            } else {
                leaderSkillInfos.add(
                        new ImmutableTriple(faction.getEmoji(), leader.get().name(), leader.get().skillCard().name())
                );
            }
        }

        discordGame.sendMessage(channelName, "__**Setup: Leader Skills**__");

        for (Triple<String, String, String> leaderSkillInfo : leaderSkillInfos) {
            String message = MessageFormat.format(
                    "{0} - {1} is a {2}",
                    leaderSkillInfo.getLeft(),
                    leaderSkillInfo.getMiddle(),
                    leaderSkillInfo.getRight()
            );

            Optional<FileUpload> fileUpload = CardImages.getLeaderSkillImage(event.getGuild(), leaderSkillInfo.getRight());

            if (fileUpload.isEmpty()) {
                discordGame.sendMessage(channelName, message);
            } else {
                discordGame.sendMessage(channelName, message, fileUpload.get());
            }
        }
    }

    public static void traitorSelectionStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getTraitorDeck());
        for (Faction faction : gameState.getFactions()) {
            if (!faction.getName().equals("BT")) {
                for (int j = 0; j < 4; j++) {
                    gameState.drawCard("traitor deck", faction.getName());
                }
                ShowCommands.writeFactionInfo(discordGame, faction);
            }
        }
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().contains("-chat") && !channel.getName().contains("game") &&
                    !channel.getName().contains("harkonnen") && !channel.getName().contains("bt")) discordGame.sendMessage(channel.getName(), "Please select your traitor.");
        }

        discordGame.sendMessage("turn-summary", "__**Setup: Traitors**__");
    }

    public static void stormSelectionStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction1 = gameState.getFactions().get(0);
        Faction faction2 = gameState.getFactions().get(gameState.getFactions().size() - 1);
        discordGame.sendMessage(faction1.getName().toLowerCase() + "-chat",
                faction1.getPlayer() + " Please submit your dial for initial storm position (0-20)."
        );
        discordGame.sendMessage(faction2.getName().toLowerCase() + "-chat",
                faction2.getPlayer() + " Please submit your dial for initial storm position (0-20).");
        gameState.setStormMovement(new Random().nextInt(6) + 1);
        discordGame.sendMessage("turn-summary", "Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");
    }

    public static void startGameStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.advanceTurn();
        discordGame.sendMessage("mod-info", "The game has begun!");
    }

    public static void shuffleTraitorsDrawFaceDancers(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getTraitorDeck());
        if (gameState.hasFaction("BT")) {
            gameState.drawCard("traitor deck", "BT");
            gameState.drawCard("traitor deck", "BT");
            gameState.drawCard("traitor deck", "BT");
            ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("BT"));
        }

        discordGame.sendMessage("turn-summary", "Bene Tleilax have drawn their Face Dancers.");
    }

    public static void factionLeaderSkill(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String factionName = event.getOption(CommandOptions.faction.getName()).getAsString();
        String leaderName = event.getOption(CommandOptions.factionLeader.getName()).getAsString();
        String leaderSkillName = event.getOption(CommandOptions.factionLeaderSkill.getName()).getAsString();


        Faction faction = gameState.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).get();
        LeaderSkillCard leaderSkillCard = faction.getLeaderSkillsHand().stream()
                .filter(l -> l.name().equalsIgnoreCase(leaderSkillName))
                .findFirst().get();

        Leader updatedLeader = new Leader(leader.name(), leader.value(), leaderSkillCard);

        faction.removeLeader(leader);
        faction.addLeader(updatedLeader);

        faction.getLeaderSkillsHand().clear();

        discordGame.sendMessage(
                faction.getName().toLowerCase() + "-chat",
                MessageFormat.format("After years of training, {0} has become a {1}! ",
                        updatedLeader.name(), leaderSkillCard.name()
                )
        );
        discordGame.pushGameState();
    }
}
