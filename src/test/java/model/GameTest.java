package model;

import enums.GameOption;
import model.factions.*;
import model.topics.DuneTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
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
            Faction atreides = new AtreidesFaction("fakePlayer", "userName", game);
            game.addFaction(atreides);
            for (TreacheryCard card : game.getTreacheryDeck()) {
                if (card.name().trim().equals("Family Atomics")) {
                    atreides.addTreacheryCard(card);
                    break;
                }
            }
            assertEquals(game.getFactionWithAtomics().getName(), "Atreides");
        }
    }

    @Nested
    @DisplayName("#breakShieldWall")
    class BreakShieldWall {
        Faction atreides = null;
        TreacheryCard atomics = null;

        @BeforeEach
        void setUpAddSpice() throws IOException {
            atreides = new AtreidesFaction("fakePlayer", "userName", game);
            game.addFaction(atreides);
            for (TreacheryCard card : game.getTreacheryDeck()) {
                if (card.name().trim().equals("Family Atomics")) {
                    atreides.addTreacheryCard(card);
                    break;
                }
            }
            atomics = atreides.getTreacheryCard("Family Atomics ");
        }

        @Test
        void atomicsRemovedFromGame() {
            assertTrue(game.getTerritory("Carthag").isRock());
            assertTrue(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertTrue(game.getTerritory("Arrakeen").isRock());
            assertNotNull(atomics);
            game.breakShieldWall(atreides);
            assertFalse(game.getTerritory("Carthag").isRock());
            assertFalse(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertFalse(game.getTerritory("Arrakeen").isRock());
            assertFalse(atreides.getTreacheryHand().contains(atomics));
            assertFalse(game.getTreacheryDiscard().contains(atomics));
        }

        @Test
        void atomicsMovedToDiscard() {
            game.addGameOption(GameOption.FAMILY_ATOMICS_TO_DISCARD);
            assertTrue(game.getTerritory("Carthag").isRock());
            assertTrue(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertTrue(game.getTerritory("Arrakeen").isRock());
            assertNotNull(atomics);
            game.breakShieldWall(atreides);
            assertFalse(game.getTerritory("Carthag").isRock());
            assertFalse(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertFalse(game.getTerritory("Arrakeen").isRock());
            assertFalse(atreides.getTreacheryHand().contains(atomics));
            assertTrue(game.getTreacheryDiscard().contains(atomics));
        }
    }

    static class TurnSummaryTopic implements DuneTopic {
        public void publish(String message) {}
    }

    @Nested
    @DisplayName("#initialStorm")
    class InitialStorm {
        Faction atreides;
        Faction bg;
        Faction emperor;
        Faction fremen;
        Faction harkonnen;
        Faction guild;
        Faction bt;
        Faction ix;
        TurnSummaryTopic turnSummary;

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
            turnSummary = new TurnSummaryTopic();
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
}