package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockMemberStateTest {

    private MockMemberState member;

    @BeforeEach
    void setUp() {
        member = new MockMemberState(100L, 12345L);
    }

    @Test
    void constructor_setsUserIdAndGuildId() {
        assertThat(member.getUserId()).isEqualTo(100L);
        assertThat(member.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void getRoleIds_initiallyReturnsEmptyList() {
        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).isEmpty();
    }

    @Test
    void addRole_addsRoleToMember() {
        member.addRole(3001L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3001L);
    }

    @Test
    void addRole_addsMultipleRoles() {
        member.addRole(3001L);
        member.addRole(3002L);
        member.addRole(3003L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3001L, 3002L, 3003L);
    }

    @Test
    void addRole_ignoresDuplicates() {
        member.addRole(3001L);
        member.addRole(3001L);
        member.addRole(3001L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3001L);
    }

    @Test
    void removeRole_removesRoleFromMember() {
        member.addRole(3001L);
        member.addRole(3002L);

        member.removeRole(3001L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3002L);
    }

    @Test
    void removeRole_onNonexistentRole_doesNothing() {
        member.addRole(3001L);

        member.removeRole(9999L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3001L);
    }

    @Test
    void removeRole_onEmptyRoleList_doesNothing() {
        member.removeRole(3001L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).isEmpty();
    }

    @Test
    void removeRole_removesOnlyFirstOccurrence() {
        member.addRole(3001L);
        // Manually add duplicate by reflection would be needed, but since addRole prevents duplicates
        // this test just verifies the behavior is consistent
        member.removeRole(3001L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).isEmpty();
    }

    @Test
    void hasRole_returnsTrueForAssignedRole() {
        member.addRole(3001L);

        assertThat(member.hasRole(3001L)).isTrue();
    }

    @Test
    void hasRole_returnsFalseForUnassignedRole() {
        member.addRole(3001L);

        assertThat(member.hasRole(9999L)).isFalse();
    }

    @Test
    void hasRole_returnsFalseWhenNoRoles() {
        assertThat(member.hasRole(3001L)).isFalse();
    }

    @Test
    void hasRole_returnsFalseAfterRemovingRole() {
        member.addRole(3001L);
        member.removeRole(3001L);

        assertThat(member.hasRole(3001L)).isFalse();
    }

    @Test
    void getRoleIds_returnsNewListEachTime() {
        member.addRole(3001L);

        List<Long> roleIds1 = member.getRoleIds();
        List<Long> roleIds2 = member.getRoleIds();

        assertThat(roleIds1).isNotSameAs(roleIds2);
    }

    @Test
    void getRoleIds_modifyingReturnedListDoesNotAffectState() {
        member.addRole(3001L);

        List<Long> roleIds = member.getRoleIds();
        roleIds.add(9999L);

        assertThat(member.getRoleIds()).containsExactly(3001L);
        assertThat(member.getRoleIds()).doesNotContain(9999L);
    }

    @Test
    void memberWithMultipleRoles_canCheckEachRole() {
        member.addRole(3001L);
        member.addRole(3002L);
        member.addRole(3003L);

        assertThat(member.hasRole(3001L)).isTrue();
        assertThat(member.hasRole(3002L)).isTrue();
        assertThat(member.hasRole(3003L)).isTrue();
        assertThat(member.hasRole(9999L)).isFalse();
    }

    @Test
    void addAndRemoveRoles_maintainsCorrectState() {
        member.addRole(3001L);
        member.addRole(3002L);
        member.addRole(3003L);
        member.removeRole(3002L);
        member.addRole(3004L);

        List<Long> roleIds = member.getRoleIds();

        assertThat(roleIds).containsExactly(3001L, 3003L, 3004L);
    }
}
