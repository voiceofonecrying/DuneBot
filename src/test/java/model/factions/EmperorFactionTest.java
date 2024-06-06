package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
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
        assertEquals(15, faction.getReservesStrength());
        assertEquals(5, faction.getSpecialReservesStrength());
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
        territory.addForces("Emperor", regularAmount);

        int specialAmount = 1;
        faction.removeSpecialReserves(specialAmount);
        territory.addForces("Emperor*", specialAmount);

        assertEquals(13, game.getTerritory("Kaitain").getForceStrength("Emperor"));
        assertEquals(4, game.getTerritory("Salusa Secundus").getForceStrength("Emperor*"));

        faction.removeForces("Habbanya Sietch", 2, false, false);
        faction.removeForces("Habbanya Sietch", 1, true, false);

        assertEquals(15, game.getTerritory("Kaitain").getForceStrength("Emperor"));
        assertEquals(5, game.getTerritory("Salusa Secundus").getForceStrength("Emperor*"));
    }

    @Test
    public void testRemovePullsFromSecundusIfNecessary() {
        Territory kaitain = game.getTerritory("Kaitain");
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        int forcesToRemove = kaitain.getForceStrength(faction.getName()) - 1;
        kaitain.removeForces(faction.getName(), forcesToRemove);
        salusaSecundus.addForces(faction.getName(), 1);
        assertDoesNotThrow(() -> faction.removeReserves(2));
    }

    @Test
    public void testRemoveSpecialPullsFromKaitainIfNecessary() {
        Territory salusaSecundus = game.getTerritory("Salusa Secundus");
        Territory kaitain = game.getTerritory("Kaitain");
        int forcesToRemove = salusaSecundus.getForceStrength(faction.getName() + "*") - 1;
        salusaSecundus.removeForces(faction.getName() + "*", forcesToRemove);
        kaitain.addForces(faction.getName() + "*", 1);
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