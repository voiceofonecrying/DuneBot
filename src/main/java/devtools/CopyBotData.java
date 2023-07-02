package devtools;

import exceptions.ChannelNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import model.DiscordGame;
import model.factions.Faction;
import model.Game;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

import java.io.IOException;
import java.util.List;

public class CopyBotData {
    public static void main(String[] args) throws ChannelNotFoundException, InterruptedException, IOException {
        String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
        String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");

        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");

        String category = Dotenv.configure().load().get("COPY_CATEGORY");
        String gameRole = Dotenv.configure().load().get("COPY_GAME_ROLE");
        String modRole = Dotenv.configure().load().get("COPY_MOD_ROLE");
        String player = Dotenv.configure().load().get("COPY_PLAYER");

        System.out.println("Copying data from " + category);

        DiscordGame mainDiscordGame = getDiscordGame(mainToken, mainGuildId, category);
        DiscordGame testDiscordGame = getDiscordGame(testToken, testGuildId, category);

        Game mainGame = mainDiscordGame.getGame();

        mainGame.setGameRole(gameRole);
        mainGame.setModRole(modRole);

        for (Faction faction : mainGame.getFactions()) {
            faction.setPlayer(player);
        }

        testDiscordGame.setGame(mainGame);

        testDiscordGame.pushGame();
    }

    private static DiscordGame getDiscordGame(String token, String guildId, String category) throws InterruptedException, ChannelNotFoundException {
        JDA jda = JDABuilder.createDefault(token)
                .build();
        jda.awaitReady();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) throw new ChannelNotFoundException("Guild not found");
        List<Category> categories = guild.getCategoriesByName(category, true);
        if (categories.size() == 0) throw new ChannelNotFoundException("Category " + category + " not found");
        Category mainCategory = categories.get(0);
        return new DiscordGame(mainCategory);
    }
}
