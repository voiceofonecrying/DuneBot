package controller.commands;

import constants.Emojis;
import controller.Alliance;
import controller.CommandCompletionGuard;
import controller.DiscordGame;
import controller.Queue;
import controller.buttons.ButtonManager;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import templates.ChannelPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;
import static controller.commands.ShowCommands.*;

public class CommandManager extends ListenerAdapter {
    public List<Member> members = new ArrayList<>();

    public void gatherMembers(List<Member> members) {
        this.members.addAll(members);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        this.members.add(event.getMember());
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        CommandCompletionGuard.incrementCommandCount();
        if (event.getComponentId().startsWith("bidding-menu-")) {
            int bid;
            boolean useExact = true;
            if (event.getInteraction().getSelectedOptions().size() == 2) {
                if (!event.getInteraction().getSelectedOptions().get(0).getValue().equals("auto-increment")) {
                    CommandCompletionGuard.decrementCommandCount();
                    event.reply("You cannot select two bids.").queue();
                    return;
                }
                bid = Integer.parseInt(event.getInteraction().getSelectedOptions().get(1).getValue());
                useExact = false;
            } else bid = Integer.parseInt(event.getInteraction().getSelectedOptions().getFirst().getValue());
            try {
                DiscordGame discordGame = new DiscordGame(event);
                Game game = discordGame.getGame();
                Faction faction = ButtonManager.getButtonPresser(event, game);
                game.getBidding().bid(game, faction, useExact, bid, null, null);
                discordGame.pushGame();
                ShowCommands.updateBiddingActions(discordGame, game, faction);
                discordGame.sendAllMessages();
            } catch (ChannelNotFoundException | InvalidGameStateException e) {
                throw new RuntimeException(e);
            }
        } else if (event.getComponentId().startsWith("play-card-menu-")) {
            event.reply("Playing your card...").queue();
            String interaction = event.getInteraction().getSelectedOptions().getFirst().getValue();
            try {
                DiscordGame discordGame = new DiscordGame(event);
                Game game = discordGame.getGame();
                Faction faction = ButtonManager.getButtonPresser(event, game);
                switch (interaction) {
                    case "Karama-buy": {
                        faction.discard("Karama");
                        if (event.getInteraction().getSelectedOptions().getFirst().getValue().split("-")[1].equals("buy")) {
                            game.getBidding().assignAndPayForCard(game, faction.getName(), "", 0, false);
                            game.getModInfo().publish(faction.getEmoji() + " has played Karama to take the card. Auction the next card or advance the game. " + game.getModOrRoleMention());
                        }
                        break;
                    }
                    case "Truthtrance": {
                        faction.discard("Truthtrance");
                        discordGame.getGameActions().publish(faction.getEmoji() + " has played Truthtrance to ask a question:");
                        break;
                    }
                    case "Amal": {
                        faction.discard("Amal");
                        for (Faction f : game.getFactions()) {
                            int spiceLost = Math.ceilDiv(f.getSpice(), 2);
                            discordGame.getTurnSummary().publish(f.getEmoji() + " loses " + spiceLost + Emojis.SPICE + " to Amal.");
                            f.subtractSpice(spiceLost, "Amal");
                        }
                        break;
                    }
                    case "Tleilaxu Ghola-forces": {
                        faction.discard("Tleilaxu Ghola");
                        discordGame.getGameActions().publish(faction.getEmoji() + " has played Tleilaxu Ghola to revive forces for free.");
                        int starred = 0;
                        int regular = game.getTleilaxuTanks().getForceStrength(faction.getName());
                        if (faction instanceof EmperorFaction && game.getTleilaxuTanks().getForceStrength("Emperor*") >= 1) {
                            starred = 1;
                        } else if (faction instanceof FremenFaction && game.getTleilaxuTanks().getForceStrength("Fremen*") >= 1) {
                            starred = 1;
                        } else if (faction instanceof IxFaction && game.getTleilaxuTanks().getForceStrength("Ix*") >= 1) {
                            starred = Math.min(game.getTleilaxuTanks().getForceStrength("Ix*"), 5);
                        }

                        int revive = Math.min(5 - starred, regular);
                        game.reviveForces(faction, false, revive, starred);
                        break;
                    }
                }
                if (interaction.startsWith("Tleilaxu Ghola-leader-")) {
                    faction.discard("Tleilaxu Ghola");
                    faction.reviveLeader(interaction.split("-")[2], 0);
                }

                discordGame.pushGame();
                showFactionInfo(faction.getName(), discordGame);
                refreshFrontOfShieldInfo(discordGame, game);
                discordGame.sendAllMessages();
            } catch (InvalidGameStateException | ChannelNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }


        }
        CommandCompletionGuard.decrementCommandCount();
    }
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        CommandCompletionGuard.incrementCommandCount();
        String name = event.getName();
        event.deferReply(true).queue();
        Member member = event.getMember();

        List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();

        try {
            if (name.equals("new-game") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                newGame(event);
                event.getHook().editOriginal("Command Done.").queue();
            } else if (name.equals("waiting-list")) {
                waitingList(event);
                event.getHook().editOriginal("Command Done.").queue();
            } else if (name.equals("update-stats") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                String result = ReportsCommands.updateStats(event, members);
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("list-members") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                String result = ReportsCommands.listMembers(event, members);
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("average-days-per-turn") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                String result = ReportsCommands.averageDaysPerTurn(event.getGuild());
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("players-fastest-speed") && roles.stream().anyMatch(role -> role.getName().equals("Moderators"))) {
                OptionMapping optionMapping = event.getOption(numFastGamesForAverageDuration.getName());
                int minGames = (optionMapping != null ? optionMapping.getAsInt() : 3);
                optionMapping = event.getOption(minTurnsForAverageDuration.getName());
                int minTurns = (optionMapping != null ? optionMapping.getAsInt() : 3);
                String result = ReportsCommands.playerFastestGame(event.getGuild(), minGames, minTurns, minGames);
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("reports")) {
                String result = ReportsCommands.runCommand(event, members);
                event.getHook().editOriginal(result).queue();
            } else if (name.equals("my-record")) {
                event.getHook().editOriginal(ReportsCommands.playerRecord(event)).queue();
            } else if (name.equals("random-dune-quote")) {
                randomDuneQuote(event);
            } else {
                String categoryName = Objects.requireNonNull(DiscordGame.categoryFromEvent(event)).getName();
                CompletableFuture<Void> future = Queue.getFuture(categoryName);

                // Incrementing count again because it will be decremented when the future is resolved.
                CommandCompletionGuard.incrementCommandCount();
                Queue.putFuture(categoryName, future
                        .thenRunAsync(() -> runGameCommand(event))
                        .thenRunAsync(CommandCompletionGuard::decrementCommandCount));
            }
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        } finally {
            CommandCompletionGuard.decrementCommandCount();
        }
    }

    private void runGameCommand(@NotNull SlashCommandInteractionEvent event) {
        String ephemeralMessage = "";

        try {
            Member member = event.getMember();
            String name = event.getName();
            List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();
            DiscordGame discordGame = new DiscordGame(event);
            Game game = discordGame.getGame();

            if (roles.stream().noneMatch(role -> role.getName().equals(game.getModRole()) ||
                    role.getName().equals(game.getGameRole()) && name.startsWith("player"))) {
                event.getHook().editOriginal("You do not have permission to use this command.").queue();
                return;
            }

            if (game.isOnHold()) {
                if (name.equals("remove-hold")) {
                    game.setOnHold(false);
                    discordGame.getTurnSummary().queueMessage("The hold has been resolved. Gameplay may proceed.");
                    discordGame.pushGame();
                    discordGame.sendAllMessages();
                    event.getHook().editOriginal("Command Done.").queue();
                } else {
                    event.getHook().editOriginal("The game is on hold. Please wait for the mod to resolve the issue.").queue();
                }
                return;
            }

            switch (name) {
                case "gamestate" -> GameStateCommands.runCommand(event, discordGame, game);
                case "show" -> ShowCommands.runCommand(event, discordGame, game);
                case "setup" -> SetupCommands.runCommand(event, discordGame, game);
                case "run" -> RunCommands.runCommand(event, discordGame, game);
                case "battle" -> BattleCommands.runCommand(event, discordGame, game);
                case "storm" -> StormCommands.runCommand(event, discordGame, game);
                case "richese" -> RicheseCommands.runCommand(event, discordGame, game);
                case "bt" -> BTCommands.runCommand(event, discordGame, game);
                case "ecaz" -> EcazCommands.runCommand(event, discordGame, game);
                case "hark" -> HarkCommands.runCommand(event, discordGame, game);
                case "choam" -> ChoamCommands.runCommand(event, discordGame, game);
                case "ix" -> IxCommands.runCommand(event, discordGame, game);
                case "moritani" -> MoritaniCommands.runCommand(event, discordGame, game);
                case "bg" -> BGCommands.runCommand(event, discordGame, game);
                case "atreides" -> AtreidesCommands.runCommand(event, discordGame, game);
                case "player" -> ephemeralMessage = PlayerCommands.runCommand(event, discordGame, game);
                case "draw-treachery-card" -> drawTreacheryCard(discordGame, game);
                case "draw-traitor-card" -> drawTraitorCard(discordGame, game);
                case "shuffle-treachery-deck" -> shuffleTreacheryDeck(discordGame, game);
                case "discard" -> discard(discordGame, game);
                case "discard-traitor" -> discardTraitor(discordGame, game);
                case "transfer-card" -> transferCard(discordGame, game);
                case "transfer-card-from-discard" -> transferCardFromDiscard(discordGame, game);
                case "set-hand-limit" -> setHandLimit(discordGame, game);
                case "place-forces" -> placeForcesEventHandler(discordGame, game);
                case "move-forces" -> moveForcesEventHandler(discordGame, game);
                case "end-shipment-movement" -> endShipmentMovement(discordGame, game);
                case "remove-forces" -> removeForcesEventHandler(discordGame, game);
                case "homeworld-occupy" -> homeworldOccupy(discordGame, game);
                case "display-state" -> displayGameState(event, discordGame, game);
                case "revive-forces" -> reviveForcesEventHandler(discordGame, game);
                case "add-card-to-bidding-market" -> addCardToBiddingMarket(discordGame, game);
                case "award-bid" -> awardBid(event, discordGame, game);
                case "award-top-bidder" -> awardTopBidder(discordGame, game);
                case "kill-leader" -> killLeader(discordGame, game);
                case "revive-leader" -> reviveLeader(discordGame, game);
                case "bribe" -> bribe(discordGame, game);
                case "mute" -> mute(discordGame, game);
                case "assign-tech-token" -> assignTechToken(discordGame, game);
                case "draw-spice-blow" -> drawSpiceBlow();
                case "place-shai-hulud" -> placeShaiHulud(discordGame, game);
                case "create-alliance" -> createAlliance(discordGame, game);
                case "remove-alliance" -> removeAlliance(discordGame, game);
                case "set-spice-in-territory" -> setSpiceInTerritory(discordGame, game);
                case "destroy-shield-wall" -> destroyShieldWall(discordGame, game);
                case "add-spice" -> addSpice(discordGame, game);
                case "remove-spice" -> removeSpice(discordGame, game);
                case "reassign-faction" -> reassignFaction(discordGame, game);
                case "reassign-mod" -> reassignMod(event, discordGame, game);
                case "team-mod" -> teamMod(discordGame, game);
                case "draw-nexus-card" -> drawNexusCard(discordGame, game);
                case "discard-nexus-card" -> discardNexusCard(discordGame, game);
                case "moritani-assassinate-traitor" -> assassinateTraitor(discordGame, game);
                case "game-result" -> ReportsCommands.gameResult(event, discordGame, game);
                case "save-game-bot-data" -> ReportsCommands.saveGameBotData(event, discordGame, game);
            }

            if (!(name.equals("setup") && Objects.requireNonNull(event.getSubcommandName()).equals("faction")))
                refreshChangedInfo(discordGame);
            discordGame.sendAllMessages();

            if (ephemeralMessage.isEmpty()) ephemeralMessage = "Command Done.";
            event.getHook().editOriginal(ephemeralMessage).queue();
        } catch (InvalidGameStateException e) {
            event.getHook().editOriginal(e.getMessage()).queue();
        } catch (Exception e) {
            event.getHook().editOriginal(e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private List<String> getQuotesFromBook(String bookName) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(bookName)) {
            byte[] allBytes = Objects.requireNonNull(stream).readAllBytes();
            return new ArrayList<>(Arrays.stream(new String(allBytes, StandardCharsets.UTF_8).split("((?<=[.?!]))")).toList());
        }
    }

    private void randomDuneQuote(SlashCommandInteractionEvent event) throws IOException {
        int lines = DiscordGame.required(CommandOptions.lines, event).getAsInt();
        List<String> quotes;
        if (DiscordGame.optional(search, event) == null) {
            if (DiscordGame.optional(book, event) == null) {
                quotes = getQuotesFromBook("Dune Books/Dune.txt");
                Random random = new Random();
                int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();

            } else {
                quotes = getQuotesFromBook("Dune Books/" + DiscordGame.required(book, event).getAsString());
                Random random = new Random();
                int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() : random.nextInt(quotes.size() - lines + 1);
                StringBuilder quote = new StringBuilder();

                for (int i = 0; i < lines; i++) {
                    quote.append(quotes.get(start + i));
                }
                event.getMessageChannel().sendMessage(quote + "\n\n(Line: " + start + ")").queue();
            }
        } else {
            String search = DiscordGame.required(CommandOptions.search, event).getAsString();
            quotes = new ArrayList<>();
            quotes.addAll(getQuotesFromBook("Dune Books/Dune.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Messiah.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Children.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/GeoD.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Heretics.txt"));
            quotes.addAll(getQuotesFromBook("Dune Books/Chapterhouse.txt"));
            List<String> matched = new LinkedList<>();

            for (int i = 0; i < quotes.size() - lines; i+=lines) {
                StringBuilder candidate = new StringBuilder();
                for (int j = 0; j < lines; j++) {
                    candidate.append(quotes.get(i + j));
                }
                if (candidate.toString().contains(search)) matched.add(candidate.toString());
            }
            if (matched.isEmpty()) {
                event.getHook().sendMessage("No results.").queue();
                return;
            }
            Random random = new Random();
            int start = DiscordGame.optional(startingLine, event) != null ? DiscordGame.required(startingLine, event).getAsInt() - 1 : random.nextInt(matched.size() - 1);

            event.getMessageChannel().sendMessage(matched.get(start) + "\n (Match " + (start + 1) + " of " + matched.size() + " for search term: '" + search + "')").queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        CompletableFuture.runAsync(() -> runCommandAutoCompleteInteraction(event));
    }

    private void runCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game game = discordGame.getGame();
            event.replyChoices(CommandOptions.getCommandChoices(event, discordGame, game)).queue();
        } catch (ChannelNotFoundException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static List<CommandData> getAllCommands() {
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("new-game", "Creates a new Dune game instance.").addOptions(gameName, gameRole, modRole));
        commandData.add(Commands.slash("draw-treachery-card", "Draw a card from the top of a deck.").addOptions(faction));
        commandData.add(Commands.slash("draw-traitor-card", "Draw a card from the top of the traitor deck.").addOptions(faction));
        commandData.add(Commands.slash("shuffle-treachery-deck", "Shuffle the treachery deck."));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, card));
        commandData.add(Commands.slash("discard-traitor", "Move a card from a player to the Traitor pile and shuffle the pile.").addOptions(faction, traitor));
        commandData.add(Commands.slash("transfer-card", "Move a card from one faction's hand to another").addOptions(faction, card, recipient));
        commandData.add(Commands.slash("transfer-card-from-discard", "Move a card from the discard to a faction's hand").addOptions(faction, discardCard));
        commandData.add(Commands.slash("set-hand-limit", "Change the hand limit for a faction.").addOptions(faction, amount));
        commandData.add(Commands.slash("place-forces", "Place forces from reserves onto the surface").addOptions(faction, amount, starredAmount, isShipment, canTrigger, territory));
        commandData.add(Commands.slash("move-forces", "Move forces from one territory to another").addOptions(faction, fromTerritory, toTerritory, amount, starredAmount));
        commandData.add(Commands.slash("end-shipment-movement", "Force end of Shipment/Movement and advance"));
        commandData.add(Commands.slash("remove-forces", "Remove forces from the board.").addOptions(faction, amount, starredAmount, toTanks, killedInBattle, fromTerritory));
        commandData.add(Commands.slash("homeworld-occupy", "Set the occupying faction in a homeworld.").addOptions(homeworld, faction));
        commandData.add(Commands.slash("add-card-to-bidding-market", "Add one more card from the deck to the bidding market."));
        commandData.add(Commands.slash("award-bid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent, paidToFaction, harkonnenKaramad));
        commandData.add(Commands.slash("award-top-bidder", "Designate that a card has been won by the top bidder during bidding phase and pay spice recipient.").addOptions(harkonnenKaramad));
        commandData.add(Commands.slash("revive-forces", "Revive forces for a faction.").addOptions(faction, revived, starredAmount, paid));
        commandData.add(Commands.slash("display-state", "Displays some element of the game in mod-info.").addOptions(data));
        commandData.add(Commands.slash("kill-leader", "Send a leader to the tanks.").addOptions(factionOrTanks, leader, faceDown));
        commandData.add(Commands.slash("revive-leader", "Revive a leader from the tanks.").addOptions(faction, reviveLeader, revivalCost));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("remove-hold", "Remove the hold and allow gameplay to proceed."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, recipient, amount, reason));
        commandData.add(Commands.slash("assign-tech-token", "Assign a Tech Token to a Faction (taking it away from previous owner)").addOptions(faction, token));
        commandData.add(Commands.slash("draw-spice-blow", "Draw the spice blow").addOptions(spiceBlowDeck));
        commandData.add(Commands.slash("place-shai-hulud", "Make Shai-Hulud appear in a sand territory.").addOptions(sandTerritory, firstWorm));
        commandData.add(Commands.slash("create-alliance", "Create an alliance between two factions")
                .addOptions(faction, otherFaction));
        commandData.add(Commands.slash("remove-alliance", "Remove alliance (only on faction of the alliance needs to be selected)")
                .addOptions(faction));
        commandData.add(Commands.slash("set-spice-in-territory", "Set the spice amount for a territory")
                .addOptions(territory, amount));
        commandData.add(Commands.slash("destroy-shield-wall", "Destroy the shield wall"));

        commandData.add(Commands.slash("add-spice", "Add spice to a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("remove-spice", "Remove spice from a faction").addOptions(faction, amount, message, frontOfShield));
        commandData.add(Commands.slash("reassign-faction", "Assign the faction to a different player").addOptions(faction, user));
        commandData.add(Commands.slash("reassign-mod", "Assign yourself as the mod to be tagged"));
        commandData.add(Commands.slash("team-mod", "Enable or disable team mod where all users with the game mod role get tagged").addOptions(teamModSwitch));
        commandData.add(Commands.slash("draw-nexus-card", "Draw a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("discard-nexus-card", "Discard a nexus card.").addOptions(faction));
        commandData.add(Commands.slash("moritani-assassinate-traitor", "Assassinate Moritani's Traitor"));
        commandData.add(Commands.slash("random-dune-quote", "Will dispense a random line of text from the specified book.").addOptions(lines, book, startingLine, search));
        commandData.add(Commands.slash("game-result", "Generate the game-results message for this game.").addOptions(faction, otherWinnerFaction, guildSpecialWin, fremenSpecialWin, bgPredictionWin, ecazOccupyWin));
        commandData.add(Commands.slash("save-game-bot-data", "Save the game bot data at the end of a game for future analysis"));
        commandData.add(Commands.slash("update-stats", "Update player, faction, and moderator stats if new games have been added to game-results.").addOptions(forcePublish));
        commandData.add(Commands.slash("list-members", "Show members loaded by loadMembers in ephemeral response."));
        commandData.add(Commands.slash("average-days-per-turn", "Very rough estimate of a player's speed."));
        commandData.add(Commands.slash("players-fastest-speed", "Show each player's days per turn in their fastest game.").addOptions(numFastGamesForAverageDuration, minTurnsForAverageDuration));

        commandData.addAll(GameStateCommands.getCommands());
        commandData.addAll(ShowCommands.getCommands());
        commandData.addAll(SetupCommands.getCommands());
        commandData.addAll(RunCommands.getCommands());
        commandData.addAll(StormCommands.getCommands());
        commandData.addAll(BattleCommands.getCommands());
        commandData.addAll(RicheseCommands.getCommands());
        commandData.addAll(BTCommands.getCommands());
        commandData.addAll(HarkCommands.getCommands());
        commandData.addAll(ChoamCommands.getCommands());
        commandData.addAll(IxCommands.getCommands());
        commandData.addAll(BGCommands.getCommands());
        commandData.addAll(AtreidesCommands.getCommands());
        commandData.addAll(EcazCommands.getCommands());
        commandData.addAll(MoritaniCommands.getCommands());

        List<CommandData> commandDataWithPermissions = commandData.stream()
                .map(command -> command.setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)
                ))
                .collect(Collectors.toList());

        commandDataWithPermissions.addAll(PlayerCommands.getCommands());
        commandDataWithPermissions.addAll(ReportsCommands.getCommands());
        commandDataWithPermissions.add(Commands.slash("waiting-list", "Add an entry to the waiting list")
                .addOptions(slowGame, midGame, fastGame, originalSixFactions, ixianstleilaxuExpansion, choamricheseExpansion, ecazmoritaniExpansion, leaderSkills, strongholdCards, homeworlds));
        commandDataWithPermissions.add(Commands.slash("my-record", "View your record in an ephemeral message"));

        return commandDataWithPermissions;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(getAllCommands()).complete();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        Role gameRoleValue = Objects.requireNonNull(event.getOption(gameRole.getName())).getAsRole();
        Role modRoleValue = Objects.requireNonNull(event.getOption(modRole.getName())).getAsRole();
        Role observerRole = Objects.requireNonNull(event.getGuild()).getRolesByName("Observer", true).getFirst();
        Role pollBot = event.getGuild().getRolesByName("EasyPoll", true).getFirst();
        String name = Objects.requireNonNull(event.getOption(gameName.getName())).getAsString();

        // Create category and set base permissions to deny everything for everyone except the mod role.
        // The channel permissions assume that this is set this way.
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(modRoleValue, ChannelPermissions.all, null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, ChannelPermissions.all)
                .addPermissionOverride(gameRoleValue, null, ChannelPermissions.all)
                .addPermissionOverride(observerRole, null, ChannelPermissions.all)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).getFirst();

        category.createTextChannel("chat")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readWriteMinimumAllow,
                        ChannelPermissions.readWriteMinimumDeny
                )
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .complete();

        // Not including Observer in pre-game-voting because there's no way to stop someone from adding to an
        // existing emoji reaction.
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        pollBot,
                        ChannelPermissions.pollBotAllow,
                        ChannelPermissions.pollBotDeny
                )
                .complete();

        TextChannel fosChannel = category.createTextChannel("front-of-shield")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        gameRoleValue,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .complete();
        fosChannel.createThreadChannel("turn-0-summary", true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS)
                .complete();
        fosChannel.createThreadChannel("whispers", true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .complete();

        String[] readWriteChannels = {"game-actions", "bribes", "bidding-phase", "rules"};
        for (String channel : readWriteChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRoleValue,
                            ChannelPermissions.readWriteAllow,
                            ChannelPermissions.readWriteDeny
                    )
                    .complete();
        }

        String[] modChannels = {"bot-data", "mod-info"};
        for (String channel : modChannels) {
            category.createTextChannel(channel).complete();
        }

        DiscordGame discordGame = new DiscordGame(category);
        discordGame.queueMessage("rules", MessageFormat.format(
                """
                        {0}  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
                        {1}  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
                        {2} {3}  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
                        {4} {5} CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf
                        {6} {7} Ecaz & Moritani Rules: https://www.gf9games.com/dune/wp-content/uploads/EcazMoritani-Rulebook-LOWRES.pdf""",
                Emojis.DUNE_RULEBOOK,
                Emojis.WEIRDING,
                Emojis.IX, Emojis.BT,
                Emojis.CHOAM, Emojis.RICHESE,
                Emojis.ECAZ, Emojis.MORITANI
        ));

        Game game = new Game();
        game.setGameRole(gameRoleValue.getName());
        game.setGameRoleMention(gameRoleValue.getAsMention());
        game.setModRole(modRoleValue.getName());
        game.setModRoleMention(modRoleValue.getAsMention());
        game.setMod(event.getUser().getAsMention());
        game.setMute(false);
        discordGame.setGame(game);
        discordGame.pushGame();
        discordGame.sendAllMessages();
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

    /**
     * Add Spice to a player.  Spice can be added behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, true);
    }

    /**
     * Remove Spice from a player.  Spice can be removed from behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void removeSpice(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        addOrRemoveSpice(discordGame, game, false);
    }

    /**
     * Add or Remove Spice from a player.  This can be behind the shield or in front, with an optional message.
     *
     * @param discordGame The discord game object.
     * @param game        The game object.
     * @param add         True to add spice, false to remove spice.
     * @throws ChannelNotFoundException If the channel is not found.
     */
    public void addOrRemoveSpice(DiscordGame discordGame, Game game, boolean add) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();
        String messageValue = discordGame.required(message).getAsString();
        boolean toFrontOfShield = discordGame.optional(frontOfShield) != null && discordGame.required(frontOfShield).getAsBoolean();

        Faction faction = game.getFaction(factionName);
        if (toFrontOfShield) {
            if (add) {
                faction.addFrontOfShieldSpice(amountValue);
            } else {
                faction.subtractFrontOfShieldSpice(amountValue);
            }
        } else {
            if (add) {
                faction.addSpice(amountValue, messageValue);
            } else {
                faction.subtractSpice(amountValue, messageValue);
            }
        }
        String frontOfShieldMessage = add ? "to front of shield" : "from front of shield";

        discordGame.getTurnSummary().queueMessage(
                MessageFormat.format(
                        "{0} {1} {2} {3} {4} {5}",
                        faction.getEmoji(),
                        add ? "gains" : "loses",
                        amountValue, Emojis.SPICE,
                        toFrontOfShield ? frontOfShieldMessage : "",
                        messageValue
                )
        );
        discordGame.pushGame();
    }

    public void drawSpiceBlow() throws InvalidGameStateException {
        throw new InvalidGameStateException("Use /run advance to progress through Spice Blow and Nexus.");
    }

    public void placeShaiHulud(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(sandTerritory).getAsString();
        boolean finalDestination = true;
        if (discordGame.optional(firstWorm) != null)
            finalDestination = discordGame.required(firstWorm).getAsBoolean();
        game.placeShaiHulud(territoryName, "Shai-Hulud", finalDestination);
        discordGame.pushGame();
    }

    private void drawNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        boolean discarded = false;
        game.removeAlliance(faction);
        if (faction.getNexusCard() != null) {
            discarded = true;
            game.getNexusDiscard().add(faction.getNexusCard());
        }
        faction.setNexusCard(game.getNexusDeck().pollFirst());
        if (discarded)
            game.getTurnSummary().publish(faction.getEmoji() + " has replaced their Nexus Card.");
        else
            game.getTurnSummary().publish(faction.getEmoji() + " has drawn a Nexus Card.");
        showFactionInfo(faction.getName(), discordGame);
        discordGame.pushGame();
    }

    private void discardNexusCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = game.getFaction(discordGame.required(CommandOptions.faction).getAsString());
        game.discardNexusCard(faction);
        discordGame.pushGame();
    }

    public void drawTreacheryCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        game.drawTreacheryCard(factionName, true, true);
        discordGame.pushGame();
    }

    public void drawTraitorCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        game.drawTraitorCard(factionName);
        discordGame.pushGame();
    }

    public void discardTraitor(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String cardName = discordGame.required(traitor).getAsString();

        game.getFaction(factionName).discardTraitor(cardName, false);
        discordGame.pushGame();
    }

    public void shuffleTreacheryDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.shuffleTreacheryDeck();
        discordGame.pushGame();
    }

    public void discard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String cardName = discordGame.required(card).getAsString();

        game.getFaction(factionName).discard(cardName);
        discordGame.pushGame();
    }

    public void transferCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.transferCard(
                discordGame.required(faction).getAsString(),
                discordGame.required(recipient).getAsString(),
                discordGame.required(card).getAsString()
        );
        discordGame.pushGame();
    }

    public void transferCardFromDiscard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction receiver = game.getFaction(discordGame.required(faction).getAsString());
        String cardName = discordGame.required(discardCard).getAsString();
        game.transferTreacheryCardFromDiscard(receiver, cardName);
        discordGame.pushGame();
    }

    public void setHandLimit(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.getFaction(discordGame.required(faction).getAsString()).setHandLimit(discordGame.required(amount).getAsInt());
        discordGame.pushGame();
    }

    public void addCardToBiddingMarket(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        game.getBidding().addMissedCardToMarket(game);
        discordGame.pushGame();
    }

    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String winnerName = discordGame.required(faction).getAsString();
        String paidToFactionName = event.getOption("paid-to-faction", "Bank", OptionMapping::getAsString);
        int spentValue = discordGame.required(spent).getAsInt();
        boolean harkBonusBlocked = discordGame.optional(harkonnenKaramad) != null && discordGame.required(harkonnenKaramad).getAsBoolean();
        game.getBidding().assignAndPayForCard(game, winnerName, paidToFactionName, spentValue, harkBonusBlocked);
        discordGame.pushGame();
    }

    public static void awardTopBidder(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        boolean harkBonusBlocked = discordGame.optional(harkonnenKaramad) != null && discordGame.required(harkonnenKaramad).getAsBoolean();
        game.getBidding().awardTopBidder(game, harkBonusBlocked);
        discordGame.pushGame();
    }

    public void killLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(factionOrTanks).getAsString();
        Faction targetFaction = null;
        if (!factionName.equals("Tanks"))
            targetFaction = game.getFaction(factionName);
        String leaderName = discordGame.required(leader).getAsString();
        boolean isFaceDown = false;
        if (discordGame.optional(faceDown) != null)
            isFaceDown = discordGame.required(faceDown).getAsBoolean();
        game.killLeader(targetFaction, leaderName, isFaceDown);
        discordGame.pushGame();
    }

    /**
     * Revive a leader from the tanks and make payment to BT if applicable or to the spice bank
     *
     * @param discordGame the discord game
     * @param game        the game
     */
    public void reviveLeader(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderToRevive = discordGame.required(reviveLeader).getAsString();
        Integer cost = discordGame.optional(revivalCost) != null ? discordGame.required(revivalCost).getAsInt() : null;

        targetFaction.reviveLeader(leaderToRevive, cost);
        discordGame.pushGame();
    }

    /**
     * Revive forces from the tanks
     *
     * @param discordGame the discord game
     * @param game        the game
     */
    public void reviveForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        boolean isPaid = discordGame.required(paid).getAsBoolean();
        int revivedValue = discordGame.required(revived).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();
        game.reviveForces(targetFaction, isPaid, revivedValue, starredAmountValue);
        discordGame.pushGame();
    }

    /**
     * Place forces in a territory
     *
     * @param discordGame the discord game
     * @param game        the game
     */
    public void placeForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(territory).getAsString());
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();
        boolean isShipment = discordGame.required(CommandOptions.isShipment).getAsBoolean();
        boolean canTrigger = discordGame.required(CommandOptions.canTrigger).getAsBoolean();
        targetFaction.placeForces(targetTerritory, amountValue, starredAmountValue, isShipment, isShipment, canTrigger, game, false, false);
        discordGame.pushGame();
    }

    public void moveForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        Territory from = game.getTerritories().get(discordGame.required(fromTerritory).getAsString());
        Territory to = game.getTerritories().get(discordGame.required(toTerritory).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int starredAmountValue = discordGame.required(starredAmount).getAsInt();

        game.moveForces(targetFaction, from, to, amountValue, starredAmountValue, true);
        discordGame.pushGame();
    }

    public void endShipmentMovement(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        game.endShipmentMovement();
        RunCommands.advance(discordGame, game);
    }

    public void removeForcesEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String territoryName = discordGame.required(fromTerritory).getAsString();
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        int specialAmount = discordGame.required(starredAmount).getAsInt();
        if (amountValue < 0 || specialAmount < 0)
            throw new InvalidGameStateException("Negative numbers are invalid.");
        if (amountValue == 0 && specialAmount == 0) {
//            throw new InvalidGameStateException("Both force amounts cannot be 0.");
            targetFaction.checkForHighThreshold();
            discordGame.pushGame();
            return;
        }
        boolean isToTanks = discordGame.required(toTanks).getAsBoolean();
        boolean isKilledInBattle = discordGame.required(killedInBattle).getAsBoolean();

        game.removeForcesAndReportToTurnSummary(territoryName, targetFaction, amountValue, specialAmount, isToTanks, isKilledInBattle);
        discordGame.pushGame();
    }

    private void homeworldOccupy(DiscordGame discordGame, Game game) throws InvalidGameStateException {
        if (!game.hasGameOption(GameOption.HOMEWORLDS))
            throw new InvalidGameStateException("This game does not have Homeworlds.");
        String homeworldName = discordGame.required(homeworld).getAsString();
        String occupierName = discordGame.required(faction).getAsString();
        HomeworldTerritory homeworld = (HomeworldTerritory) game.getTerritory(homeworldName);
        if (homeworld.getNativeName().equals(occupierName))
            homeworld.clearOccupier();
        else
            homeworld.establishOccupier(occupierName);
    }

    private void assassinateTraitor(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getMoritaniFaction().assassinateTraitor();
        discordGame.pushGame();
    }

    public void assignTechToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String tt = discordGame.required(token).getAsString();
        Faction recipient = game.getFaction(discordGame.required(faction).getAsString());
        game.assignTechToken(tt, recipient);
        discordGame.pushGame();
    }

    public void bribe(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction fromFaction = game.getFaction(discordGame.required(faction).getAsString());
        Faction recipientFaction = game.getFaction(discordGame.required(recipient).getAsString());
        int amountValue = discordGame.required(amount).getAsInt();
        OptionMapping om = discordGame.optional(reason);
        String reasonString = om == null ? "" : om.getAsString();

        fromFaction.bribe(game, recipientFaction, amountValue, reasonString);
        discordGame.pushGame();
    }

    public void mute(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.setMute(!game.getMute());

        discordGame.pushGame();
    }

    private String getFactionDisplayString(Faction faction) {
        String message = faction.getEmoji() + " ** " + faction.getName() + " ** " + faction.getEmoji() + " - " + faction.getUserName() + "\n" +
                faction.getSpice() + " " + Emojis.SPICE + "    " + Emojis.TREACHERY + " ";
        message += String.join(", " + Emojis.TREACHERY + " ", faction.getTreacheryHand().stream().map(TreacheryCard::name).toList());
        message += "\nTraitors: ";
        message += String.join(", ", faction.getTraitorHand().stream().map(TraitorCard::getEmojiNameAndStrengthString).toList());
        message += "\nLeaders: ";
        message += String.join(", ", faction.getLeaders().stream().map(Leader::getNameAndValueString).toList());
        switch (faction) {
            case AtreidesFaction atreides -> {
                int khCount = atreides.getForcesLost();
                message += "\nKH count " + khCount + (khCount < 7 ? "" : "   The Sleeper has awoken!");
            }
            case BTFaction bt -> message += "\nRevealed Face Dancers: " + String.join(" ", bt.getRevealedFaceDancers().stream().map(TraitorCard::getEmojiNameAndStrengthString).toList());
            case EcazFaction ecaz -> message += "\nAmbassador Supply: " + String.join(" ", ecaz.getAmbassadorSupply().stream().map(Emojis::getFactionEmoji).toList());
            case MoritaniFaction moritani -> message += "\nTerror Token Supply: " + String.join(", ", moritani.getTerrorTokens());
            default -> {}
        }
        return message;
    }

    private void publishTerritoriesDisplayString(DiscordGame discordGame, List<Territory> territories) throws ChannelNotFoundException {
        for (Territory territory : territories) {
            if (territory.getSpice() == 0 && !territory.isStronghold() && territory.getForces().isEmpty())
                continue;
            String spiceString = territory.getSpice() == 0 ? "" : " " + territory.getSpice() + " " + Emojis.SPICE;
            String forcesString = " " + String.join(" ", territory.getForces().stream().map(f -> f.getStrength() + " " + Emojis.getForceEmoji(f.getName())).toList());
            if (territory.hasRicheseNoField())
                forcesString += " " + territory.getRicheseNoField() + " " + Emojis.NO_FIELD;
            discordGame.getModInfo().queueMessage(territory.getTerritoryName() + ": " +
                    spiceString + forcesString);
        }
    }

    public void displayGameState(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        switch (discordGame.required(data).getAsString()) {
            case "territories" -> {
                Collection<Territory> territories = game.getTerritories().values();
                discordGame.getModInfo().queueMessage("**Strongholds**");
                publishTerritoriesDisplayString(discordGame, territories.stream().filter(Territory::isStronghold).toList());
                if (game.hasGameOption(GameOption.HOMEWORLDS))
                    discordGame.getModInfo().queueMessage("**Homeworlds**");
                else
                    discordGame.getModInfo().queueMessage("**Reserves**");
                publishTerritoriesDisplayString(discordGame, territories.stream().filter(t -> t instanceof HomeworldTerritory).toList());
                discordGame.getModInfo().queueMessage("**Other territories**");
                publishTerritoriesDisplayString(discordGame, territories.stream().filter(t -> !t.isStronghold() && !(t instanceof HomeworldTerritory)).toList());
                discordGame.getModInfo().queueMessage("**Bene Tleilaxu Tanks**");
                if (!game.getTleilaxuTanks().getForces().isEmpty())
                    discordGame.getModInfo().queueMessage(String.join(" ", game.getTleilaxuTanks().getForces().stream().map(f -> f.getStrength() + " " + Emojis.getForceEmoji(f.getName())).toList()));
                if (!game.getLeaderTanks().isEmpty())
                    discordGame.getModInfo().queueMessage(String.join(", ", game.getLeaderTanks().stream().map(Leader::getEmoiNameAndValueString).toList()));
            }
            case "treachery" -> {
                String state = Emojis.TREACHERY + " Deck:\n";
                state += String.join(", ", game.getTreacheryDeck().stream().map(TreacheryCard::name).toList().reversed());
                state += "\n\n" + Emojis.TREACHERY + " Discard:\n";
                state += String.join(", ", game.getTreacheryDiscard().stream().map(TreacheryCard::name).toList().reversed());
                try {
                    Bidding bidding = game.getBidding();
                    state += "\n\nBidding market:\n";
                    state += String.join(", ", bidding.getMarket().stream().map(TreacheryCard::name).toList());
                } catch (InvalidGameStateException ignored) {}
                if (game.hasRicheseFaction()) {
                    state += "\n\n" + Emojis.RICHESE + " Cache:\n";
                    state += String.join(", ", game.getRicheseFaction().getTreacheryCardCache().stream().map(TreacheryCard::name).toList());
                }
                discordGame.getModInfo().queueMessage(state);
            }
            case "spice" -> {
                String state = Emojis.SPICE + " Deck:\n";
                state += String.join(", ", game.getSpiceDeck().stream().map(SpiceCard::name).toList());
                state += "\n\n" + Emojis.SPICE + " Discard A:\n";
                state += String.join(", ", game.getSpiceDiscardA().stream().map(SpiceCard::name).toList().reversed());
                state += "\n\n" + Emojis.SPICE + " Discard B:\n";
                state += String.join(", ", game.getSpiceDiscardB().stream().map(SpiceCard::name).toList().reversed());
                discordGame.getModInfo().queueMessage(state);
            }
            case "dnd" -> {
                String state = "Traitor Deck: ";
                state += String.join(", ", game.getTraitorDeck().stream().map(TraitorCard::getEmojiNameAndStrengthString).toList());
                if (game.hasGameOption(GameOption.LEADER_SKILLS))
                    state += "\n\nLeader Skills Deck:\n" + String.join(", ", game.getLeaderSkillDeck().stream().map(LeaderSkillCard::name).toList().reversed());
                state += "\n\nNexus Deck:\n" + String.join(", ", game.getNexusDeck().stream().map(NexusCard::name).toList());
                state += "\n\nNexus Discard:\n" + String.join(", ", game.getNexusDiscard().stream().map(NexusCard::name).toList());
                discordGame.getModInfo().queueMessage(state);
            }
            case "factions" -> {
                for (Faction f : game.getFactions())
                    discordGame.getModInfo().queueMessage(getFactionDisplayString(f));
//                String state = String.join("\n\n", game.getFactions().stream().map(this::getFactionDisplayString).toList());
//                discordGame.getModInfo().queueMessage(state);
            }
            case "etc" -> {
                String categoryName = "";
                Category category = null;
                MessageChannelUnion mcu = event.getChannel();
                if (mcu instanceof TextChannel)
                    category = mcu.asTextChannel().getParentCategory();
                else if (mcu instanceof ThreadChannel) {
                    GuildMessageChannelUnion gmcu = mcu.asThreadChannel().getParentMessageChannel();
                    if (gmcu instanceof TextChannel)
                        category = gmcu.asTextChannel().getParentCategory();
                    else if (gmcu instanceof NewsChannel)
                        category = gmcu.asNewsChannel().getParentCategory();
                }
                if (category != null)
                    categoryName = category.getName();
                String state = ReportsCommands.activeGame(event.getGuild(), game, categoryName, true);
                discordGame.getModInfo().queueMessage(state);
            }
        }
    }

    public void createAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction1 = game.getFaction(discordGame.required(faction).getAsString());
        Faction faction2 = game.getFaction(discordGame.required(otherFaction).getAsString());

        Alliance.createAlliance(discordGame, faction1, faction2);

        discordGame.pushGame();
    }

    public void removeAlliance(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());

        game.removeAlliance(targetFaction);

        discordGame.pushGame();
    }


    public void setSpiceInTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(territory).getAsString();
        int amountValue = discordGame.required(amount).getAsInt();

        game.getTerritories().get(territoryName).setSpice(amountValue);
        game.setUpdated(UpdateType.MAP);
        discordGame.pushGame();
    }

    public void destroyShieldWall(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.destroyShieldWall();
        discordGame.pushGame();
    }

    public void reassignFaction(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String playerName = discordGame.required(user).getAsUser().getAsMention();
        Member player = discordGame.required(user).getAsMember();

        if (player == null) throw new IllegalArgumentException("Not a valid user");

        String userName = player.getNickname();

        Faction faction = game.getFaction(factionName);
        faction.setPlayer(playerName);
        faction.setUserName(userName);

        discordGame.pushGame();
    }

    public void reassignMod(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.setMod(event.getUser().getAsMention());
        discordGame.pushGame();
    }

    public void teamMod(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean enableTeamMod = discordGame.required(teamModSwitch).getAsBoolean();
        game.setTeamMod(enableTeamMod);
        discordGame.pushGame();
    }

    public String waitingListItemResult(String itemEmoji, String choice) {
        return switch (choice) {
            case "Yes" -> itemEmoji + " :white_check_mark:";
            case "Maybe" -> itemEmoji + " :ok:";
            default -> itemEmoji + " :no_entry_sign:";
        };
    }

    public void waitingList(SlashCommandInteractionEvent event) {
        String userTag = event.getUser().getId();
        TextChannel textChannel = Objects.requireNonNull(event.getGuild()).getTextChannelsByName("waiting-list", true).getFirst();
        String message = "Speed: ";
        message += waitingListItemResult(":scooter:", DiscordGame.required(slowGame, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":blue_car:", DiscordGame.required(midGame, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":race_car:", DiscordGame.required(fastGame, event).getAsString());
        message += "\nExpansions: ";
        message += waitingListItemResult("O6", DiscordGame.required(originalSixFactions, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:bt:991763325576810546> <:ix:991763319406997514>", DiscordGame.required(ixianstleilaxuExpansion, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:choam:991763324624703538> <:rich:991763318467465337>", DiscordGame.required(choamricheseExpansion, event).getAsString());
        message += " -- ";
        message += waitingListItemResult("<:ecaz:1142126129105346590> <:moritani:1142126199775182879>", DiscordGame.required(ecazmoritaniExpansion, event).getAsString());
        message += "\nOptions: ";
        message += waitingListItemResult("<:weirding:991763071775297681>", DiscordGame.required(leaderSkills, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":european_castle:", DiscordGame.required(strongholdCards, event).getAsString());
        message += " -- ";
        message += waitingListItemResult(":ringed_planet:", DiscordGame.required(homeworlds, event).getAsString());
        message += "\nUser: <@" + userTag + ">";
        textChannel.sendMessage(message).queue();
        // textChannel.sendMessage("Speed: :turtle: " + event.getOption(slowGame.getName()).getAsBoolean() + " :racehorse: " + event.getOption(midGame.getName()).getAsBoolean() + " :race_car: " + event.getOption(fastGame.getName()).getAsBoolean() + "\nExpansions: <:bt:991763325576810546> <:ix:991763319406997514>  " + event.getOption(ixianstleilaxuExpansion.getName()).getAsBoolean() + " <:choam:991763324624703538> <:rich:991763318467465337> " + event.getOption(choamricheseExpansion.getName()).getAsBoolean() + " :ecaz: :moritani: " + event.getOption(ecazmoritaniExpansion.getName()).getAsBoolean() + "\nOptions: Leader Skills " + event.getOption(leaderSkills.getName()).getAsBoolean() + " Stronghold Cards " + event.getOption(strongholdCards.getName()).getAsBoolean() + "\nUser: <@" + userTag + ">").queue();
    }
}
