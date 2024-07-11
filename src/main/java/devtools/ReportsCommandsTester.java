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
        System.out.println(ReportsCommands.updateStats(guild, testJDA, true, List.of(), true));
//        System.out.println(ReportsCommands.compareReportsMethod(guild, testJDA));
//        System.out.println(ReportsCommands.playedAllOriginalSix(guild, testJDA, guild.getMembers()));

// To fix JSON:
//        1. Edit JSON and CSV to have lower case. Keep CSV header as upper case.
//        2. Change field name to lower case
//        3. Have capitalize return lower case
// To fix factionStats and solo wins:
//        1. Change capitalization in updateFactionPerformance
//        2. Change capitalization in factionNames
//        3. Update turnStats to look for lower case G, F, Ecaz, BG
//        4. Also undo the replace in gatherGameResults for victoryType

        testJDA.shutdownNow();
        System.exit(0);
    }
}
