package model;

import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.factions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BiddingTest {
    private Bidding bidding;

    @BeforeEach
    void setUp() {
        bidding = new Bidding();
    }

    @Nested
    @DisplayName("#topBidderIdentified")
    public class TopBidderIdentified {
        Game game;
        TestTopic biddingPhase;
        AtreidesFaction atreides;
        BGFaction bg;
        EmperorFaction emperor;
        FremenFaction fremen;
        HarkonnenFaction harkonnen;
        RicheseFaction richese;

        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException, ChannelNotFoundException {
            game = new Game();
            game.setModInfo(new TestTopic());
            biddingPhase = new TestTopic();
            game.setBiddingPhase(biddingPhase);

            atreides = new AtreidesFaction("p", "u", game);
            bg = new BGFaction("p", "u", game);
            emperor = new EmperorFaction("p", "u", game);
            fremen = new FremenFaction("p", "u", game);
            harkonnen = new HarkonnenFaction("p", "u", game);
            richese = new RicheseFaction("p", "u", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);
            // RunCommands::startBiddingPhase
            game.startBidding();
            bidding = game.getBidding();
            game.getFactions().forEach(faction -> {
                faction.setBid("");
                faction.setMaxBid(0);
            });
            // RunCommands::cardCountsInBiddingPhase
            assertNull(bidding.getBidCard());
            int numCardsForBid = bidding.populateMarket(game);
            assertEquals(5, numCardsForBid);
            // RunCommands::bidding
            bidding.updateBidOrder(game);
            List<String> bidOrder = bidding.getEligibleBidOrder(game);
            assertEquals(6, bidOrder.size());
            bidding.nextBidCard(game);
            Faction factionBeforeFirstToBid = game.getFaction(bidOrder.getLast());
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
            bidding.createBidMessage(game, true);
            bidding.advanceBidder(game);
            bidding.tryBid(game, game.getFaction(bidding.getCurrentBidder()));
            assertEquals(1, biddingPhase.getMessages().size());

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
            assertEquals(8, biddingPhase.getMessages().size());
        }

        @Test
        void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
            assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
            assertEquals(8, biddingPhase.getMessages().size());
        }

        @Test
        void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
            assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
        }
    }

    @Nested
    @DisplayName("#topBidderIdentifiedRicheseCard")
    public class TopBidderIdentifiedRicheseCard {
        Game game;
        TestTopic biddingPhase;
        TestTopic turnSummary;
        AtreidesFaction atreides;
        BGFaction bg;
        EmperorFaction emperor;
        FremenFaction fremen;
        HarkonnenFaction harkonnen;
        RicheseFaction richese;

        @BeforeEach
        public void setUp() throws IOException, InvalidGameStateException, ChannelNotFoundException {
            game = new Game();
            game.setModInfo(new TestTopic());
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            biddingPhase = new TestTopic();
            game.setBiddingPhase(biddingPhase);

            atreides = new AtreidesFaction("p", "u", game);
            bg = new BGFaction("p", "u", game);
            emperor = new EmperorFaction("p", "u", game);
            fremen = new FremenFaction("p", "u", game);
            harkonnen = new HarkonnenFaction("p", "u", game);
            richese = new RicheseFaction("p", "u", game);
            game.addFaction(atreides);
            game.addFaction(bg);
            game.addFaction(emperor);
            game.addFaction(fremen);
            game.addFaction(harkonnen);
            game.addFaction(richese);
            atreides.setLedger(new TestTopic());
            richese.setLedger(new TestTopic());
            // RunCommands::startBiddingPhase
            game.startBidding();
            bidding = game.getBidding();
            game.getFactions().forEach(faction -> {
                faction.setBid("");
                faction.setMaxBid(0);
            });
            assertTrue(bidding.isRicheseCacheCardOutstanding());
            // RunCommands::cardCountsInBiddingPhase
            assertNull(bidding.getBidCard());
            int numCardsForBid = bidding.populateMarket(game);
            assertEquals(5, numCardsForBid);
            //RicheseCommands::cardBid
            bidding.setRicheseCacheCard(true);
            bidding.setBidCard(
                    richese.removeTreacheryCardFromCache(
                            richese.getTreacheryCardFromCache("Ornithopter")
                    )
            );
            bidding.incrementBidCardNumber();
            // From RicheseCommands::runRicheseBid
            for (Faction faction : game.getFactions()) {
                faction.setMaxBid(0);
                faction.setAutoBid(false);
                faction.setBid("");
            }
            List<Faction> factions = game.getFactions();
            List<Faction> bidOrderFactions = new ArrayList<>();
            List<Faction> factionsInBidDirection = factions;
            int richeseIndex = factionsInBidDirection.indexOf(game.getFaction("Richese"));
            bidOrderFactions.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrderFactions.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));
            List<String> bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
            bidding.setRicheseBidOrder(game, bidOrder);
            List<String> filteredBidOrder = bidding.getEligibleBidOrder(game);
            Faction factionBeforeFirstToBid = game.getFaction(filteredBidOrder.getLast());
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
//            discordGame.queueMessage("bidding-phase", message.toString());
            bidding.createBidMessage(game, true);
            bidding.advanceBidder(game);

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
            assertEquals(8, biddingPhase.getMessages().size());
        }

        @Test
        void testWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
            assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, atreides, true));
            assertEquals(8, biddingPhase.getMessages().size());
        }

        @Test
        void testNonWinnerAutoPassEntireTurnAfterTopBidderIdentified() {
            assertDoesNotThrow(() -> bidding.setAutoPassEntireTurn(game, bg, true));
        }
    }

    @Test
    void testSetMarketShownToIx() {
        assertFalse(bidding.isMarketShownToIx());
        assertFalse(bidding.isIxRejectOutstanding());
        bidding.setMarketShownToIx(true);
        assertTrue(bidding.isMarketShownToIx());
        assertTrue(bidding.isIxRejectOutstanding());
    }

    @Test
    void testPutBackIxCard() throws IOException, InvalidGameStateException {
        Game game = new Game();
        game.addFaction(new AtreidesFaction("fakePlayer1", "userName1", game));
        game.addFaction(new BGFaction("fakePlayer2", "userName2", game));
        game.addFaction(new EmperorFaction("fp3", "un3", game));
        game.addFaction(new FremenFaction("fp4", "un4", game));
        game.addFaction(new GuildFaction("fp5", "un5", game));
        game.addFaction(new IxFaction("fp6", "un6", game));
        game.startBidding();
        bidding = game.getBidding();
        bidding.populateMarket(game);
        TreacheryCard card = bidding.getMarket().getFirst();
        bidding.setMarketShownToIx(true);
        assertTrue(bidding.isMarketShownToIx());
        assertTrue(bidding.isIxRejectOutstanding());
        bidding.putBackIxCard(game, card.name(), "Top");
        assertTrue(bidding.isMarketShownToIx());
        assertFalse(bidding.isIxRejectOutstanding());
    }

    @Test
    void testKaramaIxBiddingAdvantage() throws IOException, InvalidGameStateException {
        Game game = new Game();
        game.addFaction(new AtreidesFaction("fakePlayer1", "userName1", game));
        game.addFaction(new BGFaction("fakePlayer2", "userName2", game));
        game.addFaction(new EmperorFaction("fp3", "un3", game));
        game.addFaction(new FremenFaction("fp4", "un4", game));
        game.addFaction(new GuildFaction("fp5", "un5", game));
        game.addFaction(new IxFaction("fp6", "un6", game));
        game.startBidding();
        bidding = game.getBidding();
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
        @Test
        void notSilentToStart() {
            assertFalse(bidding.isSilentAuction());
        }

        @Test
        void notSilentAfterNewCard() {
            bidding.setSilentAuction(true);
            assertTrue(bidding.isSilentAuction());
            bidding.clearBidCardInfo("Emperor");
            assertFalse(bidding.isSilentAuction());
        }
    }

    @Nested
    @DisplayName("updateBidOrder")
    class UpdateBidOrder {
        Game game;

        @BeforeEach
        void setUp() throws IOException {
            game = new Game();
            game.addFaction(new AtreidesFaction("fakePlayer1", "userName1", game));
            game.addFaction(new BGFaction("fakePlayer2", "userName2", game));
            game.addFaction(new EmperorFaction("fp3", "un3", game));
            game.addFaction(new FremenFaction("fp4", "un4", game));
            game.addFaction(new GuildFaction("fp5", "un5", game));
            game.addFaction(new HarkonnenFaction("fp6", "un6", game));
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
