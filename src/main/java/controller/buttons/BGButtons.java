package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

import static controller.commands.BGCommands.advise;
import static controller.commands.BGCommands.flip;

public class BGButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        if (event.getComponentId().startsWith("bg") && !ButtonManager.getButtonPresser(event, game).getName().equals("BG")) {
            discordGame.queueMessageToEphemeral("You are not the " + Emojis.BG);
            return;
        }

        if (event.getComponentId().startsWith("bg-advise-")) {
            advise(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]), 1);
        } else if (event.getComponentId().startsWith("bg-dont-advise-")) dontAdvise(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-flip-")) bgFlip(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-dont-flip-")) dontFlip(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-ht")) {
            advise(discordGame, game, game.getTerritory("Polar Sink"), 2);
        }
    }

    private static void bgFlip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " flip in " + event.getComponentId().split("-")[2]);
        flip(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]));
        discordGame.queueMessageToEphemeral("You will flip.");
        discordGame.queueDeleteMessage();
    }

    private static void dontFlip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " Don't flip in " + event.getComponentId().split("-")[3]);
        discordGame.queueMessageToEphemeral("You will not flip.");
        discordGame.queueDeleteMessage();
    }

    private static void dontAdvise(ButtonInteractionEvent event, Game game, DiscordGame discordGame) {
        discordGame.queueMessage(Emojis.BG + " Don't advise in " + event.getComponentId().split("-")[3]);
        discordGame.queueDeleteMessage();
    }
}
