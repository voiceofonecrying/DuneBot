package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.TreacheryCard;

import java.text.MessageFormat;

public class AtreidesCommands {
    public static void sendAtreidesCardPrescience(DiscordGame discordGame, Game gameState, TreacheryCard card) throws ChannelNotFoundException {
        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat",
                    MessageFormat.format(
                            "You predict {0} {1} {0} is up for bid.",
                            Emojis.TREACHERY, card.name().strip()
                    )
            );
        }
    }
}
