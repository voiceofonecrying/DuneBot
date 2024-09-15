package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import model.TreacheryCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
        faction = new EmperorFaction("player", "player");
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 10);
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testFreeReviveStars() {
            EmperorFaction emperor = (EmperorFaction) faction;
            faction.removeForces(emperor.getSecondHomeworld(), 3, true, true);
            assertEquals(1, faction.countFreeStarredRevival());
        }

        @Test
        @Override
        public void testPaidRevivalMessageAfter1StarFree() throws InvalidGameStateException {
            EmperorFaction emperor = (EmperorFaction) faction;
            assertDoesNotThrow(() -> faction.removeForces(emperor.getSecondHomeworld(), 2, true, true));
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(1);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
            assertEquals(faction.getEmoji() + " has no revivable forces in the tanks", faction.getPaidRevivalMessage());
        }
    }

    @Test
    public void testFreeReviveStarsNoneInTanks() {
        assertEquals(0, faction.countFreeStarredRevival());
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
    public void testEmperorCanPayForOneSardaukar() throws InvalidGameStateException {
        TestTopic chat = new TestTopic();
        faction.setChat(chat);
        TestTopic ledger = new TestTopic();
        faction.setLedger(ledger);
        faction.removeForces(faction.getHomeworld(), 1, false, true);
        faction.removeForces(faction.getSecondHomeworld(), 1, true, true);
        faction.setStarRevived(false);
        faction.presentPaidRevivalChoices(1);
        assertEquals("Would you like to purchase additional revivals including 1 " + Emojis.EMPEROR_SARDAUKAR + "? " + faction.getPlayer(), chat.getMessages().getFirst());
        assertEquals(2, faction.getRevivableForces());
        assertEquals(3, chat.getChoices().getFirst().size());
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(15, faction.getReservesStrength());
        assertEquals(5, faction.getSpecialReservesStrength());
        assertEquals(20, faction.getTotalReservesStrength());
        assertEquals(0, game.getTerritory(faction.getHomeworld()).getForceStrength("Emperor*"));
        assertEquals(0, game.getTerritory(faction.getSecondHomeworld()).getForceStrength("Emperor"));
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
        assertInstanceOf(HomeworldTerritory.class, territory);
        assertFalse(territory.isDiscoveryToken());
        assertFalse(territory.isNearShieldWall());
        assertFalse(territory.isRock());
    }

    @Nested
    @DisplayName("#homeworld")
    class Homeworld extends FactionTestTemplate.Homeworld {
        @Test
        public void testSecondHomweworldDialAdvantageHighThreshold() {
            HomeworldTerritory secondTerritory = (HomeworldTerritory) game.getTerritories().get(faction.getSecondHomeworld());
            assertEquals(0, faction.homeworldDialAdvantage(game, secondTerritory));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(3, faction.homeworldDialAdvantage(game, secondTerritory));
        }

        @Test
        @Override
        public void testHomweworldDialAdvantageLowThreshold() {
            int numForces = territory.getForceStrength(faction.getName());
            int numSpecials = territory.getForceStrength(faction.getName() + "*");
            game.removeForces(territory.getTerritoryName(), faction, numForces, numSpecials, true);
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.checkForLowThreshold();
            assertFalse(faction.isHighThreshold());
            assertEquals(3, faction.homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testSecondHomweworldDialAdvantageLowThreshold() {
            HomeworldTerritory secondTerritory = (HomeworldTerritory) game.getTerritories().get(faction.getSecondHomeworld());
            int numSpecials = secondTerritory.getForceStrength(faction.getName() + "*");
            game.removeForces(secondTerritory.getTerritoryName(), faction, 0, numSpecials, true);
            assertEquals(0, faction.homeworldDialAdvantage(game, secondTerritory));
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.checkForLowThreshold();
            assertEquals(2, faction.homeworldDialAdvantage(game, secondTerritory));
        }
    }

    @Nested
    @DisplayName("#secondHomeworldOccupation")
    class SecondHomeworldOccupation {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(new HarkonnenFaction("p", "u"));
        }

        @Test
        public void testNoReturnToHighThresholdWhileOccupied() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForce(faction.getName() + "*");
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertTrue(faction.isSecundusOccupied());
            assertFalse(faction.isSecundusHighThreshold());
        }

        @Test
        public void testNoReturnToHighThresholdWith2Sardaukar() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForce(faction.getName() + "*");
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertFalse(faction.isSecundusHighThreshold());
            assertTrue(faction.isSecundusOccupied());
            territory.removeForces(faction.getName() + "*", 1);
            territory.removeForce("Harkonnen");
            assertFalse(faction.isSecundusHighThreshold());
            assertFalse(faction.isSecundusOccupied());
        }

        @Test
        public void testReturnToHighThresholdWith3Sardaukar() {
            String homeworldName = faction.getSecondHomeworld();
            HomeworldTerritory territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
            game.addGameOption(GameOption.HOMEWORLDS);
            territory.removeForce(faction.getName() + "*");
            assertFalse(faction.isSecundusHighThreshold());
            territory.addForces("Harkonnen", 1);
            territory.addForces(faction.getName() + "*", 3);
            assertFalse(faction.isSecundusHighThreshold());
            assertTrue(faction.isSecundusOccupied());
            territory.removeForce("Harkonnen");
            assertTrue(faction.isSecundusHighThreshold());
            assertFalse(faction.isSecundusOccupied());
        }
    }

    @Test
    void testGetSpiceSupportPhasesString() {
        assertEquals(" for bidding, shipping, and battles!", getFaction().getSpiceSupportPhasesString());
    }
}