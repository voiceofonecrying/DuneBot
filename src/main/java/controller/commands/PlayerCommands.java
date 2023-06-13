package controller.commands;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.factions.Faction;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase.").addOptions(CommandOptions.amount),
                new SubcommandData("set-auto-bid-policy", "Set policy to either increment or " +
                        "exact.").addOptions(CommandOptions.incrementOrExact),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(CommandOptions.autoPass),
                new SubcommandData("pass", "Pass your turn during a bid.")
        ));

        return commandData;
    }


    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {

        if (gameState.getFactions().stream().noneMatch(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))) {
            return;
        }

        String command = event.getSubcommandName();

        switch (command) {
            case "bid" -> bid(event, discordGame, gameState);
            case "pass" -> pass(event, discordGame, gameState);
            case "set-auto-bid-policy" -> setAutoBidPolicy(event, discordGame, gameState);
            case "set-auto-pass" -> setAutoPass(event, discordGame, gameState);
        }
        discordGame.pushGameState();
    }

    private static void pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction player = gameState.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setBid("pass");
        player.setMaxBid(0);
        player.setAutoBid(true);
        tryBid(event, discordGame, gameState, player);
        player.setAutoBid(false);
    }

    private static void setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        gameState.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get().setAutoBid(event.getOption("enabled").getAsBoolean());
    }

    private static void setAutoBidPolicy(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) {
        gameState.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get().setUseExact(event.getOption("use-exact").getAsBoolean());
    }

    private static void bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction player = gameState.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setMaxBid(event.getOption("amount").getAsInt());
        tryBid(event, discordGame, gameState, player);
    }

    private static void tryBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState, Faction player) throws ChannelNotFoundException {
        if (!gameState.getCurrentBidder().getName().equals(player.getName())) return;

        if (player.getMaxBid() <= gameState.getCurrentBid()) {
            if (!player.isAutoBid()) return;
            player.setBid("pass");
        } else if (player.isUseExactBid()) player.setBid(String.valueOf(player.getMaxBid()));
        else player.setBid(String.valueOf(gameState.getCurrentBid() + 1));
        boolean r = RunCommands.createBidMessage(discordGame, gameState, discordGame.getGameState().getBidOrder(), player);
        if (r) return;
        tryBid(event, discordGame, gameState, discordGame.getGameState().getCurrentBidder());
    }
}