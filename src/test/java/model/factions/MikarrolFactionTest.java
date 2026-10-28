package model.factions;

import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MikarrolFactionTest extends FactionTestTemplate {
    private MikarrolFaction faction;
    private FremenFaction fremen;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new MikarrolFaction("player", "player");
        commonPostInstantiationSetUp();
        fremen = new FremenFaction("fr", "fr");
        game.addFaction(fremen);
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Nested
    @DisplayName("#canUseOrnithopters")
    class CanUseOrnithopters extends FactionTestTemplate.CanUseOrnithopters {
        @Test
        @Override
        void testNoOrnithopters() {
            assertFalse(faction.canUseOrnithopters(game, territory, false));
        }
    }

    @Test
    public void testInitialReserves() {
        assertEquals(20, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            else assertEquals(0, territory.countFactions());
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(":mikarrol:", faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(":mikarrol_troop:", faction.getForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(5, faction.getHandLimit());
    }

    @Nested
    @DisplayName("#raid")
    class Raid {
        AtreidesFaction atreides;
        TestTopic atreidesChat;
        TreacheryCard karama;

        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("at", "at");
            atreidesChat = new TestTopic();
            atreides.setChat(atreidesChat);
            atreides.setLedger(new TestTopic());
            atreides.addTreacheryCard(new TreacheryCard("Karama"));
            game.addFaction(atreides);

            karama = atreides.getTreacheryCard("Karama");
        }

        @Test
        void testMikarrolKeepsCard() throws InvalidGameStateException {
            faction.raid("Atreides");
            assertTrue(faction.getTreacheryHand().contains(karama));
            assertFalse(atreides.getTreacheryHand().contains(karama));
            assertEquals("Your raiders have returned, stealing Karama from " + atreides.getEmoji() + "!", ledger.getMessages().getLast());
            faction.keepRaidedCard();
            assertEquals("You will keep Karama from the raid", chat.getMessages().getLast());
            assertTrue(faction.getTreacheryHand().contains(karama));
        }

        @Test
        void testVictimDoesNotHaveCard() throws InvalidGameStateException {
            atreides.discard("Karama");
            faction.raid("Atreides");
            assertEquals("Your raiders have returned, revealing the enemy had nothing to steal.", chat.getMessages().getLast());
            assertFalse(faction.getTreacheryHand().contains(karama));
            assertFalse(atreides.getTreacheryHand().contains(karama));
        }

        @Test
        void testMikarrolDiscardsCard() throws InvalidGameStateException {
            faction.raid("Atreides");
            assertTrue(faction.getTreacheryHand().contains(karama));
            assertFalse(atreides.getTreacheryHand().contains(karama));
            assertEquals("Your raiders have returned, stealing Karama from " + atreides.getEmoji() + "!", ledger.getMessages().getLast());
            faction.discardRaidedCard();
            assertEquals("You chose not to keep the raided Karama", chat.getMessages().getLast());
            assertFalse(faction.getTreacheryHand().contains(karama));
        }
    }
}
