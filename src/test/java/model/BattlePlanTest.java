package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePlanTest extends DuneTest {
    Battle battle;
    Battle carthagBattle;
    TreacheryCard artilleryStrike;
    TreacheryCard weirdingWay;
    TreacheryCard chemistry;
    TreacheryCard mirrorWeapon;
    TreacheryCard poisonBlade;
    TreacheryCard poisonTooth;
    TreacheryCard shieldSnooper;
    TreacheryCard stoneBurner;
    BattlePlan emptyBattlePlan;
    BattlePlan kulonWeaponBattlePlan;
    BattlePlan crysknifePlan;
    BattlePlan chaumasPlan;
    BattlePlan lasgunPlan;
    BattlePlan artilleryStrikePlan;
    BattlePlan poisonToothPlan;
    BattlePlan poisonBladePlan;
    BattlePlan mirrorWeaponPlan;
    BattlePlan shieldPlan;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.addFaction(atreides);
        game.addFaction(bt);
        game.addFaction(emperor);
        game.addFaction(harkonnen);
        funeralPlain.addForces("Atreides", 11);
        battle = new Battle(game, List.of(funeralPlain), List.of(atreides, harkonnen));
        carthagBattle = new Battle(game, List.of(game.getTerritory("Carthag")), List.of(atreides, harkonnen));
//        arrakeenBattle = new Battle(game, "Arrakeen", List.of(game.getTerritory("Arrakeen")), List.of(atreides, harkonnen), null);
        artilleryStrike = new TreacheryCard("Artillery Strike");
        weirdingWay = new TreacheryCard("Weirding Way");
        chemistry = new TreacheryCard("Chemistry");
        mirrorWeapon = new TreacheryCard("Mirror Weapon");
        poisonBlade = new TreacheryCard("Poison Blade");
        poisonTooth = new TreacheryCard("Poison Tooth");
        shieldSnooper = new TreacheryCard("Shield Snooper");
        stoneBurner = new TreacheryCard("Stone Burner");

        atreides.addTreacheryCard(chaumas);
        atreides.addTreacheryCard(poisonTooth);
        atreides.addTreacheryCard(shield);
        bt.addTreacheryCard(harassAndWithdraw);
        bt.addTreacheryCard(snooper);
        harkonnen.addTreacheryCard(kulon);
        harkonnen.addTreacheryCard(crysknife);
        harkonnen.addTreacheryCard(lasgun);
        harkonnen.addTreacheryCard(artilleryStrike);
        harkonnen.addTreacheryCard(poisonBlade);
        harkonnen.addTreacheryCard(mirrorWeapon);
        harkonnen.addTreacheryCard(shield);

        emptyBattlePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, null, null, 0, false, 0);
        kulonWeaponBattlePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, kulon, null, 0, false, 0);
        crysknifePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, crysknife, null, 0, false, 0);
        chaumasPlan = new BattlePlan(game, battle, atreides, false, duncanIdaho, null, false, chaumas, null, 0, false, 0);
        lasgunPlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, lasgun, null, 0, false, 0);
        artilleryStrikePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, artilleryStrike, null, 0, false, 0);
        poisonToothPlan = new BattlePlan(game, battle, atreides, false, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        poisonBladePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, poisonBlade, null, 0, false, 0);
        mirrorWeaponPlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, mirrorWeapon, null, 0, false, 0);
        shieldPlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, null, shield, 0, false, 0);

        // The battle plans created for tests just above will populate atreidesChat and would interfere with testing messages in tests below
        atreidesChat.clear();
    }

    @Test
    void testLeaderIsAvailable() {
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0));
    }

    @Test
    void testCannotPlayBothLeaderAndCheapHero() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, cheapHero, false, null, null, 0, false, 0));
    }

    @Test
    void testLeaderInTanks() {
        atreides.removeLeader(duncanIdaho.getName());
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0));
    }

    @Test
    void testCheapHeroIsValid() {
        harkonnen.addTreacheryCard(cheapHero);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, harkonnen, true, null, cheapHero, false, null, null, 0, false, 0));
    }

    @Test
    void testDoesntHaveCheapHero() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, harkonnen, true, null, cheapHero, false, null, null, 0, false, 0));
    }

    @Test
    void testNoLeaderOrCheapHeroIsInvalid() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, null, null, false, null, null, 0, false, 0));
    }

    @Test
    void testAllLeadersInTanks() {
        game.killLeader(atreides, "Lady Jessica");
        game.killLeader(atreides, "Thufir Hawat");
        game.killLeader(atreides, "Gurney Halleck");
        game.killLeader(atreides, "Dr. Yueh");
        game.killLeader(atreides, "Duncan Idaho");

        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, null, null, false, null, null, 0, false, 0));
    }

    @Test
    void testNoLeaderValidWithActiveLeadersInPreviousBattles() {
        game.killLeader(atreides, "Lady Jessica");
        game.killLeader(atreides, "Thufir Hawat");
        game.killLeader(atreides, "Gurney Halleck");
        game.killLeader(atreides, "Dr. Yueh");
        assertEquals(1, atreides.getLeaders().size());
        duncanIdaho.setBattleTerritoryName("Tuek's Sietch");

        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, null, null, false, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanOtherFactionLeaderInvalid() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, harkonnen, true, duncanIdaho, null, false, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanHarkonnenCapturedLeader() {
        harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
        assertDoesNotThrow(() -> new BattlePlan(game, battle, harkonnen, true, duncanIdaho, null, false, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanChoamDoesNotSupportAlly() {
        game.addFaction(choam);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.addFaction(atreides);
        game.createAlliance(choam, atreides);
        choam.setSpiceForAlly(1);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testBattlePlanChoamSupportsAlly() {
        game.addFaction(choam);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.addFaction(atreides);
        game.createAlliance(choam, atreides);
        choam.setSpiceForAlly(1);
        choam.setAllySpiceForBattle(true);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testBattlePlanEmperorSupportsAlly() {
        game.addFaction(emperor);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.addFaction(atreides);
        game.createAlliance(emperor, atreides);
        emperor.setSpiceForAlly(1);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testWeirdingWayIsValidDefense () {
        TreacheryCard weirdingWay = new TreacheryCard("Weirding Way");
        atreides.addTreacheryCard(weirdingWay);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, weirdingWay, 0, false, 0));
    }

    @Test
    void testWeirdingWayIsInvalidDefense() {
        TreacheryCard weirdingWay = new TreacheryCard("Weirding Way");
        atreides.addTreacheryCard(weirdingWay);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, weirdingWay, 0, false, 0));
    }

    @Test
    void testChemistryIsValidWeapon() {
        TreacheryCard chemistry = new TreacheryCard("Chemistry");
        atreides.addTreacheryCard(chemistry);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chemistry, shield, 0, false, 0));
    }

    @Test
    void testChemistryIsInvalidWeapon() {
        TreacheryCard chemistry = new TreacheryCard("Chemistry");
        atreides.addTreacheryCard(chemistry);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chemistry, null, 0, false, 0));
    }

    @Test
    void testFactionHasWeapon() {
        atreides.addTreacheryCard(crysknife);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, null, 0, false, 0));
    }

    @Test
    void testDoesntHaveWeapon() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, null, 0, false, 0));
    }

    @Test
    void testFactionHasDefense() {
        atreides.addTreacheryCard(snooper);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, snooper, 0, false, 0));
    }

    @Test
    void testDoesntHaveDefense() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, snooper, 0, false, 0));
    }

    @Test
    void testLeaderSurvivesNoWeapon() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.getLeaderContribution());
    }

    @Test
    void testLeaderSurvivesAgainstWorthless() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(kulonWeaponBattlePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.getLeaderContribution());
    }

    @Test
    void testLeaderDiesWithWrongDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesWithCorrectDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testTraitorLeaderDies() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        battlePlan.setLeaderIsTraitor(true);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testBattlePlanAtreidesHasKH() {
        atreides.setForcesLost(7);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, true, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanKHInTanks() {
        atreides.setForcesLost(7);
        game.killLeader(atreides, "Kwisatz Haderach");
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, true, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanAtreidesDoesNotHaveKH() {
        atreides.setForcesLost(6);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, true, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanNoLeaderWithCheapHeroAndKH() {
        atreides.addTreacheryCard(cheapHero);
        atreides.setForcesLost(7);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, null, cheapHero, true, null, null, 0, false, 0));
    }

    @Test
    void testKHRequiresLeaderOrCheapHero() {
        atreides.getLeaders().removeAll(atreides.getLeaders());
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, null, null, true, null, null, 0, false, 0));
    }

    @Test
    void testHarkonnenCannotPlayKH() {
        harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, harkonnen, true, duncanIdaho, null, true, null, null, 0, false, 0));
    }

    @Test
    void testFactionIsNotInTheBattle() {
        emperor.addTreacheryCard(cheapHero);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, emperor, true, null, cheapHero, false, null, null, 0, false, 0));
    }

    @Test
    void testFactionDoesNotHaveEnoughSpice() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testBattlePlanSpendingTooMuch() {
        atreidesChat.clear();
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 1, false, 5));
        assertEquals("This dial can be supported with 1 " + Emojis.SPICE + ", reducing from 5.", atreidesChat.getMessages().getFirst());
    }

    @Test
    void testBattlePlanNotEnoughTroops() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 10));
    }

    @Test
    void testLeaderSurvivesWithKH() throws InvalidGameStateException {
        atreides.setForcesLost(7);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, true, null, null, 0, false, 0);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(4, battlePlan.getLeaderContribution());
    }

    @Test
    void testKHWithCheapHero() throws InvalidGameStateException {
        atreides.addTreacheryCard(cheapHero);
        atreides.setForcesLost(7);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, null, cheapHero, true, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertEquals(2, battlePlan.getLeaderContribution());
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals("Leader: Cheap Heroine (0) + KH (2)", battlePlan.getLeaderString(false));
    }

    @Test
    void testLeaderDiesArtilleryStrikeWithWrongDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, zoal, null, false, null, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesWthOwnArtilleryStrikeNoDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, artilleryStrike, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesArtilleryStrikeWithCorrectDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayDoesNotProtectAgainstArtilleryStrike() throws InvalidGameStateException {
        atreides.addTreacheryCard(weirdingWay);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, weirdingWay, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperDoesProtectAgainstArtilleryStrike() throws InvalidGameStateException {
        atreides.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(artilleryStrikePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.getLeaderContribution());
        assertEquals(2, battlePlan.getLeaderValue());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderDiesFromTheirOwnArtilleryStrike() throws InvalidGameStateException {
        harkonnen.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, artilleryStrike, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesTheirOwnArtilleryStrike() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, artilleryStrike, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
        assertEquals(1, battlePlan.getLeaderValue());
    }

    @Test
    void testLeaderDiesPoisonToothWithSnooper() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesInactivatedPoisonTooth() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, snooper, 0, false, 0);
        poisonToothPlan.revokePoisonTooth();
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testLeaderDiesWithOwnPoisonTooth() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesOwnInactivePoisonTooth() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0);
        battlePlan.revokePoisonTooth();
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testLeaderDiesWithOwnPoisonToothNoDefense() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testLeaderSurvivesPoisonToothWithChemistry() throws InvalidGameStateException {
        atreides.addTreacheryCard(chemistry);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, chemistry, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasNoValue() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, zoal, null, false, null, null, 0, false, 0);
        assertEquals(0, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString(false));
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testZoalHasOpponentLeaderValue() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, zoal, null, false, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertEquals(1, battlePlan.getLeaderValue());
        assertEquals("Leader: Zoal (X)", battlePlan.getLeaderString(false));
        assertEquals("Zoal", battlePlan.getKilledLeaderString());
        assertEquals(1, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithShield() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, lasgun, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunWithOpponentShield() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, lasgun, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testOpponentLasgunWithShield() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testLasgunNoShield() throws InvalidGameStateException {
        harkonnen.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, lasgun, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testShieldNoLasgun() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(emptyBattlePlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
    }

    @Test
    void testPoisonBladeAgainstShield() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstSnooper() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testPoisonBladeAgainstShieldSnooper() throws InvalidGameStateException {
        atreides.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstProjectileWeapon() throws InvalidGameStateException {
        atreides.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(crysknifePlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstPoisonWeapon() throws InvalidGameStateException {
        atreides.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertTrue(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testShieldSnooperAgainstLasgun() throws InvalidGameStateException {
        atreides.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertTrue(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(0, battlePlan.combatWater());
    }

    @Test
    void testWeirdingWayAgainstLasgun() throws InvalidGameStateException {
        atreides.addTreacheryCard(weirdingWay);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, weirdingWay, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(lasgunPlan);
        assertFalse(battlePlan.isLasgunShieldExplosion());
        assertFalse(battlePlan.isLeaderAlive());
        assertEquals(2, battlePlan.combatWater());
    }

    @Test
    void testWinnersWeaponNotDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, crysknife, null, 0, false, 0);
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWinnersDefenseNotDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        assertFalse(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testArtilleryStrikeMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, artilleryStrike, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testMirrorWeaponMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, mirrorWeapon, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testPoisonToothNotDiscardedIfNotUsed() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        battlePlan.revokePoisonTooth();
        assertFalse(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testLoserMustDiscardPoisonToothEventIfNotUsed() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        battlePlan.revokePoisonTooth();
        assertTrue(battlePlan.weaponMustBeDiscarded(true));
    }

    @Test
    void testPortableSnooperMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, null, 0, false, 0);
        battlePlan.addPortableSnooper();
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testStoneBurnerMustBeDisarded() throws InvalidGameStateException {
        atreides.addTreacheryCard(stoneBurner);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, stoneBurner, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testHarassAndWithdrawMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, zoal, null, false, harassAndWithdraw, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testHarassAndWithdrawWeaponNotOnHomeworld() {
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, bt, true, zoal, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawWeaponNotOnSecondHomeworld() {
        emperor.addTreacheryCard(harassAndWithdraw);
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, emperor, true, bashar, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawOnHomeworldWithPlanetologist() throws InvalidGameStateException {
        zoal.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        zoal.setSkillCard(planetologist);
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen));
        assertDoesNotThrow(() -> new BattlePlan(game, battle, bt, true, zoal, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawOnSecondHomeworldWithPlanetologist() throws InvalidGameStateException {
        emperor.addTreacheryCard(harassAndWithdraw);
        bashar.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        bashar.setSkillCard(planetologist);
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen));
        assertDoesNotThrow(() -> new BattlePlan(game, battle, emperor, true, bashar, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawDefenseNotOnHomeworld() {
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, bt, true, zoal, null, false, null, harassAndWithdraw, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawDefenseNotOnSecondHomeworld() {
        emperor.addTreacheryCard(harassAndWithdraw);
        battle = new Battle(game, List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, emperor, true, bashar, null, false, null, harassAndWithdraw, 0, false, 0));
    }

    @Test
    void testReinforcementsMustBeDiscarded() throws InvalidGameStateException {
        bt.addTreacheryCard(reinforcements);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, reinforcements, 0, false, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testReinforcementsWeaponRequires3Reserves() {
        atreides.addTreacheryCard(reinforcements);
        game.addFaction(atreides);
        atreides.removeReserves(8);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, reinforcements, null, 0, false, 0));
    }

    @Test
    void testReinforcementsWeaponCanBePlayedWithPlanetologist() throws InvalidGameStateException {
        atreides.addTreacheryCard(reinforcements);
        game.addFaction(atreides);
        atreides.removeReserves(8);
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        duncanIdaho.setSkillCard(planetologist);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, reinforcements, null, 0, false, 0));
    }

    @Test
    void testReinforcementsDefenseRequires3Reserves() {
        atreides.addTreacheryCard(reinforcements);
        game.addFaction(atreides);
        atreides.removeReserves(8);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, reinforcements, 0, false, 0));
    }

    @Test
    void testHajrAsWeaponOnlyWithPlanetologist() throws InvalidGameStateException {
        TreacheryCard hajr = new TreacheryCard("Hajr");
        atreides.addTreacheryCard(hajr);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, hajr, null, 0, false, 0));
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        duncanIdaho.setSkillCard(planetologist);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, hajr, null, 0, false, 0));
    }

    @Test
    void testTruthtranceAsWeaponOnlyWithPlanetologist() throws InvalidGameStateException {
        TreacheryCard truthtrance = new TreacheryCard("Truthtrance");
        atreides.addTreacheryCard(truthtrance);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, truthtrance, null, 0, false, 0));
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        duncanIdaho.setSkillCard(planetologist);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, truthtrance, null, 0, false, 0));
    }

    @Test
    void testWorthlessWeaponMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, kulon, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(false));
    }

    @Test
    void testWorthlessDefenseMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, null, kulon, 0, false, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(false));
    }

    @Test
    void testLosersWeaponMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, crysknife, null, 0, false, 0);
        assertTrue(battlePlan.weaponMustBeDiscarded(true));
    }

    @Test
    void testLosersDefenseMustBeDiscarded() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        assertTrue(battlePlan.defenseMustBeDiscarded(true));
    }

    @Test
    void testRevokePoisonToothFalse() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        assertFalse(battlePlan.revokePoisonTooth());
    }

    @Test
    void testRevokePoisonToothTrue() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0);
        assertTrue(battlePlan.revokePoisonTooth());
        assertNotNull(battlePlan.getWeapon());
        assertEquals("Weapon: ~~Poison Tooth~~ (not used)", battlePlan.getWeaponString());
        assertTrue(battlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testOpponentRevokesPoisonTooth() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan opponentBattlePlan = new BattlePlan(game, battle, atreides, false, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0);
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, null, null, 0, false, 0);
        assertTrue(opponentBattlePlan.revokePoisonTooth());
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        assertEquals("Weapon: ~~Poison Tooth~~ (not used)", opponentBattlePlan.getWeaponString());
        assertTrue(opponentBattlePlan.isInactivePoisonTooth());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testRestorePoisonTooth() throws InvalidGameStateException {
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, snooper, 0, false, 0);
        battlePlan.revokePoisonTooth();
        battlePlan.restorePoisonTooth();
        assertEquals("Weapon: Poison Tooth", battlePlan.getWeaponString());
        assertFalse(battlePlan.isInactivePoisonTooth());
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testRevokePoisonToothWithWeirdingWay() throws InvalidGameStateException {
        atreides.addTreacheryCard(weirdingWay);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, poisonTooth, weirdingWay, 0, false, 0);
        assertTrue(battlePlan.revokePoisonTooth());
        assertEquals("Weirding Way", battlePlan.getDefense().name());
        assertNotNull(battlePlan.getDefense());
    }

    @Test
    void testPortableSnooperFalse() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        assertFalse(battlePlan.addPortableSnooper());
    }

    @Test
    void testPortableSnooperTrue() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, null, null, 0, false, 0);
        BattlePlan opponentBattlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, chaumas, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testPortableSnooperTrueThenRemove() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, false, ummanKudu, null, false, null, null, 0, false, 0);
        BattlePlan opponentBattlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(opponentBattlePlan);
        opponentBattlePlan.revealOpponentBattlePlan(battlePlan);
        assertFalse(battlePlan.isLeaderAlive());
        assertTrue(battlePlan.addPortableSnooper());
        assertTrue(battlePlan.isLeaderAlive());
        battlePlan.removePortableSnooper();
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponNoWeapon() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithWeapon() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, crysknife, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithWeaponDefended() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, crysknife, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithPoisonBlade() throws InvalidGameStateException {
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, poisonBlade, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testMirrorWeaponWithPoisonBladeDefended() throws InvalidGameStateException {
        harkonnen.addTreacheryCard(shieldSnooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, harkonnen, true, ummanKudu, null, false, poisonBlade, shieldSnooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(mirrorWeaponPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testShieldAndCarthagCardAgainstPoisonWeapon() throws InvalidGameStateException {
        game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        atreides.addStrongholdCard(new StrongholdCard("Carthag"));
        BattlePlan battlePlan = new BattlePlan(game, carthagBattle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testShieldAndCarthagCardAgainstPoisonTooth() throws InvalidGameStateException {
        game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        atreides.addStrongholdCard(new StrongholdCard("Carthag"));
        BattlePlan battlePlan = new BattlePlan(game, carthagBattle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonToothPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testShieldAndCarthagCardAgainstPoisonBlade() throws InvalidGameStateException {
        game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        atreides.addStrongholdCard(new StrongholdCard("Carthag"));
        BattlePlan battlePlan = new BattlePlan(game, carthagBattle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertTrue(battlePlan.isLeaderAlive());
    }

    @Test
    void testShieldAndCarthagCardPlayingPoisonWeapon() throws InvalidGameStateException {
        game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        atreides.addStrongholdCard(new StrongholdCard("Carthag"));
        BattlePlan battlePlan = new BattlePlan(game, carthagBattle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testShieldAndCarthagCardPlayingMirrorWeapon() throws InvalidGameStateException {
        game.addGameOption(GameOption.STRONGHOLD_SKILLS);
        harkonnen.addStrongholdCard(new StrongholdCard("Carthag"));
        BattlePlan battlePlan = new BattlePlan(game, carthagBattle, harkonnen, true, ummanKudu, null, false, mirrorWeapon, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(chaumasPlan);
        assertFalse(battlePlan.isLeaderAlive());
    }

    @Test
    void testDiplomatInFrontWorthlessDefense() throws InvalidGameStateException {
        LeaderSkillCard diplomat = new LeaderSkillCard("Diplomat");
        wykk.setSkillCard(diplomat);
        wykk.setPulledBehindShield(false);
        bt.addTreacheryCard(baliset);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, baliset, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertTrue(battlePlan.isProjectileDefense());
    }

    @Test
    void testDiplomatInFrontNoDefenseWorthlessWeapon() throws InvalidGameStateException {
        LeaderSkillCard diplomat = new LeaderSkillCard("Diplomat");
        wykk.setSkillCard(diplomat);
        wykk.setPulledBehindShield(false);
        bt.addTreacheryCard(baliset);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, baliset, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertTrue(battlePlan.isProjectileDefense());
    }

    @Test
    void testDiplomatInFrontHasDefenseWorthlessWeapon() throws InvalidGameStateException {
        LeaderSkillCard diplomat = new LeaderSkillCard("Diplomat");
        wykk.setSkillCard(diplomat);
        wykk.setPulledBehindShield(false);
        bt.addTreacheryCard(baliset);
        bt.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, baliset, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertFalse(battlePlan.isProjectileDefense());
    }

    @Test
    void testDiplomatInFrontNoDefenseNoWeapon() throws InvalidGameStateException {
        LeaderSkillCard diplomat = new LeaderSkillCard("Diplomat");
        wykk.setSkillCard(diplomat);
        wykk.setPulledBehindShield(false);
        bt.addTreacheryCard(baliset);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, null, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(shieldPlan);
        assertFalse(battlePlan.isProjectileDefense());
    }

    @Test
    void testKillerMedicInFrontPoisonDefense() throws InvalidGameStateException {
        LeaderSkillCard killerMedic = new LeaderSkillCard("Killer Medic");
        wykk.setSkillCard(killerMedic);
        wykk.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, wykk, null, false, null, snooper, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testKillerMedicInFrontLeaderDies() throws InvalidGameStateException {
        LeaderSkillCard killerMedic = new LeaderSkillCard("Killer Medic");
        zoal.setSkillCard(killerMedic);
        zoal.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, bt, true, zoal, null, false, null, snooper, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertEquals(0, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testKillerMedicInFrontNoDefense() throws InvalidGameStateException {
        LeaderSkillCard killerMedic = new LeaderSkillCard("Killer Medic");
        duncanIdaho.setSkillCard(killerMedic);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testKillerMedicInBattlePoisonDefense() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard killerMedic = new LeaderSkillCard("Killer Medic");
        duncanIdaho.setSkillCard(killerMedic);
        atreides.addTreacheryCard(snooper);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, snooper, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testKillerMedicInBattleNoDefense() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard killerMedic = new LeaderSkillCard("Killer Medic");
        duncanIdaho.setSkillCard(killerMedic);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMasterOfAssassinsInFrontPoisonWeapon() throws InvalidGameStateException {
        LeaderSkillCard masterOfAssassins = new LeaderSkillCard("Master of Assassins");
        duncanIdaho.setSkillCard(masterOfAssassins);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMasterOfAssassinsInFrontLeaderDies() throws InvalidGameStateException {
        LeaderSkillCard masterOfAssassins = new LeaderSkillCard("Master of Assassins");
        duncanIdaho.setSkillCard(masterOfAssassins);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertEquals(0, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMasterOfAssassinsInFrontNoWeapon() throws InvalidGameStateException {
        LeaderSkillCard masterOfAssassins = new LeaderSkillCard("Master of Assassins");
        duncanIdaho.setSkillCard(masterOfAssassins);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMasterOfAssassinsInBattlePoisonWeapon() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard masterOfAssassins = new LeaderSkillCard("Master of Assassins");
        duncanIdaho.setSkillCard(masterOfAssassins);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMasterOfAssassinsInBattleNoWeapon() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard masterOfAssassins = new LeaderSkillCard("Master of Assassins");
        duncanIdaho.setSkillCard(masterOfAssassins);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, shield, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMentatInFront() throws InvalidGameStateException {
        LeaderSkillCard mentat = new LeaderSkillCard("Mentat");
        duncanIdaho.setSkillCard(mentat);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testMentatInBattle() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard mentat = new LeaderSkillCard("Mentat");
        duncanIdaho.setSkillCard(mentat);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(8, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testPranaBinduAdeptInFrontProjectileDefense() throws InvalidGameStateException {
        LeaderSkillCard pranaBinduAdept = new LeaderSkillCard("Prana Bindu Adept");
        duncanIdaho.setSkillCard(pranaBinduAdept);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testPranaBinduAdeptInFrontLeaderDies() throws InvalidGameStateException {
        LeaderSkillCard pranaBinduAdept = new LeaderSkillCard("Prana Bindu Adept");
        duncanIdaho.setSkillCard(pranaBinduAdept);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertEquals(0, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testPranaBinduAdeptInFrontNoDefense() throws InvalidGameStateException {
        LeaderSkillCard pranaBinduAdept = new LeaderSkillCard("Prana Bindu Adept");
        duncanIdaho.setSkillCard(pranaBinduAdept);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testPranaBinduAdeptInBattleProjectileDefense() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard pranaBinduAdept = new LeaderSkillCard("Prana Bindu Adept");
        duncanIdaho.setSkillCard(pranaBinduAdept);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, shield, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testPranaBinduAdeptInBattleNoDefense() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard pranaBinduAdept = new LeaderSkillCard("Prana Bindu Adept");
        duncanIdaho.setSkillCard(pranaBinduAdept);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSpiceBankerInBattle() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard spiceBanker = new LeaderSkillCard("Spice Banker");
        duncanIdaho.setSkillCard(spiceBanker);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        battlePlan.setSpiceBankerSupport(3);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInFrontProjectileWeapon() throws InvalidGameStateException {
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        duncanIdaho.setPulledBehindShield(false);
        atreides.discard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, shield, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInFrontLeaderDies() throws InvalidGameStateException {
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        duncanIdaho.setPulledBehindShield(false);
        atreides.discard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, shield, 0, false, 0);
        battlePlan.revealOpponentBattlePlan(poisonBladePlan);
        assertEquals(0, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInFrontNoWeapon() throws InvalidGameStateException {
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInBattleProjectileWeapon() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        atreides.discard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, shield, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInBattleNoWeapon() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        atreides.discard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInFrontWorthlessWeapon() throws InvalidGameStateException {
        atreides.addTreacheryCard(baliset);
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, baliset, shield, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInFrontWorthlessDefense() throws InvalidGameStateException {
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        duncanIdaho.setPulledBehindShield(false);
        atreides.addTreacheryCard(baliset);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, baliset, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInFrontNoWorthless() throws InvalidGameStateException {
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        duncanIdaho.setPulledBehindShield(false);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInBattleWorthlessWeapon() throws InvalidGameStateException {
        atreides.addTreacheryCard(baliset);
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, baliset, shield, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInBattleWorthlessDefense() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        atreides.addTreacheryCard(baliset);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chaumas, baliset, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testWarmasterInBattleNoWorthless() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard warmaster = new LeaderSkillCard("Warmaster");
        duncanIdaho.setSkillCard(warmaster);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
        assertEquals(4, battlePlan.getDoubleBattleStrength());
    }

    @Nested
    @DisplayName("#ecazAllyForcesRemaining")
    class EcazAllyForcesRemaining {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(ecaz);
            game.addFaction(emperor);
        }

        @Test
        void testForcesRemainingEcazNotAllied() throws InvalidGameStateException {
            redChasm.addForces("Harkonnen", 10);
            redChasm.addForces("Ecaz", 3);
            battle = new Battle(game, List.of(redChasm), List.of(harkonnen, ecaz));
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            BattlePlan bp = new BattlePlan(game, battle, ecaz, false, sanyaEcaz, null, false, null, null, 0, false, 0);
            assertEquals("This will leave 3 " + Emojis.ECAZ_TROOP + " in Red Chasm if you win.", bp.getForcesRemainingString());
        }

        @Test
        void testEcazAllyNoForcesLeft() throws InvalidGameStateException {
            emperor.setAlly("Ecaz");
            ecaz.setAlly("Emperor");
            redChasm.addForces("Harkonnen", 10);
            redChasm.addForces("Emperor", 3);
            redChasm.addForces("Emperor*", 1);
            redChasm.addForces("Ecaz", 1);
            battle = new Battle(game, List.of(redChasm), List.of(harkonnen, emperor, ecaz));
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            BattlePlan bp = new BattlePlan(game, battle, ecaz, false, sanyaEcaz, null, false, null, null, 5, false, 5);
            assertEquals("This will leave no " + Emojis.EMPEROR + " forces 0 " + Emojis.ECAZ_TROOP + " in Red Chasm if you win.", bp.getForcesRemainingString());
        }

        @Test
        void testEcazAllyChangeForceLosses() throws InvalidGameStateException {
            TestTopic ecazChat = new TestTopic();
            ecaz.setChat(ecazChat);
            emperor.setAlly("Ecaz");
            ecaz.setAlly("Emperor");
            redChasm.addForces("Harkonnen", 10);
            redChasm.addForces("Emperor", 6);
            redChasm.addForces("Emperor*", 1);
            redChasm.addForces("Ecaz", 7);
            Battle battle = new Battle(game, List.of(redChasm), List.of(harkonnen, emperor, ecaz));
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            BattlePlan bp = new BattlePlan(game, battle, ecaz, false, sanyaEcaz, null, false, null, null, 5, false, 5);
            assertTrue(ecazChat.getMessages().getFirst().contains("How would you like to take troop losses?"));
            assertEquals("This will leave 3 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.ECAZ_TROOP + " in Red Chasm if you win.", bp.getForcesRemainingString());
            assertEquals(3, bp.getRegularDialed());
            assertEquals(1, bp.getSpecialDialed());
            bp.setForcesDialed(5, 0);
            assertEquals(5, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals(1, bp.getSpecialNotDialed());
            assertEquals("This will leave 1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " 3 " + Emojis.ECAZ_TROOP + " in Red Chasm if you win.", bp.getForcesRemainingString());
        }
    }

    @Nested
    @DisplayName("#numForcesNotDialed")
    class NumForcesNotDialed {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            fremen.setChat(new TestTopic());
        }

        @Test
        void testFremenNegateSardaukar() throws InvalidGameStateException {
            garaKulon.addForces("Emperor", 1);
            garaKulon.addForces("Emperor*", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(emperor, fremen));
            BattlePlan bp = new BattlePlan(game, battle, emperor, true, emperor.getLeader("Burseg").orElseThrow(), null, false, null, null, 2, false, 1);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(2, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testSuboidsAlwaysCountHalf() throws InvalidGameStateException {
            game.addFaction(ix);
            Battle battle = new Battle(game, List.of(hms), List.of(ix, emperor));
            BattlePlan bp = new BattlePlan(game, battle, ix, true, ix.getLeader("Tessia Vernius").orElseThrow(), null, false, null, null, 7, false, 4);
            assertEquals(2, bp.getRegularDialed());
            assertEquals(3, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testKaramadSardaukar() throws InvalidGameStateException {
            carthag.addForces("Emperor", 1);
            carthag.addForces("Emperor*", 3);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            battle.negateSpecialForces(game, emperor);
            BattlePlan bp = new BattlePlan(game, battle, emperor, true, emperor.getLeader("Burseg").orElseThrow(), null, false, null, null, 2, false, 1);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(2, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testKaramadFedaykin() throws InvalidGameStateException {
            carthag.addForces("Fremen", 1);
            carthag.addForces("Fremen*", 3);
            Battle battle = new Battle(game, List.of(carthag), List.of(fremen, harkonnen));
            battle.negateSpecialForces(game, fremen);
            BattlePlan bp = new BattlePlan(game, battle, fremen, true, fremen.getLeader("Chani").orElseThrow(), null, false, null, null, 3, false, 0);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(2, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testKaramadCyborgs() throws InvalidGameStateException {
            game.addFaction(ix);
            carthag.addForces("Ix", 2);
            carthag.addForces("Ix*", 3);
            Battle battle = new Battle(game, List.of(carthag), List.of(ix, harkonnen));
            battle.negateSpecialForces(game, ix);
            BattlePlan bp = new BattlePlan(game, battle, ix, true, ix.getLeader("Tessia Vernius").orElseThrow(), null, false, null, null, 2, false, 1);
            assertEquals(0, bp.getRegularDialed());
            assertEquals(3, bp.getSpecialDialed());
            assertEquals(2, bp.getNumForcesNotDialed());
        }

        @Test
        void testKaramadSardaukarAlliedWithEcaz() throws InvalidGameStateException {
            emperor.setAlly("Ecaz");
            ecaz.setAlly("Emperor");
            carthag.addForces("Emperor", 1);
            carthag.addForces("Emperor*", 3);
            carthag.addForces("Ecaz", 2);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen, ecaz));
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            battle.negateSpecialForces(game, emperor);
            BattlePlan bp = new BattlePlan(game, battle, emperor, false, emperor.getLeader("Burseg").orElseThrow(), null, false, null, null, 2, false, 1);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(2, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testKaramaFremenMustPaySpice() throws InvalidGameStateException {
            game.addFaction(harkonnen);
            carthag.addForces("Fremen", 1);
            carthag.addForces("Fremen*", 3);
            Battle battle = new Battle(game, List.of(carthag), List.of(fremen, harkonnen));
            battle.karamaFremenMustPay(game);
            BattlePlan bp = new BattlePlan(game, battle, fremen, true, fremen.getLeader("Chani").orElseThrow(), null, false, null, null, 3, false, 0);
            assertEquals(0, bp.getRegularDialed());
            assertEquals(3, bp.getSpecialDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
        }

        @Test
        void testNoFieldForces() {
            game.addFaction(richese);
            carthag.setRicheseNoField(3);
            Battle battle = new Battle(game, List.of(carthag), List.of(richese, harkonnen));
            assertDoesNotThrow(() -> new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 3, false, 3));
            assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 3, true, 3));
        }

        @Test
        void testNoFieldForcesFewerReserves() {
            game.addFaction(richese);
            Territory richeseHomeworld = game.getTerritory("Richese");
            assertEquals(20, richeseHomeworld.getForceStrength("Richese"));
            richeseHomeworld.removeForces(game, "Richese", 18);
            assertEquals(2, richeseHomeworld.getForceStrength("Richese"));
            carthag.setRicheseNoField(3);
            Battle battle = new Battle(game, List.of(carthag), List.of(richese, harkonnen));
            try {
                new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 3, false, 3);
                fail("Expected InvalidGameStateException was not thrown.");
            } catch (Exception e) {
                assertEquals("Richese has only 2 forces in reserves to replace the 3 No-Field", e.getMessage());
            }
            assertDoesNotThrow(() -> new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 2, false, 2));
        }

        @Test
        void testRicheseNoFieldNotYetRevealed() throws InvalidGameStateException {
            game.addFaction(richese);
            garaKulon.setRicheseNoField(5);
            garaKulon.addForces("NoField", 5);
            garaKulon.addForces("Fremen", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(fremen, richese));
            Leader ladyHelena = richese.getLeader("Lady Helena").orElseThrow();
            assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, richese, true, ladyHelena, null, false, null, null, 3, false, 0));
            BattlePlan bp = new BattlePlan(game, battle, richese, true, ladyHelena, null, false, null, null, 2, false, 0);
            assertEquals(4, bp.getRegularDialed());
            assertEquals(1, bp.getRegularNotDialed());
            assertEquals(1, bp.getNumForcesNotDialed());
            assertEquals("This will leave 1 " + Emojis.RICHESE_TROOP + " in Gara Kulon if you win.", bp.getForcesRemainingString());
        }

        @Test
        void testEmperorNexusCunning() throws InvalidGameStateException {
            emperor.setNexusCard(new NexusCard("Emperor"));
            carthag.addForces("Emperor", 7);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            battle.emperorNexusCunning(game, true);
            BattlePlan bp = new BattlePlan(game, battle, emperor, true, burseg, null, false, null, null, 12, false, 7);
            assertEquals(7, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals(0, bp.getNumForcesNotDialed());
            assertEquals(30, bp.getDoubleBattleStrength());
        }

        @Test
        void testEmperorNexusCunningLessThan5Forces() throws InvalidGameStateException {
            emperor.setNexusCard(new NexusCard("Emperor"));
            carthag.addForces("Emperor", 4);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            battle.emperorNexusCunning(game, true);
            BattlePlan bp = new BattlePlan(game, battle, emperor, true, burseg, null, false, null, null, 2, false, 1);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals(3, bp.getNumForcesNotDialed());
            assertEquals(10, bp.getDoubleBattleStrength());
        }

        @Test
        void testEmperorNexusCunningKaramadSardaukar() throws InvalidGameStateException {
            carthag.addForces("Emperor", 7);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            battle.emperorNexusCunning(game, true);
            battle.negateSpecialForces(game, emperor);
            BattlePlan bp = new BattlePlan(game, battle, emperor, true, burseg, null, false, null, null, 7, false, 7);
            assertEquals(7, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals(0, bp.getNumForcesNotDialed());
            assertEquals(20, bp.getDoubleBattleStrength());
        }

        @Test
        void testIxNexusCunning() throws InvalidGameStateException {
            game.addFaction(ix);
            carthag.addForces("Ix", 3);
            carthag.addForces("Ix*", 1);
            Battle battle = new Battle(game, List.of(carthag), List.of(ix, harkonnen));
            battle.setIxCunning(true);
            BattlePlan bp = new BattlePlan(game, battle, ix, true, cammarPilru, null, false, null, null, 2, false, 0);
            assertEquals(1, bp.getRegularDialed());
            assertEquals(1, bp.getSpecialDialed());
            assertEquals(2, bp.getNumForcesNotDialed());
            assertEquals(6, bp.getDoubleBattleStrength());
            assertEquals("How would you like to take troop losses?", ixChat.getMessages().getFirst());
            assertEquals(2, ixChat.getChoices().getFirst().size());
        }

        @Test
        void testIxNexusCunningCyborgsNegated() throws InvalidGameStateException {
            game.addFaction(ix);
            carthag.addForces("Ix", 3);
            carthag.addForces("Ix*", 1);
            Battle battle = new Battle(game, List.of(carthag), List.of(ix, harkonnen));
            battle.setIxCunning(true);
            battle.negateSpecialForces(game, ix);
            BattlePlan bp = new BattlePlan(game, battle, ix, true, cammarPilru, null, false, null, null, 2, false, 0);
            assertEquals(2, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals(2, bp.getNumForcesNotDialed());
            assertEquals(6, bp.getDoubleBattleStrength());
            assertTrue(ixChat.getChoices().isEmpty());
        }
    }

    @Nested
    @DisplayName("#getForcesDialed")
    class GetForcesDialed {
        @Test
        void testSuboidsAlwaysCountHalf() throws InvalidGameStateException {
            game.addFaction(ix);
            Battle battle = new Battle(game, List.of(hms), List.of(ix, emperor));
            BattlePlan bp = new BattlePlan(game, battle, ix, true, ix.getLeader("Tessia Vernius").orElseThrow(), null, false, null, null, 7, false, 4);
            assertEquals(2, bp.getRegularDialed());
            assertEquals(3, bp.getSpecialDialed());
        }

        @Test
        void testNoFieldForces() throws InvalidGameStateException {
            game.addFaction(richese);
            carthag.setRicheseNoField(3);
            Battle battle = new Battle(game, List.of(carthag), List.of(richese, harkonnen));
            BattlePlan bp = new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 3, false, 3);
            assertEquals(3, bp.getRegularDialed());
        }

        @Test
        void testNoFieldForcesFewerReserves() throws InvalidGameStateException {
            game.addFaction(richese);
            Territory richeseHomeworld = game.getTerritory("Richese");
            assertEquals(20, richeseHomeworld.getForceStrength("Richese"));
            richeseHomeworld.removeForces(game, "Richese", 18);
            assertEquals(2, richeseHomeworld.getForceStrength("Richese"));
            carthag.setRicheseNoField(3);
            Battle battle = new Battle(game, List.of(carthag), List.of(richese, harkonnen));
            try {
                new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 3, false, 3);
                fail("Expected InvalidGameStateException was not thrown.");
            } catch (Exception e) {
                assertEquals("Richese has only 2 forces in reserves to replace the 3 No-Field", e.getMessage());
            }
            BattlePlan bp = new BattlePlan(game, battle, richese, true, richese.getLeader("Lady Helena").orElseThrow(), null, false, null, null, 2, false, 2);
            assertEquals(2, bp.getRegularDialed());
        }

        @Test
        void testEmperorCanChangeForcesLost() throws InvalidGameStateException {
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            carthag.addForces("Emperor", 4);
            carthag.addForces("Emperor*", 2);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            BattlePlan empPlan = new BattlePlan(game, battle, emperor, true, burseg, null, false, null, null, 4, true, 2);
            assertEquals("This will leave 3 " + Emojis.EMPEROR_TROOP + " in Carthag if you win.", emperorChat.getMessages().getLast());
            assertEquals(2, emperorChat.getChoices().getLast().size());
            assertEquals("1 + 2* (Current)", emperorChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("4 + 1*", emperorChat.getChoices().getLast().getLast().getLabel());
            assertEquals(1, empPlan.getRegularDialed());
            assertEquals(2, empPlan.getSpecialDialed());
        }
    }

    @Nested
    @DisplayName("#arrakeenStrongholdCard")
    class ArrakeenStrongholdCard {
        Battle battle1;

        @BeforeEach
        void setUp() {
            game.setTurnSummary(new TestTopic());
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            atreides.addStrongholdCard(new StrongholdCard("Arrakeen"));
            atreides.addTreacheryCard(cheapHero);
            arrakeen.addForces("Harkonnen", 1);
            battle1 = new Battle(game, List.of(arrakeen), List.of(atreides, harkonnen));
        }

        @Test
        void testBattlePlanResolution2SpiceForArrakeenStrongholdCard() throws InvalidGameStateException {
            BattlePlan bp = new BattlePlan(game, battle1, atreides, true, null, cheapHero, false, null, null, 2, false, 0);
            assertEquals(2, bp.getRegularDialed());
            assertEquals(8, bp.getNumForcesNotDialed());
        }

        @Test
        void testBattlePlanResolutionArrakeenStrongholdCardWithSpiceReductionMessage() throws InvalidGameStateException {
            BattlePlan bp = new BattlePlan(game, battle1, atreides, true, null, cheapHero, false, null, null, 2, false, 1);
            assertEquals(2, bp.getRegularDialed());
            assertEquals(8, bp.getNumForcesNotDialed());
            assertTrue(atreidesChat.getMessages().getFirst().contains("This dial can be supported with"));
        }

        @Test
        void testBattlePlanResolutionArrakeenStrongholdCardDial0NoSpiceReductionMessage() throws InvalidGameStateException {
            BattlePlan bp = new BattlePlan(game, battle1, atreides, true, null, cheapHero, false, null, null, 0, false, 0);
            assertEquals(0, bp.getRegularDialed());
            assertEquals(10, bp.getNumForcesNotDialed());
            assertFalse(atreidesChat.getMessages().getFirst().contains("This dial can be supported with"));
        }
    }

    @Nested
    @DisplayName("#presentEarlyTraitorChoices")
    class PresentEarlyTraitorChoices {
        Battle battle1;
        Battle battle2;

        @BeforeEach
        void setUp() {
            game.addFaction(bg);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(bt);
            atreides.addTreacheryCard(chaumas);
            arrakeen.addForces("BG", 1);
            battle1 = new Battle(game, List.of(arrakeen), List.of(bg, atreides));
            carthag.addForces("BT", 1);
            battle2 = new Battle(game, List.of(carthag), List.of(harkonnen, bt));
        }

        @Test
        void testFactionDoesNotHoldOpponentsTraitor() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));
            BattlePlan battlePlan = new BattlePlan(game, battle1, atreides, false, duncanIdaho, null, false, null, null, 0, false, 0);
            assertNotEquals("Will you call Traitor if " + Emojis.BG + " plays Feyd Rautha?", atreidesChat.getMessages().getLast());
            assertEquals(0, atreidesChat.getChoices().size());
            assertFalse(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testFactionHoldsOpponentsTraitor() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Alia", "BG", 5));
            BattlePlan battlePlan = new BattlePlan(game, battle1, atreides, false, duncanIdaho, null, false, null, null, 0, false, 0);
            assertEquals("Will you call Traitor if " + Emojis.BG + " plays Alia?", atreidesChat.getMessages().getLast());
            assertEquals(3, atreidesChat.getChoices().getLast().size());
            assertTrue(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testFactionHoldsCheapHeroTraitor() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
            BattlePlan battlePlan = new BattlePlan(game, battle1, atreides, false, duncanIdaho, null, false, null, null, 0, false, 0);
            assertEquals("Will you call Traitor if " + Emojis.BG + " plays Cheap Hero?", atreidesChat.getMessages().getLast());
            assertEquals(3, atreidesChat.getChoices().getLast().size());
            assertTrue(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testHarkonnenHoldsOpponentsTraitor() throws InvalidGameStateException {
            harkonnen.addTraitorCard(new TraitorCard("Zoal", "BT", -1));
            harkonnen.addTraitorCard(new TraitorCard("Blin", "BT", 2));
            harkonnen.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            harkonnen.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));
            BattlePlan battlePlan = new BattlePlan(game, battle2, harkonnen, true, ummanKudu, null, false, null, null, 0, false, 0);
            assertEquals("Will you call Traitor if " + Emojis.BT + " plays Zoal or Blin?", harkonnenChat.getMessages().getLast());
            assertEquals(3, harkonnenChat.getChoices().getLast().size());
            assertTrue(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testFactionHoldsOpponentsTraitorInTheTanks() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Alia", "BG", 5));
            game.killLeader(bg, "Alia");
            BattlePlan battlePlan = new BattlePlan(game, battle1, atreides, false, duncanIdaho, null, false, null, null, 0, false, 0);
            assertNotEquals("Will you call Traitor if " + Emojis.BG + " plays Alia?", atreidesChat.getMessages().getLast());
            assertEquals(0, atreidesChat.getChoices().size());
            assertFalse(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testFactionIsAlliedWithHarkonnen() throws InvalidGameStateException {
            game.createAlliance(atreides, harkonnen);
            BattlePlan battlePlan = new BattlePlan(game, battle1, atreides, false, duncanIdaho, null, false, null, null, 0, false, 0);
            assertEquals("Will you call Traitor if " + Emojis.BG + " plays one of " + Emojis.HARKONNEN + "'s traitors?", atreidesChat.getMessages().getLast());
            assertEquals(3, atreidesChat.getChoices().getLast().size());
            assertTrue(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }

        @Test
        void testBTDoesNotCallTraitors() throws InvalidGameStateException {
            bt.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));
            BattlePlan battlePlan = new BattlePlan(game, battle2, bt, false, zoal, null, false, null, null, 0, false, 0);
            assertFalse(btChat.getMessages().getLast().contains("Will you call Traitor"));
            assertEquals(0, btChat.getChoices().size());
            assertFalse(battlePlan.isCanCallTraitor());
            assertFalse(battlePlan.isWillCallTraitor());
        }
    }

    @Nested
    @DisplayName("#noTraitorOnAnothersHomeworld")
    class NoTraitorOnAnothersHomeworld {
        Territory caladan;
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(atreides);
            game.addFaction(emperor);
            caladan = game.getTerritory("Caladan");
            caladan.addForces("Emperor", 1);
            battle = new Battle(game, List.of(caladan), List.of(atreides, emperor));
            atreides.addTraitorCard(new TraitorCard("Burseg", "Emperor", 3));
            emperor.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
        }

        @Test
        void testAtreidesCanCallTraitor() throws InvalidGameStateException {
            BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 0, false, 0);
            assertTrue(atreidesChat.getMessages().getLast().contains("Will you call Traitor"));
            assertTrue(battlePlan.isCanCallTraitor());
        }

        @Test
        void testEmperorCannotCallTraitor() throws InvalidGameStateException {
            BattlePlan battlePlan = new BattlePlan(game, battle, emperor, false, burseg, null, false, null, null, 0, false, 0);
            assertFalse(emperorChat.getMessages().getLast().contains("Will you call Traitor"));
            assertFalse(battlePlan.isCanCallTraitor());
            assertEquals("You cannot call Traitor on Caladan.", emperorChat.getMessages().getLast());
        }
    }

    @AfterEach
    void tearDown() {
    }
}
