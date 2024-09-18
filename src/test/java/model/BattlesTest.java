package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BattlesTest {
    Game game;
    Battles battles;
    TestTopic turnSummary;
    AtreidesFaction atreides;
    BGFaction bg;
    EmperorFaction emperor;
    FremenFaction fremen;
    GuildFaction guild;
    HarkonnenFaction harkonnen;
    EcazFaction ecaz;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        game.setWhispers(new TestTopic());
        atreides = new AtreidesFaction("aPlayer", "aUser");
        bg = new BGFaction("bgPlayer", "bgUser");
        emperor = new EmperorFaction("ePlayer", "eUser");
        fremen = new FremenFaction("fPlayer", "fUser");
        guild = new GuildFaction("gPlayer", "gUser");
        harkonnen = new HarkonnenFaction("hPlayer", "hUser");
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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);

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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);

        game.startBattlePhase();
        battles = game.getBattles();
        westCielagoNorth.removeForce("BG");
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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        Territory sietchTabr = game.getTerritory("Sietch Tabr");
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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        Territory sietchTabr = game.getTerritory("Sietch Tabr");
        sietchTabr.addForces("Fremen*", 3);
        sietchTabr.addForces("BG", 1);

        game.startBattlePhase();
        battles = game.getBattles();
        sietchTabr.removeForce("BG");
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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertTrue(battles.aggressorMustChooseOpponent());
    }


    @Test
    void aggressorMustChooseOpponentFalseWithEcazAlly() throws InvalidGameStateException, IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        game.setWhispers(new TestTopic());
        atreides = new AtreidesFaction("aPlayer", "aUser");
        ecaz = new EcazFaction("bgPlayer", "bgUser");
        emperor = new EmperorFaction("ePlayer", "eUser");
        fremen = new FremenFaction("fPlayer", "fUser");
        guild = new GuildFaction("gPlayer", "gUser");
        harkonnen = new HarkonnenFaction("hPlayer", "hUser");
        game.addFaction(atreides);
        game.addFaction(ecaz);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(harkonnen);
        atreides.setAlly("Ecaz");
        ecaz.setAlly("Atreides");

        game.setStorm(10);
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("Ecaz", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Atreides", 4);

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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Atreides", 4);

        game.startBattlePhase();
        battles = game.getBattles();
        eastCielagoNorth.removeForce("Atreides");
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
        Territory westCielagoNorth = game.getTerritory("Cielago North (West Sector)");
        westCielagoNorth.addForces("BG", 6);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Atreides", 4);

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
        Territory northWindPassNorth = game.getTerritory("Wind Pass North (North Sector)");
        northWindPassNorth.addForces("Fremen", 3);
        northWindPassNorth.addForces("Emperor", 2);
        northWindPassNorth.addForces("Emperor*", 1);
        Territory southWindPassNorth = game.getTerritory("Wind Pass North (South Sector)");
        southWindPassNorth.addForces("Atreides", 1);

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
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("BG", 6);
        eastCielagoNorth.addForces("Fremen", 7);
        game.setTurnSummary(turnSummary);
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
        game.setTurnSummary(turnSummary);
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
        Territory tueksSietch = game.getTerritory("Tuek's Sietch");
//        tueksSietch.addForces("Guild", 5);
        tueksSietch.addForces("Harkonnen", 2);
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForces("BG", 2);
//        carthag.addForces("Harkonnen", 10);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("BG", 6);
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Fremen*", 1);
        eastCielagoNorth.addForces("Emperor", 4);
        eastCielagoNorth.addForces("Emperor*", 3);
        game.setTurnSummary(turnSummary);
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
        Territory tueksSietch = game.getTerritory("Tuek's Sietch");
//        tueksSietch.addForces("Guild", 5);
        tueksSietch.addForces("Harkonnen", 2);
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForces("BG", 2);
//        carthag.addForces("Harkonnen", 10);
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForces("BG", 6);
        eastCielagoNorth.addForces("Fremen", 7);
        eastCielagoNorth.addForces("Fremen*", 1);
        eastCielagoNorth.addForces("Emperor", 4);
        eastCielagoNorth.addForces("Emperor*", 3);
        game.setTurnSummary(turnSummary);
        game.startBattlePhase();
        battles = game.getBattles();
        battles.nextBattle(game);

        assertEquals(Emojis.HARKONNEN + " must choose where they will fight:\n" +
                        "Tuek's Sietch: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + "\n" +
                        "Carthag: 10 " + Emojis.HARKONNEN_TROOP + " vs 2 " + Emojis.BG_FIGHTER,
                turnSummary.messages.get(2));

        carthag.removeForce("BG");
        assertEquals(0, carthag.getForceStrength("BG"));

        battles.nextBattle(game);

        assertFalse(battles.aggressorMustChooseBattle());
        assertEquals("Next battle: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + " in Tuek's Sietch", turnSummary.messages.get(3));
    }
}
