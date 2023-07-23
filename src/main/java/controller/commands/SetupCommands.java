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
import templates.ChannelPermissions;
import utils.CardImages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static controller.Initializers.getCSVFile;
import static controller.commands.CommandOptions.*;

public class SetupCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("setup", "Commands related to game setup.").addSubcommands(
                        new SubcommandData("faction", "Register a user to a faction in a game")
                                .addOptions(allFactions, user),
                        new SubcommandData("show-game-options", "Show the selected game options"),
                        new SubcommandData("add-game-option", "Add a game option")
                                .addOptions(CommandOptions.addGameOption),
                        new SubcommandData("remove-game-option", "Remove a game option")
                                .addOptions(CommandOptions.removeGameOption),
                        new SubcommandData("ix-hand-selection", "Only use this command to select the Ix starting treachery card").addOptions(CommandOptions.ixCard),
                        new SubcommandData("traitor", "Select a starting traitor from hand.")
                                .addOptions(faction, CommandOptions.traitor),
                        new SubcommandData("advance", "Advance the setup of the game."),
                        new SubcommandData("leader-skill", "Add leader skill to faction")
                                .addOptions(faction, CommandOptions.factionLeader, CommandOptions.factionLeaderSkill),
                        new SubcommandData("harkonnen-mulligan", "Mulligan Harkonnen traitor hand")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "faction" -> addFaction(discordGame, game);
            case "show-game-options" -> showGameOptions(discordGame, game);
            case "add-game-option" -> addGameOption(discordGame, game);
            case "remove-game-option" -> removeGameOption(discordGame, game);
            case "ix-hand-selection" -> ixHandSelection(discordGame, game);
            case "traitor" -> selectTraitor(discordGame, game);
            case "advance" -> advance(event, discordGame, game);
            case "leader-skill" -> factionLeaderSkill(discordGame, game);
            case "harkonnen-mulligan" -> harkonnenMulligan(discordGame, game);
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

        discordGame.pushGame();
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
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.BG_FORCE
            );
        }

        if (game.hasFaction("Fremen")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.FREMEN_FORCES
            );
        }

        if (game.hasFaction("Ix")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TREACHERY_CARDS),
                    SetupStep.IX_CARD_SELECTION
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
            case FREMEN_FORCES -> stepStatus = fremenForcesStep(discordGame, game);
            case BG_FORCE -> stepStatus = bgForceStep(discordGame, game);
            case IX_CARD_SELECTION -> stepStatus = assignCardsToIx(discordGame, game);
            case TREACHERY_CARDS -> stepStatus = treacheryCardsStep(game);
            case LEADER_SKILL_CARDS -> stepStatus = leaderSkillCardsStep(discordGame, game);
            case SHOW_LEADER_SKILLS -> stepStatus = showLeaderSkillCardsStep(event, discordGame, game);
            case HARKONNEN_TRAITORS -> stepStatus = harkonnenTraitorsStep(discordGame, game);
            case TRAITORS -> stepStatus = traitorSelectionStep(discordGame, game);
            case BT_FACE_DANCERS -> stepStatus = btDrawFaceDancersStep(discordGame, game);
            case STORM_SELECTION -> stepStatus = stormSelectionStep(discordGame, game);
            case START_GAME -> stepStatus = startGameStep(discordGame, game);
        }

        return stepStatus;
    }

    public static void addFaction(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        TextChannel modInfo = discordGame.getTextChannel("mod-info");
        String factionName = discordGame.required(allFactions).getAsString();
        String playerName = discordGame.required(user).getAsUser().getAsMention();
        Member player = discordGame.required(user).getAsMember();

        if (player == null) throw new IllegalArgumentException("Not a valid user");

        String userName = player.getNickname();

        if (game.getTurn() != 0) {
            modInfo.sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (game.getFactions().size() >= 6) {
            modInfo.sendMessage("This game is already full!").queue();
            return;
        }
        if (game.hasFaction(factionName)) {
            modInfo.sendMessage("This faction has already been taken!").queue();
            return;
        }
        Faction faction;

        switch (factionName.toUpperCase()) {
            case "ATREIDES"  -> faction = new AtreidesFaction(playerName, userName, game);
            case "BG"        -> faction = new BGFaction(playerName, userName, game);
            case "BT"        -> faction = new BTFaction(playerName, userName, game);
            case "CHOAM"     -> faction = new ChoamFaction(playerName, userName, game);
            case "EMPEROR"   -> faction = new EmperorFaction(playerName, userName, game);
            case "FREMEN"    -> faction = new FremenFaction(playerName, userName, game);
            case "GUILD"     -> faction = new GuildFaction(playerName, userName, game);
            case "HARKONNEN" -> faction = new HarkonnenFaction(playerName, userName, game);
            case "IX"        -> faction = new IxFaction(playerName, userName, game);
            case "RICHESE"   -> faction = new RicheseFaction(playerName, userName, game);
            default -> throw new IllegalStateException("Unexpected value: " + factionName.toUpperCase());
        }

        game.addFaction(faction);

        Category gameCategory = discordGame.getGameCategory();
        discordGame.pushGame();

        gameCategory.createTextChannel(factionName.toLowerCase() + "-info")
                .addPermissionOverride(
                        player,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .queue();

        gameCategory.createTextChannel(factionName.toLowerCase() + "-chat")
                .addPermissionOverride(
                        player,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .queue();
    }

    public static void showGameOptions(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String stringBuilder = "The following options are selected:\n" +
                game.getGameOptions().stream().map(GameOption::name)
                        .collect(Collectors.joining("\n"));

        discordGame.sendMessage("mod-info", stringBuilder);
    }

    public static void addGameOption(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String gameOptionName = discordGame.required(addGameOption).getAsString();
        GameOption gameOption = GameOption.valueOf(gameOptionName);

        game.addGameOption(gameOption);
        discordGame.pushGame();
    }

    public static void removeGameOption(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String gameOptionName = discordGame.required(removeGameOption).getAsString();
        GameOption gameOption = GameOption.valueOf(gameOptionName);

        game.removeGameOption(gameOption);
        discordGame.pushGame();
    }

    public static void ixHandSelection(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String ixCardName = discordGame.required(ixCard).getAsString();
        Faction faction = game.getFaction("Ix");
        List<TreacheryCard> hand = game.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        TreacheryCard card = hand.stream().filter(treacheryCard -> treacheryCard.name().equals(ixCardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        LinkedList<TreacheryCard> ixRejects = new LinkedList<>();
        for (TreacheryCard treacheryCard : hand) {
            if (treacheryCard.equals(card)) continue;
            ixRejects.add(treacheryCard);
        }
        Collections.shuffle(ixRejects);
        for (TreacheryCard treacheryCard : ixRejects) {
            game.getTreacheryDeck().add(treacheryCard);
        }
        faction.getTreacheryHand().clear();
        faction.setHandLimit(4);
        faction.addTreacheryCard(card);

        discordGame.pushGame();
    }

    public static void selectTraitor(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String traitorName = discordGame.required(traitor).getAsString();
        Faction faction = game.getFaction(factionName);
        TraitorCard traitor = faction
                .getTraitorHand().stream().filter(
                        traitorCard -> traitorCard.name().toLowerCase()
                                .contains(traitorName.toLowerCase())
                ).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Traitor not found"));
        for (TraitorCard card : faction.getTraitorHand()) {
            if (!card.equals(traitor)) game.getTraitorDeck().add(card);
        }
        faction.getTraitorHand().clear();
        faction.addTraitorCard(traitor);

        Collections.shuffle(game.getTraitorDeck());

        discordGame.sendMessage(faction.getName().toLowerCase() + "-chat",
                MessageFormat.format(
                        "{0} is in debt to you.  I'm sure they'll find a way to pay you back...",
                        traitor.name()
                ));

        discordGame.pushGame();
    }

    public static StepStatus createDecks(Game game) throws IOException {
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

    public static StepStatus factionPositions(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Collections.shuffle(game.getFactions());

        discordGame.sendMessage("turn-summary", "__**Game Setup**__");

        ShowCommands.showBoard(discordGame, game);

        return StepStatus.CONTINUE;
    }

    public static StepStatus bgPredictionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.sendMessage("bg-chat", game.getFaction("BG").getPlayer() + " Please make your secret prediction.");

        return StepStatus.STOP;
    }

    public static StepStatus fremenForcesStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction fremen = game.getFaction("Fremen");
        discordGame.sendMessage("game-actions",
                MessageFormat.format(
                        "{0} Please provide the placement of your 10 starting {1} and {2} forces including sector. {3}",
                        fremen.getEmoji(),
                        Emojis.FREMEN_TROOP,
                        Emojis.FREMEN_FEDAYKIN,
                        fremen.getPlayer()
                )
        );

        return StepStatus.STOP;
    }

    public static StepStatus bgForceStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction bg = game.getFaction("BG");
        discordGame.sendMessage("game-actions",
                MessageFormat.format(
                        "{0} Please provide the placement of your starting {1} or {2}. {3}",
                        bg.getEmoji(),
                        Emojis.BG_ADVISOR,
                        Emojis.BG_FIGHTER,
                        bg.getPlayer()
                )
        );

        return StepStatus.STOP;
    }

    public static StepStatus assignCardsToIx(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        try {
            IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
            ixFaction.setHandLimit(6);
            for (Faction faction : game.getFactions())
                game.drawCard("treachery deck", ixFaction.getName());
            discordGame.sendMessage("mod-info", ":ix: has received :treachery: cards. Run /setup ix-hand-selection to select theirs then /setup advance");
            return StepStatus.STOP;
        } catch (IllegalArgumentException e) {
            discordGame.sendMessage("mod-info", ":ix: is not in the game. Skipping card selection and assigning :treachery: cards.");
            return StepStatus.CONTINUE;
        }
    }

    public static StepStatus treacheryCardsStep(Game game) {
        for (Faction faction : game.getFactions()) {
            if (faction.getTreacheryHand().size() == 0) {
                game.drawCard("treachery deck", faction.getName());
            }
        }
        try {
            Faction harkonnenFaction = game.getFaction("Harkonnen");
            game.drawCard("treachery deck", "Harkonnen");
        } catch (IllegalArgumentException e) {}

        return StepStatus.CONTINUE;
    }

    public static StepStatus leaderSkillCardsStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Collections.shuffle(game.getLeaderSkillDeck());
        for (Faction faction : game.getFactions()) {

            // Drawing two Leader Skill Cards for user to choose from
            game.drawCard("leader skills deck", faction.getName());
            game.drawCard("leader skills deck", faction.getName());

            String leaderSkillsMessage = "Please select your leader and their skill from the following two options:\n" +
                    MessageFormat.format(
                            "{0}{1}{0}\n",
                            Emojis.WEIRDING, faction.getLeaderSkillsHand().get(0).name()
                    ) +
                    MessageFormat.format(
                            "{0}{1}{0}\n",
                            Emojis.WEIRDING, faction.getLeaderSkillsHand().get(1).name()
                    ) +
                    faction.getPlayer();

            discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", leaderSkillsMessage);
        }

        return StepStatus.STOP;
    }

    public static StepStatus showLeaderSkillCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String channelName = "turn-summary";
        List<Map<String, String>> leaderSkills = new ArrayList<>();

        for (Faction faction : game.getFactions()) {
            Leader leader = faction.getLeaders().stream().filter(l -> l.skillCard() != null)
                    .findFirst().orElseThrow(() -> new InvalidGameStateException(
                            MessageFormat.format("Faction {0} do not have a skilled leader", faction.getName())
                    ));

            Map<String, String> leaderSkillInfo = Map.of(
                    "emoji", faction.getEmoji(),
                    "leader", leader.name(),
                    "skill", leader.skillCard().name()
            );
            leaderSkills.add(leaderSkillInfo);
        }

        discordGame.sendMessage(channelName, "__**Setup: Leader Skills**__");

        for (Map<String, String> leaderSkill : leaderSkills) {
            String message = MessageFormat.format(
                    "{0} - {1} is a {2}",
                    leaderSkill.get("emoji"),
                    leaderSkill.get("leader"),
                    leaderSkill.get("skill")
            );

            Optional<FileUpload> fileUpload = CardImages.getLeaderSkillImage(event.getGuild(), leaderSkill.get("skill"));

            if (fileUpload.isEmpty()) {
                discordGame.sendMessage(channelName, message);
            } else {
                discordGame.sendMessage(channelName, message, fileUpload.get());
            }
        }

        return StepStatus.CONTINUE;
    }

    public static StepStatus harkonnenTraitorsStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction = game.getFaction("Harkonnen");

        for (int j = 0; j < 4; j++) {
            game.drawCard("traitor deck", faction.getName());
        }

        long numHarkonnenTraitors = faction.getTraitorHand().stream()
                .map(TraitorCard::factionName)
                .filter(f -> f.equalsIgnoreCase("Harkonnen"))
                .count();

        if (numHarkonnenTraitors > 1) {
            // Harkonnen can mulligan their hand
            discordGame.sendMessage("mod-info", "Harkonnen can mulligan");
            discordGame.sendMessage("harkonnen-chat",faction.getName() + " Please decide if you will mulligan your Traitor cards.");
            return StepStatus.STOP;
        } else {
            discordGame.sendMessage("mod-info", "Harkonnen can not mulligan");
            return StepStatus.CONTINUE;
        }
    }

    public static void harkonnenMulligan(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (!game.hasFaction("Harkonnen"))
            return;

        Faction harkonnen = game.getFaction("Harkonnen");

        game.getTraitorDeck().addAll(harkonnen.getTraitorHand());
        harkonnen.getTraitorHand().clear();

        Collections.shuffle(game.getTraitorDeck());

        for (int j = 0; j < 4; j++) {
            game.drawCard("traitor deck", "Harkonnen");
        }

        discordGame.pushGame();
    }

    public static StepStatus traitorSelectionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        for (Faction faction : game.getFactions()) {
            if (!faction.getName().equals("BT") && faction.getTraitorHand().size() == 0) {
                for (int j = 0; j < 4; j++) {
                    game.drawCard("traitor deck", faction.getName());
                }
                if (!faction.getName().equalsIgnoreCase("Harkonnen")) {
                    discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", faction.getName() + " Please select your traitor.");
                }
            }
        }

        discordGame.sendMessage("turn-summary", "__**Setup: Traitors**__");

        return StepStatus.STOP;
    }

    public static StepStatus btDrawFaceDancersStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");

        discordGame.sendMessage("turn-summary", "Bene Tleilax have drawn their Face Dancers.");

        return StepStatus.CONTINUE;
    }

    public static StepStatus stormSelectionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction1 = game.getFactions().get(0);
        Faction faction2 = game.getFactions().get(game.getFactions().size() - 1);
        discordGame.sendMessage(faction1.getName().toLowerCase() + "-chat",
                faction1.getPlayer() + " Please submit your dial for initial storm position (0-20)."
        );
        discordGame.sendMessage(faction2.getName().toLowerCase() + "-chat",
                faction2.getPlayer() + " Please submit your dial for initial storm position (0-20).");
        game.setStormMovement(new Random().nextInt(6) + 1);
        discordGame.sendMessage("turn-summary", "Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");

        return StepStatus.STOP;
    }

    public static StepStatus startGameStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.advanceTurn();
        game.setSetupFinished(true);
        discordGame.sendMessage("mod-info", "The game has begun!");

        return StepStatus.STOP;
    }

    public static void factionLeaderSkill(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();
        String leaderSkillName = discordGame.required(factionLeaderSkill).getAsString();

        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getLeader(leaderName)
                .orElseThrow(() -> new IllegalArgumentException("Leader not found"));

        LeaderSkillCard leaderSkillCard = faction.getLeaderSkillsHand().stream()
                .filter(l -> l.name().equalsIgnoreCase(leaderSkillName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader Skill not found"));

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
        discordGame.pushGame();
    }
}
