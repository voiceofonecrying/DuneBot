package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.MoveType;
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
        assertEquals(3, faction.getSpice());
    }

    @Nested
    @DisplayName("#twoWormsToPlace")
    class TwoWormsToPlace {
        @BeforeEach
        public void setUp() {
            faction.addWormToPlace("Gara Kulon", "Shai-Hulud");
            assertEquals(1, faction.getWormsToPlace());
            faction.addWormToPlace("Gara Kulon", "Great Maker");
            assertEquals(2, faction.getWormsToPlace());
        }

        @Test
        public void testSecondCallDoesNotPresentChoicesYet() {
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
        }

        @Test
        public void testPlacingFirstWormPresentsSecondWormChoices() {
            faction.placeWorm(game.getTerritory("Funeral Plain"));
            assertEquals(1, faction.getWormsToPlace());
            assertEquals(3, chat.getMessages().size());
            assertEquals(2, chat.getChoices().size());
        }
    }

    @Nested
    @DisplayName("#placeWorm")
    class PlaceWorm {
        @BeforeEach
        public void setUp() {
            game.setTurn(2);
            faction.placeForcesFromReserves(game.getTerritory("Sietch Tabr"), 17, false);
            faction.addWormToPlace("Gara Kulon", "Shai-Hulud");
            assertEquals(1, faction.getWormsToPlace());
        }

        @Test
        public void testPlaceShaiHulud() {
            faction.placeWorm(game.getTerritory("Funeral Plain"));
            assertEquals("Shai-Hulud has been placed in Funeral Plain.\n", turnSummary.getMessages().getFirst());
            assertEquals("You placed Shai-Hulud in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testPlaceGreatMaker() {
            faction.getMovement().setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            faction.placeWorm(game.getTerritory("Funeral Plain"));
            assertEquals("Great Maker has been placed in Funeral Plain.\nAfter the Nexus, 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n", turnSummary.getMessages().getFirst());
            assertEquals("You placed Great Maker in Funeral Plain.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testLeaveShaiHuludWhereItIs() {
            faction.placeWorm(game.getTerritory("Gara Kulon"));
            assertEquals("Shai-Hulud remains in Gara Kulon.\n", turnSummary.getMessages().getFirst());
            assertEquals("You left Shai-Hulud in Gara Kulon.", chat.getMessages().getLast());
            assertEquals(0, faction.getWormsToPlace());
        }

        @Test
        public void testLeaveGreatMakerWhereItIs() {
            faction.getMovement().setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            faction.placeWorm(game.getTerritory("Gara Kulon"));
            assertEquals("Great Maker remains in Gara Kulon.\nAfter the Nexus, 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n", turnSummary.getMessages().getFirst());
            assertEquals("You left Great Maker in Gara Kulon.", chat.getMessages().getLast());
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
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.FREMEN_TROOP + " 1 " + Emojis.FREMEN_FEDAYKIN + " for free.", turnSummary.getMessages().getLast());
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
        assertEquals(Emojis.FREMEN, faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.FREMEN_TROOP, faction.getForceEmoji());
    }

    @Test
    @Override
    public void testSpecialForceEmoji() {
        assertEquals(Emojis.FREMEN_FEDAYKIN, faction.getSpecialForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
    }

    @Nested
    @DisplayName("#colectSpiceFromTerritory")
    class CollectSpiceFromTerritory extends FactionTestTemplate.CollectSpiceFromTerritory {
        @Test
        void testHomeworldOccupied() throws IOException {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.placeForcesFromReserves(game.getTerritory("Sietch Tabr"), 17, false);
            faction.placeForcesFromReserves(game.getTerritory("Sietch Tabr"), 3, true);
            HarkonnenFaction harkonnen = new HarkonnenFaction("ha", "ha");
            harkonnen.setLedger(new TestTopic());
            game.addFaction(harkonnen);
            harkonnen.placeForcesFromReserves(game.getTerritory("Southern Hemisphere"), 1, false);
            assertTrue(faction.isHomeworldOccupied());
            faction.setHasMiningEquipment(true);
            turnSummary.clear();
            faction.collectSpiceFromTerritory(cielagoSouth_WestSector);
            assertEquals(spiceBefore + 2, faction.getSpice());
            assertEquals(9, cielagoSouth_WestSector.getSpice());
            assertEquals(faction.getEmoji() + " collects 3 " + Emojis.SPICE + " from Cielago South (West Sector)", turnSummary.getMessages().getFirst());
            assertEquals(11, harkonnen.getSpice());
            assertEquals(harkonnen.getEmoji() + " takes 1 " + Emojis.SPICE + " from " + faction.getEmoji() + " collection as Southern Hemisphere Occupier.", turnSummary.getMessages().getLast());
        }
    }
}