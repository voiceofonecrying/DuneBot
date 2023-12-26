package model.factions;

import constants.Emojis;
import model.Territory;
import model.TestTopic;
import model.TreacheryCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EmperorFactionTest extends FactionTestTemplate {
    private EmperorFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new EmperorFaction("player", "player", game);
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
        assertEquals(faction.getReserves().getStrength(), 15);
        assertEquals(faction.getReserves().getName(), "Emperor");

        assertEquals(faction.getSpecialReserves().getStrength(), 5);
        assertEquals(faction.getSpecialReserves().getName(), "Emperor*");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.EMPEROR);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    public void testKaitainHighDiscard() {
        TestTopic turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);
        TestTopic emperorLedger = new TestTopic();
        faction.setLedger(emperorLedger);
        TreacheryCard kulon = game.getTreacheryDeck().stream()
                .filter(t -> t.name().equals("Kulon"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Kulon not found"));
        faction.addTreacheryCard(kulon);
        assertEquals(10, faction.getSpice());
        assertTrue(faction.hasTreacheryCard("Kulon"));
        assertTrue(turnSummary.getMessages().isEmpty());

        faction.kaitainHighDiscard("Kulon");

        assertEquals(8, faction.getSpice());
        assertFalse(faction.hasTreacheryCard("Kulon"));
        assertEquals(Emojis.EMPEROR + " paid 2 " + Emojis.SPICE + " to discard Kulon (Kaitain High Threshold ability)",
                turnSummary.getMessages().get(0)
        );
    }
}