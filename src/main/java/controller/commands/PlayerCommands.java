package controller.commands;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Faction;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase.").addOptions(CommandOptions.amount),
                new SubcommandData("set-auto-bid-policy", "Set policy to either increment (bids current bid + 1) or " +
                        "exact (bids whatever your current set bid is).").addOptions(CommandOptions.incrementOrExact),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting. If enabled, bot" +
                        " will pass for you if your bid is lower than current bid on your turn.").addOptions(CommandOptions.autoPass)
        ));

        return commandData;
    }


    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {

        if (gameState.getFactions().stream().noneMatch(f -> f.getUserName().equals(event.getUser().getName()))) {
            return;
        }

        String command = event.getSubcommandName();

        switch (command) {
            case "bid" -> bid(event, discordGame, gameState);
            case "set-auto-bid-policy" -> setAutoBidPolicy(event, discordGame, gameState);
            case "set-auto-pass" -> setAutoPass(event, discordGame, gameState);
        }
    }

    private static void setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        gameState.getFactions().stream().filter(faction -> faction.getUserName().equals(event.getUser().getName()))
                .findFirst().get().setUseExactOrIncrement(event.getOption("setting").getAsString());
    }

    private static void setAutoBidPolicy(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        gameState.getFactions().stream().filter(faction -> faction.getUserName().equals(event.getUser().getName()))
                .findFirst().get().setAutoBid(event.getOption("enabled").getAsBoolean());
    }

    private static void bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction player = gameState.getFactions().stream().filter(faction -> faction.getUserName().equals(event.getUser().getName()))
                .findFirst().get();
        player.setBid(String.valueOf(event.getOption("amount").getAsInt()));
        tryBid(event, discordGame, gameState, player);
    }

    private static void tryBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState, Faction player) throws ChannelNotFoundException {
        if (!gameState.getCurrentBidder().getName().equals(player.getName())) return;

        if (Integer.parseInt(player.getBid()) <= gameState.getCurrentBid() && player.isAutoBid()) {
            String tmp = player.getBid();
            player.setBid("pass");
            RunCommands.createBidMessage(discordGame, gameState, discordGame.getGameState().getBidOrder());
            player.setBid(tmp);
        }
    }
}