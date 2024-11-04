package model;

import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeaderTest extends DuneTest {
    @BeforeEach
    void setUp() throws IOException, InvalidGameStateException {
        super.setUp();
    }

    @Test
    void testLeaderNameAndValueString() {
        assertEquals("Feyd Rautha (6)", feydRautha.getNameAndValueString());
        assertEquals("Zoal (X)", zoal.getNameAndValueString());
        assertEquals("Duke Vidal (6)", dukeVidal.getNameAndValueString());
    }

    @Test
    void testZoalCosts3ToRevive() {
        assertEquals(3, zoal.getRevivalCost(bt));
    }
}
