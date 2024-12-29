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

class IxFactionTest extends FactionTestTemplate {

    private IxFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new IxFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 10);
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        @Test
        @Override
        void testForcesStringInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 3, 2, false, false, false, game, false, false);
            assertEquals(faction.getEmoji() + ": 3 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " placed on The Great Flat", turnSummary.getMessages().getFirst());
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
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.getForceEmoji(faction.getName()) + " 1 " + Emojis.getForceEmoji("Ix*") + " for free.", turnSummary.getMessages().getLast());
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
        public void testFreeReviveStarsAlliedToFremen() {
            faction.setAlly("Fremen");
            faction.removeForces(faction.getHomeworld(), 3, true, true);
            assertEquals(3, faction.countFreeStarredRevival());
        }

        @Test
        @Override
        public void testRevivalCost() {
            assertEquals(5, faction.revivalCost(1, 1));
        }

        @Test
        @Override
        public void testRevivalCostAlliedWithBT() {
            faction.setAlly("BT");
            assertEquals(3, faction.revivalCost(1, 1));
        }

        @Test
        @Override
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
//            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("There are no " + Emojis.IX_CYBORG + " in the tanks."));
            assertEquals("Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? " + faction.player, chat.getMessages().getFirst());
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? "));
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
            assertEquals("revival-forces-1", chat.getChoices().getFirst().get(1).getId());
            assertEquals("1 Suboid", chat.getChoices().getFirst().get(1).getLabel());
        }

        @Test
        public void testPaidRevivalOneCyborgInTanksWithSuboids() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.removeForces(faction.getHomeworld(), 1, true, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals("Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? " + faction.player, chat.getMessages().getFirst());
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? "));
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
            assertEquals("revival-forces-1", chat.getChoices().getFirst().get(1).getId());
            assertEquals("1 Suboid", chat.getChoices().getFirst().get(1).getLabel());
        }

        @Test
        public void testPaidRevivalOneCyborgInTanksNoSuboids() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 0, false, true);
            faction.removeForces(faction.getHomeworld(), 1, true, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(0, chat.getMessages().size());
        }

        @Test
        @Override
        public void testPaidRevivalMessageAfter1StarFree() throws InvalidGameStateException {
            assertDoesNotThrow(() -> faction.removeForces(faction.getHomeworld(), 2, true, true));
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(1);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(1, turnSummary.getMessages().size());
        }

        @Test
        public void testPaidRevivalMultipleCyborgsInTanksWithSuboids() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.removeForces(faction.getHomeworld(), 3, true, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals("Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? " + faction.player + "\n" + Emojis.IX_SUBOID + " revivals if possible would be the next step.", chat.getMessages().getFirst());
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? "));
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("\n" + Emojis.IX_SUBOID + " revivals if possible would be the next step."));
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
            assertEquals("revival-cyborgs-1-1", chat.getChoices().getFirst().get(1).getId());
            assertEquals("1 Cyborg", chat.getChoices().getFirst().get(1).getLabel());
        }

        @Test
        public void testPaidRevivalMultipleCyborgsInTanksNoSuboids() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 0, false, true);
            faction.removeForces(faction.getHomeworld(), 3, true, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals("Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? " + faction.player + "\nThere are no " + Emojis.IX_SUBOID + " in the tanks.", chat.getMessages().getFirst());
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? "));
            assertNotEquals(-1, chat.getMessages().getFirst().indexOf("\nThere are no " + Emojis.IX_SUBOID + " in the tanks."));
            assertEquals(1, chat.getChoices().size());
            assertEquals(3 - freeRevivals + 1, chat.getChoices().getFirst().size());
            assertEquals("revival-cyborgs-1-1", chat.getChoices().getFirst().get(1).getId());
            assertEquals("1 Cyborg", chat.getChoices().getFirst().get(1).getLabel());
        }

        @Test
        @Override
        public void testPaidRevivalChoicesInsufficientSpice() throws InvalidGameStateException {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
//            assertEquals("You do not have enough " + Emojis.SPICE + " to purchase additional revivals.", chat.getMessages().getFirst());
//            assertEquals(0, chat.getChoices().size());
            assertFalse(chat.getChoices().getFirst().getFirst().isDisabled());
            assertTrue(chat.getChoices().getFirst().get(1).isDisabled());
        }

        @Test
        public void testPaidRevivalChoicesTwoSpice() throws InvalidGameStateException {
            faction.subtractSpice(faction.getSpice(), "Test");
            faction.addSpice(2, "Test");
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals("Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? " + faction.player, chat.getMessages().getFirst());
            assertEquals(1, chat.getChoices().size());
        }
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
        homeworld.removeForces(faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        homeworld.removeForces(faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        homeworld.removeForces(faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        homeworld.removeForces(faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        assertEquals(10, faction.getReservesStrength());
        assertEquals(4, faction.getSpecialReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        assertTrue(game.getTerritories().containsKey("Hidden Mobile Stronghold"));

        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold")) {
                assertEquals(3, territory.getForceStrength("Ix"));
                assertEquals(3, territory.getForceStrength("Ix*"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.IX);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    public void testSpiceCollectedFromTerritory() {
        Territory theGreatFlat = game.getTerritory("The Great Flat");
        theGreatFlat.addForces("Ix", 1);
        theGreatFlat.addForces("Ix*", 1);
        theGreatFlat.setSpice(10);
        assertEquals(5, faction.getSpiceCollectedFromTerritory(theGreatFlat));
        theGreatFlat.setSpice(3);
        assertEquals(3, faction.getSpiceCollectedFromTerritory(theGreatFlat));
    }
}