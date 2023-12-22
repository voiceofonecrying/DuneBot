package controller.buttons;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.BattleCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battles;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class BattleButtons implements Pressable {
    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, InvalidGameStateException {
        if (event.getComponentId().startsWith("chooseterritory")) chooseTerritory(event, discordGame, game);
        else if (event.getComponentId().startsWith("chooseopponent")) chooseOpponent(event, discordGame, game);
        else if (event.getComponentId().startsWith("choosecombatant")) ecazChooseCombatant(event, discordGame, game);
    }

    private static void chooseTerritory(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        int battleIndex = Integer.parseInt(event.getComponentId().split("-")[1]);
        String territory = battles.getBattles(game).get(battleIndex).getWholeTerritoryName();
        discordGame.queueMessage("You selected " + territory + ".");
        discordGame.getTurnSummary().queueMessage(battles.getAggressor().getEmoji() + " will battle in " + territory + ".");
        BattleCommands.setBattleIndex(discordGame, game, battleIndex);
    }

    private static void chooseOpponent(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        String opponent = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + opponent + ".");
        discordGame.getTurnSummary().queueMessage(battles.getAggressor().getEmoji() + " will battle against " + opponent + ".");
        BattleCommands.setOpponent(discordGame, game, opponent);
    }

    private static void ecazChooseCombatant(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        discordGame.queueDeleteMessage();
        Battles battles = game.getBattles();
        String battleFaction = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + battleFaction + ".");
        discordGame.getTurnSummary().queueMessage(Emojis.getFactionEmoji(battleFaction) + " will be the combatant.");
        battles.setEcazCombatant(battleFaction);
        battles.callBattleActions(game);
    }
}
