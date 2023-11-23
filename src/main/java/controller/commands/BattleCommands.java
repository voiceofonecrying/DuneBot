package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battle;
import model.Battles;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.LinkedList;
import java.util.List;

public class BattleCommands {
    public static void setupBattle(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Battles battles = game.getBattles();
        battles.nextBattle(game);
        if (battles.aggressorMustChooseBattle()) territoryButtons(discordGame, battles);
        else setBattleIndex(discordGame, game, 0);
    }

    public static void territoryButtons(DiscordGame discordGame, Battles battles) throws ChannelNotFoundException {
        Faction aggressor = battles.getAggressor();
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (Battle battle : battles.getAggressorsBattles()) {
            buttons.add(Button.primary("chooseterritory-" + i++, battle.getWholeTerritoryName()));
        }
        discordGame.getFactionChat(aggressor.getName()).queueMessage("Where would you like to battle? " + aggressor.getPlayer(), buttons);
    }

    public static void setBattleIndex(DiscordGame discordGame, Game game, int battleIndex) throws InvalidGameStateException, ChannelNotFoundException {
        Battles battles = game.getBattles();
        battles.setTerritoryByIndex(battleIndex);
        if (battles.aggressorMustChooseOpponent()) opponentButtons(discordGame, battles);
        else battles.callBattleActions(game);
        discordGame.pushGame();
    }

    public static void opponentButtons(DiscordGame discordGame, Battles battles) throws ChannelNotFoundException {
        Faction aggressor = battles.getAggressor();
        List<Button> buttons = new LinkedList<>();
        for (Faction faction : battles.getCurrentBattle().getFactions()) {
            if (faction == aggressor) continue;
            buttons.add(Button.primary("chooseopponent-" + faction.getName(), faction.getName()));
        }
        discordGame.getFactionChat(aggressor.getName()).queueMessage("Whom would you like to battle? " + aggressor.getPlayer(), buttons);
    }

    public static void setOpponent(DiscordGame discordGame, Game game, String factionName) throws ChannelNotFoundException, InvalidGameStateException {
        Battles battles = game.getBattles();
        battles.setOpponent(factionName);
        battles.callBattleActions(game);
        discordGame.pushGame();
    }
}
