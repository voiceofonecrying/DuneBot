package controller.commands;

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
                            "You predict <:treachery:991763073281040518> {0} <:treachery:991763073281040518> is up for bid.",
                            card.name().strip()
                    )
            );
        }
    }
}
