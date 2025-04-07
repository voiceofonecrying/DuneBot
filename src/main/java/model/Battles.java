package model;

import constants.Emojis;
import enums.GameOption;
import exceptions.InvalidGameStateException;
import model.factions.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Battles {
    private List<Battle> battles;
    private Battle currentBattle;
    private boolean moritaniCanTakeVidal;
    private boolean ixCunning;
    private List<String> factionsInBattleOrder;
    private String factionNameWithSapho;
    private boolean juiceOfSaphoTBD;

    public Battles() {
    }

    public void startBattlePhase(Game game) {
        factionsInBattleOrder = new ArrayList<>(game.getFactionsInStormOrder().stream().map(Faction::getName).toList());
        getBattles(game);
        if (battles.stream().anyMatch(b -> b.getForces().stream().anyMatch(f -> f.getName().equals("Ix"))))
            presentIxCunningChoices(game);
        List<Faction> saphoList = game.getFactionsWithTreacheryCard("Juice of Sapho");
        if (!saphoList.isEmpty()) {
            Faction factionWithSapho = saphoList.getFirst();
            if (battles.stream().anyMatch(b -> b.getForces().stream().anyMatch(f -> f.getFactionName().equals(factionWithSapho.getName())))) {
                List<DuneChoice> choices = new ArrayList<>();
                choices.add(new DuneChoice("battle-battles-juice-of-sapho-yes", "Yes"));
                choices.add(new DuneChoice("battle-battles-juice-of-sapho-no", "No"));
                factionWithSapho.getChat().publish("Will you play Juice of Sapho to be first in Battle phase? " + factionWithSapho.getPlayer(), choices);
                factionNameWithSapho = factionWithSapho.getName();
                juiceOfSaphoTBD = true;
            }
        }
    }

    public void publishListOfBattles(Game game) {
        List<Battle> battleTerritories = getBattles(game);
        if (!battleTerritories.isEmpty()) {
            String battleMessages = battleTerritories.stream()
                    .map((battle) -> battle.getFactionsMessage(game) + " in " + battle.getWholeTerritoryName())
                    .collect(Collectors.joining("\n"));
            game.getTurnSummary().publish("The following battles will take place this turn:\n" + battleMessages);
            game.getModInfo().publish("Use /run battle to initiate the first battle " + game.getModOrRoleMention());
        } else {
            game.getTurnSummary().publish("There are no battles this turn.");
        }
    }

    public void playJuiceOfSapho(Game game, Faction faction, boolean playIt) {
        juiceOfSaphoTBD = false;
        factionNameWithSapho = null;
        if (playIt) {
            List<String> temp = factionsInBattleOrder;
            temp.remove(faction.getName());
            factionsInBattleOrder = new ArrayList<>();
            factionsInBattleOrder.add(faction.getName());
            factionsInBattleOrder.addAll(temp);
            faction.discard("Juice of Sapho");
            faction.getChat().reply("You will play Juice of Sapho for the entire Battle phase.");
            game.getTurnSummary().publish(faction.getEmoji() + " plays Juice of Sapho to be first in the Battle phase.");
            publishListOfBattles(game);
        } else
            faction.getChat().reply("You will not play Juice of Sapho for the entire Battle phase.");
    }

    public boolean isIxCunning() {
        return ixCunning;
    }

    public void ixNexusCunning(Game game, boolean useNexusCard) {
        IxFaction ix = game.getIxFaction();
        ixCunning = useNexusCard;
        if (useNexusCard) {
            game.discardNexusCard(ix);
            game.getTurnSummary().publish(Emojis.IX_SUBOID + " will count as full strength this turn and will not require " + Emojis.SPICE);
            battles.forEach(b -> b.setIxCunning(true));
            if (currentBattle != null)
                currentBattle.setIxCunning(true);
            ix.getChat().reply("You played the " + Emojis.IX + " Nexus Card. Your " + Emojis.IX_SUBOID + " will count as full strength and will not require " + Emojis.SPICE);
        } else
            ix.getChat().reply("You will not play the " + Emojis.IX + " Nexus Card.");
    }

    public List<Battle> getBattles(Game game) {
        battles = new ArrayList<>();
        moritaniCanTakeVidal = false;
        int dukeVidalCount = 0;
        Territories territories = game.getTerritories();
        for (String aggregateTerritoryName : territories.getDistinctAggregateTerritoryNames()) {
            List<List<Territory>> territorySectorsForBattle = territories.getAggregateTerritoryList(aggregateTerritoryName, game.getStorm(), game.hasGameOption(GameOption.BATTLE_UNDER_STORM));
            Set<String> factionNames;
            for (List<Territory> territorySectors : territorySectorsForBattle) {
                factionNames = territories.getFighterNamesInAggTerritory(territorySectors);
                if (game.hasFaction("Moritani") && territorySectors.getFirst().isStronghold() && factionNames.size() > 1 && factionNames.contains("Moritani")
                        && !factionNames.contains("Ecaz")) dukeVidalCount++;
                List<Faction> factions = factionNames.stream()
                        .sorted(Comparator.comparingInt(this::getFactionBattleTurnIndex))
                        .map(game::getFaction)
                        .toList();

                boolean addBattle = factions.size() > 1;
                if (addBattle && factions.size() == 2 &&
                        (factions.get(0).getName().equals("Ecaz") && factions.get(1).getAlly().equals("Ecaz") ||
                                factions.get(0).getAlly().equals("Ecaz") && factions.get(1).getName().equals("Ecaz"))) {
                    addBattle = false;
                }
                if (addBattle) battles.add(new Battle(game, territorySectors, factions));
            }
        }
        if (dukeVidalCount >= 2) moritaniCanTakeVidal = true;
        battles.sort(Comparator
                .comparingInt(o -> getFactionBattleTurnIndex(o.getFactions(game).getFirst().getName())));
        return battles;
    }

    public int getFactionBattleTurnIndex(String factionName) {
        return factionsInBattleOrder.indexOf(factionName);
    }

    public Faction getAggressor(Game game) {
        return battles.getFirst().getFactions(game).getFirst();
    }

    public List<Battle> getDefaultAggressorsBattles() {
        return battles.stream().filter(b -> b.getFactionNames().getFirst().equals(battles.getFirst().getFactionNames().getFirst())).toList();
    }

    boolean isMoritaniCanTakeVidal() {
        return moritaniCanTakeVidal;
    }

    public boolean noBattlesRemaining(Game game) {
        getBattles(game);
        return battles.isEmpty();
    }

    public boolean aggressorMustChooseBattle() {
        if (battles.size() <= 1) return false;
        else return battles.get(0).getFactionNames().getFirst().equals(battles.get(1).getFactionNames().getFirst());
    }

    public boolean aggressorMustChooseOpponent() {
        return !battles.isEmpty() && battles.getFirst().aggressorMustChooseOpponent();
    }

    public void setTerritoryByIndex(int territoryIndex) {
        currentBattle = battles.get(territoryIndex);
        Battle battle = battles.remove(territoryIndex);
        battles.addFirst(battle);
    }

    public void setOpponent(Game game, String opponent) throws InvalidGameStateException {
        Faction aggressor = currentBattle.getAggressor(game);
        if (opponent.equals(aggressor.getName()))
            throw new InvalidGameStateException("Cannot set aggressor's opponent to be the aggressor");
        aggressor.getChat().reply("You selected " + opponent + ".");
        game.getTurnSummary().publish(getAggressor(game).getEmoji() + " will battle against " + opponent + ".");
        currentBattle = new Battle(game, currentBattle.getTerritorySectors(game),
                currentBattle.getFactions(game).stream().filter(
                        f -> f.getName().equals(opponent) || (f instanceof EcazFaction && f.getAlly().equals(opponent))
                                || f == aggressor || (f instanceof EcazFaction && aggressor.getName().equals(f.getAlly()))
                ).toList()
        );

        if (currentBattle.hasEcazAndAlly())
            currentBattle.presentEcazAllyChoice(game);
        else
            callBattleActions(game);
    }

    public void ecazChooseCombatant(Game game, String battleFaction) {
        game.getTurnSummary().publish(Emojis.getFactionEmoji(battleFaction) + " will be the combatant.");
        Faction ecaz = game.getEcazFaction();
        Faction faction = ecaz;
        if (!faction.isHighThreshold()) {
            faction = currentBattle.getAggressor(game);
            if (currentBattle.getAggressor(game) instanceof EcazFaction || ecaz.getAlly().equals(faction.getName()))
                faction = currentBattle.getDefender(game);
        }
        faction.getChat().reply("You selected " + battleFaction + ".");
        currentBattle.setEcazCombatant(game, battleFaction);
        callBattleActions(game);
    }

    public Battle getCurrentBattle() {
        return currentBattle;
    }

    public void nextBattle(Game game) throws InvalidGameStateException {
        if (juiceOfSaphoTBD)
            throw new InvalidGameStateException(factionNameWithSapho + " must decide whether to play Juice of Sapho");
        if (currentBattle != null && !currentBattle.isResolved(game)) {
            if (currentBattle.isDiplomatMustBeResolved())
                throw new InvalidGameStateException("Diplomat must be resolved before running the next battle.");
            else
                game.getModInfo().publish("The battle in " + currentBattle.getWholeTerritoryName() + " was not resolved and will be repeated.");
        }
        if (currentBattle != null) {
            if (currentBattle.isRihaniDeciphererMustBeResolved(game))
                throw new InvalidGameStateException("Rihani Decipherer must be resolved before running the next battle.");
            else if (currentBattle.isTechTokenMustBeResolved(game))
                throw new InvalidGameStateException("Tech Token must be selected before running the next battle.");
            else if (currentBattle.isHarkonnenCaptureMustBeResolved(game))
                throw new InvalidGameStateException("Harkonnen must decide to keep or kill " + currentBattle.getHarkonnenCapturedLeader() + " before running the next battle.");
            else if (currentBattle.isAuditorMustBeResolved())
                throw new InvalidGameStateException("Auditor must be resolved before running the next battle.");
        }
        if (noBattlesRemaining(game))
            throw new InvalidGameStateException("There are no more battles");

        game.getFactions().forEach(f -> f.getLeaders().forEach(l -> l.setPulledBehindShield(false)));
        StringBuilder nextBattle = new StringBuilder();
        Faction aggressor = battles.getFirst().getAggressor(game);
        if (aggressorMustChooseBattle()) {
            nextBattle.append(aggressor.getEmoji()).append(" must choose where they will fight:");
            for (Battle battle : battles) {
                if (aggressor != battle.getFactions(game).getFirst()) break;
                nextBattle.append(MessageFormat.format("\n{1}: {0}",
                        battle.getForcesMessage(game),
                        battle.getWholeTerritoryName()
                ));
            }
        } else {
            Battle battle = battles.getFirst();
            nextBattle.append(MessageFormat.format("Next battle: {0} in {1}",
                    battle.getForcesMessage(game),
                    battle.getWholeTerritoryName()
            ));
        }
        game.getTurnSummary().publish(nextBattle.toString());
//        currentBattle = battleTerritories.
    }

    private void presentSkilledLeaderChoices(Faction faction) {
        for (Leader leader : faction.getSkilledLeaders()) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("battle-pull-leader-" + faction.getName() + "-" + leader.getName() + "-yes", "Yes, pull behind"));
            choices.add(new DuneChoice("battle-pull-leader-" + faction.getName() + "-" + leader.getName() + "-no", "No, leave out front"));
            faction.getChat().publish(
                    "Will you pull " + leader.getSkillCard().name() + " " + leader.getName() + " behind your shield? " + faction.getPlayer(),
                    choices
            );
        }
    }

    public void callBattleActions(Game game) {
        String message = "Battle in " + currentBattle.getWholeTerritoryName() + ": ";
        message += currentBattle.getForcesMessage(game) + "\n";
        Faction aggressor = currentBattle.getAggressor(game);
        Faction opponent = currentBattle.getDefender(game);
        int step = 1;
        if (aggressor.hasSkill("Mentat"))
            message += step++ + ". " + aggressor.getEmoji() + " may use Mentat skill.\n";
        else if (opponent.hasSkill("Mentat"))
            message += step++ + ". " + opponent.getEmoji() + " may use Mentat skill.\n";

        if (aggressor instanceof BGFaction || (aggressor.getAlly().equals("BG") && !((BGFaction) game.getFaction("BG")).isDenyingAllyVoice()))
            message += step++ + ". " + aggressor.getEmoji() + " use the Voice.\n";
        else if (opponent instanceof BGFaction || (opponent.getAlly().equals("BG") && !((BGFaction) game.getFaction("BG")).isDenyingAllyVoice()))
            message += step++ + ". " + opponent.getEmoji() + " use the Voice.\n";

        if (aggressor instanceof AtreidesFaction || aggressor.getAlly().equals("Atreides") && !((AtreidesFaction) game.getFaction("Atreides")).isDenyingAllyBattlePrescience())
            message += step++ + ". " + aggressor.getEmoji() + " ask the Prescience question.\n";
        else if (opponent instanceof AtreidesFaction || opponent.getAlly().equals("Atreides") && !((AtreidesFaction) game.getFaction("Atreides")).isDenyingAllyBattlePrescience())
            message += step++ + ". " + opponent.getEmoji() + " ask the Prescience question.\n";

        String skilledLeaderFactions = "";
        if (!aggressor.getSkilledLeaders().isEmpty()) {
            skilledLeaderFactions += aggressor.getEmoji() + " ";
            presentSkilledLeaderChoices(aggressor);
        }
        if (!opponent.getSkilledLeaders().isEmpty()) {
            skilledLeaderFactions += opponent.getEmoji() + " ";
            presentSkilledLeaderChoices(opponent);
        }
        if (!skilledLeaderFactions.isEmpty())
            message += step++ + ". " + skilledLeaderFactions + " pull skilled leader or leave out front. (Use buttons in your faction chat thread.)\n";

        if (step != 1) message += step + ". ";
        message += aggressor.getEmoji() + " and " + opponent.getEmoji() + " submit battle plans.\n";
        message += aggressor.getPlayer() + " " + opponent.getPlayer();
        game.getGameActions().publish(message);

        presentEmperorCunningChoices(aggressor);
        presentEmperorCunningChoices(opponent);
    }

    private void presentEmperorCunningChoices(Faction faction) {
        List<DuneChoice> emperorCunningChoices = new ArrayList<>();
        emperorCunningChoices.add(new DuneChoice("battle-emperor-nexus-cunning-yes", "Yes"));
        emperorCunningChoices.add(new DuneChoice("battle-emperor-nexus-cunning-no", "No"));
        if (faction instanceof EmperorFaction
                && faction.getNexusCard() != null&& faction.getNexusCard().name().equals("Emperor")
                && currentBattle.getForces().stream().noneMatch(f -> f.getName().equals("Emperor*")))
            faction.getChat().publish("Would you like to play the " + Emojis.EMPEROR + " Nexus Card for this battle? " + faction.getPlayer(), emperorCunningChoices);
    }

    private void presentIxCunningChoices(Game game) {
        IxFaction faction = game.getIxFaction();
        List<DuneChoice> ixCunningChoices = new ArrayList<>();
        ixCunningChoices.add(new DuneChoice("battle-ix-nexus-cunning-yes", "Yes"));
        ixCunningChoices.add(new DuneChoice("battle-ix-nexus-cunning-no", "No"));

        if (faction.getNexusCard() != null) {
            if (faction.getNexusCard().name().equals("Ixians"))
                faction.getChat().publish("Would you like to play the " + Emojis.IX + " Nexus Card this turn? " + faction.getPlayer(), ixCunningChoices);
        }
    }
}
