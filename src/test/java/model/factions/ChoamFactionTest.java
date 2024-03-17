package model.factions;

import constants.Emojis;
import enums.ChoamInflationType;
import model.Territory;
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
    }

    @Test
    public void testInitialSpice() {
        assertEquals(faction.getSpice(), 2);
    }

    @Test
    public void testFreeRevivals() {
        assertEquals(faction.getFreeRevival(), 0);
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
    public void choamCanNotSetInflationRoundTooLow() {
        assertThrows(IllegalArgumentException.class, () ->
                faction.setFirstInflation(0, ChoamInflationType.DOUBLE));
        assertDoesNotThrow(() -> faction.setFirstInflation(1, ChoamInflationType.DOUBLE));
        assertThrows(IllegalArgumentException.class, () ->
                faction.setFirstInflation(0, ChoamInflationType.CANCEL));
        assertDoesNotThrow(() -> faction.setFirstInflation(1, ChoamInflationType.CANCEL));
    }

    @Test
    public void choamCanNotSetInflationRoundTooHigh() {
        assertThrows(IllegalArgumentException.class, () ->
                faction.setFirstInflation(11, ChoamInflationType.DOUBLE));
        assertThrows(IllegalArgumentException.class, () ->
                faction.setFirstInflation(11, ChoamInflationType.CANCEL));
    }

    @RepeatedTest(9)
    public void choamCanSetInflationRound(RepetitionInfo repetitionInfo) {
        int round = repetitionInfo.getCurrentRepetition() + 1;

        faction.setFirstInflation(round, ChoamInflationType.DOUBLE);
        assertEquals(faction.getFirstInflationRound(), round);
        assertEquals(faction.getFirstInflationType(), ChoamInflationType.DOUBLE);

        faction.setFirstInflation(round, ChoamInflationType.CANCEL);
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
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @RepeatedTest(4)
    public void choamBeforeInflation(RepetitionInfo repetitionInfo) {
        int round = repetitionInfo.getCurrentRepetition();
        faction.setFirstInflation(5, ChoamInflationType.DOUBLE);
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @RepeatedTest(4)
    public void choamAfterInflation(RepetitionInfo repetitionInfo) {
        int round = repetitionInfo.getCurrentRepetition() + 6;
        faction.setFirstInflation(5, ChoamInflationType.DOUBLE);
        assertNull(faction.getInflationType(round));
        assertEquals(faction.getChoamMultiplier(round), 1);
    }

    @Test
    public void choamSetToDoubleFirst() {
        faction.setFirstInflation(5, ChoamInflationType.DOUBLE);

        assertEquals(faction.getInflationType(5), ChoamInflationType.DOUBLE);
        assertEquals(faction.getChoamMultiplier(5), 2);

        assertEquals(faction.getInflationType(6), ChoamInflationType.CANCEL);
        assertEquals(faction.getChoamMultiplier(6), 0);
    }

    @Test
    public void choamSetToCancelFirst() {
        faction.setFirstInflation(5, ChoamInflationType.CANCEL);

        assertEquals(faction.getInflationType(5), ChoamInflationType.CANCEL);
        assertEquals(0, faction.getChoamMultiplier(5));

        assertEquals(faction.getInflationType(6), ChoamInflationType.DOUBLE);
        assertEquals(2, faction.getChoamMultiplier(6));
    }
}