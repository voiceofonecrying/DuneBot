package devtools;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.List;
import java.util.Objects;

public class DeleteCategory {
    public static void main(String[] args) throws InterruptedException {
        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");

        String categoryName = Dotenv.configure().load().get("DELETE_CATEGORY");

        System.out.println("Deleting categoryName " + categoryName);

        JDA testJDA = JDABuilder.createDefault(testToken).build().awaitReady();

        List<Category> categories = Objects.requireNonNull(testJDA.getGuildById(testGuildId)).getCategoriesByName(categoryName, false);

        if (categories.size() != 1) {
            throw new IllegalArgumentException("There is not exactly one occurrence of the category");
        }

        Category category = categories.get(0);

        List<GuildChannel> channels = category.getChannels();

        channels.forEach(c -> c.delete().complete());

        category.delete().complete();

        testJDA.shutdownNow();
        System.exit(0);
    }
}
