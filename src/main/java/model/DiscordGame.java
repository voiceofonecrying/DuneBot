package model;

import com.google.gson.Gson;
import exceptions.ChannelNotFoundException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DiscordGame {
    private final Member member;
    private final Category gameCategory;
    private List<TextChannel> textChannelList;
    private Game gameState;

    public DiscordGame(@NotNull SlashCommandInteractionEvent event) throws ChannelNotFoundException {
        this.gameCategory = event.getChannel().asTextChannel().getParentCategory();
        this.member = event.getMember();
        this.gameState = this.getGameState();
    }

    public DiscordGame(@NotNull SlashCommandInteractionEvent event, @NotNull Category category) {
        this.gameCategory = category;
        this.member = event.getMember();
    }

    public DiscordGame(@NotNull CommandAutoCompleteInteractionEvent event) {
        this.gameCategory = Objects.requireNonNull(event.getChannel()).asTextChannel().getParentCategory();
        this.member = event.getMember();
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
        return this.member
                .getRoles()
                .stream().map(Role::getName)
                .toList()
                .contains(modRoleName);
    }

    public void setGameState(Game gameState) {
        this.gameState = gameState;
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
                Gson gson = new Gson();
                Game returnGame = gson.fromJson(gameStateString, Game.class);
                if (!this.isModRole(returnGame.getModRole())) {
                    throw new IllegalArgumentException("ERROR: command issuer does not have specified moderator role");
            }
            return returnGame;
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Didn't work...");
            return new Game();}
        }
        return this.gameState;
    }

    public void pushGameState() {
        Gson gson = new Gson();
        FileUpload fileUpload = FileUpload.fromData(
                gson.toJson(this.gameState).getBytes(StandardCharsets.UTF_8), "gamestate.json"
        );

        try {
            this.getBotDataChannel().sendFiles(fileUpload).complete();
        } catch (ChannelNotFoundException e) {
            System.out.println("Channel not found. State was not saved.");
        }
    }

    public void sendMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.gameState.getMute()) return;
        channel.sendMessage(message).queue();
    }
}
