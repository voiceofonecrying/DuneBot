package model;

import exceptions.InvalidGameStateException;
import model.factions.AtreidesFaction;
import model.factions.BGFaction;
import model.factions.EcazFaction;
import model.factions.Faction;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Battles {
    List<Battle> battles;
    Battle currentBattle;
    boolean moritaniCanTakeVidal;


    public Battles() {
    }

    public void startBattlePhase(Game game) {
        getBattles(game);
    }

    public List<Battle> getBattles(Game game) {
        battles = new ArrayList<>();
        moritaniCanTakeVidal = false;
        int dukeVidalCount = 0;
        Territories territories = game.getTerritories();
        for (String aggregateTerritoryName : territories.getDistinctAggregateTerritoryNames()) {
            List<List<Territory>> territorySectorsForBattle = territories.getTerritorySectorsForBattle(aggregateTerritoryName, game.getStorm());
            Set<String> factionNames;
            for (List<Territory> territorySectors : territorySectorsForBattle) {
                factionNames = territories.getFighterNamesInAggTerritory(territorySectors);
                if (game.hasFaction("Moritani") && territorySectors.get(0).isStronghold() && factionNames.size() > 1 && factionNames.contains("Moritani")
                        && !factionNames.contains("Ecaz")) dukeVidalCount++;
                List<Faction> factions = factionNames.stream()
                        .sorted(Comparator.comparingInt(game::getFactionTurnIndex))
                        .map(game::getFaction)
                        .toList();

                boolean addBattle = factions.size() > 1;
                if (addBattle && factions.size() == 2 &&
                        (factions.get(0).getName().equals("Ecaz") && factions.get(1).getAlly().equals("Ecaz") ||
                                factions.get(0).getAlly().equals("Ecaz") && factions.get(1).getName().equals("Ecaz"))) {
                    addBattle = false;
                }
                if (addBattle) battles.add(new Battle(game, aggregateTerritoryName, territorySectors, factions));
            }
        }
        if (dukeVidalCount >= 2) moritaniCanTakeVidal = true;
        battles.sort(Comparator
                .comparingInt(o -> game.getFactionTurnIndex(o.getFactions(game).get(0).getName())));
        return battles;
    }

    public Faction getAggressor(Game game) {
        return battles.get(0).getFactions(game).get(0);
    }

    public List<Battle> getDefaultAggressorsBattles() {
        return battles.stream().filter(b -> b.getFactionNames().get(0).equals(battles.get(0).getFactionNames().get(0))).toList();
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
        else return battles.get(0).getFactionNames().get(0).equals(battles.get(1).getFactionNames().get(0));
    }

    public boolean aggressorMustChooseOpponent(Game game) {
        return !battles.isEmpty() && battles.get(0).aggressorMustChooseOpponent(game);
    }

    public void setTerritoryByIndex(int territoryIndex) {
        currentBattle = battles.get(territoryIndex);
        Battle battle = battles.remove(territoryIndex);
        battles.add(0, battle);
    }

    public void setOpponent(Game game, String opponent) {
        Faction aggressor = currentBattle.getAggressor(game);
        currentBattle = new Battle(game, currentBattle.getWholeTerritoryName(), currentBattle.getTerritorySectors(),
                currentBattle.getFactions(game).stream().filter(
                        f -> f.getName().equals(opponent) || (f instanceof EcazFaction && f.getAlly().equals(opponent))
                                || f == aggressor || (f instanceof EcazFaction && aggressor.getName().equals(f.getAlly()))
                ).toList()
        );
        currentBattle.setDefenderName(opponent);
    }

    public void setEcazCombatant(Game game, String combatant) {
        Faction combatantFaction = currentBattle.getFactions(game).stream().filter(f -> f.getName().equals(combatant)).findFirst().orElseThrow();
        boolean ecazIsCombatant = combatant.equals("Ecaz");
        if (currentBattle.getAggressor(game) instanceof EcazFaction && !combatantFaction.getName().equals("Ecaz"))
            currentBattle.setAggressor(combatantFaction.getName());
        else if (currentBattle.getAggressor(game).getAlly().equals("Ecaz") && ecazIsCombatant)
            currentBattle.setAggressor(combatantFaction.getName());
        else if (currentBattle.getDefender(game) instanceof EcazFaction && !combatantFaction.getName().equals("Ecaz"))
            currentBattle.setDefenderName(combatantFaction.getName());
        else if (currentBattle.getDefender(game).getAlly().equals("Ecaz") && ecazIsCombatant)
            currentBattle.setDefenderName(combatantFaction.getName());
    }

    public Battle getCurrentBattle() {
        return currentBattle;
    }

    public void nextBattle(Game game) throws InvalidGameStateException {
        if (noBattlesRemaining(game))
            throw new InvalidGameStateException("There are no more battles");

        StringBuilder nextBattle = new StringBuilder();
        Faction aggressor = battles.get(0).getAggressor(game);
        if (aggressorMustChooseBattle()) {
            nextBattle.append(aggressor.getEmoji()).append(" must choose where they will fight:");
            for (Battle battle : battles) {
                if (aggressor != battle.getFactions(game).get(0)) break;
                nextBattle.append(MessageFormat.format("\n{1}: {0}",
                        battle.getForcesMessage(game),
                        battle.getWholeTerritoryName()
                ));
            }
        } else {
            Battle battle = battles.get(0);
            nextBattle.append(MessageFormat.format("Next battle: {0} in {1}",
                    battle.getForcesMessage(game),
                    battle.getWholeTerritoryName()
            ));
        }
        game.getTurnSummary().publish(nextBattle.toString());
//        currentBattle = battleTerritories.
    }

    public void callBattleActions(Game game) {
        String message = "Battle in " + currentBattle.getWholeTerritoryName() + ": ";
        message += currentBattle.getForcesMessage(game) + "\n";
        Faction aggressor = currentBattle.getAggressor(game);
        Faction opponent = currentBattle.getDefender(game);
        int step = 1;
        if (aggressor instanceof BGFaction || aggressor.getAlly().equals("BG"))
            message += step++ + ". " + aggressor.getEmoji() + " use the Voice.\n";
        else if (opponent instanceof BGFaction || opponent.getAlly().equals("BG"))
            message += step++ + ". " + opponent.getEmoji() + " use the Voice.\n";

        if (aggressor instanceof AtreidesFaction || aggressor.getAlly().equals("Atreides"))
            message += step++ + ". " + aggressor.getEmoji() + " ask the Prescience question.\n";
        else if (opponent instanceof AtreidesFaction || opponent.getAlly().equals("Atreides"))
            message += step++ + ". " + opponent.getEmoji() + " ask the Prescience question.\n";

        if (step != 1) message += step + ". ";
        message += aggressor.getEmoji() + " and " + opponent.getEmoji() + " submit battle plans.\n";
        message += aggressor.getPlayer() + " " + opponent.getPlayer();
        game.getGameActions().publish(message);
    }
}
