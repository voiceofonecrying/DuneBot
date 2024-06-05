package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
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
    public void testFreeRevival() {
        assertEquals(1, faction.getFreeRevival());
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
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName() + "*", 0);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName() + "*", 0);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(2, faction.getFreeRevival());
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
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName() + "*", 0);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName() + "*", 0);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(10, faction.getReservesStrength(), 10);
        assertEquals(faction.getReserves().getName(), "Ix");

        assertEquals(4, faction.getSpecialReservesStrength(), 4);
        assertEquals(faction.getSpecialReserves().getName(), "Ix*");
    }

    @Test
    public void testInitialForcePlacement() {
        assertTrue(game.getTerritories().containsKey("Hidden Mobile Stronghold"));

        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
            Territory territory = game.getTerritories().get(territoryName);
            if (territoryName.equals("Hidden Mobile Stronghold")) {
                assertEquals(3, territory.getForceStrength("Ix"));
                assertEquals(3, territory.getForceStrength("Ix*"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
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

    @Test
    public void testSpiceCollectedFromTerritory() {
        Territory theGreatFlat = game.getTerritory("The Great Flat");
        theGreatFlat.setForceStrength("Ix", 1);
        theGreatFlat.setForceStrength("Ix*", 1);
        theGreatFlat.setSpice(10);
        assertEquals(5, faction.getSpiceCollectedFromTerritory(theGreatFlat));
        theGreatFlat.setSpice(3);
        assertEquals(3, faction.getSpiceCollectedFromTerritory(theGreatFlat));
    }
}