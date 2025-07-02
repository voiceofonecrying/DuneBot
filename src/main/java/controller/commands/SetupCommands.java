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
import net.dv8tion.jda.api.entities.*;
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
import java.util.stream.IntStream;

import static controller.commands.CommandOptions.*;
import static model.Initializers.getCSVFile;

public class SetupCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("setup", "Commands related to game setup.").addSubcommands(
                        new SubcommandData("add-player-to-game-role", "Add a player to the game role").addOptions(user),
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
                        new SubcommandData("new-leader-skills", "Give the player two new leader skills to choose from.").addOptions(faction),
                        new SubcommandData("leader-skill", "Add leader skill to faction")
                                .addOptions(faction, CommandOptions.factionLeader, CommandOptions.factionLeaderSkill),
                        new SubcommandData("harkonnen-mulligan", "Mulligan Harkonnen traitor hand and advance"),
                        new SubcommandData("bg-prediction", "Set BG prediction").addOptions(faction, turn),
                        new SubcommandData("faction-board-position", "Set a board position for a faction, swap with the faction currently there").addOptions(faction, dotPosition),
                        new SubcommandData("remove-double-powered-treachery", "Remove Poison Blade and Shield Snooper from the deck.")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        switch (name) {
            case "add-player-to-game-role" -> addPlayerToGameRole(event, discordGame, game);
            case "faction" -> addFaction(event, discordGame, game);
            case "show-game-options" -> showGameOptions(game);
            case "add-game-option" -> addGameOption(discordGame, game);
            case "remove-game-option" -> removeGameOption(discordGame, game);
            case "ix-hand-selection" -> ixHandSelection(event, discordGame, game);
            case "traitor" -> selectTraitor(discordGame, game);
            case "advance" -> advance(event.getGuild(), discordGame, game);
            case "new-leader-skills" -> newLeaderSkills(discordGame, game);
            case "leader-skill" -> factionLeaderSkill(discordGame, game);
            case "harkonnen-mulligan" -> harkonnenMulligan(event, discordGame, game);
            case "bg-prediction" -> setPrediction(discordGame, game);
            case "faction-board-position" -> setFactionBoardPosition(discordGame, game);
            case "remove-double-powered-treachery" -> removeDoublePoweredBattleCards(event, discordGame, game);
        }
    }

    private static void setPrediction(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        BGFaction bg = game.getBGFaction();
        bg.setPredictionFactionName(discordGame.required(faction).getAsString());
        bg.setPredictionRound(discordGame.required(turn).getAsInt());
        discordGame.pushGame();
    }

    private static void setFactionBoardPosition(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = discordGame.required(faction).getAsString();
        int position = discordGame.required(dotPosition).getAsInt();
        if (position < 1 || position > 6)
            throw new IllegalArgumentException("dotPosition must be a value from 1 to 6.");
        game.setDotPosition(factionName, position);
        discordGame.pushGame();
    }

    public static void advance(Guild discordGuild, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (game.getFactions().size() != 6)
            throw new InvalidGameStateException("Set up all factions before running /setup advance.");
        if (game.getTurn() != 0 || game.isSetupFinished()) {
            return;
        }

        if (!game.isSetupStarted() && !game.isSetupFinished()) {
            game.setSetupStarted(true);
            createSetupSteps(game);
            showSetupSteps(game);
        }

        StepStatus stepStatus;

        do {
            SetupStep setupStep = game.getSetupSteps().removeFirst();
            stepStatus = runSetupStep(discordGuild, discordGame, game, setupStep);

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

        if (game.hasMoritaniFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.MORITANI_FORCE
            );
        }

        if (game.hasBGFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.FACTION_POSITIONS) + 1,
                    SetupStep.BG_PREDICTION
            );
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.BG_FORCE
            );
        }

        if (game.hasFremenFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.FREMEN_FORCES
            );
        }


        if (game.hasIxFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TREACHERY_CARDS),
                    SetupStep.IX_CARD_SELECTION
            );
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.STORM_SELECTION) + 1,
                    SetupStep.IX_HMS_PLACEMENT
            );
        }

        if (game.hasGameOption(GameOption.HARKONNEN_MULLIGAN) && game.hasHarkonnenFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS),
                    SetupStep.HARKONNEN_TRAITORS
            );
        }

        if (game.hasBTFaction()) {
            setupSteps.add(
                    setupSteps.indexOf(SetupStep.TRAITORS) + 1,
                    SetupStep.BT_FACE_DANCERS
            );
        }

        if (game.hasEcazFaction()) {
            if (game.hasHarkonnenFaction() && game.hasGameOption(GameOption.HARKONNEN_MULLIGAN)) {
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

    public static void showSetupSteps(Game game) {
        String steps = "The Game setup will perform the following steps:\n" +
                game.getSetupSteps().stream().map(SetupStep::name)
                        .collect(Collectors.joining("\n"));

        game.getModInfo().publish(steps);
    }

    public static StepStatus runSetupStep(Guild discordGuild, DiscordGame discordGame, Game game, SetupStep setupStep) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        discordGame.getModInfo().queueMessage("Starting step " + setupStep.name());

        StepStatus stepStatus = StepStatus.STOP;

        switch (setupStep) {
            case CREATE_DECKS -> stepStatus = createDecks(game);
            case FACTION_POSITIONS -> stepStatus = factionPositions(discordGame, game);
            case BG_PREDICTION -> stepStatus = bgPredictionStep(game);
            case FREMEN_FORCES -> stepStatus = fremenForcesStep(game);
            case BG_FORCE -> stepStatus = bgForceStep(game);
            case MORITANI_FORCE -> stepStatus = moritaniForceStep(game);
            case IX_CARD_SELECTION -> stepStatus = ixCardSelectionStep(game);
            case TREACHERY_CARDS -> stepStatus = treacheryCardsStep(game);
            case LEADER_SKILL_CARDS -> stepStatus = leaderSkillCardsStep(discordGame, game);
            case SHOW_LEADER_SKILLS -> stepStatus = showLeaderSkillCardsStep(discordGuild, discordGame, game);
            case ECAZ_LOYALTY -> stepStatus = ecazLoyaltyStep(game);
            case HARKONNEN_TRAITORS -> stepStatus = harkonnenTraitorsStep(game);
            case TRAITORS -> stepStatus = traitorSelectionStep(game);
            case BT_FACE_DANCERS -> stepStatus = btDrawFaceDancersStep(game);
            case STORM_SELECTION -> stepStatus = stormSelectionStep(game);
            case IX_HMS_PLACEMENT -> stepStatus = ixHMSPlacementStep(game);
            case START_GAME -> stepStatus = startGameStep(discordGame, game);
        }

        return stepStatus;
    }

    public static void removePlayerFromWaitingList(SlashCommandInteractionEvent event, DiscordGame discordGame, String playerName) {
        TextChannel waitingList = Objects.requireNonNull(event.getGuild()).getTextChannelsByName("waiting-list", true).getFirst();
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

    public static void addPlayerToGameRole(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) {
        List<Role> rolesWithName = Objects.requireNonNull(event.getGuild()).getRolesByName(game.getGameRole(), false);
        if (rolesWithName.isEmpty())
            throw new IllegalArgumentException("No Role with name " + game.getGameRole());
        if (rolesWithName.size() > 1)
            throw new IllegalArgumentException(rolesWithName.size() + " Roles with name " + game.getGameRole());
        Role gameRole = rolesWithName.getFirst();
        Member player = discordGame.required(user).getAsMember();
        event.getGuild().addRoleToMember(Objects.requireNonNull(player), Objects.requireNonNull(event.getJDA().getRoleById(gameRole.getId()))).queue();

        String playerName = discordGame.required(user).getAsUser().getAsMention();
        game.getTurnSummary().publish(playerName);
        if (game.getTurn() > 0)
            game.getWhispers().publish(playerName);
        removePlayerFromWaitingList(event, discordGame, playerName);
    }

    public static void removePlayerFromGameRole(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) {
        List<Role> rolesWithName = Objects.requireNonNull(event.getGuild()).getRolesByName(game.getGameRole(), false);
        if (rolesWithName.isEmpty())
            throw new IllegalArgumentException("No Role with name " + game.getGameRole());
        if (rolesWithName.size() > 1)
            throw new IllegalArgumentException(rolesWithName.size() + " Roles with name " + game.getGameRole());
        Role gameRole = rolesWithName.getFirst();
        Member player = discordGame.required(user).getAsMember();
        event.getGuild().removeRoleFromMember(Objects.requireNonNull(player), Objects.requireNonNull(event.getJDA().getRoleById(gameRole.getId()))).queue();
    }

    public static void addFaction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String factionName = discordGame.required(allFactions).getAsString();
        String playerName = discordGame.required(user).getAsUser().getAsMention();
        Member player = discordGame.required(user).getAsMember();

        if (player == null) throw new IllegalArgumentException("Not a valid user");

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

        String userName = player.getEffectiveName();
        switch (factionName.toUpperCase()) {
            case "ATREIDES" -> faction = new AtreidesFaction(playerName, userName);
            case "BG" -> faction = new BGFaction(playerName, userName);
            case "BT" -> faction = new BTFaction(playerName, userName);
            case "CHOAM" -> faction = new ChoamFaction(playerName, userName);
            case "EMPEROR" -> faction = new EmperorFaction(playerName, userName);
            case "FREMEN" -> faction = new FremenFaction(playerName, userName);
            case "GUILD" -> faction = new GuildFaction(playerName, userName);
            case "HARKONNEN" -> faction = new HarkonnenFaction(playerName, userName);
            case "IX" -> faction = new IxFaction(playerName, userName);
            case "RICHESE" -> faction = new RicheseFaction(playerName, userName);
            case "ECAZ" -> faction = new EcazFaction(playerName, userName);
            case "MORITANI" -> faction = new MoritaniFaction(playerName, userName);
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
        discordGame.createPrivateThread(channel, "chat", List.of(playerName, game.getModOrRoleMention()));
        discordGame.createPrivateThread(channel, "ledger", List.of(playerName));
        discordGame.getTurnSummary().addUser(game.getModRoleMention());
        discordGame.getTurnSummary().addUser(playerName);
        removePlayerFromWaitingList(event, discordGame, playerName);
    }

    public static void showGameOptions(Game game) {
        String options = "The following options are selected:\n" +
                game.getGameOptions().stream().map(GameOption::name)
                        .collect(Collectors.joining("\n"));

        game.getModInfo().publish(options);
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

    public static void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String ixCardName = discordGame.required(ixCard).getAsString();
        game.getIxFaction().startingCard(ixCardName);
        advance(event.getGuild(), discordGame, game);
    }

    public static void selectTraitor(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String traitorName = discordGame.required(traitor).getAsString();
        Faction faction = game.getFaction(factionName);
        faction.setLedger(discordGame.getFactionLedger(faction));
        faction.setChat(discordGame.getFactionChat(faction));
        faction.selectTraitor(traitorName);
        discordGame.pushGame();
    }

    public static StepStatus createDecks(Game game) throws IOException {
        if (game.hasGameOption(GameOption.SANDTROUT)) {
            game.getSpiceDeck().add(new SpiceCard("Sandtrout", -1, 0, null, null));
        }
        if (game.hasGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER)) {
            game.getSpiceDeck().removeFirstOccurrence(new SpiceCard("Shai-Hulud", 0, 0, null, null));
            game.getSpiceDeck().add(new SpiceCard("Great Maker", 0, 0, null, null));
        }
        Collections.shuffle(game.getSpiceDeck());
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS)) {
            CSVParser csvParser = getCSVFile("DiscoverySpiceCards.csv");
            Random random = new Random();
            int max = game.getSpiceDeck().size();
            if (game.hasGameOption(GameOption.DISCOVERY_CARDS_IN_TOP_HALF))
                max = max / 2;
            for (CSVRecord csvRecord : csvParser) {
                game.getSpiceDeck().add(random.nextInt(max++), new SpiceCard(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Integer.parseInt(csvRecord.get(2)), csvRecord.get(3), csvRecord.get(4)));
            }
            game.getSpiceDeck().add(random.nextInt(max), new SpiceCard("Great Maker", 0, 0, null, null));
        }

        if (game.hasGameOption(GameOption.CHEAP_HERO_TRAITOR)) {
            game.getTraitorDeck().add(new TraitorCard("Cheap Hero", "Any", 0));
        }
        Collections.shuffle(game.getTraitorDeck());

        if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
            }
        }
        if (game.hasGameOption(GameOption.EM_EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("EmExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
            }
        }
        Collections.shuffle(game.getTreacheryDeck());

        if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
            game.getModInfo().publish("Expansion 1 " + Emojis.TREACHERY + " cards have been added.\nUse /setup remove-double-powered-treachery if you need to remove Poison Blade and Shield Snooper. " + game.getModOrRoleMention());
            return StepStatus.STOP;
        } else
            return StepStatus.CONTINUE;
    }

    public static StepStatus factionPositions(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Collections.shuffle(game.getFactions());
        game.setTurnSummary(discordGame.getTurnSummary());
        game.setModInfo(discordGame.getModInfo());
        discordGame.getTurnSummary().queueMessage("__**Game Setup**__");

        ShowCommands.showBoard(discordGame, game);

        return StepStatus.CONTINUE;
    }

    public static StepStatus bgPredictionStep(Game game) {
        game.getBGFaction().presentPredictedFactionChoices();
        return StepStatus.STOP;
    }

    public static StepStatus fremenForcesStep(Game game) {
        FremenFaction fremen = game.getFremenFaction();
        game.getGameActions().publish(fremen.getEmoji() + " will place their starting 10 " + Emojis.FREMEN_TROOP + " and " + Emojis.FREMEN_FEDAYKIN);
        fremen.presentStartingForcesChoices();
        return StepStatus.STOP;
    }

    public static StepStatus bgForceStep(Game game) {
        game.getBGFaction().presentStartingForcesChoices();
        return StepStatus.STOP;
    }

    public static StepStatus moritaniForceStep(Game game) {
        MoritaniFaction moritani = game.getMoritaniFaction();
        game.getGameActions().publish(moritani.getEmoji() + " will place their starting 6 " + Emojis.MORITANI_TROOP);
        moritani.presentStartingForcesChoices();
        return StepStatus.STOP;
    }

    public static StepStatus ixCardSelectionStep(Game game) throws InvalidGameStateException {
        IxFaction ixFaction;
        try {
            ixFaction = game.getIxFaction();
        } catch (IllegalArgumentException e) {
            game.getModInfo().publish(Emojis.IX + " is not in the game. Skipping card selection and assigning " + Emojis.TREACHERY + " cards.");
            return StepStatus.CONTINUE;
        }
        ixFaction.presentStartingCardsListAndChoices();
        return StepStatus.STOP;
    }

    public static StepStatus treacheryCardsStep(Game game) {
        for (Faction faction : game.getFactions()) {
            if (faction.getTreacheryHand().isEmpty()) {
                game.drawTreacheryCard(faction.getName(), true, false);
            }
        }
        try {
            game.drawTreacheryCard("Harkonnen", true, false);
        } catch (IllegalArgumentException e) {
            // Harkonnen is not in the game
        }

        return StepStatus.CONTINUE;
    }

    public static void factionLeaderSkillsChoice(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException {
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

        discordGame.getFactionChat(faction).queueMessage(message);
    }

    public static void newLeaderSkills(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        Collections.shuffle(game.getLeaderSkillDeck());
        factionLeaderSkillsChoice(discordGame, game, game.getFaction(factionName));
        discordGame.pushGame();
    }

    public static StepStatus leaderSkillCardsStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Collections.shuffle(game.getLeaderSkillDeck());
        for (Faction faction : game.getFactions())
            factionLeaderSkillsChoice(discordGame, game, faction);
        return StepStatus.STOP;
    }

    public static StepStatus showLeaderSkillCardsStep(Guild discordGuild, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        TurnSummary turnSummary = discordGame.getTurnSummary();
        List<Map<String, String>> leaderSkills = new ArrayList<>();

        for (Faction faction : game.getFactions()) {
            Leader leader = faction.getLeaders().stream().filter(l -> l.getSkillCard() != null)
                    .findFirst().orElseThrow(() -> new InvalidGameStateException(
                            MessageFormat.format("Faction {0} do not have a skilled leader", faction.getName())
                    ));

            Map<String, String> leaderSkillInfo = Map.of(
                    "emoji", faction.getEmoji(),
                    "leader", leader.getName(),
                    "skill", leader.getSkillCard().name()
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

            Optional<FileUpload> fileUpload = CardImages.getLeaderSkillImage(discordGuild, leaderSkill.get("skill"));

            if (fileUpload.isEmpty()) {
                turnSummary.queueMessage(message);
            } else {
                turnSummary.queueMessage(message, fileUpload.get());
            }
        }

        return StepStatus.CONTINUE;
    }

    public static StepStatus harkonnenTraitorsStep(Game game) {
        Faction faction = game.getHarkonnenFaction();
        IntStream.range(0, 4).forEach(j -> game.drawCard("traitor deck", faction.getName()));
        long numHarkonnenTraitors = faction.getTraitorHand().stream().filter(TraitorCard::isHarkonnenTraitor).count();
        if (numHarkonnenTraitors > 1) {
            // Harkonnen can mulligan their hand
            game.getModInfo().publish("Harkonnen can mulligan.");
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("harkonnen-mulligan-yes", "Yes, draw 4 new traitors"));
            choices.add(new DuneChoice("harkonnen-mulligan-no", "No, keep my current traitors"));
            faction.getChat().publish("You have drawn " + numHarkonnenTraitors + " of your own leaders as Traitors.\nDo you want to mulligan your Traitor cards and draw a new set? " + faction.getPlayer(), choices);
            return StepStatus.STOP;
        } else {
            game.getModInfo().publish("Harkonnen cannot mulligan.");
            return StepStatus.CONTINUE;
        }
    }

    private static StepStatus ecazLoyaltyStep(Game game) {
        EcazFaction ecaz = game.getEcazFaction();
        List<Leader> leaders = ecaz.getLeaders();
        Collections.shuffle(leaders);
        ecaz.setLoyalLeader(leaders.getFirst());
        game.getTraitorDeck().removeIf(traitorCard -> traitorCard.getName().equalsIgnoreCase(ecaz.getLoyalLeader().getName()));
        game.getTurnSummary().publish(Emojis.ECAZ + " have drawn " + ecaz.getLoyalLeader().getName() + " as their loyal leader.");
        return StepStatus.CONTINUE;
    }

    public static void harkonnenMulligan(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        game.getHarkonnenFaction().mulliganTraitorHand();
        SetupCommands.advance(event.getGuild(), discordGame, game);
    }

    public static StepStatus traitorSelectionStep(Game game) {
        for (Faction faction : game.getFactions()) {
            if (!(faction instanceof BTFaction) && faction.getTraitorHand().isEmpty()) {
                IntStream.range(0, 4).forEach(j -> game.drawCard("traitor deck", faction.getName()));
                if (!(faction instanceof HarkonnenFaction))
                    faction.presentTraitorSelection();
            }
        }
        game.getTurnSummary().publish("__**Setup: Traitors**__");

        return StepStatus.STOP;
    }

    public static StepStatus btDrawFaceDancersStep(Game game) throws InvalidGameStateException {
        game.getBTFaction().drawFaceDancers();
        return StepStatus.CONTINUE;
    }

    public static StepStatus stormSelectionStep(Game game) {
        Faction faction1 = game.getFactions().getFirst();
        Faction faction2 = game.getFactions().getLast();
        List<DuneChoice> choices = IntStream.range(0, 21).mapToObj(i -> new DuneChoice("faction-storm-dial-" + i, String.valueOf(i))).toList();
        faction1.getChat().publish("Please submit your dial for initial storm position (0-20). " + faction1.getPlayer(), choices);
        faction2.getChat().publish("Please submit your dial for initial storm position (0-20). " + faction2.getPlayer(), choices);
        game.setStormMovement(new Random().nextInt(6) + 1);
        game.getTurnSummary().publish("Turn Marker is set to turn 1. The game is beginning! Initial storm is being calculated...");

        return StepStatus.STOP;
    }

    public static StepStatus ixHMSPlacementStep(Game game) {
        IxFaction ix = game.getIxFaction();
        game.getGameActions().publish(ix.getEmoji() + " will place the HMS.");
        ix.presentHMSPlacementChoices();
        game.getModInfo().publish("If " + Emojis.IX + " is unable to place the HMS, you can use /ix place-hms to place it and then /setup advance.");

        return StepStatus.STOP;
    }

    public static StepStatus startGameStep(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.advanceTurn();
        game.setTurnSummary(discordGame.getTurnSummary());
        game.getTurnSummary().publish(game.getModOrRoleMention());
        game.getTurnSummary().publish(game.getGameRoleMention());
        game.setSetupFinished(true);
        game.getWhispers().publish(game.getGameRoleMention());
        game.getWhispers().publish(game.getModOrRoleMention());
        game.setWhispersTagged(true);
        discordGame.getModInfo().queueMessage("The game has begun!");

        return StepStatus.STOP;
    }

    public static void factionLeaderSkill(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = discordGame.required(faction).getAsString();
        String leaderName = discordGame.required(factionLeader).getAsString();
        String leaderSkillName = discordGame.required(factionLeaderSkill).getAsString();
        game.getFaction(factionName).assignSkillToLeader(leaderName, leaderSkillName);
        discordGame.pushGame();
    }

    public static void removeDoublePoweredBattleCards(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        try {
            game.removeTreacheryCard("Poison Blade");
        } catch (Exception e) {
            game.getModInfo().publish("Treachery deck does not have Poison Blade.");
        }
        try {
            game.removeTreacheryCard("Shield Snooper");
        } catch (Exception e) {
            game.getModInfo().publish("Treachery deck does not have Shield Snooper.");
        }
        SetupCommands.advance(event.getGuild(), discordGame, game);
    }
}
