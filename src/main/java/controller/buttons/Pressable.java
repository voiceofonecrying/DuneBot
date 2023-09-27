package controller.buttons;

import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface Pressable {
    static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
    }
}
