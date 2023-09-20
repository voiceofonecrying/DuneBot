package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import model.Bidding;
import model.TreacheryCard;

import java.text.MessageFormat;

public class AtreidesCommands {
    public static void sendAtreidesCardPrescience(DiscordGame discordGame, Game game, TreacheryCard card) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (game.hasFaction("Atreides")) {
            discordGame.getAtreidesChat().queueMessage(
                    MessageFormat.format(
                            "You predict {0} {1} {0} is up for bid (R{2}:C{3}).",
                            Emojis.TREACHERY, card.name().strip(), game.getTurn(), bidding.getBidCardNumber()
                    )
            );
        }
    }
}
