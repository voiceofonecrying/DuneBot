package controller;

import exceptions.ChannelNotFoundException;
import model.Game;
import model.factions.Faction;

import java.text.MessageFormat;
import java.util.Arrays;

public class Alliance {
    public static void createAlliance(DiscordGame discordGame, Faction faction1, Faction faction2) throws ChannelNotFoundException {
        Game game = discordGame.getGame();

        game.createAlliance(faction1, faction2);

        String threadName = MessageFormat.format(
                "{0} {1} Alliance",
                faction1.getName(),
                faction2.getName()
        );

        discordGame.createPrivateThread(discordGame.getTextChannel("chat"), threadName, Arrays.asList(
                faction1.getPlayer(), faction2.getPlayer()
        ));

    }
}
