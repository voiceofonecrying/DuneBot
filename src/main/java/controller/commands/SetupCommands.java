package controller.commands;

import constants.Emojis;
import enums.GameOption;
import enums.SetupStep;
import enums.StepStatus;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

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
                        new SubcommandData("show-game-options", "Show the selected game options"),
                        new SubcommandData("add-game-option", "Add a game option")
                                .addOptions(CommandOptions.addGameOption),
                        new SubcommandData("remove-game-option", "Remove a game option")
                                .addOptions(CommandOptions.removeGameOption),
                        new SubcommandData("ix-hand-selection", "Only use this command to select the Ix starting treachery card").addOptions(CommandOptions.ixCard),
                        new SubcommandData("traitor", "Select a starting traitor from hand.")
                                .addOptions(CommandOptions.faction, CommandOptions.traitor),
                        new SubcommandData("advance", "Advance the setup of the game."),
                        new SubcommandData("leader-skill", "Add leader skill to faction")
                                .addOptions(CommandOptions.faction, CommandOptions.factionLeader, CommandOptions.factionLeaderSkill),
                        new SubcommandData("harkonnen-mulligan", "Mulligan Harkonnen traitor hand")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();

        switch (name) {
            case "faction" -> addFaction(event, discordGame, gameState);
            case "new-faction-resource" -> newFactionResource(event, discordGame, gameState);
            case "show-game-options" -> showGameOptions(discordGame, gameState);
            case "add-game-option" -> addGameOption(event, discordGame, gameState);
            case "remove-game-option" -> removeGameOption(event, discordGame, gameState);
            case "ix-hand-selection" -> ixHandSelection(event, discordGame, gameState);
            case "traitor" -> selectTraitor(event, discordGame, gameState);
            case "advance" -> advance(event, discordGame, gameState);
            case "leader-skill" -> factionLeaderSkill(event, discordGame, gameState);
            case "harkonnen-mulligan" -> harkonnenMulligan(discordGame, gameState);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (game.getTurn() != 0 || game.isSetupFinished()) {
            return;
        }

        if (!game.isSetupStarted() && !game.isSetupFinished()) {
            game.setSetupStarted(true);
            createSetupSteps(game);
            showSetupSteps(discordGame, game);
        }

        StepStatus stepStatus;

        do {
            SetupStep setupStep = game.getSetupSteps().remove(0);
            stepStatus = runSetupStep(event, discordGame, game, setupStep);

        } while (stepStatus == StepStatus.CONTINUE);

        discordGame.pushGameState();
    }

    public static void createSetupSteps(Game game) {
        List<SetupStep> setupSteps;

        if (game.hasGameOption(GameOption.LEADER_SKILLS)) {
            setupSteps = new ArrayList<>(List.of(
                    SetupStep.CREATE_DECKS,
                    SetupStep.FACTION_POSITIONS,
                    SetupStep.TREACHERY_CARDS,
                    SetupStep.LEADER_SKILL_CARDS,
                    SetupStep.SHOW_LEADER_SKILLS,
                    SetupStep.TRAITORS,
                    SetupStep.STORM_SELECTION,
                    SetupStep.START_GAME
            ));
        } else {
            setupSteps = new ArrayList<>(List.of(
                    SetupStep.CREATE_DECKS,
                    SetupStep.FACTION_POSITIONS,
                    SetupStep.TRAITORS,
                    SetupStep.TREACHERY_CARDS,
                    SetupStep.STORM_SELECTION,
                    SetupStep.START_GAME
            ));
        }

        if (game.hasFaction("BG")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.FACTION_POSITIONS) + 1,
                    SetupStep.BG_PREDICTION
            );
        }

        if (game.hasGameOption(GameOption.HARKONNEN_MULLIGAN) &&
                game.hasFaction("Harkonnen")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS),
                    SetupStep.HARKONNEN_TRAITORS
            );
        }

        if (game.hasFaction("BT")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.BT_FACE_DANCERS
            );
        }

        game.setSetupSteps(setupSteps);
    }

    public static void showSetupSteps(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String stringBuilder = "The Game setup will perform the following steps:\n" +
                game.getSetupSteps().stream().map(SetupStep::name)
                        .collect(Collectors.joining("\n"));

        discordGame.sendMessage("mod-info", stringBuilder);
    }

    public static StepStatus runSetupStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game, SetupStep setupStep) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        discordGame.sendMessage("mod-info", "Starting step " + setupStep.name());

        StepStatus stepStatus = StepStatus.STOP;

        switch (setupStep) {
            case CREATE_DECKS -> stepStatus = createDecks(game);
            case FACTION_POSITIONS -> stepStatus = factionPositions(discordGame, game);
            case BG_PREDICTION -> stepStatus = bgPredictionStep(discordGame, game);
            case TREACHERY_CARDS -> stepStatus = treacheryCardsStep(discordGame, game);
            case LEADER_SKILL_CARDS -> stepStatus = leaderSkillCardsStep(discordGame, game);
            case SHOW_LEADER_SKILLS -> stepStatus = showLeaderSkillCardsStep(event, discordGame, game);
            case HARKONNEN_TRAITORS -> stepStatus = harkonnenTraitorsStep(discordGame, game);
            case TRAITORS -> stepStatus = traitorSelectionStep(discordGame, game);
            case BT_FACE_DANCERS -> stepStatus = btDrawFaceDancersStep(discordGame, game);
            case STORM_SELECTION -> stepStatus = stormSelectionStep(discordGame, game);
            case START_GAME -> stepStatus = startGameStep(event, discordGame, game);
        }

        return stepStatus;
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

        String playerName = event.getOption("player").getAsUser().getAsMention();
        String userName = event.getOption("player").getAsMember().getNickname();
        Faction faction;

        switch (factionName.toUpperCase()) {
            case "ATREIDES"  -> faction = new AtreidesFaction(playerName, userName, gameState);
            case "BG"        -> faction = new BGFaction(playerName, userName, gameState);
            case "BT"        -> faction = new BTFaction(playerName, userName, gameState);
            case "CHOAM"     -> faction = new ChoamFaction(playerName, userName, gameState);
            case "EMPEROR"   -> faction = new EmperorFaction(playerName, userName, gameState);
            case "FREMEN"    -> faction = new FremenFaction(playerName, userName, gameState);
            case "GUILD"     -> faction = new GuildFaction(playerName, userName, gameState);
            case "HARKONNEN" -> faction = new HarkonnenFaction(playerName, userName, gameState);
            case "IX"        -> faction = new IxFaction(playerName, userName, gameState);
            case "RICHESE"   -> faction = new RicheseFaction(playerName, userName, gameState);
            default -> throw new IllegalStateException("Unexpected value: " + factionName.toUpperCase());
        }

        gameState.addFaction(faction);

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

    public static void newFactionResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        gameState.getFaction(event.getOption("factionname").getAsString())
                .addResource(new Resource(event.getOption("resource").getAsString(),
                        event.getOption("value").getAsString()));
        discordGame.pushGameState();
    }

    public static void showGameOptions(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String stringBuilder = "The following options are selected:\n" +
                gameState.getGameOptions().stream().map(GameOption::name)
                        .collect(Collectors.joining("\n"));

        discordGame.sendMessage("mod-info", stringBuilder);
    }

    public static void addGameOption(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        String gameOptionName = event.getOption("add-game-option").getAsString();
        GameOption gameOption = GameOption.valueOf(gameOptionName);

        gameState.addGameOption(gameOption);
        discordGame.pushGameState();
    }

    public static void removeGameOption(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        String gameOptionName = event.getOption("remove-game-option").getAsString();
        GameOption gameOption = GameOption.valueOf(gameOptionName);

        gameState.removeGameOption(gameOption);
        discordGame.pushGameState();
    }

    public static void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
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

    public static void selectTraitor(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        TraitorCard traitor = faction.getTraitorHand().stream().filter(traitorCard -> traitorCard.name().toLowerCase()
                .contains(event.getOption("traitor").getAsString().toLowerCase())).findFirst().orElseThrow();
        for (TraitorCard card : faction.getTraitorHand()) {
            if (!card.equals(traitor)) gameState.getTraitorDeck().add(card);
        }

        Collections.shuffle(faction.getTraitorHand());

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

    public static StepStatus createDecks(Game game) {
        if (game.hasGameOption(GameOption.SANDTROUT)) {
            game.getSpiceDeck().add(new SpiceCard("Sandtrout", -1, 0));
        }

        if (game.hasGameOption(GameOption.CHEAP_HERO_TRAITOR)) {
            game.getTraitorDeck().add(new TraitorCard("Cheap Hero", "Any", 0));
        }

        if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0), csvRecord.get(1)));
            }
        }

        Collections.shuffle(game.getTreacheryDeck());
        Collections.shuffle(game.getSpiceDeck());
        Collections.shuffle(game.getTraitorDeck());

        return StepStatus.CONTINUE;
    }

    public static StepStatus factionPositions(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getFactions());

        discordGame.sendMessage("turn-summary", "__**Game Setup**__");

        ShowCommands.showBoard(discordGame, gameState);

        return StepStatus.CONTINUE;
    }

    public static StepStatus bgPredictionStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("bg-chat", "Please make your secret prediction.");

        return StepStatus.STOP;
    }

    public static StepStatus treacheryCardsStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        for (Faction faction : gameState.getFactions()) {
            if (faction.getTreacheryHand().size() == 0) {
                gameState.drawCard("treachery deck", faction.getName());
                if (faction.getName().equals("Harkonnen")) gameState.drawCard("treachery deck", faction.getName());
                ShowCommands.writeFactionInfo(discordGame, faction);
            }
        }

        return StepStatus.CONTINUE;
    }

    public static StepStatus leaderSkillCardsStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Collections.shuffle(gameState.getLeaderSkillDeck());
        for (Faction faction : gameState.getFactions()) {

            // Drawing two Leader Skill Cards for user to choose from
            gameState.drawCard("leader skills deck", faction.getName());
            gameState.drawCard("leader skills deck", faction.getName());

            StringBuilder leaderSkillsMessage = new StringBuilder();
            leaderSkillsMessage.append("Please select your leader and their skill from the following two options:\n");
            leaderSkillsMessage.append(MessageFormat.format(
                    "{0}{1}{0}\n",
                    Emojis.WEIRDING, faction.getLeaderSkillsHand().get(0).name()
            ));

            leaderSkillsMessage.append(MessageFormat.format(
                    "{0}{1}{0}\n",
                    Emojis.WEIRDING, faction.getLeaderSkillsHand().get(1).name()
            ));

            discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", leaderSkillsMessage.toString());
        }

        return StepStatus.STOP;
    }

    public static StepStatus showLeaderSkillCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
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

        return StepStatus.CONTINUE;
    }

    public static StepStatus harkonnenTraitorsStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = game.getFaction("Harkonnen");

        for (int j = 0; j < 4; j++) {
            game.drawCard("traitor deck", faction.getName());
        }

        long numHarkonnenTraitors = faction.getTraitorHand().stream()
                .map(TraitorCard::factionName)
                .filter(f -> f.equalsIgnoreCase("Harkonnen"))
                .count();

        ShowCommands.writeFactionInfo(discordGame, faction);

        if (numHarkonnenTraitors > 1) {
            // Harkonnen can mulligan their hand
            discordGame.sendMessage("mod-info", "Harkonnen can mulligan");
            discordGame.sendMessage("harkonnen-chat", "Please decide if you will mulligan your Traitor cards.");
            return StepStatus.STOP;
        } else {
            discordGame.sendMessage("mod-info", "Harkonnen can not mulligan");
            return StepStatus.CONTINUE;
        }
    }

    public static void harkonnenMulligan(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (!game.hasFaction("Harkonnen"))
            return;

        Faction harkonnen = game.getFaction("Harkonnen");

        game.getTraitorDeck().addAll(harkonnen.getTraitorHand());
        harkonnen.getTraitorHand().clear();

        Collections.shuffle(game.getTraitorDeck());

        for (int j = 0; j < 4; j++) {
            game.drawCard("traitor deck", "Harkonnen");
        }

        ShowCommands.writeFactionInfo(discordGame, harkonnen);
        discordGame.pushGameState();
    }

    public static StepStatus traitorSelectionStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        for (Faction faction : gameState.getFactions()) {
            if (!faction.getName().equals("BT") && faction.getTraitorHand().size() == 0) {
                for (int j = 0; j < 4; j++) {
                    gameState.drawCard("traitor deck", faction.getName());
                }
                ShowCommands.writeFactionInfo(discordGame, faction);
                if (!faction.getName().equalsIgnoreCase("Harkonnen")) {
                    discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", "Please select your traitor.");
                }
            }
        }

        discordGame.sendMessage("turn-summary", "__**Setup: Traitors**__");

        return StepStatus.STOP;
    }

    public static StepStatus btDrawFaceDancersStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        gameState.drawCard("traitor deck", "BT");
        gameState.drawCard("traitor deck", "BT");
        gameState.drawCard("traitor deck", "BT");
        ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("BT"));

        discordGame.sendMessage("turn-summary", "Bene Tleilax have drawn their Face Dancers.");

        return StepStatus.CONTINUE;
    }

    public static StepStatus stormSelectionStep(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction1 = gameState.getFactions().get(0);
        Faction faction2 = gameState.getFactions().get(gameState.getFactions().size() - 1);
        discordGame.sendMessage(faction1.getName().toLowerCase() + "-chat",
                faction1.getPlayer() + " Please submit your dial for initial storm position (0-20)."
        );
        discordGame.sendMessage(faction2.getName().toLowerCase() + "-chat",
                faction2.getPlayer() + " Please submit your dial for initial storm position (0-20).");
        gameState.setStormMovement(new Random().nextInt(6) + 1);
        discordGame.sendMessage("turn-summary", "Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");

        return StepStatus.STOP;
    }

    public static StepStatus startGameStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.advanceTurn();
        game.setSetupFinished(true);
        ShowCommands.refreshFrontOfShieldInfo(event, discordGame, game);
        discordGame.sendMessage("mod-info", "The game has begun!");

        return StepStatus.STOP;
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

        Leader updatedLeader = new Leader(leader.name(), leader.value(), leaderSkillCard, leader.faceDown());

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
