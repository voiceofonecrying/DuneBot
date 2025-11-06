package testutil.discord.state;

import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for button functionality in MockMessageState.
 * Verifies button storage, retrieval, and management in messages.
 */
@DisplayName("MockMessageState Button Functionality Tests")
class MockMessageStateButtonTest {

    private MockMessageState message;

    @BeforeEach
    void setUp() {
        message = new MockMessageState(100L, 1000L, 2000L, "Test message");
    }

    @Test
    @DisplayName("Should have no buttons initially")
    void shouldHaveNoButtonsInitially() {
        // Then
        assertThat(message.hasButtons()).isFalse();
        assertThat(message.getButtons()).isEmpty();
    }

    @Test
    @DisplayName("Should add single button to message")
    void shouldAddSingleButtonToMessage() {
        // Given
        MockButtonState button = new MockButtonState("btn-test", "Test", "PRIMARY", false);

        // When
        message.addButton(button);

        // Then
        assertThat(message.hasButtons()).isTrue();
        assertThat(message.getButtons()).hasSize(1);
        assertThat(message.getButtons().get(0)).isEqualTo(button);
    }

    @Test
    @DisplayName("Should add multiple buttons to message")
    void shouldAddMultipleButtonsToMessage() {
        // Given
        MockButtonState button1 = new MockButtonState("btn-1", "Button 1", "PRIMARY", false);
        MockButtonState button2 = new MockButtonState("btn-2", "Button 2", "SECONDARY", false);
        MockButtonState button3 = new MockButtonState("btn-3", "Button 3", "SUCCESS", true);

        // When
        message.addButton(button1);
        message.addButton(button2);
        message.addButton(button3);

        // Then
        assertThat(message.hasButtons()).isTrue();
        assertThat(message.getButtons()).hasSize(3);
        assertThat(message.getButtons()).containsExactly(button1, button2, button3);
    }

    @Test
    @DisplayName("Should preserve button order")
    void shouldPreserveButtonOrder() {
        // Given
        MockButtonState[] buttons = new MockButtonState[5];
        for (int i = 0; i < 5; i++) {
            buttons[i] = new MockButtonState("btn-" + i, "Button " + i, "PRIMARY", false);
            message.addButton(buttons[i]);
        }

        // Then
        List<MockButtonState> retrievedButtons = message.getButtons();
        for (int i = 0; i < 5; i++) {
            assertThat(retrievedButtons.get(i)).isEqualTo(buttons[i]);
            assertThat(retrievedButtons.get(i).getLabel()).isEqualTo("Button " + i);
        }
    }


    @Test
    @DisplayName("Should handle mixed button styles")
    void shouldHandleMixedButtonStyles() {
        // When
        message.addButton(new MockButtonState("primary", "Primary", "PRIMARY", false));
        message.addButton(new MockButtonState("secondary", "Secondary", "SECONDARY", false));
        message.addButton(new MockButtonState("success", "Success", "SUCCESS", false));
        message.addButton(new MockButtonState("danger", "Danger", "DANGER", false));
        message.addButton(new MockButtonState("https://link.com", "Link", "LINK", false));

        // Then
        assertThat(message.getButtons()).hasSize(5);
        assertThat(message.getButtons()).extracting(MockButtonState::getStyle)
                .containsExactly("PRIMARY", "SECONDARY", "SUCCESS", "DANGER", "LINK");
    }

    @Test
    @DisplayName("Should handle disabled buttons")
    void shouldHandleDisabledButtons() {
        // When
        message.addButton(new MockButtonState("enabled", "Enabled", "PRIMARY", false));
        message.addButton(new MockButtonState("disabled", "Disabled", "PRIMARY", true));

        // Then
        assertThat(message.getButtons().get(0).isDisabled()).isFalse();
        assertThat(message.getButtons().get(1).isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Should return defensive copy of button list")
    void shouldReturnDefensiveCopyOfButtonList() {
        // Given
        MockButtonState button = new MockButtonState("btn-1", "Button 1", "PRIMARY", false);
        message.addButton(button);

        // When
        List<MockButtonState> buttons = message.getButtons();
        buttons.clear(); // Try to clear the returned list

        // Then
        assertThat(message.getButtons()).hasSize(1); // Original list unchanged
    }

    @Test
    @DisplayName("Should support buttons on messages with attachments")
    void shouldSupportButtonsOnMessagesWithAttachments() {
        // Given
        FileUpload attachment = FileUpload.fromData(
            new ByteArrayInputStream("test data".getBytes()),
            "test.txt"
        );
        MockMessageState messageWithAttachment = new MockMessageState(
            101L, 1000L, 2000L, "Message with attachment", List.of(attachment)
        );

        // When
        messageWithAttachment.addButton(new MockButtonState("download", "Download", "PRIMARY", false));

        // Then
        assertThat(messageWithAttachment.hasButtons()).isTrue();
        assertThat(messageWithAttachment.getButtons()).hasSize(1);
        assertThat(messageWithAttachment.getAttachments()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle empty button labels")
    void shouldHandleEmptyButtonLabels() {
        // When
        message.addButton(new MockButtonState("no-label", "", "PRIMARY", false));

        // Then
        assertThat(message.getButtons()).hasSize(1);
        assertThat(message.getButtons().get(0).getLabel()).isEmpty();
    }

    @Test
    @DisplayName("Should track buttons separately from message content")
    void shouldTrackButtonsSeparatelyFromMessageContent() {
        // Given
        MockMessageState richMessage = new MockMessageState(
            102L, 1000L, 2000L, "Click the buttons below!"
        );

        // When
        richMessage.addButton(new MockButtonState("option-1", "Option 1", "PRIMARY", false));
        richMessage.addButton(new MockButtonState("option-2", "Option 2", "SECONDARY", false));

        // Then
        assertThat(richMessage.getContent()).isEqualTo("Click the buttons below!");
        assertThat(richMessage.hasButtons()).isTrue();
        assertThat(richMessage.getButtons()).hasSize(2);
    }

    @Test
    @DisplayName("Should find button by component ID")
    void shouldFindButtonByComponentId() {
        // Given
        message.addButton(new MockButtonState("find-me", "Find Me", "PRIMARY", false));
        message.addButton(new MockButtonState("other", "Other", "SECONDARY", false));

        // When
        MockButtonState found = message.getButtons().stream()
                .filter(btn -> btn.getComponentId().equals("find-me"))
                .findFirst()
                .orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getLabel()).isEqualTo("Find Me");
    }

    @Test
    @DisplayName("Should support complex button IDs")
    void shouldSupportComplexButtonIds() {
        // When
        message.addButton(new MockButtonState(
            "ship-starting-forces-Sietch-Tabr",
            "Sietch Tabr",
            "SUCCESS",
            false
        ));

        // Then
        MockButtonState button = message.getButtons().get(0);
        assertThat(button.getComponentId()).isEqualTo("ship-starting-forces-Sietch-Tabr");
    }
}