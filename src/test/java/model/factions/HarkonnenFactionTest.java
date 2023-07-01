package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HarkonnenFactionTest extends FactionTestTemplate {

    private HarkonnenFaction faction;
    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() {
        faction = new HarkonnenFaction("player", "player", game);
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
        assertEquals(faction.getReserves().getName(), "Harkonnen");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            if (territoryName.equals("Carthag")) {
                assertEquals(territory.getForces().get(0).getStrength(), 10);
                assertEquals(territory.getForces().get(0).getName(), "Harkonnen");
            } else {
                assertEquals(territory.getForces().size(), 0);
            }
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.HARKONNEN); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 8); }
}