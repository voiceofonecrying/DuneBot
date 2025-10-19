package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockCategoryStateTest {

    private MockCategoryState category;

    @BeforeEach
    void setUp() {
        category = new MockCategoryState(5000L, "game-channels", 12345L);
    }

    @Test
    void constructor_setsCategoryIdNameAndGuildId() {
        assertThat(category.getCategoryId()).isEqualTo(5000L);
        assertThat(category.getCategoryName()).isEqualTo("game-channels");
        assertThat(category.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void getChannelIds_initiallyReturnsEmptyList() {
        List<Long> channelIds = category.getChannelIds();

        assertThat(channelIds).isEmpty();
    }

    @Test
    void addChannel_addsChannelIdToList() {
        category.addChannel(2001L);

        List<Long> channelIds = category.getChannelIds();

        assertThat(channelIds).containsExactly(2001L);
    }

    @Test
    void addChannel_addsMultipleChannels() {
        category.addChannel(2001L);
        category.addChannel(2002L);
        category.addChannel(2003L);

        List<Long> channelIds = category.getChannelIds();

        assertThat(channelIds).containsExactly(2001L, 2002L, 2003L);
    }

    @Test
    void addChannel_ignoresDuplicates() {
        category.addChannel(2001L);
        category.addChannel(2001L);

        List<Long> channelIds = category.getChannelIds();

        assertThat(channelIds).containsExactly(2001L);
    }

    @Test
    void getChannelIds_returnsNewListEachTime() {
        category.addChannel(2001L);

        List<Long> channelIds1 = category.getChannelIds();
        List<Long> channelIds2 = category.getChannelIds();

        assertThat(channelIds1).isNotSameAs(channelIds2);
    }

    @Test
    void getChannelIds_modifyingReturnedListDoesNotAffectState() {
        category.addChannel(2001L);

        List<Long> channelIds = category.getChannelIds();
        channelIds.add(9999L);

        assertThat(category.getChannelIds()).containsExactly(2001L);
        assertThat(category.getChannelIds()).doesNotContain(9999L);
    }
}
