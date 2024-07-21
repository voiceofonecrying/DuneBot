package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

abstract class FactionTestTemplate {
    Game game;

    abstract Faction getFaction();

    @BeforeEach
    void baseSetUp() throws IOException {
        game = new Game();
        game.setTurnSummary(new TestTopic());
    }

    @Nested
    @DisplayName("#revival")
    class Revival {
        Faction faction;
        TestTopic chat;
        TestTopic ledger;
        int freeRevivals;

        @BeforeEach
        public void setUp() throws InvalidGameStateException {
            faction = getFaction();
            freeRevivals =  faction.getFreeRevival();
            chat = new TestTopic();
            faction.setChat(chat);
            ledger = new TestTopic();
            faction.setLedger(ledger);
            game.startRevival();
        }

        @Test
        public void testInitialMaxRevival() {
            assertEquals(3, faction.getMaxRevival());
        }

        @Test
        public void testMaxRevivalSetTo5() {
            faction.setMaxRevival(5);
            assertEquals(5, faction.getMaxRevival());
        }

        @Test
        public void testMaxRevivalWithRecruits() throws InvalidGameStateException {
            game.getRevival().setRecruitsInPlay(true);
            assertEquals(7, faction.getMaxRevival());
        }

        @Test
        public void testRevivalCost() {
            assertEquals(4, faction.revivalCost(1, 1));
        }

        @Test
        public void testRevivalCostAlliedWithBT() {
            faction.setAlly("BT");
            assertEquals(2, faction.revivalCost(1, 1));
        }

        @Test
        public void testFreeReviveStars() {
            assertThrows(IllegalArgumentException.class, () -> faction.removeForces(faction.getHomeworld(), 1, true, true));
        }

        @Test
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.reviveForces(faction, false, freeRevivals, 0);
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
        }

        @Test
        public void testPaidRevivalChoicesInsufficientSpice() throws InvalidGameStateException {
            faction.setSpice(0);
            faction.setMaxRevival(5);
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            game.reviveForces(faction, false, freeRevivals, 0);
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals("You do not have enough " + Emojis.SPICE + " to purchase additional revivals.", chat.getMessages().getFirst());
            assertEquals(0, chat.getChoices().size());
        }
    }

    @Nested
    @DisplayName("#homeworld")
    class Homeworld {
        String homeworldName;
        HomeworldTerritory territory;

        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(new HarkonnenFaction("p", "u", game));
            game.addFaction(new EmperorFaction("p", "u", game));
            homeworldName = getFaction().getHomeworld();
            territory = (HomeworldTerritory) game.getTerritories().get(homeworldName);
        }

        @Test
        public void testHomeworld() {
            assertNotNull(territory);
            assertEquals(homeworldName, territory.getTerritoryName());
            assertEquals(-1, territory.getSector());
            assertFalse(territory.isStronghold());
            assertInstanceOf(HomeworldTerritory.class, territory);
            assertFalse(territory.isDiscoveryToken());
            assertFalse(territory.isNearShieldWall());
            assertFalse(territory.isRock());
        }

        @Test
        public void testHomweworldDialAdvantageHighThreshold() {
            assertTrue(getFaction().isHighThreshold());
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(2, getFaction().homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testHomweworldDialAdvantageLowThreshold() {
            int numForces = territory.getForceStrength(getFaction().getName());
            int numSpecials = territory.getForceStrength(getFaction().getName() + "*");
            game.removeForces(territory.getTerritoryName(), getFaction(), numForces, numSpecials, true);
            assertEquals(0, getFaction().homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            getFaction().checkForLowThreshold();
            assertFalse(getFaction().isHighThreshold());
            assertEquals(2, getFaction().homeworldDialAdvantage(game, territory));
        }

        @Test
        public void testNativeStartsAtHighThreshold() {
            assertTrue(getFaction().isHighThreshold());
        }

        @Test
        public void testNoReturnToHighThresholdWhileOccupied() {
            game.addGameOption(GameOption.HOMEWORLDS);
            int strength = territory.getForceStrength(getFaction().getName());
            territory.removeForce(getFaction().getName());
            territory.removeForce(getFaction().getName() + "*");
            getFaction().checkForLowThreshold();
            assertFalse(getFaction().isHighThreshold());
            String occupierName = getFaction().getName().equals("Harkonnen") ? "Emperor" : "Harkonnen";
            territory.addForces(occupierName, 1);
            territory.addForces(getFaction().getName(), strength);
            assertTrue(getFaction().isHomeworldOccupied());
            assertFalse(getFaction().isHighThreshold());
        }
    }

    @Nested
    @DisplayName("#addSpice")
    class AddSpice {
        Faction faction;

        @BeforeEach
        void setUpAddSpice() {
            faction = getFaction();
            faction.setSpice(10);
            TestTopic ledger = new TestTopic();
            faction.setLedger(ledger);
        }

        @Test
        void validPositive() {
            faction.addSpice(2, "test");
            assertEquals(faction.getSpice(), 12);
        }

        @Test
        void zero() {
            faction.addSpice(0, "test");
            assertEquals(faction.getSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.addSpice(-1, "test"));
        }
    }

    @Nested
    @DisplayName("#subtractSpice")
    class SubtractSpice {
        Faction faction;

        @BeforeEach
        void setUpRemoveSpice() {
            faction = getFaction();
            faction.setSpice(10);
            TestTopic ledger = new TestTopic();
            faction.setLedger(ledger);
        }

        @Test
        void validPositive() {
            faction.subtractSpice(2, "test");
            assertEquals(faction.getSpice(), 8);
        }

        @Test
        void zero() {
            faction.subtractSpice(0, "test");
            assertEquals(faction.getSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.subtractSpice(-2, "test"));
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractSpice(11, "test"));
        }
    }

    @Nested
    @DisplayName("#addFrontOfShieldSpice")
    class AddFrontOfShieldSpice {
        Faction faction;

        @BeforeEach
        void setUpAddSpice() {
            faction = getFaction();
            faction.setFrontOfShieldSpice(10);
        }

        @Test
        void validPositive() {
            faction.addFrontOfShieldSpice(2);
            assertEquals(faction.getFrontOfShieldSpice(), 12);
        }

        @Test
        void zero() {
            faction.addFrontOfShieldSpice(0);
            assertEquals(faction.getFrontOfShieldSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.addFrontOfShieldSpice(-1));
        }
    }

    @Nested
    @DisplayName("#subtractFrontOfShieldSpice")
    class SubtractFrontOfShieldSpice {
        Faction faction;

        @BeforeEach
        void setUpRemoveSpice() {
            faction = getFaction();
            faction.setFrontOfShieldSpice(10);
        }

        @Test
        void validPositive() {
            faction.subtractFrontOfShieldSpice(2);
            assertEquals(faction.getFrontOfShieldSpice(), 8);
        }

        @Test
        void zero() {
            faction.subtractFrontOfShieldSpice(0);
            assertEquals(faction.getFrontOfShieldSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.subtractFrontOfShieldSpice(-2));
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractFrontOfShieldSpice(11));
        }
    }

    @Nested
    @DisplayName("#removeForces")
    class RemoveForces {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUp() {
            faction = getFaction();

            territory = game.getTerritories().get("Sihaya Ridge");
            forceName = faction.getName();

            territory.addForces(forceName, 5);
            faction.removeReserves(5);
        }

        @Test
        void validToReserves() {
            int reservesStrength = faction.getReservesStrength();
            faction.removeForces(territory.getTerritoryName(), forceName, 2, false, false);
            assertEquals(3, territory.getForceStrength(forceName));
            assertEquals(reservesStrength + 2, faction.getReservesStrength());
            assertEquals(0, game.getTanks().stream().filter(tank -> tank.getName().equals(forceName)).count());
        }

        @Test
        void validToTanks() {
            int reservesStrength = faction.getReservesStrength();
            TleilaxuTanks tanks = game.getTleilaxuTanks();
            int tanksStrength = tanks.getForceStrength(forceName);
            faction.removeForces(territory.getTerritoryName(), forceName, 2, true, true);
            assertEquals(3, territory.getForceStrength(forceName));
            assertEquals(tanksStrength + 2, tanks.getForceStrength(forceName));
            assertEquals(reservesStrength, faction.getReservesStrength());
        }

        @Test
        void invalidToReservesTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, false, false));
        }

        @Test
        void invalidToReservesNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, false, false));
        }

        @Test
        void invalidToTanksTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, true, false));
        }

        @Test
        void invalidToTanksNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, true, false));
        }
    }

    @Nested
    @DisplayName("#isNearShieldWall")
    class IsNearShieldWall {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            forceName = faction.getName();
        }

        @Test
        void trueOnShieldWall() {
            territory = game.getTerritories().get("Shield Wall (North Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInImperialBasin() {
            territory = game.getTerritories().get("Imperial Basin (Center Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInFalseWallEast() {
            territory = game.getTerritories().get("False Wall East (Far North Sector)");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInGaraKulon() {
            territory = game.getTerritories().get("Gara Kulon");
            territory.addForces(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }
    }

    @Nested
    @DisplayName("#hasTreacheryCard")
    class HasTreacheryCard {
        Faction faction;
        TreacheryCard familyAtomics;

        @BeforeEach
        void setUp() {
            faction = getFaction();

            familyAtomics = game.getTreacheryDeck().stream()
                    .filter(t -> t.name().equals("Family Atomics"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Family Atomics not found"));
        }

        @Test
        void hasFamilyAtomics() {
            faction.addTreacheryCard(familyAtomics);
            assertTrue(faction.hasTreacheryCard("Family Atomics"));
        }

        @Test
        void doesNotHaveFamilyAtomics() {
            assertFalse(faction.hasTreacheryCard("Family Atomics"));
        }
    }

    @Nested
    @DisplayName("hmsStrongholdProxy")
    class HMSStrongholdProxy {
        Faction faction;
        StrongholdCard arrakeenCard;
        StrongholdCard hmsCard;

        @BeforeEach
        void setUp() {
            faction = getFaction();
            arrakeenCard = new StrongholdCard("Arrakeen");
            hmsCard = new StrongholdCard("Hidden Mobile Stronghold");
        }

        @Test
        void testHasArrakeenButNotHMS() {
            faction.addStrongholdCard(arrakeenCard);
            assertThrows(InvalidGameStateException.class, () -> faction.setHmsStrongholdProxy(arrakeenCard));
        }

        @Test
        void testHasHMSButNotArrakeen() {
            faction.addStrongholdCard(hmsCard);
            assertThrows(InvalidGameStateException.class, () -> faction.setHmsStrongholdProxy(arrakeenCard));
        }

        @Test
        void testHasHMSAndArrakeen() {
            faction.addStrongholdCard(arrakeenCard);
            faction.addStrongholdCard(hmsCard);
            assertDoesNotThrow(() -> faction.setHmsStrongholdProxy(arrakeenCard));
        }
    }
}