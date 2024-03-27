package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Bidding;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class ReportsCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("reports", "Commands for statistics about Dune: Play by Discord games.").addSubcommands(
                new SubcommandData("games-per-player", "Show the games each player is in listing those on waiting list first.").addOptions(months),
                new SubcommandData("active-games", "Show active games with turn, phase, and subphase.").addOptions(showPlayers)
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
            System.out.println(category.getName() + " is not a Dune game.");
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
            case 7 -> "Battle";
            case 8 -> "Spice Collection";
            case 9 -> "Mentat Pause";
            default -> "Phase not identified";
        };
    }

    public static String activeGames(SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = event.getOption(showPlayers.getName());
        boolean showPlayersInGames = (optionMapping != null && optionMapping.getAsBoolean());
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
                    response.append(", ").append(factionsLeftToGo).append(" factions remaining.");
                }
                response.append("\n");
                if (showPlayersInGames)
                    for (Faction f : game.getFactions()) {
                        response.append(f.getEmoji()).append(" - ").append(f.getPlayer()).append("\n");
                    }
                response.append("\n");
            } catch (Exception e) {
                // category is not a Dune game
            }
        }
        return response.toString();
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
            result += " - " + Emojis.getFactionEmoji("BG") + " prediction win";
        else if (ecazAllyOccupy)
            result += " - " + Emojis.getFactionEmoji("Ecaz") + Emojis.getFactionEmoji(Objects.requireNonNull(winner2Name)) + " co-occupied 3 strongholds";
        result += "\n";
        result += "> " + Emojis.getFactionEmoji(winner1Name) + " - " + winner1Name + "\n";
        if (winner2Name != null)
            result += "> " + Emojis.getFactionEmoji(winner2Name) + " - " + winner2Name + "\n";
        result += "Summary:\n";
        result += "> Edit this text to add a summary, or remove the Summary section if you do not wish to include one.";
        discordGame.getModInfo().queueMessage(result);
    }
}
