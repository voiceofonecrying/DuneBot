package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Battle;
import model.Battles;
import model.Game;
import model.Territory;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static controller.commands.CommandOptions.*;
import static controller.commands.CommandOptions.territory;

public class BattleCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("battle", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("review-resolution", "Print battle results to mod-info for review.").addOptions(deactivatePoisonTooth, addPortableSnooper, stoneBurnerKills, useJuiceOfSapho),
                new SubcommandData("place-leader-in-territory", "Place a leader in a territory where they had battled.").addOptions(faction, factionLeader, territory),
                new SubcommandData("remove-leader-from-territory", "Remove a leader from a territory where they did not batttle.").addOptions(faction, removeLeader)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "review-resolution" -> reviewResolution(discordGame, game);
            case "place-leader-in-territory" -> placeLeaderInTerritory(discordGame, game);
            case "remove-leader-from-territory" -> removeLeaderFromTerritory(discordGame, game);
        }
    }

    public static void reviewResolution(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        CommandManager.reviewBattleResolution(discordGame, game);
    }

    public static void placeLeaderInTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderName = discordGame.required(factionLeader).getAsString();
        Territory territory = game.getTerritory(discordGame.required(CommandOptions.territory).getAsString());
        targetFaction.getLeader(leaderName).orElseThrow().setBattleTerritoryName(territory.getTerritoryName());
        targetFaction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        discordGame.pushGame();
    }

    public static void removeLeaderFromTerritory(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(faction).getAsString());
        String leaderName = discordGame.required(removeLeader).getAsString();
        targetFaction.getLeader(leaderName).orElseThrow().setBattleTerritoryName(null);
        targetFaction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        discordGame.pushGame();
    }

    public static void setupBattle(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Battles battles = game.getBattles();
        Battle currentBattle = battles.getCurrentBattle();
        if (currentBattle != null && !currentBattle.isResolved(game))
            discordGame.getModInfo().queueMessage("The battle in " + currentBattle.getWholeTerritoryName() + " was not resolved and will be repeated.");
        battles.nextBattle(game);
        if (battles.aggressorMustChooseBattle()) territoryButtons(discordGame, game, battles);
        else setBattleIndex(discordGame, game, 0);
        discordGame.pushGame();
    }

    public static void territoryButtons(DiscordGame discordGame, Game game, Battles battles) throws ChannelNotFoundException {
        Faction aggressor = battles.getAggressor(game);
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (Battle battle : battles.getDefaultAggressorsBattles()) {
            buttons.add(Button.primary("chooseterritory-" + i++, battle.getWholeTerritoryName()));
        }
        discordGame.getFactionChat(aggressor.getName()).queueMessage("Where would you like to battle? " + aggressor.getPlayer(), buttons);
    }

    public static void setBattleIndex(DiscordGame discordGame, Game game, int battleIndex) throws InvalidGameStateException, ChannelNotFoundException {
        Battles battles = game.getBattles();
        battles.setTerritoryByIndex(battleIndex);
        Battle currentBattle = battles.getCurrentBattle();
        if (currentBattle.aggressorMustChooseOpponent()) opponentButtons(discordGame, game, currentBattle);
        else if (currentBattle.hasEcazAndAlly()) ecazAllyButtons(discordGame, game, currentBattle);
        else battles.callBattleActions(game);
        discordGame.pushGame();
    }

    public static void opponentButtons(DiscordGame discordGame, Game game, Battle battle) throws ChannelNotFoundException {
        Faction aggressor = battle.getAggressor(game);
        List<Button> buttons = new LinkedList<>();
        boolean ecazAndAllyIdentified = false;
        for (Faction faction : battle.getFactions(game)) {
            String opponentName = faction.getName();
            if (faction == aggressor) continue;
            else if (battle.hasEcazAndAlly() && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz"))) {
                if (ecazAndAllyIdentified) continue;
                ecazAndAllyIdentified = true;
                opponentName = faction.getName() + " and " + faction.getAlly();
            }
            buttons.add(Button.primary("chooseopponent-" + opponentName, opponentName));
        }
        discordGame.getFactionChat(aggressor.getName()).queueMessage("Whom would you like to battle first? " + aggressor.getPlayer(), buttons);
        discordGame.getTurnSummary().queueMessage(aggressor.getEmoji() + " must choose their opponent.");
    }

    public static void ecazAllyButtons(DiscordGame discordGame, Game game, Battle battle) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        Faction ecaz = battle.getFactions(game).stream().filter(f -> f instanceof EcazFaction).findFirst().orElseThrow();
        buttons.add(Button.primary("choosecombatant-Ecaz", "You - Ecaz"));
        buttons.add(Button.primary("choosecombatant-" + ecaz.getAlly(), "Your ally - " + ecaz.getAlly()));
        discordGame.getEcazChat().queueMessage("Who will provide leader and " + Emojis.TREACHERY + " cards in your alliance's battle? " + ecaz.getPlayer(), buttons);
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " must choose who will fight for their alliance.");
    }

    public static void setOpponent(DiscordGame discordGame, Game game, String factionName) throws ChannelNotFoundException, InvalidGameStateException {
        Battles battles = game.getBattles();
        int space = factionName.indexOf(" ");
        if (space != -1) factionName = factionName.substring(0, space);
        battles.setOpponent(game, factionName);
        Battle currentBattle = battles.getCurrentBattle();
        if (currentBattle.hasEcazAndAlly()) ecazAllyButtons(discordGame, game, currentBattle);
        else battles.callBattleActions(game);
        discordGame.pushGame();
    }
}
