package controller;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.DuneBotValidator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DuneBotTest {

    DuneBotValidator underTest;


    @BeforeEach
    void setUp() {
        underTest = new DuneBotValidator();
    }


    @Test
    void itShouldLoginToDiscordIfTokenIsValid() {
        // Given
        String botKey = Dotenv.configure().load().get("TOKEN");
        // When
        boolean isValid = underTest.test(botKey);
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void itShouldFailToLoginToDiscordIfTokenIsInvalid() {
        // Given
        String botKey = "THIS IS AN INVALID TOKEN";
        // When
        boolean isValid = underTest.test(botKey);
        // Then
        assertThat(isValid).isFalse();
    }
}