package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoritaniFactionTest extends FactionTestTemplate {
    private MoritaniFaction faction;

    Territory arrakeen;
    Territory carthag;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new MoritaniFaction("player", "player");
        commonPostInstantiationSetUp();

        arrakeen = game.getTerritory("Arrakeen");
        carthag = game.getTerritory("Carthag");
    }

    @Test
    public void testInitialSpice() {
        assertEquals(12, faction.getSpice());
    }

    @Test
    public void testFreeRevival() {
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalAlliedToFremen() {
        faction.setAlly("Fremen");
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThreshold() {
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(20, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            assertEquals(0, territory.countFactions());
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.MORITANI);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Nested
    @DisplayName("#assassinateLeader")
    class AssassinateLeader {
        BTFaction bt;
        TestTopic btChat;
        TestTopic btLedger;
        TestTopic turnSummary;

        @BeforeEach
        public void setUp() throws IOException {
            bt = new BTFaction("p", "u");
            btChat = new TestTopic();
            bt.setChat(btChat);
            btLedger = new TestTopic();
            bt.setLedger(btLedger);
            game.addFaction(bt);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
        }

        @Test
        public void testFactionHasLeader() {
            Leader wykk = bt.getLeader("Wykk").orElseThrow();
            assertDoesNotThrow(() -> faction.assassinateLeader(bt, wykk));
            assertEquals(Emojis.MORITANI + " collect 2 " + Emojis.SPICE + " by assassinating Wykk!", turnSummary.getMessages().getFirst());
            assertEquals("+2 " + Emojis.SPICE + " assassination of Wykk = 14 " + Emojis.SPICE, ledger.getMessages().getFirst());
        }

        @Test
        public void testLeaderIsInTanks() {
            Leader wykk = bt.getLeader("Wykk").orElseThrow();
            game.killLeader(bt, wykk.getName());
            assertThrows(IllegalArgumentException.class, () -> faction.assassinateLeader(bt, wykk));
        }

        @Test
        public void testZoalEarns3Spice() {
            Leader zoal = bt.getLeader("Zoal").orElseThrow();
            assertEquals(12, faction.getSpice());
            assertDoesNotThrow(() -> faction.assassinateLeader(bt, zoal));
            assertEquals(15, faction.getSpice());
            assertEquals(Emojis.MORITANI + " collect 3 " + Emojis.SPICE + " by assassinating Zoal!", turnSummary.getMessages().getFirst());
            assertEquals("+3 " + Emojis.SPICE + " assassination of Zoal = 15 " + Emojis.SPICE, ledger.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#checkForTerrorTrigger")
    class CheckForTerrorTrigger {
        AtreidesFaction atreides;
        TestTopic turnSummary;

        @BeforeEach
        void setUp() throws IOException, InvalidGameStateException {
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            atreides = new AtreidesFaction("p", "u");
            carthag.addTerrorToken(game, "Sabotage");
        }

        @Test
        void testMoritaniDoesNotTrigger() {
            faction.checkForTerrorTrigger(carthag, faction, 3);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(chat.getMessages().isEmpty());
        }

        @Test
        void testOtherFactionTriggers() {
            faction.checkForTerrorTrigger(carthag, atreides, 3);
            assertEquals(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + Emojis.ATREIDES, turnSummary.getMessages().getFirst());
            assertEquals("Will you trigger your Terror Token in Carthag? player", chat.getMessages().getFirst());
            assertEquals(3, chat.getChoices().getFirst().size());
        }

        @Test
        void testAllyDoesNotTrigger() {
            faction.setAlly("Atreides");
            atreides.setAlly("Moritani");
            faction.checkForTerrorTrigger(carthag, atreides, 3);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(chat.getMessages().isEmpty());
        }

        @Test
        void testCannotTriggerAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(faction.isHighThreshold);
            faction.removeReserves(13);
            assertEquals("Grumman has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertFalse(faction.isHighThreshold);
            faction.checkForTerrorTrigger(carthag, atreides, 2);
            assertEquals(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time", turnSummary.getMessages().get(1));
            assertTrue(chat.getMessages().isEmpty());
        }
    }

    @Nested
    @DisplayName("#robberyTerrorToken")
    class RobberyTerrorToken {
        TestTopic turnSummary;
        AtreidesFaction atreides;

        @BeforeEach
        void setUp() throws IOException {
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            atreides = new AtreidesFaction("p", "u");
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
        }

        @Test
        void testRobberyRobTakesHalfSpiceRoundedUp() {
            int moritaniStartingSpice = 12;
            assertEquals(moritaniStartingSpice, faction.getSpice());
            atreides.addSpice(1, "Test setup");
            int atreidesStartingSpice = 11;
            assertEquals(atreidesStartingSpice, atreides.getSpice());

            faction.robberyRob("Atreides");
            assertEquals(atreidesStartingSpice - 6, atreides.getSpice());
            assertEquals(moritaniStartingSpice + 6, faction.getSpice());
            assertEquals(Emojis.MORITANI + " stole 6 " + Emojis.SPICE + " from " + Emojis.ATREIDES + " with Robbery" , turnSummary.getMessages().getFirst());
            assertEquals(ledger.getMessages().getFirst(), "+6 " + Emojis.SPICE + " stolen from " + Emojis.ATREIDES + " with Robbery = 18 " + Emojis.SPICE);
        }

        @Test
        void testRobberyRobFactionHasNoSpice() {
            int moritaniStartingSpice = 12;
            assertEquals(moritaniStartingSpice, faction.getSpice());
            atreides.subtractSpice(10, "Test setup");
            int atreidesStartingSpice = 0;
            assertEquals(atreidesStartingSpice, atreides.getSpice());

            faction.robberyRob("Atreides");
            assertEquals(atreidesStartingSpice, atreides.getSpice());
            assertEquals(moritaniStartingSpice, faction.getSpice());
            assertEquals(Emojis.MORITANI + " stole 0 " + Emojis.SPICE + " from " + Emojis.ATREIDES + " with Robbery" , turnSummary.getMessages().getFirst());
            assertEquals(ledger.getMessages().getFirst(),  Emojis.ATREIDES + " had no " + Emojis.SPICE + " to steal");
        }

        @Test
        void testRobberyDrawEmptyHand() {
            assertTrue(faction.getTreacheryHand().isEmpty());

            faction.robberyDraw();
            assertEquals(1, faction.getTreacheryHand().size());
            assertEquals(0, chat.getMessages().size());
            assertEquals(1, ledger.getMessages().size());
            assertEquals(Emojis.MORITANI + " has drawn a " + Emojis.TREACHERY + " card with Robbery.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testRobberyDrawFullHand() {
            assertEquals(4, faction.getHandLimit());
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            assertEquals(faction.getHandLimit(), faction.getTreacheryHand().size());

            assertDoesNotThrow(() -> faction.robberyDraw());
            assertEquals(5, faction.getHandLimit());
            assertEquals(5, faction.getTreacheryHand().size());
            assertEquals(1, chat.getMessages().size());
            assertEquals(5, chat.getChoices().getFirst().size());
            assertEquals(1, ledger.getMessages().size());
            assertEquals(Emojis.MORITANI + " has drawn a " + Emojis.TREACHERY + " card with Robbery.", turnSummary.getMessages().getFirst());
            assertTrue(game.isRobberyDiscardOutstanding());
        }

        @Test
        void testRobberyDrawEmptyHandEmptyTreacheryDeck() {
            LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
            List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
            treacheryDiscard.addAll(treacheryDeck);
            treacheryDeck.clear();

            assertDoesNotThrow(() -> faction.robberyDraw());
            assertEquals(1, faction.getTreacheryHand().size());
            assertEquals(0, chat.getMessages().size());
            assertEquals(1, ledger.getMessages().size());
            assertEquals("The " + Emojis.TREACHERY + " deck was empty and has been replenished from the discard pile.", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.MORITANI + " has drawn a " + Emojis.TREACHERY + " card with Robbery.", turnSummary.getMessages().get(1));
        }

        @Test
        void testRobberyDrawFullHandEmptyTreacheryDeck() {
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            LinkedList<TreacheryCard> treacheryDeck = game.getTreacheryDeck();
            List<TreacheryCard> treacheryDiscard = game.getTreacheryDiscard();
            treacheryDiscard.addAll(treacheryDeck);
            treacheryDeck.clear();
            assertEquals(faction.getHandLimit(), faction.getTreacheryHand().size());

            assertDoesNotThrow(() -> faction.robberyDraw());
            assertEquals(5, faction.getHandLimit());
            assertEquals(5, faction.getTreacheryHand().size());
            assertEquals(1, chat.getMessages().size());
            assertEquals(5, chat.getChoices().getFirst().size());
            assertEquals(1, ledger.getMessages().size());
            assertEquals(Emojis.MORITANI + " has drawn a " + Emojis.TREACHERY + " card with Robbery.", turnSummary.getMessages().get(1));
            assertTrue(game.isRobberyDiscardOutstanding());
        }

        @Test
        void testDiscardFromOverFullHand() {
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            game.drawTreacheryCard("Moritani", false, false);
            int turnSummarySize = turnSummary.getMessages().size();
            faction.robberyDraw();
            assertEquals(5, faction.getTreacheryHand().size());
            assertEquals(5, faction.getHandLimit());
            assertEquals(1, ledger.getMessages().size());
            int discardSize = game.getTreacheryDiscard().size();
            assertTrue(game.isRobberyDiscardOutstanding());

            faction.robberyDiscard(faction.getTreacheryHand().getFirst().name());
            assertEquals(4, faction.getTreacheryHand().size());
            assertEquals(4, faction.getHandLimit());
            assertEquals(2, ledger.getMessages().size());
            assertEquals(turnSummarySize + 2, turnSummary.getMessages().size());
            assertEquals(discardSize + 1, game.getTreacheryDiscard().size());
            assertFalse(game.isRobberyDiscardOutstanding());
        }
    }

    @Nested
    @DisplayName("#triggerTerrorToken")
    class TriggerTerrorToken {
        AtreidesFaction atreides;

        @BeforeEach
        void setUp() throws InvalidGameStateException, IOException {
            atreides = new AtreidesFaction("p", "u");
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
            faction.placeTerrorToken(carthag, "Robbery");
            chat.clear();
        }

        @Test
        void testTerrorTokenTriggered() {
            faction.triggerTerrorToken(atreides, carthag, "Robbery");
            assertEquals(Emojis.MORITANI + " have triggered their Robbery Terror Token in Carthag against " + Emojis.ATREIDES + "!", turnSummary.getMessages().getLast());
            assertFalse(carthag.hasTerrorToken());
            assertFalse(faction.getTerrorTokens().contains("Robbery"));
            assertEquals("You have triggered your Robbery Terror Token in Carthag against " + Emojis.ATREIDES + ".", chat.getMessages().getFirst());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testTerrorTokenNotPresent() {
            assertThrows(IllegalArgumentException.class, () -> faction.triggerTerrorToken(atreides, carthag, "Sabotage"));
        }
    }

    @Nested
    @DisplayName("triggerAtomics")
    class TriggerAtomics {
        AtreidesFaction atreides;
        HarkonnenFaction harkonnen;

        @BeforeEach
        void setUp() throws IOException, InvalidGameStateException {
            atreides = new AtreidesFaction("p", "u");
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
            harkonnen = new HarkonnenFaction("p", "u");
            harkonnen.setLedger(new TestTopic());
            game.addFaction(harkonnen);
            faction.placeTerrorToken(arrakeen, "Atomics");
            turnSummary.clear();
        }

        @Test
        void testAtomicsKillsForcesAndReducesHandLimit() {
            faction.triggerTerrorToken(harkonnen, arrakeen, "Atomics");
            assertTrue(arrakeen.isAftermathToken());
            assertEquals(Emojis.MORITANI + " have triggered their Atomics Terror Token in Arrakeen against " + Emojis.HARKONNEN + "!", turnSummary.getMessages().getFirst());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals("10 " + Emojis.ATREIDES_TROOP + " in Arrakeen were sent to the tanks.")));
            assertEquals(3, faction.getHandLimit());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.MORITANI + " " + Emojis.TREACHERY + " limit has been reduced to 3.")));
        }

        @Test
        void testMoritaniHadFourCards() {
            faction.addTreacheryCard(new TreacheryCard("Shield"));
            faction.addTreacheryCard(new TreacheryCard("Shield"));
            faction.addTreacheryCard(new TreacheryCard("Shield"));
            faction.addTreacheryCard(new TreacheryCard("Shield"));
            assertEquals(4, faction.getTreacheryHand().size());
            faction.triggerTerrorToken(harkonnen, arrakeen, "Atomics");
            assertEquals(3, faction.getTreacheryHand().size());
            assertEquals( Emojis.MORITANI + " discards Shield.", turnSummary.getMessages().getLast());
        }

        @Test
        void testAllyHandSizeReduces() {
            game.createAlliance(faction, harkonnen);
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            harkonnen.addTreacheryCard(new TreacheryCard("Shield"));
            assertEquals(8, harkonnen.getTreacheryHand().size());
            faction.triggerTerrorToken(harkonnen, arrakeen, "Atomics");
            assertEquals(7, harkonnen.getHandLimit());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.HARKONNEN + " " + Emojis.TREACHERY + " limit has been reduced to 7.")));
            assertEquals(7, harkonnen.getTreacheryHand().size());
            assertEquals( Emojis.HARKONNEN + " discards Shield.", turnSummary.getMessages().getLast());
        }

        @Test
        void testOldAllyHandLimitRestored() {
            game.createAlliance(faction, harkonnen);
            faction.triggerTerrorToken(harkonnen, arrakeen, "Atomics");
            game.createAlliance(faction, atreides);
            assertEquals(8, harkonnen.getHandLimit());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.HARKONNEN + " " + Emojis.TREACHERY + " limit has been restored to 8.")));
        }

        @Test
        void testNewAllyHandLimitReduced() {
            game.createAlliance(faction, harkonnen);
            faction.triggerTerrorToken(harkonnen, arrakeen, "Atomics");
            game.createAlliance(faction, atreides);
            assertEquals(3, atreides.getHandLimit());
            assertTrue(turnSummary.getMessages().stream().anyMatch(m -> m.equals(Emojis.ATREIDES + " " + Emojis.TREACHERY + " limit has been reduced to 3.")));
        }

        @Test
        void testNoAtomicsNoChangesToAllyHandLimits() {
            game.createAlliance(faction, harkonnen);
            game.createAlliance(faction, atreides);
            assertEquals(8, harkonnen.getHandLimit());
            assertEquals(4, atreides.getHandLimit());
        }
    }

    @Nested
    @DisplayName("#highTresholdTerrorTokenPlacement")
    class HighTresholdTerrorTokenPlacement {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            faction.placeTerrorToken(arrakeen, "Sabotage");
            faction.placeTerrorToken(carthag, "Robbery");
        }

        @Test
        void testNonHomeworlds() {
            faction.sendTerrorTokenLocationMessage();
            assertEquals("Where would you like to place a Terror Token? player", chat.getMessages().getLast());
            DuneChoice arrakeenChoice = chat.getChoices().getLast().stream().filter(dc -> dc.getLabel().equals("Arrakeen")).findFirst().orElseThrow();
            assertTrue(arrakeenChoice.isDisabled());
        }

        @Test
        void testHomeworldsHighThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.sendTerrorTokenLocationMessage();
            assertEquals("Where would you like to place a Terror Token? player\nYou are at High Threshold and may place in a stronghold that has one.\n- Sabotage is in Arrakeen\n- Robbery is in Carthag", chat.getMessages().getLast());
            DuneChoice arrakeenChoice = chat.getChoices().getLast().stream().filter(dc -> dc.getLabel().equals("Arrakeen")).findFirst().orElseThrow();
            assertFalse(arrakeenChoice.isDisabled());
        }

        @Test
        void testHomeworldsLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.placeForceFromReserves(game, carthag, 13, false);
            faction.sendTerrorTokenLocationMessage();
            assertEquals("Where would you like to place a Terror Token? player", chat.getMessages().getLast());
            DuneChoice arrakeenChoice = chat.getChoices().getLast().stream().filter(dc -> dc.getLabel().equals("Arrakeen")).findFirst().orElseThrow();
            assertTrue(arrakeenChoice.isDisabled());
        }

        @Test
        void testRemovedTerrorTokenWithHT() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertFalse(faction.isRemovedTerrorTokenWithHT());
            faction.removeTerrorTokenWithHighThreshold("Carthag", "Robbery");
            assertTrue(faction.isRemovedTerrorTokenWithHT());
            faction.sendTerrorTokenLocationMessage();
            assertEquals("Where would you like to place a Terror Token? player", chat.getMessages().getLast());
            DuneChoice arrakeenChoice = chat.getChoices().getLast().stream().filter(dc -> dc.getLabel().equals("Arrakeen")).findFirst().orElseThrow();
            assertTrue(arrakeenChoice.isDisabled());
        }
    }

    @Nested
    @DisplayName("#placeTerrorToken")
    class PlaceTerrorToken {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            faction.placeTerrorToken(carthag, "Robbery");
        }

        @Test
        void testTerrorTokenIsPlaced() throws InvalidGameStateException {
            faction.placeTerrorToken(arrakeen, "Sabotage");
            assertFalse(faction.getTerrorTokens().contains("Sabotage"));
            assertTrue(arrakeen.hasTerrorToken("Sabotage"));
            assertEquals("A " + Emojis.MORITANI + " Terror Token was placed in Arrakeen.", turnSummary.getMessages().getLast());
            assertEquals("Sabotage Terror Token was placed in Arrakeen.", chat.getMessages().getLast());
            assertEquals("Sabotage Terror Token was placed in Arrakeen.", ledger.getMessages().getLast());
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testSecondTerrorTokenNotAllowed() {
            assertThrows(InvalidGameStateException.class, () -> faction.placeTerrorToken(carthag, "Sabotage"));
        }

        @Test
        void testSecondTerrorTokenIsPlacedWithHighThreshold() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            turnSummary.clear();
            faction.placeTerrorToken(carthag, "Sabotage");
            assertFalse(faction.getTerrorTokens().contains("Sabotage"));
            assertTrue(carthag.hasTerrorToken("Sabotage"));
            assertEquals("A " + Emojis.MORITANI + " Terror Token was placed in Carthag with Grumman High Treshold ability.", turnSummary.getMessages().getFirst());
            assertEquals("Sabotage Terror Token was placed in Carthag.", chat.getMessages().getLast());
            assertEquals("Sabotage Terror Token was placed in Carthag.", ledger.getMessages().getLast());
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testSecondTerrorTokenNotAllowedAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.placeForceFromReserves(game, carthag, 13, false);
            assertFalse(faction.isHighThreshold());
            assertThrows(InvalidGameStateException.class, () -> faction.placeTerrorToken(carthag, "Sabotage"));
        }

        @Test
        void testMoritaniDoesNotHaveTerrorToken() {
            assertThrows(IllegalArgumentException.class, () -> faction.placeTerrorToken(arrakeen, "Robbery"));
        }
    }

    @Nested
    @DisplayName("#moveTerrorToken")
    class MoveTerrorToken {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            faction.placeTerrorToken(arrakeen, "Sabotage");
            faction.getUpdateTypes().clear();
        }

        @Test
        void testTerrorTokenMoved() throws InvalidGameStateException {
            faction.moveTerrorToken(carthag, "Sabotage");
            assertTrue(carthag.hasTerrorToken("Sabotage"));
            assertFalse(arrakeen.hasTerrorToken());
            assertEquals("The " + Emojis.MORITANI + " Terror Token in Arrakeen was moved to Carthag.", turnSummary.getMessages().getLast());
            assertEquals("Sabotage Terror Token was moved to Carthag from Arrakeen.", chat.getMessages().getLast());
            assertEquals("Sabotage Terror Token was moved to Carthag from Arrakeen.", ledger.getMessages().getLast());
            assertFalse(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }

        @Test
        void testTerrorTokenNotFoundInTerritory() {
            assertThrows(IllegalArgumentException.class, () -> faction.moveTerrorToken(carthag, "Robbery"));
        }
    }

    @Nested
    @DisplayName("#removeTerrorTokenWithHighThreshold")
    class RemoveTerrorTokenWithHighThreshold {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            faction.placeTerrorToken(arrakeen, "Robbery");
            assertTrue(arrakeen.hasTerrorToken());
            faction.getUpdateTypes().clear();
            game.getUpdateTypes().clear();
        }

        @Test
        void testTerrorTokenRemovedWitHighThreshold() {
            faction.removeTerrorTokenWithHighThreshold("Arrakeen", "Robbery");
            assertFalse(arrakeen.hasTerrorToken());
            assertEquals(Emojis.MORITANI + " has removed a Terror Token from Arrakeen for 4 " + Emojis.SPICE, turnSummary.getMessages().getLast());
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
            assertTrue(faction.getTerrorTokens().contains("Robbery"));
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertEquals(16, faction.getSpice());
        }

        @Test
        void testTerrorTokenNotFoundInTerritory() {
            assertThrows(IllegalArgumentException.class, () -> faction.removeTerrorTokenWithHighThreshold("Arrakeen", "Sabotage"));
        }

        @Test
        void testDontRemoveTerrorToken() {
            faction.removeTerrorTokenWithHighThreshold("", "None");
            assertEquals("You will not remove a Terror Token.", chat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#performMentatPauseActions")
    class PerformMentatPauseActions extends FactionTestTemplate.PerformMentatPauseActions {
        @BeforeEach
        void setUp() {
            faction = getFaction();
        }

        @Test
        @Override
        void testCanPayToRemoveExtortion() {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(3, "Test");
            faction.performMentatPauseActions(true);
            assertEquals(1, chat.getMessages().size());
            assertFalse(chat.getMessages().getFirst().contains("Extortion token"));
        }

        @Test
        @Override
        void testCannotPayToRemoveExtortion() {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.performMentatPauseActions(true);
            assertEquals(1, chat.getMessages().size());
            assertFalse(chat.getMessages().getFirst().contains("to pay Extortion"));
        }

        @Test
        void testTerrorTokenMessage() {
            faction.performMentatPauseActions(false);
            assertEquals("Where would you like to place a Terror Token? " + faction.getPlayer(), chat.getMessages().getFirst());
            assertEquals(7, chat.getChoices().getFirst().size());
        }

        @Test
        void testAssassinationNewLeaderNeeded() {
            MoritaniFaction moritani = ((MoritaniFaction) faction);
            moritani.setNewAssassinationTargetNeeded(true);
            faction.performMentatPauseActions(false);
            assertFalse(moritani.isNewAssassinationTargetNeeded());
            assertEquals(1, turnSummary.getMessages().size());
            assertEquals(Emojis.MORITANI + " has drawn a new traitor.", turnSummary.getMessages().getFirst());
        }
    }
}
