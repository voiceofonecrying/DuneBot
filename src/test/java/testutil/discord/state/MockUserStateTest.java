package testutil.discord.state;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockUserStateTest {

    @Test
    void constructor_withoutDiscriminator_setsUserIdAndUsername() {
        MockUserState user = new MockUserState(100L, "Alice");

        assertThat(user.getUserId()).isEqualTo(100L);
        assertThat(user.getUsername()).isEqualTo("Alice");
    }

    @Test
    void constructor_withoutDiscriminator_setsDefaultDiscriminator() {
        MockUserState user = new MockUserState(100L, "Alice");

        assertThat(user.getDiscriminator()).isEqualTo("0000");
    }

    @Test
    void constructor_withDiscriminator_setsAllFields() {
        MockUserState user = new MockUserState(100L, "Alice", "1234");

        assertThat(user.getUserId()).isEqualTo(100L);
        assertThat(user.getUsername()).isEqualTo("Alice");
        assertThat(user.getDiscriminator()).isEqualTo("1234");
    }

    @Test
    void getAsTag_withDefaultDiscriminator_returnsUsernameWithZeros() {
        MockUserState user = new MockUserState(100L, "Alice");

        assertThat(user.getAsTag()).isEqualTo("Alice#0000");
    }

    @Test
    void getAsTag_withCustomDiscriminator_returnsUsernameWithDiscriminator() {
        MockUserState user = new MockUserState(100L, "Alice", "1234");

        assertThat(user.getAsTag()).isEqualTo("Alice#1234");
    }

    @Test
    void getUsername_returnsCorrectUsername() {
        MockUserState user = new MockUserState(100L, "BobTheBuilder");

        assertThat(user.getUsername()).isEqualTo("BobTheBuilder");
    }

    @Test
    void getUserId_returnsCorrectId() {
        MockUserState user = new MockUserState(987654321L, "Charlie");

        assertThat(user.getUserId()).isEqualTo(987654321L);
    }

    @Test
    void multipleUsers_haveIndependentState() {
        MockUserState user1 = new MockUserState(100L, "Alice", "0001");
        MockUserState user2 = new MockUserState(200L, "Bob", "0002");

        assertThat(user1.getUserId()).isNotEqualTo(user2.getUserId());
        assertThat(user1.getUsername()).isNotEqualTo(user2.getUsername());
        assertThat(user1.getDiscriminator()).isNotEqualTo(user2.getDiscriminator());
        assertThat(user1.getAsTag()).isNotEqualTo(user2.getAsTag());
    }
}
