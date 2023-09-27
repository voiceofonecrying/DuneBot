package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiddingTest {

    private Bidding bidding;

    @BeforeEach
    void setUp() throws IOException {
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
}
