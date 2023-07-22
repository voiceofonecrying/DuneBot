package model;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import helpers.Exclude;
import io.gsonfire.GsonFireBuilder;
import model.factions.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DiscordGame {
    private final Category gameCategory;
    private List<TextChannel> textChannelList;
    private Game game;

    private GenericInteractionCreateEvent event;

    public DiscordGame(@NotNull SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        this.gameCategory = event.getChannel().asTextChannel().getParentCategory();
        this.game = this.getGame();
        this.event = event;
    }
    public DiscordGame(@NotNull ButtonInteractionEvent event) throws ChannelNotFoundException, IOException {
        this.gameCategory = event.getChannel().asTextChannel().getParentCategory();
        this.game = this.getGame();
        this.event = event;
    }

    public DiscordGame(@NotNull CommandAutoCompleteInteractionEvent event) {
        this.gameCategory = Objects.requireNonNull(event.getChannel()).asTextChannel().getParentCategory();
    }

    public DiscordGame(Category category) {
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

    public SlashCommandInteractionEvent getEvent() {
        return (SlashCommandInteractionEvent) this.event;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Loads the game state from the bot data channel, or returns an already loaded game state.
     *
     * @return Game object representing the game state.
     * @throws ChannelNotFoundException If the bot data channel is not found.
     */
    public Game getGame() throws ChannelNotFoundException, IOException {
        if (this.game == null) {
            MessageHistory h = this.getBotDataChannel()
                    .getHistory();

            h.retrievePast(1).complete();

            List<Message> ml = h.getRetrievedHistory();
            Message.Attachment encoded = ml.get(0).getAttachments().get(0);
            CompletableFuture<InputStream> future = encoded.getProxy().download();

            try {
                String gameStateString = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
                Gson gson = createGsonDeserializer();
                Game returnGame = gson.fromJson(gameStateString, Game.class);
                future.get().close();
                addGameReferenceToFactions(returnGame);
                migrateGameState(returnGame);
            return returnGame;
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Didn't work...");
            return new Game();}
        }
        return this.game;
    }

    /**
     * Adds a reference to the game object to all factions in the game.
     * @param game Game object to add references to.
     */
    public static void addGameReferenceToFactions(Game game) {
        for (Faction faction : game.getFactions()) {
            faction.setGame(game);
        }
    }

    /**
     * Removes the reference to the game object from all factions in the game.
     * @param game Game object to remove references from.
     */
    public static void removeGameReferenceFromFactions(Game game) {
        for (Faction faction : game.getFactions()) {
            faction.setGame(null);
        }
    }

    /**
     * Creates a Gson object that can deserialize the GameState object.
     *
     * @return Gson object that can deserialize the GameState object.
     */
    public static Gson createGsonDeserializer() {
        GsonFireBuilder builder = new GsonFireBuilder()
                .registerTypeSelector(Faction.class, new FactionTypeSelector());
        return builder.createGson();
    }

    public static void migrateGameState(Game game) {
        if (game.getGameOptions() == null) {
            game.setGameOptions(new HashSet<>());
        }

        if (game.hasTechTokens()) {
            game.addGameOption(GameOption.TECH_TOKENS);
        }

        if (game.hasLeaderSkills()) {
            game.addGameOption(GameOption.LEADER_SKILLS);
        }

        if (game.hasStrongholdSkills()) {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        }
    }

    public void pushGame() throws ChannelNotFoundException {
        removeGameReferenceFromFactions(this.game);

        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getAnnotation(Exclude.class) != null;
            }
        };

        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(strategy)
                .create();

        FileUpload fileUpload = FileUpload.fromData(
                gson.toJson(this.game).getBytes(StandardCharsets.UTF_8), "gamestate.json"
        );

        String message = getEvent() == null ? "Manual update" : "Command: `" + getEvent().getCommandString() + "`";

        sendMessage("bot-data", message, fileUpload);
    }

    public void sendMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.game.getMute()) return;
        channel.sendMessage(message).queue();
    }
    public MessageCreateAction prepareMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        return channel.sendMessage(message);
    }

    public MessageCreateAction prepareDeferredReply(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        return channel.sendMessage(message);
    }

    public void sendMessage(String name, MessageCreateData message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.game.getMute()) return;
        channel.sendMessage(message).queue();
    }

    public void sendMessage(String name, String message, FileUpload fileUpload) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.game.getMute()) return;
        channel.sendMessage(message).addFiles(fileUpload).queue();
    }

    public void sendMessage(String name, String message, List<FileUpload> fileUploads) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        if (this.game.getMute()) return;
        channel.sendMessage(message).addFiles(fileUploads).queue();
    }

    /**
     * Creates a thread in the parent channel with the given name and adds the given users to it.
     * @param parentChannelName The name of the parent channel
     * @param threadName The name of the thread to create
     * @param userIds The ids of the users to add to the thread.  All non-numeeric characters will be removed.
     * @throws ChannelNotFoundException Thrown if the parent channel is not found.
     */
    public void createThread(String parentChannelName, String threadName, List<String> userIds) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(parentChannelName);

        channel.createThreadChannel(threadName, true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .queue(
                        threadChannel -> addUsersToThread(threadChannel, userIds)
                );
    }

    /**
     * Adds the given users to the given thread.
     * @param threadChannel The thread to add the users to
     * @param userIds The ids of the users to add to the thread.  All non-numeeric characters will be removed.
     */
    private void addUsersToThread(ThreadChannel threadChannel, List<String> userIds) {
        JDA jda = threadChannel.getJDA();

        userIds.forEach(
                userId -> jda.retrieveUserById(userId.replaceAll("\\D", "")).queue(
                        user -> threadChannel.addThreadMember(user).queue()
                )
        );
    }

    public Faction getFactionByPlayer(String user) throws ChannelNotFoundException, IOException {
        return getGame().getFactions().stream()
                .filter(f ->
                        f.getPlayer()
                                .substring(2)
                                .replace(">", "")
                                .equals(user
                                        .split("=")[1]
                                        .replace(")", "")
                                )
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public OptionMapping required(OptionData optionData) {
        OptionMapping optionMapping = optional(optionData);

        if (optionMapping == null) {
            throw new IllegalArgumentException("Required option is missing: " + optionData.getName());
        }

        return optionMapping;
    }

    public OptionMapping optional(OptionData optionData) {
        String optionName = optionData.getName();
        SlashCommandInteractionEvent newEvent = (SlashCommandInteractionEvent) event;
        return newEvent.getOption(optionName);
    }
}
