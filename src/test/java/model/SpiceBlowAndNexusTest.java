package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SpiceBlowAndNexusTest extends DuneTest {
    SpiceBlowAndNexus spiceBlowAndNexus;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.addFaction(fremen);
    }

    @Nested
    @DisplayName("#shaiHuludAfterDeckReshuffled")
    class ShaiHuludAfterDeckReshuffled {
        SpiceCard shaiHulud;
        SpiceCard sihayaRidge;

        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            shaiHulud = game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow();
            sihayaRidge = game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow();
            game.getSpiceDeck().removeIf(c -> c.name().equals("Shai-Hulud"));
            for (int i = 0; i < 7; i++) {
                game.advanceTurn();
                game.startSpiceBlowPhase();
                game.spiceBlowPhaseNextStep();
            }
            assertEquals(7, game.getTurn());
            game.advanceTurn();
            game.startSpiceBlowPhase();
            assertTrue(game.getSpiceDeck().isEmpty());
        }

        @Test
        void testDraw() {
            game.getSpiceDiscardA().clear();
            game.getSpiceDiscardB().clear();
            for (int i = 0; i < 100; i++)
                game.getSpiceDiscardA().add(shaiHulud);
            game.getSpiceDiscardB().add(sihayaRidge);
            // There is a 1% chance Sihaya Ridge will be on top, but generally this would catch a failure.
            assertDoesNotThrow(() -> game.spiceBlowPhaseNextStep());
        }
    }

    @Nested
    @DisplayName("#thumper")
    class Thumper {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.setTurn(2);
            fremen.addTreacheryCard(new TreacheryCard("Thumper"));
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            game.getSpiceDiscardA().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            game.getSpiceDiscardB().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("The Great Flat")).findFirst().orElseThrow());
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testSpiceCardNotDrawn() {
            assertEquals("Funeral Plain", game.getSpiceDiscardA().getLast().name());
        }

        @Test
        void testFremenAskedAboutThumper() {
            assertEquals("Would you like to play Thumper in Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertFalse(fremenChat.getChoices().getFirst().isEmpty());
        }

        @Test
        void testFremenPlaysThumperAndSpiceBlowCanBeDrawn() {
            assertThrows(InvalidGameStateException.class, () -> game.spiceBlowPhaseNextStep());
            spiceBlowAndNexus.playThumper(game, fremen);
            assertDoesNotThrow(() -> game.spiceBlowPhaseNextStep());
            assertEquals("Red Chasm", game.getSpiceDiscardA().getLast().name());
            assertEquals("You will play Thumper in Funeral Plain.", fremenChat.getMessages().getLast());
        }

        @Test
        void testNexusIsAnnounced() throws IOException {
            spiceBlowAndNexus.playThumper(game, fremen);
            spiceBlowAndNexus.nextStep(game);
            assertEquals(" We have a Nexus! Create your alliances, reaffirm, backstab, or go solo here.", gameActions.getMessages().getLast());
        }

        @Test
        void testFremenDeclinesThumperOnDeckA() throws IOException {
//            assertThrows(InvalidGameStateException.class, () -> spiceBlowAndNexus.nextStep(game));
            spiceBlowAndNexus.declineThumper(game, fremen);
            assertEquals("You will not play Thumper in Funeral Plain.", fremenChat.getMessages().getLast());
            spiceBlowAndNexus.nextStep(game);
            assertEquals("Red Chasm", game.getSpiceDiscardA().getLast().name());
            spiceBlowAndNexus.nextStep(game);
            assertEquals("Sihaya Ridge", game.getSpiceDiscardB().getLast().name());
        }

        @Test
        void testFremenDeclinesThumperOnDeckAThumperAllowedOnDeckB() throws IOException {
            game.addGameOption(GameOption.THUMPER_ON_DECK_B);
//            assertThrows(InvalidGameStateException.class, () -> spiceBlowAndNexus.nextStep(game));
            spiceBlowAndNexus.declineThumper(game, fremen);
            spiceBlowAndNexus.nextStep(game);
            assertEquals("Red Chasm", game.getSpiceDiscardA().getLast().name());
            fremenChat.clear();
            spiceBlowAndNexus.nextStep(game);
            assertEquals("Would you like to play Thumper in The Great Flat? fr", fremenChat.getMessages().getFirst());
            assertFalse(fremenChat.getChoices().getFirst().isEmpty());
            assertEquals("The Great Flat", game.getSpiceDiscardB().getLast().name());
        }

        @Test
        void testFremenDeclinesThumperOnDeckAAndDeckB() throws IOException {
            game.addGameOption(GameOption.THUMPER_ON_DECK_B);
//            assertThrows(InvalidGameStateException.class, () -> spiceBlowAndNexus.nextStep(game));
            spiceBlowAndNexus.declineThumper(game, fremen);
            spiceBlowAndNexus.nextStep(game);
            assertEquals("Red Chasm", game.getSpiceDiscardA().getLast().name());
            fremenChat.clear();
            spiceBlowAndNexus.nextStep(game);
            fremenChat.clear();
            spiceBlowAndNexus.declineThumper(game, fremen);
            assertTrue(fremenChat.getChoices().getFirst().isEmpty());
            assertEquals(0, game.getPhase());
        }
    }

    @Test
    void testMoritaniAllyRegainsHandLimitAfterSandtrout() throws InvalidGameStateException {
        game.addFaction(moritani);
        game.addFaction(fremen);
        game.createAlliance(moritani, fremen);
        carthag.addTerrorToken(game, "Atomics");
        moritani.triggerTerrorToken(bt, carthag, "Atomics");
        game.setTurn(2);
        game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
        game.getSpiceDeck().addFirst(new SpiceCard("Sandtrout", -1, 0, null, null));
        game.drawSpiceBlow("A", false);
        assertEquals(4, fremen.getHandLimit());
    }

    @Test
    void testFremenSeeHieregToken() {
        game.addGameOption(GameOption.DISCOVERY_TOKENS);
        game.addFaction(fremen);
        game.getSpiceDeck().addFirst(new SpiceCard("Sihaya Ridge", 8, 6, "Hiereg", "Cielago East (West Sector)"));
        game.drawSpiceBlow("A", false);
        String tokenName = game.getTerritory("Cielago East (West Sector)").getDiscoveryToken();
        assertEquals("The Hiereg Token in Cielago East (West Sector) is " + tokenName + ".", fremenChat.getMessages().getLast());
    }

    @Test
    void testGuildSeeSmugglerToken() {
        game.addGameOption(GameOption.DISCOVERY_TOKENS);
        game.addFaction(guild);
        game.getSpiceDeck().addFirst(new SpiceCard("Funeral Plain", 14, 6, "Smuggler", "Pasty Mesa (North Sector)"));
        game.drawSpiceBlow("A", false);
        String tokenName = game.getTerritory("Pasty Mesa (North Sector)").getDiscoveryToken();
        assertEquals("The Smuggler Token in Pasty Mesa (North Sector) is " + tokenName + ".", guildChat.getMessages().getLast());
    }

    @Nested
    @DisplayName("#wormThenSandtroutThenWorm")
    class WormThenSandtroutThenWorm {
        @BeforeEach
        void setUp() {
            game.getSpiceDiscardA().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("The Great Flat")).findFirst().orElseThrow());
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(new SpiceCard("Sandtrout", -1, 0, null, null));
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.drawSpiceBlow("A", false);
        }

        @Test
        void testFremenMayNotPlaceNoEffectWorm() {
            assertFalse(turnSummary.getMessages().getLast().contains("may place it in any sand territory"));
        }

        @Test
        void testWormSandtroutWormTerritoryDoublesSpiceBlow() {
            assertEquals(12, funeralPlain.getSpice());
        }

        @Test
        void testNoNexus() {
            assertTrue(gameActions.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#wormThenSandtroutThenWormThenAnotherWorm")
    class WormThenSandtroutThenWormThenAnotherWorm {
        @BeforeEach
        void setUp() {
            game.getSpiceDiscardA().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("The Great Flat")).findFirst().orElseThrow());
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(new SpiceCard("Sandtrout", -1, 0, null, null));
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.drawSpiceBlow("A", false);
        }

        @Test
        void testFremenMayPlaceIfAnotherWormAppears() {
            assertTrue(turnSummary.getMessages().getLast().contains("may place it in any sand territory"));
        }

        @Test
        void testSpiceBlowDoesNotDoubleIfAnotherWormAppearsFirst() {
            assertEquals(6, funeralPlain.getSpice());
        }

        @Test
        void testNexusOccurs() {
            assertEquals(" We have a Nexus! Create your alliances, reaffirm, backstab, or go solo here.", gameActions.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#SandtroutThenWormThenAnotherWorm")
    class SandtroutThenWormThenAnotherWorm {
        @BeforeEach
        void setUp() {
            game.getSpiceDiscardA().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("The Great Flat")).findFirst().orElseThrow());
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(new SpiceCard("Sandtrout", -1, 0, null, null));
            game.drawSpiceBlow("A", false);
        }

        @Test
        void testFremenMayNotPlaceWorm() {
            assertFalse(turnSummary.getMessages().getLast().contains("may place it in any sand territory"));
        }

        @Test
        void testWormSandtroutWormTerritoryDoublesSpiceBlow() {
            assertEquals(6, funeralPlain.getSpice());
        }

        @Test
        void testNexusOccurs() {
            assertEquals(" We have a Nexus! Create your alliances, reaffirm, backstab, or go solo here.", gameActions.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#thumperWithSandtroutActive")
    class ThumperWithSandtroutActive {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(new SpiceCard("Sandtrout", -1, 0, null, null));
            game.drawSpiceBlow("A", false);
            fremen.addTreacheryCard(new TreacheryCard("Thumper"));
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            game.getSpiceDiscardB().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("The Great Flat")).findFirst().orElseThrow());
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testSpiceCardNotDrawn() {
            assertEquals("Funeral Plain", game.getSpiceDiscardA().getLast().name());
        }

        @Test
        void testFremenAskedAboutThumper() {
            assertEquals("Would you like to play Thumper in Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertFalse(fremenChat.getChoices().getFirst().isEmpty());
        }

        @Test
        void testFremenPlaysThumperAndSpiceBlowCanBeDrawn() {
            assertThrows(InvalidGameStateException.class, () -> game.spiceBlowPhaseNextStep());
            spiceBlowAndNexus.playThumper(game, fremen);
            assertDoesNotThrow(() -> game.spiceBlowPhaseNextStep());
            assertEquals("Red Chasm", game.getSpiceDiscardA().getLast().name());
            assertEquals("You will play Thumper in Funeral Plain.", fremenChat.getMessages().getLast());
        }

        @Test
        void testFremenPlaysThumperAndSpiceBlowIsDoubled() {
            assertThrows(InvalidGameStateException.class, () -> game.spiceBlowPhaseNextStep());
            spiceBlowAndNexus.playThumper(game, fremen);
            assertDoesNotThrow(() -> game.spiceBlowPhaseNextStep());
            assertEquals(16, game.getTerritory("Red Chasm").getSpice());
        }

        @Test
        void testNoNexus() throws IOException {
            spiceBlowAndNexus.playThumper(game, fremen);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(gameActions.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#noWorms")
    class NoWorms {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().getFirst().name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(1).name());
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testPhaseIsAnnounced() {
            assertEquals("**Turn 0 Spice Blow Phase**", turnSummary.getMessages().get(1));
        }

        @Test
        void testDeckAIsDrawnAutomatically() {
            assertTrue(turnSummary.getMessages().get(2).startsWith("**Spice Deck A**"));
        }

        @Test
        void testNextStepDrawsSpiceBlowB() throws IOException {
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            assertTrue(spiceBlowAndNexus.nextStep(game));
            assertTrue(turnSummary.getMessages().getFirst().startsWith("**Spice Deck B**"));
        }
    }

    @Nested
    @DisplayName("#shaiHuludOnDeckA")
    class ShaiHuludOnDeckA {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDiscardA().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            assertEquals("Shai-Hulud", game.getSpiceDeck().getFirst().name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(1).name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(2).name());
            funeralPlain.addForces("Fremen", 5);
            funeralPlain.addForces("Fremen*", 3);
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testShaiHuludDoesNotGiveFremenButtonsYet() {
            assertTrue(turnSummary.getMessages().get(2).contains(Emojis.WORM + " Shai-Hulud has been spotted in Funeral Plain!\n"));
            assertTrue(turnSummary.getMessages().get(2).contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud!"));
            assertTrue(fremenChat.getMessages().isEmpty());
            assertTrue(fremenChat.getChoices().isEmpty());
        }

        @Test
        void testNextStepGivesFremenButtons() throws IOException {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().get(3).contains("5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!"));
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertEquals(5, fremenChat.getChoices().getFirst().size());
        }

        @Test
        void testSecondNextStepDrawsToDeckBAndEndsPhase() throws IOException {
            spiceBlowAndNexus.nextStep(game);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().getLast().contains(Emojis.SPICE + " has been spotted in Sihaya Ridge!"));
            assertTrue(spiceBlowAndNexus.isPhaseComplete());
        }
    }

    @Nested
    @DisplayName("#twoShaiHuludOnDeckB")
    class TwoShaiHuludOnDeckB {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.setTurn(2);
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Shai-Hulud")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            game.getSpiceDiscardB().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Funeral Plain")).findFirst().orElseThrow());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().getFirst().name());
            assertEquals("Shai-Hulud", game.getSpiceDeck().get(1).name());
            assertEquals("Shai-Hulud", game.getSpiceDeck().get(2).name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(3).name());
            funeralPlain.addForces("Fremen", 5);
            funeralPlain.addForces("Fremen*", 3);
            spiceBlowAndNexus = game.startSpiceBlowPhase();
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            spiceBlowAndNexus.nextStep(game);
        }

        @Test
        void testShaiHuludDoesNotGiveFremenButtonsYet() {
            assertTrue(turnSummary.getMessages().getFirst().contains(Emojis.WORM + " Shai-Hulud has been spotted in Funeral Plain!\n"));
            assertTrue(turnSummary.getMessages().getFirst().contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud!"));
            assertNotEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
        }

        @Test
        void testSecondNextStepGivesFremenButtons() throws IOException {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().get(1).contains("5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!"));
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertEquals(5, fremenChat.getChoices().getFirst().size());
        }

        @Test
        void testFremenPlacesSecondShaiHuludInFuneralPlainAndDontGetNewRideButtonsYet() {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            game.placeShaiHulud("Funeral Plain", "Shai-Hulud", false);
            assertEquals(0, fremenChat.getMessages().size());
        }

        @Test
        void testFremenPlacesSecondShaiHuludInFuneralPlainAndThirdNextStepGetsNewRideButtons() throws IOException {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            game.placeShaiHulud("Funeral Plain", "Shai-Hulud", false);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().get(2).contains("5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!"));
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertEquals(5, fremenChat.getChoices().getFirst().size());
        }

        @Test
        void testFourthNextStepEndsPhase() throws IOException {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            spiceBlowAndNexus.nextStep(game);
            game.placeShaiHulud("Funeral Plain", "Shai-Hulud", false);
            spiceBlowAndNexus.nextStep(game);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(spiceBlowAndNexus.isPhaseComplete());
        }
    }

    @Nested
    @DisplayName("#harvester")
    class Harvester {
        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().getFirst().name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(1).name());
            fremen.addTreacheryCard(new TreacheryCard("Harvester"));
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testHarvesterAciveAfterFirstBlow() {
            assertTrue(spiceBlowAndNexus.isHarvesterActive());
        }

        @Test
        void testSecondBlowDoesNotEndPhase() throws IOException {
            spiceBlowAndNexus.declineHarvester(fremen);
            assertFalse(spiceBlowAndNexus.nextStep(game));
            spiceBlowAndNexus.declineHarvester(fremen);
            assertTrue(spiceBlowAndNexus.nextStep(game));
        }
    }
}
