package controller.commands;

import controller.Initializers;
import io.github.cdimascio.dotenv.Dotenv;
import model.Faction;
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
        OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck you are drawing from", true)
                .addChoice("Spice Deck", "spice_deck")
                .addChoice("Treachery Deck", "treachery_deck")
                .addChoice("Traitor Deck", "traitor_deck");
        OptionData destination = new OptionData(OptionType.STRING, "destination", "Where the card is being drawn to (name a faction to draw to hand, or 'discard')", true);

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

        JSONObject object = new JSONObject();
        JSONObject gameState = new JSONObject();
        JSONObject gameResources = new JSONObject();
        JSONObject gameBoard = Initializers.buildBoard();
        JSONObject spiceDeck = Initializers.buildSpiceDeck();
        JSONObject treacheryDeck = Initializers.buildTreacheryDeck();

        gameResources.put("turn", 0);
        gameResources.put("storm", 1);
        gameResources.put("shieldwallbroken", false);
        gameResources.put("traitor_deck", new JSONObject());
        gameResources.put("spice_deck", spiceDeck);
        gameResources.put("spice_discardA", new JSONObject());
        gameResources.put("spice_discardB", new JSONObject());
        gameResources.put("treachery_deck", treacheryDeck);
        gameResources.put("treachery_discard", new JSONObject());
        gameResources.put("tanks_forces", new JSONObject());
        gameResources.put("tanks_leaders", new JSONObject());
        gameResources.put("turn_order", new JSONObject());
        gameState.put("factions", new JSONObject());
        gameState.put("game_resources", gameResources);
        gameState.put("game_board", gameBoard);
        object.put("game_state", gameState);
        object.put("version", 1);
        pushGameState(object, category);
    }

    public void addFaction(SlashCommandInteractionEvent event) {

        JSONObject gameState = getGameState(event);
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
        JSONObject gameState = getGameState(event);
        if (gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()) == null) {
            event.getChannel().sendMessage("That faction is not in this game!").queue();
            return;
        }

        Object value = event.getOption("isanumber").getAsBoolean() ? event.getOption("numbervalue").getAsInt() : event.getOption("othervalue").getAsString();

        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()).getJSONObject("resources").put(event.getOption("resource").getAsString(), value);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event) {
        JSONObject gameState = getGameState(event);

        int amount = event.getOption("amount").getAsInt();
        int currentAmount = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString())
                .getJSONObject("resources").getInt(event.getOption("resource").getAsString());
        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString())
                .getJSONObject("resources").remove(event.getOption("resource").getAsString());
        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString())
                .getJSONObject("resources").put(event.getOption("resource").getAsString(), amount + currentAmount);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void removeResource(SlashCommandInteractionEvent event) {
        JSONObject gameState = getGameState(event);
        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString())
                .getJSONObject("resources").remove(event.getOption("resource").getAsString());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void drawCard(SlashCommandInteractionEvent event) {
        JSONObject gameState = getGameState(event);

        JSONObject deck = gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject(event.getOption("deck").getAsString());
        int size = deck.keySet().size();
        int toDraw = new Random().nextInt(size);
        int i = 0;
        String drawn = "";
        for (String card : deck.keySet()) {
            if (i == toDraw) {
                drawn = card;
                break;
            }
            i++;
        }
        System.out.println(drawn);

        if (event.getOption("destination").getAsString().toLowerCase().startsWith("discard")) {
            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> {
                    event.getChannel().sendMessage("There is no such thing as a traitor discard pile!").queue();
                    return;
                }
                case "treachery_deck" -> gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject("treachery_discard")
                        .put(drawn, deck.getString(drawn));
                case "spice_deck" -> {
                    if (event.getOption("destination").getAsString().equalsIgnoreCase("discarda")) {
                        gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject("spice_discardA")
                                .put(drawn, deck.getJSONArray(drawn));
                    }
                    else {
                        gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject("spice_discardB")
                                .put(drawn, deck.getJSONArray(drawn));
                    }
                }
            }
        }
        else {
            JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("destination").getAsString()).getJSONObject("resources");

            switch (event.getOption("deck").getAsString()) {
                case "traitor_deck" -> resources.getJSONObject("traitors").put(drawn, deck.getInt(drawn));
                case "treachery_deck" -> resources.getJSONObject("treachery_hand").put(drawn, deck.getString(drawn));
                case "spice_deck" -> {
                    event.getChannel().sendMessage("You can't draw a spice card to someone's hand!").queue();
                    return;
                }
            }
        }

        deck.remove(drawn);
        pushGameState(gameState,event.getOption("game").getAsChannel().asCategory());
        if (drawn.startsWith("Shai-Hulud")) drawCard(event);
    }

    public void drawCard(JSONObject gameState, String deckName, String faction) {
        JSONObject deck = gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject(deckName);
        int size = deck.keySet().size();
        int toDraw = new Random().nextInt(size);
        int i = 0;
        String drawn = "";
        for (String card : deck.keySet()) {
            if (i == toDraw) {
                drawn = card;
                break;
            }
            i++;
        }
        System.out.println(drawn);
        JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(faction).getJSONObject("resources");
        switch (deckName) {
            case "traitor_deck" -> resources.getJSONObject("traitors").put(drawn, deck.getInt(drawn));
            case "treachery_deck" -> resources.getJSONObject("treachery_hand").put(drawn, deck.getString(drawn));
        }
        deck.remove(drawn);
    }


    public void startGame(SlashCommandInteractionEvent event) {
        JSONObject gameState = getGameState(event);
        JSONObject turnOrder = gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONObject("turn_order");
        Category game = event.getOption("game").getAsChannel().asCategory();
        int i = 1;
        for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
            turnOrder.put(faction, i);
            i++;

            drawCard(gameState, "treachery_deck", faction);
            if (faction.equals("Harkonnen")) drawCard(gameState, "treachery_deck", faction);
            if (!faction.equals("BT")) {
                for (int j = 0; j < 4; j++) {
                    drawCard(gameState, "traitor_deck", faction);
                }
            }

            JSONObject factionObject = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(faction);
            String emoji = factionObject.getString("emoji");
            JSONObject traitors = factionObject.getJSONObject("resources").getJSONObject("traitors");
            String[] traitorNames = new String[4];
            int k = 0;
            for (String traitor : traitors.keySet()) {
                traitorNames[k] = traitor;
                k++;
            }
            for (TextChannel channel : game.getTextChannels()) {
                if (channel.getName().equals("test-" + faction.toLowerCase() + "-info")) {
                    channel.sendMessage(emoji + "**Setup Info**" + emoji + "\n__Spice:__ " +
                            factionObject.getJSONObject("resources").getInt("spice") +
                            "\n__Traitors:__\n" + traitorNames[0] + "(" +traitors.getInt(traitorNames[0]) + ")\n" +
                            traitorNames[1] + "(" +traitors.getInt(traitorNames[1]) + ")\n" +
                            traitorNames[2] + "(" +traitors.getInt(traitorNames[2]) + ")\n" +
                            traitorNames[3] + "(" +traitors.getInt(traitorNames[3]) + ")\n").queue();

                    for (String treachery : factionObject.getJSONObject("resources").getJSONObject("treachery_hand").keySet()) {
                        channel.sendMessage(":treachery: " + treachery + ":treachery:").queue();
                    }
                }
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

    public JSONObject getGameState(SlashCommandInteractionEvent event) {
        Category game = event.getOption("game").getAsChannel().asCategory();
        MessageHistory h = game.getTextChannels().get(0).getHistory();
        h.retrievePast(1).complete();
        List<Message> ml = h.getRetrievedHistory();
        Message.Attachment encoded = ml.get(0).getAttachments().get(0);
        CompletableFuture<File> future = encoded.getProxy().downloadToFile(new File(Dotenv.configure().load().get("FILEPATH")));
        try {
            return new JSONObject(new String(Base64.getMimeDecoder().decode(new String(Files.readAllBytes(future.get().toPath())))));
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Didn't work...");
            return new JSONObject();
        }
    }
    public void pushGameState(JSONObject gameState, Category game) {
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
