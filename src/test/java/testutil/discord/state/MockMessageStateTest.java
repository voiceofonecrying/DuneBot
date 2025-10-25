package testutil.discord.state;

import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockMessageStateTest {

    @Test
    void constructor_withoutAttachments_setsBasicFields() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message");

        assertThat(message.getMessageId()).isEqualTo(1000001L);
        assertThat(message.getChannelId()).isEqualTo(2001L);
        assertThat(message.getAuthorId()).isEqualTo(100L);
        assertThat(message.getContent()).isEqualTo("Test message");
    }

    @Test
    void constructor_withoutAttachments_setsEmptyAttachmentsList() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message");

        assertThat(message.getAttachments()).isEmpty();
    }

    @Test
    void constructor_withoutAttachments_setsTimestampToNow() {
        Instant before = Instant.now();
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message");
        Instant after = Instant.now();

        assertThat(message.getTimestamp()).isAfterOrEqualTo(before);
        assertThat(message.getTimestamp()).isBeforeOrEqualTo(after);
    }

    @Test
    void constructor_withAttachments_setsAllFields() {
        List<FileUpload> attachments = new ArrayList<>();
        // Create real FileUpload instances instead of mocking (Java 23 compatibility)
        byte[] testData1 = "test attachment 1".getBytes();
        byte[] testData2 = "test attachment 2".getBytes();
        attachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData1), "test1.txt"));
        attachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData2), "test2.txt"));

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message", attachments);

        assertThat(message.getMessageId()).isEqualTo(1000001L);
        assertThat(message.getChannelId()).isEqualTo(2001L);
        assertThat(message.getAuthorId()).isEqualTo(100L);
        assertThat(message.getContent()).isEqualTo("Test message");
        assertThat(message.getAttachments()).hasSize(2);
    }

    @Test
    void constructor_withEmptyAttachments_setsEmptyList() {
        List<FileUpload> attachments = new ArrayList<>();

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message", attachments);

        assertThat(message.getAttachments()).isEmpty();
    }

    @Test
    void getAuthorId_forBotMessages_returnsZero() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 0L, "Bot message");

        assertThat(message.getAuthorId()).isEqualTo(0L);
    }

    @Test
    void getContent_returnsCorrectContent() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Hello, world!");

        assertThat(message.getContent()).isEqualTo("Hello, world!");
    }

    @Test
    void getContent_withEmptyString_returnsEmptyString() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "");

        assertThat(message.getContent()).isEmpty();
    }

    @Test
    void getAttachments_returnsNewListEachTime() {
        List<FileUpload> attachments = new ArrayList<>();
        // Create a real FileUpload instead of mocking (Java 23 compatibility)
        byte[] testData = "test attachment".getBytes();
        attachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData), "test.txt"));

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test", attachments);

        List<FileUpload> attachments1 = message.getAttachments();
        List<FileUpload> attachments2 = message.getAttachments();

        assertThat(attachments1).isNotSameAs(attachments2);
    }

    @Test
    void getAttachments_modifyingReturnedListDoesNotAffectState() {
        List<FileUpload> attachments = new ArrayList<>();
        // Create a real FileUpload instead of mocking (Java 23 compatibility)
        byte[] testData = "test attachment".getBytes();
        attachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData), "test.txt"));

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test", attachments);

        List<FileUpload> retrievedAttachments = message.getAttachments();
        // Create another real FileUpload for the modification test
        byte[] testData2 = "test attachment 2".getBytes();
        retrievedAttachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData2), "test2.txt"));

        assertThat(message.getAttachments()).hasSize(1);
    }

    @Test
    void hasAttachments_returnsTrueWhenAttachmentsExist() {
        List<FileUpload> attachments = new ArrayList<>();
        // Create a real FileUpload instead of mocking (Java 23 compatibility)
        byte[] testData = "test attachment".getBytes();
        attachments.add(FileUpload.fromData(new java.io.ByteArrayInputStream(testData), "test.txt"));

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test", attachments);

        assertThat(message.hasAttachments()).isTrue();
    }

    @Test
    void hasAttachments_returnsFalseWhenNoAttachments() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.hasAttachments()).isFalse();
    }

    @Test
    void hasAttachments_returnsFalseWhenEmptyAttachmentsList() {
        List<FileUpload> attachments = new ArrayList<>();

        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test", attachments);

        assertThat(message.hasAttachments()).isFalse();
    }

    @Test
    void getTimestamp_isNotNull() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.getTimestamp()).isNotNull();
    }

    @Test
    void multipleMessages_haveDistinctTimestamps() throws InterruptedException {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        Thread.sleep(1); // Ensure some time passes
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");

        assertThat(message1.getTimestamp()).isBefore(message2.getTimestamp());
    }

    // ========== Button Tests ==========

    @Test
    void addButton_addsButtonToMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockButtonState button = new MockButtonState("btn-1", "Click Me", "PRIMARY");

        message.addButton(button);

        assertThat(message.getButtons()).hasSize(1);
        assertThat(message.getButtons().get(0).getComponentId()).isEqualTo("btn-1");
    }

    @Test
    void addButton_addsMultipleButtons() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockButtonState button1 = new MockButtonState("btn-1", "Button 1", "PRIMARY");
        MockButtonState button2 = new MockButtonState("btn-2", "Button 2", "SECONDARY");
        MockButtonState button3 = new MockButtonState("btn-3", "Button 3", "SUCCESS");

        message.addButton(button1);
        message.addButton(button2);
        message.addButton(button3);

        assertThat(message.getButtons()).hasSize(3);
        assertThat(message.getButtons()).containsExactly(button1, button2, button3);
    }

    @Test
    void getButtons_returnsEmptyListInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.getButtons()).isEmpty();
    }

    @Test
    void getButtons_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addButton(new MockButtonState("btn-1", "Button", "PRIMARY"));

        List<MockButtonState> buttons1 = message.getButtons();
        List<MockButtonState> buttons2 = message.getButtons();

        assertThat(buttons1).isNotSameAs(buttons2);
    }

    @Test
    void getButtons_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockButtonState realButton = new MockButtonState("btn-1", "Real", "PRIMARY");
        message.addButton(realButton);

        List<MockButtonState> buttons = message.getButtons();
        MockButtonState fakeButton = new MockButtonState("btn-2", "Fake", "SECONDARY");
        buttons.add(fakeButton);

        assertThat(message.getButtons()).hasSize(1);
        assertThat(message.getButtons().get(0)).isEqualTo(realButton);
    }

    @Test
    void hasButtons_returnsFalseInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.hasButtons()).isFalse();
    }

    @Test
    void hasButtons_returnsTrueAfterAddingButton() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addButton(new MockButtonState("btn-1", "Button", "PRIMARY"));

        assertThat(message.hasButtons()).isTrue();
    }

    // ========== Embed Tests ==========

    @Test
    void addEmbed_addsEmbedToMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockEmbedState embed = new MockEmbedState();
        embed.setTitle("Test Embed");

        message.addEmbed(embed);

        assertThat(message.getEmbeds()).hasSize(1);
        assertThat(message.getEmbeds().get(0).getTitle()).isEqualTo("Test Embed");
    }

    @Test
    void addEmbed_addsMultipleEmbeds() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockEmbedState embed1 = new MockEmbedState();
        embed1.setTitle("Embed 1");
        MockEmbedState embed2 = new MockEmbedState();
        embed2.setTitle("Embed 2");

        message.addEmbed(embed1);
        message.addEmbed(embed2);

        assertThat(message.getEmbeds()).hasSize(2);
        assertThat(message.getEmbeds()).containsExactly(embed1, embed2);
    }

    @Test
    void getEmbeds_returnsEmptyListInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.getEmbeds()).isEmpty();
    }

    @Test
    void getEmbeds_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addEmbed(new MockEmbedState());

        List<MockEmbedState> embeds1 = message.getEmbeds();
        List<MockEmbedState> embeds2 = message.getEmbeds();

        assertThat(embeds1).isNotSameAs(embeds2);
    }

    @Test
    void getEmbeds_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockEmbedState realEmbed = new MockEmbedState();
        message.addEmbed(realEmbed);

        List<MockEmbedState> embeds = message.getEmbeds();
        embeds.add(new MockEmbedState());

        assertThat(message.getEmbeds()).hasSize(1);
        assertThat(message.getEmbeds().get(0)).isEqualTo(realEmbed);
    }

    @Test
    void hasEmbeds_returnsFalseInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.hasEmbeds()).isFalse();
    }

    @Test
    void hasEmbeds_returnsTrueAfterAddingEmbed() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addEmbed(new MockEmbedState());

        assertThat(message.hasEmbeds()).isTrue();
    }

    // ========== Reaction Tests ==========

    @Test
    void addReaction_addsReactionToMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockReactionState reaction = new MockReactionState("👍", message.getMessageId());

        message.addReaction(reaction);

        assertThat(message.getReactions()).hasSize(1);
        assertThat(message.getReactions().get(0).getUnicodeEmoji()).isEqualTo("👍");
    }

    @Test
    void addReaction_addsMultipleReactions() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockReactionState reaction1 = new MockReactionState("👍", message.getMessageId());
        MockReactionState reaction2 = new MockReactionState("❤️", message.getMessageId());
        MockReactionState reaction3 = new MockReactionState("🎉", message.getMessageId());

        message.addReaction(reaction1);
        message.addReaction(reaction2);
        message.addReaction(reaction3);

        assertThat(message.getReactions()).hasSize(3);
        assertThat(message.getReactions()).containsExactly(reaction1, reaction2, reaction3);
    }

    @Test
    void getReactions_returnsEmptyListInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.getReactions()).isEmpty();
    }

    @Test
    void getReactions_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addReaction(new MockReactionState("👍", message.getMessageId()));

        List<MockReactionState> reactions1 = message.getReactions();
        List<MockReactionState> reactions2 = message.getReactions();

        assertThat(reactions1).isNotSameAs(reactions2);
    }

    @Test
    void getReactions_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        MockReactionState realReaction = new MockReactionState("👍", message.getMessageId());
        message.addReaction(realReaction);

        List<MockReactionState> reactions = message.getReactions();
        reactions.add(new MockReactionState("❤️", message.getMessageId()));

        assertThat(message.getReactions()).hasSize(1);
        assertThat(message.getReactions().get(0)).isEqualTo(realReaction);
    }

    @Test
    void hasReactions_returnsFalseInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.hasReactions()).isFalse();
    }

    @Test
    void hasReactions_returnsTrueAfterAddingReaction() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.addReaction(new MockReactionState("👍", message.getMessageId()));

        assertThat(message.hasReactions()).isTrue();
    }

    // ========== Referenced Message Tests ==========

    @Test
    void getReferencedMessageId_returnsNullInitially() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        assertThat(message.getReferencedMessageId()).isNull();
    }

    @Test
    void setReferencedMessageId_setsReferencedMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.setReferencedMessageId(999999L);

        assertThat(message.getReferencedMessageId()).isEqualTo(999999L);
    }

    @Test
    void setReferencedMessageId_canBeChanged() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.setReferencedMessageId(999999L);
        message.setReferencedMessageId(888888L);

        assertThat(message.getReferencedMessageId()).isEqualTo(888888L);
    }

    @Test
    void setReferencedMessageId_canBeSetToNull() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        message.setReferencedMessageId(999999L);
        message.setReferencedMessageId(null);

        assertThat(message.getReferencedMessageId()).isNull();
    }

    // ========== Integration Tests ==========

    @Test
    void message_canHaveButtonsEmbedsAndReactions() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        message.addButton(new MockButtonState("btn-1", "Button", "PRIMARY"));
        message.addEmbed(new MockEmbedState());
        message.addReaction(new MockReactionState("👍", message.getMessageId()));

        assertThat(message.hasButtons()).isTrue();
        assertThat(message.hasEmbeds()).isTrue();
        assertThat(message.hasReactions()).isTrue();
        assertThat(message.getButtons()).hasSize(1);
        assertThat(message.getEmbeds()).hasSize(1);
        assertThat(message.getReactions()).hasSize(1);
    }

    @Test
    void message_canHaveAllNewFeatures() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");

        message.addButton(new MockButtonState("btn-1", "Button", "PRIMARY"));
        message.addEmbed(new MockEmbedState());
        message.addReaction(new MockReactionState("👍", message.getMessageId()));
        message.setReferencedMessageId(999999L);

        assertThat(message.hasButtons()).isTrue();
        assertThat(message.hasEmbeds()).isTrue();
        assertThat(message.hasReactions()).isTrue();
        assertThat(message.getReferencedMessageId()).isEqualTo(999999L);
    }
}
