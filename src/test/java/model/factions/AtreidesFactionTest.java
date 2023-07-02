package model.factions;

import constants.Emojis;
import model.Territory;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AtreidesFactionTest extends FactionTestTemplate {

    private AtreidesFaction faction;

    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() throws IOException {
        faction = new AtreidesFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 10); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 2); }

    @Test
    public void testInitialHasMiningEquipment() { assertTrue(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 10);
        assertEquals(faction.getReserves().getName(), "Atreides");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            if (territoryName.equals("Arrakeen")) {
                assertEquals(territory.getForces().get(0).getStrength(), 10);
                assertEquals(territory.getForces().get(0).getName(), "Atreides");
            } else {
                assertEquals(territory.getForces().size(), 0);
            }
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.ATREIDES); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 4); }

    @Test
    public void testInitialForcesLost() { assertEquals(faction.getForcesLost(), 0); }

    @Test
    public void testInitialHasKH() { assertFalse(faction.isHasKH()); }

    @RepeatedTest(20)
    public void testSetForcesLost(RepetitionInfo repetitionInfo) {
        int forcesLost = repetitionInfo.getCurrentRepetition();
        faction.setForcesLost(forcesLost);
        assertEquals(faction.getForcesLost(), forcesLost);
        assertEquals(faction.isHasKH(), forcesLost >= 7);
    }

    @Test
    public void testAddForcesLostNoKH() {
        faction.setForcesLost(2);
        faction.addForceLost(3);
        assertEquals(faction.getForcesLost(), 5);
        assertFalse(faction.isHasKH());
    }

    @Test
    public void testAddForcesLostKH() {
        faction.setForcesLost(4);
        faction.addForceLost(3);
        assertEquals(faction.getForcesLost(), 7);
        assertTrue(faction.isHasKH());
    }
}