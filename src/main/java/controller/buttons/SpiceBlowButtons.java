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
        if (event.getComponentId().equals("spiceblow-thumper-yes")) playThumper(event, discordGame, game);
        else if (event.getComponentId().equals("spiceblow-thumper-no")) declineThumper(event, discordGame, game);
    }

    private static void playThumper(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getSpiceBlowAndNexus().playThumper(game, faction);
        RunCommands.advance(discordGame, game);
    }

    private static void declineThumper(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getSpiceBlowAndNexus().declineThumper(game, faction);
        RunCommands.advance(discordGame, game);
    }
}
