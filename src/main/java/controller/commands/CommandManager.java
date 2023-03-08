package controller.commands;

import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import io.github.cdimascio.dotenv.Dotenv;
import model.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import templates.ChannelPermissions;

import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        List<Role> roles = member == null ? new ArrayList<>() : member.getRoles();

        if (roles.stream().noneMatch(role ->
                role.getName().equals("Game Master") || role.getName().equals("Dungeon Master"))
        ) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        String name = event.getName();
        event.deferReply(true).queue();

        try {
            if (name.equals("newgame")) newGame(event);
            else if (name.equals("clean")) clean(event);
            else {
                DiscordGame discordGame = new DiscordGame(event);
                Game gameState = discordGame.getGameState();

                switch (name) {
                    case "show" -> ShowCommands.runCommand(event, discordGame, gameState);
                    case "setup" -> SetupCommands.runCommand(event, discordGame, gameState);
                    case "run" -> RunCommands.runCommand(event, discordGame, gameState);
                    case "resourceaddorsubtract" -> resourceAddOrSubtract(event, discordGame, gameState);
                    case "removeresource" -> removeResource(event, discordGame, gameState);
                    case "draw" -> drawCard(event, discordGame, gameState);
                    case "discard" -> discard(event, discordGame, gameState);
                    case "transfercard" -> transferCard(event, discordGame, gameState);
                    case "putback" -> putBack(event, discordGame, gameState);
                    case "placeforces" -> placeForces(event, discordGame, gameState);
                    case "moveforces" -> moveForces(event, discordGame, gameState);
                    case "removeforces" -> removeForces(event, discordGame, gameState);
                    case "display" -> displayGameState(event, discordGame, gameState);
                    case "reviveforces" -> revival(event, discordGame, gameState);
                    case "awardbid" -> awardBid(event, discordGame, gameState);
                    case "killleader" -> killLeader(event, discordGame, gameState);
                    case "reviveleader" -> reviveLeader(event, discordGame, gameState);
                    case "setstorm" -> setStorm(event, discordGame, gameState);
                    case "bgflip" -> bgFlip(event, discordGame, gameState);
                    case "bribe" -> bribe(event, discordGame, gameState);
                    case "mute" -> mute(discordGame, gameState);
                    case "advancegame" -> advanceGame(discordGame, gameState);
                    case "placehms" -> placeHMS(event, discordGame, gameState);
                    case "movehms" -> moveHMS(event, discordGame, gameState);
                    case "assigntechtoken" -> assignTechToken(event, discordGame, gameState);
                    case "draw-spice-blow" -> drawSpiceBlow(event, discordGame, gameState);
                }
            }
            event.getHook().editOriginal("Command Done").queue();
        } catch (ChannelNotFoundException e) {
            e.printStackTrace();
            event.getHook().editOriginal("Channel not found!").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("An error occurred!").queue();
            e.printStackTrace();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game gameState = discordGame.getGameState();
            event.replyChoices(CommandOptions.getCommandChoices(event, gameState)).queue();
        } catch (ChannelNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(CommandOptions.password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(CommandOptions.gameName, CommandOptions.gameRole, CommandOptions.modRole));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(CommandOptions.faction, CommandOptions.resourceName, CommandOptions.amount, CommandOptions.message));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(CommandOptions.faction, CommandOptions.resourceName));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(CommandOptions.deck, CommandOptions.faction));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(CommandOptions.faction, CommandOptions.card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(CommandOptions.faction, CommandOptions.card, CommandOptions.recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(CommandOptions.putBackCard, CommandOptions.bottom));
//        commandData.add(Commands.slash("advancegame", "Send the game to the next phase, turn, or card (in bidding round"));
        commandData.add(Commands.slash("placeforces", "Place forces from reserves onto the surface").addOptions(CommandOptions.faction, CommandOptions.amount, CommandOptions.isShipment, CommandOptions.starred, CommandOptions.territory));
        commandData.add(Commands.slash("moveforces", "Move forces from one territory to another").addOptions(CommandOptions.faction, CommandOptions.fromTerritory, CommandOptions.toTerritory, CommandOptions.amount, CommandOptions.starredAmount));
        commandData.add(Commands.slash("removeforces", "Remove forces from the board.").addOptions(CommandOptions.faction, CommandOptions.amount, CommandOptions.toTanks, CommandOptions.starred, CommandOptions.fromTerritory));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(CommandOptions.faction, CommandOptions.spent, CommandOptions.paidToFaction));
        commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(CommandOptions.faction, CommandOptions.revived, CommandOptions.starred));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(CommandOptions.data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(CommandOptions.sector));
        commandData.add(Commands.slash("killleader", "Send a leader to the tanks.").addOptions(CommandOptions.faction, CommandOptions.leader));
        commandData.add(Commands.slash("reviveleader", "Revive a leader from the tanks.").addOptions(CommandOptions.faction, CommandOptions.reviveLeader));
        commandData.add(Commands.slash("bgflip", "Flip BG forces to advisor or fighter.").addOptions(CommandOptions.bgTerritories));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(CommandOptions.faction, CommandOptions.recipient, CommandOptions.amount));
        commandData.add(Commands.slash("placehms", "Starting position for Hidden Mobile Stronghold").addOptions(CommandOptions.territory));
        commandData.add(Commands.slash("movehms", "Move Hidden Mobile Stronghold to another territory").addOptions(CommandOptions.territory));
        commandData.add(Commands.slash("assigntechtoken", "Assign a Tech Token to a Faction (taking it away from previous owner)").addOptions(CommandOptions.faction, CommandOptions.token));
        commandData.add(Commands.slash("draw-spice-blow", "Draw the spice blow").addOptions(CommandOptions.spiceBlowDeck));

        commandData.addAll(ShowCommands.getCommands());
        commandData.addAll(SetupCommands.getCommands());
        commandData.addAll(RunCommands.getCommands());

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException {
        Role gameRole = event.getOption("gamerole").getAsRole();
        Role modRole = event.getOption("modrole").getAsRole();
        Role observerRole = event.getGuild().getRolesByName("Observer", true).get(0);
        Role pollBot = event.getGuild().getRolesByName("EasyPoll", true).get(0);
        String name = event.getOption("name").getAsString();

        // Create category and set base permissions to deny everything for everyone except the mod role.
        // The channel permissions assume that this is set this way.
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(modRole, ChannelPermissions.all, null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, ChannelPermissions.all)
                .addPermissionOverride(gameRole, null, ChannelPermissions.all)
                .addPermissionOverride(observerRole, null, ChannelPermissions.all)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("chat")
                .addPermissionOverride(
                        observerRole,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .addPermissionOverride(
                        gameRole,
                        ChannelPermissions.readWriteAllow,
                        ChannelPermissions.readWriteDeny
                )
                .complete();

        // Not including Observer in pre-game-voting because there's no way to stop someone from adding to an
        // existing emoji reaction.
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(
                        gameRole,
                        ChannelPermissions.readAndReactAllow,
                        ChannelPermissions.readAndReactDeny
                )
                .addPermissionOverride(
                        pollBot,
                        ChannelPermissions.pollBotAllow,
                        ChannelPermissions.pollBotDeny
                )
                .complete();

        String[] readAndReactChannels  = {"turn-summary", "rules"};

        for (String channel : readAndReactChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .complete();
        }

        String[] readWriteChannels = {"game-actions", "bribes", "bidding-phase"};
        for (String channel : readWriteChannels) {
            category.createTextChannel(channel)
                    .addPermissionOverride(
                            observerRole,
                            ChannelPermissions.readAndReactAllow,
                            ChannelPermissions.readAndReactDeny
                    )
                    .addPermissionOverride(
                            gameRole,
                            ChannelPermissions.readWriteAllow,
                            ChannelPermissions.readWriteDeny
                    )
                    .complete();
        }

        String[] modChannels  = {"bot-data", "mod-info"};
        for (String channel : modChannels) {
            category.createTextChannel(channel).complete();
        }

        DiscordGame discordGame = new DiscordGame(event, category);
        discordGame.getTextChannel("rules").sendMessage("""
            <:DuneRulebook01:991763013814198292>  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
            <:weirding:991763071775297681>  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
            <:ix:991763319406997514> <:bt:991763325576810546>  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
            <:choam:991763324624703538> <:rich:991763318467465337> CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf""").queue();

        Game game = new Game();
        game.setGameRole(gameRole.getName());
        game.setModRole(modRole.getName());
        game.setMute(false);
        discordGame.setGameState(game);
        discordGame.pushGameState();
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = event.getOption("factionname").getAsString();
        String resourceName = event.getOption("resource").getAsString();
        int amount = event.getOption("amount").getAsInt();
        String message = event.getOption("message").getAsString();
        Faction faction = gameState.getFaction(factionName);

        if (resourceName.equalsIgnoreCase("spice")) {
            if (amount < 0) {
                faction.subtractSpice(-amount);
            } else {
                faction.addSpice(amount);
            }
            String gainsOrLoses = amount >= 0 ? " gains " : " loses ";
            discordGame.sendMessage(
                    "turn-summary",
                    MessageFormat.format(
                            "{0} {1} {2} <:spice4:991763531798167573> {3}",
                            faction.getEmoji(), gainsOrLoses, Math.abs(amount), message
                    )
            );
            spiceMessage(discordGame, Math.abs(amount), faction.getName(), message, amount >= 0);

            ShowCommands.writeFactionInfo(discordGame, gameState.getFaction(factionName));
            discordGame.pushGameState();
            return;
        }

        Resource resource = gameState.getFaction(factionName).getResource(resourceName);

        if (resource instanceof IntegerResource) {
            ((IntegerResource) resource).addValue(amount);
        } else {
            throw new InvalidGameStateException("Resource is not numeric");
        }

        ShowCommands.writeFactionInfo(discordGame, gameState.getFaction(factionName));
        discordGame.pushGameState();
    }

    public void removeResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.getFaction(event.getOption("factionname").getAsString()).removeResource(event.getOption("resource").getAsString());
        discordGame.pushGameState();
    }

    public void drawSpiceBlow(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String spiceBlowDeck = event.getOption(CommandOptions.spiceBlowDeck.getName()).getAsString();

        LinkedList<SpiceCard> deck = gameState.getSpiceDeck();
        LinkedList<SpiceCard> discard = spiceBlowDeck.equalsIgnoreCase("A") ? gameState.getSpiceDiscardA() : gameState.getSpiceDiscardB();

        StringBuilder message = new StringBuilder();

        SpiceCard drawn;

        message.append("**Spice Deck " + spiceBlowDeck + "**\n");

        do {
            if (deck.isEmpty()) {
                deck.addAll(gameState.getSpiceDiscardA());
                deck.addAll(gameState.getSpiceDiscardB());
                Collections.shuffle(deck);
                gameState.getSpiceDiscardA().clear();
                gameState.getSpiceDiscardB().clear();
            }

            drawn = deck.pop();
            discard.add(drawn);
            message.append(drawn.name() + "\n");
        } while (drawn.name().equalsIgnoreCase("Shai-Hulud"));

        if (gameState.getStorm() == drawn.sector())
            message.append(" (blown away by the storm!)");
        else
            gameState.getTerritories().get(drawn.name()).addSpice(drawn.spice());

        discordGame.pushGameState();

        discordGame.sendMessage("turn-summary", message.toString());
        ShowCommands.showBoard(discordGame, gameState);

    }

    public void drawCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.addTreacheryCard(gameState.getTreacheryDeck().pop());
        discordGame.pushGameState();
    }

    public void discard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        List<TreacheryCard> hand = faction.getTreacheryHand();
        int i = 0;
        for (; i < hand.size(); i++) {
            String card = hand.get(i).name();
            if (card.toLowerCase().contains(event.getOption("card").getAsString().toLowerCase())) {
                gameState.getTreacheryDiscard().add(hand.get(i));
                break;
            }
        }
        hand.remove(i);
        ShowCommands.writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public void transferCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction giver = gameState.getFaction(event.getOption("sender").getAsString());
        Faction receiver = gameState.getFaction(event.getOption("recipient").getAsString());
        List<TreacheryCard> giverHand = giver.getTreacheryHand();
        List<TreacheryCard> receiverHand = receiver.getTreacheryHand();

        if ((receiver.getHandLimit() == receiverHand.size())) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("The recipient's hand is full!").queue());
            return;
        }
        int i = 0;

        boolean cardFound = false;
        for (; i < giverHand.size(); i++) {
            String card = giverHand.get(i).name();
            if (card.toLowerCase().contains(event.getOption("card").getAsString())) {
                cardFound = true;
                receiverHand.add(giverHand.get(i));
                break;
            }
        }
        if (!cardFound) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Could not find that card!").queue());
            return;
        }
        giverHand.remove(i);
        ShowCommands.writeFactionInfo(discordGame, giver);
        ShowCommands.writeFactionInfo(discordGame, receiver);
        discordGame.pushGameState();
    }

    public void putBack(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        LinkedList<TreacheryCard> market = gameState.getMarket();
        int i = 0;
        boolean found = false;
        for (; i < market.size(); i++) {
            if (market.get(i).name().contains(event.getOption("putbackcard").getAsString())) {
                if (!event.getOption("bottom").getAsBoolean()) gameState.getTreacheryDeck().addLast(market.get(i));
                else gameState.getTreacheryDeck().addFirst(market.get(i));
                found = true;
                break;
            }
        }
        if (!found) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Card not found, are you sure it's there?").queue());
            return;
        }
        market.remove(i);
        Collections.shuffle(market);
        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
        }
        discordGame.pushGameState();
    }

    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction winner = gameState.getFaction(event.getOption("factionname").getAsString());
        String paidToFactionName = event.getOption("paid-to-faction", "Bank", OptionMapping::getAsString);
        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
        int spent = event.getOption("spent").getAsInt();

        String currentCard = MessageFormat.format(
                "R{0}:C{1}",
                gameState.getTurn(),
                gameState.getBidCardNumber()
        );

        discordGame.sendMessage("turn-summary",
                MessageFormat.format(
                        "{0} wins {1} for {2} <:spice4:991763531798167573>",
                        winner.getEmoji(),
                        currentCard,
                        spent
                )
        );

        // Winner pays for the card
        winner.subtractSpice(spent);
        spiceMessage(discordGame, spent, winner.getName(), currentCard, false);

        if (gameState.hasFaction(paidToFactionName)) {
            Faction paidToFaction = gameState.getFaction(paidToFactionName);
            spiceMessage(discordGame, spent, paidToFaction.getName(), currentCard, true);
            gameState.getFaction(paidToFaction.getName()).addSpice(spent);
            discordGame.sendMessage("turn-summary",
                    MessageFormat.format(
                            "{0} is paid {1} <:spice4:991763531798167573> for {2}",
                            paidToFaction.getEmoji(),
                            spent,
                            currentCard
                    )
            );
            ShowCommands.writeFactionInfo(discordGame, paidToFaction);
        }

        winnerHand.add(gameState.getBidCard());
        gameState.setBidCard(null);

        // Harkonnen draw an additional card
        if (winner.getName().equals("Harkonnen") && winnerHand.size() < winner.getHandLimit()) {
            if (gameState.getTreacheryDeck().isEmpty()) {
                List<TreacheryCard> treacheryDiscard = gameState.getTreacheryDiscard();
                discordGame.sendMessage("turn-summary", "The Treachery Deck has been replenished from the Discard Pile");
                gameState.getTreacheryDeck().addAll(treacheryDiscard);
                treacheryDiscard.clear();
            }

            gameState.drawCard("treachery deck", "Harkonnen");
            discordGame.sendMessage("turn-summary", winner.getEmoji() + " draws another card from the <:treachery:991763073281040518> deck.");
        }

        ShowCommands.writeFactionInfo(discordGame, winner);

        discordGame.pushGameState();
    }

//    public void awardBid2(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        Faction winner = gameState.getFaction(event.getOption("factionname").getAsString());
//        List<TreacheryCard> winnerHand = winner.getTreacheryHand();
//        int spent = event.getOption("spent").getAsInt();
//        LinkedList<TreacheryCard> market = gameState.getMarket();
//
//        // The name of the card to be awarded, for example, the second card in round 4 would be R4:C2.
//        String currentCard = MessageFormat.format(
//                "R{0}:C{1}",
//                gameState.getTurn(),
//                gameState.getBidMarketSize() - market.size() + 1
//        );
//
//        if (winner.getHandLimit() == winnerHand.size()) {
//            discordGame.sendMessage("mod-info", "Player's hand is full, they cannot bid on this card!");
//            return;
//        }
//
//        if (market.size() == 0) {
//            discordGame.sendMessage("mod-info", "No more cards up for bid.");
//            return;
//        }
//
//        discordGame.sendMessage("turn-summary",
//                MessageFormat.format(
//                        "{0} wins {1} for {2} <:spice4:991763531798167573>",
//                        winner.getEmoji(),
//                        currentCard,
//                        spent
//                )
//        );
//
//        // Winner pays for the card
//        winner.subtractSpice(spent);
//        spiceMessage(discordGame, spent, winner.getName(), currentCard, false);
//
//        // Emperor receives payment if they are in the game
//        if (gameState.hasFaction("Emperor") && !winner.getName().equals("Emperor")) {
//            spiceMessage(discordGame, spent, "emperor", currentCard, true);
//            gameState.getFaction("Emperor").addSpice(spent);
//            discordGame.sendMessage("turn-summary",
//                    MessageFormat.format(
//                            "{0} is paid {1} <:spice4:991763531798167573> for {2}",
//                            gameState.getFaction("Emperor").getEmoji(),
//                            spent,
//                            currentCard
//                    )
//            );
//            ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("Emperor"));
//        }
//
//        // Give the winner the card
//        winnerHand.add(market.pop());
//
//        // Harkonnen draw an additional card
//        if (winner.getName().equals("Harkonnen") && winnerHand.size() < winner.getHandLimit()) {
//            gameState.drawCard("treachery deck", "Harkonnen");
//            discordGame.sendMessage("turn-summary", winner.getEmoji() + " draws another card from the <:treachery:991763073281040518> deck.");
//        }
//
//        // Write the winner's information
//        ShowCommands.writeFactionInfo(discordGame, winner);
//
//        // Get the next card up for bid
//        if (market.size() > 0) {
//            // Show Atreides the next card
//            if (gameState.hasFaction("Atreides")) {
//                discordGame.sendMessage("atreides-chat",
//                        MessageFormat.format(
//                                "The next card up for bid is <:treachery:991763073281040518> {0} <:treachery:991763073281040518>",
//                                market.peek().name()
//                        )
//                );
//            }
//
//            // Setup the bidding order
//            StringBuilder message = new StringBuilder();
//            int cardNumber = gameState.getBidMarketSize() - market.size();
//            message.append("R").append(gameState.getTurn()).append(":C").append(cardNumber + 1).append("\n");
//            int firstBid = Math.ceilDiv(gameState.getStormMovement(), 3) + cardNumber;
//            for (int i = 0; i < gameState.getFactions().size(); i++) {
//                int playerPosition = (firstBid + i + 1) % gameState.getFactions().size();
//                List<Faction> turnOrder = gameState.getFactions();
//                Faction faction = turnOrder.get(playerPosition);
//                List<TreacheryCard> hand = faction.getTreacheryHand();
//                if (faction.getHandLimit() > hand.size()) {
//                    message.append(faction.getEmoji()).append(":");
//                    if (i == 0) message.append(" ").append(faction.getPlayer());
//                    message.append("\n");
//                }
//            }
//            discordGame.sendMessage("bidding-phase", message.toString());
//        }
//
//        discordGame.pushGameState();
//    }

    public static void spiceMessage(DiscordGame discordGame, int amount, String faction, String message, boolean plus) throws ChannelNotFoundException {
        String plusSign = plus ? "+" : "-";
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), plusSign + amount + "<:spice4:991763531798167573> " + message);
            }
        }
    }

    public void killLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        gameState.getLeaderTanks().add(faction.removeLeader(event.getOption("leadertokill").getAsString()));
        discordGame.pushGameState();
    }

    public void reviveLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.getLeaders().add(gameState.removeLeaderFromTanks(event.getOption("leadertorevive").getAsString()));
        discordGame.pushGameState();
    }

    public void revival(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        if (star.equals("")) faction.getReserves().addStrength(event.getOption("revived").getAsInt());
        else faction.getSpecialReserves().addStrength(event.getOption("revived").getAsInt());
        faction.subtractSpice(2 * event.getOption("revived").getAsInt());
        spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), faction.getName(), "Revivals", false);
        if (gameState.hasFaction("BT")) {
            gameState.getFaction("BT").addSpice(2 * event.getOption("revived").getAsInt());
            spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), "bt", faction.getEmoji() + " revivals", true);
            ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("BT"));
        }
        ShowCommands.writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public void advanceGame(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        //Turn 0 is for the set-up for play section from the rules page 6.
        if (gameState.getTurn() == 0) {
            switch (gameState.getPhase()) {
                //1. Positions
                case 0 -> {
                    Collections.shuffle(gameState.getTreacheryDeck());
                    Collections.shuffle(gameState.getSpiceDeck());
                    Collections.shuffle(gameState.getFactions());
                    gameState.advancePhase();
                    ShowCommands.showBoard(discordGame, gameState);
                    //If Bene Gesserit are present, time to make a prediction
                    if (gameState.hasFaction("BG")) {
                        discordGame.sendMessage("bg-chat", "Please make your secret prediction.");
                    }
                }
                //2. Traitors
                case 1 -> {
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


                        gameState.advancePhase();
                    //If Bene Tleilax are not present, advance past the Face Dancers draw
                    if (!gameState.hasFaction("BT")) {
                        gameState.advancePhase();
                    }
                    discordGame.sendMessage("turn-summary", "2. Traitors are being selected.");
                }
                //Bene Tleilax to draw Face Dancers
                case 2 -> {
                    Collections.shuffle(gameState.getTraitorDeck());
                    gameState.drawCard("traitor deck", "BT");
                    gameState.drawCard("traitor deck", "BT");
                    gameState.drawCard("traitor deck", "BT");
                    ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("BT"));
                    gameState.advancePhase();
                    discordGame.sendMessage("turn-summary", "2b. Bene Tleilax have drawn their Face Dancers.");
                }
                //3. Spice, 4. Forces
                case 3 -> {
                    if (gameState.hasFaction("Fremen")) {
                        discordGame.sendMessage("fremen-chat", "Please distribute 10 forces between Sietch Tabr, False Wall South, and False Wall West");
                    }
                    gameState.advancePhase();
                    //If BG is not present, advance past the next step
                    if (!gameState.hasFaction("BG")) {
                        gameState.advancePhase();
                    }
                    discordGame.sendMessage("turn-summary", "3. Spice has been allocated.\n4. Forces are being placed on the board.");
                }
                case 4 -> {
                    if (gameState.hasFaction("BG")) {
                        discordGame.sendMessage("bg-chat", "Please choose where to place your advisor.");
                    }

                    gameState.advancePhase();
                    //If Ix is not present, advance past the next step
                    if (!gameState.hasFaction("Ix")) {
                        gameState.advancePhase();
                    }
                }
                //Ix to select from starting treachery cards
                case 5 -> {
                    if (gameState.hasFaction("Ix")) {
                        int toDraw = gameState.getFactions().size();
                        if (gameState.hasFaction("Harkonnen")) toDraw++;
                        for (int i = 0; i < toDraw; i++) {
                            gameState.drawCard("treachery deck", "Ix");
                        }
                        ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("Ix"));
                        discordGame.sendMessage("ix-chat", "Please select one treachery card to keep in your hand.");
                        discordGame.sendMessage("turn-summary", "Ix is selecting their starting treachery card.");
                    }
                    gameState.advancePhase();
                }
                //5. Treachery
                case 6 -> {
                    for (Faction faction : gameState.getFactions()) {
                        if (!faction.getName().equals("Ix")) gameState.drawCard("treachery deck", faction.getName());
                        if (faction.getName().equals("Harkonnen")) gameState.drawCard("treachery deck", faction.getName());
                        ShowCommands.writeFactionInfo(discordGame, faction);
                    }
                    gameState.advancePhase();
                    discordGame.sendMessage("turn-summary", "5. Treachery cards are being dealt.");
                }
                //6. Turn Marker (prompt for dial for First Storm)
                case 7 -> {
                    discordGame.sendMessage(gameState.getFactions().get(0).getName().toLowerCase() + "-chat", "Please submit your dial for initial storm position.");
                    discordGame.sendMessage(gameState.getFactions().get(gameState.getFactions().size() - 1).getName().toLowerCase() + "-chat", "Please submit your dial for initial storm position.");
                    gameState.setStormMovement(new Random().nextInt(6) + 1);
                    gameState.advanceTurn();
                    discordGame.sendMessage("turn-summary", "6. Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");
                }
            }
            discordGame.pushGameState();
        }
        else {
            switch (gameState.getPhase()) {
                //1. Storm Phase
                case 1 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Storm Phase:");
                    Map<String, Territory> territories = gameState.getTerritories();
                   if (gameState.getTurn() != 1) {
                       discordGame.sendMessage("turn-summary", "The storm moves " + gameState.getStormMovement() + " sectors this turn.");
                       for (int i = 0; i < gameState.getStormMovement(); i++) {
                           gameState.setStorm(gameState.getStorm() + 1); 
                           if (gameState.getStorm() == 19) gameState.setStorm(1);
                           for (Territory territory : territories.values()) {
                               if (territory.isRock() || territory.getSector() != gameState.getStorm()) continue;
                               List<Force> forces = territory.getForces();
                               boolean fremenSpecialCase = forces.stream()
                                       .filter(force -> force.getName().startsWith("Fremen"))
                                       .count() == 2;
                               //Defaults to play "optimally", destorying Fremen regular forces over Fedaykin
                               if (fremenSpecialCase) {
                                   int fremenForces = 0;
                                   int fremenFedaykin = 0;
                                   for (Force force : forces) {
                                       if (force.getName().equals("Fremen")) fremenForces = force.getStrength();
                                       if (force.getName().equals("Fremen*")) fremenFedaykin = force.getStrength();
                                   }
                                   int lost = (fremenForces + fremenFedaykin) / 2;
                                   if (lost < fremenForces) {
                                       for (Force force : forces) {
                                           if (force.getName().equals("Fremen")) force.setStrength(force.getStrength() - lost);
                                       }
                                   } else if (lost > fremenForces) {
                                       forces.removeIf(force -> force.getName().equals("Fremen"));
                                       for (Force force : forces) {
                                           if (force.getName().equals("Fremen*")) force.setStrength(fremenFedaykin - lost + fremenForces);
                                       }
                                   }
                                   discordGame.sendMessage("turn-summary",gameState.getFaction("Fremen").getEmoji() + " lost " + lost +
                                                   " forces to the storm in " + territory.getTerritoryName());
                               }
                               List<Force> toRemove = new LinkedList<>();
                               for (Force force : forces) {
                                   if (force.getName().contains("Fremen") && fremenSpecialCase) continue;
                                   int lost = force.getStrength();
                                   if (force.getName().contains("Fremen") && lost > 1) {
                                       lost /= 2;
                                       force.setStrength(lost);
                                       forces.add(force);
                                   } else toRemove.add(force);
                                   gameState.getTanks().stream().filter(force1 -> force1.getName().equals(force.getName())).findFirst().orElseThrow().addStrength(force.getStrength());
                                   discordGame.sendMessage("turn-summary",
                                           gameState.getFaction(force.getName().replace("*", "")).getEmoji() + " lost " +
                                                   lost + " forces to the storm in " + territory.getTerritoryName());
                               }
                               forces.removeAll(toRemove);
                               territory.setSpice(0);
                           }
                       }
                   }
                   if (gameState.hasFaction("Fremen")) {
                       gameState.setStormMovement(new Random().nextInt(6) + 1);
                       discordGame.sendMessage("fremen-chat", "The storm will move " + gameState.getStormMovement() + " sectors next turn.");

                   }
                   ShowCommands.showBoard(discordGame, gameState);

                   gameState.advancePhase();
                }
                //2. Spice Blow and Nexus
                case 2 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Blow Phase:");
                    gameState.advancePhase();
                }
                //3. Choam Charity
                case 3 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " CHOAM Charity Phase:");
                    int multiplier = 1;
                    if (gameState.getResources().stream().anyMatch(resource -> resource.getName().equals("inflation token"))) {
                        if (gameState.getResources().stream().filter(resource -> resource.getName().equals("inflation token")).findFirst().orElseThrow().getValue().equals("cancel")) {
                            discordGame.sendMessage("turn-summary","CHOAM Charity is cancelled!");
                            gameState.advancePhase();
                            break;
                        } else {
                            multiplier = 2;
                        }
                    }

                    int choamGiven = 0;
                    List<Faction> factions = gameState.getFactions();
                    if (factions.stream().anyMatch(f -> f.getName().equals("CHOAM"))) discordGame.sendMessage("turn-summary",
                            gameState.getFaction("CHOAM").getEmoji() + " receives " +
                            gameState.getFactions().size() * 2 * multiplier + " <:spice4:991763531798167573> in dividends from their many investments."
                    );
                    for (Faction faction : factions) {
                        if (faction.getName().equals("CHOAM")) continue;
                        int spice = faction.getSpice();
                        if (faction.getName().equals("BG")) {
                            choamGiven += 2 * multiplier;
                            faction.addSpice(2 * multiplier);
                            discordGame.sendMessage("turn-summary", faction.getEmoji() + " have received " + 2 * multiplier + " <:spice4:991763531798167573> in CHOAM Charity.");
                            spiceMessage(discordGame, 2 * multiplier, faction.getName(), "CHOAM Charity", true);
                        }
                        else if (spice < 2) {
                            int charity = (2 * multiplier) - (spice * multiplier);
                            choamGiven += charity;
                            faction.addSpice(charity);
                            discordGame.sendMessage("turn-summary",
                                    faction.getEmoji() + " have received " + charity + " <:spice4:991763531798167573> in CHOAM Charity."
                            );
                            spiceMessage(discordGame, charity, faction.getName(), "CHOAM Charity", true);
                        }
                        else continue;
                        ShowCommands.writeFactionInfo(discordGame, faction);
                    }
                    if (gameState.hasFaction("CHOAM")) {
                        gameState.getFaction("CHOAM").addSpice((2 * factions.size() * multiplier) - choamGiven);
                        spiceMessage(discordGame, gameState.getFactions().size() * 2 * multiplier, "choam", "CHOAM Charity", true);
                        discordGame.sendMessage("turn-summary",
                                gameState.getFaction("CHOAM").getEmoji() + " has paid " + choamGiven + " <:spice4:991763531798167573> to factions in need."
                        );
                        spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
                    }
                    gameState.advancePhase();
                }
                //4. Bidding
                case 4 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Bidding Phase:");
                    int cardsUpForBid = 0;
                    List<Faction> factions = gameState.getFactions();
                    StringBuilder countMessage = new StringBuilder();
                    countMessage.append("<:treachery:991763073281040518>Number of Treachery Cards<:treachery:991763073281040518>\n");
                    for (Faction faction : factions) {
                        int length = faction.getTreacheryHand().size();
                        countMessage.append(faction.getEmoji()).append(": ").append(length).append("\n");
                        if (faction.getHandLimit() > length) cardsUpForBid++;
                        if (faction.getName().equals("Rich")) cardsUpForBid--;
                    }
                    if (gameState.hasFaction("Ix")) {
                        discordGame.sendMessage("ix-chat", "Please select a card to put back to top or bottom.");
                    }
                    countMessage.append("There will be ").append(cardsUpForBid).append(" <:treachery:991763073281040518> cards up for bid this round.");
                    discordGame.sendMessage("turn-summary", countMessage.toString());
                    gameState.setBidMarketSize(cardsUpForBid);
                    if (gameState.hasFaction("Ix")) {
                        cardsUpForBid++;
                        discordGame.sendMessage("turn-summary", gameState.getFaction("Ix").getEmoji() + " to place a card back on the top or bottom of the deck.");
                    }
                    for (int i = 0; i < cardsUpForBid; i++) {
                        gameState.getMarket().add(gameState.getTreacheryDeck().pop());
                        if (gameState.hasFaction("Ix")) {
                            discordGame.sendMessage("ix-chat", "<:treachery:991763073281040518> " +
                                    gameState.getMarket().get(i).name() + " <:treachery:991763073281040518>");
                        }
                    }
                    if (gameState.hasFaction("Atreides") && !gameState.hasFaction("Ix")) {
                        discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getMarket().peek().name() + " <:treachery:991763073281040518>");
                    }
                    StringBuilder message = new StringBuilder();
                    message.append("R").append(gameState.getTurn()).append(":C1\n");
                    int firstBid = Math.ceilDiv(gameState.getStorm(), 3);
                    for (int i = 0; i < factions.size(); i++) {
                        int playerPosition = firstBid + i > factions.size() - 1 ? firstBid + i - factions.size() : firstBid + i;
                        Faction faction = gameState.getFactions().get(playerPosition);
                        if (faction.getHandLimit() > faction.getTreacheryHand().size()) message.append(faction.getEmoji()).append(":");
                        if (i == 0) message.append(" ").append(faction.getPlayer());
                        message.append("\n");
                    }
                    discordGame.sendMessage("bidding-phase", message.toString());
                    gameState.advancePhase();
                }
                //5. Revival
                case 5 -> {
                    if (gameState.getMarket().size() > 0) {
                        int marketLength = gameState.getMarket().size();
                        discordGame.sendMessage("turn-summary", "There were " + marketLength + " cards not bid on this round that are placed back on top of the <:treachery:991763073281040518> deck.");
                        while (!gameState.getMarket().isEmpty()) {
                            gameState.getTreacheryDeck().add(gameState.getMarket().pop());
                        }
                    }
                    gameState.setBidMarketSize(0);
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Revival Phase:");
                    List<Faction> factions = gameState.getFactions();
                    StringBuilder message = new StringBuilder();
                    message.append("Free Revivals:\n");
                    for (Faction faction : factions) {
                        int revived = 0;
                        boolean revivedStar = false;
                        for (int i = faction.getFreeRevival(); i > 0; i--) {
                            if (gameState.getForceFromTanks(faction.getName()).getStrength() == 0
                                    && gameState.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                            revived++;
                            if (gameState.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                                Force force = gameState.getForceFromTanks(faction.getName() + "*");
                                force.setStrength(force.getStrength() - 1);
                                revivedStar = true;
                                faction.getSpecialReserves().setStrength(faction.getSpecialReserves().getStrength() + 1);
                            } else if (gameState.getForceFromTanks(faction.getName()).getStrength() > 0) {
                                Force force = gameState.getForceFromTanks(faction.getName());
                                force.setStrength(force.getStrength() - 1);
                                faction.getReserves().setStrength(faction.getReserves().getStrength() + 1);
                            }
                        }
                        if (revived > 0) {
                            message.append(gameState.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
                        }
                    }
                    discordGame.sendMessage("turn-summary", message.toString());
                    ShowCommands.showBoard(discordGame, gameState);
                    gameState.advancePhase();
                }
                //6. Shipment and Movement
                case 6 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
                    if (gameState.hasFaction("Atreides")) {
                        discordGame.sendMessage("atreides-info", "You see visions of " + gameState.getSpiceDeck().peek().name() + " in your future.");
                    }
                    if(gameState.hasFaction("BG")) {
                       for (Territory territory : gameState.getTerritories().values()) {
                           if (territory.getForce("Advisor").getStrength() > 0) discordGame.sendMessage("turn-summary",gameState
                                   .getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
                       }
                    }
                    ShowCommands.showBoard(discordGame, gameState);
                    gameState.advancePhase();
                }
                //7. Battle
                case 7 -> {
                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");

                    // Get list of territories with multiple factions
                    List<Pair<Territory, List<Faction>>> battles = new ArrayList<>();
                    for (Territory territory : gameState.getTerritories().values()) {
                        List<Force> forces = territory.getForces();
                        List<Faction> factions = forces.stream()
                                .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                                .map(Force::getFactionName)
                                .distinct()
                                .sorted(Comparator.comparingInt(gameState::getFactionTurnIndex))
                                .map(gameState::getFaction)
                                .toList();
                        ;

                        if (factions.size() > 1) {
                            battles.add(new ImmutablePair<>(territory, factions));
                        }
                    }

                    if(battles.size() > 0) {
                        String battleMessages = battles.stream()
                            .sorted(Comparator
                                    .comparingInt(o -> gameState.getFactionTurnIndex(o.getRight().get(0).getName()))
                            ).map((battle) ->
                                    MessageFormat.format("{0} in {1}",
                                            battle.getRight().stream()
                                                    .map(Faction::getEmoji)
                                                    .collect(Collectors.joining(" vs ")),
                                            battle.getLeft().getTerritoryName()
                                    )
                            ).collect(Collectors.joining("\n"));

                        discordGame.sendMessage("turn-summary",
                                "The following battles will take place this turn:\n" + battleMessages
                                );
                    } else {
                        discordGame.sendMessage("turn-summary", "There are no battles this turn.");
                    }
                    ShowCommands.showBoard(discordGame, gameState);
                    gameState.advancePhase();
                }
                // 8. Spice Harvest
                case 8 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
                   Map<String, Territory> territories = gameState.getTerritories();
                   for (Territory territory : territories.values()) {
                       if (territory.getForces().size() != 1) continue;
                       if (territory.getForces().get(0).getName().equals("Advisor")) {
                           int strength = territory.getForces().get(0).getStrength();
                           territory.getForces().clear();
                           territory.getForces().add(new Force("BG", strength));
                           discordGame.sendMessage("turn-summary", "Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
                       }
                   }

                   Set<Faction> factionsWithChanges = new HashSet<>();

                   for (Faction faction : gameState.getFactions()) {
                       faction.setHasMiningEquipment(false);
                       if (territories.get("Arrakeen").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           faction.addSpice(2);
                           spiceMessage(discordGame, 2, faction.getName(), "for Arrakeen", true);
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Arrakeen");
                           faction.setHasMiningEquipment(true);
                           factionsWithChanges.add(faction);
                       }
                       if (territories.get("Carthag").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           faction.addSpice(2);
                           spiceMessage(discordGame, 2, faction.getName(), "for Carthag", true);
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Carthag");
                           faction.setHasMiningEquipment(true);
                           factionsWithChanges.add(faction);
                       }
                       if (territories.get("Tuek's Sietch").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                           discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 1 <:spice4:991763531798167573> from Tuek's Sietch");
                           faction.addSpice(1);
                           spiceMessage(discordGame, 1, faction.getName(), "for Tuek's Sietch", true);
                           factionsWithChanges.add(faction);
                       }
                   }

                    for (Territory territory: territories.values()) {
                        if (territory.getSpice() == 0 || territory.getForces().size() == 0) continue;
                        int totalStrength = 0;
                        Faction faction = gameState.getFaction(territory.getForces().stream().filter(force -> !force.getName().equals("Advisor")).findFirst().orElseThrow().getName().replace("*", ""));
                        for (Force force : territory.getForces()) {
                            if (force.getName().equals("Advisor")) continue;
                            totalStrength += force.getStrength();
                        }
                        int multiplier = faction.hasMiningEquipment() ? 3 : 2;
                        int spice = Math.min(multiplier * totalStrength, territory.getSpice());
                        faction.addSpice(spice);
                        spiceMessage(discordGame, spice, faction.getName(), "for Spice Blow", true);
                        factionsWithChanges.add(faction);
                        territory.setSpice(territory.getSpice() - spice);
                        discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects " + spice + " <:spice4:991763531798167573> from " + territory.getTerritoryName());
                    }

                    for (Faction faction : factionsWithChanges) {
                        ShowCommands.writeFactionInfo(discordGame, faction);
                    }
                    ShowCommands.showBoard(discordGame, gameState);
                    gameState.advancePhase();
                }
                //TODO: 9. Mentat Pause
                case 9 -> {
                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
                    for (Faction faction : gameState.getFactions()) {
                        if (faction.getFrontOfShieldSpice() > 0) {
                            discordGame.sendMessage("turn-summary", faction.getEmoji() + " collects " +
                                    faction.getFrontOfShieldSpice() + " <:spice4:991763531798167573> from front of shield.");
                            spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                            faction.addSpice(faction.getFrontOfShieldSpice());
                            faction.setFrontOfShieldSpice(0);
                            ShowCommands.writeFactionInfo(discordGame, faction);
                        }
                    }
                    gameState.advanceTurn();
                }
            }
            discordGame.pushGameState();
        }
    }

    public void placeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        boolean star = event.getOption("starred").getAsBoolean();
        Territory territory = gameState.getTerritories().get(event.getOption("territory").getAsString());
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        int amount = event.getOption("amount").getAsInt();

        Force reserves = star ? faction.getSpecialReserves() : faction.getReserves();
        if (reserves.getStrength() < amount) {
            discordGame.sendMessage("mod-info", "This faction does not have enough forces in reserves!");
            return;
        }
        reserves.setStrength(reserves.getStrength() - amount);

        Force force = territory.getForce(reserves.getName());
        if (force.getStrength() == 0) territory.getForces().add(force);
        force.addStrength(amount);

        if (event.getOption("isshipment").getAsBoolean()) {
            int costPerForce = territory.isStronghold() ? 1 : 2;
            int cost = costPerForce * amount;

            // Guild has half price shipping
            if (faction.getName().equalsIgnoreCase("Guild"))
                cost = Math.ceilDiv(cost, 2);

            faction.subtractSpice(cost);
            spiceMessage(discordGame, cost, faction.getName(), "shipment to " + territory.getTerritoryName(), false);
            if (gameState.hasFaction("Guild") && !(faction.getName().equals("Guild") || faction.getName().equals("Fremen"))) {
                gameState.getFaction("Guild").addSpice(cost);
                spiceMessage(discordGame, cost, "guild", faction.getEmoji() + " shipment", true);
                ShowCommands.writeFactionInfo(discordGame, gameState.getFaction("Guild"));
            }
            ShowCommands.writeFactionInfo(discordGame, faction);
        }

        discordGame.pushGameState();
    }

    public void moveForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        Territory from = gameState.getTerritories().get(event.getOption("from").getAsString());
        Territory to = gameState.getTerritories().get(event.getOption("to").getAsString());
        int amount = event.getOption("amount").getAsInt();
        int starredAmount = event.getOption("starredamount").getAsInt();

        from.setForceStrength(faction.getName(), from.getForce(faction.getName()).getStrength() - amount);
        from.setForceStrength(faction.getName() + "*", from.getForce(faction.getName() + "*").getStrength() - starredAmount);

        to.setForceStrength(faction.getName(), to.getForce(faction.getName()).getStrength() + amount);
        to.setForceStrength(faction.getName() + "*", to.getForce(faction.getName() + "*").getStrength() + starredAmount);

        discordGame.pushGameState();
    }

    public void removeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("from").getAsString());
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        int amount = event.getOption("amount").getAsInt();
        String starred = event.getOption("starred").getAsBoolean() ? "*" : "";
        String forceName = faction.getName() + starred;
        Force force = territory.getForce(forceName);
        if (force.getStrength() > amount) {
            force.setStrength(force.getStrength() - amount);
        } else if (force.getStrength() < amount) {
            discordGame.sendMessage("mod-info","You are trying to remove more forces than this faction has in this territory! Please check your info and try again.");
            return;
        } else {
            territory.getForces().remove(force);
        }

        if (event.getOption("totanks").getAsBoolean()) {
            gameState.getForceFromTanks(forceName).addStrength(amount);
        } else {
            if (starred.equals("*")) faction.getSpecialReserves().addStrength(amount);
            else faction.getReserves().addStrength(amount);
        }
        discordGame.pushGameState();
    }

    public void setStorm(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        gameState.setStorm(event.getOption("sector").getAsInt());
        discordGame.sendMessage("turn-summary","The storm has been initialized to " + event.getOption("sector").getAsInt() + " sectors");
        if (gameState.hasTechTokens()) {
            List<TechToken> techTokens = new LinkedList<>();
            if (gameState.hasFaction("BT")) {
                gameState.getFaction("BT").getTechTokens().add(new TechToken("Axlotl Tanks"));
            } else techTokens.add(new TechToken("Axlotl Tanks"));
            if (gameState.hasFaction("Ix")) {
                gameState.getFaction("Ix").getTechTokens().add(new TechToken("Heighliners"));
            } else techTokens.add(new TechToken("Heighliners"));
            if (gameState.hasFaction("Fremen")) {
                gameState.getFaction("Fremen").getTechTokens().add(new TechToken("Spice Production"));
            } else techTokens.add(new TechToken("Spice Production"));
            if (!techTokens.isEmpty()) {
                Collections.shuffle(techTokens);
                for (int i = 0; i < techTokens.size(); i++) {
                    Faction faction = gameState.getFactions().get((Math.ceilDiv(gameState.getStorm(), 3) - 1 + i) % 6);
                    faction.getTechTokens().add(techTokens.get(i));
                }
            }
        }
        discordGame.pushGameState();
        ShowCommands.showBoard(discordGame, gameState);
    }

    public void bgFlip(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("bgterritories").getAsString());
        int strength = 0;
        String found = "";
        for (Force force : territory.getForces()) {
            if (force.getName().equals("BG") || force.getName().equals("Advisor")) {
               strength = force.getStrength();
               found = force.getName();
            }
        }
        territory.getForces().removeIf(force -> force.getName().equals("BG") || force.getName().equals("Advisor"));
        if (found.equals("Advisor")) territory.getForces().add(new Force("BG", strength));
        else if (found.equals("BG")) territory.getForces().add(new Force("Advisor", strength));
        else {
            discordGame.sendMessage("mod-info","No Bene Gesserit were found in that territory.");
            return;
        }
        discordGame.pushGameState();
        ShowCommands.showBoard(discordGame, gameState);
    }

    public void placeHMS(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Territory territory = gameState.getTerritories().get(event.getOption("territory").getAsString());
        territory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
        discordGame.pushGameState();
        ShowCommands.showBoard(discordGame, gameState);
    }

    public void moveHMS(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Territory territory : gameState.getTerritories().values()) {
            territory.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
        }
        placeHMS(event, discordGame, gameState);
    }

    public void assignTechToken(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        for (Faction faction : gameState.getFactions()) {
            faction.getTechTokens().removeIf(techToken -> techToken.getName().equals(event.getOption("token").getAsString()));
        }
        gameState.getFaction(event.getOption("factionname").getAsString()).getTechTokens().add(new TechToken(event.getOption("token").getAsString()));
        discordGame.sendMessage("turn-summary", event.getOption("token").getAsString() + " has been transferred to " + gameState.getFaction(event.getOption("factionname").getAsString()).getEmoji());
        ShowCommands.showBoard(discordGame, gameState);
        discordGame.pushGameState();
    }

    public void bribe(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        Faction recipient = gameState.getFaction(event.getOption("recipient").getAsString());
        int amount = event.getOption("amount").getAsInt();

        if (faction.getSpice() < amount) {
            discordGame.getTextChannel("mod-info").sendMessage("Faction does not have enough spice to pay the bribe!").queue();
            return;
        }
        faction.subtractSpice(amount);
        spiceMessage(discordGame, amount, faction.getName(), "bribe to " + recipient.getEmoji(), false);

        discordGame.sendMessage(
                "turn-summary",
                MessageFormat.format(
                        "{0} places {1} <:spice4:991763531798167573> in front of {2} shield.",
                        faction.getEmoji(), amount, recipient.getEmoji()
                )
        );

        recipient.addFrontOfShieldSpice(amount);
        ShowCommands.writeFactionInfo(discordGame, faction);
        discordGame.pushGameState();
    }

    public void mute(DiscordGame discordGame, Game gameState) {
        gameState.setMute(!gameState.getMute());

        discordGame.pushGameState();
    }

    public void displayGameState(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        TextChannel channel = discordGame.getTextChannel("mod-info");
        switch (event.getOption("data").getAsString()) {
            case "territories" -> {
               Map<String, Territory> territories = gameState.getTerritories();
               for (Territory territory: territories.values()) {
                   if (territory.getSpice() == 0 && !territory.isStronghold() && territory.getForces().isEmpty()) continue;
                   discordGame.sendMessage(channel.getName(), "**" + territory.getTerritoryName() + "** \n" +
                           "Spice: " + territory.getSpice() + "\nForces: " + territory.getForces().toString());
               }
            }
            case "dnd" -> {
                discordGame.sendMessage("mod-info", gameState.getTreacheryDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getTreacheryDiscard().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDiscardA().toString());
                discordGame.sendMessage("mod-info", gameState.getSpiceDiscardB().toString());
                discordGame.sendMessage("mod-info", gameState.getLeaderSkillDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getTraitorDeck().toString());
                discordGame.sendMessage("mod-info", gameState.getMarket().toString());
            }
            case "factions" -> {
                for (Faction faction: gameState.getFactions()) {
                    StringBuilder message = new StringBuilder();
                    message.append("**").append(faction.getName()).append(":**\nPlayer: ").append(faction.getUserName()).append("\n");
                    message.append("spice: ").append(faction.getSpice()).append("\nTreachery Cards: ").append(faction.getTreacheryHand())
                            .append("\nTraitors:").append(faction.getTraitorHand()).append("\nLeaders: ").append(faction.getLeaders()).append("\n");
                    for (Resource resource : faction.getResources()) {
                        message.append(resource.getName()).append(": ").append(resource.getValue()).append("\n");
                    }
                    discordGame.sendMessage(channel.getName(), message.toString());
                }
            }
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public void clean(SlashCommandInteractionEvent event) {
        if (!event.getOption("password").getAsString().equals(Dotenv.configure().load().get("PASSWORD"))) {
            event.getChannel().sendMessage("""
                    You have attempted the forbidden command.

                    ...Or you're Voiceofonecrying and you fat-fingered the password""").queue();
            return;
        }
        List<Category> categories = event.getGuild().getCategories();
        for (Category category : categories) {
            //if (!category.getName().startsWith("test")) continue;
            category.delete().complete();
        }
        List<TextChannel> channels = event.getGuild().getTextChannels();
        for (TextChannel channel : channels) {
            if (//!channel.getName().startsWith("test") ||
            channel.getName().equals("general")) continue;
            channel.delete().complete();
        }
    }
}
