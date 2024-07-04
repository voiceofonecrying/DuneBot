package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.SetupCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.TreeSet;

import static controller.buttons.ShipmentAndMovementButtons.arrangeButtonsAndSend;
import static controller.buttons.ShipmentAndMovementButtons.getButtonComparator;

public class FactionButtons {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        if (event.getComponentId().startsWith("traitorselection-")) selectTraitor(event, game, discordGame);
        else if (event.getComponentId().startsWith("ally-support-")) allySpiceSupport(event, game, discordGame);
    }

    public static void selectTraitor(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String traitorName = event.getComponentId().split("-")[1];
        faction.selectTraitor(traitorName);
        discordGame.queueMessage("You selected " + traitorName);
        if (game.getFactions().stream().anyMatch(f -> !(f instanceof HarkonnenFaction) && !(f instanceof BTFaction) && f.getTraitorHand().size() != 1)) {
            discordGame.pushGame();
        } else {
            game.getModInfo().publish("All traitors have been selected. Game is auto-advancing.");
            SetupCommands.advance(event.getGuild(), discordGame, game);
        }
    }

    private static void allySpiceSupport(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String forPhases = " for bidding AND shipping";
        if (faction instanceof GuildFaction guild) {
            if (!guild.isAllySpiceForShipping())
                forPhases = " for bidding ONLY";
        } else if (faction instanceof ChoamFaction choam) {
            if (!choam.isAllySpiceForBattle())
                forPhases = " for bidding and shipping ONLY";
            else
                forPhases = " for bidding, shipping, AND battles";
        } else if (faction instanceof EmperorFaction) {
            forPhases = " for bidding, shipping, and battles";
        }
        String support = event.getComponentId().split("-")[2];
        switch (support) {
            case "max" -> {
                faction.setSpiceForAlly(faction.getSpice());
                discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                discordGame.queueMessage("You have offered your ally all of your spice.");
                discordGame.pushGame();
            }
            case "number" -> {
                TreeSet<Button> buttonList = new TreeSet<>(getButtonComparator());
                int limit = Math.min(faction.getSpice(), 40);
                for (int i = 0; i < limit; i++) {
                    buttonList.add(Button.primary("ally-support-" + (i + 1), Integer.toString(i + 1)));
                }
                arrangeButtonsAndSend("How much would you like to offer in support?", buttonList, discordGame);
                return;
            }
            case "reset" -> {
                faction.setSpiceForAlly(0);
                discordGame.queueMessage("You are not offering " + Emojis.SPICE + " support to your ally.");
            }
            case "noshipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(false);
                    discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                    discordGame.queueMessage("You will not support ally shipping cost.");
                }
            }
            case "shipping" -> {
                if (faction instanceof GuildFaction guild) {
                    guild.setAllySpiceForShipping(true);
                    discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                    discordGame.queueMessage("You will support ally shipping cost. That " + Emojis.SPICE + " will go to the bank, not to you.");
                }
            }
            case "nobattles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(false);
                    discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                    discordGame.queueMessage("You will not support ally in battles.");
                }
            }
            case "battles" -> {
                if (faction instanceof ChoamFaction choam) {
                    choam.setAllySpiceForBattle(true);
                    discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                    discordGame.queueMessage("You will support ally in battles. That " + Emojis.SPICE + " will go to the bank, not to you.");
                }
            }
            default -> {
                faction.setSpiceForAlly(Integer.parseInt(support.replace("ally-support-", "")));
                discordGame.getFactionChat(faction.getAlly()).queueMessage("Your ally will support you with " + faction.getSpiceForAlly() + " " + Emojis.SPICE + forPhases + "!");
                discordGame.queueMessage("You have offered your ally " + support.replace("ally-support-", "") + " " + Emojis.SPICE + ".");
                discordGame.pushGame();
            }
        }
        ButtonManager.deleteAllButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }
}
