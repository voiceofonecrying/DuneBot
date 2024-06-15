package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BGFactionTest extends FactionTestTemplate {

    private BGFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new BGFaction("player", "player", game);
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 5);
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
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        assertEquals(20, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            assertEquals(0, territory.countFactions());
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.BG);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

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

    @Test
    public void testFlipForces() {
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForces("BG", 1);
        assertEquals(1, carthag.getForceStrength("BG"));
        assertEquals(0, carthag.getForceStrength("Advisor"));
        faction.flipForces(carthag);
        assertEquals(0, carthag.getForceStrength("BG"));
        assertEquals(1, carthag.getForceStrength("Advisor"));
        faction.flipForces(carthag);
        assertEquals(1, carthag.getForceStrength("BG"));
        assertEquals(0, carthag.getForceStrength("Advisor"));
    }
}