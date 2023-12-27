package model;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.factions.AtreidesFaction;
import model.factions.EcazFaction;
import model.factions.EmperorFaction;
import model.factions.Faction;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Battle {
    private final String wholeTerritoryName;
    private final List<Territory> territorySectors;
    private final List<Faction> factions = null;
    private final List<String> factionNames;
    private List<Force> forces;
    private String aggressorName;
    private BattlePlan aggressorBattlePlan;
    private String defenderName;
    private BattlePlan defenderBattlePlan;

    public Battle(Game game, String wholeTerritoryName, List<Territory> territorySectors, List<Faction> battleFactionsInStormOrder) {
        this.wholeTerritoryName = wholeTerritoryName;
        this.territorySectors = territorySectors;
        this.factionNames = battleFactionsInStormOrder.stream().map(Faction::getName).toList();
        this.forces = aggregateForces(game);
    }

    public String getWholeTerritoryName() {
        return wholeTerritoryName;
    }

    public List<Territory> getTerritorySectors() {
        return territorySectors;
    }

    public List<String> getFactionNames() {
        if (factions != null)
            return factions.stream().map(Faction::getName).toList();
        return factionNames;
    }

    public List<Faction> getFactions(Game game) {
        if (factions != null)
            return factions;
        return factionNames.stream().map(game::getFaction).toList();
    }

    public String getAggressorName() {
        if (aggressorName != null) return aggressorName;
        if (factions != null)
            return factions.get(0).getName();
        return factionNames.get(0);
    }

    public Faction getAggressor(Game game) {
        if (aggressorName != null) return game.getFaction(aggressorName);
        if (factions != null)
            return factions.get(0);
        return game.getFaction(factionNames.get(0));
    }

    public void setAggressor(String aggressor) {
        this.aggressorName = aggressor;
    }

    public String getDefenderName() {
        if (defenderName != null) return defenderName;
        if (factions != null)
            return factions.get(1).getName();
        return factionNames.get(1);
    }

    public Faction getDefender(Game game) {
        if (defenderName != null) return game.getFaction(defenderName);
        if (factions != null)
            return factions.get(1);
        return game.getFaction(factionNames.get(1));
    }

    public void setDefenderName(String defenderName) {
        this.defenderName = defenderName;
    }

    public boolean hasEcazAndAlly(Game game) {
        List<Faction> battleFactions = factions;
        if (factions == null)
            battleFactions = factionNames.stream().map(game::getFaction).toList();
        return battleFactions.stream().anyMatch(f -> f instanceof EcazFaction)
                && battleFactions.stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
    }

    public boolean aggressorMustChooseOpponent(Game game) {
        int numFactions = factions == null ? factionNames.size() : factions.size();
        if (hasEcazAndAlly(game))
            numFactions--;
        return numFactions > 2;
    }

    public List<Force> getForces() {
        return forces;
    }

    public boolean isResolved(Game game) {
        List<Force> forces = new ArrayList<>();
        boolean addRichese = false;
        for (Territory t : territorySectors) {
            Territory territory = game.getTerritory(t.getTerritoryName());
            forces.addAll(territory.getForces().stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .filter(force -> !(force.getName().equalsIgnoreCase("Hidden Mobile Stronghold")))
                    .toList()
            );
            if (territory.hasRicheseNoField()) addRichese = true;
        }
        Set<String> factionNames = forces.stream()
                .map(Force::getFactionName)
                .collect(Collectors.toSet());
        if (addRichese) factionNames.add("Richese");

        if (factionNames.size() <= 1) return true;
        List<String> namesList = factionNames.stream().toList();
        return factionNames.size() == 2 && hasEcazAndAlly(game)
                && (namesList.get(0).equals("Ecaz") && namesList.get(1).equals(game.getFaction("Ecaz").getAlly())
                || namesList.get(0).equals(game.getFaction("Ecaz").getAlly()) && namesList.get(1).equals("Ecaz"));
    }

    public List<Force> aggregateForces(Game game) {
        forces = new ArrayList<>();
        for (Faction f: getFactions(game)) {
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

    public String getFactionsMessage(Game game) {
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factionNames.stream().anyMatch(f -> f.equals("Ecaz"));
        boolean ecazAllyInBattle = getFactions(game).stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : getFactions(game)) {
            if (ecazAllyComplete && (f.getName().equals("Ecaz") || f.getAlly().equals("Ecaz"))) continue;
            message.append(vs);
            message.append(f.getEmoji());
            if (ecazAllyInBattle && !ecazAllyComplete && f.getName().equals("Ecaz") && f.hasAlly()) {
                message.append(Emojis.getFactionEmoji(f.getAlly()));
                ecazAllyComplete = true;
            } else if (ecazInBattle && !ecazAllyComplete && f.getAlly().equals("Ecaz")) {
                message.append(Emojis.getFactionEmoji("Ecaz"));
                ecazAllyComplete = true;
            }
            vs = " vs ";
        }
        return message.toString().trim();
    }

    private String getFactionForceMessage(String factionName) {
        String message = "";
        Optional<Force> optForce;
        optForce = forces.stream().filter(faction -> faction.getName().equals(factionName)).findFirst();
        int regularForces = 0;
        if (optForce.isPresent()) regularForces = optForce.get().getStrength();
        optForce = forces.stream().filter(faction -> faction.getName().equals(factionName + "*")).findFirst();
        int specialForces = 0;
        if (optForce.isPresent()) specialForces = optForce.get().getStrength();
        boolean hasNoField = factionName.equals("Richese") && forces.stream().anyMatch(force -> force.getName().equals("NoField"));
        if (hasNoField) message += "1 " + Emojis.NO_FIELD + " ";
        if (regularForces > 0) message += regularForces + " " + Emojis.getForceEmoji(factionName) + " ";
        if (specialForces > 0) message += specialForces + " " + Emojis.getForceEmoji(factionName + "*") + " ";
        return message;
    }

    public String getForcesMessage(Game game) {
        StringBuilder message = new StringBuilder();
        String vs = "";
        boolean ecazInBattle = factionNames.stream().anyMatch(f -> f.equals("Ecaz"));
        boolean ecazAllyInBattle = getFactions(game).stream().anyMatch(f -> f.getAlly().equals("Ecaz"));
        boolean ecazAllyComplete = false;
        for (Faction f : getFactions(game)) {
            if (ecazAllyComplete && (f.getName().equals("Ecaz") || f.getAlly().equals("Ecaz"))) continue;
            message.append(vs);
            message.append(getFactionForceMessage(f.getName()));
            if (ecazAllyInBattle && !ecazAllyComplete && f.getName().equals("Ecaz") && f.hasAlly()) {
                message.append(getFactionForceMessage(f.getAlly()));
                ecazAllyComplete = true;
            } else if (ecazInBattle && !ecazAllyComplete && f.getAlly().equals("Ecaz")) {
                message.append(getFactionForceMessage("Ecaz"));
                ecazAllyComplete = true;
            }
            vs = "vs ";
        }
        return message.toString().trim();
    }

    public List<Force> getForcesDialed(Game game, Faction faction, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        String factionName = (hasEcazAndAlly(game) && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int spiceUsed = 0;
        int dialUsed = 0;
        int specialStrengthUsed = 0;
        int regularStrengthUsed = 0;
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 2 && specialStrength - specialStrengthUsed > 0) {
            dialUsed += 2;
            spiceUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed == 0 && wholeNumberDial - dialUsed >= 1 && specialStrength - specialStrengthUsed > 0) {
            dialUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 1 && regularStrength - regularStrengthUsed > 0) {
            dialUsed++;
            spiceUsed++;
            regularStrengthUsed++;
        }
        if ((wholeNumberDial > dialUsed) || plusHalfDial) {
            int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
            regularStrengthUsed += troopsNeeded;
        }
        if (regularStrengthUsed > regularStrength || specialStrengthUsed > specialStrength)
            throw new InvalidGameStateException(faction.getEmoji() + " does not have enough troops in the territory.");
        while (spice > spiceUsed && regularStrength > regularStrength + 1) {
            specialStrengthUsed--;
            regularStrengthUsed++;
            regularStrengthUsed++;
            spiceUsed++;
        }
        return List.of(new Force(factionName, regularStrengthUsed, factionName), new Force(factionName + "*", specialStrengthUsed, factionName));
    }

    private void validateDial(Game game, Faction faction, int wholeNumberDial, boolean plusHalfDial, int spice) throws InvalidGameStateException {
        String factionName = (hasEcazAndAlly(game) && faction instanceof EcazFaction) ? faction.getAlly() : faction.getName();
        int specialStrength = forces.stream().filter(f -> f.getName().equals(factionName + "*")).findFirst().map(Force::getStrength).orElse(0);
        int regularStrength = forces.stream().filter(f -> f.getName().equals(factionName)).findFirst().map(Force::getStrength).orElse(0);
        int spiceUsed = 0;
        int dialUsed = 0;
        int specialStrengthUsed = 0;
        int regularStrengthUsed = 0;
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 2 && specialStrength - specialStrengthUsed > 0) {
            dialUsed += 2;
            spiceUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed == 0 && wholeNumberDial - dialUsed >= 1 && specialStrength - specialStrengthUsed > 0) {
            dialUsed++;
            specialStrengthUsed++;
        }
        while (spice - spiceUsed > 0 && wholeNumberDial - dialUsed >= 1 && regularStrength - regularStrengthUsed > 0) {
            dialUsed++;
            spiceUsed++;
            regularStrengthUsed++;
        }
        if ((wholeNumberDial > dialUsed) || plusHalfDial) {
            int troopsNeeded = (wholeNumberDial - dialUsed) * 2 + (plusHalfDial ? 1 : 0);
            regularStrengthUsed += troopsNeeded;
        }
        if (regularStrengthUsed > regularStrength || specialStrengthUsed > specialStrength)
            throw new InvalidGameStateException(faction.getEmoji() + " does not have enough troops in the territory.");
        if (spice > spiceUsed)
            faction.getChat().publish("This dial can be supported with " + spiceUsed + " " + Emojis.SPICE);
    }

    public BattlePlan setBattlePlan(Game game, Faction faction, Leader leader, TreacheryCard cheapHero, boolean kwisatzHaderach, int wholeNumberDial, boolean plusHalfDial, int spice, TreacheryCard weapon, TreacheryCard defense) throws InvalidGameStateException {
        int actualSize = factions == null ? factionNames.size() : factions.size();
        int numFactionsExpected = hasEcazAndAlly(game) ? 3 : 2;
        if (actualSize != numFactionsExpected)
            throw new InvalidGameStateException("Combatants not determined yet.");
//        if (ecazAllyToBeChosen && (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz")))
//            throw new InvalidGameStateException("Ecaz must choose their alliance combatant.");

        boolean planIsForAggressor = false;
        if (getAggressorName().equals(faction.getName()))
            planIsForAggressor = true;
        else if (!getDefenderName().equals(faction.getName()))
            throw new InvalidGameStateException(faction.getEmoji() + " is not in this battle.");

        if (leader != null && cheapHero != null)
            throw new InvalidGameStateException(faction.getEmoji() + " cannot play both a leader and " + cheapHero.name());
        if (leader != null && !faction.getLeaders().contains(leader))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + leader);
        if (cheapHero != null && !faction.hasTreacheryCard(cheapHero.name()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + cheapHero.name());
        if (leader == null && cheapHero == null && !faction.getLeaders().stream().filter(l -> !l.name().equals("Kwisatz Haderach")).toList().isEmpty())
            throw new InvalidGameStateException(faction.getEmoji() + " must play a leader or a Cheap Hero");
        if (kwisatzHaderach) {
            if (leader == null && cheapHero == null)
                throw new InvalidGameStateException("A leader or Cheap Hero must be played to use the Kwisatz Haderach");
            if (!(faction instanceof AtreidesFaction))
                throw new InvalidGameStateException("Only " + Emojis.ATREIDES + " can have the Kwisatz Haderach");
            if (!((AtreidesFaction) faction).isHasKH())
                throw new InvalidGameStateException("Only " + ((AtreidesFaction) faction).getForcesLost() + " " + Emojis.getForceEmoji("Atreides") + " killed in battle. 7 required for Kwisatz Haderach");
        }

        if (spice > (faction.getSpice() + faction.getAllySpiceShipment()))
            throw new InvalidGameStateException(faction.getEmoji() + " does not have " + spice + " " + Emojis.SPICE);
        if (weapon != null) {
            if (!faction.hasTreacheryCard(weapon.name()))
                throw new InvalidGameStateException(faction.getEmoji() + " does not have " + weapon.name());
            else if (weapon.name().equals("Chemistry") && (defense == null || !defense.type().equals("Defense - Poison")))
                throw new InvalidGameStateException("Chemistry can only be played as a weapon when playing a Poison Defense");
            else if (weapon.name().equals("Harass and Withdraw") && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on Homeworld");
            else if (weapon.name().equals("Reinforcements") && (faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength() < 3))
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
        }
        if (defense != null) {
            if (!faction.hasTreacheryCard(defense.name()))
                throw new InvalidGameStateException(faction.getEmoji() + " does not have " + defense.name());
            else if (defense.name().equals("Weirding Way") && (weapon == null || !weapon.type().equals("Weapon - Projectile")))
                throw new InvalidGameStateException("Weirding Way can only be played as a defense when playing a Projectile Weapon");
            else if (defense.name().equals("Harass and Withdraw") && (faction.getHomeworld().equals(wholeTerritoryName) || (faction instanceof EmperorFaction emperor && emperor.getSecondHomeworld().equals(wholeTerritoryName))))
                throw new InvalidGameStateException("Harass and Withdraw cannot be used on Homeworld");
            else if (defense.name().equals("Reinforcements") && (faction.getReserves().getStrength() + faction.getSpecialReserves().getStrength() < 3))
                throw new InvalidGameStateException("There must be at least 3 forces in reserves to use Reinformcements");
        }

        validateDial(game, faction, wholeNumberDial, plusHalfDial, spice);

        BattlePlan battlePlan = new BattlePlan(leader, cheapHero, kwisatzHaderach, wholeNumberDial, plusHalfDial, spice, weapon, defense);
        if (planIsForAggressor) {
            aggressorBattlePlan = battlePlan;
        } else {
            defenderBattlePlan = battlePlan;
        }
        return battlePlan;
    }

    public boolean isNotResolvable() {
        return aggressorBattlePlan == null || defenderBattlePlan == null;
    }

    public String getAggressorEmojis(Game game) {
        Faction aggressor = getAggressor(game);
        StringBuilder resolution = new StringBuilder();
        resolution.append(aggressor.getEmoji());
        if (hasEcazAndAlly(game)) {
            if (aggressor instanceof EcazFaction)
                resolution.append(game.getFaction(aggressor.getAlly()).getEmoji());
            else if (aggressor.getAlly().equals("Ecaz"))
                resolution.append(Emojis.getFactionEmoji("Ecaz"));
        }
        return resolution.toString();
    }

    public String getDefenderEmojis(Game game) {
        Faction defender = getDefender(game);
        StringBuilder resolution = new StringBuilder();
        resolution.append(defender.getEmoji());
        if (hasEcazAndAlly(game)) {
            if (defender instanceof EcazFaction)
                resolution.append(game.getFaction(defender.getAlly()).getEmoji());
            else if (defender.getAlly().equals("Ecaz"))
                resolution.append(Emojis.getFactionEmoji("Ecaz"));
        }
        return resolution.toString();
    }

    public BattlePlan getAggressorBattlePlan() {
        return aggressorBattlePlan;
    }

    public BattlePlan getDefenderBattlePlan() {
        return defenderBattlePlan;
    }

    private boolean leaderSurvives(TreacheryCard weapon, TreacheryCard defense) {
        if (weapon != null) {
            if (defense == null)
                return false;
            else if ((weapon.type().equals("Weapon - Poison") || weapon.name().equals("Chemistry"))
                    && !(defense.type().equals("Defense - Poison") || defense.name().equals("Chemistry")))
                return false;
            else if ((weapon.type().equals("Weapon - Projectile") || weapon.name().equals("Weirding Way"))
                    && !(defense.type().equals("Defense - Projectile") || defense.name().equals("Weirding Way")))
                return false;
        }
        return true;
    }

    public boolean isAggressorLeaderAlive() throws InvalidGameStateException {
        if (isNotResolvable())
            throw new InvalidGameStateException("Battle cannot be resolved yet. Missing battle plan(s).");
        return leaderSurvives(defenderBattlePlan.getWeapon(), aggressorBattlePlan.getDefense());
    }

    public boolean isDefenderLeaderAlive() throws InvalidGameStateException {
        if (isNotResolvable())
            throw new InvalidGameStateException("Battle cannot be resolved yet. Missing battle plan(s).");
        return leaderSurvives(aggressorBattlePlan.getWeapon(), defenderBattlePlan.getDefense());

    }

    private int getDoubleBattleStrength(Faction faction, BattlePlan battlePlan, boolean isLeaderAlive) {
        int doubleBattleStrength = 2 * battlePlan.getWholeNumberDial();
        if (battlePlan.getPlusHalfDial()) doubleBattleStrength++;
        if (isLeaderAlive) doubleBattleStrength += 2 * battlePlan.getLeaderStrengthWithKH();
        int ecazStrength = 0;
        if (faction instanceof EcazFaction || faction.getAlly().equals("Ecaz"))
            ecazStrength = forces.stream().filter(f -> f.getFactionName().equals("Ecaz")).map(Force::getStrength).findFirst().orElse(0);
        doubleBattleStrength += 2 * Math.ceilDiv(ecazStrength, 2);
        return doubleBattleStrength;
    }

    public boolean isAggressorWin(Game game) throws InvalidGameStateException {
        if (isNotResolvable())
            throw new InvalidGameStateException("Battle cannot be resolved yet. Missing battle plan(s).");

        int doubleAggressorStrength = getDoubleBattleStrength(getAggressor(game), aggressorBattlePlan, isAggressorLeaderAlive());
        int doubleDefenderStrength = getDoubleBattleStrength(getDefender(game), defenderBattlePlan, isDefenderLeaderAlive());
        return doubleAggressorStrength >= doubleDefenderStrength;
    }

    public String getWinnerEmojis(Game game) throws InvalidGameStateException {
        return isAggressorWin(game) ? getAggressorEmojis(game) : getDefenderEmojis(game);
    }

    public String getAggressorStrengthString(Game game) throws InvalidGameStateException {
        int doubleBattleStrength = getDoubleBattleStrength(getAggressor(game), aggressorBattlePlan, isAggressorLeaderAlive());
        int wholeNumber = doubleBattleStrength / 2;
        return MessageFormat.format("{0}{1}", wholeNumber, aggressorBattlePlan.getPlusHalfDial() ? ".5" : "");
    }

    public String getDefenderStrengthString(Game game) throws InvalidGameStateException {
        int doubleBattleStrength = getDoubleBattleStrength(getDefender(game), defenderBattlePlan, isDefenderLeaderAlive());
        int wholeNumber = doubleBattleStrength / 2;
        return MessageFormat.format("{0}{1}", wholeNumber, defenderBattlePlan.getPlusHalfDial() ? ".5" : "");
    }

    public String getWinnerStrengthString(Game game) throws InvalidGameStateException {
        if (isAggressorWin(game)) return getAggressorStrengthString(game);
        return getDefenderStrengthString(game);
    }

    public String getLoserStrengthString(Game game) throws InvalidGameStateException {
        if (!isAggressorWin(game)) return getAggressorStrengthString(game);
        return getDefenderStrengthString(game);
    }

    public boolean isWeaponDiscarded(Game game, boolean isAggressor) throws InvalidGameStateException {
        BattlePlan battlePlan = isAggressor ? aggressorBattlePlan : defenderBattlePlan;
        boolean isLoser = isAggressor != isAggressorWin(game);
        return isLoser && !(battlePlan.getWeapon() == null);
    }

    public boolean isDefenseDiscarded(Game game, boolean isAggressor) throws InvalidGameStateException {
        BattlePlan battlePlan = isAggressor ? aggressorBattlePlan : defenderBattlePlan;
        boolean isLoser = isAggressor != isAggressorWin(game);
        return isLoser && !(battlePlan.getDefense() == null);
    }
}
