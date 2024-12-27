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
        if (event.getComponentId().startsWith("spiceblow-thumper-")) thumper(event, discordGame, game);
    }

    private static void thumper(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        discordGame.queueDeleteMessage();
        String playIt = event.getComponentId().split("-")[2];
        if (playIt.equals("yes")) {
            game.getSpiceBlowAndNexus().resolveThumper(true);
            String deck = event.getComponentId().split("-")[3];
            discordGame.queueMessage("You will play Thumper.");
            faction.discard("Thumper", "to summon Shai-Hulud");
            String territoryName = game.getSpiceDiscardA().getLast().name();
            if (deck.equals("B"))
                territoryName = game.getSpiceDiscardB().getLast().name();
            game.getTurnSummary().publish(game.getTerritory(territoryName).shaiHuludAppears(game, "Shai-Hulud", true));
        } else {
            game.getSpiceBlowAndNexus().resolveThumper(false);
            discordGame.queueMessage("You will not play Thumper");
        }
        RunCommands.advance(discordGame, game);
    }
}
