package controller.commands;

import caches.EmojiCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Bidding;
import model.Game;
import model.factions.BGFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
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
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;

public class ReportsCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("reports", "Commands for statistics about Dune: Play by Discord games.").addSubcommands(
                new SubcommandData("games-per-player", "Show the games each player is in listing those on waiting list first.").addOptions(months),
                new SubcommandData("active-games", "Show active games with turn, phase, and subphase.").addOptions(showFactions),
                new SubcommandData("update-stats", "Update player, faction, and moderator stats if new games have been added to game-results."),
                new SubcommandData("player-record", "Show the overall per faction record for the player").addOptions(user)
        ));

        return commandData;
    }

    public static String runCommand(SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        String responseMessage = "";
        switch (name) {
            case "games-per-player" -> responseMessage = gamesPerPlayer(event);
            case "active-games" -> responseMessage = activeGames(event);
            case "update-stats" -> responseMessage = updateStats(event);
            case "player-record" -> responseMessage = playerRecord(event);
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

    private static void addRecentlyFinishedPlayers(HashMap<String, List<String>> playerGamesMap, Category category, int monthsAgo) {
        Optional<TextChannel> optChannel = category.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase("game-results"))
                .findFirst();
        if (optChannel.isPresent()) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.add(Calendar.MONTH, -1 * monthsAgo);
            long timestamp = c.getTimeInMillis();
            String discordTimestamp = Long.toUnsignedString(TimeUtil.getDiscordTimestamp(timestamp));
            TextChannel gameResults = optChannel.get();
            MessageHistory messageHistory = MessageHistory.getHistoryAfter(gameResults, discordTimestamp).complete();
            List<Message> messages = messageHistory.getRetrievedHistory();
            for (Message m : messages) {
                for (String player : findPlayerTags(m.getContentRaw())) {
                    List<String> games = playerGamesMap.computeIfAbsent(player, k -> new ArrayList<>());
                    if (games.isEmpty()) {
                        games.add("recently-finished");
                    }
                }
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
        StringBuilder message = new StringBuilder("    " + playerGame.numGames + " - " + playerGame.player);
        if (playerGame.numGames != 0) message.append(" (");
        String comma = "";
        for (String categoryName : playerGame.games) {
            String printName = categoryName.substring(0, Math.min(6, categoryName.length()));
            if (categoryName.startsWith("D") || categoryName.startsWith("PBD")) {
                try {
                    printName = "D" + new Scanner(categoryName).useDelimiter("\\D+").nextInt();
                } catch (Exception e) {
                    // category does not follow Dune: Play by Discord game naming and numbering patterns
                }
            }
            message.append(comma).append(printName);
            comma = ", ";
        }
        if (playerGame.numGames != 0) message.append(")");
        message.append("\n");
        return message.toString();
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

    public static String gamesPerPlayer(SlashCommandInteractionEvent event) {
        String message = "**Number of games players are in**\n";
        HashMap<String, List<String>> playerGamesMap = new HashMap<>();
        OptionMapping optionMapping = event.getOption(months.getName());
        int monthsAgo = (optionMapping != null ? optionMapping.getAsInt() : 1);
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        for (Category category : categories) {
            String categoryName = category.getName();
            if (categoryName.equalsIgnoreCase("staging area")) {
                addWaitingListPlayers(playerGamesMap, category);
            } else if (categoryName.equalsIgnoreCase("dune statistics")) {
                addRecentlyFinishedPlayers(playerGamesMap, category, monthsAgo);
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
            if (pg.games.get(0).equals("waiting-list")) {
                pg.games.remove(0);
                pg.onWaitingList = true;
            } else if (pg.games.get(0).equals("recently-finished")) {
                pg.games.remove(0);
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
                response.append(categoryName).append("\nTurn ").append(game.getTurn()).append(", ");
                int phase = game.getPhaseForTracker();
                response.append(phaseName(phase));
                if (!game.hasPhaseForTracker())
                    response.append(" (old tracker)");
                if (phase == 4) {
                    Bidding bidding = game.getBidding();
                    response.append(", Card ").append(bidding.getBidCardNumber()).append(" of ").append(bidding.getNumCardsForBid());
                } else if (phase == 6) {
                    int factionsLeftToGo = game.getTurnOrder().size();
                    if (game.hasFaction("Guild") && !game.getFaction("Guild").getShipment().hasShipped() && !game.getTurnOrder().contains("Guild"))
                        factionsLeftToGo++;
                    response.append(", ").append(factionsLeftToGo).append(" factions remaining");
                }
                response.append("\nMod: ").append(game.getMod());
                if (showFactionsinGames) {
                    response.append("\nFactions: ");
                    for (Faction f: game.getFactions()) {
                        String factionEmojiName = f.getEmoji().substring(1, f.getEmoji().length() - 1);
                        RichCustomEmoji emoji = event.getGuild().getEmojis().stream().filter(e -> e.getName().equals(factionEmojiName)).findFirst().orElse(null);
                        if (emoji == null)
                            response.append(f.getEmoji());
                        else
                            response.append("<").append(f.getEmoji()).append(emoji.getId()).append(">");
                    }
                }
                response.append("\n\n");
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        return response.toString();
    }

    private static class GameResults {
        JsonArray gameResults;
        int numNewEntries;

        GameResults(JsonArray gameResults, int numNewEntries) {
            this.gameResults = gameResults;
            this.numNewEntries = numNewEntries;
        }
    }

    static List<String> numberBoxes = List.of(":zero:", ":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":keycap_ten:");

    public static String writePlayerStats(JsonArray gameResults) {
        Set<String> players = new HashSet<>();
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Atreides")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "BG")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "BT")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "CHOAM")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Ecaz")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Emperor")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Fremen")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Guild")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Harkonnen")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Ix")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Moritani")).collect(Collectors.toSet()));
        players.addAll(gameResults.asList().stream().map(gr -> getJsonRecordValueOrBlankString(gr.getAsJsonObject(), "Richese")).collect(Collectors.toSet()));
        players.remove("");
        List<PlayerPerformance> allPlayerPerformance = new ArrayList<>();
        for (String player : players) {
            allPlayerPerformance.add(playerPerformance(gameResults, player));
        }
        allPlayerPerformance.sort((a, b) -> a.numWins == b.numWins ? Long.compare(a.numGames, b.numGames) : Long.compare(b.numWins, a.numWins));
        StringBuilder playerStatsString = new StringBuilder("__Supreme Ruler of Arrakis__");
        for (PlayerPerformance pp : allPlayerPerformance) {
            String winPercentage = new DecimalFormat("%#0.0").format(pp.winPercentage);
            int tensDigit = pp.numWins % 100 / 10;
            String tensEmoji = tensDigit == 0 ? ":black_large_square:" : numberBoxes.get(tensDigit);
            int onesDigit = pp.numWins % 10;
            playerStatsString.append("\n").append(tensEmoji).append(numberBoxes.get(onesDigit)).append(" - ").append(pp.playerName).append(" - ").append(winPercentage).append(" (").append(pp.numWins).append("/").append(pp.numGames).append(")");
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

    private static long playerFactionGames(JsonArray gameResults, String playerName, String factionName) {
        return gameResults.asList().stream()
                .map(gr -> gr.getAsJsonObject().get(factionName)).filter(v -> v != null && v.getAsString().equals(playerName))
                .count();
    }

    private static PlayerPerformance playerPerformance(JsonArray gameResults, String playerName) {
        long numGames = playerFactionGames(gameResults, playerName, "Atreides")
                + playerFactionGames(gameResults, playerName, "BG")
                + playerFactionGames(gameResults, playerName, "BT")
                + playerFactionGames(gameResults, playerName, "CHOAM")
                + playerFactionGames(gameResults, playerName, "Ecaz")
                + playerFactionGames(gameResults, playerName, "Emperor")
                + playerFactionGames(gameResults, playerName, "Fremen")
                + playerFactionGames(gameResults, playerName, "Guild")
                + playerFactionGames(gameResults, playerName, "Harkonnen")
                + playerFactionGames(gameResults, playerName, "Ix")
                + playerFactionGames(gameResults, playerName, "Moritani")
                + playerFactionGames(gameResults, playerName, "Richese");
        long numWins = gameResults.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(jo -> jo.get("winner1Player").getAsString().equals(playerName) || jo.get("winner2Player") != null && jo.get("winner2Faction").getAsString().equals(playerName))
                .count();
        float winPercentage = numWins/(float)numGames;
        return new PlayerPerformance(playerName, (int) numGames, (int) numWins, winPercentage);
    }

    public static String writeModeratorStats(JsonArray gameResults) {
        Set<String> mods = gameResults.asList().stream().map(gr -> gr.getAsJsonObject().get("moderator").getAsString()).collect(Collectors.toSet());
        List<Pair<String, Integer>> modAndNumGames = new ArrayList<>();
        mods.forEach(m -> modAndNumGames.add(new ImmutablePair<>(m, gameResults.asList().stream().filter(gr -> gr.getAsJsonObject().get("moderator").getAsString().equals(m)).toList().size())));
        modAndNumGames.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));
        StringBuilder moderatorsString = new StringBuilder("__Moderators__");
        for (Pair<String, Integer> p : modAndNumGames) {
            moderatorsString.append("\n").append(p.getRight()).append(" - ").append(p.getLeft());
        }
        return moderatorsString.toString();
    }

    public static String updateStats(SlashCommandInteractionEvent event) {
        GameResults gameResults = gatherGameResults(event);
        return gameResults.numNewEntries + " new games were added to parsed results.";
    }

    public static String writeFactionStats(SlashCommandInteractionEvent event, JsonArray gameResults) {
        return updateFactionPerformance(event, gameResults) + "\n\n"
                + updateTurnStats(event, gameResults) + "\n\n"
                + soloVictories(gameResults);
    }

    private static class FactionPerformance {
        String factionEmoji;
        long numGames;
        long numWins;
        float winPercentage;

        public FactionPerformance(String factionEmoji, long numGames, long numWins, float winPercentage) {
            this.factionEmoji = factionEmoji;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
        }
    }

    private static FactionPerformance factionPerformance(JsonArray gameResults, String factionName, String factionEmoji) {
        long numGames = gameResults.asList().stream()
                .map(gr -> gr.getAsJsonObject().get(factionName)).filter(v -> v != null && !v.getAsString().isEmpty())
                .count();
        long numWins = gameResults.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(jo -> jo.get("winner1Faction").getAsString().equals(factionName) || jo.get("winner2Faction") != null && jo.get("winner2Faction").getAsString().equals(factionName))
                .count();
        float winPercentage = numWins/(float)numGames;
        return new FactionPerformance(factionEmoji, numGames, numWins, winPercentage);
    }

    public static String updateFactionPerformance(SlashCommandInteractionEvent event, JsonArray gameResults) {
        List<FactionPerformance> allFactionPerformance = new ArrayList<>();
        allFactionPerformance.add(factionPerformance(gameResults, "Atreides", Emojis.ATREIDES));
        allFactionPerformance.add(factionPerformance(gameResults, "BG", Emojis.BG));
        allFactionPerformance.add(factionPerformance(gameResults, "BT", Emojis.BT));
        allFactionPerformance.add(factionPerformance(gameResults, "CHOAM", Emojis.CHOAM));
        allFactionPerformance.add(factionPerformance(gameResults, "Ecaz", Emojis.ECAZ));
        allFactionPerformance.add(factionPerformance(gameResults, "Emperor", Emojis.EMPEROR));
        allFactionPerformance.add(factionPerformance(gameResults, "Fremen", Emojis.FREMEN));
        allFactionPerformance.add(factionPerformance(gameResults, "Guild", Emojis.GUILD));
        allFactionPerformance.add(factionPerformance(gameResults, "Harkonnen", Emojis.HARKONNEN));
        allFactionPerformance.add(factionPerformance(gameResults, "Ix", Emojis.IX));
        allFactionPerformance.add(factionPerformance(gameResults, "Moritani", Emojis.MORITANI));
        allFactionPerformance.add(factionPerformance(gameResults, "Richese", Emojis.RICHESE));
        allFactionPerformance.sort((a, b) -> Float.compare(b.winPercentage, a.winPercentage));
        StringBuilder factionStatsString = new StringBuilder("__Factions__");
        for (FactionPerformance fs : allFactionPerformance) {
            String winPercentage = new DecimalFormat("%#0.0").format(fs.winPercentage);
            factionStatsString.append("\n").append(fs.factionEmoji).append(" ").append(winPercentage).append(" - ").append(fs.numWins).append("/").append(fs.numGames);
        }
        return tagEmojis(event, factionStatsString.toString());
    }

    private static class TurnStats {
        String turn;
        long numGames;
        long numWins;
        float winPercentage;

        public TurnStats(String turn, long numGames, long numWins, float winPercentage) {
            this.turn = turn;
            this.numGames = numGames;
            this.numWins = numWins;
            this.winPercentage = winPercentage;
        }
    }

    private static TurnStats turnStats(JsonArray gameResults, String turn) {
        long numGames;
        long numWins;
        String marker;

        switch (turn) {
            case "F" -> {
                numGames = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("Fremen")).filter(v -> v != null && !v.getAsString().isEmpty())
                        .count();
                numWins = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("victoryType")).filter(v -> v != null && v.getAsString().equals("F"))
                        .count();
                marker = Emojis.FREMEN;
            }
            case "G" -> {
                numGames = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("Guild")).filter(v -> v != null && !v.getAsString().isEmpty())
                        .count();
                numWins = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("victoryType")).filter(v -> v != null && v.getAsString().equals("G"))
                        .count();
                marker = Emojis.GUILD;
            }
            case "BG" -> {
                numGames = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("BG")).filter(v -> v != null && !v.getAsString().isEmpty())
                        .count();
                numWins = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("victoryType")).filter(v -> v != null && v.getAsString().equals("BG"))
                        .count();
                marker = Emojis.BG;
            }
            case "Ecaz" -> {
                numGames = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("Ecaz")).filter(v -> v != null && !v.getAsString().isEmpty())
                        .count();
                numWins = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("victoryType")).filter(v -> v != null && v.getAsString().equals("Ecaz"))
                        .count();
                marker = Emojis.ECAZ;
            }
            default -> {
                numGames = gameResults.asList().size();
                numWins = gameResults.asList().stream()
                        .map(gr -> gr.getAsJsonObject().get("turn")).filter(v -> v != null && v.getAsString().equals(turn))
                        .count();
                marker = numberBoxes.get(Integer.parseInt(turn));
            }
        }
        float winPercentage = numWins / (float) numGames;
        return new TurnStats(marker, numGames, numWins, winPercentage);
    }

    public static String updateTurnStats(SlashCommandInteractionEvent event, JsonArray gameResults) {
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
        allTurnStats.add(turnStats(gameResults, "Ecaz"));
        StringBuilder turnStatsString = new StringBuilder("__Turns__");
        for (TurnStats ts : allTurnStats) {
            String winPercentage = new DecimalFormat("%#0.0").format(ts.winPercentage);
            turnStatsString.append("\n").append(ts.turn).append(" ").append(winPercentage).append(" - ").append(ts.numWins).append("/").append(ts.numGames);
        }
        return tagEmojis(event, turnStatsString.toString());
    }

    private static String soloVictories(JsonArray gameResults) {
        long numGames = gameResults.asList().size();
        long numWins = gameResults.asList().stream()
                .map(gr -> gr.getAsJsonObject().get("winner2Faction")).filter(v -> v == null || v.getAsString().isEmpty())
                .count();
        String winPercentage = new DecimalFormat("%#0.0").format(numWins / (float) numGames);
        return "__Solo Victories__\n" + winPercentage + " - " + numWins + "/" + numGames;
    }

    public static String getHeader() {
        return "V1.0,Atreides,BG,BT,CHOAM,Ecaz,Emperor,Fremen,Guild,Harkonnen,Ix,Moritani,Rich,Turn,Win Type,Faction 1,Faction 2,Winner 1,Winner 2,Predicted Faction,Predicted Player,Mod,Game Start,Game End,Archived";
    }

    public static String getChrisHeader() {
        return ",Atreides,BG,BT,CHOAM,Ecaz,Emperor,Fremen,Guild,Harkonnen,Ix,Moritani,Rich,Turn,Faction 1,Faction 2,Winner 1,Winner 2,Mod";
    }

    public static String getJsonRecordValueOrBlankString(JsonObject jsonGameResult, String key) {
        JsonElement element = jsonGameResult.get(key);
        return element == null ? "" : element.getAsString();
    }

    public static String getChrisGameRecord(JsonElement jsonGameElement) {
        JsonObject jsonGameObject = jsonGameElement.getAsJsonObject();
        String gameRecord = "\n" + jsonGameObject.get("gameName") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Atreides") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "BG") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "BT") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "CHOAM") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Ecaz") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Emperor") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Fremen") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Guild") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Harkonnen") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Ix") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Moritani") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Richese") + ",";
        String victoryType = getJsonRecordValueOrBlankString(jsonGameObject, "victoryType");
        if (victoryType.isEmpty() || victoryType.equals("E"))
            gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "turn") + ",";
        else
            gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "victoryType") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner1Faction") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner2Faction") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner1Player") + ",";
        if (getJsonRecordValueOrBlankString(jsonGameObject, "winner2Faction").isEmpty())
            gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "=NA()") + ",";
        else
            gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner2Player") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "moderator");
        return gameRecord;
    }

    public static String getGameRecord(JsonElement jsonGameElement) {
        JsonObject jsonGameObject = jsonGameElement.getAsJsonObject();
        String gameRecord = "\n" + jsonGameObject.get("gameName") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Atreides") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "BG") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "BT") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "CHOAM") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Ecaz") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Emperor") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Fremen") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Guild") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Harkonnen") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Ix") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Moritani") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "Richese") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "turn") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "victoryType") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner1Faction") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner2Faction") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner1Player") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "winner2Player") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "predictedFaction") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "predictedPlayer") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "moderator") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "gameStartDate") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "gameEndDate") + ",";
        gameRecord += getJsonRecordValueOrBlankString(jsonGameObject, "archiveDate");
        return gameRecord;
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
    }

    public static boolean isPlayer(JsonObject gameResult, String playerName) {
        if (getJsonRecordValueOrBlankString(gameResult, "Atreides").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "BG").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "BT").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "CHOAM").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Ecaz").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Emperor").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Fremen").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Guild").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Harkonnen").equals(playerName)) return true;
        else if (getJsonRecordValueOrBlankString(gameResult, "Ix").equals(playerName)) return true;
        else return getJsonRecordValueOrBlankString(gameResult, "Richese").equals(playerName);
    }

    public static boolean isWinner(JsonObject gameResult, String playerName) {
        return (getJsonRecordValueOrBlankString(gameResult, "winner1Player").equals(playerName)
                || getJsonRecordValueOrBlankString(gameResult, "winner2Player").equals(playerName));
    }

    private static PlayerRecord getPlayerRecord(JsonArray gameResults, String playerName) {
        PlayerRecord pr = new PlayerRecord();
        for (JsonElement gr : gameResults.asList()) {
            JsonObject gameResult = gr.getAsJsonObject();
            boolean winner = false;
            if (isPlayer(gameResult, playerName)) pr.games++;
            if (isWinner(gameResult, playerName)) {
                pr.wins++;
                winner = true;
            }
            if (getJsonRecordValueOrBlankString(gameResult, "Atreides").equals(playerName)) {
                pr.atreidesGames++;
                if (winner) pr.atreidesWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "BG").equals(playerName)) {
                pr.bgGames++;
                if (winner) pr.bgWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "BT").equals(playerName)) {
                pr.btGames++;
                if (winner) pr.btWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "CHOAM").equals(playerName)) {
                pr.choamGames++;
                if (winner) pr.choamWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Ecaz").equals(playerName)) {
                pr.ecazGames++;
                if (winner) pr.ecazWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Emperor").equals(playerName)) {
                pr.emperorGames++;
                if (winner) pr.emperorWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Fremen").equals(playerName)) {
                pr.fremenGames++;
                if (winner) pr.fremenWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Guild").equals(playerName)) {
                pr.guildGames++;
                if (winner) pr.guildWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Harkonnen").equals(playerName)) {
                pr.harkonnenGames++;
                if (winner) pr.harkonnenWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Ix").equals(playerName)) {
                pr.ixGames++;
                if (winner) pr.ixWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Moritani").equals(playerName)) {
                pr.moritaniGames++;
                if (winner) pr.moritaniWins++;
            } else if (getJsonRecordValueOrBlankString(gameResult, "Richese").equals(playerName)) {
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
        if (strippedEmoji.equals("bg") || strippedEmoji.equals("bt") || strippedEmoji.equals("choam"))
            return strippedEmoji.toUpperCase();
        if (strippedEmoji.equals("rich"))
            return "Richese";
        return strippedEmoji.substring(0, 1).toUpperCase() + strippedEmoji.substring(1);
    }

    private static GameResults gatherGameResults(SlashCommandInteractionEvent event) {
        List<Category> categories = Objects.requireNonNull(event.getGuild()).getCategories();
        Category category = categories.stream().filter(c -> c.getName().equalsIgnoreCase("dune statistics")).findFirst().orElse(null);
        if (category == null)
            throw new IllegalStateException("The DUNE STATISTICS category was not found.");
        TextChannel gameResults = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("game-results")).findFirst().orElse(null);
        if (gameResults == null)
            throw new IllegalStateException("The game-results channel was not found.");
        TextChannel playerStatsChannel = category.getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("player-stats")).findFirst().orElse(null);
        if (playerStatsChannel == null)
            throw new IllegalStateException("The player-stats channel was not found.");
        ThreadChannel parsedResults = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("parsed-results")).findFirst().orElse(null);
        if (parsedResults == null)
            parsedResults = playerStatsChannel.createThreadChannel("parsed-results").complete();

        JsonArray jsonGameResults = new JsonArray();
        MessageHistory h = parsedResults.getHistory();
        h.retrievePast(1).complete();
        List<Message> ml = h.getRetrievedHistory();
        OffsetDateTime csvFileDateTime = null;
        if (ml.size() == 1) {
            List<Message.Attachment> messageList =  ml.get(0).getAttachments();
            if (!messageList.isEmpty()) {
                Message.Attachment encoded = ml.get(0).getAttachments().get(0);
                CompletableFuture<InputStream> future = encoded.getProxy().download();
                try {
                    String jsonResults = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
                    future.get().close();
                    jsonGameResults = JsonParser.parseString(jsonResults).getAsJsonArray();
                    csvFileDateTime = ml.get(0).getTimeCreated();
                } catch (IOException | InterruptedException | ExecutionException e) {
                    // Could not load from csv file. Complete new set will be generated from game-results
                }
            }
        }

        StringBuilder chrisCSVFromJson = new StringBuilder(getChrisHeader());
        StringBuilder reportsCSVFromJson = new StringBuilder(getHeader());
        MessageHistory messageHistory = MessageHistory.getHistoryFromBeginning(gameResults).complete();
        List<Message> messages = messageHistory.getRetrievedHistory();
        JsonArray jsonNewGameResults = new JsonArray();
        for (Message m : messages) {
            try {
                if (csvFileDateTime != null && m.getTimeCreated().isBefore(csvFileDateTime))
                    break;
                JsonObject jsonGameRecord = new JsonObject();
                String raw = m.getContentRaw();
                String gameName = raw.split("\n")[0];
                if (gameName.indexOf("__") == 0)
                    gameName = gameName.substring(2, gameName.length() - 2);
                jsonGameRecord.addProperty("gameName", gameName);
                jsonGameRecord.addProperty("archiveDate", m.getTimeCreated().toLocalDate().toString());
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
                    if (foundChannelId) {
                        TextChannel posts = Objects.requireNonNull(event.getGuild()).getTextChannelById(channelString);
                        if (posts != null) {
                            jsonGameRecord.addProperty("gameStartDate", posts.getTimeCreated().toLocalDate().toString());
                            MessageHistory postsHistory = posts.getHistory();
                            postsHistory.retrievePast(1).complete();
                            ml = postsHistory.getRetrievedHistory();
                            if (!ml.isEmpty()) {
                                jsonGameRecord.addProperty("gameEndDate", ml.get(0).getTimeCreated().toLocalDate().toString());
                            }
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
                        moderator = "@" + event.getJDA().retrieveUserById(modMatcher.group(1)).complete().getName();
                    else
                        moderator = lines[1].substring(2).split("\\s+", 2)[0];
                } else if (gameName.equals("Discord 27"))
                    moderator = "@voiceofonecrying";
                jsonGameRecord.addProperty("moderator", moderator);

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
                else if (winnersString.contains(":guild: Default"))
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

                String turn = "";
                if (!victoryType.equals("G") && !victoryType.equals("F") && turnMatcher.find())
                    turn = turnMatcher.group(1);
                jsonGameRecord.addProperty("victoryType", victoryType);
                jsonGameRecord.addProperty("turn", turn);
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
                    winner1Faction = "BG";
                    predictedFaction = strippedEmoji1.equals("BG") ? strippedEmoji2 : strippedEmoji1;
                } else {
                    winner1Faction = strippedEmoji1;
                    winner2Faction = strippedEmoji2;
                }
                if (gameName.equals("Discord 38"))
                    winner1Faction = "Emperor";
                jsonGameRecord.addProperty("winner1Faction", winner1Faction);
                if (!winner2Faction.isEmpty())
                    jsonGameRecord.addProperty("winner2Faction", winner2Faction);
                if (!predictedFaction.isEmpty())
                    jsonGameRecord.addProperty("predictedFaction", predictedFaction);

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
                        playerName = "@" + event.getJDA().retrieveUserById(matcher2.group(1)).complete().getName();
                    else {
                        playerName = s.substring(matcherThatFoundEmoji.end());
                        if (playerName.indexOf(" - ") == 0)
                            playerName = playerName.substring(3).split("\\s+", 2)[0];
                    }
                    jsonGameRecord.addProperty(capitalize(factionName), playerName);
                }
                if (gameName.equals("Discord 32"))
                    jsonGameRecord.addProperty("Fremen", "@jefwiodrade");
                jsonGameRecord.addProperty("winner1Player", jsonGameRecord.get(jsonGameRecord.get("winner1Faction").getAsString()).getAsString());
                if (!winner2Faction.isEmpty())
                    jsonGameRecord.addProperty("winner2Player", jsonGameRecord.get(jsonGameRecord.get("winner2Faction").getAsString()).getAsString());
                if (!predictedFaction.isEmpty())
                    jsonGameRecord.addProperty("predictedPlayer", jsonGameRecord.get(jsonGameRecord.get("predictedFaction").getAsString()).getAsString());
                jsonNewGameResults.add(jsonGameRecord);
            } catch (Exception e) {
                // Not a game result message, so skip it.
            }
        }
        List<JsonElement> newGames = jsonNewGameResults.asList();
        Collections.reverse(newGames);
        JsonArray reversedNewGames = new JsonArray();
        newGames.forEach(reversedNewGames::add);
        jsonGameResults.addAll(reversedNewGames);
        if (!jsonNewGameResults.isEmpty()) {
            ThreadChannel factionStats = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("faction-stats-test")).findFirst().orElse(null);
            if (factionStats == null)
                factionStats = playerStatsChannel.createThreadChannel("faction-stats-test").complete();
            factionStats.sendMessage(writeFactionStats(event, jsonGameResults)).queue();
            ThreadChannel moderatorStats = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("moderator-stats-test")).findFirst().orElse(null);
            if (moderatorStats == null)
                moderatorStats = playerStatsChannel.createThreadChannel("moderator-stats-test").complete();
            moderatorStats.sendMessage(writeModeratorStats(jsonGameResults)).queue();
            ThreadChannel playerStats = playerStatsChannel.getThreadChannels().stream().filter(c -> c.getName().equalsIgnoreCase("player-stats-test")).findFirst().orElse(null);
            if (playerStats == null)
                playerStats = playerStatsChannel.createThreadChannel("player-stats-test").complete();
            StringBuilder playerStatsString = new StringBuilder();
            String[] playerStatsLines = writePlayerStats(jsonGameResults).split("\n");
            for (String s : playerStatsLines) {
                if (playerStatsString.length() + s.length() > 2000) {
                    playerStats.sendMessage(playerStatsString.toString()).queue();
                    playerStatsString = new StringBuilder();
                }
                if (!playerStatsString.isEmpty())
                    playerStatsString.append("\n");
                playerStatsString.append(s);
            }
            if (!playerStatsString.isEmpty())
                playerStats.sendMessage(playerStatsString.toString()).queue();

            List<JsonElement> jsonElements = jsonGameResults.asList();
            for (JsonElement element : jsonElements) {
                chrisCSVFromJson.append(getChrisGameRecord(element));
                reportsCSVFromJson.append(getGameRecord(element));
            }
            FileUpload fileUpload = FileUpload.fromData(
                    chrisCSVFromJson.toString().getBytes(StandardCharsets.UTF_8), "results-for-chris.csv"
            );
            parsedResults.sendFiles(fileUpload).complete();
            fileUpload = FileUpload.fromData(
                    reportsCSVFromJson.toString().getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.csv"
            );
            parsedResults.sendFiles(fileUpload).complete();
            fileUpload = FileUpload.fromData(
                    jsonGameResults.toString().getBytes(StandardCharsets.UTF_8), "dune-by-discord-results.json"
            );
            parsedResults.sendFiles(fileUpload).complete();
        }
        return new GameResults(jsonGameResults, jsonNewGameResults.size());
    }

    public static RichCustomEmoji getEmoji(SlashCommandInteractionEvent event, String emojiName) {
        Map<String, RichCustomEmoji> emojis = EmojiCache.getEmojis(Objects.requireNonNull(event.getGuild()).getId());
        return emojis.get(emojiName.replace(":", ""));
    }

    public static String getEmojiTag(SlashCommandInteractionEvent event, String emojiName) {
        RichCustomEmoji emoji = getEmoji(event, emojiName);
        return emoji == null ? ":" + emojiName.replace(":", "") + ":" : emoji.getFormatted();
    }
    public static String tagEmojis(SlashCommandInteractionEvent event, String message) {
        Matcher matcher = untaggedEmojis.matcher(message);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, getEmojiTag(event, matcher.group(1)));
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    public static String playerRecord(SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = event.getOption(user.getName());
        User thePlayer = (optionMapping != null ? optionMapping.getAsUser() : null);
        if (thePlayer == null)
            thePlayer = event.getUser();
        return playerRecord(event, thePlayer.getName());
    }

    private static String playerRecord(SlashCommandInteractionEvent event, String playerName) {
        JsonArray gameResults = gatherGameResults(event).gameResults;
        PlayerRecord pr = getPlayerRecord(gameResults, "@" + playerName);
        String returnString = playerName + " has played in " + pr.games + " games and won " + pr.wins;
        if (pr.atreidesGames > 0) {
            returnString += "\n" + Emojis.ATREIDES + " " + pr.atreidesGames + " games";
            if (pr.atreidesWins > 0) returnString += ", " + pr.atreidesWins + " wins";
        }
        if (pr.bgGames > 0) {
            returnString += "\n" + Emojis.BG + " " + pr.bgGames + " games";
            if (pr.bgWins > 0) returnString += ", " + pr.bgWins + " wins";
        }
        if (pr.btGames > 0) {
            returnString += "\n" + Emojis.BT + " " + pr.btGames + " games";
            if (pr.btWins > 0) returnString += ", " + pr.btWins + " wins";
        }
        if (pr.choamGames > 0) {
            returnString += "\n" + Emojis.CHOAM + " " + pr.choamGames + " games";
            if (pr.choamWins > 0) returnString += ", " + pr.choamWins + " wins";
        }
        if (pr.ecazGames > 0) {
            returnString += "\n" + Emojis.ECAZ + " " + pr.ecazGames + " games";
            if (pr.ecazWins > 0) returnString += ", " + pr.ecazWins + " wins";
        }
        if (pr.emperorGames > 0) {
            returnString += "\n" + Emojis.EMPEROR + " " + pr.emperorGames + " games";
            if (pr.emperorWins > 0) returnString += ", " + pr.emperorWins + " wins";
        }
        if (pr.fremenGames > 0) {
            returnString += "\n" + Emojis.FREMEN + " " + pr.fremenGames + " games";
            if (pr.fremenWins > 0) returnString += ", " + pr.fremenWins + " wins";
        }
        if (pr.guildGames > 0) {
            returnString += "\n" + Emojis.GUILD + " " + pr.guildGames + " games";
            if (pr.guildWins > 0) returnString += ", " + pr.guildWins + " wins";
        }
        if (pr.harkonnenGames > 0) {
            returnString += "\n" + Emojis.HARKONNEN + " " + pr.harkonnenGames + " games";
            if (pr.harkonnenWins > 0) returnString += ", " + pr.harkonnenWins + " wins";
        }
        if (pr.ixGames > 0) {
            returnString += "\n" + Emojis.IX + " " + pr.ixGames + " games";
            if (pr.ixWins > 0) returnString += ", " + pr.ixWins + " wins";
        }
        if (pr.moritaniGames > 0) {
            returnString += "\n" + Emojis.MORITANI + " " + pr.moritaniGames + " games";
            if (pr.moritaniWins > 0) returnString += ", " + pr.moritaniWins + " wins";
        }
        if (pr.richGames > 0) {
            returnString += "\n" + Emojis.RICHESE + " " + pr.richGames + " games";
            if (pr.richWins > 0) returnString += ", " + pr.richWins + " wins";
        }
        return tagEmojis(event, returnString);
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
        discordGame.getModInfo().queueMessage(result);
    }
}
