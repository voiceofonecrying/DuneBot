package model;

import constants.Emojis;
import enums.MoveType;
import exceptions.InvalidGameStateException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MovementTest extends DuneTest {
    @BeforeEach
    void setUp() throws InvalidGameStateException, IOException {
        super.setUp();
    }

    @Nested
    @DisplayName("#pass")
    class Pass {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            movement = ix.getMovement();
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
        }

        @AfterEach
        void tearDown() {
            assertEquals("", movement.getMovingTo());
            assertEquals("", movement.getMovingFrom());
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
            assertEquals("", movement.getSecondMovingFrom());
        }

        @Test
        void testFremenAmbassador() {
            ix.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.pass(game, ix);
            assertEquals("You will not ride the worm with the Fremen Ambassador.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + " does not ride the worm with the Fremen Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ix.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.pass(game, ix);
            assertEquals("You will not ship with the Guild Ambassador.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + " does not ship with the Guild Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#startOver")
    class StartOver {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            // TODO: Change to Ix when ally use of ambassadors is supported
            movement = ecaz.getMovement();
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
            turnSummary.clear();
        }

        @AfterEach
        void tearDown() {
            assertEquals("", movement.getMovingTo());
            assertEquals("", movement.getMovingFrom());
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
            assertEquals("", movement.getSecondMovingFrom());
        }

        @Test
        void testFremenAmbassador() {
            // TODO: Change to Ix when ally use of ambassadors is supported
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.startOver(ecaz);
            assertEquals("You have triggered your Fremen Ambassador!\nWhere would you like to ride from?", ecazChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            // TODO: Change to Ix when ally use of ambassadors is supported
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.startOver(ecaz);
            assertEquals("You have triggered your Guild Ambassador!\nWhere would you like to place up to 4 " + Emojis.ECAZ_TROOP + " from reserves?", ecazChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentStrongholdChoices")
    class PresentStrongholdChoices {
        List<String> strongholds;
        List<String> strongholdsWithHMS;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            strongholds = List.of("Arrakeen", "Carthag", "Sietch Tabr", "Habbanya Sietch", "Tuek's Sietch");
            strongholdsWithHMS = new ArrayList<>(strongholds);
            strongholdsWithHMS.add("Hidden Mobile Stronghold");
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentStrongholdChoices(game, ecaz);
            assertEquals("Which Stronghold?", ecazChat.getMessages().getLast());
            assertEquals(7, ecazChat.getChoices().getLast().size());
            assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ecaz.getMovement().presentStrongholdChoices(game, ecaz);
            assertEquals("Which Stronghold?", ecazChat.getMessages().getLast());
            assertEquals(6, ecazChat.getChoices().getLast().size());
            assertFalse(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 5)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholds.contains(c.getLabel()));
            }
            for (String s : strongholds)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassadorByIxAlly() {
            game.createAlliance(ecaz, ix);
            ix.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ix.getMovement().presentStrongholdChoices(game, ix);
            assertEquals("Which Stronghold?", ixChat.getMessages().getLast());
            assertEquals(7, ixChat.getChoices().getLast().size());
            assertTrue(ixChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("ambassador-guild-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ixChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(ixChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ix.getMovement().getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentSpiceBlowChoices")
    class PresentSpiceBlowChoices {
        List<String> spiceBlowTerritories;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            spiceBlowTerritories = game.getTerritories().getSpiceBlowTerritoryNames();
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentSpiceBlowChoices(game, ecaz);
            assertEquals("Which Spice Blow Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ecaz.getMovement().presentSpiceBlowChoices(game, ecaz);
            assertEquals("Which Spice Blow Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ecaz.getMovement().getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentRockChoices")
    class PresentRockChoices {
        List<String> rockTerritories;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            rockTerritories = game.getTerritories().getRockTerritoryNames();
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentRockChoices(game, ecaz);
            assertEquals("Which Rock Territory?", ecazChat.getMessages().getLast());
            assertEquals(8, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 7)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(rockTerritories.contains(c.getLabel()));
            }
            for (String s : rockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ecaz.getMovement().presentRockChoices(game, ecaz);
            assertEquals("Which Rock Territory?", ecazChat.getMessages().getLast());
            assertEquals(8, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 7)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(rockTerritories.contains(c.getLabel()));
            }
            for (String s : rockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ecaz.getMovement().getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentDiscoveryTokenChoices")
    class PresentDiscoveryTokenChoices {
        List<String> discoveryTokenTerritories;
        Territory meridianWestSector;
        Territory pastyMesaNorthSector;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            meridianWestSector = game.getTerritory("Meridian (West Sector)");
            meridianWestSector.setDiscoveryToken("Ecological Testing Station");
            meridianWestSector.setDiscovered(true);
            pastyMesaNorthSector = game.getTerritory("Pasty Mesa (North Sector)");
            pastyMesaNorthSector.setDiscoveryToken("Orgiz Processing Station");
            pastyMesaNorthSector.setDiscovered(true);
            discoveryTokenTerritories = List.of("Ecological Testing Station", "Orgiz Processing Station");
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentDiscoveryTokenChoices(game, ecaz);
            assertEquals("Which Discovery Token?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 2)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(discoveryTokenTerritories.contains(c.getLabel()));
            }
            for (String s : discoveryTokenTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ecaz.getMovement().presentDiscoveryTokenChoices(game, ecaz);
            assertEquals("Which Discovery Token?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 2)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(discoveryTokenTerritories.contains(c.getLabel()));
            }
            for (String s : discoveryTokenTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ecaz.getMovement().getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentNonSpiceNonRockChoices")
    class PresentNonSpiceNonRockChoices {
        List<String> nonSpiceNonRockTerritories;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            nonSpiceNonRockTerritories = game.getTerritories().getNonSpiceNonRockTerritoryNames();
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentNonSpiceNonRockChoices(game, ecaz);
            assertEquals("Which Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            ecaz.getMovement().presentNonSpiceNonRockChoices(game, ecaz);
            assertEquals("Which Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, ecaz.getMovement().getMoveType());
        }
    }

    @Nested
    @DisplayName("#presentSectorChoices")
    class PresentSectorChoices {
        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
        }

        @Test
        void testCielagoNorthWithFremenAmbassador() {
            ix.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            game.getTerritory("Cielago North (East Sector)").setSpice(6);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago North");
            ix.getMovement().presentSectorChoices(ix, "Cielago North", sectors);
            assertEquals("Which sector of Cielago North?", ixChat.getMessages().getLast());
            assertEquals(4, ixChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-sector-Cielago North (West Sector)", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("18 - West Sector", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-sector-Cielago North (Center Sector)", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("1 - Center Sector", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-fremen-sector-Cielago North (East Sector)", ixChat.getChoices().getLast().get(2).getId());
            assertEquals("2 - East Sector (6 spice)", ixChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-fremen-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
        }

        @Test
        void testWindPassWithGuildAmbassador() {
            ix.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Wind Pass");
            ix.getMovement().presentSectorChoices(ix, "Wind Pass", sectors);
            assertEquals("Which sector of Wind Pass?", ixChat.getMessages().getLast());
            assertEquals(5, ixChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-sector-Wind Pass (Far North Sector)", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("13 - Far North Sector", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-guild-sector-Wind Pass (North Sector)", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("14 - North Sector", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-guild-sector-Wind Pass (South Sector)", ixChat.getChoices().getLast().get(2).getId());
            assertEquals("15 - South Sector", ixChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-guild-sector-Wind Pass (Far South Sector)", ixChat.getChoices().getLast().get(3).getId());
            assertEquals("16 - Far South Sector", ixChat.getChoices().getLast().get(3).getLabel());
            assertEquals("ambassador-guild-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
        }
    }

    @Nested
    @DisplayName("#presentForcesChoices")
    class PresentForcesChoices {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            movement = ix.getMovement();
            movement.setMovingTo("Carthag");
            movement.setForce(0);
            movement.setSpecialForce(1);
        }

        @Test
        void testFremenAmbassador() {
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.presentForcesChoices(game, ix);
            assertEquals("Use buttons below to add forces to your ride. Currently moving:\n**0 " + Emojis.IX_SUBOID + " 1 " + Emojis.IX_CYBORG + "** to Carthag", ixChat.getMessages().getLast());
            assertEquals(8, ixChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-add-force-1", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("Add 1 troop", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-add-special-force-1", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("Add 1 * troop", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-fremen-add-force-2", ixChat.getChoices().getLast().get(2).getId());
            assertEquals("Add 2 troop", ixChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-fremen-add-special-force-2", ixChat.getChoices().getLast().get(3).getId());
            assertEquals("Add 2 * troop", ixChat.getChoices().getLast().get(3).getLabel());
            assertEquals("ambassador-fremen-add-force-3", ixChat.getChoices().getLast().get(4).getId());
            assertEquals("Add 3 troop", ixChat.getChoices().getLast().get(4).getLabel());
            assertEquals("ambassador-fremen-execute", ixChat.getChoices().getLast().get(5).getId());
            assertEquals("Confirm Movement", ixChat.getChoices().getLast().get(5).getLabel());
            assertEquals("ambassador-fremen-reset-forces", ixChat.getChoices().getLast().get(6).getId());
            assertEquals("Reset forces", ixChat.getChoices().getLast().get(6).getLabel());
            assertEquals("ambassador-fremen-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.presentForcesChoices(game, ix);
            assertEquals("Use buttons below to add forces to your shipment. Currently shipping:\n**0 " + Emojis.IX_SUBOID + " 1 " + Emojis.IX_CYBORG + "** to Carthag", ixChat.getMessages().getLast());
            assertEquals(9, ixChat.getChoices().getLast().size());
//            assertEquals(2, movement.getForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#addRegularForces")
    class AddRegularForces {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            movement = ix.getMovement();
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
        }

        @Test
        void testFremenAmbassador() {
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#resetForces")
    class ResetForces {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            movement = ix.getMovement();
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
        }

        @AfterEach
        void tearDown() {
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
        }

        @Test
        void testFremenAmbassador() {
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.resetForces();
            assertEquals("Hidden Mobile Stronghold", movement.getMovingFrom());
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.resetForces();
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#execute")
    class Execute {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            movement = ix.getMovement();
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
        }

        @AfterEach
        void tearDown() {
            assertEquals("", movement.getMovingTo());
            assertEquals("", movement.getMovingFrom());
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
            assertEquals("", movement.getSecondMovingFrom());
        }

        @Test
        void testFremenAmbassador() throws InvalidGameStateException {
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.execute(game, ix);
            assertEquals(1, carthag.getForceStrength("Ix"));
            assertEquals(2, carthag.getForceStrength("Ix*"));
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            assertEquals(2, hms.getForceStrength("Ix"));
            assertEquals(1, hms.getForceStrength("Ix*"));
            assertEquals(Emojis.IX + ": 1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " moved from Hidden Mobile Stronghold to Carthag.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() throws InvalidGameStateException {
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.execute(game, ix);
            assertEquals(1, carthag.getForceStrength("Ix"));
            assertEquals(2, carthag.getForceStrength("Ix*"));
            Territory ixHomeworld = game.getTerritory("Ix");
            assertEquals(9, ixHomeworld.getForceStrength("Ix"));
            assertEquals(2, ixHomeworld.getForceStrength("Ix*"));
            assertEquals(Emojis.IX + ": 1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " placed on Carthag", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }
}
