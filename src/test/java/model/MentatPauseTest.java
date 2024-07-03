package model;

import enums.ChoamInflationType;
import exceptions.InvalidGameStateException;
import model.factions.ChoamFaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MentatPauseTest {
    private Game game;
    private ChoamFaction choam;
    private TestTopic choamChat;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        TestTopic turnSummary = new TestTopic();
        game.setTurnSummary(turnSummary);

        choam = new ChoamFaction("cPlayer", "cUser", game);
        game.addFaction(choam);
        choamChat = new TestTopic();
        choam.setChat(choamChat);
    }

    @Test
    void testSpiceForAllyResetsToZero() {
        choam.setSpiceForAlly(5);
        assertEquals(5, choam.getSpiceForAlly());
        game.setTurn(1);
        game.startMentatPause();
        assertEquals(0, choam.getSpiceForAlly());
    }

    @Test
    void testChoamGetsInflationButtons() {
        game.setTurn(1);
        game.startMentatPause();
        assertEquals(1, choamChat.getMessages().size());
        assertEquals(1, choamChat.getChoices().size());
        assertEquals(3, choamChat.getChoices().getFirst().size());
    }

    @Test
    void testChoamDoesNotGetInflationButtons() throws InvalidGameStateException {
        game.setTurn(1);
        choam.setFirstInflation(ChoamInflationType.DOUBLE);
        game.setTurn(3);
        game.startMentatPause();
        assertEquals(0, choamChat.getMessages().size());
        assertEquals(0, choamChat.getChoices().size());
    }
}
