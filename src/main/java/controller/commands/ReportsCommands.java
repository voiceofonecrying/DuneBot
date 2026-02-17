package controller.commands;

import caches.EmojiCache;
import com.google.gson.*;
import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import helpers.GameResult;
import model.Bidding;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.MutableTriple;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import static controller.commands.CommandOptions.*;

public class ReportsCommands {
    private static final OptionData optionalAllFactions = new OptionData(allFactions.getType(), allFactions.getName(), "Limit to games with this faction", false);

    public static List<CommandData> getCommands() {
        if (optionalAllFactions.getChoices().isEmpty())
            optionalAllFactions.addChoices(allFactions.getChoices());
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("reports", "Commands for statistics about Dune: Play by Discord games.").addSubcommands(
                new SubcommandData("games-per-player", "Show the games each player is in listing those on waiting list first.").addOptions(months),
                new SubcommandData("active-games", "Show active games with turn, phase, and subphase.").addOptions(showFactions, showUserNames, optionalAllFactions),
                new SubcommandData("active-game-roles", "Show player and mod roles in active games."),
                new SubcommandData("inactive-game-roles", "Show player and mod roles that are not in use."),
                new SubcommandData("player-record", "Show the overall per faction record for the player").addOptions(user),
                new SubcommandData("played-all-original-six", "Who has played all original six factions?"),
                new SubcommandData("played-all-expansion", "Who has played all six expansion factions?"),
                new SubcommandData("played-o6-multiple-times", "Who has played all O6 factions multiple times?"),
                new SubcommandData("played-expansion-multiple-times", "Who has played all six expansion factions multiple times?"),
                new SubcommandData("played-all-twelve", "Who has played all twelve factions?"),
                new SubcommandData("won-as-all-original-six", "Who has won with all original six factions?"),
                new SubcommandData("won-as-all-expansion", "Who has won with all six expansion factions?"),
                new SubcommandData("solo-victories", "Factions and players with solo victories"),
                new SubcommandData("high-faction-plays", "Which player-faction combos have occurred the most?"),
                new SubcommandData("high-faction-wins", "Which player-faction combos have won the most?"),
                new SubcommandData("faction-masters", "Top players for a given faction").addOptions(allFactions),
                new SubcommandData("most-player-alliance-wins", "Which players have won as allies the most?"),
                new SubcommandData("longest-games", "The 10 longest games and the tortured mod."),
                new SubcommandData("fastest-games", "The 20 fastest games by days per turn.")
        ));

        return commandData;
    }

    public static String runCommand(SlashCommandInteractionEvent event, List<Member> members) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        String responseMessage = "";
        GRList grList = gatherGameResults(event.getGuild()).grList;
        switch (name) {
            case "games-per-player" -> responseMessage = gamesPerPlayer(event, members);
            case "active-games" -> responseMessage = activeGames(event);
            case "active-game-roles" -> responseMessage = activeGameRoles(event);
            case "inactive-game-roles" -> responseMessage = inactiveGameRoles(event);
            case "player-record" -> responseMessage = playerRecord(event);
            case "played-all-original-six" -> responseMessage = playedAllOriginalSix(event.getGuild(), grList, members);
            case "played-all-expansion" -> responseMessage = playedAllExpansion(event.getGuild(), grList, members);
            case "played-original-six-multiple-times" -> responseMessage = playedAllOriginalSixMultipleTimes(event.getGuild(), grList, members);
            case "played-expansion-six-multiple-times" -> responseMessage = playedAllExpansionMultipleTimes(event.getGuild(), grList, members);
            case "played-all-twelve" -> responseMessage = playedAllTwelve(event.getGuild(), grList, members);
            case "won-as-all-original-six" -> responseMessage = wonAsAllOriginalSix(event.getGuild(), grList, members);
            case "won-as-all-expansion" -> responseMessage = wonAsAllExpansion(event.getGuild(), grList, members);
            case "solo-victories" -> responseMessage = soloVictories(event.getGuild(), grList, members);
            case "high-faction-plays" -> responseMessage = highFactionGames(event.getGuild(), grList, members, false);
            case "high-faction-wins" -> responseMessage = highFactionGames(event.getGuild(), grList, members, true);
            case "faction-masters" -> responseMessage = factionMasters(event, event.getGuild(), grList, members);
            case "most-player-alliance-wins" -> responseMessage = writePlayerAllyPerformance(event.getGuild(), grList, members);
            case "longest-games" -> responseMessage = longestGames(event.getGuild(), grList, members);
            case "fastest-games" -> responseMessage = fastestGames(event.getGuild(), grList, members);
        }
        return responseMessage;
    }

    private static class PlayerGame {
        String player;
        List<String> games;
        int numGames;
        boolean onWaitingList;
        boolean recentlyFinished;
    }

    public static List<String> findPlayerTags(String message) {
        List<String> players = new ArrayList<>();
        int startChar = message.indexOf("<@");
        while (startChar != -1) {
            int endChar = message.substring(startChar).indexOf(">");
            if (endChar != -1) {
                players.add(message.substring(startChar, startChar + endChar + 1));
            }
            message = message.substring(startChar + endChar + 1);
            startChar = message.indexOf("<@");
        }
        return players;
    }

    private static void addWaitingListPlayers(HashMap<String, List<String>> playerGamesMap, Category category) {
        Optional<TextChannel> optChannel = category.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase("waiting-list"))
                .findFirst();
        if (optChannel.isPresent()) {
            TextChannel waitingList = optChannel.get();
            MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(waitingList).complete();
            List<Message> messages = messageHistory.getRetrievedHistory();
            for (Message m : messages) {
                int startChar = m.getContentRaw().indexOf("User:");
                if (startChar == -1) continue;
                for (String player : findPlayerTags(m.getContentRaw().substring(startChar))) {
                    List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                    if (games.isEmpty()) {
                        games.add("waiting-list");
                    }
                }
            }
        }
    }

    private static String getTaggedPlayer(String playerName, List<Member> members) {
        return members.stream().filter(member -> playerName.equals("@" + member.getUser().getName())).findFirst().map(member -> member.getUser().getAsMention()).orElse(playerName);
    }

    private static String getPlayerString(Guild guild, String playerName, List<Member> members) {
        boolean leftServer = true;
        if (playerName.equals("@Moderators"))
            return "Moderators";
        for (Member member1 : members) {
            if (member1.getGuild() == guild && playerName.equals("@" + member1.getUser().getName())) {
                leftServer = false;
                if (member1.getRoles().stream().noneMatch(r -> r.getName().equals("NoTagInStats")))
                    return member1.getUser().getAsMention();
            }
        }
        return playerName.replace("_", "\\_").replace("*", "\\*").replace("@", "") + (leftServer ? " (Left server)" : "");
    }

    private static void addRecentlyFinishedPlayers(Guild guild, List<Member> members, GameResults gameResults, HashMap<String, List<String>> playerGamesMap, int monthsAgo) {
        HashSet<String> filteredPlayers = getAllPlayers(gameResults.grList, monthsAgo);
        for (String player : filteredPlayers) {
            String playerTag = getPlayerString(guild, player, members);
            List<String> games = playerGamesMap.computeIfAbsent(playerTag, k -> new ArrayList<>());
            if (games.isEmpty()) {
                games.add("recently-finished");
            }
        }
    }

    private static void addGamePlayers(HashMap<String, List<String>> playerGamesMap, Category category, String categoryName) {
        try {
            DiscordGame discordGame = new DiscordGame(category, false);
            for (Faction faction : discordGame.getGame().getFactions()) {
                String player = faction.getPlayer();
                List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                games.add(categoryName);
            }
        } catch (Exception e) {
//            System.out.println(category.getName() + " is not a Dune game.");
            // category is not a Dune game
        }
    }

    private static String playerMessage(PlayerGame playerGame) {
        String response = "    " + playerGame.numGames + " - " + playerGame.player;
        if (playerGame.numGames != 0) {
            List<String> printNames = new ArrayList<>();
            for (String categoryName : playerGame.games) {
                String printName = categoryName.substring(0, Math.min(6, categoryName.length()));
                if (categoryName.startsWith("D") || categoryName.startsWith("PBD")) {
                    try {
                        printName = "D" + new Scanner(categoryName).useDelimiter("\\D+").nextInt();
                    } catch (Exception e) {
                        // category does not follow Dune: Play by Discord game naming and numbering patterns
                    }
                }
                printNames.add(printName);
            }
            response += " (" + String.join(", ", printNames) + ")";
        }
        return response + "\n";
    }

    private static String playerGamesMessage(List<PlayerGame> playerGames, String header) {
        StringBuilder message = new StringBuilder();
        if (!playerGames.isEmpty()) {
            Comparator<PlayerGame> numGamesComparator = Comparator.comparingInt(playerGame -> playerGame.numGames);
            playerGames.sort(numGamesComparator);
            message.append(header);
            for (PlayerGame playerGame : playerGames) {
                message.append(playerMessage(playerGame));
            }
        }
        return message.toString();
    }

    public static String gamesPerPlayer(SlashCommandInteractionEvent event, List<Member> members) {
        OptionMapping optionMapping = event.getOption(months.getName());
        int monthsAgo = (optionMapping != null ? optionMapping.getAsInt() : 0);
        return gamesPerPlayer(event.getGuild(), members, monthsAgo);
    }

    public static String gamesPerPlayer(Guild guild, List<Member> members, int monthsAgo) {
        String message = "**Number of games players are in**\n";
        HashMap<String, List<String>> playerGamesMap = new HashMap<>();
        List<Category> categories = Objects.requireNonNull(guild).getCategories();
        for (Category category : categories) {
            String categoryName = category.getName();
            if (category.getId().equals("991769571231023214") || category.getId().equals("1142814716394213376") || categoryName.contains("Staging Area"))
                addWaitingListPlayers(playerGamesMap, category);
            else if (category.getId().equals("1148142189931679765") || category.getId().equals("1164949164313022484") || categoryName.contains("Dune Statistics"))
                addRecentlyFinishedPlayers(guild, members, gatherGameResults(guild), playerGamesMap, monthsAgo);
            else
                addGamePlayers(playerGamesMap, category, categoryName);
        }
        List<PlayerGame> waitingPlayerGames = new ArrayList<>();
        List<PlayerGame> finishedPlayerGames = new ArrayList<>();
        List<PlayerGame> activePlayerGames = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : playerGamesMap.entrySet()) {
            PlayerGame pg = new PlayerGame();
            pg.player = entry.getKey();
            pg.games = entry.getValue();
            if (pg.games.getFirst().equals("waiting-list")) {
                pg.games.removeFirst();
                pg.onWaitingList = true;
            } else if (pg.games.getFirst().equals("recently-finished")) {
                pg.games.removeFirst();
                if (pg.games.isEmpty()) {
                    pg.recentlyFinished = true;
                }
            }
            pg.numGames = pg.games.size();
            if (pg.onWaitingList) waitingPlayerGames.add(pg);
            else if (pg.recentlyFinished) finishedPlayerGames.add(pg);
            else activePlayerGames.add(pg);
        }
        message += playerGamesMessage(waitingPlayerGames, "On waiting list:\n");
        message += playerGamesMessage(finishedPlayerGames,
                "Finished in last " + monthsAgo + " month" + (monthsAgo == 1 ? "" : "s") + ", not in a game:\n");
        message += playerGamesMessage(activePlayerGames, "Currently playing:\n");
        return message;
    }

    private static String phaseName(int phase) {
        return switch (phase) {
            case 1 -> "Storm";
            case 2 -> "Spice Blow";
            case 3 -> "CHOAM Charity";
            case 4 -> "Bidding";
            case 5 -> "Revival";
            case 6 -> "Shipment and Movement";
            case 7 -> "Battles";
            case 8 -> "Spice Collection";
            case 9 -> "Mentat Pause";
            default -> "Phase not identified";
        };
    }

    public static String activeGame(Guild guild, Game game, String categoryName, boolean showFactionsInGames, boolean showUsernamesInGames) throws InvalidGameStateException {
        StringBuilder response = new StringBuilder();
        response.append(categoryName).append("\nTurn ").append(game.getTurn()).append(", ");
        if (game.getTurn() != 0) {
            int phase = game.getPhaseForTracker();
            response.append(phaseName(phase));
            if (phase == 4) {
                Bidding bidding = game.getBidding();
                response.append(", Card ").append(bidding.getBidCardNumber()).append(" of ").append(bidding.getNumCardsForBid());
            } else if (phase == 6) {
                response.append(", ").append(game.numFactionsLeftToMove()).append(" factions remaining");
            }
        }
        response.append("\nMod: ").append(game.getMod());
        if (showFactionsInGames)
            response.append("\nFactions: ").append(String.join(", ", game.getFactions().stream().map(Faction::getName).toList()));
        if (showUsernamesInGames)
            response.append("\nUsernames: ").append(String.join(", ", game.getFactions().stream().map(Faction::getUserName).toList()));
        return response.toString();
    }

    public static String activeGames(SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = event.getOption(showFactions.getName());
        boolean showFactionsinGames = (optionMapping != null && optionMapping.getAsBoolean());
        optionMapping = event.getOption(showUserNames.getName());
        boolean showUserNamesinGames = (optionMapping != null && optionMapping.getAsBoolean());
        optionMapping = event.getOption(optionalAllFactions.getName());
        String selectedFaction = (optionMapping == null) ? null : optionMapping.getAsString();
        StringBuilder response = new StringBuilder();
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        for (Category category : categories) {
            String categoryName = category.getName();
            try {
                DiscordGame discordGame = new DiscordGame(category, false);
                Game game = discordGame.getGame();
                if (selectedFaction == null || game.hasFaction(selectedFaction)) {
                    response.append(activeGame(event.getGuild(), game, categoryName, showFactionsinGames, showUserNamesinGames));
                    response.append("\n\n");
                }
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        return response.toString();
    }

    public static String activeGameRoles(SlashCommandInteractionEvent event) {
        StringBuilder response = new StringBuilder();
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        for (Category category : categories) {
            try {
                DiscordGame discordGame = new DiscordGame(category, false);
                Game game = discordGame.getGame();
                response.append(game.getGameRole()).append(", ");
                response.append(game.getModRole()).append("\n");
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        return response.toString();
    }

    public static String inactiveGameRoles(SlashCommandInteractionEvent event) {
        List<String> roleNames = new ArrayList<>(Objects.requireNonNull(event.getGuild()).getRoles().stream().map(Role::getName).toList());
        roleNames.remove("Mod Emperor");
        roleNames.remove("Mentat");
        roleNames.remove("Moderators");
        roleNames.remove("Bot Developer");
        roleNames.remove("Ticket Tool");
        roleNames.remove("Bots");
        roleNames.remove("MEE6");
        roleNames.remove("Dune App");
        roleNames.remove("Easy Poll");
        roleNames.remove("Looking For Group");
        roleNames.remove("NoTagInStats");
        roleNames.remove("Games Archive");
        roleNames.remove("EasyPoll");
        roleNames.remove("Observer");
        roleNames.remove("Scraper");
        roleNames.remove("Dice Roller");
        roleNames.remove("@everyone");
        for (Category category : Objects.requireNonNull(event.getGuild()).getCategories()) {
            try {
                DiscordGame discordGame = new DiscordGame(category, false);
                Game game = discordGame.getGame();
                roleNames.remove(game.getGameRole());
                roleNames.remove(game.getModRole());
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        if (roleNames.isEmpty())
            return "There are no inactive roles.";
        else
            return "__Inactive roles__\n" + String.join("\n", roleNames);
    }

    private static class GameResults {
        GRList grList;
        int numNewEntries;

        GameResults(GRList grList, int numNewEntries) {
            this.grList = grList;
            this.numNewEntries = numNewEntries;
        }
    }

    static List<String> numberBoxes = List.of(":zero:", ":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":keycap_ten:");

    private static HashSet<String> getAllPlayers(GRList grResults) {
        return getAllPlayers(grResults, Integer.MAX_VALUE);
    }

    private static HashSet<String> getAllPlayers(GRList grResults, int monthsAgo) {
        List<GameResult> gameResults = grResults.gameResults.stream().filter(gr -> {
            String endString = gr.getGameEndDate();
            if (monthsAgo == Integer.MAX_VALUE) {
                return true;
            } else if (endString == null || endString.isEmpty() || monthsAgo == 0) {
                return false;
            } else {
                Instant endDate = ZonedDateTime.of(LocalDate.parse(endString).atStartOfDay(), ZoneOffset.UTC).toInstant();
                Instant pastDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(monthsAgo).toInstant();
                return pastDate.isBefore(endDate);
            }
        }).toList();
        HashSet<String> players = new HashSet<>();
        players.addAll(gameResults.stream().map(GameResult::getAtreides).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getBG).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getBT).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getCHOAM).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getEcaz).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getEmperor).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getFremen).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getGuild).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getHarkonnen).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getIx).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getMoritani).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getRichese).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getMikarrol).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getWydras).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getSpinnette).filter(Objects::nonNull).collect(Collectors.toSet()));
        players.addAll(gameResults.stream().map(GameResult::getLindaren).filter(Objects::nonNull).collect(Collectors.toSet()));
        return players;
    }

    public static String writePlayerStats(Guild guild, GRList gameResults, List<Member> members) {
        Set<String> players = getAllPlayers(gameResults);
        List<PlayerPerformance> allPlayerPerformance = new ArrayList<>();
        for (String player : players) {
            allPlayerPerformance.add(playerPerformance(gameResults, player));
        }
        allPlayerPerformance.sort((a, b) -> a.numWins == b.numWins ? a.numGames - b.numGames : b.numWins - a.numWins);
        StringBuilder playerStatsString = new StringBuilder("__Top Winners__");
        for (PlayerPerformance pp : allPlayerPerformance) {
            if (pp.numWins < 3)
                break;
            String winPercentage = new DecimalFormat("#0.0%").format(pp.winPercentage);
            int tensDigit = pp.numWins % 100 / 10;
            String tensEmoji = tensDigit == 0 ? ":black_small_square:" : numberBoxes.get(tensDigit);
            int onesDigit = pp.numWins % 10;
            String player = getPlayerString(guild, pp.playerName, members);
            playerStatsString.append("\n").append(tensEmoji).append(numberBoxes.get(onesDigit)).append(" - ").append(player).append(" - ").append(winPercentage).append(" (").append(pp.numWins).append("/").append(pp.numGames).append(")");
        }
        return playerStatsString.toString();
    }

    public static String writeTopWinsAboveExpected(Guild guild, GRList gameResults, List<Member> members) {
        Set<String> players = getAllPlayers(gameResults);
        List<PlayerPerformance> allPlayerPerformance = new ArrayList<>();
        for (String player : players) {
            allPlayerPerformance.add(playerPerformance(gameResults, player));
        }
        allPlayerPerformance.sort((a, b) -> Float.compare(b.winsOverExpectation, a.winsOverExpectation));
        StringBuilder playerStatsString = new StringBuilder("__Supreme Rulers of Arrakis__");
//        playerStatsString.append("\n*Players must have played in a game that ended in the past year*");
        for (PlayerPerformance pp : allPlayerPerformance) {
            if (LocalDate.parse(pp.lastGameEnd).isBefore(LocalDate.now().minusYears(1)))
                continue;
            String numExpectedWins = new DecimalFormat("#0.0").format(pp.numExpectedWins);
            String winsOverExpectation = new DecimalFormat("#0.0").format(pp.winsOverExpectation);
            String player = getPlayerString(guild, pp.playerName, members);
            playerStatsString.append("\n").append(winsOverExpectation).append(" - ").append(player).append(" - ").append(" Expected wins: ").append(numExpectedWins).append(", Actual: ").append(pp.numWins);
        }
        return playerStatsString.toString();
    }

    private static List<PlayerPerformance> getTopFactionWinsAboveExpected(Guild guild, GRList gameResults, List<Member> members, String factionName) {
        Set<String> players = getAllPlayers(gameResults);
        List<PlayerPerformance> allPlayerPerformance = new ArrayList<>();
        for (String player : players) {
            allPlayerPerformance.add(playerFactionPerformance(gameResults, player, factionName));
        }
        allPlayerPerformance.sort((a, b) -> Float.compare(b.winsOverExpectation, a.winsOverExpectation));
        return allPlayerPerformance;
    }

    public static String factionMasters(SlashCommandInteractionEvent event, Guild guild, GRList gameResults, List<Member> members) {
        OptionMapping optionMapping = event.getOption(allFactions.getName());
        String factionName = Objects.requireNonNull(optionMapping).getAsString();
        String factionNameLowerCase = factionName.toLowerCase();
        List<PlayerPerformance> allPlayerPerformance = getTopFactionWinsAboveExpected(guild, gameResults, members, factionNameLowerCase);
        StringBuilder playerStatsString = new StringBuilder();
        int lines = 0;
        for (PlayerPerformance pp : allPlayerPerformance) {
            if (lines > 10)
                break;
            if (pp.winsOverExpectation < 0)
                break;
            if (pp.numWins < 2)
                continue;
            if (LocalDate.parse(pp.lastGameEnd).isBefore(LocalDate.now().minusYears(1)))
                continue;
            String numExpectedWins = new DecimalFormat("#0.0").format(pp.numExpectedWins);
            String winsOverExpectation = new DecimalFormat("#0.0").format(pp.winsOverExpectation);
            String player = getPlayerString(guild, pp.playerName, members);
            if (!playerStatsString.isEmpty())
                playerStatsString.append("\n");
            playerStatsString.append(winsOverExpectation).append(" - ").append(player).append(" - ").append(" Expected wins: ").append(numExpectedWins).append(", Actual: ").append(pp.numWins);
            lines++;
        }
        if (playerStatsString.isEmpty())
            return "No players have 2 or more wins with " + factionName;
        return playerStatsString.toString();
    }

    public static String writeTopFactionWinsAboveExpected(Guild guild, GRList gameResults, List<Member> members, String factionName) {
        factionName = factionName.toLowerCase();
        List<PlayerPerformance> allPlayerPerformance = getTopFactionWinsAboveExpected(guild, gameResults, members, factionName);
        StringBuilder playerStatsString = new StringBuilder();
        Float winsOverExpected = null;
        for (PlayerPerformance pp : allPlayerPerformance) {
            if (LocalDate.parse(pp.lastGameEnd).isBefore(LocalDate.now().minusYears(1)))
                continue;
            if (pp.numWins < 2)
                continue;
            if (winsOverExpected != null && pp.winsOverExpectation < winsOverExpected)
                break;
            winsOverExpected = pp.winsOverExpectation;
            String numExpectedWins = new DecimalFormat("#0.0").format(pp.numExpectedWins);
            String winsOverExpectation = new DecimalFormat("#0.0").format(pp.winsOverExpectation);
            String player = getPlayerString(guild, pp.playerName, members);
            if (!playerStatsString.isEmpty())
                playerStatsString.append("\n");
            playerStatsString.append(Emojis.getFactionEmoji(factionName)).append(" ").append(winsOverExpectation).append(" - ").append(player).append(" - ").append(" Expected wins: ").append(numExpectedWins).append(", Actual: ").append(pp.numWins);
        }
        if (playerStatsString.isEmpty())
            return "No players have 2 or more wins with " + Emojis.getFactionEmoji(factionName);
        return playerStatsString.toString();
    }

    private static class PlayerPerformance {
        String playerName;
        int numGames;
        int numWins;
        float winPercentage;
        float numExpectedWins;
        float winsOverExpectation;
        String lastGameEnd;

        public PlayerPerformance(String playerName, int numGames, int numWins, float winPercentage, float numExpectedWins, String lastGameEnd) {
            this.playerName = playerName;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
            this.numExpectedWins = numExpectedWins;
            this.winsOverExpectation = numWins - numExpectedWins;
            this.lastGameEnd = lastGameEnd;
        }
    }

    private static int playerFactionGames(GRList gameResults, String playerName, String factionName, boolean winsOnly) {
        return gameResults.gameResults.stream()
                .filter(gr -> {
                    String p = gr.getFieldValue(factionName);
                    return p != null && p.equals(playerName);
                })
                .filter(gr -> !winsOnly || gr.isWinningPlayer(playerName))
                .toList().size();
    }

    private static PlayerPerformance playerPerformance(GRList gameResults, String playerName) {
        int numGames = gameResults.gameResults.stream().filter(gr -> gr.getAtreides() != null && gr.getAtreides().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getBG() != null && gr.getBG().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getBT() != null && gr.getBT().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getCHOAM() != null && gr.getCHOAM().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getEcaz() != null && gr.getEcaz().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getEmperor() != null && gr.getEmperor().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getFremen() != null && gr.getFremen().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getGuild() != null && gr.getGuild().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getHarkonnen() != null && gr.getHarkonnen().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getIx() != null && gr.getIx().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getMoritani() != null && gr.getMoritani().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getRichese() != null && gr.getRichese().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getMikarrol() != null && gr.getMikarrol().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getWydras() != null && gr.getWydras().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getSpinnette() != null && gr.getSpinnette().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getLindaren() != null && gr.getLindaren().equals(playerName)).toList().size();
        int numWins = gameResults.gameResults.stream().filter(gr -> gr.isWinningPlayer(playerName)).toList().size();

        int totalWins = 0;
        for (GameResult gr : gameResults.gameResults) {
            for (Set<String> ss : gr.getWinningFactions()) {
                totalWins += ss.size();
            }
        }
        double overallWinPercentage = totalWins/(6.0 * gameResults.gameResults.size());
        List<FactionPerformance> allFactionPerformance = getAllFactionPerformance(gameResults);
        float expectedWins = 0;
        String lastGameEnd = "2020-01-01";
        for (GameResult gr : gameResults.gameResults) {
            float expectedWinsThisGame = 0;
            float totalFactionWinPercentage = 0;
            String pf = gr.getFactionForPlayer(playerName);
            if (pf != null) {
                expectedWinsThisGame = factionPerformance(gameResults, pf, Emojis.getFactionEmoji(pf)).winPercentage;
                for (String f : factionNames)
                    for (FactionPerformance fp : allFactionPerformance)
                        if (gr.getFieldValue(f) != null && fp.factionEmoji.equals(Emojis.getFactionEmoji(f)))
                            totalFactionWinPercentage += fp.winPercentage;
                expectedWins += (float) (expectedWinsThisGame / totalFactionWinPercentage * overallWinPercentage * 6);
//                System.out.println(gr.getGameName() + " " + playerName + " " + expectedWins + " " + expectedWinsThisGame + " " + expectedWinsThisGame / totalFactionWinPercentage * overallWinPercentage * 6);
                if (LocalDate.parse(gr.getGameEndDate()).isAfter(LocalDate.parse(lastGameEnd)))
                    lastGameEnd = gr.getGameEndDate();
            }
        }
        float winPercentage = numWins/(float)numGames;
        return new PlayerPerformance(playerName, numGames, numWins, winPercentage, expectedWins, lastGameEnd);
    }

    private static PlayerPerformance playerFactionPerformance(GRList gameResults, String playerName, String factionName) {
        int numGames = gameResults.gameResults.stream().filter(gr -> gr.getFieldValue(factionName) != null && gr.getFieldValue(factionName).equals(playerName)).toList().size();
        int numWins = gameResults.gameResults.stream().filter(gr -> gr.getFieldValue(factionName) != null && gr.getFieldValue(factionName).equals(playerName) && gr.isWinningPlayer(playerName)).toList().size();
//        int numGames = gameResults.gameResults.stream().filter(gr -> gr.getAtreides() != null && gr.getAtreides().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getBG() != null && gr.getBG().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getBT() != null && gr.getBT().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getCHOAM() != null && gr.getCHOAM().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getEcaz() != null && gr.getEcaz().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getEmperor() != null && gr.getEmperor().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getFremen() != null && gr.getFremen().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getGuild() != null && gr.getGuild().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getHarkonnen() != null && gr.getHarkonnen().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getIx() != null && gr.getIx().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getMoritani() != null && gr.getMoritani().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getRichese() != null && gr.getRichese().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getMikarrol() != null && gr.getMikarrol().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getWydras() != null && gr.getWydras().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getSpinnette() != null && gr.getSpinnette().equals(playerName)).toList().size()
//                + gameResults.gameResults.stream().filter(gr -> gr.getLindaren() != null && gr.getLindaren().equals(playerName)).toList().size();
//        int numWins = gameResults.gameResults.stream().filter(gr -> gr.isWinningPlayer(playerName)).toList().size();

        int totalWins = 0;
        for (GameResult gr : gameResults.gameResults) {
            for (Set<String> ss : gr.getWinningFactions()) {
                totalWins += ss.size();
            }
        }
        double overallWinPercentage = totalWins/(6.0 * gameResults.gameResults.size());
        List<FactionPerformance> allFactionPerformance = getAllFactionPerformance(gameResults);
        float expectedWins = 0;
        String lastGameEnd = "2020-01-01";
        for (GameResult gr : gameResults.gameResults) {
            float expectedWinsThisGame = 0;
            float totalFactionWinPercentage = 0;
            String pf = gr.getFactionForPlayer(playerName);
            if (pf != null) {
                if (pf.equals(factionName))
                    expectedWinsThisGame = factionPerformance(gameResults, pf, Emojis.getFactionEmoji(pf)).winPercentage;
                for (String f : factionNames)
                    for (FactionPerformance fp : allFactionPerformance)
                        if (gr.getFieldValue(f) != null && fp.factionEmoji.equals(Emojis.getFactionEmoji(f)))
                            totalFactionWinPercentage += fp.winPercentage;
                expectedWins += (float) (expectedWinsThisGame / totalFactionWinPercentage * overallWinPercentage * 6);
//                System.out.println(gr.getGameName() + " " + playerName + " " + expectedWins + " " + expectedWinsThisGame + " " + expectedWinsThisGame / totalFactionWinPercentage * overallWinPercentage * 6);
                if (LocalDate.parse(gr.getGameEndDate()).isAfter(LocalDate.parse(lastGameEnd)))
                    lastGameEnd = gr.getGameEndDate();
            }
        }
        float winPercentage = numWins/(float)numGames;
        return new PlayerPerformance(playerName, numGames, numWins, winPercentage, expectedWins, lastGameEnd);
    }

    public static String listMembers(SlashCommandInteractionEvent event, List<Member> members) {
        int num = (int) members.stream().filter(m -> m.getGuild() == event.getGuild()).count();
        return members.stream()
                .filter(m -> m.getGuild() == event.getGuild())
                .map(m -> m.getUser().getName() + "\n")
                .collect(Collectors.joining("", num + " members\n", ""));
    }

    public static String writeModeratorStats(Guild guild, GRList gameResults, List<Member> members) {
        Set<String> mods = gameResults.gameResults.stream().map(GameResult::getModerator).collect(Collectors.toSet());
        List<Pair<String, List<GameResult>>> modAndNumGames = new ArrayList<>();
        List<Pair<String, Float>> modAndAverageTurns = new ArrayList<>();
        List<MutableTriple<String, Integer, List<String>>> modAndMaxFactionWins = new ArrayList<>();
        mods.forEach(m -> modAndNumGames.add(new ImmutablePair<>(m, gameResults.gameResults.stream().filter(gr -> gr.getModerator().equals(m)).toList())));
        modAndNumGames.sort((a, b) -> Integer.compare(b.getRight().size(), a.getRight().size()));
        StringBuilder moderatorsString = new StringBuilder("__Moderators__");
        for (Pair<String, List<GameResult>> p : modAndNumGames) {
            String moderator = getPlayerString(guild, p.getLeft(), members);
            int totalTurns = p.getRight().stream().mapToInt(GameResult::getTurn).sum();
            float averageTurns = (float) totalTurns / p.getRight().size();
            modAndAverageTurns.add(new ImmutablePair<>(moderator, averageTurns));
            moderatorsString.append("\n").append(p.getRight().size()).append(" - ").append(moderator);
            int maxWins = 0;
            List<String> emojis = new ArrayList<>();
            for (String fn : factionNames) {
                int fnWins = p.getRight().stream().filter(gr -> gr.isWinningFaction(fn)).toList().size();
                if (fnWins > maxWins) {
                    emojis = new ArrayList<>();
                    maxWins = fnWins;
                }
                if (fnWins == maxWins)
                    emojis.add(Emojis.getFactionEmoji(fn));
            }
            modAndMaxFactionWins.add(MutableTriple.of(moderator, maxWins, emojis));
        }
//        moderatorsString.append("\n\n__Most frequent faction winners for each mod__");
//        for (MutableTriple<String, Integer, List<String>> p : modAndMaxFactionWins)
//            moderatorsString.append("\n").append(p.getMiddle()).append(" - ").append(tagEmojis(guild, String.join(" ", p.getRight()))).append(" - ").append(p.getLeft());
        moderatorsString.append("\n\n__Moderator Assists__");
        Set<String> assisters = gameResults.gameResults.stream().filter(gameResult -> gameResult.getAssistantModerators() != null).flatMap(gameResult -> gameResult.getAssistantModerators().stream()).collect(Collectors.toSet());
        List<Pair<String, List<GameResult>>> assistersAndNumGames = new ArrayList<>();
        assisters.forEach(m -> assistersAndNumGames.add(new ImmutablePair<>(m, gameResults.gameResults.stream().filter(gr -> gr.getAssistantModerators() != null && gr.getAssistantModerators().contains(m)).toList())));
        assistersAndNumGames.sort((a, b) -> Integer.compare(b.getRight().size(), a.getRight().size()));
        for (Pair<String, List<GameResult>> p : assistersAndNumGames) {
            String assiter = getPlayerString(guild, p.getLeft(), members);
            moderatorsString.append("\n").append(p.getRight().size()).append(" - ").append(assiter);
        }

        modAndAverageTurns.sort((a, b) -> Float.compare(b.getRight(), a.getRight()));
//        moderatorsString.append("\n\n__Average number of turns__");
//        for (Pair<String, Float> p : modAndAverageTurns) {
//            moderatorsString.append("\n").append(new DecimalFormat("#0.0").format(p.getRight())).append(" - ").append(p.getLeft());
//        }
        return moderatorsString.toString();
    }

    private static int longerGame(GameResult a, GameResult b) {
        int aDuration = a.getGameDuration() == null ? 0 : Integer.parseInt(a.getGameDuration());
        int bDuration = b.getGameDuration() == null ? 0 : Integer.parseInt(b.getGameDuration());
        return Integer.compare(bDuration, aDuration);
    }

    public static String longestGames(Guild guild, GRList gameResults, List<Member> members) {
        List<GameResult> longestGames = gameResults.gameResults.stream().filter(gr -> !gr.getStartingForum().equals("BGG")).sorted(ReportsCommands::longerGame).toList();
        StringBuilder longestGamesString = new StringBuilder("__Longest games__ (The tortured mod stat! :grin:)");
        for (int i = 0; i < 10; i++) {
            GameResult result = longestGames.get(i);
            longestGamesString.append("\n").append(result.getGameDuration()).append(" days, ");
            longestGamesString.append(result.getGameName()).append(", ");
            String moderator = getPlayerString(guild, result.getModerator(), members);
            longestGamesString.append(moderator);
        }
        return longestGamesString.toString();
    }

    private static int fasterGame(GameResult a, GameResult b) {
        float aDaysPerTurn = a.getGameDuration() == null ? Integer.MAX_VALUE : Integer.parseInt(a.getGameDuration()) / (float) a.getTurn();
        float bDaysPerTurn = b.getGameDuration() == null ? Integer.MAX_VALUE : Integer.parseInt(b.getGameDuration()) / (float) b.getTurn();
        return Float.compare(aDaysPerTurn, bDaysPerTurn);
    }

    public static String fastestGames(Guild guild, GRList gameResults, List<Member> members) {
        List<GameResult> longestGames = gameResults.gameResults.stream().sorted(ReportsCommands::fasterGame).toList();
        StringBuilder longestGamesString = new StringBuilder("__Fastest games__");
        for (int i = 0; i < 20; i++) {
            GameResult a = longestGames.get(i);
            String daysPerTurn = new DecimalFormat("#0.0").format(Integer.parseInt(a.getGameDuration()) / (float) a.getTurn());
            longestGamesString.append("\n").append(daysPerTurn).append(" days per turn, ");
            longestGamesString.append(a.getTurn()).append(" turns, ");
            longestGamesString.append(a.getGameName()).append(", ");
            String moderator = getPlayerString(guild, a.getModerator(), members);
            longestGamesString.append(moderator);
        }
        return longestGamesString.toString();
    }

    public static class GRList {
        List<GameResult> gameResults;

        public String generateCSV() {
            StringBuilder reportsCSVFromJson = new StringBuilder(GameResult.getHeader());
            for (GameResult gr : gameResults) {
                reportsCSVFromJson.append(gr.csvString());
            }
            return reportsCSVFromJson.toString();
        }
    }

    public static String updateStats(Guild guild, JDA jda, boolean loadNewGames, List<Member> members, boolean publishIfNoNewGames, boolean publishStatsFileOnly) {
        GameResults gameResults = gatherGameResults(guild);
        int numNewGames = 0;
        if (loadNewGames) {
            numNewGames = loadNewGames(guild, jda, gameResults.grList);
            if (numNewGames > 0 || publishIfNoNewGames)
                publishStats(guild, gameResults.grList, members, publishStatsFileOnly);
        }
        return numNewGames + " new games were added to parsed results.";
    }

    public static String updateStats(SlashCommandInteractionEvent event, List<Member> members) {
        OptionMapping optionMapping = event.getOption(forcePublish.getName());
        boolean publishIfNoNewGames = (optionMapping != null && optionMapping.getAsBoolean());
        optionMapping = event.getOption(statsFileOnly.getName());
        boolean publishStatsFileOnly = (optionMapping != null && optionMapping.getAsBoolean());
        return updateStats(event.getGuild(), event.getJDA(), true, members, publishIfNoNewGames, publishStatsFileOnly);
    }

    public static String statsDiagnostic(SlashCommandInteractionEvent event, List<Member> members) {
        OptionMapping optionMapping = event.getOption(message.getName());
        String channelString = optionMapping.getAsString();
        JDA mainJDA = event.getJDA();
        Guild mainGuild = event.getGuild();
//        String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
//        String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");
//        if (mainToken != null && mainGuildId != null) {
//            mainJDA = JDABuilder.createDefault(mainToken).build().awaitReady();
//            mainGuild = mainJDA.getGuildById(mainGuildId);
//        }
        String response = "";
        try {
            mainJDA.awaitReady();
            TextChannel posts = Objects.requireNonNull(mainGuild).getTextChannelById(channelString);
            if (posts != null) {
                LocalDate startDate = posts.getTimeCreated().toLocalDate();
                response += "start = " + startDate.toString();
                MessageHistory postsHistory = posts.getHistory();
                postsHistory.retrievePast(1).complete();
                List<Message> ml = postsHistory.getRetrievedHistory();
                if (!ml.isEmpty()) {
                    LocalDate endDate = ml.getFirst().getTimeCreated().toLocalDate();
                    response += "\nend = " + endDate.toString();
                }
            } else
                response = "stats is null";
        } catch (Exception e) {
            return response + " ---\n" + e.getMessage();
            // Can't get game start and end, but save everything else
        }
        return response;
    }

    private static class FactionPerformance {
        String factionEmoji;
        int numGames;
        int numWins;
        float winPercentage;
        float averageTurns;
        float averageWinsTurns;

        public FactionPerformance(String factionEmoji, int numGames, int numWins, float winPercentage, float averageTurns, float averageWinsTurns) {
            this.factionEmoji = factionEmoji;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
            this.averageTurns = averageTurns;
            this.averageWinsTurns = averageWinsTurns;
        }
    }

    private static FactionPerformance factionPerformance(GRList gameResults, String factionName, String factionEmoji) {
        List<GameResult> gamesWithFaction = gameResults.gameResults.stream()
                .filter(gr -> {
                    String n = gr.getFieldValue(factionName);
                    return n != null && !n.isEmpty();
                }).toList();
        int numGames = gamesWithFaction.size();
        int totalTurns = gamesWithFaction.stream().mapToInt(GameResult::getTurn).sum();
        List<GameResult> gamesWithFactionWin = gameResults.gameResults.stream()
                .filter(gr -> gr.isWinningFaction(factionName))
                .toList();
        int numWins = gamesWithFactionWin.size();
        int totalWinsTurns = gamesWithFactionWin.stream().mapToInt(GameResult::getTurn).sum();
        float winPercentage = numWins/(float)numGames;
        float averageTurns = totalTurns/(float)numGames;
        float averageWinsTurns = totalWinsTurns/(float)numWins;
        return new FactionPerformance(factionEmoji, numGames, numWins, winPercentage, averageTurns, averageWinsTurns);
    }

    private static List<FactionPerformance> getAllFactionPerformance(GRList gameResults) {
        List<FactionPerformance> allFactionPerformance = new ArrayList<>();
        allFactionPerformance.add(factionPerformance(gameResults, "atreides", Emojis.ATREIDES));
        allFactionPerformance.add(factionPerformance(gameResults, "bg", Emojis.BG));
        allFactionPerformance.add(factionPerformance(gameResults, "bt", Emojis.BT));
        allFactionPerformance.add(factionPerformance(gameResults, "choam", Emojis.CHOAM));
        allFactionPerformance.add(factionPerformance(gameResults, "ecaz", Emojis.ECAZ));
        allFactionPerformance.add(factionPerformance(gameResults, "emperor", Emojis.EMPEROR));
        allFactionPerformance.add(factionPerformance(gameResults, "fremen", Emojis.FREMEN));
        allFactionPerformance.add(factionPerformance(gameResults, "guild", Emojis.GUILD));
        allFactionPerformance.add(factionPerformance(gameResults, "harkonnen", Emojis.HARKONNEN));
        allFactionPerformance.add(factionPerformance(gameResults, "ix", Emojis.IX));
        allFactionPerformance.add(factionPerformance(gameResults, "moritani", Emojis.MORITANI));
        allFactionPerformance.add(factionPerformance(gameResults, "richese", Emojis.RICHESE));
        allFactionPerformance.add(factionPerformance(gameResults, "mikarrol", ":mikarrol:"));
        allFactionPerformance.add(factionPerformance(gameResults, "wydras", ":wydras:"));
        allFactionPerformance.add(factionPerformance(gameResults, "spinnette", ":regional_indicator_s:"));
        allFactionPerformance.add(factionPerformance(gameResults, "lindaren", ":lindaren:"));
        allFactionPerformance.sort((a, b) -> Float.compare(b.winPercentage, a.winPercentage));
        return allFactionPerformance;
    }

    public static String writeFactionStats(Guild guild, GRList gameResults) {
        List<FactionPerformance> allFactionPerformance = getAllFactionPerformance(gameResults);
        StringBuilder factionStatsString = new StringBuilder("__Factions__");
        for (FactionPerformance fs : allFactionPerformance) {
            String winPercentage = new DecimalFormat("#0.0%").format(fs.winPercentage);
            factionStatsString.append("\n").append(fs.factionEmoji).append(" ").append(winPercentage).append(" - ").append(fs.numWins).append("/").append(fs.numGames)
                    ;//.append(", Average number of turns with faction win = ").append(fs.averageWinsTurns);
        }
//        factionStatsString.append("\n\n__Average Turns with Faction__ (includes " + Emojis.GUILD + " and " + Emojis.FREMEN + " special victories as 10 turns)");
//        for (FactionPerformance fs : allFactionPerformance) {
//            String averageTurns = new DecimalFormat("#0.0").format(fs.averageTurns);
//            String averageTurnsWins = new DecimalFormat("#0.0").format(fs.averageWinsTurns);
//            factionStatsString.append("\n").append(fs.factionEmoji).append(" ").append(averageTurns).append(" per game, ").append(averageTurnsWins).append(" per win");
//        }
        return tagEmojis(guild, factionStatsString.toString());
    }

    private static class FactionAllyPerformance {
        String name1;
        String name2;
        int numGames;
        int numWins;

        FactionAllyPerformance(String name1, String name2, int numGames, int numWins) {
            this.name1 = name1;
            this.name2 = name2;
            this.numGames = numGames;
            this.numWins = numWins;
        }
    }

    public static String writeFactionAllyPerformance(Guild guild, GRList gameResults) {
        List<FactionAllyPerformance> factionAllyPerformances = new ArrayList<>();
        for (String name1 : factionNames) {
            boolean foundNewName = false;
            for (String name2 : factionNames) {
                if (name1.equals(name2))
                    foundNewName = true;
                else if (foundNewName) {
                    Set<String> factions =  Set.of(name1, name2);
                    int factionAllyWins = gameResults.gameResults.stream()
                            .filter(gr -> gr.getWinningFactions().stream().anyMatch(s -> s.equals(factions)))
                            .toList().size();
                    int factionAllyGames = gameResults.gameResults.stream()
                            .filter(gr -> gr.getFieldValue(name1) != null && gr.getFieldValue(name2) != null)
                            .toList().size();
                    if (factionAllyWins > 0)
                        factionAllyPerformances.add(new FactionAllyPerformance(name1, name2, factionAllyGames, factionAllyWins));
                }
            }
        }
        factionAllyPerformances.sort((a, b) -> Integer.compare(b.numWins, a.numWins));
        StringBuilder result = new StringBuilder("__Most Faction Alliance Wins__\n");
        int lines = 0;
        int currentWins = Integer.MAX_VALUE;
        String ofGamesWithBothFactions = " of games with both factions";
        for (FactionAllyPerformance fp : factionAllyPerformances) {
            if (fp.numWins != currentWins && lines > 5)
                break;
            String winPercentage = new DecimalFormat("#0.0%").format((float)fp.numWins/fp.numGames);
            result.append(tagEmojis(guild, fp.numWins + " - " + Emojis.getFactionEmoji(capitalize(fp.name1)) + " " + Emojis.getFactionEmoji(capitalize(fp.name2)) + " - " + winPercentage + ofGamesWithBothFactions + "\n"));
            ofGamesWithBothFactions = "";
            currentWins = fp.numWins;
            lines++;
        }
        return result.toString();
    }

    private static class TurnStats {
        String turn;
        int numGames;
        int numWins;
        float winPercentage;
        int numTotalWins;

        public TurnStats(String turn, int numGames, int numWins, float winPercentage, int numTotalWins) {
            this.turn = turn;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
            this.numTotalWins = numTotalWins;
        }
    }

    private static TurnStats turnStats(GRList gameResults, String turn) {
        int numGames;
        int numWins;
        int numTotalWins = 0;
        String marker;

        switch (turn) {
            case "F" -> {
                FactionPerformance fp = factionPerformance(gameResults, "fremen", Emojis.FREMEN);
                numGames = gameResults.gameResults.stream()
                        .map(GameResult::getFremen).filter(v -> v != null && !v.isEmpty())
                        .toList().size();
                numWins = gameResults.gameResults.stream()
                        .map(GameResult::getVictoryType).filter(v -> v != null && v.equals("F"))
                        .toList().size();
                numTotalWins = fp.numWins;
                marker = Emojis.FREMEN;
            }
            case "G" -> {
                FactionPerformance gp = factionPerformance(gameResults, "guild", Emojis.GUILD);
                numGames = gameResults.gameResults.stream()
                        .map(GameResult::getGuild).filter(v -> v != null && !v.isEmpty())
                        .toList().size();
                numWins = gameResults.gameResults.stream()
                        .map(GameResult::getVictoryType).filter(v -> v != null && v.equals("G"))
                        .toList().size();
                numTotalWins = gp.numWins;
                marker = Emojis.GUILD;
            }
            case "BG" -> {
                FactionPerformance bgp = factionPerformance(gameResults, "bg", Emojis.BG);
                numGames = gameResults.gameResults.stream()
                        .map(GameResult::getBG).filter(v -> v != null && !v.isEmpty())
                        .toList().size();
                numWins = gameResults.gameResults.stream()
                        .map(GameResult::getVictoryType).filter(v -> v != null && v.equals("BG"))
                        .toList().size();
                numTotalWins = bgp.numWins;
                marker = Emojis.BG;
            }
            case "E" -> {
                FactionPerformance ep = factionPerformance(gameResults, "ecaz", Emojis.ECAZ);
                numGames = gameResults.gameResults.stream()
                        .map(GameResult::getEcaz).filter(v -> v != null && !v.isEmpty())
                        .toList().size();
                numWins = gameResults.gameResults.stream()
                        .map(GameResult::getVictoryType).filter(v -> v != null && v.equals("E"))
                        .toList().size();
                numTotalWins = ep.numWins;
                marker = Emojis.ECAZ;
            }
            case "10" -> {
                numGames = gameResults.gameResults.size();
                numWins = gameResults.gameResults.stream()
                        .filter(gr -> {
                            String e = gr.getVictoryType();
                            return e == null || !e.equals("G") && !e.equals("F");
                        }).map(GameResult::getTurn).filter(v -> v == Integer.parseInt(turn)).toList().size();
                marker = numberBoxes.get(Integer.parseInt(turn));
            }
            default -> {
                numGames = gameResults.gameResults.size();
                numWins = gameResults.gameResults.stream()
                        .map(GameResult::getTurn).filter(v -> v == Integer.parseInt(turn))
                        .toList().size();
                marker = numberBoxes.get(Integer.parseInt(turn));
            }
        }
        float winPercentage = numWins / (float) numGames;
        return new TurnStats(marker, numGames, numWins, winPercentage, numTotalWins);
    }

    public static String updateTurnStats(Guild guild, GRList gameResults) {
        List<TurnStats> allTurnStats = new ArrayList<>();
        allTurnStats.add(turnStats(gameResults, "1"));
        allTurnStats.add(turnStats(gameResults, "2"));
        allTurnStats.add(turnStats(gameResults, "3"));
        allTurnStats.add(turnStats(gameResults, "4"));
        allTurnStats.add(turnStats(gameResults, "5"));
        allTurnStats.add(turnStats(gameResults, "6"));
        allTurnStats.add(turnStats(gameResults, "7"));
        allTurnStats.add(turnStats(gameResults, "8"));
        allTurnStats.add(turnStats(gameResults, "9"));
        allTurnStats.add(turnStats(gameResults, "10"));
        allTurnStats.add(turnStats(gameResults, "F"));
        allTurnStats.add(turnStats(gameResults, "G"));
        allTurnStats.add(turnStats(gameResults, "BG"));
        allTurnStats.add(turnStats(gameResults, "E"));
        StringBuilder turnStatsString = new StringBuilder("__Turns__");
        for (TurnStats ts : allTurnStats) {
            String winPercentage = new DecimalFormat("#0.0%").format(ts.winPercentage);
            turnStatsString.append("\n").append(ts.turn).append(" ").append(winPercentage).append(" - ").append(ts.numWins).append("/").append(ts.numGames);
            if (ts.turn.equals(numberBoxes.get(10)))
                turnStatsString.append(" (excludes " + Emojis.GUILD + " and " + Emojis.FREMEN + " special victories)");
            if (ts.numTotalWins != 0)
                turnStatsString.append(" games with ").append(ts.turn).append(", ")
                        .append(new DecimalFormat("#0.0%").format(ts.numWins/(float)ts.numTotalWins))
                        .append(" of ").append(ts.numTotalWins).append(" ").append(ts.turn).append(" wins");
        }
        return tagEmojis(guild, turnStatsString.toString());
    }

    private static String turnsHistogram(GRList gameResults) {
        int maxTurnWins = 0;
        for (int i = 1; i <= 10; i++) {
            int finalI = i;
            int turnWins = gameResults.gameResults.stream().filter(gr -> gr.getTurn() == finalI).toList().size() / 2;
            if (turnWins > maxTurnWins)
                maxTurnWins = turnWins;
        }
        StringBuilder response = new StringBuilder("__Turns Histogram__```\n\n");
        for (int j = maxTurnWins; j >= 1; j--) {
            StringBuilder responseRow = new StringBuilder(" ");
            for (int i = 1; i <= 10; i++) {
                int finalI = i;
                responseRow.append(gameResults.gameResults.stream().filter(gr -> gr.getTurn() == finalI).toList().size() / 2 >= j ? "║  " : "   ");
            }
            response.append(responseRow).append("\n");
        }
        response.append("\n 1  2  3  4  5  6  7  8  9 10```");
        return response.toString();
    }

    private static String factionSoloVictories(Guild guild, GRList gameResults) {
        int numGames = gameResults.gameResults.size();
        List<GameResult> soloWinGames = gameResults.gameResults.stream()
                .filter(gr -> gr.getWinningFactions().size() == 1 && gr.getWinningFactions().getFirst().size() == 1).toList();
        int numWins = soloWinGames.size();
        String winPercentage = new DecimalFormat("#0.0%").format(numWins / (float) numGames);
        StringBuilder response = new StringBuilder("__Faction Solo Wins__\n" + winPercentage + " - " + numWins + "/" + numGames);
        List<Pair<String, Integer>> factionsSoloWins = new ArrayList<>();
        for (String factionName : factionNames) {
            int factionSoloWins = soloWinGames.stream().filter(gr -> gr.isWinningFaction(factionName)).toList().size();
            if (factionSoloWins > 0)
                factionsSoloWins.add(new ImmutablePair<>(factionName, factionSoloWins));
        }
        factionsSoloWins.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));
        for (Pair<String, Integer> fsw : factionsSoloWins)
            response.append("\n").append(tagEmojis(guild, Emojis.getFactionEmoji(fsw.getLeft()))).append(" ").append(fsw.getRight());
        return response.toString();
    }

    private static class PlayerRecord {
        int games;
        int wins;
        int atreidesGames;
        int bgGames;
        int btGames;
        int choamGames;
        int ecazGames;
        int emperorGames;
        int fremenGames;
        int guildGames;
        int harkonnenGames;
        int ixGames;
        int moritaniGames;
        int richGames;
        int mikarrolGames;
        int wydrasGames;
        int spinnetteGames;
        int lindarenGames;
        int atreidesWins;
        int bgWins;
        int btWins;
        int choamWins;
        int ecazWins;
        int emperorWins;
        int fremenWins;
        int guildWins;
        int harkonnenWins;
        int ixWins;
        int moritaniWins;
        int richWins;
        int mikarrolWins;
        int wydrasWins;
        int spinnetteWins;
        int lindarenWins;

        public String publish(String playerTag, Guild guild) {
            String returnString = playerTag + " has played in " + games + " games and won " + wins;
            if (atreidesGames > 0) {
                returnString += "\n" + Emojis.ATREIDES + " " + atreidesGames + " games";
                if (atreidesWins > 0) returnString += ", " + atreidesWins + " wins";
            }
            if (bgGames > 0) {
                returnString += "\n" + Emojis.BG + " " + bgGames + " games";
                if (bgWins > 0) returnString += ", " + bgWins + " wins";
            }
            if (emperorGames > 0) {
                returnString += "\n" + Emojis.EMPEROR + " " + emperorGames + " games";
                if (emperorWins > 0) returnString += ", " + emperorWins + " wins";
            }
            if (fremenGames > 0) {
                returnString += "\n" + Emojis.FREMEN + " " + fremenGames + " games";
                if (fremenWins > 0) returnString += ", " + fremenWins + " wins";
            }
            if (guildGames > 0) {
                returnString += "\n" + Emojis.GUILD + " " + guildGames + " games";
                if (guildWins > 0) returnString += ", " + guildWins + " wins";
            }
            if (harkonnenGames > 0) {
                returnString += "\n" + Emojis.HARKONNEN + " " + harkonnenGames + " games";
                if (harkonnenWins > 0) returnString += ", " + harkonnenWins + " wins";
            }
            if (btGames > 0) {
                returnString += "\n" + Emojis.BT + " " + btGames + " games";
                if (btWins > 0) returnString += ", " + btWins + " wins";
            }
            if (ixGames > 0) {
                returnString += "\n" + Emojis.IX + " " + ixGames + " games";
                if (ixWins > 0) returnString += ", " + ixWins + " wins";
            }
            if (choamGames > 0) {
                returnString += "\n" + Emojis.CHOAM + " " + choamGames + " games";
                if (choamWins > 0) returnString += ", " + choamWins + " wins";
            }
            if (richGames > 0) {
                returnString += "\n" + Emojis.RICHESE + " " + richGames + " games";
                if (richWins > 0) returnString += ", " + richWins + " wins";
            }
            if (ecazGames > 0) {
                returnString += "\n" + Emojis.ECAZ + " " + ecazGames + " games";
                if (ecazWins > 0) returnString += ", " + ecazWins + " wins";
            }
            if (moritaniGames > 0) {
                returnString += "\n" + Emojis.MORITANI + " " + moritaniGames + " games";
                if (moritaniWins > 0) returnString += ", " + moritaniWins + " wins";
            }
            if (mikarrolGames > 0) {
                returnString += "\n:mikarrol: " + mikarrolGames + " games";
                if (mikarrolWins > 0) returnString += ", " + mikarrolWins + " wins";
            }
            if (wydrasGames > 0) {
                returnString += "\n:wydras: " + wydrasGames + " games";
                if (wydrasWins > 0) returnString += ", " + wydrasWins + " wins";
            }
            if (spinnetteGames > 0) {
                returnString += "\nSpinnette " + spinnetteGames + " games";
                if (spinnetteWins > 0) returnString += ", " + spinnetteWins + " wins";
            }
            if (lindarenGames > 0) {
                returnString += "\nLindaren " + lindarenGames + " games";
                if (lindarenWins > 0) returnString += ", " + lindarenWins + " wins";
            }
            return tagEmojis(guild, returnString);
        }
    }

    private static PlayerRecord getPlayerRecord(GRList gameResults, String playerName) {
        PlayerRecord pr = new PlayerRecord();
        for (GameResult gameResult : gameResults.gameResults) {
            boolean winner = false;
            if (gameResult.isPlayer(playerName)) pr.games++;
            if (gameResult.isWinningPlayer(playerName)) {
                pr.wins++;
                winner = true;
            }
            if (gameResult.getAtreides() != null && gameResult.getAtreides().equals(playerName)) {
                pr.atreidesGames++;
                if (winner) pr.atreidesWins++;
            } else if (gameResult.getBG() != null && gameResult.getBG().equals(playerName)) {
                pr.bgGames++;
                if (winner) pr.bgWins++;
            } else if (gameResult.getBT() != null && gameResult.getBT().equals(playerName)) {
                pr.btGames++;
                if (winner) pr.btWins++;
            } else if (gameResult.getCHOAM() != null && gameResult.getCHOAM().equals(playerName)) {
                pr.choamGames++;
                if (winner) pr.choamWins++;
            } else if (gameResult.getEcaz() != null && gameResult.getEcaz().equals(playerName)) {
                pr.ecazGames++;
                if (winner) pr.ecazWins++;
            } else if (gameResult.getEmperor() != null && gameResult.getEmperor().equals(playerName)) {
                pr.emperorGames++;
                if (winner) pr.emperorWins++;
            } else if (gameResult.getFremen() != null && gameResult.getFremen().equals(playerName)) {
                pr.fremenGames++;
                if (winner) pr.fremenWins++;
            } else if (gameResult.getGuild() != null && gameResult.getGuild().equals(playerName)) {
                pr.guildGames++;
                if (winner) pr.guildWins++;
            } else if (gameResult.getHarkonnen() != null && gameResult.getHarkonnen().equals(playerName)) {
                pr.harkonnenGames++;
                if (winner) pr.harkonnenWins++;
            } else if (gameResult.getIx() != null && gameResult.getIx().equals(playerName)) {
                pr.ixGames++;
                if (winner) pr.ixWins++;
            } else if (gameResult.getMoritani() != null && gameResult.getMoritani().equals(playerName)) {
                pr.moritaniGames++;
                if (winner) pr.moritaniWins++;
            } else if (gameResult.getRichese() != null && gameResult.getRichese().equals(playerName)) {
                pr.richGames++;
                if (winner) pr.richWins++;
            } else if (gameResult.getMikarrol() != null && gameResult.getMikarrol().equals(playerName)) {
                pr.mikarrolGames++;
                if (winner) pr.mikarrolWins++;
            } else if (gameResult.getWydras() != null && gameResult.getWydras().equals(playerName)) {
                pr.wydrasGames++;
                if (winner) pr.wydrasWins++;
            } else if (gameResult.getSpinnette() != null && gameResult.getSpinnette().equals(playerName)) {
                pr.spinnetteGames++;
                if (winner) pr.spinnetteWins++;
            } else if (gameResult.getLindaren() != null && gameResult.getLindaren().equals(playerName)) {
                pr.lindarenGames++;
                if (winner) pr.lindarenWins++;
            }
        }
        return pr;
    }

    private static final Pattern taggedEmojis = Pattern.compile("<:([a-zA-Z0-9_]+):\\d+>");
    private static final Pattern untaggedEmojis = Pattern.compile("(?<!<):([a-zA-Z0-9_]+):(?!\\d+>)");
    private static final Pattern playerAndRoleTags = Pattern.compile("<@&?([a-zA-Z0-9_]+)>");
    private static final Pattern turn = Pattern.compile(".*Turn ([0-9]+)");

    private static String capitalize(String strippedEmoji) {
        if (strippedEmoji == null || strippedEmoji.isEmpty())
            return "";
        if (strippedEmoji.equals("rich"))
            return "richese";
        return strippedEmoji;
    }

    private static void publishStats(Guild guild, GRList grList, List<Member> members, boolean publishStatsFileOnly) {
        Category category = getStatsCategory(guild);
        TextChannel playerStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("player-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The player-stats channel was not found."));
        ThreadChannel parsedResults = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("parsed-results")).findFirst().orElseThrow(() -> new IllegalStateException("The parsed-results thread was not found."));

        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getAnnotation(Exclude.class) != null;
            }
        };

        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(strategy)
                .create();
        FileUpload jsonFileUpload = FileUpload.fromData(
                gson.toJson(grList).getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.json"
        );
        parsedResults.sendFiles(jsonFileUpload).queue();
        if (publishStatsFileOnly)
            return;

        FileUpload csvFileUpload = FileUpload.fromData(
                grList.generateCSV().getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.csv"
        );
//        parsedResults.sendFiles(fileUpload).complete();
        TextChannel statsDiscussionChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("stats-discussion")).findFirst().orElseThrow(() -> new IllegalStateException("The stats-discussion channel was not found."));
        statsDiscussionChannel.sendFiles(csvFileUpload).queue();
        statsDiscussionChannel.sendFiles(jsonFileUpload).queue();

        TextChannel factionStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("faction-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The faction-stats channel was not found."));
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(factionStatsChannel).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        factionStatsChannel.sendMessage(writeFactionStats(guild, grList)).queue();
        factionStatsChannel.sendMessage(turnsHistogram(grList)).queue();
        factionStatsChannel.sendMessage(updateTurnStats(guild, grList)).queue();
        factionStatsChannel.sendMessage(factionSoloVictories(guild, grList)).queue();
        factionStatsChannel.sendMessage(writeFactionAllyPerformance(guild, grList)).queue();

        TextChannel moderatorStats = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("moderator-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The moderator-stats channel was not found."));
        messageHistory = MessageHistory.getHistoryFromBeginning(moderatorStats).complete();
        messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        moderatorStats.sendMessage(writeModeratorStats(guild, grList, members)).queue();

        messageHistory = MessageHistory.getHistoryFromBeginning(playerStatsChannel).complete();
        messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        playerStatsChannel.sendMessage("Use **/my-record** to check your own wins and faction plays and **/reports player-record** for other players on the server.").queue();
        StringBuilder playerStatsString = new StringBuilder();
        String[] playerStatsLines = writeTopWinsAboveExpected(guild, grList, members).split("\n");
        int expectLines = 0;
        for (String s : playerStatsLines) {
            if (expectLines == 12)
                break;
            if (!playerStatsString.isEmpty())
                playerStatsString.append("\n");
            playerStatsString.append(s);
            expectLines++;
        }
        if (!playerStatsString.isEmpty())
            playerStatsChannel.sendMessage(playerStatsString.toString()).queue();

        playerStatsString = new StringBuilder();
        playerStatsLines = writePlayerStats(guild, grList, members).split("\n");
        int mentions = 0;
        for (String s : playerStatsLines) {
            if (playerStatsString.length() + 1 + s.length() > 2000 || mentions == 20) {
                playerStatsChannel.sendMessage(playerStatsString.toString()).queue();
                playerStatsString = new StringBuilder();
                mentions = 0;
            }
            if (!playerStatsString.isEmpty())
                playerStatsString.append("\n");
            playerStatsString.append(s);
            if (s.contains("<@"))
                mentions++;
        }
        if (!playerStatsString.isEmpty())
            playerStatsChannel.sendMessage(playerStatsString.toString()).queue();

        playerStatsChannel.sendMessage(writePlayerAllyPerformance(guild, grList, members)).queue();
//        String factionPlays = highFactionPlays(guild, grList, members);
//        if (!factionPlays.isEmpty())
//            playerStatsChannel.sendMessage("__High Faction Plays__\n" + factionPlays).queue();
        playerStatsChannel.sendMessage("__Played All 12 Factions__\n" + playedAllTwelve(guild, grList, members)).queue();
        playerStatsChannel.sendMessage("__Won as All Original 6 Factions__\n" + wonAsAllOriginalSix(guild, grList, members)).queue();
//        playerStatsChannel.sendMessage("__High Faction Wins__\n" + highFactionGames(guild, grList, members, true)).queue();
//        playerStatsChannel.sendMessage("__High Faction Plays__\n" + highFactionGames(guild, grList, members, false)).queue();

        playerStatsString = new StringBuilder("__Faction Masters__\n_Minimum 2 wins and a game played in the last year._\n");
        for (String fn : factionNames) {
            String s = writeTopFactionWinsAboveExpected(guild, grList, members, fn);
            playerStatsString.append(s).append("\n");
        }
        playerStatsChannel.sendMessage(tagEmojis(guild, playerStatsString.toString())).queue();

//        playerStatsChannel.sendMessage(playerSoloVictories(guild, grList, members)).queue();
        playerStatsChannel.sendMessage("__Won with Most Different Factions__\n" + wonAsMostFactions(guild, grList, members)).queue();
//        playerStatsChannel.sendMessage("__Played Original 6 Multiple Times__\n" + playedAllOriginalSixMultipleTimes(guild, grList, members)).queue();
//        playerStatsChannel.sendMessage("__Played Expansion 6 Multiple Times__\n" + playedAllExpansionMultipleTimes(guild, grList, members)).queue();
    }

    public static String loadJsonString(Category category) {
        TextChannel playerStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("player-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The player-stats channel was not found."));
        ThreadChannel parsedResults = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("parsed-results")).findFirst().orElseThrow(() -> new IllegalStateException("The parsed-results thread was not found."));

        MessageHistory h = parsedResults.getHistory();
        h.retrievePast(1).complete();
        List<Message> ml = h.getRetrievedHistory();
        if (ml.size() == 1) {
            List<Message.Attachment> messageList =  ml.getFirst().getAttachments();
            if (!messageList.isEmpty()) {
                Message.Attachment encoded = ml.getFirst().getAttachments().getFirst();
                CompletableFuture<InputStream> future = encoded.getProxy().download();
                try {
                    String jsonResults = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
                    future.get().close();
                    return jsonResults;
                } catch (IOException | InterruptedException | ExecutionException e) {
                    // Could not load from json file. Complete new set will be generated from game-results
                }
            }
        }
        return "[]";
    }

    private static GameResults gatherGameResults(Guild guild) {
        Category category = getStatsCategory(guild);
        String jsonResults = loadJsonString(category);
//        String nameToChange = "Atreides";
        String nameToChange = "Harkonnen";
        if (jsonResults.contains(nameToChange)) {
            for (String fn : factionNamesCapitalized) {
                jsonResults = jsonResults.replace(fn, fn.toLowerCase());
                jsonResults = jsonResults.replace("victoryType\":\"bg", "victoryType\":\"BG");
                jsonResults = jsonResults.replace("victoryType\":\"e", "victoryType\":\"E");
                jsonResults = jsonResults.replace("victoryType\":\"f", "victoryType\":\"F");
                jsonResults = jsonResults.replace("victoryType\":\"g", "victoryType\":\"G");
            }
        }
        Gson gson = DiscordGame.createGsonDeserializer();
        GRList grList;
        if (!jsonResults.contains("{\"gameResults\":"))
            grList = gson.fromJson("{\"gameResults\":" + jsonResults + "}", GRList.class);
        else
            grList = gson.fromJson(jsonResults, GRList.class);
        for (GameResult gr : grList.gameResults) {
            if (gr.getDaysUntilArchive() == null && gr.getGameEndDate() != null) {
                LocalDate endDate = LocalDate.parse(gr.getGameEndDate());
                LocalDate archiveDate = LocalDate.parse(gr.getArchiveDate());
                if (endDate.isAfter(archiveDate)) {
                    gr.setDaysUntilArchive("-" + archiveDate.datesUntil(endDate).count());
                } else {
                    gr.setDaysUntilArchive("" + endDate.datesUntil(archiveDate).count());
                }
            }
        }
        return new GameResults(grList, 0);
    }

    private static GameResult loadNewGame(Guild guild, JDA jda, Message m) {
        GameResult gr;
        try {
            gr = new GameResult();
            String raw = m.getContentRaw();
            String gameName = raw.split("\n")[0];
            if (gameName.indexOf("__") == 0)
                gameName = gameName.substring(2, gameName.length() - 2);
            gr.setGameName(gameName);
            gr.setMessageID(m.getId());
            LocalDate archiveDate = m.getTimeCreated().toLocalDate();
            gr.setArchiveDate(archiveDate.toString());
            int modStart = raw.indexOf("Moderator");
            int factionsStart = raw.indexOf("Factions");
            if (modStart == -1)
                modStart = factionsStart;
            int winnersStart = raw.indexOf("Winner");
            int summaryStart = raw.indexOf("Summary");

            try {
                String channelString = raw.substring(0, modStart);
                int channelIdStart = channelString.indexOf("<#");
                boolean foundChannelId = false;
                if (gameName.startsWith("PBD 126")) {
                    channelString = "1389629843196612629";
                    foundChannelId = true;
                } else if (channelIdStart != -1) {
                    channelString = channelString.substring(channelString.indexOf("<#"));
                    channelString = channelString.substring(2, channelString.indexOf(">"));
                    foundChannelId = true;
                } else {
                    channelIdStart = channelString.indexOf("https://discord.com/channels/");
                    channelString = channelString.substring(channelIdStart + "https://discord.com/channels/".length() + 1);
                    if (channelIdStart != -1) channelIdStart = channelString.indexOf("/");
                    if (channelIdStart != -1) {
                        channelString = channelString.substring(channelIdStart + 1, channelString.indexOf("\n"));
                        foundChannelId = true;
                    }
                }
                if (foundChannelId) {
                    JDA mainJDA = jda;
                    Guild mainGuild = guild;
//                    String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
//                    String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");
//                    if (mainToken != null && mainGuildId != null) {
//                        mainJDA = JDABuilder.createDefault(mainToken).build().awaitReady();
//                        mainGuild = mainJDA.getGuildById(mainGuildId);
//                    }
                    try {
                        mainJDA.awaitReady();
                        TextChannel posts = Objects.requireNonNull(mainGuild).getTextChannelById(channelString);
                        if (posts != null) {
                            LocalDate startDate = posts.getTimeCreated().toLocalDate();
                            gr.setGameStartDate(startDate.toString());
                            MessageHistory postsHistory = posts.getHistory();
                            postsHistory.retrievePast(1).complete();
                            List<Message> ml = postsHistory.getRetrievedHistory();
                            if (!ml.isEmpty()) {
                                LocalDate endDate = ml.getFirst().getTimeCreated().toLocalDate();
                                gr.setGameEndDate(endDate.toString());
                                gr.setGameDuration("" + startDate.datesUntil(endDate).count());
                                if (endDate.isAfter(archiveDate)) {
                                    gr.setDaysUntilArchive("-" + archiveDate.datesUntil(endDate).count());
                                } else {
                                    gr.setDaysUntilArchive("" + endDate.datesUntil(archiveDate).count());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Can't get game start and end, but save everything else
                    }
                }
            } catch (Exception e) {
                // Can't get a channel to find last post date
            }

            String[] lines;
            String moderator = "";
            List<String> assistants = new ArrayList<>();
            String modString = raw.substring(modStart, factionsStart);
            if (!modString.isEmpty()) {
                lines = modString.split("\n");
                Matcher modMatcher = playerAndRoleTags.matcher(modString);
                if (modMatcher.find()) {
                    Role mods = jda.getRoleById(modMatcher.group(1));
                    if (mods != null)
                        moderator = "@" + mods.getName();
                    else
                        moderator = "@" + jda.retrieveUserById(modMatcher.group(1)).complete().getName();
                    while (modMatcher.find())
                        assistants.add("@" + jda.retrieveUserById(modMatcher.group(1)).complete().getName());
                } else
                    moderator = lines[1].substring(2).split("\\s+", 2)[0];
            }
            gr.setModerator(moderator);
            if (!assistants.isEmpty())
                gr.setAssistantModerators(assistants);

            String winnersString;
            String strippedEmoji1 = "";
            String strippedEmoji2 = "";
            if (summaryStart == -1)
                winnersString = raw.substring(winnersStart);
            else
                winnersString = raw.substring(winnersStart, summaryStart);
            Matcher turnMatcher = turn.matcher(winnersString);
            String victoryType = "";
            if (winnersString.toLowerCase().contains("predict"))
                victoryType = "BG";
            else if (winnersString.toLowerCase().contains("co-occup"))
                victoryType = "E";
            else if (winnersString.contains("Guild Default"))
                victoryType = "G";
            else if (winnersString.contains(":guild: victory condition"))
                victoryType = "G";
            else if (winnersString.contains(tagEmojis(guild,":guild: Default")))
                victoryType = "G";
            else if (gameName.contains("PBD67"))
                victoryType = "Most strongholds";
            else if (winnersString.toLowerCase().contains("victory condition")) {
                if (winnersString.toLowerCase().contains("guild"))
                    victoryType = "G";
                else if (winnersString.toLowerCase().contains("fremen"))
                    victoryType = "F";
                else if (winnersString.toLowerCase().contains("ecaz"))
                    victoryType = "E";
            }

            gr.setVictoryType(victoryType);
            if (turnMatcher.find()) {
                gr.setTurn(Integer.parseInt(turnMatcher.group(1)));
            }
            if (!winnersString.toLowerCase().contains("predict"))
                winnersString = winnersString.substring(winnersString.indexOf("\n"));
            Matcher taggedMatcher = taggedEmojis.matcher(winnersString);
            Matcher untaggedMatcher = untaggedEmojis.matcher(winnersString);
            if (taggedMatcher.find()) {
                strippedEmoji1 = taggedMatcher.group(1);
                if (taggedMatcher.find())
                    strippedEmoji2 = taggedMatcher.group(1);
                else if (untaggedMatcher.find())
                    strippedEmoji2 = untaggedMatcher.group(1);
            } else if (untaggedMatcher.find()) {
                strippedEmoji1 = untaggedMatcher.group(1);
                if (taggedMatcher.find())
                    strippedEmoji2 = taggedMatcher.group(1);
                else if (untaggedMatcher.find())
                    strippedEmoji2 = untaggedMatcher.group(1);
            }
            strippedEmoji1 = capitalize(strippedEmoji1);
            strippedEmoji2 = capitalize(strippedEmoji2);
            String winner1Faction;
            String winner2Faction = "";
            String predictedFaction = "";
            if (victoryType.equals("BG")) {
                winner1Faction = "bg";
                if (gameName.equals("PBD 56: Caladanian Kicks"))
                    predictedFaction = "Emperor";
                else
                    predictedFaction = strippedEmoji1.equals("bg") ? strippedEmoji2 : strippedEmoji1;
            } else {
                winner1Faction = strippedEmoji1;
                winner2Faction = strippedEmoji2;
            }
            if (gameName.equals("Discord 38"))
                winner1Faction = "Emperor";
            else if (gameName.equals("Discord 40")) {
                winner1Faction = "BT";
                winner2Faction = "Harkonnen";
            }
//            gr.setWinner1Faction(winner1Faction);
            if (!winner2Faction.isEmpty())
                gr.setWinningFactions(List.of(Set.of(winner1Faction, winner2Faction)));
//                gr.setWinner2Faction(winner2Faction);
            else
                gr.setWinningFactions(List.of(Set.of(winner1Faction)));
            if (gameName.contains("PBD67"))
                gr.setWinningFactions(List.of(Set.of("bt"), Set.of("atreides"), Set.of("choam"), Set.of("richese"), Set.of("bg"), Set.of("moritani")));
            if (!predictedFaction.isEmpty())
                gr.setPredictedFaction(predictedFaction);

            String factionsString = raw.substring(factionsStart, winnersStart);
            lines = factionsString.split("\n");
            String playerName;
            for (String s : lines) {
                if (s.contains("Factions") || s.isEmpty())
                    continue;
                taggedMatcher = taggedEmojis.matcher(s);
                untaggedMatcher = untaggedEmojis.matcher(s);
                Matcher matcherThatFoundEmoji;
                String factionName;
                if (taggedMatcher.find()) {
                    factionName = taggedMatcher.group(1);
                    matcherThatFoundEmoji = taggedMatcher;
                } else if (untaggedMatcher.find()) {
                    factionName = untaggedMatcher.group(1);
                    matcherThatFoundEmoji = untaggedMatcher;
                } else {
                    continue;
                }
                Matcher matcher2 = playerAndRoleTags.matcher(s.substring(matcherThatFoundEmoji.end()));
                if (matcher2.find())
                    playerName = "@" + jda.retrieveUserById(matcher2.group(1)).complete().getName();
                else {
                    playerName = s.substring(matcherThatFoundEmoji.end());
                    if (playerName.indexOf(" - ") == 0)
                        playerName = playerName.substring(3).split("\\s+", 2)[0];
                }
                gr.setFieldValue(capitalize(factionName), playerName);
            }
            if (gameName.equals("Discord 32"))
                gr.setFremen("@jefwiodrade");
            else if (gameName.equals("Discord 47"))
                gr.setRichese("@jadedaf");
            if (winner2Faction.isEmpty())
                gr.setWinningPlayers(List.of(Set.of(gr.getFieldValue(winner1Faction))));
            else
                gr.setWinningPlayers(List.of(Set.of(gr.getFieldValue(winner1Faction), gr.getFieldValue(winner2Faction))));
            if (gameName.contains("PBD67"))
                gr.setWinningPlayers(List.of(Set.of(gr.getBT()), Set.of(gr.getAtreides()), Set.of(gr.getCHOAM()), Set.of(gr.getRichese()), Set.of(gr.getBG()), Set.of(gr.getMoritani())));
//            gr.setWinner1Player(gr.getFieldValue(winner1Faction));
//            gr.setWinner2Player(gr.getFieldValue(winner2Faction));
            if (!predictedFaction.isEmpty())
                gr.setPredictedPlayer(gr.getFieldValue(predictedFaction));
        } catch (Exception e) {
            // Not a game result message, so skip it.
            gr = null;
        }
        return gr;
    }

    private static int loadNewGames(Guild guild, JDA jda, GRList grList) {
        Category category = getStatsCategory(guild);
        TextChannel gameResults = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("game-results")).findFirst().orElseThrow(() -> new IllegalStateException("The game-results channel was not found."));

        String lastMessageID = "";
        if (!grList.gameResults.isEmpty())
            lastMessageID = grList.gameResults.getFirst().getMessageID();

        MessageHistory messageHistory;
        if (lastMessageID.isEmpty())
            messageHistory = MessageHistory.getHistoryFromBeginning(gameResults).limit(10).complete();
        else
            messageHistory = MessageHistory.getHistoryAfter(gameResults, lastMessageID).limit(10).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();
        List<GameResult> newGRs = messages.stream().map(m -> loadNewGame(guild, jda, m)).filter(Objects::nonNull).collect(Collectors.toList());
        int numNewGames = newGRs.size();
        Collections.reverse(newGRs);
        newGRs.forEach(gr -> grList.gameResults.addFirst(gr));
        return numNewGames;
    }

    public static RichCustomEmoji getEmoji(Guild guild, String emojiName) {
        Map<String, RichCustomEmoji> emojis = EmojiCache.getEmojis(Objects.requireNonNull(guild).getId());
        return emojis.get(emojiName.replace(":", ""));
    }

    public static String getEmojiTag(Guild guild, String emojiName) {
        RichCustomEmoji emoji = getEmoji(guild, emojiName);
        return emoji == null ? ":" + emojiName.replace(":", "") + ":" : emoji.getFormatted();
    }
    public static String tagEmojis(Guild guild, String message) {
        Matcher matcher = untaggedEmojis.matcher(message);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, getEmojiTag(guild, matcher.group(1)));
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    public static String playerRecord(SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = event.getOption(user.getName());
        User thePlayer = (optionMapping != null ? optionMapping.getAsUser() : null);
        if (thePlayer == null)
            thePlayer = event.getUser();
        return playerRecord(event.getGuild(), thePlayer.getName(), thePlayer.getAsMention());
    }

    public static String playedAllOriginalSix(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen");
        return playedFactions(guild, grList, factions, members, false);
    }

    public static String playedAllExpansion(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("bt", "choam", "ecaz", "ix", "moritani", "richese");
        return playedFactions(guild, grList, factions, members, false);
    }

    public static String playedAllOriginalSixMultipleTimes(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen");
        return playedFactionsMultipleTimes(guild, grList, factions, members);
    }

    public static String playedAllExpansionMultipleTimes(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("bt", "choam", "ecaz", "ix", "moritani", "richese");
        return playedFactionsMultipleTimes(guild, grList, factions, members);
    }

    public static String playedAllTwelve(Guild guild, GRList grList, List<Member> members) {
        return playedFactions(guild, grList, factionNames, members, false);
    }

    public static String playedFactions(Guild guild, GRList gameResults, List<String> factions, List<Member> members, boolean showMissingTwo) {
        int listSize = factions.size();
        StringBuilder playedAll = new StringBuilder();
        StringBuilder missingOne = new StringBuilder();
        StringBuilder missingTwo = new StringBuilder();
        for (String playerName : getAllPlayers(gameResults)) {
            int factionsPlayed = 0;
            StringBuilder missedFactionEmojis = new StringBuilder();
            factionsPlayed += (int) factions.stream().map(factionName -> gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName)).toList()).filter(grs -> !grs.isEmpty()).count();
            factions.forEach(factionName -> {
                if (gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName)).toList().isEmpty())
                    missedFactionEmojis.append(Emojis.getFactionEmoji(factionName)).append(" ");
            });

            String playerTag = getPlayerString(guild, playerName, members);
            if (factionsPlayed == listSize)
                playedAll.append(playerTag).append(" has played all ").append(listSize).append(".\n");
            else if (listSize > 1 && factionsPlayed == listSize - 1)
                missingOne.append(playerTag).append(" has played as ").append(listSize - 1).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (listSize > 2 && factionsPlayed == listSize - 2)
                missingTwo.append(playerTag).append(" has played as ").append(listSize - 2).append(", missing only ").append(missedFactionEmojis).append("\n");
        }
        if (playedAll.isEmpty() && missingOne.isEmpty())
            return "No players have played all " + listSize + " factions.";
        return tagEmojis(guild, playedAll.toString() + missingOne + (showMissingTwo ? missingTwo : ""));
    }

    private static class PlayerFactionCounts {
        String playerName;
        int minPlays;
        List<String> nonMaxFactionEmojis;
    }

    public static String playedFactionsMultipleTimes(Guild guild, GRList gameResults, List<String> factions, List<Member> members) {
        StringBuilder playedAll = new StringBuilder();
        List<PlayerFactionCounts> pfcs = new ArrayList<>();
        for (String playerName : getAllPlayers(gameResults)) {
            List<Pair<String, Integer>> playsPerFaction = new ArrayList<>();
            for (String factionName : factions) {
                int numPlays = gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName)).toList().size();
                playsPerFaction.add(new ImmutablePair<>(factionName, numPlays));
            }
            PlayerFactionCounts pfc = new PlayerFactionCounts();
            pfc.playerName = playerName;
            pfc.minPlays = playsPerFaction.stream().map(Pair::getRight).reduce(Integer::min).orElseThrow();
            pfc.nonMaxFactionEmojis = playsPerFaction.stream().filter(p -> p.getRight() == pfc.minPlays).map(p -> Emojis.getFactionEmoji(p.getLeft())).toList();
            pfcs.add(pfc);
        }
        pfcs.sort((p1, p2) -> {
            if (p1.minPlays == p2.minPlays) {
                int missed1 = 0;
                int missed2 = 0;
                if (p1.nonMaxFactionEmojis != null)
                    missed1 = p1.nonMaxFactionEmojis.size();
                if (p2.nonMaxFactionEmojis != null) {
                    missed2 = p2.nonMaxFactionEmojis.size();
                }
                return Integer.compare(missed1, missed2);
            }
            return (Integer.compare(p2.minPlays, p1.minPlays));
        });
        for (PlayerFactionCounts pfc : pfcs) {
            if (pfc.minPlays == 1 && pfc.nonMaxFactionEmojis.size() > 1)
                break;
            String playerTag = getPlayerString(guild, pfc.playerName, members);

            playedAll.append(playerTag);
            if (pfc.minPlays == 0)
                playedAll.append(" has played ").append(factions.size() - 1).append(", needs only ").append(pfc.nonMaxFactionEmojis.getFirst());
            else {
                playedAll.append(" has played all ").append(factions.size());
                if (pfc.minPlays > 1)
                    playedAll.append(" at least ").append(pfc.minPlays).append(" times each");
                if (pfc.nonMaxFactionEmojis.size() == 1)
                    playedAll.append(", needs only ").append(pfc.nonMaxFactionEmojis.getFirst()).append(" for ").append(pfc.minPlays + 1).append(" times each");
            }
            playedAll.append("\n");
        }
        return tagEmojis(guild, playedAll.toString());
    }

    public static String wonAsAllOriginalSix(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen");
        return wonAsFactions(guild, grList, factions, members, false);
    }

    public static String wonAsAllExpansion(Guild guild, GRList grList, List<Member> members) {
        List<String> factions = List.of("bt", "choam", "ecaz", "ix", "moritani", "richese");
        return wonAsFactions(guild, grList, factions, members, false);
    }

    public static String wonAsFactions(Guild guild, GRList gameResults, List<String> factions, List<Member> members, boolean showMissingTwo) {
        int listSize = factions.size();
        StringBuilder playedAll = new StringBuilder();
        StringBuilder missingOne = new StringBuilder();
        StringBuilder missingTwo = new StringBuilder();
        for (String playerName : getAllPlayers(gameResults)) {
            int factionsPlayed = 0;
            StringBuilder missedFactionEmojis = new StringBuilder();
            factionsPlayed += (int) factions.stream().map(factionName -> gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinningPlayer(playerName)).toList()).filter(grs -> !grs.isEmpty()).count();
            factions.forEach(factionName -> {
                if (gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinningPlayer(playerName)).toList().isEmpty())
                    missedFactionEmojis.append(Emojis.getFactionEmoji(factionName)).append(" ");
            });

            String playerTag = getPlayerString(guild, playerName, members);
            if (factionsPlayed == listSize)
                playedAll.append(playerTag).append(" has won as all ").append(listSize).append(".\n");
            else if (listSize > 1 && factionsPlayed == listSize - 1)
                missingOne.append(playerTag).append(" has won as ").append(listSize - 1).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (listSize > 2 && factionsPlayed == listSize - 2)
                missingTwo.append(playerTag).append(" has won as ").append(listSize - 2).append(", missing only ").append(missedFactionEmojis).append("\n");
        }
        if (playedAll.isEmpty() && missingOne.isEmpty())
            return "No players have won with all " + listSize + " factions.";
        return tagEmojis(guild, playedAll.toString() + missingOne + (showMissingTwo ? missingTwo : ""));
    }

    public static String wonAsMostFactions(Guild guild, GRList gameResults, List<Member> members) {
        int maxFactionsPlayed = 12;
        StringBuilder playedMax = new StringBuilder();
        StringBuilder maxMinusOne = new StringBuilder();
        StringBuilder maxMinusTwo = new StringBuilder();
        StringBuilder maxMinusThree = new StringBuilder();
        for (String playerName : getAllPlayers(gameResults)) {
            int factionsPlayed = 0;
            StringBuilder missedFactionEmojis = new StringBuilder();
            factionsPlayed += (int) factionNames.stream().map(factionName -> gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinningPlayer(playerName)).toList()).filter(grs -> !grs.isEmpty()).count();
            factionNames.forEach(factionName -> {
                if (gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinningPlayer(playerName)).toList().isEmpty())
                    missedFactionEmojis.append(Emojis.getFactionEmoji(factionName)).append(" ");
            });
            String playerTag = getPlayerString(guild, playerName, members);
            if (factionsPlayed == maxFactionsPlayed)
                playedMax.append(factionsPlayed).append(" - ").append(playerTag).append("\n");
            else if (factionsPlayed == maxFactionsPlayed - 1)
                maxMinusOne.append(factionsPlayed).append(" - ").append(playerTag).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (factionsPlayed == maxFactionsPlayed - 2)
                maxMinusTwo.append(factionsPlayed).append(" - ").append(playerTag).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (factionsPlayed == maxFactionsPlayed - 3)
                maxMinusThree.append(factionsPlayed).append(" - ").append(playerTag).append(", missing only ").append(missedFactionEmojis).append("\n");
        }
        playedMax.append(maxMinusOne).append(maxMinusTwo).append(maxMinusThree);
        if (playedMax.isEmpty())
            return "No players have played.";
        return tagEmojis(guild, playedMax.toString());
    }

    public static String soloVictories(Guild guild, GRList gameResults, List<Member> members) {
        return factionSoloVictories(guild, gameResults) + "\n\n" + playerSoloVictories(guild, gameResults, members);
    }

    private static final List<String> factionNamesCapitalized = List.of("Atreides", "BG", "Emperor", "Fremen", "Guild", "Harkonnen", "BT", "Ix", "CHOAM", "Richese", "Ecaz", "Moritani");
    private static final List<String> factionNames = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen", "bt", "ix", "choam", "richese", "ecaz", "moritani");

    public static String writePlayerAllyPerformance(Guild guild, GRList gameResults, List<Member> members) {
        List<MutableTriple<String, String, Integer>> playerAllyWinsTriple = new ArrayList<>();
        List<String> playerNames = getAllPlayers(gameResults).stream().toList();
        List<String> playerNames2 = new ArrayList<>(getAllPlayers(gameResults).stream().toList());
        for (String name1 : playerNames) {
            playerNames2.remove(name1);
            for (String name2 : playerNames2) {
                Set<String> players =  Set.of(name1, name2);
                int playerAllyWins = gameResults.gameResults.stream()
                        .filter(gr -> gr.getWinningPlayers().stream().anyMatch(s -> s.equals(players)))
                        .toList().size();
                if (playerAllyWins > 0)
                    playerAllyWinsTriple.add(MutableTriple.of(name1, name2, playerAllyWins));
            }
        }
        playerAllyWinsTriple.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));
        StringBuilder result = new StringBuilder("__Most Player Alliance Wins__\n");
        for (MutableTriple<String, String, Integer> pawt : playerAllyWinsTriple) {
            if (pawt.getRight() < 2)
                break;
            String tag1 = getPlayerString(guild, pawt.getLeft(), members);
            String tag2 = getPlayerString(guild, pawt.getMiddle(), members);
            result.append(pawt.getRight()).append(" - ").append(tag1).append(" ").append(tag2).append("\n");
        }
        return result.toString();
    }

    public static String highFactionGames(Guild guild, GRList gameResults, List<Member> members, boolean winsOnly) {
        Set<String> players = getAllPlayers(gameResults);
        List<MutableTriple<String, String, Integer>> playerFactionCounts = new ArrayList<>();
        players.forEach(playerName -> factionNames.stream().map(factionName -> MutableTriple.of(playerName, factionName, playerFactionGames(gameResults, playerName, factionName, winsOnly))).forEach(playerFactionCounts::add));
        int maxGames = playerFactionCounts.stream().map(t -> t.right).mapToInt(t -> t).max().orElse(0);
        List<StringBuilder> highFactionPlays = new ArrayList<>();
        IntStream.range(0, maxGames).forEach(i -> highFactionPlays.add(new StringBuilder()));
        playerFactionCounts.forEach(t -> {
            String punct = t.right == maxGames ? "!" : "";
            if (t.right != 0) {
                String playerTag = getPlayerString(guild, t.left, members);
                highFactionPlays.get(t.right - 1).append(Emojis.getFactionEmoji(t.middle)).append(" ").append(t.right).append(" times").append(punct).append(" - ").append(playerTag).append("\n");
            }
        });
        StringBuilder result = new StringBuilder();
        int lines = 0;
        int numPlays = maxGames - 1;
        while (numPlays >= 2 && lines < 6) {
            String hfpString = highFactionPlays.get(numPlays).toString();
            for (char c : hfpString.toCharArray())
                if (c == '\n')
                    lines++;
            result.append(hfpString);
            numPlays--;
        }
        if (maxGames == 0)
            return "No players have played.";
        return tagEmojis(guild, result.toString());
    }

    private static String playerSoloVictories(Guild guild, GRList gameResults, List<Member> members) {
        List<GameResult> soloWinGames = gameResults.gameResults.stream()
                .filter(gr -> gr.getWinningPlayers().size() == 1 && gr.getWinningPlayers().getFirst().size() == 1).toList();
        StringBuilder response = new StringBuilder("__Player Solo Wins__");
        Set<String> players = getAllPlayers(gameResults);
        List<Pair<String, Integer>> playersSoloWins = new ArrayList<>();
        for (String playerName : players) {
            int playerSoloWins = soloWinGames.stream().filter(gr -> gr.isWinningPlayer(playerName)).toList().size();
            if (playerSoloWins > 0)
                playersSoloWins.add(new ImmutablePair<>(playerName, playerSoloWins));
        }
        playersSoloWins.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));
        for (Pair<String, Integer> fsw : playersSoloWins) {
            // Capturing the faction win counts should really be done above with a Triple instead of a Pair
            StringBuilder factionCounts = new StringBuilder();
            factionNames.forEach(factionName -> {
                int factionWins = soloWinGames.stream().filter(gr -> gr.isWinningPlayer(fsw.getLeft())).filter(gr -> gr.isWinningFaction(factionName)).toList().size();
                if (factionWins > 0)
                    factionCounts.append(factionWins).append(" ").append(tagEmojis(guild, Emojis.getFactionEmoji(factionName))).append(" ");
            });
            response.append("\n").append(fsw.getRight()).append(" - ").append(tagEmojis(guild, getPlayerString(guild, fsw.getLeft(), members))).append(" - ").append(factionCounts);
        }
        return response.toString();
    }

    public static String averageDaysPerTurn(Guild guild) {
        return playerFastestGame(guild, 5, 1, Integer.MAX_VALUE);
    }

    public static String playerFastestGame(Guild guild, int minGames, int minTurns, int maxGames) {
        GRList grList = gatherGameResults(guild).grList;
        Set<String> players = getAllPlayers(grList);
        List<Pair<String, Integer>> playerAverageDuration = new ArrayList<>();
        int overallTotalDuration = 0;
        int overallTotalTurns = 0;
        for (String playerName : players) {
            int totalDuration = 0;
            int totalTurns = 0;
            List<GameResult> playersGames = new ArrayList<>(grList.gameResults.stream().filter(gr -> gr.getTurn() >= minTurns).filter(gr -> gr.isPlayer(playerName)).toList());
            if (playersGames.size() < minGames)
                continue;
            playersGames.sort((a, b) -> Float.compare((float) Integer.parseInt(a.getGameDuration()) / a.getTurn(), (float) Integer.parseInt(b.getGameDuration()) / b.getTurn()));
            for (GameResult gr : playersGames.subList(0, Math.min(playersGames.size(), maxGames))) {
                int duration = Integer.parseInt(gr.getGameDuration());
                int numTurns = gr.getTurn();
                overallTotalDuration += duration;
                overallTotalTurns += numTurns;
                totalDuration += duration;
                totalTurns += numTurns;
            }
            playerAverageDuration.add(new ImmutablePair<>(playerName, totalDuration * 10 / totalTurns));
        }
        String overallAverage = "Fastest " + minGames + " games with minimum " + minTurns + " turns by average days per turn\n";
        if (maxGames == Integer.MAX_VALUE)
            overallAverage = new DecimalFormat("#0.0").format((float)overallTotalDuration/overallTotalTurns) + " days per turn - " + "Overall average\n(Minimum " + minGames + " games played to be in list below)\n";
        Comparator<Pair<String, Integer>> numGamesComparator = Comparator.comparingInt(Pair::getRight);
        playerAverageDuration.sort(numGamesComparator);
        StringBuilder response = new StringBuilder();
        response.append(overallAverage);
        for (Pair<String, Integer> pair : playerAverageDuration)
            response.append(new DecimalFormat("#0.0").format((float)pair.getRight()/10)).append(" ").append(pair.getLeft()).append("\n");
        return response.toString();
    }

    private static String playerRecord(Guild guild, String playerName, String playerTag) {
        GRList gameResults = gatherGameResults(guild).grList;
        return getPlayerRecord(gameResults, "@" + playerName).publish(playerTag, guild);
    }

    public static void gameResult(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        OptionMapping optionMapping = event.getOption(faction.getName());
        if (optionMapping == null)
            throw new IllegalArgumentException("There must be at least on winner for this command.");
        String winner1Name = optionMapping.getAsString();
        String winner2Name = null;
        optionMapping = event.getOption(otherWinnerFaction.getName());
        if (optionMapping != null)
            winner2Name = optionMapping.getAsString();
        int totalSpecials = 0;
        boolean guildSpecial = false;
        optionMapping = event.getOption(guildSpecialWin.getName());
        if (optionMapping != null) {
            guildSpecial = optionMapping.getAsBoolean();
            totalSpecials++;
        }
        boolean fremenSpecial = false;
        optionMapping = event.getOption(fremenSpecialWin.getName());
        if (optionMapping != null) {
            fremenSpecial = optionMapping.getAsBoolean();
            totalSpecials++;
        }
        boolean bgPrediction = false;
        optionMapping = event.getOption(bgPredictionWin.getName());
        if (optionMapping != null) {
            bgPrediction = optionMapping.getAsBoolean();
            totalSpecials++;
        }
        boolean ecazAllyOccupy = false;
        optionMapping = event.getOption(ecazOccupyWin.getName());
        if (optionMapping != null) {
            ecazAllyOccupy = optionMapping.getAsBoolean();
            totalSpecials++;
        }
        if (totalSpecials > 1)
            throw new InvalidGameStateException("There can only be 1 special victory type.");


        String result = "__" + discordGame.getGameCategory().getName() + "__\n";
        result += "> " + discordGame.getTextChannel("front-of-shield").getJumpUrl() + "\n";
        result += "Moderator:\n";
        String modCommandsExecuted = "";
        result += "> " + game.getMod() + "\n";
        if (game.isTeamMod()) {
            List<Map.Entry<String, Integer>> list = new ArrayList<>(game.getModCommandExecutions().entrySet());
            list.sort(Map.Entry.comparingByValue());
            list.removeIf(e -> e.getKey().equals(game.getMod()));
            result += String.join("", list.reversed().stream().map(e -> "> " + e.getKey() + "\n").toList());
            modCommandsExecuted = String.join("", list.reversed().stream().map(e -> "\n" + e.getValue() + " - " + e.getKey()).toList());
            if (!game.getModCommandExecutions().containsKey(game.getMod()))
                result += "> " + game.getMod() + "\n";
        }
        result += "Factions:\n";
        StringBuilder factions = new StringBuilder();
        for (Faction f : game.getFactions()) {
            factions.append("> ").append(f.getEmoji()).append(" - ").append(f.getPlayer()).append("\n");
        }
        result += factions;
        result += "Winner: Turn " + game.getTurn();
        if (guildSpecial)
            result += " - " + Emojis.getFactionEmoji("Guild") + " special victory condition";
        else if (fremenSpecial)
            result += " - " + Emojis.getFactionEmoji("Fremen") + " special victory condition";
        else if (bgPrediction)
            result += " - " + Emojis.getFactionEmoji("BG") + " predicted " + Emojis.getFactionEmoji(game.getBGFaction().getPredictionFactionName());
        else if (ecazAllyOccupy)
            result += " - " + Emojis.getFactionEmoji("Ecaz") + Emojis.getFactionEmoji(Objects.requireNonNull(winner2Name)) + " co-occupied 3 strongholds";
        result += "\n";
        result += "> " + Emojis.getFactionEmoji(winner1Name) + " - " + game.getFaction(winner1Name).getPlayer() + "\n";
        if (winner2Name != null)
            result += "> " + Emojis.getFactionEmoji(winner2Name) + " - " + game.getFaction(winner2Name).getPlayer() + "\n";
        result += "Summary:\n";
        result += "> Edit this text to add a summary, or remove the Summary section if you do not wish to include one.";
//        if (game.isTeamMod() && !modCommandsExecuted.isEmpty())
//            result += "\n\nCommand executions by each mod team member (data capture started Oct. 27):" + modCommandsExecuted;
        result += "\n\n(Use the Discord menu item Copy Text to retain formatting when pasting.)";
        discordGame.getModInfo().queueMessage(result);
    }

    public static Category getStatsCategory(Guild guild) {
        List<Category> categories = Objects.requireNonNull(guild).getCategories();
        // Try Dune Statistics category ID from Dune: Play by Discord server first
        Category category = guild.getCategoryById("1148142189931679765");
        if (category == null)
            // Next try category ID from Tom's test server
            category = guild.getCategoryById("1164949164313022484");
        if (category == null)
            // Then look for a category that has "Dune Statistics" in the name
            category = categories.stream().filter(c -> c.getName().contains("Dune Statistics")).findFirst().orElse(null);
        if (category == null)
            throw new IllegalStateException("The Dune Statistics category was not found.");
        return category;
    }

    /**
     * Saves game-related bot data from Discord into a compressed JSONL format and uploads it to a Discord thread.
     *
     * @param event the SlashCommandInteractionEvent associated with the command execution
     * @param discordGame the DiscordGame instance containing details about the game, channels, and relevant operations
     * @param game the Game object representing the state or context of the game for which the data is being saved
     * @throws ChannelNotFoundException if the required Discord channel cannot be found
     * @throws ExecutionException if an error occurs during asynchronous operation execution
     * @throws InterruptedException if the operation is interrupted while waiting for completion
     * @throws IOException if an I/O error occurs during data processing or compression
     */
    public static void saveGameBotData(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, ExecutionException, InterruptedException, IOException {
        TextChannel botDataChannel = discordGame.getBotDataChannel();

        StringBuilder allBotData = new StringBuilder();

        String lastMessageId = null;

        while (true) {
            List<Message> messages = (lastMessageId == null)
                    ? botDataChannel.getHistory().retrievePast(100).complete() // First batch
                    : botDataChannel.getHistoryBefore(lastMessageId, 100).complete().getRetrievedHistory();

            if (messages.isEmpty()) {
                break;
            }

            for (Message message : messages) {
                CompletableFuture<InputStream> future = message.getAttachments().getFirst().getProxy().download();
                String botData = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
                future.get().close();

                String messageTimestamp = message.getTimeCreated().toString();
                String messageText = message.getContentRaw();
                String user = message.getAuthor().getName();

                String botDataBase64 = Base64.getEncoder().encodeToString(botData.getBytes());

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("user", user);
                jsonObject.addProperty("timestamp", messageTimestamp);
                jsonObject.addProperty("text", messageText);
                jsonObject.addProperty("botDataBase64", botDataBase64);

                allBotData.append(jsonObject.toString().replaceAll("\\n", ""));
                allBotData.append("\n");
            }

            lastMessageId = messages.getLast().getId();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream gzip = new GZIPOutputStream(baos);

        gzip.write(allBotData.toString().getBytes(StandardCharsets.UTF_8));
        gzip.close();
        FileUpload fileUpload = FileUpload.fromData(baos.toByteArray(), "gamestate-aggregate.jsonl.gz");

        TextChannel frontOfShield = discordGame.getTextChannel("front-of-shield");
        discordGame.createPublicThread(frontOfShield, "bot-data", null);

        ThreadChannel channel = discordGame.getThreadChannel("front-of-shield", "bot-data");

        channel.sendMessage(LocalDateTime.now().toString()).addFiles(fileUpload).complete();
    }
}
