package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
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
        assertEquals(10, faction.getSpice());
    }

    @Nested
    @DisplayName("#placeHMS")
    class PlaceHMS {
    }

    @Nested
    @DisplayName("#getTerritoryWithHMS")
    class GetTerritoryWithHMS {
        Territory windPassNorth_NorthSector;
        Territory polarSink;

        @BeforeEach
        public void setUp() {
            faction.placeHMS("Wind Pass North (North Sector)");
            windPassNorth_NorthSector = game.getTerritory("Wind Pass North (North Sector)");
            polarSink = game.getTerritory("Polar Sink");
        }

        @Test
        public void testGetTerritoryWithHMS() {
            assertEquals(windPassNorth_NorthSector, faction.getTerritoryWithHMS());
        }

        @Test
        public void testGetTerritoryWithHMSAfterMove() throws InvalidGameStateException {
            faction.startHMSMovement();
            faction.moveHMSOneTerritory("Polar Sink");
            assertEquals(polarSink, faction.getTerritoryWithHMS());
        }
    }

    @Nested
    @DisplayName("#startHMSMovement")
    class StartHMSMovement {
        @BeforeEach
        public void setUp() {
            faction.placeHMS("Wind Pass North (North Sector)");
            faction.startHMSMovement();
        }

        @Test
        public void testIxHasThreeMovesLeft() {
            assertEquals(3, faction.getHMSMoves());
        }

        @Test
        public void testHMSTerritoriesIncludesCurrentTerritory() {
            assertTrue(faction.hmsTerritories.contains("Wind Pass North (North Sector)"));
        }

        @Test
        public void testMessagePublishedToTurnSummary() {
            assertEquals(Emojis.IX + " to decide if they want to move the HMS.", turnSummary.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#moveHMSOneTerritory")
    class MoveHMSOneTerritory {
        Territory hms;
        Territory windPassNorth_NorthSector;
        Territory polarSink;

        @BeforeEach
        public void setUp() throws InvalidGameStateException {
            game.setStorm(10);
            faction.placeHMS("Wind Pass North (North Sector)");
            windPassNorth_NorthSector = game.getTerritory("Wind Pass North (North Sector)");
            polarSink = game.getTerritory("Polar Sink");
            hms = game.getTerritory("Hidden Mobile Stronghold");
            faction.startHMSMovement();
            game.getUpdateTypes().clear();
            faction.moveHMSOneTerritory("Polar Sink");
        }

        @Test
        public void testHMSMovedToNewTerritory() {
            assertFalse(windPassNorth_NorthSector.hasForce("Hidden Mobile Stronghold"));
            assertTrue(polarSink.hasForce("Hidden Mobile Stronghold"));
        }

        @Test
        public void testAdjacencyListUpdated() {
            assertFalse(game.getAdjacencyList().get("Wind Pass North").contains("Hidden Mobile Stronghold"));
            assertFalse(game.getAdjacencyList().get("Hidden Mobile Stronghold").contains("Wind Pass North"));
            assertTrue(game.getAdjacencyList().get("Polar Sink").contains("Hidden Mobile Stronghold"));
            assertTrue(game.getAdjacencyList().get("Hidden Mobile Stronghold").contains("Polar Sink"));
        }

        @Test
        public void testMovementReportedToTurnSummary() {
            assertEquals(Emojis.IX + " moved the HMS to Polar Sink.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testMapWillBeUpdated() {
            assertTrue(game.getUpdateTypes().contains(UpdateType.MAP));
        }
    }

    @Nested
    @DisplayName("#hmsMovement")
    class HMSMovement {
        Territory hms;

        @BeforeEach
        public void setUp() {
            game.setStorm(10);
            faction.placeHMS("Wind Pass North (North Sector)");
            hms = game.getTerritory("Hidden Mobile Stronghold");
            game.moveForces(faction, hms, game.getTerritory("Polar Sink"), 2, 2, false);
            game.getTerritory("Wind Pass North (North Sector)").setSpice(6);
            game.getTerritory("Cielago North (East Sector)").setSpice(8);
            game.getTerritory("Habbanya Ridge Flat (East Sector)").setSpice(10);
            faction.startHMSMovement();
        }

        @Test
        public void testIxCollectsSpiceFromDepartedTerritory() throws InvalidGameStateException {
            faction.moveHMSOneTerritory("Polar Sink");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(14, faction.getSpice());
            assertEquals(Emojis.IX + " collects 4 " + Emojis.SPICE + " from Wind Pass North (North Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxCollectsOnlyAsMuchSpiceIsPresent() throws InvalidGameStateException {
            game.moveForces(faction, game.getTerritory("Polar Sink"), hms, 2, 2, false);
            faction.moveHMSOneTerritory("Polar Sink");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(16, faction.getSpice());
            assertEquals(Emojis.IX + " collects 6 " + Emojis.SPICE + " from Wind Pass North (North Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxCollectsOnlyOncePerTerritory() throws InvalidGameStateException {
            faction.moveHMSOneTerritory("Polar Sink");
            faction.moveHMSOneTerritory("Wind Pass North (North Sector)");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(14, faction.getSpice());
            assertEquals(Emojis.IX + " collects 4 " + Emojis.SPICE + " from Wind Pass North (North Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxDoesNotCollectsSpiceIfHMSDoesNotMove() {
            faction.endHMSMovement();
            assertEquals(10, faction.getSpice());
        }

        @Test
        public void testIxCollectsSpiceFromDestinationTerritory() throws InvalidGameStateException {
            game.getTerritory("Wind Pass North (North Sector)").setSpice(0);
            faction.moveHMSOneTerritory("Cielago West (North Sector)");
            faction.moveHMSOneTerritory("Habbanya Ridge Flat (East Sector)");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(14, faction.getSpice());
            assertEquals(Emojis.IX + " collects 4 " + Emojis.SPICE + " from Habbanya Ridge Flat (East Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxCollectsSpiceMovingThroughATerritory() throws InvalidGameStateException {
            game.getTerritory("Wind Pass North (North Sector)").setSpice(0);
            faction.moveHMSOneTerritory("Cielago North (East Sector)");
            faction.moveHMSOneTerritory("Polar Sink");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(14, faction.getSpice());
            assertEquals(Emojis.IX + " collects 4 " + Emojis.SPICE + " from Cielago North (East Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxCollectsSpiceForEcazAllyInHMS() throws InvalidGameStateException, IOException {
            EcazFaction ecaz = new EcazFaction("ec", "ec");
            ecaz.setLedger(new TestTopic());
            game.addFaction(ecaz);
            game.createAlliance(faction, ecaz);
            ecaz.placeForcesFromReserves(hms, 1, false);
            faction.moveHMSOneTerritory("Polar Sink");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(16, faction.getSpice());
            assertEquals(Emojis.IX + " collects 6 " + Emojis.SPICE + " from Wind Pass North (North Sector).", turnSummary.getMessages().getFirst());
        }

        @Test
        public void testIxDoesNotCollectSpiceForAdvisorsInHMS() throws InvalidGameStateException, IOException {
            BGFaction bg = new BGFaction("bg", "bg");
            bg.setLedger(new TestTopic());
            game.addFaction(bg);
            bg.placeAdvisorsFromReserves(game, hms, 1);
            faction.startHMSMovement();
            faction.moveHMSOneTerritory("Polar Sink");
            turnSummary.clear();
            faction.endHMSMovement();
            assertEquals(14, faction.getSpice());
            assertEquals(Emojis.IX + " collects 4 " + Emojis.SPICE + " from Wind Pass North (North Sector).", turnSummary.getMessages().getFirst());
        }
    }

    @Nested
    @DisplayName("#presentStartingCardsListAndChoices")
    class PresentStartingCardsListAndChoices {
        @BeforeEach
        public void setUp() throws IOException {
            game.addFaction(new AtreidesFaction("at", "at"));
            game.addFaction(new BGFaction("bg", "bg"));
            game.addFaction(new FremenFaction("fr", "fr"));
            game.addFaction(new EmperorFaction("em", "em"));
            game.addFaction(new HarkonnenFaction("ha", "ha"));
            game.setModInfo(new TestTopic());
        }

        @Test
        public void testTwoCardsForHarkonnen() throws InvalidGameStateException {
            faction.presentStartingCardsListAndChoices();
            assertEquals(7, faction.getTreacheryHand().size());
        }

        @Test
        public void testOnlyOneCardPerFaction() throws InvalidGameStateException {
            game.addGameOption(GameOption.IX_ONLY_1_CARD_PER_FACTION);
            faction.presentStartingCardsListAndChoices();
            assertEquals(6, faction.getTreacheryHand().size());
        }
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        @Test
        @Override
        void testForcesStringInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 3, 2, false, false, false, false, false);
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
            assertEquals(faction.getEmoji() + " revives 2 " + Emojis.IX_SUBOID + " 1 " + Emojis.IX_CYBORG + " for free.", turnSummary.getMessages().getLast());
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

    @Nested
    @DisplayName("#reviveForcesWithBTAmbassador")
    class reviveForcesWithBTAmbassador extends FactionTestTemplate.reviveForcesWithBTAmbassador {
        @Test
        @Override
        void testTwoForcesAndTwoSpecialsInTanks() {
            faction.removeForces(faction.getHomeworld(), 2, false, true);
            faction.removeForces(faction.getHomeworld(), 3, true, true);
            faction.reviveForcesWithBTAmbassador();
            assertEquals(faction.getEmoji() + " revives 1 " + Emojis.IX_SUBOID + " 3 " + Emojis.IX_CYBORG + " for free.", turnSummary.getMessages().getLast());
            assertEquals("You revived 1 " + Emojis.IX_SUBOID + " 3 " + Emojis.IX_CYBORG + " with the BT Ambassador.", chat.getMessages().getLast());
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        homeworld.removeForces(game, faction.getName(), forcesToRemove);
        homeworld.removeForces(game, faction.getName() + "*", homeworld.getForceStrength(faction.getName() + "*"));
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
        assertEquals(Emojis.IX, faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.IX_SUBOID, faction.getForceEmoji());
    }

    @Test
    @Override
    public void testSpecialForceEmoji() {
        assertEquals(Emojis.IX_CYBORG, faction.getSpecialForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
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