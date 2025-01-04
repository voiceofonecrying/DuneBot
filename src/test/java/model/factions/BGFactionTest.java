package model.factions;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BGFactionTest extends FactionTestTemplate {

    private BGFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new BGFaction("player", "player");
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
        homeworld.removeForces(faction.getName(), forcesToRemove);
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
        assertEquals(faction.getEmoji(), Emojis.BG);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 4);
    }

    @Test
    public void testInitialPredictionFactionName() {
        assertNull(faction.getPredictionFactionName());
    }

    @Test
    public void testInitialPredictionRound() {
        assertEquals(faction.getPredictionRound(), 0);
    }

    @Test
    public void testSetPredictionRound() {
        faction.setPredictionRound(1);
        assertEquals(faction.getPredictionRound(), 1);
    }

    @Test
    public void testSetPredictionRoundInvalid() {
        assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(0));
        assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(11));
    }

    @Test
    public void testSetPredictionFactionName() {
        faction.setPredictionFactionName("Atreides");
        assertEquals(faction.getPredictionFactionName(), "Atreides");
    }

    @Test
    public void testNoFlipMessageInPolarSink() throws IOException {
        Territory polarSink = game.getTerritory("Polar Sink");
        AtreidesFaction atreides = new AtreidesFaction("at", "at");
        game.addFaction(atreides);
        faction.placeForceFromReserves(game, polarSink, 1, false);
        game.moveForces(atreides, game.getTerritory("Arrakeen"), polarSink, 1, 0, false);
        assertTrue(turnSummary.getMessages().stream().noneMatch(m -> m.contains(Emojis.BG + " to decide whether they want to flip")));
        assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    public void testFlipForces() {
        Territory carthag = game.getTerritory("Carthag");
        carthag.addForces("BG", 1);
        assertEquals(1, carthag.getForceStrength("BG"));
        assertEquals(0, carthag.getForceStrength("Advisor"));
        faction.flipForces(carthag);
        assertEquals(0, carthag.getForceStrength("BG"));
        assertEquals(1, carthag.getForceStrength("Advisor"));
        faction.flipForces(carthag);
        assertEquals(1, carthag.getForceStrength("BG"));
        assertEquals(0, carthag.getForceStrength("Advisor"));
    }

    @Test
    public void testHasIntrudedTerritoriesDecisions() {
        Territory sietchTabr = game.getTerritory("Sietch Tabr");
        sietchTabr.addForces("BG", 1);
        faction.bgFlipMessageAndButtons(game, "Sietch Tabr");
        Territory habbanyaSietch = game.getTerritory("Habbanya Sietch");
        habbanyaSietch.addForces("BG", 1);
        faction.bgFlipMessageAndButtons(game, "Habbanya Sietch");
        assertTrue(faction.hasIntrudedTerritoriesDecisions());
        assertTrue(faction.getIntrudeTerritoriesString().equals("Sietch Tabr, Habbanya Sietch") || faction.getIntrudeTerritoriesString().equals("Habbanya Sietch, Sietch Tabr"));
        faction.flipForces(sietchTabr);
        assertTrue(faction.hasIntrudedTerritoriesDecisions());
        assertEquals("Habbanya Sietch", faction.getIntrudeTerritoriesString());
        faction.dontFlipFighters(game, habbanyaSietch.getTerritoryName());
        assertFalse(faction.hasIntrudedTerritoriesDecisions());
        assertTrue(faction.getIntrudeTerritoriesString().isEmpty());
    }

    @Nested
    @DisplayName("#homeworld")
    class Homeworld extends FactionTestTemplate.Homeworld {
        @Test
        @Override
        public void testHomweworldDialAdvantageHighThreshold() {
            assertEquals(0, faction.homeworldDialAdvantage(game, territory));
            game.addGameOption(GameOption.HOMEWORLDS);
            assertEquals(3, faction.homeworldDialAdvantage(game, territory));
        }
    }

    @Nested
    @DisplayName("#advise")
    class Advise {
        Territory carthag;
        Territory arrakeen;
        Territory polarSink;

        @BeforeEach
        public void setUp() {
            carthag = game.getTerritory("Carthag");
            arrakeen = game.getTerritory("Arrakeen");
            polarSink = game.getTerritory("Polar Sink");
        }

        @Test
        public void testAdvisorCannotBeSentToTerritoryWithBGFighter() {
            carthag.addForces("BG", 1);
            assertThrows(InvalidGameStateException.class, () -> faction.advise(game, carthag, 1));
        }

        @Test
        public void testAdvisorSentToTerritoryRemainsAdvisor() {
            carthag.addForces("Advisor", 1);
            assertDoesNotThrow(() -> faction.advise(game, carthag, 1));
            assertEquals(2, carthag.getForceStrength("Advisor"));
            assertEquals(0, carthag.getForceStrength("BG"));
        }

        @Test
        public void testAdvisorSentToPolarSinkBecomesFighter() {
            polarSink.addForces("BG", 1);
            assertDoesNotThrow(() -> faction.advise(game, polarSink, 1));
            assertEquals(2, polarSink.getForceStrength("BG"));
            assertEquals(0, polarSink.getForceStrength("Advisor"));
        }

        @Test
        public void testAdvisorCanTriggerTerrorToken() throws IOException, InvalidGameStateException {
            Faction moritani = new MoritaniFaction("p", "u");
            moritani.setChat(new TestTopic());
            game.addFaction(moritani);
            Territory carthag = game.getTerritory("Carthag");
            carthag.addTerrorToken("Robbery");
            faction.advise(game, carthag, 1);
            assertEquals(Emojis.MORITANI + " has an opportunity to trigger their Terror Token against " + Emojis.BG, turnSummary.getMessages().getLast());
        }

        @Nested
        @DisplayName("#allyCoexistence")
        class AllyCoexistence {
            EmperorFaction emperor;

            @BeforeEach
            public void setUp() throws IOException {
                emperor = new EmperorFaction("p", "u");
                emperor.setLedger(new TestTopic());
                game.addFaction(emperor);
                game.createAlliance(faction, emperor);
                carthag.addForces("Emperor", 1);
            }

            @Test
            public void testAdviseButtonDisabledWithAllyInTerritoryWithGF9Rules() {
                faction.presentAdvisorChoices(game, emperor, carthag);
                DuneChoice adviseChoice = chat.getChoices().getFirst().getFirst();
                assertEquals("Advise", adviseChoice.getLabel());
                assertTrue(adviseChoice.isDisabled());
            }

            @Test
            public void testAdviseButtonEnabledWithAllyNotInTerritory() {
                faction.presentAdvisorChoices(game, emperor, arrakeen);
                DuneChoice adviseChoice = chat.getChoices().getFirst().getFirst();
                assertEquals("Advise", adviseChoice.getLabel());
                assertFalse(adviseChoice.isDisabled());
            }

            @Test
            public void testAdviseButtonEnabledWithAllyInTerritoryWithAllyCoexistence() {
                game.addGameOption(GameOption.BG_COEXIST_WITH_ALLY);
                faction.presentAdvisorChoices(game, emperor, carthag);
                DuneChoice adviseChoice = chat.getChoices().getFirst().getFirst();
                assertEquals("Advise", adviseChoice.getLabel());
                assertFalse(adviseChoice.isDisabled());
            }

            @Test
            public void testBGCannotAdviseAllyTerritoryWithGF9Rules() {
                assertThrows(InvalidGameStateException.class, () -> faction.advise(game, carthag, 1));
            }

            @Test
            public void testBGCanAdviseAllyTerrititoryWithAllyCoexistence() {
                game.addGameOption(GameOption.BG_COEXIST_WITH_ALLY);
                assertDoesNotThrow(() -> faction.advise(game, carthag, 1));
            }
        }
    }
}