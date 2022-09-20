package controller.commands;

import controller.Initializers;
import io.github.cdimascio.dotenv.Dotenv;
import model.Faction;
import model.Resource;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        try {
            switch (name) {
                case "newgame" -> newGame(event);
                case "addfaction" -> addFaction(event);
                case "newfactionresource" -> newFactionResource(event);
                case "resourceaddorsubtract" -> resourceAddOrSubtract(event);
                case "removeresource" -> removeResource(event);
                case "clean" -> clean(event);
            }
            //implement new slash commands here
        } catch (IOException e) {
            event.getChannel().sendMessage("sorry, something happened on the backend that caused your action to not go through. :(").queue();
        }

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
        OptionData isNumber = new OptionData(OptionType.BOOLEAN, "isanumber", "Set true if it is a numerical value, false otherwise.", true);
        OptionData resourceValNumber = new OptionData(OptionType.INTEGER, "numbervalue", "Set the initial value if the resource is a number (leave blank otherwise)");
        OptionData resourceValString = new OptionData(OptionType.STRING, "othervalue", "Set the initial value if the resource is not a number (leave blank otherwise)");
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "amount to be added or subtracted (e.g. -3, 4)", true);
        OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you.", true);
        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName, role));
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(faction, user, game));
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, game, resourceName, isNumber, resourceValNumber, resourceValString));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(game, faction, resourceName, amount));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(game, faction, resourceName));
        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) {

        String name = event.getOption("name").getAsString();
        event.getGuild().createCategory(name).addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getGuild().getRolesByName("Bot Tester", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null)
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
        JSONObject factions = new JSONObject();
        JSONObject gameResources = new JSONObject();
        JSONObject gameBoard = Initializers.buildBoard();
        JSONObject traitorDeck = new JSONObject();
        JSONObject spiceDeck = Initializers.buildSpiceDeck();
        JSONObject spiceDiscardA = new JSONObject();
        JSONObject spiceDiscardB = new JSONObject();
        JSONObject treacheryDeck = Initializers.buildTreacheryDeck();
        JSONObject treacheryDiscard = new JSONObject();
        JSONObject tanksForces = new JSONObject();
        JSONObject tanksLeaders = new JSONObject();

        gameResources.put("turn", 0);
        gameResources.put("shieldwallbroken", false);
        gameResources.put("traitor_deck", traitorDeck);
        gameResources.put("spice_deck", spiceDeck);
        gameResources.put("spice_discardA", spiceDiscardA);
        gameResources.put("spice_discardB", spiceDiscardB);
        gameResources.put("treachery_deck", treacheryDeck);
        gameResources.put("treachery_discard", treacheryDiscard);
        gameResources.put("tanks_forces", tanksForces);
        gameResources.put("tanks_leaders", tanksLeaders);
        gameState.put("factions", factions);
        gameState.put("game_resources", gameResources);
        gameState.put("game_board", gameBoard);
        object.put("game_state", gameState);
        object.put("version", 1);
        pushGameState(object, category);
    }

    public void addFaction(SlashCommandInteractionEvent event) throws IOException {

        JSONObject gameState = getGameState(event);
        String factionName = event.getOption("factionname").getAsString();

        Faction faction = new Faction(factionName, ":" + factionName + ":", event.getOption("player").getAsUser().getAsTag());
        HashMap<String, Integer> startingSpice = new HashMap<>();
        startingSpice.put("Atreides", 10);
        startingSpice.put("Harkonnen", 10);
        startingSpice.put("Emperor", 10);
        startingSpice.put("Fremen", 3);
        startingSpice.put("BG", 5);
        startingSpice.put("Guild", 5);
        startingSpice.put("Ix", 10);
        startingSpice.put("BT", 5);
        startingSpice.put("CHOAM", 2);
        startingSpice.put("Rich", 5);
        faction.addResource(new Resource<>("spice", startingSpice.get(factionName)));

        gameState.getJSONObject("game_state").getJSONObject("factions").put(faction.getName(), faction);

        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
        Category game = event.getOption("game").getAsChannel().asCategory();
        game.createTextChannel(factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                        .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        game.createTextChannel(factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();

    }

    public void newFactionResource(SlashCommandInteractionEvent event) throws IOException {
        JSONObject gameState = getGameState(event);
        if (gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()) == null) {
            event.getChannel().sendMessage("That faction is not in this game!").queue();
            return;
        }

        Object value = event.getOption("isanumber").getAsBoolean() ? event.getOption("numbervalue").getAsInt() : event.getOption("othervalue").getAsString();

        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()).getJSONObject("resources").put(event.getOption("resource").getAsString(), value);
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event) throws IOException {
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

    public void removeResource(SlashCommandInteractionEvent event) throws IOException {
        JSONObject gameState = getGameState(event);
        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString())
                .getJSONObject("resources").remove(event.getOption("resource").getAsString());
        pushGameState(gameState, event.getOption("game").getAsChannel().asCategory());
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
