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
        // Buttons handled by this class must begin with "battle"
        // And any button that begins with "battle" must be handled by this class
        if (event.getComponentId().startsWith("battle-choose-territory")) chooseTerritory(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-choose-opponent")) chooseOpponent(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-choose-combatant")) ecazChooseCombatant(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-hms-stronghold-power-")) hmsStrongholdPower(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-spice-banker-")) spiceBanker(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-pull-leader-")) pullLeader(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-forces-dialed-")) forcesDialed(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-juice-of-sapho")) juiceOfSaphoDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-portable-snooper")) portableSnooperDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-stone-burner")) stoneBurnerDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-poison-tooth")) poisonToothDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-take-tech-token-")) techTokenDecision(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-harkonnen-keep-captured-leader")) keepCapturedLeader(discordGame, game);
        else if (event.getComponentId().startsWith("battle-harkonnen-kill-captured-leader")) killCapturedLeader(discordGame, game);
        else if (event.getComponentId().startsWith("battle-harkonnen-return-captured-leader")) returnCapturedLeader(discordGame, game);
        else if (event.getComponentId().startsWith("battle-publish-resolution")) publishResolution(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-resolve")) resolveBattle(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-dont-resolve")) dontResolveBattle(event, discordGame);
        else if (event.getComponentId().startsWith("battle-cancel-audit")) cancelAudit(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-emperor-nexus-cunning")) emperorNexusCunning(event, discordGame, game);
        else if (event.getComponentId().startsWith("battle-ix-nexus-cunning")) ixNexusCunning(event, discordGame, game);
    }

    private static void chooseTerritory(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        int battleIndex = Integer.parseInt(event.getComponentId().split("-")[3]);
        String territory = battles.getBattles(game).get(battleIndex).getWholeTerritoryName();
        discordGame.queueMessage("You selected " + territory + ".");
        discordGame.getTurnSummary().queueMessage(battles.getAggressor(game).getEmoji() + " will battle in " + territory + ".");
        BattleCommands.setBattleIndex(discordGame, game, battleIndex);
        discordGame.pushGame();
    }

    private static void chooseOpponent(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        String opponent = event.getComponentId().split("-")[3];
        game.getBattles().setOpponent(game, opponent);
        discordGame.pushGame();
    }

    private static void ecazChooseCombatant(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        String battleFaction = event.getComponentId().split("-")[3];
        discordGame.queueMessage("You selected " + battleFaction + ".");
        discordGame.getTurnSummary().queueMessage(Emojis.getFactionEmoji(battleFaction) + " will be the combatant.");
        battles.getCurrentBattle().setEcazCombatant(game, battleFaction);
        battles.callBattleActions(game);
        discordGame.pushGame();
    }

    private static void hmsStrongholdPower(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String strongholdName = event.getComponentId().split("-")[4];
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
        int spice = Integer.parseInt(event.getComponentId().split("-")[3]);
        discordGame.queueMessage("You will spend " + spice + " " + Emojis.SPICE + " with Spice Banker to increase your leader strength.");
        discordGame.getModInfo().queueMessage(faction.getEmoji() + " will spend " + spice + " " + Emojis.SPICE + " with Spice Banker.");
        Battle currentBattle = game.getBattles().getCurrentBattle();
        BattlePlan plan = faction.getName().equals(currentBattle.getAggressorName()) ? currentBattle.getAggressorBattlePlan() : currentBattle.getDefenderBattlePlan();
        plan.setSpiceBankerSupport(spice);
        if (spice > 0) {
            discordGame.getModInfo().queueMessage(faction.getEmoji() + " updated battle plan:\n" + plan.getPlanMessage(false));
            faction.getChat().publish("Your updated battle plan has been submitted:\n" + plan.getPlanMessage(false));
        }
        currentBattle.spiceBankerDecisionMade();
        discordGame.pushGame();
    }

    private static void pullLeader(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        String factionName = event.getComponentId().split("-")[3];
        String leaderName = event.getComponentId().split("-")[4];
        String yesOrNo = event.getComponentId().split("-")[5];
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
        String factionName = event.getComponentId().split("-")[3];
        int regularDialed = Integer.parseInt(event.getComponentId().split("-")[4]);
        int specialDialed = Integer.parseInt(event.getComponentId().split("-")[5]);
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
            battle.juiceOfSaphoDontAdd(game);
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
            battle.portableSnooperDontAdd(game);
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
            discordGame.queueMessage("You will not use Poison Tooth.");
        } else if (event.getComponentId().equals("battle-poison-tooth-keep")) {
            battle.keepPoisonTooth(game, faction);
            discordGame.queueMessage("You will use Poison Tooth.");
        } else {
            throw new IllegalArgumentException("Button ID is invalid: " + event.getComponentId());
        }
        discordGame.pushGame();
    }

    private static void techTokenDecision(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Faction faction = ButtonManager.getButtonPresser(event, game);
        String ttName = event.getComponentId().replace("battle-take-tech-token-", "");
        game.assignTechToken(ttName, faction);
        discordGame.queueMessage("You took " + Emojis.getTechTokenEmoji(ttName));
        discordGame.pushGame();
    }

    private static void keepCapturedLeader(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battle battle = game.getBattles().getCurrentBattle();
        game.harkonnenKeepLeader(battle.getHarkonnenLeaderVictim(), battle.getHarkonnenCapturedLeader());
        discordGame.queueMessage("You kept " + battle.getHarkonnenCapturedLeader());
        discordGame.pushGame();
    }

    private static void killCapturedLeader(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battle battle = game.getBattles().getCurrentBattle();
        game.harkonnenKillLeader(battle.getHarkonnenLeaderVictim(), battle.getHarkonnenCapturedLeader());
        discordGame.queueMessage("You killed " + battle.getHarkonnenCapturedLeader());
        discordGame.pushGame();
    }

    private static void returnCapturedLeader(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battle battle = game.getBattles().getCurrentBattle();
        String leader = battle.getHarkonnenCapturedLeader();
        battle.returnHarkonnenCapturedLeader(game);
        discordGame.queueMessage("You chose not to capture " + leader);
        discordGame.pushGame();
    }

    private static void publishResolution(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String[] params = event.getComponentId().replace("battle-publish-resolution-turn-", "").split("-");
        int turn = Integer.parseInt(params[0]);
        String wholeTerritoryName = params[1];
        game.getBattles().getCurrentBattle().printBattleResolution(game, true, turn, wholeTerritoryName);
        discordGame.queueDeleteMessage();
        deletePublishResolutionButtonsInChannel(event.getMessageChannel());
        discordGame.pushGame();
    }

    private static void resolveBattle(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String[] params = event.getComponentId().replace("battle-resolve-turn-", "").split("-");
        int turn = Integer.parseInt(params[0]);
        String wholeTerritoryName = params[1];
        game.getBattles().getCurrentBattle().resolveBattle(game, true, turn, wholeTerritoryName);
        discordGame.queueDeleteMessage();
        deletePublishResolutionButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage("Resolving the battle");
        discordGame.pushGame();
    }

    private static void dontResolveBattle(ButtonInteractionEvent event, DiscordGame discordGame) {
        discordGame.queueDeleteMessage();
        deletePublishResolutionButtonsInChannel(event.getMessageChannel());
        discordGame.queueMessage("You will resolve the battle yourself.");
    }

    private static void cancelAudit(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String response = event.getComponentId().replace("battle-cancel-audit-", "");
        Battle battle = game.getBattles().getCurrentBattle();
        if (response.equals("yes")) {
            battle.cancelAudit(game, true);
            discordGame.queueMessage("You will pay to cancel the audit.");
        } else {
            battle.cancelAudit(game, false);
            discordGame.queueMessage("You will not pay to cancel the audit.");
        }
        discordGame.pushGame();
    }

    private static void emperorNexusCunning(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String response = event.getComponentId().replace("battle-emperor-nexus-cunning-", "");
        game.getBattles().getCurrentBattle().emperorNexusCunning(game, response.equals("yes"));
        discordGame.pushGame();
    }

    private static void ixNexusCunning(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String response = event.getComponentId().replace("battle-ix-nexus-cunning-", "");
        game.getBattles().ixNexusCunning(game, response.equals("yes"));
        discordGame.pushGame();
    }

    public static void deletePublishResolutionButtonsInChannel(MessageChannel channel) {
        List<Message> messages = channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory();
        List<Message> messagesToDelete = messages.stream().filter(message -> message.getButtons().stream().map(ActionComponent::getId).anyMatch(id -> id != null && id.startsWith("battle-publish-resolution"))).toList();
        messagesToDelete.forEach(message -> message.delete().complete());
    }
}
