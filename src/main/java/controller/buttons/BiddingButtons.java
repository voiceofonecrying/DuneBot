package controller.buttons;

import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class BiddingButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        // Buttons handled by this class must begin with "bidding"
        // And any button that begins with "bidding" must be handled by this class
        if (event.getComponentId().startsWith("bidding-auto-pass")) toggleAutoPass(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-turn-pass")) toggleAutoPassTurn(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-pass")) passOnce(event, discordGame, game);
        game.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    private static void passOnce(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().pass(game, faction);
        String infoChannelName = faction.getName().toLowerCase() + "-info";

        discordGame.queueMessage("You will pass the next bid.");
        discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-pass", "Pass One Time")).build());
        discordGame.pushGame();
    }

    private static void toggleAutoPassTurn(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().setAutoPassEntireTurn(game, faction, !faction.isAutoBidTurn());
        String infoChannelName = faction.getName().toLowerCase() + "-info";
        if (!faction.isAutoBidTurn()) {
            discordGame.queueMessage("You will not auto-pass on every card this round.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.success("bidding-turn-pass", "Enable Auto-Pass (Whole Round)")).build());
        } else {
            int maxBid = faction.getMaxBid();
            if (maxBid > game.getBidding().getCurrentBid())
                discordGame.queueMessage("You will auto-pass on this card if the bid gets to " + maxBid + " or higher and then auto-pass on every card this round.");
            else
                discordGame.queueMessage("You will auto-pass on every card this round.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-turn-pass", "Disable Auto-Pass (Whole Round)")).build());
        }
        discordGame.pushGame();
    }

    private static void toggleAutoPass(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        game.getBidding().setAutoPass(game, faction, !faction.isAutoBid());
        String infoChannelName = faction.getName().toLowerCase() + "-info";
        if (!faction.isAutoBid()) {
            discordGame.queueMessage("You will not auto-pass on this card.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.success("bidding-auto-pass", "Enable Auto-Pass")).build());
        } else {
            int maxBid = faction.getMaxBid();
            if (maxBid > game.getBidding().getCurrentBid())
                discordGame.queueMessage("You will auto-pass on this card if the bid gets to " + maxBid + " or higher.");
            else
                discordGame.queueMessage("You will auto-pass on this card.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-auto-pass", "Disable Auto-Pass")).build());
        }
        discordGame.pushGame();
    }
}
