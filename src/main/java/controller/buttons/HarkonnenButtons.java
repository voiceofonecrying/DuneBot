package controller.buttons;

import controller.DiscordGame;
import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class HarkonnenButtons implements Pressable {
    // Buttons handled by this class must begin with "harkonnen"
    // And any button that begins with "harkonnen" must be handled by this class
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("harkonnen-mulligan-")) harkonnenMulligan(event, game, discordGame);
    }

    public static void harkonnenMulligan(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String yesOrNo = event.getComponentId().replace("harkonnen-mulligan-", "");
        String response = "You kept your Traitor Hand as is.";
        if (yesOrNo.equals("yes")) {
            game.getHarkonnenFaction().mulliganTraitorHand();
            response = "You mulliganed your Traitor Hand.";
        }
        discordGame.queueDeleteMessage();
        discordGame.queueMessage(response);
        SetupCommands.advance(event.getGuild(), discordGame, game);
    }
}
