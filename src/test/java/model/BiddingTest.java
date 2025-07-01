package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BiddingTest extends DuneTest {
    private Bidding bidding;
    private TestTopic biddingPhase;

    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
        game.setModLedger(new TestTopic());
        biddingPhase = new TestTopic();
        game.setBiddingPhase(biddingPhase);
    }

    @Nested
    @DisplayName("#declaration")
    public class Declaration {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
        }

        @Test
        public void testOriginalSixFactions() throws InvalidGameStateException {
            game.addFaction(harkonnen);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(6, bidding.getMarket().size());
            assertEquals(1, modInfo.getMessages().size());
            assertFalse(bidding.isIxRejectOutstanding());
            assertFalse(bidding.isRicheseCacheCardOutstanding());
        }

        @Test
        public void testIxInGame() throws InvalidGameStateException {
            game.addFaction(ix);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(7, bidding.getMarket().size());
            assertEquals("If anyone wants to Karama " + Emojis.IX + " bidding advantage, that must be done now.",
                    modInfo.getMessages().getFirst());
            assertFalse(bidding.isMarketShownToIx());
            assertFalse(bidding.isRicheseCacheCardOutstanding());

            int deckSize = game.getTreacheryDeck().size();
            bidding.blockIxBiddingAdvantage(game);
            assertEquals(6, bidding.getMarket().size());
            assertTrue(bidding.isMarketShownToIx());
            assertEquals(deckSize + 1, game.getTreacheryDeck().size());
            assertEquals(Emojis.IX + " have been blocked from using their bidding advantage.\n6 cards will be pulled from the " + Emojis.TREACHERY + " deck for bidding.",
                    turnSummary.getMessages().getLast());
        }

        @Test
        public void testRicheseInGame() throws InvalidGameStateException {
            game.addFaction(richese);
            bidding = game.startBidding();
            modInfo.clear();
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(5, bidding.getMarket().size());
            assertEquals(1, modInfo.getMessages().size());
            assertFalse(bidding.isIxRejectOutstanding());
            assertTrue(bidding.isRicheseCacheCardOutstanding());
        }

        @Test
        public void testRicheseCanSellBlackMarket() {
            game.addFaction(richese);
            richese.addTreacheryCard(kulon);
            bidding = game.startBidding();
            modInfo.clear();
            assertThrows(InvalidGameStateException.class, () -> bidding.cardCountsInBiddingPhase(game));
            try {
                bidding.cardCountsInBiddingPhase(game);
            } catch (InvalidGameStateException e) {
                assertEquals("Richese must decide on black market before advancing.", e.getMessage());
            }
        }

        @Test
        public void testRicheseDoesNotSellBlackMarket() {
            game.addFaction(richese);
            richese.addTreacheryCard(kulon);
            bidding = game.startBidding();
            modInfo.clear();
            bidding.setBlackMarketDecisionInProgress(false);
            assertDoesNotThrow(() -> bidding.cardCountsInBiddingPhase(game));
        }

        @Test
        public void testRicheseSellsBlackMarket() throws InvalidGameStateException {
            game.addFaction(richese);
            richese.addTreacheryCard(kulon);
            bidding = game.startBidding();
            modInfo.clear();
            bidding.blackMarketAuction(game, "Kulon", "Silent");
            assertThrows(InvalidGameStateException.class, () -> bidding.cardCountsInBiddingPhase(game));
            try {
                bidding.cardCountsInBiddingPhase(game);
            } catch (InvalidGameStateException e) {
                assertEquals("The black market card must be awarded before advancing.", e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("#finishBiddingPhase")
    public class FinishBiddingPhase {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
        }

        @Test
        public void testRicheseDecidingOnCacheCard() throws InvalidGameStateException {
            game.addFaction(richese);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            assertTrue(bidding.isCacheCardDecisionInProgress());
            assertThrows(InvalidGameStateException.class, () -> bidding.finishBiddingPhase(game));
            try {
                bidding.finishBiddingPhase(game);
            } catch (InvalidGameStateException e) {
                assertEquals("Richese must decide on their cache card.", e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("#prescienceBlockedBeforeCardIsAuctioned")
    public class PrescienceBlockedBeforeCardIsAuctioned {
        TestTopic atreidesAllianceThread;

        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);

            game.createAlliance(atreides, emperor);
            atreides.setGrantingAllyTreacheryPrescience(true);
            atreidesAllianceThread = new TestTopic();
            atreides.setAllianceThread(atreidesAllianceThread);

            game.addGameOption(GameOption.HOMEWORLDS);
            Territory caladan = game.getTerritory("Caladan");
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 5);
            assertTrue(atreides.isHomeworldOccupied());

            atreides.setCardPrescienceBlocked(true);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            bidding.auctionNextCard(game, false);
        }

        @Test
        public void testAtreidesInformedPrescienceWasBlocked() {
            assertEquals("Your " + Emojis.TREACHERY + " Prescience was blocked by Karama.", atreidesChat.getMessages().getLast());
            assertFalse(atreides.isCardPrescienceBlocked());
        }

        @Test
        public void testAtreidesAllyInformedPrescienceWasBlocked() {
            assertEquals(Emojis.ATREIDES + " " + Emojis.TREACHERY + " Prescience was blocked by Karama.", atreidesAllianceThread.getMessages().getLast());
            assertFalse(atreides.isCardPrescienceBlocked());
        }

        @Test
        public void testCaladanOccupierInformedPrescienceWasBlocked() {
            assertEquals("Your " + Emojis.ATREIDES + " subjects in Caladan " + Emojis.TREACHERY + " Prescience was blocked by Karama.", harkonnenChat.getMessages().getLast());
            assertFalse(atreides.isCardPrescienceBlocked());
        }
    }

    @Nested
    @DisplayName("#prescienceBlockedWhenCardIsAuctioned")
    public class PrescienceBlockedWhenCardIsAuctioned {
        TestTopic atreidesAllianceThread;

        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);

            game.createAlliance(atreides, emperor);
            atreides.setGrantingAllyTreacheryPrescience(true);
            atreidesAllianceThread = new TestTopic();
            atreides.setAllianceThread(atreidesAllianceThread);

            game.addGameOption(GameOption.HOMEWORLDS);
            Territory caladan = game.getTerritory("Caladan");
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 5);
            assertTrue(atreides.isHomeworldOccupied());

            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            bidding.auctionNextCard(game, true);
        }

        @Test
        public void testAtreidesInformedPrescienceWasBlocked() {
            assertEquals("Your " + Emojis.TREACHERY + " Prescience was blocked by Karama.", atreidesChat.getMessages().getLast());
        }

        @Test
        public void testAtreidesAllyInformedPrescienceWasBlocked() {
            assertEquals(Emojis.ATREIDES + " " + Emojis.TREACHERY + " Prescience was blocked by Karama.", atreidesAllianceThread.getMessages().getLast());
        }

        @Test
        public void testCaladanOccupierInformedPrescienceWasBlocked() {
            assertEquals("Your " + Emojis.ATREIDES + " subjects in Caladan " + Emojis.TREACHERY + " Prescience was blocked by Karama.", harkonnenChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#sendAtreidesCardPrescience")
    class SendAtreidesCardPrescience {
        Bidding bidding;
        TestTopic atreidesAllianceThread;

        @BeforeEach
        void setUp() {
            game.addFaction(atreides);
            game.addFaction(emperor);
            game.addFaction(harkonnen);
            bidding = game.startBidding();
            game.createAlliance(atreides, emperor);
            atreidesAllianceThread = new TestTopic();
            atreides.setAllianceThread(atreidesAllianceThread);

            game.addGameOption(GameOption.HOMEWORLDS);
            Territory caladan = game.getTerritory("Caladan");
            caladan.removeForces(game, "Atreides", 10);
            caladan.addForces("Harkonnen", 5);
            assertTrue(atreides.isHomeworldOccupied());

            bidding.setBidCard(game, shield);
        }

        @Test
        void testAtreidesGetsCardPrescience() {
            bidding.sendAtreidesCardPrescience(game, false);
            assertEquals("You predict " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY + " is up for bid (R0:C0).", atreidesChat.getMessages().getLast());
        }

        @Test
        void testAtreidesNotGrantingPrescienceToAlly() {
            bidding.sendAtreidesCardPrescience(game, false);
            assertTrue(atreidesAllianceThread.getMessages().isEmpty());
        }

        @Test
        void testAtreidesIsGrantingPrescienceToAlly() {
            atreides.setGrantingAllyTreacheryPrescience(true);
            bidding.sendAtreidesCardPrescience(game, false);
            assertEquals(Emojis.ATREIDES + " predicts " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY + " is up for bid (R0:C0).", atreidesAllianceThread.getMessages().getLast());
        }

        @Test
        void testCaladanOccupierGetsCardPrescience() {
            bidding.sendAtreidesCardPrescience(game, false);
            assertEquals("Your " + Emojis.ATREIDES + " subjects in Caladan predict " + Emojis.TREACHERY + " Shield " + Emojis.TREACHERY + " is up for bid (R0:C0).", harkonnenChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#assignAndPayForCard")
    public class AssignAndPayForCard {
        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            game.createAlliance(atreides, emperor);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
            bidding.auctionNextCard(game, false);
            turnSummary.clear();
        }

        @Test
        public void testWinnerPaysAll() throws InvalidGameStateException {
            bidding.assignAndPayForCard(game, "Atreides", "Emperor", 3, false);
            assertEquals(7, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyPaysAll() throws InvalidGameStateException {
            emperor.setSpiceForAlly(5);
            bidding.assignAndPayForCard(game, "Atreides", "Emperor", 3, false);
            assertEquals(10, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE + " (3 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyPaysAll2() throws InvalidGameStateException {
            assertEquals(10, atreides.getSpice());
            emperor.setSpiceForAlly(5);
            bidding.assignAndPayForCard(game, "Atreides", "Emperor", 11, false);
            assertEquals(4, atreides.getSpice());
            assertEquals(16, emperor.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 11 " + Emojis.SPICE + " (5 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.EMPEROR + " is paid 11 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyBuysCardCantAffordToSupport() throws InvalidGameStateException {
            emperor.setSpiceForAlly(5);
            bidding.assignAndPayForCard(game, "Emperor", "Bank", 10, false);
            assertEquals(0, emperor.getSpice());
            assertEquals(0, emperor.getSpiceForAlly());
            bidding.auctionNextCard(game, false);
            assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 11, false, false));
        }

        @Test
        public void testAllyMakesBribeCantAffordToSupport() throws InvalidGameStateException {
            emperor.setSpiceForAlly(5);
            emperor.bribe(game, harkonnen, 10, "For test");
            assertEquals(0, emperor.getSpice());
            assertEquals(0, emperor.getSpiceForAlly());
            assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 11, false, false));
        }

        @Test
        public void testAllyPaysPart() throws InvalidGameStateException {
            emperor.setSpiceForAlly(2);
            bidding.assignAndPayForCard(game, "Atreides", "Emperor", 3, false);
            assertEquals(9, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE + " (2 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testHarkonnenDrawsBonusCard() throws InvalidGameStateException {
            bidding.pass(game, atreides);
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, guild);
            bidding.bid(game, harkonnen, true, 3, false, false);
            bidding.pass(game, atreides);
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, guild);
            bidding.awardTopBidder(game, false);
            assertEquals(Emojis.HARKONNEN + " wins R0:C1 for 3 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            assertEquals(Emojis.HARKONNEN + " draws another card from the " + Emojis.TREACHERY + " deck.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testHarkonnenBonusBlockedByKarama() throws InvalidGameStateException {
            bidding.pass(game, atreides);
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, guild);
            bidding.bid(game, harkonnen, true, 3, false, false);
            bidding.pass(game, atreides);
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, guild);
            bidding.awardTopBidder(game, true);
            assertEquals(Emojis.HARKONNEN + " wins R0:C1 for 3 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            assertEquals(Emojis.HARKONNEN + " bonus card was blocked by Karama.", turnSummary.getMessages().getLast());
            assertFalse(turnSummary.getMessages().getLast().contains("draws another card"));
        }
    }

    @Nested
    @DisplayName("#topBidderIdentified")
    public class TopBidderIdentified {
        int numMessagesInBiddingPhaseChannel;
        int expectedMessagesInBiddingPhase;

        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);

            bidding = game.startBidding();
            assertTrue(bidding.isRicheseCacheCardOutstanding());

            assertNull(bidding.getBidCard());
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(5, bidding.getMarket().size());
        }

        @Nested
        @DisplayName("#normalBidding")
        public class NormalBidding {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.setCacheCardDecisionInProgress(false);
                bidding.auctionNextCard(game, false);
                assertEquals(6, bidding.getEligibleBidOrder(game).size());

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                bidding.pass(game, richese);
                numMessagesInBiddingPhaseChannel = biddingPhase.getMessages().size();
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(numMessagesInBiddingPhaseChannel, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(numMessagesInBiddingPhaseChannel, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#normalBiddingAllPass")
        public class NormalBiddingAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.setCacheCardDecisionInProgress(false);
                bidding.auctionNextCard(game, false);
                assertEquals(6, bidding.getEligibleBidOrder(game).size());

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                bidding.pass(game, richese);
                numMessagesInBiddingPhaseChannel = biddingPhase.getMessages().size();
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(numMessagesInBiddingPhaseChannel, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(numMessagesInBiddingPhaseChannel, biddingPhase.getMessages().size());
            }

            @Test
            public void testMarketAndBidCardSentBackToDeck() {
                bidding.setCacheCardDecisionInProgress(false);
                bidding.setRicheseCacheCardOutstanding(false);
                assertThrows(InvalidGameStateException.class, () -> bidding.auctionNextCard(game, false));
                int treacheryDeckSize = game.getTreacheryDeck().size();
                int marketSize = bidding.getMarket().size();
                assertNotNull(bidding.getBidCard());
                assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
                assertEquals(28, treacheryDeckSize);
                assertEquals(4, marketSize);
                assertEquals(0, bidding.getMarket().size());
                assertEquals("All players passed. 5 cards were returned to top of the Treachery Deck.",
                        turnSummary.getMessages().getLast());
                assertEquals(1 + marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
            }

            @Test
            public void testMarketAndBidCardSentBackToDeckRicheseCardRemaining() {
                bidding.setCacheCardDecisionInProgress(false);
                assertThrows(InvalidGameStateException.class, () -> bidding.auctionNextCard(game, false));
                int treacheryDeckSize = game.getTreacheryDeck().size();
                int marketSize = bidding.getMarket().size();
                assertNotNull(bidding.getBidCard());
                assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
                assertEquals(28, treacheryDeckSize);
                assertEquals(4, marketSize);
                assertEquals(0, bidding.getMarket().size());
                assertEquals("All players passed. 5 cards were returned to top of the Treachery Deck.",
                        turnSummary.getMessages().getLast());
                assertEquals(1 + marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
            }
        }

        @Nested
        @DisplayName("#richeseCacheCardOnceAround")
        public class RicheseCacheCardOnceAround {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.richeseCardAuction(game, "Ornithopter", "OnceAroundCCW");

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(7, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(9, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(9, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(9, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseCacheCardOnceAroundAllPass")
        public class RicheseCacheCardOnceAroundAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.richeseCardAuction(game, "Ornithopter", "OnceAroundCCW");

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(7, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                expectedMessagesInBiddingPhase = 9;
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseCacheCardSilent")
        public class RicheseCacheCardSilent {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.richeseCardAuction(game, "Ornithopter", "Silent");

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(1, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(3, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(3, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(3, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseCacheCardSilentAllPass")
        public class RicheseCacheCardSilentAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.richeseCardAuction(game, "Ornithopter", "Silent");

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(1, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                expectedMessagesInBiddingPhase = 3;
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketNormal")
        public class RicheseBlackMarketNormal {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(familyAtomics);
                bidding.blackMarketAuction(game, "Family Atomics", "Normal");
                String cardHeader = biddingPhase.getMessages().getFirst();
                int newlinePosition = cardHeader.indexOf("\n");
                cardHeader = cardHeader.substring(0, newlinePosition);
                assertEquals("R0:C1 (Black Market, Normal bidding)", cardHeader);

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(6, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(8, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(8, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(8, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketNormalAllPass")
        public class RicheseBlackMarketNormalAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(weatherControl);
                assertEquals(1, richese.getTreacheryHand().size());
                bidding.blackMarketAuction(game, "Weather Control", "Normal");
                assertEquals(0, richese.getTreacheryHand().size());
                assertEquals(5, bidding.getMarket().size());

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(6, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
//                for (String m : biddingPhase.getMessages())
//                    System.out.println("======Normal\n" + m);
//                assertEquals(9, biddingPhase.getMessages().size());
                assertEquals(8, biddingPhase.getMessages().size());
            }

            @Test
            void testCardReturnedToRicheseMessage() {
                assertEquals("All players passed. The black market card has been returned to " + Emojis.RICHESE, biddingPhase.getMessages().getLast());
            }

            @Test
            void testCardIsInRicheseHand() {
                assertEquals(1, richese.getTreacheryHand().size());
            }

            @Test
            void testOneMoreCardAddedToBiddingMarket() {
                assertEquals(5, bidding.getMarket().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, richese, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, richese));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, richese, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, richese, true));
                assertEquals(8, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(8, biddingPhase.getMessages().size());
            }

            @Test
            public void testNextCardIsC1() {
                bidding.setCacheCardDecisionInProgress(false);
                biddingPhase.clear();
                assertDoesNotThrow(() -> bidding.auctionNextCard(game, false));
                assertEquals(" You may now place your bids for R0:C1.", biddingPhase.getMessages().getFirst());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketOnceAround")
        public class RicheseBlackMarketOnceAround {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(familyAtomics);
                bidding.blackMarketAuction(game, "Family Atomics", "OnceAroundCCW");

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(7, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(9, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(9, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(9, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketOnceAroundAllPass")
        public class RicheseBlackMarketOnceAroundAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(familyAtomics);
                assertEquals(1, richese.getTreacheryHand().size());
                bidding.blackMarketAuction(game, "Family Atomics", "OnceAroundCCW");
                assertEquals(0, richese.getTreacheryHand().size());

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(7, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                expectedMessagesInBiddingPhase = 9;
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testCardReturnedToRicheseMessage() {
                assertEquals("All players passed. The black market card has been returned to " + Emojis.RICHESE, biddingPhase.getMessages().getLast());
            }

            @Test
            void testCardIsInRicheseHand() {
                assertEquals(1, richese.getTreacheryHand().size());
            }

            @Test
            void testOneMoreCardAddedToBiddingMarket() {
                assertEquals(5, bidding.getMarket().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, richese, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, richese));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, richese, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, richese, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            public void testBlackMarketCardSentBackToRichese() {
                assertEquals(1, richese.getTreacheryHand().size());
            }

            @Test
            public void testNextCardIsC1() {
                bidding.setCacheCardDecisionInProgress(false);
                biddingPhase.clear();
                assertDoesNotThrow(() -> bidding.auctionNextCard(game, false));
                assertEquals(" You may now place your bids for R0:C1.", biddingPhase.getMessages().getFirst());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketSilent")
        public class RicheseBlackMarketSilent {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(familyAtomics);
                bidding.blackMarketAuction(game, "Family Atomics", "Silent");

                bidding.bid(game, atreides, true, 1, null, null);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(1, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(3, biddingPhase.getMessages().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, atreides, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, atreides));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, atreides, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
                assertEquals(3, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(3, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketSilentAllPass")
        public class RicheseBlackMarketSilentAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(familyAtomics);
                assertEquals(1, richese.getTreacheryHand().size());
                bidding.blackMarketAuction(game, "Family Atomics", "Silent");
                assertEquals(0, richese.getTreacheryHand().size());

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(1, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                expectedMessagesInBiddingPhase = 3;
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testCardReturnedToRicheseMessage() {
                assertEquals("All players passed. The black market card has been returned to " + Emojis.RICHESE, biddingPhase.getMessages().getLast());
            }

            @Test
            void testCardIsInRicheseHand() {
                assertEquals(1, richese.getTreacheryHand().size());
            }

            @Test
            void testOneMoreCardAddedToBiddingMarket() {
                assertEquals(5, bidding.getMarket().size());
            }

            @Test
            void testWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, richese, true, 2, null, null));
            }

            @Test
            void testNonWinnerBidAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.bid(game, bg, true, 2, null, null));
            }

            @Test
            void testWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, richese));
            }

            @Test
            void testNonWinnerPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.pass(game, bg));
            }

            @Test
            void testWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, richese, true));
            }

            @Test
            void testNonWinnerAutoPassAfterTopBidderIdentified() {
                assertThrows(InvalidGameStateException.class, () -> bidding.setAutoPass(game, bg, true));
            }

            @Test
            void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, richese, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(expectedMessagesInBiddingPhase, biddingPhase.getMessages().size());
            }

            @Test
            public void testBlackMarketCardSentBackToRichese() {
                assertEquals(1, richese.getTreacheryHand().size());
            }

            @Test
            public void testNextCardIsC1() {
                bidding.setCacheCardDecisionInProgress(false);
                biddingPhase.clear();
                assertDoesNotThrow(() -> bidding.auctionNextCard(game, false));
                assertEquals(" You may now place your bids for R0:C1.", biddingPhase.getMessages().getFirst());
            }
        }
    }

    @Nested
    @DisplayName("#allHandsFillWithCardsRemaining")
    class AllHandsFillWithCardsRemaining {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(atreides);
            atreides.addTreacheryCard(shield);
            atreides.addTreacheryCard(shield);
            atreides.addTreacheryCard(shield);
            atreides.addTreacheryCard(shield);
            game.addFaction(bg);
            bg.addTreacheryCard(shield);
            bg.addTreacheryCard(shield);
            bg.addTreacheryCard(shield);
            game.addFaction(emperor);
            emperor.addTreacheryCard(shield);
            emperor.addTreacheryCard(shield);
            emperor.addTreacheryCard(shield);
            emperor.addTreacheryCard(shield);
            game.addFaction(fremen);
            fremen.addTreacheryCard(shield);
            fremen.addTreacheryCard(shield);
            fremen.addTreacheryCard(shield);
            fremen.addTreacheryCard(shield);
            game.addFaction(harkonnen);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            harkonnen.addTreacheryCard(shield);
            game.addFaction(richese);
            richese.addTreacheryCard(shield);
            richese.addTreacheryCard(shield);
            richese.addTreacheryCard(shield);

            game.addGameOption(GameOption.HOMEWORLDS);
            Territory geidiPrime = game.getTerritory(harkonnen.getHomeworld());
            geidiPrime.removeForces(game, "Harkonnen", 10);
            geidiPrime.addForces("BG", 1);
            assertTrue(harkonnen.isHomeworldOccupied());
            bidding = game.startBidding();
            bidding.setBlackMarketDecisionInProgress(false);
//            numMessagesInBiddingPhaseChannel = biddingPhase.getMessages().size();
        }
        
        @Test
        void testAllFillCardsRemainingRicheseCardAlreadySold() throws InvalidGameStateException {
            richese.addTreacheryCard(new TreacheryCard("Ornithopter"));
            bidding.setCacheCardDecisionInProgress(false);
            bidding.setRicheseCacheCardOutstanding(false);

            bidding.cardCountsInBiddingPhase(game);
            assertEquals(2, bidding.getNumCardsForBid());
            assertEquals(2, bidding.getMarket().size());
            bidding.auctionNextCard(game, false);
            bidding.assignAndPayForCard(game, "Harkonnen", "Bank", 1, false);
            assertEquals(4, bg.getTreacheryHand().size());
            assertEquals(8, harkonnen.getTreacheryHand().size());
            assertThrows(InvalidGameStateException.class, () -> bidding.auctionNextCard(game, false));
            int treacheryDeckSize = game.getTreacheryDeck().size();
            int marketSize = bidding.getMarket().size();
            assertNull(bidding.getBidCard());
            assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
            assertEquals("All hands are full. 1 cards were returned to top of the Treachery Deck.", turnSummary.getMessages().getLast());
            assertEquals(0, bidding.getMarket().size());
            assertEquals(marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
        }

        @Test
        void testAllHandsFilledWithCardsRemainingRicheseCardRemaining() throws InvalidGameStateException {
            richese.addTreacheryCard(shield);
            assertTrue(bidding.isRicheseCacheCardOutstanding());
            assertFalse(bidding.isCacheCardDecisionInProgress());

            bidding.cardCountsInBiddingPhase(game);
            assertEquals(2, bidding.getNumCardsForBid());
            assertEquals(1, bidding.getMarket().size());
            bidding.setCacheCardDecisionInProgress(false);
            bidding.auctionNextCard(game, false);
            bidding.assignAndPayForCard(game, "Harkonnen", "Bank", 1, false);
            assertEquals(4, bg.getTreacheryHand().size());
            assertEquals(8, harkonnen.getTreacheryHand().size());
            assertThrows(InvalidGameStateException.class, () -> bidding.auctionNextCard(game, false));
            int treacheryDeckSize = game.getTreacheryDeck().size();
            int marketSize = bidding.getMarket().size();
            assertNull(bidding.getBidCard());
            assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
            assertEquals("All hands are full. 0 cards were returned to top of the Treachery Deck.", turnSummary.getMessages().getLast());
            assertEquals("If anyone discards now, use /richese card-bid to auction the " + Emojis.RICHESE + " cache card. Otherwise, use /run advance to end the bidding phase.", modInfo.getMessages().getLast());
            assertEquals(0, bidding.getMarket().size());
            assertEquals(marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
        }
    }

    @Nested
    @DisplayName("#autoPassEntireTurnPerformsPass")
    public class AutoPassEntireTurnPerformsPass {
        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);

            bidding = game.startBidding();
            assertTrue(bidding.isRicheseCacheCardOutstanding());

            assertNull(bidding.getBidCard());
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(5, bidding.getMarket().size());
        }

        @Test
        public void testNormalBidding() throws InvalidGameStateException {
            bidding.setCacheCardDecisionInProgress(false);
            bidding.auctionNextCard(game, false);
            bidding.setAutoPassEntireTurn(game, atreides, true);
            assertEquals(6, bidding.getEligibleBidOrder(game).size());

            assertEquals("BG", bidding.getCurrentBidder());
        }

        @Test
        public void testRicheseCacheCardOnceAround() throws InvalidGameStateException {
            bidding.richeseCardAuction(game, "Ornithopter", "OnceAroundCCW");
            bidding.setAutoPassEntireTurn(game, atreides, true);
            assertEquals("BG", bidding.getCurrentBidder());
        }

        @Test
        public void testRicheseCacheCardSilent() throws InvalidGameStateException {
            bidding.richeseCardAuction(game, "Ornithopter", "Silent");
            bidding.setAutoPassEntireTurn(game, atreides, true);
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, harkonnen);
            assertEquals(1, biddingPhase.getMessages().size());
            bidding.pass(game, richese);
            assertNotEquals(1, biddingPhase.getMessages().size());
        }

        @Test
        public void testRicheseBlackMarketNormal() throws InvalidGameStateException {
            richese.addTreacheryCard(familyAtomics);
            bidding.blackMarketAuction(game, "Family Atomics", "Normal");
            bidding.setAutoPassEntireTurn(game, atreides, true);
            assertEquals("BG", bidding.getCurrentBidder());
        }
    }

    @Nested
    @DisplayName("#autoPassEntireTurnClearsOnNextTurn")
    public class AutoPassEntireTurnClearsOnNextTurn {
        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);

            bidding = game.startBidding();
            bidding.setAutoPassEntireTurn(game, atreides, true);
            assertTrue(bidding.isRicheseCacheCardOutstanding());

            assertNull(bidding.getBidCard());
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(5, bidding.getMarket().size());
        }

        @Test
        public void testNormalBidding() throws InvalidGameStateException {
            assertTrue(atreides.isAutoBidTurn());
            bidding.setCacheCardDecisionInProgress(false);
            bidding.auctionNextCard(game, false);
            assertFalse(atreides.isAutoBidTurn());
            assertEquals(6, bidding.getEligibleBidOrder(game).size());

            assertEquals("Atreides", bidding.getCurrentBidder());
        }

        @Test
        public void testRicheseCacheCardOnceAround() throws InvalidGameStateException {
            bidding.richeseCardAuction(game, "Ornithopter", "OnceAroundCCW");
            assertEquals("Atreides", bidding.getCurrentBidder());
        }

        @Test
        public void testRicheseCacheCardSilent() throws InvalidGameStateException {
            bidding.richeseCardAuction(game, "Ornithopter", "Silent");
            bidding.pass(game, bg);
            bidding.pass(game, emperor);
            bidding.pass(game, fremen);
            bidding.pass(game, harkonnen);
            bidding.pass(game, richese);
            assertEquals(1, biddingPhase.getMessages().size());
            bidding.pass(game, atreides);
            assertNotEquals(1, biddingPhase.getMessages().size());
        }

        @Test
        public void testRicheseBlackMarketNormal() throws InvalidGameStateException {
            richese.addTreacheryCard(familyAtomics);
            bidding.blackMarketAuction(game, "Family Atomics", "Normal");
            assertEquals("Atreides", bidding.getCurrentBidder());
        }
    }

    @Nested
    @DisplayName("#richeseToSellOnlyOneHandOpen")
    class RicheseToSellOnlyOneHandOpen {
        Bidding bidding;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(richese);
            game.addFaction(choam);
            game.addFaction(atreides);
            game.addFaction(guild);
            game.addFaction(emperor);
            game.addFaction(ecaz);
            richese.setHandLimit(0);
            choam.setHandLimit(0);
            atreides.setHandLimit(0);
            guild.setHandLimit(0);
            emperor.setHandLimit(0);
            ecaz.setHandLimit(1);
            game.advanceTurn();
            bidding = game.startBidding();
            bidding.setBlackMarketDecisionInProgress(false);
            richeseChat.clear();
        }

        @Test
        void testOneHandOpen() throws InvalidGameStateException {
            game.getBidding().cardCountsInBiddingPhase(game);
            assertEquals("Please select your cache card to be sold. You must sell now. ri", richeseChat.getMessages().getLast());
            assertEquals(Emojis.RICHESE + " has been given buttons for selling their cache card.", modInfo.getMessages().getLast());
            assertThrows(InvalidGameStateException.class, () -> bidding.finishBiddingPhase(game));
        }

        @Test
        void testAllHandsFull() throws InvalidGameStateException {
            ecaz.setHandLimit(0);
            game.getBidding().cardCountsInBiddingPhase(game);
            assertTrue(richeseChat.getMessages().isEmpty());
            assertEquals("If anyone discards now, use /richese card-bid to auction the " + Emojis.RICHESE + " cache card. Otherwise, use /run advance to end the bidding phase.", modInfo.getMessages().getLast());
            assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
        }
    }

    @Nested
    @DisplayName("#richeseCacheCardNotOccupied")
    class RicheseCacheCardNotOccupied {
        Bidding bidding;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(richese);
            game.addFaction(choam);
            game.advanceTurn();
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
        }

        @Test
        void testRicheseGetsCardChoices() {
            assertEquals("Please select your cache card to sell or choose to sell last. ri", richeseChat.getMessages().getFirst());
            assertEquals(11, richeseChat.getChoices().getFirst().size());
        }

        @Test
        void testRicheseGetsMethodChoices() {
            richeseChat.clear();
            bidding.presentCacheCardMethodChoices(game, "Ornithopter");
            assertEquals("How would you like to sell Ornithopter?", richeseChat.getMessages().getFirst());
            assertEquals(4, richeseChat.getChoices().getFirst().size());
        }

        @Test
        void testRicheseGetsConfirmChoices() {
            richeseChat.clear();
            bidding.presentCacheCardConfirmChoices(game, "Ornithopter", "Silent");
            assertEquals("", richeseChat.getMessages().getFirst());
            assertEquals(2, richeseChat.getChoices().getLast().size());
            assertEquals("Start over", richeseChat.getChoices().getLast().getLast().getLabel());
        }

        @Test
        void testRicheseGetsConfirmationMessage() throws InvalidGameStateException {
            richeseChat.clear();
            bidding.richeseCardAuction(game, "Ornithopter", "Silent");
            assertEquals("Selling Ornithopter by Silent auction.", richeseChat.getMessages().getFirst());
        }

        @Test
        void testRicheseSellsLast() {
            richeseChat.clear();
            bidding.richeseCardLast(game);
            assertEquals("You will sell last.", richeseChat.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#richeseCacheCardOccupied")
    class RicheseCacheCardOccupied {
        Bidding bidding;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(richese);
            game.addFaction(choam);
            game.addGameOption(GameOption.HOMEWORLDS);
            HomeworldTerritory richeseHomeworld = (HomeworldTerritory) game.getTerritory("Richese");
            richeseHomeworld.removeForces(game, "Richese", 20);
            richeseHomeworld.addForces("CHOAM", 1);
            assertTrue(richese.isHomeworldOccupied());
            game.advanceTurn();
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
        }

        @Test
        void testRicheseGetsCardtimingChoices() {
            assertEquals("Would you like to sell your cache card first or last? ri\n" + Emojis.CHOAM + " occupies your homeworld and will choose the card.", richeseChat.getMessages().getFirst());
            assertEquals(2, richeseChat.getChoices().getFirst().size());
        }

        @Test
        void testOccupierGetsCardChoicesWhenRicheseSellsFirst() {
            richeseChat.clear();
            bidding.richeseCardFirstByOccupier(game);
            assertEquals("You will sell first.", richeseChat.getMessages().getFirst());
            assertEquals("Please select the " + Emojis.RICHESE + " cache card to be sold. ch", choamChat.getMessages().getFirst());
            assertEquals(10, choamChat.getChoices().getFirst().size());
        }

        @Test
        void testRicheseGetsMethodChoices() {
            richeseChat.clear();
            choamChat.clear();
            bidding.presentCacheCardMethodChoices(game, "Ornithopter");
            assertEquals("You selected Ornithopter. " + Emojis.RICHESE + " chooses the bidding method.", choamChat.getMessages().getFirst());
            assertTrue(choamChat.getChoices().isEmpty());
            assertEquals("How would you like to sell Ornithopter?", richeseChat.getMessages().getFirst());
            assertEquals(3, richeseChat.getChoices().getFirst().size());
        }

        @Test
        void testRicheseGetsConfirmChoices() {
            richeseChat.clear();
            choamChat.clear();
            bidding.presentCacheCardConfirmChoices(game, "Ornithopter", "Silent");
            assertTrue(choamChat.getMessages().isEmpty());
            assertEquals("", richeseChat.getMessages().getFirst());
            assertEquals(2, richeseChat.getChoices().getLast().size());
            assertEquals("Reselect bid type", richeseChat.getChoices().getLast().getLast().getLabel());
        }

        @Test
        void testRicheseGetsConfirmationMessage() throws InvalidGameStateException {
            richeseChat.clear();
            choamChat.clear();
            bidding.richeseCardAuction(game, "Ornithopter", "Silent");
            assertEquals(2, richeseChat.getMessages().size());
            assertEquals("Selling Ornithopter by Silent auction.", richeseChat.getMessages().getFirst());
            assertEquals("ri Use the bot to place your bid for the silent auction. Your bid will be the exact amount you set.", richeseChat.getMessages().getLast());
            assertEquals("ch Use the bot to place your bid for the silent auction. Your bid will be the exact amount you set.", choamChat.getMessages().getFirst());
        }

        @Nested
        @DisplayName("#richeseSellsLast")
        class RicheseSellsLast {
            @BeforeEach
            void setUp() throws InvalidGameStateException {
                bidding.richeseCardLast(game);
                bidding.auctionNextCard(game, false);
                richeseChat.clear();
                choamChat.clear();
                bidding.assignAndPayForCard(game, "Richese", "Bank", 1, false);
            }

            @Test
            void testOccupierGetsCardChoicesWhenRicheseSellsLast() {
                assertTrue(richeseChat.getMessages().isEmpty());
                assertEquals("Please select the " + Emojis.RICHESE + " cache card to be sold. ch", choamChat.getMessages().getFirst());
            }

            @Test
            void testRicheseGetsMethodChoices() {
                richeseChat.clear();
                choamChat.clear();
                bidding.presentCacheCardMethodChoices(game, "Ornithopter");
                assertEquals("You selected Ornithopter. " + Emojis.RICHESE + " chooses the bidding method.", choamChat.getMessages().getFirst());
                assertTrue(choamChat.getChoices().isEmpty());
                assertEquals("How would you like to sell Ornithopter?", richeseChat.getMessages().getFirst());
                assertEquals(3, richeseChat.getChoices().getFirst().size());
            }
        }
    }

    @Nested
    @DisplayName("#ixBiddingAdvantage")
    class IxBiddingAdvantage {
        @BeforeEach
        void setUp() {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(ix);

            while (game.getTreacheryDeck().size() > 7) {
                TreacheryCard card = game.getTreacheryDeck().removeFirst();
                game.getTreacheryDiscard().add(card);
            }
        }

        @Test
        void testShowingIxRequiresReshuffle() throws InvalidGameStateException {
            TreacheryCard card = game.getTreacheryDeck().removeFirst();
            game.getTreacheryDiscard().add(card);
            assertEquals(6, game.getTreacheryDeck().size());
            TreacheryCard cardToPutBack = game.getTreacheryDeck().getFirst();
            bidding = game.startBidding();
            bidding.populateMarket(game);
            assertEquals(26, game.getTreacheryDeck().size());
            turnSummary.clear();
            bidding.auctionNextCard(game, false);
            bidding.putBackIxCard(game, cardToPutBack.name(), "top", false);
            assertTrue(turnSummary.getMessages().getFirst().contains("replenished from the discard pile"));
        }

        @Test
        void testAllCardsLeftInDeckShownToIxNoReshuffleNeeded() throws InvalidGameStateException {
            assertEquals(7, game.getTreacheryDeck().size());
            TreacheryCard cardToPutBack = game.getTreacheryDeck().getFirst();
            bidding = game.startBidding();
            bidding.populateMarket(game);
            assertEquals(0, game.getTreacheryDeck().size());
            turnSummary.clear();
            bidding.auctionNextCard(game, false);
            bidding.putBackIxCard(game, cardToPutBack.name(), "top", false);
            assertFalse(turnSummary.getMessages().getFirst().contains("replenished from the discard pile"));
        }

        @Test
        void testKaramaIxBiddingAdvantage() throws InvalidGameStateException {
            bidding = game.startBidding();
            bidding.populateMarket(game);
            assertFalse(bidding.isMarketShownToIx());
            assertFalse(bidding.isIxRejectOutstanding());
            assertEquals(7, bidding.getMarket().size());
            bidding.blockIxBiddingAdvantage(game);
            assertTrue(bidding.isMarketShownToIx());
            assertFalse(bidding.isIxRejectOutstanding());
            assertEquals(6, bidding.getMarket().size());
        }

        @Nested
        @DisplayName("#ixOccupied")
        class IxOccupied {
            TreacheryCard card;

            @BeforeEach
            void setUp() throws InvalidGameStateException {
                game.addGameOption(GameOption.HOMEWORLDS);
                ix.placeForceFromReserves(game, arrakeen, 10, false);
                ix.placeForceFromReserves(game, arrakeen, 4, true);
                atreides.placeForceFromReserves(game, game.getTerritory("Ix"), 1, false);
                assertTrue(ix.isHomeworldOccupied());
                assertEquals(atreides, ix.getOccupier());
                bidding = game.startBidding();
                turnSummary.clear();
                bidding.cardCountsInBiddingPhase(game);
                bidding.auctionNextCard(game, false);
                card = bidding.getMarket().getFirst();
            }

            @Test
            void testTurnSummarySaysAtreidesWillSendCardBack() {
                assertTrue(turnSummary.getMessages().getFirst().endsWith(Emojis.ATREIDES + " will send one of them back to the deck."));
                assertEquals("7 " + Emojis.TREACHERY + " cards have been shown to " + Emojis.ATREIDES, turnSummary.getMessages().getLast());
            }

            @Test
            void testOccupierOfferedCardChoices() {
                assertTrue(ixChat.getChoices().isEmpty());
                assertEquals(7, atreidesChat.getChoices().getFirst().size());
            }

            @Test
            void testOccupierOfferedLocationChoices() throws InvalidGameStateException {
                bidding.presentRejectedCardLocationChoices(game, card.name(), 0);
                assertTrue(ixChat.getChoices().isEmpty());
                assertEquals(3, atreidesChat.getChoices().getLast().size());
            }

            @Test
            void testOccupierOfferedConfirmationChoices() throws InvalidGameStateException {
                bidding.presentRejectConfirmationChoices(game, card.name(), "top", 0);
                assertTrue(ixChat.getChoices().isEmpty());
                assertEquals(2, atreidesChat.getChoices().getLast().size());
            }

            @Test
            void testTurnSummaryReportsAtreidesSentCardBack() throws InvalidGameStateException {
                ixChat.clear();
                bidding.putBackIxCard(game, card.name(), "top", false);
                assertEquals(Emojis.ATREIDES + " sent a " + Emojis.TREACHERY + " to the top of the deck.", turnSummary.getMessages().getLast());
                assertNotNull(bidding.getBidCard());
                assertTrue(ixChat.getMessages().isEmpty());
            }

            @Test
            void testIxAskedAboutTechnology() throws InvalidGameStateException {
                ix.addTreacheryCard(kulon);
                bidding.putBackIxCard(game, card.name(), "top", false);
                assertNull(bidding.getBidCard());
                assertEquals("Would you like to use Technology on the first card? ix", ixChat.getMessages().getLast());
            }
        }
    }

    @Nested
    @DisplayName("#putBackIxCard")
    class PutBackIxCard {
        TreacheryCard card;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(ix);
            bidding = game.startBidding();

            assertFalse(bidding.isMarketShownToIx());
            assertFalse(bidding.isIxRejectOutstanding());
            bidding.cardCountsInBiddingPhase(game);
            assertThrows(InvalidGameStateException.class, () -> bidding.finishBiddingPhase(game));
            assertEquals(6, bidding.getNumCardsForBid());
            bidding.auctionNextCard(game, false);
            assertTrue(bidding.isMarketShownToIx());
            assertTrue(bidding.isIxRejectOutstanding());

            card = bidding.getMarket().getFirst();
            assertTrue(bidding.isMarketShownToIx());
            assertTrue(bidding.isIxRejectOutstanding());
            assertTrue(biddingPhase.getMessages().isEmpty());
            ixChat.clear();
        }

        @Test
        void testAllowAutomaticAuction() throws InvalidGameStateException {
            assertThrows(InvalidGameStateException.class, () -> bidding.finishBiddingPhase(game));
            bidding.putBackIxCard(game, card.name(), "Top", false);
            assertTrue(bidding.isMarketShownToIx());
            assertFalse(bidding.isIxRejectOutstanding());
            assertEquals(Emojis.IX + " sent a " + Emojis.TREACHERY + " to the top of the deck.",
                    turnSummary.getMessages().getLast());
            assertTrue(biddingPhase.getMessages().getFirst().contains(" You may now place your bids"));
        }

        @Test
        void testRequestTechnology() throws InvalidGameStateException {
            assertThrows(InvalidGameStateException.class, () -> bidding.finishBiddingPhase(game));
            bidding.putBackIxCard(game, card.name(), "Top", true);
            assertTrue(bidding.isMarketShownToIx());
            assertFalse(bidding.isIxRejectOutstanding());
            assertEquals(Emojis.IX + " sent a " + Emojis.TREACHERY + " to the top of the deck.", turnSummary.getMessages().getLast());
            assertEquals(Emojis.IX + " would like to use Technology on the first card. ", ixChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("silentAuction")
    class SilentAuction {
        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(richese);
            bidding = game.startBidding();
            bidding.cardCountsInBiddingPhase(game);
        }

        @Test
        void notSilentToStart() {
            assertFalse(bidding.isSilentAuction());
        }

        @Test
        void notSilentAfterNewCard() throws InvalidGameStateException {
            bidding.richeseCardAuction(game, "Ornithopter", "Silent");
            assertTrue(bidding.isSilentAuction());
            bidding.clearBidCardInfo("Emperor");
            assertFalse(bidding.isSilentAuction());
        }
    }

    @Nested
    @DisplayName("updateBidOrder")
    class UpdateBidOrder {
        @BeforeEach
        void setUp() throws IOException {
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(guild);
            game.addFaction(harkonnen);
            bidding = game.startBidding();
        }

        @Test
        void dot4IsFirst() {
            List<String> expected = Arrays.asList("Fremen", "Guild", "Harkonnen", "Atreides", "BG", "Emperor");
            game.setStorm(7);
            bidding.updateBidOrder(game);
            assertArrayEquals(expected.toArray(), bidding.getEligibleBidOrder(game).toArray());
        }
    }
}
