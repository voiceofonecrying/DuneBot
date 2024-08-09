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
        discordGame.pushGame();
    }
}
