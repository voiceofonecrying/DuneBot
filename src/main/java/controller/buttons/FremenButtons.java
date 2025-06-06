package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class FremenButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        // Buttons handled by this class must begin with "fremen"
        // And any button that begins with "fremen" must be handled by this class
        if (event.getComponentId().startsWith("fremen-ht-")) triggerHT(event, game, discordGame);
        if (event.getComponentId().equals("fremen-cancel")) cancelHT(discordGame);

    }

    private static void cancelHT(DiscordGame discordGame) {
        discordGame.queueMessage("Your " + Emojis.FREMEN_FEDAYKIN + " will stay in the Southern Hemisphere.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerHT(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        String territoryName = event.getComponentId().split("-")[2];
        game.getFremenFaction().placeFreeRevivalWithHighThreshold(territoryName);
        discordGame.pushGame();
    }
}
