package controller.commands;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.factions.Faction;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase.").addOptions(CommandOptions.amount, CommandOptions.outbidAlly),
                new SubcommandData("set-auto-bid-policy", "Set policy to either increment or " +
                        "exact.").addOptions(incrementOrExact),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(autoPass),
                new SubcommandData("pass", "Pass your turn during a bid.")
        ));

        return commandData;
    }


    public static String runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {

        if (game.getFactions().stream().noneMatch(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))) {
            return "";
        }

        String command = event.getSubcommandName();
        if (command == null) throw new IllegalArgumentException("Invalid command name: null");

        String responseMessage = "";
        switch (command) {
            case "bid" -> responseMessage = bid(event, discordGame, game);
            case "pass" -> responseMessage = pass(event, discordGame, game);
            case "set-auto-bid-policy" -> responseMessage = setAutoBidPolicy(event, discordGame);
            case "set-auto-pass" -> responseMessage = setAutoPass(event, discordGame, game);
        }
        discordGame.pushGame();
        return responseMessage;
    }

    private static String pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setMaxBid(-1);
        tryBid(discordGame, game, faction);
        discordGame.sendMessage("mod-info", faction.getEmoji() + " passed their bid.");
        if (faction.isAutoBid()) return "You will auto-pass until the next card or until you set auto-pass to false.";
        return "You will pass one time.";
    }

    private static String setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        boolean enabled = discordGame.required(autoPass).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setAutoBid(enabled);
        tryBid(discordGame, game, faction);
        String responseMessage = faction.getEmoji() + " set auto-pass to " + enabled;
        discordGame.sendMessage("mod-info", responseMessage);
        responseMessage = "You set auto-pass to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher.";
        }
        return responseMessage;
    }

    private static String setAutoBidPolicy(SlashCommandInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException, IOException {
        boolean useExact = discordGame.required(incrementOrExact).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setUseExact(useExact);
        String responseMessage = faction.getEmoji() + " set their auto-bid policy to " + (useExact ? "exact" : "increment");
        discordGame.sendMessage("mod-info", responseMessage);
        discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", responseMessage);

        responseMessage = "You set your auto-bid policy to " + (useExact ? "exact" : "increment");
        int bidAmount = faction.getMaxBid();
        if (bidAmount != 0) {
            responseMessage += ". You will bid ";
            if (faction.isUseExactBid()) {
                responseMessage += "+1 up to " + bidAmount + ".";
            } else {
                responseMessage += "exactly " + bidAmount + " if possible.";
            }
        }
        return responseMessage;
    }

    private static String bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        int bidAmount = discordGame.required(amount).getAsInt();
        faction.setMaxBid(bidAmount);
        discordGame.sendMessage("mod-info", faction.getEmoji() + " set their bid to " + bidAmount);
        String responseMessage = "You will bid ";
        if (faction.isUseExactBid()) {
            responseMessage += "exactly " + bidAmount + " if possible.";
        } else {
            responseMessage += "+1 up to " + bidAmount + ".";
        }
        if (faction.isAutoBid()) {
            responseMessage += "\nYou will then auto-pass.";
        } else {
            responseMessage += "\nYou will not auto-pass.\nA new bid or pass will be needed if you are outbid.";
        }
        String responseMessage2 = "";
        if (discordGame.optional(outbidAlly) != null) {
            boolean outbidAllyValue = discordGame.optional(outbidAlly).getAsBoolean();
            faction.setOutbidAlly(outbidAllyValue);
            responseMessage2 = faction.getEmoji() + " set their outbid ally policy to " + outbidAllyValue;
            discordGame.sendMessage("mod-info", responseMessage2);
            discordGame.sendMessage(faction.getName().toLowerCase() + "-chat", responseMessage2);
            responseMessage2 = "\nYou will" + (outbidAllyValue ? "" : "not") + " outbid your ally";
        }
        tryBid(discordGame, game, faction);
        return responseMessage + responseMessage2;
    }

    private static void tryBid(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, IOException {
        if (!game.getCurrentBidder().equals(faction.getName())) return;

        if (faction.getMaxBid() == -1) {
            faction.setBid("pass");
            faction.setMaxBid(0);
        } else if (faction.getMaxBid() <= game.getCurrentBid()) {
            if (!faction.isAutoBid()) return;
            faction.setBid("pass");
        } else if (!faction.isOutbidAlly() && faction.hasAlly() && faction.getAlly().equals(game.getBidLeader())) faction.setBid("pass");
        else if (faction.isUseExactBid()) faction.setBid(String.valueOf(faction.getMaxBid()));
        else faction.setBid(String.valueOf(game.getCurrentBid() + 1));
        boolean r = RunCommands.createBidMessage(discordGame, game, discordGame.getGame().getEligibleBidOrder(), faction);
        if (r) return;
        tryBid(discordGame, game, game.getFaction(game.getCurrentBidder()));
    }
}