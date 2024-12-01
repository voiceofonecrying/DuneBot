package model;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class MentatPauseTest extends DuneTest {
    private MentatPause mentatPause;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        mentatPause = new MentatPause();
        game.addFaction(choam);
    }

    @Test
    void testSpiceForAllyResetsToZero() {
        game.addFaction(richese);
        game.createAlliance(choam, richese);
        choam.setSpiceForAlly(5);
        assertEquals(5, choam.getSpiceForAlly());
        game.setTurn(1);
        mentatPause.startPhase(game);
        assertEquals(0, choam.getSpiceForAlly());
    }

    @Nested
    @DisplayName("#inflation")
    class Inflation {
        @Test
        void testChoamGetsInflationButtons() {
            game.setTurn(1);
            mentatPause.startPhase(game);
            assertEquals(1, choamChat.getMessages().size());
            assertEquals(1, choamChat.getChoices().size());
            assertEquals(3, choamChat.getChoices().getFirst().size());
        }

        @Test
        void testChoamDoesNotGetInflationButtons() throws InvalidGameStateException {
            game.setTurn(1);
            choam.setFirstInflation(ChoamInflationType.DOUBLE);
            game.setTurn(3);
            mentatPause.startPhase(game);
            assertEquals(0, choamChat.getMessages().size());
            assertEquals(0, choamChat.getChoices().size());
        }
    }

    @Nested
    @DisplayName("#decliningCharity")
    class DecliningCharity {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(emperor);
        }

        @Test
        public void testHasSpiceNoDeclineMessage() throws InvalidGameStateException {
            emperor.setDecliningCharity(true);
            mentatPause.startPhase(game);
            assertEquals(0, emperorChat.getMessages().size());
        }

        @Test
        public void testNotDecliningNoDeclineMessage() {
            emperor.subtractSpice(10, "Test");
            mentatPause.startPhase(game);
            assertEquals(0, emperorChat.getMessages().size());
        }

        @Test
        public void testGetsDeclineMessage() throws InvalidGameStateException {
            emperor.subtractSpice(10, "Test");
            emperor.setDecliningCharity(true);
            mentatPause.startPhase(game);
            assertEquals(1, emperorChat.getMessages().size());
            assertEquals("You have only 0 " + Emojis.SPICE + " but are declining CHOAM charity.\nYou must change this in your info channel if you want to receive charity. em", emperorChat.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#extortionAndTerrorTokenPlacementChoices")
    class ExtortionAndTerrorTokenPlacementChoices {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(fremen);
            game.addFaction(moritani);
            game.addFaction(richese);
            game.addFaction(bt);
            game.addFaction(guild);

            game.advanceTurn();
            game.advanceTurn();
            game.setStorm(14);
            game.triggerExtortionToken();
            mentatPause = game.getMentatPause();
        }

        @Test
        void moritaniAskedAboutPlacingTerrorToken() {
            mentatPause.startPhase(game);
            assertEquals(1, moritaniChat.getMessages().size());
            assertEquals("Use these buttons to place a Terror Token from your supply. mo", moritaniChat.getMessages().getFirst());
            assertEquals(7, moritaniChat.getChoices().getFirst().size());
            assertEquals("Move a Terror Token", moritaniChat.getChoices().getFirst().get(5).getLabel());
            assertTrue(moritaniChat.getChoices().getFirst().get(5).isDisabled());
        }

        @Test
        void moritaniAskedAboutPlacingAndMovingTerrorToken() {
            Territory carthag = game.getTerritory("Carthag");
            moritani.placeTerrorToken(carthag, "Atomics");
            Territory arrakeen = game.getTerritory("Arrakeen");
            moritani.placeTerrorToken(arrakeen, "Sabotage");
            mentatPause.startPhase(game);
            assertEquals(1, moritaniChat.getMessages().size());
            assertEquals("Use these buttons to place a Terror Token from your supply. mo", moritaniChat.getMessages().getFirst());
            assertEquals(7, moritaniChat.getChoices().getFirst().size());
            assertEquals("Move a Terror Token", moritaniChat.getChoices().getFirst().get(5).getLabel());
            assertFalse(moritaniChat.getChoices().getFirst().get(5).isDisabled());
        }

        @Test
        void moritaniNotAskedWhenOutOfTerrorTokensAndNoneOnMap() {
            moritani.getTerrorTokens().removeAll(moritani.getTerrorTokens());
            mentatPause.startPhase(game);
            assertTrue(moritani.getTerrorTokens().isEmpty());
            assertEquals(0, moritaniChat.getMessages().size());
            assertTrue(turnSummary.getMessages().contains(Emojis.MORITANI + " does not have Terror Tokens to place or move."));
        }

        @Test
        void moritaniAskedAboutMovingTerrorTokenEvenIfNoneInSupply() {
            Territory carthag = game.getTerritory("Carthag");
            moritani.placeTerrorToken(carthag, "Atomics");
            moritani.getTerrorTokens().removeAll(moritani.getTerrorTokens());
            mentatPause.startPhase(game);
            assertTrue(moritani.getTerrorTokens().isEmpty());
            assertEquals(1, moritaniChat.getMessages().size());
            assertEquals("Which Terror Token would you like to move to a new stronghold? mo", moritaniChat.getMessages().getFirst());
            assertEquals(1, moritaniChat.getChoices().size());
            assertEquals("Atomics from Carthag", moritaniChat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("No move", moritaniChat.getChoices().getFirst().get(1).getLabel());
        }

        @Test
        void testExtortionWillBeReturnedMessage() {
            mentatPause.startPhase(game);
            assertEquals("The Extortion token will be returned to " + Emojis.MORITANI + " unless someone pays 3 " + Emojis.SPICE + " to remove it from the game.", turnSummary.getMessages().get(1));
        }

        @Test
        void firstPlayerPays() {
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionWouldPayExtortion(game, guild);
            assertEquals(Emojis.GUILD + " pays 3 " + Emojis.SPICE + " to remove the Extortion token from the game.", turnSummary.messages.getFirst());
            assertEquals(2, guild.getSpice());
            assertEquals("-3 " + Emojis.SPICE + " " + Emojis.MORITANI + " Extortion " + "= 2 " + Emojis.SPICE, guildLedger.messages.getFirst());
            assertEquals(15, moritani.getSpice());
            assertEquals("+3 " + Emojis.SPICE + " " + Emojis.GUILD + " paid Extortion " + "= 15 " + Emojis.SPICE, moritaniLedger.messages.getFirst());
            game.getMentatPause().factionDeclinesExtortion(game, choam);
        }

        @Test
        void fourthOffersToPayThenfirstPlayerPays() {
            fremen.subtractSpice(3, "Test");
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionWouldPayExtortion(game, richese);
            game.getMentatPause().factionDeclinesExtortion(game, choam);
            game.getMentatPause().factionWouldPayExtortion(game, guild);
            assertEquals(Emojis.GUILD + " pays 3 " + Emojis.SPICE + " to remove the Extortion token from the game.", turnSummary.messages.getFirst());
            assertEquals(2, guild.getSpice());
            assertEquals("-3 " + Emojis.SPICE + " " + Emojis.MORITANI + " Extortion " + "= 2 " + Emojis.SPICE, guildLedger.messages.getFirst());
            assertEquals(15, moritani.getSpice());
            assertEquals("+3 " + Emojis.SPICE + " " + Emojis.GUILD + " paid Extortion " + "= 15 " + Emojis.SPICE, moritaniLedger.messages.getFirst());
        }

        @Test
        void fourthPlayerPays() {
            fremen.subtractSpice(3, "Test");
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionWouldPayExtortion(game, richese);
            game.getMentatPause().factionDeclinesExtortion(game, choam);
            game.getMentatPause().factionDeclinesExtortion(game, guild);
            assertEquals(Emojis.RICHESE + " pays 3 " + Emojis.SPICE + " to remove the Extortion token from the game.", turnSummary.messages.getFirst());
            assertEquals(2, richese.getSpice());
            assertEquals("-3 " + Emojis.SPICE + " " + Emojis.MORITANI + " Extortion " + "= 2 " + Emojis.SPICE, richeseLedger.messages.getFirst());
            assertEquals(15, moritani.getSpice());
            assertEquals("+3 " + Emojis.SPICE + " " + Emojis.RICHESE + " paid Extortion " + "= 15 " + Emojis.SPICE, moritaniLedger.messages.getFirst());
        }

        @Test
        void allDecline() {
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionDeclinesExtortion(game, fremen);
            game.getMentatPause().factionDeclinesExtortion(game, choam);
            game.getMentatPause().factionDeclinesExtortion(game, richese);
            game.getMentatPause().factionDeclinesExtortion(game, bt);
            game.getMentatPause().factionDeclinesExtortion(game, guild);
            assertEquals("No faction paid Extortion. The token returns to " + Emojis.MORITANI, turnSummary.messages.getFirst());
            assertEquals(12, moritani.getSpice());
            assertTrue(moritaniLedger.messages.isEmpty());
        }

        @Test
        void allButOneDeclineMoritaniPoor() {
            choam.addSpice(1, "Test");
            moritani.subtractSpice(10, "Test");
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionDeclinesExtortion(game, fremen);
            game.getMentatPause().factionDeclinesExtortion(game, choam);
            game.getMentatPause().factionDeclinesExtortion(game, richese);
            game.getMentatPause().factionDeclinesExtortion(game, bt);
            assertTrue(turnSummary.messages.isEmpty());
        }

        @Test
        void allDeclineFremenHas0Spice() {
            fremen.subtractSpice(3, "Test");
            mentatPause.startPhase(game);
            turnSummary.clear();
            game.getMentatPause().factionDeclinesExtortion(game, choam);
            game.getMentatPause().factionDeclinesExtortion(game, richese);
            game.getMentatPause().factionDeclinesExtortion(game, bt);
            game.getMentatPause().factionDeclinesExtortion(game, guild);
            assertEquals("No faction paid Extortion. The token returns to " + Emojis.MORITANI, turnSummary.messages.getFirst());
            assertEquals(12, moritani.getSpice());
            assertTrue(moritaniLedger.messages.isEmpty());
        }
    }
}
