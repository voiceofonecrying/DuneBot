package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BiddingTest {
    private Game game;
    private Bidding bidding;
    private TestTopic biddingPhase;
    private TestTopic turnSummary;
    private TestTopic modInfo;

    private AtreidesFaction atreides;
    private BGFaction bg;
    private EmperorFaction emperor;
    private FremenFaction fremen;
    private GuildFaction guild;
    private HarkonnenFaction harkonnen;
    private IxFaction ix;
    private RicheseFaction richese;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        modInfo = new TestTopic();
        game.setModInfo(modInfo);
        turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        biddingPhase = new TestTopic();
        game.setBiddingPhase(biddingPhase);

        atreides = new AtreidesFaction("p", "u", game);
        bg = new BGFaction("p", "u", game);
        emperor = new EmperorFaction("p", "u", game);
        fremen = new FremenFaction("p", "u", game);
        guild = new GuildFaction("p", "u", game);
        harkonnen = new HarkonnenFaction("p", "u", game);
        ix = new IxFaction("p", "u", game);
        richese = new RicheseFaction("p", "u", game);

        atreides.setChat(new TestTopic());
        bg.setChat(new TestTopic());
        emperor.setChat(new TestTopic());
        fremen.setChat(new TestTopic());
        guild.setChat(new TestTopic());
        harkonnen.setChat(new TestTopic());
        ix.setChat(new TestTopic());
        richese.setChat(new TestTopic());

        atreides.setLedger(new TestTopic());
        bg.setLedger(new TestTopic());
        emperor.setLedger(new TestTopic());
        fremen.setLedger(new TestTopic());
        guild.setLedger(new TestTopic());
        harkonnen.setLedger(new TestTopic());
        ix.setLedger(new TestTopic());
        richese.setLedger(new TestTopic());
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
            bidding.cardCountsInBiddingPhase(game);
            assertEquals(6, bidding.getNumCardsForBid());
            assertEquals(5, bidding.getMarket().size());
            assertEquals(1, modInfo.getMessages().size());
            assertFalse(bidding.isIxRejectOutstanding());
            assertTrue(bidding.isRicheseCacheCardOutstanding());
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
            bidding.bidding(game);
        }

        @Test
        public void testWinnerPaysAll() throws InvalidGameStateException {
            bidding.bid(game, atreides, true, 3, false, false);
            bidding.awardTopBidder(game);
            assertEquals(7, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE, turnSummary.getMessages().get(2));
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyPaysAll() throws InvalidGameStateException {
            emperor.setSpiceForAlly(5);
            bidding.bid(game, atreides, true, 3, false, false);
            bidding.awardTopBidder(game);
            assertEquals(10, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE + " (3 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().get(2));
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyPaysAll2() throws InvalidGameStateException {
            assertEquals(10, atreides.getSpice());
            emperor.setSpiceForAlly(5);
            bidding.bid(game, atreides, true, 11, false, false);
            bidding.awardTopBidder(game);
            assertEquals(4, atreides.getSpice());
            assertEquals(16, emperor.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 11 " + Emojis.SPICE + " (5 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().get(2));
            assertEquals(Emojis.EMPEROR + " is paid 11 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }

        @Test
        public void testAllyBuysCardCantAffordToSupport() throws InvalidGameStateException {
            emperor.setSpiceForAlly(5);
            bidding.bid(game, atreides, true, 1, false, false);
            bidding.bid(game, bg, true, 2, false, false);
            bidding.bid(game, emperor, true, 10, false, false);
            bidding.awardTopBidder(game);
            assertEquals(0, emperor.getSpice());
            assertEquals(0, emperor.getSpiceForAlly());
            bidding.bidding(game);
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
            bidding.bid(game, atreides, true, 3, false, false);
            bidding.awardTopBidder(game);
            assertEquals(9, atreides.getSpice());
            assertEquals(Emojis.ATREIDES + " wins R0:C1 for 3 " + Emojis.SPICE + " (2 from " + Emojis.EMPEROR + ")", turnSummary.getMessages().get(2));
            assertEquals(Emojis.EMPEROR + " is paid 3 " + Emojis.SPICE + " for R0:C1", turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#topBidderIdentified")
    public class TopBidderIdentified {
        int numMessagesInBiddingPhaseChannel;

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
                bidding.bidding(game);
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

            @Test
            void testAllHandsFilledWithCardsRemaining() throws InvalidGameStateException {
                bidding.setRicheseCacheCardOutstanding(false);
                atreides.setHandLimit(1);
                bg.setHandLimit(0);
                emperor.setHandLimit(0);
                fremen.setHandLimit(0);
                harkonnen.setHandLimit(0);
                richese.setHandLimit(0);
                bidding.awardTopBidder(game);
                assertThrows(InvalidGameStateException.class, () -> bidding.bidding(game));
                int treacheryDeckSize = game.getTreacheryDeck().size();
                int marketSize = bidding.getMarket().size();
                assertNull(bidding.getBidCard());
                assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
                assertEquals(28, treacheryDeckSize);
                assertEquals(4, marketSize);
                assertEquals(0, bidding.getMarket().size());
                assertEquals("All hands are full. 4 cards were returned to top of the Treachery Deck.",
                        turnSummary.getMessages().getLast());
                assertEquals(marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
            }

            @Test
            void testAllHandsFilledWithCardsRemainingRicheseCardRemaining() throws InvalidGameStateException {
                bidding.awardTopBidder(game);
                atreides.setHandLimit(1);
                bidding.bidding(game);
                bidding.assignAndPayForCard(game, "BG", "Emperor", 0);
                bg.setHandLimit(1);
                bidding.bidding(game);
                bidding.assignAndPayForCard(game, "Emperor", "Bank", 0);
                emperor.setHandLimit(1);
                bidding.bidding(game);
                bidding.assignAndPayForCard(game, "Fremen", "Emperor", 0);
                fremen.setHandLimit(1);
                bidding.bidding(game);
                bidding.assignAndPayForCard(game, "Harkonnen", "Emperor", 0);
                harkonnen.setHandLimit(2);
                richese.setHandLimit(0);
                assertThrows(InvalidGameStateException.class, () -> bidding.bidding(game));
                int treacheryDeckSize = game.getTreacheryDeck().size();
                int marketSize = bidding.getMarket().size();
                assertNull(bidding.getBidCard());
                assertDoesNotThrow(() -> bidding.finishBiddingPhase(game));
                assertEquals(27, treacheryDeckSize);
                assertEquals(0, marketSize);
                assertEquals(0, bidding.getMarket().size());
                assertEquals("All hands are full. " + Emojis.RICHESE + " may not auction a card from their cache.",
                        turnSummary.getMessages().getLast());
                assertEquals(marketSize + treacheryDeckSize, game.getTreacheryDeck().size());
            }
        }

        @Nested
        @DisplayName("#normalBiddingAllPass")
        public class NormalBiddingAllPass {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                bidding.bidding(game);
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
                bidding.setRicheseCacheCardOutstanding(false);
                assertThrows(InvalidGameStateException.class, () -> bidding.bidding(game));
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
                assertThrows(InvalidGameStateException.class, () -> bidding.bidding(game));
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
                assertEquals(10, biddingPhase.getMessages().size());
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
                assertEquals(10, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(10, biddingPhase.getMessages().size());
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
                assertEquals(4, biddingPhase.getMessages().size());
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
                assertEquals(4, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(4, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketNormal")
        public class RicheseBlackMarketNormal {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
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
            int richeseHandSize;

            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
                richeseHandSize = richese.getTreacheryHand().size();
                assertEquals(1, richeseHandSize);
                bidding.blackMarketAuction(game, "Family Atomics", "Normal");
                assertEquals(0, richese.getTreacheryHand().size());

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

            @Test
            public void testBlackMarketCardSentBackToRichese() {
                bidding.setRicheseCacheCardOutstanding(false);
                assertThrows(InvalidGameStateException.class, () -> bidding.bidding(game));
                int treacheryDeckSize = game.getTreacheryDeck().size();
                assertNotNull(bidding.getBidCard());
                assertEquals(28, treacheryDeckSize);
                assertEquals(5, bidding.getMarket().size());
                assertNotEquals(0, turnSummary.getMessages().getLast().indexOf("Number of Treachery Cards"));
                assertEquals(1, richeseHandSize);
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketOnceAround")
        public class RicheseBlackMarketOnceAround {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
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
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
                bidding.blackMarketAuction(game, "Family Atomics", "OnceAroundCCW");

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(7, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(10, biddingPhase.getMessages().size());
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
                assertEquals(10, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(10, biddingPhase.getMessages().size());
            }
        }

        @Nested
        @DisplayName("#richeseBlackMarketSilent")
        public class RicheseBlackMarketSilent {
            @BeforeEach
            public void setUp() throws IOException, InvalidGameStateException {
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
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
                richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
                bidding.blackMarketAuction(game, "Family Atomics", "Silent");

                bidding.pass(game, atreides);
                bidding.pass(game, bg);
                bidding.pass(game, emperor);
                bidding.pass(game, fremen);
                bidding.pass(game, harkonnen);
                assertEquals(1, biddingPhase.getMessages().size());
                bidding.pass(game, richese);
                assertEquals(4, biddingPhase.getMessages().size());
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
                assertEquals(4, biddingPhase.getMessages().size());
            }

            @Test
            void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
                assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
                assertEquals(4, biddingPhase.getMessages().size());
            }
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
            bidding.bidding(game);
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
            richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
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
            bidding.bidding(game);
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
            richese.addTreacheryCard(new TreacheryCard("Family Atomics"));
            bidding.blackMarketAuction(game, "Family Atomics", "Normal");
            assertEquals("Atreides", bidding.getCurrentBidder());
        }
    }

    @Test
    void testPutBackIxCard() throws InvalidGameStateException {
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
        assertEquals(6, bidding.getNumCardsForBid());
        bidding.bidding(game);
        assertTrue(bidding.isMarketShownToIx());
        assertTrue(bidding.isIxRejectOutstanding());

        TreacheryCard card = bidding.getMarket().getFirst();
        assertTrue(bidding.isMarketShownToIx());
        assertTrue(bidding.isIxRejectOutstanding());
        bidding.putBackIxCard(game, card.name(), "Top");
        assertTrue(bidding.isMarketShownToIx());
        assertFalse(bidding.isIxRejectOutstanding());
        assertEquals(Emojis.IX + " sent a " + Emojis.TREACHERY + " to the top of the deck.",
                turnSummary.getMessages().getLast());
    }

    @Test
    void testKaramaIxBiddingAdvantage() throws InvalidGameStateException {
        game.addFaction(atreides);
        game.addFaction(bg);
        game.addFaction(emperor);
        game.addFaction(fremen);
        game.addFaction(guild);
        game.addFaction(ix);
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
