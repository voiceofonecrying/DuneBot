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
        void testSardaukarNegatedByOccupation() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            emperor.placeForces(carthag, 0, 5, true, true, true, game, false, false);
            bg.placeForces(salusaSecundus, 1, 0, true, true, true, game, false, false);
            assertTrue(emperor.isSecundusOccupied());
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            assertTrue(battle.isSardaukarNegated());
        }

        @Test
        void testNoFieldInBattlePlanFewerForcesInReserves() {
            richese.addTreacheryCard(cheapHero);
            richese.addTreacheryCard(chaumas);
            garaKulon.setRicheseNoField(3);
            Territory richeseHomeworld = game.getTerritory("Richese");
            assertEquals(20, richeseHomeworld.getForceStrength("Richese"));
            richese.placeForceFromReserves(game, garaKulon, 5, false);
            assertEquals(15, richeseHomeworld.getForceStrength("Richese"));
            richeseHomeworld.removeForces(game, "Richese", 13);
            assertEquals(2, richeseHomeworld.getForceStrength("Richese"));
            garaKulon.addForces("BG", 1);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
            richese.addSpice(3, "Test");
            assertEquals(8, richese.getSpice());
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
        void testRicheseNoFieldNotYetRevealed() throws InvalidGameStateException {
            garaKulon.setRicheseNoField(5);
            garaKulon.addForces("Fremen", 3);
            game.startBattlePhase();
            battles = game.getBattles();
            Battle battle = new Battle(game, List.of(garaKulon), List.of(fremen, richese));
            assertThrows(InvalidGameStateException.class, () -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 3, false, 0, null, null));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, richese, ladyHelena, null, false, 2, false, 0, null, null));
        }

        @Test
        void testBattleResolved() {
            carthag.addForces("Emperor", 5);
            carthag.addForces("Emperor*", 2);
            Battle battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
            assertFalse(battle.isResolved(game));
            carthag.removeForces(game, "Harkonnen", 10);
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
            garaKulon.removeForces(game, "Harkonnen", 10);
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
    @DisplayName("#ecazChoosingCombatant")
    class EcazChoosingCombatant {
        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            game.createAlliance(ecaz, emperor);
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 6);
            garaKulon.addForces("Emperor*", 1);
            garaKulon.addForces("Ecaz", 7);
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
            game.removeAlliance(ecaz);
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            assertFalse(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazMustChooseBattleFactionTrue() {
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            assertTrue(battle.hasEcazAndAlly());
        }

        @Test
        void testEcazCombatantChoices() {
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            battle.presentEcazAllyChoice(game);
            assertEquals("Who will provide leader and " + Emojis.TREACHERY + " cards in your alliance's battle? ec", ecazChat.getMessages().getFirst());
            assertEquals(2, ecazChat.getChoices().getFirst().size());
            assertEquals("You", ecazChat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("Your ally", ecazChat.getChoices().getFirst().getLast().getLabel());
            assertTrue(harkonnenChat.getChoices().isEmpty());
            assertEquals(Emojis.ECAZ + " must choose who will fight for their alliance.", turnSummary.getMessages().getLast());
        }

        @Test
        void testEcazLowThresholdOpponentGetsCombatantChoices() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.getTerritory("Ecaz").removeForces(game, "Ecaz", 14);
            assertFalse(ecaz.isHighThreshold());
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            battle.presentEcazAllyChoice(game);
            assertTrue(ecazChat.getChoices().isEmpty());
            assertEquals(Emojis.ECAZ + " is at Low Threshold.\nWho will provide leader and " + Emojis.TREACHERY + " cards against you? ha", harkonnenChat.getMessages().getFirst());
            assertEquals(2, harkonnenChat.getChoices().getFirst().size());
            assertEquals("Ecaz", harkonnenChat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("Their ally", harkonnenChat.getChoices().getFirst().getLast().getLabel());
            assertEquals(Emojis.HARKONNEN + " must choose who will fight for the " + Emojis.ECAZ + " alliance.", turnSummary.getMessages().getLast());
        }

        @Test
        void testEcazAllyFighting() {
            Battle battle = new Battle(game, List.of(garaKulon), List.of(ecaz, harkonnen, emperor));
            battle.setEcazCombatant(game, emperor.getName());
            assertEquals(emperor, battle.getDefender(game));
            assertDoesNotThrow(() -> battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 5, null, null));
        }

        @Test
        void testEcazFighting() {
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 5, null, null));
        }

        @Test
        void testEcazAllyChangeForceLosses() throws InvalidGameStateException {
            Battle battle = new Battle(game, List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
            battle.setEcazCombatant(game, "Ecaz");
            assertEquals(ecaz, battle.getDefender(game));
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 4, null, null));
            BattlePlan bp = battle.getDefenderBattlePlan();
            assertEquals("This will leave 3 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", bp.getForcesRemainingString());
            assertEquals(3, bp.getRegularDialed());
            assertEquals(1, bp.getSpecialDialed());
            battle.updateTroopsDialed(game, "Ecaz", 5, 0);
            assertEquals(5, bp.getRegularDialed());
            assertEquals(0, bp.getSpecialDialed());
            assertEquals("This will leave 1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " 3 " + Emojis.ECAZ_TROOP + " in Gara Kulon if you win.", bp.getForcesRemainingString());
        }
    }

    @Nested
    @DisplayName("#negateeSpecialForces")
    class NegateSpecialForces {
        @Nested
        @DisplayName("#ecazNotInGame")
        class EcazNotInGame {
            @BeforeEach
            void setUp() {
                game.addFaction(fremen);
                game.addFaction(emperor);
                game.addFaction(ix);
                game.addFaction(harkonnen);
                battles = game.startBattlePhase();
            }

            @Test
            void testNegateFedaykin() throws InvalidGameStateException {
                carthag.addForces("Fremen*", 1);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertFalse(currentBattle.isFedaykinNegated());
                currentBattle.negateSpecialForces(game, fremen);
                assertTrue(currentBattle.isFedaykinNegated());
            }

            @Test
            void testNegateSardaukar() throws InvalidGameStateException {
                carthag.addForces("Emperor*", 1);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertFalse(currentBattle.isSardaukarNegated());
                currentBattle.negateSpecialForces(game, emperor);
                assertTrue(currentBattle.isSardaukarNegated());
            }

            @Test
            void testNegateCyborgs() throws InvalidGameStateException {
                carthag.addForces("Ix*", 1);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertFalse(currentBattle.isCyborgsNegated());
                currentBattle.negateSpecialForces(game, ix);
                assertTrue(currentBattle.isCyborgsNegated());
            }
        }

        @Nested
        @DisplayName("#emperorNexusCunning")
        class EmperorNexusCunning {
            Battle battle;

            @BeforeEach
            void setUp() {
                game.addFaction(emperor);
                game.addFaction(harkonnen);
                emperor.setNexusCard(new NexusCard("Emperor"));
                emperor.placeForceFromReserves(game, carthag, 2, false);
                battle = new Battle(game, List.of(carthag), List.of(emperor, harkonnen));
                assertFalse(battle.isSardaukarNegated());
            }

            @Test
            void testEmperorNexusCunningPlayed() {
                battle.emperorNexusCunning(game, true);
                assertNull(emperor.getNexusCard());
                assertEquals("You played the " + Emojis.EMPEROR + " Nexus Card. Up to 5 " + Emojis.EMPEROR_TROOP + " will count as " + Emojis.EMPEROR_SARDAUKAR, emperorChat.getMessages().getLast());
                assertEquals(Emojis.EMPEROR + " may count up to 5 " + Emojis.EMPEROR_TROOP + " as " + Emojis.EMPEROR_SARDAUKAR + " in this battle.", turnSummary.getMessages().getLast());
            }

            @Test
            void testEmperorNexusCunningNotPlayed() {
                turnSummary.clear();
                battle.emperorNexusCunning(game, false);
                assertFalse(battle.isSardaukarNegated());
                assertEquals("Emperor", emperor.getNexusCard().name());
                assertEquals("You will not play the " + Emojis.EMPEROR + " Nexus Card.", emperorChat.getMessages().getLast());
                assertTrue(turnSummary.getMessages().isEmpty());
            }
        }

        @Nested
        @DisplayName("#ecazInGame")
        class EcazInGame {
            @BeforeEach
            void setUp() {
                game.addFaction(atreides);
                game.addFaction(ecaz);
                game.addFaction(fremen);
                game.addFaction(emperor);
                game.addFaction(ix);
                game.addFaction(harkonnen);
                battles = game.startBattlePhase();
            }

            @Test
            void testNegateDefenderStarsWithAggressorAndEcazallied() throws InvalidGameStateException {
                game.createAlliance(ecaz, ix);
                sietchTabr.addForces("Emperor*", 1);
                sietchTabr.addForces("Ix*", 1);
                sietchTabr.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getAggressor(game));
                assertFalse(currentBattle.isSardaukarNegated());
                currentBattle.negateSpecialForces(game, emperor);
                assertTrue(currentBattle.isSardaukarNegated());
                assertFalse(currentBattle.isCyborgsNegated());
            }

            @Test
            void testNegateFedaykinAlliedWithEcazAggressor() throws InvalidGameStateException {
                game.createAlliance(ecaz, fremen);
                carthag.addForces("Fremen*", 1);
                carthag.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getAggressor(game));
                assertFalse(currentBattle.isFedaykinNegated());
                currentBattle.negateSpecialForces(game, fremen);
                assertTrue(currentBattle.isFedaykinNegated());
            }

            @Test
            void testNegateSardaukarAlliedWithEcazAggressor() throws InvalidGameStateException {
                game.createAlliance(ecaz, emperor);
                carthag.addForces("Emperor*", 1);
                carthag.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getAggressor(game));
                assertFalse(currentBattle.isSardaukarNegated());
                currentBattle.negateSpecialForces(game, emperor);
                assertTrue(currentBattle.isSardaukarNegated());
            }

            @Test
            void testNegateCyborgsAlliedWithEcazAggressor() throws InvalidGameStateException {
                game.createAlliance(ecaz, ix);
                carthag.addForces("Ix*", 1);
                carthag.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getAggressor(game));
                assertFalse(currentBattle.isCyborgsNegated());
                currentBattle.negateSpecialForces(game, ix);
                assertTrue(currentBattle.isCyborgsNegated());
            }

            @Test
            void testNegateFedaykinAlliedWithEcazDefender() throws InvalidGameStateException {
                game.createAlliance(ecaz, fremen);
                arrakeen.addForces("Fremen*", 1);
                arrakeen.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getDefender(game));
                assertFalse(currentBattle.isFedaykinNegated());
                currentBattle.negateSpecialForces(game, fremen);
                assertTrue(currentBattle.isFedaykinNegated());
            }

            @Test
            void testNegateSardaukarAlliedWithEcazDefender() throws InvalidGameStateException {
                game.createAlliance(ecaz, emperor);
                arrakeen.addForces("Emperor*", 1);
                arrakeen.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getDefender(game));
                assertFalse(currentBattle.isSardaukarNegated());
                currentBattle.negateSpecialForces(game, emperor);
                assertTrue(currentBattle.isSardaukarNegated());
            }

            @Test
            void testNegateCyborgsAlliedWithEcazDefender() throws InvalidGameStateException {
                game.createAlliance(ecaz, ix);
                arrakeen.addForces("Ix*", 1);
                arrakeen.addForces("Ecaz", 2);
                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                Battle currentBattle = battles.getCurrentBattle();
                assertEquals(ecaz, currentBattle.getDefender(game));
                assertFalse(currentBattle.isCyborgsNegated());
                currentBattle.negateSpecialForces(game, ix);
                assertTrue(currentBattle.isCyborgsNegated());
            }
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
    @DisplayName("#karamaFremenMustPay")
    class KaramaFremenMustPay {
        Battle battle;
        Battle battleWithoutFremen;

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(atreides);
            fremen.placeForceFromReserves(game, carthag, 2, false);
            fremen.placeForceFromReserves(game, carthag, 2, true);
            battle = new Battle(game, List.of(carthag), List.of(fremen, harkonnen));
            harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
            battleWithoutFremen = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
        }

        @Test
        void testBeforeBattlePlanSubmitted() throws InvalidGameStateException {
            battle.karamaFremenMustPay(game);
            assertEquals("Your free dial advantage has been negated by Karama. fr", fremenChat.getMessages().getLast());
        }

        @Test
        void testAfterBattlePlanSubmitted() throws InvalidGameStateException {
            battle.setBattlePlan(game, fremen, chani, null, false, 0, false, 0, null, null);
            assertNotNull(battle.getAggressorBattlePlan());
            battle.karamaFremenMustPay(game);
            assertEquals("Your free dial advantage has been negated by Karama.\nYou must submit a new battle plan. fr", fremenChat.getMessages().getLast());
            assertNull(battle.getAggressorBattlePlan());
        }

        @Test
        void testFremenNotInTheBattle() {
        }
    }

    @Nested
    @DisplayName("#lasgunShieldOnHomeworld")
    class LasgunShieldOnHomeworld {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(atreides);
            game.addFaction(fremen);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            harkonnen.addTreacheryCard(lasgun);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(cheapHero);
        }

        @Nested
        @DisplayName("#atreidesOnCaladan")
        class AtreidesOnCaladan {
            Territory caladan;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                caladan = game.getTerritory(atreides.getHomeworld());
                caladan.addForces("Harkonnen", 1);
                battle = new Battle(game, List.of(caladan), List.of(atreides, harkonnen));
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, lasgun, shield);
            }

            @Test
            void testAtreidesLosesOnly2ToLasgunShieldOnCaladan() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(8, caladan.getForceStrength("Atreides"));
            }

            @Test
            void testAtreidesLosesOnly2MoreToLasgunShieldOnCaladanDialedAway1() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(7, caladan.getForceStrength("Atreides"));
            }

            @Test
            void testAtreidesLosesOnly2MoreToLasgunShieldOnCaladanDialedAway4() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(4, caladan.getForceStrength("Atreides"));
            }

            @Test
            void testAtreidesDialsAllBut1WithLasgunShieldOnCaladan() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, true, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, caladan.getForceStrength("Atreides"));
            }
        }

        @Nested
        @DisplayName("#fremenOnSouthernHemisphere")
        class FremenOnSouthernHemisphere {
            Territory southernHemisphere;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                southernHemisphere = game.getTerritory(fremen.getHomeworld());
                southernHemisphere.addForces("Harkonnen", 1);
                battle = new Battle(game, List.of(southernHemisphere), List.of(fremen, harkonnen));
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, lasgun, shield);
            }

            @Test
            void testFremenLosesOnly2ToLasgunShieldOnSouthernHemisphere() throws InvalidGameStateException {
                battle.setBattlePlan(game, fremen, chani, null, false, 0, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(15, southernHemisphere.getForceStrength("Fremen"));
                assertEquals(3, southernHemisphere.getForceStrength("Fremen*"));
            }

            @Test
            void testFremenLosesOnly2MoreToLasgunShieldOnSouthernHemisphereDialedAway1Fedaykin() throws InvalidGameStateException {
                battle.setBattlePlan(game, fremen, chani, null, false, 2, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(15, southernHemisphere.getForceStrength("Fremen"));
                assertEquals(2, southernHemisphere.getForceStrength("Fremen*"));
            }

            @Test
            void testFremenLosesOnly2MoreToLasgunShieldOnSouthernHemisphereDialedAway2Fedaykin1Force() throws InvalidGameStateException {
                BattlePlan bp = battle.setBattlePlan(game, fremen, chani, null, false, 5, false, 0, null, null);
                assertEquals(3, bp.getRegularDialed());
                assertEquals(1, bp.getSpecialDialed());
                battle.printBattleResolution(game, false, true);
                assertEquals(12, southernHemisphere.getForceStrength("Fremen"));
                assertEquals(2, southernHemisphere.getForceStrength("Fremen*"));
            }

            @Test
            void testFremenDialsAwayAllBut1WithLasgunShieldOnSouthernHemisphere() throws InvalidGameStateException {
                battle.setBattlePlan(game, fremen, chani, null, false, 22, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, southernHemisphere.getForceStrength("Fremen"));
                assertEquals(0, southernHemisphere.getForceStrength("Fremen*"));
            }
        }

        @Nested
        @DisplayName("#emperorOnSalusaSecundus")
        class EmperorOnSalusaSecundus {
            Territory salusaSecundus;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                salusaSecundus = game.getTerritory(emperor.getSecondHomeworld());
                salusaSecundus.addForces("Harkonnen", 1);
                salusaSecundus.addForces("Emperor", 3);
                battle = new Battle(game, List.of(salusaSecundus), List.of(emperor, harkonnen));
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, lasgun, shield);
            }

            @Test
            void testEmperorLosesOnly3ToLasgunShieldOnSalusaSecundus() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, bashar, null, false, 0, false, 0, null, null);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, salusaSecundus.getForceStrength("Emperor"));
                assertEquals(5, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testEmperorLosesOnly3MoreToLasgunShieldOnSalusaSecundusDialedAway1Sardaukar() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, bashar, null, false, 1, false, 0, null, null);
                battle.updateTroopsDialed(game, "Emperor", 0, 1);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, salusaSecundus.getForceStrength("Emperor"));
                assertEquals(4, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testEmperorLosesOnly2MoreToLasgunShieldOnSalusaSecundusDialedAway1Sardaukar3Force() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, bashar, null, false, 2, true, 0, null, null);
                battle.updateTroopsDialed(game, "Emperor", 3, 1);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, salusaSecundus.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testEmperorDialsAwayAllBut1WithLasgunShieldOnSalusaSecundus() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, bashar, null, false, 5, true, 0, null, null);
                battle.updateTroopsDialed(game, "Emperor", 3, 4);
                battle.printBattleResolution(game, false, true);
                assertEquals(0, salusaSecundus.getForceStrength("Emperor"));
                assertEquals(0, salusaSecundus.getForceStrength("Emperor*"));
            }
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
            ecaz.placeAmbassador(cielagoNorth_eastSector.getTerritoryName(), "Fremen", 1);
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
                assertTrue(modInfo.getMessages().getFirst().contains("Lasgun-Shield carnage:\n"));
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
                assertFalse(modInfo.getMessages().getFirst().contains("Lasgun-Shield"));
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            modInfo.clear();
            battle.willCallTraitor(game, bg, false, 0, "Arrakeen");
            assertEquals(Emojis.BG + " declines calling Traitor in Arrakeen.", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            void testHarkonnenAsAllyNotAskedWithBattlePlanSubmission() {
                assertTrue(harkonnenChat.getMessages().isEmpty());
                assertFalse(battle.getAggressorBattlePlan().isHarkCanCallTraitor());
            }

            @Test
            void testHarkonnenAsAllyNotAskedAfterFactionSaysTheyMightCallTraitor() throws InvalidGameStateException {
                battle.mightCallTraitor(game, bg, 0, "Arrakeen");
                // Harkonnen should not be asked if main faction chose to wait
//                assertTrue(harkonnenChat.getMessages().isEmpty());
                assertEquals("Will you call Traitor for your ally if " + Emojis.ATREIDES + " plays Duncan Idaho or Gurney Halleck? ha", harkonnenChat.getMessages().getLast());
                assertTrue(battle.getAggressorBattlePlan().isHarkCanCallTraitor());
                assertEquals(Emojis.HARKONNEN + " can call Traitor for ally " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllyAskedAfterResolutionPublishedFactionMightCallTraitor() throws InvalidGameStateException {
                battle.mightCallTraitor(game, bg, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
                assertEquals("Will you call Traitor for your ally against Duncan Idaho in Arrakeen? ha", harkonnenChat.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllyAskedAfterFactionSaysTheyWillCallTraitor() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                assertEquals("Will you call Traitor for your ally if " + Emojis.ATREIDES + " plays Duncan Idaho or Gurney Halleck? ha", harkonnenChat.getMessages().getLast());
                assertTrue(battle.getAggressorBattlePlan().isHarkCanCallTraitor());
                assertEquals(Emojis.HARKONNEN + " can call Traitor for ally " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllySaysNoBeforeResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.willCallTraitor(game, harkonnen, false, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " declines calling Traitor for " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllySaysYesBeforeResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.willCallTraitor(game, harkonnen, true, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " will call Traitor for " + Emojis.BG + " in Arrakeen if possible.", modInfo.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllySaysNoAfterResolutionPublished() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
//                throwTestTopicMessages(harkonnenChat);
                assertEquals("Will you call Traitor for your ally against Duncan Idaho in Arrakeen? ha", harkonnenChat.getMessages().getLast());
                modInfo.clear();
                battle.willCallTraitor(game, harkonnen, false, 0, "Arrakeen");
                assertEquals(Emojis.HARKONNEN + " declines calling Traitor for " + Emojis.BG + " in Arrakeen.", modInfo.getMessages().getFirst());
            }

            @Test
            void testHarkonnenAsAllySaysYesAfterResolutionPublishedCorrectLeader() throws InvalidGameStateException {
                battle.willCallTraitor(game, bg, true, 0, "Arrakeen");
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.printBattleResolution(game, true);
//                throwTestTopicMessages(harkonnenChat);
                assertEquals("Will you call Traitor for your ally against Duncan Idaho in Arrakeen? ha", harkonnenChat.getMessages().getLast());
                battle.willCallTraitor(game, harkonnen, true, 0, "Arrakeen");
                assertTrue(turnSummary.getMessages().getLast().contains(Emojis.HARKONNEN + " calls Traitor against Duncan Idaho!"));
                assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for your ally!", harkonnenChat.getMessages().getLast());
                assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for " + Emojis.HARKONNEN + " and you!", bgChat.getMessages().getLast());
            }

            @Test
            void testHarkonnenAsAllySaysYesAfterResolutionPublishedWrongLeader() throws InvalidGameStateException {
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

        @Nested
        @DisplayName("#aggressorCallsTraitor")
        class AggressorCallsTraitor {
            @Test
            void testDefaultFalse() throws InvalidGameStateException {
                bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                assertFalse(battle.aggressorCallsTraitor(game));
            }

            @Test
            void testAggressorDoesNotHaveCorrectTraitor() throws InvalidGameStateException {
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                assertFalse(battle.aggressorCallsTraitor(game));
            }

            @Test
            void testAggressorCallsTraitor() throws InvalidGameStateException {
                bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                assertTrue(battle.aggressorCallsTraitor(game));
            }

            @Test
            void testHarkonnenAsAllyCallsTraitor() throws InvalidGameStateException {
                game.createAlliance(bg, harkonnen);
                harkonnen.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getAggressorBattlePlan().setHarkWillCallTraitor(true);
                assertTrue(battle.aggressorCallsTraitor(game));
            }

            @Test
            void testHarkonnenAsAllyDoesNotHaveCorrectTraitor() throws InvalidGameStateException {
                game.createAlliance(bg, harkonnen);
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getAggressorBattlePlan().setHarkWillCallTraitor(true);
                assertFalse(battle.aggressorCallsTraitor(game));
            }
        }

        @Nested
        @DisplayName("#defenderCallsTraitor")
        class DefenderCallsTraitor {
            @BeforeEach
            void setUp() {
                battle = new Battle(game, List.of(arrakeen), List.of(atreides, bg));
            }

            @Test
            void testDefaultFalse() throws InvalidGameStateException {
                bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                assertFalse(battle.defenderCallsTraitor(game));
            }

            @Test
            void testDefenderDoesNotHaveCorrectTraitor() throws InvalidGameStateException {
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                assertFalse(battle.defenderCallsTraitor(game));
            }

            @Test
            void testDefenderCallsTraitor() throws InvalidGameStateException {
                bg.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                assertTrue(battle.defenderCallsTraitor(game));
            }

            @Test
            void testHarkonnenAsAllyCallsTraitor() throws InvalidGameStateException {
                game.createAlliance(bg, harkonnen);
                harkonnen.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getDefenderBattlePlan().setHarkWillCallTraitor(true);
                assertTrue(battle.defenderCallsTraitor(game));
            }

            @Test
            void testHarkonnenAsAllyDoesNotHaveCorrectTraitor() throws InvalidGameStateException {
                game.createAlliance(bg, harkonnen);
                battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 0, chaumas, null);
                battle.getDefenderBattlePlan().setHarkWillCallTraitor(true);
                assertFalse(battle.defenderCallsTraitor(game));
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
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
            assertEquals("Would you like the bot to resolve the battle? ", modInfo.getMessages().getFirst());
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " calls Traitor against Alia!"));
            assertFalse(turnSummary.getMessages().getFirst().contains("Duncan Idaho to the tanks"));
            assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES_TROOP + " to the tanks"));
        }
    }

    @Nested
    @DisplayName("#checkIfResolvable")
    class CheckIfResolvable {
        @Nested
        @DisplayName("#juiceOfSapho")
        class JuiceOfSapho {
            Battle battle;
            BattlePlan bgPlan;
            TreacheryCard juiceOfSapho;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addFaction(richese);
                game.addFaction(bg);
                richese.addTreacheryCard(cheapHero);
                juiceOfSapho = richese.getTreacheryCardFromCache("Juice of Sapho");
                garaKulon.addForces("Richese", 3);
                garaKulon.addForces("BG", 1);
                battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
                bgPlan = battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                richese.removeTreacheryCardFromCache(juiceOfSapho);
            }

            @Test
            void testJuiceOfSaphoNotNeeded() throws InvalidGameStateException {
                bg.addTreacheryCard(juiceOfSapho);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, null, null);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }

            @Test
            void testJuiceOfSaphoDecisionRequired() throws InvalidGameStateException {
                richese.addTreacheryCard(chaumas);
                bg.addTreacheryCard(juiceOfSapho);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, chaumas, null);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertEquals("The following must be decided before the battle can be resolved:\n  Juice of Sapho", modInfo.getMessages().getLast());
                battle.juiceOfSaphoAdd(game, bg);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }

            @Test
            void neitherFactionHasJuiceOfSapho() throws InvalidGameStateException {
                battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, null, null);
                battle.printBattleResolution(game, false, false);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }
        }

        @Nested
        @DisplayName("#portableSnooper")
        class PortableSnooper {
            Battle battle;
            BattlePlan bgPlan;
            TreacheryCard portableSnooper;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addFaction(richese);
                game.addFaction(bg);
                richese.addTreacheryCard(cheapHero);
                richese.addTreacheryCard(chaumas);
                portableSnooper = richese.getTreacheryCardFromCache("Portable Snooper");
                garaKulon.addForces("Richese", 3);
                garaKulon.addForces("BG", 1);
                battle = new Battle(game, List.of(garaKulon), List.of(richese, bg));
                bgPlan = battle.setBattlePlan(game, bg, alia, null, false, 0, false, 0, null, null);
                richese.removeTreacheryCardFromCache(portableSnooper);
            }

            @Test
            void testPortableSnooperNotNeeded() throws InvalidGameStateException {
                bg.addTreacheryCard(portableSnooper);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, null, null);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }

            @Test
            void testPortableSnooperCanSaveLeader() throws InvalidGameStateException {
                bg.addTreacheryCard(portableSnooper);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, chaumas, null);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertEquals("The following must be decided before the battle can be resolved:\n  Portable Snooper", modInfo.getMessages().getLast());
                battle.portableSnooperAdd(game, bg);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }

            @Test
            void neitherFactionHasPortableSnooper() throws InvalidGameStateException {
                battle.setBattlePlan(game, richese, null, cheapHero, false, 2, false, 2, chaumas, null);
                battle.printBattleResolution(game, false, false);
                battle.printBattleResolution(game, true, false);
                battle.checkIfResolvable(game);
                assertTrue(modInfo.getMessages().getLast().contains("Would you like the bot to resolve the battle?"));
            }
        }

        @Nested
        @DisplayName("#getHarkonnenNexusBetrayalFaction")
        class GetHarkonnenNexusBetrayalFaction {
            @BeforeEach
            void setUp() {
                game.addFaction(harkonnen);
                game.addFaction(emperor);
                game.addFaction(atreides);
                game.addFaction(fremen);
                game.createAlliance(harkonnen, emperor);
                harkonnen.addTraitorCard(new TraitorCard("Lady Jessica", "Atreides", 5));
                fremen.setNexusCard(new NexusCard("Harkonnen"));
            }

            @Test
            void testNoFactionHoldsHarkonnenNexusCard() throws InvalidGameStateException {
                game.discardNexusCard(fremen);
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                assertNull(battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenNotInTheBattle() throws InvalidGameStateException {
                game.removeAlliance(harkonnen);
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, emperor));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                assertNull(battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenDoesNotCallTraitor() throws InvalidGameStateException {
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                assertNull(battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenAsAllyDoesNotCallTraitor() throws InvalidGameStateException {
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, emperor));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                assertNull(battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenAsAggressorCalledTraitor() throws InvalidGameStateException {
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                assertEquals(fremen, battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenAsAllyAggressorCalledTraitor() throws InvalidGameStateException {
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(emperor, atreides));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getAggressorBattlePlan().setHarkWillCallTraitor(true);
                assertEquals(fremen, battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenAsDefenderCalledTraitor() throws InvalidGameStateException {
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, harkonnen));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                assertEquals(fremen, battle.getHarkonnenNexusBetrayalFaction(game));
            }

            @Test
            void testHarkonnenAsAllyDefenderCallsTraitor() throws InvalidGameStateException {
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, emperor));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getDefenderBattlePlan().setHarkWillCallTraitor(true);
                assertEquals(fremen, battle.getHarkonnenNexusBetrayalFaction(game));
            }
        }

        @Nested
        @DisplayName("#harkonnenNexusBetrayal")
        class HarkonnenNexusBetrayal {
            @BeforeEach
            void setUp() {
                game.addFaction(harkonnen);
                game.addFaction(emperor);
                game.addFaction(atreides);
                game.addFaction(fremen);
                game.createAlliance(harkonnen, emperor);
                harkonnen.addTraitorCard(new TraitorCard("Lady Jessica", "Atreides", 5));
                fremen.setNexusCard(new NexusCard("Harkonnen"));
            }

            @Test
            void testHarkonnenCallsTraitor() throws InvalidGameStateException {
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                modInfo.clear();
                battle.checkIfResolvable(game);
                assertEquals(Emojis.FREMEN + " may play " + Emojis.HARKONNEN + " Nexus Card Betrayal to cancel the Traitor call.", modInfo.getMessages().getFirst());
                assertEquals(3, modInfo.getChoices().getLast().size());
                assertEquals("Resolve with Hark Betrayal", modInfo.getChoices().getLast().getFirst().getLabel());
                turnSummary.clear();
                battle.betrayHarkTraitorAndResolve(game, true, 0, "Arrakeen");
            }

            @Test
            void testHarkonnenHasWrongTraitor() throws InvalidGameStateException {
                harkonnen.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(harkonnen, atreides));
                battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.getAggressorBattlePlan().setWillCallTraitor(true);
                modInfo.clear();
                battle.checkIfResolvable(game);
                assertEquals(2, modInfo.getChoices().getLast().size());
                assertEquals("Yes", modInfo.getChoices().getLast().getFirst().getLabel());
            }

            @Test
            void testHarkonnenCallsTraitorForAlly() throws InvalidGameStateException {
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, emperor));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                battle.getDefenderBattlePlan().setHarkWillCallTraitor(true);
                assertFalse(battle.isAggressorWin(game));
                modInfo.clear();
                battle.checkIfResolvable(game);
                assertEquals(Emojis.FREMEN + " may play " + Emojis.HARKONNEN + " Nexus Card Betrayal to cancel the Traitor call.", modInfo.getMessages().getFirst());
                assertEquals(3, modInfo.getChoices().getLast().size());
                assertEquals("Resolve with Hark Betrayal", modInfo.getChoices().getLast().getFirst().getLabel());
            }

            @Test
            void testHarkonnenHasWrongTraitorForAlly() throws InvalidGameStateException {
                emperor.placeForceFromReserves(game, arrakeen, 1, false);
                Battle battle = new Battle(game, List.of(arrakeen), List.of(atreides, emperor));
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.getDefenderBattlePlan().setWillCallTraitor(true);
                battle.getDefenderBattlePlan().setHarkWillCallTraitor(true);
                modInfo.clear();
                battle.checkIfResolvable(game);
                assertEquals(2, modInfo.getChoices().getLast().size());
                assertEquals("Yes", modInfo.getChoices().getLast().getFirst().getLabel());
            }
        }
    }

    @Nested
    @DisplayName("#resolveBattleHarkonnenLeaderCapture")
    class ResolveBattleHarkonnenLeaderCapture {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(harkonnen);
            game.addFaction(atreides);
            carthag.addForces("Atreides", 5);
            battle = new Battle(game, List.of(carthag), List.of(harkonnen, atreides));
        }

        @Nested
        @DisplayName("#resolutionHarkonnenCanCapture")
        class ResolutionHarkonnenCanCapture {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
                harkonnenChat.clear();
            }

            @Test
            void testReviewDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " captures a " + Emojis.ATREIDES + " leader"));
                assertTrue(harkonnenChat.getMessages().isEmpty());
            }

            @Test
            void testPublishDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " captures a " + Emojis.ATREIDES + " leader"));
                assertTrue(harkonnenChat.getMessages().isEmpty());
            }

            @Test
            void testResolveAsksHarkonnenAndBlocksAdvance() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(harkonnenChat.getMessages().getFirst().contains("Will you keep or kill"));
                assertTrue(harkonnenChat.getMessages().getFirst().contains("? ha"));
                assertEquals(3, harkonnenChat.getChoices().getFirst().size());
                assertTrue(battle.isHarkonnenCaptureMustBeResolved(game));
                game.killLeader(atreides, battle.getHarkonnenCapturedLeader());
                assertFalse(battle.isHarkonnenCaptureMustBeResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSurvivingCapturedLeader")
        class ResolutionWithSurvivingCapturedLeader {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.removeLeader(ladyJessica);
                harkonnen.addLeader(ladyJessica);
                battle.setBattlePlan(game, harkonnen, ladyJessica, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
                harkonnenChat.clear();
            }

            @Test
            void testReviewDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(harkonnen.getLeaders().contains(ladyJessica));
                assertFalse(atreides.getLeaders().contains(ladyJessica));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " returns Lady Jessica to " + Emojis.ATREIDES));
                assertTrue(atreidesLedger.getMessages().isEmpty());
                assertTrue(harkonnenLedger.getMessages().isEmpty());
            }

            @Test
            void testPublishDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(harkonnen.getLeaders().contains(ladyJessica));
                assertFalse(atreides.getLeaders().contains(ladyJessica));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " returns Lady Jessica to " + Emojis.ATREIDES));
                assertTrue(atreidesLedger.getMessages().isEmpty());
                assertTrue(harkonnenLedger.getMessages().isEmpty());
            }

            @Test
            void testResolveAsksHarkonnenAndBlocksAdvance() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(harkonnen.getLeaders().contains(ladyJessica));
                assertTrue(atreides.getLeaders().contains(ladyJessica));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.HARKONNEN + " has returned Lady Jessica to " + Emojis.ATREIDES)));
                assertTrue(atreidesLedger.getMessages().stream().anyMatch(m -> m.equals("Lady Jessica has returned to you.")));
                assertTrue(harkonnenLedger.getMessages().stream().anyMatch(m -> m.equals("Lady Jessica has returned to " + Emojis.ATREIDES)));
            }
        }

        @Nested
        @DisplayName("#resolutionWithKilledCapturedLeader")
        class ResolutionWithKilledCapturedLeader {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.removeLeader(ladyJessica);
                harkonnen.addLeader(ladyJessica);
                atreides.addTreacheryCard(chaumas);
                battle.setBattlePlan(game, harkonnen, ladyJessica, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, chaumas, null);
                turnSummary.clear();
                modInfo.clear();
                harkonnenChat.clear();
            }

            @Test
            void testReviewDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(harkonnen.getLeaders().contains(ladyJessica));
                assertFalse(atreides.getLeaders().contains(ladyJessica));
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " returns Lady Jessica to " + Emojis.ATREIDES));
            }

            @Test
            void testPublishDoesNotAskHarkonnen() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(harkonnen.getLeaders().contains(ladyJessica));
                assertFalse(atreides.getLeaders().contains(ladyJessica));
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " returns Lady Jessica to " + Emojis.ATREIDES));
            }

            @Test
            void testResolveAsksHarkonnenAndBlocksAdvance() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(harkonnen.getLeaders().contains(ladyJessica));
                assertFalse(atreides.getLeaders().contains(ladyJessica));
                assertTrue(game.getLeaderTanks().contains(ladyJessica));
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.HARKONNEN + " has returned Lady Jessica to " + Emojis.ATREIDES)));
            }
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
        @DisplayName("#weaponDiscard")
        class WeaponDiscard {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                richese.addTreacheryCard(chaumas);
                atreides.addTreacheryCard(snooper);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, chaumas, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, snooper);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotDiscardWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(richese.getTreacheryHand().contains(chaumas));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " discards Chaumas"));
            }

            @Test
            void testPublishDoesNotDiscardWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(richese.getTreacheryHand().contains(chaumas));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " discards Chaumas"));
            }

            @Test
            void testResolveDiscardsWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(richese.getTreacheryHand().contains(chaumas));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.RICHESE + " discards Chaumas.")));
            }

            @Test
            void testLoserCallsTraitorDoesNotDiscard() throws InvalidGameStateException {
                turnSummary.clear();
                richese.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.getAggressorBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, richese, true, 0, "Gara Kulon");
                battle.printBattleResolution(game, false, true);
                assertTrue(richese.getTreacheryHand().contains(chaumas));
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.RICHESE + " discards Chaumas.")));
            }

            @Test
            void testBothCallTraitorWeaponIsDiscarded() throws InvalidGameStateException {
                richese.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.getAggressorBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, richese, true, 0, "Gara Kulon");
                atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
                battle.getDefenderBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, atreides, true, 0, "Gara Kulon");
                battle.printBattleResolution(game, false, true);
                assertFalse(richese.getTreacheryHand().contains(chaumas));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.RICHESE + " discards Chaumas.")));
            }
        }

        @Nested
        @DisplayName("#worthlessWeaponDiscard")
        class WorthlessWeaponDiscard {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                richese.addTreacheryCard(chaumas);
                atreides.addTreacheryCard(snooper);
                atreides.addTreacheryCard(baliset);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, chaumas, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, baliset, snooper);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotDiscardWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(atreides.getTreacheryHand().contains(baliset));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " discards Baliset"));
            }

            @Test
            void testPublishDoesNotDiscardWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(atreides.getTreacheryHand().contains(baliset));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " discards Baliset"));
            }

            @Test
            void testResolveDiscardsWeapon() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(atreides.getTreacheryHand().contains(baliset));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " discards Baliset.")));
            }

            @Test
            void testCallsTraitorDoesNotDiscard() throws InvalidGameStateException {
                turnSummary.clear();
                atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
                battle.getDefenderBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, atreides, true, 0, "Gara Kulon");
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getLast().contains(Emojis.ATREIDES + " may discard Baliset"));
                battle.printBattleResolution(game, false, true);
                assertTrue(atreides.getTreacheryHand().contains(baliset));
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " discards Baliset.")));
            }

            @Test
            void testBothCallTraitorWeaponIsDiscarded() throws InvalidGameStateException {
                richese.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.getAggressorBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, richese, true, 0, "Gara Kulon");
                atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
                battle.getDefenderBattlePlan().setCanCallTraitor(true);
                battle.willCallTraitor(game, atreides, true, 0, "Gara Kulon");
                battle.printBattleResolution(game, false, true);
                assertFalse(atreides.getTreacheryHand().contains(baliset));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " discards Baliset.")));
            }
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
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " loses 3 " + Emojis.RICHESE_TROOP + " to the tanks"));
            }

            @Test
            void testPublishDoesNotRevealTheNoField() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " reveals"));
                assertEquals(3, garaKulon.getRicheseNoField());
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " loses 3 " + Emojis.RICHESE_TROOP + " to the tanks"));
            }

            @Test
            void testResolveRevealsTheNoField() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(garaKulon.hasRicheseNoField());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("The 3 " + Emojis.NO_FIELD + " in Gara Kulon reveals 3 " + Emojis.RICHESE_TROOP)));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.RICHESE_TROOP + " in Gara Kulon were sent to the tanks.")));
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

        @Nested
        @DisplayName("#resolutionWithCombatSpice")
        class ResolutionWithCombatSpice {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                richese.addTreacheryCard(chaumas);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 2, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Nested
            @DisplayName("#factionsOwnSpice")
            class FactionsOwnSpice {
                @Test
                void testReviewDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, false);
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testPublishDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, true, false);
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testResolveRemovesSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, true);
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice.")));
                    assertEquals(8, atreides.getSpice());
                }
            }

            @Nested
            @DisplayName("#factionAndAllySpice")
            class FactionsAndAllySpice {
                @BeforeEach
                void setUp() {
                    game.addFaction(emperor);
                    game.createAlliance(atreides, emperor);
                    emperor.setSpiceForAlly(1);
                    turnSummary.clear();
                }

                @Test
                void testReviewDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, false);
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 1 " + Emojis.SPICE + " combat spice"));
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 1 " + Emojis.SPICE + " ally support"));
                    assertEquals(10, atreides.getSpice());
                    assertEquals(10, emperor.getSpice());
                }

                @Test
                void testPublishDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, true, false);
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 1 " + Emojis.SPICE + " combat spice"));
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 1 " + Emojis.SPICE + " ally support"));
                    assertEquals(10, atreides.getSpice());
                    assertEquals(10, emperor.getSpice());
                }

                @Test
                void testResolveRemovesSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, true);
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " loses 1 " + Emojis.SPICE + " combat spice.")));
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.EMPEROR + " loses 1 " + Emojis.SPICE + " ally support.")));
                    assertEquals(9, atreides.getSpice());
                    assertEquals(9, emperor.getSpice());
                }
            }

            @Nested
            @DisplayName("#factionsOwnSpiceCHOAMInGame")
            class FactionsOwnSpiceCHOAMInGame {
                @BeforeEach
                void setUp() {
                    game.addFaction(choam);
                }

                @Test
                void testReviewDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, false);
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice"));
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.CHOAM + " gains 1 " + Emojis.SPICE + " combat spice"));
                    assertEquals(10, atreides.getSpice());
                    assertEquals(2, choam.getSpice());
                }

                @Test
                void testPublishDoesNotRemoveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, true, false);
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice"));
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.CHOAM + " gains 1 " + Emojis.SPICE + " combat spice"));
                    assertEquals(10, atreides.getSpice());
                    assertEquals(2, choam.getSpice());
                }

                @Test
                void testResolveRemovesSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, true);
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " loses 2 " + Emojis.SPICE + " combat spice.")));
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.CHOAM + " gains 1 " + Emojis.SPICE + " combat spice.")));
                    assertEquals(8, atreides.getSpice());
                    assertEquals(3, choam.getSpice());
                }
            }
        }

        @Nested
        @DisplayName("#resolutionWithRihaniDeciphererInFront")
        class ResolutionWithRihaniDeciphererInFront {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                duncanIdaho.setSkillCard(new LeaderSkillCard("Rihani Decipherer"));
                duncanIdaho.setPulledBehindShield(false);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
                atreidesChat.clear();
            }

            @Test
            void testReviewDoesNotShowTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may peek at 2 random cards in the Traitor Deck with Rihani Decipherer"));
                assertTrue(atreidesChat.getMessages().isEmpty());
            }

            @Test
            void testPublishDoesNotShowTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may peek at 2 random cards in the Traitor Deck with Rihani Decipherer"));
                assertTrue(atreidesChat.getMessages().isEmpty());
            }

            @Test
            void testResolveShowsTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(atreidesChat.getMessages().getFirst().contains(" are in the Traitor deck."));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " has been shown 2 Traitor cards for Rihani Decipherer.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithRihaniDeciphererBehind")
        class ResolutionWithRihaniDeciphererBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.addTraitorCard(game.getTraitorDeck().pop());
                duncanIdaho.setSkillCard(new LeaderSkillCard("Rihani Decipherer"));
                duncanIdaho.setPulledBehindShield(true);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
                atreidesChat.clear();
            }

            @Test
            void testReviewDoesNotDrawTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may draw 2 Traitor Cards and keep one of them with Rihani Decipherer"));
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertFalse(battle.isRihaniDeciphererMustBeResolved(game));
                assertEquals(1, atreides.getTraitorHand().size());
            }

            @Test
            void testPublishDoesNotDrawTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may draw 2 Traitor Cards and keep one of them with Rihani Decipherer"));
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertFalse(battle.isRihaniDeciphererMustBeResolved(game));
                assertEquals(1, atreides.getTraitorHand().size());
            }

            @Test
            void testResolveDrawsTraitors() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(3, atreides.getTraitorHand().size());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " has drawn 2 Traitor cards for Rihani Decipherer.")));
                assertTrue(atreidesChat.getMessages().getFirst().contains("You must discard two Traitors."));
                assertTrue(atreidesChat.getMessages().get(1).contains("Reveal and discard an unused traitor:"));
                assertEquals(3, atreidesChat.getChoices().getFirst().size());
                assertTrue(atreidesChat.getMessages().get(2).contains("Discard a traitor just drawn:"));
                assertEquals(3, atreidesChat.getChoices().getFirst().size());
                assertTrue(battle.isRihaniDeciphererMustBeResolved(game));
                atreides.discardTraitor(atreides.getTraitorHand().getFirst().getName(), true);
                atreides.discardTraitor(atreides.getTraitorHand().getFirst().getName(), false);
                assertFalse(battle.isRihaniDeciphererMustBeResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSandmasterBehind")
        class ResolutionWithSandmasterBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                garaKulon.setSpice(1);
                duncanIdaho.setSkillCard(new LeaderSkillCard("Sandmaster"));
                duncanIdaho.setPulledBehindShield(true);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotAddSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains("3 " + Emojis.SPICE + " will be added to Gara Kulon with Sandmaster"));
                assertEquals(1, garaKulon.getSpice());
            }

            @Test
            void testPublishDoesNotAddSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains("3 " + Emojis.SPICE + " will be added to Gara Kulon with Sandmaster"));
                assertEquals(1, garaKulon.getSpice());
            }

            @Test
            void testResolveAddsSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.SPICE + " were added to Gara Kulon with Sandmaster.")));
                assertEquals(4, garaKulon.getSpice());
            }
        }

        @Nested
        @DisplayName("#resolutionWithSmugglerBehind")
        class ResolutionWithSmugglerBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                garaKulon.setSpice(3);
                duncanIdaho.setSkillCard(new LeaderSkillCard("Smuggler"));
                duncanIdaho.setPulledBehindShield(true);
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotTakeSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " will take 2 " + Emojis.SPICE + " from Gara Kulon with Smuggler"));
                assertEquals(10, atreides.getSpice());
                assertEquals(3, garaKulon.getSpice());
            }

            @Test
            void testPublishDoesNotTakeSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " will take 2 " + Emojis.SPICE + " from Gara Kulon with Smuggler"));
                assertEquals(10, atreides.getSpice());
                assertEquals(3, garaKulon.getSpice());
            }

            @Test
            void testResolveTakesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " took 2 " + Emojis.SPICE + " from Gara Kulon with Smuggler.")));
                assertEquals(12, atreides.getSpice());
                assertEquals(1, garaKulon.getSpice());
            }
        }

        @Nested
        @DisplayName("#resolutionWithLoserHoldingOneTechToken")
        class ResolutionWithLoserHoldingOneTechToken {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addGameOption(GameOption.TECH_TOKENS);
                richese.addTechToken("Heighliners");
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotTransferToken() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " loses " + Emojis.HEIGHLINERS + " to " + Emojis.ATREIDES));
                assertTrue(richese.hasTechToken("Heighliners"));
                assertFalse(atreides.hasTechToken("Heighliners"));
            }

            @Test
            void testPublishDoesNotTransferToken() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " loses " + Emojis.HEIGHLINERS + " to " + Emojis.ATREIDES));
                assertTrue(richese.hasTechToken("Heighliners"));
                assertFalse(atreides.hasTechToken("Heighliners"));
            }

            @Test
            void testResolveTransfersToken() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " takes " + Emojis.HEIGHLINERS + " from " + Emojis.RICHESE)));
                assertTrue(atreides.hasTechToken("Heighliners"));
                assertFalse(richese.hasTechToken("Heighliners"));
            }
        }

        @Nested
        @DisplayName("#resolutionWithLoserHoldingTwoTechTokens")
        class ResolutionWithLoserHoldingTwoTechTokens {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addGameOption(GameOption.TECH_TOKENS);
                richese.addTechToken("Heighliners");
                richese.addTechToken("Spice Production");
                battle.setBattlePlan(game, richese, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                turnSummary.clear();
                modInfo.clear();
                atreidesChat.clear();
            }

            @Test
            void testReviewDoesNotTransferTokenOrAskWinner() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " loses " + Emojis.HEIGHLINERS + " or " + Emojis.SPICE_PRODUCTION + " to " + Emojis.ATREIDES));
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertTrue(richese.hasTechToken("Heighliners"));
                assertFalse(atreides.hasTechToken("Heighliners"));
                assertTrue(richese.hasTechToken("Spice Production"));
                assertFalse(atreides.hasTechToken("Spice Production"));
                assertFalse(battle.isTechTokenMustBeResolved(game));
            }

            @Test
            void testPublishDoesNotTransferTokenOrAskWinner() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " loses " + Emojis.HEIGHLINERS + " or " + Emojis.SPICE_PRODUCTION + " to " + Emojis.ATREIDES));
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertTrue(richese.hasTechToken("Heighliners"));
                assertFalse(atreides.hasTechToken("Heighliners"));
                assertTrue(richese.hasTechToken("Spice Production"));
                assertFalse(atreides.hasTechToken("Spice Production"));
                assertFalse(battle.isTechTokenMustBeResolved(game));
            }

            @Test
            void testResolveAsksWinner() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " must choose which Tech Token to take from " + Emojis.RICHESE)));
                assertEquals("Which Tech Token would you like to take? at", atreidesChat.getMessages().getFirst());
                assertEquals(2, atreidesChat.getChoices().getFirst().size());
                assertTrue(richese.hasTechToken("Heighliners"));
                assertFalse(atreides.hasTechToken("Heighliners"));
                assertTrue(richese.hasTechToken("Spice Production"));
                assertFalse(atreides.hasTechToken("Spice Production"));
                assertTrue(battle.isTechTokenMustBeResolved(game));
                game.assignTechToken("Heighliners", atreides);
                assertFalse(battle.isTechTokenMustBeResolved(game));
            }
        }
        @Nested
        @DisplayName("#resolutionWithCombatWater")
        class ResolutionWithCombatWater {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.addTreacheryCard(chaumas);
                battle.setBattlePlan(game, richese, ladyHelena, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, chaumas, null);
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " combat water"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " combat water"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testResolveGivesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " combat water.")));
                assertEquals(14, atreides.getSpice());
            }
        }
    }

    @Nested
    @DisplayName("#resolveBattleMultipleSectorsAndStarredForces")
    class ResolveBattleMultipleSectorsAndStarredForces {
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
            cielagoNorth_eastSector.addForces("Atreides", 5);
            kaitain.removeForces(game, "Emperor", 5);
            cielagoNorth_eastSector.addForces("Emperor", 3);
            cielagoNorth_westSector.addForces("Emperor", 2);
            salusaSecundus.removeForces(game, "Emperor*", 4);
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

        @Nested
        @DisplayName("#resolutionWithDiplomatBehind")
        class ResolutionWithDiplomatBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                burseg.setSkillCard(new LeaderSkillCard("Diplomat"));
                burseg.setPulledBehindShield(true);
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testPublishDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testResolveSetsDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isDiplomatMustBeResolved());
                assertEquals(Emojis.EMPEROR + " retreat with Diplomat must be resolved. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
                assertEquals("You may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat.\nPlease tell the mod where you would like to move them. em", emperorChat.getMessages().getLast());
            }
        }

        @Nested
        @DisplayName("#resolutionLoserForcesToTheTanks")
        class ResolutionLoserForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWinnerForcesToTheTanks")
        class ResolutionWinnerForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWithReinforcements")
        class ResolutionWithReinforcements {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                kaitain.removeForces(game, "Emperor", 8);
                salusaSecundus.addForces("Emperor*", 1);
                emperor.addTreacheryCard(reinforcements);
                emperor.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, emperor, burseg, null, false, 1, true, 0, reinforcements, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillReserves() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " must send 3 forces from reserves to the tanks for Reinforcements"));
                assertEquals(2, kaitain.getForceStrength("Emperor"));
                assertEquals(2, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotKillReserves() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " must send 3 forces from reserves to the tanks for Reinforcements"));
                assertEquals(2, kaitain.getForceStrength("Emperor"));
                assertEquals(2, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveWithdrawsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(0, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " in Kaitain were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_SARDAUKAR + " in Salusa Secundus were sent to the tanks.")));
            }

            @Test
            void testResolveAndCallTraitor() throws InvalidGameStateException {
                battle.getAggressorBattlePlan().setCanCallTraitor(true);
                emperor.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.willCallTraitor(game, emperor, true, 0, "Cielago North");
                battle.printBattleResolution(game, false, true);
                assertEquals(2, kaitain.getForceStrength("Emperor"));
                assertEquals(2, salusaSecundus.getForceStrength("Emperor*"));
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " in Kaitain were sent to the tanks.")));
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_SARDAUKAR + " in Salusa Secundus were sent to the tanks.")));
            }

            @Test
            void testResolveAndBothCallTraitor() throws InvalidGameStateException {
                battle.getAggressorBattlePlan().setCanCallTraitor(true);
                emperor.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 2));
                battle.willCallTraitor(game, emperor, true, 0, "Cielago North");
                battle.getDefenderBattlePlan().setCanCallTraitor(true);
                atreides.addTraitorCard(new TraitorCard("Burseg", "Atreides", 2));
                battle.willCallTraitor(game, atreides, true, 0, "Cielago North");
                battle.printBattleResolution(game, false, true);
                assertEquals(0, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " in Kaitain were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_SARDAUKAR + " in Salusa Secundus were sent to the tanks.")));
            }
        }
    }

    @Nested
    @DisplayName("#resolveEcazAllyBattle")
    class ResolveEcazAllyBattle {
        Battle battle;
        Territory kaitain;
        Territory salusaSecundus;
        Territory ecazHomeworld;

        @BeforeEach
        void setUp() {
            game.addFaction(emperor);
            game.addFaction(atreides);
            game.addFaction(ecaz);
            ecaz.addLeader(dukeVidal);
            game.createAlliance(ecaz, emperor);
            atreides.setForcesLost(7);
            emperor.addTreacheryCard(cheapHero);
            kaitain = game.getTerritory(emperor.getHomeworld());
            salusaSecundus = game.getTerritory(emperor.getSecondHomeworld());
            ecazHomeworld = game.getTerritory(ecaz.getHomeworld());
            cielagoNorth_eastSector.addForces("Atreides", 5);
            ecazHomeworld.removeForces(game, "Ecaz", 5);
            cielagoNorth_eastSector.addForces("Ecaz", 5);
            kaitain.removeForces(game, "Emperor", 5);
            cielagoNorth_eastSector.addForces("Emperor", 3);
            cielagoNorth_westSector.addForces("Emperor", 2);
            salusaSecundus.removeForces(game, "Emperor*", 4);
            cielagoNorth_eastSector.addForces("Emperor*", 2);
            cielagoNorth_westSector.addForces("Emperor*", 2);
            battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(ecaz, atreides, emperor));
        }

        @Nested
        @DisplayName("#resolutionWithHarassAndWithdrawFromLoser")
        class ResolutionWithHarassAndWithdrawFromLoser {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ecaz.addTreacheryCard(harassAndWithdraw);
                ecaz.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, ecaz, null, cheapHero, false, 1, true, 0, harassAndWithdraw, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " returns"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " returns 2 " + Emojis.ECAZ_TROOP + " to reserves with Harass and Withdraw"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " returns"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " returns 2 " + Emojis.ECAZ_TROOP + " to reserves with Harass and Withdraw"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveWithdrawsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(11, ecazHomeworld.getForceStrength("Ecaz"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.ECAZ_TROOP + " returned to reserves with Harass and Withdraw.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }
        }

        @Nested
        @DisplayName("#resolutionWithHarassAndWithdrawFromWinner")
        class ResolutionWithHarassAndWithdrawFromWinner {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ecaz.addLeader(dukeVidal);
                ecaz.addTreacheryCard(harassAndWithdraw);
                battle.setBattlePlan(game, ecaz, dukeVidal, null, false, 1, true, 0, harassAndWithdraw, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " returns"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " returns 2 " + Emojis.ECAZ_TROOP + " to reserves with Harass and Withdraw"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotWithdrawForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " returns"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " returns 2 " + Emojis.ECAZ_TROOP + " to reserves with Harass and Withdraw"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveWithdrawsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(11, ecazHomeworld.getForceStrength("Ecaz"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.ECAZ_TROOP + " returned to reserves with Harass and Withdraw.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSukGraduateInFront")
        class ResolutionWithSukGraduateInFront {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                bindikkNarvi.setSkillCard(new LeaderSkillCard("Suk Graduate"));
                bindikkNarvi.setPulledBehindShield(false);
                battle.setBattlePlan(game, ecaz, dukeVidal, null, false, 1, true, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " returns 1 " + Emojis.ECAZ_TROOP + " to reserves with Suk Graduate"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " returns 1 " + Emojis.ECAZ_TROOP + " to reserves with Suk Graduate"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveReturnsForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(10, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.ECAZ_TROOP + " returned to reserves with Suk Graduate.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithSukGraduateBehind")
        class ResolutionWithSukGraduateBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                dukeVidal.setSkillCard(new LeaderSkillCard("Suk Graduate"));
                dukeVidal.setPulledBehindShield(true);
                battle.setBattlePlan(game, ecaz, dukeVidal, null, false, 5, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " saves 3 " + Emojis.ECAZ_TROOP + " and may leave 1 in the territory with Suk Graduate"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testPublishDoesNotReturnForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " saves 3 " + Emojis.ECAZ_TROOP + " and may leave 1 in the territory with Suk Graduate"));
                assertEquals(9, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            }

            @Test
            void testResolveReturnsForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(11, ecazHomeworld.getForceStrength("Ecaz"));
                assertEquals(10, kaitain.getForceStrength("Emperor"));
                assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ECAZ + " leaves 1 " + Emojis.ECAZ_TROOP + " in Cielago North, may return it to reserves.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.ECAZ_TROOP + " returned to reserves with Suk Graduate.")));
            }
        }

        @Nested
        @DisplayName("#resolutionWithDiplomatBehind")
        class ResolutionWithDiplomatBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                bindikkNarvi.setSkillCard(new LeaderSkillCard("Diplomat"));
                bindikkNarvi.setPulledBehindShield(true);
                battle.setBattlePlan(game, ecaz, bindikkNarvi, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " may retreat 2 " + Emojis.ECAZ_TROOP + " to an empty adjacent non-stronghold with Diplomat"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testPublishDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " may retreat 2 " + Emojis.ECAZ_TROOP + " to an empty adjacent non-stronghold with Diplomat"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testResolveSetsDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isDiplomatMustBeResolved());
                assertEquals(Emojis.ECAZ + " retreat with Diplomat must be resolved. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
                assertEquals("You may retreat 2 " + Emojis.ECAZ_TROOP + " to an empty adjacent non-stronghold with Diplomat.\nPlease tell the mod where you would like to move them. ec", ecazChat.getMessages().getLast());
            }
        }

        @Nested
        @DisplayName("#resolutionLoserForcesToTheTanks")
        class ResolutionLoserForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ecaz.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, ecaz, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 5 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 5 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("5 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWinnerForcesToTheTanks")
        class ResolutionWinnerForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ecaz.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, ecaz, null, cheapHero, false, 5, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }
    }

    @Nested
    @DisplayName("#resolveAllyEcazBattle")
    class ResolveAllyEcazBattle {
        Battle battle;
        Territory kaitain;
        Territory salusaSecundus;
        Territory ecazHomeworld;

        @BeforeEach
        void setUp() {
            game.addFaction(emperor);
            game.addFaction(atreides);
            game.addFaction(ecaz);
            game.createAlliance(ecaz, emperor);
            atreides.setForcesLost(7);
            emperor.addTreacheryCard(cheapHero);
            kaitain = game.getTerritory(emperor.getHomeworld());
            ecazHomeworld = game.getTerritory(ecaz.getHomeworld());
            salusaSecundus = game.getTerritory(emperor.getSecondHomeworld());
            cielagoNorth_eastSector.addForces("Atreides", 5);
            ecazHomeworld.removeForces(game, "Ecaz", 6);
            cielagoNorth_eastSector.addForces("Ecaz", 5);
            kaitain.removeForces(game, "Emperor", 5);
            cielagoNorth_eastSector.addForces("Emperor", 3);
            cielagoNorth_westSector.addForces("Emperor", 2);
            salusaSecundus.removeForces(game, "Emperor*", 4);
            cielagoNorth_eastSector.addForces("Emperor*", 2);
            cielagoNorth_westSector.addForces("Emperor*", 2);
            battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(emperor, atreides, ecaz));
        }

        @Nested
        @DisplayName("#resolutionWithHarassAndWithdrawFromLoser")
        class ResolutionWithHarassAndWithdrawFromLoser {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                emperor.addTreacheryCard(harassAndWithdraw);
                emperor.addTreacheryCard(cheapHero);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 1, true, 0, harassAndWithdraw, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 2, false, 0, null, null);
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

        @Nested
        @DisplayName("#resolutionWithDiplomatBehind")
        class ResolutionWithDiplomatBehind {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                burseg.setSkillCard(new LeaderSkillCard("Diplomat"));
                burseg.setPulledBehindShield(true);
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 2, true, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testPublishDoesNotSetResolveDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat"));
                assertFalse(battle.isDiplomatMustBeResolved());
            }

            @Test
            void testResolveSetsDiplomatFlag() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isDiplomatMustBeResolved());
                assertEquals(Emojis.EMPEROR + " retreat with Diplomat must be resolved. " + game.getModOrRoleMention(), modInfo.getMessages().getFirst());
                assertEquals("You may retreat 3 " + Emojis.EMPEROR_SARDAUKAR + " to an empty adjacent non-stronghold with Diplomat.\nPlease tell the mod where you would like to move them. em", emperorChat.getMessages().getLast());
            }
        }

        @Nested
        @DisplayName("#resolutionLoserForcesToTheTanks")
        class ResolutionLoserForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, true, 2, false, 0, null, null);
                assertFalse(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 5 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 5 " + Emojis.EMPEROR_TROOP + " 4 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 5 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("5 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }

        @Nested
        @DisplayName("#resolutionWinnerForcesToTheTanks")
        class ResolutionWinnerForcesToTheTanks {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, emperor, burseg, null, false, 5, false, 0, null, null);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                turnSummary.clear();
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotKillForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " loses 4 " + Emojis.EMPEROR_TROOP + " 3 " + Emojis.EMPEROR_SARDAUKAR + " to the tanks"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " loses 3 " + Emojis.ECAZ_TROOP + " to the tanks"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveKillsForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("3 " + Emojis.ECAZ_TROOP + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }
    }

    @Nested
    @DisplayName("#resolveIxCyborgsAndSuboids")
    class ResolveIxCyborgsAndSuboids {
        Territory ixHomeworld;
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(ix);
            game.addFaction(bt);
            ixHomeworld = game.getTerritory(ix.getHomeworld());
        }

        @Nested
        @DisplayName("#suboidsInTwoSectorsReplaced")
        class SuboidsInTwoSectorsReplaced {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ixHomeworld.removeForces(game, "Ix", 3);
                ixHomeworld.removeForces(game, "Ix*", 4);
                cielagoNorth_eastSector.addForces("Ix", 2);
                cielagoNorth_eastSector.addForces("Ix*", 2);
                cielagoNorth_eastSector.addForces("BT", 1);
                cielagoNorth_westSector.addForces("Ix", 1);
                cielagoNorth_westSector.addForces("Ix*", 2);
                battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(ix, bt));
                battle.setBattlePlan(game, ix, cammarPilru, null, false, 4, false, 0, null, null);
                battle.setBattlePlan(game, bt, zoal, null, false, 0, false, 0, null, null);
                modInfo.clear();
            }

            @Test
            void testReviewAnnouncesReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.IX + " loses 4 " + Emojis.IX_CYBORG + " to the tanks\n"
                        + Emojis.IX + " may send 3 " + Emojis.IX_SUBOID + " to the tanks instead of 3 " + Emojis.IX_CYBORG));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishAnnouncesReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.IX + " loses 4 " + Emojis.IX_CYBORG + " to the tanks\n"
                        + Emojis.IX + " may send 3 " + Emojis.IX_SUBOID + " to the tanks instead of 3 " + Emojis.IX_CYBORG));
                assertFalse(battle.isResolved(game));
            }

//            @Test
//            void testResolveReplacesSuboids() throws InvalidGameStateException {
//                battle.printBattleResolution(game, false, true);
//                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.IX_SUBOID + " in Cielago North (East Sector) were sent to the tanks.")));
//                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.IX_SUBOID + " in Cielago North (West Sector) were sent to the tanks.")));
//                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.IX_CYBORG + " in Cielago North (West Sector) were sent to the tanks.")));
//                assertTrue(turnSummary.getMessages().stream().noneMatch(m -> m.contains(Emojis.IX_CYBORG + " in Cielago North (East Sector) were sent to the tanks.")));
//                assertTrue(battle.isResolved(game));
//            }
        }

        @Nested
        @DisplayName("#noSuboidsSurvived")
        class NoSuboidsSurvived {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ixHomeworld.removeForces(game, "Ix", 3);
                ixHomeworld.removeForces(game, "Ix*", 4);
                cielagoNorth_eastSector.addForces("Ix", 2);
                cielagoNorth_eastSector.addForces("Ix*", 2);
                cielagoNorth_eastSector.addForces("BT", 1);
                cielagoNorth_westSector.addForces("Ix", 1);
                cielagoNorth_westSector.addForces("Ix*", 2);
                battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(ix, bt));
                battle.setBattlePlan(game, ix, cammarPilru, null, false, 5, true, 0, null, null);
                battle.setBattlePlan(game, bt, zoal, null, false, 0, false, 0, null, null);
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotAnnounceReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.IX + " loses 3 " + Emojis.IX_SUBOID + " 4 " + Emojis.IX_CYBORG + " to the tanks"));
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.IX + " may send"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotAnnounceReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.IX + " loses 3 " + Emojis.IX_SUBOID + " 4 " + Emojis.IX_CYBORG + " to the tanks"));
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.IX + " may send"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveRemovesAllForces() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }

        @Nested
        @DisplayName("#noCyborgsInBattle")
        class NoCyborgsInBattle {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                ix.addTreacheryCard(chaumas);
                ixHomeworld.removeForces(game, "Ix", 3);
                cielagoNorth_eastSector.addForces("Ix", 2);
                cielagoNorth_eastSector.addForces("BT", 1);
                cielagoNorth_westSector.addForces("Ix", 1);
                battle = new Battle(game, List.of(cielagoNorth_eastSector, cielagoNorth_westSector), List.of(ix, bt));
                battle.setBattlePlan(game, ix, cammarPilru, null, false, 1, false, 0, chaumas, null);
                battle.setBattlePlan(game, bt, zoal, null, false, 0, false, 0, null, null);
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotAnnounceReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.IX + " loses 2 " + Emojis.IX_SUBOID + " to the tanks"));
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.IX + " may send"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testPublishDoesNotAnnounceReplacement() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.IX + " loses 2 " + Emojis.IX_SUBOID + " to the tanks"));
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.IX + " may send"));
                assertFalse(battle.isResolved(game));
            }

            @Test
            void testResolveRemovesSuboids() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.IX_SUBOID + " in Cielago North (East Sector) were sent to the tanks.")));
                assertTrue(battle.isResolved(game));
            }
        }
    }

    @Nested
    @DisplayName("#resolutionInJacururuSietchAndKHCounteIncrease")
    class ResolutionInJacurutuSietchAndKHCounterIncrease {
        Battle battle;
        Territory jacurutuSietch;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(emperor);
            game.getTerritories().addDiscoveryToken("Jacurutu Sietch", true);
            jacurutuSietch = game.getTerritory("Jacurutu Sietch");
            jacurutuSietch.addForces("Atreides", 3);
            jacurutuSietch.addForces("Emperor", 2);
            jacurutuSietch.addForces("Emperor*", 2);
            battle = new Battle(game, List.of(jacurutuSietch), List.of(atreides, emperor));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, null, null);
            battle.setBattlePlan(game, emperor, bashar, null, false, 1, false, 0, null, null);
            battle.updateTroopsDialed(game, "Emperor", 0, 1);
            modInfo.clear();
        }

        @Nested
        @DisplayName("#khCounterIncrease")
        class KHCounterIncrease {
            @Test
            void testResolveIncreasesKHCounter() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(2, atreides.getForcesLost());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " KH counter increased from 0 to 2.")));
            }
        }

        @Nested
        @DisplayName("#jacurutuSietchSpice")
        class JacurutuSietchSpice {
            @Test
            void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 3 " + Emojis.SPICE + " for 2 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 3 " + Emojis.SPICE + " for 2 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testResolveGivesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 3 " + Emojis.SPICE + " for 2 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR + " not dialed.")));
                assertEquals(13, atreides.getSpice());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithCaladanHighThreshold")
    class ResolutionWithCaladanHighThreshold {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            harkonnen.addTreacheryCard(cheapHero);
            carthag.addForces("Atreides", 1);
            battle = new Battle(game, List.of(carthag), List.of(atreides, harkonnen));
        }

        @Nested
        @DisplayName("#atreidesWinsAtHighThreshold")
        class AtreidesWinsAtHighThreshold {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
                modInfo.clear();
                atreidesChat.clear();
                assertTrue(battle.isAggressorWin(game));
            }

            @Test
            void testReviewDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testPublishDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testResolveGivesChoicesDoesNotPlaceForce() throws InvalidGameStateException {
                assertEquals(10, game.getTerritory("Caladan").getForceStrength("Atreides"));
                battle.printBattleResolution(game, false, true);
                assertEquals("Would you like to place 1 " + Emojis.ATREIDES_TROOP + " from reserves in Carthag with Caladan High Threshold? at", atreidesChat.getMessages().getFirst());
                assertEquals(2, atreidesChat.getChoices().getFirst().size());
                assertEquals(10, game.getTerritory("Caladan").getForceStrength("Atreides"));
                assertEquals(1, carthag.getForceStrength("Atreides"));
            }
        }

        @Nested
        @DisplayName("#atreidesWinsAtHighThresholdNoForcesLeft")
        class AtreidesWinsAtHighThresholNoForcesLeft {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
                modInfo.clear();
                atreidesChat.clear();
                assertTrue(battle.isAggressorWin(game));
            }

            @Test
            void testReviewDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testPublishDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testResolveDoesNotAddForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(0, carthag.getForceStrength("Atreides"));
                assertEquals(0, atreidesChat.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#atreidesLosesAtHighThreshold")
        class AtreidesLosesAtHighThreshold {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true, 0, null, null);
                battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
                modInfo.clear();
                atreidesChat.clear();
                assertFalse(battle.isAggressorWin(game));
            }

            @Test
            void testReviewDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testPublishDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testResolveDoesNotAddForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(0, carthag.getForceStrength("Atreides"));
                assertEquals(0, atreidesChat.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#atreidesWinsAtLowThreshold")
        class AtreidesWinsAtLowThreshold {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.removeReserves(5);
                assertFalse(atreides.isHighThreshold());
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
                modInfo.clear();
                atreidesChat.clear();
                assertTrue(battle.isAggressorWin(game));
            }

            @Test
            void testReviewDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testPublishDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testResolveDoesNotAddForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(1, carthag.getForceStrength("Atreides"));
                assertEquals(0, atreidesChat.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#atreidesWinsNonHomeworldsGame")
        class AtreidesWinsNonHomewoldsGame {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.removeGameOption(GameOption.HOMEWORLDS);
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
                modInfo.clear();
                atreidesChat.clear();
                assertTrue(battle.isAggressorWin(game));
            }

            @Test
            void testReviewDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testPublishDoesNotAnnounceCaladan() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " may add 1 " + Emojis.ATREIDES_TROOP + " from reserves to Carthag with Caladan High Threshold\n"));
            }

            @Test
            void testResolveDoesNotAddForce() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals(1, carthag.getForceStrength("Atreides"));
                assertEquals(0, atreidesChat.getMessages().size());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithStrongholdCards")
    class ResolutionWithStrongholdCards {
        Battle battle;
        StrongholdCard sietchTabrCard;
        StrongholdCard tueksSietchCard;
        StrongholdCard hmsCard;
        Territory hms;

        @BeforeEach
        void setUp() {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            game.addFaction(atreides);
            game.addFaction(ix);
            hms = game.getTerritory("Hidden Mobile Stronghold");
            sietchTabrCard = new StrongholdCard("Sietch Tabr");
            tueksSietchCard = new StrongholdCard("Tuek's Sietch");
            hmsCard = new StrongholdCard("Hidden Mobile Stronghold");
            atreides.addStrongholdCard(sietchTabrCard);
            atreides.addStrongholdCard(tueksSietchCard);
            atreides.addStrongholdCard(hmsCard);
            atreides.addTreacheryCard(baliset);
            atreides.addTreacheryCard(kulon);
        }

        @Nested
        @DisplayName("#sietchTabr")
        class SietchTabr {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                sietchTabr.addForces("Atreides", 4);
                sietchTabr.addForces("Ix", 1);
                sietchTabr.addForces("Ix*", 1);
                battle = new Battle(game, List.of(sietchTabr), List.of(atreides, ix));
                battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, baliset, kulon);
                battle.setBattlePlan(game, ix, cammarPilru, null, false, 2, true, 1, null, null);
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card\n"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card\n"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testResolveGivesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card.")));
                assertEquals(12, atreides.getSpice());
            }
        }

        @Nested
        @DisplayName("#tueksSietch")
        class TueksSietch {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                tueksSietch.addForces("Atreides", 4);
                tueksSietch.addForces("Ix", 1);
                tueksSietch.addForces("Ix*", 1);
                battle = new Battle(game, List.of(tueksSietch), List.of(atreides, ix));
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, baliset, kulon);
                battle.setBattlePlan(game, ix, cammarPilru, null, false, 2, true, 1, null, null);
                modInfo.clear();
            }

            @Test
            void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n"));
                assertEquals(10, atreides.getSpice());
            }

            @Test
            void testResolveGivesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card.")));
                assertEquals(14, atreides.getSpice());
            }
        }

        @Nested
        @DisplayName("#hiddenMobileStronghold")
        class HiddenMobileStronghold {
            @BeforeEach
            void setUp() {
                hms.addForces("Atreides", 4);
                hms.addForces("Ix", 1);
                hms.addForces("Ix*", 1);
            }

            @Nested
            @DisplayName("#sietchTabrChosen")
            class SietchTabrChosen {
                @BeforeEach
                void setUp() throws InvalidGameStateException {
                    battle = new Battle(game, List.of(hms), List.of(atreides, ix));
                    battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, baliset, kulon);
                    battle.setHMSStrongholdCard(atreides, "Sietch Tabr");
                    battle.setBattlePlan(game, ix, cammarPilru, null, false, 2, true, 1, null, null);
                    modInfo.clear();
                }

                @Test
                void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, false);
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card\n"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, true, false);
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card\n"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testResolveGivesSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, true);
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 2 " + Emojis.SPICE + " for Sietch Tabr stronghold card.")));
                    assertEquals(12, atreides.getSpice());
                }
            }

            @Nested
            @DisplayName("#tueksSietchChosen")
            class TueksSietchChosen {
                @BeforeEach
                void setUp() throws InvalidGameStateException {
                    battle = new Battle(game, List.of(hms), List.of(atreides, ix));
                    battle.setHMSStrongholdCard(atreides, "Tuek's Sietch");
                    battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, baliset, kulon);
                    battle.setBattlePlan(game, ix, cammarPilru, null, false, 2, true, 1, null, null);
                    modInfo.clear();
                }

                @Test
                void testReviewDoesNotGiveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, false);
                    assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testPublishDoesNotGiveSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, true, false);
                    assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card\n"));
                    assertEquals(10, atreides.getSpice());
                }

                @Test
                void testResolveGivesSpice() throws InvalidGameStateException {
                    battle.printBattleResolution(game, false, true);
                    assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " gains 4 " + Emojis.SPICE + " for Tuek's Sietch stronghold card.")));
                    assertEquals(14, atreides.getSpice());
                }
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithAuditor")
    class ResolutionWithAuditor {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(choam);
            game.addFaction(atreides);
            arrakeen.addForces("CHOAM", 1);
        }

        @Nested
        @DisplayName("#auditorLivesAndIsAggressor")
        class AuditorLivesAndIsAggressor {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle = new Battle(game, List.of(arrakeen), List.of(choam, atreides));
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
                battle.setBattlePlan(game, choam, auditor, null, false, 0, false, 0, null, null);
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.CHOAM + " may audit 2 " + Emojis.TREACHERY + " cards not used in the battle unless " + Emojis.ATREIDES + " pays to cancel the audit.\n"));
            }

            @Test
            void testPublishDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.CHOAM + " may audit 2 " + Emojis.TREACHERY + " cards not used in the battle unless " + Emojis.ATREIDES + " pays to cancel the audit.\n"));
            }

            @Test
            void testResolveGivesChoicesTwoCardsToAudit() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                atreides.addTreacheryCard(shield);
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isAuditorMustBeResolved());
                assertEquals("Will you pay 2 " + Emojis.SPICE + " to cancel the audit? at", atreidesChat.getMessages().getLast());
                assertEquals(2, atreidesChat.getChoices().getLast().size());
                assertFalse(game.getLeaderTanks().contains(auditor));
            }

            @Test
            void testResolveGivesChoicesOneCardToAudit() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isAuditorMustBeResolved());
                assertEquals("Will you pay 1 " + Emojis.SPICE + " to cancel the audit? at", atreidesChat.getMessages().getLast());
                assertEquals(2, atreidesChat.getChoices().getLast().size());
                assertFalse(game.getLeaderTanks().contains(auditor));
            }

            @Test
            void testResolveWithNoCardsToAudit() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " has no cards that can be audited.")));
                assertFalse(game.getLeaderTanks().contains(auditor));
            }

            @Test
            void testOpponentPaysToCancelTwoCardsToAudit() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                atreides.addTreacheryCard(shield);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(8, atreides.getSpice());
                assertEquals(4, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " paid " + Emojis.CHOAM + " 2 " + Emojis.SPICE + " to cancel the audit.", turnSummary.getMessages().getLast());
            }

            @Test
            void testOpponentPaysToCancelOneCardToAudit() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(9, atreides.getSpice());
                assertEquals(3, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " paid " + Emojis.CHOAM + " 1 " + Emojis.SPICE + " to cancel the audit.", turnSummary.getMessages().getLast());
            }

            @Test
            void testOpponentPaysToCancelNoCardsToAudit() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(10, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " paid " + Emojis.CHOAM + " 0 " + Emojis.SPICE + " to cancel the audit.", turnSummary.getMessages().getLast());
            }

            @Test
            void testAuditedOpponentHasNoCards() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(10, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has no " + Emojis.TREACHERY + " cards not played in the battle.", choamChat.getMessages().getLast());
                assertEquals("You had no " + Emojis.TREACHERY + " cards not played in the battle for " + Emojis.CHOAM + " to audit.", atreidesChat.getMessages().getLast());
                assertTrue(modInfo.getMessages().isEmpty());
            }

            @Test
            void testAuditedOpponentHasOneCard() throws InvalidGameStateException {
                atreides.addTreacheryCard(shield);
                battle.printBattleResolution(game, false, true);
                modInfo.clear();
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(10, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY, choamChat.getMessages().getLast());
                assertEquals(Emojis.CHOAM + " audited " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY, atreidesChat.getMessages().getLast());
                assertTrue(modInfo.getMessages().isEmpty());
            }

            @Test
            void testAuditedOpponentHasTwoCards() throws InvalidGameStateException {
                atreides.addTreacheryCard(shield);
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(10, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                boolean lasgunAndShield = choamChat.getMessages().getLast().equals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY + " and " + Emojis.TREACHERY + " Lasgun " + Emojis.TREACHERY);
                boolean shielsAndLasgun = choamChat.getMessages().getLast().equals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Lasgun " + Emojis.TREACHERY + " and " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY);
                assertTrue(lasgunAndShield || shielsAndLasgun);
                lasgunAndShield = atreidesChat.getMessages().getLast().equals(Emojis.CHOAM + " audited " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY + " and " + Emojis.TREACHERY + " Lasgun " + Emojis.TREACHERY);
                shielsAndLasgun = atreidesChat.getMessages().getLast().equals(Emojis.CHOAM + " audited " + Emojis.TREACHERY + " Lasgun " + Emojis.TREACHERY + " and " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY);
                assertTrue(lasgunAndShield || shielsAndLasgun);
                assertTrue(modInfo.getMessages().isEmpty());
            }
        }

        @Nested
        @DisplayName("#auditorDiesAndIsDefender")
        class AuditorDiesAndIsDefender {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.addTreacheryCard(chaumas);
                atreides.addTreacheryCard(shield);
                battle = new Battle(game, List.of(arrakeen), List.of(atreides, choam));
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, chaumas, shield);
                battle.setBattlePlan(game, choam, auditor, null, false, 0, false, 0, null, null);
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.CHOAM + " may audit 1 " + Emojis.TREACHERY + " cards not used in the battle unless " + Emojis.ATREIDES + " pays to cancel the audit.\n"));
            }

            @Test
            void testPublishDoesNotGiveChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.CHOAM + " may audit 1 " + Emojis.TREACHERY + " cards not used in the battle unless " + Emojis.ATREIDES + " pays to cancel the audit.\n"));
            }

            @Test
            void testResolveGivesChoices() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                assertTrue(battle.isAuditorMustBeResolved());
                assertEquals("Will you pay 1 " + Emojis.SPICE + " to cancel the audit? at", atreidesChat.getMessages().getLast());
                assertEquals(2, atreidesChat.getChoices().getLast().size());
                assertTrue(game.getLeaderTanks().contains(auditor));
                assertEquals(12, atreides.getSpice());
            }

            @Test
            void testResolveWithNoCardsToAudit() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " has no cards that can be audited.")));
                assertTrue(game.getLeaderTanks().contains(auditor));
                assertEquals(12, atreides.getSpice());
            }

            @Test
            void testOpponentPaysToCancelOneCardToAudit() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(11, atreides.getSpice());
                assertEquals(3, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " paid " + Emojis.CHOAM + " 1 " + Emojis.SPICE + " to cancel the audit.", turnSummary.getMessages().getLast());
            }

            @Test
            void testOpponentPaysToCancelNoCardsToAudit() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, true);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(12, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " paid " + Emojis.CHOAM + " 0 " + Emojis.SPICE + " to cancel the audit.", turnSummary.getMessages().getLast());
            }

            @Test
            void testAuditedOpponentHasTwoCardsPlayedBothInBattle() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(12, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has no " + Emojis.TREACHERY + " cards not played in the battle.", choamChat.getMessages().getLast());
                assertEquals("You had no " + Emojis.TREACHERY + " cards not played in the battle for " + Emojis.CHOAM + " to audit.", atreidesChat.getMessages().getLast());
                assertTrue(modInfo.getMessages().isEmpty());
            }

            @Test
            void testAuditedOpponentHasThreeCardsPlayedTwoInBattle() throws InvalidGameStateException {
                atreides.addTreacheryCard(lasgun);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(12, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Lasgun " + Emojis.TREACHERY, choamChat.getMessages().getLast());
            }

            @Test
            void testAuditedOpponentHasTwoShieldsPlayedOneInBattle() throws InvalidGameStateException {
                atreides.addTreacheryCard(shield);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(12, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY, choamChat.getMessages().getLast());
            }

            @Test
            void testAuditedOpponentHasFourCardsPlayedTwoInBattle() throws InvalidGameStateException {
                atreides.addTreacheryCard(shield);
                atreides.addTreacheryCard(shield);
                battle.printBattleResolution(game, false, true);
                battle.cancelAudit(game, false);
                assertFalse(battle.isAuditorMustBeResolved());
                assertEquals(12, atreides.getSpice());
                assertEquals(2, choam.getSpice());
                assertEquals(Emojis.ATREIDES + " accepts audit from " + Emojis.CHOAM, turnSummary.getMessages().getLast());
                assertEquals(Emojis.ATREIDES + " has " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY, choamChat.getMessages().getLast());
                assertEquals(Emojis.CHOAM + " audited " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY, atreidesChat.getMessages().getLast());
                assertTrue(modInfo.getMessages().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithDukeVidal")
    class ResolutionWithDukeVidal {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(atreides);
            game.addFaction(bt);
        }

        @Nested
        @DisplayName("#ecazPlaysDukeVidal")
        class EcazPlaysDukeVidal {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                arrakeen.addForces("Ecaz", 1);
                ecaz.addLeader(dukeVidal);
                battle = new Battle(game, List.of(arrakeen), List.of(atreides, ecaz));
                battle.setBattlePlan(game, atreides, "Duncan Idaho", false, "0", 0, "None", "None");
                battle.setBattlePlan(game, ecaz, "Duke Vidal", false, "0.5", 0, "None", "None");
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " sets Duke Vidal aside"));
                assertTrue(ecaz.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testPublishDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ECAZ + " sets Duke Vidal aside"));
                assertTrue(ecaz.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testResolveSetsVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("Duke Vidal is no longer in service to " + Emojis.ECAZ)));
                assertFalse(ecaz.getLeader("Duke Vidal").isPresent());
                assertFalse(game.getLeaderTanks().contains(dukeVidal));
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }
        }

        @Nested
        @DisplayName("#btPlaysDukeVidalAsAGhola")
        class BTPlaysDukeVidalAsAGhola {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                arrakeen.addForces("BT", 1);
                bt.addLeader(dukeVidal);
                battle = new Battle(game, List.of(arrakeen), List.of(atreides, bt));
                battle.setBattlePlan(game, atreides, "Duncan Idaho", false, "0", 0, "None", "None");
                battle.setBattlePlan(game, bt, "Duke Vidal", false, "0.5", 0, "None", "None");
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertFalse(modInfo.getMessages().getFirst().contains(" sets Duke Vidal aside"));
                assertTrue(bt.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testPublishDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertFalse(turnSummary.getMessages().getFirst().contains(" sets Duke Vidal aside"));
                assertTrue(bt.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testResolveSetsVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertFalse(turnSummary.getMessages().stream().anyMatch(m -> m.equals("Duke Vidal is no longer in service to " + Emojis.BT)));
                assertTrue(bt.getLeader("Duke Vidal").isPresent());
                assertFalse(game.getLeaderTanks().contains(dukeVidal));
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }
        }

        @Nested
        @DisplayName("#btPlaysDukeVidalAsAGhola")
        class BTAlliedWithEcazPlaysDukeVidal {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.createAlliance(bt, ecaz);
                arrakeen.addForces("BT", 1);
                bt.addLeader(dukeVidal);
                battle = new Battle(game, List.of(arrakeen), List.of(atreides, bt));
                battle.setBattlePlan(game, atreides, "Duncan Idaho", false, "0", 0, "None", "None");
                battle.setBattlePlan(game, bt, "Duke Vidal", false, "0.5", 0, "None", "None");
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.BT + " sets Duke Vidal aside"));
                assertTrue(modInfo.getMessages().getFirst().contains("If Duke Vidal was a Ghola, he should be assigned back to " + Emojis.BT));
                assertTrue(bt.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testPublishDoesNotSetVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.BT + " sets Duke Vidal aside"));
                assertTrue(turnSummary.getMessages().getFirst().contains("If Duke Vidal was a Ghola, he should be assigned back to " + Emojis.BT));
                assertTrue(bt.getLeader("Duke Vidal").isPresent());
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }

            @Test
            void testResolveSetsVidalAside() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("Duke Vidal is no longer in service to " + Emojis.BT)));
                assertTrue(modInfo.getMessages().stream().anyMatch(m -> m.equals("If Duke Vidal was a Ghola, he should be assigned back to " + Emojis.BT + " ")));
                assertFalse(bt.getLeader("Duke Vidal").isPresent());
                assertFalse(game.getLeaderTanks().contains(dukeVidal));
                assertEquals("Arrakeen", dukeVidal.getBattleTerritoryName());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithLasgunShieldCarnage")
    class ResolutionWithLasgunShieldCarnage {
        Battle battle;

        @BeforeEach
        void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            atreides.addTreacheryCard(lasgun);
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(shield);
            cielagoNorth_eastSector.addForces("Atreides", 5);
            cielagoNorth_eastSector.addForces("Harkonnen", 2);
            // battle specification reflects storm being in sector 1 and splitting Cielago North
            battle = new Battle(game, List.of(cielagoNorth_eastSector), List.of(atreides, harkonnen));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, lasgun, null);
            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, shield);
            modInfo.clear();
            turnSummary.clear();
        }

        @Nested
        @DisplayName("#advsorsKilledByLasgunShield")
        class AdvisorsKilledByLasgunShield {
            @BeforeEach
            void setUp() {
                game.addFaction(bg);
                cielagoNorth_westSector.addForces("Advisor", 1);
            }

            @Test
            void testReviewDoesNotKillAdvisor() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.BG + " loses 1 " + Emojis.BG_ADVISOR + " in Cielago North (West Sector) to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("BG"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Advisor"));
            }

            @Test
            void testPublishDoesNotKillAdvisor() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.BG + " loses 1 " + Emojis.BG_ADVISOR + " in Cielago North (West Sector) to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("BG"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Advisor"));
            }

            @Test
            void testResolveKillsAdvisor() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.BG_ADVISOR + " in Cielago North (West Sector) were sent to the tanks.")));
                assertEquals(1, game.getTleilaxuTanks().getForceStrength("BG"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Advisor"));
            }
        }

        @Nested
        @DisplayName("#noFieldRevealedAndForcesKilledByLasgunShield")
        class NoFieldRevealedAndForcesKilledByLasgunShield {
            @BeforeEach
            void setUp() {
                game.addFaction(richese);
                cielagoNorth_westSector.setRicheseNoField(5);
            }

            @Test
            void testReviewDoesNotRevealNoFieldAndKill() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be 5 " + Emojis.RICHESE_TROOP + " and loses them to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Richese"));
            }

            @Test
            void testPublishDoesNotRevealNoFieldAndKill() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.RICHESE + " reveals " + Emojis.NO_FIELD + " to be 5 " + Emojis.RICHESE_TROOP + " and loses them to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Richese"));
            }

            @Test
            void testResolveRevealsAndKills() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("The 5 " + Emojis.NO_FIELD + " in Cielago North (West Sector) reveals 5 " + Emojis.RICHESE_TROOP)));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("5 " + Emojis.RICHESE_TROOP + " in Cielago North (West Sector) were sent to the tanks.")));
                assertEquals(5, richese.getFrontOfShieldNoField());
                assertEquals(5, game.getTleilaxuTanks().getForceStrength("Richese"));
            }
        }

        @Nested
        @DisplayName("#forcesAcrossStormKilledByLasgunShield")
        class ForcesAcrossStormKilledByLasgunShield {
            @BeforeEach
            void setUp() {
                cielagoNorth_westSector.addForces("Atreides", 2);
                cielagoNorth_westSector.addForces("Harkonnen", 1);
            }

            @Test
            void testReviewDoesNotKillAcrossStorm() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.ATREIDES_TROOP + " in Cielago North (West Sector) to the tanks"));
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 1 " + Emojis.HARKONNEN_TROOP + " in Cielago North (West Sector) to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Atreides"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Harkonnen"));
            }

            @Test
            void testPublishDoesNotKillAcrossStorm() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.ATREIDES + " loses 2 " + Emojis.ATREIDES_TROOP + " in Cielago North (West Sector) to the tanks"));
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.HARKONNEN + " loses 1 " + Emojis.HARKONNEN_TROOP + " in Cielago North (West Sector) to the tanks"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Atreides"));
                assertEquals(0, game.getTleilaxuTanks().getForceStrength("Harkonnen"));
            }

            @Test
            void testResolveKillsAcrossStorm() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("2 " + Emojis.ATREIDES_TROOP + " in Cielago North (West Sector) were sent to the tanks.")));
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("1 " + Emojis.HARKONNEN_TROOP + " in Cielago North (West Sector) were sent to the tanks.")));
                assertEquals(7, game.getTleilaxuTanks().getForceStrength("Atreides"));
                assertEquals(3, game.getTleilaxuTanks().getForceStrength("Harkonnen"));
            }
        }

        @Nested
        @DisplayName("#ambassadorRemovedByLasgunShield")
        class AmbassadorRemovedByLasgunShield {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addFaction(ecaz);
                // Ambassador test really should be moved to a new test setup in a Stronghold
                ecaz.placeAmbassador(cielagoNorth_westSector.getTerritoryName(), "Fremen", 1);
            }

            @Test
            void testReviewDoesNotRemoveAmbassador() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.ECAZ + " Fremen ambassador returned to supply"));
                assertEquals("Fremen", cielagoNorth_westSector.getEcazAmbassador());
            }

            @Test
            void testPublishDoesNotRemoveAmbassador() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertTrue(turnSummary.getMessages().getLast().contains(Emojis.ECAZ + " Fremen ambassador returned to supply"));
                assertEquals("Fremen", cielagoNorth_westSector.getEcazAmbassador());
            }

            @Test
            void testResolveRemovesAmbassador() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                // Ambassador test really should be moved to a new test setup in a Stronghold
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ECAZ + " Fremen ambassador returned to supply.")));
                assertNull(cielagoNorth_westSector.getEcazAmbassador());
                assertTrue(ecaz.getAmbassadorSupply().contains("Fremen"));
            }
        }

        @Nested
        @DisplayName("#spiceDestroyeByLasgunShield")
        class SpiceDestroyedByLasgunShield {
            @BeforeEach
            void setUp() {
                cielagoNorth_westSector.setSpice(8);
            }

            @Test
            void testReviewDoesNotRemoveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(modInfo.getMessages().getFirst().contains("8 " + Emojis.SPICE + " destroyed in Cielago North (West Sector)"));
                assertEquals(8, cielagoNorth_westSector.getSpice());
            }

            @Test
            void testPublishDoesNotRemoveSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(turnSummary.getMessages().getFirst().contains("8 " + Emojis.SPICE + " destroyed in Cielago North (West Sector)"));
                assertEquals(8, cielagoNorth_westSector.getSpice());
            }

            @Test
            void testResolveRemovesSpice() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("8 " + Emojis.SPICE + " in Cielago North (West Sector) was destroyed by Lasgun-Shield.")));
                assertEquals(0, cielagoNorth_westSector.getSpice());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithAssassination")
    class ResolutionWithAssassination {
        Battle battle;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(moritani);
            game.addFaction(harkonnen);
            moritani.addTraitorCard(new TraitorCard("Umman Kudu", "Harkonnen", 1));
            harkonnen.addTraitorCard(new TraitorCard("Hiih Resser", "Moritani", 4));
            carthag.addForces("Moritani", 7);
            battle = new Battle(game, List.of(carthag), List.of(moritani, harkonnen));
            battle.setBattlePlan(game, moritani, hiihResser, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
        }

        @Test
        void testCanAssassinate() throws InvalidGameStateException {
            assertFalse(battle.isAggressorWin(game));
            battle.printBattleResolution(game, false, true);
            assertEquals("Would you like to assassinate Umman Kudu? mo", moritaniChat.getMessages().getLast());
            assertEquals(2, moritaniChat.getChoices().getLast().size());
            assertEquals("moritani-assassinate-traitor-yes", moritaniChat.getChoices().getLast().getFirst().getId());
            assertEquals("moritani-assassinate-traitor-no", moritaniChat.getChoices().getLast().getLast().getId());
            assertTrue(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testTraitorIsInTanks() throws InvalidGameStateException {
            game.killLeader(harkonnen, "Umman Kudu");
            battle.printBattleResolution(game, false, true);
            assertEquals("Umman Kudu is in the tanks. Would you like to assassinate and draw a new Traitor in Mentat Pause? mo", moritaniChat.getMessages().getLast());
            assertEquals(2, moritaniChat.getChoices().getLast().size());
            assertEquals("moritani-assassinate-traitor-yes", moritaniChat.getChoices().getLast().getFirst().getId());
            assertEquals("moritani-assassinate-traitor-no", moritaniChat.getChoices().getLast().getLast().getId());
            assertTrue(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testMoritaniWonTheBattle() throws InvalidGameStateException {
            battle.setBattlePlan(game, moritani, hiihResser, null, false, 3, false, 0, null, null);
            assertTrue(battle.isAggressorWin(game));
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testMoritaniKilledTheLeader() throws InvalidGameStateException {
            moritani.addTreacheryCard(chaumas);
            battle.setBattlePlan(game, moritani, hiihResser, null, false, 3, false, 0, chaumas, null);
            assertTrue(battle.isAggressorWin(game));
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testTraitorUsedInBattle() throws InvalidGameStateException {
            battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 5, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testOpponentPlayedCheapHero() throws InvalidGameStateException {
            harkonnen.addTreacheryCard(cheapHero);
            battle.setBattlePlan(game, harkonnen, null, cheapHero, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testMoritaniHasNoTraitorCard() throws InvalidGameStateException {
            moritani.assassinateTraitor();
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testOpponentCallsTraitor() throws InvalidGameStateException {
            battle.getDefenderBattlePlan().setWillCallTraitor(true);
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }

        @Test
        void testAlreadyAssassinatedFaction() throws InvalidGameStateException {
            moritani.assassinateTraitor();
            moritani.performMentatPauseActions(false);
            battle.printBattleResolution(game, false, true);
            assertFalse(battle.isAssassinationMustBeResolved());
        }
    }

    @Nested
    @DisplayName("#resolutoinWithMoritaniAndAlly")
    class ResolutionWithMoritaniAndAlly {
        Battle battle;

        @BeforeEach
        void setUp() {
            game.addFaction(moritani);
            game.addFaction(emperor);
            game.addFaction(atreides);
            game.createAlliance(moritani, emperor);
            emperor.addTreacheryCard(cheapHero);
            emperor.addTreacheryCard(chaumas);
            emperor.addTreacheryCard(shield);
            atreides.addTreacheryCard(crysknife);
            atreides.addTreacheryCard(shield);
            atreides.addTreacheryCard(lasgun);
            emperor.placeForceFromReserves(game, sietchTabr, 10, false);
            atreides.placeForceFromReserves(game, sietchTabr, 10, false);
            battle = new Battle(game, List.of(sietchTabr), List.of(atreides, emperor));
            assertTrue(emperor.hasTreacheryCard("Cheap Heroine"));
            assertTrue(emperor.hasTreacheryCard("Chaumas"));
            assertTrue(emperor.hasTreacheryCard("Shield"));
            assertTrue(atreides.hasTreacheryCard("Crysknife"));
            assertTrue(atreides.hasTreacheryCard("Shield"));
            assertTrue(atreides.hasTreacheryCard("Lasgun"));
        }

        @Nested
        @DisplayName("#moritaniAllyLoses")
        class MoritaniAllyLoses {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, null, null);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, chaumas, shield);
                assertTrue(battle.isAggressorWin(game));
                emperorChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertTrue(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testResolveOffersChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertEquals("Would you like to retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally? em", emperorChat.getMessages().getFirst());
                assertEquals(4, emperorChat.getChoices().getLast().size());
            }
        }

        @Nested
        @DisplayName("#moritaniAllyLosesPlayedNoCards")
        class MoritaniAllyLosesPlayedNoCards {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, null, null);
                battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
                assertTrue(battle.isAggressorWin(game));
                emperorChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testResolveOffersChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
            }
        }

        @Nested
        @DisplayName("#moritaniAllyWins")
        class MoritaniAllyWins {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, crysknife, shield);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 3, false, 0, chaumas, shield);
                assertFalse(battle.isAggressorWin(game));
                emperorChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testResolveDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
            }
        }

        @Nested
        @DisplayName("#moritaniAllyLasgunShield")
        class MoritaniAllyLasgunShield {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, lasgun, null);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, chaumas, shield);
                emperorChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testResolveDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
            }
        }

        @Nested
        @DisplayName("#moritaniAllyDualTraitors")
        class MoritaniAllyDualTraitors {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                atreides.addTraitorCard(new TraitorCard("Cheap Hero", "Any", 0));
                emperor.addTraitorCard(new TraitorCard("Duncan Idaho", "Atreides", 0));
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false, 0, lasgun, null);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 0, false, 0, chaumas, shield);
                battle.willCallTraitor(game, atreides, true, 0, "Sietch Tabr");
                battle.willCallTraitor(game, emperor, true, 0, "Sietch Tabr");
                emperorChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertEquals(1, emperorChat.getMessages().size());
                assertEquals("Duncan Idaho has betrayed " + Emojis.ATREIDES + " for you!", emperorChat.getMessages().getFirst());
                assertTrue(emperorChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.EMPEROR + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testResolveDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(emperorChat.getMessages().isEmpty());
                assertTrue(emperorChat.getChoices().isEmpty());
            }
        }

        @Nested
        @DisplayName("#notMoritaniAllyLoses")
        class NotMoritaniAllyLoses {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, crysknife, shield);
                battle.setBattlePlan(game, emperor, null, cheapHero, false, 3, false, 0, chaumas, shield);
                atreidesChat.clear();
                modInfo.clear();
                turnSummary.clear();
            }

            @Test
            void testReviewDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, false);
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertTrue(atreidesChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testPublishDoesNotOfferChoices() throws InvalidGameStateException {
                battle.printBattleResolution(game, true, false);
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertTrue(atreidesChat.getChoices().isEmpty());
                assertFalse(modInfo.getMessages().getFirst().contains(Emojis.ATREIDES + " may retain a discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally"));
            }

            @Test
            void testNotMoritaniAllyLoses() throws InvalidGameStateException {
                battle.printBattleResolution(game, false, true);
                assertTrue(atreidesChat.getMessages().isEmpty());
                assertTrue(atreidesChat.getChoices().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("#resolutionWithOccupyScenarios")
    class ResolutionWithOccupyScenarios {
        @BeforeEach
        void setUp() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(emperor);
        }

        @Test
        void testHomeworldBecomesOccupied() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            assertNull(giediPrime.getOccupyingFaction());
            Battle battle = new Battle(game, List.of(giediPrime), List.of(atreides, harkonnen));
            battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            assertEquals("Giedi Prime is now occupied by " + Emojis.ATREIDES, turnSummary.getMessages().get(1));
        }

        @Test
        void testAllForcesLostDoesNotEstablishOccupy() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            assertNull(giediPrime.getOccupyingFaction());
            Battle battle = new Battle(game, List.of(giediPrime), List.of(atreides, harkonnen));
            battle.setBattlePlan(game, harkonnen, ummanKudu, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, true, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertTrue(giediPrime.getForces().isEmpty());
            assertNull(giediPrime.getOccupyingFaction());
            assertTrue(turnSummary.getMessages().stream().noneMatch(m -> m.contains("occupied")));
        }

        @Test
        void testNativeEndsOccation() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 10, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            harkonnen.reviveForces(false, 10);
            Battle battle = new Battle(game, List.of(giediPrime), List.of(harkonnen, atreides));
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertNull(giediPrime.getOccupyingFaction());
        }

        @Test
        void testNativeDefeatsInvaderButLosesAllForces() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 9, false, true);
            Battle battle = new Battle(game, List.of(giediPrime), List.of(harkonnen, atreides));
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, true, 0, null, null);
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertTrue(giediPrime.getForces().isEmpty());
            assertNull(giediPrime.getOccupyingFaction());
        }

        @Test
        void testNativeEndsOccationButLosesAllForces() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 10, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            harkonnen.reviveForces(false, 1);
            Battle battle = new Battle(game, List.of(giediPrime), List.of(harkonnen, atreides));
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, true, 0, null, null);
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertTrue(giediPrime.getForces().isEmpty());
            assertNull(giediPrime.getOccupyingFaction());
        }

        @Test
        void testOccupierChanges() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 10, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            emperor.placeForceFromReserves(game, giediPrime, 1, false);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            Battle battle = new Battle(game, List.of(giediPrime), List.of(emperor, atreides));
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertEquals(emperor, giediPrime.getOccupyingFaction());
        }

        @Test
        void testOccupierDoesNotChange() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 10, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            emperor.placeForceFromReserves(game, giediPrime, 1, false);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            Battle battle = new Battle(game, List.of(giediPrime), List.of(emperor, atreides));
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, emperor, burseg, null, false, 0, false, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            assertTrue(turnSummary.getMessages().stream().noneMatch(m -> m.contains("occupied")));
        }

        @Test
        void testTwoNonNativeNoForcesRemain() throws InvalidGameStateException {
            HomeworldTerritory giediPrime = (HomeworldTerritory) game.getTerritory(harkonnen.getHomeworld());
            atreides.placeForceFromReserves(game, giediPrime, 1, false);
            harkonnen.removeForces("Giedi Prime", 10, false, true);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            emperor.placeForceFromReserves(game, giediPrime, 1, false);
            assertEquals(atreides, giediPrime.getOccupyingFaction());
            Battle battle = new Battle(game, List.of(giediPrime), List.of(emperor, atreides));
            battle.setBattlePlan(game, atreides, ladyJessica, null, false, 0, true, 0, null, null);
            battle.setBattlePlan(game, emperor, burseg, null, false, 0, true, 0, null, null);
            turnSummary.clear();
            battle.printBattleResolution(game, false, true);
            assertNull(giediPrime.getOccupyingFaction());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.contains("is no longer occupied")));
        }
    }
}
