package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePlanTest {
    Leader zoal;
    Leader duncanIdaho;
    TreacheryCard cheapHero;
    TreacheryCard crysknife;
    TreacheryCard shield;
    TreacheryCard snooper;
    TreacheryCard lasgun;
    TreacheryCard artilleryStrike;
    TreacheryCard weirdingWay;
    @BeforeEach
    void setUp() throws IOException {
        zoal = new Leader("Zoal", -1, null, false);
        duncanIdaho = new Leader("Duncan Idaho", 2, null, false);
        cheapHero = new TreacheryCard("Cheap Hero", "Special");
        crysknife = new TreacheryCard("Crysknife", "Weapon - Projectile");
        shield = new TreacheryCard("Shield", "Defense - Projectile");
        snooper = new TreacheryCard("Snooper", "Defense - Poison");
        lasgun = new TreacheryCard("Lasgun", "Weapon - Special");
        artilleryStrike = new TreacheryCard("Artillery Strike", "Weapon - Special");
        weirdingWay = new TreacheryCard("Weirding Way", "Weapon - Defense - Special");
    }

    @Test
    void testLeaderSurvivesNoWeapon() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.getLeaderContribution());
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
    void testLeaderSurvivesWithKH() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, true, 0, false, 0, null, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(4, battlePlan.getLeaderContribution());
    }

    @Test
    void testKHWithCheapHero() {
        BattlePlan battlePlan = new BattlePlan(null, cheapHero, true, 0, false, 0, null, snooper);
        battlePlan.setOpponentWeaponAndLeader(null, null);
        assertEquals(2, battlePlan.getLeaderContribution());
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesArtilleryStrikeWithWrongDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.setOpponentWeaponAndLeader(artilleryStrike, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesArtilleryStrikeWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.setOpponentWeaponAndLeader(artilleryStrike, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals(2, battlePlan.getLeaderValue());
    }

    @Test
    void testWeirdingWayDoesNotProtectAgainstArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, weirdingWay);
        battlePlan.setOpponentWeaponAndLeader(artilleryStrike, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesFromTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, snooper);
        battlePlan.setOpponentWeaponAndLeader(null, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, shield);
        battlePlan.setOpponentWeaponAndLeader(null, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals(2, battlePlan.getLeaderValue());
    }

    @Test
    void testZoalHasNoValue() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        assertEquals(0, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString());
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasOpponentLeaderValue() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        battlePlan.setOpponentWeaponAndLeader(crysknife, duncanIdaho);
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString());
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, lasgun, shield);
        battlePlan.setOpponentWeaponAndLeader(crysknife, duncanIdaho);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testOpponentLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, shield);
        battlePlan.setOpponentWeaponAndLeader(lasgun, duncanIdaho);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunNoShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, lasgun, snooper);
        battlePlan.setOpponentWeaponAndLeader(crysknife, duncanIdaho);
        assertFalse(battlePlan.isLasgunShieldExplosion());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testShieldNoLasgun() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, shield);
        battlePlan.setOpponentWeaponAndLeader(crysknife, duncanIdaho);
        assertFalse(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @AfterEach
    void tearDown() {
    }
}
