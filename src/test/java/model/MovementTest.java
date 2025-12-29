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
    List<String> strongholds;
    List<String> strongholdsWithHMS;
    List<String> spiceBlowTerritories;
    List<String> rockTerritories;
    List<String> nonSpiceNonRockTerritories;
    List<String> discoveryTokenTerritories;
    Territory meridianWestSector;
    Territory pastyMesaNorthSector;

    @BeforeEach
    void setUp() throws InvalidGameStateException, IOException {
        super.setUp();
        game.addFaction(ix);
        strongholds = List.of("Arrakeen", "Carthag", "Sietch Tabr", "Habbanya Sietch", "Tuek's Sietch");
        strongholdsWithHMS = new ArrayList<>(strongholds);
        strongholdsWithHMS.add("Hidden Mobile Stronghold");
        spiceBlowTerritories = game.getTerritories().getSpiceBlowTerritoryNames();
        rockTerritories = game.getTerritories().getRockTerritoryNames();
        nonSpiceNonRockTerritories = game.getTerritories().getNonSpiceNonRockTerritoryNames();
        meridianWestSector = game.getTerritory("Meridian (West Sector)");
        meridianWestSector.setDiscoveryToken("Ecological Testing Station");
        meridianWestSector.setDiscovered(true);
        pastyMesaNorthSector = game.getTerritory("Pasty Mesa (North Sector)");
        pastyMesaNorthSector.setDiscoveryToken("Orgiz Processing Station");
        pastyMesaNorthSector.setDiscovered(true);
        discoveryTokenTerritories = List.of("Ecological Testing Station", "Orgiz Processing Station");
    }

    @Nested
    @DisplayName("#pass")
    class Pass {
        Movement movement;

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
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
        void testShaiHuludPlacement() throws InvalidGameStateException {
            fremen.addWormToPlace("Funeral Plain", "Shai-Hulud");
            assertEquals(1, fremen.getWormsToPlace());
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.SHAI_HULUD_PLACEMENT);
            movement.setMovingTo("Sihaya Ridge");
            movement.setMovingFrom("Funeral Plain");
            movement.pass();
            assertEquals("You left Shai-Hulud in Funeral Plain.", fremenChat.getMessages().getLast());
            assertEquals("Shai-Hulud remains in Funeral Plain.\n", turnSummary.getMessages().getLast());
            assertEquals(0, fremen.getWormsToPlace());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testGreatMakerPlacement() throws InvalidGameStateException {
            fremen.addWormToPlace("Funeral Plain", "Great Maker");
            assertEquals(1, fremen.getWormsToPlace());
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            movement.setMovingTo("Sihaya Ridge");
            movement.setMovingFrom("Funeral Plain");
            movement.pass();
            assertEquals("You left Great Maker in Funeral Plain.", fremenChat.getMessages().getLast());
            assertEquals("Great Maker remains in Funeral Plain.\nAfter the Nexus, 17 " + Emojis.FREMEN_TROOP + " 3 " + Emojis.FREMEN_FEDAYKIN + " in reserves may ride Great Maker!\n", turnSummary.getMessages().getLast());
            assertEquals(0, fremen.getWormsToPlace());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }

        @Test
        void testFremenAmbassador() throws InvalidGameStateException {
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
        void testGuildAmbassador() throws InvalidGameStateException {
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
        void testShaiHuludPlacement() {
            fremen.addWormToPlace("Funeral Plain", "Shai-Hulud");
            assertEquals(1, fremen.getWormsToPlace());
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.SHAI_HULUD_PLACEMENT);
            movement.setMovingTo("Sihaya Ridge");
            movement.setMovingFrom("Funeral Plain");
            saveMovingFrom = movement.getMovingFrom();
            turnSummary.clear();
            movement.startOver();
            assertEquals(1, fremen.getWormsToPlace());
            assertEquals("Where would you like to place Shai-Hulud? fr", fremenChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.SHAI_HULUD_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testGreatMakerPlacement() {
            fremen.addWormToPlace("Funeral Plain", "Great Maker");
            assertEquals(1, fremen.getWormsToPlace());
            movement = fremen.getMovement();
            movement.setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            movement.setMovingTo("Sihaya Ridge");
            movement.setMovingFrom("Funeral Plain");
            saveMovingFrom = movement.getMovingFrom();
            turnSummary.clear();
            movement.startOver();
            assertEquals(1, fremen.getWormsToPlace());
            assertEquals("Where would you like to place Great Maker? fr", fremenChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.GREAT_MAKER_PLACEMENT, movement.getMoveType());
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
        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
        }

        @Test
        void testFremenRide() throws InvalidGameStateException {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            fremen.getMovement().presentStrongholdChoices();
            assertEquals("Which Stronghold?", fremenChat.getMessages().getLast());
            assertEquals(7, fremenChat.getChoices().getLast().size());
            assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testFremenAmbassador() throws InvalidGameStateException {
            ecaz.getMovement().setMoveType(MoveType.FREMEN_AMBASSADOR);
            ecaz.getMovement().presentStrongholdChoices();
            assertEquals("Which Stronghold?", ecazChat.getMessages().getLast());
            assertEquals(7, ecazChat.getChoices().getLast().size());
            assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, ecaz.getMovement().getMoveType());
        }

        @Test
        void testGuildAmbassador() throws InvalidGameStateException {
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
        void testGuildAmbassadorByIxAlly() throws InvalidGameStateException {
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
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
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

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
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
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testShaiHuludPlacement() {
            fremen.getMovement().setMoveType(MoveType.SHAI_HULUD_PLACEMENT);
            fremen.getMovement().presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-place-shai-hulud-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-place-shai-hulud-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-place-shai-hulud-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.SHAI_HULUD_PLACEMENT, fremen.getMovement().getMoveType());
        }

        @Test
        void testGreatMakerPlacement() {
            fremen.getMovement().setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            fremen.getMovement().presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-place-great-maker-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-place-great-maker-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-place-great-maker-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GREAT_MAKER_PLACEMENT, fremen.getMovement().getMoveType());
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

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
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

        @BeforeEach
        void setUp() {
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
        }

        @Test
        void testFremenRide() throws InvalidGameStateException {
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
        void testFremenAmbassador() throws InvalidGameStateException {
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
        void testGuildAmbassador() throws InvalidGameStateException {
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

        @BeforeEach
        void setUp() {
            game.addFaction(fremen);
            game.addFaction(ecaz);
            game.addFaction(ix);
            ecaz.setChat(ecazChat);
            ix.setChat(ixChat);
        }

        @Test
        void testFremenRide() {
            fremen.getMovement().setMoveType(MoveType.FREMEN_RIDE);
            fremen.getMovement().presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-ride-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-ride-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-ride-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_RIDE, fremen.getMovement().getMoveType());
        }

        @Test
        void testShaiHuludPlacement() {
            fremen.getMovement().setMoveType(MoveType.SHAI_HULUD_PLACEMENT);
            fremen.getMovement().presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-place-shai-hulud-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-place-shai-hulud-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-place-shai-hulud-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.SHAI_HULUD_PLACEMENT, fremen.getMovement().getMoveType());
        }

        @Test
        void testGreatMakerPlacement() {
            fremen.getMovement().setMoveType(MoveType.GREAT_MAKER_PLACEMENT);
            fremen.getMovement().presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", fremenChat.getMessages().getLast());
            assertEquals(16, fremenChat.getChoices().getLast().size());
            assertEquals("fremen-place-great-maker-start-over", fremenChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", fremenChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : fremenChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("fremen-place-great-maker-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("fremen-place-great-maker-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(fremenChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GREAT_MAKER_PLACEMENT, fremen.getMovement().getMoveType());
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
            DuneChoice choice = fremenChat.getChoices().getLast().getFirst();
            assertEquals("fremen-ride-add-force-1", choice.getId());
            assertEquals("+1", choice.getLabel());
            choice = fremenChat.getChoices().getLast().get(1);
            assertEquals("fremen-ride-add-special-force-1", choice.getId());
            assertEquals("+1 *", choice.getLabel());
            choice = fremenChat.getChoices().getLast().get(2);
            assertEquals("fremen-ride-add-special-force-2", choice.getId());
            assertEquals("+2 *", choice.getLabel());
            choice = fremenChat.getChoices().getLast().get(3);
            assertEquals("fremen-ride-execute", choice.getId());
            assertEquals("Confirm Movement", choice.getLabel());
            choice = fremenChat.getChoices().getLast().get(4);
            assertEquals("fremen-ride-reset-forces", choice.getId());
            assertEquals("Reset forces", choice.getLabel());
            choice = fremenChat.getChoices().getLast().getLast();
            assertEquals("fremen-ride-start-over", choice.getId());
            assertEquals("Start over", choice.getLabel());
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
            DuneChoice choice = ixChat.getChoices().getLast().getFirst();
            assertEquals("ambassador-fremen-add-force-1", choice.getId());
            assertEquals("+1", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(1);
            assertEquals("ambassador-fremen-add-special-force-1", choice.getId());
            assertEquals("+1 *", choice.getLabel());
            assertEquals(Emojis.IX_CYBORG, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(2);
            assertEquals("ambassador-fremen-add-force-2", choice.getId());
            assertEquals("+2", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(3);
            assertEquals("ambassador-fremen-add-special-force-2", choice.getId());
            assertEquals("+2 *", choice.getLabel());
            assertEquals(Emojis.IX_CYBORG, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(4);
            assertEquals("ambassador-fremen-add-force-3", choice.getId());
            assertEquals("+3", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(5);
            assertEquals("ambassador-fremen-execute", choice.getId());
            assertEquals("Confirm Movement", choice.getLabel());
            choice = ixChat.getChoices().getLast().get(6);
            assertEquals("ambassador-fremen-reset-forces", choice.getId());
            assertEquals("Reset forces", choice.getLabel());
            choice = ixChat.getChoices().getLast().getLast();
            assertEquals("ambassador-fremen-start-over", choice.getId());
            assertEquals("Start over", choice.getLabel());
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
            DuneChoice choice = ixChat.getChoices().getLast().getFirst();
            assertEquals("ambassador-guild-add-force-1", choice.getId());
            assertEquals("+1", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(1);
            assertEquals("ambassador-guild-add-special-force-1", choice.getId());
            assertEquals("+1 *", choice.getLabel());
            assertEquals(Emojis.IX_CYBORG, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(2);
            assertEquals("ambassador-guild-add-force-2", choice.getId());
            assertEquals("+2", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(3);
            assertEquals("ambassador-guild-add-special-force-2", choice.getId());
            assertEquals("+2 *", choice.getLabel());
            assertEquals(Emojis.IX_CYBORG, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(4);
            assertEquals("ambassador-guild-add-force-3", choice.getId());
            assertEquals("+3", choice.getLabel());
            assertEquals(Emojis.IX_SUBOID, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(5);
            assertEquals("ambassador-guild-add-special-force-3", choice.getId());
            assertEquals("+3 *", choice.getLabel());
            assertEquals(Emojis.IX_CYBORG, choice.getEmoji());
            choice = ixChat.getChoices().getLast().get(6);
            assertEquals("ambassador-guild-execute", choice.getId());
            assertEquals("Confirm Shipment", choice.getLabel());
            choice = ixChat.getChoices().getLast().get(7);
            assertEquals("ambassador-guild-reset-forces", choice.getId());
            assertEquals("Reset forces", choice.getLabel());
            choice = ixChat.getChoices().getLast().getLast();
            assertEquals("ambassador-guild-start-over", choice.getId());
            assertEquals("Start over", choice.getLabel());
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
            game.addFaction(fremen);
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
            game.addFaction(fremen);
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

    @Nested
    @DisplayName("#fremenAmbassador")
    class FremenAmbassador {
        Movement movement;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(ecaz);
            game.addFaction(harkonnen);
            carthag.setEcazAmbassador("Fremen");
            ecaz.triggerAmbassador(harkonnen, "Fremen");
            ecazChat.clear();
            turnSummary.clear();
            movement = ecaz.getMovement();
        }

        @Test
        void testPass() throws InvalidGameStateException {
            movement.pass();
            assertEquals("You will not ride the worm with the Fremen Ambassador.", ecazChat.getMessages().getLast());
            assertEquals(Emojis.ECAZ + " does not ride the worm with the Fremen Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
            assertTrue(movement.getMovingFrom().isEmpty());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStartOver() {
            movement.setForce(4);
            movement.setMovingTo("Carthag");
            movement.startOver();
            assertEquals("You have triggered your Fremen Ambassador!\nWhere would you like to ride from?", ecazChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(2, ecazChat.getChoices().getLast().size());
            assertEquals("ecaz-fremen-move-from-Imperial Basin (Center Sector)", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("Imperial Basin (Center Sector)", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-pass", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Decline ride", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
            assertTrue(movement.getMovingFrom().isEmpty());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStrongholdChoices() throws InvalidGameStateException {
            movement.presentStrongholdChoices();
            assertEquals("Which Stronghold?", ecazChat.getMessages().getLast());
            assertEquals(7, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 5)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testSpiceBlowChoices() {
            movement.presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testRockChoices() {
            movement.presentRockChoices();
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
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testDiscoveryTokenChoices() throws InvalidGameStateException {
            movement.presentDiscoveryTokenChoices();
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
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testNonSpiceNonRockChoices() {
            movement.presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-fremen-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ambassador-fremen-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_OneSectorTerritory() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            boolean stateChanged = movement.processTerritory("Carthag");
            assertTrue(stateChanged);
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals("Use buttons below to add forces to your ride. Currently moving:\n**0 " + Emojis.ECAZ_TROOP + " ** to Carthag", ecazChat.getMessages().getLast());
            assertEquals(7, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-add-force-1", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("+1", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().getFirst().getEmoji());
            assertEquals("ambassador-fremen-add-force-2", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("+2", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(1).getEmoji());
            assertEquals("ambassador-fremen-add-force-3", ecazChat.getChoices().getLast().get(2).getId());
            assertEquals("+3", ecazChat.getChoices().getLast().get(2).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(2).getEmoji());
            assertEquals("ambassador-fremen-add-force-4", ecazChat.getChoices().getLast().get(3).getId());
            assertEquals("+4", ecazChat.getChoices().getLast().get(3).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(3).getEmoji());
            assertEquals("ambassador-fremen-add-force-5", ecazChat.getChoices().getLast().get(4).getId());
            assertEquals("+5", ecazChat.getChoices().getLast().get(4).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(4).getEmoji());
            assertEquals("ambassador-fremen-add-force-6", ecazChat.getChoices().getLast().get(5).getId());
            assertEquals("+6", ecazChat.getChoices().getLast().get(5).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(5).getEmoji());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_MultiSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Cielago South");
            assertFalse(stateChanged);
            assertEquals("Which sector of Cielago South?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-sector-Cielago South (West Sector)", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-sector-Cielago South (East Sector)", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testSectorChoices() {
            game.getTerritory("Cielago South (West Sector)").setSpice(12);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago South");
            movement.presentSectorChoices("Cielago South", sectors);
            assertEquals("Which sector of Cielago South?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-sector-Cielago South (West Sector)", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector (12 spice)", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-fremen-sector-Cielago South (East Sector)", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessSector() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.processSector("Cielago South (West Sector)");
            assertEquals("Cielago South (West Sector)", movement.getMovingTo());
            assertEquals("Use buttons below to add forces to your ride. Currently moving:\n**0 " + Emojis.ECAZ_TROOP + " ** to Cielago South (West Sector)", ecazChat.getMessages().getLast());
            assertEquals(7, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-fremen-add-force-1", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("+1", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().getFirst().getEmoji());
            assertEquals("ambassador-fremen-add-force-2", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("+2", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(1).getEmoji());
            assertEquals("ambassador-fremen-add-force-3", ecazChat.getChoices().getLast().get(2).getId());
            assertEquals("+3", ecazChat.getChoices().getLast().get(2).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(2).getEmoji());
            assertEquals("ambassador-fremen-add-force-4", ecazChat.getChoices().getLast().get(3).getId());
            assertEquals("+4", ecazChat.getChoices().getLast().get(3).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(3).getEmoji());
            assertEquals("ambassador-fremen-add-force-5", ecazChat.getChoices().getLast().get(4).getId());
            assertEquals("+5", ecazChat.getChoices().getLast().get(4).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(4).getEmoji());
            assertEquals("ambassador-fremen-add-force-6", ecazChat.getChoices().getLast().get(5).getId());
            assertEquals("+6", ecazChat.getChoices().getLast().get(5).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(5).getEmoji());
            assertEquals("ambassador-fremen-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testPresentForcesChoices_OneForceAlreadySelected() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(0);
            movement.presentForcesChoices();
            // TODO: Fix the extra space at end of forcesStringWithZeroes
            assertEquals("Use buttons below to add forces to your ride. Currently moving:\n**1 " + Emojis.ECAZ_TROOP + " ** to Carthag", ecazChat.getMessages().getLast());
            assertEquals(8, ecazChat.getChoices().getLast().size());
            DuneChoice choice = ecazChat.getChoices().getLast().getFirst();
            assertEquals("ambassador-fremen-add-force-1", choice.getId());
            assertEquals("+1", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(1);
            assertEquals("ambassador-fremen-add-force-2", choice.getId());
            assertEquals("+2", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(2);
            assertEquals("ambassador-fremen-add-force-3", choice.getId());
            assertEquals("+3", choice.getLabel());
            choice = ecazChat.getChoices().getLast().get(3);
            assertEquals("ambassador-fremen-add-force-4", choice.getId());
            assertEquals("+4", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(4);
            assertEquals("ambassador-fremen-add-force-5", choice.getId());
            assertEquals("+5", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(5);
            assertEquals("ambassador-fremen-execute", choice.getId());
            assertEquals("Confirm Movement", choice.getLabel());
            choice = ecazChat.getChoices().getLast().get(6);
            assertEquals("ambassador-fremen-reset-forces", choice.getId());
            assertEquals("Reset forces", choice.getLabel());
            choice = ecazChat.getChoices().getLast().getLast();
            assertEquals("ambassador-fremen-start-over", choice.getId());
            assertEquals("Start over", choice.getLabel());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testAddRegularForces() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testAddSpecialForces() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.addSpecialForces(1);
            // TODO: Should throw since Ecaz does not have special forces
//            assertThrows(InvalidGameStateException.class, () -> movement.addSpecialForces(1));
            assertEquals(3, movement.getSpecialForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testResetForces() {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.resetForces();
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(0, movement.getForce());
            assertEquals(MoveType.FREMEN_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testExecute() throws InvalidGameStateException {
            movement.setMovingFrom("Imperial Basin (Center Sector)");
            movement.setMovingTo("Carthag");
            movement.setForce(4);
            boolean advanceGame = movement.execute();
            assertFalse(advanceGame);
            assertFalse(bt.isBtHTActive());
            assertEquals(4, carthag.getForceStrength("Ecaz"));
            assertEquals(2, game.getTerritory("Imperial Basin (Center Sector)").getForceStrength("Ecaz"));
            assertEquals("Ride with Fremen Ambassador complete.", ecazChat.getMessages().getLast());
            assertEquals(Emojis.ECAZ + ": 4 " + Emojis.ECAZ_TROOP + " moved from Imperial Basin (Center Sector) to Carthag.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#guildAmbassador")
    class GuildAmbassador {
        Movement movement;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(ecaz);
            game.addFaction(harkonnen);
            carthag.setEcazAmbassador("Guild");
            ecaz.triggerAmbassador(harkonnen, "Guild");
            ecazChat.clear();
            turnSummary.clear();
            movement = ecaz.getMovement();
        }

        @Test
        void testPass() throws InvalidGameStateException {
            movement.pass();
            assertEquals("You will not ship with the Guild Ambassador.", ecazChat.getMessages().getLast());
            assertEquals(Emojis.ECAZ + " does not ship with the Guild Ambassador.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
            assertTrue(movement.getMovingFrom().isEmpty());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStartOver() {
            movement.setForce(4);
            movement.setMovingTo("Carthag");
            movement.startOver();
            assertEquals("You have triggered your Guild Ambassador!\nWhere would you like to place up to 4 " + Emojis.ECAZ_TROOP + " from reserves?", ecazChat.getMessages().getLast());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(5, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-stronghold", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("Stronghold", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-guild-spice-blow", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("Spice Blow Territories", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-guild-rock", ecazChat.getChoices().getLast().get(2).getId());
            assertEquals("Rock Territories", ecazChat.getChoices().getLast().get(2).getLabel());
            assertEquals("ambassador-guild-other", ecazChat.getChoices().getLast().get(3).getId());
            assertEquals("Somewhere else", ecazChat.getChoices().getLast().get(3).getLabel());
            assertEquals("ambassador-guild-pass", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Pass shipment", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStrongholdChoices() throws InvalidGameStateException {
            movement.presentStrongholdChoices();
            assertEquals("Which Stronghold?", ecazChat.getMessages().getLast());
            assertEquals(6, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 5)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholds.contains(c.getLabel()));
            }
            for (String s : strongholds)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testSpiceBlowChoices() {
            movement.presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testRockChoices() {
            movement.presentRockChoices();
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
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testDiscoveryTokenChoices() throws InvalidGameStateException {
            movement.presentDiscoveryTokenChoices();
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
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testNonSpiceNonRockChoices() {
            movement.presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", ecazChat.getMessages().getLast());
            assertEquals(16, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ecazChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ambassador-guild-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ambassador-guild-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ecazChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_OneSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Carthag");
            assertTrue(stateChanged);
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals("Use buttons below to add forces to your shipment. Currently shipping:\n**0 " + Emojis.ECAZ_TROOP + " ** to Carthag", ecazChat.getMessages().getLast());
            assertEquals(5, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-add-force-1", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("+1", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().getFirst().getEmoji());
            assertEquals("ambassador-guild-add-force-2", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("+2", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(1).getEmoji());
            assertEquals("ambassador-guild-add-force-3", ecazChat.getChoices().getLast().get(2).getId());
            assertEquals("+3", ecazChat.getChoices().getLast().get(2).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(2).getEmoji());
            assertEquals("ambassador-guild-add-force-4", ecazChat.getChoices().getLast().get(3).getId());
            assertEquals("+4", ecazChat.getChoices().getLast().get(3).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(3).getEmoji());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_MultiSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Cielago South");
            assertFalse(stateChanged);
            assertEquals("Which sector of Cielago South?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-sector-Cielago South (West Sector)", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-guild-sector-Cielago South (East Sector)", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testSectorChoices() {
            game.getTerritory("Cielago South (West Sector)").setSpice(12);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago South");
            movement.presentSectorChoices("Cielago South", sectors);
            assertEquals("Which sector of Cielago South?", ecazChat.getMessages().getLast());
            assertEquals(3, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-sector-Cielago South (West Sector)", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector (12 spice)", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ambassador-guild-sector-Cielago South (East Sector)", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testProcessSector() {
            movement.processSector("Cielago South (West Sector)");
            assertEquals("Cielago South (West Sector)", movement.getMovingTo());
            assertEquals("Use buttons below to add forces to your shipment. Currently shipping:\n**0 " + Emojis.ECAZ_TROOP + " ** to Cielago South (West Sector)", ecazChat.getMessages().getLast());
            assertEquals(5, ecazChat.getChoices().getLast().size());
            assertEquals("ambassador-guild-add-force-1", ecazChat.getChoices().getLast().getFirst().getId());
            assertEquals("+1", ecazChat.getChoices().getLast().getFirst().getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().getFirst().getEmoji());
            assertEquals("ambassador-guild-add-force-2", ecazChat.getChoices().getLast().get(1).getId());
            assertEquals("+2", ecazChat.getChoices().getLast().get(1).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(1).getEmoji());
            assertEquals("ambassador-guild-add-force-3", ecazChat.getChoices().getLast().get(2).getId());
            assertEquals("+3", ecazChat.getChoices().getLast().get(2).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(2).getEmoji());
            assertEquals("ambassador-guild-add-force-4", ecazChat.getChoices().getLast().get(3).getId());
            assertEquals("+4", ecazChat.getChoices().getLast().get(3).getLabel());
            assertEquals(Emojis.ECAZ_TROOP, ecazChat.getChoices().getLast().get(3).getEmoji());
            assertEquals("ambassador-guild-start-over", ecazChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ecazChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testPresentForcesChoices_OneForceAlreadySelected() {
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(0);
            movement.presentForcesChoices();
            // TODO: Fix the extra space at end of forcesStringWithZeroes
            assertEquals("Use buttons below to add forces to your shipment. Currently shipping:\n**1 " + Emojis.ECAZ_TROOP + " ** to Carthag", ecazChat.getMessages().getLast());
            assertEquals(6, ecazChat.getChoices().getLast().size());
            DuneChoice choice = ecazChat.getChoices().getLast().getFirst();
            assertEquals("ambassador-guild-add-force-1", choice.getId());
            assertEquals("+1", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(1);
            assertEquals("ambassador-guild-add-force-2", choice.getId());
            assertEquals("+2", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(2);
            assertEquals("ambassador-guild-add-force-3", choice.getId());
            assertEquals("+3", choice.getLabel());
            assertEquals(Emojis.ECAZ_TROOP, choice.getEmoji());
            choice = ecazChat.getChoices().getLast().get(3);
            assertEquals("ambassador-guild-execute", choice.getId());
            assertEquals("Confirm Shipment", choice.getLabel());
            choice = ecazChat.getChoices().getLast().get(4);
            assertEquals("ambassador-guild-reset-forces", choice.getId());
            assertEquals("Reset forces", choice.getLabel());
            choice = ecazChat.getChoices().getLast().getLast();
            assertEquals("ambassador-guild-start-over", choice.getId());
            assertEquals("Start over", choice.getLabel());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testAddRegularForces() {
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.addRegularForces(1);
            assertEquals(2, movement.getForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testAddSpecialForces() {
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.setSpecialForce(2);
            movement.addSpecialForces(1);
            // TODO: Should throw since Ecaz does not have special forces
//            assertThrows(InvalidGameStateException.class, () -> movement.addSpecialForces(1));
            assertEquals(3, movement.getSpecialForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testResetForces() {
            movement.setMovingTo("Carthag");
            movement.setForce(1);
            movement.resetForces();
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(0, movement.getForce());
            assertEquals(MoveType.GUILD_AMBASSADOR, movement.getMoveType());
        }

        @Test
        void testExecute() throws InvalidGameStateException {
            movement.setMovingTo("Carthag");
            movement.setForce(4);
            boolean advanceGame = movement.execute();
            assertFalse(advanceGame);
            assertFalse(bt.isBtHTActive());
            assertEquals(4, carthag.getForceStrength("Ecaz"));
            assertEquals(10, game.getTerritory("Ecaz").getForceStrength("Ecaz"));
            assertEquals("Shipment with Guild Ambassador complete.", ecazChat.getMessages().getLast());
            assertEquals(Emojis.ECAZ + ": 4 " + Emojis.ECAZ_TROOP + " placed on Carthag", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#btHighThreshold")
    class BTHighThreshold {
        Movement movement;

        @BeforeEach
        void setUp() throws InvalidGameStateException {
            game.addFaction(bt);
            bt.removeForces("Tleilax", 2, false, true);
            game.startRevival();
            if (game.getRevival().performPreSteps(game)) {
                game.getRevival().startRevivingForces(game);
            }
            bt.presentHTChoices();
            btChat.clear();
            turnSummary.clear();
            movement = bt.getMovement();
        }

        @Test
        void testPass() throws InvalidGameStateException {
            movement.pass();
            assertEquals("You will leave your free revivals on Tleilax.", btChat.getMessages().getLast());
            assertEquals(Emojis.BT + " leaves their free revivals on Tleilax.", turnSummary.getMessages().getLast());
            assertFalse(bt.isBtHTActive());
            assertEquals(MoveType.TBD, movement.getMoveType());
            assertTrue(movement.getMovingFrom().isEmpty());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStartOver() {
            movement.setMovingTo("Carthag");
            movement.startOver();
            assertEquals("Where would you like to place your 2 " + Emojis.BT_TROOP + " free revivals? bt", btChat.getMessages().getLast());
            assertEquals(Emojis.BT + " may place 2 free revived " + Emojis.BT_TROOP + " in any territory or homeworld.", turnSummary.getMessages().getLast());
//            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(6, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-stronghold", btChat.getChoices().getLast().getFirst().getId());
            assertEquals("Stronghold", btChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("bt-ht-spice-blow", btChat.getChoices().getLast().get(1).getId());
            assertEquals("Spice Blow Territories", btChat.getChoices().getLast().get(1).getLabel());
            assertEquals("bt-ht-rock", btChat.getChoices().getLast().get(2).getId());
            assertEquals("Rock Territories", btChat.getChoices().getLast().get(2).getLabel());
            assertEquals("bt-ht-homeworlds", btChat.getChoices().getLast().get(3).getId());
            assertEquals("Homeworlds", btChat.getChoices().getLast().get(3).getLabel());
            assertEquals("bt-ht-other", btChat.getChoices().getLast().get(4).getId());
            assertEquals("Somewhere else", btChat.getChoices().getLast().get(4).getLabel());
            assertEquals("bt-ht-pass", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Leave them on Tleilax", btChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.BT_HT, movement.getMoveType());
            assertEquals("Tleilax", movement.getMovingFrom());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(2, movement.getForce());
        }

        @Test
        void testStrongholdChoices() throws InvalidGameStateException {
            movement.presentStrongholdChoices();
            assertEquals("Which Stronghold?", btChat.getMessages().getLast());
            assertEquals(7, btChat.getChoices().getLast().size());
            assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals("Hidden Mobile Stronghold")));
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : btChat.getChoices().getLast().subList(0, 6)) {
                assertTrue(c.getId().startsWith("bt-ht-"));
                assertFalse(c.isDisabled(), () -> c.getLabel() + " button was disabled.");
                String action = c.getId().replace("bt-ht-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(strongholdsWithHMS.contains(c.getLabel()));
            }
            for (String s : strongholdsWithHMS)
                assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testSpiceBlowChoices() {
            movement.presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", btChat.getMessages().getLast());
            assertEquals(16, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : btChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("bt-ht-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("bt-ht-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testRockChoices() {
            movement.presentRockChoices();
            assertEquals("Which Rock Territory?", btChat.getMessages().getLast());
            assertEquals(8, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : btChat.getChoices().getLast().subList(0, 7)) {
                assertTrue(c.getId().startsWith("bt-ht-"));
                String action = c.getId().replace("bt-ht-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(rockTerritories.contains(c.getLabel()));
            }
            for (String s : rockTerritories)
                assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testDiscoveryTokenChoices() throws InvalidGameStateException {
            movement.presentDiscoveryTokenChoices();
            assertEquals("Which Discovery Token?", btChat.getMessages().getLast());
            assertEquals(3, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : btChat.getChoices().getLast().subList(0, 2)) {
                assertTrue(c.getId().startsWith("bt-ht-"));
                String action = c.getId().replace("bt-ht-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(discoveryTokenTerritories.contains(c.getLabel()));
            }
            for (String s : discoveryTokenTerritories)
                assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testNonSpiceNonRockChoices() {
            movement.presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", btChat.getMessages().getLast());
            assertEquals(16, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : btChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("bt-ht-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("bt-ht-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(btChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_OneSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Carthag");
            assertTrue(stateChanged);
            assertEquals("Carthag", movement.getMovingTo());
            assertEquals(2, movement.getForce());
            assertEquals("Sending **2 " + Emojis.BT_TROOP + "** free revivals to Carthag", btChat.getMessages().getLast());
            assertEquals(3, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-execute", btChat.getChoices().getLast().getFirst().getId());
            assertEquals("Confirm placement", btChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().get(1).getId());
            assertEquals("Start over", btChat.getChoices().getLast().get(1).getLabel());
            assertEquals("bt-ht-pass", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Leave them on Tleilax", btChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_MultiSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Cielago South");
            assertFalse(stateChanged);
            assertEquals("Which sector of Cielago South?", btChat.getMessages().getLast());
            assertEquals(3, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-sector-Cielago South (West Sector)", btChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector", btChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("bt-ht-sector-Cielago South (East Sector)", btChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", btChat.getChoices().getLast().get(1).getLabel());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testSectorChoices() {
            game.getTerritory("Cielago South (West Sector)").setSpice(12);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago South");
            movement.presentSectorChoices("Cielago South", sectors);
            assertEquals("Which sector of Cielago South?", btChat.getMessages().getLast());
            assertEquals(3, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-sector-Cielago South (West Sector)", btChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector (12 spice)", btChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("bt-ht-sector-Cielago South (East Sector)", btChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", btChat.getChoices().getLast().get(1).getLabel());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", btChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testProcessSector() {
            movement.processSector("Cielago South (West Sector)");
            assertEquals("Cielago South (West Sector)", movement.getMovingTo());
            assertEquals(2, movement.getForce());
            assertEquals("Sending **2 " + Emojis.BT_TROOP + "** free revivals to Cielago South (West Sector)", btChat.getMessages().getLast());
            assertEquals(3, btChat.getChoices().getLast().size());
            assertEquals("bt-ht-execute", btChat.getChoices().getLast().getFirst().getId());
            assertEquals("Confirm placement", btChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("bt-ht-start-over", btChat.getChoices().getLast().get(1).getId());
            assertEquals("Start over", btChat.getChoices().getLast().get(1).getLabel());
            assertEquals("bt-ht-pass", btChat.getChoices().getLast().getLast().getId());
            assertEquals("Leave them on Tleilax", btChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.BT_HT, movement.getMoveType());
        }

        @Test
        void testExecute() throws InvalidGameStateException {
            movement.setMovingTo("Carthag");
            movement.setForce(2);
            boolean advanceGame = movement.execute();
            assertFalse(advanceGame);
            assertFalse(bt.isBtHTActive());
            assertEquals(2, carthag.getForceStrength("BT"));
            assertEquals(18, game.getTerritory("Tleilax").getForceStrength("BT"));
            assertEquals("Placement of 2 free revivals complete.", btChat.getMessages().getLast());
            assertEquals(Emojis.BT + ": 2 " + Emojis.BT_TROOP + " placed on Carthag", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }

    @Nested
    @DisplayName("#hmsPlacement")
    class HMSPlacement {
        Movement movement;

        @BeforeEach
        void setUp() {
            movement = ix.getMovement();
            ix.presentHMSPlacementChoices();
            ixChat.clear();
        }

        @Test
        void testPass() {
            assertThrows(InvalidGameStateException.class, () -> movement.pass());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testStartOver() {
            movement.setMovingTo("Polar Sink");
            movement.startOver();
            assertEquals("Where would you like to place the HMS? ix", ixChat.getMessages().getLast());
            assertEquals(3, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-spice-blow", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("Spice Blow Territories", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ix-hms-placement-rock", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("Rock Territories", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ix-hms-placement-other", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Somewhere else", ixChat.getChoices().getLast().getLast().getLabel());
            assertTrue(turnSummary.getMessages().isEmpty());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
            assertTrue(movement.getMovingFrom().isEmpty());
            assertTrue(movement.getMovingTo().isEmpty());
            assertEquals(0, movement.getForce());
        }

        @Test
        void testStrongholdChoices() {
            assertThrows(InvalidGameStateException.class, () -> movement.presentStrongholdChoices());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testSpiceBlowChoices() {
            movement.presentSpiceBlowChoices();
            assertEquals("Which Spice Blow Territory?", ixChat.getMessages().getLast());
            assertEquals(16, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ixChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ix-hms-placement-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ix-hms-placement-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(spiceBlowTerritories.contains(c.getLabel()));
            }
            for (String s : spiceBlowTerritories)
                assertTrue(ixChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testRockChoices() {
            movement.presentRockChoices();
            assertEquals("Which Rock Territory?", ixChat.getMessages().getLast());
            assertEquals(8, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ixChat.getChoices().getLast().subList(0, 7)) {
                assertTrue(c.getId().startsWith("ix-hms-placement-"));
                String action = c.getId().replace("ix-hms-placement-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(rockTerritories.contains(c.getLabel()));
            }
            for (String s : rockTerritories)
                assertTrue(ixChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testDiscoveryTokenChoices() {
            assertThrows(InvalidGameStateException.class, () ->movement.presentDiscoveryTokenChoices());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testNonSpiceNonRockChoices() {
            movement.presentNonSpiceNonRockChoices();
            assertEquals("Which Territory?", ixChat.getMessages().getLast());
            assertEquals(16, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            for (DuneChoice c : ixChat.getChoices().getLast().subList(0, 15)) {
                assertTrue(c.getId().startsWith("ix-hms-placement-"));
                assertFalse(c.isDisabled());
                String action = c.getId().replace("ix-hms-placement-", "").replace(c.getLabel(), "");
                assertEquals("territory-", action);
                assertTrue(nonSpiceNonRockTerritories.contains(c.getLabel()));
            }
            for (String s : nonSpiceNonRockTerritories)
                assertTrue(ixChat.getChoices().getLast().stream().anyMatch(c -> c.getLabel().equals(s)));
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_OneSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Funeral Plain");
            assertTrue(stateChanged);
            assertEquals("Funeral Plain", movement.getMovingTo());
            assertEquals(0, movement.getForce());
            assertEquals("Placing the HMS in Funeral Plain", ixChat.getMessages().getLast());
            assertEquals(2, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-execute", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("Confirm placement", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("Start over", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testProcessTerritory_MultiSectorTerritory() {
            boolean stateChanged = movement.processTerritory("Cielago South");
            assertFalse(stateChanged);
            assertEquals("Which sector of Cielago South?", ixChat.getMessages().getLast());
            assertEquals(3, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-sector-Cielago South (West Sector)", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ix-hms-placement-sector-Cielago South (East Sector)", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testSectorChoices() {
            game.getTerritory("Cielago South (West Sector)").setSpice(12);
            List<Territory> sectors = game.getTerritories().getTerritorySectorsInStormOrder("Cielago South");
            movement.presentSectorChoices("Cielago South", sectors);
            assertEquals("Which sector of Cielago South?", ixChat.getMessages().getLast());
            assertEquals(3, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-sector-Cielago South (West Sector)", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("1 - West Sector (12 spice)", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ix-hms-placement-sector-Cielago South (East Sector)", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("2 - East Sector", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().getLast().getId());
            assertEquals("Start over", ixChat.getChoices().getLast().getLast().getLabel());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testProcessSector() {
            movement.processSector("Cielago South (West Sector)");
            assertEquals("Cielago South (West Sector)", movement.getMovingTo());
            assertEquals(0, movement.getForce());
            assertEquals("Placing the HMS in Cielago South (West Sector)", ixChat.getMessages().getLast());
            assertEquals(2, ixChat.getChoices().getLast().size());
            assertEquals("ix-hms-placement-execute", ixChat.getChoices().getLast().getFirst().getId());
            assertEquals("Confirm placement", ixChat.getChoices().getLast().getFirst().getLabel());
            assertEquals("ix-hms-placement-start-over", ixChat.getChoices().getLast().get(1).getId());
            assertEquals("Start over", ixChat.getChoices().getLast().get(1).getLabel());
            assertEquals(MoveType.HMS_PLACEMENT, movement.getMoveType());
        }

        @Test
        void testExecute() throws InvalidGameStateException {
            movement.setMovingTo("Funeral Plain");
            boolean advanceGame = movement.execute();
            assertTrue(advanceGame);
            assertEquals(funeralPlain, ix.getTerritoryWithHMS());
            assertEquals("You placed the HMS in Funeral Plain.", ixChat.getMessages().getLast());
            assertEquals(Emojis.IX + " placed the HMS in Funeral Plain.", turnSummary.getMessages().getLast());
            assertEquals(MoveType.TBD, movement.getMoveType());
        }
    }
}
