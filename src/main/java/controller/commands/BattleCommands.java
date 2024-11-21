package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.EcazFaction;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;
import static controller.commands.CommandOptions.territory;

public class BattleCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("battle", "Commands for the players of the game.").addSubcommands(
                new SubcommandData("review-resolution", "Print battle results to mod-info for review.").addOptions(deactivatePoisonTooth, addPortableSnooper, stoneBurnerDoesNotKill, useJuiceOfSapho, aggressorTraitor, defenderTraitor, forceResolution),
                new SubcommandData("publish-resolution", "Publish battle results to turn summary.").addOptions(deactivatePoisonTooth, addPortableSnooper, stoneBurnerDoesNotKill, useJuiceOfSapho, aggressorTraitor, defenderTraitor, forceResolution),
                new SubcommandData("place-leader-in-territory", "Place a leader in a territory where they had battled.").addOptions(faction, factionLeader, territory),
                new SubcommandData("remove-leader-from-territory", "Remove a leader from a territory where they did not batttle.").addOptions(faction, removeLeader),
                new SubcommandData("karama-starred-forces", "Negate the starred forces advantage in the current battle.").addOptions(starredForcesFaction),
                new SubcommandData("karama-fremen-must-pay-spice", "Require Fremen to pay spice for full force value in the current battle."),
                new SubcommandData("set-hms-stronghold-card", "Set the HMS Stronghold Card.").addOptions(hmsStrongoldCard),
                new SubcommandData("set-spice-banker-support", "Set the amount of spice to be spent with Spice Banker.").addOptions(spiceBankerPayment),
                new SubcommandData("release-duke-vidal", "Set Duke Vidal aside, not assigned to any faction."),
                new SubcommandData("mark-traitor-used", "Mark that the faction has used the traitor.").addOptions(faction, traitor)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        switch (name) {
            case "review-resolution" -> reviewResolution(discordGame, game);
            case "publish-resolution" -> publishResolution(discordGame, game);
            case "place-leader-in-territory" -> placeLeaderInTerritory(discordGame, game);
            case "remove-leader-from-territory" -> removeLeaderFromTerritory(discordGame, game);
            case "karama-starred-forces" -> karamaStarredForces(discordGame, game);
            case "karama-fremen-must-pay-spice" -> karamaFremenMustPay(discordGame, game);
            case "set-hms-stronghold-card" -> setHMSStrongholdCard(discordGame, game);
            case "set-spice-banker-support" -> setSpiceBankerSupport(discordGame, game);
            case "release-duke-vidal" -> releaseDukeVidal(discordGame, game);
            case "mark-traitor-used" -> markTraitorUsed(discordGame, game);
        }
    }

    public static void reviewResolution(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        battleResolution(discordGame, game, false);
    }

    public static void publishResolution(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        battleResolution(discordGame, game, true);
    }

    public static void battleResolution(DiscordGame discordGame, Game game, boolean publishToTurnSummary) throws InvalidGameStateException, ChannelNotFoundException {
        boolean playedJuiceOfSapho = discordGame.optional(useJuiceOfSapho) != null && discordGame.required(useJuiceOfSapho).getAsBoolean();
        boolean noKillStoneBurner = discordGame.optional(stoneBurnerDoesNotKill) != null && discordGame.required(stoneBurnerDoesNotKill).getAsBoolean();
        boolean portableSnooper = discordGame.optional(addPortableSnooper) != null && discordGame.required(addPortableSnooper).getAsBoolean();
        boolean noPoisonTooth = discordGame.optional(deactivatePoisonTooth) != null && discordGame.required(deactivatePoisonTooth).getAsBoolean();
        boolean aggressorCallsTraitor = discordGame.optional(aggressorTraitor) != null && discordGame.required(aggressorTraitor).getAsBoolean();
        boolean defenderCallsTraitor = discordGame.optional(defenderTraitor) != null && discordGame.required(defenderTraitor).getAsBoolean();
        boolean overrideDecisions = discordGame.optional(forceResolution) != null && discordGame.required(forceResolution).getAsBoolean();

        game.getBattles().getCurrentBattle().battleResolution(game, publishToTurnSummary, playedJuiceOfSapho, noKillStoneBurner, portableSnooper, noPoisonTooth, aggressorCallsTraitor, defenderCallsTraitor, overrideDecisions);
        discordGame.pushGame();
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

    public static void karamaStarredForces(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        Faction targetFaction = game.getFaction(discordGame.required(starredForcesFaction).getAsString());
        game.getBattles().getCurrentBattle().negateSpecialForces(game, targetFaction);
        discordGame.pushGame();
    }

    public static void karamaFremenMustPay(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        game.getBattles().getCurrentBattle().karamaFremenMustPay(game);
        discordGame.pushGame();
    }

    public static void setHMSStrongholdCard(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        String strongHoldCard = discordGame.required(hmsStrongoldCard).getAsString();
        Faction faction = game.getFactions().stream()
                .filter(f -> f.getStrongholdCards().stream()
                        .anyMatch(c -> c.name().equals(strongHoldCard)))
                .findAny().orElseThrow(() -> new InvalidGameStateException("No faction has " + strongHoldCard + " Stronghold Card"));
        game.getBattles().getCurrentBattle().setHMSStrongholdCard(faction, strongHoldCard);
        discordGame.pushGame();
    }

    public static void setSpiceBankerSupport(DiscordGame discordGame, Game game) throws InvalidGameStateException, ChannelNotFoundException {
        int spice = discordGame.required(spiceBankerPayment).getAsInt();
        if (spice < 0 || spice > 3)
            throw new InvalidGameStateException("Spice Banker support must be 0-3");
        Faction faction = game.getFactions().stream()
                .filter(f -> f.getLeaderSkillsHand().stream()
                        .anyMatch(c -> c.name().equals("Spice Banker")))
                .findAny().orElseThrow(() -> new InvalidGameStateException("No faction has Spice Banker"));
        if (faction.getSpice() < spice)
            throw new InvalidGameStateException(faction.getEmoji() + " does not have enough spice.");
        game.getBattles().getCurrentBattle().setSpiceBankerSupport(faction, spice);
        discordGame.pushGame();
    }

    public static void releaseDukeVidal(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction faction = game.getFactions().stream().filter(f -> f.getLeader("Duke Vidal").isPresent()).findFirst().orElse(null);
        if (faction != null) {
            faction.removeLeader("Duke Vidal");
            game.getTurnSummary().publish("Duke Vidal is no longer in service to " + faction.getEmoji());
        }
        discordGame.pushGame();
    }

    public static void markTraitorUsed(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        String traitorName = discordGame.required(traitor).getAsString();
        game.getFaction(factionName).useTraitor(traitorName);
        discordGame.pushGame();
    }

    public static void setupBattle(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Battles battles = game.getBattles();
        battles.nextBattle(game);
        if (battles.aggressorMustChooseBattle()) territoryButtons(game, battles);
        else setBattleIndex(discordGame, game, 0);
        discordGame.pushGame();
    }

    public static void territoryButtons(Game game, Battles battles) {
        Faction aggressor = battles.getAggressor(game);
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0;
        for (Battle battle : battles.getDefaultAggressorsBattles())
            choices.add(new DuneChoice("chooseterritory-" + i++, battle.getWholeTerritoryName()));
        aggressor.getChat().publish("Where would you like to battle? " + aggressor.getPlayer(), choices);
    }

    public static void setBattleIndex(DiscordGame discordGame, Game game, int battleIndex) throws InvalidGameStateException, ChannelNotFoundException {
        Battles battles = game.getBattles();
        battles.setTerritoryByIndex(battleIndex);
        Battle currentBattle = battles.getCurrentBattle();
        if (currentBattle.aggressorMustChooseOpponent()) opponentButtons(discordGame, game, currentBattle);
        else if (currentBattle.hasEcazAndAlly()) ecazAllyButtons(discordGame, game);
        else battles.callBattleActions(game);
    }

    public static void opponentButtons(DiscordGame discordGame, Game game, Battle battle) throws ChannelNotFoundException {
        Faction aggressor = battle.getAggressor(game);
        List<DuneChoice> choices = new ArrayList<>();
        boolean ecazAndAllyIdentified = false;
        for (Faction faction : battle.getFactions(game)) {
            String opponentName = faction.getName();
            if (faction == aggressor) continue;
            else if (battle.hasEcazAndAlly() && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz"))) {
                if (ecazAndAllyIdentified) continue;
                ecazAndAllyIdentified = true;
                opponentName = faction.getName() + " and " + faction.getAlly();
            }
            choices.add(new DuneChoice("chooseopponent-" + opponentName, opponentName));
        }
        aggressor.getChat().publish("Whom would you like to battle first? " + aggressor.getPlayer(), choices);
        discordGame.getTurnSummary().queueMessage(aggressor.getEmoji() + " must choose their opponent.");
    }

    public static void ecazAllyButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Faction ecaz = game.getFaction("Ecaz");
        List<DuneChoice> choices = List.of(
                new DuneChoice("choosecombatant-Ecaz", "You - Ecaz"),
                new DuneChoice("choosecombatant-" + ecaz.getAlly(), "Your ally - " + ecaz.getAlly())
        );
        ecaz.getChat().publish("Who will provide leader and " + Emojis.TREACHERY + " cards in your alliance's battle? " + ecaz.getPlayer(), choices);
        discordGame.getTurnSummary().queueMessage(Emojis.ECAZ + " must choose who will fight for their alliance.");
    }
}
