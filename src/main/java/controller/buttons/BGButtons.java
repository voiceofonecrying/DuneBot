package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

import static controller.commands.BGCommands.advise;

public class BGButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {

        if (event.getComponentId().startsWith("bg-advise-")) {
            if (!ButtonManager.getButtonPresser(event, game).getName().equals("BG")) {
                discordGame.queueMessageToEphemeral("You are not the " + Emojis.BG);
                return;
            }
            advise(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]));
            discordGame.queueDeleteMessage();
        }
        else if (event.getComponentId().startsWith("bg-dont-advise-")) dontAdvise(event, game, discordGame);

    }

    private static void dontAdvise(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        if (!ButtonManager.getButtonPresser(event, game).getName().equals("BG")) {
            discordGame.queueMessageToEphemeral("You are not the " + Emojis.BG);
            return;
        }
        discordGame.queueMessage(Emojis.BG + " Don't advise in " + event.getComponentId().split("-")[3]);
        discordGame.queueDeleteMessage();
    }
}
