package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.MoveType;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdualiFactionTest extends FactionTestTemplate {

    private IdualiFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new IdualiFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(10, faction.getSpice());
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testPaidRevival() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.reviveForces(faction, false, 2, 0);
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.getForceEmoji("Iduali") + " for free.", turnSummary.getMessages().getLast());
        }

        @Test
        @Override
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
        }
    }

    @Nested
    @DisplayName("#reviveForcesWithBTAmbassador")
    class ReviveForcesWithBTAmbassador extends FactionTestTemplate.ReviveForcesWithBTAmbassador {
        @Test
        @Override
        void testTwoForcesAndTwoSpecialsInTanks() {
            faction.removeForces(faction.getHomeworld(), 2, false, true);
            faction.reviveForcesWithBTAmbassador();
            assertEquals(faction.getEmoji() + " revives 2 " + faction.getForceEmoji() + " for free.", turnSummary.getMessages().getLast());
            assertEquals("You revived 2 " + faction.getForceEmoji() + " with the BT Ambassador.", chat.getMessages().getLast());
        }
    }

    @Test
    public void testFreeRevival() {
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThreshold() {
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Nested
    @DisplayName("#executeShipment")
    class ExecuteShipment extends FactionTestTemplate.ExecuteShipment {
        @Override
        @Test
        // Shipment to Sietch Tabr, not in range, isn't free
        void testPaidShipment() throws InvalidGameStateException {
            int spice = faction.getSpice();
            faction.executeShipment(game, false, false);
            assertEquals(spice - 1, faction.getSpice());
        }
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
        assertEquals(Emojis.getFactionEmoji("Iduali"), faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.getForceEmoji("Iduali"), faction.getForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
    }
}