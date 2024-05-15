import caches.EmojiCache;
import controller.CommandCompletionGuard;
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
import sun.misc.Signal;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DuneBot {
    public static String getConfigValue(String variable, boolean isRequired) {
        if (System.getenv(variable) != null) {
            return System.getenv(variable);
        } else {
            try {
                Dotenv dotenv = Dotenv.configure().load();
                return dotenv.get("TOKEN");
            } catch (DotenvException e) {
                if (isRequired) {
                    throw new RuntimeException(e);
                } else {
                    return null;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("There are " + CommandManager.getAllCommands().size() + " commands in Dunebot.");
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        try {
            String token = getConfigValue("TOKEN", true);

            JDA jda = JDABuilder.createDefault(token)
                    .setEnableShutdownHook(false)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Dune"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .build();

            if ("1".equals(getConfigValue("ALLOW_MOD_BUTTON_PRESS", false))) {
                ButtonManager.setAllowModButtonPress();
            }

            CommandManager commandManager = new CommandManager();
            EventListener eventListener = new EventListener();
            ButtonManager buttonManager = new ButtonManager();
            jda.addEventListener(eventListener, commandManager, buttonManager);

            jda.awaitReady();

            jda.getGuilds().forEach((guild) -> {
                EmojiCache.setEmojis(guild.getId(), guild.getEmojis());
                guild.loadMembers().onSuccess(commandManager::gatherMembers);
            });

            Signal.handle(new Signal("USR1"), signal -> {
                System.out.println("Received USR1 signal. Stopping all commands...");
                jda.removeEventListener(eventListener, commandManager, buttonManager);
                CommandCompletionGuard.blockUntilNoCommands();
                System.exit(0);
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down bot...");
                jda.shutdown();
            }));
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
