package testutil.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import testutil.discord.state.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Factory for creating Mockito mocks backed by {@link MockDiscordServer} state.
 *
 * <p>This factory bridges the gap between JDA Discord entities (Guild, TextChannel, etc.)
 * and the stateful mock infrastructure. When you call methods on the returned mocks,
 * they read from and write to the underlying state objects.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Stateful Mocks</b> - Calling {@code guild.getTextChannels()} returns channels from state</li>
 *   <li><b>Persistent Changes</b> - {@code channel.sendMessage()} stores messages in state</li>
 *   <li><b>Verifiable Actions</b> - Tests can verify state changes after actions</li>
 * </ul>
 *
 * <p><b>Architecture:</b>
 * <pre>
 * MockDiscordServer (state storage)
 *   └─ MockGuildState
 *       ├─ MockChannelState (stores messages)
 *       ├─ MockUserState
 *       └─ MockMemberState
 *
 * StatefulMockFactory (creates JDA mocks)
 *   ├─ mockGuild() → Guild (reads channels from state)
 *   ├─ mockTextChannel() → TextChannel (sendMessage writes to state!)
 *   ├─ mockUser() → User
 *   └─ mockMember() → Member (getRoles reads from state)
 * </pre>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create state
 * MockDiscordServer server = MockDiscordServer.create();
 * MockGuildState guildState = server.createGuild(12345L, "Test Guild");
 * MockCategoryState categoryState = guildState.createCategory("game-channels");
 * MockChannelState channelState = guildState.createTextChannel("game-actions", categoryState.getCategoryId());
 *
 * // Create stateful mocks
 * Guild guild = StatefulMockFactory.mockGuild(guildState);
 * TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
 *
 * // Use the mocks - they interact with state!
 * channel.sendMessage("Test message").queue();
 *
 * // Verify state was updated
 * List<MockMessageState> messages = channelState.getMessages();
 * assertThat(messages).hasSize(1);
 * assertThat(messages.get(0).getContent()).isEqualTo("Test message");
 *
 * // Guild methods also read from state
 * List<TextChannel> channels = guild.getTextChannels();
 * assertThat(channels).hasSize(1);
 * assertThat(channels.get(0).getName()).isEqualTo("game-actions");
 * }</pre>
 *
 * <p><b>Important Notes:</b>
 * <ul>
 *   <li>All mocks use lenient stubbing to avoid UnnecessaryStubbingException</li>
 *   <li>Circular references are handled (e.g., guild → channel → guild)</li>
 *   <li>State changes are immediate and synchronous (no async behavior)</li>
 * </ul>
 *
 * @see MockDiscordServer
 * @see MockGuildState
 * @see MockChannelState
 */
public class StatefulMockFactory {

    // ========== Helper Methods for Type-Safe Mock Creation ==========

    /**
     * Creates a mock RestAction with proper generic types.
     * <p>
     * This helper method centralizes the unchecked warning suppression required
     * due to Java's type erasure when creating mocks of generic types.
     * See: https://github.com/mockito/mockito/issues/1531
     *
     * @param <T> The return type of the RestAction
     * @return A properly typed mock RestAction
     */
    @SuppressWarnings("unchecked") // Required due to Mockito's generic type handling limitations
    private static <T> RestAction<T> createMockRestAction() {
        return mock(RestAction.class);
    }

    /**
     * Creates a mock CacheRestAction with proper generic types.
     */
    @SuppressWarnings("unchecked") // Required due to Mockito's generic type handling limitations
    private static <T> CacheRestAction<T> createMockCacheRestAction() {
        return mock(CacheRestAction.class);
    }

    /**
     * Creates a mock AuditableRestAction with proper generic types.
     */
    @SuppressWarnings("unchecked") // Required due to Mockito's generic type handling limitations
    private static <T> AuditableRestAction<T> createMockAuditableRestAction() {
        return mock(AuditableRestAction.class);
    }

    /**
     * Safely adds FileUpload items from a collection to a list.
     * <p>
     * This method performs runtime type checking to ensure type safety
     * when handling collections with unknown generic types.
     *
     * @param collection The collection to extract FileUploads from
     * @param targetList The list to add FileUploads to
     */
    private static void addFileUploadsFromCollection(Collection<?> collection, List<FileUpload> targetList) {
        for (Object item : collection) {
            if (item instanceof FileUpload) {
                targetList.add((FileUpload) item);
            }
            // Silently ignore non-FileUpload items for safety
        }
    }

    /**
     * Creates a Guild mock backed by state.
     *
     * <p>The returned mock reads all guild data (channels, categories, roles, members)
     * from the provided {@link MockGuildState}. Methods like {@code getTextChannels()}
     * return mocks created from the current state, so state changes are immediately
     * visible when you call guild methods again.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns guild ID from state</li>
     *   <li>{@code getId()} - Returns guild ID as string</li>
     *   <li>{@code getName()} - Returns guild name from state</li>
     *   <li>{@code getTextChannels()} - Creates mocks for all channels in state</li>
     *   <li>{@code getCategories()} - Creates mocks for all categories in state</li>
     *   <li>{@code getRoles()} - Creates mocks for all roles in state</li>
     *   <li>{@code getMembers()} - Creates mocks for all members in state</li>
     * </ul>
     *
     * <p><b>Note:</b> This method may be called multiple times for the same guild
     * (e.g., when mocking circular references like channel → guild). Each call
     * creates a new mock instance, but all read from the same state.
     *
     * @param guildState The guild state containing all guild data
     * @return A Mockito mock of {@link Guild} backed by the provided state
     */
    public static Guild mockGuild(MockGuildState guildState) {
        Guild guild = mock(Guild.class);

        lenient().when(guild.getIdLong()).thenReturn(guildState.getGuildId());
        lenient().when(guild.getId()).thenReturn(String.valueOf(guildState.getGuildId()));
        lenient().when(guild.getName()).thenReturn(guildState.getGuildName());

        // getTextChannels() reads from state
        lenient().when(guild.getTextChannels()).thenAnswer(inv -> {
            return guildState.getChannels().stream()
                .map(channelState -> mockTextChannel(channelState, guildState))
                .collect(Collectors.toList());
        });

        // getCategories() reads from state
        lenient().when(guild.getCategories()).thenAnswer(inv -> {
            return guildState.getCategories().stream()
                .map(categoryState -> mockCategory(categoryState, guildState))
                .collect(Collectors.toList());
        });

        // getRoles() reads from state
        lenient().when(guild.getRoles()).thenAnswer(inv -> {
            return guildState.getRoles().stream()
                .map(StatefulMockFactory::mockRole)
                .collect(Collectors.toList());
        });

        // getMembers() reads from state
        lenient().when(guild.getMembers()).thenAnswer(inv -> {
            return guildState.getMembers().stream()
                .map(memberState -> mockMember(memberState, guildState))
                .collect(Collectors.toList());
        });

        // createCategory() creates a category in state
        lenient().when(guild.createCategory(anyString())).thenAnswer(inv -> {
            String categoryName = inv.getArgument(0);
            MockCategoryState categoryState = guildState.createCategory(categoryName);

            // Use array to work around lambda capture restriction
            final ChannelAction[] actionHolder = new ChannelAction[1];

            @SuppressWarnings("unchecked")
            ChannelAction<Category> action = (ChannelAction<Category>) mock(ChannelAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // Permission override methods return the action itself for chaining
                if (methodName.equals("addPermissionOverride") ||
                    methodName.equals("addMemberPermissionOverride") ||
                    methodName.equals("addRolePermissionOverride")) {
                    return actionHolder[0];
                }

                // complete() returns the created category
                if (methodName.equals("complete")) {
                    return mockCategory(categoryState, guildState);
                }

                // Default: return null for other methods
                return null;
            });

            actionHolder[0] = action;
            return action;
        });

        // getCategoryById() looks up category from state
        lenient().when(guild.getCategoryById(anyLong())).thenAnswer(inv -> {
            long categoryId = inv.getArgument(0);
            MockCategoryState categoryState = guildState.getCategory(categoryId);
            return categoryState != null ? mockCategory(categoryState, guildState) : null;
        });

        // getCategoriesByName() searches for categories by name
        lenient().when(guild.getCategoriesByName(anyString(), anyBoolean())).thenAnswer(inv -> {
            String categoryName = inv.getArgument(0);
            boolean ignoreCase = inv.getArgument(1);

            return guildState.getCategories().stream()
                    .filter(categoryState -> {
                        if (ignoreCase) {
                            return categoryState.getCategoryName().equalsIgnoreCase(categoryName);
                        } else {
                            return categoryState.getCategoryName().equals(categoryName);
                        }
                    })
                    .map(categoryState -> mockCategory(categoryState, guildState))
                    .collect(Collectors.toList());
        });

        // getTextChannelsByName() searches for text channels by name
        lenient().when(guild.getTextChannelsByName(anyString(), anyBoolean())).thenAnswer(inv -> {
            String channelName = inv.getArgument(0);
            boolean ignoreCase = inv.getArgument(1);

            return guildState.getChannels().stream()
                    .filter(channelState -> {
                        if (ignoreCase) {
                            return channelState.getChannelName().equalsIgnoreCase(channelName);
                        } else {
                            return channelState.getChannelName().equals(channelName);
                        }
                    })
                    .map(channelState -> mockTextChannel(channelState, guildState))
                    .collect(Collectors.toList());
        });

        // getPublicRole() returns a mock @everyone role
        lenient().when(guild.getPublicRole()).thenAnswer(inv -> {
            Role publicRole = mock(Role.class);
            lenient().when(publicRole.getIdLong()).thenReturn(guildState.getGuildId());
            lenient().when(publicRole.getId()).thenReturn(String.valueOf(guildState.getGuildId()));
            lenient().when(publicRole.getName()).thenReturn("@everyone");
            return publicRole;
        });

        // getRolesByName() searches for roles by name
        lenient().when(guild.getRolesByName(anyString(), anyBoolean())).thenAnswer(inv -> {
            String roleName = inv.getArgument(0);
            boolean ignoreCase = inv.getArgument(1);

            return guildState.getRoles().stream()
                    .filter(roleState -> {
                        if (ignoreCase) {
                            return roleState.getRoleName().equalsIgnoreCase(roleName);
                        } else {
                            return roleState.getRoleName().equals(roleName);
                        }
                    })
                    .map(StatefulMockFactory::mockRole)
                    .collect(Collectors.toList());
        });

        // getMemberById() looks up member from state
        lenient().when(guild.getMemberById(anyString())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            long userIdLong = Long.parseLong(userId);
            MockMemberState memberState = guildState.getMember(userIdLong);
            return memberState != null ? mockMember(memberState, guildState) : null;
        });

        // addRoleToMember() adds the role to the member state and returns a mock AuditableRestAction
        lenient().when(guild.addRoleToMember(any(Member.class), any(Role.class))).thenAnswer(inv -> {
            Member member = inv.getArgument(0);
            Role role = inv.getArgument(1);

            // Find the member state and add the role
            long userId = member.getIdLong();
            MockMemberState memberState = guildState.getMember(userId);
            if (memberState != null) {
                memberState.addRole(role.getIdLong());
            }

            AuditableRestAction<Void> action = createMockAuditableRestAction();
            lenient().when(action.complete()).thenReturn(null);
            doNothing().when(action).queue();
            return action;
        });

        // removeRoleFromMember() removes the role from the member state and returns a mock AuditableRestAction
        lenient().when(guild.removeRoleFromMember(any(Member.class), any(Role.class))).thenAnswer(inv -> {
            Member member = inv.getArgument(0);
            Role role = inv.getArgument(1);

            // Find the member state and remove the role
            long userId = member.getIdLong();
            MockMemberState memberState = guildState.getMember(userId);
            if (memberState != null) {
                memberState.removeRole(role.getIdLong());
            }

            AuditableRestAction<Void> action = createMockAuditableRestAction();
            lenient().when(action.complete()).thenReturn(null);
            doNothing().when(action).queue();
            return action;
        });

        return guild;
    }

    /**
     * Creates a Category mock backed by state.
     *
     * <p>The returned mock represents a Discord channel category (channel group).
     * It reads category metadata and child channels from the provided state objects.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns category ID from state</li>
     *   <li>{@code getId()} - Returns category ID as string</li>
     *   <li>{@code getName()} - Returns category name from state</li>
     *   <li>{@code getTextChannels()} - Creates mocks for all channels in this category</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     * </ul>
     *
     * @param categoryState The category state containing category metadata
     * @param guildState The parent guild state (needed to look up channels and create guild mock)
     * @return A Mockito mock of {@link Category} backed by the provided state
     */
    public static Category mockCategory(MockCategoryState categoryState, MockGuildState guildState) {
        Category category = mock(Category.class);

        lenient().when(category.getIdLong()).thenReturn(categoryState.getCategoryId());
        lenient().when(category.getId()).thenReturn(String.valueOf(categoryState.getCategoryId()));
        lenient().when(category.getName()).thenReturn(categoryState.getCategoryName());

        // getTextChannels() reads from state
        lenient().when(category.getTextChannels()).thenAnswer(inv -> {
            return guildState.getChannelsInCategory(categoryState.getCategoryId()).stream()
                .map(channelState -> mockTextChannel(channelState, guildState))
                .collect(Collectors.toList());
        });

        // Mock guild reference
        Guild guild = mockGuild(guildState);
        lenient().when(category.getGuild()).thenReturn(guild);

        // createTextChannel() creates a channel in state
        lenient().when(category.createTextChannel(anyString())).thenAnswer(inv -> {
            String channelName = inv.getArgument(0);
            MockChannelState channelState = guildState.createTextChannel(channelName, categoryState.getCategoryId());

            // Use array to work around lambda capture restriction
            final ChannelAction[] actionHolder = new ChannelAction[1];

            @SuppressWarnings("unchecked")
            ChannelAction<TextChannel> action = (ChannelAction<TextChannel>) mock(ChannelAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // Permission override methods return the action itself for chaining
                if (methodName.equals("addPermissionOverride") ||
                    methodName.equals("addMemberPermissionOverride") ||
                    methodName.equals("addRolePermissionOverride")) {
                    return actionHolder[0];
                }

                // complete() returns the created channel
                if (methodName.equals("complete")) {
                    return mockTextChannel(channelState, guildState);
                }

                return null;
            });

            actionHolder[0] = action;
            return action;
        });

        return category;
    }

    /**
     * Creates a TextChannel mock backed by state.
     *
     * <p><b>CRITICAL FEATURE:</b> This is the heart of the stateful mock system.
     * When you call {@code channel.sendMessage()}, the message is actually stored
     * in the {@link MockChannelState}! This enables true message verification in tests.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns channel ID from state</li>
     *   <li>{@code getId()} - Returns channel ID as string</li>
     *   <li>{@code getName()} - Returns channel name from state</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     *   <li>{@code sendMessage(String)} - <b>STORES THE MESSAGE IN STATE</b></li>
     *   <li>{@code sendMessage(MessageCreateData)} - <b>STORES THE MESSAGE IN STATE</b></li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * MockChannelState channelState = guild.createTextChannel("test", categoryId);
     * TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guild);
     *
     * // This message gets stored in state!
     * channel.sendMessage("Hello world").queue();
     *
     * // Verify it was stored
     * List<MockMessageState> messages = channelState.getMessages();
     * assertThat(messages.get(0).getContent()).isEqualTo("Hello world");
     * }</pre>
     *
     * @param channelState The channel state where messages will be stored
     * @param guildState The parent guild state (needed to generate message IDs and create guild mock)
     * @return A Mockito mock of {@link TextChannel} backed by the provided state
     */
    public static TextChannel mockTextChannel(MockChannelState channelState, MockGuildState guildState) {
        TextChannel channel = mock(TextChannel.class);

        lenient().when(channel.getIdLong()).thenReturn(channelState.getChannelId());
        lenient().when(channel.getId()).thenReturn(String.valueOf(channelState.getChannelId()));
        lenient().when(channel.getName()).thenReturn(channelState.getChannelName());

        // getGuild() returns stateful guild mock
        Guild guild = mockGuild(guildState);
        lenient().when(channel.getGuild()).thenReturn(guild);

        // getParentCategory() returns the category this channel belongs to
        long categoryId = channelState.getCategoryId();
        MockCategoryState categoryState = guildState.getCategories().stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .orElse(null);
        if (categoryState != null) {
            Category category = mockCategory(categoryState, guildState);
            lenient().when(channel.getParentCategory()).thenReturn(category);
        }

        // retrieveArchivedPublicThreadChannels() and retrieveArchivedPrivateThreadChannels()
        // Return thread channels that exist for this channel
        ThreadChannelPaginationAction publicThreadsAction =
            mock(ThreadChannelPaginationAction.class, RETURNS_DEEP_STUBS);

        List<ThreadChannel> publicThreads =
            guildState.getThreadsInChannel(channelState.getChannelId()).stream()
                .map(threadState -> mockThreadChannel(threadState, guildState))
                .collect(java.util.stream.Collectors.toList());

        lenient().when(publicThreadsAction.complete()).thenReturn(publicThreads);
        lenient().when(channel.retrieveArchivedPublicThreadChannels()).thenReturn(publicThreadsAction);

        ThreadChannelPaginationAction privateThreadsAction =
            mock(ThreadChannelPaginationAction.class, RETURNS_DEEP_STUBS);
        lenient().when(privateThreadsAction.complete()).thenReturn(Collections.emptyList());
        lenient().when(channel.retrieveArchivedPrivateThreadChannels()).thenReturn(privateThreadsAction);

        // sendMessage() STORES the message in state
        lenient().when(channel.sendMessage(anyString())).thenAnswer(inv -> {
            final String[] currentContent = {inv.getArgument(0)};  // Use array to allow modification
            long messageId = guildState.getServer().nextMessageId();

            // Use a list to track file attachments
            final List<FileUpload> fileAttachments = new ArrayList<>();

            // Return mock action that supports chaining and complete
            final MessageCreateAction[] actionHolder = new MessageCreateAction[1];
            MessageCreateAction action = mock(MessageCreateAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // getContent() returns the current content
                if (methodName.equals("getContent")) {
                    return currentContent[0];
                }

                // setContent() updates the content and returns the action for chaining
                if (methodName.equals("setContent")) {
                    currentContent[0] = inv2.getArgument(0);
                    return actionHolder[0];
                }

                // Capture file attachments
                if (methodName.equals("addFiles")) {
                    // addFiles can take varargs or Collection
                    Object[] args = inv2.getArguments();
                    for (Object arg : args) {
                        if (arg instanceof FileUpload) {
                            fileAttachments.add((FileUpload) arg);
                        } else if (arg instanceof Collection) {
                            // Use type-safe helper method instead of unchecked cast
                            addFileUploadsFromCollection((Collection<?>) arg, fileAttachments);
                        }
                    }
                    return actionHolder[0];
                }

                // Chaining methods return the action itself
                if (methodName.equals("setFiles") ||
                    methodName.equals("addActionRow") ||
                    methodName.equals("addComponents")) {
                    return actionHolder[0];
                }

                // complete() or queue() finalizes the message with attachments
                if (methodName.equals("complete") || methodName.equals("queue")) {
                    // Create message with attachments and add to channel
                    MockMessageState message = new MockMessageState(messageId, channelState.getChannelId(), 0L, currentContent[0], fileAttachments);
                    channelState.addMessage(message);
                    return null;
                }

                return null;
            });
            actionHolder[0] = action;
            return action;
        });

        lenient().when(channel.sendMessage(any(MessageCreateData.class))).thenAnswer(inv -> {
            MessageCreateData data = inv.getArgument(0);
            long messageId = guildState.getServer().nextMessageId();

            // Extract file attachments from MessageCreateData and make mutable content
            List<FileUpload> fileAttachments = new ArrayList<>(data.getFiles());
            final String[] currentContent = {data.getContent() != null ? data.getContent() : ""};

            // Return mock action that supports chaining and complete
            final MessageCreateAction[] actionHolder = new MessageCreateAction[1];
            MessageCreateAction action = mock(MessageCreateAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // getContent() returns the current content (never null)
                if (methodName.equals("getContent")) {
                    return currentContent[0];
                }

                // setContent() updates the content and returns the action for chaining
                if (methodName.equals("setContent")) {
                    String newContent = inv2.getArgument(0);
                    currentContent[0] = newContent != null ? newContent : "";
                    return actionHolder[0];
                }

                // Capture additional file attachments
                if (methodName.equals("addFiles")) {
                    Object[] args = inv2.getArguments();
                    for (Object arg : args) {
                        if (arg instanceof FileUpload) {
                            fileAttachments.add((FileUpload) arg);
                        } else if (arg instanceof Collection) {
                            // Use type-safe helper method instead of unchecked cast
                            addFileUploadsFromCollection((Collection<?>) arg, fileAttachments);
                        }
                    }
                    return actionHolder[0];
                }

                // Chaining methods return the action itself
                if (methodName.equals("setFiles") ||
                    methodName.equals("addActionRow") ||
                    methodName.equals("addComponents")) {
                    return actionHolder[0];
                }

                // complete() or queue() finalizes the message with attachments
                if (methodName.equals("complete") || methodName.equals("queue")) {
                    MockMessageState message = new MockMessageState(messageId, channelState.getChannelId(), 0L, currentContent[0], fileAttachments);
                    channelState.addMessage(message);
                    return null;
                }

                return null;
            });
            actionHolder[0] = action;
            return action;
        });

        // getHistory() returns a MessageHistory that can retrieve messages with attachments
        MessageHistory messageHistory = mock(MessageHistory.class, RETURNS_DEEP_STUBS);
        lenient().when(channel.getHistory()).thenReturn(messageHistory);

        // retrievePast() retrieves messages from the channel state
        lenient().when(messageHistory.retrievePast(anyInt())).thenAnswer(inv -> {
            int limit = inv.getArgument(0);
            List<MockMessageState> messageStates = channelState.getMessages();

            // Get the most recent 'limit' messages
            List<MockMessageState> recentMessages = messageStates.subList(
                Math.max(0, messageStates.size() - limit),
                messageStates.size()
            );

            // Convert to Message mocks
            List<Message> messages = new ArrayList<>();
            for (MockMessageState msgState : recentMessages) {
                Message msg = mock(Message.class, RETURNS_DEEP_STUBS);
                lenient().when(msg.getContentRaw()).thenReturn(msgState.getContent());
                lenient().when(msg.getIdLong()).thenReturn(msgState.getMessageId());

                // Mock attachments
                List<Message.Attachment> attachments = new ArrayList<>();
                for (FileUpload fileUpload : msgState.getAttachments()) {
                    Message.Attachment attachment =
                        mock(Message.Attachment.class, RETURNS_DEEP_STUBS);

                    // Store the file data
                    byte[] fileData = null;
                    try {
                        fileData = fileUpload.getData().readAllBytes();
                    } catch (Exception e) {
                        fileData = new byte[0];
                    }

                    final byte[] finalFileData = fileData;
                    lenient().when(attachment.getProxy().download()).thenAnswer(inv2 -> {
                        return java.util.concurrent.CompletableFuture.completedFuture(
                            new java.io.ByteArrayInputStream(finalFileData)
                        );
                    });

                    attachments.add(attachment);
                }

                lenient().when(msg.getAttachments()).thenReturn(attachments);
                messages.add(msg);
            }

            // Mock the completion of retrievePast
            RestAction<List<Message>> action = createMockRestAction();
            lenient().when(action.complete()).thenReturn(messages);

            // Also update the MessageHistory to return these messages
            lenient().when(messageHistory.getRetrievedHistory()).thenReturn(messages);

            return action;
        });

        // createThreadChannel() creates a thread in state (1-parameter version)
        lenient().when(channel.createThreadChannel(anyString())).thenAnswer(inv -> {
            String threadName = inv.getArgument(0);
            MockThreadChannelState threadState = guildState.createThread(threadName, channelState.getChannelId());

            // Use array to work around lambda capture restriction
            final ThreadChannelAction[] actionHolder = new ThreadChannelAction[1];

            ThreadChannelAction action = mock(ThreadChannelAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // Setter methods return the action itself for chaining
                if (methodName.equals("setInvitable") ||
                    methodName.equals("setAutoArchiveDuration")) {
                    return actionHolder[0];
                }

                // complete() returns the created thread
                if (methodName.equals("complete")) {
                    return mockThreadChannel(threadState, guildState);
                }

                return null;
            });

            actionHolder[0] = action;
            return action;
        });

        // createThreadChannel() creates a thread in state (2-parameter version)
        lenient().when(channel.createThreadChannel(anyString(), anyBoolean())).thenAnswer(inv -> {
            String threadName = inv.getArgument(0);
            MockThreadChannelState threadState = guildState.createThread(threadName, channelState.getChannelId());

            // Use array to work around lambda capture restriction
            final ThreadChannelAction[] actionHolder = new ThreadChannelAction[1];

            ThreadChannelAction action = mock(ThreadChannelAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // Setter methods return the action itself for chaining
                if (methodName.equals("setInvitable") ||
                    methodName.equals("setAutoArchiveDuration")) {
                    return actionHolder[0];
                }

                // complete() returns the created thread
                if (methodName.equals("complete")) {
                    return mockThreadChannel(threadState, guildState);
                }

                return null;
            });

            actionHolder[0] = action;
            return action;
        });

        // getJDA() returns a mock JDA with getSelfUser() for permission checking
        JDA jda = mock(JDA.class, RETURNS_DEEP_STUBS);
        lenient().when(channel.getJDA()).thenReturn(jda);

        // Create a self user for the bot (ID 1)
        SelfUser selfUser = mock(SelfUser.class);
        lenient().when(selfUser.getIdLong()).thenReturn(1L);
        lenient().when(selfUser.getId()).thenReturn("1");
        lenient().when(jda.getSelfUser()).thenReturn(selfUser);

        // Create a self member with full permissions
        Member selfMember = mock(Member.class, RETURNS_DEEP_STUBS);
        lenient().when(selfMember.getIdLong()).thenReturn(1L);
        lenient().when(selfMember.getId()).thenReturn("1");
        lenient().when(selfMember.getUser()).thenReturn(selfUser);
        lenient().when(selfMember.hasAccess(any())).thenReturn(true);
        lenient().when(selfMember.hasPermission(any(GuildChannel.class), any(Permission[].class))).thenReturn(true);
        lenient().when(guild.getSelfMember()).thenReturn(selfMember);

        return channel;
    }

    /**
     * Creates a ThreadChannel mock backed by state.
     *
     * <p>The returned mock represents a Discord thread channel. Thread channels
     * are special channels that branch off from regular text channels for focused discussions.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns thread ID from state</li>
     *   <li>{@code getId()} - Returns thread ID as string</li>
     *   <li>{@code getName()} - Returns thread name from state</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     *   <li>{@code sendMessage(String)} - <b>STORES THE MESSAGE IN STATE</b></li>
     *   <li>{@code sendMessage(MessageCreateData)} - <b>STORES THE MESSAGE IN STATE</b></li>
     * </ul>
     *
     * @param threadState The thread state where messages will be stored
     * @param guildState The parent guild state (needed to generate message IDs and create guild mock)
     * @return A Mockito mock of {@link ThreadChannel} backed by the provided state
     */
    public static ThreadChannel mockThreadChannel(MockThreadChannelState threadState, MockGuildState guildState) {
        ThreadChannel thread = mock(ThreadChannel.class);

        lenient().when(thread.getIdLong()).thenReturn(threadState.getThreadId());
        lenient().when(thread.getId()).thenReturn(String.valueOf(threadState.getThreadId()));
        lenient().when(thread.getName()).thenReturn(threadState.getThreadName());

        // getGuild() returns stateful guild mock
        Guild guild = mockGuild(guildState);
        lenient().when(thread.getGuild()).thenReturn(guild);

        // getJDA() returns a mock JDA that supports retrieveUserById()
        JDA jda = mock(JDA.class, RETURNS_DEEP_STUBS);
        lenient().when(thread.getJDA()).thenReturn(jda);

        // Mock retrieveUserById() to return a CacheRestAction with a user
        lenient().when(jda.retrieveUserById(anyString())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            long userIdLong = Long.parseLong(userId);
            MockUserState userState = guildState.getUser(userIdLong);
            User user = userState != null ? mockUser(userState) : mock(User.class);

            CacheRestAction<User> action = createMockCacheRestAction();
            doNothing().when(action).queue(any());
            lenient().when(action.complete()).thenReturn(user);
            return action;
        });

        // addThreadMember() returns a RestAction
        lenient().when(thread.addThreadMember(any(User.class))).thenAnswer(inv -> {
            RestAction<Void> action = createMockRestAction();
            doNothing().when(action).queue();
            lenient().when(action.complete()).thenReturn(null);
            return action;
        });

        // sendMessage() STORES the message in state
        lenient().when(thread.sendMessage(anyString())).thenAnswer(inv -> {
            final String[] currentContent = {inv.getArgument(0)};  // Use array to allow modification
            long messageId = guildState.getServer().nextMessageId();

            // Return mock action with getContent() and setContent() support
            final MessageCreateAction[] actionHolder = new MessageCreateAction[1];
            MessageCreateAction action = mock(MessageCreateAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // getContent() returns the current content
                if (methodName.equals("getContent")) {
                    return currentContent[0];
                }

                // setContent() updates the content and returns the action for chaining
                if (methodName.equals("setContent")) {
                    currentContent[0] = inv2.getArgument(0);
                    return actionHolder[0];
                }

                // queue() or complete() finalizes the message
                if (methodName.equals("queue") || methodName.equals("complete")) {
                    MockMessageState message = new MockMessageState(messageId, threadState.getThreadId(), 0L, currentContent[0]);
                    threadState.addMessage(message);
                    return null;
                }

                return null;
            });
            actionHolder[0] = action;
            return action;
        });

        lenient().when(thread.sendMessage(any(MessageCreateData.class))).thenAnswer(inv -> {
            MessageCreateData data = inv.getArgument(0);
            long messageId = guildState.getServer().nextMessageId();

            // Extract file attachments from MessageCreateData and make mutable content
            List<FileUpload> fileAttachments = new ArrayList<>(data.getFiles());
            final String[] currentContent = {data.getContent() != null ? data.getContent() : ""};

            // Return mock action that supports chaining and complete
            final MessageCreateAction[] actionHolder = new MessageCreateAction[1];
            MessageCreateAction action = mock(MessageCreateAction.class, inv2 -> {
                String methodName = inv2.getMethod().getName();

                // getContent() returns the current content (never null)
                if (methodName.equals("getContent")) {
                    return currentContent[0];
                }

                // setContent() updates the content and returns the action for chaining
                if (methodName.equals("setContent")) {
                    String newContent = inv2.getArgument(0);
                    currentContent[0] = newContent != null ? newContent : "";
                    return actionHolder[0];
                }

                // Capture additional file attachments
                if (methodName.equals("addFiles")) {
                    Object[] args = inv2.getArguments();
                    for (Object arg : args) {
                        if (arg instanceof FileUpload) {
                            fileAttachments.add((FileUpload) arg);
                        } else if (arg instanceof Collection) {
                            // Use type-safe helper method instead of unchecked cast
                            addFileUploadsFromCollection((Collection<?>) arg, fileAttachments);
                        }
                    }
                    return actionHolder[0];
                }

                // Chaining methods return the action itself
                if (methodName.equals("setFiles") ||
                    methodName.equals("addActionRow") ||
                    methodName.equals("addComponents")) {
                    return actionHolder[0];
                }

                // complete() or queue() finalizes the message with attachments
                if (methodName.equals("complete") || methodName.equals("queue")) {
                    MockMessageState message = new MockMessageState(messageId, threadState.getThreadId(), 0L, currentContent[0], fileAttachments);
                    threadState.addMessage(message);
                    return null;
                }

                return null;
            });
            actionHolder[0] = action;
            return action;
        });

        return thread;
    }

    /**
     * Creates a User mock backed by state.
     *
     * <p>The returned mock represents a Discord user account with basic identity
     * information (ID, username, discriminator).
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns user ID from state</li>
     *   <li>{@code getId()} - Returns user ID as string</li>
     *   <li>{@code getName()} - Returns username from state</li>
     *   <li>{@code getDiscriminator()} - Returns discriminator from state (e.g., "0000")</li>
     *   <li>{@code getAsTag()} - Returns full user tag from state (e.g., "Alice#0000")</li>
     * </ul>
     *
     * @param userState The user state containing user identity information
     * @return A Mockito mock of {@link User} backed by the provided state
     */
    public static User mockUser(MockUserState userState) {
        User user = mock(User.class);

        lenient().when(user.getIdLong()).thenReturn(userState.getUserId());
        lenient().when(user.getId()).thenReturn(String.valueOf(userState.getUserId()));
        lenient().when(user.getName()).thenReturn(userState.getUsername());
        lenient().when(user.getDiscriminator()).thenReturn(userState.getDiscriminator());
        lenient().when(user.getAsTag()).thenReturn(userState.getAsTag());
        lenient().when(user.getAsMention()).thenReturn("<@" + userState.getUserId() + ">");

        return user;
    }

    /**
     * Creates a Member mock backed by state.
     *
     * <p>The returned mock represents a user's membership in a specific guild.
     * It combines user information with guild-specific data like roles. The
     * {@code getRoles()} method reads the member's current roles from state.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getUser()} - Returns a user mock backed by the associated user state</li>
     *   <li>{@code getIdLong()} - Returns user ID from state</li>
     *   <li>{@code getId()} - Returns user ID as string</li>
     *   <li>{@code getRoles()} - Creates mocks for all roles assigned to this member in state</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     * </ul>
     *
     * @param memberState The member state containing guild-specific user data (e.g., roles)
     * @param guildState The parent guild state (needed to look up user and role information)
     * @return A Mockito mock of {@link Member} backed by the provided state
     */
    public static Member mockMember(MockMemberState memberState, MockGuildState guildState) {
        Member member = mock(Member.class);

        MockUserState userState = guildState.getUser(memberState.getUserId());
        User user = mockUser(userState);

        lenient().when(member.getUser()).thenReturn(user);
        lenient().when(member.getIdLong()).thenReturn(memberState.getUserId());
        lenient().when(member.getId()).thenReturn(String.valueOf(memberState.getUserId()));
        lenient().when(member.getAsMention()).thenReturn("<@" + memberState.getUserId() + ">");
        lenient().when(member.getEffectiveName()).thenReturn(userState.getUsername());

        // getRoles() reads from state
        lenient().when(member.getRoles()).thenAnswer(inv -> {
            return memberState.getRoleIds().stream()
                .map(roleId -> guildState.getRole(roleId))
                .filter(roleState -> roleState != null)
                .map(StatefulMockFactory::mockRole)
                .collect(Collectors.toList());
        });

        Guild guild = mockGuild(guildState);
        lenient().when(member.getGuild()).thenReturn(guild);

        return member;
    }

    /**
     * Creates a Role mock backed by state.
     *
     * <p>The returned mock represents a Discord role (permission group) with
     * basic role information (ID and name).
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns role ID from state</li>
     *   <li>{@code getId()} - Returns role ID as string</li>
     *   <li>{@code getName()} - Returns role name from state (e.g., "Moderator")</li>
     * </ul>
     *
     * @param roleState The role state containing role information
     * @return A Mockito mock of {@link Role} backed by the provided state
     */
    public static Role mockRole(MockRoleState roleState) {
        Role role = mock(Role.class);

        lenient().when(role.getIdLong()).thenReturn(roleState.getRoleId());
        lenient().when(role.getId()).thenReturn(String.valueOf(roleState.getRoleId()));
        lenient().when(role.getName()).thenReturn(roleState.getRoleName());
        lenient().when(role.getAsMention()).thenReturn("<@&" + roleState.getRoleId() + ">");

        return role;
    }
}
