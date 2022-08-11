

package Controller;

import Model.Territory;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.security.auth.login.LoginException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends ListenerAdapter
{
    int spice = 0;
    static String gameName = "";
    static Session thisSession;
    public static void run(String key, ResultSet game, Session session) throws LoginException, SQLException {
        // args[0] should be the token
        // We only need 3 intents in this bot. We only respond to messages in guilds and private channels.
        // All other events will be disabled.
        thisSession = session;
        JDABuilder.createLight(key, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Bot())
                .setActivity(Activity.playing("Type !ping"))
                .build();

        game.next();
        gameName = game.getString("GAME_NAME");
        game.close();

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();
        channel.sendMessage("The game is: " + gameName).queue();
        if (msg.getContentRaw().equals("!ping"))
        {
            channel.sendMessage("Pong!").queue();
        } else if (msg.getContentRaw().equals("spend spice")) {
            spice--;
            if (spice >= 0) channel.sendMessage("you have " + spice + " spice left.").queue();
            else channel.sendMessage("you are out of spice!").queue();
        } else if (msg.getContentRaw().equals("gain spice")) {
            spice++;
            channel.sendMessage("you have " + spice + " spice left.").queue();
        } else if (msg.getContentRaw().equals("list territories")) {
            channel.sendMessage("Dune is comprised of the following territories:").queue();
            List<String> territories = thisSession.createQuery("select distinct territoryName from Territory", String.class).list();
            for (String territory : territories) {
                channel.sendMessage(territory).queue();
            }

        }
    }
}