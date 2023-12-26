package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleTest {
    Game game;
    Battles battles;
    TestTopic turnSummary;
    AtreidesFaction atreides;
    EcazFaction ecaz;
    BGFaction bg;
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
        carthag = game.getTerritory("Carthag");
        cielagoNorth_eastSector = game.getTerritory("Cielago North (East Sector)");
        cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        garaKulon = game.getTerritory("Gara Kulon");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void aggregateForces() {
        Force emperorTroops = new Force("Emperor", 5);
        carthag.addForce(emperorTroops);
        Force sardaukar = new Force("Emperor*", 2);
        carthag.addForce(sardaukar);
        Battle battle = new Battle(game, "Carthag", List.of(carthag), List.of(emperor, harkonnen));
        Force harkonnenTroops = carthag.getForce("Harkonnen");
        List<Force> expectedResult = List.of(sardaukar, emperorTroops, harkonnenTroops);
        assertEquals(expectedResult, battle.aggregateForces(game));
    }

    @Test
    void aggregateForcesMultipleSectorsPlusNoField() {
        Force harkonnenTroops = new Force("Harkonnen", 3);
        cielagoNorth_eastSector.addForce(new Force("Emperor", 2));
        cielagoNorth_eastSector.addForce(harkonnenTroops);
        Force sardaukar = new Force("Emperor*", 2);
        cielagoNorth_westSector.addForce(new Force("Emperor", 2));
        cielagoNorth_westSector.addForce(sardaukar);
        cielagoNorth_westSector.setRicheseNoField(5);
        List<Territory> cielagoNorthSectors = List.of(
                game.getTerritory("Cielago North (West Sector)"),
                game.getTerritory("Cielago North (Center Sector)"),
                game.getTerritory("Cielago North (East Sector)")
        );
        Battle battle = new Battle(game, "Cielago North", cielagoNorthSectors, List.of(emperor, harkonnen, richese));
        Force emperorTroops = new Force("Emperor", 4);
        Force noFieldForce = new Force("NoField", 1, "Richese");
        List<Force> expectedResult = List.of(sardaukar, emperorTroops, harkonnenTroops, noFieldForce);
        assertEquals(expectedResult, battle.aggregateForces(game));
    }

    @Test
    void testAggressorMustChooseOpponentFalse() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor));
        assertFalse(battle.aggressorMustChooseOpponent(game));
    }

    @Test
    void testAggressorMustChooseOpponentTrue() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
        assertTrue(battle.aggressorMustChooseOpponent(game));
    }

    @Test
    void testAggressorMustChooseOpponentEcazAllyFalse() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
        assertFalse(battle.aggressorMustChooseOpponent(game));
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
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(fremen, harkonnen, emperor, ecaz));
        assertTrue(battle.aggressorMustChooseOpponent(game));
    }

    @Test
    void testEcazMustChooseBattleFactionFalseNoEcaz() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor));
        assertFalse(battle.hasEcazAndAlly(game));
    }

    @Test
    void testEcazMustChooseBattleFactionFalseWithEcaz() {
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
        assertFalse(battle.hasEcazAndAlly(game));
    }

    @Test
    void testEcazMustChooseBattleFactionTrue() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
        assertTrue(battle.hasEcazAndAlly(game));
    }

    @Test
    void testEcazAllyFighting() {
        emperor.setAlly("Ecaz");
        ecaz.setAlly("Emperor");
        garaKulon.addForce(new Force("Harkonnen", 10));
        garaKulon.addForce(new Force("Emperor", 5));
        garaKulon.addForce(new Force("Ecaz", 3));
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(ecaz, harkonnen, emperor));
        battle.setAggressor(emperor.getName());
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
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
        battle.setDefenderName(ecaz.getName());
        Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
        assertDoesNotThrow(() -> battle.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 5, false, 5, null, null));
    }

    @Test
    void testBattleResolved() {
        Force emperorTroops = new Force("Emperor", 5);
        carthag.addForce(emperorTroops);
        Force sardaukar = new Force("Emperor*", 2);
        carthag.addForce(sardaukar);
        Battle battle = new Battle(game, "Carthag", List.of(carthag), List.of(emperor, harkonnen));
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
        Battle battle = new Battle(game, "Gara Kulon", List.of(garaKulon), List.of(harkonnen, emperor, ecaz));
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
        TreacheryCard shield;
        Territory arrakeen;
        Territory carthag;
        Battle battle1;
        Battle battle2;

        @BeforeEach
        void setUp() throws IOException {
            game = new Game();
            atreides = new AtreidesFaction("aPlayer", "aUser", game);
            fremen = new FremenFaction("fPlayer", "fUser", game);
            harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
            ecaz = new EcazFaction("ePlayer", "eUser", game);
            game.addFaction(atreides);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
            arrakeen = game.getTerritory("Arrakeen");
            arrakeen.setForceStrength("Harkonnen", 1);
            battle1 = new Battle(game, "Arrakeen", List.of(arrakeen), List.of(atreides, harkonnen));
            ecaz.setAlly("Atreides");
            atreides.setAlly("Ecaz");
            carthag = game.getTerritory("Carthag");
            carthag.setForceStrength("Ecaz", 5);
            carthag.setForceStrength("Atreides", 1);
            battle2 = new Battle(game, "Carthag", List.of(carthag), List.of(atreides, harkonnen, ecaz));

            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
            cheapHero = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Cheap Hero ")). findFirst().orElseThrow();
            crysknife = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Crysknife ")). findFirst().orElseThrow();
            shield = game.getTreacheryDeck().stream().filter(c -> c.name().equals("Shield ")). findFirst().orElseThrow();
        }

        @Test
        void testBattlePlanLeaderAvailable() {
            assertDoesNotThrow(() -> battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 4, false, 3, null, null));
        }

        @Test
        void testBattlePlanLeaderInTanks() {
            atreides.removeLeader(duncanIdaho.name());
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
            assertThrows(InvalidGameStateException.class, () -> battle1.setBattlePlan(game, fremen, null, cheapHero, false, 4, false,3, null, null));
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
            assertEquals(1, atreidesChat.messages.size());
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
            assertEquals("2.5", battle1.getWinnerStrengthString(game));
            assertEquals("1", battle1.getLoserStrengthString(game));
        }

        @Test
        void testBattlePlanResolutionLeaderKilled() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertEquals(Emojis.HARKONNEN, battle1.getWinnerEmojis(game));
            assertEquals("1", battle1.getWinnerStrengthString(game));
            assertEquals("0.5", battle1.getLoserStrengthString(game));
        }

        @Test
        void testBattlePlanResolutionAggressorWinsTies() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            harkonnen.addTreacheryCard(crysknife);
            battle1.setBattlePlan(game, atreides, duncanIdaho, null, false, 1, false,0, null, null);
            battle1.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, crysknife, null);
            assertEquals(Emojis.ATREIDES, battle1.getWinnerEmojis(game));
            assertEquals("1", battle1.getWinnerStrengthString(game));
            assertEquals("1", battle1.getLoserStrengthString(game));
        }

        @Test
        void testBattlePlanResolutionAllyEcaz() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setAggressor("Atreides");
            battle2.setDefenderName("Harkonnen");
            battle2.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            assertEquals(Emojis.ATREIDES + Emojis.ECAZ, battle2.getWinnerEmojis(game));
            assertEquals("5.5", battle2.getWinnerStrengthString(game));
            assertEquals("1", battle2.getLoserStrengthString(game));
        }

        @Test
        void testBattlePlanResolutionEcazAlly() throws InvalidGameStateException {
            game.setTurnSummary(new TestTopic());
            harkonnen.addTreacheryCard(cheapHero);
            battle2.setAggressor("Ecaz");
            battle2.setDefenderName("Harkonnen");
            Leader sanyaEcaz = ecaz.getLeader("Sanya Ecaz").orElseThrow();
            battle2.setBattlePlan(game, ecaz, sanyaEcaz, null, false, 0, true,0, null, null);
            battle2.setBattlePlan(game, harkonnen, null, cheapHero, false, 1, false,1, null, null);
            assertEquals(Emojis.ECAZ + Emojis.ATREIDES, battle2.getWinnerEmojis(game));
            assertEquals("7.5", battle2.getWinnerStrengthString(game));
            assertEquals("1", battle2.getLoserStrengthString(game));
        }
    }
}
