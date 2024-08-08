package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.HomeworldTerritory;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ChoamFactionTest extends FactionTestTemplate {

    private ChoamFaction faction;

    @Override
    Faction getFaction() {
        return faction;
    }

    @BeforeEach
    void setUp() throws IOException {
        faction = new ChoamFaction("player", "player", game);
        game.addFaction(faction);
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 2);
    }

    @Nested
    @DisplayName("#revival")
    class Revival extends FactionTestTemplate.Revival {
        @Test
        @Override
        public void testInitialMaxRevival() {
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testMaxRevivalSetTo5() {
            faction.setMaxRevival(5);
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testMaxRevivalWithRecruits() throws InvalidGameStateException {
            game.startRevival();
            game.getRevival().setRecruitsInPlay(true);
            assertEquals(20, faction.getMaxRevival());
        }

        @Test
        @Override
        public void testRevivalCost() {
            assertEquals(2, faction.revivalCost(2, 0));
        }

        @Test
        @Override
        public void testRevivalCostAlliedWithBT() {
            faction.setAlly("BT");
            assertEquals(1, faction.revivalCost(2, 0));
        }

        @Test
        @Override
        public void testPaidRevivalChoices() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
            faction.performFreeRevivals();
            faction.presentPaidRevivalChoices(freeRevivals);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(5 - freeRevivals + 1, chat.getChoices().getFirst().size());
        }

        @Test
        @Override
        public void testPaidRevivalMessageAfter3Free() throws InvalidGameStateException {
            faction.removeForces(faction.getHomeworld(), 5, false, true);
//            faction.performFreeRevivals();
            // The following should be replaced with performFreeRevivals after setting Fremen as ally, override for Fremen
            game.reviveForces(faction, false, 3, 0);
            int numRevived = 3;
            faction.presentPaidRevivalChoices(numRevived);
            assertEquals(1, chat.getMessages().size());
            assertEquals(1, chat.getChoices().size());
            assertEquals(5 - numRevived + 1, chat.getChoices().getFirst().size());
        }
    }

    @Test
    public void testFreeRevival() {
        assertEquals(0, faction.getFreeRevival());
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
        assertEquals(1, faction.getFreeRevival());
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
        assertEquals(0, faction.getFreeRevival());
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
        assertEquals(2, faction.getFreeRevival());
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
        assertEquals(faction.getEmoji(), Emojis.CHOAM);
    }

    @Test
    public void testHandLimit() {
        assertEquals(faction.getHandLimit(), 5);
    }

    @Test
    public void choamInflationBeforeGameStart() throws InvalidGameStateException {
        game.setTurn(0);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertEquals(2, faction.getFirstInflationRound());
    }

    @Test
    public void choamInflationEarlyTurn1() throws InvalidGameStateException {
        game.setTurn(1);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertEquals(2, faction.getFirstInflationRound());
    }

    @Test
    public void choamInflationBeforeChoamPhase() throws InvalidGameStateException {
        int round = 3;
        game.setTurn(round);
        game.setPhase(3);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertEquals(round, faction.getFirstInflationRound());
    }

    @Test
    public void choamInflationAfterChoamPhase() throws InvalidGameStateException {
        int round = 3;
        game.setTurn(round);
        game.setPhase(5);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertEquals(round + 1, faction.getFirstInflationRound());
    }

    @Test
    public void choamCInflationCannotBeSetTwice() throws InvalidGameStateException {
        int round = 3;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        game.setTurn(round + 2);
        assertThrows(InvalidGameStateException.class, () -> faction.setFirstInflation(ChoamInflationType.DOUBLE));
    }

    @Test
    public void choamCInflationCanBeClearedAndSetAgain() throws InvalidGameStateException {
        int round = 3;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        faction.clearInflation();
        game.setTurn(round + 2);
        assertDoesNotThrow(() -> faction.setFirstInflation(ChoamInflationType.DOUBLE));
    }

    @RepeatedTest(9)
    public void choamCanSetInflationDoubleRound(RepetitionInfo repetitionInfo) throws InvalidGameStateException {
        int round = repetitionInfo.getCurrentRepetition() + 1;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertEquals(faction.getFirstInflationRound(), round);
        assertEquals(faction.getFirstInflationType(), ChoamInflationType.DOUBLE);
    }

    @RepeatedTest(9)
    public void choamCanSetInflationCancelRound(RepetitionInfo repetitionInfo) throws InvalidGameStateException {
        int round = repetitionInfo.getCurrentRepetition() + 1;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.CANCEL);
        assertEquals(faction.getFirstInflationRound(), round);
        assertEquals(faction.getFirstInflationType(), ChoamInflationType.CANCEL);
    }

    @Test
    public void choamInflationTypeBeginningOfGame() {
        assertNull(faction.getInflationType(0));
    }

    @RepeatedTest(10)
    public void choamWhenInflationIsNotSet(RepetitionInfo repetitionInfo) {
        int round = repetitionInfo.getCurrentRepetition();
        game.setTurn(round);
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @RepeatedTest(4)
    public void choamBeforeInflation(RepetitionInfo repetitionInfo) throws InvalidGameStateException {
        int round = repetitionInfo.getCurrentRepetition();
        game.setTurn(5);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @RepeatedTest(4)
    public void choamAfterInflation(RepetitionInfo repetitionInfo) throws InvalidGameStateException {
        int round = repetitionInfo.getCurrentRepetition() + 6;
        game.setTurn(5);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @Test
    public void choamSetToDoubleFirst() throws InvalidGameStateException {
        int round = 5;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.DOUBLE);

        assertEquals(faction.getInflationType(round), ChoamInflationType.DOUBLE);
        assertEquals(faction.getChoamMultiplier(round), 2);

        assertEquals(faction.getInflationType(round + 1), ChoamInflationType.CANCEL);
        assertEquals(faction.getChoamMultiplier(round + 1), 0);
    }

    @Test
    public void choamSetToCancelFirst() throws InvalidGameStateException {
        int round = 5;
        game.setTurn(round);
        faction.setFirstInflation(ChoamInflationType.CANCEL);

        assertEquals(faction.getInflationType(round), ChoamInflationType.CANCEL);
        assertEquals(0, faction.getChoamMultiplier(round));

        assertEquals(faction.getInflationType(round + 1), ChoamInflationType.DOUBLE);
        assertEquals(2, faction.getChoamMultiplier(round + 1));
    }

    @Nested
    @DisplayName("#performMentatPauseActions")
    class PerformMentatPauseActions extends FactionTestTemplate.PerformMentatPauseActions {
        @BeforeEach
        void setUp() {
            faction = getFaction();
            chat = new TestTopic();
            faction.setChat(chat);
        }

        @Test
        @Override
        void testCanPayToRemoveExtortion() {
            faction.setSpice(3);
            faction.performMentatPauseActions(true);
            assertEquals("Will you pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game? " + faction.getPlayer(), chat.getMessages().getFirst());
            assertEquals(2, chat.getChoices().size());
        }

        @Test
        @Override
        void testCannotPayToRemoveExtortion() {
            faction.setSpice(0);
            faction.performMentatPauseActions(true);
            assertEquals("You do not have enough spice to pay Extortion.", chat.getMessages().getFirst());
            assertEquals(1, chat.getChoices().size());
        }

        @Test
        public void testChoamAskedAboutInflation() {
            faction.performMentatPauseActions(false);
            assertEquals(1, chat.getMessages().size());
            assertEquals("Would you like to set Inflation? " + faction.getPlayer(), chat.getMessages().getFirst());
            assertEquals(3, chat.getChoices().getFirst().size());
        }

        @Test
        public void testNoBribesMayBeMadeMessage() throws InvalidGameStateException {
            ChoamFaction choam = (ChoamFaction) faction;
            game.advanceTurn();
            assertEquals(1, game.getTurn());
            choam.setFirstInflation(ChoamInflationType.DOUBLE);
            assertEquals(Emojis.CHOAM + " set Inflation to DOUBLE for turn 2", turnSummary.getMessages().getFirst());
            faction.performMentatPauseActions(false);
            assertEquals("No bribes may be made while the " + Emojis.CHOAM + " Inflation token is Double side up.", turnSummary.getMessages().get(1));
        }

        @Test
        public void testBribesMayBeMadeAgainMessage() throws InvalidGameStateException {
            ChoamFaction choam = (ChoamFaction) faction;
            game.advanceTurn();
            assertEquals(1, game.getTurn());
            choam.setFirstInflation(ChoamInflationType.DOUBLE);
            assertEquals(Emojis.CHOAM + " set Inflation to DOUBLE for turn 2", turnSummary.getMessages().getFirst());
            game.advanceTurn();
            faction.performMentatPauseActions(false);
            assertEquals("Bribes may be made again. The Inflation Token is no longer Double side up.", turnSummary.getMessages().get(1));
        }
    }
}