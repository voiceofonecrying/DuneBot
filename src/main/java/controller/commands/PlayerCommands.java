package controller.commands;

import constants.Emojis;
import controller.channels.FactionWhispers;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import controller.DiscordGame;
import model.factions.AtreidesFaction;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static controller.commands.CommandOptions.*;

public class PlayerCommands {


    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("player", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("bid", "Place a bid during bidding phase (silent auction will be exact bid only).").addOptions(incrementOrExact, amount, autoPassAfterMax, outbidAlly),
                new SubcommandData("set-auto-pass", "Enable or disable auto-pass setting.").addOptions(autoPass),
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

    private static String battlePlan(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game, boolean withKH) throws ChannelNotFoundException, InvalidGameStateException {
        String returnString = "";
        Battle currentBattle = game.getBattles().getCurrentBattle();
        if (currentBattle == null)
            throw new InvalidGameStateException("There is no current battle.");
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        if (!currentBattle.getAggressorName().equals(faction.getName()) && !currentBattle.getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException("You are not in the current battle.");
        faction.getLeaders().stream().filter(l -> l.getBattleTerritoryName() != null && l.getBattleTerritoryName().equals(currentBattle.getWholeTerritoryName())).forEach(l -> l.setBattleTerritoryName(null));
        String leaderName = discordGame.required(combatLeader).getAsString();
        Leader leader = null;
        TreacheryCard cheapHero = null;
        if (leaderName.startsWith("Cheap"))
            cheapHero = faction.getTreacheryHand().stream().filter(f -> f.name().equals(leaderName)).findFirst().orElseThrow();
        else if (!leaderName.equals("None")) {
            leader = faction.getLeaders().stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElseThrow();
            Territory battleTerritory = currentBattle.getTerritorySectors().stream()
                    .filter(t -> !t.getForces(faction).isEmpty() || faction instanceof RicheseFaction && t.hasRicheseNoField())
                    .findAny().orElseThrow();
            leader.setBattleTerritoryName(battleTerritory.getTerritoryName());
            faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        }
        String dial = discordGame.required(combatDial).getAsString();
        int decimalPoint = dial.indexOf(".");
        int wholeNumberDial;
        boolean plusHalfDial = false;
        if (decimalPoint == -1)
            wholeNumberDial = Integer.parseInt(dial);
        else {
            wholeNumberDial = decimalPoint == 0 ? 0 : Integer.parseInt(dial.substring(0, decimalPoint));
            if (dial.length() == decimalPoint + 2 && dial.substring(decimalPoint + 1).equals("5"))
                plusHalfDial = true;
            else
                throw new InvalidGameStateException(dial + " is not a valid dial");
        }
        int spice = Integer.parseInt(discordGame.required(combatSpice).getAsString());
        String weaponName = discordGame.required(weapon).getAsString();
        TreacheryCard weapon = null;
        if (!weaponName.equals("None")) {
            weapon = faction.getTreacheryHand().stream().filter(c -> c.name().equals(weaponName)).findFirst().orElseThrow();
        }
        String defenseName = discordGame.required(defense).getAsString();
        TreacheryCard defense = null;
        if (!defenseName.equals("None")) {
            defense = faction.getTreacheryHand().stream().filter(c -> c.name().equals(defenseName)).findFirst().orElseThrow();
        }
        if (faction instanceof AtreidesFaction atreidesFaction) {
            if (withKH && atreidesFaction.getForcesLost() < 7) {
                withKH = false;
                returnString += "Only " + ((AtreidesFaction) faction).getForcesLost() + " " + Emojis.getForceEmoji("Atreides") + " killed in battle. KH has been omitted from the battle plan.\n";
            } else if (withKH && leader == null && cheapHero == null) {
                withKH = false;
                returnString += "You must play a leader or a Cheap Hero to use Kwisatz Haderach. KH has been omitted from the battle plan.\n";
            }
        } else if (withKH) {
            withKH = false;
            returnString += "You are not " + Emojis.ATREIDES + ". KH has been omitted from the battle plan.\n";
        }
        BattlePlan battlePlan = currentBattle.setBattlePlan(game, faction, leader, cheapHero, withKH, wholeNumberDial, plusHalfDial, spice, weapon, defense);
        if (game.hasGameOption(GameOption.STRONGHOLD_SKILLS) && currentBattle.getWholeTerritoryName().equals("Hidden Mobile Stronghold") && faction.hasStrongholdCard("Hidden Mobile Stronghold")) {
            List<String> strongholdNames = faction.getStrongholdCards().stream().map(StrongholdCard::name).filter(n -> !n.equals("Hidden Mobile Stronghold")).toList();
            if (strongholdNames.size() == 1) {
                discordGame.getFactionChat(faction).queueMessage(strongholdNames.get(0) + " Stronghold card will be applied in the HMS battle.");
                if (strongholdNames.get(0).equals("Carthag"))
                    battlePlan.addCarthagStrongholdPower();
            } else if (strongholdNames.size() >= 2) {
                discordGame.getModInfo().queueMessage(faction.getEmoji() + " must select which Stronghold Card they want to apply in the HMS. Please wait to resolve the battle.");
                List<Button> buttons = strongholdNames.stream().map(strongholdName -> Button.primary("hmsstrongholdpower-" + strongholdName, strongholdName)).collect(Collectors.toList());
                discordGame.getFactionChat(faction).queueMessage("Which Stronghold Card would you like to use in the HMS battle?", buttons);
            }
        }
        int availableSpice = faction.getSpice() - spice;
        if (availableSpice > 0 && battlePlan.isSkillBehindAndLeaderAlive("Spice Banker")) {
            discordGame.getModInfo().queueMessage(faction.getEmoji() + " may spend spice to increase leader value with Spice Banker. Please wait to resolve the battle.");
            List<Button> buttons = new ArrayList<>();
            IntStream.range(0, 4).forEachOrdered(i -> {
                Button button = Button.primary("spicebanker-" + i, Integer.toString(i));
                buttons.add(availableSpice >= i ? button : button.asDisabled());
            });
            discordGame.getFactionChat(faction).queueMessage("How much would you like to spend with Spice Banker?", buttons);
        }
//        currentBattle.checkJuiceOfSapho(game, faction);
        return returnString;
    }

    private static String pass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setMaxBid(-1);
        discordGame.getModInfo().queueMessage(faction.getEmoji() + " passed their bid.");
        tryBid(discordGame, game, faction);
        if (faction.isAutoBid() && !game.getBidding().isSilentAuction())
            return "You will auto-pass until the next card or until you set auto-pass to false.";
        return "You will pass one time.";
    }

    private static String setAutoPass(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        boolean enabled = discordGame.required(autoPass).getAsBoolean();
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        faction.setAutoBid(enabled);
        String responseMessage = faction.getEmoji() + " set auto-pass to " + enabled;
        discordGame.getModInfo().queueMessage(responseMessage);
        tryBid(discordGame, game, faction);
        responseMessage = "You set auto-pass to " + enabled + ".";
        if (enabled) {
            responseMessage += "\nYou will auto-pass if the top bid is " + faction.getMaxBid() + " or higher.";
        }
        return responseMessage;
    }

    private static String bid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Faction faction = discordGame.getFactionByPlayer(event.getUser().toString());
        boolean silentAuction = game.getBidding().isSilentAuction();
        boolean useExact = discordGame.required(incrementOrExact).getAsBoolean();
        int bidAmount = discordGame.required(amount).getAsInt();
        if (bidAmount > faction.getSpice() + faction.getAllySpiceBidding()
                && faction.getTreacheryHand().stream().noneMatch(c -> c.name().equals("Karama")))
            throw new InvalidGameStateException("You have insufficient " + Emojis.SPICE + " for this bid and no Karama to avoid paying.");

        faction.setUseExact(useExact);
        faction.setMaxBid(bidAmount);
        String modMessage = faction.getEmoji() + " set their bid to " + (useExact ? "exactly " : "increment up to ") + bidAmount + ".";
        String responseMessage = "You will bid ";
        if (silentAuction) {
            responseMessage += "exactly " + bidAmount + " in the silent auction.";
        } else if (useExact) {
            responseMessage += "exactly " + bidAmount + " if possible.";
        } else {
            responseMessage += "+1 up to " + bidAmount + ".";
        }
        int spiceAvaiable = faction.getSpice() + faction.getAllySpiceBidding();
        if (bidAmount > faction.getSpice() + faction.getAllySpiceBidding())
            responseMessage += "\nIf you win for more than " + spiceAvaiable + ", you will have to use your Karama.";
        if (discordGame.optional(autoPassAfterMax) != null) {
            boolean enableAutoPass = discordGame.optional(autoPassAfterMax).getAsBoolean();
            faction.setAutoBid(enableAutoPass);
            modMessage += enableAutoPass ? " Auto-pass enabled." : " No auto-pass.";
        }
        discordGame.getModInfo().queueMessage(modMessage);
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
                discordGame.getModInfo().queueMessage(responseMessage2);
                discordGame.getFactionChat(faction.getName()).queueMessage(responseMessage2);
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
                discordGame.getModInfo().queueMessage("Use /award-top-bidder to assign card back to " + Emojis.RICHESE + ". Use /richese remove-card to remove it from the game.");
            } else {
                bidding.decrementBidCardNumber();
                discordGame.queueMessage("bidding-phase", "The black market card has been returned to " + Emojis.RICHESE);
                discordGame.getModInfo().queueMessage("The black market card has been returned to " + Emojis.RICHESE);
                discordGame.getModInfo().queueMessage("Use /run advance to continue the bidding phase.");
                CommandManager.awardTopBidder(discordGame, game);
                return true;
            }
        } else {
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
            discordGame.getModInfo().queueMessage("The card has been awarded to " + winnerEmoji);
            discordGame.getModInfo().queueMessage(modMessage);
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
                discordGame.getModInfo().queueMessage(modMessage);
            } else if (topBidderDeclared) {
                discordGame.getModInfo().queueMessage("Use /award-top-bidder to assign card to the winner and pay appropriate recipient.\nUse /award-bid if a Karama affected winner or payment.");
            }

            faction = game.getFaction(bidding.advanceBidder(game));
        } while (!topBidderDeclared && !allPlayersPassed && !onceAroundFinished);
    }

    private static String whisper(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
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
        discordGame.getModInfo().queueMessage(game.getMod() + " " + faction.getEmoji() + " put the game on hold because: " + reason);
        return "";
    }
}