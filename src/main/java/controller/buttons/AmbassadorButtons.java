package controller.buttons;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class AmbassadorButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "ambassador"
        // And any button that begins with "ambassador" must be handled by this class
        if (event.getComponentId().startsWith("ambassador-fremen-")) handleFremenAmbassadorButtons(event, game, discordGame);
        else if (event.getComponentId().startsWith("ambassador-guild-")) handleGuildAmbassadorButtons(event, game, discordGame);
    }

    private static void handleFremenAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-fremen-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }

    private static void handleGuildAmbassadorButtons(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String action = event.getComponentId().replace("ambassador-guild-", "");
        MovementButtonActions.handleMovementAction(event, discordGame, action);
    }
}
