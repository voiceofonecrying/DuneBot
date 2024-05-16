package model.factions;

import constants.Emojis;
import enums.GameOption;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AtreidesFactionTest extends FactionTestTemplate {

    private AtreidesFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new AtreidesFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 10);
    }

    @Test
    public void testFreeRevival() {
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalAlliedToFremen() {
        faction.setAlly("Fremen");
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThreshold() {
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() {
        game.setRecruitsInPlay(true);
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() {
        game.setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertTrue(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 10);
        assertEquals(faction.getReserves().getName(), "Atreides");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
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
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.ATREIDES);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    public void testInitialForcesLost() {
        assertEquals(faction.getForcesLost(), 0);
    }

    @Test
    public void testInitialHasKH() {
        assertFalse(faction.isHasKH());
    }

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