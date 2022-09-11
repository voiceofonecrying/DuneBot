package controller.commands;

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
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        if (name.equals("newgame")) {
            event.reply("working...").queue();
            newGame(event);
            event.getChannel().sendMessage("done!").queue();
        } else if (name.equals("addfaction")) {
            event.reply("adding faction...").setEphemeral(true).queue();
            addFaction(event);
        } else if (name.equals("newfactionresource")) {
            event.reply("adding new resource...").setEphemeral(true).queue();
            newFactionResource(event);
        }
        //implement new slash commands here

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        //add new slash command definitions to commandData list

        OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName));
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
        OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
        OptionData game = new OptionData(OptionType.CHANNEL, "game", "The game this action will be applied to", true).setChannelTypes(ChannelType.CATEGORY);
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(faction, user, game));
        OptionData resourceName = new OptionData(OptionType.STRING, "resource", "The name of the resource", true);
        OptionData isNumber = new OptionData(OptionType.BOOLEAN, "isanumber", "Set true if it is a numerical value, false otherwise.", true);
        OptionData resourceValNumber = new OptionData(OptionType.INTEGER, "numbervalue", "Set the initial value if the resource is a number (leave blank otherwise)");
        OptionData resourceValString = new OptionData(OptionType.STRING, "othervalue", "Set the initial value if the resource is not a number (leave blank otherwise)");
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, game, resourceName, isNumber, resourceValNumber, resourceValString));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) {

        if (event.getMember() == null) {
            event.getChannel().sendMessage("You are not a Game Master").queue();
            return;
        }
        List<Role> roles = event.getMember().getRoles();
        for (Role role : roles) {
            if (!role.getName().equals("Game Master") && !role.getName().equals("Dungeon Master")) {
                event.getChannel().sendMessage("You are not a Game Master").queue();
                return;
            }
        }


        String name = event.getOption("name").getAsString();
        event.getGuild().createCategory(name).complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("bot-data").complete();
        category.createTextChannel("out-of-game-chat").complete();
        category.createTextChannel("in-game-chat").complete();
        category.createTextChannel("turn-summary").complete();
        category.createTextChannel("game-actions").complete();
        category.createTextChannel("bribes").complete();
        category.createTextChannel("bidding-phase").complete();
        category.createTextChannel("rules").complete();
        category.createTextChannel("pre-game-voting").complete();

        TextChannel rules = category.getTextChannels().get(7);
        rules.sendMessage(":DuneRulebook01:  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf\n" +
                ":weirding:  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf\n" +
                ":ix: :bt:  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf\n" +
                ":choam: :rich: CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf").queue();


        TextChannel botData = category.getTextChannels().get(0);

        JSONObject object = new JSONObject();
        JSONObject gameState = new JSONObject();
        JSONObject factions = new JSONObject();
        JSONObject gameResources = new JSONObject();
        gameState.put("factions", factions);
        gameState.put("game_resources", gameResources);
        object.put("game_state", gameState);
        object.put("version", 1);
        botData.sendMessage(Base64.getEncoder().encodeToString(object.toString().getBytes(StandardCharsets.UTF_8))).queue();
    }

    public void addFaction(SlashCommandInteractionEvent event) {

        JSONObject gameState = getGameState(event);
        String factionName = event.getOption("factionname").getAsString();

        Faction faction = new Faction(String.valueOf(factionName.charAt(0)),factionName, ":" + factionName + ":", event.getOption("player").getAsUser().getAsTag());
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

        Category game = event.getOption("game").getAsChannel().asCategory();
        game.getTextChannels().get(0).sendMessage(Base64.getEncoder().encodeToString(gameState.toString().getBytes(StandardCharsets.UTF_8))).queue();

        game.createTextChannel(factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                        .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        game.createTextChannel(factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(game.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();

    }

    public void newFactionResource(SlashCommandInteractionEvent event) {
        JSONObject gameState = getGameState(event);
        if (gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()) == null) {
            event.getChannel().sendMessage("That faction is not in this game!");
            return;
        }

        Object value = event.getOption("isanumber").getAsBoolean() ? event.getOption("numbervalue").getAsInt() : event.getOption("othervalue").getAsString();

        gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(event.getOption("factionname").getAsString()).getJSONObject("resources").put(event.getOption("resource").getAsString(), value);
        Category game = event.getOption("game").getAsChannel().asCategory();
        game.getTextChannels().get(0).sendMessage(Base64.getEncoder().encodeToString(gameState.toString().getBytes(StandardCharsets.UTF_8))).queue();
    }

    public JSONObject getGameState(SlashCommandInteractionEvent event) {
        Category game = event.getOption("game").getAsChannel().asCategory();
        MessageHistory h = game.getTextChannels().get(0).getHistory();
        h.retrievePast(1).complete();
        List<Message> ml = h.getRetrievedHistory();
        String encoded = ml.get(0).getContentRaw();
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        String decoded = new String(decodedBytes);

        return new JSONObject(decoded);
    }

}
