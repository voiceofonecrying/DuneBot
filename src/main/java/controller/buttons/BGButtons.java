package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.BGFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

import static controller.commands.BGCommands.advise;
import static controller.commands.BGCommands.flip;

public class BGButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "bg"
        // And any button that begins with "bg" must be handled by this class
        if (event.getComponentId().startsWith("bg-advise-")) advise(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]), 1);
        else if (event.getComponentId().startsWith("bg-dont-advise-")) dontAdvise(event, discordGame);
        else if (event.getComponentId().startsWith("bg-flip-")) bgFlip(event, game, discordGame);
        else if (event.getComponentId().startsWith("bg-dont-flip-")) dontFlip(event, discordGame);
        else if (event.getComponentId().startsWith("bg-ht")) advise(discordGame, game, game.getTerritory("Polar Sink"), 2);
        else if (event.getComponentId().startsWith("bg-ally-voice-")) allyVoice(event, game, discordGame);
    }

    private static void bgFlip(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " flip in " + event.getComponentId().split("-")[2]);
        flip(discordGame, game, game.getTerritory(event.getComponentId().split("-")[2]));
        discordGame.queueMessageToEphemeral("You will flip.");
        discordGame.queueDeleteMessage();
    }

    private static void dontFlip(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " don't flip in " + event.getComponentId().split("-")[3]);
        discordGame.queueMessageToEphemeral("You will not flip.");
        discordGame.queueDeleteMessage();
    }

    private static void dontAdvise(ButtonInteractionEvent event, DiscordGame discordGame) {
        discordGame.queueMessage(Emojis.BG + " Don't advise in " + event.getComponentId().split("-")[3]);
        discordGame.queueDeleteMessage();
    }

    private static void allyVoice(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        BGFaction bg = (BGFaction) ButtonManager.getButtonPresser(event, game);
        String action = event.getComponentId().replace("bg-ally-voice-", "");
        if (action.equals("yes")) {
            bg.setDenyingAllyVoice(false);
            discordGame.queueMessage("You will allow " + Emojis.TREACHERY + " Prescience to be published to your ally thread.");
        } else {
            bg.setDenyingAllyVoice(true);
            discordGame.queueMessage("You will not allow " + Emojis.TREACHERY + " Prescience to be published to your ally thread.");
        }
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
