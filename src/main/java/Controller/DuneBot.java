package Controller;

import Model.Territory;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.security.auth.login.LoginException;
import java.util.List;

public class DuneBot extends ListenerAdapter {

    public static void main(String[] args) throws LoginException {
        JDABuilder.createLight("MTAwNTUzODI2NjQ0OTE5MDk0Mg.GvY98f.28Tl-Bzeaqy9_ssjFbci1hQWt849sqxlhWOPw4", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DuneBot())
                .setActivity(Activity.playing("Type !ping"))
                .build();
        }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;
        if (!event.getGuild().getCategoriesByName("Dune Game Instance", true).get(0).getChannels().contains(event.getChannel())) return;
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();
        if (msg.getContentRaw().equals("!ping"))
        {
            channel.sendMessage("Pong!").queue();
        } else if (msg.getContentRaw().equals("list territories")) {
            SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
            Session session = sessionFactory.openSession();
            channel.sendMessage("Dune is comprised of the following territories:").queue();
            List<Territory> territories = session.createQuery("select Territory from Territory", Territory.class).list();
            session.close();
            for (Territory territory : territories) {
                channel.sendMessage(territory.getTerritoryName() + ", sector " + territory.getSector()).queue();
            }

        }
    }
}
