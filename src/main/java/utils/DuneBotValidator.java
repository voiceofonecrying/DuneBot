package utils;

import controller.commands.CommandManager;
import controller.listeners.EventListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.function.Predicate;

public class DuneBotValidator implements Predicate<String> {

    @Override
    public boolean test(String s) {
        try {
            String token = Dotenv.configure().load().get("TOKEN");

            JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Dune"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build()
                    .addEventListener(new EventListener(), new CommandManager());
        } catch (InvalidTokenException e) {
            return false;
        }

        return true;
    }
}
