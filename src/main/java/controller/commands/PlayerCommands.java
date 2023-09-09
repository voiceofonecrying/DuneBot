package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.factions.Faction;
import model.Game;
import model.Bidding;
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
                new SubcommandData("bid", "Place a bid during bidding phase (use-exact is ignored for silent auctions).").addOptions(incrementOrExact, amount, autoPassAfterMax, outbidAlly),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(autoPass),
                new SubcommandData("pass", "Pass your turn during a bid."),
                new SubcommandData("holdgame", "Prevent the bot from proceeding until mod can resolve your issue.").addOptions(holdgameReason)
        ));

        return commandData;
    }


    public static String runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {

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
            case "hold-game" -> responseMessage = holdGame(event, discordGame, game);
        }
        discordGame.pushGame();
        return responseMessage;
    }

    private static String pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setMaxBid(-1);
        discordGame.queueMessage("mod-info", faction.getEmoji() + " passed their bid.");
        tryBid(discordGame, game, faction);
        if (faction.isAutoBid() && !game.getBidding().isSilentAuction()) return "You will auto-pass until the next card or until you set auto-pass to false.";
        return "You will pass one time.";
    }

    private static String setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        boolean enabled = discordGame.required(autoPass).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setAutoBid(enabled);
        String responseMessage = faction.getEmoji() + " set auto-pass to " + enabled;
        discordGame.queueMessage("mod-info", responseMessage);
        tryBid(discordGame, game, faction);
        responseMessage = "You set auto-pass to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher.";
        }
        return responseMessage;
    }

    private static String bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        boolean silentAuction = game.getBidding().isSilentAuction();
        boolean useExact = discordGame.required(incrementOrExact).getAsBoolean();
        int bidAmount = discordGame.required(amount).getAsInt();
        faction.setUseExact(useExact);
        faction.setMaxBid(bidAmount);
        discordGame.queueMessage("mod-info", faction.getEmoji() + " set their bid to " + bidAmount);
        String responseMessage = "You will bid ";
        if (silentAuction) {
            responseMessage += "exactly " + bidAmount + " in the silent auction.";
        } else if (useExact) {
            responseMessage += "exactly " + bidAmount + " if possible.";
        } else {
            responseMessage += "+1 up to " + bidAmount + ".";
        }
        if (discordGame.optional(autoPassAfterMax) != null) {
            boolean enableAutoPass = discordGame.optional(autoPassAfterMax).getAsBoolean();
            faction.setAutoBid(enableAutoPass);
        }
        String responseMessage2 = "";
        if (!silentAuction) {
            if (faction.isAutoBid()) {
                responseMessage += "\nYou will then auto-pass.";
            } else {
                responseMessage += "\nYou will not auto-pass.\nA new bid or pass will be needed if you are outbid.";
            }
            boolean outbidAllyValue = faction.isOutbidAlly();
            if (discordGame.optional(outbidAlly) != null) {
                outbidAllyValue = discordGame.optional(outbidAlly).getAsBoolean();
                faction.setOutbidAlly(outbidAllyValue);
                responseMessage2 = faction.getEmoji() + " set their outbid ally policy to " + outbidAllyValue;
                discordGame.queueMessage("mod-info", responseMessage2);
                discordGame.queueMessage(faction.getName().toLowerCase() + "-chat", responseMessage2);
            }
            if (faction.hasAlly()) {
                responseMessage2 = "\nYou will" + (outbidAllyValue ? "" : " not") + " outbid your ally";
            }
        }
        tryBid(discordGame, game, faction);
        return responseMessage + responseMessage2;
    }

    private static boolean richeseWinner(DiscordGame discordGame, Game game, boolean allPlayersPassed) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (allPlayersPassed) {
            discordGame.queueMessage("bidding-phase", "All players passed.\n");
            if (bidding.isRicheseCacheCard()) {
                discordGame.queueMessage("bidding-phase", Emojis.RICHESE + " may take cache card for free or remove it from the game.");
                discordGame.queueMessage("mod-info", "Use /award-top-bidder to assign card back to " + Emojis.RICHESE + ". Use /richese remove-card to remove it from the game.");
            } else {
                bidding.decrementBidCardNumber();
                discordGame.queueMessage("bidding-phase", "The black market card has been returned to " + Emojis.RICHESE);
                discordGame.queueMessage("mod-info", "The black market card has been returned to " + Emojis.RICHESE);
                discordGame.queueMessage("mod-info", "Use /run advance to continue the bidding phase.");
                CommandManager.awardTopBidder(discordGame, game);
                return true;
            }
        }
        else {
            String winnerEmoji = game.getFaction(bidding.getBidLeader()).getEmoji();
            discordGame.queueMessage("bidding-phase", winnerEmoji + " has the top bid.");
            String modMessage;
            if (bidding.isRicheseCacheCard()) {
                if (bidding.getBidCardNumber() == bidding.getNumCardsForBid()) {
                    modMessage = "Use /run advance to end the bidding phase.";
                } else {
                    modMessage = "Use /run bidding to put the next card up for bid.";
                }
            } else {
                modMessage = "Use /run advance to continue the bidding phase.";
            }
            CommandManager.awardTopBidder(discordGame, game);
            discordGame.queueMessage("mod-info", "The card has been awarded to " + winnerEmoji);
            discordGame.queueMessage("mod-info", modMessage);
            return true;
        }
        return false;
    }

    private static void tryBid(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        List<String> eligibleBidOrder = bidding.getEligibleBidOrder(game);
        if (eligibleBidOrder.isEmpty() && !bidding.isSilentAuction()) {
            throw new InvalidGameStateException("All hands are full.");
        }
        if (bidding.isSilentAuction()) {
            if (faction.getMaxBid() == -1) {
                faction.setBid("pass");
                faction.setMaxBid(0);
            } else {
                faction.setBid(String.valueOf(faction.getMaxBid()));
                if (faction.getMaxBid() > bidding.getCurrentBid()) {
                    bidding.setCurrentBid(Integer.parseInt(faction.getBid()));
                    bidding.setBidLeader(faction.getName());
                }
            }
            boolean allHaveBid = true;
            for (String factionName : bidding.getEligibleBidOrder(game)) {
                Faction f = game.getFaction(factionName);
                if (f.getBid().isEmpty()) {
                    allHaveBid = false;
                    bidding.setCurrentBid(0);
                    bidding.setBidLeader("");
                    break;
                }
                if (f.getMaxBid() > bidding.getCurrentBid()) {
                    bidding.setCurrentBid(Integer.parseInt(f.getBid()));
                    bidding.setBidLeader(factionName);
                }
            }
            if (allHaveBid) {
                RunCommands.createBidMessage(discordGame, game, false);
                richeseWinner(discordGame, game, bidding.getCurrentBid() == 0);
            }
            return;
        }
        if (!bidding.getCurrentBidder().equals(faction.getName())) return;
        boolean topBidderDeclared = false;
        boolean onceAroundFinished = false;
        boolean allPlayersPassed = false;
        do {
            if (!faction.isOutbidAlly() && faction.hasAlly() && faction.getAlly().equals(bidding.getBidLeader())) {
                faction.setBid("pass (ally had top bid)");
            } else if (faction.getMaxBid() == -1) {
                faction.setBid("pass");
                faction.setMaxBid(0);
            } else if (faction.getMaxBid() <= bidding.getCurrentBid()) {
                if (!faction.isAutoBid()) return;
                faction.setBid("pass");
            } else {
                if (faction.isUseExactBid()) faction.setBid(String.valueOf(faction.getMaxBid()));
                else faction.setBid(String.valueOf(bidding.getCurrentBid() + 1));
                bidding.setCurrentBid(Integer.parseInt(faction.getBid()));
                bidding.setBidLeader(faction.getName());
            }

            boolean tag = true;
            if (bidding.getCurrentBidder().equals(eligibleBidOrder.get(eligibleBidOrder.size() - 1))) {
                if (bidding.isRicheseBidding()) onceAroundFinished = true;
                if (bidding.getBidLeader().isEmpty()) allPlayersPassed = true;
                if (onceAroundFinished || allPlayersPassed) tag = false;
            }
            if (!bidding.isSilentAuction())
                topBidderDeclared = RunCommands.createBidMessage(discordGame, game, tag);

            if (onceAroundFinished) {
                if (richeseWinner(discordGame, game, allPlayersPassed))
                    return;
            } else if (allPlayersPassed) {
                discordGame.queueMessage("bidding-phase", "All players passed. " + Emojis.TREACHERY + " cards will be returned to the deck.");
                String modMessage = "Use /run advance to return the " + Emojis.TREACHERY + " cards to the deck";
                if (bidding.isRicheseCacheCardOutstanding())
                    modMessage += ". Then use /richese card-bid to auction the " + Emojis.RICHESE + " cache card.";
                else
                    modMessage += " and end the bidding phase.";
                discordGame.queueMessage("mod-info", modMessage);
            } else if (topBidderDeclared) {
                discordGame.queueMessage("mod-info", "Use /award-top-bidder to assign card to the winner and pay appropriate recipient.\nUse /award-bid if a Karama affected winner or payment.");
            }

            faction = game.getFaction(bidding.advanceBidder(game));
        } while (!topBidderDeclared && !allPlayersPassed && !onceAroundFinished);
    }

    private static String holdGame(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String reason = discordGame.required(holdgameReason).getAsString();
        game.setOnHold(true);
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        discordGame.queueMessage("turn-summary", faction.getEmoji() + " put the game on hold. Please wait for the mod to resolve the issue.");
        discordGame.queueMessage("mod-info", game.getMod() + " " + faction.getEmoji() + " put the game on hold because: " + reason);
        return "";
    }
}