package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmperorFactionTest extends FactionTestTemplate {
    Stream<Arguments> provideForceTypes() {
        return Stream.of(
                Arguments.of("Emperor", false),
                Arguments.of("Emperor*", true)
        );
    }

    private EmperorFaction faction;
    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() {
        faction = new EmperorFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 10); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 1); }

    @Test
    public void testInitialHasMiningEquipment() { assertFalse(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 15);
        assertEquals(faction.getReserves().getName(), "Emperor");

        assertEquals(faction.getSpecialReserves().getStrength(), 5);
        assertEquals(faction.getSpecialReserves().getName(), "Emperor*");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.EMPEROR); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 4); }
}