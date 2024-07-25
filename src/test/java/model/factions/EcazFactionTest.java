package model.factions;

import constants.Emojis;
import enums.GameOption;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EcazFactionTest extends FactionTestTemplate {
    private EcazFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new EcazFaction("player", "player", game);
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(12, faction.getSpice());
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
        assertEquals(14, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            if (territory.getTerritoryName().equals("Imperial Basin (Center Sector)")) {
                assertEquals(6, territory.getForceStrength("Ecaz"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.ECAZ);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Nested
    @DisplayName("#checkForTerrorTrigger")
    class CheckForTerrorTrigger {
        AtreidesFaction atreides;
        BGFaction bg;
        Territory carthag;
        TestTopic turnSummary;
        TestTopic ecazChat;

        @BeforeEach
        void setUp() throws IOException {
            ecazChat = new TestTopic();
            faction.setChat(ecazChat);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            atreides = new AtreidesFaction("p", "u", game);
            bg = new BGFaction("p", "u", game);
            carthag = game.getTerritory("Carthag");
            carthag.setEcazAmbassador("BG");
        }

        @Test
        void testEcazDoesNotTrigger() {
            faction.checkForAmbassadorTrigger(carthag, faction);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(ecazChat.getMessages().isEmpty());
        }

        @Test
        void testOtherFactionTriggers() {
            faction.checkForAmbassadorTrigger(carthag, atreides);
            assertEquals(Emojis.ECAZ + " has an opportunity to trigger their BG ambassador.", turnSummary.getMessages().getFirst());
            assertTrue(ecazChat.getMessages().getFirst().contains("Will you trigger your BG ambassador against Atreides in Carthag?"));
            assertEquals(2, ecazChat.getChoices().getFirst().size());
        }

        @Test
        void testAllyDoesNotTrigger() {
            faction.setAlly("Atreides");
            atreides.setAlly("Ecaz");
            faction.checkForAmbassadorTrigger(carthag, atreides);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(ecazChat.getMessages().isEmpty());
        }

        @Test
        void testAmbassadorFactionDoesNotTrigger() {
            faction.checkForAmbassadorTrigger(carthag, bg);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(ecazChat.getMessages().isEmpty());
        }
    }
}
