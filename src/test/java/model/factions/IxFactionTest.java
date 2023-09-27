package model.factions;

import constants.Emojis;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IxFactionTest extends FactionTestTemplate {

    private IxFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new IxFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 10);
    }

    @Test
    public void testFreeRevivals() {
        assertEquals(faction.getFreeRevival(), 1);
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 10);
        assertEquals(faction.getReserves().getName(), "Ix");

        assertEquals(faction.getSpecialReserves().getStrength(), 4);
        assertEquals(faction.getSpecialReserves().getName(), "Ix*");
    }

    @Test
    public void testInitialForcePlacement() {
        assertTrue(game.getTerritories().containsKey("Hidden Mobile Stronghold"));

        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
            Territory territory = game.getTerritories().get(territoryName);
            if (territoryName.equals("Hidden Mobile Stronghold")) {
                assertEquals(territory.getForces().get(0).getStrength(), 3);
                assertEquals(territory.getForces().get(0).getName(), "Ix");

                assertEquals(territory.getForces().get(1).getStrength(), 3);
                assertEquals(territory.getForces().get(1).getName(), "Ix*");
            } else {
                assertEquals(territory.getForces().size(), 0);
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.IX);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }
}