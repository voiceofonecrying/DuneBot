package controller.buttons;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.Territory;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class FremenButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException {

        if (event.getComponentId().startsWith("fremen-ht-")) triggerHT(event, game, discordGame);
        if (event.getComponentId().equals("fremen-cancel")) cancelHT(discordGame);

    }

    private static void cancelHT(DiscordGame discordGame) {
        discordGame.queueMessage("Your " + Emojis.FREMEN_FEDAYKIN + " will stay in the Southern Hemisphere.");
        discordGame.queueDeleteMessage();
    }

    private static void triggerHT(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Territory territory = game.getTerritory(event.getComponentId().split("-")[2]);
        CommandManager.placeForces(territory, game.getFaction("Fremen"), 0, 1, false, discordGame, game, false);
        discordGame.getTurnSummary().queueMessage(Emojis.FREMEN + " place their revived " + Emojis.FREMEN_FEDAYKIN + " with their forces in " + territory.getTerritoryName());
        discordGame.queueMessage("Your " + Emojis.FREMEN_FEDAYKIN + " has left for the northern hemisphere.");
        discordGame.pushGame();
    }
}
