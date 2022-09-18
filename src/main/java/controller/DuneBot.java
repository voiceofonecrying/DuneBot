package controller;

import controller.commands.CommandManager;
import controller.listeners.EventListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

import javax.security.auth.login.LoginException;

public class DuneBot {

    public static void main(String[] args) {
        try {
            DefaultShardManagerBuilder.createDefault(Dotenv.configure().load().get("TOKEN")).setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Dune")).enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build().addEventListener(new EventListener(), new CommandManager());
        } catch (LoginException e) {
            System.out.println("ERROR: token is invalid");
        }
    }
}
