package controller.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Collections;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        System.out.println(event.getMessage().getContentRaw());
        String message = event.getMessage().getContentRaw().replaceAll("\\d", "");

        //Treachery Card Service
        if (message.matches(".*<:treachery:> .* <:treachery:>.*")) {
            String fileName = message.split("<:treachery:>")[1].strip().replace(" ", "_") + ".jpg";
            String resourceName = "Treachery Cards/" + fileName;

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);

            if (inputStream == null) {
                System.out.println("No Treachery Card named " + message);
            } else {
                FileUpload fileUpload = FileUpload.fromData(inputStream, fileName);
                event.getChannel().sendFiles(fileUpload).queue();
            }
        }

        //Add any other text based commands here
    }


}
