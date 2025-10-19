package testutil.discord.state;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockRoleStateTest {

    @Test
    void constructor_setsRoleIdAndName() {
        MockRoleState role = new MockRoleState(3001L, "Moderator");

        assertThat(role.getRoleId()).isEqualTo(3001L);
        assertThat(role.getRoleName()).isEqualTo("Moderator");
    }

    @Test
    void getRoleId_returnsCorrectId() {
        MockRoleState role = new MockRoleState(3002L, "Player");

        assertThat(role.getRoleId()).isEqualTo(3002L);
    }

    @Test
    void getRoleName_returnsCorrectName() {
        MockRoleState role = new MockRoleState(3003L, "Admin");

        assertThat(role.getRoleName()).isEqualTo("Admin");
    }

    @Test
    void multipleRoles_haveIndependentState() {
        MockRoleState role1 = new MockRoleState(3001L, "Moderator");
        MockRoleState role2 = new MockRoleState(3002L, "Player");

        assertThat(role1.getRoleId()).isNotEqualTo(role2.getRoleId());
        assertThat(role1.getRoleName()).isNotEqualTo(role2.getRoleName());
    }

    @Test
    void roleWithLongName_storesFullName() {
        String longName = "Super Administrator with Extra Permissions";
        MockRoleState role = new MockRoleState(3004L, longName);

        assertThat(role.getRoleName()).isEqualTo(longName);
    }

    @Test
    void roleWithEmptyName_storesEmptyName() {
        MockRoleState role = new MockRoleState(3005L, "");

        assertThat(role.getRoleName()).isEmpty();
    }
}
