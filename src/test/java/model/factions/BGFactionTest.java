package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BGFactionTest extends FactionTestTemplate {

    private BGFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new BGFaction("player", "user");
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

    @Nested
    @DisplayName("#placeChosenStartingForces")
    class placeChosenStartingForces {
        Shipment shipment;
        Territory carthag;

        @BeforeEach
        public void setUp() {
            shipment = faction.getShipment();
            shipment.clear();
            shipment.setForce(1);
            shipment.setTerritoryName("Carthag");
            carthag = game.getTerritory("Carthag");
        }

        @Test
        public void testAdvisorToEmptyTerritory() {
            faction.placeChosenStartingForces();
            assertEquals(1, carthag.getForceStrength("BG"));
            assertEquals(0, carthag.getForceStrength("Advisor"));
            assertEquals("Initial force placement complete.", chat.getMessages().getLast());
        }

        @Test
        public void testAdvisorToPopulatedTerritory() throws IOException {
            game.addFaction(new HarkonnenFaction("p", "u"));
            faction.placeChosenStartingForces();
            assertEquals(0, carthag.getForceStrength("BG"));
            assertEquals(1, carthag.getForceStrength("Advisor"));
            assertEquals("Initial force placement complete.", chat.getMessages().getLast());
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

    @Nested
    @DisplayName("#predictedFaction")
    class PredictedFaction {
        @Test
        public void testInitialPredictionFactionName() {
            assertNull(faction.getPredictionFactionName());
        }

        @Test
        public void testPresentPredictedFactionChoices() throws IOException {
            game.addFaction(new AtreidesFaction("aPlayer", "aName"));
            game.addFaction(new EmperorFaction("ePlayer", "eName"));
            faction.presentPredictedFactionChoices();
            assertEquals("Which faction do you predict to win? player\n" + Emojis.ATREIDES + " - aPlayer\n" + Emojis.EMPEROR + " - ePlayer", chat.getMessages().getFirst());
            assertEquals(Emojis.ATREIDES, chat.getChoices().getFirst().getFirst().getEmoji());
            assertNull(chat.getChoices().getFirst().getFirst().getLabel());
            assertEquals("bg-prediction-faction-Atreides", chat.getChoices().getFirst().getFirst().getId());
            assertEquals(Emojis.EMPEROR, chat.getChoices().getFirst().getLast().getEmoji());
            assertNull(chat.getChoices().getFirst().getLast().getLabel());
            assertEquals("bg-prediction-faction-Emperor", chat.getChoices().getFirst().getLast().getId());
        }

        @Test
        public void testSetPredictionFactionName() {
            faction.setPredictionFactionName("Atreides");
            assertEquals(faction.getPredictionFactionName(), "Atreides");
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
        }
    }

    @Nested
    @DisplayName("#predictedTurn")
    class PredictedTurn {
        @Test
        public void testInitialPredictionRound() {
            assertEquals(faction.getPredictionRound(), 0);
        }

        @Test
        public void testPresentPredictedTurnChoices() {
            faction.setPredictionFactionName("Atreides");
            faction.presentPredictedTurnChoices("Atreides");
            assertEquals(10, chat.getChoices().getFirst().size());
            IntStream.rangeClosed(1, 10).forEach(i -> assertEquals("bg-prediction-turn-" + i, chat.getChoices().getFirst().get(i - 1).getId()));
            IntStream.rangeClosed(1, 10).forEach(i -> assertEquals("" + i, chat.getChoices().getFirst().get(i - 1).getLabel()));
        }

        @Test
        public void testFactionMustBeSetBeforeTurn() {
            assertNull(faction.getPredictionFactionName());
            assertThrows(InvalidGameStateException.class, () ->faction.setPredictionRound(1));
        }

        @Test
        public void testSetPredictionRound() throws InvalidGameStateException {
            faction.setPredictionFactionName("Atreides");
            faction.setPredictionRound(1);
            assertEquals(faction.getPredictionRound(), 1);
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MISC_BACK_OF_SHIELD));
            assertEquals("You predict " + Emojis.ATREIDES + " to win on turn 1.", chat.getMessages().getLast());
        }

        @Test
        public void testSetPredictionRoundInvalid() {
            faction.setPredictionFactionName("Atreides");
            assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(0));
            assertThrows(IllegalArgumentException.class, () -> faction.setPredictionRound(11));
        }
    }

    @Test
    public void testPresentStartingForcesChoices() {
        faction.presentStartingForcesChoices();
        assertEquals("Where would you like to place your starting " + Emojis.BG_ADVISOR + " or " + Emojis.BG_FIGHTER + "? player", chat.getMessages().getFirst());
        assertEquals(4, chat.getChoices().getFirst().size());
        assertEquals(Emojis.BG + " will place their starting " + Emojis.BG_ADVISOR + " or " + Emojis.BG_FIGHTER, gameActions.getMessages().getFirst());
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

    @Nested
    @DisplayName("#bgFlipMessageAndButtons")
    class BGFlipMessageAndButtons {
        Territory sietchTabr;
        Territory habbanyaSietch;

        @BeforeEach
        public void setUp() {
            sietchTabr = game.getTerritory("Sietch Tabr");
            sietchTabr.addForces("BG", 1);
            faction.presentFlipMessage(game, "Sietch Tabr");
            habbanyaSietch = game.getTerritory("Habbanya Sietch");
            habbanyaSietch.addForces("BG", 1);
            faction.presentFlipMessage(game, "Habbanya Sietch");
        }

        @Test
        public void testFlipDecisionReportedToTurnSummary() {
            assertEquals(Emojis.BG + " to decide if they want to flip to " + Emojis.BG_ADVISOR + " in Sietch Tabr.", turnSummary.getMessages().getFirst());
            assertEquals(Emojis.BG + " to decide if they want to flip to " + Emojis.BG_ADVISOR + " in Habbanya Sietch.", turnSummary.getMessages().getLast());
        }

        @Test
        public void testFlipChoicesPresentedToBGChat() {
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in Sietch Tabr? player", chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().getFirst().size());
            assertEquals("Will you flip to " + Emojis.BG_ADVISOR + " in Habbanya Sietch? player", chat.getMessages().getLast());
            assertEquals(2, chat.getChoices().getLast().size());
        }

        @Test
        public void testHasIntrudedTerritoriesDecisions() {
            assertTrue(faction.hasIntrudedTerritoriesDecisions());
            assertTrue(faction.getIntrudedTerritoriesString().equals("Sietch Tabr, Habbanya Sietch") || faction.getIntrudedTerritoriesString().equals("Habbanya Sietch, Sietch Tabr"));
        }

        @Test
        public void testFlipDecisionsClearIntrudedTerritories() {
            faction.flipForces(sietchTabr);
            assertTrue(faction.hasIntrudedTerritoriesDecisions());
            assertEquals("Habbanya Sietch", faction.getIntrudedTerritoriesString());
            faction.dontFlipFighters(game, habbanyaSietch.getTerritoryName());
            assertFalse(faction.hasIntrudedTerritoriesDecisions());
            assertTrue(faction.getIntrudedTerritoriesString().isEmpty());
        }
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
        public void testResponseMessage() throws InvalidGameStateException {
            faction.advise(game, carthag, 1);
            assertEquals("You sent 1 " + Emojis.BG_ADVISOR + " to Carthag.", chat.getMessages().getLast());
            assertTrue(faction.getUpdateTypes().contains(UpdateType.MAP));
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
            carthag.addTerrorToken(game, "Robbery");
            faction.advise(game, carthag, 1);
            assertEquals(Emojis.MORITANI + " has an opportunity to trigger their Terror Token in Carthag against " + Emojis.BG, turnSummary.getMessages().getLast());
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
                assertEquals("Would you like to advise the shipment to Carthag? player", chat.getMessages().getLast());
                DuneChoice adviseChoice = chat.getChoices().getFirst().getFirst();
                assertEquals("Advise", adviseChoice.getLabel());
                assertTrue(adviseChoice.isDisabled());
            }

            @Test
            public void testAdviseButtonEnabledWithAllyNotInTerritory() {
                faction.presentAdvisorChoices(game, emperor, arrakeen);
                assertEquals("Would you like to advise the shipment to Arrakeen? player", chat.getMessages().getLast());
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
            public void testAdviseButtonEnabledWithEcazAllyInTerritoryWithGF9Rules() throws IOException {
                carthag.removeForces(game, "Emperor", 1);
                EcazFaction ecaz = new EcazFaction("p", "u");
                ecaz.setLedger(new TestTopic());
                game.addFaction(ecaz);
                game.createAlliance(faction, ecaz);
                carthag.addForces("Ecaz", 1);
                faction.presentAdvisorChoices(game, ecaz, carthag);
                DuneChoice adviseChoice = chat.getChoices().getFirst().getFirst();
                assertEquals("Advise", adviseChoice.getLabel());
                assertFalse(adviseChoice.isDisabled());
            }

            @Test
            public void testBGCanAdviseEcazAllyTerritoryWithGF9Rules() throws IOException {
                carthag.removeForces(game, "Emperor", 1);
                EcazFaction ecaz = new EcazFaction("p", "u");
                ecaz.setLedger(new TestTopic());
                game.addFaction(ecaz);
                game.createAlliance(faction, ecaz);
                carthag.addForces("Ecaz", 1);
                assertDoesNotThrow(() -> faction.advise(game, carthag, 1));
            }

            @Test
            public void testBGCanAdviseAllyTerrititoryWithAllyCoexistence() {
                game.addGameOption(GameOption.BG_COEXIST_WITH_ALLY);
                assertDoesNotThrow(() -> faction.advise(game, carthag, 1));
            }
        }
    }

    @Nested
    @DisplayName("#placeForces")
    class PlaceForces extends FactionTestTemplate.PlaceForces {
        Territory arrakeen;

        @BeforeEach
        void setUp() throws IOException {
            super.setUp();
            arrakeen = game.getTerritory("Arrakeen");
        }

        @Test
        void testPlaceInTerritoryWithAdvisors() throws InvalidGameStateException {
            arrakeen.addForces("Advisor", 1);
            faction.placeForces(arrakeen, 2, 0, true, true, true, game, false, false);
            assertEquals(Emojis.BG + ": 2 " + Emojis.BG_ADVISOR + " placed on Arrakeen for 2 " + Emojis.SPICE, turnSummary.getMessages().getLast());
            assertEquals(3, arrakeen.getForceStrength("Advisor"));
            assertEquals(0, arrakeen.getForceStrength("BG"));
        }

        @Test
        @Override
        void testBGGetFlipMessage() {
        }

        @Test
        @Override
        void testBGGetAdviseMessage() {
        }
    }

    @Nested
    @DisplayName("executeShipment")
    class ExecuteShipment extends FactionTestTemplate.ExecuteShipment {
        Territory sietchTabr;

        @BeforeEach
        void setUp() {
            super.setUp();
            sietchTabr = game.getTerritory("Sietch Tabr");
            sietchTabr.addForces("Advisor", 2);
        }

        @Test
        void testShipToTerritoryWithAdvisors() throws InvalidGameStateException {
            faction.executeShipment(game, false, false);
            assertEquals(Emojis.BG + ": 1 " + Emojis.BG_ADVISOR + " placed on Sietch Tabr for 1 " + Emojis.SPICE, turnSummary.getMessages().getLast());
            assertEquals(3, sietchTabr.getForceStrength("Advisor"));
            assertEquals(0, sietchTabr.getForceStrength("BG"));
        }
    }

    @Nested
    @DisplayName("#executeMovement")
    class ExecuteMovement extends FactionTestTemplate.ExecuteMovement {
        @Test
        @Override
        void testBGGetFlipMessage() {
        }
    }
}