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
    TreacheryCard kulon;
    TreacheryCard baliset;
    TreacheryCard crysknife;
    TreacheryCard chaumas;
    TreacheryCard shield;
    TreacheryCard snooper;
    TreacheryCard lasgun;
    TreacheryCard artilleryStrike;
    TreacheryCard weirdingWay;
    TreacheryCard poisonBlade;
    TreacheryCard poisonTooth;
    TreacheryCard shieldSnooper;
    @BeforeEach
    void setUp() throws IOException {
        zoal = new Leader("Zoal", -1, null, false);
        duncanIdaho = new Leader("Duncan Idaho", 2, null, false);
        cheapHero = new TreacheryCard("Cheap Hero", "Special");
        kulon = new TreacheryCard("Kulon", "Worthless Card");
        baliset = new TreacheryCard("Baliset", "Worthless Card");
        crysknife = new TreacheryCard("Crysknife", "Weapon - Projectile");
        chaumas = new TreacheryCard("Chaumas", "Weapon - Poison");
        shield = new TreacheryCard("Shield", "Defense - Projectile");
        snooper = new TreacheryCard("Snooper", "Defense - Poison");
        lasgun = new TreacheryCard("Lasgun", "Weapon - Special");
        artilleryStrike = new TreacheryCard("Artillery Strike", "Weapon - Special");
        weirdingWay = new TreacheryCard("Weirding Way", "Weapon - Defense - Special");
        poisonBlade = new TreacheryCard("Poison Blade","Weapon - Special");
        poisonTooth = new TreacheryCard("Poison Tooth","Weapon - Poison - Special");
        shieldSnooper = new TreacheryCard("Shield Snooper", "Defense - Special");
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
    void testLeaderDiesPoisonToothWithSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.setOpponentWeaponAndLeader(poisonTooth, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesWithOwnPoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
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

    @Test
    void testPoisonBladeAgainstShield() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.setOpponentWeaponAndLeader(poisonBlade, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.setOpponentWeaponAndLeader(poisonBlade, null);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstShieldSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.setOpponentWeaponAndLeader(poisonBlade, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstProjectileWeapon() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.setOpponentWeaponAndLeader(crysknife, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstPoisonWeapon() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.setOpponentWeaponAndLeader(chaumas, null);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.setOpponentWeaponAndLeader(lasgun, null);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, weirdingWay);
        battlePlan.setOpponentWeaponAndLeader(lasgun, null);
        assertFalse(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testWinnersWeaponNotDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, crysknife, null);
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWinnersDefenseNotDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, kulon, chaumas);
        assertFalse(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testArtilleryStrikeMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWorthlessWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, kulon, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWorthlessDefenseMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, kulon, baliset);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testLosersWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, crysknife, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(true));
    }

    @Test
    void testLosersDefenseMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, kulon, snooper);
        assertTrue(battlePlan.defenseMustBeDiscarded(true));
    }

    @Test
    void testRevokePoisonToothFalse() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, snooper);
        assertFalse(battlePlan.revokePoisonTooth());
        assertNotNull(battlePlan.getWeapon());
    }

    @Test
    void testRevokePoisonToothTrue() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        assertTrue(battlePlan.revokePoisonTooth());
        assertNull(battlePlan.getWeapon());
    }

    @Test
    void testRestorePoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        battlePlan.revokePoisonTooth();
        battlePlan.restorePoisonTooth();
        assertEquals("Poison Tooth", battlePlan.getWeapon().name());
    }

    @Test
    void testRevokePoisonToothWithWeirdingWay() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, weirdingWay);
        assertTrue(battlePlan.revokePoisonTooth());
        assertEquals("Weirding Way", battlePlan.getWeapon().name());
        assertNull(battlePlan.getDefense());
    }

    @Test
    void testRestorePoisonToothWithWeirdingWay() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, weirdingWay);
        battlePlan.revokePoisonTooth();
        battlePlan.restorePoisonTooth();
        assertEquals("Poison Tooth", battlePlan.getWeapon().name());
        assertEquals("Weirding Way", battlePlan.getDefense().name());
    }

    @AfterEach
    void tearDown() {
    }
}
