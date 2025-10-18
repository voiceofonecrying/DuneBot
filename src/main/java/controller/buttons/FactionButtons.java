package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.channels.FactionWhispers;
import controller.commands.SetupCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.TreeSet;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class FactionButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("faction-ally-support-")) allySpiceSupport(event, game, discordGame);
        else if (event.getComponentId().startsWith("ally-support-")) allySpiceSupport(event, game, discordGame);
        else if (event.getComponentId().startsWith("faction-whisper-")) whisper(event, game, discordGame);
        else if (event.getComponentId().startsWith("faction-storm-dial-")) stormDial(event, game, discordGame);
        else if (event.getComponentId().startsWith("faction-charity-")) charity(event, game, discordGame);
        else if (event.getComponentId().equals("faction-pay-extortion")) payExtortion(event, game, discordGame);
        else if (event.getComponentId().equals("faction-decline-extortion")) declineExtortion(event, game, discordGame);
        else if (event.getComponentId().startsWith("faction-restore-to-player-")) restoreFactionToPlayer(event, game, discordGame);
    }

    private static void allySpiceSupport(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String support = event.getComponentId().split("-")[3];
        if (event.getComponentId().startsWith("ally-support-"))
            support = event.getComponentId().split("-")[3];
        String ally = faction.getAlly();
        switch (support) {
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(faction.getSpice(), 40);
                for (int i = 0; i <= limit; i++)
                    buttonList.add(Button.primary("faction-ally-support-" + i + "-number", i + (i == faction.getSpiceForAlly() ? " (Current)" : "")));
                arrangeButtonsAndSend("How much would you like to offer in support?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                faction.setSpiceForAlly(0);
                game.getFaction(ally).getChat().publish("Your ally has removed " + Emojis.SPICE + " support.");
            }
            case "noshipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(false);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                }
            }
            case "shipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(true);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                }
            }
            case "nobattles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(false);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                }
            }
            case "battles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(true);
                    game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
                }
            }
            default -> {
                if (event.getComponentId().startsWith("ally-support-"))
                    faction.setSpiceForAlly(Integer.parseInt(support.replace("ally-support-", "")));
                else
                    faction.setSpiceForAlly(Integer.parseInt(support.replace("faction-ally-support-", "")));
                game.getFaction(ally).getChat().publish("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + faction.getSpiceSupportPhasesString());
            }
        }
        ShowCommands.sendAllySpiceSupportButtons(discordGame, game, faction, true);
        discordGame.pushGame();
    }

    private static void whisper(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String recipient = event.getComponentId().split("-")[2];
        String action = event.getComponentId().split("-")[3];
        if (action.equals("yes")) {
            String message = event.getComponentId().replace("faction-whisper-" + recipient + "-yes-", "");
            switch (recipient) {
                case "bg" -> recipient = "BG";
                case "bt" -> recipient = "BT";
                case "choam" -> recipient = "CHOAM";
                default -> StringUtils.capitalize(recipient);
            }
            Faction recipientFaction = game.getFaction(recipient);
            FactionWhispers senderWhispers = discordGame.getFactionWhispers(faction, recipientFaction);
            FactionWhispers recipientWhispers = discordGame.getFactionWhispers(recipientFaction, faction);
            faction.sendWhisper(recipientFaction, message, senderWhispers, recipientWhispers);
            discordGame.queueMessage("Message has been sent as a whisper.");
            discordGame.queueDeleteMessage();
        } else {
            discordGame.queueMessage("Message was not sent as a whisper.");
            discordGame.queueDeleteMessage();
        }
    }

    private static void stormDial(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        int dial = Integer.parseInt(event.getComponentId().replace("faction-storm-dial-", ""));
        discordGame.queueMessage("You dialed " + dial + ".");
        if (game.setStormDial(faction, dial))
            SetupCommands.advance(event.getGuild(), discordGame, game);
        else
            discordGame.pushGame();
    }

    private static void charity(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String action = event.getComponentId().replace("faction-charity-", "");
        faction.setDecliningCharity(action.equals("decline"));
        ShowCommands.sendCharityAction(discordGame, faction, true);
        discordGame.pushGame();
    }

    private static void payExtortion(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.payExtortion();
        discordGame.pushGame();
    }

    private static void declineExtortion(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        faction.declineExtortion();
        discordGame.pushGame();
    }

    private static void restoreFactionToPlayer(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        String factionName = event.getComponentId().split("-")[4];
        String playerName = event.getComponentId().split("-")[5];
        Faction faction = game.getFaction(factionName);
        faction.setPlayer(playerName);
        discordGame.queueMessage(faction.getEmoji() + " has been restored to " + faction.getUserName());
        discordGame.pushGame();
    }
}
