package controller.buttons;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.ChoamFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class ChoamButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "choam"
        // And any button that begins with "choam" must be handled by this class

        ChoamFaction choam;
        try {
            choam = (ChoamFaction) game.getFaction("CHOAM");
        } catch (IllegalArgumentException e) {
            return;
        }

        if (event.getComponentId().equals("choam-inflation-double")) {
            choam.setFirstInflation(ChoamInflationType.DOUBLE);
            discordGame.queueMessage("You placed your Inflation token Double side up.");
            discordGame.pushGame();
        } else if (event.getComponentId().equals("choam-inflation-cancel")) {
            choam.setFirstInflation(ChoamInflationType.CANCEL);
            discordGame.queueMessage("You placed your Inflation token Cancel side up.");
            discordGame.pushGame();
        } else if (event.getComponentId().equals("choam-inflation-not-yet")) {
            discordGame.queueMessage("You will not place your Inflation token at this time.");
            discordGame.getTurnSummary().queueMessage(Emojis.CHOAM + " does not place their Inflation token at this time.");
        }
    }
}
