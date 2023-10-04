package devtools;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CopyEmojis {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
        String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");

        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");

        boolean deleteEmojis = Objects.requireNonNullElse(Dotenv.configure().load().get("DELETE_EMOJIS"), "0")
                .equals("1");

        JDA mainJDA = getJDA(mainToken);
        JDA testJDA = getJDA(testToken);
        Guild mainGuild = mainJDA.getGuildById(mainGuildId);
        Guild testGuild = testJDA.getGuildById(testGuildId);

        if (mainGuild == null || testGuild == null) throw new RuntimeException("Guild not found");

        List<RichCustomEmoji> testEmojis = testGuild.getEmojis();
        List<RichCustomEmoji> mainEmojis = mainGuild.getEmojis();

        if (deleteEmojis) {
            testEmojis.forEach(emoji -> {
                System.out.println("Deleting: " + emoji.getName());
                emoji.delete().complete();
            });
        }


        List<String> testEmojiNames = testGuild.getEmojis().stream().map(Emoji::getName).toList();

        for (RichCustomEmoji emoji : mainEmojis) {
            if (testEmojiNames.contains(emoji.getName())) {
                System.out.println("Skipping: " + emoji.getName());
            } else {
                System.out.println("Copying: " + emoji.getName());
                InputStream inputStream = emoji.getImage().download().get();
                testGuild.createEmoji(emoji.getName(), Icon.from(inputStream)).complete();
            }
        }

        System.out.println("Done");

        mainJDA.shutdown();
        testJDA.shutdown();
    }

    private static JDA getJDA(String token) throws InterruptedException {
        return JDABuilder.createDefault(token)
                .build()
                .awaitReady();
    }
}
