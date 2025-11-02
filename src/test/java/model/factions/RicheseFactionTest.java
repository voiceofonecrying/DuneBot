package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RicheseFactionTest extends FactionTestTemplate {

    private RicheseFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new RicheseFaction("player", "player");
        commonPostInstantiationSetUp();
    }

    @Test
    public void testInitialSpice() {
        assertEquals(5, faction.getSpice());
    }

    @Test
    public void testFreeRevival() {
        assertEquals(2, faction.getFreeRevival());
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
        assertEquals(3, faction.getFreeRevival());
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
        assertEquals(4, faction.getFreeRevival());
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
        assertEquals(6, faction.getFreeRevival());
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

    @Test
    public void testInitialHasMiningEquipment() {
        assertFalse(faction.hasMiningEquipment());
    }

    @Test
    public void testInitialReserves() {
        assertEquals(20, faction.getReservesStrength());
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
        assertEquals(Emojis.RICHESE, faction.getEmoji());
    }

    @Test
    public void testForceEmoji() {
        assertEquals(Emojis.RICHESE_TROOP, faction.getForceEmoji());
    }

    @Test
    public void testHandLimit() {
        assertEquals(4, faction.getHandLimit());
    }

    @Test
    public void testSpiceCollectedFromTerritory() {
        Territory theGreatFlat = game.getTerritory("The Great Flat");
        theGreatFlat.addForces(faction.getName(), 2);
        theGreatFlat.setRicheseNoField(5);
        theGreatFlat.setSpice(10);
        assertEquals(6, faction.getSpiceCollectedFromTerritory(theGreatFlat));
        theGreatFlat.setSpice(3);
        assertEquals(3, faction.getSpiceCollectedFromTerritory(theGreatFlat));
    }

    @Test
    public void testNoFieldNearShieldWall() {
        Territory sihayaRidge = game.getTerritory("Sihaya Ridge");
        sihayaRidge.setRicheseNoField(0);
        assertTrue(faction.isNearShieldWall());
    }

    @RepeatedTest(20)
    public void setFrontOfShieldNoFieldValidAndInvalid(RepetitionInfo repetitionInfo) {
        int frontOfShieldNoField = repetitionInfo.getCurrentRepetition() - 1;
        if (List.of(0, 3, 5).contains(frontOfShieldNoField)) {
            faction.setFrontOfShieldNoField(frontOfShieldNoField);
            assertEquals(faction.getFrontOfShieldNoField(), frontOfShieldNoField);
        } else {
            assertThrows(IllegalArgumentException.class, () -> faction.setFrontOfShieldNoField(frontOfShieldNoField));
        }
    }

    @Nested
    @DisplayName("#executeShipment")
    class ExecuteShipment extends FactionTestTemplate.ExecuteShipment {
        @Test
        void testShipNoFieldWithForces() {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(faction.isHighThreshold());
            shipment.setNoField(3);
            assertDoesNotThrow(() -> faction.executeShipment(game, false, true));
        }

        @Test
        void testCannotShipNoFieldWithForces() {
            shipment.setNoField(3);
            assertThrows(InvalidGameStateException.class, () -> faction.executeShipment(game, false, true));
        }
    }

    @Nested
    @DisplayName("#shipNoField")
    class ShipNoField {
        BGFaction bg;
        ChoamFaction choam;
        TestTopic bgChat;
        Territory sietchTabr;

        @BeforeEach
        public void setUp() throws IOException {
            sietchTabr = game.getTerritory("Sietch Tabr");
            bg = new BGFaction("p", "u");
            bgChat = new TestTopic();
            bg.setChat(bgChat);
            bg.setLedger(new TestTopic());
            game.addFaction(bg);
            choam = new ChoamFaction("p", "u");
            choam.setLedger(new TestTopic());
            game.addFaction(choam);
//            emperor = new EmperorFaction("p", "u", game);
//            emperor.setLedger(new TestTopic());
//            game.addFaction(emperor);
//            turnSummary = new TestTopic();
//            game.setTurnSummary(turnSummary);
        }

        @Test
        public void testForcesWithNoFieldMustBeHomeworldGame() {
            assertThrows(InvalidGameStateException.class, () -> faction.shipNoField(faction, sietchTabr, 3, false, false, 1));
        }

        @Test
        public void testRicheseMustBeHighThresholdToShipForcesWithNoField() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.removeReserves(11);
            assertFalse(faction.isHighThreshold());
            assertThrows(InvalidGameStateException.class, () -> faction.shipNoField(faction, sietchTabr, 3, false, false, 1));
        }

        @Test
        public void testRicheseAllyCannotShipForcesWithNoField() {
            game.addGameOption(GameOption.HOMEWORLDS);
            faction.setAlly(choam.getName());
            choam.setAlly(faction.getName());
            assertThrows(InvalidGameStateException.class, () -> faction.shipNoField(choam, sietchTabr, 3, false, false, 1));
        }

        @Test
        public void testRicheseHighThresholdCanShipForcesWithNoField() throws InvalidGameStateException {
            game.addGameOption(GameOption.HOMEWORLDS);
            assertTrue(faction.isHighThreshold());
            faction.shipNoField(faction, sietchTabr, 3, false, false, 1);
            assertEquals(3, sietchTabr.getRicheseNoField());
            assertEquals(1, sietchTabr.getForceStrength("Richese"));
        }

        @Test
        void testBGGetFlipMessage() throws InvalidGameStateException {
            bg.placeForcesFromReserves(sietchTabr, 1, false);
            faction.shipNoField(faction, sietchTabr, 3, false, false, 0);
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in Sietch Tabr? p", bgChat.getMessages().getFirst());
        }

        @Test
        void testBGGetAdviseMessage() throws InvalidGameStateException {
            faction.shipNoField(faction, sietchTabr, 3, false, false, 0);
            assertEquals("Would you like to advise the shipment to Sietch Tabr? p", bgChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#moveNoField")
    class MoveNoField {
        Territory ixHomeworld;
        Territory sietchTabr;

        @BeforeEach
        void setUp() throws IOException {
            game.addGameOption(GameOption.HOMEWORLDS);
            game.addFaction(new IxFaction("ix", "ix"));
            ixHomeworld = game.getTerritory("Ix");
            sietchTabr = game.getTerritory("Sietch Tabr");
            ixHomeworld.setRicheseNoField(0);
        }

        @Test
        void testNoFieldGoesToNewTerritory() throws InvalidGameStateException {
            assertNull(sietchTabr.getRicheseNoField());
            faction.moveNoField("Sietch Tabr", true);
            assertNull(ixHomeworld.getRicheseNoField());
            assertEquals(0, sietchTabr.getRicheseNoField());
            assertEquals(Emojis.RICHESE + " move their " + Emojis.NO_FIELD + " to Sietch Tabr.", turnSummary.getMessages().getFirst());
        }

        @Test
        void testBGCanFlipToAdvisors() throws IOException, InvalidGameStateException {
            BGFaction bg = new BGFaction("bg", "bg");
            TestTopic bgChat = new TestTopic();
            bg.setChat(bgChat);
            bg.setLedger(new TestTopic());
            game.addFaction(bg);
            bg.placeForcesFromReserves(sietchTabr, 1, false);
            faction.moveNoField("Sietch Tabr", true);
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in Sietch Tabr? bg", bgChat.getMessages().getLast());
        }

        @Test
        void testNoFieldTriggersTerrorToken() throws IOException, InvalidGameStateException {
            MoritaniFaction moritani = new MoritaniFaction("mo", "mo");
            TestTopic moritaniChat = new TestTopic();
            moritani.setChat(moritaniChat);
            game.addFaction(moritani);
            sietchTabr.addTerrorToken(game, "Robbery");
            faction.moveNoField("Sietch Tabr", true);
            assertEquals("Will you trigger your Robbery Terror Token in Sietch Tabr against " + Emojis.RICHESE + "? mo", moritaniChat.getMessages().getLast());
        }

        @Test
        void testNoFieldTriggersAmbassador() throws IOException, InvalidGameStateException {
            EcazFaction ecaz = new EcazFaction("ec", "ec");
            TestTopic ecazChat = new TestTopic();
            ecaz.setChat(ecazChat);
            game.addFaction(ecaz);
            sietchTabr.setEcazAmbassador("Atreides");
            faction.moveNoField("Sietch Tabr", true);
            assertEquals("Will you trigger your Atreides Ambassador in Sietch Tabr against " + Emojis.RICHESE + "? ec", ecazChat.getMessages().getLast());
        }
    }

    @Nested
    @DisplayName("#revealNoField")
    class RevealNoField {
        Territory sietchTabr;
        ChoamFaction choam;
        EmperorFaction emperor;
        TestTopic turnSummary;

        @BeforeEach
        public void setUp() throws IOException {
            sietchTabr = game.getTerritory("Sietch Tabr");
            choam = new ChoamFaction("p", "u");
            choam.setLedger(new TestTopic());
            game.addFaction(choam);
            emperor = new EmperorFaction("p", "u");
            emperor.setLedger(new TestTopic());
            game.addFaction(emperor);
            turnSummary = new TestTopic();
            game.setTurnSummary(turnSummary);
        }

        @Test
        public void test3NoFieldWithRicheseForces() {
            sietchTabr.setRicheseNoField(3);
            assertEquals(20, faction.getReservesStrength());
            assertEquals(0, sietchTabr.getForceStrength("Richese"));
            faction.revealNoField();
            assertEquals(3, sietchTabr.getForceStrength("Richese"));
            assertEquals(17, faction.getReservesStrength());
            assertEquals("The 3 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 3 " + Emojis.RICHESE_TROOP,
                    turnSummary.getMessages().getLast());
            assertEquals(3, faction.getFrontOfShieldNoField());
        }

        @Test
        public void test5NoFieldRicheseHasOnly4Reserves() {
            sietchTabr.setRicheseNoField(5);
            faction.removeReserves(16);
            assertEquals(4, faction.getReservesStrength());
            assertEquals(0, sietchTabr.getForceStrength("Richese"));
            faction.revealNoField();
            assertEquals(4, sietchTabr.getForceStrength("Richese"));
            assertEquals(0, faction.getReservesStrength());
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 4 " + Emojis.RICHESE_TROOP,
                    turnSummary.getMessages().getLast());
            assertEquals(5, faction.getFrontOfShieldNoField());
        }

        @Test
        public void test3NoFieldWithCHOAMForces() {
            sietchTabr.setRicheseNoField(3);
            assertEquals(20, choam.getReservesStrength());
            assertEquals(0, sietchTabr.getForceStrength("CHOAM"));
            faction.revealNoField(game, choam);
            assertEquals(3, sietchTabr.getForceStrength("CHOAM"));
            assertEquals(17, choam.getReservesStrength());
            assertEquals("The 3 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 3 " + Emojis.CHOAM_TROOP,
                    turnSummary.getMessages().getLast());
            assertEquals(3, faction.getFrontOfShieldNoField());
        }

        @Test
        public void test5NoFieldEmperor3Regular2Sardaukar() {
            sietchTabr.setRicheseNoField(5);
            emperor.removeReserves(12);
            assertEquals(0, sietchTabr.getForceStrength("Emperor"));
            assertEquals(0, sietchTabr.getForceStrength("Emperor*"));
            faction.revealNoField(game, emperor);
            assertEquals(3, sietchTabr.getForceStrength("Emperor"));
            assertEquals(2, sietchTabr.getForceStrength("Emperor*"));
            assertEquals(0, emperor.getReservesStrength());
            assertEquals(3, emperor.getSpecialReservesStrength());
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 3 " + Emojis.EMPEROR_TROOP + " 2 " + Emojis.EMPEROR_SARDAUKAR
                            + "\n" + Emojis.EMPEROR + " may replace up to 3 " + Emojis.EMPEROR_TROOP + " with " + Emojis.EMPEROR_SARDAUKAR,
                    turnSummary.getMessages().getLast());
            assertEquals(5, faction.getFrontOfShieldNoField());
        }

        @Test
        public void test5NoFieldEmperor3Regular1Sardaukar() {
            sietchTabr.setRicheseNoField(5);
            emperor.removeReserves(12);
            emperor.removeSpecialReserves(4);
            assertEquals(0, sietchTabr.getForceStrength("Emperor"));
            assertEquals(0, sietchTabr.getForceStrength("Emperor*"));
            faction.revealNoField(game, emperor);
            assertEquals(3, sietchTabr.getForceStrength("Emperor"));
            assertEquals(1, sietchTabr.getForceStrength("Emperor*"));
            assertEquals(0, emperor.getReservesStrength());
            assertEquals(0, emperor.getSpecialReservesStrength());
            assertEquals("The 5 " + Emojis.NO_FIELD + " in Sietch Tabr reveals 3 " + Emojis.EMPEROR_TROOP + " 1 " + Emojis.EMPEROR_SARDAUKAR,
                    turnSummary.getMessages().getLast());
            assertEquals(5, faction.getFrontOfShieldNoField());
        }
    }

    @Nested
    @DisplayName("#executeMovement")
    class ExecuteMovement extends FactionTestTemplate.ExecuteMovement {
        @BeforeEach
        @Override
        void setUp() {
            super.setUp();
            theGreatFlat.setRicheseNoField(3);
            movement.setMovingNoField(true);
        }

        @Test
        @Override
        void testBGGetFlipMessage() throws IOException, InvalidGameStateException {
            super.testBGGetFlipMessage();
            assertEquals(1, bgChat.getMessages().size());
        }

        @Test
        @Override
        void testEcazGetAmbassadorMessage() throws InvalidGameStateException, IOException {
            super.testEcazGetAmbassadorMessage();
            assertEquals(1, ecazChat.getMessages().size());
        }

        @Test
        @Override
        void testMoritaniGetTerrorTokenMessage() throws InvalidGameStateException, IOException {
            super.testMoritaniGetTerrorTokenMessage();
            assertEquals(1, moritaniChat.getMessages().size());
        }

        @Test
        void testMoveNoFieldAndForces() throws InvalidGameStateException {
            game.getRicheseFaction().shipNoField(faction, theGreatFlat, 3, false, false, 0);
            faction.getMovement().setMovingNoField(true);
            turnSummary.clear();
            faction.executeMovement();
            assertEquals(3, funeralPlain.getRicheseNoField());
            assertNull(theGreatFlat.getRicheseNoField());
            assertEquals(Emojis.RICHESE + " move their " + Emojis.NO_FIELD + " to Funeral Plain.", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.RICHESE + ": 1 " + Emojis.RICHESE_TROOP + " moved from The Great Flat to Funeral Plain.", turnSummary.getMessages().getLast());
        }
    }
}