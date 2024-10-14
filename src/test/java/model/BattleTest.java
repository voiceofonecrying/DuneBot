package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleTest extends DuneTest {
    Battles battles;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
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
            garaKulon.setRicheseNoField(3);
            Territory richeseHomeworld = game.getTerritory("Richese");
            assertEquals(20, richeseHomeworld.getForceStrength("Richese"));
            garaKulon.placeForceFromReserves(game, richese, 5, false);
            assertEquals(15, richeseHomeworld.getForceStrength("Richese"));
            richeseHomeworld.removeForces("Richese", 13);
            assertEquals(2, richeseHomeworld.getForceStrength("Richese"));
            garaKulon.addForces("BG", 1);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
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
            garaKulon.addForces("Richese", 3);
            garaKulon.addForces("BG", 1);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
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
            garaKulon.addForces("Richese", 3);
            garaKulon.addForces("BG", 1);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor));
            assertFalse(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testAggressorMustChooseOpponentTrue() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            assertTrue(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testAggressorMustChooseOpponentEcazAllyFalse() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(fremen, harkonnen, emperor, ecaz));
            assertTrue(battle.hasEcazAndAlly());
            assertTrue(battle.aggressorMustChooseOpponent());
        }

        @Test
        void testEcazMustChooseBattleFactionFalseNoEcaz() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor));
            assertFalse(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazMustChooseBattleFactionFalseWithEcaz() {
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            assertFalse(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazMustChooseBattleFactionTrue() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            assertTrue(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazAllyFighting() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(ecaz, harkonnen, emperor));
            battle.setEcazCombatant(game, emperor.getName());
            assertEquals(emperor, battle.getDefender(game));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 5, null, null));
        }

        @Test
        void testEcazFighting() {
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
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
            battle.updateTroopsDialed(game, "Ecaz", 5, 0);
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(fremen, richese));
            Leader ladyHelena = richese.getLeader("Lady Helena").orElseThrow();
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 3, false, 0, null, null));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 2, false, 0, null, null));
        }

        @Test
        void testBattleResolved() {
            carthag.addForces("Emperor", 5);
            carthag.addForces("Emperor*", 2);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
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
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
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
    @DisplayName("#aggregateForces")
    class AggregateForces {
        @Test
        void testAggregateForces() {
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            carthag.addForces("Emperor", 5);
            carthag.addForces("Emperor*", 2);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            List<Force> battleForces = battle.aggregateForces(List.of(carthag), List.of(emperor, harkonnen));
            assertEquals(carthag.getForceStrength("Emperor*"), battleForces.stream().filter(f -> f.getName().equals("Emperor*")).findFirst().orElseThrow().getStrength());
            assertEquals(carthag.getForceStrength("Emperor"), battleForces.stream().filter(f -> f.getName().equals("Emperor")).findFirst().orElseThrow().getStrength());
            assertEquals(carthag.getForceStrength("Harkonnen"), battleForces.stream().filter(f -> f.getName().equals("Harkonnen")).findFirst().orElseThrow().getStrength());
        }

        @Test
        void testAggregateForcesMultipleSectorsPlusNoField() {
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            game.addFaction(richese);
            cielagoNorth_eastSector.addForces("Emperor", 2);
            cielagoNorth_eastSector.addForces("Harkonnen", 3);
            cielagoNorth_westSector.addForces("Emperor", 2);
            cielagoNorth_westSector.addForces("Emperor*", 2);
            cielagoNorth_westSector.setRicheseNoField(5);
            List<Territory> cielagoNorthSectors = List.of(cielagoNorth_westSector, cielagoNorth_middleSector, cielagoNorth_eastSector);
            Battle battle = new Battle(game, cielagoNorthSectors, List.of(emperor, harkonnen, richese));
            List<Force> battleForces = battle.aggregateForces(cielagoNorthSectors, List.of(emperor, harkonnen, richese));
            assertEquals(2, battleForces.stream().filter(f -> f.getName().equals("Emperor*")).findFirst().orElseThrow().getStrength());
            assertEquals(4, battleForces.stream().filter(f -> f.getName().equals("Emperor")).findFirst().orElseThrow().getStrength());
            assertEquals(3, battleForces.stream().filter(f -> f.getName().equals("Harkonnen")).findFirst().orElseThrow().getStrength());
            assertEquals(5, battleForces.stream().filter(f -> f.getName().equals("NoField")).findFirst().orElseThrow().getStrength());
        }
    }

    @Nested
    @DisplayName("#battlePlans")
    class BattlePlans {
        Territory habbanyaSietch;
        Battle battle1;
        Battle battle2;
        Battle battle2a;
        Battle battle3;
        TestTopic gameActions;

        @BeforeEach
        void setUp() throws IOException {
            gameActions = new TestTopic();
            game.setGameActions(gameActions);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(bt);
            game.addFaction(emperor);
            arrakeen.addForces("Harkonnen", 1);
            battle1 = new Battle(game, List.of(arrakeen), List.of(atreides, harkonnen));
            game.createAlliance(atreides, ecaz);
            turnSummary.clear();
            carthag.addForces("Ecaz", 5);
            carthag.addForces("Atreides", 1);
            battle2 = new Battle(game, List.of(carthag), List.of(atreides, harkonnen, ecaz));
            battle2a = new Battle(game, List.of(carthag), List.of(harkonnen, atreides, ecaz));
            habbanyaSietch = game.getTerritory("Habbanya Sietch");
            habbanyaSietch.addForces("BT", 6);
            habbanyaSietch.addForces("Emperor", 6);
            habbanyaSietch.addForces("Emperor*", 5);
            battle3 = new Battle(game, List.of(habbanyaSietch), List.of(bt, emperor));

            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        }

        @Test
        void testFirstSubmissionPublishesToGameActionsSecondDoesNot() throws InvalidGameStateException {
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            assertEquals(Emojis.ATREIDES + " battle plan submitted.", gameActions.getMessages().getLast());
            gameActions.clear();
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            assertTrue(gameActions.getMessages().isEmpty());
            battle1.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
            assertEquals(Emojis.HARKONNEN + " battle plan submitted.", gameActions.getMessages().getLast());
            gameActions.clear();
            battle1.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
            assertTrue(gameActions.getMessages().isEmpty());
        }

        @Test
        void testBattlePlanMessagePublishedAfterChoosingForcesDialed() throws InvalidGameStateException {
            battle3.setBattlePlan(game, emperor, burseg, null, false, 1, false, 0, null, null);
            assertTrue(gameActions.getMessages().isEmpty());
            battle3.updateTroopsDialed(game, "Emperor", 2, 0);
            assertEquals(Emojis.EMPEROR + " battle plan submitted.", gameActions.getMessages().getLast());
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
            tueksSietch.addForces("Harkonnen", 11);
            tueksSietch.addForces("Ecaz", 10);
            Battle ecazBattle = new Battle(game, List.of(tueksSietch), List.of(harkonnen, ecaz));
            harkonnen.addTreacheryCard(hunterSeeker);
            harkonnen.addTreacheryCard(weirdingWay);
            ecazBattle.setBattlePlan(game, harkonnen, feydRautha, null, false, 6, false, 4, hunterSeeker, weirdingWay);
            ecazBattle.setBattlePlan(game, ecaz, ecaz.getLeader("Bindikk Narvi").orElseThrow(), null, false, 5, false, 0, null, null);
            BattlePlan atreidesPlan = ecazBattle.getAggressorBattlePlan();
            BattlePlan ecazPlan = ecazBattle.getDefenderBattlePlan();
            atreidesPlan.revealOpponentBattlePlan(ecazPlan);
            ecazPlan.revealOpponentBattlePlan(atreidesPlan);
            assertEquals("5", ecazPlan.getTotalStrengthString());
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
            bt.addTreacheryCard(crysknife);
            battle3.setBattlePlan(game, bt, zoal, null, false, 5, false, 5, crysknife, null);
            battle3.setBattlePlan(game, emperor, burseg, null, false, 7, false, 5, null, null);
            assertEquals(Emojis.BT, battle3.getWinnerEmojis(game));
            assertEquals("8", battle3.getAggressorBattlePlan().getTotalStrengthString());
        }

        @Test
        void testZoalHasNoValue() throws InvalidGameStateException {
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
            wallachIX.addForces("Emperor", 9);
            wallachIX.addForces("Emperor*", 3);
            wallachIX.addForces("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(emperor.isSecundusHighThreshold());
            Battle battle = new Battle(game, List.of(wallachIX), List.of(emperor, bg));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }

        @Test
        void testSalusaSecundusLowSpiceNeededForSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            wallachIX.addForces("Emperor", 9);
            wallachIX.addForces("Emperor*", 1);
            wallachIX.addForces("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(emperor.isSecundusHighThreshold());
            Battle battle = new Battle(game, List.of(wallachIX), List.of(emperor, bg));
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, emperor, burseg, null, false, 11, false, 1, null, null));
        }

        @Test
        void testSalusaSecundusOccupiedNegatesSardaukar() {
            Territory wallachIX = new Territory("Wallach IX", -1, false, false, false);
            wallachIX.addForces("Emperor*", 1);
            wallachIX.addForces("BG", 15);
            game.addGameOption(GameOption.HOMEWORLDS);
            HomeworldTerritory salusaSecundus = (HomeworldTerritory) game.getTerritory(emperor.getSecondHomeworld());
            salusaSecundus.addForces("Harkonnen", 1);
            game.removeForces("Salusa Secundus", emperor, 0, 5, false);
            assertTrue(emperor.isSecundusOccupied());
            Battle battle = new Battle(game, List.of(wallachIX), List.of(emperor, bg));
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, emperor, burseg, null, false, 2, false, 1, null, null));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 1, false, 1, null, null));
        }
    }

    @Nested
    @DisplayName("#jacurutuSietchBattleResolution")
    class JacurutuSietchBattleResolution {
        Territory jacurutuSietch;

        @BeforeEach
        void setUp() throws IOException {
            jacurutuSietch = game.getTerritories().addDiscoveryToken("Jacurutu Sietch", true);
            game.putTerritoryInAnotherTerritory(jacurutuSietch, garaKulon);

            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(emperor);

            jacurutuSietch.addForces("Atreides", 3);
        }

        @Test
        void testWinnerSpiceInJacurutuSietch() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            jacurutuSietch.addForces("Harkonnen", 5);
            Battle battle = new Battle(game, List.of(jacurutuSietch), List.of(atreides, harkonnen));
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
            Battle battle = new Battle(game, List.of(jacurutuSietch), List.of(atreides, harkonnen));
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
            Battle battle = new Battle(game, List.of(jacurutuSietch), List.of(atreides, emperor));
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
            Battle battle = new Battle(game, List.of(jacurutuSietch), List.of(atreides, emperor, ecaz));
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
        Battle battle1;
        Battle battle2;
        Battle battle2a;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            game.addFaction(emperor);
            game.addFaction(choam);
            arrakeen.addForces("Atreides", 1);
            arrakeen.addForces("Harkonnen", 1);
            battle1 = new Battle(game, List.of(arrakeen), List.of(atreides, harkonnen));
            game.createAlliance(atreides, ecaz);
            carthag.addForces("Ecaz", 5);
            carthag.addForces("Atreides", 1);
            battle2 = new Battle(game, List.of(carthag), List.of(atreides, harkonnen, ecaz));
            battle2a = new Battle(game, List.of(carthag), List.of(harkonnen, atreides, ecaz));

        }

        @Test
        void testBattlePlanResolutionAlliedWithChoam() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            game.createAlliance(choam, atreides);
            choam.setSpiceForAlly(2);
            choam.setAllySpiceForBattle(true);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,11, null, null));
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            modInfo.clear();
            battle1.printBattleResolution(game, false);
            assertNotEquals(-1, modInfo.getMessages().getFirst().indexOf(Emojis.ATREIDES + " loses 9 " + Emojis.SPICE + " combat spice\n" + Emojis.CHOAM + " loses 2 " + Emojis.SPICE + " ally support\n" + Emojis.CHOAM + " gains 4 " + Emojis.SPICE + " combat spice"));
        }

        @Test
        void testBattlePlanResolutionAlliedWithEmperor() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            game.createAlliance(emperor, atreides);
            emperor.setSpiceForAlly(2);
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 11, false,11, null, null));
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            modInfo.clear();
            battle1.printBattleResolution(game, false);
            assertNotEquals(-1, modInfo.getMessages().getFirst().indexOf(Emojis.ATREIDES + " loses 9 " + Emojis.SPICE + " combat spice\n" + Emojis.EMPEROR + " loses 2 " + Emojis.SPICE + " ally support\n"));// + Emojis.CHOAM + " gains 5 " + Emojis.SPICE + " combat spice"));
        }
    }

    @Nested
    @DisplayName("#lasgunShieldDestroysEverythingEvenAcrossStorm")
    class LasgunShieldDestroysEverythingEvenAcrossStorm {
        Battle battle;

        @BeforeEach
        void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            atreides.setForcesLost(7);
            atreides.addTreacheryCard(lasgun);
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(shield);
            cielagoNorth_eastSector.addForces("Atreides", 5);
            cielagoNorth_eastSector.addForces("Harkonnen", 2);
            cielagoNorth_eastSector.addForces("Advisor", 1);
            // Ambassador test really should be moved to a new test setup in a Stronghold
            ecaz.placeAmbassador(cielagoNorth_eastSector, "Fremen");
            cielagoNorth_westSector.addForces("Atreides", 2);
            cielagoNorth_westSector.addForces("Harkonnen", 1);
            cielagoNorth_westSector.addForces("Emperor", 1);
            cielagoNorth_westSector.addForces("Emperor*", 3);
            cielagoNorth_westSector.addForces("Advisor", 1);
            cielagoNorth_westSector.setRicheseNoField(5);
            cielagoNorth_westSector.setSpice(8);
            // battle specification reflects storm being in sector 1 and splitting Cielago North
            battle = new Battle(game, List.of(cielagoNorth_eastSector), List.of(atreides, harkonnen));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 0, false, 0, lasgun, null);
        }

        @Test
        void testlasgunShieldWithHarrassAndWithdraw() throws InvalidGameStateException {
            TreacheryCard harassAndWithdraw = new TreacheryCard("Harass and Withdraw");
            harkonnen.addTreacheryCard(harassAndWithdraw);
            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, harassAndWithdraw, shield);
            modInfo.clear();
            battle.printBattleResolution(game, false);
            assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " returns 2 " + Emojis.HARKONNEN_TROOP + " to reserves with Harass and Withdraw"));
        }

        @Nested
        @DisplayName("#hasLasgunShield")
        class HasLasgunShield {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, shield);
                modInfo.clear();
                battle.printBattleResolution(game, false);
            }

            @Test
            void testResolutionReportsAdvisorsGetKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.BG + " loses 1 " + Emojis.BG_ADVISOR + " in Cielago North (West Sector) to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.BG + " loses 1 " + Emojis.BG_ADVISOR + " in Cielago North (East Sector) to the tanks"));
            }

            @Test
            void testResolutionReportsNoncombatantNoFieldRevealed() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be 5 " + Emojis.RICHESE_TROOP + " and loses them to the tanks"));
            }

            @Test
            void testResolutionReportsEmperorForcesInTheTerritory() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 1 " + Emojis.EMPEROR_TROOP + " in Cielago North (West Sector) to the tanks\n" + Emojis.EMPEROR + " loses 3 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) to the tanks"));
            }

            @Test
            void testResolutionReportsKHKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses Kwisatz Haderach to the tanks"));
            }

            @Test
            void testResolutionReportsAtreidesForcesInBothSectorGetKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.ATREIDES_TROOP + " in Cielago North (West Sector) to the tanks"));
            }

            @Test
            void testResolutionReportsHarkonnenForcesInBothSectorGetKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 1 " + Emojis.HARKONNEN_TROOP + " in Cielago North (West Sector) to the tanks"));
            }

            @Test
            void testResolutionReportsAmbassadorReturnedToEcazSupply() {
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " Fremen ambassador returned to supply"));
            }

            @Test
            void testResolutionReportsSpiceDestroyed() {
                assertTrue(modInfo.getMessages().getFirst().contains("8 " + Emojis.SPICE + " destroyed in Cielago North (West Sector)"));
            }

            @Test
            void testKaboom() {
                assertTrue(modInfo.getMessages().getFirst().contains("KABOOM!"));
            }

            @Test
            void testTraitorCallNegatesLasgunShield() throws InvalidGameStateException {
                harkonnen.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                modInfo.clear();
                battle.printBattleResolution(game, false);
                assertFalse(modInfo.getMessages().getFirst().contains("KABOOM!"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " calls Traitor against Duncan Idaho!"));
            }
        }

        @Nested
        @DisplayName("#noLasgunShield")
        class NoLasgunShield {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
                modInfo.clear();
                battle.printBattleResolution(game, false);
            }

            @Test
            void testResolutionDoesNotMentionAdvisors() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.BG_ADVISOR));
            }

            @Test
            void testResolutionDoesNotMentionNoField() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.NO_FIELD));
            }

            @Test
            void testResolutionDoesNotMentionEmperorForcesInTheTerritory() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR));
            }

            @Test
            void testResolutionReportsKHKilled() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses Kwisatz Haderach to the tanks"));
            }

            @Test
            void testResolutionReportsHarkonnenForcesInBattleSectorOnlyGetKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 2 " + Emojis.HARKONNEN_TROOP + " to the tanks"));
            }
            @Test
            void testResolutionDoesNotMentionAmbassador() {
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertFalse(modInfo.getMessages().getFirst().contains("ambassador"));
            }

            @Test
            void testResolutionDoesNotMentionSpiceDestroyed() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.SPICE + " destroyed"));
            }

            @Test
            void testNoKaboom() {
                assertFalse(modInfo.getMessages().getFirst().contains("KABOOM!"));
            }
        }

        @Nested
        @DisplayName("#lasgunShieldWithTraitorCall")
        class LasgunShieldWithTraitorCall {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, shield);
                modInfo.clear();
                atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                battle.printBattleResolution(game, false);
            }


            @Test
            void testResolutionDoesNotMentionAdvisors() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.BG_ADVISOR));
            }

            @Test
            void testResolutionDoesNotMentionNoField() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.NO_FIELD));
            }

            @Test
            void testResolutionDoesNotMentionEmperorForcesInTheTerritory() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR));
            }

            @Test
            void testResolutionReportsKHKilled() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses Kwisatz Haderach to the tanks"));
            }

            @Test
            void testResolutionReportsHarkonnenForcesInBattleSectorOnlyGetKilled() {
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 2 " + Emojis.HARKONNEN_TROOP + " to the tanks"));
            }
            @Test
            void testResolutionDoesNotMentionAmbassador() {
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertFalse(modInfo.getMessages().getFirst().contains("ambassador"));
            }

            @Test
            void testResolutionDoesNotMentionSpiceDestroyed() {
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.SPICE + " destroyed"));
            }

            @Test
            void testNoKaboom() {
                assertFalse(modInfo.getMessages().getFirst().contains("KABOOM!"));
            }
        }
    }

    @Nested
    @DisplayName("#traitorCalls")
    class TraitorCalls {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(bg);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(bt);
            atreides.addTreacheryCard(chaumas);
            arrakeen.addForces("BG", 1);
            battle = new Battle(game, List.of(arrakeen), List.of(bg, atreides));
        }

        @Test
        void testNeitherHasCorrectTraitor() throws InvalidGameStateException {
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " cannot call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testAggressorCanCallTraitor() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " can call Traitor against Duncan Idaho in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertEquals("Will you call Traitor against Duncan Idaho in Arrakeen? bg", bgChat.getMessages().getLast());
            assertEquals("The following must be decided before the battle can be resolved:\n  Aggressor Traitor Call", modInfo.getMessages().getFirst());
        }

        @Test
        void testDefenderCanCallTraitor() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Alia", "BG", 5));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertTrue(battle.isAggressorWin(game));
            assertTrue(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " cannot call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " can call Traitor against Alia in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertTrue(battle.isAggressorWin(game));
            assertTrue(battle.getAggressorBattlePlan().isLeaderAlive());
            assertEquals("Will you call Traitor against Alia in Arrakeen? at", atreidesChat.getMessages().getLast());
            assertEquals("The following must be decided before the battle can be resolved:\n  Defender Traitor Call", modInfo.getMessages().getFirst());
        }

        @Test
        void testBothCanCallTraitor() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Alia", "BG", 5));
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " can call Traitor against Duncan Idaho in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " can call Traitor against Alia in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertEquals("Will you call Traitor against Duncan Idaho in Arrakeen? bg", bgChat.getMessages().getLast());
            assertEquals("Will you call Traitor against Alia in Arrakeen? at", atreidesChat.getMessages().getLast());
            assertEquals("The following must be decided before the battle can be resolved:\n  Aggressor Traitor Call, Defender Traitor Call", modInfo.getMessages().getFirst());
        }

        @Test
        void testKHPreventsTraitor() throws InvalidGameStateException {
            atreides.setForcesLost(7);
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            int bgTraitorMessageIndex = modInfo.getMessages().size() - 2;
            assertEquals(Emojis.BG + " cannot call Traitor against Kwisatz Haderach.", modInfo.getMessages().get(bgTraitorMessageIndex));
            modInfo.clear();
            bgChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testFactionHadNoLosses () throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertTrue(battle.isAggressorWin(game));
            assertNotEquals("You won with no losses. Will you call Traitor against Duncan Idaho in Arrakeen? p", bgChat.getMessages().getLast());
        }

        @Test
        void testCheapHeroTraitor() throws InvalidGameStateException {
            atreides.addTreacheryCard(cheapHero);
            bg.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, null, cheapHero, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertEquals("Will you call Traitor against Cheap Hero in Arrakeen? bg", bgChat.getMessages().getLast());
        }

        @Test
        void testBothCallTraitor() {
            // Might not belong here. This is post Traitor call
        }

        @Test
        void testBTDoNotGetAskedAboutTraitor() throws InvalidGameStateException {
            carthag.addForces("BT", 1);
            bt.addTraitorCard(new TraitorCard("Feyd Rautha", "Harkonnen", 6));
            battle = new Battle(game, List.of(carthag), List.of(harkonnen, bt));
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 1, false, 0, null, null);
            battle.setBattlePlan(game, bt, zoal, null, false, 1, false, 1, null, null);
            modInfo.clear();
            battle.printBattleResolution(game, false);
            assertEquals(Emojis.BT + " does not call Traitors.", modInfo.getMessages().getLast());
            modInfo.clear();
            btChat.clear();
            battle.printBattleResolution(game, true);
            assertTrue(btChat.getMessages().isEmpty());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testFactionDeclinesTraitorWithBattleSubmission() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Lady Jessica", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.willCallTraitor(game, bg, false, 0, "Arrakeen");
            assertEquals(Emojis.BG + " declines calling Traitor in Arrakeen.", modInfo.getMessages().getLast());
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " declined calling Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertFalse(modInfo.getMessages().contains(Emojis.BG + " cannot call Traitor in Arrakeen."));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testFactionDeclinesTraitorAfterResolutionPublished() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " can call Traitor against Duncan Idaho in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertEquals("Will you call Traitor against Duncan Idaho in Arrakeen? bg", bgChat.getMessages().getLast());
            assertEquals(2, bgChat.getChoices().getLast().size());
            battle.willCallTraitor(game, bg, false, 0, "Arrakeen");
            assertEquals(Emojis.BG + " declines calling Traitor in Arrakeen.", modInfo.getMessages().getLast());
        }

        @Test
        void testFactionWillCallTraitorWithBattlePlanSubmissionCorrectLeader() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertEquals(Emojis.BG + " will call Traitor in Arrakeen if possible.", modInfo.getMessages().getLast());
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " will call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for you!", bgChat.getMessages().getLast());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testFactionWillCallTraitorWithBattlePlanSubmissionWrongLeader() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Lady Jessica", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertEquals(Emojis.BG + " will call Traitor in Arrakeen if possible.", modInfo.getMessages().getLast());
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " cannot call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertFalse(bgChat.getMessages().contains("Will you call Traitor against Lady Jessica in Arrakeen? p"));
            assertFalse(bgChat.getMessages().contains("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for you!"));
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
        }

        @Test
        void testFactionWillCallTraitorAfterResolutionPublishedCorrectLeader() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " can call Traitor against Duncan Idaho in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertEquals("Will you call Traitor against Duncan Idaho in Arrakeen? bg", bgChat.getMessages().getLast());
            assertEquals(2, bgChat.getChoices().getLast().size());
            assertEquals("The following must be decided before the battle can be resolved:\n  Aggressor Traitor Call", modInfo.getMessages().getFirst());
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertFalse(modInfo.getMessages().contains(Emojis.BG + " will call Traitor in Arrakeen if possible."));
            assertTrue(modInfo.getMessages().contains(Emojis.BG + " calls Traitor in Arrakeen!"));
            assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for you!", bgChat.getMessages().getLast());
        }

        @Test
        void testTraitorCallKillsTraitorLeader() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses Duncan Idaho to the tanks"));
            assertTrue(battle.getDefenderBattlePlan().isLeaderAlive());
            battle.printBattleResolution(game, true);
            turnSummary.clear();
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " loses Duncan Idaho to the tanks"));
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.BG + " gains 2 " + Emojis.SPICE + " combat water"));
        }

        @Test
        void testFactionWillCallTraitorAfterResolutionPublishedWrongLeader() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Lady Jessica", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertTrue(atreidesChat.getMessages().isEmpty());
            assertEquals(Emojis.BG + " cannot call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            assertEquals(Emojis.ATREIDES + " cannot call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertFalse(battle.isAggressorWin(game));
            assertFalse(battle.getAggressorBattlePlan().isLeaderAlive());
            assertTrue(bgChat.getMessages().isEmpty());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertEquals("You cannot call Traitor.", bgChat.getMessages().getLast());
        }

        @Nested
        @DisplayName("#harkonnenAllyPower")
        class HarkonnenAllyPower {
            TraitorCard duncanTraitor;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                duncanTraitor = new TraitorCard("Duncan Idaho", "Atreides", 2);
                harkonnen.addTraitorCard(duncanTraitor);
                harkonnen.addTraitorCard(new TraitorCard("Gurney Halleck", "Atreides", 4));
                harkonnen.addTraitorCard(new TraitorCard("Alia", "BG", 5));
                game.createAlliance(bg, harkonnen);
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
            }

            @Test
            void testHarkonnenAllyNotAskedWithBattlePlanSubmission() {
                assertTrue(harkonnenChat.getMessages().isEmpty());
                assertFalse(battle.getAggressorBattlePlan().isHarkCanCallTraitor());
            }

            @Test
            void testHarkonnenAllyAskedAfterFactionSaysTheyWillCallTraitor() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                assertEquals("Will you call Traitor for your ally if " + Emojis.ATREIDES + " plays Duncan Idaho or Gurney Halleck? ha", harkonnenChat.getMessages().getLast());
                assertTrue(battle.getAggressorBattlePlan().isHarkCanCallTraitor());
                assertEquals(Emojis.HARKONNEN + " can call Traitor for ally " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAllySaysNoBeforeResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.willCallTraitor(game, harkonnen, false, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " declines calling Traitor for " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAllySaysYesBeforeResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.willCallTraitor(game, harkonnen, true, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " will call Traitor for " + Emojis.BG + " in Arrakeen if possible.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAllySaysNoAfterResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
                assertEquals("Will you call Traitor for your ally against Duncan Idaho in Arrakeen? ha", harkonnenChat.getMessages().getLast());
                battle.willCallTraitor(game, harkonnen, false, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " declines calling Traitor for " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAllySaysYesAfterResolutionPublishedCorrectLeader() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
                assertEquals("Will you call Traitor for your ally against Duncan Idaho in Arrakeen? ha", harkonnenChat.getMessages().getLast());
                battle.willCallTraitor(game, harkonnen, true, 0, "Arrakeen");
                assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for your ally!", harkonnenChat.getMessages().getLast());
                assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for " + Emojis.HARKONNEN + " and you!", bgChat.getMessages().getLast());
            }

            @Test
            void testHarkonnenAllySaysYesAfterResolutionPublishedWrongLeader() throws InvalidGameStateException {
                harkonnen.removeTraitorCard(duncanTraitor);
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
                bgChat.clear();
                battle.willCallTraitor(game, harkonnen, true, 0, "Arrakeen");
                assertEquals("You cannot call Traitor for " + Emojis.BG + ".", harkonnenChat.getMessages().getLast());
                assertTrue(bgChat.getMessages().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("#battleResolutionWithTraitorCall")
    class BattleResolutionWithTraitorCall {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(bg);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(bt);
            atreides.addTreacheryCard(chaumas);
            arrakeen.addForces("BG", 1);
            battle = new Battle(game, List.of(arrakeen), List.of(bg, atreides));
        }

        @Test
        void testAggressorCallsTraitorWithBattlePlan() throws InvalidGameStateException {
            bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
            battle.setBattlePlan(game, bg, alia, null, false, 0, true, 0, null, null);
            battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
            assertEquals(Emojis.BG + " will call Traitor in Arrakeen if possible.", modInfo.getMessages().getLast());
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(modInfo.getMessages().getFirst().contains("Alia to the tanks"));
            assertFalse(modInfo.getMessages().getFirst().contains(Emojis.BG_FIGHTER + " to the tanks"));
            assertEquals(Emojis.BG + " will call Traitor in Arrakeen.", modInfo.getMessages().get(1));
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for you!", bgChat.getMessages().getLast());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.BG + " calls Traitor against Duncan Idaho!"));
            assertFalse(turnSummary.getMessages().getFirst().contains("Alia to the tanks"));
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.BG_FIGHTER + " to the tanks"));
        }

        @Test
        void testDefenderCallsTraitorWithBattlePlan() throws InvalidGameStateException {
            atreides.addTraitorCard(new TraitorCard("Alia", "BG", 5));
            bg.addTreacheryCard(crysknife);
            battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, crysknife, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, null, null);
            battle.willCallTraitor(game, atreides, true, 0, "Arrakeen");
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, false);
            assertFalse(modInfo.getMessages().getFirst().contains("Duncan Idaho to the tanks"));
            assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES_TROOP + " to the tanks"));
            assertEquals(Emojis.ATREIDES + " will call Traitor in Arrakeen.", modInfo.getMessages().getLast());
            modInfo.clear();
            bgChat.clear();
            atreidesChat.clear();
            battle.printBattleResolution(game, true);
            assertEquals("Alia has betrayed " + Emojis.BG + " for you!", atreidesChat.getMessages().getLast());
            assertEquals("The battle can be resolved.", modInfo.getMessages().getFirst());
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " calls Traitor against Alia!"));
            assertFalse(turnSummary.getMessages().getFirst().contains("Duncan Idaho to the tanks"));
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES_TROOP + " to the tanks"));
        }
    }

    @Nested
    @DisplayName("#resolveBattle")
    class ResolveBattle {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(richese);
            game.addFaction(atreides);
            richese.addTreacheryCard(cheapHero);
            garaKulon.setRicheseNoField(3);
            garaKulon.addForces("Atreides", 5);
            battle = new Battle(game, List.of(garaKulon), List.of(richese, atreides));
        }

        @Nested
        @DisplayName("#resolutionWithNoField")
        class ResolutionWithNoField {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotRevealTheNoField() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " reveals"));
                assertEquals(3, garaKulon.getRicheseNoField());
            }

            @Test
            void testPublishDoesNotRevealTheNoField() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " reveals"));
                assertEquals(3, garaKulon.getRicheseNoField());
            }

            @Test
            void testResolveRevealsTheNoField() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(garaKulon.hasRicheseNoField());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("The 3 " + Emojis.NO_FIELD + " in Gara Kulon reveals 3 " + Emojis.RICHESE_TROOP)));
            }
        }

        @Nested
        @DisplayName("#resolutionWithKilledLeader")
        class ResolutionWithKilledLeader {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                richese.addTreacheryCard(chaumas);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, chaumas, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillLeader() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains("Duncan Idaho to the tanks"));
                assertTrue(atreides.getLeaders().contains(duncanIdaho));
                assertFalse(game.getLeaderTanks().contains(duncanIdaho));
            }

            @Test
            void testPublishDoesNotKillLeader() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains("Duncan Idaho to the tanks"));
                assertTrue(atreides.getLeaders().contains(duncanIdaho));
                assertFalse(game.getLeaderTanks().contains(duncanIdaho));
            }

            @Test
            void testResolveKillsLeader() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(atreides.getLeaders().contains(duncanIdaho));
                assertTrue(game.getLeaderTanks().contains(duncanIdaho));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " Duncan Idaho was sent to the tanks.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithKilledKH")
        class ResolutionWithKilledKH {
            Leader kwisatzHaderach;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.setForcesLost(7);
                kwisatzHaderach = atreides.getLeader("Kwisatz Haderach").orElseThrow();
//                atreides.addLeader(kwisatzHaderach);
                richese.addTreacheryCard(lasgun);
                richese.addTreacheryCard(shield);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, lasgun, shield);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 0, false, 0, null, null);
                assertTrue(battle.getDefenderBattlePlan().isLasgunShieldExplosion());
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillKH() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains("Kwisatz Haderach to the tanks"));
                assertTrue(atreides.getLeaders().contains(kwisatzHaderach));
                assertFalse(game.getLeaderTanks().contains(kwisatzHaderach));
            }

            @Test
            void testPublishDoesNotKillKH() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains("Kwisatz Haderach to the tanks"));
                assertTrue(atreides.getLeaders().contains(kwisatzHaderach));
                assertFalse(game.getLeaderTanks().contains(kwisatzHaderach));
            }

            @Test
            void testResolveKillsKH() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(atreides.getLeaders().contains(kwisatzHaderach));
                assertTrue(game.getLeaderTanks().contains(kwisatzHaderach));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " Kwisatz Haderach was sent to the tanks.")));
            }
        }
    }

    @Nested
    @DisplayName("#resolveBattleMultipleSectors")
    class resolveBattleMultipleSectors {
        Battle battle;
        Territory kaitain;
        Territory salusaSecundus;

        @BeforeEach
        void setUp() {
            game.addFaction(emperor);
            game.addFaction(atreides);
            emperor.addTreacheryCard(cheapHero);
            kaitain = game.getTerritory(emperor.getHomeworld());
            salusaSecundus = game.getTerritory(emperor.getSecondHomeworld());
            cielagoNorth_eastSector.setRicheseNoField(3);
            cielagoNorth_eastSector.addForces("Atreides", 5);
            kaitain.removeForces("Emperor", 5);
            cielagoNorth_eastSector.addForces("Emperor", 3);
            cielagoNorth_westSector.addForces("Emperor", 2);
            salusaSecundus.removeForces("Emperor*", 4);
            cielagoNorth_eastSector.addForces("Emperor*", 2);
            cielagoNorth_westSector.addForces("Emperor*", 2);
            battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(emperor, atreides));
        }

        @Nested
        @DisplayName("#resolutionWithHarassAndWithdrawFromLoser")
        class ResolutionWithHarassAndWithdrawFromLoser {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                emperor.addTreacheryCard(harassAndWithdraw);
                emperor.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 1, true, 0, harassAndWithdraw, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Harass and Withdraw"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Harass and Withdraw"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveWithdrawsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(14, kaitain.getForceStrength("Emperor"));
                assertEquals(4, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Harass and Withdraw.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Harass and Withdraw.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithHarassAndWithdrawFromWinner")
        class ResolutionWithHarassAndWithdrawFromWinner {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                emperor.addTreacheryCard(harassAndWithdraw);
                emperor.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, emperor, burseg, null, false, 1, true, 0, harassAndWithdraw, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Harass and Withdraw"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Harass and Withdraw"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveWithdrawsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(14, kaitain.getForceStrength("Emperor"));
                assertEquals(4, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Harass and Withdraw.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Harass and Withdraw.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSukGraduateInFront")
        class ResolutionWithSukGraduateInFront {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                bashar.setSkillCard(new LeaderSkillCard("Suk Graduate"));
                bashar.setPulledBehindShield(false);
                battle.setBattlePlan(game, emperor, burseg, null, false, 1, true, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 1 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Suk Graduate"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " returns 1 " + Emojis.EMPEROR_SARDAUKAR + " to reserves with Suk Graduate"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveReturnsForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(2, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Suk Graduate.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSukGraduateBehind")
        class ResolutionWithSukGraduateBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                burseg.setSkillCard(new LeaderSkillCard("Suk Graduate"));
                burseg.setPulledBehindShield(true);
                battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " saves 3 " + Emojis.EMPEROR_SARDAUKAR + " and may leave 1 in the territory with Suk Graduate"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " saves 3 " + Emojis.EMPEROR_SARDAUKAR + " and may leave 1 in the territory with Suk Graduate"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveReturnsForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(3, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.EMPEROR + " leaves 1 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North, may return it to reserves.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_SARDAUKAR + " returned to reserves with Suk Graduate.")));
            }
        }
    }
}
