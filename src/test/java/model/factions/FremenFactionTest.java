package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FremenFactionTest extends FactionTestTemplate {

    private FremenFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new FremenFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 3);
    }

    @Nested
    @DisplayName("#placeWorm")
    class PlaceWorm {
        @BeforeEach
        public void setUp() {
            game.setTurn(2);
            faction.placeForcesFromReserves(game.getTerritory("Sietch Tabr"), 17, false);
            faction.presentWormPlacementChoices("Gara Kulon", "Shai-Hulud");
            assertEquals(1, faction.getWormsToPlace());
        }

        @Test
        public void testPlaceShaiHulud() {
            faction.placeWorm(false, game.getTerritory("Funeral Plain"), false);
            assertEquals("Shai-Hulud has been placed in Funeral Plain\n", turnSummary.getMessages().getFirst());
            assertEquals("You placed Shai-Hulud in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testPlaceGreatMaker() {
            faction.placeWorm(true, game.getTerritory("Funeral Plain"), false);
            assertEquals("Great Maker has been placed in Funeral Plain\nAfter the Nexus, 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n", turnSummary.getMessages().getFirst());
            assertEquals("You placed Great Maker in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testLeaveShaiHuludWhereItIs() {
            faction.placeWorm(false, game.getTerritory("Funeral Plain"), true);
            assertEquals("Shai-Hulud has been placed in Funeral Plain\n", turnSummary.getMessages().getFirst());
            assertEquals("You left Shai-Hulud in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testLeaveGreatMakerWhereItIs() {
            faction.placeWorm(true, game.getTerritory("Funeral Plain"), true);
            assertEquals("Great Maker has been placed in Funeral Plain\nAfter the Nexus, 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n", turnSummary.getMessages().getFirst());
            assertEquals("You left Great Maker in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        @Test
        @Override
        void testForcesStringInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 3, 2, false, false, false, false, false);
            assertEquals(faction.getEmoji() + ": 3 " + Emojis.FREMEN_TROOP + " 2 " + Emojis.FREMEN_FEDAYKIN + " placed on The Great Flat", turnSummary.getMessages().getFirst());
        }

        @Test
        @Override
        void testSpiceCostInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.FREMEN_TROOP + " placed on The Great Flat", turnSummary.getMessages().getFirst());
        }

        @Test
        @Override
        void testHeighlinersMessageAfterFirstShipmentMessage() throws InvalidGameStateException {
            game.addGameOption(GameOption.TECH_TOKENS);
            faction.addTechToken("Heighliners");
            faction.addTechToken("Axlotl Tanks");
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.FREMEN_TROOP + " placed on The Great Flat", turnSummary.getMessages().getFirst());
            assertFalse(turnSummary.getMessages().getLast().contains(Emojis.SPICE + " is placed on " + Emojis.HEIGHLINERS));
        }

        @Test
        @Override
        void testBGGetAdviseMessage() {
        }
    }

    @Nested
    @DisplayName("#placeForcesFromReserves")
    class PlaceForcesFromReserves extends FactionTestTemplate.PlaceForcesFromReserves {
        @Test
        void testFlipToLowThreshold() {
            faction.placeForcesFromReserves(sietchTabr, 1, true);
            super.testFlipToLowThreshold();
        }
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testPaidRevival() {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.removeForces(faction.getHomeworld(), 2, true, true);
            game.reviveForces(faction, false, 2, 1);
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.getForceEmoji(faction.getName()) + " 1 " + Emojis.getForceEmoji("Fremen*") + " for free.", turnSummary.getMessages().getLast());
        }

        @Test
        @Override
        public void testFreeReviveStars() {
            faction.removeForces(faction.getHomeworld(), 3, true, true);
            assertEquals(1, faction.countFreeStarredRevival());
        }

        @Test
        public void testFreeReviveStarsNoneInTanks() {
            assertEquals(0, faction.countFreeStarredRevival());
        }

        @Test
        @Override
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
        }

        @Test
        @Override
        public void testPaidRevivalMessageAfter1StarFree() throws InvalidGameStateException {
            assertDoesNotThrow(() -> faction.removeForces(faction.getHomeworld(), 2, true, true));
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(1);
            assertEquals(0, chat.getMessages().size());
            assertEquals(0, chat.getChoices().size());
            assertEquals(faction.getEmoji() + " has no revivable forces in the tanks", faction.getPaidRevivalMessage());
        }
    }

    @Test
    public void testFreeRevival() {
        assertEquals(3, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThreshold() {
        game.addGameOption(GameOption.HOMEWORLDS);
        Territory homeworld = game.getTerritory(faction.getHomeworld());
        int forcesToRemove = homeworld.getForceStrength(faction.getName()) - (faction.highThreshold - 1);
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() throws InvalidGameStateException {
        game.startRevival();
        game.getRevival().setRecruitsInPlay(true);
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
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Test
    public void testPlaceFreeRevivalWithHighThreshold() throws InvalidGameStateException {
        faction.removeForces("Southern Hemisphere", 2, true, true);
        faction.placeFreeRevivalWithHighThreshold("Sietch Tabr");
        assertEquals(1, game.getTerritory("Sietch Tabr").getForceStrength("Fremen*"));
        assertEquals(Emojis.FREMEN + " place their revived " + Emojis.FREMEN_FEDAYKIN + " with their forces in Sietch Tabr.", turnSummary.getMessages().getLast());
        assertEquals("Your " + Emojis.FREMEN_FEDAYKIN + " has left for the northern hemisphere.", chat.getMessages().getLast());
    }

    @Nested
    @DisplayName("#executeShipment")
    class ExecuteShipment extends FactionTestTemplate.ExecuteShipment {
        @Override
        @Test
        void testPaidShipment() throws InvalidGameStateException {
            int spice = faction.getSpice();
            faction.executeShipment(game, false, false);
            assertEquals(spice, faction.getSpice());
        }
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(17, faction.getReservesStrength());
        assertEquals(3, faction.getSpecialReservesStrength());
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
        assertEquals(faction.getEmoji(), Emojis.FREMEN);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }
}