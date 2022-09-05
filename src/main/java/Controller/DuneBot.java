package Controller;

import Model.Constants;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class DuneBot extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {
        JDABuilder.createLight(Constants.key, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
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