package Controller;

import Model.Game;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class Commands {

    public static void newGame(MessageReceivedEvent event) {
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
        String name = event.getMessage().getContentRaw().replace("$new game$", "").strip();
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

    public static void buildChannels(MessageReceivedEvent event, String name) throws InterruptedException {
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
