package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Leader;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class MoritaniFactionTest extends FactionTestTemplate {
    private MoritaniFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new MoritaniFaction("player", "player", game);
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
        assertEquals(faction.getEmoji(), Emojis.MORITANI);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Nested
    @DisplayName("#assassinateLeader")
    class AssassinateLeader {
        TestTopic moritaniLedger;
        BTFaction bt;
        TestTopic btChat;
        TestTopic btLedger;
        TestTopic turnSummary;

        @BeforeEach
        public void setUp() throws IOException {
            moritaniLedger = new TestTopic();
            faction.setLedger(moritaniLedger);
            bt = new BTFaction("p", "u", game);
            btChat = new TestTopic();
            bt.setChat(btChat);
            btLedger = new TestTopic();
            bt.setLedger(btLedger);
            game.addFaction(bt);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
        }

        @Test
        public void testFactionHasLeader() {
            Leader wykk = bt.getLeader("Wykk").orElseThrow();
            assertDoesNotThrow(() -> faction.assassinateLeader(bt, wykk));
            assertEquals(Emojis.MORITANI + " collect 2 " + Emojis.SPICE + " by assassinating Wykk!", turnSummary.getMessages().getFirst());
            assertEquals("+2 " + Emojis.SPICE + " assassination of Wykk = 14 " + Emojis.SPICE, moritaniLedger.getMessages().getFirst());
        }

        @Test
        public void testLeaderIsInTanks() {
            Leader wykk = bt.getLeader("Wykk").orElseThrow();
            game.killLeader(bt, wykk.getName());
            assertThrows(IllegalArgumentException.class, () -> faction.assassinateLeader(bt, wykk));
        }

        @Test
        public void testZoalEarns3Spice() {
            Leader zoal = bt.getLeader("Zoal").orElseThrow();
            assertEquals(12, faction.getSpice());
            assertDoesNotThrow(() -> faction.assassinateLeader(bt, zoal));
            assertEquals(15, faction.getSpice());
            assertEquals(Emojis.MORITANI + " collect 3 " + Emojis.SPICE + " by assassinating Zoal!", turnSummary.getMessages().getFirst());
            assertEquals("+3 " + Emojis.SPICE + " assassination of Zoal = 15 " + Emojis.SPICE, moritaniLedger.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#checkForTerrorTrigger")
    class CheckForTerrorTrigger {
        AtreidesFaction atreides;
        Territory carthag;
        TestTopic turnSummary;
        TestTopic moritaniChat;

        @BeforeEach
        void setUp() throws IOException {
            moritaniChat = new TestTopic();
            faction.setChat(moritaniChat);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
            atreides = new AtreidesFaction("p", "u", game);
            carthag = game.getTerritory("Carthag");
            carthag.addTerrorToken("Sabotage");
        }

        @Test
        void testMoritaniDoesNotTrigger() {
            faction.checkForTerrorTrigger(carthag, faction, 3);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(moritaniChat.getMessages().isEmpty());
        }

        @Test
        void testOtherFactionTriggers() {
            faction.checkForTerrorTrigger(carthag, atreides, 3);
            assertEquals(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + Emojis.ATREIDES, turnSummary.getMessages().getFirst());
            assertTrue(moritaniChat.getMessages().getFirst().contains("Will you trigger your terror token in Carthag?"));
            assertEquals(3, moritaniChat.getChoices().getFirst().size());
        }

        @Test
        void testAllyDoesNotTrigger() {
            faction.setAlly("Atreides");
            atreides.setAlly("Moritani");
            faction.checkForTerrorTrigger(carthag, atreides, 3);
            assertTrue(turnSummary.getMessages().isEmpty());
            assertTrue(moritaniChat.getMessages().isEmpty());
        }

        @Test
        void testCannotTriggerAtLowThreshold() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(faction.isHighThreshold);
            faction.removeReserves(13);
            assertEquals("Grumman has flipped to Low Threshold.", turnSummary.getMessages().getFirst());
            assertFalse(faction.isHighThreshold);
            faction.checkForTerrorTrigger(carthag, atreides, 2);
            assertEquals(Emojis.MORITANI + " are at low threshold and may not trigger their Terror Token at this time", turnSummary.getMessages().get(1));
            assertTrue(moritaniChat.getMessages().isEmpty());
        }
    }
}
