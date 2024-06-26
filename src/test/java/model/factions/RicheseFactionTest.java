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
        faction = new RicheseFaction("player", "player", game);
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 5);
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
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        assertEquals(faction.getEmoji(), Emojis.RICHESE);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
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
    @DisplayName("#revealNoField")
    class RevealNoField {
        Territory sietchTabr;
        ChoamFaction choam;
        EmperorFaction emperor;
        TestTopic turnSummary;

        @BeforeEach
        public void setUp() throws IOException {
            sietchTabr = game.getTerritory("Sietch Tabr");
            faction.setLedger(new TestTopic());
            choam = new ChoamFaction("p", "u", game);
            choam.setLedger(new TestTopic());
            game.addFaction(choam);
            emperor = new EmperorFaction("p", "u", game);
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
            faction.revealNoField(game);
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
            faction.revealNoField(game);
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
}