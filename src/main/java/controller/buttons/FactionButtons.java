package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.channels.FactionWhispers;
import controller.commands.SetupCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battle;
import model.Game;
import model.factions.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class FactionButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("traitor-selection-")) selectTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-call-")) callTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-discard-")) discardTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("traitor-reveal-and-discard-")) revealAndDiscardTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("ally-support-")) allySpiceSupport(event, game, discordGame);
        else if (event.getComponentId().startsWith("whisper-")) whisper(event, game, discordGame);
    }

    public static void selectTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-selection-", "");
        faction.selectTraitor(traitorName);
        discordGame.queueMessage("You selected " + traitorName);
        if (game.getFactions().stream().anyMatch(f -> !(f instanceof HarkonnenFaction) && !(f instanceof BTFaction) && f.getTraitorHand().size() != 1)) {
            discordGame.pushGame();
        } else {
            game.getModInfo().publish("All traitors have been selected. Game is auto-advancing.");
            SetupCommands.advance(event.getGuild(), discordGame, game);
        }
    }

    public static void callTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String[] params = event.getComponentId().replace("traitor-call-", "").split("-");
        String response = params[0];
        int turn = Integer.parseInt(params[2]);
        String wholeTerritoryName = params[3];
        discordGame.queueDeleteMessage();
        Battle battle = game.getBattles().getCurrentBattle();
        switch (response) {
            case "wait" -> {
                discordGame.queueMessage("You will wait for battle wheels to decide.");
                if (battle.mightCallTraitor(game, faction, turn, wholeTerritoryName))
                    discordGame.pushGame();
            }
            case "yes" -> {
                if (battle.isResolutionPublished())
                    discordGame.queueMessage("You will call Traitor.");
                else
                    discordGame.queueMessage("You will call Traitor if possible.");
                battle.willCallTraitor(game, faction, true, turn, wholeTerritoryName);
                deleteTraitorCallButtonsInChannel(event.getMessageChannel());
                discordGame.pushGame();
            }
            case "no" -> {
                discordGame.queueMessage("You will not call Traitor.");
                battle.willCallTraitor(game, faction, false, turn, wholeTerritoryName);
                deleteTraitorCallButtonsInChannel(event.getMessageChannel());
                discordGame.pushGame();
            }
        }
    }

    public static void deleteTraitorCallButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> message.getButtons().stream().map(ActionComponent::getId).anyMatch(id -> id != null && id.startsWith("traitor-call"))).toList();
        messagesToDelete.forEach(message -> message.delete().complete());
    }

    public static void discardTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-discard-", "");
        faction.discardTraitor(traitorName, false);
        discordGame.queueMessage("You discarded " + traitorName);
        discordGame.pushGame();
    }

    public static void revealAndDiscardTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().replace("traitor-reveal-and-discard-", "");
        faction.discardTraitor(traitorName, true);
        discordGame.queueMessage("You revealed and discarded " + traitorName);
        discordGame.pushGame();
    }

    private static void allySpiceSupport(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String support = event.getComponentId().split("-")[2];
        String ally = faction.getAlly();
        switch (support) {
            case "max" -> {
                // This block can be removed when games D66, D67, D69, and D70 have completed
                faction.setSpiceForAlly(faction.getSpice());
                game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                discordGame.queueMessage("You have offered your ally all of your spice.");
            }
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(faction.getSpice(), 40);
                for (int i = 0; i <= limit; i++)
                    buttonList.add(Button.primary("ally-support-" + i + "-number", i + (i == faction.getSpiceForAlly() ? " (Current)" : "")));
                arrangeButtonsAndSend("How much would you like to offer in support?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                faction.setSpiceForAlly(0);
                game.getFaction(ally).getChat().publish("Your ally has removed " + Emojis.SPICE + " support.");
                discordGame.queueMessage("You are not offering " + Emojis.SPICE + " support to your ally.");
            }
            case "noshipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(false);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                    discordGame.queueMessage("You will not support ally shipping cost.");
                }
            }
            case "shipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(true);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                    discordGame.queueMessage("You will support ally shipping cost. That " + Emojis.SPICE + " will go to the bank, not to you.");
                }
            }
            case "nobattles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(false);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                    discordGame.queueMessage("You will not support ally in battles.");
                }
            }
            case "battles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(true);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                    discordGame.queueMessage("You will support ally in battles. That " + Emojis.SPICE + " will go to the bank, not to you.");
                }
            }
            default -> {
                faction.setSpiceForAlly(Integer.parseInt(support.replace("ally-support-", "")));
                game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                discordGame.queueMessage("You have offered your ally " + support.replace("ally-support-", "") + " " + Emojis.SPICE + ".");
            }
        }
        ShowCommands.showFactionInfo(ally, discordGame);
        discordGame.pushGame();
    }

    private static void whisper(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String recipient = event.getComponentId().split("-")[1];
        String action = event.getComponentId().split("-")[2];
        if (action.equals("yes")) {
            String message = event.getComponentId().replace("whisper-" + recipient + "-yes-", "");
            switch (recipient) {
                case "bg" -> recipient = "BG";
                case "bt" -> recipient = "BT";
                case "choam" -> recipient = "CHOAM";
                default -> StringUtils.capitalize(recipient);
            }
            FactionWhispers senderWhispers = discordGame.getFactionWhispers(faction, game.getFaction(recipient));
            FactionWhispers recipientWhispers = discordGame.getFactionWhispers(game.getFaction(recipient), faction);
            faction.sendWhisper(faction, message, senderWhispers, recipientWhispers);
            discordGame.queueMessage("Message has been sent as a whisper.");
            discordGame.queueDeleteMessage();
        } else {
            discordGame.queueMessage("Message was not sent as a whisper.");
            discordGame.queueDeleteMessage();
        }
    }
}
