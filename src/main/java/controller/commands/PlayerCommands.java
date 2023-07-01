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
                new SubcommandData("bid", "Place a bid during bidding phase.").addOptions(CommandOptions.amount, CommandOptions.outbidAlly),
                new SubcommandData("set-auto-bid-policy", "Set policy to either increment or " +
                        "exact.").addOptions(CommandOptions.incrementOrExact),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(CommandOptions.autoPass),
                new SubcommandData("pass", "Pass your turn during a bid.")
        ));

        return commandData;
    }


    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {

        if (game.getFactions().stream().noneMatch(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))) {
            return;
        }

        String command = event.getSubcommandName();

        switch (command) {
            case "bid" -> bid(event, discordGame, game);
            case "pass" -> pass(event, discordGame, game);
            case "set-auto-bid-policy" -> setAutoBidPolicy(event, discordGame, game);
            case "set-auto-pass" -> setAutoPass(event, discordGame, game);
        }
        discordGame.pushGame();
    }

    private static void pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction player = game.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setMaxBid(-1);
        tryBid(event, discordGame, game, player);
        discordGame.sendMessage("mod-info", player.getEmoji() + " passed their bid.");

    }

    private static void setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean enabled = event.getOption("enabled").getAsBoolean();
        Faction player = game.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setAutoBid(enabled);
        tryBid(event, discordGame, gameState, player);
        discordGame.sendMessage("mod-info", player.getEmoji() + " set auto-pass to " + enabled);
    }

    private static void setAutoBidPolicy(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        boolean useExact = event.getOption("use-exact").getAsBoolean();
        Faction player = game.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setUseExact(useExact);
        discordGame.sendMessage("mod-info", player.getEmoji() + " set auto-bid-policy to " + (useExact ? "exact" : "increment"));
    }

    private static void bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction player = game.getFactions().stream().filter(f -> f.getPlayer().substring(2).replace(">", "").equals(event.getUser().toString().split("=")[1].replace(")", "")))
                .findFirst().get();
        player.setMaxBid(event.getOption("amount").getAsInt());
        discordGame.sendMessage("mod-info", player.getEmoji() + " set their bid to " + event.getOption("amount").getAsInt());
        if (event.getOption("outbid-ally") != null){
            player.setOutbidAlly(event.getOption("outbid-ally").getAsBoolean());
            discordGame.sendMessage("mod-info", player.getEmoji() + " set their outbid ally policy to " + event.getOption("outbid-ally").getAsBoolean());
        }
        tryBid(event, discordGame, game, player);
    }

    private static void tryBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game, Faction player) throws ChannelNotFoundException {
        if (!game.getCurrentBidder().equals(player.getName())) return;

        if (player.getMaxBid() == -1) {
            player.setBid("pass");
            player.setMaxBid(0);
        } else if (player.getMaxBid() <= game.getCurrentBid()) {
            if (!player.isAutoBid()) return;
            player.setBid("pass");
        } else if (!player.isOutbidAlly() && player.hasAlly() && player.getAlly().equals(game.getBidLeader())) player.setBid("pass");
        else if (player.isUseExactBid()) player.setBid(String.valueOf(player.getMaxBid()));
        else player.setBid(String.valueOf(game.getCurrentBid() + 1));
        boolean r = RunCommands.createBidMessage(discordGame, game, discordGame.getGame().getEligibleBidOrder(), player);
        if (r) return;
        tryBid(event, discordGame, game, game.getFaction(game.getCurrentBidder()));
    }
}