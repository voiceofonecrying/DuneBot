package model.factions;

import model.Force;
import model.Game;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class FactionTestTemplate {
    Game game;
    abstract Faction getFaction();

    @BeforeEach
    void baseSetUp() {
        game = new Game();
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
    class RemoveSpice {
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
            faction.subtractSpice(-2);
            assertEquals(faction.getSpice(), 8);
        }

        @Test
        void tooMuch() {
            assertThrows(IllegalStateException.class, () -> faction.subtractSpice(11));
        }
    }

    @Nested
    @DisplayName("#removeForces")
    class RemoveForces {
        Faction faction;
        Territory territory;
        String forceName;

        @BeforeEach
        void setUpRemoveSpice() {
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
            assertEquals(reservesStrength + 2, faction.reserves.getStrength());
            assertEquals(game.getTanks().stream().filter(tank -> tank.getName().equals(forceName)).count(), 0);
        }

        @Test
        void validToTanks() {
            int reservesStrength = faction.getReserves().getStrength();
            int tanksStrength = game.getForceFromTanks(forceName).getStrength();
            faction.removeForces(territory.getTerritoryName(), forceName, 2, true, true, forceName);
            assertEquals(territory.getForce(forceName).getStrength(), 3);
            assertEquals(tanksStrength + 2, game.getForceFromTanks(forceName).getStrength());
            assertEquals(reservesStrength, faction.reserves.getStrength());
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
}