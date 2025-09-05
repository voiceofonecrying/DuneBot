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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            game.createAlliance(ecaz, ix);
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
        void testFremenRide() {
            fremen.setWormRideActive(true);
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_RIDE);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Funeral Plain");
            movement.pass();
            assertEquals("You will not ride the worm.", fremenChat.getMessages().getLast());
            assertEquals(Emojis.FREMEN + " does not ride the worm.", turnSummary.getMessages().getLast());
            assertFalse(fremen.isWormRideActive());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.pass();
            assertEquals("You will not ride the worm with the Fremen Ambassador.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + " does not ride the worm with the Fremen Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.pass();
            assertEquals("You will not ship with the Guild Ambassador.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + " does not ship with the Guild Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#startOver")
    class StartOver {
        Movement movement;
        String saveMovingFrom = "";

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            game.createAlliance(ecaz, ix);
            turnSummary.clear();
        }

        @AfterEach
        void tearDown() {
            assertEquals("", movement.getMovingTo());
            assertEquals(saveMovingFrom, movement.getMovingFrom());
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
            assertEquals("", movement.getSecondMovingFrom());
        }

        @Test
        void testFremenRide() {
            Territory funeralPlain = game.getTerritory("Funeral Plain");
            funeralPlain.addForces("Fremen", 1);
            funeralPlain.addForces("Fremen*", 2);
            fremen.setWormRideActive(true);
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_RIDE);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Funeral Plain");
            saveMovingFrom = movement.getMovingFrom();
            turnSummary.clear();
            movement.startOver();
            assertTrue(fremen.isWormRideActive());
            assertEquals("Where would you like to ride to from Funeral Plain? fr", fremenChat.getMessages().getLast());
            // TODO: Prevent new turn summary message when Fremen starts over
            assertEquals("1 " + Emojis.FREMEN_TROOP + " 2 " + Emojis.FREMEN_FEDAYKIN + " may ride Shai-Hulud from Funeral Plain!", turnSummary.getMessages().getLast());
//            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.FREMEN_RIDE, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            // TODO: Change to Ix when ally use of ambassadors is supported
            movement = ecaz.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.startOver();
            assertEquals("You have triggered your Fremen Ambassador!\nWhere would you like to ride from?", ecazChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            // TODO: Change to Ix when ally use of ambassadors is supported
            movement = ecaz.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingTo("Carthag");
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.startOver();
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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            strongholds = List.of("Arrakeen", "Carthag", "Sietch Tabr", "Habbanya Sietch", "Tuek's Sietch");
            strongholdsWithHMS = new ArrayList<>(strongholds);
            strongholdsWithHMS.add("Hidden Mobile Stronghold");
        }

        @Test
        void testFremenRide() {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            fremen.getMovement().presentStrongholdChoices();
            assertEquals("Which Stronghold?", fremenChat.getMessages().getLast());
            assertEquals(7, fremenChat.getChoices().getLast().size());
            assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentStrongholdChoices();
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
            ecaz.getMovement().presentStrongholdChoices();
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
            ix.getMovement().presentStrongholdChoices();
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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            spiceBlowTerritories = game.getTerritories().getSpiceBlowTerritoryNames();
        }

        @Test
        void testFremenRide() {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            fremen.getMovement().presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentSpiceBlowChoices();
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
            ecaz.getMovement().presentSpiceBlowChoices();
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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
            rockTerritories = game.getTerritories().getRockTerritoryNames();
        }

        @Test
        void testFremenRide() {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            fremen.getMovement().presentRockChoices();
            assertEquals("Which Rock Territory?", fremenChat.getMessages().getLast());
            assertEquals(8, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 7)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(rockTerritories.contains(c.getLabel()));
            }
            for (String s : rockTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentRockChoices();
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
            ecaz.getMovement().presentRockChoices();
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
        void testFremenRide() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            ecaz.getMovement().presentDiscoveryTokenChoices();
            assertEquals("Which Discovery Token?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("fremen-ride-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 2)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(discoveryTokenTerritories.contains(c.getLabel()));
            }
            for (String s : discoveryTokenTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, ecaz.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentDiscoveryTokenChoices();
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
            ecaz.getMovement().presentDiscoveryTokenChoices();
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
        void testFremenRide() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            ecaz.getMovement().presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("fremen-ride-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, ecaz.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentNonSpiceNonRockChoices();
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
            ecaz.getMovement().presentNonSpiceNonRockChoices();
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
        void testCielagoNorthWithFremenRide() {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            game.getTerritory("Cielago South (West Sector)").setSpice(12);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago South");
            fremen.getMovement().presentSectorChoices("Cielago South", sectors);
            assertEquals("Which sector of Cielago South?", fremenChat.getMessages().getLast());
            assertEquals(3, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-ride-sector-Cielago South (West Sector)", fremenChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector (12 spice)", fremenChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("fremen-ride-sector-Cielago South (East Sector)", fremenChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", fremenChat.getChoices().getLast().get(1).getLabel());
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
        }

        @Test
        void testCielagoNorthWithFremenAmbassador() {
            ix.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            game.getTerritory("Cielago North (East Sector)").setSpice(8);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago North");
            ix.getMovement().presentSectorChoices("Cielago North", sectors);
            assertEquals("Which sector of Cielago North?", ixChat.getMessages().getLast());
            assertEquals(4, ixChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-sector-Cielago North (West Sector)", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("18 - West Sector", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-sector-Cielago North (Center Sector)", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("1 - Center Sector", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-fremen-sector-Cielago North (East Sector)", ixChat.getChoices().getLast().get(2).getId());
            assertEquals("2 - East Sector (8 spice)", ixChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-fremen-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
        }

        @Test
        void testWindPassWithGuildAmbassador() {
            ix.getMovement().setMoveType(MoveType.GUILD_AMBASSADOR);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Wind Pass");
            ix.getMovement().presentSectorChoices("Wind Pass", sectors);
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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
        }

        @Test
        void testFremenRide() {
            Territory funeralPlain = game.getTerritory("Funeral Plain");
            funeralPlain.addForces("Fremen", 1);
            funeralPlain.addForces("Fremen*", 3);
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_RIDE);
            movement.setMovingTo("Carthag");
            movement.setForce(0);
            movement.setSpecialForce(1);
            movement.setMovingFrom("Funeral Plain");
            movement.presentForcesChoices();
            assertEquals("Use buttons below to add forces to your ride. Currently moving:\n**0 " + Emojis.FREMEN_TROOP + " 1 " + Emojis.FREMEN_FEDAYKIN + "** to Carthag", fremenChat.getMessages().getLast());
            assertEquals(6, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-ride-add-force-1", fremenChat.getChoices().getLast().getFirst().getId());
            assertEquals("Add 1 troop", fremenChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("fremen-ride-add-special-force-1", fremenChat.getChoices().getLast().get(1).getId());
            assertEquals("Add 1 * troop", fremenChat.getChoices().getLast().get(1).getLabel());
            assertEquals("fremen-ride-add-special-force-2", fremenChat.getChoices().getLast().get(2).getId());
            assertEquals("Add 2 * troop", fremenChat.getChoices().getLast().get(2).getLabel());
            assertEquals("fremen-ride-execute", fremenChat.getChoices().getLast().get(3).getId());
            assertEquals("Confirm Movement", fremenChat.getChoices().getLast().get(3).getLabel());
            assertEquals("fremen-ride-reset-forces", fremenChat.getChoices().getLast().get(4).getId());
            assertEquals("Reset forces", fremenChat.getChoices().getLast().get(4).getLabel());
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_RIDE, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(0);
            movement.setSpecialForce(1);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.presentForcesChoices();
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
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(0);
            movement.setSpecialForce(1);
            movement.presentForcesChoices();
            assertEquals("Use buttons below to add forces to your shipment. Currently shipping:\n**0 " + Emojis.IX_SUBOID + " 1 " + Emojis.IX_CYBORG + "** to Carthag", ixChat.getMessages().getLast());
            assertEquals(9, ixChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-add-force-1", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("Add 1 troop", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-guild-add-special-force-1", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("Add 1 * troop", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-guild-add-force-2", ixChat.getChoices().getLast().get(2).getId());
            assertEquals("Add 2 troop", ixChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-guild-add-special-force-2", ixChat.getChoices().getLast().get(3).getId());
            assertEquals("Add 2 * troop", ixChat.getChoices().getLast().get(3).getLabel());
            assertEquals("ambassador-guild-add-force-3", ixChat.getChoices().getLast().get(4).getId());
            assertEquals("Add 3 troop", ixChat.getChoices().getLast().get(4).getLabel());
            assertEquals("ambassador-guild-add-special-force-3", ixChat.getChoices().getLast().get(5).getId());
            assertEquals("Add 3 * troop", ixChat.getChoices().getLast().get(5).getLabel());
            assertEquals("ambassador-guild-execute", ixChat.getChoices().getLast().get(6).getId());
            assertEquals("Confirm Shipment", ixChat.getChoices().getLast().get(6).getLabel());
            assertEquals("ambassador-guild-reset-forces", ixChat.getChoices().getLast().get(7).getId());
            assertEquals("Reset forces", ixChat.getChoices().getLast().get(7).getLabel());
            assertEquals("ambassador-guild-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
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
        }

        @Test
        void testFremenRide() {
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Funeral Plain");
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#addSpecialForces")
    class AddSpecialForces {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
        }

        @Test
        void testFremenRide() {
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Funeral Plain");
            movement.addSpecialForces(1);
            assertEquals(3, movement.getSpecialForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.addSpecialForces(1);
            assertEquals(3, movement.getSpecialForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.addSpecialForces(1);
            assertEquals(3, movement.getSpecialForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#resetForces")
    class ResetForces {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
        }

        @AfterEach
        void tearDown() {
            assertEquals(0, movement.getForce());
            assertEquals(0, movement.getSpecialForce());
            assertEquals(0, movement.getSecondForce());
            assertEquals(0, movement.getSecondSpecialForce());
        }

        @Test
        void testFremenRide() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_RIDE);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Funeral Plain");
            movement.resetForces();
            assertEquals("Funeral Plain", movement.getMovingFrom());
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(MoveType.FREMEN_RIDE, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.resetForces();
            assertEquals("Hidden Mobile Stronghold", movement.getMovingFrom());
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
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
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            emperor.setChat(ixChat);
            game.createAlliance(ecaz, ix);
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
        void testFremenRide() throws InvalidGameStateException {
            Territory funeralPlain = game.getTerritory("Funeral Plain");
            funeralPlain.addForces("Fremen", 1);
            funeralPlain.addForces("Fremen*", 2);
            fremen.setWormRideActive(true);
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.FREMEN_RIDE);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Funeral Plain");
            movement.execute();
            assertFalse(fremen.isWormRideActive());
            assertEquals(1, carthag.getForceStrength("Fremen"));
            assertEquals(2, carthag.getForceStrength("Fremen*"));
            assertEquals(0, funeralPlain.getForceStrength("Fremen"));
            assertEquals(0, funeralPlain.getForceStrength("Fremen*"));
            assertEquals("Worm ride complete.", fremenChat.getMessages().getLast());
            assertEquals(Emojis.FREMEN + ": 1 " + Emojis.FREMEN_TROOP + " 2 " + Emojis.FREMEN_FEDAYKIN + " moved from Funeral Plain to Carthag.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() throws InvalidGameStateException {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.FREMEN_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.setMovingFrom("Hidden Mobile Stronghold");
            movement.execute();
            assertEquals(1, carthag.getForceStrength("Ix"));
            assertEquals(2, carthag.getForceStrength("Ix*"));
            Territory hms = game.getTerritory("Hidden Mobile Stronghold");
            assertEquals(2, hms.getForceStrength("Ix"));
            assertEquals(1, hms.getForceStrength("Ix*"));
            assertEquals("Ride with Fremen Ambassador complete.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + ": 1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " moved from Hidden Mobile Stronghold to Carthag.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testGuildAmbassador() throws InvalidGameStateException {
            movement = ix.getMovement();
            movement.setMoveType(MoveType.GUILD_AMBASSADOR);
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.execute();
            assertEquals(1, carthag.getForceStrength("Ix"));
            assertEquals(2, carthag.getForceStrength("Ix*"));
            Territory ixHomeworld = game.getTerritory("Ix");
            assertEquals(9, ixHomeworld.getForceStrength("Ix"));
            assertEquals(2, ixHomeworld.getForceStrength("Ix*"));
            assertEquals("Shipment with Guild Ambassador complete.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + ": 1 " + Emojis.IX_SUBOID + " 2 " + Emojis.IX_CYBORG + " placed on Carthag", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }
}
