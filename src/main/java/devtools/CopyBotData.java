package devtools;

import exceptions.ChannelNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import model.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

import java.util.List;

public class CopyBotData {
    public static void main(String[] args) throws ChannelNotFoundException, InterruptedException {
        String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
        String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");

        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");

        String category = Dotenv.configure().load().get("COPY_CATEGORY");
        String gameRole = Dotenv.configure().load().get("COPY_GAME_ROLE");
        String modRole = Dotenv.configure().load().get("COPY_MOD_ROLE");
        String player = Dotenv.configure().load().get("COPY_PLAYER");

        System.out.println("Copying data from " + category);

        JDA mainJDA = JDABuilder.createDefault(mainToken).build().awaitReady();
        JDA testJDA = JDABuilder.createDefault(testToken).build().awaitReady();

        DiscordGame mainDiscordGame = getDiscordGame(mainJDA, mainGuildId, category);
        DiscordGame testDiscordGame = getDiscordGame(testJDA, testGuildId, category);

        int copyOffset = Dotenv.configure().load().get("COPY_OFFSET") != null ?
                Integer.parseInt(Dotenv.configure().load().get("COPY_OFFSET")) : 0;

        MessageHistory h = mainDiscordGame.getBotDataChannel()
                .getHistory();

        h.retrievePast(1 + copyOffset).complete();

        List<Message> ml = h.getRetrievedHistory();
        Game mainGame = mainDiscordGame.getGame(ml.get(copyOffset));

        mainGame.setGameRole(gameRole);
        mainGame.setModRole(modRole);

        for (Faction faction : mainGame.getFactions()) {
            faction.setPlayer(player);
        }

        testDiscordGame.setGame(mainGame);

        testDiscordGame.pushGame();
        testDiscordGame.sendAllMessages();

        mainJDA.shutdownNow();
        testJDA.shutdownNow();
        System.exit(0);
    }

    private static DiscordGame getDiscordGame(JDA jda, String guildId, String category) throws InterruptedException, ChannelNotFoundException {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) throw new ChannelNotFoundException("Guild not found");
        List<Category> categories = guild.getCategoriesByName(category, true);
        if (categories.isEmpty()) throw new ChannelNotFoundException("Category " + category + " not found");
        Category mainCategory = categories.get(0);
        return new DiscordGame(mainCategory);
    }
}
