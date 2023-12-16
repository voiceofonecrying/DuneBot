package model;

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

    private Bidding bidding;

    @BeforeEach
    void setUp() {
        bidding = new Bidding();
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
