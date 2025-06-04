package controller.buttons;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class EmperorButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        // Buttons handled by this class must begin with "bt"
        // And any button that begins with "bt" must be handled by this class
        if (event.getComponentId().startsWith("emperor-discard-")) discardCard(event, game, discordGame);
        if (event.getComponentId().equals("emperor-finished-discarding")) finishedDiscarding(discordGame);

    }

    private static void finishedDiscarding(DiscordGame discordGame) {
        discordGame.queueMessage("You have discarded all that you want to discard.");
        discordGame.queueDeleteMessage();
    }

    private static void discardCard(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String cardName = event.getComponentId().split("-")[2];
        game.getEmperorFaction().kaitainHighDiscard(cardName);
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }
}
