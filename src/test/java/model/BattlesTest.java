package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

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
    void testAggregateForces() throws IOException, InvalidGameStateException {
        game = new Game();
        game.setTurnSummary(turnSummary);
        game.setWhispers(new TestTopic());
        ecaz = new EcazFaction("aPlayer", "aUser", game);
        bg = new BGFaction("bgPlayer", "bgUser", game);
        emperor = new EmperorFaction("ePlayer", "eUser", game);
        fremen = new FremenFaction("fPlayer", "fUser", game);
        harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
        RicheseFaction richese = new RicheseFaction("rPlayer", "rUser", game);
        game.addFaction(ecaz);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(harkonnen);
        game.addFaction(richese);
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForces("Emperor", 5);
        carthag.addForces("Emperor*", 2);
        game.startBattlePhase();
        Battles battles = game.getBattles();
        List<Force> battleForces = battles.aggregateForces(List.of(carthag), List.of(emperor, harkonnen));
        assertEquals(carthag.getForceStrength("Emperor*"), battleForces.stream().filter(f -> f.getName().equals("Emperor*")).findFirst().orElseThrow().getStrength());
        assertEquals(carthag.getForceStrength("Emperor"), battleForces.stream().filter(f -> f.getName().equals("Emperor")).findFirst().orElseThrow().getStrength());
        assertEquals(carthag.getForceStrength("Harkonnen"), battleForces.stream().filter(f -> f.getName().equals("Harkonnen")).findFirst().orElseThrow().getStrength());
    }

    @Test
    void aggregateForcesMultipleSectorsPlusNoField() throws IOException, InvalidGameStateException {
        game = new Game();
        game.setTurnSummary(turnSummary);
        game.setWhispers(new TestTopic());
        ecaz = new EcazFaction("aPlayer", "aUser", game);
        bg = new BGFaction("bgPlayer", "bgUser", game);
        emperor = new EmperorFaction("ePlayer", "eUser", game);
        fremen = new FremenFaction("fPlayer", "fUser", game);
        harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
        RicheseFaction richese = new RicheseFaction("rPlayer", "rUser", game);
        game.addFaction(ecaz);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(harkonnen);
        game.addFaction(richese);
        Territory cielagoNorth_eastSector = game.getTerritory("Cielago North (East Sector)");
        Territory cielagoNorth_westSector = game.getTerritory("Cielago North (West Sector)");
        cielagoNorth_eastSector.addForces("Emperor", 2);
        cielagoNorth_eastSector.addForces("Harkonnen", 3);
        cielagoNorth_westSector.addForces("Emperor", 2);
        cielagoNorth_westSector.addForces("Emperor*", 2);
        cielagoNorth_westSector.setRicheseNoField(5);
        List<Territory> cielagoNorthSectors = List.of(
                game.getTerritory("Cielago North (West Sector)"),
                game.getTerritory("Cielago North (Center Sector)"),
                game.getTerritory("Cielago North (East Sector)")
        );
        game.startBattlePhase();
        Battles battles = game.getBattles();
        List<Force> battleForces = battles.aggregateForces(cielagoNorthSectors, List.of(emperor, harkonnen, richese));
        assertEquals(2, battleForces.stream().filter(f -> f.getName().equals("Emperor*")).findFirst().orElseThrow().getStrength());
        assertEquals(4, battleForces.stream().filter(f -> f.getName().equals("Emperor")).findFirst().orElseThrow().getStrength());
        assertEquals(3, battleForces.stream().filter(f -> f.getName().equals("Harkonnen")).findFirst().orElseThrow().getStrength());
        assertEquals(5, battleForces.stream().filter(f -> f.getName().equals("NoField")).findFirst().orElseThrow().getStrength());
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
    void aggressorMustChooseOpponentFalseWithEcazAlly() throws InvalidGameStateException, IOException {
        game = new Game();
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        game.setWhispers(new TestTopic());
        atreides = new AtreidesFaction("aPlayer", "aUser", game);
        ecaz = new EcazFaction("bgPlayer", "bgUser", game);
        emperor = new EmperorFaction("ePlayer", "eUser", game);
        fremen = new FremenFaction("fPlayer", "fUser", game);
        guild = new GuildFaction("gPlayer", "gUser", game);
        harkonnen = new HarkonnenFaction("hPlayer", "hUser", game);
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
        westCielagoNorth.addForce(new Force("Ecaz", 6));
        Territory eastCielagoNorth = game.getTerritory("Cielago North (East Sector)");
        eastCielagoNorth.addForce(new Force("Fremen", 7));
        eastCielagoNorth.addForce(new Force("Atreides", 4));

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
        northWindPassNorth.addForce(new Force("Fremen", 3));
        northWindPassNorth.addForce(new Force("Emperor", 2));
        northWindPassNorth.addForce(new Force("Emperor*", 1));
        Territory southWindPassNorth = game.getTerritory("Wind Pass North (South Sector)");
        southWindPassNorth.addForce(new Force("Atreides", 1));

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
        assertEquals(0, carthag.getForceStrength("BG"));

        battles.nextBattle(game);

        assertFalse(battles.aggressorMustChooseBattle());
        assertEquals("Next battle: 2 " + Emojis.HARKONNEN_TROOP + " vs 5 " + Emojis.GUILD_TROOP + " in Tuek's Sietch", turnSummary.messages.get(3));
    }
}
