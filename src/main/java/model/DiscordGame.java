package model;

import caches.GameCache;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.channels.FactionChat;
import controller.channels.ModInfo;
import controller.channels.TurnSummary;
import exceptions.ChannelNotFoundException;
import helpers.Exclude;
import io.gsonfire.GsonFireBuilder;
import model.factions.EmperorFaction;
import model.factions.Faction;
import model.factions.FactionTypeSelector;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DiscordGame {
    private final Category gameCategory;
    @SuppressWarnings("rawtypes")
    private final List<RestAction> messageQueue = new ArrayList<>();
    private List<TextChannel> textChannelList;
    private Game game;
    private GenericInteractionCreateEvent event;

    public DiscordGame(@NotNull GenericInteractionCreateEvent event) throws ChannelNotFoundException, IOException {
        this.gameCategory = categoryFromEvent(event);
        this.game = this.getGame();
        this.event = event;
    }

    public DiscordGame(@NotNull MessageReceivedEvent event) throws ChannelNotFoundException, IOException {
        this.gameCategory = categoryFromEvent(event);
        this.game = this.getGame();
    }

    public DiscordGame(@NotNull CommandAutoCompleteInteractionEvent event) {
        this.gameCategory = categoryFromEvent(event);
    }

    public DiscordGame(Category category) {
        this.gameCategory = category;
    }

    public static Category categoryFromEvent(@NotNull GenericInteractionCreateEvent event) {
        Channel channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel.getParentCategory();
    }

    public static Category categoryFromEvent(@NotNull MessageReceivedEvent event) {
        MessageChannelUnion channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel.getParentCategory();
    }

    public static Category categoryFromEvent(@NotNull CommandAutoCompleteInteractionEvent event) {
        MessageChannelUnion channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel.getParentCategory();
    }

    /**
     * Adds a reference to the game object to all factions in the game.
     *
     * @param game Game object to add references to.
     */
    public static void addGameReferenceToFactions(Game game) {
        for (Faction faction : game.getFactions()) {
            faction.setGame(game);
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

    public TurnSummary getTurnSummary() throws ChannelNotFoundException {
        return new TurnSummary(this, game);
    }

    public ModInfo getModInfo() throws ChannelNotFoundException {
        return new ModInfo(this);
    }

    public FactionChat getFactionChat(String factionName) throws ChannelNotFoundException {
        return new FactionChat(this, factionName);
    }

    public FactionChat getAtreidesChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Atreides");
    }

    public FactionChat getBGChat() throws ChannelNotFoundException {
        return new FactionChat(this, "BG");
    }

    public FactionChat getBTChat() throws ChannelNotFoundException {
        return new FactionChat(this, "BT");
    }

    public FactionChat getCHOAMChat() throws ChannelNotFoundException {
        return new FactionChat(this, "CHOAM");
    }

    public FactionChat getEcazChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Ecaz");
    }

    public FactionChat getEmperorChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Emperor");
    }

    public FactionChat getFremenChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Fremen");
    }

    public FactionChat getGuildChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Guild");
    }

    public FactionChat getHarkonnenChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Harkonnen");
    }

    public FactionChat getIxChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Ix");
    }

    public FactionChat getMoritaniChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Moritani");
    }

    public FactionChat getRicheseChat() throws ChannelNotFoundException {
        return new FactionChat(this, "Richese");
    }

    public GenericInteractionCreateEvent getEvent() {
        return this.event;
    }

    /**
     * Loads the game state from the bot data channel, or returns an already loaded game state.
     *
     * @return Game object representing the game state.
     * @throws ChannelNotFoundException If the bot data channel is not found.
     */
    public Game getGame() throws ChannelNotFoundException {
        if (this.game == null) {
            String gameName = this.gameCategory.getName();

            if (GameCache.hasGameJson(gameName)) {
                this.game = gameJsonToGame(GameCache.getGameJson(gameName));
                return this.game;
            }

            MessageHistory h = this.getBotDataChannel()
                    .getHistory();

            h.retrievePast(1).complete();

            List<Message> ml = h.getRetrievedHistory();
            String gameJson = getGameJson(ml.get(0));
            Game game = gameJsonToGame(gameJson);
            if (game.getFactions().isEmpty() && game.getFactions().get(0).reserves != null) {
                runHomeworldsMigration(game);
            } else GameCache.setGameJson(gameName, gameJson);
            this.game = game;
        }
        return this.game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    private void runHomeworldsMigration(Game game) {
        HashMap<String, String> homeworlds = new HashMap<>();
        homeworlds.put("Atreides", "Caladan");
        homeworlds.put("Harkonnen", "Giedi Prime");
        homeworlds.put("Emperor", "Kaitain");
        homeworlds.put("Fremen", "Southern Hemisphere");
        homeworlds.put("Guild", "Junction");
        homeworlds.put("BG", "Wallach IX");
        homeworlds.put("Ix", "Ix");
        homeworlds.put("BT", "Tleilax");
        homeworlds.put("CHOAM", "Tupile");
        homeworlds.put("Richese", "Richese");
        homeworlds.put("Ecaz", "Ecaz");
        homeworlds.put("Moritani", "Grumman");

        for (Faction faction : game.getFactions()) {
            int reserves = faction.getReserves().getStrength();
            int specialReserves = faction.getSpecialReserves().getStrength();
            String homeworldName = homeworlds.get(faction.getName());
            faction.setHomeworld(homeworldName);
            Territory homeworld = new Territory(homeworldName, -1, false, false, false);
            game.getTerritories().put(homeworldName, homeworld);
            game.getHomeworlds().put(faction.getName(), homeworldName);
            game.getTerritory(homeworldName).getForces().add(new Force(faction.getName(), reserves));
            if (faction.getName().matches("Fremen|Ix"))
                game.getTerritory(homeworldName).getForces().add(new Force(faction.getName() + "*", specialReserves));
        }
        if (game.hasFaction("Emperor")) {
            game.getHomeworlds().put("Emperor*", "Salusa Secundus");
            ((EmperorFaction) game.getFaction("Emperor")).setSecondHomeworld("Salusa Secundus");
            game.getTerritories().put("Salusa Secundus", new Territory("Salusa Secundus", -1, false, false, false));
            game.getTerritory("Salusa Secundus").getForces().add(game.getFaction("Emperor").getSpecialReserves());
        }
    }

    /**
     * Loads the game state from the bot data channel, or returns an already loaded game state.
     *
     * @return Game object representing the game state.
     */
    public Game getGame(Message message) {
        String gameJson = getGameJson(message);
        return gameJsonToGame(gameJson);
    }

    public String getGameJson(Message message) {
        Message.Attachment encoded = message.getAttachments().get(0);
        CompletableFuture<InputStream> future = encoded.getProxy().download();

        try {
            String gameJson = new String(future.get().readAllBytes(), StandardCharsets.UTF_8);
            future.get().close();
            return gameJson;
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Didn't work...");
            return "";
        }
    }

    public Game gameJsonToGame(String gameJson) {
        Gson gson = createGsonDeserializer();
        Game returnGame = gson.fromJson(gameJson, Game.class);
        addGameReferenceToFactions(returnGame);
        return returnGame;
    }

    public void pushGame(Game game) throws ChannelNotFoundException {
        this.game = game;
        pushGame();
    }

    public void pushGame() throws ChannelNotFoundException {

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

        String gameJson = gson.toJson(this.game);
        GameCache.setGameJson(this.gameCategory.getName(), gameJson);

        FileUpload fileUpload = FileUpload.fromData(
                gameJson.getBytes(StandardCharsets.UTF_8), "gamestate.json"
        );

        if (getEvent() instanceof SlashCommandInteractionEvent) {
            SlashCommandInteractionEvent slashCommandInteractionEvent = (SlashCommandInteractionEvent) event;
            String message = getEvent() == null ? "Manual update" : "Command: `" + slashCommandInteractionEvent.getCommandString() + "`";
            queueMessage("bot-data", message, fileUpload);
        } else {
            ButtonInteractionEvent buttonInteractionEvent = (ButtonInteractionEvent) event;
            String message = getEvent() == null ? "Manual update" : "Button Pressed: `" + buttonInteractionEvent.getComponentId() + " pressed by " + buttonInteractionEvent.getMember().getUser().getName() + "`";
            queueMessage("bot-data", message, fileUpload);
        }
    }

    public MessageCreateAction prepareMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        return channel.sendMessage(message);
    }

    @SuppressWarnings("rawtypes")
    public List<RestAction> getMessageQueue() {
        return messageQueue;
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param message     Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        messageQueue.add(channel.sendMessage(message));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param message     Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, MessageCreateData message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        messageQueue.add(channel.sendMessage(message));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param message     Message to send.
     * @param fileUpload  File to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, String message, FileUpload fileUpload) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        messageQueue.add(channel.sendMessage(message).addFiles(fileUpload));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param message     Message to send.
     * @param fileUploads Files to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, String message, List<FileUpload> fileUploads) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        messageQueue.add(channel.sendMessage(message).addFiles(fileUploads));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param fileUpload  File to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, FileUpload fileUpload) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        MessageCreateBuilder messageCreateBuilder =
                (new MessageCreateBuilder()).addFiles(fileUpload);
        messageQueue.add(channel.sendMessage(messageCreateBuilder.build()));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName          Channel name to send the message to.
     * @param messageCreateBuilder Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, MessageCreateBuilder messageCreateBuilder) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        messageQueue.add(channel.sendMessage(messageCreateBuilder.build()));
    }

    /**
     * Queues a message to be sent.
     *
     * @param messageCreateAction Message to send.
     */
    public void queueMessage(WebhookMessageCreateAction<Message> messageCreateAction) {
        messageQueue.add(messageCreateAction);
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param messageEditRequest Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(MessageEditRequest messageEditRequest) throws ChannelNotFoundException {
        messageQueue.add((RestAction) messageEditRequest);
    }

    /**
     * Queues a message to be sent.
     *
     * @param messageCreateAction Message to send.
     */
    public void queueMessage(MessageCreateAction messageCreateAction) {
        messageQueue.add(messageCreateAction);
    }

    /**
     * Queues a message to be sent to the event channel.
     *
     * @param message Message to send.
     */
    public void queueMessage(String message) {
        messageQueue.add(getHook().sendMessage(message));
    }

    /**
     * Queues a message to be sent to the event channel.
     *
     * @param message Message to send.
     */
    public void queueMessage(MessageCreateBuilder message) {
        messageQueue.add(getHook().sendMessage(message.build()));
    }

    public void queueMessage(String parentChannel, String threadChannel, String message) throws ChannelNotFoundException {
        TextChannel parent = getTextChannel(parentChannel);
        ThreadChannel thread = parent.getThreadChannels().stream().filter(channel -> channel.getName().equals(threadChannel)).findFirst().get();
        messageQueue.add(thread.sendMessage(message));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName          Name of the parent channel which has the thread
     * @param threadName           Name of the thread to send the message to
     * @param messageCreateBuilder Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, String threadName, MessageCreateBuilder messageCreateBuilder) throws ChannelNotFoundException {
        TextChannel parent = getTextChannel(channelName);
        ThreadChannel thread = parent.getThreadChannels().stream().filter(channel -> channel.getName().equals(threadName)).findFirst().get();
        messageQueue.add(thread.sendMessage(messageCreateBuilder.build()));
    }

    /**
     * Queues a message to be sent to the ephemeral channel.
     *
     * @param message Message to send.
     */
    public void queueMessageToEphemeral(String message) {
        messageQueue.add(getHook().sendMessage(message).setEphemeral(true));
    }

    /**
     * Queue deletion of the message that triggered the event.
     */
    public void queueDeleteMessage() {
        if (event instanceof ButtonInteractionEvent) {
            messageQueue.add(((ButtonInteractionEvent) event).getMessage().delete());
        } else {
            throw new IllegalArgumentException("Unknown event type");
        }
    }

    /**
     * Queue deletion of the given message.
     *
     * @param message Message to delete.
     */
    public void queueDeleteMessage(Message message) {
        messageQueue.add(message.delete());
    }

    /**
     * Get hook from the current event
     *
     * @return InteractionHook from the current event
     */
    private InteractionHook getHook() {
        if (event instanceof SlashCommandInteractionEvent)
            return ((SlashCommandInteractionEvent) event).getHook();
        else if (event instanceof ButtonInteractionEvent)
            return ((ButtonInteractionEvent) event).getHook();
        else
            throw new IllegalArgumentException("Unknown event type");
    }

    /**
     * Sends all messages in the message queue.
     */
    @SuppressWarnings("rawtypes")
    public void sendAllMessages() {
        if (this.game.getMute()) return;
        for (RestAction restAction : messageQueue) {
            try {
                restAction.complete();
            } catch (Exception e) {
                restAction.submit();
            }
        }
        messageQueue.clear();
    }

    /**
     * Creates a thread in the parent channel with the given name and adds the given users to it.
     *
     * @param parentChannel The parent channel
     * @param threadName    The name of the thread to create
     * @param userIds       The ids of the users to add to the thread.  All non-numeric characters will be removed.
     */
    public void createThread(TextChannel parentChannel, String threadName, List<String> userIds) {
        ThreadChannel threadChannel = parentChannel.createThreadChannel(threadName, true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .complete();
        addUsersToThread(threadChannel, userIds);
    }

    /**
     * Adds the given users to the given thread.
     *
     * @param threadChannel The thread to add the users to
     * @param userIds       The ids of the users to add to the thread.  All non-numeric characters will be removed.
     */
    public void addUsersToThread(ThreadChannel threadChannel, List<String> userIds) {
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
