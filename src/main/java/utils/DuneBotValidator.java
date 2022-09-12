package utils;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

import javax.security.auth.login.LoginException;
import java.util.function.Predicate;

public class DuneBotValidator implements Predicate<String> {

    @Override
    public boolean test(String s) {
        try {
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(s);
            builder.setStatus(OnlineStatus.ONLINE);
            builder.setActivity(Activity.playing("Dune"));
            builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
            builder.build();
        } catch (LoginException e) {
            return false;
        }
        return true;
    }
}
