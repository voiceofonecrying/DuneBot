package model;

import exceptions.InvalidGameStateException;
import model.factions.AtreidesFaction;
import model.factions.BGFaction;
import model.factions.EcazFaction;
import model.factions.Faction;

import java.text.MessageFormat;
import java.util.*;

public class Battles {
    List<Battle> battles;
    Battle currentBattle;
    boolean moritaniCanTakeVidal;


    public Battles() {
    }

    public void startBattlePhase(Game game) {
        getBattles(game);
    }

    public List<Force> aggregateForces(List<Territory> territorySectors, List<Faction> factions) {
        List<Force> forces = new ArrayList<>();
        for (Faction f: factions) {
            Optional<Integer> optInt;
            optInt = territorySectors.stream().map(t -> t.getForce(f.getName() + "*").getStrength()).reduce(Integer::sum);
            int totalSpecialStrength = optInt.orElse(0);
            if (totalSpecialStrength > 0) forces.add(new Force(f.getName() + "*", totalSpecialStrength));
            optInt  = territorySectors.stream().map(t -> t.getForce(f.getName()).getStrength()).reduce(Integer::sum);
            int totalForceStrength = optInt.orElse(0);
            if (totalForceStrength > 0) forces.add(new Force(f.getName(), totalForceStrength));
            boolean hasNoField = territorySectors.stream().anyMatch(Territory::hasRicheseNoField);
            if (hasNoField && f.getName().equals("Richese")) {
                forces.add(new Force("NoField", 1, "Richese"));
            }
        }
        return forces;
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
                String ecazAllyName = factions.stream().filter(f -> f instanceof EcazFaction).map(Faction::getAlly).findFirst().orElse(null);
                List<Force> forces = aggregateForces(territorySectors, factions);
                if (addBattle) battles.add(new Battle(aggregateTerritoryName, territorySectors, factions, forces, ecazAllyName));
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

    public boolean aggressorMustChooseOpponent() {
        return !battles.isEmpty() && battles.get(0).aggressorMustChooseOpponent();
    }

    public void setTerritoryByIndex(int territoryIndex) {
        currentBattle = battles.get(territoryIndex);
        Battle battle = battles.remove(territoryIndex);
        battles.add(0, battle);
    }

    public void setOpponent(Game game, String opponent) throws InvalidGameStateException {
        Faction aggressor = currentBattle.getAggressor(game);
        if (opponent.equals(aggressor.getName()))
            throw new InvalidGameStateException("Cannot set aggressor's opponent to be the aggressor");
        String ecazAlly = null;
        if (currentBattle.hasEcazAndAlly())
            ecazAlly = game.getFaction("Ecaz").getAlly();
        boolean opponentIsEcazAlly = ecazAlly != null && ecazAlly.equals(opponent);
        boolean aggressorIsEcazAlly = ecazAlly != null && ecazAlly.equals(aggressor.getName());
        List<Force> newForces = new ArrayList<>();
        for (Force f : currentBattle.getForces()) {
            if (f.getFactionName().equals(opponent)) newForces.add(f);
            else if (f.getFactionName().equals("Ecaz") && opponentIsEcazAlly) newForces.add(f);
            else if (f.getFactionName().equals(aggressor.getName())) newForces.add(f);
            else if (f.getFactionName().equals("Ecaz") && aggressorIsEcazAlly) newForces.add(f);
        }
        currentBattle = new Battle(currentBattle.getWholeTerritoryName(), currentBattle.getTerritorySectors(),
                currentBattle.getFactions(game).stream().filter(
                        f -> f.getName().equals(opponent) || (f instanceof EcazFaction && f.getAlly().equals(opponent))
                                || f == aggressor || (f instanceof EcazFaction && aggressor.getName().equals(f.getAlly()))
                ).toList(),
                newForces,
                currentBattle.getEcazAllyName()
        );
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

        if (aggressor.getSkilledLeaders().stream().anyMatch(l -> l.skillCard().name().equals("Mentat")))
            message += step++ + ". " + aggressor.getEmoji() + " use Mentat skill.\n";
        else if (opponent.getSkilledLeaders().stream().anyMatch(l -> l.skillCard().name().equals("Mentat")))
            message += step++ + ". " + opponent.getEmoji() + " use Mentat skill.\n";

        if (aggressor instanceof AtreidesFaction || aggressor.getAlly().equals("Atreides"))
            message += step++ + ". " + aggressor.getEmoji() + " ask the Prescience question.\n";
        else if (opponent instanceof AtreidesFaction || opponent.getAlly().equals("Atreides"))
            message += step++ + ". " + opponent.getEmoji() + " ask the Prescience question.\n";

        String skilledLeaderFactions = "";
        if (!aggressor.getSkilledLeaders().isEmpty()) skilledLeaderFactions += aggressor.getEmoji() + " ";
        if (!opponent.getSkilledLeaders().isEmpty()) skilledLeaderFactions += opponent.getEmoji() + " ";
        if (!skilledLeaderFactions.isEmpty())
            message += step++ + ". " + skilledLeaderFactions + " pull skilled leader or leave out front.\n";

        if (step != 1) message += step + ". ";
        message += aggressor.getEmoji() + " and " + opponent.getEmoji() + " submit battle plans.\n";
        message += aggressor.getPlayer() + " " + opponent.getPlayer();
        game.getGameActions().publish(message);
    }
}
