package Controller;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.List;

public class DuneBot extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {
        JDABuilder.createLight("MTAwNTUzODI2NjQ0OTE5MDk0Mg.GFEkn0.Ybjmg2SJGJRZ8G8p8Tt57bGaSXhDtegOLzyab8", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DuneBot())
                .setActivity(Activity.playing("Dune"))
                .build();
        }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Message msg = event.getMessage();
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;
        if (msg.getContentRaw().contains("$new game$")) Commands.newGame(event);
        List<Category> categories = event.getGuild().getCategories();
    }
}

/*
        if (msg.getContentRaw().equals("!ping"))
        {
            channel.sendMessage("Pong!").queue();
        } else if (msg.getContentRaw().equals("list territories")) {

            channel.sendMessage("Dune is comprised of the following territories:").queue();
            List<String> territories = session.createQuery("select distinct territoryName from Territory", String.class).list();
            for (String territory : territories) {
                channel.sendMessage(territory).queue();
            }

        }
 */