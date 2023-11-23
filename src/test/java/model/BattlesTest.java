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

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        atreides = new AtreidesFaction("aPlayer", "aUser", game);
        bg = new BGFaction("bgPlayer", "bgUser", game);
        emperor = new EmperorFaction("ePlayer", "eUser", game);
        fremen = new FremenFaction("fPlayer", "fUser", game);
        guild = new GuildFaction("gPlayer", "gUser", game);
        harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));

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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));

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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        Territory sietchTabr = game.getTerritory("Sietch Tabr");
        sietchTabr.addForce(new Force("Fremen*", 3));
        sietchTabr.addForce(new Force("BG", 1));

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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        Territory sietchTabr = game.getTerritory("Sietch Tabr");
        sietchTabr.addForce(new Force("Fremen*", 3));
        sietchTabr.addForce(new Force("BG", 1));

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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Atreides", 4));

        game.startBattlePhase();
        battles = game.getBattles();
        assertFalse(battles.noBattlesRemaining(game));
        assertFalse(battles.aggressorMustChooseBattle());
        assertTrue(battles.aggressorMustChooseOpponent());
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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Atreides", 4));

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
        westCielagoNorth.addForce(new Force("BG", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Atreides", 4));

        game.startBattlePhase();
        battles = game.getBattles();
        battles.setTerritoryByIndex(0);
        battles.setOpponent("Fremen");
        Battle currentBattle = battles.getCurrentBattle();
        assertEquals(2, currentBattle.getFactions().size());
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
        northWindPassNorth.addForce(new Force("Fremen", 3));
        northWindPassNorth.addForce(new Force("Emperor", 2));
        northWindPassNorth.addForce(new Force("Emperor*", 1));
        Territory southWindPassNorth = game.getTerritory("Wind Pass North (South Sector)");
        southWindPassNorth.addForce(new Force("Atreides", 1));

        game.startBattlePhase();
        battles = game.getBattles();
        battles.setTerritoryByIndex(0);
        battles.setOpponent("Emperor");
        Battle currentBattle = battles.getCurrentBattle();
        assertEquals(2, currentBattle.getFactions().size());
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
        eastCielagoNorth.addForce(new Force("BG", 6));
        eastCielagoNorth.addForce(new Force("Fremen", 7));
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
//        tueksSietch.addForce(new Force("Guild", 5));
        tueksSietch.addForce(new Force("Harkonnen", 2));
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForce(new Force("BG", 2));
//        carthag.addForce(new Force("Harkonnen", 10));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("BG", 6));
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Fremen*", 1));
        eastCielagoNorth.addForce(new Force("Emperor", 4));
        eastCielagoNorth.addForce(new Force("Emperor*", 3));
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
//        tueksSietch.addForce(new Force("Guild", 5));
        tueksSietch.addForce(new Force("Harkonnen", 2));
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForce(new Force("BG", 2));
//        carthag.addForce(new Force("Harkonnen", 10));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("BG", 6));
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Fremen*", 1));
        eastCielagoNorth.addForce(new Force("Emperor", 4));
        eastCielagoNorth.addForce(new Force("Emperor*", 3));
        game.setTurnSummary(turnSummary);
        game.startBattlePhase();
        battles = game.getBattles();
        battles.nextBattle(game);

        assertEquals(Emojis.HARKONNEN + " must choose where they will fight:\n" +
                        "Tuek's Sietch: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + "\n" +
                        "Carthag: 10 " + Emojis.HARKONNEN_TROOP + " vs 2 " + Emojis.BG_FIGHTER,
                turnSummary.messages.get(2));

        carthag.removeForce("BG");
        assertEquals(0, carthag.getForce("BG").getStrength());

        battles.nextBattle(game);

        assertFalse(battles.aggressorMustChooseBattle());
        assertEquals("Next battle: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + " in Tuek's Sietch", turnSummary.messages.get(3));
    }
}
