package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import model.factions.*;
import model.topics.DuneTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;
    private TreacheryCard familyAtomics;
    private TreacheryCard shield;
    private TreacheryCard weatherControl;
    private Faction atreides;
    private Faction bg;
    private EmperorFaction emperor;
    private Faction fremen;
    private Faction guild;
    private Faction harkonnen;
    private Faction bt;
    private Faction ix;
    private TestTopic turnSummary;
    private TestTopic bgChat;
    private TestTopic emperorChat;
    private TestTopic fremenChat;
    private TestTopic guildChat;


    @BeforeEach
    void setUp() throws IOException {
        game = new Game();

        familyAtomics = game.getTreacheryDeck().stream()
                .filter(t -> t.name().equals("Family Atomics "))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family Atomics not found"));
        weatherControl = game.getTreacheryDeck().stream()
                .filter(t -> t.name().equals("Weather Control "))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Weather Control not found"));

    }

    @Nested
    @DisplayName("#getTerritory")
    class GetTerritory {
        @Test
        void exists() {
            assertEquals(game.getTerritory("Arrakeen").getTerritoryName(), "Arrakeen");
        }

        @Test
        void doesNotExist() {
            assertThrows(IllegalArgumentException.class, () -> game.getTerritory("DoesNotExist"));
        }
    }

    @Nested
    @DisplayName("#getFactionWithAtomics")
    class GetFactionWithAtomics {
        @Test
        void noFactionHoldsAtomics() {
            assertThrows(Exception.class, () -> game.getFactionWithAtomics());
        }

        @Test
        void atreidesHoldsAtomics() throws IOException, NullPointerException {
            atreides = new AtreidesFaction("fakePlayer", "userName", game);
            game.addFaction(atreides);
            atreides.addTreacheryCard(familyAtomics);
            assertEquals(game.getFactionWithAtomics().getName(), "Atreides");
        }
    }

    @Nested
    @DisplayName("#breakShieldWall")
    class BreakShieldWall {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer", "userName", game);
            game.addFaction(atreides);
            atreides.addTreacheryCard(familyAtomics);
        }

        @Test
        void atomicsRemovedFromGame() {
            assertTrue(game.getTerritory("Carthag").isRock());
            assertTrue(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertTrue(game.getTerritory("Arrakeen").isRock());
            assertNotNull(familyAtomics);
            game.breakShieldWall(atreides);
            assertFalse(game.getTerritory("Carthag").isRock());
            assertFalse(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertFalse(game.getTerritory("Arrakeen").isRock());
            assertFalse(atreides.getTreacheryHand().contains(familyAtomics));
            assertFalse(game.getTreacheryDiscard().contains(familyAtomics));
        }

        @Test
        void atomicsMovedToDiscard() {
            game.addGameOption(GameOption.FAMILY_ATOMICS_TO_DISCARD);
            assertTrue(game.getTerritory("Carthag").isRock());
            assertTrue(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertTrue(game.getTerritory("Arrakeen").isRock());
            assertNotNull(familyAtomics);
            game.breakShieldWall(atreides);
            assertFalse(game.getTerritory("Carthag").isRock());
            assertFalse(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertFalse(game.getTerritory("Arrakeen").isRock());
            assertFalse(atreides.getTreacheryHand().contains(familyAtomics));
            assertTrue(game.getTreacheryDiscard().contains(familyAtomics));
        }
    }

    static class TestTopic implements DuneTopic {
        List<String> messages = new ArrayList<>();

        public void publish(String message) {
            messages.add(message);
        }
    }

    @Nested
    @DisplayName("#initialStorm")
    class InitialStorm {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            bt = new BTFaction("fp7", "un7", game);
            ix = new IxFaction("fp8", "un8", game);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            game.addGameOption(GameOption.TECH_TOKENS);
        }

        @Test
        void o6FremenNotInFirstThreePositions() {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);

            game.setInitialStorm(0, 0);
            assertEquals(18, game.getStorm());

            assertEquals(1, atreides.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, atreides.getTechTokens().get(0).getName());
            assertEquals(1, bg.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, bg.getTechTokens().get(0).getName());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().get(0).getName());
            assertTrue(guild.getTechTokens().isEmpty());
            assertTrue(harkonnen.getTechTokens().isEmpty());
        }

        @Test
        void o6FremenInFirstThreePositions() {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);

            game.setInitialStorm(6, 3);
            assertEquals(9, game.getStorm());

            assertTrue(atreides.getTechTokens().isEmpty());
            assertTrue(bg.getTechTokens().isEmpty());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().get(0).getName());
            assertEquals(1, guild.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, guild.getTechTokens().get(0).getName());
            assertEquals(1, harkonnen.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, harkonnen.getTechTokens().get(0).getName());
        }

        @Test
        void expansion1InFirstThreePositions() {
            game.addFaction(bt);
            game.addFaction(ix);
            game.addFaction(fremen);
            game.addFaction(emperor);
            game.addFaction(bg);
            game.addFaction(atreides);

            game.setInitialStorm(8, 10);
            assertEquals(18, game.getStorm());

            assertEquals(1, bt.getTechTokens().size());
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().get(0).getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().get(0).getName());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().get(0).getName());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertTrue(bg.getTechTokens().isEmpty());
            assertTrue(atreides.getTechTokens().isEmpty());
        }

        @Test
        void expansion1NotInFirstThreePositions() {
            game.addFaction(bt);
            game.addFaction(ix);
            game.addFaction(fremen);
            game.addFaction(emperor);
            game.addFaction(bg);
            game.addFaction(atreides);

            game.setInitialStorm(13, 14);
            assertEquals(9, game.getStorm());

            assertEquals(1, bt.getTechTokens().size());
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().get(0).getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().get(0).getName());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().get(0).getName());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertTrue(bg.getTechTokens().isEmpty());
            assertTrue(atreides.getTechTokens().isEmpty());
        }

        @Test
        void expansion1NoFremen() {
            game.addFaction(bt);
            game.addFaction(ix);
            game.addFaction(harkonnen);
            game.addFaction(emperor);
            game.addFaction(bg);
            game.addFaction(atreides);

            game.setInitialStorm(9, 18);
            assertEquals(9, game.getStorm());

            assertEquals(1, bt.getTechTokens().size());
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().get(0).getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().get(0).getName());
            assertTrue(harkonnen.getTechTokens().isEmpty());
            assertEquals(1, emperor.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, emperor.getTechTokens().get(0).getName());
            assertTrue(bg.getTechTokens().isEmpty());
            assertTrue(atreides.getTechTokens().isEmpty());
        }

        @Test
        void techTokensNotInGame() {
            game.removeGameOption(GameOption.TECH_TOKENS);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(bt);
            game.addFaction(ix);
            game.addFaction(fremen);

            game.setInitialStorm(20, 20);
            assertEquals(4, game.getStorm());

            assertTrue(atreides.getTechTokens().isEmpty());
            assertTrue(bg.getTechTokens().isEmpty());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertTrue(bt.getTechTokens().isEmpty());
            assertTrue(ix.getTechTokens().isEmpty());
            assertTrue(fremen.getTechTokens().isEmpty());
        }
    }

    @Nested
    @DisplayName("#stormOrderFactions")
    class StormOrderFactions {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
        }

        @Test
        void dot1IsFirst() {
            List<Faction> expected = Arrays.asList(atreides, bg, emperor, fremen, guild, harkonnen);
            game.setStorm(16);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(17);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(18);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }

        @Test
        void dot2IsFirst() {
            List<Faction> expected = Arrays.asList(bg, emperor, fremen, guild, harkonnen, atreides);
            game.setStorm(1);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(2);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(3);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }

        @Test
        void dot3IsFirst() {
            List<Faction> expected = Arrays.asList(emperor, fremen, guild, harkonnen, atreides, bg);
            game.setStorm(5);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(5);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(5);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }

        @Test
        void dot4IsFirst() {
            List<Faction> expected = Arrays.asList(fremen, guild, harkonnen, atreides, bg, emperor);
            game.setStorm(7);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(8);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(9);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }

        @Test
        void dot5IsFirst() {
            List<Faction> expected = Arrays.asList(guild, harkonnen, atreides, bg, emperor, fremen);
            game.setStorm(10);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(11);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(12);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }

        @Test
        void dot6IsFirst() {
            List<Faction> expected = Arrays.asList(harkonnen, atreides, bg, emperor, fremen, guild);
            game.setStorm(13);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(14);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
            game.setStorm(15);
            assertArrayEquals(expected.toArray(), game.getFactionsInStormOrder().toArray());
        }
    }

    @Nested
    @DisplayName("#ixCanMoveHMS")
    class IxCanMoveHMS {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
        }

        @Test
        void ixNotInGame() throws IOException {
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            game.addFaction(harkonnen);
            assertFalse(game.ixCanMoveHMS());
        }

        @Test
        void ixInGameInHMS() throws IOException {
            ix = new IxFaction("fp6", "un6", game);
            game.addFaction(ix);
            assertTrue(game.ixCanMoveHMS());
        }

        @Test
        void ixInGameNotInHMS() throws IOException {
            ix = new IxFaction("fp6", "un6", game);
            game.addFaction(ix);
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            hms.getForce("Ix*").setStrength(0);
            hms.getForce("Ix").setStrength(0);
            assertFalse(game.ixCanMoveHMS());
        }
    }

    @Nested
    @DisplayName("#getFactionsWithTreacheryCard")
    class GetFactionsWithTreacheryCard {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            shield = game.getTreacheryDeck().stream()
                    .filter(t -> t.name().equals("Shield "))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Shield not found"));
        }

        @Test
        void fremenAndGuldHaveShields() {
            fremen.addTreacheryCard(shield);
            guild.addTreacheryCard(shield);
            List<Faction> factionsWithShield = game.getFactionsWithTreacheryCard("Shield ");
            assertEquals(2, factionsWithShield.size());
            assertTrue(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Fremen")));
            assertTrue(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Guild")));
            assertFalse(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Emperor")));
        }
    }

    @Nested
    @DisplayName("#startStormPhase")
    class StartStormPhase {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            guildChat = new TestTopic();
            guild.setChat(guildChat);
        }

        @Test
        void turn1FremenHasWeatherControl() {
            game.advanceTurn();
            assertEquals(1, game.getTurn());

            game.getTreacheryDeck().remove(weatherControl);
            fremen.addTreacheryCard(weatherControl);

            game.startStormPhase();
            assertEquals(1, turnSummary.messages.size());
            assertEquals("Turn 1 Storm Phase:", turnSummary.messages.get(0));
            assertEquals(0, fremenChat.messages.size());
        }

        @Test
        void turn2NoFactionNearShieldWall() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            game.setStormMovement(1);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 1 sectors this turn. Weather Control may be played at this time.", turnSummary.messages.get(1));
            assertEquals(0, fremenChat.messages.size());
        }

        @Test
        void turn2FremenHasWeatherControl() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            game.getTreacheryDeck().remove(weatherControl);
            fremen.addTreacheryCard(weatherControl);

            game.setStormMovement(1);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 1 sectors this turn. Weather Control may be played at this time.", turnSummary.messages.get(1));
            assertEquals(1, fremenChat.messages.size());
            assertEquals("fp4 will you play Weather Control?", fremenChat.messages.get(0));
        }

        @Test
        void turn2GuildNearShieldWallStormAt3() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Guild", 1);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
        }

        @Test
        void turn2GuildNearShieldWallEmperorHasAtomics() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);
            emperor.addTreacheryCard(familyAtomics);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals(0, guildChat.messages.size());
        }

        @Test
        void turn2GuildNearShieldWallAndHasAtomics() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);
            guild.addTreacheryCard(familyAtomics);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals(1, guildChat.messages.size());
            assertEquals("fp5 will you play Family Atomics?", guildChat.messages.get(0));
        }

        @Test
        void turn2GuildNearShieldWallStormAt8() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Guild", 1);

            game.setStorm(8);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(3, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals("(Check if storm position prevents use of Family Atomics.)", turnSummary.messages.get(2));
        }

        @Test
        void turn2GuildNearShieldWallStormAt8AtomicsRemovedFromGame() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);

            game.setStorm(8);
            game.setStormMovement(2);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Turn 2 Storm Phase:", turnSummary.messages.get(0));
            assertEquals("The storm would move 2 sectors this turn. Weather Control may be played at this time.", turnSummary.messages.get(1));
        }
    }


    @Nested
    @DisplayName("#drawSpiceBlow")
    class DrawSpiceBlow {
        Territory sihayaRidge;
        SpiceCard lastBlow;
        SpiceCard shaiHulud;
        SpiceCard greatMaker;
        SpiceCard sandtrout;

        @BeforeEach
        void setUp() throws IOException {
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            game.addFaction(emperor);
            game.addFaction(fremen);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);

            game.advanceTurn();
            game.advanceTurn();
            sihayaRidge = game.getTerritory("Sihaya Ridge");
            lastBlow = game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow();
            game.getSpiceDiscardA().add(lastBlow);
            assertEquals(lastBlow, game.getSpiceDiscardA().getLast());

            shaiHulud = game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow();
            greatMaker = new SpiceCard("Great Maker", 0, 0, null, null);
            sandtrout = new SpiceCard("Sandtrout", 0, 0, null, null);
        }

        @Test
        void shaiHuludDevoursTroops() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            sihayaRidge.setForceStrength("Emperor", 3);
            sihayaRidge.setForceStrength("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(0, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(0, sihayaRidge.getForce("Emperor").getStrength());
            assertEquals(5, game.getForceFromTanks("Emperor*").getStrength());
            assertEquals(3, game.getForceFromTanks("Emperor").getStrength());
            assertNotEquals(-1, turnSummary.messages.get(0).indexOf(MessageFormat.format("5 {0} devoured by Shai-Hulud", Emojis.EMPEROR_SARDAUKAR)));
            assertNotEquals(-1, turnSummary.messages.get(0).indexOf(MessageFormat.format("3 {0} devoured by Shai-Hulud", Emojis.EMPEROR_TROOP)));
        }

        @Test
        void shaiHuludDoesNotDevourFremen() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            sihayaRidge.setForceStrength("Fremen", 5);
            sihayaRidge.setForceStrength("Fremen*", 3);
            assertEquals(3, sihayaRidge.getForce("Fremen*").getStrength());
            assertEquals(5, sihayaRidge.getForce("Fremen").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(3, sihayaRidge.getForce("Fremen*").getStrength());
            assertEquals(5, sihayaRidge.getForce("Fremen").getStrength());
            assertEquals(0, game.getForceFromTanks("Fremen*").getStrength());
            assertEquals(0, game.getForceFromTanks("Fremen").getStrength());
            assertEquals(-1, turnSummary.messages.get(0).indexOf("devoured"));
        }

        @Test
        void shaiHuludDoesNotDevourFremenAlly() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            emperor.setAlly("Fremen");
            fremen.setAlly("Emperor");
            sihayaRidge.setForceStrength("Emperor", 3);
            sihayaRidge.setForceStrength("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor*").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor").getStrength());
            assertEquals(-1, turnSummary.messages.get(0).indexOf("devoured"));
        }

        @Test
        void greatMakerDevoursTroops() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            sihayaRidge.setForceStrength("Emperor", 3);
            sihayaRidge.setForceStrength("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(0, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(0, sihayaRidge.getForce("Emperor").getStrength());
            assertEquals(5, game.getForceFromTanks("Emperor*").getStrength());
            assertEquals(3, game.getForceFromTanks("Emperor").getStrength());
            assertNotEquals(-1, turnSummary.messages.get(0).indexOf(MessageFormat.format("5 {0} devoured by Great Maker", Emojis.EMPEROR_SARDAUKAR)));
            assertNotEquals(-1, turnSummary.messages.get(0).indexOf(MessageFormat.format("3 {0} devoured by Great Maker", Emojis.EMPEROR_TROOP)));
        }

        @Test
        void greatMakerDoesNotDevourFremen() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            sihayaRidge.setForceStrength("Fremen", 5);
            sihayaRidge.setForceStrength("Fremen*", 3);
            assertEquals(3, sihayaRidge.getForce("Fremen*").getStrength());
            assertEquals(5, sihayaRidge.getForce("Fremen").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(3, sihayaRidge.getForce("Fremen*").getStrength());
            assertEquals(5, sihayaRidge.getForce("Fremen").getStrength());
            assertEquals(0, game.getForceFromTanks("Fremen*").getStrength());
            assertEquals(0, game.getForceFromTanks("Fremen").getStrength());
            assertEquals(-1, turnSummary.messages.get(0).indexOf("devoured"));
        }

        @Test
        void greatMakerDoesNotDevourFremenAlly() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            emperor.setAlly("Fremen");
            fremen.setAlly("Emperor");
            sihayaRidge.setForceStrength("Emperor", 3);
            sihayaRidge.setForceStrength("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor*").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor").getStrength());
            assertEquals(-1, turnSummary.messages.get(0).indexOf("devoured"));
        }

        @Test
        void shaiHuludAfterSandtroutDoesNotDevourTroops() throws ChannelNotFoundException {
            game.getSpiceDeck().addFirst(shaiHulud);
            game.getSpiceDeck().addFirst(sandtrout);
            assertEquals(sandtrout, game.getSpiceDeck().getFirst());

            sihayaRidge.setForceStrength("Emperor", 3);
            sihayaRidge.setForceStrength("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForce("Emperor*").getStrength());
            assertEquals(3, sihayaRidge.getForce("Emperor").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor*").getStrength());
            assertEquals(0, game.getForceFromTanks("Emperor").getStrength());
            assertEquals(-1, turnSummary.messages.get(0).indexOf("devoured"));
        }
    }

    @Nested
    @DisplayName("#rempveForces")
    class RemoveForces {
        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("fakePlayer1", "userName1", game);
            bg = new BGFaction("fakePlayer2", "userName2", game);
            emperor = new EmperorFaction("fp3", "un3", game);
            fremen = new FremenFaction("fp4", "un4", game);
            guild = new GuildFaction("fp5", "un5", game);
            harkonnen = new HarkonnenFaction("fp6", "un6", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            bgChat = new TestTopic();
            bg.setChat(bgChat);
            emperorChat = new TestTopic();
            emperor.setChat(emperorChat);
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
        }

        @Test
        void removeEmperorTroopAndSardaukarToTanks() throws ChannelNotFoundException {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Emperor", 3);
            territory.setForceStrength("Emperor*", 5);
            assertEquals(3, territory.getForce("Emperor").getStrength());
            assertEquals(5, territory.getForce("Emperor*").getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName() + "*").getStrength());

            game.removeForces("Sihaya Ridge", emperor, 1, 1, true);
            assertEquals(2, territory.getForce("Emperor").getStrength());
            assertEquals(4, territory.getForce("Emperor*").getStrength());
            assertEquals(1, game.getForceFromTanks(emperor.getName()).getStrength());
            assertEquals(1, game.getForceFromTanks(emperor.getName() + "*").getStrength());
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, emperorChat.messages.size());
        }

        @Test
        void removeEmperorTroopAndSardaukarToReserves() throws ChannelNotFoundException {
            // Change to placeForces after placeForces moves to Game
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Emperor", 3);
            territory.setForceStrength("Emperor*", 5);
            assertEquals(3, territory.getForce("Emperor").getStrength());
            assertEquals(5, territory.getForce("Emperor*").getStrength());
            // Change to 12 after placeForces is used instead of setForceStrength
            assertEquals(15, emperor.getReserves().getStrength());
            // Change to 0 after placeForces is used instead of setForceStrength
            assertEquals(5, emperor.getSpecialReserves().getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName() + "*").getStrength());

            game.removeForces("Sihaya Ridge", emperor, 1, 1, false);
            assertEquals(2, territory.getForce("Emperor").getStrength());
            assertEquals(4, territory.getForce("Emperor*").getStrength());
            // Change to 13 after placeForces is used instead of setForceStrength
            assertEquals(16, emperor.getReserves().getStrength());
            // Change to 1 after placeForces is used instead of setForceStrength
            // assertEquals(6, emperor.getSpecialReserves().getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName() + "*").getStrength());
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, emperorChat.messages.size());
        }

        @Test
        void removeEmperorTroopAndSardaukarFromHomeworlds() throws ChannelNotFoundException {
            game.addGameOption(GameOption.HOMEWORLDS);
            // Change to placeForces after placeForces moves to Game
            Territory kaitain = game.getTerritory("Kaitain");
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            kaitain.setForceStrength("Emperor", 15);
            salusaSecundus.setForceStrength("Emperor*", 5);
            assertEquals(15, kaitain.getForce("Emperor").getStrength());
            assertEquals(5, salusaSecundus.getForce("Emperor*").getStrength());
            // Change to 12 after placeForces is used instead of setForceStrength
            assertEquals(15, emperor.getReserves().getStrength());
            // Change to 0 after placeForces is used instead of setForceStrength
            assertEquals(5, emperor.getSpecialReserves().getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(emperor.getName() + "*").getStrength());
            assertTrue(emperor.isHighThreshold());
            assertTrue(emperor.isSecundusHighThreshold());

            assertTrue(emperor.isHighThreshold());
            assertEquals(5, emperor.getHighThreshold());
            assertEquals(2, emperor.getSecundusHighThreshold());
            assertEquals(15, kaitain.getForce("Emperor").getStrength());
            assertEquals(5, salusaSecundus.getForce("Emperor*").getStrength());
            assertEquals(0, kaitain.getForce("Emperor*").getStrength());
            game.removeForces("Kaitain", emperor, 0, 0, true);
//            assertEquals(5, kaitain.getForce("Emperor").getStrength());
//            assertEquals(5, emperor.getReserves().getStrength());
//            assertTrue(emperor.isHighThreshold());
            assertEquals(1, turnSummary.messages.size());
            assertEquals("Kaitain has flipped to low threshold.", turnSummary.messages.get(0));

            game.removeForces("Salusa Secundus", emperor, 0, 3, true);
            assertEquals(2, salusaSecundus.getForce("Emperor*").getStrength());
            assertEquals(2, emperor.getSpecialReserves().getStrength());
            assertTrue(emperor.isSecundusHighThreshold());
            assertFalse(emperor.isHighThreshold());
            game.removeForces("Salusa Secundus", emperor, 0, 1, true);
            assertEquals(1, salusaSecundus.getForce("Emperor*").getStrength());
            assertEquals(1, emperor.getSpecialReserves().getStrength());
            assertFalse(emperor.isSecundusHighThreshold());
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Salusa Secundus has flipped to low threshold.", turnSummary.messages.get(1));
        }

        @Test
        void removeFremenTroopAndFedaykinToReserves() throws ChannelNotFoundException {
            // Change to placeForces after placeForces moves to Game
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Fremen", 3);
            territory.setForceStrength("Fremen*", 3);
            assertEquals(3, territory.getForce("Fremen").getStrength());
            assertEquals(3, territory.getForce("Fremen*").getStrength());
            // Change to 14 after placeForces is used instead of setForceStrength
            assertEquals(17, fremen.getReserves().getStrength());
            // Change to 0 after placeForces is used instead of setForceStrength
            assertEquals(3, fremen.getSpecialReserves().getStrength());
            assertEquals(0, game.getForceFromTanks(fremen.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(fremen.getName() + "*").getStrength());

            game.removeForces("Sihaya Ridge", fremen, 1, 1, false);
            assertEquals(2, territory.getForce("Fremen").getStrength());
            assertEquals(2, territory.getForce("Fremen*").getStrength());
            // Change to 13 after placeForces is used instead of setForceStrength
            assertEquals(18, fremen.getReserves().getStrength());
            // Change to 1 after placeForces is used instead of setForceStrength
            assertEquals(4, fremen.getSpecialReserves().getStrength());
            assertEquals(0, game.getForceFromTanks(fremen.getName()).getStrength());
            assertEquals(0, game.getForceFromTanks(fremen.getName() + "*").getStrength());
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, fremenChat.messages.size());
        }

        @Test
        void removeBGAdvisorToTanks() throws ChannelNotFoundException {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Advisor", 1);
            assertEquals(1, territory.getForce("Advisor").getStrength());

            game.removeForces("Sihaya Ridge", bg, 1, 0, true);
            assertEquals(0, territory.getForce("Advisor").getStrength());
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, bgChat.messages.size());
        }

        @Test
        void removeSpecialFailsForBG() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Advisor", 1);
            assertEquals(1, territory.getForce("Advisor").getStrength());

            assertThrows(IllegalArgumentException.class, () -> game.removeForces("Sihaya Ridge", bg, 1, 1, true));
        }

        @Test
        void removeTooManyBG() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.setForceStrength("Advisor", 1);
            assertEquals(1, territory.getForce("Advisor").getStrength());

            assertThrows(IllegalArgumentException.class, () -> game.removeForces("Sihaya Ridge", bg, 2, 0, true));
        }
    }

    @Nested
    @DisplayName("#putTerritoryInAnotherTerritory")
    class PutTerritoryInAnotherTerritory {
        Territory hms;
        String hmsName = "Hidden Mobile Stronghold";
        Territory shieldWallNorth;
        String shieldWallNorthName = "Shield Wall (North Sector)";
        String shieldWallName = shieldWallNorthName.replaceAll("\\(.*\\)", "").strip();

        @BeforeEach
        void setUp() {
            hms = game.getTerritory("Hidden Mobile Stronghold");
            shieldWallNorth = game.getTerritory(shieldWallNorthName);
        }

        @Test
        void putHMSInShieldWallNorth() {
            HashMap<String, List<String>> adjacencyList = game.getAdjacencyList();
            assertNull(adjacencyList.get(hmsName));
            List<String> shieldWallAdjacency = adjacencyList.get(shieldWallName);
            assertEquals(7, shieldWallAdjacency.size());
            game.putTerritoryInAnotherTerritory(hms, shieldWallNorth);
            List<String> hmsAdjacency = adjacencyList.get(hmsName);
            assertEquals(1, hmsAdjacency.size());
            assertTrue(hmsAdjacency.contains(shieldWallName));
            assertEquals(8, shieldWallAdjacency.size());
            assertTrue(shieldWallAdjacency.contains(hmsName));
        }
    }

    @Nested
    @DisplayName("#removeTerritoryFromAnotherTerritory")
    class RemoveTerritoryFromAnotherTerritory {
        Territory hms;
        String hmsName = "Hidden Mobile Stronghold";
        Territory shieldWallNorth;
        String shieldWallNorthName = "Shield Wall (North Sector)";
        String shieldWallName = shieldWallNorthName.replaceAll("\\(.*\\)", "").strip();

        @BeforeEach
        void setUp() {
            hms = game.getTerritory("Hidden Mobile Stronghold");
            shieldWallNorth = game.getTerritory(shieldWallNorthName);
        }

        @Test
        void removeHMSFromShieldWallNorth() {
            HashMap<String, List<String>> adjacencyList = game.getAdjacencyList();
            assertNull(adjacencyList.get(hmsName));
            assertEquals(7, adjacencyList.get(shieldWallName).size());
            game.putTerritoryInAnotherTerritory(hms, shieldWallNorth);
            List<String> hmsAdjacency = adjacencyList.get(hmsName);
            assertEquals(1, hmsAdjacency.size());
            assertTrue(hmsAdjacency.contains(shieldWallName));
            List<String> shieldWallAdjacency = adjacencyList.get(shieldWallName);
            assertEquals(8, shieldWallAdjacency.size());
            assertTrue(shieldWallAdjacency.contains(hmsName));

            game.removeTerritoryFromAnotherTerritory(hms, shieldWallNorth);
            assertEquals(0, adjacencyList.get(hmsName).size());
            assertFalse(hmsAdjacency.contains(shieldWallName));
            assertEquals(7, adjacencyList.get(shieldWallName).size());
            assertFalse(shieldWallAdjacency.contains(hmsName));
        }
    }
}
