package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.BattleCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;

import java.text.MessageFormat;
import java.util.List;

public class BattleButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        if (event.getComponentId().startsWith("chooseterritory")) chooseTerritory(event, discordGame, game);
        else if (event.getComponentId().startsWith("chooseopponent")) chooseOpponent(event, discordGame, game);
        else if (event.getComponentId().startsWith("choosecombatant")) ecazChooseCombatant(event, discordGame, game);
        else if (event.getComponentId().startsWith("hmsstrongholdpower-")) hmsStrongholdPower(event, discordGame, game);
        else if (event.getComponentId().startsWith("spicebanker-")) spiceBanker(event, discordGame, game);
        else if (event.getComponentId().startsWith("pullleader-")) pullLeader(event, discordGame, game);
        else if (event.getComponentId().startsWith("forcesdialed-")) forcesDialed(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-juice-of-sapho")) juiceOfSaphoDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-portable-snooper")) portableSnooperDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-stone-burner")) stoneBurnerDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-poison-tooth")) poisonToothDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-publish-resolution")) publishResolution(event, discordGame, game);
    }

    private static void chooseTerritory(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        int battleIndex = Integer.parseInt(event.getComponentId().split("-")[1]);
        String territory = battles.getBattles(game).get(battleIndex).getWholeTerritoryName();
        discordGame.queueMessage("You selected " + territory + ".");
        discordGame.getTurnSummary().queueMessage(battles.getAggressor(game).getEmoji() + " will battle in " + territory + ".");
        BattleCommands.setBattleIndex(discordGame, game, battleIndex);
    }

    private static void chooseOpponent(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        String opponent = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + opponent + ".");
        discordGame.getTurnSummary().queueMessage(battles.getAggressor(game).getEmoji() + " will battle against " + opponent + ".");
        BattleCommands.setOpponent(discordGame, game, opponent);
    }

    private static void ecazChooseCombatant(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        String battleFaction = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + battleFaction + ".");
        discordGame.getTurnSummary().queueMessage(Emojis.getFactionEmoji(battleFaction) + " will be the combatant.");
        battles.getCurrentBattle().setEcazCombatant(game, battleFaction);
        battles.callBattleActions(game);
        discordGame.pushGame();
    }

    private static void hmsStrongholdPower(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String strongholdName = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + strongholdName + " Stronghold Card.");
        discordGame.getModInfo().queueMessage(faction.getEmoji() + " selected " + strongholdName + " Stronghold Card for HMS battle.");
        faction.setHmsStrongholdProxy(new StrongholdCard(strongholdName));
        Battle currentBattle = game.getBattles().getCurrentBattle();
        if (strongholdName.equals("Carthag"))
            currentBattle.addCarthagStrongholdPower(faction);
        currentBattle.hmsCardDecisionMade();
        discordGame.pushGame();
    }

    private static void spiceBanker(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        int spice = Integer.parseInt(event.getComponentId().split("-")[1]);
        discordGame.queueMessage("You will spend " + spice + " " + Emojis.SPICE + " with Spice Banker to increase your leader strength.");
        discordGame.getModInfo().queueMessage(faction.getEmoji() + " will spend " + spice + " " + Emojis.SPICE + " with Spice Banker.");
        Battle currentBattle = game.getBattles().getCurrentBattle();
        BattlePlan plan = faction.getName().equals(currentBattle.getAggressorName()) ? currentBattle.getAggressorBattlePlan() : currentBattle.getDefenderBattlePlan();
        plan.setSpiceBankerSupport(spice);
        if (spice > 0) {
            discordGame.getModInfo().queueMessage(faction.getEmoji() + " updated battle plan:\n" + plan.getPlanMessage(false));
            discordGame.getFactionChat(faction).queueMessage("Your updated battle plan has been submitted:\n" + plan.getPlanMessage(false));
        }
        currentBattle.spiceBankerDecisionMade();
        discordGame.pushGame();
    }

    private static void pullLeader(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        String factionName = event.getComponentId().split("-")[1];
        String leaderName = event.getComponentId().split("-")[2];
        String yesOrNo = event.getComponentId().split("-")[3];
        Faction faction = game.getFaction(factionName);
        Leader leader = faction.getSkilledLeaders().stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElseThrow();
        if (yesOrNo.equals("yes")) {
            leader.setPulledBehindShield(true);
            discordGame.queueMessage(MessageFormat.format(
                    "You pulled {0} {1} behind your shield.",
                    leader.getSkillCard().name(), leader.getName()));
            discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                    "{0} pulled {1} {2} behind their shield.",
                    faction.getEmoji(), leader.getSkillCard().name(), leader.getName()));
        } else {
            leader.setPulledBehindShield(false);
            discordGame.queueMessage(MessageFormat.format(
                    "You left {0} {1} in front of your shield.",
                    leader.getSkillCard().name(), leader.getName()));
            discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                    "{0} left {1} {2} in front of their shield.",
                    faction.getEmoji(), leader.getSkillCard().name(), leader.getName()));
        }
        discordGame.pushGame();
    }

    private static void forcesDialed(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        String factionName = event.getComponentId().split("-")[1];
        int regularDialed = Integer.parseInt(event.getComponentId().split("-")[2]);
        int specialDialed = Integer.parseInt(event.getComponentId().split("-")[3]);
        Battle battle = game.getBattles().getCurrentBattle();
        discordGame.queueMessage(battle.updateTroopsDialed(game, factionName, regularDialed, specialDialed));
        discordGame.pushGame();
    }

    private static void juiceOfSaphoDecision(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Battle battle = game.getBattles().getCurrentBattle();
        if (event.getComponentId().equals("battle-juice-of-sapho-add")) {
            battle.juiceOfSaphoAdd(game, faction);
            discordGame.queueMessage("You will play Juice of Sapho.");
        } else if (event.getComponentId().equals("battle-juice-of-sapho-don't-add")) {
            battle.juiceOfSaphoDontAdd();
            discordGame.queueMessage("You will not play Juice of Sapho.");
        } else {
            throw new IllegalArgumentException("Button ID is invalid: " + event.getComponentId());
        }
        discordGame.pushGame();
    }

    private static void portableSnooperDecision(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Battle battle = game.getBattles().getCurrentBattle();
        if (event.getComponentId().equals("battle-portable-snooper-add")) {
            battle.portableSnooperAdd(game, faction);
            discordGame.queueMessage("You will add Portable Snooper to your plan.");
        } else if (event.getComponentId().equals("battle-portable-snooper-don't-add")) {
            battle.portableSnooperDontAdd();
            discordGame.queueMessage("You will not add Portable Snooper.");
        } else {
            throw new IllegalArgumentException("Button ID is invalid: " + event.getComponentId());
        }
        discordGame.pushGame();
    }

    private static void stoneBurnerDecision(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Battle battle = game.getBattles().getCurrentBattle();
        if (event.getComponentId().equals("battle-stone-burner-no-kill")) {
            battle.stoneBurnerNoKill(game, faction);
            discordGame.queueMessage("You will not kill both leaders.");
        } else if (event.getComponentId().equals("battle-stone-burner-kill")) {
            battle.stoneBurnerKill(game, faction);
            discordGame.queueMessage("You will kill both leaders.");
        } else {
            throw new IllegalArgumentException("Button ID is invalid: " + event.getComponentId());
        }
        discordGame.pushGame();
    }

    private static void poisonToothDecision(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        Battle battle = game.getBattles().getCurrentBattle();
        if (event.getComponentId().equals("battle-poison-tooth-remove")) {
            battle.removePoisonTooth(game, faction);
            discordGame.queueMessage("You removed Poison Tooth from your plan.");
        } else if (event.getComponentId().equals("battle-poison-tooth-keep")) {
            battle.keepPoisonTooth(game, faction);
            discordGame.queueMessage("You kept Poison Tooth in your plan.");
        } else {
            throw new IllegalArgumentException("Button ID is invalid: " + event.getComponentId());
        }
        discordGame.pushGame();
    }

    private static void publishResolution(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String[] params = event.getComponentId().replace("battle-publish-resolution-turn-", "").split("-");
        int turn = Integer.parseInt(params[0]);
        String wholeTerritoryName = params[1];
        boolean playedJuiceOfSapho = params[2].equals("true");
        boolean noKillStoneBurner = params[3].equals("true");
        boolean portableSnooper = params[4].equals("true");
        boolean noPoisonTooth = params[5].equals("true");
        boolean overrideDecisions = params[6].equals("true");
        game.getBattles().getCurrentBattle().battleResolution(game, true, playedJuiceOfSapho, noKillStoneBurner, portableSnooper, noPoisonTooth, overrideDecisions);
        discordGame.queueDeleteMessage();
        deletePublishResolutionButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage("Published to turn summary");
    }

    public static void deletePublishResolutionButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> message.getButtons().stream().map(ActionComponent::getId).anyMatch(id -> id != null && id.startsWith("battle-publish-resolution"))).toList();
        messagesToDelete.forEach(message -> message.delete().complete());
    }
}
