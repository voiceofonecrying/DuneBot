package devtools;

import exceptions.ChannelNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import model.DiscordGame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class CopyCardChannel {
    public static void main(String[] args) throws ChannelNotFoundException, InterruptedException, ExecutionException {
        String mainToken = Dotenv.configure().load().get("MAIN_TOKEN");
        String mainGuildId = Dotenv.configure().load().get("MAIN_GUILD_ID");

        String testToken = Dotenv.configure().load().get("TEST_TOKEN");
        String testGuildId = Dotenv.configure().load().get("TEST_GUILD_ID");

        String channelName = Dotenv.configure().load().get("COPY_CARD_CHANNEL");

        System.out.println("Copying data from " + channelName);

        TextChannel mainChannel = getChannel(mainToken, mainGuildId, channelName);
        TextChannel testChannel = getChannel(testToken, testGuildId, channelName);

        MessageHistory messageHistory = mainChannel.getHistory();

        messageHistory.retrievePast(100).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        System.out.println("Number of cards: " + messages.size());

        for (Message message : messages) {
            MessageCreateData data = new MessageCreateBuilder()
                    .addContent(message.getContentRaw())
                    .addFiles(FileUpload.fromData(message.getAttachments().get(0).getProxy().download().get(), "card.png"))
                    .build();
            testChannel.sendMessage(data).complete();
        }
    }

    private static TextChannel getChannel(String token, String guildId, String channelName) throws InterruptedException, ChannelNotFoundException {
        JDA jda = JDABuilder.createDefault(token)
                .build();
        jda.awaitReady();
        Guild guild = jda.getGuildById(guildId);
        Category category = guild.getCategoriesByName("Mod area", true).get(0);
        List<TextChannel> channels = category.getTextChannels();
        Optional<TextChannel> channel = channels.stream().filter(c -> c.getName().equalsIgnoreCase(channelName))
                .findFirst();

        if (channel.isEmpty()) {
            throw new ChannelNotFoundException("The channel was not found");
        }

        return channel.get();
    }
}
