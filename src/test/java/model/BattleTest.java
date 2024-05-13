package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static model.Initializers.getCSVFile;
import static org.junit.jupiter.api.Assertions.*;

class BattleTest {
    Game game;
    Battles battles;
    TestTopic turnSummary;
    AtreidesFaction atreides;
    EcazFaction ecaz;
    BGFaction bg;
    BTFaction bt;
    EmperorFaction emperor;
    FremenFaction fremen;
    HarkonnenFaction harkonnen;
    RicheseFaction richese;
    Territory carthag;
    Territory cielagoNorth_westSector;
    Territory cielagoNorth_eastSector;
    Territory garaKulon;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        game.setModInfo(new TestTopic());
        game.setWhispers(new TestTopic());
        ecaz = new EcazFaction("aPlayer", "aUser", game);
        bg = new BGFaction("bgPlayer", "bgUser", game);
        emperor = new EmperorFaction("ePlayer", "eUser", game);
        fremen = new FremenFaction("fPlayer", "fUser", game);
        harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
        richese = new RicheseFaction("rPlayer", "rUser", game);
        game.addFaction(ecaz);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(harkonnen);
        game.addFaction(richese);
        ecaz.setChat(new TestTopic());
        emperor.setChat(new TestTopic());
        fremen.setChat(new TestTopic());
        carthag = game.getTerritory("Carthag");
        cielagoNorth_eastSector = game.getTerritory("Cielago North (East Sector)");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        garaKulon = game.getTerritory("Gara Kulon");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testJuiceOfSaphoAdded() throws InvalidGameStateException {
        TreacheryCard cheapHero = new TreacheryCard("Cheap Hero");
        richese.addTreacheryCard(cheapHero);
        TreacheryCard chaumas = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chaumas")). findFirst().orElseThrow();
        richese.addTreacheryCard(chaumas);
        Leader alia = new Leader("Alia", 5, null, false);
        bg.addLeader(alia);
        Force richeseForces = new Force("Richese", 3, "Richese");
        Force bgForces = new Force("BG", 1, "BG");
        bg.setChat(new TestTopic());
        richese.setChat(new TestTopic());
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
        BattlePlan richesePlan = battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, chaumas, null);
        BattlePlan bgPlan = battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
        assertEquals(Emojis.RICHESE, battle.getWinnerEmojis(game));
        assertEquals("0", richesePlan.getTotalStrengthString());
        assertEquals("0", bgPlan.getTotalStrengthString());
        assertEquals(0, turnSummary.getMessages().size());
        battle.juiceOfSaphoAdd(game, bg);
        assertEquals(Emojis.BG, battle.getWinnerEmojis(game));
        assertEquals("0", richesePlan.getTotalStrengthString());
        assertEquals("0", bgPlan.getTotalStrengthString());
        assertEquals(1, turnSummary.getMessages().size());
    }

    @Test
    void testPortableSnooperAdded() throws InvalidGameStateException {
        TreacheryCard cheapHero = new TreacheryCard("Cheap Hero");
        richese.addTreacheryCard(cheapHero);
        TreacheryCard chaumas = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chaumas")). findFirst().orElseThrow();
        richese.addTreacheryCard(chaumas);
        TreacheryCard portableSnooper = new TreacheryCard("Portable Snooper");
        bg.addTreacheryCard(portableSnooper);
        Leader alia = new Leader("Alia", 5, null, false);
        bg.addLeader(alia);
        Force richeseForces = new Force("Richese", 3, "Richese");
        Force bgForces = new Force("BG", 1, "BG");
        bg.setChat(new TestTopic());
        richese.setChat(new TestTopic());
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
        BattlePlan richesePlan = battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, chaumas, null);
        BattlePlan bgPlan = battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
        assertFalse(bgPlan.isLeaderAlive());
        assertEquals("2", richesePlan.getTotalStrengthString());
        assertEquals("0", bgPlan.getTotalStrengthString());
        assertEquals(0, turnSummary.getMessages().size());
        battle.portableSnooperAdd(game, bg);
        assertTrue(bgPlan.isLeaderAlive());
        assertEquals("2", richesePlan.getTotalStrengthString());
        assertEquals("5", bgPlan.getTotalStrengthString());
        assertEquals(1, turnSummary.getMessages().size());
    }

    @Test
    void testPoisonToothRemoved() throws InvalidGameStateException {
        TreacheryCard cheapHero = new TreacheryCard("Cheap Hero");
        richese.addTreacheryCard(cheapHero);
        TreacheryCard poisonTooth = new TreacheryCard("Poison Tooth");
        bg.addTreacheryCard(poisonTooth);
        Leader alia = new Leader("Alia", 5, null, false);
        bg.addLeader(alia);
        Force richeseForces = new Force("Richese", 3, "Richese");
        Force bgForces = new Force("BG", 1, "BG");
        bg.setChat(new TestTopic());
        richese.setChat(new TestTopic());
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
        BattlePlan richesePlan = battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, null, null);
        BattlePlan bgPlan = battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, poisonTooth, null);
        assertFalse(richesePlan.isLeaderAlive());
        assertFalse(bgPlan.isLeaderAlive());
        assertEquals("2", richesePlan.getTotalStrengthString());
        assertEquals("0", bgPlan.getTotalStrengthString());
        assertEquals(0, turnSummary.getMessages().size());
        battle.removePoisonTooth(game, bg);
        assertTrue(richesePlan.isLeaderAlive());
        assertTrue(bgPlan.isLeaderAlive());
        assertEquals("2", richesePlan.getTotalStrengthString());
        assertEquals("5", bgPlan.getTotalStrengthString());
        assertEquals(1, turnSummary.getMessages().size());
    }

    @Nested
    @DisplayName("#numForcesNotDialed")
    class NumForcesNotDialed {
        @Test
        void testFremenNegateSardaukar() throws InvalidGameStateException {
            Force empRegulars = new Force("Emperor", 1, "Emperor");
            Force sardaukar = new Force("Emperor*", 3, "Emperor");
            garaKulon.addForce(empRegulars);
            garaKulon.addForce(sardaukar);
            Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(emperor, fremen), List.of(empRegulars, sardaukar), null);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, emperor, 2, false, 1);
            assertEquals(1, forcesDialed.regularForcesDialed);
            assertEquals(2, forcesDialed.specialForcesDialed);
            assertEquals(1, battle.numForcesNotDialed(forcesDialed, emperor, 1));
        }

        @Test
        void testSuboidsAlwaysCountHalf() throws IOException, InvalidGameStateException {
            IxFaction ix = new IxFaction("iPlayer", "iUser", game);
            TestTopic ixChat = new TestTopic();
            ix.setChat(ixChat);
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            Force cyborgs = new Force("Ix*", 3, "Ix");
            Force suboids = new Force("Ix", 3, "Ix");
            hms.addForce(cyborgs);
            hms.addForce(suboids);
            Battle battle = new Battle("Hidden Mobile Stronghold", List.of(hms), List.of(ix, emperor), List.of(cyborgs, suboids), null);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, ix, 7, false, 4);
            assertEquals(1, battle.numForcesNotDialed(forcesDialed, ix, 7));
        }

        @Test
        void testKaramadSardaukar() throws InvalidGameStateException {
            Force empRegulars = new Force("Emperor", 1, "Emperor");
            Force sardaukar = new Force("Emperor*", 3, "Emperor");
            carthag.addForce(empRegulars);
            carthag.addForce(sardaukar);
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(emperor, harkonnen), List.of(empRegulars, sardaukar), null);
            battle.negateSpecialForces(emperor);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, emperor, 2, false, 1);
            assertEquals(1, forcesDialed.regularForcesDialed);
            assertEquals(2, forcesDialed.specialForcesDialed);
            assertEquals(1, battle.numForcesNotDialed(forcesDialed, emperor, 1));
        }

        @Test
        void testKaramadFedaykin() throws InvalidGameStateException {
            Force fremRegulars = new Force("Fremen", 1, "Fremen");
            Force fedaykin = new Force("Fremen*", 3, "Fremen");
            carthag.addForce(fremRegulars);
            carthag.addForce(fedaykin);
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(fremen, harkonnen), List.of(fremRegulars, fedaykin), null);
            battle.negateSpecialForces(fremen);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, fremen, 3, false, 0);
            assertEquals(1, forcesDialed.regularForcesDialed);
            assertEquals(2, forcesDialed.specialForcesDialed);
            assertEquals(1, battle.numForcesNotDialed(forcesDialed, fremen, 0));
        }

        @Test
        void testKaramadCyborgs() throws InvalidGameStateException, IOException {
            IxFaction ix = new IxFaction("iPlayer", "iUser", game);
            TestTopic ixChat = new TestTopic();
            ix.setChat(ixChat);
            Force suboids = new Force("Ix", 2, "Ix");
            Force cyborgs = new Force("Ix*", 3, "Ix");
            carthag.addForce(suboids);
            carthag.addForce(cyborgs);
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(ix, harkonnen), List.of(suboids, cyborgs), null);
            battle.negateSpecialForces(ix);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, ix, 2, false, 1);
            assertEquals(2, forcesDialed.regularForcesDialed);
            assertEquals(1, forcesDialed.specialForcesDialed);
            assertEquals(2, battle.numForcesNotDialed(forcesDialed, ix, 1));
        }

        @Test
        void testKaramaFremenMustPaySpice() throws InvalidGameStateException {
            Force fremRegulars = new Force("Fremen", 1, "Fremen");
            Force fedaykin = new Force("Fremen*", 3, "Fremen");
            carthag.addForce(fremRegulars);
            carthag.addForce(fedaykin);
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(fremen, harkonnen), List.of(fremRegulars, fedaykin), null);
            battle.karamaFremenMustPay(game);
            Battle.ForcesDialed forcesDialed = battle.getForcesDialed(game, fremen, 3, false, 0);
            assertEquals(0, forcesDialed.regularForcesDialed);
            assertEquals(3, forcesDialed.specialForcesDialed);
            assertEquals(1, battle.numForcesNotDialed(forcesDialed, fremen, 0));
        }

        @Test
        void testNoFieldForces() throws IOException {
            RicheseFaction richese = new RicheseFaction("rPlayer", "rUser", game);
            TestTopic richeseChat = new TestTopic();
            richese.setChat(richeseChat);
            Territory carthag = game.getTerritory("Carthag");
            carthag.setRicheseNoField(3);
            Force noField = new Force("NoField", 3, "Richese");
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(richese, harkonnen), List.of(noField), null);
            assertDoesNotThrow(() -> battle.getForcesDialed(game, richese, 3, false, 3));
            assertThrows(InvalidGameStateException.class, () -> battle.getForcesDialed(game, richese, 3, true, 3));
        }
    }

    @Nested
    @DisplayName("#getForcesDialed")
    class GetForcesDialed {
        @Test
        void testSuboidsAlwaysCountHalf() throws IOException, InvalidGameStateException {
            IxFaction ix = new IxFaction("iPlayer", "iUser", game);
            TestTopic ixChat = new TestTopic();
            ix.setChat(ixChat);
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            Force cyborgs = new Force("Ix*", 3, "Ix");
            Force suboids = new Force("Ix", 3, "Ix");
            hms.addForce(cyborgs);
            hms.addForce(suboids);
            Battle battle = new Battle("Hidden Mobile Stronghold", List.of(hms), List.of(ix, emperor), List.of(cyborgs, suboids), null);
            Battle.ForcesDialed ixForces = battle.getForcesDialed(game, ix, 7, false, 4);
            assertEquals(3, ixForces.specialForcesDialed);
            assertEquals(2, ixForces.regularForcesDialed);
        }

        @Test
        void testNoFieldForces() throws IOException, InvalidGameStateException {
            RicheseFaction richese = new RicheseFaction("rPlayer", "rUser", game);
            TestTopic richeseChat = new TestTopic();
            richese.setChat(richeseChat);
            Territory carthag = game.getTerritory("Carthag");
            carthag.setRicheseNoField(3);
            Force noField = new Force("NoField", 3, "Richese");
            Battle battle = new Battle("Carthag", List.of(carthag), List.of(richese, harkonnen), List.of(noField), null);
            Battle.ForcesDialed richeseForces = battle.getForcesDialed(game, richese, 3, false, 3);
            assertEquals(3, richeseForces.regularForcesDialed);
        }
    }

    @Test
    void testAggressorMustChooseOpponentFalse() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor), null, null);
        assertFalse(battle.aggressorMustChooseOpponent());
    }

    @Test
    void testAggressorMustChooseOpponentTrue() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, null);
        assertTrue(battle.aggressorMustChooseOpponent());
    }

    @Test
    void testAggressorMustChooseOpponentEcazAllyFalse() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
        assertFalse(battle.aggressorMustChooseOpponent());
    }

    @Test
    void testAggressorMustChooseOpponentEcazAllyTrue() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Fremen", 1));
        garaKulon.addForce(new Force("Fremen*", 1));
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(fremen, harkonnen, emperor, ecaz), null, "Emperor");
        assertTrue(battle.hasEcazAndAlly());
        assertTrue(battle.aggressorMustChooseOpponent());
    }

    @Test
    void testEcazMustChooseBattleFactionFalseNoEcaz() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor), null, null);
        assertFalse(battle.hasEcazAndAlly());
    }

    @Test
    void testEcazMustChooseBattleFactionFalseWithEcaz() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, null);
        assertFalse(battle.hasEcazAndAlly());
    }

    @Test
    void testEcazMustChooseBattleFactionTrue() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
        assertTrue(battle.hasEcazAndAlly());
    }

    @Test
    void testEcazAllyFighting() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(ecaz, harkonnen, emperor), garaKulon.getForces(), "Emperor");
        battle.setEcazCombatant(game, emperor.getName());
        Leader burseg = emperor.getLeader("Burseg").orElseThrow();
        assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 5, null, null));
    }

    @Test
    void testEcazFighting() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), garaKulon.getForces(), "Emperor");
        battle.setEcazCombatant(game, "Ecaz");
        Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
        assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 5, null, null));
    }

    @Test
    void testBattleResolved() {
        Force emperorTroops = new Force("Emperor", 5);
        carthag.addForce(emperorTroops);
        Force sardaukar = new Force("Emperor*", 2);
        carthag.addForce(sardaukar);
        Battle battle = new Battle("Carthag", List.of(carthag), List.of(emperor, harkonnen), null, null);
        assertFalse(battle.isResolved(game));
        carthag.removeForce("Harkonnen");
        assertTrue(battle.isResolved(game));
    }

    @Test
    void testBattleResolvedEcazAlly() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle("Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
        assertFalse(battle.isResolved(game));
        garaKulon.removeForce("Harkonnen");
        assertTrue(battle.isResolved(game));
    }

    @Nested
    @DisplayName("#twoFactionsNoSpecials")
    class TwoFactionsNoSpecials {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1}", Emojis.BG, Emojis.FREMEN),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                    "6 {0} vs 7 {1}", Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#twoFactionsWithSpecials")
    class TwoFactionsWithSpecials {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1}", Emojis.BG, Emojis.FREMEN),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "6 {0} vs 7 {1} 2 {2}", Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#threeFactionsWithSpecials")
    class ThreeFactionsWithSpecials {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            westCielagoNorth.addForce(new Force("Emperor*", 1));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1} vs {2}", Emojis.BG, Emojis.EMPEROR, Emojis.FREMEN),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "6 {0} vs 1 {1} vs 7 {2} 2 {3}", Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#threeFactionsWithNoField")
    class ThreeFactionsWithNoField {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            westCielagoNorth.setRicheseNoField(5);
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1} vs {2}", Emojis.RICHESE, Emojis.BG, Emojis.FREMEN),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "1 {0} vs 6 {1} vs 7 {2} 2 {3}", Emojis.NO_FIELD, Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#fourFactionsEcazAlly")
    class FourFactionsEcazAlly {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            westCielagoNorth.addForce(new Force("Emperor*", 1));
            westCielagoNorth.addForce(new Force("Ecaz", 1));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0}{1} vs {2} vs {3}", Emojis.ECAZ, Emojis.FREMEN, Emojis.BG, Emojis.EMPEROR),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "1 {0} 7 {1} 2 {2} vs 6 {3} vs 1 {4}", Emojis.ECAZ_TROOP, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN, Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#fourFactionsEcazStormSeparatesAlly")
    class FourFactionsEcazStormSeparatesAlly {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(1);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            westCielagoNorth.addForce(new Force("Emperor*", 1));
            westCielagoNorth.addForce(new Force("Ecaz", 1));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1} vs {2}", Emojis.BG, Emojis.EMPEROR, Emojis.ECAZ),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "6 {0} vs 1 {1} vs 1 {2}", Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR, Emojis.ECAZ_TROOP),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#fourFactionsAllyEcaz")
    class FourFactionsAllyEcaz {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.setStorm(4);
            ecaz.setAlly("Fremen");
            fremen.setAlly("Ecaz");
            Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
            westCielagoNorth.addForce(new Force("BG", 6));
            westCielagoNorth.addForce(new Force("Emperor*", 1));
            westCielagoNorth.addForce(new Force("Ecaz", 1));
            Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
            eastCielagoNorth.addForce(new Force("Fremen", 7));
            eastCielagoNorth.addForce(new Force("Fremen*", 2));

            game.startBattlePhase();
            battles = game.getBattles();
        }

        @Test
        void getFactionsMessage() {
            assertEquals(MessageFormat.format(
                            "{0} vs {1}{2} vs {3}", Emojis.EMPEROR, Emojis.FREMEN, Emojis.ECAZ, Emojis.BG),
                    battles.getBattles(game).get(0).getFactionsMessage(game)
            );
        }

        @Test
        void getForcesMessage() {
            assertEquals(MessageFormat.format(
                            "1 {0} vs 7 {1} 2 {2} 1 {3} vs 6 {4}", Emojis.EMPEROR_SARDAUKAR, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN, Emojis.ECAZ_TROOP, Emojis.BG_FIGHTER),
                    battles.getBattles(game).get(0).getForcesMessage(game)
            );
        }
    }

    @Nested
    @DisplayName("#battlePlans")
    class BattlePlans {
        Leader duncanIdaho;
        TreacheryCard cheapHero;
        TreacheryCard crysknife;
        TreacheryCard chaumas;
        TreacheryCard shield;
        Territory arrakeen;
        Territory carthag;
        Territory habbanyaSietch;
        Battle battle1;
        Battle battle2;
        Battle battle2a;
        Battle battle3;

        @BeforeEach
        void setUp() throws IOException {
            game = new Game();
            game.setModInfo(new TestTopic());
            atreides = new AtreidesFaction("aPlayer", "aUser", game);
            bg = new BGFaction("fPlayer", "fUser", game);
            harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
            ecaz = new EcazFaction("ePlayer", "eUser", game);
            bt = new BTFaction("btPlayer", "btUser", game);
            emperor = new EmperorFaction("empPlayer", "empUser", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(bt);
            game.addFaction(emperor);
            atreides.setChat(new TestTopic());
            bt.setChat(new TestTopic());
            ecaz.setChat(new TestTopic());
            emperor.setChat(new TestTopic());
            harkonnen.setChat(new TestTopic());
            arrakeen = game.getTerritory("Arrakeen");
            arrakeen.setForceStrength("Harkonnen", 1);
            battle1 = new Battle("Arrakeen", List.of(arrakeen), List.of(atreides, harkonnen), arrakeen.getForces(), "Atreides");
            ecaz.setAlly("Atreides");
            atreides.setAlly("Ecaz");
            carthag = game.getTerritory("Carthag");
            carthag.setForceStrength("Ecaz", 5);
            carthag.setForceStrength("Atreides", 1);
            battle2 = new Battle("Carthag", List.of(carthag), List.of(atreides, harkonnen, ecaz), carthag.getForces(), "Atreides");
            battle2a = new Battle("Carthag", List.of(carthag), List.of(harkonnen, atreides, ecaz), carthag.getForces(), "Atreides");
            habbanyaSietch = game.getTerritory("Habbanya Sietch");
            habbanyaSietch.setForceStrength("BT", 6);
            habbanyaSietch.setForceStrength("Emperor", 6);
            habbanyaSietch.setForceStrength("Emperor*", 5);
            battle3 = new Battle("Habbanya Sietch", List.of(habbanyaSietch), List.of(bt, emperor), habbanyaSietch.getForces(), "Atreides");

            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
            cheapHero = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Cheap Hero")). findFirst().orElseThrow();
            crysknife = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Crysknife")). findFirst().orElseThrow();
            chaumas = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chaumas")). findFirst().orElseThrow();
            shield = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Shield")). findFirst().orElseThrow();
        }

        @Test
        void testEcazAllyChoiceMakesOpponentAggressor() {
            battle2.setEcazCombatant(game, "Ecaz");
            assertInstanceOf(HarkonnenFaction.class, battle2.getAggressor(game));
        }

        @Test
        void testEcazAloneTroopStrength() throws InvalidGameStateException {
            Territory tueksSietch = new Territory("Habbanya Sietch", 16, false, true, false);
            TreacheryCard hunterSeeker = new TreacheryCard("Hunter Seeker");
            TreacheryCard weirdingWay = new TreacheryCard("Weirding Way");
            Force atreidesForce = new Force("Atreides", 11);
            Force ecazForce = new Force("Ecaz", 10);
            Battle ecazBattle = new Battle("Tuek's Sietch", List.of(tueksSietch), List.of(atreides, ecaz), List.of(atreidesForce, ecazForce), null);
            atreides.addTreacheryCard(hunterSeeker);
            atreides.addTreacheryCard(weirdingWay);
            ecazBattle.setBattlePlan(game, atreides, atreides.getLeader("Lady Jessica").orElseThrow(), null, false, 6, false, 4, hunterSeeker, weirdingWay);
            ecazBattle.setBattlePlan(game, ecaz, ecaz.getLeader("Bindikk Narvi").orElseThrow(), null, false, 5, false, 0, null, null);
            BattlePlan atreidesPlan = ecazBattle.getAggressorBattlePlan();
            BattlePlan ecazPlan = ecazBattle.getDefenderBattlePlan();
            atreidesPlan.revealOpponentBattlePlan(ecazPlan);
            ecazPlan.revealOpponentBattlePlan(atreidesPlan);
            assertEquals("5", ecazPlan.getTotalStrengthString());
        }

        @Test
        void testWeirdingWayValidDefense() throws IOException {
            game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
            if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
                CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
                for (CSVRecord csvRecord : csvParser) {
                    game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
                }
            }
            TreacheryCard weirdingWay = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Weirding Way")). findFirst().orElseThrow();
            atreides.addTreacheryCard(chaumas);
            atreides.addTreacheryCard(weirdingWay);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chaumas, weirdingWay));
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
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, weirdingWay));
        }

        @Test
        void testChemistryValidWeapon() throws IOException {
            game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
            if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
                CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
                for (CSVRecord csvRecord : csvParser) {
                    game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
                }
            }
            TreacheryCard chemistry = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chemistry")). findFirst().orElseThrow();
            atreides.addTreacheryCard(chemistry);
            atreides.addTreacheryCard(shield);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chemistry, shield));
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
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chemistry, null));
        }

        @Test
        void testBattlePlanLeaderAvailable() {
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanLeaderInTanks() {
            atreides.removeLeader(duncanIdaho.getName());
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderWithCheapHero() {
            atreides.addTreacheryCard(cheapHero);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, cheapHero, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderValid() {
            atreides.removeLeader("Duncan Idaho");
            atreides.removeLeader("Lady Jessica");
            atreides.removeLeader("Gurney Halleck");
            atreides.removeLeader("Thufir Hawat");
            atreides.removeLeader("Dr. Yueh");
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, null, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderInvalid() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, null, null, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanOtherFactionLeaderInvalid() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, harkonnen, duncanIdaho, null, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanHarkonnenCapturedLeader() {
            harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, harkonnen, duncanIdaho, null, false, 1, false,1, null, null));
        }

        @Test
        void testBattlePlanFactionNotInvolved() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, bg, null, cheapHero, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanNotEnoughSpice() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,11, null, null));
        }

        @Test
        void testBattlePlanHasWeapon() {
            atreides.addTreacheryCard(crysknife);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, crysknife, null));
        }

        @Test
        void testBattlePlanHasDefense() {
            atreides.addTreacheryCard(shield);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, null, shield));
        }

        @Test
        void testBattlePlanDoesntHaveWeapon() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, crysknife, null));
        }

        @Test
        void testBattlePlanDoesntHaveDefense() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, null, shield));
        }

        @Test
        void testBattlePlanDoesntHaveCheapHero() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, null, cheapHero, false, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanSpendingTooMuch() {
            TestTopic atreidesChat = new TestTopic();
            atreides.setChat(atreidesChat);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, true,5, null, null));
            assertEquals(3, atreidesChat.messages.size());
        }

        @Test
        void testBattlePlanAtreidesHasKH() {
            atreides.setForcesLost(7);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, true, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderWithCheapHeroAndKH() {
            atreides.addTreacheryCard(cheapHero);
            atreides.setForcesLost(10);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, cheapHero, true, 4, false,3, null, null));
        }

        @Test
        void testBattlePlanAtreidesDoesNotHaveKH() {
            atreides.setForcesLost(6);
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, true, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanHarkonnenCannotPlayKH() {
            harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, harkonnen, duncanIdaho, null, true, 1, false,1, null, null));
        }

        @Test
        void testBattlePlanNotEnoughTroops() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,10, null, null));
        }

        @Test
        void testBattlePlanResolution() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            assertEquals(Emojis.ATREIDES, battle1.getWinnerEmojis(game));
            assertEquals("2.5", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionLeaderKilled() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertEquals(Emojis.HARKONNEN, battle1.getWinnerEmojis(game));
            assertEquals("0.5", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAggressorWinsTies() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false,0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertTrue(battle1.isAggressorWin(game));
            assertEquals(Emojis.ATREIDES, battle1.getWinnerEmojis(game));
            assertEquals("1", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAggressorWinsTiesInHRSWithStrongholdCard() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            bt.addStrongholdCard(new StrongholdCard("Habbanya Sietch"));
            bt.addTreacheryCard(cheapHero);
            emperor.addTreacheryCard(cheapHero);
            emperor.setChat(new TestTopic());
            battle3.setBattlePlan(game, bt, null, cheapHero, false, 1, false,0, null, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 1, false,0, null, null);
            assertTrue(battle3.isAggressorWin(game));
            assertEquals(Emojis.BT, battle3.getWinnerEmojis(game));
            assertEquals("1", battle3.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle3.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionDefenderWinsTiesInHRSWithStrongholdCard() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            emperor.addStrongholdCard(new StrongholdCard("Habbanya Sietch"));
            bt.addTreacheryCard(cheapHero);
            emperor.addTreacheryCard(cheapHero);
            emperor.setChat(new TestTopic());
            battle3.setBattlePlan(game, bt, null, cheapHero, false, 1, false,0, null, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 1, false,0, null, null);
            assertFalse(battle3.isAggressorWin(game));
            assertEquals(Emojis.EMPEROR, battle3.getWinnerEmojis(game));
            assertEquals("1", battle3.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle3.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAllyEcaz() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle2.setEcazCombatant(game, "Atreides");
            battle2.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertEquals(Emojis.ATREIDES + Emojis.ECAZ, battle2.getWinnerEmojis(game));
            assertTrue(battle2.isAggressorWin(game));
            assertEquals("3.5", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAllyEcaz2() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setEcazCombatant(game, "Atreides");
            battle2.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            assertEquals(Emojis.ATREIDES + Emojis.ECAZ, battle2.getWinnerEmojis(game));
            assertEquals("5.5", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionEcazAlly() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle2.setEcazCombatant(game, "Ecaz");
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            battle2.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertEquals(Emojis.ECAZ + Emojis.ATREIDES, battle2.getWinnerEmojis(game));
            assertFalse(battle2.isAggressorWin(game));
            assertEquals("1", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("3.5", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionEcazAlly2() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setEcazCombatant(game, "Ecaz");
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            battle2.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            assertEquals(Emojis.ECAZ + Emojis.ATREIDES, battle2.getWinnerEmojis(game));
            assertEquals("1", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("7.5", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testZoalHasOpposingLeaderValue() throws InvalidGameStateException {
            TestTopic emperorChat = new TestTopic();
            emperor.setChat(emperorChat);
            Leader zoal = bt.getLeader("Zoal").orElseThrow();
            Leader burseg = emperor.getLeader("Burseg").orElseThrow();
            bt.addTreacheryCard(crysknife);
            battle3.setBattlePlan(game, bt, zoal, null, false, 5, false, 5, crysknife, null);
            battle3.setBattlePlan(game, emperor, burseg, null, false, 7, false, 5, null, null);
            assertEquals(Emojis.BT, battle3.getWinnerEmojis(game));
            assertEquals("8", battle3.getAggressorBattlePlan().getTotalStrengthString());
        }

        @Test
        void testZoalHasNoValue() throws InvalidGameStateException {
            TestTopic emperorChat = new TestTopic();
            emperor.setChat(emperorChat);
            Leader zoal = bt.getLeader("Zoal").orElseThrow();
            bt.addTreacheryCard(crysknife);
            emperor.addTreacheryCard(cheapHero);
            battle3.setBattlePlan(game, bt, zoal, null, false, 5, false, 5, crysknife, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 7, false, 5, null, null);
            assertEquals(Emojis.EMPEROR, battle3.getWinnerEmojis(game));
            assertEquals("5", battle3.getAggressorBattlePlan().getTotalStrengthString());
        }

        @Test
        void testHomeworldDialAdvantageKaitainHigh() {
            Territory kaitain = game.getTerritory("Kaitain");
            assertTrue(emperor.isHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, kaitain, emperor));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(0, battle3.homeworldDialAdvantage(game, kaitain, bt));
            assertEquals(2, battle3.homeworldDialAdvantage(game, kaitain, emperor));
        }

        @Test
        void testHomeworldDialAdvantageKaitainLow() {
            game.setTurnSummary(new TestTopic());
            Territory kaitain = game.getTerritory("Kaitain");
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Kaitain", emperor, 12, 0, true);
            assertFalse(emperor.isHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, kaitain, bt));
            assertEquals(3, battle3.homeworldDialAdvantage(game, kaitain, emperor));
        }

        @Test
        void testHomeworldDialAdvantageSalusaSecundusHigh() {
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            assertTrue(emperor.isSecundusHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, salusaSecundus, emperor));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(0, battle3.homeworldDialAdvantage(game, salusaSecundus, bt));
            assertEquals(3, battle3.homeworldDialAdvantage(game, salusaSecundus, emperor));
        }

        @Test
        void testHomeworldDialAdvantageSalusaSecundusLow() {
            game.setTurnSummary(new TestTopic());
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(emperor.isSecundusHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, salusaSecundus, bt));
            assertEquals(2, battle3.homeworldDialAdvantage(game, salusaSecundus, emperor));
        }

        @Test
        void testHomeworldDialAdvantageTleilax() {
            game.setTurnSummary(new TestTopic());
            Territory tleilax = game.getTerritory("Tleilax");
            assertTrue(bt.isHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, tleilax, bt));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(2, battle3.homeworldDialAdvantage(game, tleilax, bt));
            assertEquals(0, battle3.homeworldDialAdvantage(game, tleilax, emperor));
            game.removeForces("Tleilax", bt, 12, 0, true);
            assertFalse(bt.isHighThreshold());
            assertEquals(2, battle3.homeworldDialAdvantage(game, tleilax, bt));
        }

        @Test
        void testHomeworldDialAdvantageWallachIXHigh() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            assertTrue(bg.isHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, wallachIX, bg));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(0, battle3.homeworldDialAdvantage(game, wallachIX, bt));
            assertEquals(3, battle3.homeworldDialAdvantage(game, wallachIX, bg));
        }

        @Test
        void testHomeworldDialAdvantageWallachIXLow() {
            game.setTurnSummary(new TestTopic());
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Wallach IX", bg, 10, 0, true);
            assertFalse(bg.isHighThreshold());
            assertEquals(0, battle3.homeworldDialAdvantage(game, wallachIX, bt));
            assertEquals(2, battle3.homeworldDialAdvantage(game, wallachIX, bg));
        }

        @Test
        void testSalusaSecundusHighSpiceNotNeededForSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            Force emperorForce = new Force("Emperor", 9, "Emperor");
            Force sardaukarForce = new Force("Emperor*", 3, "Emperor");
            Force bgForce = new Force("BG", 15, "BG");
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(emperor.isSecundusHighThreshold());
            Battle battle = new Battle("Wallach IX", List.of(wallachIX), List.of(emperor, bg), List.of(emperorForce, sardaukarForce, bgForce), null);
            Leader burseg = emperor.getLeader("Burseg").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }

        @Test
        void testSalusaSecundusLowSpiceNeededForSardaukar() {
            game.setTurnSummary(new TestTopic());
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            Force emperorForce = new Force("Emperor", 9, "Emperor");
            Force sardaukarForce = new Force("Emperor*", 1, "Emperor");
            Force bgForce = new Force("BG", 15, "BG");
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(emperor.isSecundusHighThreshold());
            Battle battle = new Battle("Wallach IX", List.of(wallachIX), List.of(emperor, bg), List.of(emperorForce, sardaukarForce, bgForce), null);
            Leader burseg = new Leader("Burseg", 1, null, false);
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }
    }
}
