package model;

import exceptions.InvalidGameStateException;
import model.factions.Faction;
import model.factions.AtreidesFaction;
import model.TreacheryCard;
import enums.GameOption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BiddingTest {

    private Bidding bidding;
    private Game game;

    @BeforeEach
    void setUp() throws IOException {
        bidding = new Bidding();
        game = new Game();
    }

    @Nested
    @DisplayName("silentAuction")
    class SilentAuction {
        @Test
        void notSilentTostart() {
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
}
