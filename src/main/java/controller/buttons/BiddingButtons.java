package controller.buttons;

import controller.DiscordGame;
import controller.commands.ShowCommands;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class BiddingButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        // Buttons handled by this class must begin with "bidding"
        // And any button that begins with "bidding" must be handled by this class
        if (event.getComponentId().startsWith("bidding-auto-pass")) toggleAutoPass(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-turn-pass")) toggleAutoPassTurn(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-pass")) passOnce(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-toggle-outbid-ally")) toggleOutbidAlly(event, discordGame, game);
        game.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    private static void passOnce(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().pass(game, faction);
        discordGame.queueMessage("You will pass the next bid.");
        ShowCommands.sendBiddingActions(discordGame, game, faction);
        discordGame.pushGame();
    }

    private static void toggleAutoPassTurn(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().setAutoPassEntireTurn(game, faction, !faction.isAutoBidTurn());
        if (!faction.isAutoBidTurn()) {
            discordGame.queueMessage("You will not auto-pass on every card this round.");
        } else {
            int maxBid = faction.getMaxBid();
            if (maxBid > game.getBidding().getCurrentBid())
                discordGame.queueMessage("You will auto-pass on this card if the bid gets to " + maxBid + " or higher and then auto-pass on every card this round.");
            else
                discordGame.queueMessage("You will auto-pass on every card this round.");
        }
        ShowCommands.sendBiddingActions(discordGame, game, faction);
        discordGame.pushGame();
    }

    private static void toggleAutoPass(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().setAutoPass(game, faction, !faction.isAutoBid());
        if (!faction.isAutoBid()) {
            discordGame.queueMessage("You will not auto-pass on this card.");
        } else {
            int maxBid = faction.getMaxBid();
            if (maxBid > game.getBidding().getCurrentBid())
                discordGame.queueMessage("You will auto-pass on this card if the bid gets to " + maxBid + " or higher.");
            else
                discordGame.queueMessage("You will auto-pass on this card.");
        }
        ShowCommands.sendBiddingActions(discordGame, game, faction);
        discordGame.pushGame();
    }

    private static void toggleOutbidAlly(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.setOutbidAlly(!faction.isOutbidAlly());
        if (!faction.isOutbidAlly()) {
            discordGame.queueMessage("You will not outbid your ally.");
        } else {
            discordGame.queueMessage("You will have the chance to outbid your ally.");
        }
        ShowCommands.sendBiddingActions(discordGame, game, faction);
        discordGame.pushGame();
    }
}
