package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TleilaxuTanksTest {
    private TleilaxuTanks tanks;

    @BeforeEach
    void setUp() throws IOException {
        Game game = new Game();
        tanks = game.getTleilaxuTanks();
    }

    @Test
    void getForceStrength() {
        assertEquals(0, tanks.getForceStrength("Emperor*"));
        tanks.addForces("Emperor", 1);
        assertEquals(0, tanks.getForceStrength("Emperor*"));
        tanks.addForces("Emperor*", 1);
        assertEquals(1, tanks.getForceStrength("Emperor*"));
    }

    @Test
    void addForces() {
        assertEquals(0, tanks.getForceStrength("Emperor"));
        tanks.addForces("Emperor", 1);
        assertEquals(1, tanks.getForceStrength("Emperor"));

        assertEquals(0, tanks.getForceStrength("Emperor*"));
        tanks.addForces("Emperor*", 1);
        assertEquals(1, tanks.getForceStrength("Emperor*"));

        assertEquals(0, tanks.getForceStrength("BG"));
        tanks.addForces("Advisor", 1);
        assertEquals(1, tanks.getForceStrength("BG"));
        tanks.addForces("BG", 1);
        assertEquals(2, tanks.getForceStrength("BG"));
    }

    @Test
    void removeForces() {
        assertEquals(0, tanks.getForceStrength("Emperor*"));
        tanks.addForces("Emperor*", 1);
        assertEquals(1, tanks.getForceStrength("Emperor*"));
        tanks.removeForces("Emperor*", 1);
        assertEquals(0, tanks.getForceStrength("Emperor*"));
    }

    @Test
    void removeForcesThrows() {
        assertEquals(0, tanks.getForceStrength("Emperor*"));
        assertThrows(IllegalArgumentException.class, () -> tanks.removeForces("Emperor*", 1));
    }
}
