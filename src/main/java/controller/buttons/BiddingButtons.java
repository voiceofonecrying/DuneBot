package controller.buttons;

import constants.Emojis;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.TreeSet;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class BiddingButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {

        if (!event.getComponentId().startsWith("bid")) return;

        if (event.getComponentId().startsWith("bid-support-")) supportBidding(event, game, discordGame);

    }

    private static void supportBidding(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String support = event.getComponentId().split("-")[2];
        switch (support) {
            case "max" -> {
                game.getFaction(faction.getAlly()).setAllySpiceBidding(faction.getSpice());
                discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support your bidding this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceBidding() + " " + Emojis.SPICE + "!");
                discordGame.queueMessage("You have offered your ally all of your spice to ship with.");
                discordGame.pushGame();
            }
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(faction.getSpice(), 40);
                for (int i = 0; i < limit; i++) {
                    buttonList.add(Button.primary("bid-support-" + (i + 1), Integer.toString(i + 1)));
                }
                arrangeButtonsAndSend("How much would you like to offer in support?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                game.getFaction(faction.getAlly()).setAllySpiceBidding(0);
                discordGame.queueMessage("Resetting bidding support.");
            }
            default -> {
                game.getFaction(faction.getAlly()).setAllySpiceBidding(Integer.parseInt(support.replace("bid-support-", "")));
                discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support your bidding this turn up to " + game.getFaction(faction.getAlly()).getAllySpiceBidding() + " " + Emojis.SPICE + "!");
                discordGame.queueMessage("You have offered your ally " + support.replace("bid-support-", "") + " " + Emojis.SPICE + " to bid with.");
                discordGame.pushGame();
            }
        }
        ButtonManager.deleteAllButtonsInChannel(event.getMessageChannel());
        ShowCommands.sendInfoButtons(game, discordGame, faction);
        discordGame.pushGame();
    }
}
