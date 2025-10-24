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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.util.logging.Level;

public class DuneBot {
    private static final Logger logger = LoggerFactory.getLogger(DuneBot.class);
    public static String getConfigValue(String variable, boolean isRequired) {
        if (System.getenv(variable) != null) {
            return System.getenv(variable);
        } else {
            try {
                Dotenv dotenv = Dotenv.configure().load();
                return dotenv.get(variable);
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
        logger.info("There are {} commands in DuneBot", CommandManager.getAllCommands().size());
        java.util.logging.Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
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
            EventListener eventListener = new EventListener(jda);
            ButtonManager buttonManager = new ButtonManager();
            jda.addEventListener(eventListener, commandManager, buttonManager);

            jda.awaitReady();

            jda.getGuilds().forEach((guild) -> {
                EmojiCache.setEmojis(guild.getId(), guild.getEmojis());
                guild.loadMembers().onSuccess(commandManager::gatherMembers);
            });

            Signal.handle(new Signal("USR1"), signal -> {
                logger.info("Received USR1 signal. Stopping all commands...");
                jda.removeEventListener(eventListener, commandManager, buttonManager);
                CommandCompletionGuard.blockUntilNoCommands();
                System.exit(0);
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down bot...");
                jda.shutdown();
            }));
        } catch (DotenvException e) {
            logger.error("Dotenv file or Token not found", e);
            throw new RuntimeException(e);
        } catch (InvalidTokenException e) {
            logger.error("Invalid Token", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Bot interrupted during startup", e);
            throw new RuntimeException(e);
        }
    }
}
