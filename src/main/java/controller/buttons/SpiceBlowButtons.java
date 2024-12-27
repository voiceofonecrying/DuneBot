package controller.buttons;

import controller.DiscordGame;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class SpiceBlowButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "spiceblow"
        // And any button that begins with "spiceblow" must be handled by this class
        if (event.getComponentId().startsWith("spiceblow-thumper-yes-")) playThumper(event, discordGame, game);
        else if (event.getComponentId().equals("spiceblow-thumper-no")) declineThumper(discordGame, game);
    }

    private static void playThumper(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String deck = event.getComponentId().split("-")[3];
        game.getSpiceBlowAndNexus().playThumper(game, faction, deck);
        discordGame.queueMessage("You will play Thumper.");
        RunCommands.advance(discordGame, game);
    }

    private static void declineThumper(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        discordGame.queueDeleteMessage();
        game.getSpiceBlowAndNexus().declineThumper();
        discordGame.queueMessage("You will not play Thumper.");
        RunCommands.advance(discordGame, game);
    }
}
