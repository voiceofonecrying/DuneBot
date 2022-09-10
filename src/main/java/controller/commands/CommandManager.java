package controller.commands;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("newgame")) {
            event.reply("working...").queue();
            newGame(event);
            event.getChannel().sendMessage("done!").queue();
        }
        //implement new slash commands here

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        //add new slash command definitions to commandData list

        OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName));
        commandData.add(Commands.slash("testing", "test"));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public static void newGame(SlashCommandInteractionEvent event) {
        if (event.getMember() == null) {
            event.getChannel().sendMessage("You are not a Game Master").queue();
            return;
        }
        List<Role> roles = event.getMember().getRoles();
        for (Role role : roles) {
            if (!role.getName().equals("Game Master")) {
                event.getChannel().sendMessage("You are not a Game Master").queue();
                return;
            }
        }
        String name = event.getOption("name").getAsString();
        event.getGuild().createCategory(name).complete();

        try {
            buildChannels(event, name);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TextChannel botData = event.getGuild().getCategoriesByName(name, true).get(0).getTextChannels().get(0);

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

    public static void buildChannels(SlashCommandInteractionEvent event, String name) throws InterruptedException {
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
    }
}
