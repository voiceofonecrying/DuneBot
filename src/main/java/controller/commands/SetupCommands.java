package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.SetupStep;
import enums.StepStatus;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import templates.ChannelPermissions;
import utils.CardImages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;
import static model.Initializers.getCSVFile;

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
                        new SubcommandData("harkonnen-mulligan", "Mulligan Harkonnen traitor hand"),
                        new SubcommandData("bg-prediction", "Set BG prediction")
                                .addOptions(faction, turn)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "faction" -> addFaction(event, discordGame, game);
            case "show-game-options" -> showGameOptions(discordGame, game);
            case "add-game-option" -> addGameOption(discordGame, game);
            case "remove-game-option" -> removeGameOption(discordGame, game);
            case "ix-hand-selection" -> ixHandSelection(discordGame, game);
            case "traitor" -> selectTraitor(discordGame, game);
            case "advance" -> advance(event, discordGame, game);
            case "leader-skill" -> factionLeaderSkill(discordGame, game);
            case "harkonnen-mulligan" -> harkonnenMulligan(discordGame, game);
            case "bg-prediction" -> setPrediction(discordGame, game);
        }
    }

    private static void setPrediction(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        BGFaction bg = (BGFaction) game.getFaction("BG");
        bg.setPredictionRound(discordGame.required(turn).getAsInt());
        bg.setPredictionFactionName(discordGame.required(faction).getAsString());
        discordGame.pushGame();
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

        if (game.hasFaction("Moritani")) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.MORITANI_FORCE
            );
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
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.STORM_SELECTION) + 1,
                    SetupStep.IX_HMS_PLACEMENT
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

        if (game.hasFaction("Ecaz")) {
            if (game.hasFaction("Harkonnen") && game.hasGameOption(GameOption.HARKONNEN_MULLIGAN)) {
                setupSteps.add(
                        setupSteps.indexOf(SetupStep.HARKONNEN_TRAITORS),
                        SetupStep.ECAZ_LOYALTY
                );

            } else {
                setupSteps.add(
                        setupSteps.indexOf(SetupStep.TRAITORS),
                        SetupStep.ECAZ_LOYALTY
                );
            }
        }


        game.setSetupSteps(setupSteps);
    }

    public static void showSetupSteps(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String stringBuilder = "The Game setup will perform the following steps:\n" +
                game.getSetupSteps().stream().map(SetupStep::name)
                        .collect(Collectors.joining("\n"));

        discordGame.getModInfo().queueMessage(stringBuilder);
    }

    public static StepStatus runSetupStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game, SetupStep setupStep) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        discordGame.getModInfo().queueMessage("Starting step " + setupStep.name());

        StepStatus stepStatus = StepStatus.STOP;

        switch (setupStep) {
            case CREATE_DECKS -> stepStatus = createDecks(game);
            case FACTION_POSITIONS -> stepStatus = factionPositions(discordGame, game);
            case BG_PREDICTION -> stepStatus = bgPredictionStep(discordGame, game);
            case FREMEN_FORCES -> stepStatus = fremenForcesStep(discordGame, game);
            case BG_FORCE -> stepStatus = bgForceStep(discordGame, game);
            case MORITANI_FORCE -> stepStatus = moritaniForceStep(discordGame, game);
            case IX_CARD_SELECTION -> stepStatus = ixCardSelectionStep(discordGame, game);
            case TREACHERY_CARDS -> stepStatus = treacheryCardsStep(game);
            case LEADER_SKILL_CARDS -> stepStatus = leaderSkillCardsStep(discordGame, game);
            case SHOW_LEADER_SKILLS -> stepStatus = showLeaderSkillCardsStep(event, discordGame, game);
            case ECAZ_LOYALTY -> stepStatus = ecazLoyaltyStep(discordGame, game);
            case HARKONNEN_TRAITORS -> stepStatus = harkonnenTraitorsStep(discordGame, game);
            case TRAITORS -> stepStatus = traitorSelectionStep(discordGame, game);
            case BT_FACE_DANCERS -> stepStatus = btDrawFaceDancersStep(discordGame, game);
            case STORM_SELECTION -> stepStatus = stormSelectionStep(discordGame, game);
            case IX_HMS_PLACEMENT -> stepStatus = ixHMSPlacementStep(discordGame, game);
            case START_GAME -> stepStatus = startGameStep(discordGame, game);
        }

        return stepStatus;
    }

    public static void addFaction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String factionName = discordGame.required(allFactions).getAsString();
        String playerName = discordGame.required(user).getAsUser().getAsMention();
        Member player = discordGame.required(user).getAsMember();

        if (player == null) throw new IllegalArgumentException("Not a valid user");

        String userName = player.getNickname();

        if (game.getTurn() != 0) {
            discordGame.getModInfo().queueMessage("The game has already started, you can't add more factions!");
            return;
        }
        if (game.getFactions().size() >= 6) {
            discordGame.getModInfo().queueMessage("This game is already full!");
            return;
        }
        if (game.hasFaction(factionName)) {
            discordGame.getModInfo().queueMessage("This faction has already been taken!");
            return;
        }
        Faction faction;

        switch (factionName.toUpperCase()) {
            case "ATREIDES" -> faction = new AtreidesFaction(playerName, userName, game);
            case "BG" -> faction = new BGFaction(playerName, userName, game);
            case "BT" -> faction = new BTFaction(playerName, userName, game);
            case "CHOAM" -> faction = new ChoamFaction(playerName, userName, game);
            case "EMPEROR" -> faction = new EmperorFaction(playerName, userName, game);
            case "FREMEN" -> faction = new FremenFaction(playerName, userName, game);
            case "GUILD" -> faction = new GuildFaction(playerName, userName, game);
            case "HARKONNEN" -> faction = new HarkonnenFaction(playerName, userName, game);
            case "IX" -> faction = new IxFaction(playerName, userName, game);
            case "RICHESE" -> faction = new RicheseFaction(playerName, userName, game);
            case "ECAZ" -> faction = new EcazFaction(playerName, userName, game);
            case "MORITANI" -> faction = new MoritaniFaction(playerName, userName, game);
            default -> throw new IllegalStateException("Unexpected value: " + factionName.toUpperCase());
        }

        game.addFaction(faction);

        Category gameCategory = discordGame.getGameCategory();
        discordGame.pushGame();

        TextChannel channel = gameCategory.createTextChannel(factionName.toLowerCase() + "-info")
                .addPermissionOverride(
                        player,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .complete();
        discordGame.createPrivateThread(channel, "notes", List.of(playerName));
        discordGame.createPrivateThread(channel, "chat", List.of(playerName, game.getMod()));
        discordGame.createPrivateThread(channel, "ledger", List.of(playerName));
        discordGame.getTurnSummary().addUser(playerName);
        discordGame.getTurnSummary().addUser(game.getMod());

        TextChannel waitingList = Objects.requireNonNull(event.getGuild()).getTextChannelsByName("waiting-list", true).get(0);
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(waitingList).complete();
        List<Message> messagesToDelete = new ArrayList<>();
        for (Message m : messageHistory.getRetrievedHistory()) {
            for (String playerNameInWL : CommandManager.findPlayerTags(m.getContentRaw())) {
                if (playerNameInWL.equalsIgnoreCase(playerName)) {
                    messagesToDelete.add(m);
                }
            }
        }
        for (Message mtd : messagesToDelete) {
            try {
                discordGame.queueDeleteMessage(mtd);
            } catch (Exception e) {
                // Message was already deleted
            }
        }
    }

    public static void showGameOptions(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String stringBuilder = "The following options are selected:\n" +
                game.getGameOptions().stream().map(GameOption::name)
                        .collect(Collectors.joining("\n"));

        discordGame.getModInfo().queueMessage(stringBuilder);
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
        faction.setLedger(discordGame.getFactionLedger(faction));
        faction.selectTraitor(traitorName);
        discordGame.pushGame();
    }

    public static StepStatus createDecks(Game game) throws IOException {
        if (game.hasGameOption(GameOption.SANDTROUT)) {
            game.getSpiceDeck().add(new SpiceCard("Sandtrout", -1, 0, null, null));
        }

        if (game.hasGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER)) {
            game.getSpiceDeck().add(new SpiceCard("Great Maker", 0, 0, null, null));
        }

        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS)) {
            CSVParser csvParser = getCSVFile("DiscoverySpiceCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getSpiceDeck().add(new SpiceCard(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Integer.parseInt(csvRecord.get(2)), csvRecord.get(3), csvRecord.get(4)));
            }
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

        if (game.hasGameOption(GameOption.EM_EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("EmExpansionTreacheryCards.csv");
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
        game.setTurnSummary(discordGame.getTurnSummary());
        discordGame.getTurnSummary().queueMessage("__**Game Setup**__");

        ShowCommands.showBoard(discordGame, game);

        return StepStatus.CONTINUE;
    }

    public static StepStatus bgPredictionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getBGChat().queueMessage(game.getFaction("BG").getPlayer() + " Please make your secret prediction.");
        return StepStatus.STOP;
    }

    public static StepStatus fremenForcesStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction fremen = game.getFaction("Fremen");
        discordGame.queueMessage("game-actions",
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
        discordGame.queueMessage("game-actions",
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

    public static StepStatus moritaniForceStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction moritani = game.getFaction("Moritani");
        discordGame.queueMessage("game-actions",
                MessageFormat.format(
                        "{0} Please provide the placement of your starting {1}. {2}",
                        moritani.getEmoji(),
                        Emojis.MORITANI_TROOP,
                        moritani.getPlayer()
                )
        );

        return StepStatus.STOP;
    }

    public static StepStatus ixCardSelectionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        try {
            IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
            ixFaction.setHandLimit(13); // Only needs 7 with Harkonnen in a 6p game, but allowing here for a 12p game with Hark.
            for (Faction ignored : game.getFactions())
                game.drawCard("treachery deck", ixFaction.getName());
            if (game.hasFaction("Harkonnen") && !game.hasGameOption(GameOption.IX_ONLY_1_CARD_PER_FACTION)) {
                game.drawCard("treachery deck", ixFaction.getName());
            }
            discordGame.getModInfo().queueMessage(Emojis.IX + " has received " + Emojis.TREACHERY + " cards.\nIx player can use buttons or mod can use /setup ix-hand-selection to select theirs. Then /setup advance.");
            IxCommands.initialCard(discordGame, game);
            return StepStatus.STOP;
        } catch (IllegalArgumentException e) {
            discordGame.getModInfo().queueMessage(Emojis.IX + " is not in the game. Skipping card selection and assigning :treachery: cards.");
            return StepStatus.CONTINUE;
        }
    }

    public static StepStatus treacheryCardsStep(Game game) {
        for (Faction faction : game.getFactions()) {
            if (faction.getTreacheryHand().isEmpty()) {
                game.drawCard("treachery deck", faction.getName());
            }
        }
        try {
            game.drawCard("treachery deck", "Harkonnen");
        } catch (IllegalArgumentException e) {
            // Harkonnen is not in the game
        }

        return StepStatus.CONTINUE;
    }

    public static StepStatus leaderSkillCardsStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Collections.shuffle(game.getLeaderSkillDeck());
        for (Faction faction : game.getFactions()) {

            // Drawing two Leader Skill Cards for user to choose from
            game.drawCard("leader skills deck", faction.getName());
            game.drawCard("leader skills deck", faction.getName());

            MessageCreateBuilder message = new MessageCreateBuilder();
            message.setContent(faction.getPlayer());
            message.addContent(" please select your leader and their skill from the following two options:\n");
            faction.getLeaderSkillsHand().forEach(leaderSkillCard -> message.addContent("* " + leaderSkillCard.name() + "\n"));

            faction.getLeaderSkillsHand().stream()
                    .map(leaderSkillCard -> CardImages.getLeaderSkillImage(
                            discordGame.getEvent().getGuild(), leaderSkillCard.name())
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(message::addFiles);

            discordGame.getFactionChat(faction.getName()).queueMessage(message);
        }

        return StepStatus.STOP;
    }

    public static StepStatus showLeaderSkillCardsStep(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
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
        turnSummary.queueMessage("__**Setup: Leader Skills**__");

        for (Map<String, String> leaderSkill : leaderSkills) {
            String message = MessageFormat.format(
                    "{0} - {1} is a {2}",
                    leaderSkill.get("emoji"),
                    leaderSkill.get("leader"),
                    leaderSkill.get("skill")
            );

            Optional<FileUpload> fileUpload = CardImages.getLeaderSkillImage(event.getGuild(), leaderSkill.get("skill"));

            if (fileUpload.isEmpty()) {
                turnSummary.queueMessage(message);
            } else {
                turnSummary.queueMessage(message, fileUpload.get());
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
            discordGame.getModInfo().queueMessage("Harkonnen can mulligan");
            discordGame.getHarkonnenChat().queueMessage(faction.getPlayer() + " please decide if you will mulligan your Traitor cards.");
            return StepStatus.STOP;
        } else {
            discordGame.getModInfo().queueMessage("Harkonnen cannot mulligan");
            return StepStatus.CONTINUE;
        }
    }

    private static StepStatus ecazLoyaltyStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        EcazFaction ecaz = (EcazFaction) game.getFaction("Ecaz");
        List<Leader> leaders = ecaz.getLeaders();
        Collections.shuffle(leaders);
        ecaz.setLoyalLeader(leaders.get(0));
        game.getTraitorDeck().removeIf(traitorCard -> traitorCard.name().equalsIgnoreCase(ecaz.getLoyalLeader().name()));
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " have drawn " + ecaz.getLoyalLeader().name() + " as their loyal leader.");
        return StepStatus.CONTINUE;
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
            if (!faction.getName().equals("BT") && faction.getTraitorHand().isEmpty()) {
                for (int j = 0; j < 4; j++) {
                    game.drawCard("traitor deck", faction.getName());
                }
                if (!faction.getName().equalsIgnoreCase("Harkonnen")) {
                    discordGame.getFactionChat(faction.getName()).queueMessage(faction.getPlayer() + " please select your traitor.");
                }
            }
        }
        discordGame.getTurnSummary().queueMessage("__**Setup: Traitors**__");

        return StepStatus.STOP;
    }

    public static StepStatus btDrawFaceDancersStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");
        discordGame.getTurnSummary().queueMessage("Bene Tleilax have drawn their Face Dancers.");

        return StepStatus.CONTINUE;
    }

    public static StepStatus stormSelectionStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction1 = game.getFactions().get(0);
        Faction faction2 = game.getFactions().get(game.getFactions().size() - 1);
        discordGame.getFactionChat(faction1.getName()).queueMessage(faction1.getPlayer() + " Please submit your dial for initial storm position (0-20).");
        discordGame.getFactionChat(faction2.getName()).queueMessage(faction2.getPlayer() + " Please submit your dial for initial storm position (0-20).");
        game.setStormMovement(new Random().nextInt(6) + 1);
        discordGame.getTurnSummary().queueMessage("Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");

        return StepStatus.STOP;
    }

    public static StepStatus ixHMSPlacementStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.getModInfo().queueMessage("Use /placehms to set the initial placement of the HMS then /setup advance.");

        return StepStatus.STOP;
    }

    public static StepStatus startGameStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.advanceTurn();
        game.setSetupFinished(true);
        discordGame.getModInfo().queueMessage("The game has begun!");

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
        discordGame.getFactionChat(faction.getName()).queueMessage(MessageFormat.format("After years of training, {0} has become a {1}! ",
                        updatedLeader.name(), leaderSkillCard.name()
                )
        );
        discordGame.pushGame();
    }
}
