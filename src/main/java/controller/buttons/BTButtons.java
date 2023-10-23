package controller.buttons;

import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.Game;
import model.factions.BTFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class BTButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {

        if (event.getComponentId().startsWith("bt-revival-rate-set-")) {
            btRevivalRateSet(event, game, discordGame);
        }
    }

    private static void btRevivalRateSet(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        BTFaction bt = (BTFaction) game.getFaction("BT");
        Faction faction = game.getFaction(event.getComponentId().split("-")[4]);
        faction.setMaxRevival(Integer.parseInt(event.getComponentId().split("-")[5]));
        bt.addRevivalRatesSet(faction.getName());
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " revival limit has been set to " + faction.getMaxRevival());
        discordGame.queueMessage("Revival limit for " + faction.getName() + " has been set.");
        if (bt.hasSetAllRevivalRates()) {
            bt.clearRevivalRatesSet();
            if (game.getPhase() == 5 && game.getSubPhase() == 2) RunCommands.advance(discordGame, game);
            return;
        }
        discordGame.pushGame();
    }
}
