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
    TestTopic modInfo;
    AtreidesFaction atreides;
    EcazFaction ecaz;
    BGFaction bg;
    BTFaction bt;
    ChoamFaction choam;
    EmperorFaction emperor;
    FremenFaction fremen;
    HarkonnenFaction harkonnen;
    RicheseFaction richese;
    TestTopic atreidesChat;
    Territory carthag;
    Territory cielagoNorth_westSector;
    Territory cielagoNorth_eastSector;
    Territory garaKulon;
    TreacheryCard cheapHero;
    TreacheryCard crysknife;
    TreacheryCard chaumas;
    TreacheryCard shield;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        modInfo = new TestTopic();
        game.setModInfo(modInfo);
        game.setWhispers(new TestTopic());
        atreides = new AtreidesFaction("p", "u");
        bg = new BGFaction("p", "u");
        bt = new BTFaction("p", "u");
        choam = new ChoamFaction("p", "u");
        ecaz = new EcazFaction("p", "u");
        emperor = new EmperorFaction("p", "u");
        fremen = new FremenFaction("p", "u");
        harkonnen = new HarkonnenFaction("p", "u");
        richese = new RicheseFaction("p", "u");
        atreidesChat = new TestTopic();
        atreides.setChat(atreidesChat);
        bg.setChat(new TestTopic());
        bt.setChat(new TestTopic());
        ecaz.setChat(new TestTopic());
        emperor.setChat(new TestTopic());
        fremen.setChat(new TestTopic());
        harkonnen.setChat(new TestTopic());
        richese.setChat(new TestTopic());
        atreides.setLedger(new TestTopic());
        choam.setLedger(new TestTopic());
        ecaz.setLedger(new TestTopic());
        emperor.setLedger(new TestTopic());
        fremen.setLedger(new TestTopic());
        harkonnen.setLedger(new TestTopic());
        richese.setLedger(new TestTopic());
        carthag = game.getTerritory("Carthag");
        cielagoNorth_eastSector = game.getTerritory("Cielago North (East Sector)");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        garaKulon = game.getTerritory("Gara Kulon");
        cheapHero = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Cheap Hero")).findFirst().orElseThrow();
        crysknife = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Crysknife")).findFirst().orElseThrow();
        chaumas = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chaumas")).findFirst().orElseThrow();
        shield = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Shield")).findFirst().orElseThrow();
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("#oldTestsRelyingOnGlobalAddingFactions")
    class OldTestsRelyingOnGlobalAddingFactions {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(ecaz);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);
        }

        @Test
        void testNoFieldInBattlePlanFewerForcesInReserves() {
            richese.addTreacheryCard(cheapHero);
            richese.addTreacheryCard(chaumas);
            Leader alia = new Leader("Alia", 5, null, false);
            bg.addLeader(alia);
            Force noFieldForces = new Force("NoField", 3);
            Territory richeseHomeworld = game.getTerritory("Richese");
            assertEquals(20, richeseHomeworld.getForceStrength("Richese"));
            garaKulon.placeForceFromReserves(game, richese, 5, false);
            Force richeseForces = new Force("Richese", 5);
            assertEquals(15, richeseHomeworld.getForceStrength("Richese"));
            richeseHomeworld.removeForces("Richese", 13);
            assertEquals(2, richeseHomeworld.getForceStrength("Richese"));
            Force bgForces = new Force("BG", 1);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, noFieldForces, bgForces), null);
            richese.setSpice(8);
            try {
                battle.setBattlePlan(game, richese, null, cheapHero, false, 8, false, 8, chaumas, null);
                fail("Expected InvalidGameStateException was not thrown.");
            } catch (Exception e) {
                assertEquals("Richese has only 2 forces in reserves to replace the 3 No-Field", e.getMessage());
            }
            assertDoesNotThrow(() -> battle.setBattlePlan(game, richese, null, cheapHero, false, 7, false, 7, chaumas, null));
        }

        @Test
        void testJuiceOfSaphoAdded() throws InvalidGameStateException {
            richese.addTreacheryCard(cheapHero);
            richese.addTreacheryCard(chaumas);
            Leader alia = new Leader("Alia", 5, null, false);
            bg.addLeader(alia);
            Force richeseForces = new Force("Richese", 3);
            Force bgForces = new Force("BG", 1);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
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
            richese.addTreacheryCard(cheapHero);
            richese.addTreacheryCard(chaumas);
            TreacheryCard portableSnooper = new TreacheryCard("Portable Snooper");
            bg.addTreacheryCard(portableSnooper);
            Leader alia = new Leader("Alia", 5, null, false);
            bg.addLeader(alia);
            Force richeseForces = new Force("Richese", 3);
            Force bgForces = new Force("BG", 1);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
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
            richese.addTreacheryCard(cheapHero);
            TreacheryCard poisonTooth = new TreacheryCard("Poison Tooth");
            bg.addTreacheryCard(poisonTooth);
            Leader alia = new Leader("Alia", 5, null, false);
            bg.addLeader(alia);
            Force richeseForces = new Force("Richese", 3);
            Force bgForces = new Force("BG", 1);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(richese, bg), List.of(richeseForces, bgForces), null);
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

        @Test
        void testAggressorMustChooseOpponentFalse() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor), null, null);
            assertFalse(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testAggressorMustChooseOpponentTrue() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, null);
            assertTrue(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testAggressorMustChooseOpponentEcazAllyFalse() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
            assertFalse(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testAggressorMustChooseOpponentEcazAllyTrue() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Fremen", 1);
            garaKulon.addForces("Fremen*", 1);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(fremen, harkonnen, emperor, ecaz), null, "Emperor");
            assertTrue(battle.hasEcazAndAlly());
            assertTrue(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testEcazMustChooseBattleFactionFalseNoEcaz() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor), null, null);
            assertFalse(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazMustChooseBattleFactionFalseWithEcaz() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, null);
            assertFalse(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazMustChooseBattleFactionTrue() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
            assertTrue(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazAllyFighting() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(ecaz, harkonnen, emperor), garaKulon.getForces(), "Emperor");
            battle.setEcazCombatant(game, emperor.getName());
            assertEquals(emperor, battle.getDefender(game));
            Leader burseg = emperor.getLeader("Burseg").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 5, null, null));
        }

        @Test
        void testEcazFighting() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), garaKulon.getForces(), "Emperor");
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 5, null, null));
        }

        @Test
        void testEcazAllyChangeForceLosses() throws InvalidGameStateException {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 6);
            garaKulon.addForces("Emperor*", 1);
            garaKulon.addForces("Ecaz", 7);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), garaKulon.getForces(), "Emperor");
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
//        BattlePlan bp = new BattlePlan(game, battle, ecaz, false, sanyaEcaz, null, false, null, null, 5, false, 5);
            assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 5, null, null));
            BattlePlan bp = battle.getDefenderBattlePlan();
            assertEquals("This will leave 3 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", bp.getForcesRemainingString());
//        assertEquals("This will leave 3 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", battle.getForcesRemainingString("Ecaz", 3, 1));
            assertEquals(3, bp.getRegularDialed());
            assertEquals(1, bp.getSpecialDialed());
            battle.updateTroopsDialed("Ecaz", 5, 0);
            assertEquals(5, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals("This will leave 1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", bp.getForcesRemainingString());
//        assertEquals("This will leave 1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", battle.getForcesRemainingString("Ecaz", 5, 0));
        }

        @Test
        void testRicheseNoFieldNotYetRevealed() throws InvalidGameStateException {
            garaKulon.setRicheseNoField(5);
            garaKulon.addForces("Fremen", 3);
            game.startBattlePhase();
            battles = game.getBattles();
            List<Force> battleForces = battles.aggregateForces(List.of(garaKulon), List.of(fremen, richese));
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(fremen, richese), battleForces, "Emperor");
            Leader ladyHelena = richese.getLeader("Lady Helena").orElseThrow();
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 3, false, 0, null, null));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 2, false, 0, null, null));
        }

        @Test
        void testBattleResolved() {
            carthag.addForces("Emperor", 5);
            carthag.addForces("Emperor*", 2);
            Battle battle = new Battle(game, "Carthag", List.of(carthag), List.of(emperor, harkonnen), null, null);
            assertFalse(battle.isResolved(game));
            carthag.removeForce("Harkonnen");
            assertTrue(battle.isResolved(game));
        }

        @Test
        void testBattleResolvedEcazAlly() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz), null, "Emperor");
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
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_eastSector.addForces("Fremen", 7);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1}", Emojis.BG, Emojis.FREMEN),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "6 {0} vs 7 {1}", Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#twoFactionsWithSpecials")
        class TwoFactionsWithSpecials {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(10);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1}", Emojis.BG, Emojis.FREMEN),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "6 {0} vs 7 {1} 2 {2}", Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#threeFactionsWithSpecials")
        class ThreeFactionsWithSpecials {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(10);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_westSector.addForces("Emperor*", 1);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1} vs {2}", Emojis.BG, Emojis.EMPEROR, Emojis.FREMEN),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "6 {0} vs 1 {1} vs 7 {2} 2 {3}", Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#threeFactionsWithNoField")
        class ThreeFactionsWithNoField {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(10);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_westSector.setRicheseNoField(5);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1} vs {2}", Emojis.RICHESE, Emojis.BG, Emojis.FREMEN),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "1 {0} vs 6 {1} vs 7 {2} 2 {3}", Emojis.NO_FIELD, Emojis.BG_FIGHTER, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#fourFactionsEcazAlly")
        class FourFactionsEcazAlly {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(10);
                game.createAlliance(fremen, ecaz);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_westSector.addForces("Emperor*", 1);
                cielagoNorth_westSector.addForces("Ecaz", 1);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0}{1} vs {2} vs {3}", Emojis.ECAZ, Emojis.FREMEN, Emojis.BG, Emojis.EMPEROR),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "1 {0} 7 {1} 2 {2} vs 6 {3} vs 1 {4}", Emojis.ECAZ_TROOP, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN, Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#fourFactionsEcazStormSeparatesAlly")
        class FourFactionsEcazStormSeparatesAlly {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(1);
                game.createAlliance(fremen, ecaz);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_westSector.addForces("Emperor*", 1);
                cielagoNorth_westSector.addForces("Ecaz", 1);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1} vs {2}", Emojis.BG, Emojis.EMPEROR, Emojis.ECAZ),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "6 {0} vs 1 {1} vs 1 {2}", Emojis.BG_FIGHTER, Emojis.EMPEROR_SARDAUKAR, Emojis.ECAZ_TROOP),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Nested
        @DisplayName("#fourFactionsAllyEcaz")
        class FourFactionsAllyEcaz {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.setStorm(4);
                game.createAlliance(fremen, ecaz);
                cielagoNorth_westSector.addForces("BG", 6);
                cielagoNorth_westSector.addForces("Emperor*", 1);
                cielagoNorth_westSector.addForces("Ecaz", 1);
                cielagoNorth_eastSector.addForces("Fremen", 7);
                cielagoNorth_eastSector.addForces("Fremen*", 2);

                game.startBattlePhase();
                battles = game.getBattles();
            }

            @Test
            void getFactionsMessage() {
                assertEquals(MessageFormat.format(
                                "{0} vs {1}{2} vs {3}", Emojis.EMPEROR, Emojis.FREMEN, Emojis.ECAZ, Emojis.BG),
                        battles.getBattles(game).getFirst().getFactionsMessage(game)
                );
            }

            @Test
            void getForcesMessage() {
                assertEquals(MessageFormat.format(
                                "1 {0} vs 7 {1} 2 {2} 1 {3} vs 6 {4}", Emojis.EMPEROR_SARDAUKAR, Emojis.FREMEN_TROOP, Emojis.FREMEN_FEDAYKIN, Emojis.ECAZ_TROOP, Emojis.BG_FIGHTER),
                        battles.getBattles(game).getFirst().getForcesMessage(game)
                );
            }
        }

        @Test
        void testNonNativeNoAdvantageOnOtherHomeworlds() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.getHomeworlds().forEach((fn, h) -> game.getFactions().stream().filter(f -> !f.getHomeworld().equals(h) && !(f instanceof EmperorFaction e && e.getSecondHomeworld().equals(h)))
                    .forEach(f -> assertEquals(0, f.homeworldDialAdvantage(game, game.getTerritory(h)))));
        }
    }

    @Nested
    @DisplayName("#battlePlans")
    class BattlePlans {
        Leader duncanIdaho;
        Territory arrakeen;
        Territory habbanyaSietch;
        Battle battle1;
        Battle battle2;
        Battle battle2a;
        Battle battle3;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(bt);
            game.addFaction(emperor);
            arrakeen = game.getTerritory("Arrakeen");
            arrakeen.addForces("Harkonnen", 1);
            battle1 = new Battle(game, "Arrakeen", List.of(arrakeen), List.of(atreides, harkonnen), arrakeen.getForces(), "Atreides");
            game.createAlliance(atreides, ecaz);
            turnSummary.clear();
            carthag.addForces("Ecaz", 5);
            carthag.addForces("Atreides", 1);
            battle2 = new Battle(game, "Carthag", List.of(carthag), List.of(atreides, harkonnen, ecaz), carthag.getForces(), "Atreides");
            battle2a = new Battle(game, "Carthag", List.of(carthag), List.of(harkonnen, atreides, ecaz), carthag.getForces(), "Atreides");
            habbanyaSietch = game.getTerritory("Habbanya Sietch");
            habbanyaSietch.addForces("BT", 6);
            habbanyaSietch.addForces("Emperor", 6);
            habbanyaSietch.addForces("Emperor*", 5);
            battle3 = new Battle(game, "Habbanya Sietch", List.of(habbanyaSietch), List.of(bt, emperor), habbanyaSietch.getForces(), "Atreides");

            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
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
            Battle ecazBattle = new Battle(game, "Tuek's Sietch", List.of(tueksSietch), List.of(atreides, ecaz), List.of(atreidesForce, ecazForce), null);
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
            TreacheryCard weirdingWay = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Weirding Way")).findFirst().orElseThrow();
            atreides.addTreacheryCard(chaumas);
            atreides.addTreacheryCard(weirdingWay);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chaumas, weirdingWay));
        }

        //        @Test
//        void testWeirdingWayInvalidDefense() throws IOException {
//            game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
//            if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
//                CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
//                for (CSVRecord csvRecord : csvParser) {
//                    game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
//                }
//            }
//            TreacheryCard weirdingWay = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Weirding Way")). findFirst().orElseThrow();
//            atreides.addTreacheryCard(weirdingWay);
//            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, weirdingWay));
//        }
//
        @Test
        void testChemistryValidWeapon() throws IOException {
            game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
            if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
                CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
                for (CSVRecord csvRecord : csvParser) {
                    game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
                }
            }
            TreacheryCard chemistry = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chemistry")).findFirst().orElseThrow();
            atreides.addTreacheryCard(chemistry);
            atreides.addTreacheryCard(shield);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chemistry, shield));
        }

//        @Test
//        void testChemistryInvalidWeapon() throws IOException {
//            game.addGameOption(GameOption.EXPANSION_TREACHERY_CARDS);
//            if (game.hasGameOption(GameOption.EXPANSION_TREACHERY_CARDS)) {
//                CSVParser csvParser = getCSVFile("ExpansionTreacheryCards.csv");
//                for (CSVRecord csvRecord : csvParser) {
//                    game.getTreacheryDeck().add(new TreacheryCard(csvRecord.get(0)));
//                }
//            }
//            TreacheryCard chemistry = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Chemistry")). findFirst().orElseThrow();
//            atreides.addTreacheryCard(chemistry);
//            // Move
//            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, chemistry, null));
//        }

        @Test
        void testBattlePlanLeaderAvailable() {
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderWithCheapHero() {
            atreides.addTreacheryCard(cheapHero);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, cheapHero, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanNoLeaderValid() {
            atreides.removeLeader("Duncan Idaho");
            atreides.removeLeader("Lady Jessica");
            atreides.removeLeader("Gurney Halleck");
            atreides.removeLeader("Thufir Hawat");
            atreides.removeLeader("Dr. Yueh");
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, null, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanOtherFactionLeaderInvalid() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, harkonnen, duncanIdaho, null, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanHarkonnenCapturedLeader() {
            harkonnen.addLeader(atreides.removeLeader("Duncan Idaho"));
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, harkonnen, duncanIdaho, null, false, 1, false, 1, null, null));
        }

        @Test
        void testBattlePlanFactionNotInvolved() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, bg, null, cheapHero, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanNotEnoughSpice() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false, 11, null, null));
        }

        @Test
        void testBattlePlanHasWeapon() {
            atreides.addTreacheryCard(crysknife);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, crysknife, null));
        }

        @Test
        void testBattlePlanHasDefense() {
            atreides.addTreacheryCard(shield);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, shield));
        }

//        @Test
//        void testBattlePlanDoesntHaveWeapon() {
//            // Move
//            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, crysknife, null));
//        }
//
//        @Test
//        void testBattlePlanDoesntHaveDefense() {
//            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false,3, null, shield));
//        }

        @Test
        void testBattlePlanSpendingTooMuch() {
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, true, 5, null, null));
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
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, null, cheapHero, true, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanNotEnoughTroops() {
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false, 10, null, null));
        }

        @Test
        void testBattlePlanResolution() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, null, null);
            assertEquals(Emojis.ATREIDES, battle1.getWinnerEmojis(game));
            assertEquals("2.5", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
            battle1.printBattleResolution(game, true);
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " captures"));
        }

        @Test
        void testBattlePlanResolutionLeaderKilled() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, crysknife, null);
            assertEquals(Emojis.HARKONNEN, battle1.getWinnerEmojis(game));
            assertEquals("0.5", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
            battle1.printBattleResolution(game, true);
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " captures a " + Emojis.ATREIDES + " leader\n"));
            int index = turnSummary.getMessages().getFirst().indexOf(Emojis.HARKONNEN + " captures a " + Emojis.ATREIDES + " leader");
            assertEquals(-1, turnSummary.getMessages().getFirst().substring(index + 1).indexOf(Emojis.HARKONNEN + " captures a " + Emojis.ATREIDES + " leader"));
        }

        @Test
        void testBattlePlanResolutionAggressorWinsTies() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, crysknife, null);
            assertTrue(battle1.isAggressorWin(game));
            assertEquals(Emojis.ATREIDES, battle1.getWinnerEmojis(game));
            assertEquals("1", battle1.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle1.getDefenderBattlePlan().getTotalStrengthString());
            battle1.printBattleResolution(game, true);
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " captures"));
        }

        @Test
        void testBattlePlanResolutionAggressorWinsTiesInHRSWithStrongholdCard() throws InvalidGameStateException {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            bt.addStrongholdCard(new StrongholdCard("Habbanya Sietch"));
            bt.addTreacheryCard(cheapHero);
            emperor.addTreacheryCard(cheapHero);
            battle3.setBattlePlan(game, bt, null, cheapHero, false, 1, false, 0, null, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 1, false, 0, null, null);
            assertTrue(battle3.isAggressorWin(game));
            assertEquals(Emojis.BT, battle3.getWinnerEmojis(game));
            assertEquals("1", battle3.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle3.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionDefenderWinsTiesInHRSWithStrongholdCard() throws InvalidGameStateException {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            emperor.addStrongholdCard(new StrongholdCard("Habbanya Sietch"));
            bt.addTreacheryCard(cheapHero);
            emperor.addTreacheryCard(cheapHero);
            battle3.setBattlePlan(game, bt, null, cheapHero, false, 1, false, 0, null, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 1, false, 0, null, null);
            assertFalse(battle3.isAggressorWin(game));
            assertEquals(Emojis.EMPEROR, battle3.getWinnerEmojis(game));
            assertEquals("1", battle3.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle3.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAllyEcaz() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle2.setEcazCombatant(game, "Atreides");
            battle2.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, crysknife, null);
            assertEquals(Emojis.ATREIDES + Emojis.ECAZ, battle2.getWinnerEmojis(game));
            assertTrue(battle2.isAggressorWin(game));
            assertEquals("3.5", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionAllyEcaz2() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setEcazCombatant(game, "Atreides");
            battle2.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, null, null);
            assertEquals(Emojis.ATREIDES + Emojis.ECAZ, battle2.getWinnerEmojis(game));
            assertEquals("5.5", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("1", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionEcazAlly() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle2.setEcazCombatant(game, "Ecaz");
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            battle2.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 0, true, 0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, crysknife, null);
            assertEquals(Emojis.ECAZ + Emojis.ATREIDES, battle2.getWinnerEmojis(game));
            assertFalse(battle2.isAggressorWin(game));
            assertEquals("1", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("3.5", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testBattlePlanResolutionEcazAlly2() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setEcazCombatant(game, "Ecaz");
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            battle2.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 0, true, 0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false, 1, null, null);
            assertEquals(Emojis.ECAZ + Emojis.ATREIDES, battle2.getWinnerEmojis(game));
            assertEquals("1", battle2.getAggressorBattlePlan().getTotalStrengthString());
            assertEquals("7.5", battle2.getDefenderBattlePlan().getTotalStrengthString());
        }

        @Test
        void testZoalHasOpposingLeaderValue() throws InvalidGameStateException {
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
            Leader zoal = bt.getLeader("Zoal").orElseThrow();
            bt.addTreacheryCard(crysknife);
            emperor.addTreacheryCard(cheapHero);
            battle3.setBattlePlan(game, bt, zoal, null, false, 5, false, 5, crysknife, null);
            battle3.setBattlePlan(game, emperor, null, cheapHero, false, 7, false, 5, null, null);
            assertEquals(Emojis.EMPEROR, battle3.getWinnerEmojis(game));
            assertEquals("5", battle3.getAggressorBattlePlan().getTotalStrengthString());
        }

        @Test
        void testSalusaSecundusHighSpiceNotNeededForSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            Force emperorForce = new Force("Emperor", 9);
            Force sardaukarForce = new Force("Emperor*", 3);
            Force bgForce = new Force("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(emperor.isSecundusHighThreshold());
            Battle battle = new Battle(game, "Wallach IX", List.of(wallachIX), List.of(emperor, bg), List.of(emperorForce, sardaukarForce, bgForce), null);
            Leader burseg = emperor.getLeader("Burseg").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }

        @Test
        void testSalusaSecundusLowSpiceNeededForSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            Force emperorForce = new Force("Emperor", 9);
            Force sardaukarForce = new Force("Emperor*", 1);
            Force bgForce = new Force("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(emperor.isSecundusHighThreshold());
            Battle battle = new Battle(game, "Wallach IX", List.of(wallachIX), List.of(emperor, bg), List.of(emperorForce, sardaukarForce, bgForce), null);
            Leader burseg = new Leader("Burseg", 1, null, false);
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }

        @Test
        void testSalusaSecundusOccupiedNegatesSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            Force sardaukarForce = new Force("Emperor*", 1);
            Force bgForce = new Force("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            HomeworldTerritory salusaSecundus = (HomeworldTerritory) game.getTerritory(emperor.getSecondHomeworld());
            salusaSecundus.addForces("Harkonnen", 1);
            game.removeForces("Salusa Secundus", emperor, 0, 5, false);
            assertTrue(emperor.isSecundusOccupied());
            Battle battle = new Battle(game, "Wallach IX", List.of(wallachIX), List.of(emperor, bg), List.of(sardaukarForce, bgForce), null);
            Leader burseg = emperor.getLeader("Burseg").orElseThrow();
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, emperor, burseg, null, false, 2, false, 1, null, null));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 1, false, 1, null, null));
        }
    }

    @Nested
    @DisplayName("#jacurutuSietchBattleResolution")
    class JacurutuSietchBattleResolution {
        Territory jacurutuSietch;
        Leader duncanIdaho;

        @BeforeEach
        void setUp() throws IOException {
            jacurutuSietch = game.getTerritories().addDiscoveryToken("Jacurutu Sietch", true);
            game.putTerritoryInAnotherTerritory(jacurutuSietch, garaKulon);

            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(emperor);

            jacurutuSietch.addForces("Atreides", 3);
            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        }

        @Test
        void testWinnerSpiceInJacurutuSietch() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            jacurutuSietch.addForces("Harkonnen", 5);
            Battle battle = new Battle(game, "Jacurutu Sietch", List.of(jacurutuSietch), List.of(atreides, harkonnen), jacurutuSietch.getForces(), null);
            assertEquals(atreides, battle.getAggressor(game));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, true);
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 5 " + Emojis.SPICE + " for 5 " + Emojis.HARKONNEN_TROOP + " not dialed.\n\n"));
        }

        @Test
        void testWinnerSpiceInJacurutuSietchNoUndialedForces() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            jacurutuSietch.addForces("Harkonnen", 5);
            Battle battle = new Battle(game, "Jacurutu Sietch", List.of(jacurutuSietch), List.of(atreides, harkonnen), jacurutuSietch.getForces(), null);
            assertEquals(atreides, battle.getAggressor(game));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, true, 0, null, null);
            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 2, true, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, true);
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 5 " + Emojis.SPICE + " for 5 " + Emojis.HARKONNEN_TROOP + " not dialed.\n\n"));
        }

        @Test
        void testWinnerSpiceInJacurutuSietchWithStarredUndialed() throws InvalidGameStateException {
            emperor.addTreacheryCard(cheapHero);
            jacurutuSietch.addForces("Emperor", 5);
            jacurutuSietch.addForces("Emperor*", 1);
            Battle battle = new Battle(game, "Jacurutu Sietch", List.of(jacurutuSietch), List.of(atreides, emperor), jacurutuSietch.getForces(), null);
            assertEquals(atreides, battle.getAggressor(game));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, true);
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 6 " + Emojis.SPICE + " for 5 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " not dialed.\n\n"));
        }

        @Test
        void testWinnerSpiceInJacurutuSietchAgainstEcazAlly() throws InvalidGameStateException {
            game.createAlliance(emperor, ecaz);
            emperor.addTreacheryCard(cheapHero);
            jacurutuSietch.addForces("Emperor", 5);
            jacurutuSietch.addForces("Emperor*", 1);
            jacurutuSietch.addForces("Ecaz", 3);
            Battle battle = new Battle(game, "Jacurutu Sietch", List.of(jacurutuSietch), List.of(atreides, emperor, ecaz), jacurutuSietch.getForces(), "Emperor");
            battle.setEcazCombatant(game, emperor.getName());
            assertEquals(atreides, battle.getAggressor(game));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, true);
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 7 " + Emojis.SPICE + " for 5 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " 1 " + Emojis.ECAZ_TROOP + " not dialed.\n\n"));
        }
    }

    @Nested
    @DisplayName("#battlePlans2")
    class BattlePlans2 {
        Leader duncanIdaho;
        Territory arrakeen;
        Battle battle1;
        Battle battle2;
        Battle battle2a;

        @BeforeEach
        void setUp() throws IOException {
            bt = null;

            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(emperor);
            game.addFaction(choam);
            arrakeen = game.getTerritory("Arrakeen");
            arrakeen.addForces("Atreides", 1);
            arrakeen.addForces("Harkonnen", 1);
            battle1 = new Battle(game, "Arrakeen", List.of(arrakeen), List.of(atreides, harkonnen), arrakeen.getForces(), "Atreides");
            game.createAlliance(atreides, ecaz);
            carthag.addForces("Ecaz", 5);
            carthag.addForces("Atreides", 1);
            battle2 = new Battle(game, "Carthag", List.of(carthag), List.of(atreides, harkonnen, ecaz), carthag.getForces(), "Atreides");
            battle2a = new Battle(game, "Carthag", List.of(carthag), List.of(harkonnen, atreides, ecaz), carthag.getForces(), "Atreides");

            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        }

        @Test
        void testBattlePlanResolutionAlliedWithChoam() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            game.createAlliance(choam, atreides);
            choam.setSpiceForAlly(2);
            choam.setAllySpiceForBattle(true);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,11, null, null));
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            battle1.printBattleResolution(game, false);
            assertNotEquals(-1, modInfo.getMessages().getLast().indexOf(Emojis.ATREIDES + " loses 9 " + Emojis.SPICE + " combat spice\n" + Emojis.CHOAM + " loses 2 " + Emojis.SPICE + " ally support\n" + Emojis.CHOAM + " gains 4 " + Emojis.SPICE + " combat spice"));
        }

        @Test
        void testBattlePlanResolutionAlliedWithEmperor() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            game.createAlliance(emperor, atreides);
            emperor.setSpiceForAlly(2);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,11, null, null));
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            battle1.printBattleResolution(game, false);
            assertNotEquals(-1, modInfo.getMessages().getLast().indexOf(Emojis.ATREIDES + " loses 9 " + Emojis.SPICE + " combat spice\n" + Emojis.EMPEROR + " loses 2 " + Emojis.SPICE + " ally support\n"));// + Emojis.CHOAM + " gains 5 " + Emojis.SPICE + " combat spice"));
        }
    }

//    @Nested
//    @DisplayName("#lasgunShieldDestroysEverything")
//    class LasgunShieldDestroysEverything {
////        Leader duncanIdaho;
////        TreacheryCard lasgun;
//        Battle battle;
//
//        @BeforeEach
//        void setUp() throws IOException, InvalidGameStateException {
//            game.addFaction(atreides);
//            game.addFaction(bg);
//            game.addFaction(harkonnen);
//            game.addFaction(ecaz);
//            game.addFaction(emperor);
//            game.addFaction(richese);
//            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
////            lasgun = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Lasgun")). findFirst().orElseThrow();
//            atreides.setForcesLost(7);
//            atreides.addTreacheryCard(lasgun);
//            harkonnen.addTreacheryCard(cheapHero);
//            harkonnen.addTreacheryCard(shield);
//            cielagoNorth_eastSector.addForces("Atreides", 5);
//            cielagoNorth_eastSector.addForces("Harkonnen", 3);
//            cielagoNorth_eastSector.setRicheseNoField(5);
//            cielagoNorth_eastSector.addForces("Advisor", 1);
//            // Ambassador test really should be moved to a new test setup in a Stronghold
//            ecaz.placeAmbassador(cielagoNorth_eastSector, "Ecaz");
//            cielagoNorth_westSector.addForces("Atreides", 2);
//            cielagoNorth_westSector.addForces("Emperor*", 3);
//            cielagoNorth_eastSector.setSpice(8);
//            battle = new Battle(game, "Arrakeen", List.of(cielagoNorth_eastSector), List.of(atreides, harkonnen), cielagoNorth_eastSector.getForces(), null);
//            battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 0, false, 0, lasgun, null);
//            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, shield);
//////            battle.printBattleResolution(game, false);
//        }
//
//        @Test
//        void testResolutionsReportsAdvisorsGetKilled() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.BG + " loses 1 " + Emojis.BG_ADVISOR + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionsReportsNoncombatantNoFieldRevealed() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be 5 " + Emojis.RICHESE_TROOP));
//        }
//
//        @Test
//        void testResolutionsReportsRicheseForcesGetKilled() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " loses 5 " + Emojis.RICHESE_TROOP + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionsReportsEmperorForcesInOtherSectorGetKilled() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionsReportsAtreidesForcesInBothSectorGetKilled() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 7 " + Emojis.ATREIDES_TROOP + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionsDoesNoReportAtreidesForcesSeparatelyInBattleSector() {
//            assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 5 " + Emojis.ATREIDES_TROOP + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionsReportsHarkonnenForcesInBothSectorGetKilled() {
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 3 " + Emojis.HARKONNEN_TROOP + " to the tanks"));
//        }
//
//        @Test
//        void testResolutionReportAmbassadorReturnedToEcazSupply() {
//            // Ambassador test really should be moved to a new test setup in a Stronghold
//            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " Ecaz ambassador returned to supply."));
//        }
//
//        @Test
//        void testResolutionReportsSpiceDestroyed() {
//            assertTrue(modInfo.getMessages().getFirst().contains("8 " + Emojis.SPICE + " destroyed in Cielago North (East Sector)."));
//        }
//
//        @Test
//        void testSeeMessage() {
//            assertTrue(modInfo.getMessages().getFirst().contains("KABOOM!"));
//            assertNull(modInfo.getMessages().getFirst());
//        }
//    }
}
