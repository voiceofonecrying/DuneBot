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
        if (event.getComponentId().startsWith("bidding-auto-pass")) toggleAutoPass(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-turn-pass")) toggleAutoPassTurn(event, discordGame, game);
        else if (event.getComponentId().startsWith("bidding-pass")) passOnce(event, discordGame, game);
        game.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
}

    private static void passOnce(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        Faction faction = game.getFaction(event.getComponentId().split("-")[2]);
        game.getBidding().pass(game, faction);
        String infoChannelName = faction.getName().toLowerCase() + "-info";

        discordGame.queueMessage("You will pass the next bid.");
        discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-pass-" + faction.getName(), "Pass One Time")).build());
        discordGame.pushGame();
    }

    private static void toggleAutoPassTurn(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction = game.getFaction(event.getComponentId().split("-")[3]);
        faction.setAutoBidTurn(!faction.isAutoBidTurn());
        String infoChannelName = faction.getName().toLowerCase() + "-info";
        if (!faction.isAutoBidTurn()) {
            discordGame.queueMessage("You will not pass on every card this round.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.success("bidding-turn-pass-" + faction.getName(), "Enable Auto-Pass (Whole Round)")).build());
        } else {
            discordGame.queueMessage("You will pass on every card this round.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-turn-pass-" + faction.getName(), "Disable Auto-Pass (Whole Round)")).build());
        }
        discordGame.pushGame();
    }

    private static void toggleAutoPass(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction = game.getFaction(event.getComponentId().split("-")[3]);
        faction.setAutoBid(!faction.isAutoBid());
        String infoChannelName = faction.getName().toLowerCase() + "-info";
        if (!faction.isAutoBid()) {
            discordGame.queueMessage("You will not pass on this card.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.success("bidding-auto-pass-" + faction.getName(), "Enable Auto-Pass")).build());
        } else {
            discordGame.queueMessage("You will pass on this card.");
            discordGame.queueMessage(infoChannelName, new MessageCreateBuilder().addActionRow(Button.secondary("bidding-auto-pass-" + faction.getName(), "Disable Auto-Pass")).build());
        }
        discordGame.pushGame();
    }

    }
