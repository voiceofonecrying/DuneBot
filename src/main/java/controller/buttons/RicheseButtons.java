package controller.buttons;

import exceptions.InvalidGameStateException;
import controller.commands.RicheseCommands;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import java.io.IOException;

public class RicheseButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        if (event.getComponentId().startsWith("richeserunblackmarket-")) runBlackMarket(event, discordGame, game);
        else if (event.getComponentId().startsWith("richeseblackmarketmethod-")) blackMarketMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richeseblackmarket-")) confirmBlackMarket(event, discordGame, game);
    }

    private static void runBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        RicheseFaction richeseFaction = (RicheseFaction)game.getFaction("Richese");
        /*
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        */
        String cardName = event.getComponentId().split("-")[1];
        if (cardName.equals("skip")) {
            discordGame.queueMessage("No black market this turn");
            RunCommands.advance(discordGame, game);
        } else {
            discordGame.queueMessage("You selected " + cardName.trim() + ".");
            RicheseCommands.blackMarketMethod(discordGame, game, cardName);
        }
        discordGame.queueDeleteMessage();
    }

    private static void blackMarketMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        RicheseFaction richeseFaction = (RicheseFaction)game.getFaction("Richese");
        /*
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        */
        String cardName = event.getComponentId().split("-")[1];
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.askBlackMarket(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You selected " + method + " auction.");
            RicheseCommands.confirmBlackMarket(discordGame, game, cardName, method);
        }
        discordGame.queueDeleteMessage();
//        IxCommands.confirmStartingCard(discordGame, game, event.getComponentId().split("-")[0]);
    }

    private static void confirmBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        RicheseFaction richeseFaction = (RicheseFaction)game.getFaction("Richese");
        /*
        if (game.isSetupFinished()) {
            throw new InvalidGameStateException("Setup phase is completed.");
        } else if (ixFaction.getTreacheryHand().size() <= 4) {
            throw new InvalidGameStateException("You have already selected your card.");
        }
        */
        String cardName = event.getComponentId().split("-")[1];
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.askBlackMarket(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You are selling " + cardName.trim() + " by " + method + " auction.");
            RicheseCommands.blackMarketBidComplete(discordGame, game, cardName, method);
            if (method.equals("Silent"))
                discordGame.queueMessage("mod-info", "Run /awardbid after all bids are in, then /run advance.");
        }
        discordGame.queueDeleteMessage();
//        IxCommands.confirmStartingCard(discordGame, game, event.getComponentId().split("-")[0]);
    }
}
