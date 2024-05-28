package model.factions;

import constants.Emojis;
import enums.GameOption;
import model.Force;
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
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(2, faction.getFreeRevival());
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
    public void testFreeRevivalWithRecruits() {
        game.setRecruitsInPlay(true);
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() {
        game.setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
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
        assertEquals(15, faction.getReservesStrength(), 15);
        assertEquals(faction.getReserves().getName(), "Emperor");

        assertEquals(5, faction.getSpecialReservesStrength(), 5);
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
                turnSummary.getMessages().getFirst()
        );
    }

    @Test
    public void sardaukarGetRemovedToSalusaSecundus() {
        Territory territory = game.getTerritory("Habbanya Sietch");

        int regularAmount = 2;
        faction.removeReserves(regularAmount);
        Force territoryForce = territory.getForce("Emperor");
        territory.setForceStrength("Emperor", territoryForce.getStrength() + regularAmount);

        int specialAmount = 1;
        faction.removeSpecialReserves(specialAmount);
        territoryForce = territory.getForce("Emperor*");
        territory.setForceStrength("Emperor*", territoryForce.getStrength() + specialAmount);

        assertEquals(13, game.getTerritory("Kaitain").getForce("Emperor").getStrength());
        assertEquals(4, game.getTerritory("Salusa Secundus").getForce("Emperor*").getStrength());

        faction.removeForces("Habbanya Sietch", 2, false, false);
        faction.removeForces("Habbanya Sietch", 1, true, false);

        assertEquals(15, game.getTerritory("Kaitain").getForce("Emperor").getStrength());
        assertEquals(5, game.getTerritory("Salusa Secundus").getForce("Emperor*").getStrength());
    }

    @Test
    public void testRemovePullsFromSecundusIfNecessary() {
        Territory kaitain = game.getTerritory("Kaitain");
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        kaitain.setForceStrength("Emperor", 1);
        salusaSecundus.setForceStrength("Emperor", 1);
        assertDoesNotThrow(() -> faction.removeReserves(2));
    }

    @Test
    public void testRemoveSpecialPullsFromKaitainIfNecessary() {
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        Territory kaitain = game.getTerritory("Kaitain");
        salusaSecundus.setForceStrength("Emperor*", 1);
        kaitain.setForceStrength("Emperor*", 1);
        assertDoesNotThrow(() -> faction.removeSpecialReserves(2));
    }

    @Test
    public void testSecondHomeworld() {
        String homeworldName = faction.getSecondHomeworld();
        Territory territory = game.getTerritories().get(homeworldName);
        assertNotNull(territory);
        assertEquals(homeworldName, territory.getTerritoryName());
        assertEquals(-1, territory.getSector());
        assertFalse(territory.isStronghold());
        assertTrue(territory.isHomeworld());
        assertFalse(territory.isDiscoveryToken());
        assertFalse(territory.isNearShieldWall());
        assertFalse(territory.isRock());
    }
}