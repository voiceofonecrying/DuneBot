package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.Territory;
import model.TraitorCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BTFactionTest extends FactionTestTemplate {

    private BTFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new BTFaction("player", "player", game);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 5);
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
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testMaxRevivals() {
        assertEquals(faction.getMaxRevival(), 20);
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(faction.getReservesStrength(), 20);
        assertEquals(faction.getReserves().getName(), "BT");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(0, territory.countFactions());
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.BT);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    public void testInitialRevealedFaceDancers() {
        assertEquals(faction.getRevealedFaceDancers().size(), 0);
    }

    @Test
    public void testFaceDancerReveal() {
        TraitorCard revealedTraitor = game.getTraitorDeck().get(0);
        faction.getTraitorHand().add(revealedTraitor);
        faction.getTraitorHand().add(game.getTraitorDeck().get(1));
        faction.revealFaceDancer(revealedTraitor, game);
        assertEquals(faction.getRevealedFaceDancers().size(), 1);
        assertEquals(faction.getRevealedFaceDancers().stream().toList().getFirst(), revealedTraitor);
        assertEquals(faction.getTraitorHand().size(), 1);
        assertNotEquals(faction.getTraitorHand().getFirst(), revealedTraitor);
    }

    @Test
    public void testInvalidFaceDancerRevealEmptyTraitorHand() {
        TraitorCard revealedTraitor = game.getTraitorDeck().getFirst();
        assertThrows(IllegalArgumentException.class, () -> faction.revealFaceDancer(revealedTraitor, game));
    }

    @Test
    public void testInvalidFaceDancerReveal() {
        TraitorCard revealedTraitor = game.getTraitorDeck().get(0);
        faction.getTraitorHand().add(game.getTraitorDeck().get(1));
        faction.getTraitorHand().add(game.getTraitorDeck().get(2));
        assertThrows(IllegalArgumentException.class, () -> faction.revealFaceDancer(revealedTraitor, game));
    }

}