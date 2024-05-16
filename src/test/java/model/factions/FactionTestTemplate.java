package model.factions;

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

    @Test
    void testMaxRevival() {
        Faction faction = getFaction();
        if (faction instanceof BTFaction)
            assertEquals(20, faction.getMaxRevival());
        else
            assertEquals(3, faction.getMaxRevival());
        faction.setMaxRevival(5);
        assertEquals(5, faction.getMaxRevival());
        game.setRecruitsInPlay(true);
        assertEquals(7, faction.getMaxRevival());
    }

    @Nested
    @DisplayName("#addSpice")
    class AddSpice {
        Faction faction;

        @BeforeEach
        void setUpAddSpice() {
            faction = getFaction();
            faction.setSpice(10);
        }

        @Test
        void validPositive() {
            faction.addSpice(2);
            assertEquals(faction.getSpice(), 12);
        }

        @Test
        void zero() {
            faction.addSpice(0);
            assertEquals(faction.getSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.addSpice(-1));
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
        }

        @Test
        void validPositive() {
            faction.subtractSpice(2);
            assertEquals(faction.getSpice(), 8);
        }

        @Test
        void zero() {
            faction.subtractSpice(0);
            assertEquals(faction.getSpice(), 10);
        }

        @Test
        void negative() {
            assertThrows(IllegalArgumentException.class, () -> faction.subtractSpice(-2));
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractSpice(11));
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

            territory = game.getTerritories().get("Arrakeen");
            Force force = faction.getReserves();
            forceName = force.getName();

            territory.getForce("someforce").setStrength(8);
            territory.setForceStrength(force.getName(), 5);
            territory.getForce("someotherforce").setStrength(4);

            force.setStrength(force.getStrength() - 5);
        }

        @Test
        void validToReserves() {
            int reservesStrength = faction.getReserves().getStrength();
            faction.removeForces(territory.getTerritoryName(), forceName, 2, false, false, forceName);
            assertEquals(territory.getForce(forceName).getStrength(), 3);
            assertEquals(reservesStrength + 2, faction.getReserves().getStrength());
            assertEquals(game.getTanks().stream().filter(tank -> tank.getName().equals(forceName)).count(), 0);
        }

        @Test
        void validToTanks() {
            int reservesStrength = faction.getReserves().getStrength();
            int tanksStrength = game.getForceFromTanks(forceName).getStrength();
            faction.removeForces(territory.getTerritoryName(), forceName, 2, true, true, forceName);
            assertEquals(territory.getForce(forceName).getStrength(), 3);
            assertEquals(tanksStrength + 2, game.getForceFromTanks(forceName).getStrength());
            assertEquals(reservesStrength, faction.getReserves().getStrength());
        }

        @Test
        void invalidToReservesTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, false, false, forceName));
        }

        @Test
        void invalidToReservesNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, false, false, forceName));
        }

        @Test
        void invalidToTanksTooMany() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, 6, true, false, forceName));
        }

        @Test
        void invalidToTanksNegatives() {
            assertThrows(IllegalArgumentException.class,
                    () -> faction.removeForces(territory.getTerritoryName(), forceName, -1, true, false, forceName));
        }
    }

    @Nested
    @DisplayName("#isNearShieldWall")
    class IsNearShieldWall {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUpRemoveSpice() {
            faction = getFaction();

            Force force = faction.getReserves();
            forceName = force.getName();

            territory = game.getTerritories().get("Arrakeen");
            territory.setForceStrength(forceName, 1);
            territory = game.getTerritories().get("Polar Sink");
            territory.setForceStrength(forceName, 1);
            territory = game.getTerritories().get("Hidden Mobile Stronghold");
            territory.setForceStrength(forceName, 1);
        }

        @Test
        void trueOnShieldWall() {
            territory = game.getTerritories().get("Shield Wall (North Sector)");
            territory.setForceStrength(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInImperialBasin() {
            territory = game.getTerritories().get("Imperial Basin (Center Sector)");
            territory.setForceStrength(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInFalseWallEast() {
            territory = game.getTerritories().get("False Wall East (Far North Sector)");
            territory.setForceStrength(forceName, 1);
            assertTrue(faction.isNearShieldWall());
        }

        @Test
        void trueInGaraKulon() {
            territory = game.getTerritories().get("Gara Kulon");
            territory.setForceStrength(forceName, 1);
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