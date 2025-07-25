package devtools;

import caches.EmojiCache;
import controller.commands.CommandManager;
import controller.commands.ReportsCommands;
import exceptions.ChannelNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.List;

public class ReportsCommandsTester {
    public List<Member> members = new ArrayList<>();
    public static void main(String[] args) throws InterruptedException, ChannelNotFoundException {
        CommandManager cm = new CommandManager();
        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");
        JDA testJDA = JDABuilder.createDefault(testToken)
                .addEventListeners(cm)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .build().awaitReady();
        Guild guild = testJDA.getGuildById(testGuildId);
        if (guild == null) throw new ChannelNotFoundException("Guild not found");

        EmojiCache.setEmojis(guild.getId(), guild.getEmojis());
//        String a = ReportsCommands.playedAllExpansion(guild, List.of());
//        System.out.println(a + " " + a.length() + "\n============");
        System.out.println(ReportsCommands.updateStats(guild, testJDA, true, List.of(), true, false));
//        System.out.println(ReportsCommands.playerFastestGame(guild, 3, 3, 3));
//        System.out.println(ReportsCommands.wonAsMostFactions(guild, List.of()));

        testJDA.shutdownNow();
        System.exit(0);
    }
}
