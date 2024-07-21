package model;

import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static model.Initializers.getCSVFile;
import static org.junit.jupiter.api.Assertions.*;

public class BattlePlanTest {
    Game game;
    Battle battle;
    Battle carthagBattle;
    AtreidesFaction atreides;
    BTFaction bt;
    ChoamFaction choam;
    EmperorFaction emperor;
    HarkonnenFaction harkonnen;
    TestTopic atreidesChat;
    Leader zoal;
    Leader wykk;
    Leader duncanIdaho;
    Leader ummanKudu;
    Leader bashar;
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
        game = new Game();
        game.setTurnSummary(new TestTopic());
        game.setModInfo(new TestTopic());
        atreides = new AtreidesFaction("p", "u", game);
        bt = new BTFaction("p", "u", game);
        choam = new ChoamFaction("p", "u", game);
        emperor = new EmperorFaction("p", "u", game);
        harkonnen = new HarkonnenFaction("p", "u", game);
        atreides.setLedger(new TestTopic());
        choam.setLedger(new TestTopic());
        emperor.setLedger(new TestTopic());
        bt.setChat(new TestTopic());
        harkonnen.setChat(new TestTopic());
        emperor.setChat(new TestTopic());
        atreidesChat = new TestTopic();
        atreides.setChat(atreidesChat);
        Force atreidesForce = new Force("Atreides", 11);
        battle = new Battle(game, "Gara Kulon", List.of(game.getTerritory("Gara Kulon")), List.of(atreides, harkonnen), List.of(atreidesForce), null);
        Force harkonnenForce = new Force("Harkonnen", 10);
        carthagBattle = new Battle(game, "Carthag", List.of(game.getTerritory("Carthag")), List.of(atreides, harkonnen), List.of(atreidesForce, harkonnenForce), null);
//        arrakeenBattle = new Battle(game, "Arrakeen", List.of(game.getTerritory("Arrakeen")), List.of(atreides, harkonnen), List.of(atreidesForce, harkonnenForce), null);
        zoal = bt.getLeader("Zoal").orElseThrow();
        wykk = bt.getLeader("Wykk").orElseThrow();
        duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        ummanKudu = harkonnen.getLeader("Umman Kudu").orElseThrow();
        bashar = emperor.getLeader("Bashar").orElseThrow();
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
    void testDoesntHaveCheapHero() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, harkonnen, true, null, cheapHero, false, null, null, 0, false, 0));
    }

    @Test
    void testNoLeaderInvalid() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, null, null, false, null, null, 0, false, 0));
    }

    @Test
    void testBattlePlanChoamDoesNotSupportAlly() {
        game.addFaction(choam);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.createAlliance(choam, atreides);
        choam.setSpiceForAlly(1);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testBattlePlanChoamSupportsAlly() {
        game.addFaction(choam);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.createAlliance(choam, atreides);
        choam.setSpiceForAlly(1);
        choam.setAllySpiceForBattle(true);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testBattlePlanEmperorSupportsAlly() {
        game.addFaction(emperor);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
        game.createAlliance(emperor, atreides);
        emperor.setSpiceForAlly(1);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, null, 11, false, 11));
    }

    @Test
    void testWeirdingWayInvalidDefense() throws IOException {
        game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
        if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
            }
        }
        TreacheryCard weirdingWay = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Weirding Way")). findFirst().orElseThrow();
        atreides.addTreacheryCard(weirdingWay);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, null, weirdingWay, 0, false, 0));
    }

    @Test
    void testChemistryInvalidWeapon() throws IOException {
        game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
        if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
            CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
            for (CSVRecord csvRecord : csvParser) {
                game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
            }
        }
        TreacheryCard chemistry = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chemistry")). findFirst().orElseThrow();
        atreides.addTreacheryCard(chemistry);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, chemistry, null, 0, false, 0));
    }

    @Test
    void testDoesntHaveWeapon() {
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, null, 0, false, 0));
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
    void testBattlePlanAtreidesDoesNotHaveKH() {
        atreides.setForcesLost(6);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, duncanIdaho, null, true, null, null, 0, false, 0));
    }

    @Test
    void testKHRequiresLeaderOrCheapHero() {
        atreides.removeTreacheryCard(cheapHero);
        atreides.getLeaders().removeAll(atreides.getLeaders());
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, atreides, true, null, null, true, null, snooper, 0, false, 0));
    }

    @Test
    void testHarkonnenCannotPlayKH() {
        harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, harkonnen, true, duncanIdaho, null, true, null, null, 0, false, 0));
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
        assertFalse(battlePlan.weaponMustBeDiscarded(true));
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
        battle = new Battle(game, "Tleilax", List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen), null, null);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, bt, true, zoal, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawWeaponNotOnSecondHomeworld() {
        emperor.addTreacheryCard(harassAndWithdraw);
        battle = new Battle(game, "Salusa Secundus", List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen), null, null);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, emperor, true, bashar, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawOnHomeworldWithPlanetologist() throws InvalidGameStateException {
        zoal.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        zoal.setSkillCard(planetologist);
        battle = new Battle(game, "Tleilax", List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen), List.of(), null);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, bt, true, zoal, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawOnSecondHomeworldWithPlanetologist() throws InvalidGameStateException {
        emperor.addTreacheryCard(harassAndWithdraw);
        bashar.setPulledBehindShield(true);
        LeaderSkillCard planetologist = new LeaderSkillCard("Planetologist");
        bashar.setSkillCard(planetologist);
        battle = new Battle(game, "Salusa Secundus", List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen), List.of(), null);
        assertDoesNotThrow(() -> new BattlePlan(game, battle, emperor, true, bashar, null, false, harassAndWithdraw, null, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawDefenseNotOnHomeworld() {
        battle = new Battle(game, "Tleilax", List.of(new HomeworldTerritory(game, "Tleilax", "BT")), List.of(bt, harkonnen), null, null);
        assertThrows(InvalidGameStateException.class, () -> new BattlePlan(game, battle, bt, true, zoal, null, false, null, harassAndWithdraw, 0, false, 0));
    }

    @Test
    void testHarassAndWithdrawDefenseNotOnSecondHomeworld() {
        emperor.addTreacheryCard(harassAndWithdraw);
        battle = new Battle(game, "Salusa Secundus", List.of(new HomeworldTerritory(game, "Salusa Secundus", "Emperor")), List.of(emperor, harkonnen), null, null);
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
    void testReinforcementsWeaponCanBePlayedWithPlanetologisy() throws InvalidGameStateException {
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
        assertEquals("Weapon: ~~Poison Tooth~~ (removed from plan)", battlePlan.getWeaponString());
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
        assertEquals("Weapon: ~~Poison Tooth~~ (removed from plan)", opponentBattlePlan.getWeaponString());
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
    void testSwordmasterOfGinazInFrontProjectileWeapon() throws InvalidGameStateException {
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        duncanIdaho.setPulledBehindShield(false);
        atreides.removeTreacheryCard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, shield, 0, false, 0);
        assertEquals(6, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInFrontLeaderDies() throws InvalidGameStateException {
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        duncanIdaho.setPulledBehindShield(false);
        atreides.removeTreacheryCard("Poison Tooth");
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
        atreides.removeTreacheryCard("Poison Tooth");
        atreides.addTreacheryCard(crysknife);
        BattlePlan battlePlan = new BattlePlan(game, battle, atreides, true, duncanIdaho, null, false, crysknife, shield, 0, false, 0);
        assertEquals(10, battlePlan.getDoubleBattleStrength());
    }

    @Test
    void testSwordmasterOfGinazInBattleNoWeapon() throws InvalidGameStateException {
        duncanIdaho.setPulledBehindShield(true);
        LeaderSkillCard swordmasterOfGinaz = new LeaderSkillCard("Swordmaster of Ginaz");
        duncanIdaho.setSkillCard(swordmasterOfGinaz);
        atreides.removeTreacheryCard("Poison Tooth");
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

    @AfterEach
    void tearDown() {
    }
}
