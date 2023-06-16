package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import model.TraitorCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChoamFactionTest {

    private Game game;
    private ChoamFaction faction;

    @BeforeEach
    void setUp() {
        game = new Game();
        faction = new ChoamFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 2); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 0); }

    @Test
    public void testInitialHasMiningEquipment() { assertFalse(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 20);
        assertEquals(faction.getReserves().getName(), "CHOAM");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.CHOAM); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 5); }
}