package model.factions;

import constants.Emojis;
import enums.GameOption;
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

class GuildFactionTest extends FactionTestTemplate {

    private GuildFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new GuildFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 5);
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
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(7, faction.getFreeRevival());
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        @Test
        @Override
        void testSpiceCostInTurnSummaryMessage() throws InvalidGameStateException {
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.GUILD_TROOP + " placed on The Great Flat for 1 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
        }

        @Test
        @Override
        void testHeighlinersMessageAfterFirstShipmentMessage() throws InvalidGameStateException {
            game.addGameOption(GameOption.TECH_TOKENS);
            faction.addTechToken("Heighliners");
            faction.addTechToken("Axlotl Tanks");
            faction.placeForces(territory, 1, 0, true, true, true, false, false);
            assertEquals(faction.getEmoji() + ": 1 " + Emojis.GUILD_TROOP + " placed on The Great Flat for 1 " + Emojis.SPICE, turnSummary.getMessages().getFirst());
            assertFalse(turnSummary.getMessages().getLast().contains(Emojis.SPICE + " is placed on " + Emojis.HEIGHLINERS));
        }
    }

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(15, faction.getReservesStrength());
    }

    @Test
    public void testInitialForcePlacement() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory instanceof HomeworldTerritory) continue;
            if (territory.getTerritoryName().equals("Tuek's Sietch")) {
                assertEquals(5, territory.getForceStrength("Guild"));
                assertEquals(1, territory.countFactions());
            } else {
                assertEquals(0, territory.countFactions());
            }
        }
    }

    @Test
    public void testEmoji() {
        assertEquals(faction.getEmoji(), Emojis.GUILD);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    @Override
    void testGetSpiceSupportPhasesString() {
        assertEquals(" for bidding only!", getFaction().getSpiceSupportPhasesString());
        faction.setAllySpiceForShipping(true);
        assertEquals(" for bidding and shipping!", getFaction().getSpiceSupportPhasesString());
    }

    @Nested
    @DisplayName("#payForShipment")
    class PayForShipment extends FactionTestTemplate.PayForShipment {
        HarkonnenFaction harkonnen;

        @BeforeEach
        @Override
        void setUp() throws IOException {
            harkonnen = new HarkonnenFaction("ha", "ha");
            harkonnen.setLedger(new TestTopic());
            super.setUp();
        }

        @Test
        void testFactionCanPayWithAllySupport() throws InvalidGameStateException {
            game.addFaction(guild);
            game.addFaction(emperor);
            faction.subtractSpice(spiceBeforeShipment - 1, "Test");
            game.createAlliance(faction, emperor);
            emperor.setSpiceForAlly(1);
            assertEquals(" for 2 " + Emojis.SPICE + " (1 from " + Emojis.EMPEROR + ")", faction.payForShipment(2, habbanyaSietch, false, false));
            assertEquals(0, faction.getSpice());
            assertEquals(9, emperor.getSpice());
        }

        @Test
        @Override
        void testNormalPaymentWithGuildInGame() throws InvalidGameStateException {
            assertEquals(" for 1 " + Emojis.SPICE, faction.payForShipment(1, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 1, faction.getSpice());
        }

        @Test
        @Override
        void testKaramaShipmentDoesNotPayGuild() {
        }

        @Test
        @Override
        void testGuildAtLowThresdhold() {
        }

        @Test
        @Override
        void testGuildOccupied() throws InvalidGameStateException {
            // Occupier does not get paid for Guild shipment. "The occupier collects the other half (rounded down) of shipping payments made by other players."
            game.addFaction(harkonnen);
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.placeForces(habbanyaSietch, 15, 0, false, false, false, false, false);
            assertFalse(faction.isHighThreshold());
            harkonnen.placeForces(faction.getHomeworldTerritory(), 1, 0, false, false, false, false, false);
            assertTrue(faction.isHomeworldOccupied());
            faction.addSpice(1, "Test");
            spiceBeforeShipment = faction.getSpice();
            assertEquals(" for 3 " + Emojis.SPICE, faction.payForShipment(3, habbanyaSietch, false, false));
            assertEquals(spiceBeforeShipment - 3, faction.getSpice());
            assertEquals(10, harkonnen.getSpice());
        }

        @Test
        @Override
        void testNormalPaymentWithGuildNotInGame() {
        }
    }
}