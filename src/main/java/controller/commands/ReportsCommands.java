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
import model.factions.BGFaction;
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
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.MutableTriple;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
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

import static controller.commands.CommandOptions.*;

public class ReportsCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("reports", "Commands for statistics about Dune: Play by Discord games.").addSubcommands(
                new SubcommandData("games-per-player", "Show the games each player is in listing those on waiting list first.").addOptions(months),
                new SubcommandData("active-games", "Show active games with turn, phase, and subphase.").addOptions(showFactions),
                new SubcommandData("player-record", "Show the overall per faction record for the player").addOptions(user),
                new SubcommandData("played-all-original-six", "Who has played all original six factions?"),
                new SubcommandData("played-all-expansion", "Who has played all six expansion factions?"),
                new SubcommandData("played-all-twelve", "Who has played all twelve factions?"),
                new SubcommandData("won-as-all-original-six", "Who has won with all original six factions?"),
                new SubcommandData("won-as-all-expansion", "Who has won with all six expansion factions?"),
                new SubcommandData("high-faction-plays", "Which player-faction combos have occurred the most?"),
                new SubcommandData("longest-games", "The 10 longest games and the tortured mod.")
        ));

        return commandData;
    }

    public static String runCommand(SlashCommandInteractionEvent event, List<Member> members) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        String responseMessage = "";
        switch (name) {
            case "games-per-player" -> responseMessage = gamesPerPlayer(event, members);
            case "active-games" -> responseMessage = activeGames(event);
            case "player-record" -> responseMessage = playerRecord(event);
            case "played-all-original-six" -> responseMessage = playedAllOriginalSix(event.getGuild(), members);
            case "played-all-expansion" -> responseMessage = playedAllExpansion(event.getGuild(), members);
            case "played-all-twelve" -> responseMessage = playedAllTwelve(event.getGuild(), members);
            case "won-as-all-original-six" -> responseMessage = wonAsAllOriginalSix(event.getGuild(), members);
            case "won-as-all-expansion" -> responseMessage = wonAsAllExpansion(event.getGuild(), members);
            case "high-faction-plays" -> responseMessage = highFactionPlays(event.getGuild(), members);
            case "longest-games" -> responseMessage = longestGames(event.getGuild(), members);
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

    private static String getPlayerMention(String playerName, List<Member> members) {
        return members.stream().filter(member -> playerName.equals("@" + member.getUser().getName())).findFirst().map(member -> member.getUser().getAsMention()).orElse(playerName);
    }

    private static void addRecentlyFinishedPlayers(List<Member> members, GameResults gameResults, HashMap<String, List<String>> playerGamesMap, int monthsAgo) {
        HashSet<String> filteredPlayers = getAllPlayers(gameResults.grList, monthsAgo);
        for (String player : filteredPlayers) {
            String playerTag = getPlayerMention(player, members);
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
            if (categoryName.equalsIgnoreCase("staging area")) {
                addWaitingListPlayers(playerGamesMap, category);
            } else if (categoryName.equalsIgnoreCase("dune statistics")) {
                addRecentlyFinishedPlayers(members, gatherGameResults(guild), playerGamesMap, monthsAgo);
            } else {
                addGamePlayers(playerGamesMap, category, categoryName);
            }
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

    public static String activeGame(Guild guild, Game game, String categoryName, boolean showFactionsinGames) throws InvalidGameStateException {
        StringBuilder response = new StringBuilder();
        response.append(categoryName).append("\nTurn ").append(game.getTurn()).append(", ");
        if (game.getTurn() != 0) {
            int phase = game.getPhaseForTracker();
            response.append(phaseName(phase));
            if (phase == 4) {
                Bidding bidding = game.getBidding();
                response.append(", Card ").append(bidding.getBidCardNumber()).append(" of ").append(bidding.getNumCardsForBid());
            } else if (phase == 6) {
                int factionsLeftToGo = game.getTurnOrder().size();
                if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild"))
                    factionsLeftToGo++;
                response.append(", ").append(factionsLeftToGo).append(" factions remaining");
            }
        }
        response.append("\nMod: ").append(game.getMod());
        if (showFactionsinGames) {
            response.append("\nFactions: ");
            for (Faction f: game.getFactions()) {
                String factionEmojiName = f.getEmoji().substring(1, f.getEmoji().length() - 1);
                RichCustomEmoji emoji = guild.getEmojis().stream().filter(e -> e.getName().equals(factionEmojiName)).findFirst().orElse(null);
                if (emoji == null)
                    response.append(f.getEmoji());
                else
                    response.append("<").append(f.getEmoji()).append(emoji.getId()).append(">");
            }
        }
        return response.toString();
    }

    public static String activeGames(SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = event.getOption(showFactions.getName());
        boolean showFactionsinGames = (optionMapping != null && optionMapping.getAsBoolean());
        StringBuilder response = new StringBuilder();
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        for (Category category : categories) {
            String categoryName = category.getName();
            try {
                DiscordGame discordGame = new DiscordGame(category, false);
                Game game = discordGame.getGame();
                response.append(activeGame(event.getGuild(), game, categoryName, showFactionsinGames));
                response.append("\n\n");
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        return response.toString();
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
        return players;
    }

    public static String writePlayerStats(GRList gameResults, List<Member> members) {
        Set<String> players = getAllPlayers(gameResults);
        List<PlayerPerformance> allPlayerPerformance = new ArrayList<>();
        for (String player : players) {
            allPlayerPerformance.add(playerPerformance(gameResults, player));
        }
        allPlayerPerformance.sort((a, b) -> a.numWins == b.numWins ? a.numGames - b.numGames : b.numWins - a.numWins);
        StringBuilder playerStatsString = new StringBuilder("__Supreme Ruler of Arrakis__");
        for (PlayerPerformance pp : allPlayerPerformance) {
            String winPercentage = new DecimalFormat("#0.0%").format(pp.winPercentage);
            int tensDigit = pp.numWins % 100 / 10;
            String tensEmoji = tensDigit == 0 ? ":black_small_square:" : numberBoxes.get(tensDigit);
            int onesDigit = pp.numWins % 10;
            String player = getPlayerMention(pp.playerName, members);
            playerStatsString.append("\n").append(tensEmoji).append(numberBoxes.get(onesDigit)).append(" - ").append(player).append(" - ").append(winPercentage).append(" (").append(pp.numWins).append("/").append(pp.numGames).append(")");
        }
        return playerStatsString.toString();
    }

    private static class PlayerPerformance {
        String playerName;
        int numGames;
        int numWins;
        float winPercentage;

        public PlayerPerformance(String playerName, int numGames, int numWins, float winPercentage) {
            this.playerName = playerName;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
        }
    }

    private static int playerFactionGames(GRList gameResults, String playerName, String factionName) {
        return gameResults.gameResults.stream()
                .filter(gr -> {
                    String p = gr.getFieldValue(factionName);
                    return p != null && p.equals(playerName);
                })
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
                + gameResults.gameResults.stream().filter(gr -> gr.getRichese() != null && gr.getRichese().equals(playerName)).toList().size();
        int numWins = gameResults.gameResults.stream().filter(gr -> gr.getWinner1Player() != null && gr.getWinner1Player().equals(playerName)).toList().size()
                + gameResults.gameResults.stream().filter(gr -> gr.getWinner2Player() != null && gr.getWinner2Player().equals(playerName)).toList().size();
        float winPercentage = numWins/(float)numGames;
        return new PlayerPerformance(playerName, numGames, numWins, winPercentage);
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
            String moderator = getPlayerMention(p.getLeft(), members);
            int totalTurns = p.getRight().stream().mapToInt(GameResult::getTurn).sum();
            float averageTurns = (float) totalTurns / p.getRight().size();
            modAndAverageTurns.add(new ImmutablePair<>(moderator, averageTurns));
            moderatorsString.append("\n").append(p.getRight().size()).append(" - ").append(moderator);
            int maxWins = 0;
            List<String> emojis = new ArrayList<>();
            for (String fn : factionNames) {
                int fnWins = p.getRight().stream().filter(gr -> gr.isFactionWinner(fn)).toList().size();
                if (fnWins > maxWins) {
                    emojis = new ArrayList<>();
                    maxWins = fnWins;
                }
                if (fnWins == maxWins)
                    emojis.add(Emojis.getFactionEmoji(fn));
            }
            modAndMaxFactionWins.add(MutableTriple.of(moderator, maxWins, emojis));
        }

//        modAndAverageTurns.sort((a, b) -> Float.compare(b.getRight(), a.getRight()));
//        moderatorsString.append("\n\n__Average number of turns__");
//        for (Pair<String, Float> p : modAndAverageTurns) {
//            moderatorsString.append("\n").append(new DecimalFormat("#0.0").format(p.getRight())).append(" - ").append(p.getLeft());
//        }
        moderatorsString.append("\n\n__Most frequent faction winners for each mod__");
        for (MutableTriple<String, Integer, List<String>> p : modAndMaxFactionWins)
            moderatorsString.append("\n").append(p.getMiddle()).append(" - ").append(tagEmojis(guild, String.join(" ", p.getRight()))).append(" - ").append(p.getLeft());
        return moderatorsString.toString();
    }

    private static int longerGame(GameResult a, GameResult b) {
        int aDuration = a.getGameDuration() == null ? 0 : Integer.parseInt(a.getGameDuration());
        int bDuration = b.getGameDuration() == null ? 0 : Integer.parseInt(b.getGameDuration());
        return Integer.compare(bDuration, aDuration);
    }

    public static String longestGames(GRList gameResults, List<Member> members) {
        List<GameResult> longestGames = gameResults.gameResults.stream().sorted(ReportsCommands::longerGame).toList();
        StringBuilder longestGamesString = new StringBuilder("__Longest games__ (The tortured mod stat! :grin:)");
        for (int i = 0; i < 10; i++) {
            GameResult result = longestGames.get(i);
            longestGamesString.append("\n").append(result.getGameDuration()).append(" days, ");
            longestGamesString.append(result.getGameName()).append(", ");
            String moderator = getPlayerMention(result.getModerator(), members);
            longestGamesString.append(moderator);
        }
        return longestGamesString.toString();
    }

    public static String longestGames(Guild guild, List<Member> members) {
        GameResults gameResults = gatherGameResults(guild);
        return longestGames(gameResults.grList, members);
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

    public static String updateStats(Guild guild, JDA jda, boolean loadNewGames, List<Member> members, boolean publishIfNoNewGames) {
        GameResults gameResults = gatherGameResults(guild);
        int numNewGames = 0;
        if (loadNewGames) {
            numNewGames = loadNewGames(guild, jda, gameResults.grList);
            if (numNewGames > 0 || publishIfNoNewGames)
                publishStats(guild, gameResults.grList, members, numNewGames > 0);
        }
//        getPlayerRecord(gameResults.grList, "@tomhomebrew");
        return numNewGames + " new games were added to parsed results.";
    }

    public static String updateStats(SlashCommandInteractionEvent event, List<Member> members) {
        OptionMapping optionMapping = event.getOption(forcePublish.getName());
        boolean publishIfNoNewGames = (optionMapping != null && optionMapping.getAsBoolean());
        return updateStats(event.getGuild(), event.getJDA(), true, members, publishIfNoNewGames);
    }

    public static String writeFactionStats(Guild guild, GRList gameResults) {
//        System.out.println(updateFactionPerformance(guild, gameResults).length());
//        System.out.println(updateTurnStats(guild, gameResults).length());
//        System.out.println(soloVictories(guild, gameResults).length());
        return updateFactionPerformance(guild, gameResults);
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
                .filter(jo -> jo.getWinner1Faction().equals(factionName) || jo.getWinner2Faction() != null && jo.getWinner2Faction().equals(factionName))
                .toList();
        int numWins = gamesWithFactionWin.size();
        int totalWinsTurns = gamesWithFactionWin.stream().mapToInt(GameResult::getTurn).sum();
        float winPercentage = numWins/(float)numGames;
        float averageTurns = totalTurns/(float)numGames;
        float averageWinsTurns = totalWinsTurns/(float)numWins;
        return new FactionPerformance(factionEmoji, numGames, numWins, winPercentage, averageTurns, averageWinsTurns);
    }

    public static String updateFactionPerformance(Guild guild, GRList gameResults) {
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
        allFactionPerformance.sort((a, b) -> Float.compare(b.winPercentage, a.winPercentage));
        StringBuilder factionStatsString = new StringBuilder("__Factions__");
        for (FactionPerformance fs : allFactionPerformance) {
            String winPercentage = new DecimalFormat("#0.0%").format(fs.winPercentage);
            factionStatsString.append("\n").append(fs.factionEmoji).append(" ").append(winPercentage).append(" - ").append(fs.numWins).append("/").append(fs.numGames)
                    ;//.append(", Average number of turns with faction win = ").append(fs.averageWinsTurns);
        }
        factionStatsString.append("\n\n__Average Turns with Faction__ (includes " + Emojis.GUILD + " and " + Emojis.FREMEN + " special victories as 10 turns)");
        for (FactionPerformance fs : allFactionPerformance) {
            String averageTurns = new DecimalFormat("#0.0").format(fs.averageTurns);
            String averageTurnsWins = new DecimalFormat("#0.0").format(fs.averageWinsTurns);
            factionStatsString.append("\n").append(fs.factionEmoji).append(" ").append(averageTurns).append(" per game, ").append(averageTurnsWins).append(" per win");
        }
        return tagEmojis(guild, factionStatsString.toString());
    }

    public static String writeFactionAllyPerformance(Guild guild, GRList gameResults) {
        List<MutableTriple<String, String, Integer>> factionAllyWinsTriple = new ArrayList<>();
        for (String name1 : factionNames) {
            boolean foundNewName = false;
            for (String name2 : factionNames) {
                if (name1.equals(name2)) foundNewName = true;
                else if (foundNewName) {
                    int factionAllyWins = gameResults.gameResults.stream()
                            .filter(gr -> gr.isFactionWinner(name1))
                            .filter(gr -> gr.isFactionWinner(name2))
                            .toList().size();
                    if (factionAllyWins > 0)
                        factionAllyWinsTriple.add(MutableTriple.of(name1, name2, factionAllyWins));
                }
            }
        }
        factionAllyWinsTriple.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));
        StringBuilder result = new StringBuilder("__Most Faction Alliance Wins__\n");
        int lines = 0;
        int currentWins = Integer.MAX_VALUE;
        for (MutableTriple<String, String, Integer> fawt : factionAllyWinsTriple) {
            if (fawt.getRight() != currentWins && lines > 5)
                break;
            result.append(tagEmojis(guild, fawt.getRight() + " - " + Emojis.getFactionEmoji(capitalize(fawt.getLeft())) + " " + Emojis.getFactionEmoji(capitalize(fawt.getMiddle())) + "\n"));
            currentWins = fawt.getRight();
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
            int turnWins = gameResults.gameResults.stream().filter(gr -> gr.getTurn() == finalI).toList().size();
            if (turnWins > maxTurnWins)
                maxTurnWins = turnWins;
        }
        StringBuilder response = new StringBuilder("__Turns Histogram__`\n");
        for (int j = maxTurnWins; j >= 1; j--) {
            StringBuilder responseRow = new StringBuilder(" ");
            for (int i = 1; i <= 10; i++) {
                int finalI = i;
                responseRow.append(gameResults.gameResults.stream().filter(gr -> gr.getTurn() == finalI).toList().size() >= j ? "║  " : "   ");
            }
            response.append(responseRow).append("\n");
        }
        response.append(" 1  2  3  4  5  6  7  8  9 10`");
        return response.toString();
    }

    private static String soloVictories(Guild guild, GRList gameResults) {
        int numGames = gameResults.gameResults.size();
        List<GameResult> soloWinGames = gameResults.gameResults.stream()
                .filter(gr -> gr.getWinner2Faction() == null).toList();
        int numWins = soloWinGames.size();
        String winPercentage = new DecimalFormat("#0.0%").format(numWins / (float) numGames);
        StringBuilder response = new StringBuilder("__Solo Victories__\n" + winPercentage + " - " + numWins + "/" + numGames);
        List<Pair<String, Integer>> factionsSoloWins = new ArrayList<>();
        for (String factionName : factionNames) {
            int factionSoloWins = soloWinGames.stream().filter(gr -> gr.getWinner1Faction().equals(factionName)).toList().size();
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
            return tagEmojis(guild, returnString);
        }
    }

    private static PlayerRecord getPlayerRecord(GRList gameResults, String playerName) {
        PlayerRecord pr = new PlayerRecord();
        for (GameResult gameResult : gameResults.gameResults) {
            boolean winner = false;
            if (gameResult.isPlayer(playerName)) pr.games++;
            if (gameResult.isWinner(playerName)) {
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
            }
        }
        return pr;
    }

    private static final Pattern taggedEmojis = Pattern.compile("<:([a-zA-Z0-9_]+):\\d+>");
    private static final Pattern untaggedEmojis = Pattern.compile("(?<!<):([a-zA-Z0-9_]+):(?!\\d+>)");
    private static final Pattern playerTags = Pattern.compile("<@([a-zA-Z0-9_]+)>");
    private static final Pattern turn = Pattern.compile(".*Turn ([0-9]+)");

    private static String capitalize(String strippedEmoji) {
        if (strippedEmoji == null || strippedEmoji.isEmpty())
            return "";
        if (strippedEmoji.equals("rich"))
            return "richese";
        return strippedEmoji;
    }

    private static void publishStats(Guild guild, GRList grList, List<Member> members, boolean hasNewGames) {
        Category category = getStatsCategory(guild);
        TextChannel playerStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("player-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The player-stats channel was not found."));
        ThreadChannel parsedResults = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("parsed-results")).findFirst().orElseThrow(() -> new IllegalStateException("The parsed-results thread was not found."));

        TextChannel factionStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("faction-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The faction-stats channel was not found."));
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(factionStatsChannel).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        factionStatsChannel.sendMessage(writeFactionStats(guild, grList)).queue();
        factionStatsChannel.sendMessage(updateTurnStats(guild, grList)).queue();
        factionStatsChannel.sendMessage(turnsHistogram(grList)).queue();
        factionStatsChannel.sendMessage(soloVictories(guild, grList)).queue();
        factionStatsChannel.sendMessage(writeFactionAllyPerformance(guild, grList)).queue();

        TextChannel moderatorStats = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("moderator-stats")).findFirst().orElseThrow(() -> new IllegalStateException("The moderator-stats channel was not found."));
        messageHistory = MessageHistory.getHistoryFromBeginning(moderatorStats).complete();
        messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        moderatorStats.sendMessage(writeModeratorStats(guild, grList, members)).queue();

        messageHistory = MessageHistory.getHistoryFromBeginning(playerStatsChannel).complete();
        messages = messageHistory.getRetrievedHistory();
        messages.forEach(msg -> msg.delete().queue());
        StringBuilder playerStatsString = new StringBuilder();
        String[] playerStatsLines = writePlayerStats(grList, members).split("\n");
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
        playerStatsChannel.sendMessage(writePlayerAllyPerformance(grList, members)).queue();
//        String factionPlays = highFactionPlays(guild, grList, members);
//        if (!factionPlays.isEmpty())
//            playerStatsChannel.sendMessage("__High Faction Plays__\n" + factionPlays).queue();
        playerStatsChannel.sendMessage("__Won as All Original 6 Factions__\n" + wonAsAllOriginalSix(guild, members)).queue();
//        playerStatsChannel.sendMessage("__Played All 12 Factions__\n" + playedAllTwelve(guild, members)).queue();

        FileUpload fileUpload;
        fileUpload = FileUpload.fromData(
                grList.generateCSV().getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.csv"
        );
//        parsedResults.sendFiles(fileUpload).complete();
        TextChannel statsDiscussionChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("stats-discussion")).findFirst().orElseThrow(() -> new IllegalStateException("The stats-discussion channel was not found."));
        statsDiscussionChannel.sendFiles(fileUpload).queue();

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
        fileUpload = FileUpload.fromData(
                gson.toJson(grList).getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.json"
        );
        parsedResults.sendFiles(fileUpload).complete();
        statsDiscussionChannel.sendFiles(fileUpload).queue();
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

    public static String publishFactionStats(Guild guild, JDA jda) {
        GameResults gameResults = gatherGameResults(guild);
        int numNewGames = loadNewGames(guild, jda, gameResults.grList);
        System.out.println("Num new games = " + numNewGames);
        return writeFactionStats(guild, gameResults.grList);
    }

    public static String compareReportsMethod(Guild guild, JDA jda) {
        GameResults gameResultsOld = gatherGameResults(guild);
        int numNewGames = loadNewGames(guild, jda, gameResultsOld.grList);
        System.out.println("New games = " + numNewGames);
        String oldResultF = writeFactionStats(guild, gameResultsOld.grList);
        String oldResultM = writeModeratorStats(guild, gameResultsOld.grList, List.of());
        String oldResultP = writePlayerStats(gameResultsOld.grList, List.of());
        String oldResultH = highFactionPlays(guild, gameResultsOld.grList, List.of());
        GameResults gameResults = gatherGameResults(guild);
        numNewGames = loadNewGames(guild, jda, gameResults.grList);
        System.out.println("New games = " + numNewGames);
        String newResultF = writeFactionStats(guild, gameResults.grList);
        String newResultM = writeModeratorStats(guild, gameResults.grList, List.of());
        String newResultP = writePlayerStats(gameResults.grList, List.of());
        String newResultH = highFactionPlays(guild, gameResults.grList, List.of());
        String result = "";
        if (gameResultsOld.grList.gameResults.size() == gameResults.grList.gameResults.size()) {
            result = "Gameresults are same size " + gameResults.grList.gameResults.size() + "\n";
            boolean allMatch = true;
            for (int i = 0; i < gameResults.grList.gameResults.size(); i++)
                if (!gameResults.grList.gameResults.get(i).equals(gameResultsOld.grList.gameResults.get(i))) {
                    allMatch = false;
                    break;
                }
            result += "Do all GRs match? " + allMatch + "\n";
        }
        return result + "Does Faction match? " + oldResultF.equals(newResultF)
                + "\nDoes Moderator match? " + oldResultM.equals(newResultM)
                + "\nDoes Player match? " + oldResultP.equals(newResultP)
                + "\nDoes High Faction match? " + oldResultH.equals(newResultH);
//        return oldResult + "\n\n" + newResult + "\n\n" + "Do they match? " + newResult.equals(oldResult);
//        StringBuilder response = new StringBuilder();
//        boolean allMatch = true;
//        for (String player : getAllPlayers(gameResults.grList)) {
//            if (player.indexOf("@") == 0)
//                player = player.substring(1);
//            String oldResult = playerRecord(guild, jda, player, player);
//            String newResult = playerRecord(guild, jda, player, player);
//            response.append(player).append(" ").append(oldResult).append(" ").append(newResult).append("\n");
//            if (!oldResult.equals(newResult))
//                allMatch = false;
//        }
//        response.append("All match: ").append(allMatch);
//        return response.toString();
//        String oldResult = averageDaysPerTurn(guild, jda);
//        String adptNew = averageDaysPerTurn(guild, jda);
//        return oldResult + "\n\n" + adptNew + "\n\nAre the Strings the same? " + oldResult.equals(adptNew);
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
                if (channelIdStart != -1) {
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
                switch (gameName) {
                    case "Dune 22b" -> {
                        channelString = "952976095697829918";
                        foundChannelId = true;
                    }
                    case "Dune 22c" -> {
                        channelString = "962367250101325975";
                        foundChannelId = true;
                    }
                    case "Discord 4" -> {
                        channelString = "996300327856906280";
                        foundChannelId = true;
                    }
                    case "Discord 23" -> {
                        channelString = "1097244553947389992";
                        foundChannelId = true;
                    }
                }
                if (foundChannelId) {
                    try {
                        TextChannel posts = Objects.requireNonNull(guild).getTextChannelById(channelString);
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
                                    gr.setDaysUntilArchive("" + archiveDate.datesUntil(endDate).count());
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
            String modString = raw.substring(modStart, factionsStart);
            if (!modString.isEmpty()) {
                lines = modString.split("\n");
                Matcher modMatcher = playerTags.matcher(modString);
                if (modMatcher.find())
                    moderator = "@" + jda.retrieveUserById(modMatcher.group(1)).complete().getName();
                else
                    moderator = lines[1].substring(2).split("\\s+", 2)[0];
            } else if (gameName.equals("Discord 27"))
                moderator = "@voiceofonecrying";
            gr.setModerator(moderator);

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
            else if (gameName.equals("Discord 26"))
                victoryType = "F";
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
            gr.setWinner1Faction(winner1Faction);
            if (!winner2Faction.isEmpty()) {
                gr.setWinner2Faction(winner2Faction);
            }
            if (!predictedFaction.isEmpty()) {
                gr.setPredictedFaction(predictedFaction);
            }

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
                Matcher matcher2 = playerTags.matcher(s.substring(matcherThatFoundEmoji.end()));
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
            gr.setWinner1Player(gr.getFieldValue(winner1Faction));
            gr.setWinner2Player(gr.getFieldValue(winner2Faction));
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
        if (!grList.gameResults.isEmpty()) {
            if (grList.gameResults.getFirst().getGameName().equals("Dune 8"))
                lastMessageID = grList.gameResults.getLast().getMessageID();
            else
                lastMessageID = grList.gameResults.getFirst().getMessageID();
        }

        MessageHistory messageHistory;
        if (lastMessageID.isEmpty())
            messageHistory = MessageHistory.getHistoryFromBeginning(gameResults).limit(10).complete();
        else
            messageHistory = MessageHistory.getHistoryAfter(gameResults, lastMessageID).limit(10).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();
        List<GameResult> newGRs = messages.stream().map(m -> loadNewGame(guild, jda, m)).filter(Objects::nonNull).collect(Collectors.toList());
        int numNewGames = newGRs.size();
        if (grList.gameResults.getFirst().getGameName().equals("Dune 8"))
            Collections.reverse(grList.gameResults);
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

    public static String playedAllOriginalSix(Guild guild, List<Member> members) {
        List<String> factions = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen");
        return playedFactions(guild, factions, members);
    }

    public static String playedAllExpansion(Guild guild, List<Member> members) {
        List<String> factions = List.of("bt", "choam", "ecaz", "ix", "moritani", "richese");
        return playedFactions(guild, factions, members);
    }

    public static String playedAllTwelve(Guild guild, List<Member> members) {
        return playedFactions(guild, factionNames, members);
    }

    public static String playedFactions(Guild guild, List<String> factions, List<Member> members) {
        GRList gameResults = gatherGameResults(guild).grList;
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

            String playerTag = getPlayerMention(playerName, members);
            if (factionsPlayed == listSize)
                playedAll.append(playerTag).append(" has played all ").append(listSize).append(".\n");
            else if (listSize > 1 && factionsPlayed == listSize - 1)
                missingOne.append(playerTag).append(" has played as ").append(listSize - 1).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (listSize > 2 && factionsPlayed == listSize - 2)
                missingTwo.append(playerTag).append(" has played as ").append(listSize - 2).append(", missing only ").append(missedFactionEmojis).append("\n");
        }
        if (playedAll.isEmpty() && missingOne.isEmpty())
            return "No players have played all " + listSize + " factions.";
        return tagEmojis(guild, playedAll + missingOne.toString());
    }

    public static String wonAsAllOriginalSix(Guild guild, List<Member> members) {
        List<String> factions = List.of("atreides", "bg", "emperor", "fremen", "guild", "harkonnen");
        return wonAsFactions(guild, factions, members);
    }

    public static String wonAsAllExpansion(Guild guild, List<Member> members) {
        List<String> factions = List.of("bt", "choam", "ecaz", "ix", "moritani", "richese");
        return wonAsFactions(guild, factions, members);
    }

    public static String wonAsFactions(Guild guild, List<String> factions, List<Member> members) {
        GRList gameResults = gatherGameResults(guild).grList;
        int listSize = factions.size();
        StringBuilder playedAll = new StringBuilder();
        StringBuilder missingOne = new StringBuilder();
        StringBuilder missingTwo = new StringBuilder();
        for (String playerName : getAllPlayers(gameResults)) {
            int factionsPlayed = 0;
            StringBuilder missedFactionEmojis = new StringBuilder();
            factionsPlayed += (int) factions.stream().map(factionName -> gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinner(playerName)).toList()).filter(grs -> !grs.isEmpty()).count();
            factions.forEach(factionName -> {
                if (gameResults.gameResults.stream().filter(gr -> gr.isFactionPlayer(factionName, playerName) && gr.isWinner(playerName)).toList().isEmpty())
                    missedFactionEmojis.append(Emojis.getFactionEmoji(factionName)).append(" ");
            });

            String playerTag = getPlayerMention(playerName, members);
            if (factionsPlayed == listSize)
                playedAll.append(playerTag).append(" has won as all ").append(listSize).append(".\n");
            else if (listSize > 1 && factionsPlayed == listSize - 1)
                missingOne.append(playerTag).append(" has won as ").append(listSize - 1).append(", missing only ").append(missedFactionEmojis).append("\n");
            else if (listSize > 2 && factionsPlayed == listSize - 2)
                missingTwo.append(playerTag).append(" has won as ").append(listSize - 2).append(", missing only ").append(missedFactionEmojis).append("\n");
        }
        if (playedAll.isEmpty() && missingOne.isEmpty())
            return "No players have won with all " + listSize + " factions.";
        return tagEmojis(guild, playedAll + missingOne.toString());
    }

    private static final List<String> factionNamesCapitalized = List.of("Atreides", "BG", "BT", "CHOAM", "Ecaz", "Emperor", "Fremen", "Guild", "Harkonnen", "Ix", "Moritani", "Richese");
    private static final List<String> factionNames = List.of("atreides", "bg", "bt", "choam", "ecaz", "emperor", "fremen", "guild", "harkonnen", "ix", "moritani", "richese");

    private static String highFactionPlays(Guild guild, List<Member> members) {
        GRList gameResults = gatherGameResults(guild).grList;
        return highFactionPlays(guild, gameResults, members);
    }

    public static String writePlayerAllyPerformance(GRList gameResults, List<Member> members) {
        List<MutableTriple<String, String, Integer>> playerAllyWinsTriple = new ArrayList<>();
        List<String> playerNames = getAllPlayers(gameResults).stream().toList();
        List<String> playerNames2 = new ArrayList<>(getAllPlayers(gameResults).stream().toList());
        for (String name1 : playerNames) {
            playerNames2.remove(name1);
            for (String name2 : playerNames2) {
                int playerAllyWins = gameResults.gameResults.stream()
                        .filter(gr -> gr.isWinner(name1))
                        .filter(gr -> gr.isWinner(name2))
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
            String tag1 = getPlayerMention(pawt.getLeft(), members);
            String tag2 = getPlayerMention(pawt.getMiddle(), members);
            result.append(pawt.getRight()).append(" - ").append(tag1).append(" ").append(tag2).append("\n");
        }
        return result.toString();
    }

    private static String highFactionPlays(Guild guild, GRList gameResults, List<Member> members) {
        Set<String> players = getAllPlayers(gameResults);
        List<MutableTriple<String, String, Integer>> playerFactionCounts = new ArrayList<>();
        for (String playerName : players) {
            for (String factionName : factionNames) {
                playerFactionCounts.add(MutableTriple.of(playerName, factionName, playerFactionGames(gameResults, playerName, factionName)));
            }
        }
        int maxGames = playerFactionCounts.stream().map(t -> t.right).mapToInt(t -> t).filter(t -> t >= 0).max().orElse(0);
        ArrayList<StringBuilder> highFactionPlays = new ArrayList<>();
        IntStream.range(0, maxGames).forEach(i -> highFactionPlays.add(new StringBuilder()));
        for (MutableTriple<String, String, Integer> t : playerFactionCounts) {
            String punct = t.right == maxGames ? "!" : "";
            if (t.right != 0) {
                String playerTag = getPlayerMention(t.left, members);
//                highFactionPlays.get(t.right - 1).append(playerTag).append(" has played ").append(Emojis.getFactionEmoji(t.middle)).append(" ").append(t.right).append(" times").append(punct);
                highFactionPlays.get(t.right - 1).append(Emojis.getFactionEmoji(t.middle)).append(" ").append(t.right).append(" times").append(punct).append(" - ").append(playerTag).append("\n");
            }
        }
        StringBuilder result = new StringBuilder();
        int lines = 0;
        int numPlays = maxGames - 1;
        while (numPlays >= 0 && lines < 6) {
            String hfpString = highFactionPlays.get(numPlays).toString();
            for (char c : hfpString.toCharArray())
                if (c == '\n')
                    lines++;
            result.append(hfpString);
            numPlays--;
        }
        if (maxGames == 0)
            return "No players have won.";
        return tagEmojis(guild, result.toString());
    }

    public static String averageDaysPerTurn(Guild guild) {
        int minGames = 5;
        GRList grList = gatherGameResults(guild).grList;
        Set<String> players = getAllPlayers(grList);
        List<Pair<String, Integer>> playerAverageDuration = new ArrayList<>();
        String overallAverage = "";
        for (String playerName : players) {
            int overallTotalDuration = 0;
            int totalDuration = 0;
            int overallTotalTurns = 0;
            int totalTurns = 0;
            int totalGames = 0;
            for (GameResult gr : grList.gameResults) {
                if (gr.getGameDuration() == null || gr.getGameDuration().isBlank())
                    continue;
                int duration = Integer.parseInt(gr.getGameDuration());
                int numTurns = gr.getTurn();
                overallTotalDuration += duration;
                overallTotalTurns += numTurns;
                if (!gr.isPlayer(playerName))
                    continue;
                totalDuration += duration;
                totalTurns += numTurns;
                totalGames++;
            }
            overallAverage = new DecimalFormat("#0.0").format((float)overallTotalDuration/overallTotalTurns) + " days per turn - " + "Overall average\n(Minimum " + minGames + " games played to be in list below)\n";
            if (totalGames < minGames)
                continue;
            playerAverageDuration.add(new ImmutablePair<>(playerName, totalDuration * 10 / totalTurns));
        }
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
        result += "Moderator:\n> " + game.getMod() + "\n";
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
            result += " - " + Emojis.getFactionEmoji("BG") + " predicted " + Emojis.getFactionEmoji(((BGFaction) game.getFaction("BG")).getPredictionFactionName());
        else if (ecazAllyOccupy)
            result += " - " + Emojis.getFactionEmoji("Ecaz") + Emojis.getFactionEmoji(Objects.requireNonNull(winner2Name)) + " co-occupied 3 strongholds";
        result += "\n";
        result += "> " + Emojis.getFactionEmoji(winner1Name) + " - " + game.getFaction(winner1Name).getPlayer() + "\n";
        if (winner2Name != null)
            result += "> " + Emojis.getFactionEmoji(winner2Name) + " - " + game.getFaction(winner2Name).getPlayer() + "\n";
        result += "Summary:\n";
        result += "> Edit this text to add a summary, or remove the Summary section if you do not wish to include one.";
        result += "\n\n(Use the Discord menu item Copy Text to retain formatting when pasting.)";
        discordGame.getModInfo().queueMessage(result);
    }

    public static Category getStatsCategory(Guild guild) {
        List<Category> categories = Objects.requireNonNull(guild).getCategories();
        Category category = categories.stream().filter(c -> c.getName().equalsIgnoreCase("dune statistics")).findFirst().orElse(null);
        if (category == null)
            throw new IllegalStateException("The DUNE STATISTICS category was not found.");
        return category;
    }
}
