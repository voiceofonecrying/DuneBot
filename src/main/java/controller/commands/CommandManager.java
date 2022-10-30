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
            case "ixhandselection" -> ixHandSelection(event);
            case "selecttraitor" -> selectTraitor(event);
            case "shipforces" -> shipForces(event);
            case "moveforces" -> moveForces(event);
            case "display" -> displayGameState(event);
            case "revival" -> revival(event);
            case "awardbid" -> awardBid(event);
            case "setstorm" -> setStorm(event);
            case "advancegame" -> advanceGame(event);
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
        OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true);
        OptionData territory = new OptionData(OptionType.STRING, "mostlikelyterritories", "The name of the territory (more 'important' territories).")
                .addChoice("Cielago North", "Cielago North")
                .addChoice("Cielago South", "Cielago South")
                .addChoice("False Wall South", "False Wall South")
                .addChoice("South Mesa", "South Mesa")
                .addChoice("False Wall East", "False Wall East")
                .addChoice("The Minor Erg", "The Minor Erg")
                .addChoice("Tuek's Sietch", "Tuek's Sietch")
                .addChoice("Red Chasm", "Red Chasm")
                .addChoice("Imperial Basin", "Imperial Basin")
                .addChoice("Old Gap", "Old Gap")
                .addChoice("Sihaya Ridge", "Sihaya Ridge")
                .addChoice("Arrakeen", "Arrakeen")
                .addChoice("Broken Land", "Broken Land")
                .addChoice("Carthag", "Carthag")
                .addChoice("Hagga Basin", "Hagga Basin")
                .addChoice("Rock Outcroppings", "Rock Outcroppings")
                .addChoice("Sietch Tabr", "Sietch Tabr")
                .addChoice("Funeral Plain", "Funeral Plain")
                .addChoice("The Great Flat", "The Great Flat")
                .addChoice("False Wall West", "False Wall West")
                .addChoice("Habbanya Erg", "Habbanya Erg")
                .addChoice("Habbanya Ridge Flat", "Habbanya Ridge Flat")
                .addChoice("Habbanya Sietch", "Habbanya Sietch")
                .addChoice("Wind Pass North", "Wind Pass North")
                .addChoice("Polar Sink", "Polar Sink");
        OptionData otherTerritory = new OptionData(OptionType.STRING, "otherterritories", "Added for completeness, less likely to use.")
                .addChoice("Cielago Depression","Cielago Depression")
                .addChoice("Meridian", "Meridian")
                .addChoice("Cielago East", "Cielago East")
                .addChoice("Harg Pass", "Harg Pass")
                .addChoice("Pasty Mesa", "Pasty Mesa")
                .addChoice("Gara Kulon", "Gara Kulon")
                .addChoice("Basin", "Basin")
                .addChoice("Hole in the Rock", "Hole in the Rock")
                .addChoice("Rim Wall West", "Rim Wall West")
                .addChoice("Arsunt", "Arsunt")
                .addChoice("Tsimpo", "Tsimpo")
                .addChoice("Plastic Basin", "Plastic Basin")
                .addChoice("Bight of the Cliff", "Bight of the Cliff")
                .addChoice("Wind Pass", "Wind Pass")
                .addChoice("The Greater Flat", "The Greater Flat")
                .addChoice("Cielago West", "Cielago West")
                .addChoice("Shield Wall", "Shield Wall");
        OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The storm sector");
        OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
        OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
        OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
        OptionData toTerritory = new OptionData(OptionType.STRING, "toterritory", "Where the forces are moving to.", true);
        OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
                .addChoice("Territories", "territories")
                .addChoice("Decks and Discards", "dnd")
                .addChoice("Phase, Turn, and everything else", "etc")
                .addChoice("Faction Info", "factions");

        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        //commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName, role));
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(faction, user, game));
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, game, resourceName, isNumber, resourceValNumber, resourceValString));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(game, faction, resourceName, amount));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(game, faction, resourceName));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(game, deck, destination));
        //commandData.add(Commands.slash("peek", "Peek at the top n number of cards of a deck without moving them.").addOptions(game, deck, numberToPeek));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(game, faction, card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(game, sender, card, recipient));
        //commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(game, card, bottom));
        commandData.add(Commands.slash("advancegame", "Send the game to the next phase, turn, or card (in bidding round").addOptions(game));
        //commandData.add(Commands.slash("ixhandselection", "Only use this command to select the Ix starting treachery card").addOptions(game, card));
        commandData.add(Commands.slash("selecttraitor", "Select a starting traitor from hand.").addOptions(game, faction, traitor));
        //commandData.add(Commands.slash("shipforces", "Place forces from reserves onto the surface").addOptions(game, faction, amount, starred, territory, otherTerritory, sector));
        //commandData.add(Commands.slash("moveforces", "Move forces from one territory to another. (Does not check movement rules).").addOptions(game, faction, toTerritory, amount, starred, territory));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(game, faction, spent));
        //commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(game, faction, revived, starred));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(game, data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(game, sector));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) {

        String name = event.getOption("name").getAsString();
        event.getGuild().createCategory(name).addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getGuild().getRolesByName("Bot Testers", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(event.getOption("role").getAsRole().getName(), true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("bot-data").addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getGuild().getBotRole(), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();
        category.createTextChannel("out-of-game-chat").complete();
        category.createTextChannel("in-game-chat").complete();
        category.createTextChannel("turn-summary").complete();
        category.createTextChannel("game-actions").complete();
        category.createTextChannel("bribes").complete();
        category.createTextChannel("bidding-phase").complete();
        category.createTextChannel("rules").complete();
        category.createTextChannel("pre-game-voting").complete();
        category.createTextChannel("mod-info").complete();

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
        JSONArray stormDeck = Initializers.buildStormDeck();

        gameResources.put("turn", 0);
        gameResources.put("phase", 0);
        gameResources.put("storm", 1);
        gameResources.put("shieldwallbroken", false);
        gameResources.put("traitor_deck", new JSONArray());
        gameResources.put("spice_deck", spiceDeck);
        gameResources.put("spice_discardA", new JSONArray());
        gameResources.put("spice_discardB", new JSONArray());
        gameResources.put("treachery_deck", treacheryDeck);
        gameResources.put("storm_deck", stormDeck);
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
        if (gameState.getResources().getInt("turn") != 0) {
            event.getChannel().sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (gameState.getJSONObject("game_state").getJSONObject("factions").length() >= 6) {
            event.getChannel().sendMessage("This game is already full!").queue();
            return;
        }
        String factionName = event.getOption("factionname").getAsString();
        if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull(factionName)) {
            event.getChannel().sendMessage("This faction has already been taken!").queue();
            return;
        }

        Faction faction = new Faction(factionName, ":" + factionName + ":", event.getOption("player").getAsUser().getAsTag());
        Initializers.newFaction(faction, gameState);

        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
        Category game = event.getOption("game").getAsChannel().asCategory();
        game.createTextChannel(factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                        .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        game.createTextChannel(factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
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
        writeFactionInfo(event, gameState, event.getOption("factionname").getAsString());
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
        String drawn = deck.getString(deck.length() - 1);

        if (event.getOption("destination").getAsString().toLowerCase().startsWith("discard")) {
            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> {
                    event.getChannel().sendMessage("There is no such thing as a traitor discard pile!").queue();
                    return;
                }
                case "treachery_deck" -> gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("treachery_discard").put(drawn);
            }
        }
        else {
            JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("destination").getAsString()).getJSONObject("resources");

            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> resources.getJSONArray("traitors").put(drawn);
                case "treachery_deck" -> resources.getJSONArray("treachery_hand").put(drawn);
            }
            writeFactionInfo(event, gameState, event.getOption("destination").getAsString());
        }

        deck.remove(deck.length() - 1);
        if (drawn.equals("Shai-Hulud")) drawCard(event);
        pushGameState(gameState,event.getOption("game").getAsChannel().asCategory());
    }

    public String drawCard(Game gameState, String deckName, String faction) {
        JSONArray deck = gameState.getDeck(deckName);
        
        String drawn = deck.getString(deck.length() - 1);
        if (gameState.getResources().getInt("turn") == 1 && drawn.equals("Shai-Hulud")) {
            shuffle(deck);
            return drawCard(gameState, deckName, faction);
        }
        if (deckName.equals("spice_deck")) {
            //In this case, faction is used as a flag to determine if this is the first or second spice blow of the turn.
            if (faction.equals("a")) {
                gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardA").put(drawn);
            }
            else {
                gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardB").put(drawn);
            }
            deck.remove(deck.length() - 1);
            if (!drawn.equals("Shai-Hulud") && gameState.getResources().getInt("storm") != gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).getInt("sector")) {
                int spice = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).getInt("spice");
                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).put("spice", spice + Integer.parseInt(drawn.split("-")[1].strip()));
            }
            if (drawn.equals("Shai-Hulud")) drawn += ", " + drawCard(gameState, deckName, faction);
            else if (faction.equals("a")) drawn += "\nB: " + drawCard(gameState, deckName, "b");
            return drawn;
        }
        JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(faction).getJSONObject("resources");
        switch (deckName) {
            case "traitor_deck" -> resources.getJSONArray("traitors").put(drawn);
            case "treachery_deck" -> resources.getJSONArray("treachery_hand").put(drawn);

        }
        deck.remove(deck.length() - 1);
        if (drawn.equals("Shai-Hulud")) drawn += ", " + drawCard(gameState, deckName, faction);
        return drawn;
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
            message.append(deck.get(deck.length() - 1 - i));
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
                if (!event.getOption("bottom").getAsBoolean()) gameState.getResources().getJSONArray("treachery_deck")
                        .put(market.getString(i));
                else gameState.getResources().getJSONArray("treachery_deck").put(0, market.getString(i));
                found = true;
                break;
            }
        }
        if (!found) {
            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Card not found, are you sure it's there?"));
            return;
        }
        market.remove(i);
        shuffle(market);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void ixHandSelection(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        JSONArray hand = gameState.getFaction("Ix").getJSONObject("resources").getJSONArray("treachery_hand");
        shuffle(hand);
        for (int i = 0; i < hand.length(); i++) {
            if (hand.getString(i).toLowerCase().contains(event.getOption("card").getAsString())) continue;
            gameState.getResources().getJSONArray("treachery_deck").put(hand.getString(i));
        }
        int shift = 0;
        int length = hand.length() - 1;
        for (int i = 0; i < length; i++) {
            if (hand.getString(0).toLowerCase().contains(event.getOption("card").getAsString())) {
                shift = 1;
                continue;
            }
            hand.remove(shift);
        }
        writeFactionInfo(event, gameState, "Ix");
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void awardBid(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        try {
            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getJSONArray("treachery_hand").put(gameState.getResources().getJSONArray("market").getString(0));
            event.getChannel().sendMessage(gameState.getFaction(event.getOption("factionname").getAsString()).getString("emoji") + " wins card up for bid for " + event.getOption("spent").getAsInt() + " <:spice4:991763531798167573>").queue();
        } catch (JSONException e) {
            event.getChannel().sendMessage("No more cards up for bid.  Please advance the game.").queue();
            return;
        }
        gameState.getResources().getJSONArray("market").remove(0);
        if (event.getOption("factionname").getAsString().equals("Harkonnen") && gameState.getFaction("Harkonnen").getJSONObject("resources").getJSONArray("treachery_hand").length() < 8) {
            drawCard(gameState, "treachery_deck", "Harkonnen");
            event.getChannel().sendMessage(gameState.getFaction(event.getOption("factionname").getAsString()).getString("emoji") + " draws another card from the <:treachery:991763073281040518> deck.").queue();
        }
        int spice = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("spice");
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("spice");
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("spice", spice - event.getOption("spent").getAsInt());
        writeFactionInfo(event, gameState, event.getOption("factionname").getAsString());
        if (gameState.getJSONObject("game_state").getJSONObject("factions").keySet().contains("Emperor") && !event.getOption("factionname").getAsString().equals("Emperor")) {
            int spiceEmp = gameState.getFaction("Emperor").getJSONObject("resources").getInt("spice");
            gameState.getFaction("Emperor").getJSONObject("resources").remove("spice");
            gameState.getFaction("Emperor").getJSONObject("resources").put("spice", spiceEmp + event.getOption("spent").getAsInt());
            event.getChannel().sendMessage(gameState.getFaction("Emperor").getString("emoji") + " is paid " + event.getOption("spent").getAsInt() + " <:spice4:991763531798167573>").queue();
            writeFactionInfo(event, gameState, "Emperor");
        }
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void revival(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
        String faction = event.getOption("factionname").getAsString();
        int tanksForces = gameState.getResources().getJSONObject("tanks_forces").getInt(faction + star);
        gameState.getResources().getJSONObject("tanks_forces").remove(faction + star);
        gameState.getResources().getJSONObject("tanks_forces").put(faction + star, tanksForces - event.getOption("revived").getAsInt());
        int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves" + star);
        gameState.getFaction(faction).getJSONObject("resources").remove("reserves" + star);
        gameState.getFaction(faction).getJSONObject("resources").put("reserves" + star, reserves + event.getOption("revived").getAsInt());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void moveForces(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        String faction = event.getOption("factionname").getAsString();
        JSONObject originForces = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(event.getOption("territory").getAsString()).getJSONObject("forces");
        JSONObject destinationForces = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(event.getOption("toterritory").getAsString()).getJSONObject("forces");
        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
        int originForcesStrength = originForces.getInt(faction + star);
        originForces.remove(faction + star);
        if (originForcesStrength > event.getOption("amount").getAsInt()) {
            originForces.put(faction + star, originForcesStrength - event.getOption("amount").getAsInt());
        } else if (originForcesStrength < event.getOption("amount").getAsInt()) {
            event.getChannel().sendMessage("There are not enough forces to perform the movement").queue();
        }
        int destinationForcesStrength = destinationForces.isNull(faction + star) ? 0 : destinationForces.getInt(faction + star);
        destinationForces.remove(faction + star);
        destinationForces.put(faction + star, destinationForcesStrength + event.getOption("amount").getAsInt());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void advanceGame(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        //Turn 0 is for the set-up for play section from the rules page 6.
        if (gameState.getTurn() == 0) {
            switch (gameState.getPhase()) {
                //1. Positions
                case 0 -> {
                    shuffle(gameState.getDeck("spice_deck"));
                    shuffle(gameState.getDeck("treachery_deck"));
                    JSONObject turnOrder = gameState.getResources().getJSONObject("turn_order");
                    int i = 1;
                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
                        turnOrder.put(faction, i);
                        i++;
                    }
                    gameState.advancePhase();
                    //If Bene Gesserit are present, time to make a prediction
                    if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("BG")) {
                        for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                            if (channel.getName().equals("bg-chat")) {
                                channel.sendMessage("Please make your secret prediction.").queue();
                            }
                        }
                    }
                    event.getChannel().sendMessage("1. Positions have been assigned.").queue();
                }
                //2. Traitors
                case 1 -> {
                    shuffle(gameState.getDeck("traitor_deck"));
                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
                        if (!faction.equals("BT")) {
                            for (int j = 0; j < 4; j++) {
                                drawCard(gameState, "traitor_deck", faction);
                            }
                            writeFactionInfo(event, gameState, faction);
                        }
                    }
                    for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                            if (channel.getName().contains("-chat") && !channel.getName().contains("game") &&
                                    !channel.getName().contains("harkonnen") && !channel.getName().contains("bt")) channel.sendMessage("Please select your traitor.").queue();
                        }


                        gameState.advancePhase();
                    //If Bene Tleilax are not present, advance past the Face Dancers draw
                    if (gameState.getJSONObject("game_state").getJSONObject("factions").isNull("BT")) {
                        gameState.advancePhase();
                    }
                    event.getChannel().sendMessage("2. Traitors are being selected.").queue();
                }
                //Bene Tleilax to draw Face Dancers
                case 2 -> {
                    shuffle(gameState.getDeck("traitor_deck"));
                    drawCard(gameState, "traitor_deck", "BT");
                    drawCard(gameState, "traitor_deck", "BT");
                    drawCard(gameState, "traitor_deck", "BT");
                    writeFactionInfo(event, gameState, "BT");
                    gameState.advancePhase();
                    event.getChannel().sendMessage("2b. Bene Tleilax have drawn their Face Dancers.").queue();
                }
                //3. Spice, 4. Forces (prompts are sent out)
                case 3 -> {
                    for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                        switch (channel.getName()) {
                            case "fremen-chat" -> channel.sendMessage("Please distribute 10 forces between Sietch Tabr, False Wall South, and False Wall West").queue();
                            case "bg-chat" -> channel.sendMessage("Please decide where to place your advisor").queue();
                        }
                    }
                    gameState.advancePhase();
                    //If Ix is not present, advance past the next step
                    if (gameState.getJSONObject("game_state").getJSONObject("factions").isNull("Ix")) {
                        gameState.advancePhase();
                    }
                    event.getChannel().sendMessage("3. Spice has been allocated.\n4. Forces are being placed on the board.").queue();
                }
                //Ix to select from starting treachery cards
                case 4 -> {
                    int toDraw = gameState.getJSONObject("game_state").getJSONObject("factions").length();
                    if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("Harkonnen")) toDraw++;
                    for (int i = 0; i < toDraw; i++) {
                        drawCard(gameState, "treachery_deck", "Ix");
                    }
                    writeFactionInfo(event, gameState, "Ix");
                    for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                        if (channel.getName().equals("ix-chat")) channel.sendMessage("Please select one treachery card to keep in your hand.").queue();
                    }
                    gameState.advancePhase();
                    event.getChannel().sendMessage("Ix is selecting their starting treachery card.").queue();
                }
                //5. Treachery
                case 5 -> {
                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
                        if (!faction.equals("Ix")) drawCard(gameState, "treachery_deck", faction);
                        if (faction.equals("Harkonnen")) drawCard(gameState, "treachery_deck", faction);
                        writeFactionInfo(event, gameState, faction);
                    }
                    gameState.advancePhase();
                    event.getChannel().sendMessage("5. Treachery cards are being dealt.").queue();
                }
                //6. Turn Marker (prompt for dial for First Storm)
                case 6 -> {
                    JSONObject turnOrder = gameState.getResources().getJSONObject("turn_order");
                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
                        if (turnOrder.getInt(faction) == 1 || turnOrder.getInt(faction) == 6) {
                            for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                                if (channel.getName().equals(faction.toLowerCase() + "-chat")) channel.sendMessage("Please submit your dial for initial storm position.").queue();
                            }
                        }
                    }
                    shuffle(gameState.getDeck("storm_deck"));
                    gameState.advanceTurn();
                    event.getChannel().sendMessage("6. Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...").queue();
                }
            }
        }
        else {
            switch (gameState.getPhase()) {
                //1. Storm Phase
                case 1 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Storm Phase:").queue();
                    JSONObject territories = gameState.getJSONObject("game_state").getJSONObject("game_board");
                   if (gameState.getTurn() != 1) {
                       int stormMovement = gameState.getDeck("storm_deck").getInt(0);
                       shuffle(gameState.getDeck("storm_deck"));
                       event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("The storm moves " + stormMovement + " sectors this turn.").queue();
                       for (int i = 0; i < stormMovement; i++) {
                           gameState.getResources().put("storm", (gameState.getResources().getInt("storm") + 1));
                           if (gameState.getResources().getInt("storm") == 19) gameState.getResources().put("storm", 1);
                           for (String territory : territories.keySet()) {
                               if (!territories.getJSONObject(territory).getBoolean("is_rock") && territories.getJSONObject(territory).getInt("sector") == gameState.getResources().getInt("storm")) {
                                   Set<String> forces = territories.getJSONObject(territory).getJSONObject("forces").keySet();
                                   boolean fremenSpecialCase = false;
                                   //Defaults to play "optimally", destorying Fremen regular forces over Fedaykin
                                   if (forces.contains("Fremen") && forces.contains("Fremen*")) {
                                       fremenSpecialCase = true;
                                       int fremenForces = territories.getJSONObject(territory).getJSONObject("forces").getInt("Fremen");
                                       int fremenFedaykin = territories.getJSONObject(territory).getJSONObject("forces").getInt("Fremen*");
                                       int lost = (fremenForces + fremenFedaykin) / 2;
                                       territories.getJSONObject(territory).getJSONObject("forces").remove("Fremen");
                                       if (lost < fremenForces) {
                                           territories.getJSONObject(territory).getJSONObject("forces").put("Fremen", fremenForces - lost);
                                       } else if (lost > fremenForces) {
                                           territories.getJSONObject(territory).getJSONObject("forces").remove("Fremen*");
                                           territories.getJSONObject(territory).getJSONObject("forces").put("Fremen*", lost - fremenForces);
                                       }
                                       event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                                               gameState.getFaction("Fremen").getString("emoji") + " lost " + lost +
                                                       " forces to the storm in " + territory
                                       ).queue();
                                   }
                                   for (String force : forces) {
                                       if (force.contains("Fremen") && fremenSpecialCase) continue;
                                       int lost = territories.getJSONObject(territory).getJSONObject("forces").getInt(force);
                                       territories.getJSONObject(territory).getJSONObject("forces").remove(force);
                                       if (force.contains("Fremen")) territories.getJSONObject(territory).getJSONObject("forces").put(force, lost / 2);
                                       if (gameState.getResources().getJSONObject("tanks_forces").isNull(force)) {
                                           gameState.getResources().getJSONObject("tanks_forces").put(force, lost);
                                       } else {
                                           gameState.getResources().getJSONObject("tanks_forces").put(force,
                                                   gameState.getResources().getJSONObject("tanks_forces").getInt(force) + lost);
                                       }
                                       event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                                               gameState.getFaction(force.replace("*", "")).getString("emoji") + " lost " +
                                                       lost + " forces to the storm in " + territory
                                       ).queue();
                                   }

                               }
                               territories.getJSONObject(territory).remove("spice");
                               territories.getJSONObject(territory).put("spice", 0);
                           }
                       }
                   }
                   if (gameState.getFaction("Fremen") != null) {
                       for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                           if (channel.getName().contains("fremen-info")) {
                               channel.sendMessage("The storm will move " + gameState.getDeck("storm_deck").getInt(0) + " sectors next turn.").queue();
                           }
                       }
                   }
                    gameState.advancePhase();
                }
                //2. Spice Blow and Nexus
                case 2 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Spice Blow Phase:").queue();
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(drawCard(gameState, "spice_deck", "a")).queue();
                    gameState.advancePhase();
                }
                //3. Choam Charity
                case 3 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " CHOAM Charity Phase:").queue();
                    int multiplier = 1;
                    if (!gameState.getResources().isNull("inflation token")) {
                        if (gameState.getResources().getString("inflation token").equals("cancel")) {
                            event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("CHOAM Charity is cancelled!").queue();
                            gameState.advancePhase();
                            break;
                        } else {
                            multiplier = 2;
                        }
                    }

                    int choamGiven = 0;
                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
                    if (factions.contains("CHOAM")) event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                            gameState.getFaction("CHOAM").getString("emoji") + " receives " + 10 * multiplier + " <:spice4:991763531798167573> in dividends from their many investments."
                    ).queue();
                    for (String faction : factions) {
                        if (faction.equals("CHOAM")) continue;
                        int spice = gameState.getFaction(faction).getJSONObject("resources").getInt("spice");
                        if (faction.equals("BG")) {
                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
                            choamGiven += 2 * multiplier;
                            gameState.getFaction(faction).getJSONObject("resources").put("spice", spice + (2 * multiplier));
                            event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                                    gameState.getFaction(faction).getString("emoji") + " have received " + 2 * multiplier + " <:spice4:991763531798167573> in CHOAM Charity."
                            ).queue();
                        }
                        else if (spice < 2) {
                            int charity = (2 * multiplier) - (spice * multiplier);
                            choamGiven += charity;
                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
                            gameState.getFaction(faction).getJSONObject("resources").put("spice", spice + charity);
                            event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                                    gameState.getFaction(faction).getString("emoji") + " have received " + charity + " <:spice4:991763531798167573> in CHOAM Charity."
                            ).queue();
                        }
                        else continue;
                        writeFactionInfo(event, gameState, faction);
                    }
                    if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("CHOAM")) {
                        int spice = gameState.getFaction("CHOAM").getJSONObject("resources").getInt("spice");
                        gameState.getFaction("CHOAM").getJSONObject("resources").remove("spice");
                        gameState.getFaction("CHOAM").getJSONObject("resources").put("spice", (10 * multiplier) + spice - choamGiven);
                        event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage(
                                gameState.getFaction("CHOAM").getString("emoji") + " has paid " + choamGiven + " <:spice4:991763531798167573> to factions in need."
                        ).queue();
                    }
                    gameState.advancePhase();
                }
                //4. Bidding
                case 4 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Bidding Phase:").queue();
                    int cardsUpForBid = 0;
                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
                    for (String faction : factions) {
                        int length = gameState.getFaction(faction).getJSONObject("resources").getJSONArray("treachery_hand").length();
                        if (faction.equals("Harkonnen") && length < 8 || faction.equals("CHOAM") && length < 5 ||
                                !(faction.equals("Harkonnen") || faction.equals("CHOAM")) && length < 4) cardsUpForBid++;
                        if (faction.equals("Ix")) cardsUpForBid++;
                        if (faction.equals("Rich")) cardsUpForBid--;
                    }
                    JSONArray deck = gameState.getDeck("treachery_deck");
                    if (factions.contains("Ix")) {
                        for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                            if (channel.getName().equals("ix-chat")) channel.sendMessage("Please select a card to put back to top or bottom.").queue();
                        }
                    }
                    for (int i = 0; i < cardsUpForBid; i++) {
                        gameState.getResources().getJSONArray("market").put(deck.getString(deck.length() - 1));
                        deck.remove(deck.length() - 1);
                        if (factions.contains("Ix")) {
                            for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
                                if (channel.getName().equals("ix-chat"))
                                    channel.sendMessage("<:treachery:991763073281040518> " +
                                            deck.getString(deck.length() - i - 1) + " <:treachery:991763073281040518>").queue();
                            }
                        }
                    }
                    gameState.advancePhase();
                }
                //5. Revival
                case 5 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Revival Phase:").queue();
                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
                    for (String faction : factions) {
                        int free = gameState.getFaction(faction).getInt("free_revival");
                        boolean revivedStar = false;
                        for (int i = free; i > 0; i--) {
                            if (gameState.getResources().getJSONObject("tanks_forces").isNull(faction) && gameState.getResources().getJSONObject("tanks_forces").isNull(faction + "*")) continue;
                            if (!gameState.getResources().getJSONObject("tanks_forces").isNull(faction + "*") && !revivedStar) {
                                int starred = gameState.getResources().getJSONObject("tanks_forces").getInt(faction + "*");
                                gameState.getResources().getJSONObject("tanks_forces").remove(faction + "*");
                                if (starred > 1) gameState.getResources().getJSONObject("tanks_forces").put(faction + "*", starred - 1);
                                revivedStar = true;
                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves*");
                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves*");
                                gameState.getFaction(faction).getJSONObject("resources").put("reserves*", reserves + 1);
                            } else if (!gameState.getResources().getJSONObject("tanks_forces").isNull(faction)) {
                                int forces = gameState.getResources().getJSONObject("tanks_forces").getInt(faction);
                                gameState.getResources().getJSONObject("tanks_forces").remove(faction);
                                gameState.getResources().getJSONObject("tanks_forces").put(faction, forces - 1);
                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves");
                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves");
                                gameState.getFaction(faction).getJSONObject("resources").put("reserves", reserves + 1);
                            }
                        }
                    }
                    gameState.advancePhase();
                }
                //6. Shipment and Movement
                case 6 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Shipment and Movement Phase:").queue();
                    gameState.advancePhase();
                }
                //TODO: 7. Battle
                case 7 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Battle Phase:").queue();
                    gameState.advancePhase();

                }
                //TODO: 8. Spice Harvest
                case 8 -> {
                   event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Spice Harvest Phase:").queue();
                   gameState.advancePhase();
                }
                //TODO: 9. Mentat Pause
                case 9 -> {
                    event.getOption("game").getAsChannel().asCategory().getTextChannels().get(3).sendMessage("Turn " + gameState.getTurn() + " Mentat Pause Phase:").queue();
                    gameState.advanceTurn();
                }
            }
        }
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void selectTraitor(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);

        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
        for (int i = 0; i < 4; i++) {
            if (!faction.getJSONObject("resources").getJSONArray("traitors").getString(i).toLowerCase().contains(event.getOption("traitor").getAsString().toLowerCase())) {
                gameState.getDeck("traitor_deck").put(faction.getJSONObject("resources").getJSONArray("traitors").get(i));
                String traitor = faction.getJSONObject("resources").getJSONArray("traitors").getString(i);
                faction.getJSONObject("resources").getJSONArray("traitors").put(i, "~~" + traitor + "~~");
            }
        }
        writeFactionInfo(event, gameState, event.getOption("factionname").getAsString());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void shipForces(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        String sector = event.getOption("sector") == null ? "" : "(" + event.getOption("sector").getAsString() + ")";
        String territory = "";
        if (event.getOption("mostlikelyterritories") == null && event.getOption("otherterritories") == null) {
            event.getChannel().sendMessage("You have to select a territory.").queue();
            return;
        } else if (event.getOption("mostlikelyterritories") == null) {
            territory = event.getOption("otherterritories").getAsString();
        } else {
            territory = event.getOption("mostlikelyterritories").getAsString();
        }
        if (gameState.getJSONObject("game_state").getJSONObject("game_board").isNull(territory + sector)) {
            event.getChannel().sendMessage("Territory does not exist in that sector. Check your sector number and try again.").queue();
            return;
        }
            String star = "";
        if (event.getOption("starred") != null && event.getOption("starred").getAsBoolean()) {
            star = "*";
        }
        int reserves = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("reserves" + star);
        if (reserves < event.getOption("amount").getAsInt()) {
            event.getChannel().sendMessage("This faction does not have enough forces in reserves for this shipment!").queue();
            return;
        }
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("reserves" + star);
        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("reserves" + star, reserves - event.getOption("amount").getAsInt());
        int previous = 0;

        if (!gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory + sector)
                .getJSONObject("forces").isNull(event.getOption("factionname").getAsString() + star)) {
            previous = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory + sector).getJSONObject("forces").getInt(event.getOption("factionname").getAsString() + star);
        }

        gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory + sector).getJSONObject("forces").put(event.getOption("factionname").getAsString() + star, event.getOption("amount").getAsInt() + previous);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void setStorm(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        gameState.getResources().remove("storm");
        try {
            gameState.getResources().put("storm", event.getOption("sector").getAsInt());
        } catch (NullPointerException e) {
            event.getChannel().sendMessage("No storm sector was selected.").queue();
            return;
        }
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void writeFactionInfo(SlashCommandInteractionEvent event, Game gameState, String faction) {
        if (gameState.getFaction(faction) == null) return;
        String emoji = gameState.getFaction(faction).getString("emoji");
        JSONObject factionObject = gameState.getFaction(faction);
        JSONArray traitors = factionObject.getJSONObject("resources").getJSONArray("traitors");
        StringBuilder traitorString = new StringBuilder();
        if (faction.equals("BT")) traitorString.append("\n__Face Dancers:__\n");
        else traitorString.append("\n__Traitors:__\n");
        for (Object traitor : traitors) {
            traitorString.append(traitor);
            traitorString.append("\n");
        }
        for (TextChannel channel : event.getOption("game").getAsChannel().asCategory().getTextChannels()) {
            if (channel.getName().equals(faction.toLowerCase() + "-info")) {
                channel.sendMessage(emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        factionObject.getJSONObject("resources").getInt("spice") +
                        traitorString).queue();

                for (int j = factionObject.getJSONObject("resources").getJSONArray("treachery_hand").length() - 1; j >= 0; j--) {
                    channel.sendMessage("<:treachery:991763073281040518> " + factionObject.getJSONObject("resources").getJSONArray("treachery_hand").getString(j).split("\\|")[0].strip() + " <:treachery:991763073281040518>").queue();
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

    public void displayGameState(SlashCommandInteractionEvent event) {
        Game gameState = getGameState(event);
        TextChannel channel = event.getOption("game").getAsChannel().asCategory().getTextChannels().get(9);
        switch (event.getOption("data").getAsString()) {
            case "territories" -> {
               JSONObject territories = gameState.getJSONObject("game_state").getJSONObject("game_board");
               for (String territoryName : territories.keySet()) {
                   JSONObject territory = territories.getJSONObject(territoryName);
                   if (territory.getInt("spice") == 0 && !territory.getBoolean("is_stronghold") && territory.getJSONObject("forces").length() == 0) continue;
                   channel.sendMessage("**" + territory.getString("territory_name") + "(" + territory.getInt("sector") + "):** \n" +
                           "Spice: " + territory.getInt("spice") + "\nForces: " + territory.getJSONObject("forces").toString(4)).queue();
               }
            }
            case "dnd" -> {
                for (String key : gameState.getResources().keySet()) {
                    if (key.contains("deck") || key.contains("discard")) {
                        channel.sendMessage("**" + key + ":** " + gameState.getResources().getJSONArray(key).toString(4)).queue();
                    }
                }

            }
            case "etc" -> {
                for (String key : gameState.getResources().keySet()) {
                    if (!key.contains("deck") && !key.contains("discard")) {
                        channel.sendMessage("**" + key + ":** " + gameState.getResources().get(key).toString()).queue();
                    }
                }
            }
            case "factions" -> {
                for (String factionName : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
                    JSONObject faction = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(factionName);
                    StringBuilder message = new StringBuilder();
                    message.append("**" + faction.getString("name")).append(":**\nPlayer: ").append(faction.getString("player")).append("\n");
                    for (String resourceName : faction.getJSONObject("resources").keySet()) {
                        Object resource = faction.getJSONObject("resources").get(resourceName);
                        message.append(resourceName).append(": ").append(resource.toString()).append("\n");
                    }
                    channel.sendMessage(message.toString()).queue();
                }
            }
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
            if (!channel.getName().startsWith("test") || channel.getName().equals("test")) continue;
            channel.delete().complete();
        }
    }
}
