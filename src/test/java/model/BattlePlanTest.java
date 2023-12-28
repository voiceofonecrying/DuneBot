package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePlanTest {
    Leader zoal;
    Leader duncanIdaho;
    TreacheryCard crysknife;
    TreacheryCard shield;
    TreacheryCard snooper;
    @BeforeEach
    void setUp() throws IOException {
        zoal = new Leader("Zoal", -1, null, false);
        duncanIdaho = new Leader("Duncan Idaho", 2, null, false);
        crysknife = new TreacheryCard("Crysknife", "Weapon - Projectile");
        shield = new TreacheryCard("Shield", "Defense - Projectile");
        snooper = new TreacheryCard("Snooper", "Defense - Poison");
    }

    @Test
    void testLeaderSurvivesNoWeapon() {
        BattlePlan battlePlan = new BattlePlan(null, null, false, 0, false, 0, null, null);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testLeaderDiesWithWrongDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.setOpponentWeaponAndLeader(crysknife, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.setOpponentWeaponAndLeader(crysknife, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasNoValue() {
        BattlePlan plan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        assertEquals(0, plan.getLeaderStrength());
        assertEquals("Leader: Zoal (X)", plan.getLeaderString());
        assertEquals("Zoal", plan.getKilledLeaderString());
        assertEquals(0, plan.combatWater());
    }

    @Test
    void testZoalHasOpponentLeaderValue() {
        BattlePlan plan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        plan.setOpponentWeaponAndLeader(crysknife, duncanIdaho);
        assertEquals(2, plan.getLeaderStrength());
        assertEquals("Leader: Zoal (X)", plan.getLeaderString());
        assertEquals("Zoal", plan.getKilledLeaderString());
        assertEquals(2, plan.combatWater());
    }

    @AfterEach
    void tearDown() {
    }
}
