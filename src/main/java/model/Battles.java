package model;

import exceptions.InvalidGameStateException;
import model.factions.AtreidesFaction;
import model.factions.BGFaction;
import model.factions.EcazFaction;
import model.factions.Faction;

import java.text.MessageFormat;
import java.util.*;

public class Battles {
    private List<Battle> battles;
    private Battle currentBattle;
    private boolean moritaniCanTakeVidal;


    public Battles() {
    }

    public void startBattlePhase(Game game) {
        getBattles(game);
    }

    public List<Force> aggregateForces(List<Territory> territorySectors, List<Faction> factions) {
        List<Force> forces = new ArrayList<>();
        for (Faction f: factions) {
            Optional<Integer> optInt;
            optInt = territorySectors.stream().map(t -> t.getForceStrength(f.getName() + "*")).reduce(Integer::sum);
            int totalSpecialStrength = optInt.orElse(0);
            if (totalSpecialStrength > 0) forces.add(new Force(f.getName() + "*", totalSpecialStrength));
            optInt  = territorySectors.stream().map(t -> t.getForceStrength(f.getName())).reduce(Integer::sum);
            int totalForceStrength = optInt.orElse(0);
            if (totalForceStrength > 0) forces.add(new Force(f.getName(), totalForceStrength));
            Integer noField = territorySectors.stream().map(Territory::getRicheseNoField).filter(Objects::nonNull).findFirst().orElse(null);
            if (noField != null && f.getName().equals("Richese")) {
                forces.add(new Force("NoField", noField, "Richese"));
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
                if (game.hasFaction("Moritani") && territorySectors.getFirst().isStronghold() && factionNames.size() > 1 && factionNames.contains("Moritani")
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
                .comparingInt(o -> game.getFactionTurnIndex(o.getFactions(game).getFirst().getName())));
        return battles;
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
            choices.add(new DuneChoice("pullleader-" + faction.getName() + "-" + leader.getName() + "-yes", "Yes, pull behind"));
            choices.add(new DuneChoice("pullleader-" + faction.getName() + "-" + leader.getName() + "-no", "No, leave out front"));
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

        if (aggressor instanceof BGFaction || aggressor.getAlly().equals("BG"))
            message += step++ + ". " + aggressor.getEmoji() + " use the Voice.\n";
        else if (opponent instanceof BGFaction || opponent.getAlly().equals("BG"))
            message += step++ + ". " + opponent.getEmoji() + " use the Voice.\n";

        if (aggressor instanceof AtreidesFaction || aggressor.getAlly().equals("Atreides"))
            message += step++ + ". " + aggressor.getEmoji() + " ask the Prescience question.\n";
        else if (opponent instanceof AtreidesFaction || opponent.getAlly().equals("Atreides"))
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
    }
}
