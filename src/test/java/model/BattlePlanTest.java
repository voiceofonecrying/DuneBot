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
    BattlePlan mirrorWeaponPlan;
    BattlePlan shieldPlan;

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
        emptyBattlePlan = new BattlePlan(false,null, null, false, null, null, 0, false, 0, 0, 0);
        crysknifePlan = new BattlePlan(false,null, null, false, crysknife, null, 0, false, 0, 0,0);
        duncanCrysknifePlan = new BattlePlan(false,duncanIdaho, null, false, crysknife, null, 0, false, 0, 0, 0);
        chaumasPlan = new BattlePlan(false,null, null, false, chaumas, null, 0, false, 0, 0, 0);
        lasgunPlan = new BattlePlan(false,null, null, false, lasgun, null, 0, false, 0, 0, 0);
        artilleryStrikePlan = new BattlePlan(false,null, null, false, artilleryStrike, null, 0, false, 0, 0, 0);
        poisonToothPlan = new BattlePlan(false,null, null, false, poisonTooth, null, 0, false, 0, 0, 0);
        poisonBladePlan = new BattlePlan(false,null, null, false, poisonBlade, null, 0, false, 0, 0, 0);
        mirrorWeaponPlan = new BattlePlan(false,null, null, false, mirrorWeapon, null, 0, false, 0, 0, 0);
        shieldPlan = new BattlePlan(false,null, null, false, null, shield, 0, false, 0, 0, 0);
    }

    @Test
    void testLeaderSurvivesNoWeapon() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.getLeaderContribution());
    }

    @Test
    void testLeaderDiesWithWrongDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesWithKH() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, true, null, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(4, battlePlan.getLeaderContribution());
    }

    @Test
    void testKHWithCheapHero() {
        BattlePlan battlePlan = new BattlePlan(true, null, cheapHero, true, null, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertEquals(2, battlePlan.getLeaderContribution());
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesArtilleryStrikeWithWrongDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesWthOwnArtilleryStrikeNoDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, artilleryStrike, null, 0, false, 0, 0,0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesArtilleryStrikeWithCorrectDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayDoesNotProtectAgainstArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, weirdingWay, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperDoesProtectAgainstArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesFromTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, artilleryStrike, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesTheirOwnArtilleryStrike() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, artilleryStrike, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals(2, battlePlan.getLeaderValue());
    }

    @Test
    void testLeaderDiesPoisonToothWithSnooper() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesInactivatedPoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        poisonToothPlan.revokePoisonTooth();
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testLeaderDiesWithOwnPoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0, 0,0);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesOwnInactivePoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0, 0, 0);
        battlePlan.revokePoisonTooth();
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testLeaderDiesWithOwnPoisonToothNoDefense() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0, 0,0);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesPoisonToothWithChemistry() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, chemistry, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasNoValue() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, null, null, 0, false, 0, 0, 0);
        assertEquals(0, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString());
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasOpponentLeaderValue() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, null, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(duncanCrysknifePlan);
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString());
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, lasgun, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(duncanCrysknifePlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithOpponentShield() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, lasgun, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testOpponentLasgunWithShield() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, null, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunNoShield() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, lasgun, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testShieldNoLasgun() {
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, null, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testPoisonBladeAgainstShield() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstSnooper() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstShieldSnooper() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstProjectileWeapon() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstPoisonWeapon() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayAgainstLasgun() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, weirdingWay, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testWinnersWeaponNotDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, crysknife, null, 0, false, 0, 0, 0);
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWinnersDefenseNotDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, kulon, chaumas, 0, false, 0, 0, 0);
        assertFalse(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testArtilleryStrikeMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, artilleryStrike, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testMirrorWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, mirrorWeapon, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothNotDiscardedIfNotUsed() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0, 0, 0);
        battlePlan.revokePoisonTooth();
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPortableSnooperMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0, 0, 0);
        battlePlan.addPortableSnooper();
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testStoneBurnerMustBeDisarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, stoneBurner, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testHarassAndWithdrawMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, harassAndWithdraw, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testReinforcementsMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, kulon, reinforcements, 0, false, 0, 0, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testWorthlessWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, kulon, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWorthlessDefenseMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, kulon, baliset, 0, false, 0, 0, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testLosersWeaponMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, crysknife, null, 0, false, 0, 0, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(true));
    }

    @Test
    void testLosersDefenseMustBeDiscarded() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, kulon, snooper, 0, false, 0, 0, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(true));
    }

    @Test
    void testRevokePoisonToothFalse() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, artilleryStrike, snooper, 0, false, 0, 0, 0);
        assertFalse(battlePlan.revokePoisonTooth());
    }

    @Test
    void testRevokePoisonToothTrue() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0, 0, 0);
        assertTrue(battlePlan.revokePoisonTooth());
        assertNotNull(battlePlan.getWeapon());
        assertEquals("Weapon: Poison Tooth (not used)", battlePlan.getWeaponString());
        assertTrue(battlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testOpponentRevokesPoisonTooth() {
        BattlePlan opponentBattlePlan = new BattlePlan(false, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0, 0, 0);
        BattlePlan battlePlan = new BattlePlan(true, zoal, null, false, null, null, 0, false, 0, 0, 0);
        assertTrue(opponentBattlePlan.revokePoisonTooth());
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        assertEquals("Weapon: Poison Tooth (not used)", opponentBattlePlan.getWeaponString());
        assertTrue(opponentBattlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testRestorePoisonTooth() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0, 0, 0);
        battlePlan.revokePoisonTooth();
        battlePlan.restorePoisonTooth();
        assertEquals("Weapon: Poison Tooth", battlePlan.getWeaponString());
        assertFalse(battlePlan.isInactivePoisonTooth());
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testRevokePoisonToothWithWeirdingWay() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonTooth, weirdingWay, 0, false, 0, 0, 0);
        assertTrue(battlePlan.revokePoisonTooth());
        assertEquals("Weirding Way", battlePlan.getDefense().name());
        assertNotNull(battlePlan.getDefense());
    }

    @Test
    void testPortableSnooperFalse() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, snooper, 0, false, 0, 0, 0);
        assertFalse(battlePlan.addPortableSnooper());
    }

    @Test
    void testPortableSnooperTrue() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, null, 0, false, 0, 0, 0);
        BattlePlan opponentBattlePlan = new BattlePlan(false, zoal, null, false, chaumas, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testPortableSnooperTrueThenRemove() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, null, 0, false, 0, 0, 0);
        BattlePlan opponentBattlePlan = new BattlePlan(false, zoal, null, false, chaumas, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
        battlePlan.removePortableSnooper();
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponNoWeapon() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, null, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithWeapon() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, crysknife, null, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithWeaponDefended() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, crysknife, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithPoisonBlade() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonBlade, shield, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithPoisonBladeDefended() {
        BattlePlan battlePlan = new BattlePlan(true, duncanIdaho, null, false, poisonBlade, shieldSnooper, 0, false, 0, 0, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @AfterEach
    void tearDown() {
    }
}
