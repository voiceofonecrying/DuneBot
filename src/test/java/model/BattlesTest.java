package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BattlesTest extends DuneTest {
    Battles battles;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void noBattlesRemaining() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.startBattlePhase();
        battles = game.getBattles();
        assertTrue(battles.noBattlesRemaining(game));
    }

    @Test
    void noBattlesRemaining_oneBattle() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void noBattlesRemaining_oneBattleResolved() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);

        game.startBattlePhase();
        battles = game.getBattles();
        cielagoNorth_westSector.removeForces(game, "BG", 6);
        assertTrue(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void aggressorMustChooseBattle() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        sietchTabr.addForces("Fremen*", 3);
        sietchTabr.addForces("BG", 1);

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertTrue(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void aggressorMustChooseBattle_firstBattleResolved() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        sietchTabr.addForces("Fremen*", 3);
        sietchTabr.addForces("BG", 1);

        game.startBattlePhase();
        battles = game.getBattles();
        sietchTabr.removeForces(game, "BG", 1);
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void aggressorMustChooseOpponent() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertTrue(battles.aggressorMustChooseOpponent());
    }


    @Test
    void aggressorMustChooseOpponentFalseWithEcazAlly() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(ecaz);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);
        atreides.setAlly("Ecaz");
        ecaz.setAlly("Atreides");

        game.setStorm(10);
        cielagoNorth_westSector.addForces("Ecaz", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void aggressorMustChooseOpponent_firstBattleResolved() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        cielagoNorth_eastSector.removeForces(game, "Atreides", 4);
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertFalse(battles.aggressorMustChooseOpponent());
    }

    @Test
    void testSetOpponent() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(10);
        cielagoNorth_westSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        battles.setTerritoryByIndex(0);
        battles.setOpponent(game, "Fremen");
        Battle currentBattle = battles.getCurrentBattle();
        assertEquals(2, currentBattle.getFactions(game).size());
        assertTrue(currentBattle.getForces().stream().noneMatch(f -> f.getName().equals("BG")));
        assertTrue(currentBattle.getForces().stream().anyMatch(f -> f.getName().equals("Atreides")));
        assertTrue(currentBattle.getForces().stream().anyMatch(f -> f.getName().equals("Fremen")));
    }

    @Test
    void testSetOpponent2() throws InvalidGameStateException {
        game.addFaction(emperor);
        game.addFaction(guild);
        game.addFaction(atreides);
        game.addFaction(fremen);
        game.addFaction(bg);
        game.addFaction(harkonnen);

        game.setStorm(5);
        windPassNorth_northSector.addForces("Fremen", 3);
        windPassNorth_northSector.addForces("Emperor", 2);
        windPassNorth_northSector.addForces("Emperor*", 1);
        windPassNorth_southSector.addForces("Atreides", 1);

        game.startBattlePhase();
        battles = game.getBattles();
        battles.setTerritoryByIndex(0);
        battles.setOpponent(game, "Emperor");
        Battle currentBattle = battles.getCurrentBattle();
        assertEquals(2, currentBattle.getFactions(game).size());
        assertTrue(currentBattle.getForces().stream().noneMatch(f -> f.getName().equals("Fremen")));
        assertTrue(currentBattle.getForces().stream().anyMatch(f -> f.getName().equals("Atreides")));
        assertTrue(currentBattle.getForces().stream().anyMatch(f -> f.getName().equals("Emperor")));
    }

    @Test
    void nextBattle() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(14);
        cielagoNorth_eastSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        game.startBattlePhase();
        battles = game.getBattles();
        battles.nextBattle(game);

        assertEquals("Next battle: 6 " + Emojis.BG_FIGHTER + " vs 7 " + Emojis.FREMEN_TROOP + " in Cielago North", turnSummary.messages.get(2));
    }

    @Test
    void nextBattleNoneLeft() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(14);
        game.startBattlePhase();
        battles = game.getBattles();

        assertThrows(InvalidGameStateException.class, () -> battles.nextBattle(game));
    }

    @Test
    void nextBattleAggressorMustChoose() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(14);
//        tueksSietch.addForces("Guild", 5);
        tueksSietch.addForces("Harkonnen", 2);
        carthag.addForces("BG", 2);
//        carthag.addForces("Harkonnen", 10);
        cielagoNorth_eastSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Fremen*", 1);
        cielagoNorth_eastSector.addForces("Emperor", 4);
        cielagoNorth_eastSector.addForces("Emperor*", 3);
        game.startBattlePhase();
        battles = game.getBattles();
        battles.nextBattle(game);

        assertEquals(Emojis.HARKONNEN + " must choose where they will fight:\n" +
                "Tuek's Sietch: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + "\n" +
                "Carthag: 10 " + Emojis.HARKONNEN_TROOP + " vs 2 " + Emojis.BG_FIGHTER,
                turnSummary.messages.get(2));
    }

    @Test
    void nextBattleDetectResolvedBattles() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);

        game.setStorm(14);
//        tueksSietch.addForces("Guild", 5);
        tueksSietch.addForces("Harkonnen", 2);
        carthag.addForces("BG", 2);
//        carthag.addForces("Harkonnen", 10);
        cielagoNorth_eastSector.addForces("BG", 6);
        cielagoNorth_eastSector.addForces("Fremen", 7);
        cielagoNorth_eastSector.addForces("Fremen*", 1);
        cielagoNorth_eastSector.addForces("Emperor", 4);
        cielagoNorth_eastSector.addForces("Emperor*", 3);
        game.startBattlePhase();
        battles = game.getBattles();
        battles.nextBattle(game);

        assertEquals(Emojis.HARKONNEN + " must choose where they will fight:\n" +
                        "Tuek's Sietch: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + "\n" +
                        "Carthag: 10 " + Emojis.HARKONNEN_TROOP + " vs 2 " + Emojis.BG_FIGHTER,
                turnSummary.messages.get(2));

        carthag.removeForces(game, "BG", 2);
        assertEquals(0, carthag.getForceStrength("BG"));

        battles.nextBattle(game);

        assertFalse(battles.aggressorMustChooseBattle());
        assertEquals("Next battle: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + " in Tuek's Sietch", turnSummary.messages.get(3));
    }

    @Nested
    @DisplayName("#DiplomatMustBeResolved")
    class DiplomatMustBeResolved {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(harkonnen);
            game.addFaction(atreides);
            carthag.addForces("Atreides", 5);
            battles = game.startBattlePhase();
            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            assertEquals(1, battles.getBattles(game).size());
            Battle battle = battles.getCurrentBattle();
            duncanIdaho.setSkillCard(new LeaderSkillCard("Diplomat"));
            duncanIdaho.setPulledBehindShield(true);
            battle.setBattlePlan(game, harkonnen, feydRautha, null, false, 0, false, 0, null, null);
            battle.setBattlePlan(game, atreides, duncanIdaho, null, false, 0, false, 0, null, null);
            battle.printBattleResolution(game, false, true);
            assertEquals(10, carthag.getForceStrength("Harkonnen"));
            assertEquals(2, carthag.getForceStrength("Atreides"));
        }

        @Test
        void testDiplomatMustBeResolvedBeforeNextBattle() {
            try {
                battles.nextBattle(game);
                fail("Exception was not thrown");
            } catch (InvalidGameStateException e) {
                assertEquals("Diplomat must be resolved before running the next battle.", e.getMessage());
            }
        }

        @Test
        void testDiplomatMustBeResolvedBeforeEndingBattlePhase() {
            assertThrows(InvalidGameStateException.class, () -> game.endBattlePhase());
        }
    }

    @Nested
    @DisplayName("#callBattleActions")
    class CallBattleActions {
        Battles battles;
        @BeforeEach
        void setUp() {
            game.setStorm(8);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(ix);
            game.addFaction(harkonnen);

            game.createAlliance(bg, emperor);
            game.createAlliance(atreides, fremen);
        }

        @Test
        void testBGGrantsTheVoice() {
            arrakeen.addForces("Emperor", 1);
            battles = game.startBattlePhase();
//            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            battles.callBattleActions(game);
            assertTrue(gameActions.getMessages().getFirst().contains(Emojis.EMPEROR + " use the Voice."));
        }

        @Test
        void testBGDeniesTheVoice() {
            arrakeen.addForces("Emperor", 1);
            bg.setDenyingAllyVoice(true);
            battles = game.startBattlePhase();
//            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            battles.callBattleActions(game);
            assertFalse(gameActions.getMessages().getFirst().contains("Voice"));
        }

        @Test
        void testAtreidesGrantsPrescience() {
            carthag.addForces("Fremen", 1);
            battles = game.startBattlePhase();
//            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            battles.callBattleActions(game);
            assertTrue(gameActions.getMessages().getFirst().contains(Emojis.FREMEN + " ask the Prescience question."));
        }

        @Test
        void testAtreidesDeniesPrescience() {
            carthag.addForces("Fremen", 1);
            atreides.setDenyingAllyBattlePrescience(true);
            battles = game.startBattlePhase();
//            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            battles.callBattleActions(game);
            assertFalse(gameActions.getMessages().getFirst().contains("Prescience"));
        }

        @Test
        void testNoVoiceOrPrescienceForNonAlly() {
            sietchTabr.addForces("Ix", 1);
            sietchTabr.addForces("Harkonnen", 1);
            battles = game.startBattlePhase();
//            battles.nextBattle(game);
            battles.setTerritoryByIndex(0);
            battles.callBattleActions(game);
            assertFalse(gameActions.getMessages().getFirst().contains("Voice"));
            assertFalse(gameActions.getMessages().getFirst().contains("Prescience"));
        }

        @Nested
        @DisplayName("#emperorCunning")
        class EmperorCunning {
            @Test
            void testNoCunningWithWrongNexusCard() {
                emperor.setNexusCard(new NexusCard("Ix"));
                carthag.addForces("Emperor*", 1);
                battles = game.startBattlePhase();
//                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                battles.callBattleActions(game);
                assertTrue(emperorChat.getMessages().isEmpty());
            }

            @Test
            void testNoCunningWithSardaukarInTheBattle() {
                emperor.setNexusCard(new NexusCard("Emperor"));
                carthag.addForces("Emperor*", 1);
                battles = game.startBattlePhase();
//                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                battles.callBattleActions(game);
                assertTrue(emperorChat.getMessages().isEmpty());
            }

            @Test
            void testCunningWithEmperorNexusCard() {
                emperor.setNexusCard(new NexusCard("Emperor"));
                carthag.addForces("Emperor", 1);
                battles = game.startBattlePhase();
//                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                battles.callBattleActions(game);
                assertEquals("Would you like to play the " + Emojis.EMPEROR + " Nexus Card for this battle? em", emperorChat.getMessages().getFirst());
            }

            @Test
            void testNonEmperorNoEmperorCunning() {
                ix.setNexusCard(new NexusCard("Emperor"));
                carthag.addForces("Ix*", 1);
                battles = game.startBattlePhase();
//                battles.nextBattle(game);
                battles.setTerritoryByIndex(0);
                battles.callBattleActions(game);
                assertTrue(ixChat.getMessages().isEmpty());
            }
        }
    }
}
