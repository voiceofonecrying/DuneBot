package controller.buttons;

import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class StormButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidOptionException {
        if (!event.getComponentId().startsWith("storm")) return;
        int stormAdjustment = Integer.parseInt(event.getComponentId().replace("storm", ""));
        game.setStormMovement(game.getStormMovement() + stormAdjustment);
        discordGame.getTurnSummary().queueMessage("The Ecological Testing Station has predicted that the projected storm movement will be off by " + stormAdjustment);
        discordGame.queueMessage("Your Ecological Prediction has been submitted.");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
