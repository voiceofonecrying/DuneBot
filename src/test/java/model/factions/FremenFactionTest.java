package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FremenFactionTest extends FactionTestTemplate {

    private FremenFaction faction;
    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() {
        faction = new FremenFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 3); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 3); }

    @Test
    public void testInitialHasMiningEquipment() { assertFalse(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 17);
        assertEquals(faction.getReserves().getName(), "Fremen");

        assertEquals(faction.getSpecialReserves().getStrength(), 3);
        assertEquals(faction.getSpecialReserves().getName(), "Fremen*");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.FREMEN); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 4); }
}