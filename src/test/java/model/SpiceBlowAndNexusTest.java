package model;

import constants.Emojis;
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
    @DisplayName("#noWorms")
    class NoWorms {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Sihaya Ridge")).findFirst().orElseThrow());
            game.getSpiceDeck().addFirst(game.getSpiceDeck().stream().filter(c -> c.name().equals("Red Chasm")).findFirst().orElseThrow());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().getFirst().name());
            assertNotEquals("Shai-Hulud", game.getSpiceDeck().get(1).name());
            spiceBlowAndNexus = game.startSpiceBlowPhase();
        }

        @Test
        void testPhaseIsAnnounced() {
            assertEquals("**Turn 0 Spice Blow Phase**", turnSummary.getMessages().getFirst());
        }

        @Test
        void testDeckAIsDrawnAutomatically() {
            assertTrue(turnSummary.getMessages().get(1).startsWith("**Spice Deck A**"));
        }

        @Test
        void testNextStepDrawsSpiceBlowB() {
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
        void setUp() throws InvalidGameStateException {
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
            assertTrue(turnSummary.getMessages().get(1).contains(Emojis.WORM + " Shai-Hulud has been spotted in Funeral Plain!\n"));
            assertTrue(turnSummary.getMessages().get(1).contains("After the Nexus, 5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud!"));
            assertTrue(fremenChat.getMessages().isEmpty());
            assertTrue(fremenChat.getChoices().isEmpty());
        }

        @Test
        void testNextStepGivesFremenButtons() {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().get(2).contains("5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!"));
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertEquals(5, fremenChat.getChoices().getFirst().size());
        }

        @Test
        void testSecondNextStepDrawsToDeckBAndEndsPhase() {
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
        void setUp() throws InvalidGameStateException {
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
        void testSecondNextStepGivesFremenButtons() {
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
        void testFremenPlacesSecondShaiHuludInFuneralPlainAndThirdNextStepGetsNewRideButtons() {
            fremenChat = new TestTopic();
            fremen.setChat(fremenChat);
            game.placeShaiHulud("Funeral Plain", "Shai-Hulud", false);
            spiceBlowAndNexus.nextStep(game);
            assertTrue(turnSummary.getMessages().get(2).contains("5 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!"));
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getFirst());
            assertEquals(5, fremenChat.getChoices().getFirst().size());
        }

        @Test
        void testFourthNextStepEndsPhase() {
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
        void setUp() throws InvalidGameStateException {
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
        void testSecondBlowDoesNotEndPhase() {
            spiceBlowAndNexus.resolveHarvester();
            assertFalse(spiceBlowAndNexus.nextStep(game));
            spiceBlowAndNexus.resolveHarvester();
            assertTrue(spiceBlowAndNexus.nextStep(game));
        }
    }
}
