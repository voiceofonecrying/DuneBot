package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BGFactionTest extends FactionTestTemplate {

    private BGFaction faction;
    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() {
        faction = new BGFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 5); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 1); }

    @Test
    public void testInitialHasMiningEquipment() { assertFalse(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 20);
        assertEquals(faction.getReserves().getName(), "BG");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.BG); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 4); }

    @Test
    public void testInitialPredictionFactionName() {
        assertNull(faction.getPredictionFactionName());
    }

    @Test
    public void testInitialPredictionRound() {
        assertEquals(faction.getPredictionRound(), 0);
    }

    @Test
    public void testSetPredictionRound() {
        faction.setPredictionRound(1);
        assertEquals(faction.getPredictionRound(), 1);
    }

    @Test
    public void testSetPredictionRoundInvalid() {
        assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(0));
        assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(11));
    }

    @Test
    public void testSetPredictionFactionName() {
        faction.setPredictionFactionName("Atreides");
        assertEquals(faction.getPredictionFactionName(), "Atreides");
    }
}