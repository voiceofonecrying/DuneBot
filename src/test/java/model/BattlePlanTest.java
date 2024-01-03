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
    TreacheryCard chemistry;
    TreacheryCard mirrorWeapon;
    TreacheryCard poisonBlade;
    TreacheryCard poisonTooth;
    TreacheryCard shieldSnooper;
    TreacheryCard stoneBurner;
    TreacheryCard harassAndWithdraw;
    TreacheryCard reinforcements;
    BattlePlan emptyBattlePlan;
    BattlePlan crysknifePlan;
    BattlePlan duncanCrysknifePlan;
    BattlePlan chaumasPlan;
    BattlePlan lasgunPlan;
    BattlePlan artilleryStrikePlan;
    BattlePlan poisonToothPlan;
    BattlePlan poisonBladePlan;
    @BeforeEach
    void setUp() throws IOException {
        zoal = new Leader("Zoal", -1, null, false);
        duncanIdaho = new Leader("Duncan Idaho", 2, null, false);
        cheapHero = new TreacheryCard("Cheap Hero");
        kulon = new TreacheryCard("Kulon");
        baliset = new TreacheryCard("Baliset");
        crysknife = new TreacheryCard("Crysknife");
        chaumas = new TreacheryCard("Chaumas");
        shield = new TreacheryCard("Shield");
        snooper = new TreacheryCard("Snooper");
        lasgun = new TreacheryCard("Lasgun");
        artilleryStrike = new TreacheryCard("Artillery Strike");
        weirdingWay = new TreacheryCard("Weirding Way");
        chemistry = new TreacheryCard("Chemistry");
        mirrorWeapon = new TreacheryCard("Mirror Weapon");
        poisonBlade = new TreacheryCard("Poison Blade");
        poisonTooth = new TreacheryCard("Poison Tooth");
        shieldSnooper = new TreacheryCard("Shield Snooper");
        stoneBurner = new TreacheryCard("Stone Burner");
        harassAndWithdraw = new TreacheryCard("Harass and Withdraw");
        reinforcements = new TreacheryCard("Reinforcements");
        emptyBattlePlan = new BattlePlan(null, null, false, 0, false, 0, null, null);
        crysknifePlan = new BattlePlan(null, null, false, 0, false, 0, crysknife, null);
        duncanCrysknifePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, crysknife, null);
        chaumasPlan = new BattlePlan(null, null, false, 0, false, 0, chaumas, null);
        lasgunPlan = new BattlePlan(null, null, false, 0, false, 0, lasgun, null);
        artilleryStrikePlan = new BattlePlan(null, null, false, 0, false, 0, artilleryStrike, null);
        poisonToothPlan = new BattlePlan(null, null, false, 0, false, 0, poisonTooth, null);
        poisonBladePlan = new BattlePlan(null, null, false, 0, false, 0, poisonBlade, null);
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
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
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
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertEquals(2, battlePlan.getLeaderContribution());
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesArtilleryStrikeWithWrongDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesArtilleryStrikeWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayDoesNotProtectAgainstArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, weirdingWay);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperDoesProtectAgainstArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesFromTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, snooper);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, artilleryStrike, shield);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals(2, battlePlan.getLeaderValue());
    }

    @Test
    void testLeaderDiesPoisonToothWithSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
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
    void testLeaderSurvivesPoisonToothWithChemistry() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, chemistry);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
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
        battlePlan.revealOpponentBattlePlan(duncanCrysknifePlan);
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString());
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, lasgun, shield);
        battlePlan.revealOpponentBattlePlan(duncanCrysknifePlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testOpponentLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, shield);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunNoShield() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, lasgun, snooper);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testShieldNoLasgun() {
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, shield);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testPoisonBladeAgainstShield() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shield);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstShieldSnooper() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstProjectileWeapon() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstPoisonWeapon() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, shieldSnooper);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, weirdingWay);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
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
    void testMirrorWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, mirrorWeapon, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothNotDiscardedIfNotUsed() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, null);
        battlePlan.revokePoisonTooth();
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPortableSnooperMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, null);
        battlePlan.addPortableSnooper();
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testStoneBurnerMustBeDisarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, stoneBurner, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testHarassAndWithdrawMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, harassAndWithdraw, null);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testReinforcementsMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, kulon, reinforcements);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
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
    }

    @Test
    void testRevokePoisonToothTrue() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        assertTrue(battlePlan.revokePoisonTooth());
        assertNotNull(battlePlan.getWeapon());
        assertEquals("Weapon: Poison Tooth (not used)", battlePlan.getWeaponString());
        assertTrue(battlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testOpponentRevokesPoisonTooth() {
        BattlePlan opponentBattlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        BattlePlan battlePlan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        assertTrue(opponentBattlePlan.revokePoisonTooth());
        opponentBattlePlan.revealOpponentBattlePlan(new BattlePlan(battlePlan.getLeader(), null, false, 0, false, 0, battlePlan.getEffectiveWeapon(), null));
        battlePlan.revealOpponentBattlePlan(new BattlePlan(opponentBattlePlan.getLeader(), null, false, 0, false, 0, opponentBattlePlan.getEffectiveWeapon(), null));
        assertEquals("Weapon: Poison Tooth (not used)", opponentBattlePlan.getWeaponString());
        assertTrue(opponentBattlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testRestorePoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, snooper);
        battlePlan.revokePoisonTooth();
        battlePlan.restorePoisonTooth();
        assertEquals("Weapon: Poison Tooth", battlePlan.getWeaponString());
        assertFalse(battlePlan.isInactivePoisonTooth());
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testRevokePoisonToothWithWeirdingWay() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, poisonTooth, weirdingWay);
        assertTrue(battlePlan.revokePoisonTooth());
        assertEquals("Weirding Way", battlePlan.getDefense().name());
        assertNotNull(battlePlan.getDefense());
    }

    @Test
    void testPortableSnooperFalse() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, snooper);
        assertFalse(battlePlan.addPortableSnooper());
    }

    @Test
    void testPortableSnooperTrue() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, null);
        BattlePlan opponentBattlePlan = new BattlePlan(zoal, null, false, 0, false, 0, chaumas, null);
        battlePlan.revealOpponentBattlePlan(new BattlePlan(opponentBattlePlan.getLeader(), null, false, 0, false, 0, opponentBattlePlan.getEffectiveWeapon(), null));
        opponentBattlePlan.revealOpponentBattlePlan(new BattlePlan(battlePlan.getLeader(), null, false, 0, false, 0, battlePlan.getEffectiveWeapon(), null));
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testPortableSnooperTrueThenRemove() {
        BattlePlan battlePlan = new BattlePlan(duncanIdaho, null, false, 0, false, 0, null, null);
        BattlePlan opponentBattlePlan = new BattlePlan(zoal, null, false, 0, false, 0, chaumas, null);
        battlePlan.revealOpponentBattlePlan(new BattlePlan(opponentBattlePlan.getLeader(), null, false, 0, false, 0, opponentBattlePlan.getEffectiveWeapon(), null));
        opponentBattlePlan.revealOpponentBattlePlan(new BattlePlan(battlePlan.getLeader(), null, false, 0, false, 0, battlePlan.getEffectiveWeapon(), null));
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
        battlePlan.removePortableSnooper();
        assertFalse(battlePlan.isLeaderAlive());
    }

    @AfterEach
    void tearDown() {
    }
}
