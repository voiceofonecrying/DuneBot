package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.BattleCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battles;
import model.Game;
import model.Leader;
import model.StrongholdCard;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.text.MessageFormat;

public class BattleButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        if (event.getComponentId().startsWith("chooseterritory")) chooseTerritory(event, discordGame, game);
        else if (event.getComponentId().startsWith("chooseopponent")) chooseOpponent(event, discordGame, game);
        else if (event.getComponentId().startsWith("choosecombatant")) ecazChooseCombatant(event, discordGame, game);
        else if (event.getComponentId().startsWith("hmsstrongholdpower-")) hmsStrongholdPower(event, discordGame, game);
        else if (event.getComponentId().startsWith("pullleader-")) pullLeader(event, discordGame, game);
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
        if (strongholdName.equals("Carthag")) {
            Battles battles = game.getBattles();
            battles.getCurrentBattle().addCarthagStrongholdPower(faction);
        }
        discordGame.pushGame();
    }

    private static void pullLeader(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        String factionName = event.getComponentId().split("-")[1];
        String leaderName = event.getComponentId().split("-")[2];
        String yesOrNo = event.getComponentId().split("-")[3];
        discordGame.queueMessage("You selected " + yesOrNo + ".");
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
}
