package controller.buttons;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class FremenButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {

        if (event.getComponentId().startsWith("fremen-ht-")) triggerHT(event, game, discordGame);
        if (event.getComponentId().equals("fremen-cancel")) cancelHT(discordGame);

    }

    private static void cancelHT(DiscordGame discordGame) {
        discordGame.queueMessage("Your " + Emojis.FREMEN_FEDAYKIN + " will stay in the Southern Hemisphere.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerHT(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Territory territory = game.getTerritory(event.getComponentId().split("-")[2]);
        game.getFaction("Fremen").placeForces(territory, 0, 1, false, true, discordGame, game, false, false);
        discordGame.getTurnSummary().queueMessage(Emojis.FREMEN + " place their revived " + Emojis.FREMEN_FEDAYKIN + " with their forces in " + territory.getTerritoryName());
        discordGame.queueMessage("Your " + Emojis.FREMEN_FEDAYKIN + " has left for the northern hemisphere.");
        discordGame.pushGame();
    }
}
