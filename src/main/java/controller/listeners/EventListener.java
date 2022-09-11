package controller.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().replaceAll("\\d", "");

        //Treachery Card Service
        if (message.matches(".*<:treachery:> .* <:treachery:>.*")) {
            URL file = getClass().getClassLoader().getResource("Treachery Cards/" + message.split("<:treachery:>")[1].strip().replace(" ", "_") + ".jpg");
            try {
                event.getChannel().sendFile(new File(file.toURI())).queue();
            } catch (URISyntaxException e) {
                System.out.println("No Treachery Card named " + message);
            }
        }

        //Add any other text based commands here
    }


}
