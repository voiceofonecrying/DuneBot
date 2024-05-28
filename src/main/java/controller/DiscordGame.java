package controller;

import caches.EmojiCache;
import caches.GameCache;
import caches.LeaderSkillCardsCache;
import com.google.gson.*;
import controller.channels.*;
import exceptions.ChannelNotFoundException;
import helpers.DiscordRequest;
import helpers.Exclude;
import io.gsonfire.GsonFireBuilder;
import model.*;
import model.factions.Faction;
import model.factions.FactionTypeSelector;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordGame {
    private final Category gameCategory;
    private final List<DiscordRequest> discordRequests = new ArrayList<>();
    private List<TextChannel> textChannelList;
    private Game game;
    private GenericInteractionCreateEvent event;
    private final Map<String, RichCustomEmoji> emojis;
    private static final Pattern taggedEmojis = Pattern.compile("<(:[a-zA-Z0-9_]+:)\\d+>");
    private static final Pattern untaggedEmojis = Pattern.compile("(?<!<):([a-zA-Z0-9_]+):(?!\\d+>)");

    public DiscordGame(@NotNull GenericInteractionCreateEvent event) throws ChannelNotFoundException {
        this.gameCategory = categoryFromEvent(event);
        this.game = this.getGame();
        this.event = event;
        emojis = EmojiCache.getEmojis(Objects.requireNonNull(event.getGuild()).getId());
    }

    public DiscordGame(@NotNull MessageReceivedEvent event) throws ChannelNotFoundException {
        this.gameCategory = categoryFromEvent(event);
        this.game = this.getGame();
        emojis = EmojiCache.getEmojis(Objects.requireNonNull(event.getGuild()).getId());
    }

    public DiscordGame(@NotNull CommandAutoCompleteInteractionEvent event) {
        this.gameCategory = categoryFromEvent(event);
        emojis = EmojiCache.getEmojis(Objects.requireNonNull(event.getGuild()).getId());
    }

    public DiscordGame(Category category) {
        this.gameCategory = category;
        emojis = EmojiCache.getEmojis(Objects.requireNonNull(category.getGuild()).getId());
    }

    public DiscordGame(Category category, boolean isNew) throws ChannelNotFoundException {
        this.gameCategory = category;
        if (!isNew) this.game = getGame();
        emojis = EmojiCache.getEmojis(Objects.requireNonNull(category.getGuild()).getId());
    }

    public static Category categoryFromEvent(@NotNull GenericInteractionCreateEvent event) {
        Channel channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel != null ? textChannel.getParentCategory() : null;
    }

    public static Category categoryFromEvent(@NotNull MessageReceivedEvent event) {
        MessageChannelUnion channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel != null ? textChannel.getParentCategory() : null;
    }

    public static Category categoryFromEvent(@NotNull CommandAutoCompleteInteractionEvent event) {
        MessageChannelUnion channel = Objects.requireNonNull(event.getChannel());
        TextChannel textChannel = null;
        if (channel instanceof TextChannel) {
            textChannel = (TextChannel) channel;
        } else if (channel instanceof ThreadChannel) {
            textChannel = ((ThreadChannel) channel).getParentChannel().asTextChannel();
        }
        return textChannel != null ? textChannel.getParentCategory() : null;
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
                .registerTypeSelector(Faction.class, new FactionTypeSelector())
                .registerPreProcessor(TreacheryCard.class, (clazz, src, gson) -> {
                    JsonObject jsonObject = src.getAsJsonObject();
                    jsonObject.addProperty("name", jsonObject.get("name").getAsString().trim());
                })
                .registerPreProcessor(LeaderSkillCard.class, (clazz, src, gson) -> {
                    JsonObject jsonObject = src.getAsJsonObject();
                    jsonObject.addProperty("name", LeaderSkillCardsCache.getCardInfo(jsonObject.get("name").getAsString()).get("Name"));
                })
                .registerPreProcessor(Game.class, (clazz, src, gson) -> {
                    JsonObject jsonObject = src.getAsJsonObject();
                    JsonArray jsonArray = jsonObject.get("gameOptions").getAsJsonArray();
                    jsonArray.remove(new JsonPrimitive("MAP_IN_FRONT_OF_SHIELD"));
                    jsonObject.add("gameOptions", jsonArray);
                })
                ;
        GsonBuilder gsonBuilder = builder.createGsonBuilder();

        return gsonBuilder.create();
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

    /**
     * Gets the text channel with the given name.
     *
     * @param name Name of the text channel to get.
     * @return Text channel with the given name.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public TextChannel getTextChannel(String name) throws ChannelNotFoundException {
        for (TextChannel channel : this.getTextChannels()) {
            if (channel.getName().equals(name)) {
                return channel;
            }
        }
        throw new ChannelNotFoundException("Channel not found : " + name);
    }

    /**
     * Gets a thread with the given name.
     *
     * @param channelName Name of the text channel which contains the thread.
     * @param threadName  Name of the thread to get.
     * @return Text channel with the given name.
     * @throws ChannelNotFoundException Thrown if the channel or thread is not found.
     */
    public ThreadChannel getThreadChannel(String channelName, String threadName) throws ChannelNotFoundException {
        Optional<ThreadChannel> threadChannel = getOptionalThreadChannel(channelName, threadName);
        if (threadChannel.isPresent()) return threadChannel.get();

        throw new ChannelNotFoundException("Thread not found: " + threadName + " in channel " + channelName);
    }

    /**
     * Gets an optional thread with the given name.
     *
     * @param channelName Name of the text channel which contains the thread.
     * @param threadName  Name of the thread to get.
     * @return Optional Thread channel with the given name.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public Optional<ThreadChannel> getOptionalThreadChannel(String channelName, String threadName) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(channelName);
        Optional<ThreadChannel> thread = channel.getThreadChannels().stream().filter(c -> c.getName().equals(threadName)).findFirst();
        if (thread.isPresent()) return thread;

        thread = channel
                .retrieveArchivedPublicThreadChannels().complete().stream()
                .filter(c -> c.getName().equals(threadName))
                .findFirst();

        if (thread.isPresent()) return thread;

        thread = channel
                .retrieveArchivedPrivateThreadChannels().complete().stream()
                .filter(c -> c.getName().equals(threadName))
                .findFirst();

        return thread;
    }

    public TextChannel getBotDataChannel() throws ChannelNotFoundException {
        return this.getTextChannel("bot-data");
    }

    public TurnSummary getTurnSummary() throws ChannelNotFoundException {
        return getTurnSummary(this.game);
    }

    public TurnSummary getTurnSummary(Game game) throws ChannelNotFoundException {
        return new TurnSummary(this, game);
    }

    public Whispers getWhispers() throws ChannelNotFoundException {
        return getWhispers(this.game);
    }

    public Whispers getWhispers(Game game) throws ChannelNotFoundException {
        return new Whispers(this, game);
    }

    public GameActions getGameActions() throws ChannelNotFoundException {
        return new GameActions(this);
    }

    public ModInfo getModInfo() throws ChannelNotFoundException {
        return new ModInfo(this);
    }

    public FactionChat getFactionChat(Faction faction) throws ChannelNotFoundException {
        return getFactionChat(faction.getName());
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

    public FactionLedger getFactionLedger(Faction faction) throws ChannelNotFoundException {
        return getFactionLedger(faction.getName());
    }

    public FactionLedger getFactionLedger(String factionName) throws ChannelNotFoundException {
        return new FactionLedger(this, factionName);
    }

    public FactionLedger getAtreidesLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Atreides");
    }

    public FactionLedger getBGLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "BG");
    }

    public FactionLedger getBTLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "BT");
    }

    public FactionLedger getCHOAMLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "CHOAM");
    }

    public FactionLedger getEcazLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Ecaz");
    }

    public FactionLedger getEmperorLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Emperor");
    }

    public FactionLedger getFremenLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Fremen");
    }

    public FactionLedger getGuildLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Guild");
    }

    public FactionLedger getHarkonnenLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Harkonnen");
    }

    public FactionLedger getIxLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Ix");
    }

    public FactionLedger getMoritaniLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Moritani");
    }

    public FactionLedger getRicheseLedger() throws ChannelNotFoundException {
        return new FactionLedger(this, "Richese");
    }

    public FactionWhispers getFactionWhispers(String factionName, String interlocutorName) throws ChannelNotFoundException {
        return new FactionWhispers(this, this.game, factionName, interlocutorName);
    }

    public FactionWhispers getFactionWhispers(Faction faction, Faction interlocutor) throws ChannelNotFoundException {
        return new FactionWhispers(this, this.game, faction.getName(), interlocutor.getName());
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
            String gameJson = getGameJson(ml.getFirst());
            Game game = gameJsonToGame(gameJson);
            GameCache.setGameJson(gameName, gameJson);
            this.game = game;
        }
        return this.game;
    }

    public void setGame(Game game) {
        this.game = game;
    }


    /**
     * Loads the game state from the bot data channel, or returns an already loaded game state.
     *
     * @return Game object representing the game state.
     */
    public Game getGame(Message message) throws ChannelNotFoundException {
        String gameJson = getGameJson(message);
        return gameJsonToGame(gameJson);
    }

    public String getGameJson(Message message) {
        Message.Attachment encoded = message.getAttachments().getFirst();
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

    public Game gameJsonToGame(String gameJson) throws ChannelNotFoundException {
        Gson gson = createGsonDeserializer();
        Game game = gson.fromJson(gameJson, Game.class);
        addGameReferenceToFactions(game);

        for (Territory territory : game.getTerritories().values()) {
            if (territory.hasForce("Hidden Mobile Stronghold")) {
                Territory hms = game.getTerritory("Hidden Mobile Stronghold");
                game.putTerritoryInAnotherTerritory(hms, territory);
            }
            if (territory.isDiscovered()) {
                Territory revealedTerritory = game.getTerritory(territory.getDiscoveryToken());
                game.putTerritoryInAnotherTerritory(revealedTerritory, territory);
            }
        }
        // Temporary migration to properly mark Homeworlds and Discovery Token Territory objects
        game.getHomeworlds().values().forEach(homeworld -> game.getTerritories().get(homeworld).setHomeworld(true));
        Territory t = game.getTerritories().get("Jacurutu Sietch");
        if (t != null) t.setDiscoveryToken(true);
        t = game.getTerritories().get("Cistern");
        if (t != null) t.setDiscoveryToken(true);
        t = game.getTerritories().get("Ecological Testing Station");
        if (t != null) t.setDiscoveryToken(true);
        t = game.getTerritories().get("Shrine");
        if (t != null) t.setDiscoveryToken(true);
        t = game.getTerritories().get("Orgiz Processing Station");
        if (t != null) {
            t.setDiscoveryToken(true);
            t.setStronghold(false);
        }

        for (Faction f : game.getFactions()) {
            f.setLedger(getFactionLedger(f));
            f.setChat(getFactionChat(f));
        }

        game.setTurnSummary(getTurnSummary(game));
        game.setWhispers(getWhispers(game));
        game.setGameActions(getGameActions());
        game.setModInfo(getModInfo());
        return game;
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
            String message = getEvent() == null ? "Manual update" : "Button Pressed: `" + buttonInteractionEvent.getComponentId() + " pressed by " + Objects.requireNonNull(buttonInteractionEvent.getMember()).getUser().getName() + "`";
            queueMessage("bot-data", message, fileUpload);
        }
    }

    public MessageCreateAction prepareMessage(String name, String message) throws ChannelNotFoundException {
        TextChannel channel = getTextChannel(name);
        return channel.sendMessage(message);
    }

    private static List<MessageCreateBuilder> splitMessageBuilders(MessageCreateBuilder inMessageCreateBuilder) {
        List<MessageCreateBuilder> messageCreateBuilders = new ArrayList<>();
        messageCreateBuilders.add(new MessageCreateBuilder());
        messageCreateBuilders.getFirst().setContent(inMessageCreateBuilder.getContent());

        int totalEmbedCount = 0;
        int totalEmbedLength = 0;
        for (MessageEmbed messageEmbed : inMessageCreateBuilder.getEmbeds()) {
            totalEmbedCount++;
            totalEmbedLength += messageEmbed.getLength();

            if (totalEmbedLength > MessageEmbed.EMBED_MAX_LENGTH_BOT || totalEmbedCount > 10) {
                messageCreateBuilders.add(new MessageCreateBuilder());
                totalEmbedCount = 1;
                totalEmbedLength = messageEmbed.getLength();
            }

            messageCreateBuilders.getLast().addEmbeds(messageEmbed);
        }

        messageCreateBuilders.getLast().setFiles(inMessageCreateBuilder.getAttachments());
        return messageCreateBuilders;
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
        discordRequests.add(new DiscordRequest(channel.sendMessage(tagEmojis(message))));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName Channel name to send the message to.
     * @param message     Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, MessageCreateData message) throws ChannelNotFoundException {
        MessageCreateData updatedMessage = MessageCreateBuilder.from(message)
                .setContent(tagEmojis(message.getContent()))
                .build();
        TextChannel channel = getTextChannel(channelName);
        discordRequests.add(new DiscordRequest(channel.sendMessage(updatedMessage)));
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
        discordRequests.add(new DiscordRequest(channel.sendMessage(tagEmojis(message)).addFiles(fileUpload)));
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
        discordRequests.add(new DiscordRequest(channel.sendMessage(tagEmojis(message)).addFiles(fileUploads)));
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
        discordRequests.add(new DiscordRequest(channel.sendMessage(messageCreateBuilder.build())));
    }

    /**
     * Queues a message to be sent to the given channel.
     *
     * @param channelName          Channel name to send the message to.
     * @param messageCreateBuilder Message to send.
     * @throws ChannelNotFoundException Thrown if the channel is not found.
     */
    public void queueMessage(String channelName, MessageCreateBuilder messageCreateBuilder) throws ChannelNotFoundException {
        List<MessageCreateBuilder> messageCreateBuilders = DiscordGame.splitMessageBuilders(messageCreateBuilder);
        TextChannel channel = getTextChannel(channelName);

        for ( MessageCreateBuilder m : messageCreateBuilders ) {
            m.setContent(tagEmojis(m.getContent()));
            discordRequests.add(new DiscordRequest(channel.sendMessage(m.build())));
        }
    }

    /**
     * Queues a message to be sent.
     *
     * @param messageCreateAction Message to send.
     */
    public void queueMessage(WebhookMessageCreateAction<Message> messageCreateAction) {
        messageCreateAction.setContent(tagEmojis(messageCreateAction.getContent()));
        discordRequests.add(new DiscordRequest(messageCreateAction));
    }

    /**
     * Queues a message to be sent.
     *
     * @param messageCreateAction Message to send.
     */
    public void queueMessage(MessageCreateAction messageCreateAction) {
        messageCreateAction.setContent(tagEmojis(messageCreateAction.getContent()));
        discordRequests.add(new DiscordRequest(messageCreateAction));
    }

    /**
     * Queues a message to be sent to the event channel.
     *
     * @param message Message to send.
     */
    public void queueMessage(String message) {
        discordRequests.add(new DiscordRequest(getHook().sendMessage(tagEmojis(message))));
    }

    /**
     * Queues a message to be sent to the event channel.
     *
     * @param message Message to send.
     */
    public void queueMessage(MessageCreateBuilder message) {
        message.setContent(tagEmojis(message.getContent()));
        discordRequests.add(new DiscordRequest(getHook().sendMessage(message.build())));
    }

    public void queueMessage(String parentChannel, String threadChannel, String message) throws ChannelNotFoundException {
        TextChannel parent = getTextChannel(parentChannel);
        ThreadChannel thread = parent.getThreadChannels().stream()
                .filter(channel -> channel.getName().equals(threadChannel))
                .findFirst().orElseThrow(() -> new ChannelNotFoundException("Thread not found"));
        discordRequests.add(new DiscordRequest(thread.sendMessage(tagEmojis(message))));
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
        messageCreateBuilder.setContent(tagEmojis(messageCreateBuilder.getContent()));
        TextChannel parent = getTextChannel(channelName);
        ThreadChannel thread = parent.getThreadChannels().stream()
                .filter(channel -> channel.getName().equals(threadName))
                .findFirst().orElseThrow(() -> new ChannelNotFoundException("Thread not found"));
        discordRequests.add(new DiscordRequest(thread.sendMessage(messageCreateBuilder.build())));
    }

    /**
     * Queues a message to be sent to the ephemeral channel.
     *
     * @param message Message to send.
     */
    public void queueMessageToEphemeral(String message) {
        discordRequests.add(new DiscordRequest(getHook().sendMessage(tagEmojis(message)).setEphemeral(true)));
    }

    /**
     * Queue deletion of the message that triggered the event.
     */
    public void queueDeleteMessage() {
        if (event instanceof ButtonInteractionEvent) {
            discordRequests.add(new DiscordRequest(((ButtonInteractionEvent) event).getMessage().delete()));
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
        discordRequests.add(new DiscordRequest(message.delete()));
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
    public void sendAllMessages() {
        if (this.game.getMute()) return;
        for (DiscordRequest discordRequest : discordRequests) {
            try {
                discordRequest.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        discordRequests.clear();
    }

    /**
     * Remove Guild-specific information from emojis
     * @param message String to remove tags from
     * @return String with tags removed
     */
    public String untagEmojis(String message) {
        Matcher matcher = taggedEmojis.matcher(message);

        StringBuilder stringBuilder = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, matcher.group(1));
        }

        matcher.appendTail(stringBuilder);

        return stringBuilder.toString();
    }

    /**
     * Tag emojis with guild-specific information
     * @param message String to tag emojis in
     * @return String with tags added
     */
    public String tagEmojis(String message) {
        String untaggedMessage = untagEmojis(message);
        Matcher matcher = untaggedEmojis.matcher(untaggedMessage);

        StringBuilder stringBuilder = new StringBuilder();


        while (matcher.find()) {
            matcher.appendReplacement(stringBuilder, getEmojiTag(matcher.group(1)));
        }

        matcher.appendTail(stringBuilder);

        return stringBuilder.toString();
    }

    /**
     * Creates a private thread in the parent channel with the given name and adds the given users to it.
     *
     * @param parentChannel The parent channel
     * @param threadName    The name of the thread to create
     * @param userIds       The ids of the users to add to the thread.  All non-numeric characters will be removed.
     */
    public void createPrivateThread(TextChannel parentChannel, String threadName, List<String> userIds) {
        ThreadChannel threadChannel = parentChannel.createThreadChannel(threadName, true)
                .setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .complete();
        addUsersToThread(threadChannel, userIds);
    }

    /**
     * Creates a public thread in the parent channel with the given name and adds the given users to it.
     *
     * @param parentChannel The parent channel
     * @param threadName    The name of the thread to create
     * @param userIds       The ids of the users to add to the thread.  All non-numeric characters will be removed.
     */
    public void createPublicThread(TextChannel parentChannel, String threadName, List<String> userIds) {
        ThreadChannel threadChannel = parentChannel.createThreadChannel(threadName, false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .complete();

        try {
            parentChannel.deleteMessageById(parentChannel.getLatestMessageId()).complete();
        } catch (Exception ignore) {}
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

    public Faction getFactionByPlayer(String user) throws ChannelNotFoundException {
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

    public static OptionMapping required(OptionData optionData, SlashCommandInteractionEvent event) {
        OptionMapping optionMapping = optional(optionData, event);

        if (optionMapping == null) {
            throw new IllegalArgumentException("Required option is missing: " + optionData.getName());
        }

        return optionMapping;
    }

    public OptionMapping required(OptionData optionData) {
        return required(optionData, (SlashCommandInteractionEvent) event);
    }

    public static OptionMapping optional(OptionData optionData, SlashCommandInteractionEvent event) {
        String optionName = optionData.getName();
        return event.getOption(optionName);
    }

    public OptionMapping optional(OptionData optionData) {
        return optional(optionData, (SlashCommandInteractionEvent) event);
    }

    public RichCustomEmoji getEmoji(String emojiName) {
        return emojis.get(emojiName.replace(":", ""));
    }

    public String getEmojiTag(String emojiName) {
        RichCustomEmoji emoji = getEmoji(emojiName);
        return emoji == null ? ":" + emojiName.replace(":", "") + ":" : emoji.getFormatted();
    }
}
