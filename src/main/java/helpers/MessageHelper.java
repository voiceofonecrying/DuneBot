package helpers;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper methods for working with Discord messages in JDA 6.
 */
public class MessageHelper {
    /**
     * Extracts all buttons from a message's components.
     * In JDA 6, buttons are nested within ActionRows, so this method flattens the component tree.
     *
     * @param message The message to extract buttons from
     * @return A list of all buttons in the message
     */
    public static List<Button> getButtons(Message message) {
        return message.getComponents().stream()
                .filter(component -> component instanceof ActionRow)
                .flatMap(component -> ((ActionRow) component).getActionComponents().stream())
                .filter(component -> component instanceof Button)
                .map(component -> (Button) component)
                .collect(Collectors.toList());
    }
}
