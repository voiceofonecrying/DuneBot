package controller.commands;

import controller.channels.FactionWhispers;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import controller.DiscordGame;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase (silent auction will be exact bid only).").addOptions(incrementOrExact, amount, autoPassAfterMax, outbidAlly),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(autoPass),
                new SubcommandData("set-auto-pass-entire-turn", "Enable or disable auto-pass setting for the rest of the turn.").addOptions(autoPass),
                new SubcommandData("pass", "Pass your turn during a bid."),
                new SubcommandData("battle-plan", "Submit your plan for the current battle").addOptions(combatLeader, weapon, defense, combatDial, combatSpice),
                new SubcommandData("battle-plan-kh", "Submit your plan using Kwisatz-Haderach for the current battle").addOptions(combatLeader, weapon, defense, combatDial, combatSpice),
                new SubcommandData("whisper", "Whisper to another player.").addOptions(message, whisperFaction),
                new SubcommandData("hold-game", "Prevent the bot from proceeding until mod can resolve your issue.").addOptions(holdgameReason)
        ));

        return commandData;
    }


    public static String runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {

        if (game.getFactions().stream().noneMatch(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))) {
            return "";
        }

        String command = event.getSubcommandName();
        if (command == null) throw new IllegalArgumentException("Invalid command name: null");

        String responseMessage = "";
        switch (command) {
            case "bid" -> responseMessage = bid(event, discordGame, game);
            case "pass" -> responseMessage = pass(event, discordGame, game);
            case "set-auto-pass" -> responseMessage = setAutoPass(event, discordGame, game);
            case "set-auto-pass-entire-turn" -> responseMessage = setAutoPassEntireTurn(event, discordGame, game);
            case "battle-plan" -> responseMessage = battlePlan(event, discordGame, game, false);
            case "battle-plan-kh" -> responseMessage = battlePlanKH(event, discordGame, game);
            case "whisper" -> responseMessage = whisper(event, discordGame, game);
            case "hold-game" -> responseMessage = holdGame(event, discordGame, game);
        }
        discordGame.pushGame();
        return responseMessage;
    }

    private static String battlePlanKH(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        return battlePlan(event, discordGame, game, true);
    }

    private static String battlePlan(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game, boolean kwisatzHaderach) throws ChannelNotFoundException, InvalidGameStateException {
        Battle currentBattle = game.getBattles().getCurrentBattle();
        if (currentBattle == null)
            throw new InvalidGameStateException("There is no current battle.");
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        String leaderName = discordGame.required(combatLeader).getAsString();
        String dial = discordGame.required(combatDial).getAsString();
        int spice = Integer.parseInt(discordGame.required(combatSpice).getAsString());
        String weaponName = discordGame.required(weapon).getAsString();
        String defenseName = discordGame.required(defense).getAsString();
        return currentBattle.setBattlePlan(game, faction, leaderName, kwisatzHaderach, dial, spice, weaponName, defenseName);
    }

    private static String pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        return game.getBidding().pass(game, faction);
    }

    private static String setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        boolean enabled = discordGame.required(autoPass).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        return game.getBidding().setAutoPass(game, faction, enabled);
    }

    private static String setAutoPassEntireTurn(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        boolean enabled = discordGame.required(autoPass).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        return game.getBidding().setAutoPassEntireTurn(game, faction, enabled);
    }

    private static String bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        boolean useExact = discordGame.required(incrementOrExact).getAsBoolean();
        int bidAmount = discordGame.required(amount).getAsInt();
        Boolean newOutbidAllySetting = null;
        if (discordGame.optional(outbidAlly) != null)
            newOutbidAllySetting = discordGame.optional(outbidAlly).getAsBoolean();
        Boolean enableAutoPass = null;
        if (discordGame.optional(autoPassAfterMax) != null)
            enableAutoPass = discordGame.optional(autoPassAfterMax).getAsBoolean();
        return game.getBidding().bid(game, faction, useExact, bidAmount, newOutbidAllySetting, enableAutoPass);
    }

    private static String whisper(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (!game.isWhispersTagged()) {
            game.getWhispers().publish(game.getGameRoleMention());
            game.getWhispers().publish(game.getModOrRoleMention());
            game.setWhispersTagged(true);
        }
        Faction sender = discordGame.getFactionByPlayer(event.getUser().toString());
        Faction recipient;
        if (discordGame.optional(whisperFaction) == null) {
            String channelName = event.getChannel().getName();
            if (channelName.endsWith("-whispers")) {
                String name = channelName.replace("-whispers", "");
                if (name.equals("bg") || name.equals("bt") || name.equals("choam"))
                    name = name.toUpperCase();
                if (name.equals("rich"))
                    name = "Richese";
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                recipient = game.getFaction(name);
            } else {
                throw new IllegalArgumentException("Recipient faction must be specified unless sending from a -whispers thread.");
            }
        } else {
            recipient = game.getFaction(discordGame.required(whisperFaction).getAsString());
        }
        if (sender == recipient)
            throw new IllegalArgumentException("You cannot whisper to yourself.");
        if (sender.getAlly().equals(recipient.getName()) || recipient.getAlly().equals(sender.getName()))
            throw new IllegalArgumentException("Please use your alliance thread to communicate with your ally.");
        String whisperedMessage = discordGame.required(message).getAsString();
        FactionWhispers senderWhispers = discordGame.getFactionWhispers(sender, recipient);
        FactionWhispers recipientWhispers = discordGame.getFactionWhispers(recipient, sender);

        sender.sendWhisper(recipient, whisperedMessage, senderWhispers, recipientWhispers);
        return "";
    }

    private static String holdGame(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String reason = discordGame.required(holdgameReason).getAsString();
        game.setOnHold(true);
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " put the game on hold. Please wait for the mod to resolve the issue.");
        discordGame.getModInfo().queueMessage(game.getModOrRoleMention() + " " + faction.getEmoji() + " put the game on hold because: " + reason);
        return "";
    }
}