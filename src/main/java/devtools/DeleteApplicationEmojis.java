package devtools;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;

import java.util.List;

public class DeleteApplicationEmojis {
    public static void main(String[] args) throws InterruptedException {
        String testToken = Dotenv.configure().load().get("TEST_TOKEN");

        JDA testJDA = getJDA(testToken);

        List<ApplicationEmoji> emojis = testJDA.retrieveApplicationEmojis().complete();
        System.out.println("Deleting " + emojis.size() + " application emojis");

        for (ApplicationEmoji emoji : emojis) {
            System.out.println("Deleting: " + emoji.getName());
            emoji.delete().complete();
        }

        System.out.println("Done");

        testJDA.shutdownNow();
        System.exit(0);
    }

    private static JDA getJDA(String token) throws InterruptedException {
        return JDABuilder.createDefault(token)
                .build()
                .awaitReady();
    }
}
