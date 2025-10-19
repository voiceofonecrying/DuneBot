package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockReactionStateTest {

    private MockEmojiState customEmoji;
    private MockReactionState customReaction;
    private MockReactionState unicodeReaction;
    private long messageId;

    @BeforeEach
    void setUp() {
        messageId = 1000001L;
        customEmoji = new MockEmojiState(7000001L, "atreides", false, 12345L);
        customReaction = new MockReactionState(customEmoji, messageId);
        unicodeReaction = new MockReactionState("👍", messageId);
    }

    @Test
    void constructor_withCustomEmoji_setsEmojiAndMessageId() {
        assertThat(customReaction.getEmoji()).isEqualTo(customEmoji);
        assertThat(customReaction.getMessageId()).isEqualTo(messageId);
    }

    @Test
    void constructor_withUnicodeEmoji_setsEmojiAndMessageId() {
        assertThat(unicodeReaction.getEmoji()).isEqualTo("👍");
        assertThat(unicodeReaction.getMessageId()).isEqualTo(messageId);
    }

    @Test
    void isCustomEmoji_returnsTrueForCustomEmoji() {
        assertThat(customReaction.isCustomEmoji()).isTrue();
    }

    @Test
    void isCustomEmoji_returnsFalseForUnicodeEmoji() {
        assertThat(unicodeReaction.isCustomEmoji()).isFalse();
    }

    @Test
    void getCustomEmoji_returnsEmojiForCustomEmojiReaction() {
        MockEmojiState result = customReaction.getCustomEmoji();

        assertThat(result).isEqualTo(customEmoji);
        assertThat(result.getName()).isEqualTo("atreides");
    }

    @Test
    void getCustomEmoji_returnsNullForUnicodeEmojiReaction() {
        assertThat(unicodeReaction.getCustomEmoji()).isNull();
    }

    @Test
    void getUnicodeEmoji_returnsNullForCustomEmojiReaction() {
        assertThat(customReaction.getUnicodeEmoji()).isNull();
    }

    @Test
    void getUnicodeEmoji_returnsEmojiStringForUnicodeEmojiReaction() {
        assertThat(unicodeReaction.getUnicodeEmoji()).isEqualTo("👍");
    }

    @Test
    void getEmoji_returnsObjectForBothTypes() {
        assertThat(customReaction.getEmoji()).isInstanceOf(MockEmojiState.class);
        assertThat(unicodeReaction.getEmoji()).isInstanceOf(String.class);
    }

    @Test
    void addReaction_addsUserToReactionList() {
        customReaction.addReaction(100L);

        assertThat(customReaction.getCount()).isEqualTo(1);
        assertThat(customReaction.hasUser(100L)).isTrue();
    }

    @Test
    void addReaction_addsMultipleUsers() {
        customReaction.addReaction(100L);
        customReaction.addReaction(200L);
        customReaction.addReaction(300L);

        assertThat(customReaction.getCount()).isEqualTo(3);
        assertThat(customReaction.hasUser(100L)).isTrue();
        assertThat(customReaction.hasUser(200L)).isTrue();
        assertThat(customReaction.hasUser(300L)).isTrue();
    }

    @Test
    void addReaction_ignoresDuplicateUserIds() {
        customReaction.addReaction(100L);
        customReaction.addReaction(100L);
        customReaction.addReaction(100L);

        assertThat(customReaction.getCount()).isEqualTo(1);
    }

    @Test
    void removeReaction_removesUserFromReactionList() {
        customReaction.addReaction(100L);
        customReaction.addReaction(200L);

        customReaction.removeReaction(100L);

        assertThat(customReaction.getCount()).isEqualTo(1);
        assertThat(customReaction.hasUser(100L)).isFalse();
        assertThat(customReaction.hasUser(200L)).isTrue();
    }

    @Test
    void removeReaction_onNonexistentUser_doesNothing() {
        customReaction.addReaction(100L);

        customReaction.removeReaction(9999L);

        assertThat(customReaction.getCount()).isEqualTo(1);
        assertThat(customReaction.hasUser(100L)).isTrue();
    }

    @Test
    void getCount_initiallyReturnsZero() {
        assertThat(customReaction.getCount()).isZero();
    }

    @Test
    void getCount_returnsCorrectCountAfterAddingReactions() {
        customReaction.addReaction(100L);
        customReaction.addReaction(200L);

        assertThat(customReaction.getCount()).isEqualTo(2);
    }

    @Test
    void hasUser_returnsTrueForUserWhoReacted() {
        customReaction.addReaction(100L);

        assertThat(customReaction.hasUser(100L)).isTrue();
    }

    @Test
    void hasUser_returnsFalseForUserWhoDidNotReact() {
        customReaction.addReaction(100L);

        assertThat(customReaction.hasUser(200L)).isFalse();
    }

    @Test
    void hasUser_returnsFalseAfterRemovingReaction() {
        customReaction.addReaction(100L);
        customReaction.removeReaction(100L);

        assertThat(customReaction.hasUser(100L)).isFalse();
    }

    @Test
    void getUserIds_returnsAllUserIdsWhoReacted() {
        customReaction.addReaction(100L);
        customReaction.addReaction(200L);
        customReaction.addReaction(300L);

        List<Long> userIds = customReaction.getUserIds();

        assertThat(userIds).containsExactly(100L, 200L, 300L);
    }

    @Test
    void getUserIds_returnsEmptyListInitially() {
        assertThat(customReaction.getUserIds()).isEmpty();
    }

    @Test
    void getUserIds_returnsNewListEachTime() {
        customReaction.addReaction(100L);

        List<Long> userIds1 = customReaction.getUserIds();
        List<Long> userIds2 = customReaction.getUserIds();

        assertThat(userIds1).isNotSameAs(userIds2);
    }

    @Test
    void getUserIds_modifyingReturnedListDoesNotAffectState() {
        customReaction.addReaction(100L);

        List<Long> userIds = customReaction.getUserIds();
        userIds.add(9999L);

        assertThat(customReaction.getUserIds()).containsExactly(100L);
        assertThat(customReaction.getUserIds()).doesNotContain(9999L);
    }

    @Test
    void unicodeReaction_supportsAddingUsers() {
        unicodeReaction.addReaction(100L);
        unicodeReaction.addReaction(200L);

        assertThat(unicodeReaction.getCount()).isEqualTo(2);
        assertThat(unicodeReaction.hasUser(100L)).isTrue();
    }

    @Test
    void differentEmojiTypes_canBeUsedOnSameMessage() {
        assertThat(customReaction.getMessageId()).isEqualTo(unicodeReaction.getMessageId());
        assertThat(customReaction.isCustomEmoji()).isTrue();
        assertThat(unicodeReaction.isCustomEmoji()).isFalse();
    }
}
