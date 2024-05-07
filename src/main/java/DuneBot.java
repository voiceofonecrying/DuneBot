import caches.EmojiCache;
import controller.buttons.ButtonManager;
import controller.commands.CommandManager;
import controller.listeners.EventListener;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DuneBot {

    public static void main(String[] args) {
        System.out.println("There are " + CommandManager.getAllCommands().size() + " commands in Dunebot.");
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        try {
            String token = Dotenv.configure().load().get("TOKEN");

            JDA jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Dune"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .build();

            CommandManager cm = new CommandManager();
            jda.addEventListener(new EventListener(), cm, new ButtonManager());

            jda.awaitReady();

            jda.getGuilds().forEach((guild) -> {
                EmojiCache.setEmojis(guild.getId(), guild.getEmojis());
//                guild.loadMembers().onSuccess(cm::gatherMembers);
            });
        } catch (DotenvException e) {
            System.err.println("Dotenv file or Token not found.");
            throw new RuntimeException(e);
        } catch (InvalidTokenException e) {
            System.err.println("Invalid Token");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
