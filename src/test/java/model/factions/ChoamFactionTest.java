package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.Territory;
import model.TestTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

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
        game.setTurnSummary(new TestTopic());
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 2);
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
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(1, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalLowThresholdAlliedToFremen() {
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(4, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruits() {
        game.setRecruitsInPlay(true);
        assertEquals(0, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsAlliedWithFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        assertEquals(6, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThreshold() {
        game.setRecruitsInPlay(true);
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
        faction.checkForLowThreshold();
        assertFalse(faction.isHighThreshold());
        assertEquals(2, faction.getFreeRevival());
    }

    @Test
    public void testFreeRevivalWithRecruitsLowThresholdAlliedToFremen() {
        game.setRecruitsInPlay(true);
        faction.setAlly("Fremen");
        game.addGameOption(GameOption.HOMEWORLDS);
        game.getTerritory(faction.homeworld).setForceStrength(faction.getName(), faction.highThreshold - 1);
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
        assertEquals(faction.getReserves().getStrength(), 20);
        assertEquals(faction.getReserves().getName(), "CHOAM");
    }

    @Test
    public void testInitialForcePlacement() {
        for (String territoryName : game.getTerritories().keySet()) {
            if (game.getHomeworlds().containsValue(territoryName)) continue;
            Territory territory = game.getTerritories().get(territoryName);
            assertEquals(territory.getForces().size(), 0);
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
}