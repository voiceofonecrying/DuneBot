package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class HarkonnenFactionTest extends FactionTestTemplate {

    private HarkonnenFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new HarkonnenFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 10);
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
        assertTrue(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(10, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            if (territory.getTerritoryName().equals("Carthag")) {
                assertEquals(10, territory.getForceStrength("Harkonnen"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.HARKONNEN);
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.HARKONNEN_TROOP, faction.getForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 8);
    }

    @Nested
    @DisplayName("#keepCapturedLeader")
    class KeepCapturedLeader {
        AtreidesFaction atreides;
        TestTopic atreidesChat;
        Leader duncanIdaho;

        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("at", "at");
            atreidesChat = new TestTopic();
            atreides.setChat(atreidesChat);
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        }

        @Test
        void testHarkonnenKeepsLeader() {
            faction.keepCapturedLeader("Atreides", "Duncan Idaho");
            assertTrue(faction.getLeaders().contains(duncanIdaho));
            assertFalse(atreides.getLeaders().contains(duncanIdaho));
            assertEquals("You have captured Duncan Idaho.", ledger.getMessages().getLast());
            assertEquals("Duncan Idaho has been captured by the treacherous " + Emojis.HARKONNEN + "!", atreidesChat.getMessages().getLast());
        }

        @Test
        void testVictimDoesNotHaveLeader() {
            game.killLeader(atreides, "Duncan Idaho");
            assertThrows(NoSuchElementException.class, () -> faction.keepCapturedLeader("Atreides", "Duncan Idaho"));
        }

        @Test
        void testCaptureLeader() {
            faction.keepCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertEquals(Emojis.HARKONNEN + " has captured a Leader from " + Emojis.ATREIDES, turnSummary.getMessages().getLast());
        }

        @Test
        void testCaptureSkilledLeader() throws InvalidGameStateException {
            LeaderSkillCard swordmaster = game.getLeaderSkillDeck().stream().filter(ls -> ls.name().equals("Swordmaster of Ginaz")).findFirst().orElseThrow();
            game.getLeaderSkillDeck().remove(swordmaster);
            duncanIdaho.setSkillCard(swordmaster);
            assertFalse(game.getLeaderSkillDeck().contains(swordmaster));
            faction.keepCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertEquals(Emojis.HARKONNEN + " has captured the " + Emojis.ATREIDES + " skilled leader, Duncan Idaho the Swordmaster of Ginaz.", turnSummary.getMessages().getLast());
            assertSame(swordmaster, duncanIdaho.getSkillCard());
            assertFalse(game.getLeaderSkillDeck().contains(swordmaster));
        }

        @Test
        void testKeepDukeVidal() throws IOException {
            EcazFaction ecaz = new EcazFaction("ec", "ec");
            game.addFaction(ecaz);
            ecaz.setChat(new TestTopic());
            ecaz.setLedger(new TestTopic());
            ecaz.addLeader(game.getDukeVidal());
            faction.keepCapturedLeader("Ecaz", "Duke Vidal");
            assertTrue(faction.isDukeVidalCaptured());
        }
    }

    @Nested
    @DisplayName("#killCapturedLeader")
    class KillCapturedLeader {
        AtreidesFaction atreides;
        TestTopic atreidesChat;
        Leader duncanIdaho;
        BTFaction bt;
        TestTopic btChat;

        @BeforeEach
        void setUp() throws IOException {
            atreides = new AtreidesFaction("at", "at");
            atreidesChat = new TestTopic();
            atreides.setChat(atreidesChat);
            atreides.setLedger(new TestTopic());
            game.addFaction(atreides);
            bt = new BTFaction("bt", "bt");
            btChat = new TestTopic();
            bt.setChat(btChat);
            game.addFaction(bt);
            duncanIdaho = atreides.getLeader("Duncan Idaho").orElseThrow();
        }

        @Test
        void testKillLeaderGainsSpice() {
            faction.killCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertEquals(12, faction.getSpice());
            assertEquals(Emojis.HARKONNEN + " has killed the " + Emojis.ATREIDES + " leader for 2 " + Emojis.SPICE, turnSummary.getMessages().getLast());
        }

        @Test
        void testKilledLeaderIsFaceDown() {
            faction.killCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertTrue(duncanIdaho.isFaceDown());
        }

        @Test
        void testFaceDownLeaderReportedToBT() {
            faction.killCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertTrue(bt.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertEquals(Emojis.ATREIDES + " Duncan Idaho (2) is face down in the tanks.", btChat.getMessages().getLast());
        }

        @Test
        void testKillSkilledLeader() throws InvalidGameStateException {
            LeaderSkillCard swordmaster = game.getLeaderSkillDeck().stream().filter(ls -> ls.name().equals("Swordmaster of Ginaz")).findFirst().orElseThrow();
            game.getLeaderSkillDeck().remove(swordmaster);
            duncanIdaho.setSkillCard(swordmaster);
            assertFalse(game.getLeaderSkillDeck().contains(swordmaster));
            faction.killCapturedLeader(duncanIdaho.getOriginalFactionName(), duncanIdaho.getName());
            assertEquals(Emojis.HARKONNEN + " has killed the " + Emojis.ATREIDES + " skilled leader, Duncan Idaho, for 2 " + Emojis.SPICE, turnSummary.getMessages().getLast());
            assertTrue(game.getLeaderSkillDeck().contains(swordmaster));
        }
    }

    @Nested
    @DisplayName("#returnCapturedLeader")
    class ReturnCapturedLeader {
        @Test
        void testReturnDuncanIdaho() throws IOException {
            AtreidesFaction atreides = new AtreidesFaction("at", "at");
            game.addFaction(atreides);
            atreides.setChat(new TestTopic());
            atreides.setLedger(new TestTopic());
            faction.keepCapturedLeader("Atreides", "Duncan Idaho");
            assertFalse(atreides.getLeader("Duncan Idaho").isPresent());
            faction.returnCapturedLeader("Duncan Idaho");
            assertTrue(atreides.getLeader("Duncan Idaho").isPresent());
        }

        @Test
        void testReleaseDukeVidal() throws IOException {
            MoritaniFaction moritani = new MoritaniFaction("mo", "mo");
            game.addFaction(moritani);
            moritani.setChat(new TestTopic());
            moritani.setLedger(new TestTopic());
            moritani.addLeader(game.getDukeVidal());
            faction.keepCapturedLeader("Moritani", "Duke Vidal");
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            faction.returnCapturedLeader("Duke Vidal");
            assertFalse(moritani.getLeader("Duke Vidal").isPresent());
            assertFalse(game.getFactions().stream().anyMatch(f -> f.getLeaders().stream().anyMatch(l -> l.getName().equals("Duke Vidal"))));
        }
    }

    @Test
    public void testNexusCardBetrayal() throws IOException {
        AtreidesFaction atreides = new AtreidesFaction("at", "at");
        game.addFaction(atreides);
        TraitorCard drYueh = new TraitorCard("Dr. Yueh", "Atreides", 1);
        TraitorCard feydRautha = new TraitorCard("Feyd Rautha", "Harkonnen", 6);
        TraitorCard alia = new TraitorCard("Alia", "BG", 5);
        TraitorCard jamis = new TraitorCard("Jamis", "Fremen", 2);
        faction.addTraitorCard(drYueh);
        faction.addTraitorCard(feydRautha);
        faction.addTraitorCard(alia);
        faction.addTraitorCard(jamis);
        faction.nexusCardBetrayal(drYueh.getName());
        assertEquals(3, faction.getTraitorHand().size());
        assertFalse(faction.getTraitorHand().contains(drYueh));
        assertTrue(faction.nexusBetrayalTraitorNeeded);
        assertTrue(game.getTraitorDeck().contains(drYueh));
        assertEquals("Dr. Yueh has been shuffled back into the Traitor Deck.", ledger.getMessages().getLast());
        assertEquals(faction.getEmoji() + " loses Dr. Yueh and will draw a new Traitor in Mentat Pause.", turnSummary.getMessages().getLast());
    }
}