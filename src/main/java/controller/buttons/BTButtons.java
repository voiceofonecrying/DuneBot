package controller.buttons;

import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import model.factions.BTFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class BTButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {

        if (event.getComponentId().startsWith("bt-revival-rate-set-")) {
            btRevivalRateSet(event, game, discordGame);
            discordGame.queueDeleteMessage();
        }
    }

    private static void btRevivalRateSet(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        BTFaction bt = (BTFaction) game.getFaction("BT");
        bt.setRevivalRatesSet(bt.getRevivalRatesSet() + 1);
        Faction faction = game.getFaction(event.getComponentId().split("-")[4]);
        faction.setMaxRevival(Integer.parseInt(event.getComponentId().split("-")[5]));
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " revival limit has been set to " + faction.getMaxRevival());
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("Revival limit for " + faction.getName() + " has been set.");
        if (bt.getRevivalRatesSet() == 5) {
            bt.setRevivalRatesSet(0);
            RunCommands.advance(discordGame, game);
            return;
        }
        discordGame.pushGame();
    }
}
