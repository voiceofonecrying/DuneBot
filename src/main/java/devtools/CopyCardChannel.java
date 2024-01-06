package devtools;

import exceptions.ChannelNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

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

        JDA mainJDA = JDABuilder.createDefault(mainToken).build().awaitReady();
        JDA testJDA = JDABuilder.createDefault(testToken).build().awaitReady();

        TextChannel mainChannel = getChannel(mainJDA, mainGuildId, channelName);
        TextChannel testChannel = getChannel(testJDA, testGuildId, channelName);

        MessageHistory messageHistory = mainChannel.getHistory();

        messageHistory.retrievePast(100).complete();

        List<Message> messages = messageHistory.getRetrievedHistory();

        System.out.println("Number of cards: " + messages.size());

        for (Message message : messages) {
            MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                    .addContent(message.getContentRaw());

            for (Message.Attachment attachment : message.getAttachments()) {
                messageCreateBuilder = messageCreateBuilder.addFiles(FileUpload.fromData(attachment.getProxy().download().get(), "card.png"));
            }

            testChannel.sendMessage(messageCreateBuilder.build()).complete();
        }

        mainJDA.shutdownNow();
        testJDA.shutdownNow();
        System.exit(0);
    }

    private static TextChannel getChannel(JDA jda, String guildId, String channelName) throws ChannelNotFoundException {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) throw new ChannelNotFoundException("Guild not found");
        List<Category> categories = guild.getCategoriesByName("Game Resources", true);
        if (categories.isEmpty()) throw new ChannelNotFoundException("Category Game Resources not found");
        Category category = categories.get(0);
        List<TextChannel> channels = category.getTextChannels();
        Optional<TextChannel> channel = channels.stream().filter(c -> c.getName().equalsIgnoreCase(channelName))
                .findFirst();

        if (channel.isEmpty()) {
            throw new ChannelNotFoundException("The channel was not found");
        }

        return channel.get();
    }
}
