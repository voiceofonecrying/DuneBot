package controller.commands;

import controller.Initializers;
import io.github.cdimascio.dotenv.Dotenv;
import model.Faction;
import model.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        List<Role> roles = event.getMember().getRoles();
        boolean isGameMaster = false;
        for (Role role : roles) {
            if (role.getName().equals("Game Master") || role.getName().equals("Dungeon Master")) {
                isGameMaster = true;
                break;
            }
        }
        if (!isGameMaster) {
            event.reply("You are not a Game Master!").setEphemeral(true).queue();
            return;
        }

        String name = event.getName();
        event.reply("processing...").setEphemeral(true).queue();
        switch (name) {
            case "newgame" -> newGame(event);
            case "addfaction" -> addFaction(event);
            case "newfactionresource" -> newFactionResource(event);
            case "resourceaddorsubtract" -> resourceAddOrSubtract(event);
            case "removeresource" -> removeResource(event);
            case "draw" -> drawCard(event);
            case "peek" -> peek(event);
            case "discard" -> discard(event);
            case "transfercard" -> transferCard(event);
            case "putback" -> putBack(event);
            case "start" -> startGame(event);
            case "clean" -> clean(event);

        }
        //implement new slash commands here

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {

        OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
        OptionData role = new OptionData(OptionType.ROLE, "role", "The role you created for the players of this game", true);
        OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
        OptionData game = new OptionData(OptionType.CHANNEL, "game", "The game this action will be applied to", true).setChannelTypes(ChannelType.CATEGORY);
        OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich");
        OptionData resourceName = new OptionData(OptionType.STRING, "resource", "The name of the resource", true);
        OptionData isNumber = new OptionData(OptionType.BOOLEAN, "isanumber", "Set true if it is a numerical value, false otherwise", true);
        OptionData resourceValNumber = new OptionData(OptionType.INTEGER, "numbervalue", "Set the initial value if the resource is a number (leave blank otherwise)");
        OptionData resourceValString = new OptionData(OptionType.STRING, "othervalue", "Set the initial value if the resource is not a number (leave blank otherwise)");
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "amount to be added or subtracted (e.g. -3, 4)", true);
        OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you", true);
        OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
                .addChoice("Spice Deck", "spice_deck")
                .addChoice("Treachery Deck", "treachery_deck")
                .addChoice("Traitor Deck", "traitor_deck");
        OptionData destination = new OptionData(OptionType.STRING, "destination", "Where the card is being drawn to (name a faction to draw to hand, or 'discard')", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("discard", "discard");
        OptionData numberToPeek = new OptionData(OptionType.STRING, "number", "The number of cards you want to peek (default is 1).");
        OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true);
        OptionData sender = new OptionData(OptionType.STRING, "sender", "The one giving the card", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("Add from discard", "discard");
        OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The one receiving the card", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("Place up for bid", "market");
        OptionData bottom = new OptionData(OptionType.BOOLEAN, "bottom", "Place on bottom?", true);
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName, role));
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(faction, user, game));
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, game, resourceName, isNumber, resourceValNumber, resourceValString));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(game, faction, resourceName, amount));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(game, faction, resourceName));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(game, deck, destination));
        commandData.add(Commands.slash("start", "Begin startup sequence for a new game").addOptions(game));
        commandData.add(Commands.slash("peek", "Peek at the top n number of cards of a deck without moving them.").addOptions(game, deck, numberToPeek));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(game, faction, card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(game, sender, card, recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(game, card, bottom));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) {

        String name = event.getOption("name").getAsString();
        event.getGuild().createCategory(name).addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getGuild().getRolesByName("Bot Testers", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(event.getOption("role").getAsRole().getName(), true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("test-bot-data").addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getGuild().getBotRole(), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();
        category.createTextChannel("test-out-of-game-chat").complete();
        category.createTextChannel("test-in-game-chat").complete();
        category.createTextChannel("test-turn-summary").complete();
        category.createTextChannel("test-game-actions").complete();
        category.createTextChannel("test-bribes").complete();
        category.createTextChannel("test-bidding-phase").complete();
        category.createTextChannel("test-rules").complete();
        category.createTextChannel("test-pre-game-voting").complete();

        TextChannel rules = category.getTextChannels().get(7);
        rules.sendMessage("""
            <:DuneRulebook01:991763013814198292>  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
            <:weirding:991763071775297681>  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
            <:ix:991763319406997514> <:bt:991763325576810546>  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
            <:choam:991763324624703538> <:rich:991763318467465337> CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf""").queue();

        Game game = new Game();
        JSONObject gameState = new JSONObject();
        JSONObject gameResources = new JSONObject();
        JSONObject gameBoard = Initializers.buildBoard();
        JSONArray spiceDeck = Initializers.buildSpiceDeck();
        JSONArray treacheryDeck = Initializers.buildTreacheryDeck();
        shuffle(spiceDeck);
        shuffle(treacheryDeck);

        gameResources.put("turn", 0);
        gameResources.put("storm", 1);
        gameResources.put("shieldwallbroken", false);
        gameResources.put("traitor_deck", new JSONArray());
        gameResources.put("spice_deck", spiceDeck);
        gameResources.put("spice_discardA", new JSONArray());
        gameResources.put("spice_discardB", new JSONArray());
        gameResources.put("treachery_deck", treacheryDeck);
        gameResources.put("market", new JSONArray());
        gameResources.put("treachery_discard", new JSONArray());
        gameResources.put("tanks_forces", new JSONObject());
        gameResources.put("tanks_leaders", new JSONObject());
        gameResources.put("turn_order", new JSONObject());
        gameState.put("factions", new JSONObject());
        gameState.put("game_resources", gameResources);
        gameState.put("game_board", gameBoard);
        game.put("game_state", gameState);
        game.put("version", 1);
        pushGameState(game, category);
    }

    public void addFaction(SlashCommandInteractionEvent event) {

        Game gameState = getGameState(event);
        String factionName = event.getOption("factionname").getAsString();

        Faction faction = new Faction(factionName, ":" + factionName + ":", event.getOption("player").getAsUser().getAsTag());
        Initializers.newFaction(faction, gameState);

        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
        Category game = event.getOption("game").getAsChannel().asCategory();
        game.createTextChannel("test-" + factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                        .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        game.createTextChannel("test-" + factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();

    }

    public void newFactionResource(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        if (gameState.getFaction(event.getOption("factionname").getAsString()) == null) {
            event.getChannel().sendMessage("That faction is not in this game!").queue();
            return;
        }

        Object value = event.getOption("isanumber").getAsBoolean() ? event.getOption("numbervalue").getAsInt() : event.getOption("othervalue").getAsString();

        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put(event.getOption("resource").getAsString(), value);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        int amount = event.getOption("amount").getAsInt();
        int currentAmount = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources")
                .getInt(event.getOption("resource").getAsString());
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources")
                .remove(event.getOption("resource").getAsString());
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources")
                .put(event.getOption("resource").getAsString(), amount + currentAmount);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void removeResource(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources")
                .remove(event.getOption("resource").getAsString());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void drawCard(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        JSONArray deck = gameState.getDeck(event.getOption("deck").getAsString());
        String drawn = deck.getString(0);

        if (event.getOption("destination").getAsString().toLowerCase().startsWith("discard")) {
            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> {
                    event.getChannel().sendMessage("There is no such thing as a traitor discard pile!").queue();
                    return;
                }
                case "treachery_deck" -> gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("treachery_discard").put(drawn);
                case "spice_deck" -> {
                    if (event.getOption("destination").getAsString().equalsIgnoreCase("discarda")) {
                        gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardA").put(drawn);
                    }
                    else {
                        gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardB").put(drawn);
                    }
                }
            }
        }
        else {
            JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("destination").getAsString()).getJSONObject("resources");

            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> resources.getJSONArray("traitors").put(drawn);
                case "treachery_deck" -> resources.getJSONArray("treachery_hand").put(drawn);
                case "spice_deck" -> {
                    event.getChannel().sendMessage("You can't draw a spice card to someone's hand!").queue();
                    return;
                }
            }
            writeFactionInfo(event, gameState, event.getOption("destination").getAsString());
        }

        deck.remove(0);
        if (drawn.equals("Shai-Hulud")) drawCard(event);
        pushGameState(gameState,event.getOption("game").getAsChannel().asCategory());
    }

    public void drawCard(Game gameState, String deckName, String faction) {
        JSONArray deck = gameState.getDeck(deckName);
        
        String drawn = deck.getString(0);
 
        JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(faction).getJSONObject("resources");
        switch (deckName) {
            case "traitor_deck" -> resources.getJSONArray("traitors").put(drawn);
            case "treachery_deck" -> resources.getJSONArray("treachery_hand").put(drawn);
        }
        deck.remove(0);
    }

    public void shuffle (JSONArray deck) throws JSONException {
        // Implementing Fisher–Yates shuffle
        Random rnd = new Random();
        rnd.setSeed(System.currentTimeMillis());
        for (int i = deck.length() - 1; i >= 0; i--)
        {
            int j = rnd.nextInt(i + 1);
            // Simple swap
            Object object = deck.get(j);
            deck.put(j, deck.get(i));
            deck.put(i, object);
        }
    }

    public void peek(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        JSONArray deck = gameState.getDeck(event.getOption("deck").getAsString());
        StringBuilder message = new StringBuilder();
        message.append("Top of deck: \n");
        int number = 1;
        if (event.getOption("number") != null) {
            number = event.getOption("number").getAsInt();
        }
        for (int i = 0; i < number; i++) {
            message.append(deck.get(i));
            message.append("\n");
        }
        event.getUser().openPrivateChannel().queue((privateChannel -> privateChannel.sendMessage(message).queue()));
    }

    public void discard(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());

        JSONArray hand = faction.getJSONObject("resources").getJSONArray("treachery_hand");
        int i = 0;

        for (; i < hand.length(); i++) {
            String card = hand.getString(i);
            if (card.toLowerCase().contains(event.getOption("card").getAsString())) {
                gameState.getResources().getJSONArray("treachery_discard").put(card);
                break;
            }
        }
        hand.remove(i);
        writeFactionInfo(event, gameState, faction.getString("name"));
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void transferCard(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        JSONArray giverHand;
        JSONArray receiverHand;
        JSONObject giver = null;
        JSONObject receiver = null;

        if (event.getOption("recipient").getAsString().equals("market")) {
            receiverHand = gameState.getResources().getJSONArray("market");
        }
        else {
            receiver = gameState.getFaction(event.getOption("recipient").getAsString());
            receiverHand = receiver.getJSONObject("resources").getJSONArray("treachery_hand");
        }

        if (event.getOption("sender").getAsString().equals("discard")) giverHand = gameState.getResources().getJSONArray("treachery_discard");
        else {
            giver = gameState.getFaction(event.getOption("sender").getAsString());
            giverHand = giver.getJSONObject("resources").getJSONArray("treachery_hand");
        }

        if ((giver == null && !event.getOption("sender").getAsString().equals("discard")) ||
                (receiver == null && !event.getOption("recipient").getAsString().equals("market"))) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("At least one of the selected factions is not playing in this game!").queue());
            return;
        }

        if ((receiver != null && receiver.getString("name").equals("Harkonnen") && receiverHand.length() >= 8) || receiverHand.length() >= 4) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("The recipient's hand is full!").queue());
            return;
        }
        int i = 0;

        boolean cardFound = false;
        for (; i < giverHand.length(); i++) {
            String card = giverHand.getString(i);
            if (card.toLowerCase().contains(event.getOption("card").getAsString())) {
                cardFound = true;
                receiverHand.put(card);
                break;
            }
        }
        if (!cardFound) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Could not find that card!").queue());
            return;
        }
        giverHand.remove(i);
        if (giver != null) writeFactionInfo(event, gameState, giver.getString("name"));
        if (receiver != null) writeFactionInfo(event, gameState, receiver.getString("name"));
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void putBack(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        JSONArray market = gameState.getResources().getJSONArray("market");
        int i = 0;
        boolean found = false;
        for (; i < market.length(); i++) {
            if (market.getString(i).contains(event.getOption("card").getAsString())) {
                if (event.getOption("bottom").getAsBoolean()) gameState.getResources().getJSONArray("treachery_deck")
                        .put(gameState.getResources().getJSONArray("treachery_deck").length());
                else gameState.getResources().getJSONArray("treachery_deck").put(market.getString(i));
                found = true;
                break;
            }
        }
        if (!found) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Card not found, are you sure it's there?"));
            return;
        }
        market.remove(i);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void startGame(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        JSONObject turnOrder = gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject("turn_order");
        Category game = event.getOption("game").getAsChannel().asCategory();
        shuffle(gameState.getDeck("traitor_deck"));
        int i = 1;
        for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
            turnOrder.put(faction, i);
            i++;

            drawCard(gameState, "treachery_deck", faction);
            if (faction.equals("Harkonnen"))  {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                drawCard(gameState, "treachery_deck", faction);
            }
            if (!faction.equals("BT")) {
                for (int j = 0; j < 4; j++) {
                    drawCard(gameState, "traitor_deck", faction);
                }
            }
            for (TextChannel channel : game.getTextChannels()) {
                writeFactionInfo(event, gameState, faction);
                if (channel.getName().equals("test-" + faction.toLowerCase() + "-chat")) {
                    switch (faction) {
                        case "BG" -> channel.sendMessage("Please make your secret prediction and determine where your starting force will be shipped.").queue();
                        case "Fremen" -> channel.sendMessage("Please determine how you will split your initial 10 forces.").queue();
                        case "Ix" -> channel.sendMessage("Please indicate where you would like your Hidden Mobile Stronghold to start").queue();
                    }
                    if (!faction.equals("Harkonnen")) channel.sendMessage("Please select your traitor.").queue();
                    if (turnOrder.getInt(faction) == 1 || turnOrder.getInt(faction) == 6) channel.sendMessage("Please submit your dial for initial storm position.").queue();
                }
            }
        }
        gameState.getJSONObject("game_state").getJSONObject("game_resources").put("turn", 1);
        pushGameState(gameState, game);
    }

    public void writeFactionInfo(SlashCommandInteractionEvent event, Game gameState, String faction) {
        if (gameState.getFaction(faction) == null) return;
        String emoji = gameState.getFaction(faction).getString("emoji");
        JSONObject factionObject = gameState.getFaction(faction);
        JSONArray traitors = factionObject.getJSONObject("resources").getJSONArray("traitors");
        for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
            if (channel.getName().equals("test-" + faction.toLowerCase() + "-info")) {
                channel.deleteMessages(channel.getHistory().retrievePast(10).complete()).complete();
                channel.sendMessage(emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        factionObject.getJSONObject("resources").getInt("spice") +
                        "\n__Traitors:__\n" + traitors.getString(0) + "\n" + traitors.getString(1) + "\n" +
                        traitors.getString(2) + "\n" + traitors.getString(3)).queue();

                for (int j = factionObject.getJSONObject("resources").getJSONArray("treachery_hand").length() - 1; j >= 0; j--) {
                    channel.sendMessage("<:treachery:> " + factionObject.getJSONObject("resources").getJSONArray("treachery_hand").getString(j).split("\\|")[0].strip() + " <:treachery:>").queue();
                }
            }
        }

    }

    public Game getGameState(SlashCommandInteractionEvent event) {
        Category game = event.getOption("game").getAsChannel().asCategory();
        MessageHistory h = game.getTextChannels().get(0).getHistory();
        h.retrievePast(1).complete();
        List<Message> ml = h.getRetrievedHistory();
        Message.Attachment encoded = ml.get(0).getAttachments().get(0);
        CompletableFuture<File> future = encoded.getProxy().downloadToFile(new File(Dotenv.configure().load().get("FILEPATH")));
        try {
            return new Game(new String(Base64.getMimeDecoder().decode(new String(Files.readAllBytes(future.get().toPath())))));
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Didn't work...");
            return new Game();
        }
    }

    public void pushGameState(Game gameState, Category game) {
        TextChannel botData = game.getTextChannels().get(0);
        try {
            File file = new File("gamestate.txt");
            PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8);
            pw.println(Base64.getEncoder().encodeToString(gameState.toString().getBytes(StandardCharsets.UTF_8)));
            pw.close();
            botData.sendFile(file).complete();
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clean(SlashCommandInteractionEvent event) {
        if (!event.getOption("password").getAsString().equals(Dotenv.configure().load().get("PASSWORD"))) {
            event.getChannel().sendMessage("You have attempted the forbidden command.\n\n...Or you're Voiceofonecrying " +
                    "and you fat-fingered the password").queue();
            return;
        }
        List<Category> categories = event.getGuild().getCategories();
        for (Category category : categories) {
            if (!category.getName().startsWith("test")) continue;
            category.delete().complete();
        }
        List<TextChannel> channels = event.getGuild().getTextChannels();
        for (TextChannel channel : channels) {
            if (!channel.getName().startsWith("test")) continue;
            channel.delete().complete();
        }
    }
}
