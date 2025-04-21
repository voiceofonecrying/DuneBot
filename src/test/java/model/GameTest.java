package model;

import constants.Emojis;
import controller.commands.SetupCommands;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest extends DuneTest {
    @BeforeEach
    public void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @Test
    public void testSpiceDeckWithDiscoveryTokens() throws IOException {
        game.addGameOption(GameOption.DISCOVERY_TOKENS);
        SetupCommands.createDecks(game);
        assertEquals(28, game.getSpiceDeck().size());
    }

    @Test
    public void testReplaceShaiHuludWithGreatMaker() throws IOException {
        game.addGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER);
        SetupCommands.createDecks(game);
        List<SpiceCard> spiceDeck = game.getSpiceDeck();
        assertEquals(21, spiceDeck.size());
        assertEquals(5, spiceDeck.stream().filter(c -> c.name().equals("Shai-Hulud")).count());
        assertEquals(1, spiceDeck.stream().filter(c -> c.name().equals("Great Maker")).count());
    }

    @Test
    public void testDiscoveryTokensAndReplaceShaiHulud() throws IOException {
        game.addGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER);
        game.addGameOption(GameOption.DISCOVERY_TOKENS);
        SetupCommands.createDecks(game);
        List<SpiceCard> spiceDeck = game.getSpiceDeck();
        assertEquals(28, spiceDeck.size());
        assertEquals(5, spiceDeck.stream().filter(c -> c.name().equals("Shai-Hulud")).count());
        assertEquals(2, spiceDeck.stream().filter(c -> c.name().equals("Great Maker")).count());
        assertEquals("The game already has REPLACE_SHAI_HULUD_WITH_MAKER. The " + Emojis.SPICE + " deck will have 5 Shai-Huluds and 2 Great Makers.", modInfo.getMessages().getFirst());
    }

    @Test
    public void testReplaceShaiHuludAndDiscoveryTokens() throws IOException {
        game.addGameOption(GameOption.DISCOVERY_TOKENS);
        game.addGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER);
        SetupCommands.createDecks(game);
        List<SpiceCard> spiceDeck = game.getSpiceDeck();
        assertEquals(28, spiceDeck.size());
        assertEquals(5, spiceDeck.stream().filter(c -> c.name().equals("Shai-Hulud")).count());
        assertEquals(2, spiceDeck.stream().filter(c -> c.name().equals("Great Maker")).count());
        assertEquals("The game already has DISCOVERY_TOKENS. The " + Emojis.SPICE + " deck will have 5 Shai-Huluds and 2 Great Makers.", modInfo.getMessages().getFirst());
    }

    @Nested
    @DisplayName("#choamCharityPhase")
    class ChoamCharity {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(emperor);
            game.addFaction(bg);
            game.addFaction(choam);
            game.addFaction(atreides);
            game.addFaction(fremen);
            game.addFaction(richese);
        }

        @Test
        public void testEmperorHasSpiceDoesNotCollect() {
            assertEquals(10, emperor.getSpice());
            game.choamCharity();
            assertEquals(10, emperor.getSpice());
        }

        @Test
        public void testEmperorLacksSpiceDoesCollect() {
            emperor.subtractSpice(10, "Test");
            game.choamCharity();
            assertEquals(2, emperor.getSpice());
            emperor.subtractSpice(1, "Test");
            game.choamCharity();
            assertEquals(2, emperor.getSpice());
        }

        @Test
        public void testEmperorCollectsAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            emperor.removeReserves(15);
            assertFalse(emperor.isHighThreshold());
            emperor.subtractSpice(10, "Test");
            game.choamCharity();
            assertEquals(3, emperor.getSpice());
            emperor.subtractSpice(2, "Test");
            game.choamCharity();
            assertEquals(3, emperor.getSpice());
            emperor.subtractSpice(1, "Test");
            game.choamCharity();
            assertEquals(2, emperor.getSpice());
        }

        @Test
        public void testEmperorLacksSpiceDeclinesCharity() throws InvalidGameStateException {
            emperor.setDecliningCharity(true);
            emperor.subtractSpice(10, "Test");
            game.choamCharity();
            assertEquals(0, emperor.getSpice());
            emperor.addSpice(1, "Test");
            game.choamCharity();
            assertEquals(1, emperor.getSpice());
        }

        @Test
        public void testBGAlwaysCollectsCharity() {
            assertEquals(5, bg.getSpice());
            game.choamCharity();
            assertEquals(7, bg.getSpice());
        }

        @Test
        public void testBGAlwaysCollectsAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            bg.removeReserves(10);
            assertEquals(5, bg.getSpice());
            assertFalse(bg.isHighThreshold());
            game.choamCharity();
            assertEquals(8, bg.getSpice());
        }

        @Test
        public void testBGDoesNotDeclineCharity() {
            assertThrows(InvalidGameStateException.class, () -> bg.setDecliningCharity(true));
            bg.subtractSpice(5, "Test");
            assertEquals(0, bg.getSpice());
            game.choamCharity();
            assertEquals(2, bg.getSpice());
        }

        @Test
        public void testCHOAMAlwaysCollectsCharity() {
            assertEquals(2, choam.getSpice());
            game.choamCharity();
            assertEquals(12, choam.getSpice());
        }

        @Test
        public void testCHOAMDoesNotGetPlusOneSpiceAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            choam.removeReserves(20);
            assertEquals(2, choam.getSpice());
            assertFalse(choam.isHighThreshold());
            turnSummary.clear();
            game.choamCharity();
            assertEquals(12, choam.getSpice());
            assertEquals(Emojis.CHOAM + " receives 12 " + Emojis.SPICE + " in dividends from their many investments.", turnSummary.getMessages().get(1));
        }

        @Test
        public void testCHOAMDoesNotDeclineCharity() {
            assertThrows(InvalidGameStateException.class, () -> choam.setDecliningCharity(true));
            choam.subtractSpice(2, "Test");
            assertEquals(0, choam.getSpice());
            game.choamCharity();
            assertEquals(10, choam.getSpice());
        }
    }

    @Nested
    @DisplayName("#shippingPhase")
    class ShippingPhase {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(emperor);
            game.addFaction(guild);
            game.addFaction(richese);
            game.addFaction(fremen);
            game.addFaction(atreides);
            game.addFaction(bg);
        }

        @Nested
        @DisplayName("#allFactionsHaveMoved")
        class AllFactionsHaveMoved {
            @BeforeEach
            void setUp() {
                game.startShipmentPhase();
                game.guildWaitLast();
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                bg.placeForceFromReserves(game, sietchTabr, 1, false);
                game.completeCurrentFactionMovement();
            }

            @Test
            void testGuildPassesAllHaveMovedIsTrue() {
                game.completeCurrentFactionMovement();
                assertTrue(game.allFactionsHaveMoved());
            }

            @Test
            void testGuildIntrudesOnBGAndBGFlips() {
                // Really should replace these first three lines with a single function
                guild.placeForceFromReserves(game, sietchTabr, 1, false);
                bg.bgFlipMessageAndButtons(game, sietchTabr.getTerritoryName());
                game.completeCurrentFactionMovement();
                assertFalse(game.allFactionsHaveMoved());
                bg.flipForces(sietchTabr);
                assertTrue(game.allFactionsHaveMoved());
            }

            @Test
            void testGuildIntrudesOnBGAndBGDoesNotFlip() {
                // Really should replace these first three lines with a single function
                guild.placeForceFromReserves(game, sietchTabr, 1, false);
                bg.bgFlipMessageAndButtons(game, sietchTabr.getTerritoryName());
                game.completeCurrentFactionMovement();
                assertFalse(game.allFactionsHaveMoved());
                bg.dontFlipFighters(game, sietchTabr.getTerritoryName());
                assertTrue(game.allFactionsHaveMoved());
            }
        }

        @Nested
        @DisplayName("#guildDefer")
        class GuildDefer {
            @BeforeEach
            void setUp() {
                game.startShipmentPhase();
            }

            @Test
            void testGuildIsToldWhichFactionTheyWouldDeferTo() {
                assertEquals("Defer to Emperor.", guildChat.getChoices().getFirst().get(1).getLabel());
            }

            @Test
            void testDeferOnce() throws InvalidGameStateException {
                game.guildDefer();
                assertFalse(game.getTurnOrder().contains("Guild"));
                assertEquals(1, emperorChat.getMessages().size());
                assertEquals("Use buttons to perform Shipment and Movement actions on your turn. em", emperorChat.getMessages().getFirst());
                assertFalse(emperorChat.getChoices().isEmpty());
            }

            @Test
            void testDeferThenEmperorPasses() throws InvalidGameStateException {
                game.guildDefer();
                game.completeCurrentFactionMovement();
                assertFalse(game.allFactionsHaveMoved());
                assertFalse(game.getTurnOrder().contains("Emperor"));
                assertEquals("Use buttons to take your turn out of order. gu", guildChat.getMessages().getLast());
            }

            @Test
            void testDeferThenGo() throws InvalidGameStateException {
                game.guildDefer();
                game.completeCurrentFactionMovement();
                game.guildTakeTurn();
                assertEquals("Guild", game.getTurnOrder().getFirst());
            }

            @Test
            void testDeferUntilAfterFremen() throws InvalidGameStateException {
                assertEquals(List.of("AskGuild", "Emperor", "Richese", "Fremen", "Atreides", "BG"), game.getTurnOrder());
                game.guildDeferUntilAfter("Fremen");
                guildChat.clear();
                assertEquals(List.of("Emperor", "Richese", "Fremen", "AskGuild", "Atreides", "BG"), game.getTurnOrder());
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                assertTrue(guildChat.getMessages().isEmpty());
                game.completeCurrentFactionMovement();
                assertEquals("Use buttons to take your turn out of order. gu", guildChat.getMessages().getLast());
            }

            @Test
            void testGuildGoesLast() {
                game.guildWaitLast();
                assertNotEquals("Guild", game.getTurnOrder().getFirst());
                assertEquals("Guild", game.getTurnOrder().getLast());
            }
        }

        @Nested
        @DisplayName("#juiceOfSapho")
        class JuiceOfSapho {
            @BeforeEach
            void setUp() {
                atreides.addTreacheryCard(new TreacheryCard("Juice of Sapho"));
                game.startShipmentPhase();
            }

            @Test
            void testPlayJuiceToGoFirst() throws InvalidGameStateException {
                assertTrue(guildChat.getMessages().isEmpty());
                assertTrue(atreidesChat.getMessages().getFirst().contains("Will you play Juice of Sapho to ship and move first? at"));
                game.playJuiceOfSapho(atreides, false);
                assertEquals("Atreides", game.getTurnOrder().getFirst());
            }

            @Test
            void testDontPlayJuiceToGoFirst() throws InvalidGameStateException {
                assertTrue(guildChat.getMessages().isEmpty());
                assertTrue(atreidesChat.getMessages().getFirst().contains("Will you play Juice of Sapho to ship and move first? at"));
                game.juiceOfSaphoDontPlay(atreides);
                assertEquals("AskGuild", game.getTurnOrder().getFirst());
            }

            @Test
            void testPlayJuiceToGoLast() throws InvalidGameStateException {
                game.juiceOfSaphoDontPlay(atreides);
                game.guildWaitLast();
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                game.completeCurrentFactionMovement();
                assertEquals("Atreides", game.getTurnOrder().getFirst());
                assertEquals("Play Juice of Sapho to go last", atreidesChat.getChoices().getLast().getLast().getLabel());
                assertFalse(atreidesChat.getChoices().getLast().getLast().isDisabled());
                game.playJuiceOfSapho(atreides, true);
                assertEquals("juice-of-sapho-last", game.getTurnOrder().getLast());
                assertNotEquals("Atreides", game.getTurnOrder().getFirst());
                assertTrue(game.getTurnOrder().contains("Atreides"));
                assertEquals("BG", game.getTurnOrder().getFirst());
            }
        }

        @Test
        void testFremenAlliedWithGuildShipFree() {
            game.createAlliance(guild, fremen);
            assertEquals(0, game.shipmentCost(fremen, 2, habbanyaSietch, false, false));
        }

        @Test
        void testFremenAlliedWithGuildPayForCrossShipOrShipToReserves() {
            game.createAlliance(guild, fremen);
            assertEquals(1, game.shipmentCost(fremen, 2, habbanyaSietch, false, true));
        }

        @Test
        void testFremenShipToOtherHomeworld() {
            game.addGameOption(GameOption.HOMEWORLDS);
            String junction = game.getHomeworlds().get("Guild");
            assertEquals("Junction", junction);
            assertEquals(2, game.shipmentCost(fremen, 2, game.getTerritory(junction), false, false));
        }

        @Test
        void testNormalPaymentToGuild() throws InvalidGameStateException {
            assertEquals(" for 10 " + Emojis.SPICE + " paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 10, habbanyaSietch, false, false));
            assertEquals(0, emperor.getSpice());
            assertEquals(15, guild.getSpice());
        }

        @Test
        void testNormalPaymentToGuildAsAlly() throws InvalidGameStateException {
            game.createAlliance(guild, emperor);
            assertEquals(" for 5 " + Emojis.SPICE + " paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 5, habbanyaSietch, false, false));
            assertEquals(5, emperor.getSpice());
            assertEquals(10, guild.getSpice());
        }

        @Test
        void testPaymentToGuildAsAllyWithGuildSupport() throws InvalidGameStateException {
            game.createAlliance(guild, emperor);
            guild.setSpiceForAlly(3);
            guild.setAllySpiceForShipping(true);
            assertEquals(" for 5 " + Emojis.SPICE + " (3 from " + Emojis.GUILD + "), 2 " + Emojis.SPICE + " paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 5, habbanyaSietch, false, false));
            assertEquals(8, emperor.getSpice());
            assertEquals(4, guild.getSpice());
        }

        @Test
        void testPaymentToGuildAsAllyGuildSupportNotForShipping() throws InvalidGameStateException {
            game.createAlliance(guild, emperor);
            guild.setSpiceForAlly(3);
            assertEquals(" for 5 " + Emojis.SPICE + " paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 5, habbanyaSietch, false, false));
            assertEquals(5, emperor.getSpice());
            assertEquals(10, guild.getSpice());
        }

        @Test
        void testPaymentToLowThresholdGuild() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            guild.removeReserves(11);
            assertFalse(guild.isHighThreshold());
            assertEquals(" for 9 " + Emojis.SPICE + ", 5 " + Emojis.SPICE + " paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 9, habbanyaSietch, false, false));
            assertEquals(1, emperor.getSpice());
            assertEquals(10, guild.getSpice());
        }

        @Test
        void testPaymentToGuildJunctionOccupied() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            guild.removeReserves(15);
            emperor.placeForces(game.getTerritory("Junction"), 1, 0, false, false, false, game, false, false);
            assertTrue(guild.isHomeworldOccupied());
            assertEquals(" for 9 " + Emojis.SPICE + ", 5 " + Emojis.SPICE + " paid to " + Emojis.GUILD + ", 4 " + Emojis.SPICE + " paid to " + Emojis.EMPEROR,
                    emperor.payForShipment(game, 9, habbanyaSietch, false, false));
            assertEquals(5, emperor.getSpice());
            assertEquals(10, guild.getSpice());
        }

        @Test
        void testPaymentToFremenAsAlly() throws InvalidGameStateException {
            game.createAlliance(fremen, emperor);
            fremen.setSpiceForAlly(3);
            assertEquals(" for 5 " + Emojis.SPICE + " (3 from " + Emojis.FREMEN + ") paid to " + Emojis.GUILD,
                    emperor.payForShipment(game, 5, habbanyaSietch, false, false));
            assertEquals(8, emperor.getSpice());
            assertEquals(0, fremen.getSpice());
            assertEquals(10, guild.getSpice());
        }

        @Test
        void testAllyShippedAndCanNoLongerSupport() throws InvalidGameStateException {
            game.createAlliance(atreides, emperor);
            atreides.setSpiceForAlly(3);
            Territory carthag = game.getTerritory("Carthag");
            atreides.payForShipment(game, 10, carthag, false, false);
            assertEquals(0, atreides.getSpice());
            assertEquals(0, atreides.getSpiceForAlly());
            assertThrows(InvalidGameStateException.class, () -> emperor.payForShipment(game, 11, habbanyaSietch, false, false));
        }

        @Test
        void testAllyBribedAndCanNoLongerSupport() throws InvalidGameStateException {
            game.createAlliance(atreides, emperor);
            atreides.setSpiceForAlly(3);
            atreides.bribe(game, guild, 10, "For test");
            assertEquals(0, atreides.getSpice());
            assertEquals(0, atreides.getSpiceForAlly());
            assertThrows(InvalidGameStateException.class, () -> emperor.payForShipment(game, 11, habbanyaSietch, false, false));
        }

        @Test
        void testKaramaPayment() throws InvalidGameStateException {
            assertEquals(" for 5 " + Emojis.SPICE,
                    emperor.payForShipment(game, 5, habbanyaSietch, true, false));
            assertEquals(5, emperor.getSpice());
            assertEquals(5, guild.getSpice());
        }

        @Test
        void testGuildPaysSpiceBank() throws InvalidGameStateException {
            assertEquals(" for 5 " + Emojis.SPICE,
                    guild.payForShipment(game, 5, habbanyaSietch, false, false));
            assertEquals(0, guild.getSpice());
        }
    }

    @Nested
    @DisplayName("#planetologistIsSingleMovement")
    class PlanetologistIsSingleMovement {
        @BeforeEach
        void setUp() throws IOException, InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(moritani);
            game.removeForces("Grumman", moritani, 13, 0, true);
            assertFalse(moritani.isHighThreshold());
            game.getTerritory("Carthag").addTerrorToken(game, "Robbery");

            game.addFaction(ecaz);
            game.getTerritory("Arrakeen").addForces("Ecaz", 2);
            game.getTerritory("Polar Sink").addForces("Ecaz", 2);
            Movement ecazMovement = ecaz.getMovement();
            ecazMovement.setMovingTo("Carthag");
            ecazMovement.setMovingFrom("Arrakeen");
            ecazMovement.setForce(1);
            ecazMovement.setSecondMovingFrom("Polar Sink");
            ecazMovement.setSecondForce(2);

            game.addFaction(richese);
            game.getTerritory("Arrakeen").setRicheseNoField(5);
            game.getTerritory("Polar Sink").addForces("Richese", 2);
            Movement richeseMovement = richese.getMovement();
            richeseMovement.setMovingTo("Carthag");
            richeseMovement.setMovingFrom("Arrakeen");
            richeseMovement.setMovingNoField(true);
            richeseMovement.setSecondMovingFrom("Polar Sink");
            richeseMovement.setSecondForce(2);
        }

        @Test
        void testPlanetologistIsSingleMoveForTerrorTokens() {
            ecaz.executeMovement(game);
            assertEquals("Will you trigger your Robbery Terror Token in Carthag against " + Emojis.ECAZ + "? mo", moritaniChat.getMessages().getFirst());
        }

        @Test
        void testPlanetologistWithNoFieldIsSingleMoveForTerrorTokens() {
            richese.executeMovement(game);
            assertEquals("Will you trigger your Robbery Terror Token in Carthag against " + Emojis.RICHESE + "? mo", moritaniChat.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#battlePhase")
    class BattlePhase {
        Territory garaKulon;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(emperor);
            game.addFaction(ecaz);
            game.createAlliance(ecaz, emperor);
            game.addFaction(harkonnen);
            garaKulon = game.getTerritory("Gara Kulon");
            garaKulon.addForces("Harkonnen", 10);
            garaKulon.addForces("Emperor", 5);
            garaKulon.addForces("Ecaz", 3);
        }

        @Test
        void testEndBattlePhase() {
            game.startBattlePhase();
            assertThrows(InvalidGameStateException.class, () -> game.endBattlePhase());
            garaKulon.removeForces(game, "Harkonnen", 10);
            assertDoesNotThrow(() -> game.endBattlePhase());
        }
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
    @DisplayName("#destroyShieldWall")
    class DestroyShieldWall {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(ecaz);
            ecaz.addTreacheryCard(familyAtomics);
            assertTrue(game.getTerritory("Carthag").isRock());
            assertTrue(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertTrue(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertTrue(game.getTerritory("Arrakeen").isRock());
        }

        @Test
        void testTerritoriesNoLongerRock() throws InvalidGameStateException {
            game.destroyShieldWall();
            assertFalse(game.getTerritory("Carthag").isRock());
            assertFalse(game.getTerritory("Imperial Basin (Center Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (East Sector)").isRock());
            assertFalse(game.getTerritory("Imperial Basin (West Sector)").isRock());
            assertFalse(game.getTerritory("Arrakeen").isRock());
        }

        @Test
        void testAtomicsRemovedFromGame() throws InvalidGameStateException {
            game.destroyShieldWall();
            assertFalse(ecaz.getTreacheryHand().contains(familyAtomics));
            assertFalse(game.getTreacheryDiscard().contains(familyAtomics));
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.contains(Emojis.ECAZ + " plays Family Atomics.")));
            assertEquals("The Shield Wall has been destroyed!\nFamily Atomics has been removed from the game.", turnSummary.getMessages().get(1));
        }

        @Test
        void testAtomicsMovedToDiscard() throws InvalidGameStateException {
            game.addGameOption(GameOption.FAMILY_ATOMICS_TO_DISCARD);
            game.destroyShieldWall();
            assertFalse(ecaz.getTreacheryHand().contains(familyAtomics));
            assertTrue(game.getTreacheryDiscard().contains(familyAtomics));
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.contains(Emojis.ECAZ + " discards Family Atomics.")));
            assertEquals("The Shield Wall has been destroyed!\nFamily Atomics is in the discard pile.", turnSummary.getMessages().get(1));
        }

        @Test
        void testForcesOnShieldWallKilled() throws InvalidGameStateException {
            game.addFaction(atreides);
            Territory shieldWallWest = game.getTerritory("Shield Wall (North Sector)");
            shieldWallWest.addForces("Atreides", 1);
            game.destroyShieldWall();
            assertEquals(1, game.getTleilaxuTanks().getForce("Atreides").getStrength());
        }

        @Test
        void testNoFactionHasAtomics() {
            ecaz.removeTreacheryCardWithoutDiscard(familyAtomics);
            assertThrows(InvalidGameStateException.class, () -> game.destroyShieldWall());
        }

        @Test
        void testNoFactionInPositionToUseAtomics() {
            game.getTerritory("Imperial Basin (Center Sector)").removeForces(game, "Ecaz", 6);
            assertThrows(InvalidGameStateException.class, () -> game.destroyShieldWall());
        }
    }

    @Nested
    @DisplayName("#initialStorm")
    class InitialStorm {
        @BeforeEach
        void setUp() throws IOException {
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
            assertNotEquals(TechToken.SPICE_PRODUCTION, atreides.getTechTokens().getFirst().getName());
            assertEquals(1, bg.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, bg.getTechTokens().getFirst().getName());
            assertTrue(emperor.getTechTokens().isEmpty());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().getFirst().getName());
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
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().getFirst().getName());
            assertEquals(1, guild.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, guild.getTechTokens().getFirst().getName());
            assertEquals(1, harkonnen.getTechTokens().size());
            assertNotEquals(TechToken.SPICE_PRODUCTION, harkonnen.getTechTokens().getFirst().getName());
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
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().getFirst().getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().getFirst().getName());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().getFirst().getName());
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
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().getFirst().getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().getFirst().getName());
            assertEquals(1, fremen.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, fremen.getTechTokens().getFirst().getName());
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
            assertEquals(TechToken.AXLOTL_TANKS, bt.getTechTokens().getFirst().getName());
            assertEquals(1, ix.getTechTokens().size());
            assertEquals(TechToken.HEIGHLINERS, ix.getTechTokens().getFirst().getName());
            assertTrue(harkonnen.getTechTokens().isEmpty());
            assertEquals(1, emperor.getTechTokens().size());
            assertEquals(TechToken.SPICE_PRODUCTION, emperor.getTechTokens().getFirst().getName());
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
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
        }

        @Test
        void ixNotInGame() {
            game.addFaction(harkonnen);
            assertFalse(game.ixCanMoveHMS());
        }

        @Test
        void ixInGameInHMS() {
            game.addFaction(ix);
            assertTrue(game.ixCanMoveHMS());
        }

        @Test
        void ixInGameNotInHMS() {
            game.addFaction(ix);
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            hms.removeForces(game, "Ix", 3);
            hms.removeForces(game, "Ix*", 3);
            assertEquals(0, hms.getTotalForceCount(ix));
            assertFalse(game.ixCanMoveHMS());
        }
    }

    @Nested
    @DisplayName("#getFactionsWithTreacheryCard")
    class GetFactionsWithTreacheryCard {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
        }

        @Test
        void fremenAndGuldHaveShields() {
            fremen.addTreacheryCard(shield);
            guild.addTreacheryCard(shield);
            List<Faction> factionsWithShield = game.getFactionsWithTreacheryCard("Shield");
            assertEquals(2, factionsWithShield.size());
            assertTrue(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Fremen")));
            assertTrue(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Guild")));
            assertFalse(factionsWithShield.stream().anyMatch(f -> f.getName().equals("Emperor")));
        }
    }

    @Nested
    @DisplayName("#startNewTurn")
    class StartNewTurn {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
        }

        @Test
        void testNewTurnResetsOccupation() {
            game.addFaction(guild);
            HomeworldTerritory junction = (HomeworldTerritory) game.getTerritory(guild.getHomeworld());
            junction.addForces("Fremen*", 1);
            junction.removeForces(game, "Guild", 15);
            assertEquals("Fremen", junction.getOccupierName());
            assertEquals("Junction is now occupied by " + Emojis.FREMEN, turnSummary.getMessages().getFirst());
            junction.removeForces(game, "Fremen*", 1);
            assertEquals("Fremen", junction.getOccupierName());
            game.advanceTurn();
            assertNull(junction.getOccupierName());
            assertEquals("Junction is no longer occupied.", turnSummary.getMessages().get(1));
        }

        @Test
        void testNewTurnResetsSalusaSecundusOccupation() {
            game.addFaction(emperor);
            HomeworldTerritory salusaSecudus = (HomeworldTerritory) game.getTerritory(emperor.getSecondHomeworld());
            salusaSecudus.addForces("Fremen*", 1);
            salusaSecudus.removeForces(game, "Emperor*", 5);
            assertEquals("Fremen", salusaSecudus.getOccupierName());
            assertEquals("Salusa Secundus is now occupied by " + Emojis.FREMEN, turnSummary.getMessages().getFirst());
            salusaSecudus.removeForces(game, "Fremen*", 1);
            assertEquals("Fremen", salusaSecudus.getOccupierName());
            game.advanceTurn();
            assertNull(salusaSecudus.getOccupierName());
            assertEquals("Salusa Secundus is no longer occupied.", turnSummary.getMessages().get(1));
        }
    }

    @Nested
    @DisplayName("#startStormPhase")
    class StartStormPhase {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
        }

        @Test
        void turn1FremenHasWeatherControl() {
            game.advanceTurn();
            assertEquals(1, game.getTurn());

            game.getTreacheryDeck().remove(weatherControl);
            fremen.addTreacheryCard(weatherControl);

            game.startStormPhase();
            assertEquals(1, turnSummary.messages.size());
            assertEquals("**Turn 1 Storm Phase**", turnSummary.messages.getFirst());
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
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
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
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 1 sectors this turn. Weather Control may be played at this time.", turnSummary.messages.get(1));
            assertEquals(1, fremenChat.messages.size());
            assertEquals("fr will you play Weather Control?", fremenChat.messages.getFirst());
        }

        @Test
        void turn2GuildNearShieldWallStormAt3() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Guild", 1);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
        }

        @Test
        void turn2GuildNearShieldWallEmperorHasAtomics() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);
            emperor.addTreacheryCard(familyAtomics);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals(0, guildChat.messages.size());
        }

        @Test
        void turn2GuildNearShieldWallAndHasAtomics() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);
            guild.addTreacheryCard(familyAtomics);

            game.setStorm(3);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals(1, guildChat.messages.size());
            assertEquals("gu will you play Family Atomics?", guildChat.messages.getFirst());
        }

        @Test
        void turn2GuildNearShieldWallStormAt8() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Guild", 1);

            game.setStorm(8);
            game.setStormMovement(5);
            game.startStormPhase();
            assertEquals(3, turnSummary.messages.size());
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 5 sectors this turn. Weather Control and Family Atomics may be played at this time.", turnSummary.messages.get(1));
            assertEquals("(Check if storm position prevents use of Family Atomics.)", turnSummary.messages.get(2));
        }

        @Test
        void turn2GuildNearShieldWallStormAt8AtomicsRemovedFromGame() {
            game.advanceTurn();
            game.advanceTurn();
            assertEquals(2, game.getTurn());

            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Guild", 1);
            game.getTreacheryDeck().remove(familyAtomics);

            game.setStorm(8);
            game.setStormMovement(2);
            game.startStormPhase();
            assertEquals(2, turnSummary.messages.size());
            assertEquals("**Turn 2 Storm Phase**", turnSummary.messages.get(0));
            assertEquals("The storm would move 2 sectors this turn. Weather Control may be played at this time.", turnSummary.messages.get(1));
        }

        @Test
        void testFactionMayMoveIntoDiscoveryToken() {
            game.setStorm(9);
            Territory meridianWest = game.getTerritory("Meridian (West Sector)");
            meridianWest.setDiscoveryToken("Jacurutu Sietch");
            Territory jacurutuSietch = game.getTerritories().addDiscoveryToken("Jacurutu Sietch", true);
            game.putTerritoryInAnotherTerritory(jacurutuSietch, meridianWest);
            meridianWest.setDiscovered(true);
            meridianWest.addForces("BG", 4);

            game.startStormPhase();
            assertEquals(Emojis.BG + " may move into Jacurutu Sietch from Meridian (West Sector).", turnSummary.getMessages().get(1));
            assertEquals("Would you like to move into Jacurutu Sietch from Meridian (West Sector)? bg", bgChat.getMessages().getFirst());
            assertEquals(2, bgChat.getChoices().getFirst().size());
        }

        @Test
        void testFactionMayMoveIntoDiscoveryTokenFromNeighboringSector() {
            game.setStorm(9);
            Territory meridianWest = game.getTerritory("Meridian (West Sector)");
            meridianWest.setDiscoveryToken("Jacurutu Sietch");
            Territory jacurutuSietch = game.getTerritories().addDiscoveryToken("Jacurutu Sietch", true);
            game.putTerritoryInAnotherTerritory(jacurutuSietch, meridianWest);
            meridianWest.setDiscovered(true);
            Territory meridianEast = game.getTerritory("Meridian (East Sector)");
            meridianEast.addForces("BG", 4);

            game.startStormPhase();
            assertEquals(Emojis.BG + " may move into Jacurutu Sietch from Meridian (East Sector).", turnSummary.getMessages().get(1));
            assertEquals("Would you like to move into Jacurutu Sietch from Meridian (East Sector)? bg", bgChat.getMessages().getFirst());
            assertEquals(2, bgChat.getChoices().getFirst().size());
        }
    }

    @Nested
    @DisplayName("#endStormPhase")
    class EndStormPhase {
        @BeforeEach
        void setUp() {
            game.setStorm(16);
            game.setStormMovement(4);
        }

        @Test
        void testStormTurn1() {
            game.setTurn(1);
            game.setStorm(0);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            game.setStormDial(atreides, 3);
            game.setStormDial(harkonnen, 5);
            assertEquals("The storm has been initialized to sector 8: " + Emojis.HARKONNEN + " 5 + " + Emojis.ATREIDES + " 3.", turnSummary.getMessages().getLast());
        }

        @Test
        void testStormTurn2() throws IOException {
            game.setTurn(2);
            game.endStormPhase();
            assertEquals(2, game.getStorm());
            assertEquals("The storm moves 4 sectors this turn.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testFremenLoseHalfForces() throws IOException {
            game.addFaction(fremen);
            fremen.placeForceFromReserves(game, cielagoNorth_eastSector, 2, false);
            fremen.placeForceFromReserves(game, cielagoNorth_eastSector, 3, true);
            game.endStormPhase();
            assertEquals(Emojis.FREMEN + " lose 2 " + Emojis.FREMEN_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().get(1));
            assertEquals(Emojis.FREMEN + " lose 1 " + Emojis.FREMEN_FEDAYKIN + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testOtherFactionLosesAllForces() throws IOException {
            game.addFaction(emperor);
            emperor.placeForceFromReserves(game, cielagoNorth_eastSector, 2, false);
            emperor.placeForceFromReserves(game, cielagoNorth_eastSector, 3, true);
            game.endStormPhase();
            assertEquals(Emojis.EMPEROR + " lose 2 " + Emojis.EMPEROR_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().get(1));
            assertEquals(Emojis.EMPEROR + " lose 3 " + Emojis.EMPEROR_SARDAUKAR + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testRicheseNoFieldRevealedAndForcesLost() throws IOException {
            game.addFaction(richese);
            cielagoNorth_eastSector.setRicheseNoField(5);
            game.endStormPhase();
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Cielago North (East Sector) reveals 5 " + Emojis.RICHESE_TROOP, turnSummary.getMessages().get(1));
            assertEquals(Emojis.RICHESE + " lose 5 " + Emojis.RICHESE_TROOP + " to the storm in Cielago North (East Sector).", turnSummary.getMessages().getLast());
        }

        @Test
        void testSpiceBlownAway() throws IOException {
            cielagoNorth_eastSector.setSpice(6);
            game.endStormPhase();
            assertEquals("6 " + Emojis.SPICE + " in Cielago North (East Sector) was blown away by the storm.", turnSummary.getMessages().getLast());
        }

        @Test
        void testAmbassadorsReturnedToSupply() throws IOException {
            game.addFaction(ecaz);
            cielagoNorth_eastSector.setEcazAmbassador("BG");
            game.endStormPhase();
            assertEquals(Emojis.ECAZ + " BG Ambassador was removed from Cielago North (East Sector) and returned to supply.", turnSummary.getMessages().getLast());
        }

        @Test
        void testTerrorTokensRemainOnMap() throws IOException, InvalidGameStateException {
            cielagoNorth_eastSector.addTerrorToken(game, "Sabotage");
            game.endStormPhase();
            assertTrue(cielagoNorth_eastSector.hasTerrorToken("Sabotage"));
        }

        @Test
        void testDiscoveryTokensRemainOnMap() throws IOException {
            meridian_westSector.setDiscoveryToken("Cistern");
            meridian_westSector.setDiscovered(false);
            game.endStormPhase();
            assertEquals("Cistern", meridian_westSector.getDiscoveryToken());
        }
    }

    @Nested
    @DisplayName("#drawSpiceBlow")
    class DrawSpiceBlow {
        Territory sihayaRidge;
        Territory funeralPlain;
        SpiceCard lastBlow;
        SpiceCard nextBlow;
        SpiceCard shaiHulud;
        SpiceCard greatMaker;
        SpiceCard sandtrout;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(emperor);
            game.addFaction(fremen);

            game.advanceTurn();
            game.advanceTurn();
            sihayaRidge = game.getTerritory("Sihaya Ridge");
            funeralPlain = game.getTerritory("Funeral Plain");
            lastBlow = game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow();
            nextBlow = new SpiceCard("Funeral Plain",14,6,"Smuggler","Pasty Mesa (North Sector)");
            game.getSpiceDiscardA().add(lastBlow);
            assertEquals(lastBlow, game.getSpiceDiscardA().getLast());

            shaiHulud = game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow();
            greatMaker = new SpiceCard("Great Maker", 0, 0, null, null);
            sandtrout = new SpiceCard("Sandtrout", 0, 0, null, null);
        }

        @Test
        void testShaiHuludDestroysSpice() {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());
            sihayaRidge.setSpice(6);
            game.drawSpiceBlow("A");
            assertEquals(0, sihayaRidge.getSpice());
//            assertNull(turnSummary.getMessages().getFirst());
            assertTrue(turnSummary.getMessages().getFirst().contains("Shai-Hulud has been spotted in Sihaya Ridge!"));
            assertTrue(turnSummary.getMessages().getFirst().contains("6 " + Emojis.SPICE + " is eaten by the worm!"));
        }

        @Test
        void shaiHuludDevoursTroops() {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            sihayaRidge.addForces("Emperor", 3);
            sihayaRidge.addForces("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(0, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(0, sihayaRidge.getForceStrength("Emperor"));
            assertEquals(5, tanks.getForceStrength("Emperor*"));
            assertEquals(3, tanks.getForceStrength("Emperor"));
            assertNotEquals(-1, turnSummary.messages.getFirst().indexOf(MessageFormat.format("5 {0} devoured by Shai-Hulud", Emojis.EMPEROR_SARDAUKAR)));
            assertNotEquals(-1, turnSummary.messages.getFirst().indexOf(MessageFormat.format("3 {0} devoured by Shai-Hulud", Emojis.EMPEROR_TROOP)));
        }

        @Test
        void shaiHuludDoesNotDevourFremen() {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            sihayaRidge.addForces("Fremen", 5);
            sihayaRidge.addForces("Fremen*", 3);
            assertEquals(3, sihayaRidge.getForceStrength("Fremen*"));
            assertEquals(5, sihayaRidge.getForceStrength("Fremen"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(3, sihayaRidge.getForceStrength("Fremen*"));
            assertEquals(5, sihayaRidge.getForceStrength("Fremen"));
            assertEquals(0, tanks.getForceStrength("Fremen*"));
            assertEquals(0, tanks.getForceStrength("Fremen"));
            assertEquals(-1, turnSummary.messages.getFirst().indexOf("devoured"));
        }

        @Test
        void shaiHuludDoesNotDevourFremenAlly() {
            game.getSpiceDeck().addFirst(shaiHulud);
            assertEquals(shaiHulud, game.getSpiceDeck().getFirst());

            game.createAlliance(fremen, emperor);
            sihayaRidge.addForces("Emperor", 3);
            sihayaRidge.addForces("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));
            assertEquals(0, tanks.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength("Emperor"));
            assertEquals(-1, turnSummary.messages.getFirst().indexOf("devoured"));
        }

        @Test
        void greatMakerDevoursTroops() {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            sihayaRidge.addForces("Emperor", 3);
            sihayaRidge.addForces("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(0, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(0, sihayaRidge.getForceStrength("Emperor"));
            assertEquals(5, tanks.getForceStrength("Emperor*"));
            assertEquals(3, tanks.getForceStrength("Emperor"));
            assertNotEquals(-1, turnSummary.messages.getFirst().indexOf(MessageFormat.format("5 {0} devoured by Great Maker", Emojis.EMPEROR_SARDAUKAR)));
            assertNotEquals(-1, turnSummary.messages.getFirst().indexOf(MessageFormat.format("3 {0} devoured by Great Maker", Emojis.EMPEROR_TROOP)));
        }

        @Test
        void greatMakerDoesNotDevourFremen() {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            sihayaRidge.addForces("Fremen", 5);
            sihayaRidge.addForces("Fremen*", 3);
            assertEquals(3, sihayaRidge.getForceStrength("Fremen*"));
            assertEquals(5, sihayaRidge.getForceStrength("Fremen"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(3, sihayaRidge.getForceStrength("Fremen*"));
            assertEquals(5, sihayaRidge.getForceStrength("Fremen"));
            assertEquals(0, tanks.getForceStrength("Fremen*"));
            assertEquals(0, tanks.getForceStrength("Fremen"));
            assertEquals(-1, turnSummary.messages.getFirst().indexOf("devoured"));
        }

        @Test
        void greatMakerDoesNotDevourFremenAlly() {
            game.getSpiceDeck().addFirst(greatMaker);
            assertEquals(greatMaker, game.getSpiceDeck().getFirst());

            game.createAlliance(fremen, emperor);
            sihayaRidge.addForces("Emperor", 3);
            sihayaRidge.addForces("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(greatMaker, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));
            assertEquals(0, tanks.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength("Emperor"));
            assertEquals(-1, turnSummary.messages.getFirst().indexOf("devoured"));
        }

        @Test
        void shaiHuludAfterSandtroutDoesNotDevourTroops() {
            game.getSpiceDeck().addFirst(shaiHulud);
            game.getSpiceDeck().addFirst(sandtrout);
            assertEquals(sandtrout, game.getSpiceDeck().getFirst());

            sihayaRidge.addForces("Emperor", 3);
            sihayaRidge.addForces("Emperor*", 5);
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(shaiHulud, game.getSpiceDiscardA().get(1));
            assertEquals(5, sihayaRidge.getForceStrength("Emperor*"));
            assertEquals(3, sihayaRidge.getForceStrength("Emperor"));
            assertEquals(0, tanks.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength("Emperor"));
            assertEquals(-1, turnSummary.messages.getFirst().indexOf("devoured"));
        }

        @Test
        void discoverySpiceBlowKillsTropps() {
            game.getSpiceDeck().addFirst(nextBlow);
            assertEquals(nextBlow, game.getSpiceDeck().getFirst());

            funeralPlain.addForces("Fremen", 5);
            funeralPlain.addForces("Fremen*", 3);
            assertEquals(3, funeralPlain.getForceStrength("Fremen*"));
            assertEquals(5, funeralPlain.getForceStrength("Fremen"));

            game.drawSpiceBlow("A");
            assertEquals(lastBlow, game.getSpiceDiscardA().get(0));
            assertEquals(nextBlow, game.getSpiceDiscardA().get(1));
            assertEquals(0, funeralPlain.getForceStrength("Fremen*"));
            assertEquals(0, funeralPlain.getForceStrength("Fremen"));
            assertEquals(3, tanks.getForceStrength("Fremen*"));
            assertEquals(5, tanks.getForceStrength("Fremen"));
            assertNotEquals(-1, turnSummary.messages.getFirst().indexOf("all forces in the territory were killed in the spice blow!\n"));
        }

        @Test
        void testFremenMayMoveSecondShaiHulud() {
            game.getSpiceDeck().addFirst(nextBlow);
            game.getSpiceDeck().addFirst(shaiHulud);
            game.getSpiceDeck().addFirst(shaiHulud);
            sihayaRidge.addForces("Fremen", 5);

            game.drawSpiceBlow("A");
            assertTrue(turnSummary.getMessages().getFirst().contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " may ride Shai-Hulud!\n"));
            assertTrue(turnSummary.getMessages().getFirst().contains("Shai-Hulud has been spotted! " + Emojis.FREMEN + " may place it in any sand territory.\n"));
//            assertEquals("Where would you like to place Shai-Hulud? p", fremenChat.getMessages().get(1));
//            assertEquals(3, fremenChat.getChoices().get(1).size());
        }

        @Test
        void testFremenMayMoveShaiHuludAfterGreatMaker() {
            game.getSpiceDeck().addFirst(nextBlow);
            game.getSpiceDeck().addFirst(shaiHulud);
            game.getSpiceDeck().addFirst(greatMaker);

            game.drawSpiceBlow("A");
            assertTrue(turnSummary.getMessages().getFirst().contains("After the Nexus, 17 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n"));
            assertTrue(turnSummary.getMessages().getFirst().contains("Shai-Hulud has been spotted! " + Emojis.FREMEN + " may place it in any sand territory.\n"));
//            assertEquals("Where would you like to place Shai-Hulud? p", fremenChat.getMessages().get(1));
//            assertEquals(3, fremenChat.getChoices().get(1).size());
        }

        @Test
        void testFremenMayMoveGreatMakerAfterShaiHulud() {
            game.getSpiceDeck().addFirst(nextBlow);
            game.getSpiceDeck().addFirst(greatMaker);
            game.getSpiceDeck().addFirst(shaiHulud);
            sihayaRidge.addForces("Fremen", 5);

            game.drawSpiceBlow("A");
            assertTrue(turnSummary.getMessages().getFirst().contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " may ride Shai-Hulud!\n"));
            assertTrue(turnSummary.getMessages().getFirst().contains("Great Maker has been spotted! " + Emojis.FREMEN + " may place it in any sand territory."));
            assertFalse(turnSummary.getMessages().getFirst().contains("Great Maker has been spotted in Sihaya Ridge!"));
//            assertEquals("Where would you like to place Great Maker? p", fremenChat.getMessages().get(1));
//            assertEquals(3, fremenChat.getChoices().get(1).size());
        }

        @Test
        void testFremenReservesMayRideGreatMakerAfterShaiHulud() {
            game.getSpiceDeck().addFirst(nextBlow);
            game.getSpiceDeck().addFirst(greatMaker);
            game.getSpiceDeck().addFirst(shaiHulud);
            sihayaRidge.addForces("Fremen", 5);

            game.drawSpiceBlow("A");
            assertTrue(turnSummary.getMessages().getFirst().contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " may ride Shai-Hulud!\n"));
            assertTrue(turnSummary.getMessages().getFirst().contains("Great Maker has been spotted! " + Emojis.FREMEN + " may place it in any sand territory.\n"));
        }
    }

    @Nested
    @DisplayName("#drawSpiceBlowNoFremen")
    class DrawSpiceBlowNoFremen {
        @Test
        void shaiHuludWithNoFremenDoesNotThrowException() {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(bt);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            game.addFaction(ix);

            game.advanceTurn();
            game.advanceTurn();
            game.advanceTurn();
            Territory shaiHuludTerritory = game.getTerritory("Cielago North (East Sector)");
            shaiHuludTerritory.addForces("Atreides", 3);
            SpiceCard lastBlow = game.getSpiceDeck().stream().filter(c -> c.name().equals("Cielago North (East Sector)")).findFirst().orElseThrow();
            game.getSpiceDiscardA().add(lastBlow);

            SpiceCard shaiHulud = game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow();
            game.getSpiceDeck().addFirst(shaiHulud);
            assertDoesNotThrow(() -> game.drawSpiceBlow("A"));
        }
    }

    @Nested
    @DisplayName("#removeForces")
    class RemoveForces {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
        }

        @Test
        void removeNotToTanksFlipsToHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Caladan", atreides, 5, 0, true);
            assertFalse(atreides.isHighThreshold());
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Atreides", 3);
            game.removeForces("Sihaya Ridge", atreides, 1, 0, false);
            assertTrue(atreides.isHighThreshold());
        }

        @Test
        void removeEmperorTroopAndSardaukarToTanks() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Emperor", 3);
            territory.addForces("Emperor*", 5);
            assertEquals(3, territory.getForceStrength("Emperor"));
            assertEquals(5, territory.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength(emperor.getName()));
            assertEquals(0, tanks.getForceStrength(emperor.getName() + "*"));

            game.removeForces("Sihaya Ridge", emperor, 1, 1, true);
            assertEquals(2, territory.getForceStrength("Emperor"));
            assertEquals(4, territory.getForceStrength("Emperor*"));
            assertEquals(1, tanks.getForceStrength(emperor.getName()));
            assertEquals(1, tanks.getForceStrength(emperor.getName() + "*"));
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, emperorChat.messages.size());
        }

        @Test
        void removeEmperorTroopAndSardaukarToReserves() {
            // Change to placeForces after placeForces moves to Game
            Territory territory = game.getTerritory("Sihaya Ridge");
            // Replace with placeForces after that method is moved to Game
            territory.addForces("Emperor", 3);
            territory.addForces("Emperor*", 5);
            assertEquals(3, territory.getForceStrength("Emperor"));
            assertEquals(5, territory.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength(emperor.getName()));
            assertEquals(0, tanks.getForceStrength(emperor.getName() + "*"));

            game.removeForces("Sihaya Ridge", emperor, 1, 1, false);
            assertEquals(2, territory.getForceStrength("Emperor"));
            assertEquals(4, territory.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength(emperor.getName()));
            assertEquals(0, tanks.getForceStrength(emperor.getName() + "*"));
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, emperorChat.messages.size());
        }

        @Test
        void removeEmperorTroopAndSardaukarFromHomeworlds() {
            game.addGameOption(GameOption.HOMEWORLDS);
            // Change to placeForces after placeForces moves to Game
            Territory kaitain = game.getTerritory("Kaitain");
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            assertEquals(15, kaitain.getForceStrength("Emperor"));
            assertEquals(5, salusaSecundus.getForceStrength("Emperor*"));
            assertEquals(0, tanks.getForceStrength(emperor.getName()));
            assertEquals(0, tanks.getForceStrength(emperor.getName() + "*"));
            assertTrue(emperor.isHighThreshold());
            assertTrue(emperor.isSecundusHighThreshold());

            assertTrue(emperor.isHighThreshold());
            assertEquals(15, kaitain.getForceStrength("Emperor"));
            assertEquals(5, salusaSecundus.getForceStrength("Emperor*"));
            assertEquals(0, kaitain.getForceStrength("Emperor*"));
            game.removeForces("Kaitain", emperor, 14, 0, true);
            assertEquals(1, kaitain.getForceStrength("Emperor"));
            assertFalse(emperor.isHighThreshold());
            assertEquals(1, turnSummary.messages.size());
            assertEquals("Kaitain has flipped to Low Threshold.", turnSummary.messages.getFirst());

            game.removeForces("Salusa Secundus", emperor, 0, 3, true);
            assertEquals(2, salusaSecundus.getForceStrength("Emperor*"));
            assertEquals(2, emperor.getSpecialReservesStrength());
            assertTrue(emperor.isSecundusHighThreshold());
            assertFalse(emperor.isHighThreshold());
            game.removeForces("Salusa Secundus", emperor, 0, 1, true);
            assertEquals(1, salusaSecundus.getForceStrength("Emperor*"));
            assertEquals(1, emperor.getSpecialReservesStrength());
            assertFalse(emperor.isSecundusHighThreshold());
            assertEquals(2, turnSummary.messages.size());
            assertEquals("Salusa Secundus has flipped to Low Threshold.", turnSummary.messages.get(1));
        }

        @Test
        void removeEmperorTroopButSardaukarCountForHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            // Change to placeForces after placeForces moves to Game
            Territory kaitain = game.getTerritory("Kaitain");
            kaitain.addForces("Emperor*", 1);
            assertTrue(emperor.isHighThreshold());
            game.removeForces("Kaitain", emperor, 11, 0, true);
            assertTrue(emperor.isHighThreshold());
        }

        @Test
        void removeSardaukarButRegularDoNotCountForHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            // Change to placeForces after placeForces moves to Game
            Territory salusaSecundus = game.getTerritory("Salusa Secundus");
            salusaSecundus.addForces("Emperor", 1);
            assertTrue(emperor.isSecundusHighThreshold());
            game.removeForces("Salusa Secundus", emperor, 0, 4, true);
            assertFalse(emperor.isSecundusHighThreshold());
        }

        @Test
        void removeFremenTroopAndFedaykinToReserves() {
            // Change to placeForces after placeForces moves to Game
            Territory territory = game.getTerritory("Sihaya Ridge");
            // Replace with placeForces after that method is moved to Game
            territory.addForces("Fremen", 3);
            territory.addForces("Fremen*", 3);
            assertEquals(3, territory.getForceStrength("Fremen"));
            assertEquals(3, territory.getForceStrength("Fremen*"));
            assertEquals(0, tanks.getForceStrength(fremen.getName()));
            assertEquals(0, tanks.getForceStrength(fremen.getName() + "*"));

            game.removeForces("Sihaya Ridge", fremen, 1, 1, false);
            assertEquals(2, territory.getForceStrength("Fremen"));
            assertEquals(2, territory.getForceStrength("Fremen*"));
            assertEquals(0, tanks.getForceStrength(fremen.getName()));
            assertEquals(0, tanks.getForceStrength(fremen.getName() + "*"));
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, fremenChat.messages.size());
        }

        @Test
        void removeBGAdvisorToTanks() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Advisor", 1);
            assertEquals(1, territory.getForceStrength("Advisor"));

            game.removeForces("Sihaya Ridge", bg, 1, 0, true);
            assertEquals(0, territory.getForceStrength("Advisor"));
            assertEquals(0, turnSummary.messages.size());
            assertEquals(0, bgChat.messages.size());
        }

        @Test
        void removeSpecialFailsForBG() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Advisor", 1);
            assertEquals(1, territory.getForceStrength("Advisor"));

            assertThrows(IllegalArgumentException.class, () -> game.removeForces("Sihaya Ridge", bg, 1, 1, true));
        }

        @Test
        void removeTooManyBG() {
            Territory territory = game.getTerritory("Sihaya Ridge");
            territory.addForces("Advisor", 1);
            assertEquals(1, territory.getForceStrength("Advisor"));

            assertThrows(IllegalArgumentException.class, () -> game.removeForces("Sihaya Ridge", bg, 2, 0, true));
        }
    }

    @Nested
    @DisplayName("killLeader")
    class KillLeader {
        List<Leader> leaders;

        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);

            leaders = emperor.getLeaders();
            assertEquals(5, leaders.size());
            game.killLeader(emperor, "Caid");
        }

        @Test
        void testOneFewerLeader () {
            assertEquals(4, leaders.size());
        }

        @Test
        void testMessageSentToTurnSummary() {
            assertEquals(Emojis.EMPEROR + " Caid was sent to the tanks.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testMessageSentToLedger() {
            assertEquals("Caid was sent to the tanks.", emperorLedger.getMessages().getFirst());
        }

//        @Test
//        void testCantKillLeaderInTanks() {
//            assertThrows(IllegalArgumentException.class, () -> game.killLeader(emperor, "Caid"));
//        }
    }

    @Nested
    @DisplayName("reviveForces")
    class ReviveForces {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(emperor);
            game.addFaction(ix);
//            game.addFaction(fremen);
//            game.addFaction(bt);
        }

        @Test
        void testIxHighTresholdPaidRevival() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Ix", ix, 2, 1, true);
            game.reviveForces(ix, true, 0, 1, false);
            assertEquals(0, game.getTleilaxuTanks().getForceStrength("Ix"));
        }

        @Test
        void testIxPaidRevivalNonHomeworldGameDoesNotGetBonusSuboids() {
            game.removeForces("Ix", ix, 2, 1, true);
            game.reviveForces(ix, true, 0, 1, false);
            assertEquals(2, game.getTleilaxuTanks().getForceStrength("Ix"));
        }

        @Test
        void testIxHighTresholFreeRevivalDoesNotGetBonusSuboids() {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Ix", ix, 2, 1, true);
            game.reviveForces(ix, false, 0, 1, false);
            assertEquals(2, game.getTleilaxuTanks().getForceStrength("Ix"));
        }

        @Test
        void testIxHighTresholdEmperorAllyRevivalDoesNotGetBonusSuboids() {
            game.createAlliance(ix, emperor);
            game.addGameOption(GameOption.HOMEWORLDS);
            game.removeForces("Ix", ix, 2, 1, true);
            game.reviveForces(ix, true, 0, 1, true);
            assertEquals(2, game.getTleilaxuTanks().getForceStrength("Ix"));
        }

        @Test
        void testReviveMoreThanAreInTanks () {
            tanks.addForces(emperor.getName(), 1);
            tanks.addForces(emperor.getName() + "*", 1);
            assertEquals(1, tanks.getForceStrength(emperor.getName()));
            assertEquals(1, tanks.getForceStrength(emperor.getName() + "*"));

            assertThrows(IllegalArgumentException.class, () -> game.reviveForces(emperor, false, 2, 0));
            assertThrows(IllegalArgumentException.class, () -> game.reviveForces(emperor, false, 0, 2));
            assertDoesNotThrow(() -> game.reviveForces(emperor, false, 1, 1));
        }

        @Test
        void testRevivalFlipsToHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(tanks.getForces().isEmpty());
            game.removeForces("Kaitain", emperor, 15, 0, true);
            assertFalse(emperor.isHighThreshold());
            game.removeForces("Salusa Secundus", emperor, 0, 5, true);
            assertFalse(emperor.isSecundusHighThreshold());

            game.reviveForces(emperor, false, 4, 2);
            assertFalse(emperor.isHighThreshold());
            assertFalse(emperor.isSecundusHighThreshold());
            game.reviveForces(emperor, false, 1, 1);
            assertTrue(emperor.isHighThreshold());
            assertTrue(emperor.isSecundusHighThreshold());
        }

        @Test
        void testSardaukarOnKaitainCountForHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(tanks.getForces().isEmpty());
            // Replace with game.moveForces after that is refactored
            game.getTerritory("Kaitain").addForces("Emperor*", 1);
            game.removeForces("Kaitain", emperor, 15, 0, true);
            assertFalse(emperor.isHighThreshold());

            game.reviveForces(emperor, false, 4, 0);
            assertTrue(emperor.isHighThreshold());
        }

        @Test
        void testRegularOnSalusaSecundusDoNotCountForHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(tanks.getForces().isEmpty());
            // Replace with game.moveForces after that is refactored
            game.getTerritory("Salusa Secundus").addForces("Emperor", 1);
            game.removeForces("Salusa Secundus", emperor, 0, 5, true);
            assertFalse(emperor.isSecundusHighThreshold());

            game.reviveForces(emperor, false, 0, 2);
            assertFalse(emperor.isSecundusHighThreshold());
        }
    }

    @Nested
    @DisplayName("#putTerritoryInAnotherTerritory")
    class PutTerritoryInAnotherTerritory {
        Territory hms;
        final String hmsName = "Hidden Mobile Stronghold";
        Territory shieldWallNorth;
        final String shieldWallNorthName = "Shield Wall (North Sector)";
        final String shieldWallName = shieldWallNorthName.replaceAll("\\(.*\\)", "").strip();

        @BeforeEach
        void setUp() {
            game.addFaction(ix);
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
        final String hmsName = "Hidden Mobile Stronghold";
        Territory shieldWallNorth;
        final String shieldWallNorthName = "Shield Wall (North Sector)";
        final String shieldWallName = shieldWallNorthName.replaceAll("\\(.*\\)", "").strip();

        @BeforeEach
        void setUp() {
            game.addFaction(ix);
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

    @Nested
    @DisplayName("#updateStrongholdSkills")
    class UpdateStrongholdSkills {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            game.addFaction(ecaz);
        }

        @Test
        void testNoStrongholdCards() {
            game.updateStrongholdSkills();
            assertTrue(atreides.getStrongholdCards().isEmpty());
            assertTrue(guild.getStrongholdCards().isEmpty());
            assertTrue(harkonnen.getStrongholdCards().isEmpty());
        }

        @Test
        void testWithStrongholdCards() {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            game.updateStrongholdSkills();
            assertEquals(1, atreides.getStrongholdCards().size());
            assertTrue(atreides.hasStrongholdCard("Arrakeen"));
            assertFalse(guild.hasStrongholdCard("Arrakeen"));
            assertFalse(harkonnen.hasStrongholdCard("Arrakeen"));
            assertEquals(1, guild.getStrongholdCards().size());
            assertTrue(guild.hasStrongholdCard("Tuek's Sietch"));
            assertFalse(atreides.hasStrongholdCard("Tuek's Sietch"));
            assertFalse(harkonnen.hasStrongholdCard("Tuek's Sietch"));
            assertEquals(1, harkonnen.getStrongholdCards().size());
            assertTrue(harkonnen.hasStrongholdCard("Carthag"));
            assertFalse(atreides.hasStrongholdCard("Carthag"));
            assertFalse(guild.hasStrongholdCard("Carthag"));
        }

        @Test
        void testWithStrongholdCardsEcazAlly() {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            arrakeen.addForces("Ecaz", 1);
            game.createAlliance(atreides, ecaz);
            game.updateStrongholdSkills();
            assertTrue(atreides.getStrongholdCards().isEmpty());
            assertEquals(1, ecaz.getStrongholdCards().size());
            assertTrue(ecaz.hasStrongholdCard("Arrakeen"));
            assertFalse(atreides.hasStrongholdCard("Arrakeen"));
            assertEquals(1, guild.getStrongholdCards().size());
            assertTrue(guild.hasStrongholdCard("Tuek's Sietch"));
            assertEquals(1, harkonnen.getStrongholdCards().size());
            assertTrue(harkonnen.hasStrongholdCard("Carthag"));
        }

        @Test
        void testWithStrongholdCardsEcazAllyFalse() {
            game.addGameOption(GameOption.STRONGHOLD_SKILLS);
            arrakeen.addForces("Harkonnen", 1);
            game.createAlliance(atreides, ecaz);
            game.updateStrongholdSkills();
            assertTrue(atreides.getStrongholdCards().isEmpty());
            assertTrue(ecaz.getStrongholdCards().isEmpty());
            assertEquals(1, guild.getStrongholdCards().size());
            assertTrue(guild.hasStrongholdCard("Tuek's Sietch"));
            assertEquals(1, harkonnen.getStrongholdCards().size());
            assertTrue(harkonnen.hasStrongholdCard("Carthag"));
        }
    }

    @Nested
    @DisplayName("#createAlliance")
    class CreateAlliance {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(bt);
        }

        @Test
        void noCurrentAlliance() {
            game.createAlliance(fremen, guild);
            assertEquals(Emojis.FREMEN + " and " + Emojis.GUILD + " have formed an alliance.", turnSummary.messages.getFirst());
            assertEquals("Guild", fremen.getAlly());
            assertEquals("Fremen", guild.getAlly());
            assertEquals("You are now allies with " + Emojis.GUILD + "!", fremenLedger.getMessages().getFirst());
            assertEquals("You are now allies with " + Emojis.FREMEN + "!", guildLedger.getMessages().getFirst());

            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void noCurrentAllianceWithNexusCard() {
            fremen.setNexusCard(new NexusCard("Atreides"));
            game.createAlliance(fremen, guild);
            assertEquals(Emojis.FREMEN + " has discarded a Nexus Card.", turnSummary.messages.get(0));
            assertEquals(Emojis.FREMEN + " and " + Emojis.GUILD + " have formed an alliance.", turnSummary.messages.get(1));
            assertEquals("Guild", fremen.getAlly());
            assertEquals("Fremen", guild.getAlly());
            assertEquals("You are now allies with " + Emojis.GUILD + "!", fremenLedger.getMessages().getFirst());
            assertEquals("You are now allies with " + Emojis.FREMEN + "!", guildLedger.getMessages().getFirst());
            assertNull(fremen.getNexusCard());

            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void withCurrentAlliance() {
            game.createAlliance(fremen, guild);
            turnSummary.clear();
            fremenLedger.clear();
            guildLedger.clear();
            game.getUpdateTypes().clear();

            game.createAlliance(fremen, bt);
            assertEquals(Emojis.FREMEN + " and " + Emojis.GUILD + " are no longer allies.", turnSummary.messages.get(0));
            assertEquals(Emojis.FREMEN + " and " + Emojis.BT + " have formed an alliance.", turnSummary.messages.get(1));

            assertEquals("BT", fremen.getAlly());
            assertEquals("", guild.getAlly());
            assertEquals("Fremen", bt.getAlly());

            assertEquals("Your alliance with " + Emojis.GUILD + " has been dissolved!", fremenLedger.getMessages().get(0));
            assertEquals("Your alliance with " + Emojis.FREMEN + " has been dissolved!", guildLedger.getMessages().getFirst());

            assertEquals("You are now allies with " + Emojis.BT + "!", fremenLedger.getMessages().get(1));
            assertEquals("You are now allies with " + Emojis.FREMEN + "!", btLedger.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#removeAlliance")
    class RemoveAlliance {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(guild);
        }

        @Test
        void noAlliance() {
            game.removeAlliance(fremen);
            assertEquals(0, turnSummary.messages.size());
            assertEquals("", fremen.getAlly());
            assertEquals(0, fremenLedger.getMessages().size());

            assertTrue(game.getUpdateTypes().isEmpty());
        }

        @Test
        void withAlliance() {
            game.createAlliance(fremen, guild);
            turnSummary.clear();
            fremenLedger.clear();
            guildLedger.clear();
            game.getUpdateTypes().clear();

            game.removeAlliance(fremen);
            assertEquals("", fremen.getAlly());
            assertEquals("", guild.getAlly());
            assertEquals(Emojis.FREMEN + " and " + Emojis.GUILD + " are no longer allies.", turnSummary.messages.getFirst());
            assertEquals("Your alliance with " + Emojis.GUILD + " has been dissolved!", fremenLedger.getMessages().getFirst());
            assertEquals("Your alliance with " + Emojis.FREMEN + " has been dissolved!", guildLedger.getMessages().getFirst());

            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }
    }

    @Nested
    @DisplayName("#spiceHarvestPhase")
    class SpiceHarvestPhase {
        String advisorFlipMessage = Emojis.BG_ADVISOR + " are alone in Habbanya Sietch and have flipped to " + Emojis.BG_FIGHTER;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(bg);
            game.startBattlePhase();
            habbanyaSietch.addForces("Advisor", 1);
        }

        @Test
        void testAdvisorsAloneFlipWithGF9Rules() throws InvalidGameStateException {
            game.startSpiceHarvest();
            assertEquals(advisorFlipMessage, turnSummary.getMessages().getLast());
        }

        @Test
        void testAdvisorsAloneDontFlipWithAllyCoexistence() throws InvalidGameStateException {
            game.addGameOption(GameOption.BG_COEXIST_WITH_ALLY);
            game.startSpiceHarvest();
            assertFalse(turnSummary.getMessages().contains(advisorFlipMessage));
        }

        @Test
        void testMoritaniLosesDukeVidal() throws InvalidGameStateException {
            game.addFaction(moritani);
            moritani.getDukeVidal();
            assertTrue(moritani.getLeader("Duke Vidal").isPresent());
            turnSummary.clear();
            game.startSpiceHarvest();
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertEquals("Duke Vidal has left the " + Emojis.MORITANI + " services... for now.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testMoritaniHighThreshold() throws InvalidGameStateException {
            game.addFaction(moritani);
            game.addGameOption(GameOption.HOMEWORLDS);
            moritani.placeTerrorToken(sietchTabr, "Assassination");
            game.startSpiceHarvest();
            assertEquals("You are at High Threshold and can remove a Terror Token to gain 4 " + Emojis.SPICE + ". mo", moritaniChat.getMessages().getLast());
        }

        @Test
        void testMoritaniLowThreshold() throws InvalidGameStateException {
            game.addFaction(moritani);
            game.addGameOption(GameOption.HOMEWORLDS);
            moritani.placeForceFromReserves(game, habbanyaSietch, 13, false);
            moritani.placeTerrorToken(sietchTabr, "Assassination");
            moritaniChat.clear();
            game.startSpiceHarvest();
            assertTrue(moritaniChat.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#harkonnenSecretAlly")
    class HarkonnenSecretAlly {
        NexusCard harkNexusCard;

        @BeforeEach
        void setUp() {
            game.addFaction(atreides);
            harkNexusCard = game.getNexusDeck().stream().filter(c -> c.name().equals("Harkonnen")).findFirst().orElseThrow();
        }

        @Test
        void testNobodyHasHarkonnenNexusCard() {
            assertThrows(InvalidGameStateException.class, () -> game.harkonnenSecretAlly());
        }

        @Test
        void testAtreidesHasHarkonnenNexusCard() {
            atreides.addTraitorCard(game.getTraitorDeck().pop());
            atreides.setNexusCard(harkNexusCard);
            assertDoesNotThrow(() -> game.harkonnenSecretAlly());
            assertEquals(Emojis.ATREIDES + " has drawn 2 Traitor cards for " + Emojis.HARKONNEN + " Secret Ally.", turnSummary.getMessages().getLast());
            assertEquals("You must discard two Traitors. at", atreidesChat.getMessages().getFirst());
            assertEquals("First discard:", atreidesChat.getMessages().get(1));
            assertEquals("Second discard:", atreidesChat.getMessages().getLast());
            assertEquals(3, atreidesChat.getChoices().getFirst().size());
            assertEquals(3, atreidesChat.getChoices().getLast().size());
        }

        @Test
        void testAtreidesHasHarkonnenNexusCardHarkonnenInTheGame() {
            game.addFaction(harkonnen);
            atreides.setNexusCard(harkNexusCard);
            assertThrows(InvalidGameStateException.class, () -> game.harkonnenSecretAlly());
        }

        @Test
        void testHarkonnenHasHarkonnenNexusCard() {
            game.addFaction(harkonnen);
            harkonnen.setNexusCard(harkNexusCard);
            assertThrows(InvalidGameStateException.class, () -> game.harkonnenSecretAlly());
        }
    }
}
