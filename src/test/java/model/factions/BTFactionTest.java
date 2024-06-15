package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TraitorCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 5);
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testInitialMaxRevival() {
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testMaxRevivalSetTo5() {
            faction.setMaxRevival(5);
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testMaxRevivalWithRecruits() throws InvalidGameStateException {
            game.startRevival();
            game.getRevival().setRecruitsInPlay(true);
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testRevivalCost() {
            assertEquals(2, faction.revivalCost(2, 0));
        }

        @Test
        @Override
        public void testRevivalCostAlliedWithBT() {
            // Not really a valid scenario, but test needed for completeness
            faction.setAlly("BT");
            assertEquals(1, faction.revivalCost(2, 0));
        }

        @Test
        @Override
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.reviveForces(faction, false, freeRevivals, 0);
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(5 - freeRevivals + 1, chat.getChoices().getFirst().size());
        }
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
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(3, faction.getFreeRevival());
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
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(faction.getName(), forcesToRemove);
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