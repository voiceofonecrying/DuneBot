package model;

import exceptions.ChannelNotFoundException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DiscordGame {
    private SlashCommandInteractionEvent event;
    private Category gameCategory;
    private List<TextChannel> textChannelList;
    private Game gameState;

    public DiscordGame(@NotNull SlashCommandInteractionEvent event) throws ChannelNotFoundException {
        this.event = event;
        this.gameCategory = event.getChannel().asTextChannel().getParentCategory();
        this.gameState = this.getGameState();
    }

    public DiscordGame(@NotNull SlashCommandInteractionEvent event, @NotNull Category category) {
        this.event = event;
        this.gameCategory = category;
    }

    public Category getGameCategory() {
        return this.gameCategory;
    }

    public List<TextChannel> getTextChannels() {
        if (this.textChannelList == null) {
            this.textChannelList = this.gameCategory.getTextChannels();
        }

        return this.textChannelList;
    }

    public TextChannel getTextChannel(String name) throws ChannelNotFoundException {
        for (TextChannel channel : this.getTextChannels()) {
            if (channel.getName().equals(name)) {
                return channel;
            }
        }
        throw new ChannelNotFoundException("The channel was not found");
    }

    public TextChannel getBotDataChannel() throws ChannelNotFoundException {
        return this.getTextChannel("bot-data");
    }

    public boolean isModRole(String modRoleName) {
        return this.event.getMember()
                .getRoles()
                .stream().map(role -> role.getName())
                .collect(Collectors.toList())
                .contains(modRoleName);
    }

    public Game getGameState() throws ChannelNotFoundException {
        if (this.gameState == null) {
            MessageHistory h = this.getBotDataChannel()
                    .getHistory();

            h.retrievePast(1).complete();

            List<Message> ml = h.getRetrievedHistory();
            Message.Attachment encoded = ml.get(0).getAttachments().get(0);
            CompletableFuture<InputStream> future = encoded.getProxy().download();

            try {
                String gameStateString = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
                Game returnGame = new Game(gameStateString);
                if (!this.isModRole(returnGame.getString("modrole"))) {
                    event.getHook().sendMessage("Only the moderator can do that!").queue();
                    throw new IllegalArgumentException("ERROR: command issuer does not have specified moderator role");
                }
                return returnGame;
            } catch (IOException | InterruptedException | ExecutionException e) {
                System.out.println("Didn't work...");
                return new Game();
            }
        }
        return this.gameState;
    }

    public void pushGameState(Game gameState) {
        FileUpload fileUpload = FileUpload.fromData(
                gameState.toString().getBytes(StandardCharsets.UTF_8), "gamestate.txt"
        );

        try {
            this.getBotDataChannel().sendFiles(fileUpload).complete();
        } catch (ChannelNotFoundException e) {
            System.out.println("Channel not found. State was not saved.");
        }
    }

    public void sendMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.gameState.getBoolean("mute")) return;
        channel.sendMessage(message).queue();
    }
}
