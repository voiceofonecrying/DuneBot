package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmojiStateTest {

    private MockEmojiState staticEmoji;
    private MockEmojiState animatedEmoji;

    @BeforeEach
    void setUp() {
        staticEmoji = new MockEmojiState(7000001L, "atreides", false, 12345L);
        animatedEmoji = new MockEmojiState(7000002L, "storm", true, 12345L);
    }

    @Test
    void constructor_setsEmojiIdNameAnimatedAndGuildId() {
        assertThat(staticEmoji.getEmojiId()).isEqualTo(7000001L);
        assertThat(staticEmoji.getName()).isEqualTo("atreides");
        assertThat(staticEmoji.isAnimated()).isFalse();
        assertThat(staticEmoji.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void constructor_handlesAnimatedEmoji() {
        assertThat(animatedEmoji.getEmojiId()).isEqualTo(7000002L);
        assertThat(animatedEmoji.getName()).isEqualTo("storm");
        assertThat(animatedEmoji.isAnimated()).isTrue();
        assertThat(animatedEmoji.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void getAsString_staticEmojiReturnsCorrectFormat() {
        String formatted = staticEmoji.getAsString();

        assertThat(formatted).isEqualTo("<:atreides:7000001>");
    }

    @Test
    void getAsString_animatedEmojiReturnsCorrectFormat() {
        String formatted = animatedEmoji.getAsString();

        assertThat(formatted).isEqualTo("<a:storm:7000002>");
    }

    @Test
    void getAsString_handlesEmojiNamesWithUnderscores() {
        MockEmojiState emoji = new MockEmojiState(7000003L, "fremen_faction", false, 12345L);

        assertThat(emoji.getAsString()).isEqualTo("<:fremen_faction:7000003>");
    }

    @Test
    void getAsString_handlesEmojiNamesWithNumbers() {
        MockEmojiState emoji = new MockEmojiState(7000004L, "spice2", false, 12345L);

        assertThat(emoji.getAsString()).isEqualTo("<:spice2:7000004>");
    }

    @Test
    void getName_returnsExactNameProvided() {
        assertThat(staticEmoji.getName()).isEqualTo("atreides");
        assertThat(animatedEmoji.getName()).isEqualTo("storm");
    }

    @Test
    void isAnimated_returnsFalseForStaticEmoji() {
        assertThat(staticEmoji.isAnimated()).isFalse();
    }

    @Test
    void isAnimated_returnsTrueForAnimatedEmoji() {
        assertThat(animatedEmoji.isAnimated()).isTrue();
    }

    @Test
    void getEmojiId_returnsCorrectId() {
        assertThat(staticEmoji.getEmojiId()).isEqualTo(7000001L);
        assertThat(animatedEmoji.getEmojiId()).isEqualTo(7000002L);
    }

    @Test
    void getGuildId_returnsCorrectGuildId() {
        assertThat(staticEmoji.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void multipleEmojis_canHaveSameName() {
        MockEmojiState emoji1 = new MockEmojiState(7000010L, "test", false, 12345L);
        MockEmojiState emoji2 = new MockEmojiState(7000011L, "test", false, 12345L);

        assertThat(emoji1.getName()).isEqualTo(emoji2.getName());
        assertThat(emoji1.getEmojiId()).isNotEqualTo(emoji2.getEmojiId());
    }

    @Test
    void emojis_canBelongToDifferentGuilds() {
        MockEmojiState emoji1 = new MockEmojiState(7000020L, "atreides", false, 12345L);
        MockEmojiState emoji2 = new MockEmojiState(7000021L, "atreides", false, 67890L);

        assertThat(emoji1.getGuildId()).isNotEqualTo(emoji2.getGuildId());
        assertThat(emoji1.getAsString()).isNotEqualTo(emoji2.getAsString());
    }
}
