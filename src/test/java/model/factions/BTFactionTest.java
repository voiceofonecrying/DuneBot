package model.factions;

import constants.Emojis;
import model.Territory;
import model.TraitorCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BTFactionTest extends FactionTestTemplate {

    private BTFaction faction;
    @Override
    Faction getFaction() { return faction; }

    @BeforeEach
    void setUp() throws IOException {
        faction = new BTFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() { assertEquals(faction.getSpice(), 5); }

    @Test
    public void testFreeRevivals() { assertEquals(faction.getFreeRevival(), 2); }

    @Test
    public void testInitialHasMiningEquipment() { assertFalse(faction.hasMiningEquipment()); }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReserves().getStrength(), 20);
        assertEquals(faction.getReserves().getName(), "BT");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() { assertEquals(faction.getEmoji(), Emojis.BT); }

    @Test
    public void testHandLimit() { assertEquals(faction.getHandLimit(), 4); }

    @Test
    public void testInitialRevealedFaceDancers() {
        assertEquals(faction.getRevealedFaceDancers().size(), 0);
    }

    @Test
    public void testFaceDancerReveal() {
        TraitorCard revealedTraitor = game.getTraitorDeck().get(0);
        faction.getTraitorHand().add(revealedTraitor);
        faction.getTraitorHand().add(game.getTraitorDeck().get(1));
        faction.addRevealedFaceDancer(revealedTraitor);
        assertEquals(faction.getRevealedFaceDancers().size(), 1);
        assertEquals(faction.getRevealedFaceDancers().stream().toList().get(0), revealedTraitor);
        assertEquals(faction.getTraitorHand().size(), 1);
        assertNotEquals(faction.getTraitorHand().get(0), revealedTraitor);
    }

    @Test
    public void testInvalidFaceDancerRevealEmptyTraitorHand() {
        TraitorCard revealedTraitor = game.getTraitorDeck().get(0);
        assertThrows(IllegalArgumentException.class, () -> faction.addRevealedFaceDancer(revealedTraitor));
    }

    @Test
    public void testInvalidFaceDancerReveal() {
        TraitorCard revealedTraitor = game.getTraitorDeck().get(0);
        faction.getTraitorHand().add(game.getTraitorDeck().get(1));
        faction.getTraitorHand().add(game.getTraitorDeck().get(2));
        assertThrows(IllegalArgumentException.class, () -> faction.addRevealedFaceDancer(revealedTraitor));
    }

}