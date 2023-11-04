package model;

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
