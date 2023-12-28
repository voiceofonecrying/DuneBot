package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BattlePlanTest {
    Leader zoal;
    Leader duncanIdaho;

    @BeforeEach
    void setUp() throws IOException {
        zoal = new Leader("Zoal", -1, null, false);
        duncanIdaho = new Leader("Duncan Idaho", 2, null, false);
    }

    @Test
    void testZoalHasNoValue() {
        BattlePlan plan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        assertEquals(0, plan.getLeaderStrength());
        assertEquals("Leader: Zoal (X)", plan.getLeaderString());
        assertEquals("Zoal", plan.getKilledLeaderString());
    }

    @Test
    void testZoalHasOpponentLeaderValue() {
        BattlePlan plan = new BattlePlan(zoal, null, false, 0, false, 0, null, null);
        plan.setOpponentLeader(duncanIdaho);
        assertEquals(2, plan.getLeaderStrength());
        assertEquals("Leader: Zoal (X)", plan.getLeaderString());
        assertEquals("Zoal", plan.getKilledLeaderString());
    }

    @AfterEach
    void tearDown() {
    }
}
